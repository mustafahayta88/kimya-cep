package com.kimya.uygulama.features

import android.graphics.Color
import kotlin.math.*

// =====================================================================
// OPTİK MOTORU — 2D recursive raycasting (ışın izleme)
//
// Mimari:
//  1. Tüm nesneler `elements` listesinde tutulur (dinamik dizi).
//  2. Her ışık kaynağı için bir ana ışın üretilir.
//  3. cast() ışını ilerletir, çarptığı İLK nesneyi bulur (nearestHit),
//     fizik kuralını uygular ve SONUCU YENİDEN cast() ile izler
//     (özyineleme/recursion + derinlik sınırı).
//  4. Her çarpışma bir RaySeg (çizgi parçası) üretir; View bunları çizer.
// =====================================================================

// ---------- 2D vektör yardımcıları ----------
data class Vec(val x: Float, val y: Float) {
    operator fun plus(o: Vec) = Vec(x + o.x, y + o.y)
    operator fun minus(o: Vec) = Vec(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec(x * k, y * k)
    fun dot(o: Vec) = x * o.x + y * o.y
    fun cross(o: Vec) = x * o.y - y * o.x
    fun len() = sqrt(x * x + y * y)
    fun norm(): Vec {
        val l = len()
        return if (l < 1e-9f) Vec(1f, 0f) else Vec(x / l, y / l)
    }
}

/** Açı (derece, 0°=sağa, saat yönü +) → birim yön vektörü */
fun deg2dir(aDeg: Float): Vec {
    val r = aDeg * PI.toFloat() / 180f
    return Vec(cos(r), sin(r))
}

// ---------- Optik nesneler ----------

/** Cam cisimler (prizma/plaka): ışın bunlardan birinin içindeyken medium olarak taşınır */
interface GlassBody {
    val cx: Float
    val cy: Float
}

/** Işık yayanlar: renk + açma/kapama */
interface LightEmitter {
    var colorMode: Int
    var on: Boolean
}

sealed class OpticElement {
    var id: Int = 0
    abstract var cx: Float
    abstract var cy: Float
    /** Yön (derece). Kaynak: ışın yönü. Ayna/Filtre: çizgi yönü. Prizma: döndürme. */
    abstract var angleDeg: Float
    abstract val title: String
}

/** Düz hatta tek ışın yayan kaynak */
class LightSource(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = 0f
) : OpticElement(), LightEmitter {
    override val title = "Işık Kaynağı"
    /** 0=beyaz, 1=kırmızı, 2=yeşil, 3=mavi */
    override var colorMode: Int = 0
    override var on: Boolean = true
    var bodyR: Float = 26f
    fun emitDir() = deg2dir(angleDeg)
    fun emitPoint(): Vec = Vec(cx, cy) + emitDir() * (bodyR + 2f)
}

/** Gelme açısı = yansıma açısı */
class FlatMirror(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = 0f
) : OpticElement() {
    override val title = "Düz Ayna"
    var len: Float = 170f
    fun ends(): Pair<Vec, Vec> {
        val d = deg2dir(angleDeg)
        val c = Vec(cx, cy)
        return Pair(c - d * (len / 2f), c + d * (len / 2f))
    }
}

/** Snell kırılması + beyaz ışıkta dispersiyon (renklere ayrılma) */
class GlassPrism(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = 0f
) : OpticElement(), GlassBody {
    override val title = "Cam Prizma"
    /** Çevresel yarıçap (üçgen boyutu) */
    var size: Float = 84f
    /** Eşkenar üçgen; 0°'de bir kenar sola bakar */
    fun vertices(): List<Vec> {
        return List(3) { k ->
            val a = (angleDeg + k * 120f) * PI.toFloat() / 180f
            Vec(cx + cos(a) * size, cy + sin(a) * size)
        }
    }
    fun edges(): List<Pair<Vec, Vec>> {
        val v = vertices()
        return listOf(Pair(v[0], v[1]), Pair(v[1], v[2]), Pair(v[2], v[0]))
    }
}

/** Kendi rengini geçirir, diğerlerini soğurur */
class LightFilter(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = 0f
) : OpticElement() {
    override val title = "Renk Filtresi"
    var len: Float = 150f
    /** 0=kırmızı, 1=yeşil, 2=mavi */
    var filter: Int = 0
    fun ends(): Pair<Vec, Vec> {
        val d = deg2dir(angleDeg)
        val c = Vec(cx, cy)
        return Pair(c - d * (len / 2f), c + d * (len / 2f))
    }
}

/** İnce mercek (paraksiyal): paralel demeti odağa toplar ya da dağıtır */
class ThinLens(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = 0f,
    var converging: Boolean = true
) : OpticElement() {
    override val title: String get() = if (converging) "Yakınsak Mercek" else "Iraksak Mercek"
    var len: Float = 170f
    /** Odak uzaklığı (px). Iraksakta -f kullanılır */
    var focal: Float = 220f
    fun ends(): Pair<Vec, Vec> {
        val d = deg2dir(angleDeg)
        val c = Vec(cx, cy)
        return Pair(c - d * (len / 2f), c + d * (len / 2f))
    }
}

/** Işığı geçirmeyen kare blok (gölge deneyleri) */
class SquareBlocker(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = 0f
) : OpticElement() {
    override val title = "Engel"
    var half: Float = 34f
    fun corners(): List<Vec> {
        val r = angleDeg * PI.toFloat() / 180f
        val co = cos(r); val si = sin(r)
        return listOf(Vec(-half, -half), Vec(half, -half), Vec(half, half), Vec(-half, half))
            .map { Vec(cx + it.x * co - it.y * si, cy + it.x * si + it.y * co) }
    }
    fun edges(): List<Pair<Vec, Vec>> {
        val v = corners()
        return listOf(Pair(v[0], v[1]), Pair(v[1], v[2]), Pair(v[2], v[3]), Pair(v[3], v[0]))
    }
}

/** Gelen ışığı yansıyan + geçen olarak ikiye böler */
class BeamSplitter(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = -45f
) : OpticElement() {
    override val title = "Yarı Geçirgen Ayna"
    var len: Float = 170f
    fun ends(): Pair<Vec, Vec> {
        val d = deg2dir(angleDeg)
        val c = Vec(cx, cy)
        return Pair(c - d * (len / 2f), c + d * (len / 2f))
    }
}

/** Cam plaka (paralel yüz): kırar + ışını paralel kaydırır */
class GlassSlab(
    override var cx: Float,
    override var cy: Float,
    override var angleDeg: Float = -20f
) : OpticElement(), GlassBody {
    override val title = "Cam Plaka"
    var w: Float = 170f
    var h: Float = 64f
    fun corners(): List<Vec> {
        val r = angleDeg * PI.toFloat() / 180f
        val co = cos(r); val si = sin(r)
        return listOf(Vec(-w / 2f, -h / 2f), Vec(w / 2f, -h / 2f), Vec(w / 2f, h / 2f), Vec(-w / 2f, h / 2f))
            .map { Vec(cx + it.x * co - it.y * si, cy + it.x * si + it.y * co) }
    }
    fun edges(): List<Pair<Vec, Vec>> {
        val v = corners()
        return listOf(Pair(v[0], v[1]), Pair(v[1], v[2]), Pair(v[2], v[3]), Pair(v[3], v[0]))
    }
}

/** Her yöne ışın yayan noktasal kaynak */
class PointSource(
    override var cx: Float,
    override var cy: Float
) : OpticElement(), LightEmitter {
    override val title = "Noktasal Kaynak"
    override var angleDeg: Float = 0f
    override var colorMode: Int = 0
    override var on: Boolean = true
    var bodyR: Float = 22f
    var rayCount: Int = 18
}

/** Ekrana çizilen ışın parçası */
data class RaySeg(
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val color: Int, val intensity: Float
)

/** Tayf bandı: dalgaboyu → renk + cama göre kırılma indisi */
data class SpecBand(val lambda: Float, val color: Int, val n: Float)

class OpticsEngine {

