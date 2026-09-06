package com.kimya.uygulama.features

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class BiomoleculesFlatView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private var bioType = 0
    private var fadeAlpha = 0f
    private var fadeAnimator: ValueAnimator? = null
    private var breathe = 0f
    private var breatheAnimator: ValueAnimator? = null

    private val bgPaint = Paint().apply { color = 0xFF0D1219.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x08FFFFFF.toInt(); strokeWidth = 0.5f }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF.toInt(); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
    private val boxP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
    private var bgBitmap: Bitmap? = null

    fun setType(t: Int, animate: Boolean = true) {
        bioType = t
        if (animate) {
            fadeAnimator?.cancel(); fadeAlpha = 0f
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply { duration = 350L; interpolator = DecelerateInterpolator(); addUpdateListener { fadeAlpha = it.animatedValue as Float; postInvalidate() }; start() }
        } else { fadeAlpha = 1f; postInvalidate() }
    }

    init {
        isClickable = true; isFocusable = true
        breatheAnimator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = 6000L; interpolator = LinearInterpolator(); repeatCount = ValueAnimator.INFINITE
            addUpdateListener { breathe = it.animatedValue as Float; postInvalidate() }; start()
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); breatheAnimator?.start() }
    override fun onDetachedFromWindow() { breatheAnimator?.cancel(); fadeAnimator?.cancel(); bgBitmap?.recycle(); bgBitmap = null; super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        bgBitmap?.recycle()
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        val gs = 40f; val cols = (w / gs).toInt() + 1; val rows = (h / gs).toInt() + 1
        for (i in 0 until cols) c.drawLine(i * gs, 0f, i * gs, h.toFloat(), gridPaint)
        for (i in 0 until rows) c.drawLine(0f, i * gs, w.toFloat(), i * gs, gridPaint)
        bgBitmap = bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.save(); canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        when (bioType) {
            0 -> drawCarb(canvas); 1 -> drawProtein(canvas); 2 -> drawFat(canvas)
            3 -> drawVitamin(canvas); 4 -> drawDNA(canvas); 5 -> drawEnzyme(canvas)
            6 -> drawHormone(canvas); 7 -> drawMetabolism(canvas)
        }
        canvas.restore()
    }

    private fun S(v: Float) = v * width / 400f
    private fun X(f: Float) = width * f
    private fun Y(f: Float) = height * f
    private fun A(a: Int) = (a * fadeAlpha).toInt()

    private fun drawBar(c: Canvas, x: Float, yt: Float, yb: Float, w: Float, color: Int, lbl: String = "", val_: String = "") {
        barPaint.color = Color.argb(A(220), Color.red(color), Color.green(color), Color.blue(color))
        c.drawRoundRect(x - w / 2, yt, x + w / 2, yb, S(4f), S(4f), barPaint)
        c.drawRoundRect(x - w / 2, yt, x + w / 2, yb, S(4f), S(4f), barBorder)
        if (lbl.isNotEmpty()) { textP.color = Color.argb(A(255), 255, 255, 255); textP.textSize = S(9f); c.drawText(lbl, x, yb + S(13f), textP) }
        if (val_.isNotEmpty()) { smallP.color = Color.argb(A(180), 180, 180, 180); smallP.textSize = S(8f); c.drawText(val_, x, yt - S(4f), smallP) }
    }

    private fun drawBox(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int, txt: String, sub: String = "") {
        boxP.color = Color.argb(A(100), Color.red(color), Color.green(color), Color.blue(color))
        c.drawRoundRect(x - w / 2, y - h / 2, x + w / 2, y + h / 2, S(6f), S(6f), boxP)
        textP.color = Color.argb(A(255), 255, 255, 255); textP.textSize = S(10f); c.drawText(txt, x, y + S(3f), textP)
        if (sub.isNotEmpty()) { smallP.color = Color.argb(A(160), 160, 160, 160); smallP.textSize = S(8f); c.drawText(sub, x, y + S(16f), smallP) }
    }

    private fun title(c: Canvas, t: String, y: Float, clr: Int = 0xFFFF69B4.toInt()) {
        labelP.color = Color.argb(A(220), Color.red(clr), Color.green(clr), Color.blue(clr)); labelP.textSize = S(14f); c.drawText(t, X(0.5f), y, labelP)
    }

    private fun sub(c: Canvas, t: String, x: Float, y: Float) {
        smallP.color = Color.argb(A(160), 160, 160, 160); smallP.textSize = S(9f); c.drawText(t, x, y, smallP)
    }

    private fun hexRing(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val ang = 2f * PI.toFloat() / 6f
        for (i in 0 until 6) {
            val x1 = cx + r * cos(i * ang - PI.toFloat() / 2f); val y1 = cy + r * sin(i * ang - PI.toFloat() / 2f)
            val x2 = cx + r * cos((i + 1) * ang - PI.toFloat() / 2f); val y2 = cy + r * sin((i + 1) * ang - PI.toFloat() / 2f)
            lineP.color = Color.argb(A(200), Color.red(color), Color.green(color), Color.blue(color)); c.drawLine(x1, y1, x2, y2, lineP)
            barPaint.color = Color.argb(A(180), Color.red(color), Color.green(color), Color.blue(color)); c.drawCircle(x1, y1, S(4f), barPaint)
        }
    }

    private fun drawCarb(c: Canvas) {
        title(c, "Karbonhidratlar", Y(0.08f)); sub(c, "Cn(H2O)n - Enerji kaynagi", X(0.5f), Y(0.15f))
        hexRing(c, X(0.28f), Y(0.38f), S(35f), 0xFFFF8C00.toInt()); sub(c, "Glikoz (C6H12O6)", X(0.28f), Y(0.38f) + S(50f))
        hexRing(c, X(0.72f), Y(0.38f), S(28f), 0xFF4DD0E1.toInt()); sub(c, "Fruktoz", X(0.72f), Y(0.38f) + S(42f))
        val items = arrayOf("Glikoz" to 0xFFFF8C00.toInt(), "Sukroz" to 0xFFFFD700.toInt(), "Nisasta" to 0xFF32CD32.toInt(), "Seluloz" to 0xFF8B4513.toInt())
        val bw = S(70f); val gap = S(82f); val sx = X(0.5f) - 1.5f * gap; val by = Y(0.85f)
        for (i in items.indices) { val bh = S(30f) + i * S(8f); drawBox(c, sx + i * gap, by - bh / 2, bw, bh, items[i].second, items[i].first, "") }
        sub(c, "Monomer -> Disakkarit -> Polisakkarit", X(0.5f), Y(0.95f))
    }

    private fun drawProtein(c: Canvas) {
        title(c, "Proteinler", Y(0.08f)); sub(c, "Amino asit zincirleri", X(0.5f), Y(0.15f))
        val aas = arrayOf("Gli" to 0xFFEF4444.toInt(), "Ala" to 0xFFFF8C00.toInt(), "Val" to 0xFFFFD700.toInt(), "Leu" to 0xFF32CD32.toInt(), "Ile" to 0xFF4DD0E1.toInt(), "Pro" to 0xFF8B5CF6.toInt(), "Phe" to 0xFFFF69B4.toInt())
        val gap = S(50f); val sx = X(0.5f) - 3f * gap; val cy = Y(0.32f)
        for (i in aas.indices) {
            val x = sx + i * gap
            if (i > 0) { lineP.color = Color.argb(A(150), 100, 100, 100); c.drawLine(x - gap, cy, x, cy, lineP) }
            barPaint.color = Color.argb(A(200), Color.red(aas[i].second), Color.green(aas[i].second), Color.blue(aas[i].second)); c.drawCircle(x, cy, S(14f), barPaint)
            textP.color = Color.argb(A(255), 255, 255, 255); textP.textSize = S(8f); c.drawText(aas[i].first, x, cy + S(3f), textP)
        }
        sub(c, "Peptit bagi: -CO-NH-", X(0.5f), cy + S(28f))
        val tp = arrayOf("Enzim" to 0xFFFF4444.toInt(), "Kollajen" to 0xFFFF8C00.toInt(), "Hemoglobin" to 0xFFEF4444.toInt(), "Antikor" to 0xFF3B82F6.toInt())
        val bw = S(70f); val gap2 = S(82f); val sx2 = X(0.5f) - 1.5f * gap2
        for (i in tp.indices) drawBox(c, sx2 + i * gap2, Y(0.82f), bw, S(36f), tp[i].second, tp[i].first, "")
        sub(c, "20 amino asit | Esansiyel: 10, Kalanabilen: 10", X(0.5f), Y(0.95f))
    }

    private fun drawFat(c: Canvas) {
        title(c, "Yaglar (Lipitler)", Y(0.08f)); sub(c, "Gliserol + 3 yag asidi", X(0.5f), Y(0.15f))
        val gx = X(0.22f); val gy = Y(0.35f)
        lineP.color = Color.argb(A(200), 0xFF, 0x00, 0x80); lineP.strokeWidth = S(3f)
        c.drawLine(gx, gy - S(30f), gx, gy + S(30f), lineP)
        sub(c, "Gliserol", gx, gy - S(38f))
        for (j in 0 until 3) {
            val sy = gy - S(30f) + j * S(30f)
            barPaint.color = Color.argb(A(200), 0xFF, 0x00, 0x80); c.drawCircle(gx, sy, S(5f), barPaint)
            lineP.color = Color.argb(A(180), 0xFF, 0xD7, 0x00); lineP.strokeWidth = S(2f)
            c.drawLine(gx + S(5f), sy, X(0.75f), sy, lineP)
            for (k in 1..4) { val kx = gx + S(5f) + k * S(30f); barPaint.color = Color.argb(A(150), 0xFF, 0xD7, 0x00); c.drawCircle(kx, sy, S(3f), barPaint) }
        }
        sub(c, "Tekli doymamis: zeytinyagi", X(0.5f), Y(0.62f))
        sub(c, "Coklu doymamis: balik yagi (Omega-3)", X(0.5f), Y(0.68f))
        sub(c, "Doymus: tereyagi, palm yagi", X(0.5f), Y(0.74f))
        sub(c, "9 kcal/g - Enerji depolama | Hucre zar | Isi yalitim", X(0.5f), Y(0.88f))
        sub(c, "Omega-3: balik, ceviz | Omega-6: bitkisel yag", X(0.5f), Y(0.94f))
    }

    private fun drawVitamin(c: Canvas) {
        title(c, "Vitaminler", Y(0.08f)); sub(c, "Az miktarda gerekli - Hayati organik molekuller", X(0.5f), Y(0.15f))
        val vs = arrayOf("A" to 0xFFFF8C00.toInt(), "B12" to 0xFFEF4444.toInt(), "C" to 0xFF81C784.toInt(), "D" to 0xFFFFD700.toInt(), "E" to 0xFF4DD0E1.toInt(), "K" to 0xFF8B5CF6.toInt())
        val cols = 3; val cw = S(110f); val ch = S(70f); val sx = X(0.5f) - cols * cw / 2 + cw / 2; val sy = Y(0.30f)
        for (i in vs.indices) { val col = i % cols; val row = i / cols; drawBox(c, sx + col * cw, sy + row * ch, cw * 0.9f, ch * 0.85f, vs[i].second, vs[i].first, "") }
        sub(c, "Yagda cozunen: A, D, E, K", X(0.5f), Y(0.78f))
        sub(c, "Suda cozunen: B kompleks, C", X(0.5f), Y(0.84f))
        sub(c, "Eksiklik: Avitaminoz | Fazlasi: Hipervitaminoz", X(0.5f), Y(0.90f))
        sub(c, "A: Gorus | C: Antioksidan | D: Kemik | K: Pih", X(0.5f), Y(0.96f))
    }

    private fun drawDNA(c: Canvas) {
        title(c, "DNA - Deoksiribonukleik Asit", Y(0.08f)); sub(c, "Genetik bilgi - Ikili sarmal", X(0.5f), Y(0.15f))
        val n = 10; val gap = S(30f); val sx = X(0.5f) - (n - 1) * gap / 2; val hy = Y(0.45f); val hh = S(60f)
        val pairs = arrayOf(0xFF4DD0E1.toInt() to 0xFFFF4444.toInt(), 0xFF81C784.toInt() to 0xFFFFD700.toInt())
        for (i in 0 until n) {
            val x = sx + i * gap; val off = sin(i * 0.8f + breathe) * hh * 0.4f; val p = pairs[i % 2]
            lineP.color = Color.argb(A(150), 100, 100, 100); lineP.strokeWidth = S(1.5f)
            c.drawLine(x, hy - off - S(20f), x, hy + off + S(20f), lineP)
            barPaint.color = Color.argb(A(200), Color.red(p.first), Color.green(p.first), Color.blue(p.first)); c.drawCircle(x, hy - off - S(20f), S(7f), barPaint)
            barPaint.color = Color.argb(A(200), Color.red(p.second), Color.green(p.second), Color.blue(p.second)); c.drawCircle(x, hy + off + S(20f), S(7f), barPaint)
            val bases = arrayOf("A", "T", "G", "C"); textP.color = Color.argb(A(255), 255, 255, 255); textP.textSize = S(7f)
            c.drawText(bases[i % 4], x, hy - off - S(17f), textP); c.drawText(bases[(i + 2) % 4], x, hy + off + S(23f), textP)
        }
        sub(c, "A=T (2 H-bagi) | G=C (3 H-bagi)", X(0.5f), Y(0.78f))
        sub(c, "Replikasyon: DNA->DNA | Transkripsiyon: DNA->mRNA", X(0.5f), Y(0.85f))
        sub(c, "Insan: 46 kromozom | 3 milyar baz cifti", X(0.5f), Y(0.92f))
    }

    private fun drawEnzyme(c: Canvas) {
        title(c, "Enzimler", Y(0.08f)); sub(c, "Biyoumzel katalizor", X(0.5f), Y(0.15f))
        val ex = X(0.35f); val ey = Y(0.38f); val er = S(50f)
        barPaint.color = Color.argb(A(60), 0x39, 0xFF, 0x14); c.drawCircle(ex, ey, er, barPaint)
        lineP.color = Color.argb(A(180), 0x39, 0xFF, 0x14); lineP.strokeWidth = S(2f); c.drawCircle(ex, ey, er, lineP)
        sub(c, "Enzim", ex, ey - er - S(10f))
        barPaint.color = Color.argb(A(180), 0xFF, 0x44, 0x44); c.drawRoundRect(ex + S(15f) - S(12f), ey - S(18f) - S(8f), ex + S(15f) + S(12f), ey - S(18f) + S(8f), S(4f), S(4f), barPaint)
        sub(c, "Substrat", ex + S(15f), ey - S(18f) - S(16f))
        lineP.color = Color.argb(A(100), 200, 200, 200); c.drawLine(X(0.52f), Y(0.38f), X(0.62f), Y(0.38f), lineP)
        barPaint.color = Color.argb(A(150), 0xFF, 0x44, 0x44); c.drawCircle(X(0.72f), Y(0.32f), S(6f), barPaint)
        barPaint.color = Color.argb(A(150), 0x00, 0xF0, 0xFF); c.drawCircle(X(0.72f), Y(0.46f), S(6f), barPaint)
        sub(c, "Urun 1 + Urun 2", X(0.72f), Y(0.54f))
        sub(c, "Substrat + Enzim -> ES -> Urun + Enzim", X(0.5f), Y(0.70f))
        val fs = arrayOf("Sicaklik" to 0xFFFF4444.toInt(), "pH" to 0xFF4DD0E1.toInt(), "Inhibitor" to 0xFFFFD700.toInt())
        val bw = S(100f); val gap = S(115f); val sx = X(0.5f) - gap
        for (i in fs.indices) drawBox(c, sx + i * gap, Y(0.84f), bw, S(30f), fs[i].second, fs[i].first, "")
        sub(c, "Opt. 37C | pH 6-8 | Denaturasyon: >60C", X(0.5f), Y(0.95f))
    }

    private fun drawHormone(c: Canvas) {
        title(c, "Hormonlar", Y(0.08f)); sub(c, "Iletisim molekulleri - Bezi > Kan > Hedef", X(0.5f), Y(0.15f))
        val hs = arrayOf("Insulin" to 0xFF4DD0E1.toInt(), "Adrenalin" to 0xFFFF4444.toInt(), "Tiroid" to 0xFFFFD700.toInt(), "Testosteron" to 0xFF81C784.toInt(), "Estrojen" to 0xFFFF69B4.toInt(), "Kortizol" to 0xFFFF8C00.toInt())
        val cols = 3; val cw = S(110f); val ch = S(70f); val sx = X(0.5f) - cols * cw / 2 + cw / 2; val sy = Y(0.30f)
        for (i in hs.indices) { val col = i % cols; val row = i / cols; drawBox(c, sx + col * cw, sy + row * ch, cw * 0.9f, ch * 0.85f, hs[i].second, hs[i].first, "") }
        sub(c, "Peptit: Insulin, GH | Steroid: Kortizol, Testosteron", X(0.5f), Y(0.78f))
        sub(c, "Amin: Adrenalin, Tiroid", X(0.5f), Y(0.84f))
        sub(c, "Endokrin bezlerden salgilanir, kana karisir", X(0.5f), Y(0.90f))
        sub(c, "Geri besleme ile kontrol | Metabolizma regulasyonu", X(0.5f), Y(0.96f))
    }

    private fun drawMetabolism(c: Canvas) {
        title(c, "Metabolizma", Y(0.08f)); sub(c, "Katabolizma vs Anabolizma", X(0.5f), Y(0.15f))
        drawBox(c, X(0.28f), Y(0.30f), S(120f), S(50f), 0xFFEF4444.toInt(), "Katabolizma", "Parcalama")
        drawBox(c, X(0.72f), Y(0.30f), S(120f), S(50f), 0xFF81C784.toInt(), "Anabolizma", "Olusturma")
        lineP.color = Color.argb(A(100), 200, 200, 200); c.drawLine(X(0.40f), Y(0.30f), X(0.60f), Y(0.30f), lineP)
        val kat = arrayOf("Glikoz->CO2" to 0xFFEF4444.toInt(), "Protein->AA" to 0xFFFF8C00.toInt(), "Yag->Gliserol" to 0xFFFFD700.toInt())
        val ana = arrayOf("CO2->Glikoz" to 0xFF81C784.toInt(), "AA->Protein" to 0xFF4DD0E1.toInt(), "Glikoz->Glikojen" to 0xFF8B5CF6.toInt())
        val bw = S(105f); val gap = S(115f)
        for (i in kat.indices) { drawBox(c, X(0.28f), Y(0.52f) + i * S(38f), bw, S(32f), kat[i].second, kat[i].first, ""); drawBox(c, X(0.72f), Y(0.52f) + i * S(38f), bw, S(32f), ana[i].second, ana[i].first, "") }
        sub(c, "BMR: Bazal Metabolizma Hizi", X(0.5f), Y(0.90f))
        sub(c, "Katabolizma: enerji cikarir | Anabolizma: enerji harcar", X(0.5f), Y(0.96f))
    }

    override fun onMeasure(wm: Int, hm: Int) { setMeasuredDimension(MeasureSpec.getSize(wm), MeasureSpec.getSize(hm)) }
}

