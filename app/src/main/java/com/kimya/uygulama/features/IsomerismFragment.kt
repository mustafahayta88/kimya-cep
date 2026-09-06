package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import kotlin.math.*

class IsomerCanvasView(context: Context) : View(context) {

    var isoIdx = 0; private set
    private var pulse = 0f
    private var zoom = 1f; private var panX = 0f; private var panY = 0f
    private var lastX = 0f; private var lastY = 0f; private var downX = 0f; private var downY = 0f
    private var isPinch = false

    var onTypeChanged: ((Int) -> Unit)? = null

    private val scDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(d: ScaleGestureDetector): Boolean { isPinch = true; return true }
        override fun onScale(d: ScaleGestureDetector): Boolean { zoom *= d.scaleFactor; zoom = zoom.coerceIn(0.5f, 2.5f); invalidate(); return true }
        override fun onScaleEnd(d: ScaleGestureDetector) { isPinch = false }
    })

    // Paints
    private val bgFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF030810.toInt() }
    private val bgGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111825.toInt() }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF546E7A.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bond2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0097A7.toInt(); strokeWidth = 3.5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val mirrorP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f) }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val atomEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f }
    private val atomLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4DD0E1.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val capP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF78909C.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val propP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val diffP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF69F0AE.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val eqP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB2FF59.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val energyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE }
    private val particleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    data class IsoInfo(
        val title: String, val subtitle: String, val formula: String,
        val leftName: String, val rightName: String,
        val leftProps: List<String>, val rightProps: List<String>,
        val diff: String, val detail: String,
        val leftColor: Int, val rightColor: Int,
        val leftExtra: String, val rightExtra: String
    )

    val isos = listOf(
        IsoInfo("Yapi Izomerligi", "Konstitusyonel", "C4H10",
            "n-Butan", "Izobutan",
            listOf("kn -0.5 C", "en -138 C", "Yogunluk 0.58 g/mL"),
            listOf("kn -11.7 C", "en -159 C", "Yogunluk 0.56 g/mL"),
            "Duz vs dalli: fiziksel ozellikler farkli",
            "Karbon iskeleti farkli baglanmis. n-butan: 4C duz zincir, izobutan: merkezi C'ye 3C bagli. C8H18'de 18, C10H22'de 75, C15H32'de 4347 izomer var. Dallanma arttikca kn duser.",
            0xFF5C6BC0.toInt(), 0xFFEF5350.toInt(),
            "Duz 4C zincir", "Merkezi C'ye 3C bagli"),
        IsoInfo("Geometrik Izomerlik", "Cis-Trans / E-Z", "C4H8",
            "Cis-2-butene", "Trans-2-butene",
            listOf("kn 3.7 C", "polar, mu > 0", "en -139 C"),
            listOf("kn 0.9 C", "apolar, mu = 0", "en -106 C"),
            "Cis: polar dipol var | Trans: apolar",
            "C=C donme kisitli (~264 kJ/mol bariyer). Cis: ayni tarafta, dipol moment > 0. Trans: karsi tarafta, dipol = 0. E-Z notasyonu: Cahn-Ingold-Prelog oncelik kurallari ile. Sikloalkenlerde de gorulur (cis-cikloheksen daha kararli).",
            0xFF42A5F5.toInt(), 0xFFEF5350.toInt(),
            "Ayni taraf: polar", "Karsi taraf: apolar"),
        IsoInfo("Optik Izomerlik", "Enantiyomerler", "C3H7NO2",
            "L-Alanin", "D-Alanin",
            listOf("[a]D = +8.5", "Vucutta aktif", "Proteinlerde kullanilir"),
            listOf("[a]D = -8.5", "Vucutta etkisiz", "Sentez urunu"),
            "Ayna goruntusu cakismaz - polarizasyon farkli",
            "Kiralk karbon: 4 farkli grup bagli. Enantiyomerler: ayna goruntusu, superimpoz degil. Diastereomerler: ayna goruntusu degil, fiziksel ozellikleri farkli. Meso bilesikler: kiralk ama superimpoz. Polarimetri ile olculur. Sadece L-amino asitler, D-sekerler biyolojik sistemlerde kullanilir.",
            0xFFAB47BC.toInt(), 0xFF26A69A.toInt(),
            "Sol dondurur (+)", "Sag dondurur (-)"),
        IsoInfo("Fonksiyonel Grup", "Ayni formül, farkli grup", "C2H6O",
            "Etanol (alkol)", "Dimetil eter",
            listOf("kn 78 C", "polar, suda cozunur", "H-baglisi yapar"),
            listOf("kn -24 C", "apolar, suda az cozunur", "H-baglisi yapmaz"),
            "Alkol vs eter: tamamen farkli ozellikler",
            "Ayni formül ama farkli fonksiyonel grup. Etanol: -OH, su ile H-baglisi yapar, polar, suda sinirsiz cozunur. Dimetil eter: C-O-C, apolar, su da az cozunur. Kimyasal reaktivite cok farkli: etanol oksidasyon ile asetaldehit/asit olusur, eter oksidasyona direncli.",
            0xFFFFCA28.toInt(), 0xFF26A69A.toInt(),
            "-OH: H-baglisi", "C-O-C: apolar"),
        IsoInfo("Pozisyon Izomerligi", "Grup farkli karbonda", "C3H8O",
            "1-Propanol", "2-Propanol",
            listOf("kn 97 C", "OH terminal", "Oksidasyon: propanal"),
            listOf("kn 83 C", "OH 2. karbon", "Oksidasyon: aseton"),
            "OH pozisyonu: 14 C fark, farkli urunler",
            "Fonksiyonel grup ayni, konum farkli. 1-propanol: primery alkol, oksidasyon ile propanal -> propanoik asit. 2-propanol: sekonder alkol, oksidasyon ile aseton. 14 C kn farki van der Waals kuvvetleri farkindan kaynaklanir.",
            0xFFFF7043.toInt(), 0xFF7E57C2.toInt(),
            "Terminal: primery", "Orta: sekonder"),
        IsoInfo("Tautomeri", "Keto-Enol denge", "C3H6O",
            "Keto (Aseton)", "Enol (Propenol)",
            listOf("C=O bagi", "Cok kararli", "%99.9999 keto"),
            listOf("C=C-OH", "Kararsiz", "%0.0001 enol"),
            "Keto formu cok daha kararli",
            "H ve cift bag farkli konumda. Asit kataliz: C=O protonlanir -> enol. Baz kataliz: a-H kopar -> enolat. Fenol: tamamen enol (aromatik kararlilik). DNA bazlarinda tautomer: Watson-Crick eslesmeyi bozar, nokta mutasyona yol acar.",
            0xFFEF5350.toInt(), 0xFF42A5F5.toInt(),
            "C=O karbonil", "C=C-OH enol"),
        IsoInfo("Konformasyon", "Donme sonucu 3D", "C2H6",
            "Staggered", "Eclipsed",
            listOf("0 kJ/mol", "Dusuk enerji", "En kararli"),
            listOf("12 kJ/mol", "Yuksek enerji", "Kararsiz"),
            "Enerji farki: 12 kJ/mol (tutulma vs kayma)",
            "Tek bag etrafinda donme. Staggered: H'ler arasi 60, minimum itisme. Eclipsed: H'ler ustuste, maksimum itisme. Butan: anti(0) < gauche(3.8) < eclipsed(19) < fully-eclipsed(22) kJ/mol. Torsiyonel gerilim: anti > gauche kararlilik.",
            0xFF69F0AE.toInt(), 0xFFFF5252.toInt(),
            "Kayma: dusuk E", "Tutulma: yuksek E")
    )

    fun setIso(i: Int) { isoIdx = i.coerceIn(0, isos.size - 1); invalidate() }

    // ===== Drawing helpers =====

    // drawBond ONCE (atoms will be drawn ON TOP)
    private fun drawBond(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, type: Int = 1) {
        when (type) {
            1 -> c.drawLine(x1, y1, x2, y2, bondP)
            2 -> { val dx = x2 - x1; val dy = y2 - y1; val len = sqrt(dx * dx + dy * dy); if (len < 2f) return; val nx = -dy / len * 5f; val ny = dx / len * 5f; c.drawLine(x1 + nx, y1 + ny, x2 + nx, y2 + ny, bond2P); c.drawLine(x1 - nx, y1 - ny, x2 - nx, y2 - ny, bond2P) }
        }
    }

    // drawAtom SONRA (her zaman bond'un ustunde)
    private fun drawAtom(c: Canvas, x: Float, y: Float, r: Float, elem: String, alpha: Float = 1f) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val col = when (elem) {
            "C" -> intArrayOf(0xFF5C6BC0.toInt(), 0xFF9FA8DA.toInt(), 0xFF283593.toInt())
            "O" -> intArrayOf(0xFFEF5350.toInt(), 0xFFEF9A9A.toInt(), 0xFFC62828.toInt())
            "N" -> intArrayOf(0xFF42A5F5.toInt(), 0xFF90CAF9.toInt(), 0xFF1565C0.toInt())
            "H" -> intArrayOf(0xFF78909C.toInt(), 0xFFB0BEC5.toInt(), 0xFF455A64.toInt())
            "Cl" -> intArrayOf(0xFF26A69A.toInt(), 0xFF80CBC4.toInt(), 0xFF00796B.toInt())
            else -> intArrayOf(0xFF9E9E9E.toInt(), 0xFFE0E0E0.toInt(), 0xFF616161.toInt())
        }
        // Glow
        glow.shader = RadialGradient(x, y, r + 8f, intArrayOf((col[1] and 0x00FFFFFF or 0x44000000).toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
        glow.alpha = a; c.drawCircle(x, y, r + 8f, glow)
        // Shadow
        shadow.shader = RadialGradient(x + 1f, y + 2f, r * 1.1f, intArrayOf(0x44000000.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
        c.drawCircle(x + 1f, y + 2f, r * 1.1f, shadow)
        // Sphere
        val sphere = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x - r * 0.3f, y - r * 0.35f, r * 1.6f,
                intArrayOf(0xFFFFFFFF.toInt(), col[1], col[0], col[2]), floatArrayOf(0f, 0.15f, 0.5f, 1f), Shader.TileMode.CLAMP)
            this.alpha = a
        }
        c.drawCircle(x, y, r, sphere); c.drawCircle(x, y, r, atomEdge)
        // Specular
        val spec = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x - r * 0.28f, y - r * 0.32f, r * 0.38f, intArrayOf(0xDDFFFFFF.toInt(), 0x00FFFFFF.toInt()), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            this.alpha = a
        }
        c.drawCircle(x - r * 0.28f, y - r * 0.32f, r * 0.38f, spec)
        // Label
        atomLabel.textSize = r * 0.8f; atomLabel.alpha = a
        atomLabel.color = when (elem) { "O" -> 0xFFFFCDD2.toInt(); "N" -> 0xFFBBDEFB.toInt(); "Cl" -> 0xFFC8E6C9.toInt(); else -> 0xFFFFFFFF.toInt() }
        c.drawText(elem, x, y + atomLabel.textSize * 0.33f, atomLabel)
        atomLabel.alpha = 255
    }

    private fun drawParticles(c: Canvas, cx: Float, cy: Float, count: Int, color: Int, radius: Float) {
        for (i in 0 until count) {
            val angle = pulse * 1.5f + i * 2f * PI.toFloat() / count
            val dist = radius * (0.4f + 0.6f * sin(pulse * 2f + i * 0.9f))
            val px = cx + cos(angle) * dist; val py = cy + sin(angle) * dist
            val sz = 1.5f + 1.2f * sin(pulse * 3f + i)
            particleP.color = color; particleP.alpha = (50 + 40 * sin(pulse * 4f + i)).toInt().coerceIn(15, 150)
            c.drawCircle(px, py, sz, particleP)
        }
    }

    private fun drawMirrorLine(c: Canvas, x: Float, y1: Float, y2: Float) {
        c.drawLine(x, y1, x, y2, mirrorP)
        subP.textSize = 11f; subP.color = 0xFFFFFFFF.toInt(); c.drawText("Ayna", x, y2 + 12f, subP); subP.color = 0xFF78909C.toInt()
        // Shimmer
        val shimmerY = y1 + (y2 - y1) * ((pulse * 0.4f) % 1f)
        particleP.color = 0xFFFFFFFF.toInt(); particleP.alpha = (30 + 25 * sin(pulse * 3f)).toInt()
        c.drawCircle(x, shimmerY, 2.5f, particleP)
    }

    // ===== ISOMER DRAWING =====
    // Rule: draw bonds FIRST, atoms ON TOP

    private fun drawYapi(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.40f
        // Left: n-Butan
        capP.textSize = 50f; capP.color = isos[0].leftColor; c.drawText("n-Butan", w * 0.25f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFFFA726.toInt(); c.drawText("kn -0.5 C", w * 0.25f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[0].leftExtra, w * 0.25f, h * 0.72f, subP)
        val lx = w * 0.25f - sp * 1.5f
        // Bonds first
        for (i in 0 until 3) drawBond(c, lx + i * sp, midY, lx + (i + 1) * sp, midY)
        // Atoms on top
        for (i in 0 until 4) drawAtom(c, lx + i * sp, midY, ar, "C")
        drawParticles(c, w * 0.25f, midY, 4, isos[0].leftColor, sp * 1.5f)

        drawMirrorLine(c, cx, h * 0.13f, h * 0.82f)

        // Right: Izobutan
        capP.textSize = 50f; capP.color = isos[0].rightColor; c.drawText("Izobutan", w * 0.75f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFEF5350.toInt(); c.drawText("kn -11.7 C", w * 0.75f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[0].rightExtra, w * 0.75f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.75f, midY, w * 0.75f - sp, midY + sp * 0.7f)
        drawBond(c, w * 0.75f, midY, w * 0.75f + sp, midY + sp * 0.7f)
        drawBond(c, w * 0.75f, midY, w * 0.75f, midY - sp * 0.7f)
        // Atoms on top
        drawAtom(c, w * 0.75f, midY, ar, "C")
        drawAtom(c, w * 0.75f - sp, midY + sp * 0.7f, ar, "C")
        drawAtom(c, w * 0.75f + sp, midY + sp * 0.7f, ar, "C")
        drawAtom(c, w * 0.75f, midY - sp * 0.99f, ar, "C")
        drawParticles(c, w * 0.75f, midY, 4, isos[0].rightColor, sp * 1.5f)
    }

    private fun drawGeometrik(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.42f
        // Left: Cis
        capP.textSize = 50f; capP.color = isos[1].leftColor; c.drawText("Cis", w * 0.25f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFFFA726.toInt(); c.drawText("kn 3.7 C, polar", w * 0.25f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[1].leftExtra, w * 0.25f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.25f - sp * 0.5f, midY, w * 0.25f + sp * 0.5f, midY, 2)
        drawBond(c, w * 0.25f - sp * 0.5f, midY, w * 0.25f - sp * 1.2f, midY - sp * 0.5f)
        drawBond(c, w * 0.25f - sp * 0.5f, midY, w * 0.25f - sp * 1.2f, midY + sp * 0.5f)
        drawBond(c, w * 0.25f + sp * 0.5f, midY, w * 0.25f + sp * 1.2f, midY - sp * 0.5f)
        drawBond(c, w * 0.25f + sp * 0.5f, midY, w * 0.25f + sp * 1.2f, midY + sp * 0.5f)
        // Atoms on top
        drawAtom(c, w * 0.25f - sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.25f + sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.25f - sp * 1.2f, midY - sp * 0.5f, ar * 0.75f, "H")
        drawAtom(c, w * 0.25f - sp * 1.2f, midY + sp * 0.5f, ar * 0.75f, "Cl")
        drawAtom(c, w * 0.25f + sp * 1.2f, midY - sp * 0.5f, ar * 0.75f, "H")
        drawAtom(c, w * 0.25f + sp * 1.2f, midY + sp * 0.5f, ar * 0.75f, "Cl")
        drawParticles(c, w * 0.25f, midY, 5, isos[1].leftColor, sp * 0.8f)

        drawMirrorLine(c, cx, h * 0.13f, h * 0.82f)

        // Right: Trans
        capP.textSize = 50f; capP.color = isos[1].rightColor; c.drawText("Trans", w * 0.75f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFEF5350.toInt(); c.drawText("kn 0.9 C, apolar", w * 0.75f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[1].rightExtra, w * 0.75f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.75f - sp * 0.5f, midY, w * 0.75f + sp * 0.5f, midY, 2)
        drawBond(c, w * 0.75f - sp * 0.5f, midY, w * 0.75f - sp * 1.2f, midY - sp * 0.5f)
        drawBond(c, w * 0.75f - sp * 0.5f, midY, w * 0.75f - sp * 1.2f, midY + sp * 0.5f)
        drawBond(c, w * 0.75f + sp * 0.5f, midY, w * 0.75f + sp * 1.2f, midY - sp * 0.5f)
        drawBond(c, w * 0.75f + sp * 0.5f, midY, w * 0.75f + sp * 1.2f, midY + sp * 0.5f)
        // Atoms on top
        drawAtom(c, w * 0.75f - sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.75f + sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.75f - sp * 1.2f, midY - sp * 0.5f, ar * 0.75f, "Cl")
        drawAtom(c, w * 0.75f - sp * 1.2f, midY + sp * 0.5f, ar * 0.75f, "H")
        drawAtom(c, w * 0.75f + sp * 1.2f, midY - sp * 0.5f, ar * 0.75f, "H")
        drawAtom(c, w * 0.75f + sp * 1.2f, midY + sp * 0.5f, ar * 0.75f, "Cl")
        drawParticles(c, w * 0.75f, midY, 5, isos[1].rightColor, sp * 0.8f)
    }

    private fun drawOptik(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.40f; val off = sp * 0.8f
        // Left: L-Alanin
        capP.textSize = 50f; capP.color = isos[2].leftColor; c.drawText("L-Alanin", w * 0.25f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFAB47BC.toInt(); c.drawText("[a]D = +8.5", w * 0.25f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[2].leftExtra, w * 0.25f, h * 0.72f, subP)
        val cLx = w * 0.25f
        // Bonds first
        drawBond(c, cLx, midY, cLx + off, midY - off * 0.7f)
        drawBond(c, cLx, midY, cLx - off * 0.6f, midY + off * 0.8f)
        drawBond(c, cLx, midY, cLx + off * 0.4f, midY + off)
        drawBond(c, cLx, midY, cLx - off, midY - off * 0.4f)
        // Atoms on top
        drawAtom(c, cLx, midY, ar, "C")
        drawAtom(c, cLx + off, midY - off * 0.7f, ar * 0.8f, "N")
        drawAtom(c, cLx - off * 0.6f, midY + off * 0.8f, ar * 0.65f, "H")
        drawAtom(c, cLx + off * 0.4f, midY + off, ar * 0.8f, "C")
        drawAtom(c, cLx - off, midY - off * 0.4f, ar * 0.9f, "C")
        drawParticles(c, cLx, midY, 5, isos[2].leftColor, off * 0.8f)

        drawMirrorLine(c, cx, h * 0.13f, h * 0.82f)

        // Right: D-Alanin
        capP.textSize = 50f; capP.color = isos[2].rightColor; c.drawText("D-Alanin", w * 0.75f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFF26A69A.toInt(); c.drawText("[a]D = -8.5", w * 0.75f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[2].rightExtra, w * 0.75f, h * 0.72f, subP)
        val cRx = w * 0.75f
        // Bonds first
        drawBond(c, cRx, midY, cRx - off, midY - off * 0.7f)
        drawBond(c, cRx, midY, cRx + off * 0.6f, midY + off * 0.8f)
        drawBond(c, cRx, midY, cRx - off * 0.4f, midY + off)
        drawBond(c, cRx, midY, cRx + off, midY - off * 0.4f)
        // Atoms on top
        drawAtom(c, cRx, midY, ar, "C")
        drawAtom(c, cRx - off, midY - off * 0.7f, ar * 0.8f, "N")
        drawAtom(c, cRx + off * 0.6f, midY + off * 0.8f, ar * 0.65f, "H")
        drawAtom(c, cRx - off * 0.4f, midY + off, ar * 0.8f, "C")
        drawAtom(c, cRx + off, midY - off * 0.4f, ar * 0.9f, "C")
        drawParticles(c, cRx, midY, 5, isos[2].rightColor, off * 0.8f)
    }

    private fun drawFonksiyonel(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.40f
        // Left: Etanol
        capP.textSize = 50f; capP.color = isos[3].leftColor; c.drawText("Etanol", w * 0.25f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFFFA726.toInt(); c.drawText("kn 78 C", w * 0.25f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[3].leftExtra, w * 0.25f, h * 0.72f, subP)
        val lx = w * 0.25f - sp * 0.8f
        // Bonds first
        drawBond(c, lx, midY, lx + sp * 0.6f, midY)
        drawBond(c, lx + sp * 0.6f, midY, lx + sp * 1.3f, midY)
        drawBond(c, lx + sp * 1.3f, midY, lx + sp * 1.8f, midY)
        // Atoms on top
        drawAtom(c, lx, midY, ar, "C")
        drawAtom(c, lx + sp * 0.6f, midY, ar, "C")
        drawAtom(c, lx + sp * 1.3f, midY, ar * 0.8f, "O")
        drawAtom(c, lx + sp * 1.8f, midY, ar * 0.6f, "H")
        subP.textSize = 11f; c.drawText("-OH grubu", w * 0.25f, midY + ar + 12f, subP)
        drawParticles(c, w * 0.25f, midY, 4, isos[3].leftColor, sp)

        drawMirrorLine(c, cx, h * 0.13f, h * 0.82f)

        // Right: Dimetil eter
        capP.textSize = 50f; capP.color = isos[3].rightColor; c.drawText("Dimetil eter", w * 0.75f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFF26A69A.toInt(); c.drawText("kn -24 C", w * 0.75f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[3].rightExtra, w * 0.75f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.75f - sp * 0.6f, midY, w * 0.75f, midY)
        drawBond(c, w * 0.75f, midY, w * 0.75f + sp * 0.6f, midY)
        // Atoms on top
        drawAtom(c, w * 0.75f - sp * 0.6f, midY, ar, "C")
        drawAtom(c, w * 0.75f, midY, ar * 0.8f, "O")
        drawAtom(c, w * 0.75f + sp * 0.6f, midY, ar, "C")
        subP.textSize = 11f; c.drawText("C-O-C", w * 0.75f, midY + ar + 12f, subP)
        drawParticles(c, w * 0.75f, midY, 4, isos[3].rightColor, sp)
    }

    private fun drawPozisyon(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.40f
        // Left: 1-Propanol
        capP.textSize = 50f; capP.color = isos[4].leftColor; c.drawText("1-Propanol", w * 0.25f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFFF7043.toInt(); c.drawText("kn 97 C", w * 0.25f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[4].leftExtra, w * 0.25f, h * 0.72f, subP)
        val lx = w * 0.25f - sp * 1.2f
        // Bonds first
        for (i in 0 until 2) drawBond(c, lx + i * sp, midY, lx + (i + 1) * sp, midY)
        drawBond(c, lx + sp * 2f, midY, lx + sp * 2.6f, midY)
        drawBond(c, lx + sp * 2.6f, midY, lx + sp * 3.1f, midY)
        // Atoms on top
        for (i in 0 until 3) drawAtom(c, lx + i * sp, midY, ar, "C")
        drawAtom(c, lx + sp * 2.6f, midY, ar * 0.8f, "O")
        drawAtom(c, lx + sp * 3.1f, midY, ar * 0.6f, "H")
        subP.textSize = 11f; c.drawText("OH terminal", w * 0.25f, midY + ar + 12f, subP)
        drawParticles(c, w * 0.25f, midY, 4, isos[4].leftColor, sp)

        drawMirrorLine(c, cx, h * 0.13f, h * 0.82f)

        // Right: 2-Propanol
        capP.textSize = 50f; capP.color = isos[4].rightColor; c.drawText("2-Propanol", w * 0.75f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFF7E57C2.toInt(); c.drawText("kn 83 C", w * 0.75f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[4].rightExtra, w * 0.75f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.75f - sp, midY, w * 0.75f, midY)
        drawBond(c, w * 0.75f, midY, w * 0.75f + sp, midY)
        drawBond(c, w * 0.75f, midY, w * 0.75f, midY - sp * 0.7f)
        drawBond(c, w * 0.75f, midY - sp * 0.7f, w * 0.75f, midY - sp * 1.2f)
        // Atoms on top
        drawAtom(c, w * 0.75f - sp, midY, ar, "C")
        drawAtom(c, w * 0.75f, midY, ar, "C")
        drawAtom(c, w * 0.75f + sp, midY, ar, "C")
        drawAtom(c, w * 0.75f, midY - sp * 0.7f, ar * 0.8f, "O")
        drawAtom(c, w * 0.75f, midY - sp * 1.2f, ar * 0.6f, "H")
        subP.textSize = 11f; c.drawText("OH 2. karbon", w * 0.75f, midY + ar + 12f, subP)
        drawParticles(c, w * 0.75f, midY, 4, isos[4].rightColor, sp)
    }

    private fun drawTautomeri(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.40f
        // Left: Keto
        capP.textSize = 50f; capP.color = isos[5].leftColor; c.drawText("Keto", w * 0.25f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFFEF5350.toInt(); c.drawText("C=O, kararli", w * 0.25f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[5].leftExtra, w * 0.25f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.25f - sp * 0.5f, midY, w * 0.25f + sp * 0.5f, midY)
        drawBond(c, w * 0.25f + sp * 0.5f, midY, w * 0.25f + sp * 0.5f, midY - sp * 0.6f)
        // Atoms on top
        drawAtom(c, w * 0.25f - sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.25f + sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.25f + sp * 0.5f, midY - sp * 0.6f, ar * 0.8f, "O")
        drawParticles(c, w * 0.25f, midY, 4, isos[5].leftColor, sp)

        // Equilibrium
        eqP.textSize = 18f; c.drawText("<->", cx, midY - 5f, eqP)
        subP.textSize = 11f; c.drawText("Denge", cx, midY + 12f, subP)

        // Right: Enol
        capP.textSize = 50f; capP.color = isos[5].rightColor; c.drawText("Enol", w * 0.75f, h * 0.18f, capP)
        propP.textSize = 30f; propP.color = 0xFF42A5F5.toInt(); c.drawText("C=C-OH, kararsiz", w * 0.75f, h * 0.70f, propP)
        subP.textSize = 22f; c.drawText(isos[5].rightExtra, w * 0.75f, h * 0.72f, subP)
        // Bonds first
        drawBond(c, w * 0.75f - sp * 0.5f, midY, w * 0.75f + sp * 0.5f, midY, 2)
        drawBond(c, w * 0.75f + sp * 0.5f, midY, w * 0.75f + sp * 0.5f, midY - sp * 0.6f)
        drawBond(c, w * 0.75f + sp * 0.5f, midY - sp * 0.6f, w * 0.75f + sp * 0.5f, midY - sp * 1.1f)
        // Atoms on top
        drawAtom(c, w * 0.75f - sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.75f + sp * 0.5f, midY, ar, "C")
        drawAtom(c, w * 0.75f + sp * 0.5f, midY - sp * 0.6f, ar * 0.8f, "O")
        drawAtom(c, w * 0.75f + sp * 0.5f, midY - sp * 1.1f, ar * 0.55f, "H")
        drawParticles(c, w * 0.75f, midY, 4, isos[5].rightColor, sp)
    }

    private fun drawKonformasyon(c: Canvas, w: Float, h: Float, cx: Float, ar: Float, sp: Float) {
        val midY = h * 0.32f; val r6 = sp * 1.1f
        val positions = listOf(w * 0.18f, w * 0.50f, w * 0.82f)
        val labels = listOf("Staggered", "60 donmus", "Eclipsed")
        val eColors = listOf(0xFF69F0AE.toInt(), 0xFFFFCA28.toInt(), 0xFFFF5252.toInt())
        val energies = listOf("0 kJ/mol", "3.8 kJ/mol", "12 kJ/mol")
        for (idx in 0 until 3) {
            val px = positions[idx]
            // Bonds first (circle is bond)
            bondP.strokeWidth = 2.5f; c.drawCircle(px, midY, r6, bondP)
            bondP.strokeWidth = 4f
            // Center
            c.drawCircle(px, midY, r6 * 0.18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF546E7A.toInt(); style = Paint.Style.FILL })
            // H atoms
            val baseAngle = if (idx == 2) PI.toFloat() / 6f else 0f
            for (i in 0 until 3) { val a = baseAngle + i * 2f * PI.toFloat() / 3f; drawAtom(c, px + r6 * cos(a), midY + r6 * sin(a), ar * 0.65f, "H") }
            capP.textSize = 30f; capP.color = eColors[idx]; c.drawText(labels[idx], px, h * 0.55f, capP)
            propP.textSize = 25f; propP.color = eColors[idx]; c.drawText(energies[idx], px, h * 0.57f, propP)
        }
        // Energy curve
        val egrY = h * 0.70f; val egrW = w * 0.80f; val egrX = w * 0.10f
        energyP.strokeWidth = 3.5f
        val epath = Path(); epath.moveTo(egrX, egrY)
        for (i in 0..60) { val fx = i / 60f; epath.lineTo(egrX + fx * egrW, egrY - sin(fx * PI.toFloat() * 2f) * 30f + 30f) }
        c.drawPath(epath, energyP)
        subP.textSize = 30f; c.drawText("Enerji vs donme acisi", cx, egrY + 99f, subP)
    }

    // ===== Main draw =====
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat(); val cx = w / 2f
        val iso = isos[isoIdx]

        c.drawRect(0f, 0f, w, h, bgFill)
        bgGlow.shader = RadialGradient(cx, h * 0.3f, w * 0.65f, intArrayOf(0xFF0C1825.toInt(), 0xFF030810.toInt(), 0xFF010305.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, bgGlow)

        val gs = 30f
        for (gx in 0 until (w / gs).toInt()) for (gy in 0 until (h / gs).toInt()) {
            val x = gx * gs + gs / 2; val y = gy * gs + gs / 2
            val wave = sin(x * 0.008f + y * 0.006f + pulse * 1.2f) * 0.5f + 0.5f
            gridDot.alpha = (3 + (5 * wave).toInt()).coerceIn(2, 8); c.drawCircle(x, y, 0.5f, gridDot)
        }

        c.save(); c.scale(zoom, zoom, w / 2f, h / 2f); c.translate(panX / zoom, panY / zoom)

        val ar = (w * 0.038f).coerceAtMost(h * 0.05f).coerceAtMost(24f)
        val sp = ar * 3.5f

        when (isoIdx) {
            0 -> drawYapi(c, w, h, cx, ar, sp)
            1 -> drawGeometrik(c, w, h, cx, ar, sp)
            2 -> drawOptik(c, w, h, cx, ar, sp)
            3 -> drawFonksiyonel(c, w, h, cx, ar, sp)
            4 -> drawPozisyon(c, w, h, cx, ar, sp)
            5 -> drawTautomeri(c, w, h, cx, ar, sp)
            6 -> drawKonformasyon(c, w, h, cx, ar, sp)
        }

        c.restore()
        pulse += 0.035f; if (pulse > 2f * PI.toFloat()) pulse -= 2f * PI.toFloat()
        invalidate()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scDetector.onTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> { lastX = ev.x; lastY = ev.y; downX = ev.x; downY = ev.y; isPinch = false; return true }
            MotionEvent.ACTION_MOVE -> { if (!isPinch && zoom > 1.05f) { panX += ev.x - lastX; panY += ev.y - lastY }; lastX = ev.x; lastY = ev.y; invalidate(); return true }
            MotionEvent.ACTION_UP -> {
                if (!isPinch) { val dx = ev.x - downX; val dy = ev.y - downY; if (abs(dx) > width * 0.15f && abs(dx) > abs(dy) && zoom <= 1.05f) { onTypeChanged?.invoke(((isoIdx + if (dx < 0) 1 else -1) + isos.size) % isos.size) } }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> { isPinch = false }
        }
        return true
    }
}

class IsomerismFragment : Fragment() {
    private lateinit var canvas: IsomerCanvasView
    private val cats = listOf("Yapi", "Geometrik", "Optik", "Fonk.Grup", "Pozisyon", "Tautomeri", "Konformasyon")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_isomerism, container, false)
        val placeholder = v.findViewById<View>(R.id.iso_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)

        val btnRow = v.findViewById<LinearLayout>(R.id.iso_btn_row)
        val btnIds = mutableListOf<TextView>()
        val d = resources.displayMetrics.density
        cats.forEachIndexed { i, name ->
            TextView(requireContext()).apply {
                text = name; textSize = 11f; setTextColor(0xFF4DD0E1.toInt())
                typeface = android.graphics.Typeface.DEFAULT_BOLD; isSingleLine = true
                background = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_mk_chip)
                gravity = android.view.Gravity.CENTER; minWidth = (50 * d).toInt()
                setPadding((8 * d).toInt(), 0, (8 * d).toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, (34 * d).toInt()).apply { setMargins(2, 0, 2, 0) }
                isClickable = true; isFocusable = true
                setOnClickListener { AnimUtils.press(this); btnIds.forEach { b -> b.alpha = 0.4f }; alpha = 1f; canvas.setIso(i); updateInfo(v) }
                btnIds.add(this); btnRow.addView(this)
            }
        }

        canvas = IsomerCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            onTypeChanged = { i -> btnIds.forEach { b -> b.alpha = 0.4f }; if (i < btnIds.size) btnIds[i].alpha = 1f; canvas.setIso(i); updateInfo(v) }
        }
        parent.addView(canvas, idx)
        btnIds.firstOrNull()?.alpha = 1f

        v.findViewById<View>(R.id.btn_help)?.setOnClickListener { HelpDialog.showGuide(requireContext(), "Izomerlik", "7 izomer turu", listOf("Kaydir: izomer degistir", "Pinch: zoom")) }
        updateInfo(v); return v
    }

    private fun updateInfo(v: View) {
        val iso = canvas.isos[canvas.isoIdx]
        v.findViewById<TextView>(R.id.iso_title).text = iso.title
        v.findViewById<TextView>(R.id.iso_subtitle).text = "${iso.subtitle} | ${iso.formula}"
        v.findViewById<TextView>(R.id.iso_left_name).text = iso.leftName
        v.findViewById<TextView>(R.id.iso_right_name).text = iso.rightName
        for (i in 0..2) {
            val leftId = resources.getIdentifier("iso_left_prop${i + 1}", "id", requireContext().packageName)
            if (leftId != 0) v.findViewById<TextView>(leftId)?.text = iso.leftProps[i]
            val rightId = resources.getIdentifier("iso_right_prop${i + 1}", "id", requireContext().packageName)
            if (rightId != 0) v.findViewById<TextView>(rightId)?.text = iso.rightProps[i]
        }
        v.findViewById<TextView>(R.id.iso_diff).text = iso.diff
        v.findViewById<TextView>(R.id.iso_detail).text = iso.detail
    }
}