    val elements = mutableListOf<OpticElement>()
    var selected: OpticElement? = null
        private set

    fun select(e: OpticElement?) { selected = e }

    companion object {
        const val MAX_DEPTH = 7
        const val MAX_SEGS = 800
        const val MAX_ELEMENTS = 15
        const val EPS_T = 0.5f   // aynı yüzeyin tekrar yakalanmaması için eşik (px)
        const val EPS_PUSH = 0.6f

        // NOT: Telefonda görünür gökkuşağı için dispersiyon abartıldı
        // (gerçek camda kırmızı-mor farkı ~0.02, burada ~0.22)
        val SPECTRUM = listOf(
            SpecBand(640f, Color.rgb(255, 60, 40), 1.46f),
            SpecBand(610f, Color.rgb(255, 140, 0), 1.50f),
            SpecBand(580f, Color.rgb(255, 225, 0), 1.53f),
            SpecBand(530f, Color.rgb(60, 220, 80), 1.57f),
            SpecBand(500f, Color.rgb(60, 220, 230), 1.60f),
            SpecBand(460f, Color.rgb(70, 130, 255), 1.64f),
            SpecBand(410f, Color.rgb(170, 90, 255), 1.68f)
        )

        fun nOf(lambda: Float?): Float {
            if (lambda == null) return 1.55f
            return SPECTRUM.minByOrNull { abs(it.lambda - lambda) }?.n ?: return 1.55f
        }

        fun lambdaOfSource(mode: Int): Float? = when (mode) {
            1 -> 640f
            2 -> 530f
            3 -> 460f
            else -> null // beyaz
        }

        fun colorOfSource(mode: Int): Int = when (mode) {
            1 -> Color.rgb(255, 60, 40)
            2 -> Color.rgb(60, 220, 80)
            3 -> Color.rgb(70, 130, 255)
            else -> Color.WHITE
        }

        fun sourceColorName(mode: Int) = when (mode) {
            1 -> "KIRMIZI"
            2 -> "YEŞİL"
            3 -> "MAVİ"
            else -> "BEYAZ"
        }

        fun filterColorName(f: Int) = when (f) {
            1 -> "YEŞİL"
            2 -> "MAVİ"
            else -> "KIRMIZI"
        }

        fun filterColor(f: Int): Int = when (f) {
            1 -> Color.rgb(60, 220, 80)
            2 -> Color.rgb(70, 130, 255)
            else -> Color.rgb(255, 60, 40)
        }

        /** Filtre maskesi: ışın renginden geçen bileşen; null = soğuruldu */
        fun applyFilter(rayColor: Int, f: Int): Int? {
            val r = Color.red(rayColor); val g = Color.green(rayColor); val b = Color.blue(rayColor)
            val nr = if (f == 0) r else 0
            val ng = if (f == 1) g else 0
            val nb = if (f == 2) b else 0
            return if (maxOf(nr, ng, nb) < 14) null else Color.rgb(nr, ng, nb)
        }

        /** Yansıma: R = I - 2(I·N)N (N'in yönü fark etmez) */
        fun reflect(d: Vec, n: Vec): Vec {
            val nn = n.norm()
            return (d - nn * (2f * d.dot(nn))).norm()
        }

        /**
         * Snell kırılması (vektörel).
         * @param nOut yüzeyin DIŞA bakan normali
         * @return kırılan yön ya da null (tam iç yansıma / TIR)
         */
        fun refract(d: Vec, nOut: Vec, n1: Float, n2: Float): Vec? {
            val n = if (d.dot(nOut) < 0f) nOut.norm() else nOut.norm() * -1f
            val cosI = -(d.dot(n))
            val eta = n1 / n2
            val k = 1f - eta * eta * (1f - cosI * cosI)
            if (k < 0f) return null
            return (d * eta + n * (eta * cosI - sqrt(k))).norm()
        }

        /** Işın-parça kesişimi: ışın üzerindeki t mesafesini verir ya da null */
        fun raySeg(ox: Float, oy: Float, dx: Float, dy: Float,
                   ax: Float, ay: Float, bx: Float, by: Float): Float? {
            val sx = bx - ax; val sy = by - ay
            val cr = dx * sy - dy * sx
            if (abs(cr) < 1e-9f) return null // paralel
            val qpx = ax - ox; val qpy = ay - oy
            val t = (qpx * sy - qpy * sx) / cr
            val u = (qpx * dy - qpy * dx) / cr
            if (t > EPS_T && u >= 0f && u <= 1f) return t
            return null
        }
    }

