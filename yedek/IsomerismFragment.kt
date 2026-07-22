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

class IsomerCanvasView(context: Context) : View(context) {
    private var isoType = 0
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val bond2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val cP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B6B6B.toInt(); style = Paint.Style.FILL }
    private val hP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val oP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); style = Paint.Style.FILL }
    private val nP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3050F8.toInt(); style = Paint.Style.FILL }
    private val bP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val et = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val capP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val mirP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f) }
    private val wedgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(6f, 3f), 0f) }
    private val chiP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); style = Paint.Style.FILL }
    private val highP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    fun setIsomer(type: Int) { isoType = type; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        val c = canvas
        val ar = (w * 0.035f).coerceAtMost(h * 0.05f).coerceAtMost(22f); val sp = ar * 4f; val cx = w / 2f
        tP.textSize = h * 0.06f; sP.textSize = h * 0.035f; capP.textSize = h * 0.045f; highP.textSize = h * 0.04f; et.textSize = ar * 0.9f

        fun da(x: Float, y: Float, elem: String, paint: Paint = cP) {
            c.drawCircle(x, y, ar, paint); c.drawCircle(x, y, ar, bP); et.textSize = ar * 0.9f; c.drawText(elem, x, y + et.textSize / 3f, et)
        }
        fun db(x1: Float, y1: Float, x2: Float, y2: Float, t: Int = 1, w: Boolean = false) {
            if (w) { c.drawLine(x1, y1, x2, y2, wedgeP); return }; val bp = when (t) { 2 -> bond2P; else -> bondP }; c.drawLine(x1, y1, x2, y2, bp)
        }

        when (isoType) {
            0 -> {
                c.drawText("Yapi Izomerligi (Konstitusyonel)", cx, h * 0.05f, tP)
                c.drawText("C4H10 - Ayni formul, farkli baglanti duzeni", cx, h * 0.10f, sP)

                c.drawText("n-Butan (duz)", w * 0.28f, h * 0.16f, capP)
                c.drawText("kn -0.5 C", w * 0.28f, h * 0.95f, highP)

                val ly = h * 0.42f; val sp3 = sp * 0.7f
                val lx = w * 0.28f - sp3 * 1.5f
                for (i in 0 until 4) { da(lx + i * sp3, ly, "C") }
                for (i in 0 until 3) { db(lx + i * sp3, ly, lx + (i + 1) * sp3, ly) }
                c.drawText("4 karbon duz zincir", w * 0.28f, ly + sp3, sP)

                c.drawLine(cx, h * 0.15f, cx, h * 0.75f, mirP)

                c.drawText("Izobutan (dalli)", w * 0.72f, h * 0.16f, capP)
                c.drawText("kn -11.7 C", w * 0.72f, h * 0.95f, highP)

                val ry = h * 0.42f
                da(w * 0.72f, ry, "C")
                da(w * 0.72f - sp3, ry + sp3, "C"); da(w * 0.72f + sp3, ry + sp3, "C"); da(w * 0.72f, ry - sp3, "C")
                db(w * 0.72f, ry, w * 0.72f - sp3, ry + sp3); db(w * 0.72f, ry, w * 0.72f + sp3, ry + sp3); db(w * 0.72f, ry, w * 0.72f, ry - sp3)
                c.drawText("3 karbon dali", w * 0.72f, ry + sp3 * 1.5f, sP)

                c.drawText("Fark: zincir dallanmasi - fiziksel ozellikler degisir", cx, h * 0.04f, sP)
            }
            1 -> {
                c.drawText("Geometrik Izomerlik (Cis-Trans)", cx, h * 0.05f, tP)
                c.drawText("C=C - Cift bag etrafinda donme kisitli", cx, h * 0.10f, sP)

                c.drawText("Cis (ayni tarafta)", w * 0.27f, h * 0.16f, capP)
                c.drawText("Trans (karsi tarafta)", w * 0.73f, h * 0.16f, capP)

                val ly1 = h * 0.50f; val sp4 = sp * 0.9f
                db(w * 0.27f - sp4 * 0.6f, ly1, w * 0.27f + sp4 * 0.6f, ly1, 2)
                da(w * 0.27f - sp4 * 0.6f, ly1, "C"); da(w * 0.27f + sp4 * 0.6f, ly1, "C")
                da(w * 0.27f - sp4 * 1.4f, ly1 - sp4 * 0.7f, "H", hP); da(w * 0.27f - sp4 * 1.4f, ly1 + sp4 * 0.7f, "CH3")
                da(w * 0.27f + sp4 * 1.4f, ly1 - sp4 * 0.7f, "H", hP); da(w * 0.27f + sp4 * 1.4f, ly1 + sp4 * 0.7f, "CH3")
                c.drawText("Cis: polar, dipol moment var", w * 0.27f, h * 0.78f, highP)

                db(w * 0.73f - sp4 * 0.6f, ly1, w * 0.73f + sp4 * 0.6f, ly1, 2)
                da(w * 0.73f - sp4 * 0.6f, ly1, "C"); da(w * 0.73f + sp4 * 0.6f, ly1, "C")
                da(w * 0.73f - sp4 * 1.4f, ly1 - sp4 * 0.7f, "CH3"); da(w * 0.73f - sp4 * 1.4f, ly1 + sp4 * 0.7f, "H", hP)
                da(w * 0.73f + sp4 * 1.4f, ly1 - sp4 * 0.7f, "CH3"); da(w * 0.73f + sp4 * 1.4f, ly1 + sp4 * 0.7f, "H", hP)
                c.drawText("Trans: daha az polar", w * 0.73f, h * 0.78f, highP)

                c.drawText("CH3 ve H konumu farkli fiziksel ozelliklere yol acar", cx, h * 0.92f, sP)
            }
            2 -> {
                c.drawText("Optik Izomerlik (Enantiyomerler)", cx, h * 0.05f, tP)
                c.drawText("Kiral karbon (4 farkli grup) - ayna goruntusu cakismaz", cx, h * 0.10f, sP)

                val ly2 = h * 0.35f; val off = ar * 2.8f

                da(w * 0.27f, ly2, "C", chiP)
                db(w * 0.27f, ly2, w * 0.27f + off, ly2 - off); da(w * 0.27f + off, ly2 - off, "NH2", nP)
                db(w * 0.27f, ly2, w * 0.27f - off * 0.7f, ly2 + off * 0.8f, w = true); da(w * 0.27f - off * 0.7f, ly2 + off * 0.8f, "H", hP)
                db(w * 0.27f, ly2, w * 0.27f + off * 0.5f, ly2 + off * 1.2f); da(w * 0.27f + off * 0.5f, ly2 + off * 1.2f, "CH3")
                db(w * 0.27f, ly2, w * 0.27f - off, ly2 - off * 0.5f); da(w * 0.27f - off, ly2 - off * 0.5f, "COOH")
                c.drawText("L-Alanin (sol)", w * 0.27f, h * 0.85f, capP)

                c.drawLine(cx - sp * 0.3f, h * 0.18f, cx - sp * 0.3f, h * 0.72f, mirP)
                c.drawText("Ayna", cx - sp * 0.3f, h * 0.75f, sP)

                da(w * 0.67f, ly2, "C", chiP)
                db(w * 0.67f, ly2, w * 0.67f - off, ly2 - off); da(w * 0.67f - off, ly2 - off, "NH2", nP)
                db(w * 0.67f, ly2, w * 0.67f + off * 0.7f, ly2 + off * 0.8f, w = true); da(w * 0.67f + off * 0.7f, ly2 + off * 0.8f, "H", hP)
                db(w * 0.67f, ly2, w * 0.67f - off * 0.5f, ly2 + off * 1.2f); da(w * 0.67f - off * 0.5f, ly2 + off * 1.2f, "CH3")
                db(w * 0.67f, ly2, w * 0.67f + off, ly2 - off * 0.5f); da(w * 0.67f + off, ly2 - off * 0.5f, "COOH")
                c.drawText("D-Alanin (sag)", w * 0.67f, h * 0.85f, capP)

                c.drawText("Polarize isigi farkli yonlerde dondurur | Vucutta sadece L-aa kullanilir", cx, h * 0.94f, sP)
            }
            3 -> {
                c.drawText("Fonksiyonel Grup Izomerligi", cx, h * 0.05f, tP)
                c.drawText("C2H6O - Ayni formul, farkli grup, farkli ozellikler", cx, h * 0.10f, sP)

                c.drawText("Etanol (alkol)", w * 0.27f, h * 0.16f, capP)
                val ly3 = h * 0.42f; val sp5 = sp * 0.7f
                da(w * 0.18f, ly3, "C"); da(w * 0.18f + sp5, ly3, "C"); da(w * 0.18f + sp5 * 2f, ly3, "O", oP); da(w * 0.18f + sp5 * 2.8f, ly3, "H", hP)
                db(w * 0.18f, ly3, w * 0.18f + sp5, ly3); db(w * 0.18f + sp5, ly3, w * 0.18f + sp5 * 2f, ly3)
                c.drawText("-OH (alkol) kn 78 C", w * 0.27f, h * 0.70f, highP)
                c.drawText("Suda cozunur, polar", w * 0.27f, h * 0.76f, sP)

                c.drawLine(cx, h * 0.15f, cx, h * 0.68f, mirP)

                c.drawText("Dimetil eter", w * 0.72f, h * 0.16f, capP)
                da(w * 0.65f, ly3, "C"); da(w * 0.65f + sp5 * 1.2f, ly3, "O", oP); da(w * 0.65f + sp5 * 2.4f, ly3, "C")
                db(w * 0.65f, ly3, w * 0.65f + sp5 * 1.2f, ly3); db(w * 0.65f + sp5 * 1.2f, ly3, w * 0.65f + sp5 * 2.4f, ly3)
                c.drawText("C-O-C (eter) kn -24 C", w * 0.72f, h * 0.70f, highP)
                c.drawText("Suda az cozunur", w * 0.72f, h * 0.76f, sP)

                c.drawText("Ayni formul, tamamen farkli kimyasal/fiziksel ozellikler", cx, h * 0.90f, sP)
            }
            4 -> {
                c.drawText("Pozisyon Izomerligi", cx, h * 0.05f, tP)
                c.drawText("C3H8O - OH grubu farkli karbonda, farkli ozellikler", cx, h * 0.10f, sP)

                c.drawText("1-Propanol (OH ucta)", w * 0.27f, h * 0.16f, capP)
                c.drawText("kn 97 C", w * 0.27f, h * 0.92f, highP)
                val ly4 = h * 0.45f; val sp6 = sp * 0.7f
                val lx4 = w * 0.15f
                for (i in 0 until 3) { da(lx4 + i * sp6, ly4, "C") }
                da(lx4 + sp6 * 3f, ly4, "O", oP); da(lx4 + sp6 * 3.7f, ly4, "H", hP)
                for (i in 0 until 3) { db(lx4 + i * sp6, ly4, lx4 + (i + 1) * sp6, ly4) }
                db(lx4 + sp6 * 2f, ly4, lx4 + sp6 * 3f, ly4)
                c.drawText("OH terminal", w * 0.27f, ly4 + sp6, sP)

                c.drawLine(cx, h * 0.15f, cx, h * 0.72f, mirP)

                c.drawText("2-Propanol (OH ortada)", w * 0.72f, h * 0.16f, capP)
                c.drawText("kn 83 C", w * 0.72f, h * 0.92f, highP)
                val rx4 = w * 0.60f
                da(rx4, ly4, "C"); da(rx4 - sp6, ly4, "C"); da(rx4 + sp6, ly4, "C")
                da(rx4, ly4 - sp6, "O", oP); da(rx4, ly4 - sp6 * 1.7f, "H", hP)
                db(rx4 - sp6, ly4, rx4, ly4); db(rx4, ly4, rx4 + sp6, ly4); db(rx4, ly4, rx4, ly4 - sp6)
                c.drawText("OH 2. karbon", w * 0.72f, ly4 + sp6, sP)

                c.drawText("OH pozisyonu kaynama noktasini 14 C degistirir", cx, h * 0.96f, sP)
            }
        }
    }
}

