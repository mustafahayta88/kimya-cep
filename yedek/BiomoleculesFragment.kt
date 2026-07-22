package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class BioCanvasView(context: Context) : View(context) {
    private var bioType = 0
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val bP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val capP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val cP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B6B6B.toInt(); style = Paint.Style.FILL }
    private val oP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); style = Paint.Style.FILL }
    private val nAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3050F8.toInt(); style = Paint.Style.FILL }
    private val hP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val pP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL }
    private val bdr = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val elT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val highP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    fun setBio(type: Int) { bioType = type; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        val c = canvas
        val ar = (w * 0.032f).coerceAtMost(h * 0.045f).coerceAtMost(22f); val sp = ar * 4f; val cx = w / 2f
        tP.textSize = h * 0.055f; sP.textSize = h * 0.035f; capP.textSize = h * 0.04f; highP.textSize = h * 0.04f; elT.textSize = ar * 0.9f

        fun da(x: Float, y: Float, e: String, p: Paint = cP) { c.drawCircle(x, y, ar, p); c.drawCircle(x, y, ar, bdr); elT.textSize = ar * 0.9f; c.drawText(e, x, y + elT.textSize / 3f, elT) }
        fun db(x1: Float, y1: Float, x2: Float, y2: Float, t: Int = 1) { c.drawLine(x1, y1, x2, y2, if (t == 2) bP else bP) }

        when (bioType) {
            0 -> {
                c.drawText("Karbonhidratlar", cx, h * 0.05f, tP)
                c.drawText("Cn(H2O)n - sekerler, nisasta, seluloz", cx, h * 0.10f, sP)
                c.drawText("Monosakkarit: 1 seker | Disakkarit: 2 | Polisakkarit: cok", cx, h * 0.14f, sP)

                val rc = cx - w * 0.15f; val ry = h * 0.38f; val rr = sp * 1.3f
                val pang = 2f * PI.toFloat() / 6f
                val hexPts = (0 until 6).map { i -> Pair(rc + rr * cos(i * pang - PI.toFloat() / 2f), ry + rr * sin(i * pang - PI.toFloat() / 2f)) }
                for (pt in hexPts) da(pt.first, pt.second, "C")
                for (i in 0 until 6) { val n = (i + 1) % 6; db(hexPts[i].first, hexPts[i].second, hexPts[n].first, hexPts[n].second) }
                da(hexPts[0].first - sp * 0.8f, hexPts[0].second + sp * 0.4f, "O", oP)
                da(hexPts[1].first - sp * 0.3f, hexPts[1].second - sp * 0.6f, "OH")
                da(hexPts[2].first + sp * 0.3f, hexPts[2].second - sp * 0.6f, "OH")
                da(hexPts[3].first + sp * 0.8f, hexPts[3].second + sp * 0.1f, "O", oP)
                da(hexPts[4].first + sp * 0.6f, hexPts[4].second + sp * 0.6f, "OH")
                da(hexPts[5].first - sp * 0.3f, hexPts[5].second + sp * 0.6f, "OH")
                db(hexPts[0].first, hexPts[0].second, hexPts[0].first - sp * 0.8f, hexPts[0].second + sp * 0.4f)
                c.drawText("Glukoz (C6H12O6)", rc, ry + rr + sp * 0.5f, capP)
                c.drawText("Kan sekeri, ana enerji", rc, ry + rr + sp, sP)

                c.drawText("Sukroz = Glukoz+Fruktoz", cx + w * 0.18f, h * 0.35f, highP)
                c.drawText("Nisasta: 100-1000 glukoz", cx + w * 0.18f, h * 0.48f, highP)
                c.drawText("Seluloz: bitki duvari", cx + w * 0.18f, h * 0.58f, highP)
                c.drawText("Glikozid bagi", cx + w * 0.18f, h * 0.68f, sP)
                c.drawText("Enerji depo + yapi", cx + w * 0.18f, h * 0.80f, sP)
            }
            1 -> {
                c.drawText("Proteinler", cx, h * 0.05f, tP)
                c.drawText("Amino asit polimerleri - 20 standart aa", cx, h * 0.10f, sP)

                val aa_x = cx - sp * 3f
                da(aa_x, h * 0.28f, "H2N", nAt); db(aa_x, h * 0.28f, aa_x + sp, h * 0.28f); da(aa_x + sp, h * 0.28f, "C")
                da(aa_x + sp, h * 0.28f - sp * 0.8f, "H", hP); da(aa_x + sp, h * 0.28f + sp * 0.8f, "R")
                db(aa_x + sp, h * 0.28f, aa_x + sp * 2f, h * 0.28f); da(aa_x + sp * 2f, h * 0.28f, "C")
                da(aa_x + sp * 2f, h * 0.28f + sp * 0.7f, "O", oP); da(aa_x + sp * 2f, h * 0.28f - sp * 0.8f, "OH")
                c.drawText("Amino asit genel: NH2-C(R)-COOH", cx - sp, h * 0.52f, sP)

                val pepP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                val path = Path(); path.moveTo(w * 0.12f, h * 0.62f)
                for (i in 0 until 6) { path.lineTo(w * 0.12f + i * w * 0.11f, h * 0.62f + sin(i * 0.6f) * 6f) }; c.drawPath(path, pepP)
                c.drawText("-[NH-CO]n- polipeptid zinciri (protein)", cx, h * 0.72f, capP)
                c.drawText("1: aa dizisi | 2: a-heliks, b-tabaka | 3: 3D katlanma | 4: alt birim", cx, h * 0.80f, sP)
                c.drawText("Enzim, antikor, kas, kolajen - 4 kcal/g", cx, h * 0.88f, sP)
            }
            2 -> {
                c.drawText("Yaglar (Lipitler)", cx, h * 0.05f, tP)
                c.drawText("Gliserol + 3 yag asidi = Trigliserit", cx, h * 0.10f, sP)

                val glyY = h * 0.28f; val s5 = sp * 0.8f
                da(cx, glyY, "C"); da(cx - s5, glyY + s5, "C"); da(cx + s5, glyY + s5, "C")
                db(cx, glyY, cx - s5, glyY + s5); db(cx, glyY, cx + s5, glyY + s5)
                da(cx - s5, glyY + s5 + s5, "O", oP); da(cx + s5, glyY + s5 + s5, "O", oP); da(cx, glyY - s5, "O", oP)

                for ((dx, dy) in listOf(cx - s5 to glyY + s5 + s5, cx + s5 to glyY + s5 + s5, cx to glyY - s5)) {
                    for (j in 0 until 3) {
                        val ex = dx + s5 * (j + 1) * if (dx < cx) -1f else 1f
                        da(ex, dy + s5 * 0.3f * j, "C")
                        if (j > 0) db(ex - s5 * if (dx < cx) -1f else 1f, dy + s5 * 0.3f * (j - 1), ex, dy + s5 * 0.3f * j)
                    }
                }
                c.drawText("Gliserol + 3 yag asidi zinciri", cx, h * 0.66f, sP)

                c.drawText("Doymus yag: hayvansal, kati (tereyagi)", cx, h * 0.76f, highP)
                c.drawText("Doymamis yag: bitkisel, sivi (zeytinyagi)", cx, h * 0.82f, capP)
                c.drawText("Hucrre zari, enerji depo (9 kcal/g), hormon", cx, h * 0.89f, sP)
            }
            3 -> {
                c.drawText("Vitaminler", cx, h * 0.05f, tP)
                c.drawText("Organik bilesikler - Eksikliginde hastalik", cx, h * 0.10f, sP)
                c.drawText("Yagda cozunen: A, D, E, K | Suda cozunen: B, C", cx, h * 0.14f, sP)

                data class Vit(val name: String, val source: String, val fn: String, val def: String)
                val vits = listOf(
                    Vit("A", "Havuc ispanak", "Gorme bagisiklik", "Gece korlugu"),
                    Vit("C", "Portakal limon", "Kollajen antioksidan", "Iskorbüt"),
                    Vit("D", "Gunes balik", "Kalsiyum emilimi", "Rasitizm"),
                    Vit("B12", "Et sut yumurta", "Sinir sistemi", "Anemi"),
                    Vit("E", "Findik aycicek", "Hucre koruma", "Noropati"),
                )
                val tableTop = h * 0.20f; val rowH = h * 0.10f; val colW = w * 0.22f
                val headerP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = h * 0.035f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                val dataP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }
                val defP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4444.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334455.toInt(); strokeWidth = 1f }

                val hdrs = listOf("Vitamin", "Kaynak", "Gorev", "Eksiklik")
                for ((ci, hdr) in hdrs.withIndex()) { c.drawText(hdr, cx - colW * 1.5f + ci * colW, tableTop - 6f, headerP) }
                for ((ri, vit) in vits.withIndex()) {
                    val y = tableTop + (ri + 1) * rowH
                    c.drawLine(w * 0.08f, y, w * 0.92f, y, linePaint)
                    c.drawText(vit.name, cx - colW * 1.5f, y + rowH * 0.6f, dataP)
                    c.drawText(vit.source, cx - colW * 0.5f, y + rowH * 0.6f, dataP)
                    c.drawText(vit.fn, cx + colW * 0.5f, y + rowH * 0.6f, dataP)
                    c.drawText(vit.def, cx + colW * 1.5f, y + rowH * 0.6f, defP2)
                }
            }
            4 -> {
                c.drawText("DNA / RNA", cx, h * 0.05f, tP)
                c.drawText("Nukleik asitler - Genetik bilgi", cx, h * 0.10f, sP)
                c.drawText("Nukleotid: fosfat + seker + baz (A,T,G,C/U)", cx, h * 0.14f, sP)

                val helP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                val helP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                val baseP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }

                val path1 = Path(); val path2 = Path(); val cx1 = w * 0.35f; val cx2 = w * 0.65f
                val topY = h * 0.22f; val botY = h * 0.68f; val steps = 80
                for (i in 0..steps) {
                    val frac = i.toFloat() / steps; val y = topY + (botY - topY) * frac
                    val amp = h * 0.07f; val a1x = cx1 + sin(frac * PI.toFloat() * 5f) * amp
                    val a2x = cx2 + sin(frac * PI.toFloat() * 5f + PI.toFloat()) * amp
                    if (i == 0) { path1.moveTo(a1x, y); path2.moveTo(a2x, y) }
                    else { path1.lineTo(a1x, y); path2.lineTo(a2x, y) }
                }
                c.drawPath(path1, helP); c.drawPath(path2, helP2)

                val ar2 = ar * 0.5f
                for (i in 0 until 6) {
                    val frac = (i + 0.5f) / 6; val y = topY + (botY - topY) * frac
                    val bx1 = cx1 + sin(frac * PI.toFloat() * 5f) * h * 0.07f
                    val bx2 = cx2 + sin(frac * PI.toFloat() * 5f + PI.toFloat()) * h * 0.07f
                    c.drawLine(bx1, y, bx2, y, baseP)
                    c.drawCircle(bx1 + (bx2 - bx1) * 0.3f, y, ar2, hP)
                    c.drawCircle(bx1 + (bx2 - bx1) * 0.7f, y, ar2, nAt)
                }

                c.drawText("DNA: Cift sarmal (Watson-Crick)", cx, h * 0.78f, capP)
                c.drawText("Eslesme: A=T (2 H bagi), G=C (3 H bagi)", cx, h * 0.84f, highP)
                c.drawText("Insan: ~3 milyar bc, ~20.000 gen", cx, h * 0.90f, sP)
                c.drawText("RNA: Tek sarmal, U (urasil), protein sentezi", cx, h * 0.96f, sP)
            }
        }
    }
}