    private var nextId = 1
    private fun track(e: OpticElement): OpticElement {
        e.id = nextId++
        elements.add(e)
        return e
    }

    fun addSource(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(LightSource(x, y))
    }

    fun addMirror(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(FlatMirror(x, y, -45f))
    }

    fun addPrism(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(GlassPrism(x, y, 40f))
    }

    fun addFilter(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(LightFilter(x, y, 90f))
    }

    fun addLensConv(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(ThinLens(x, y, 90f, true))
    }

    fun addLensDiv(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(ThinLens(x, y, 90f, false))
    }

    fun addBlocker(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(SquareBlocker(x, y, 0f))
    }

    fun addBeamSplitter(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(BeamSplitter(x, y, -45f))
    }

    fun addSlab(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(GlassSlab(x, y, -20f))
    }

    fun addPoint(x: Float, y: Float): OpticElement? {
        if (elements.size >= MAX_ELEMENTS) return null
        return track(PointSource(x, y))
    }

    fun removeSelected() {
        selected?.let { elements.remove(it) }
        selected = null
    }

    fun clear() {
        elements.clear()
        selected = null
    }

    private var seeded = false

    /** Açılış sahnesi: beyaz kaynak + prizma (anında gökkuşağı). Yalnızca 1 kez. */
    fun seedDefault(w: Float, h: Float) {
        if (seeded || elements.isNotEmpty()) return
        seeded = true
        track(LightSource(w * 0.16f, h * 0.50f, 0f))
        track(GlassPrism(w * 0.56f, h * 0.52f, 40f))
    }