class BiomoleculesFragment : Fragment() {
    private lateinit var flatView: BiomoleculesFlatView
    private lateinit var titleTv: TextView; private lateinit var whatTv: TextView
    private lateinit var howTv: TextView; private lateinit var useTv: TextView
    private lateinit var examplesTv: TextView; private lateinit var prosTv: TextView
    private lateinit var consTv: TextView; private lateinit var propsTv: TextView

    private data class BType(val name: String, val color: Int)
    private val types = arrayOf(
        BType("Karbonhidrat", 0xFFFF8C00.toInt()), BType("Protein", 0xFFEF4444.toInt()),
        BType("Yag", 0xFFFFD700.toInt()), BType("Vitamin", 0xFF81C784.toInt()),
        BType("DNA", 0xFF4DD0E1.toInt()), BType("Enzim", 0xFF8B5CF6.toInt()),
        BType("Hormon", 0xFFFF69B4.toInt()), BType("Metabolizma", 0xFFFF4444.toInt())
    )

    private val fullInfo = arrayOf(
        arrayOf("Karbonhidratlar",
            "Karbonhidratlar Cn(H2O)n formuluyla gosterilen, karbon, hidrojen ve oksijen iceren organik bileşiklerdir. Canlilarin en onemli enerji kaynagidir. Monomer birimleri monosakkarittir (glikoz, fruktoz, galaktoz).",
            "Monomer: Monosakkarit (glikoz C6H12O6). Disakkarit: 2 monomer + glikozit bagi (sukroz = glikoz + fruktoz). Polisakkarit: yuzlerce monomer (nisasta, seluloz, glikojen). Glikoliz: Glikoz -> 2 piruvat + 2 ATP. Fotosentez: 6CO2 + 6H2O -> C6H12O6 + 6O2.",
            "Enerji kaynagi (ATP uretimi). Hucre yapisi (seluloz-bitki, kitin-bocek). Hucresel tanima (glikoproteinler). Depo enerji (glikojen-karaciger, nisasta-bitki). Kan grubu belirleme. Sinir sistemi beslemesi (beyin: yalnizca glikoz). Sindirim ve emilim.",
            "Glikoz: C6H12O6, 4 kcal/g. Sukroz: C12H22O11. Nisasta: (C6H10O5)n. Seluloz: (C6H10O5)n. Kan sekeri: 70-110 mg/dL. Depo: glikojen 100g karaciger, 400g kas.",
            "Hizli enerji, evrensel enerji kaynagi, cesitli kaynaklar, sindirimi kolay, hayati fonksiyonlar icin zorunlu.",
            "Fazla tuketim: Obezite, diyabet, dis caruqlari, trigliserid yukselmesi, insul direnci, kronik hastaliklar.",
            "Glikoz: 4 kcal/g | Kan: 70-110 mg/dL | Depo: glikojen ~500g | Nisasta granul: 1-5 mikron | Seluloz fibril: 3 mikron"),

        arrayOf("Proteinler",
            "Proteinler, amino asitlerin peptit baglariyla birlesmesiyle olusan buyuk molekullerdir. 20 farkli amino asit vardir. Her protein ozel bir 3D yapiya sahiptir.",
            "20 amino asit: H2N-CHR-COOH. 10 kalanamayan (essential): vucut uretemez. 10 kalanabilen: vucut sentezleyebilir. Peptit bagi: -CO-NH- (dekondansasyon). 1.derece: siralama. 2.derece: alfa-heliks, beta-levha. 3.derece: 3D kivrim. 4.derece: alt birimler.",
            "Enzimler: Tum biyokimyasal reaksiyonlari hizlandirir. Yapi: Kollajen, keratin, aktin. Tasima: Hemoglobin (O2). Savunma: Antikorlar. Sinyal: Insulin. Kas hareketi: Aktin, Miyozin. Kan pihtilasma: Fibrinogen. Depolama: Kazein, ferritin.",
            "Esansiyel: Val, Leu, Ile, Phe, Trp, Met, Thr, Lys, His, Arg. Proteinde 4 kcal/g. Vucut: ~100.000 farkli protein. Gunde: 0.8-1.2 g/kg vucut agirligi gerekli.",
            "Hayati fonksiyonlar icin zorunlu, cok yonlu gorev, ozel yapi-fonksiyon iliskisi, enzim aktivitesi, savunma sistemi.",
            "Eksiklik: Kas kaybi, bagisiklik dusmesi, yara iyilesmesi yavas, Kwashiorkor (karin sisme), Marasmus (kasinma).",
            "Amino asit: 20 tip | Peptit: 2-50 AA | Protein: 50+ AA | MW: 5.000-1.000.000 | Vucut: ~18% | Gunde: 0.8-1.2 g/kg"),

        arrayOf("Yaglar (Lipitler)",
            "Lipitler (yaglar), hidrofobik veya amphipatik molekullerdir. Gliserol + 3 yag asidinden olusan trigliseritler en yaygin lipit turudur.",
            "Trigliserit: Gliserol + 3 yag asidi. Doymus: C-C tekli baglar (tereyagi). Tekli doymamis: 1 C=C (zeytinyagi). Coklu doymamis: 2+ C=C (balik yagi). Omega-3: ALA, EPA, DHA. Omega-6: Linoleik asit. Trans yag: islenmis yaglar (zararli).",
            "Enerji depolama (9 kcal/g - en yuksek). Hucre zar yapisi (fosfolipit). Isi yalitim (deri alti yag). Organ koruma. Hormon uretimi (testosteron, estrogen). Vitamin emilimi (A, D, E, K). Sinir izolasyonu (miyelin).",
            "Trigliserit: 9 kcal/g (2.25x karbonhidrat). O-3: 200-500 mg/gun onerilen. Kolesterol: <300 mg/gun. Fosfolipit: hucre zarinin %50'si.",
            "Enerji depolamada cok verimli, uzun vadeli enerji, hucre yapisinda zorunlu, isinma saglar, vitamin emilimi icin gerekli.",
            "Fazla tuketim: Obezite, kalp hastaligi, ateroskleroz, yukselmis kolesterol, inflamasyon, insul direnci, kanser riski artisi.",
            "Enerji: 9 kcal/g | Gunde: 65-80g onerilen | Vucut: ~15-25% | Doymus: <10% kalori | Trans yag: 0% hedefi"),

        arrayOf("Vitaminler",
            "Vitaminler, vucudun kendi sentezleyemedigi, besinlerle alinmasi gereken organik bileşiklerdir. Az miktarda gerekli olmasina ragmen hayati oneme sahiptir.",
            "Yagda cozunen: A, D, E, K (sindirim kanali ile emilir, vucutta depolanabilir, fazlasi toksik). Suda cozunen: B kompleks, C (hizli atilir, gunluk alim gerekir). Vitamin provitaminlari: Beta-karoten -> A.",
            "A: Gorus, bagisiklik. B1: Tiamin, sinir sistemi. B12: Kan yapimi, sinir. C: Kolajen, antioksidan. D: Kalsiyum, kemik. E: Antioksidan, hucre. K: Pıhtilasma, kemik.",
            "A: 900 mcg/gun. D: 15 mcg/gun. C: 90 mg/gun. E: 15 mg/gun. B12: 2.4 mcg/gun. K: 120 mcg/gun. Avitaminoz: eksiklik. Hipervitaminoz: fazlalik.",
            "Az miktarda buyuk etki, hayati fonksiyonlar icin zorunlu, cesitli roller, antioksidan koruma, bagisiklik destegi.",
            "A eksik: Gece kirliligi. C eksik: Skorbut. D eksik: Rikets. B12 eksik: Anemi. K eksik: Kanama bozuklugu.",
            "A: 900 mcg | C: 90 mg | D: 15 mcg (600 IU) | E: 15 mg | K: 120 mcg | B12: 2.4 mcg"),

        arrayOf("DNA - Deoksiribonukleik Asit",
            "DNA, genetik bilgiyi saklayan ve nesilden nesile aktaran makromoleküldur. Iki zincirin birbirine sarmaliyla olusan ikili sarmal yapisi vardir.",
            "Nukleotid: Fosfat + Deoksiriboz + Baz. Baz eslesmesi: A=T (2 hidrojen bagi), G=C (3 hidrojen bagi). Yapi: Sag eli sarmal, 3.4 nm/pitch, 10 baz/pitch. Replikasyon: DNA->DNA (S phase). Transkripsiyon: DNA->mRNA. Tlasyon: mRNA->Protein.",
            "Genetik bilgi depolama. Protein sentezi talimatlari. Gen ifadesi kontrolu. Hucresi bolunmesi. Evrim ve cesitlilik (mutasyon). Adli tıp (DNA parmakizi). Tibbi tani. Tarimda islahi.",
            "Insan: 3 milyar baz cifti, 23 kromozom cifti, 20.000-25.000 gen. %1.5 gen kodlama, %98.5 regulator. Baz sayisi: A=T, G=C (Chargaff kurali).",
            "Evrensel genetik kod, yuksek dogrulukta replikasyon, cesitlilik yaratma, biyolojik programlama, tani ve arastirma.",
            "Mutasyon: nokta, ekleme, silinme. Genetik: kistik fibrozis, orak hucre. Kanser: somatik mutasyonlar. Yashlanma: telomer kisalmasi.",
            "DNA: 3.2 milyar bp | Uzunluk: ~2 metre/hucre | Gen: ~20.000 | Mutasyon: ~10^-8/baz/bolunme"),

        arrayOf("Enzimler",
            "Enzimler, biyokimyasal reaksiyonlari yuzlerce milyon kat hizlandiran protein tabanli katalizorlerdir. Aktif bolge yapisi ile substrata uyum saglar.",
            "Substrat + Enzim -> ES kompleksi -> Urun + Enzim. 1. Substrat aktif bolgeye baglanir. 2. Gecici ES kompleksi olusur. 3. Urun olusur. 4. Enzim degismez, tekrar kullanilir. Kompetitif inhibitor: aktif bolgeye baglanir. Non-kompetitif: allosterik bolge.",
            "Tum metabolik reaksiyonlarda kataliz. Sindirim: Amilaz, proteaz, lipaz. Nefes: Sitokrom oksidaz. Fotosentez: Rubisco. DNA: DNA polimeraz. Protein: Ribozom. Kan: Trombin.",
            "Turnover: 10^2-10^8/sn. Aktivasyon enerjisi dusurme: %50-80. Km: 0.01-100 mM. Opt. sicaklik: 37 C. Opt. pH: 6-8. Verimlilik: kcat/Km.",
            "Yuksek selectivite, yuksek hiz, yumusak kosullar, geri donusluluk, regule edilebilirlik, enerji tasarrufu.",
            "Denaturasyon: >60 C veya asidik/temel pH. Eksiklik: metabolik hastaliklar. Fenilketonüri. Laktoz intol. Inhibitor: zehirler (sarin).",
            "kcat: 10^2-10^8 /sn | Km: 0.01-100 mM | Opt. Sicaklik: 20-60 C | Opt. pH: 3-10 | MW: 10.000-1.000.000"),

        arrayOf("Hormonlar",
            "Hormonlar, endokrin bezler tarafindan uretilen ve kan yoluyla hedef organlara ulasarak vucut dengesini regule eden kimyasal ileticilerdir.",
            "3 ana grup: 1. Peptit (insulin, glukagon, GH): suda cozunur, hucre yuzeyine baglanir, cAMP yolu. 2. Steroid (kortizol, testosteron, estrogen): yagda cozunur, hucre icine girer, DNA baglanma. 3. Amin (adrenalin, tiroid): T3/T4 iyot gerektirir.",
            "Metabolizma regulasyonu (insulin, glukagon). Buyume (GH, tiroid). Stres (kortizol, adrenalin). Cinsel fonksiyon. Su-tuz dengesi. Uyku-uyanik (melatonin). Ruh hali (serotonin, dopamin). Gebelik (progesteron).",
            "Insulin: 51 amino asit, 2 zincir. Adrenalin: C9H13NO3, MW 183. Testosteron: C19H28O2, MW 288. Estrojen: C18H24O2, MW 272. Kortizol: 362 g/mol.",
            "Vucut dengesini saglar, uzun sureli etki, dusuk dozda buyuk etki, geri besleme ile kontrol, metabolizma regulasyonu.",
            "Hiper/Hipofonksiyon: Diabetes, Hiperaktivite (tiroid), Cushing, Addison. Hormonal bozukluklar: Obezite, infertilite.",
            "Insulin: 51 AA | Kan sekeri: Insulin dusurur | Kortizol: Sabah en yuksek | Melatonin: Gece en yuksek | Dongu: 10-120 dk"),

        arrayOf("Metabolizma",
            "Metabolizma, vucutta gerceklesen tum kimyasal reaksiyonlarin toplamidir. Katabolizma (parcalama, enerji) ve Anabolizma (olusturma, enerji) olmak uzere 2 surecten olusur.",
            "Katabolizma: Buyuk->kucuk parcalama, enerji cikarir. Glikoliz: Glikoz->2 piruvat+2 ATP. Beta-oksidasyon: Yag asidi parcalama. Sitrat dongusu: Asetil-CoA->CO2+enerji. Elektron tasiyici: O2 ile ATP. Anabolizma: Kucuk->buyuk olusturma, ATP harcar.",
            "Enerji uretimi (ATP). Buyume ve onarim. Hucresi bolunmesi. Protein sentezi. Depo enerji. Hucre diferansiyesi. Bagisiklik. Sinir besleme. Vucut sicakligi. Metabolik hiz regulasyonu.",
            "BMR: 1.400-1.800 kcal/gun. Toplam: 2.000-2.500 kcal/gun. ATP: ~40 kg/gun uretilir. Glikoliz: 10 enzim. Sitrat: 8 adim. Teorik: ~38 ATP/glikoz. Gercek: ~30-32 ATP.",
            "Her canlida calisir, enerji uretimi, yapi onarim, homeostaz, uyum, buyume, ureme icin zorunlu.",
            "Metabolik: Diabetes, gut, fenilketonuriu. Metabolik sendrom: Obezite+hipertansiyon+insul direnci. Yashlanma: Metabolizma yavaslar. Egzersiz: Hiz artar.",
            "BMR: 1.400-1.800 kcal/gun | ATP: 30-38/glikoz | Beyin: ~20% O2 | Isi: ~37 C sabit | Kalp: ~5 L/dk")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_biomolecules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flatView = view.findViewById(R.id.bio_canvas)
        titleTv = view.findViewById(R.id.bio_title); whatTv = view.findViewById(R.id.bio_what)
        howTv = view.findViewById(R.id.bio_how); useTv = view.findViewById(R.id.bio_use)
        examplesTv = view.findViewById(R.id.bio_examples); prosTv = view.findViewById(R.id.bio_pros)
        consTv = view.findViewById(R.id.bio_cons); propsTv = view.findViewById(R.id.bio_props)
        val btnRow = view.findViewById<LinearLayout>(R.id.bio_btn_row)
        types.forEachIndexed { idx, t ->
            val btn = TextView(requireContext()).apply {
                text = t.name; textSize = 11f; setTextColor(if (idx == 0) 0xFF0D1219.toInt() else t.color)
                setPadding(24, 12, 24, 12)
                background = resources.getDrawable(if (idx == 0) R.drawable.bg_chip_dark_sel else R.drawable.bg_chip_dark, null)
                isClickable = true; isFocusable = true
                setOnTouchListener { v, e -> when (e.action) { android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).start(); android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(60).start() }; false }
                setOnClickListener { selectType(idx) }
            }
            btnRow.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 })
        }
        selectType(0)
    }

    private fun selectType(idx: Int) {
        flatView.setType(idx)
        val btnRow = view?.findViewById<LinearLayout>(R.id.bio_btn_row) ?: return
        for (i in 0 until btnRow.childCount) {
            val child = btnRow.getChildAt(i) as? TextView ?: continue
            child.background = resources.getDrawable(if (i == idx) R.drawable.bg_chip_dark_sel else R.drawable.bg_chip_dark, null)
            child.setTextColor(if (i == idx) 0xFF0D1219.toInt() else types[i].color)
        }
        val info = fullInfo[idx]; titleTv.text = info[0]; titleTv.setTextColor(types[idx].color)
        whatTv.text = info[1]; howTv.text = info[2]; useTv.text = info[3]
        examplesTv.text = info[4]; prosTv.text = info[5]; consTv.text = info[6]; propsTv.text = info[7]
    }
}