class BiomoleculesFragment : Fragment() {
    private lateinit var bioView: BioCanvasView
    private val categories = listOf("Karbonhidrat", "Protein", "Yag", "Vitamin", "DNA/RNA")
    private val details = listOf(
        "Karbonhidratlar: Genel formul Cn(H2O)n. Monosakkarit (glukoz C6H12O6, fruktoz), disakkarit (sukroz C12H22O11, laktoz), polisakkarit (nisasta, seluloz, glikojen). Glikozid bagi ile baglanirlar. Enerji kaynagi (4 kcal/g), yapi maddesi (seluloz). Kan sekeri glukoz.",
        "Proteinler: 20 standart amino asidin peptid bagi (-CO-NH-) ile polimerlesmesi. Birincil yapi (aa dizisi), ikincil (a-heliks, b-tabaka), ucuncul (3D katlanma), dorduncul (alt birimler). Enzim, antikor, kas proteini, kolajen. 4 kcal/g.",
        "Yaglar (Lipitler): Gliserol + 3 yag asidi (trigliserit). Ester bagi. Doymus yag: hayvansal, kati. Doymamis yag: bitkisel, sivi. Hucrre zari (fosfolipit), enerji depo (9 kcal/g), hormon (steroid). C18:1 (oleik), C18:2 (linoleik).",
        "Vitaminler: Organik bilesikler, vucut sentezleyemez. Yagda cozunen: A (gorme), D (Ca emilimi), E (antioksidan), K (pıhtilasma). Suda cozunen: B (metabolizma), C (kollajen). Eksiklik: A -> gece korlugu, C -> iskorbüt, D -> rasitizm, B12 -> anemi.",
        "DNA/RNA: Nukleotid: fosfat + seker (deoksiriboz/riboz) + baz (A,T,G,C/U). DNA: cift sarmal, A=T (2 H bagi), G=C (3 H bagi). RNA: tek sarmal, U (urasil). mRNA, tRNA, rRNA. Insan: 3 milyar bc, 20000 gen."
    )
    private val formulas = listOf(
        "Glukoz C6H12O6 | Seluloz: (C6H10O5)n | Nisasta: ~100-1000 glukoz",
        "Insulin: 51 aa | Kollajen: ~1000 aa | Hemoglobin: 4 alt birim 574 aa",
        "Doymus: C16-C18 | Doymamis: C18:1 oleik, C18:2 linoleik | 9 kcal/g",
        "A: gece korlugu | C: iskorbüt | D: rasitizm | B12: anemi",
        "Insan: 3 milyar bc | DNA: A=T G=C | RNA: Uracil A=U"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_biomolecules, container, false)
        val placeholder = v.findViewById<View>(R.id.bio_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        bioView = BioCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * resources.displayMetrics.density).toInt())
        }
        parent.addView(bioView, idx)

        val btnRow = v.findViewById<LinearLayout>(R.id.bio_btn_row)
        val btnIds = mutableListOf<Button>()
        categories.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 12f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    btnIds.forEach { it.alpha = 0.5f }; alpha = 1f
                    bioView.setBio(i)
                    v.findViewById<TextView>(R.id.bio_title).text = categories[i]
                    v.findViewById<TextView>(R.id.bio_detail).text = details[i]
                    v.findViewById<TextView>(R.id.bio_facts).text = formulas[i]
                }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        return v
    }
}
