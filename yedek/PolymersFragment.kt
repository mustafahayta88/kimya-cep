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

class PolymerCanvasView(context: Context) : View(context) {
    private var polType = 0
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val bP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val b2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val lP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCCCC.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val capP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val nP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val cP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B6B6B.toInt(); style = Paint.Style.FILL }
    private val hP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val oP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); style = Paint.Style.FILL }
    private val nAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3050F8.toInt(); style = Paint.Style.FILL }
    private val bdr = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val elT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val brkP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }

    fun setPolymer(type: Int) { polType = type; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        val c = canvas
        val ar = (w * 0.032f).coerceAtMost(h * 0.045f).coerceAtMost(20f); val sp = ar * 4f; val cx = w / 2f
        tP.textSize = h * 0.06f; lP.textSize = h * 0.045f; sP.textSize = h * 0.035f; capP.textSize = h * 0.045f; nP.textSize = sp * 0.7f; elT.textSize = ar * 0.9f

        fun da(x: Float, y: Float, e: String, p: Paint = cP) { c.drawCircle(x, y, ar, p); c.drawCircle(x, y, ar, bdr); elT.textSize = ar * 0.9f; c.drawText(e, x, y + elT.textSize / 3f, elT) }
        fun db(x1: Float, y1: Float, x2: Float, y2: Float, t: Int = 1) { val bp = when (t) { 2 -> b2P; else -> bP }; c.drawLine(x1, y1, x2, y2, bp) }

        when (polType) {
            0 -> {
                c.drawText("Katilma (Zincir) Polimerlesmesi", cx, h * 0.05f, tP)
                c.drawText("n CH2=CH2 -> -(CH2-CH2)n- (Polietilen)", cx, h * 0.11f, lP)

                val my = h * 0.25f
                db(cx - sp * 0.7f, my, cx + sp * 0.7f, my, 2); da(cx - sp * 0.7f, my, "C"); da(cx + sp * 0.7f, my, "C")
                c.drawText("Etilen monomeri", cx, my + sp * 0.8f, sP)
                c.drawText("Bag radikal ile acilir", cx, my + sp * 1.3f, sP)

                val ay = h * 0.42f
                c.drawLine(cx - sp * 2f, ay, cx - sp * 2f, ay + sp * 1.5f, brkP)
                c.drawLine(cx + sp * 2f, ay, cx + sp * 2f, ay + sp * 1.5f, brkP)
                c.drawText("n", cx + sp * 2.5f, ay + sp * 0.6f, nP)

                for (j in 0 until 4) {
                    val bx = cx - sp * 1.5f + j * sp
                    da(bx, ay, "C"); da(bx, ay + sp * 0.8f, "C")
                    db(bx, ay, bx, ay + sp * 0.8f)
                    if (j < 3) { db(bx + sp * 0.5f, ay, bx + sp, ay); db(bx + sp * 0.5f, ay + sp * 0.8f, bx + sp, ay + sp * 0.8f) }
                }
                c.drawText("Polietilen (PE) - en yaygin plastik", cx, h * 0.78f, capP)
                c.drawText("Monomer: Etilen (C2H4) | 1000-100000 monomer unit", cx, h * 0.85f, sP)
                c.drawText("Kullanim: Torba, sise, boru, yalitim", cx, h * 0.92f, sP)
            }
            1 -> {
                c.drawText("Yogunlasma (Basamak) Polimerlesmesi", cx, h * 0.05f, tP)
                c.drawText("Diasit + Diamin -> Poliamid (Nylon) + n H2O", cx, h * 0.11f, lP)

                val by = h * 0.30f; val sp5 = sp * 0.7f
                da(cx - sp5 * 3f, by, "C"); da(cx - sp5 * 4f, by - sp5 * 0.6f, "O", oP); da(cx - sp5 * 4f, by + sp5 * 0.6f, "O", oP)
                db(cx - sp5 * 3f, by, cx - sp5 * 1.2f, by); da(cx - sp5 * 1.2f, by, "C"); db(cx - sp5 * 1.2f, by, cx + sp5 * 0.6f, by); da(cx + sp5 * 0.6f, by, "C")
                db(cx + sp5 * 0.6f, by, cx + sp5 * 2.4f, by); da(cx + sp5 * 2.4f, by, "C"); da(cx + sp5 * 3.4f, by - sp5 * 0.6f, "O", oP); da(cx + sp5 * 3.4f, by + sp5 * 0.6f, "O", oP)
                c.drawText("Diasit", cx + sp5 * 0.6f, by + sp5 * 1.4f, sP)

                val by2 = h * 0.52f
                da(cx - sp5 * 3f, by2, "N", nAt); da(cx - sp5 * 3.8f, by2 - sp5 * 0.6f, "H", hP); da(cx - sp5 * 3.8f, by2 + sp5 * 0.6f, "H", hP)
                db(cx - sp5 * 3f, by2, cx - sp5 * 1.2f, by2); da(cx - sp5 * 1.2f, by2, "C"); db(cx - sp5 * 1.2f, by2, cx + sp5 * 0.6f, by2); da(cx + sp5 * 0.6f, by2, "C")
                db(cx + sp5 * 0.6f, by2, cx + sp5 * 2.4f, by2); da(cx + sp5 * 2.4f, by2, "C"); da(cx + sp5 * 3.5f, by2 - sp5 * 0.6f, "N", nAt); da(cx + sp5 * 4.3f, by2 - sp5 * 0.6f, "H", hP)
                c.drawText("Diamin", cx + sp5 * 0.6f, by2 + sp5 * 1.4f, sP)

                c.drawText("-> -[CO-NH]-n + n H2O (Nylon 6,6)", cx, h * 0.80f, capP)
                c.drawText("Amid bagi (-CO-NH-) ile baglanir", cx, h * 0.87f, sP)
                c.drawText("Kullanim: Tekstil, halat, yedek parca", cx, h * 0.94f, sP)
            }
            2 -> {
                c.drawText("Termoplastik Polimerler", cx, h * 0.05f, tP)
                c.drawText("Dogrusal zincirler - Van der Waals kuvvetleri", cx, h * 0.11f, sP)
                c.drawText("Tekrar isitilip sekillendirilebilir", cx, h * 0.16f, sP)

                val chainP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 6f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                for (i in 0 until 5) {
                    val cy2 = h * 0.25f + i * h * 0.06f
                    val path = Path(); path.moveTo(w * 0.08f, cy2)
                    for (j in 0 until 9) { path.lineTo(w * 0.08f + j * w * 0.09f, cy2 + sin(j * 0.7f + i * 0.3f) * 4f) }; c.drawPath(path, chainP)
                }
                c.drawText("Polimer zincirleri birbirine paralel", cx, h * 0.64f, sP)
                c.drawText("PE, PP, PVC, PET, PS, PMMA", cx, h * 0.73f, capP)
                c.drawText("Erime: 100-260 C | Geri donusum mumkun", cx, h * 0.80f, sP)
                c.drawText("Kullanim: Ambalaj, oyuncak, elektronik, otomotiv", cx, h * 0.88f, sP)
            }
            3 -> {
                c.drawText("Termoset Polimerler", cx, h * 0.05f, tP)
                c.drawText("Capraz bagli 3D ag - Bir kez sertlesir erimez", cx, h * 0.11f, sP)

                val crossP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
                val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL }
                val stepX = w * 0.18f; val stepY = h * 0.055f; val startX = w * 0.12f; val startY = h * 0.22f
                val nodes = mutableListOf<Pair<Float,Float>>()
                for (i in 0 until 5) for (j in 0 until 4) nodes.add(Pair(startX + i * stepX, startY + j * stepY))
                for (n in nodes) for (m in nodes) { val dx = abs(n.first - m.first); val dy = abs(n.second - m.second)
                    if ((dx < stepX * 0.7f && dy < 4f) || (dx < 4f && dy < stepY * 0.6f) || (dx < stepX * 0.4f && dy < stepY * 0.3f)) c.drawLine(n.first, n.second, m.first, m.second, crossP) }
                for (n in nodes) { c.drawCircle(n.first, n.second, 5f, dotP) }
                c.drawText("Capraz baglar siki ag yapar", cx, h * 0.67f, sP)
                c.drawText("Epoksi, Bakalit, Melamin, Poliuretan", cx, h * 0.75f, capP)
                c.drawText("Yuksek sicaklik dayanimi 150-300 C", cx, h * 0.82f, sP)
                c.drawText("Geri donusum zor | Elektrik yalitimi", cx, h * 0.89f, sP)
            }
            4 -> {
                c.drawText("Elastomerler (Kaucuk)", cx, h * 0.05f, tP)
                c.drawText("Zayif capraz bagli zincirler - Yuksek elastikiyet", cx, h * 0.11f, sP)

                val elaP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                for (i in 0 until 4) {
                    val cy2 = h * 0.25f + i * h * 0.075f
                    val path = Path(); path.moveTo(w * 0.08f, cy2)
                    for (j in 0 until 7) { path.lineTo(w * 0.08f + j * w * 0.11f, cy2 + sin(j * 0.9f + i * 0.6f) * 14f) }; c.drawPath(path, elaP)
                }
                val crossP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 3f }
                for (i in 0 until 3) {
                    val cy2 = h * 0.25f + i * h * 0.075f + h * 0.035f
                    c.drawLine(w * 0.32f, cy2 - 6f, w * 0.42f, cy2 + 6f, crossP2)
                }
                c.drawText("Zayif capraz baglar zincirlerin uzamasina izin verir", cx, h * 0.74f, sP)
                c.drawText("Dogal Kaucuk (izopren), Silikon, Neopren", cx, h * 0.82f, capP)
                c.drawText("Vulkanizasyon: Kukurt ile capraz bag (Goodyear 1839)", cx, h * 0.89f, sP)
                c.drawText("Kullanim: Lastik, conta, eldiven, ayakkabi", cx, h * 0.96f, sP)
            }
        }
    }
}