class IsomerismFragment : Fragment() {
    private lateinit var isoView: IsomerCanvasView
    private val categories = listOf("Yapi", "Geometrik", "Optik", "Fonk. Grup", "Pozisyon")
    private val details = listOf(
        "Yapi (Konstitusyonel) Izomerlik: Ayni molekul formulu farkli bag yapisi. n-butan (C4H10, duz zincir, kn -0.5 C) ve izobutan (C4H10, dallanmis, kn -11.7 C). Fiziksel ve kimyasal ozellikleri farkli. Karbon iskeleti farkli sekilde duzenlenmistir. Daha fazla karbon iceren molekullerde izomer sayisi hizla artar (C8H18: 18 izomer).",
        "Geometrik (Cis-Trans) Izomerlik: C=C cift bagi etrafinda gorulur, donme kisitlidir. Cis: ayni gruplar ayni tarafta, polar, dipol moment var. Trans: karsi tarafta, daha az polar. Erime noktasi, kaynama noktasi, polarite farklidir. Cis-2-buten vs trans-2-buten.",
        "Optik Izomerlik: Kiral karbon (asimetrik karbon, 4 farkli grup bagli). Enantiyomerler birbirinin ayna goruntusudur ve cakismaz. Polarize isigi farkli yonlerde dondurur (L: sol, D: sag). Biyolojik sistemler sadece L-amino asitleri kullanir. Ila molekullerinde bir enantiyomer etkili, digeri etkisiz/zirarli olabilir.",
        "Fonksiyonel Grup Izomerligi: Ayni molekul formulu farkli fonksiyonel grup. Etanol (alkol, -OH, kn 78 C, polar, suda cozunur) ve dimetil eter (eter, C-O-C, kn -24 C, apolar, suda az cozunur). Fiziksel/kimyasal ozellikler tamamen farkli. C2H6O -> alkol veya eter.",
        "Pozisyon Izomerligi: Fonksiyonel grup ayni zincirde farkli pozisyonda. 1-propanol (OH terminal, kn 97 C) ve 2-propanol (OH ortada, kn 83 C). Fiziksel ozellikler (kn, en, yogunluk) farklidir. Reaktivite de farkli olabilir (1-propanol daha reaktif)."
    )
    private val props = listOf(
        "n-butan: en -138 C kn -0.5 C | izobutan: en -159 C kn -11.7 C | Duz ve dalli zincir",
        "cis-2-buten: en -139 C kn 3.7 C polar | trans-2-buten: en -106 C kn 0.9 C polar degil",
        "Alanin [a]D = +8.5 (L) / -8.5 (D) | Sadece L-aa vucutta kullanilir | Kiral ilaclar",
        "Etanol: kn 78 C, polar, suda cozunur | Eter: kn -24 C, apolar, suda az cozunur",
        "1-propanol: kn 97 C | 2-propanol: kn 83 C | 14 C fark | Farkli reaktivite"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_isomerism, container, false)
        val placeholder = v.findViewById<View>(R.id.iso_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        isoView = IsomerCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * resources.displayMetrics.density).toInt())
        }
        parent.addView(isoView, idx)

        val btnRow = v.findViewById<LinearLayout>(R.id.iso_btn_row)
        val btnIds = mutableListOf<Button>()
        categories.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 12f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    btnIds.forEach { it.alpha = 0.5f }; alpha = 1f
                    isoView.setIsomer(i)
                    v.findViewById<TextView>(R.id.iso_title).text = categories[i]
                    v.findViewById<TextView>(R.id.iso_detail).text = details[i]
                    v.findViewById<TextView>(R.id.iso_prop).text = props[i]
                }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        return v
    }
}
