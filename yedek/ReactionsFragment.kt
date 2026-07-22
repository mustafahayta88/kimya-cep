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

class ReactionSchemeView(context: Context) : View(context) {
    private var rxnType = 0
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val eqP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF3.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val condP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f) }
    private val arrowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); style = Paint.Style.FILL }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val bond2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val bond3P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val cAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B6B6B.toInt(); style = Paint.Style.FILL }
    private val hAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val oAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); style = Paint.Style.FILL }
    private val nAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3050F8.toInt(); style = Paint.Style.FILL }
    private val clAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1FC01F.toInt(); style = Paint.Style.FILL }
    private val brAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA52525.toInt(); style = Paint.Style.FILL }
    private val naAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDAA520.toInt(); style = Paint.Style.FILL }
    private val elT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val noteP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val stepP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val mechP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f) }
    private val mechFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); style = Paint.Style.FILL }

    fun setReaction(type: Int) { rxnType = type; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        val c = canvas
        val ar = (w * 0.035f).coerceAtMost(h * 0.05f).coerceAtMost(24f); val sp = ar * 4f; val ar2 = (w * 0.025f).coerceAtMost(h * 0.035f).coerceAtMost(18f); val sp2 = ar2 * 3.5f; val cx = w / 2f
        titleP.textSize = h * 0.065f; eqP.textSize = h * 0.045f; subP.textSize = h * 0.035f
        condP.textSize = h * 0.04f; noteP.textSize = h * 0.035f; stepP.textSize = h * 0.03f; elT.textSize = ar * 0.9f
        val topY = h * 0.1f; val eqY = h * 0.18f

        fun da(x: Float, y: Float, elem: String, paint: Paint = cAt) {
            c.drawCircle(x, y, ar, paint); c.drawCircle(x, y, ar, borderP)
            elT.textSize = ar * 0.9f; c.drawText(elem, x, y + elT.textSize / 3f, elT)
        }
        fun da2(x: Float, y: Float, elem: String, paint: Paint = cAt) {
            c.drawCircle(x, y, ar2, paint); c.drawCircle(x, y, ar2, borderP)
            elT.textSize = ar2 * 0.9f; c.drawText(elem, x, y + elT.textSize / 3f, elT)
        }
        fun db(x1: Float, y1: Float, x2: Float, y2: Float, t: Int = 1) {
            val bp = when (t) { 2 -> bond2P; 3 -> bond3P; else -> bondP }; c.drawLine(x1, y1, x2, y2, bp)
        }
        fun curArrow(x1: Float, y1: Float, x2: Float, y2: Float) {
            val path = Path(); val mx = (x1 + x2) / 2f; val my = (y1 + y2) / 2f - sp * 1.0f
            path.moveTo(x1, y1); path.quadTo(mx, my, x2, y2); c.drawPath(path, mechP)
            val angle = atan2(y2 - my, x2 - mx); val tipX = x2; val tipY = y2
            val ap = Path(); ap.moveTo(tipX, tipY); ap.lineTo(tipX - 12f * cos(angle - 0.4f), tipY - 12f * sin(angle - 0.4f))
            ap.lineTo(tipX - 12f * cos(angle + 0.4f), tipY - 12f * sin(angle + 0.4f)); ap.close(); c.drawPath(ap, mechFill)
        }

        when (rxnType) {
            0 -> {
                c.drawText("Yer Degistirme (SN2)", cx, topY, titleP)
                c.drawText("Nu: + R-L -> R-Nu + L:", cx, eqY, eqP)

                val my = h * 0.45f
                da(cx - sp * 1.2f, my, "OH", oAt)
                c.drawText("Nu:", cx - sp * 1.2f, my + ar + sp * 0.4f, stepP)

                curArrow(cx - sp * 1.2f, my, cx, my - sp * 1.2f)
                c.drawText("saldiri", cx - sp * 0.3f, my - sp * 1.7f, noteP)

                da(cx, my, "C")
                c.drawText("CH3", cx, my - sp * 0.9f, noteP)

                da(cx + sp * 2f, my, "Cl", clAt)
                db(cx, my, cx + sp * 2f, my)
                c.drawText("ayrilan grup", cx + sp * 2f, my + ar + sp * 0.4f, stepP)
                curArrow(cx + sp * 0.3f, my + ar * 0.5f, cx + sp * 1.7f, my + ar * 0.5f)

                val arrY = h * 0.78f
                c.drawLine(cx - sp * 2f, arrY, cx + sp * 2f, arrY, arrowP)
                val ap = Path(); ap.moveTo(cx + sp * 2f, arrY); ap.lineTo(cx + sp * 2f - 14f, arrY - 8f); ap.lineTo(cx + sp * 2f - 14f, arrY + 8f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("NaOH / H2O, 25 C", cx, arrY - 10f, condP)

                da(cx - sp * 1f, h * 0.87f, "OH", oAt)
                da(cx + sp * 0.2f, h * 0.87f, "C")
                da(cx + sp * 1.8f, h * 0.87f, "Na", naAt)
                da(cx + sp * 3f, h * 0.87f, "Cl", clAt)
                db(cx - sp * 1f, h * 0.87f, cx + sp * 0.2f, h * 0.87f)
                db(cx + sp * 0.2f, h * 0.87f, cx + sp * 1.8f, h * 0.87f)
                db(cx + sp * 1.8f, h * 0.87f, cx + sp * 3f, h * 0.87f)
                c.drawText("CH3-OH + NaCl (urun)", cx + sp * 1f, h * 0.94f, condP)

                c.drawText("1. Nu saldirisi -> 2. Ayrilan grup cikis -> 3. Urun", cx, h * 0.04f, subP)
            }
            1 -> {
                c.drawText("Katilma (Adisyon)", cx, topY, titleP)
                c.drawText("C=C + X-Y -> X-C-C-Y", cx, eqY, eqP)

                val my = h * 0.45f
                da(cx - sp * 0.7f, my, "C"); da(cx + sp * 0.7f, my, "C")
                db(cx - sp * 0.7f, my, cx + sp * 0.7f, my, 2)
                c.drawText("CH2", cx - sp * 0.7f, my - sp * 0.8f, noteP)
                c.drawText("CH2", cx + sp * 0.7f, my - sp * 0.8f, noteP)

                da(cx - sp * 2f, my + sp * 1.3f, "Br", brAt)
                da(cx + sp * 2f, my + sp * 1.3f, "Br", brAt)
                db(cx - sp * 2f, my + sp * 1.3f, cx - sp * 0.7f, my + sp * 0.3f)
                db(cx + sp * 2f, my + sp * 1.3f, cx + sp * 0.7f, my + sp * 0.3f)
                c.drawText("Br2 saldirisi", cx, my + sp * 2.2f, noteP)
                curArrow(cx - sp * 2f, my + sp * 1.3f, cx - sp * 0.7f, my + sp * 0.3f)
                curArrow(cx + sp * 2f, my + sp * 1.3f, cx + sp * 0.7f, my + sp * 0.3f)

                val arrY = h * 0.78f
                c.drawLine(cx - sp * 2f, arrY, cx + sp * 2f, arrY, arrowP)
                val ap = Path(); ap.moveTo(cx + sp * 2f, arrY); ap.lineTo(cx + sp * 2f - 14f, arrY - 8f); ap.lineTo(cx + sp * 2f - 14f, arrY + 8f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("CCL4, 25 C", cx, arrY - 10f, condP)

                da(cx - sp * 1f, h * 0.86f, "Br", brAt); da(cx + sp * 1f, h * 0.86f, "Br", brAt)
                da(cx - sp * 1f, h * 0.86f - sp * 0.5f, "C"); da(cx + sp * 1f, h * 0.86f - sp * 0.5f, "C")
                db(cx - sp * 1f, h * 0.86f - sp * 0.5f, cx + sp * 1f, h * 0.86f - sp * 0.5f)
                db(cx - sp * 1f, h * 0.86f - sp * 0.5f, cx - sp * 1f, h * 0.86f)
                db(cx + sp * 1f, h * 0.86f - sp * 0.5f, cx + sp * 1f, h * 0.86f)
                c.drawText("Dibromoetan (C2H4Br2)", cx, h * 0.94f, condP)
                c.drawText("pi-bagi acilir, 2 yeni sigma-bagi olusur", cx, h * 0.04f, subP)
            }
            2 -> {
                c.drawText("Eliminasyon (E2)", cx, topY, titleP)
                c.drawText("R-X + Baz -> C=C + H-Baz + X:", cx, eqY, eqP)

                val my = h * 0.40f
                da(cx - sp * 0.6f, my, "C"); da(cx + sp * 0.6f, my, "C")
                db(cx - sp * 0.6f, my, cx + sp * 0.6f, my)

                da(cx - sp * 0.6f, my - sp * 1f, "H", hAt); db(cx - sp * 0.6f, my, cx - sp * 0.6f, my - sp * 1f)
                da(cx - sp * 0.6f, my + sp * 1f, "Br", brAt); db(cx - sp * 0.6f, my, cx - sp * 0.6f, my + sp * 1f)
                da(cx + sp * 0.6f, my - sp * 1f, "H", hAt); db(cx + sp * 0.6f, my, cx + sp * 0.6f, my - sp * 1f)
                c.drawText("CH3", cx, my + sp * 1.8f, noteP)

                da(cx + sp * 2.5f, my - sp * 1f, "OH", oAt)
                curArrow(cx + sp * 2.5f, my - sp * 1f, cx + sp * 0.6f, my - sp * 1f)
                c.drawText("Baz: OH-", cx + sp * 2.5f, my - sp * 1.8f, noteP)

                curArrow(cx - sp * 0.6f, my + sp * 1f, cx - sp * 2.2f, my + sp * 1.5f)
                c.drawText("Br- ayrilir", cx - sp * 2.8f, my + sp * 2f, noteP)

                val arrY = h * 0.80f
                c.drawLine(cx - sp * 2f, arrY, cx + sp * 2f, arrY, arrowP)
                val ap = Path(); ap.moveTo(cx + sp * 2f, arrY); ap.lineTo(cx + sp * 2f - 14f, arrY - 8f); ap.lineTo(cx + sp * 2f - 14f, arrY + 8f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("KOH / Etanol, 80 C", cx, arrY - 10f, condP)

                da(cx, h * 0.88f, "C"); da(cx + sp * 0.8f, h * 0.88f, "C")
                db(cx, h * 0.88f, cx + sp * 0.8f, h * 0.88f, 2)
                c.drawText("C2H4 (eten) + KBr + H2O", cx, h * 0.95f, condP)
                c.drawText("Baz H+ ceker, Br- ayrilir, cift bag olusur (tek basamak)", cx, h * 0.04f, subP)
            }
            3 -> {
                c.drawText("Halkalasma (Diels-Alder)", cx, topY, titleP)
                c.drawText("Dien + Dienofil -> Halkali urun", cx, eqY, eqP)

                val my = h * 0.38f; val rr = sp * 1.1f
                val pts = (0 until 4).map { i ->
                    val a = i * PI.toFloat() / 2f - PI.toFloat() / 2f
                    Pair(cx - sp * 2.5f + rr * cos(a), my + rr * sin(a))
                }
                for (pt in pts) da(pt.first, pt.second, "C")
                for (i in 0 until 4) { val n = (i + 1) % 4; db(pts[i].first, pts[i].second, pts[n].first, pts[n].second, if (i % 2 == 0) 2 else 1) }
                c.drawText("1,3-butadien (dien)", cx - sp * 2.5f, my + rr + sp * 0.6f, noteP)

                da(cx + sp * 0.5f, my - sp * 0.4f, "C"); da(cx + sp * 1.8f, my - sp * 0.4f, "C")
                da(cx + sp * 0.5f, my + sp * 0.4f, "C"); da(cx + sp * 1.8f, my + sp * 0.4f, "C")
                da(cx + sp * 2.8f, my - sp * 0.4f, "O", oAt); da(cx + sp * 2.8f, my + sp * 0.4f, "O", oAt)
                db(cx + sp * 0.5f, my - sp * 0.4f, cx + sp * 0.5f, my + sp * 0.4f, 2)
                db(cx + sp * 0.5f, my - sp * 0.4f, cx + sp * 1.8f, my - sp * 0.4f)
                db(cx + sp * 0.5f, my + sp * 0.4f, cx + sp * 1.8f, my + sp * 0.4f)
                db(cx + sp * 1.8f, my - sp * 0.4f, cx + sp * 2.8f, my - sp * 0.4f, 2)
                db(cx + sp * 1.8f, my + sp * 0.4f, cx + sp * 2.8f, my + sp * 0.4f, 2)
                c.drawText("Maleik anhidrit", cx + sp * 1.7f, my + sp * 1.2f, noteP)

                curArrow(pts[2].first + sp * 0.3f, pts[2].second, cx + sp * 0.5f, my + sp * 0.4f)
                curArrow(pts[3].first + sp * 0.3f, pts[3].second, cx + sp * 0.5f, my - sp * 0.4f)

                val arrY = h * 0.80f
                c.drawLine(cx - sp * 1.5f, arrY, cx + sp * 1.5f, arrY, arrowP)
                val ap = Path(); ap.moveTo(cx + sp * 1.5f, arrY); ap.lineTo(cx + sp * 1.5f - 14f, arrY - 8f); ap.lineTo(cx + sp * 1.5f - 14f, arrY + 8f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("Isi, 100 C", cx, arrY - 10f, condP)

                val rpts = (0 until 6).map { i ->
                    val a = i * PI.toFloat() / 3f - PI.toFloat() / 2f
                    Pair(cx + rr * 0.6f * cos(a), h * 0.88f + rr * 0.6f * sin(a))
                }
                for (pt in rpts) da(pt.first, pt.second, "C")
                for (i in 0 until 6) { val n = (i + 1) % 6; db(rpts[i].first, rpts[i].second, rpts[n].first, rpts[n].second, if (i % 2 == 0) 2 else 1) }
                c.drawText("Sikloheksen turevi (urun)", cx, h * 0.95f, condP)
                c.drawText("2 sigma + 1 pi = 2 yeni sigma (perisiklik)", cx, h * 0.04f, subP)
            }
            4 -> {
                c.drawText("Duzenlenme (Cope)", cx, topY, titleP)
                c.drawText("1,5-dien -> 3-metil-1,5-dien (sigmatropik)", cx, eqY, eqP)

                val sp5 = sp * 0.7f; val hh = h
                val cy = hh * 0.35f; val cx2 = cx - sp5 * 2.0f
                for (i in 0 until 6) { da(cx2 + i * sp5, cy, "C") }
                for (i in 0 until 5) { db(cx2 + i * sp5, cy, cx2 + (i + 1) * sp5, cy, if (i % 2 == 0) 2 else 1) }
                c.drawText("1,5-heksadien", cx, cy + sp5, noteP)
                c.drawText("[3,3]-sigmatropik gecis", cx, cy + sp5 * 2.0f, noteP)

                val arrY = hh * 0.74f
                c.drawLine(cx - sp, arrY, cx + sp, arrY, arrowP)
                val ap = Path(); ap.moveTo(cx + sp, arrY); ap.lineTo(cx + sp - 14f, arrY - 8f); ap.lineTo(cx + sp - 14f, arrY + 8f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("Isi 200 C", cx, arrY - 10f, condP)

                val dcy = hh * 0.84f
                for (i in 0 until 6) { da(cx2 + i * sp5, dcy, "C") }
                db(cx2, dcy, cx2 + sp5, dcy, 2)
                db(cx2 + sp5, dcy, cx2 + sp5 * 2f, dcy)
                db(cx2 + sp5 * 2f, dcy, cx2 + sp5 * 3f, dcy, 2)
                db(cx2 + sp5 * 3f, dcy, cx2 + sp5 * 4f, dcy)
                db(cx2 + sp5 * 4f, dcy, cx2 + sp5 * 5f, dcy)
                c.drawText("3-metil-1,5-heksadien", cx, dcy + sp5, condP)
                c.drawText("sigma bagi kopar, yeni sigma bagi olusur", cx, hh * 0.04f, subP)
            }
        }
    }
}

class ReactionsFragment : Fragment() {
    private lateinit var rxnView: ReactionSchemeView
    private val categories = listOf("Yer Deg.", "Katilma", "Eliminasyon", "Halkalasma", "Duz.")
    private val titles = listOf(
        "Yer Degistirme (SN2)",
        "Katilma (Adisyon)",
        "Eliminasyon (E2)",
        "Halkalasma (Diels-Alder)",
        "Duzenlenme (Cope)"
    )
    private val eqs = listOf(
        "CHCl3 + NaOH -> CHOH + NaCl",
        "CH2=CH2 + Br2 -> CH2Br-CH2Br",
        "CH3-CH2Br + KOH -> CH2=CH2 + KBr + H2O",
        "1,3-butadien + maleik anhidrit -> halkali urun",
        "1,5-heksadien -> 3-metil-1,5-heksadien"
    )
    private val details = listOf(
        "SN2: Nucleophilic Substitution Bimolecular. Tek basamakli mekanizma. Nu saldirisi ve ayrilan grup cikisi es zamanli. Karbon merkezde konfigurasyon ters doner (Walden inversiyonu). OH- iyi nukleofil, Cl- iyi ayrilan grup. Hizi: [RX][Nu].",
        "Adisyon: C=C cift bagina elektrofil (E+) ve nukleofil (Nu-) eklenir. pi bagi acilir, 2 yeni sigma bagi olusur. Br2 elektrofil olarak davranir. Markovnikov kurali: H daha cok H olan karbona eklenir. Katalitik hidrojenasyon (H2/Pt) da adisyon ornegidir.",
        "E2: Bimolecular Elimination. Tek basamakli. Baz (OH-) H+ cekerken, ayrilan grup (Br-) ayrilir. E/Z izomerleri olusabilir. Zaitsev kurali: daha cok substituye olan alken ana urundur. KOH/etanol ile gerceklesir.",
        "Diels-Alder: [4+2] sikloadisyon. 4 pi elektronlu dien (1,3-butadien) + 2 pi elektronlu dienofil. Perisiklik reaksiyon, tek basamak. 6-uyeli halka olusur. Stereospesifik, endo kurali. Organik sentezde cok onemli.",
        "Cope Duzenlenmesi: [3,3]-sigmatropik yeniden duzenlenme. 1,5-dien farkli bir 1,5-diene donusur. Perisiklik, karbon-karbon bagi kopar ve olusur. Isi ile gerceklesir. Oxy-Cope varyanti daha hizli."
    )
    private val mechs = listOf(
        "Mekanizma: OH- -> C+ (gecis hali) -> Cl- ayrilir -> C-OH",
        "Mekanizma: Br+ eklenir (bromonyum) -> Br- acar -> dibromo",
        "Mekanizma: OH- H+ ceker, Br- ayrilir, C=C olusur",
        "Mekanizma: Dien + dienofil -> [4+2] -> sikloheksen",
        "Mekanizma: [3,3] kayma -> sigma bagi kopar/yeniden olusur"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_reactions, container, false)
        val placeholder = v.findViewById<View>(R.id.rxn_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        rxnView = ReactionSchemeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * resources.displayMetrics.density).toInt())
        }
        parent.addView(rxnView, idx)

        val btnRow = v.findViewById<LinearLayout>(R.id.rxn_btn_row)
        val btnIds = mutableListOf<Button>()
        categories.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 12f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    btnIds.forEach { it.alpha = 0.5f }; alpha = 1f
                    rxnView.setReaction(i)
                    v.findViewById<TextView>(R.id.rxn_title).text = titles[i]
                    v.findViewById<TextView>(R.id.rxn_eq).text = eqs[i]
                    v.findViewById<TextView>(R.id.rxn_detail).text = details[i]
                    v.findViewById<TextView>(R.id.rxn_mech).text = mechs[i]
                }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        return v
    }
}