class PolymersFragment : Fragment() {
    private lateinit var polView: PolymerCanvasView
    private val categories = listOf("Katilma", "Yogunlasma", "Termoplastik", "Termoset", "Elastomer")
    private val details = listOf(
        "Katilma (Zincir) Polimerlesmesi: Doymamis monomerlerin (C=C) radikal, anyonik veya katyonik baslatici ile acilarak zincir olusturmasi. Baslama, uzama ve sonlanma basamaklarindan olusur. Orn: PE (polietilen), PP (polipropilen), PS (polistiren), PVC. Polimerizasyon derecesi (n) 1000-100000.",
        "Yogunlasma (Basamak) Polimerlesmesi: Iki farkli fonksiyonel grup iceren monomerlerin su, metanol veya HCl gibi kucuk molekuller cikararak reaksiyonu. Amid, ester, eter baglari olusur. Orn: Nylon 6,6 (diasit + diamin), Kevlar, Polyester (PET), Polikarbonat.",
        "Termoplastikler: Dogrusal veya dallanmis polimer zincirleri. Zincirler arasi sadece zayif van der Waals kuvvetleri. Isitilinca yumusar, sogutulunca sertlesir. Tekrar tekrar islenebilir, geri donusumu mumkun. Orn: PE, PP, PVC, PET, PS, PMMA, PTFE. Enjekte edilebilir, ekstrude edilebilir.",
        "Termosetler: Capraz bagli uc boyutlu ag yapisi. Ilk isitmada capraz baglar olusur ve sertlesir, tekrar isitilinca erimez (bozusur). Yuksek sicaklik ve kimyasal dayanim. Gevrek olabilir. Orn: Bakalit, Epoksi, Melamin, Poliuretan kopuk. Geri donusum zor.",
        "Elastomerler: Zincirler arasinda seyrek ve zayif capraz baglar. Gerilim uygulandiginda zincirler uzar (dogrusallasir), birakinca eski haline doner. Dogal kaucuk: izopren monomeri, cis-1,4-poliizopren. Vulkanizasyon: kukurt ile capraz baglanma (Goodyear, 1839)."
    )
    private val examples = listOf(
        "PE (torba), PP (boru), PS (kutulan), PVC (pencere), PTFE (Teflon), PMMA (pleksi)",
        "Nylon 6,6 (tekstil), Kevlar (zirh), Polyester, Polikarbonat (CD), PET (sise)",
        "PE (siseler), PP (otomotiv), PVC (boru), PET (icecek), PS (ambalaj), ABS (elektronik)",
        "Bakalit (elektrik), Epoksi (yapistirici), Melamin (mutfak), PU (kopuk yalitim)",
        "Araba lastigi (kaucuk), Conta (silikon), lastik bant (neopren), eldiven"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_polymers, container, false)
        val placeholder = v.findViewById<View>(R.id.pol_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        polView = PolymerCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * resources.displayMetrics.density).toInt())
        }
        parent.addView(polView, idx)

        val btnRow = v.findViewById<LinearLayout>(R.id.pol_btn_row)
        val btnIds = mutableListOf<Button>()
        categories.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 12f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    btnIds.forEach { it.alpha = 0.5f }; alpha = 1f
                    polView.setPolymer(i)
                    v.findViewById<TextView>(R.id.pol_title).text = categories[i]
                    v.findViewById<TextView>(R.id.pol_detail).text = details[i]
                    v.findViewById<TextView>(R.id.pol_examples).text = examples[i]
                }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        return v
    }
}