    // ---------------- Işın izleme ----------------

    private sealed class Hit {
        class Mirror(val el: FlatMirror, val t: Float) : Hit()
        class Prism(val el: GlassPrism, val t: Float, val a: Vec, val b: Vec) : Hit()
        class Filter(val el: LightFilter, val t: Float) : Hit()
        class Lens(val el: ThinLens, val t: Float) : Hit()
        class Block(val el: SquareBlocker, val t: Float) : Hit()
        class BS(val el: BeamSplitter, val t: Float) : Hit()
        class Slab(val el: GlassSlab, val t: Float, val a: Vec, val b: Vec) : Hit()
    }

    private val out = mutableListOf<RaySeg>()

    /** Tüm kaynaklardan ışınları izle; çizilecek parçaları döndür */
    fun trace(W: Float, H: Float): List<RaySeg> {
        out.clear()
        for (s in elements.filterIsInstance<LightSource>()) {
            if (!s.on) continue
            val d = s.emitDir()
            val p = s.emitPoint()
            cast(p.x, p.y, d.x, d.y,
                colorOfSource(s.colorMode), lambdaOfSource(s.colorMode),
                1f, 0, null, W, H)
        }
        for (s in elements.filterIsInstance<PointSource>()) {
            if (!s.on) continue
            val col = colorOfSource(s.colorMode)
            val lam = lambdaOfSource(s.colorMode)
            for (k in 0 until s.rayCount) {
                val d = deg2dir(k * 360f / s.rayCount)
                cast(s.cx + d.x * (s.bodyR + 2f), s.cy + d.y * (s.bodyR + 2f),
                    d.x, d.y, col, lam, 0.8f, 0, null, W, H)
            }
        }
        return out
    }

    /** Schlick yaklaşıklığı: Fresnel yansıma oranı (0..1) */
    private fun schlick(cosI: Float, n1: Float, n2: Float): Float {
        val r0 = ((n1 - n2) / (n1 + n2)).let { it * it }
        val m = 1f - cosI.coerceIn(0f, 1f)
        return (r0 + (1f - r0) * m * m * m * m * m).coerceIn(0f, 1f)
    }

    /** Kenarın dışa bakan normali (ağırlık merkezi testiyle) */
    private fun outwardNormal(a: Vec, b: Vec, cx: Float, cy: Float): Vec {
        var n = Vec(-(b.y - a.y), b.x - a.x).norm()
        val mid = Vec((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        if (n.dot(Vec(cx, cy) - mid) > 0f) n = n * -1f
        return n
    }

    /** Hava → cam: yansıyan (Fresnel) + kırılan */
    private fun enterGlass(ex: Float, ey: Float, dx: Float, dy: Float,
                           color: Int, lambda: Float?, intensity: Float,
                           depth: Int, body: GlassBody, n: Vec, W: Float, H: Float) {
        val d = Vec(dx, dy)
        val nG = nOf(lambda)
        val cosI = abs(d.dot(n))
        val R = schlick(cosI, 1f, nG)
        val r = reflect(d, n)
        if (R > 0.01f) {
            cast(ex + r.x * EPS_PUSH, ey + r.y * EPS_PUSH, r.x, r.y,
                color, lambda, intensity * R, depth + 1, null, W, H)
        }
        val refr = refract(d, n, 1f, nG) ?: return
        cast(ex + refr.x * EPS_PUSH, ey + refr.y * EPS_PUSH, refr.x, refr.y,
            color, lambda, intensity * (1f - R), depth + 1, body, W, H)
    }

    /**
     * Cam → hava: iç yansıma + kırılan.
     * @param disperse true = beyaz 7 renge ayrılır (prizma);
     *                 false = beyaz yönünü koruyup paralel çıkar (plaka teoremi)
     */
    private fun exitGlass(ex: Float, ey: Float, dx: Float, dy: Float,
                          color: Int, lambda: Float?, intensity: Float,
                          depth: Int, body: GlassBody, n: Vec,
                          disperse: Boolean, W: Float, H: Float) {
        val d = Vec(dx, dy)
        val nG = nOf(lambda)
        val cosI = abs(d.dot(n))
        val R = schlick(cosI, nG, 1f)
        // İç yansıma (TIR dahil: o durumda R≈1 olur)
        val r = reflect(d, n)
        if (R > 0.01f) {
            cast(ex + r.x * EPS_PUSH, ey + r.y * EPS_PUSH, r.x, r.y,
                color, lambda, intensity * R, depth + 1, body, W, H)
        }
        if (lambda == null && disperse) {
            for (band in SPECTRUM) {
                val refr = refract(d, n, band.n, 1f) ?: continue // o bant TIR'da kaldı
                cast(ex + refr.x * EPS_PUSH, ey + refr.y * EPS_PUSH, refr.x, refr.y,
                    band.color, band.lambda, intensity * (1f - R) * 0.7f, depth + 1, null, W, H)
            }
        } else if (lambda == null) {
            // Paralel yüz: yön tamamen korunur, ışın yalnız yana kayar
            val refr = refract(d, n, 1.55f, 1f) ?: return
            cast(ex + refr.x * EPS_PUSH, ey + refr.y * EPS_PUSH, refr.x, refr.y,
                color, lambda, intensity * (1f - R), depth + 1, null, W, H)
        } else {
            val refr = refract(d, n, nG, 1f) ?: return // tam iç yansıma (yansıyan zaten eklendi)
            cast(ex + refr.x * EPS_PUSH, ey + refr.y * EPS_PUSH, refr.x, refr.y,
                color, lambda, intensity * (1f - R), depth + 1, null, W, H)
        }
    }

    /**
     * Özyinelemeli ışın: ilerle → İLK çarpmayı bul → fizik uygula → devam ışın(lar)ını izle.
     * @param lambda null = beyaz ışık (dağılabilir), sayı = dalgaboyu (nm)
     * @param medium null = hava, cam cismi = o camın içindeyiz
     */
    private fun cast(ox: Float, oy: Float, dx: Float, dy: Float,
                     color: Int, lambda: Float?, intensity: Float,
                     depth: Int, medium: GlassBody?, W: Float, H: Float) {
        if (depth > MAX_DEPTH || out.size >= MAX_SEGS || intensity < 0.04f) return

        val tB = boundT(ox, oy, dx, dy, W, H)
        val hit = nearestHit(ox, oy, dx, dy)
        val tEnd = if (hit != null) minOf(hitT(hit), tB) else tB
        val ex = ox + dx * tEnd
        val ey = oy + dy * tEnd
        out.add(RaySeg(ox, oy, ex, ey, color, intensity))
        if (hit == null || hitT(hit) >= tB) return // sahneden çıktı

        // KRİTİK DÜZELTME: devam noktası HER DALDA yeni yöne göre itilir.
        // Eski yönle itilince yansıyan ışın aynı yüzeyi tekrar yakalayıp
        // ping-pong yapıyor ve derinlik sınırında sönüyordu!
        when (hit) {
            is Hit.Mirror -> {
                val (a, b) = hit.el.ends()
                val n = Vec(-(b.y - a.y), b.x - a.x)
                val r = reflect(Vec(dx, dy), n)
                cast(ex + r.x * EPS_PUSH, ey + r.y * EPS_PUSH, r.x, r.y,
                    color, lambda, intensity * 0.95f, depth + 1, medium, W, H)
            }
            is Hit.Filter -> {
                if (medium != null) {
                    // Cam içinde filtre yok sayılır: düz devam
                    cast(ex + dx * EPS_PUSH, ey + dy * EPS_PUSH, dx, dy,
                        color, lambda, intensity, depth, medium, W, H)
                    return
                }
                // Gerçekçi yüzey parlaması (Fresnel ~%6)
                val (fa, fb) = hit.el.ends()
                val fn = Vec(-(fb.y - fa.y), fb.x - fa.x)
                val fr = reflect(Vec(dx, dy), fn)
                cast(ex + fr.x * EPS_PUSH, ey + fr.y * EPS_PUSH, fr.x, fr.y,
                    color, lambda, intensity * 0.06f, depth + 1, null, W, H)
                val passed = applyFilter(color, hit.el.filter) ?: return // soğuruldu
                val nl = if (color == Color.WHITE) lambdaOfFilter(hit.el.filter) else lambda
                cast(ex + dx * EPS_PUSH, ey + dy * EPS_PUSH, dx, dy,
                    passed, nl, intensity * 0.85f, depth, null, W, H)
            }
            is Hit.Lens -> {
                if (medium != null) {
                    cast(ex + dx * EPS_PUSH, ey + dy * EPS_PUSH, dx, dy,
                        color, lambda, intensity, depth, medium, W, H)
                    return
                }
                // İnce mercek (paraksiyal): sapma = -h/f (eksen = segment normali)
                val el = hit.el
                val (la, lb) = el.ends()
                var ax = Vec(-(lb.y - la.y), lb.x - la.x).norm()
                if (dx * ax.x + dy * ax.y < 0f) ax = ax * -1f
                val nx = Vec(-ax.y, ax.x)
                val h = (ex - el.cx) * nx.x + (ey - el.cy) * nx.y
                val dPar = dx * ax.x + dy * ax.y
                if (abs(dPar) < 1e-6f) return
                // Yüzey parlaması (mercek kaplamasız cam ~%4)
                val R = schlick(abs(dPar), 1f, 1.52f)
                if (R > 0.02f) {
                    val r = reflect(Vec(dx, dy), nx)
                    cast(ex + r.x * EPS_PUSH, ey + r.y * EPS_PUSH, r.x, r.y,
                        color, lambda, intensity * R, depth + 1, null, W, H)
                }
                val s1 = (dx * nx.x + dy * nx.y) / dPar
                val f = if (el.converging) el.focal else -el.focal
                val s2 = s1 - h / f
                val nd = (ax + nx * s2).norm()
                cast(ex + nd.x * EPS_PUSH, ey + nd.y * EPS_PUSH, nd.x, nd.y,
                    color, lambda, intensity * (1f - R), depth + 1, null, W, H)
            }
            is Hit.Block -> {
                // Engel soğurur (parça zaten eklendi); cam içindeyse yok say
                if (medium != null) {
                    cast(ex + dx * EPS_PUSH, ey + dy * EPS_PUSH, dx, dy,
                        color, lambda, intensity, depth, medium, W, H)
                }
                return
            }
            is Hit.BS -> {
                val (a, b) = hit.el.ends()
                val n = Vec(-(b.y - a.y), b.x - a.x)
                val r = reflect(Vec(dx, dy), n)
                cast(ex + r.x * EPS_PUSH, ey + r.y * EPS_PUSH, r.x, r.y,
                    color, lambda, intensity * 0.45f, depth + 1, medium, W, H)
                cast(ex + dx * EPS_PUSH, ey + dy * EPS_PUSH, dx, dy,
                    color, lambda, intensity * 0.45f, depth + 1, medium, W, H)
            }
            is Hit.Prism -> {
                val pr = hit.el
                val n = outwardNormal(hit.a, hit.b, pr.cx, pr.cy)
                if (medium !== pr) {
                    enterGlass(ex, ey, dx, dy, color, lambda, intensity, depth, pr, n, W, H)
                } else {
                    exitGlass(ex, ey, dx, dy, color, lambda, intensity, depth, pr, n, true, W, H)
                }
            }
            is Hit.Slab -> {
                val sl = hit.el
                val n = outwardNormal(hit.a, hit.b, sl.cx, sl.cy)
                if (medium !== sl) {
                    enterGlass(ex, ey, dx, dy, color, lambda, intensity, depth, sl, n, W, H)
                } else {
                    exitGlass(ex, ey, dx, dy, color, lambda, intensity, depth, sl, n, false, W, H)
                }
            }
        }
    }

    private fun hitT(h: Hit): Float = when (h) {
        is Hit.Mirror -> h.t
        is Hit.Prism -> h.t
        is Hit.Filter -> h.t
        is Hit.Lens -> h.t
        is Hit.Block -> h.t
        is Hit.BS -> h.t
        is Hit.Slab -> h.t
    }

    private fun lambdaOfFilter(f: Int): Float? = when (f) {
        0 -> 640f
        1 -> 530f
        2 -> 460f
        else -> null
    }

    /** Işının çarptığı İLK nesne (en küçük t) */
    private fun nearestHit(ox: Float, oy: Float, dx: Float, dy: Float): Hit? {
        var best: Hit? = null
        var bestT = Float.MAX_VALUE
        for (e in elements) {
            when (e) {
                is FlatMirror -> {
                    val (a, b) = e.ends()
                    val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                    if (t != null && t < bestT) {
                        bestT = t
                        best = Hit.Mirror(e, t)
                    }
                }
                is LightFilter -> {
                    val (a, b) = e.ends()
                    val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                    if (t != null && t < bestT) {
                        bestT = t
                        best = Hit.Filter(e, t)
                    }
                }
                is GlassPrism -> {
                    for ((a, b) in e.edges()) {
                        val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                        if (t != null && t < bestT) {
                            bestT = t
                            best = Hit.Prism(e, t, a, b)
                        }
                    }
                }
                is ThinLens -> {
                    val (a, b) = e.ends()
                    val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                    if (t != null && t < bestT) {
                        bestT = t
                        best = Hit.Lens(e, t)
                    }
                }
                is SquareBlocker -> {
                    for ((a, b) in e.edges()) {
                        val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                        if (t != null && t < bestT) {
                            bestT = t
                            best = Hit.Block(e, t)
                        }
                    }
                }
                is BeamSplitter -> {
                    val (a, b) = e.ends()
                    val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                    if (t != null && t < bestT) {
                        bestT = t
                        best = Hit.BS(e, t)
                    }
                }
                is GlassSlab -> {
                    for ((a, b) in e.edges()) {
                        val t = raySeg(ox, oy, dx, dy, a.x, a.y, b.x, b.y)
                        if (t != null && t < bestT) {
                            bestT = t
                            best = Hit.Slab(e, t, a, b)
                        }
                    }
                }
                is LightSource -> { /* kaynaklar ışını engellemez */ }
                is PointSource -> { /* kaynaklar ışını engellemez */ }
            }
        }
        return best
    }

    /** Işının ekran sınırına uzaklığı (sonlandırıcı) */
    private fun boundT(ox: Float, oy: Float, dx: Float, dy: Float, W: Float, H: Float): Float {
        var best = Float.MAX_VALUE
        val edges = arrayOf(
            floatArrayOf(0f, 0f, W, 0f),
            floatArrayOf(W, 0f, W, H),
            floatArrayOf(W, H, 0f, H),
            floatArrayOf(0f, H, 0f, 0f)
        )
        for (e in edges) {
            val t = raySeg(ox, oy, dx, dy, e[0], e[1], e[2], e[3])
            if (t != null && t < best) best = t
        }
        if (best == Float.MAX_VALUE) {
            best = sqrt(W * W + H * H)
        }
        return best
    }
}
