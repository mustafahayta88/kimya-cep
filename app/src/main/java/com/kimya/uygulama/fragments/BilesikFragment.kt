package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData
import kotlin.math.abs
import android.widget.EditText
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class BilesikCanvasView(context: Context) : View(context) {
    var atom1: Map<String, Any>? = null
    var atom2: Map<String, Any>? = null
    var bagTipi: String = ""
    var formul: String = ""
    var bilesikAdi: String = ""

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_MOVE -> { if (tMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }; lastTx = e.x; lastTy = e.y; invalidate() }
            MotionEvent.ACTION_UP -> { tMode = 0; return true }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cy = h * 0.42f
        val a1 = atom1 ?: return; val a2 = atom2 ?: return
        val no1 = (a1["atomNo"] as? Int) ?: 0; val no2 = (a2["atomNo"] as? Int) ?: 0
        val ad1 = (a1["sembol"] as? String) ?: ""; val ad2 = (a2["sembol"] as? String) ?: ""
        val elKonf1 = (a1["elektron"] as? String) ?: ""; val elKonf2 = (a2["elektron"] as? String) ?: ""

        val bagRenk = when {
            bagTipi.contains("Iyonik", true) -> Color.rgb(255, 150, 50)
            bagTipi.contains("Metalik", true) -> Color.rgb(200, 100, 255)
            else -> Color.rgb(57, 255, 20)
        }

        // Background card
        val bgPaint = Paint().apply { color = Color.argb(12, 57, 255, 20); style = Paint.Style.FILL }
        canvas.drawRoundRect(4f, 4f, w - 4f, h - 4f, 16f, 16f, bgPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        // Draw atom with electron shells
        fun drawAtomShells(cx: Float, s: String, no: Int, elektronConf: String) {
            val cR = min(w * 0.10f, 38f)

            // Parse shells from electron config
            val shells = parseBasitKabuklar(elektronConf, no)
            val maxShell = min(shells.size, 4)

            // Draw orbital circles
            val orbPaint = Paint().apply { color = Color.argb(40, 0, 200, 255); style = Paint.Style.STROKE; strokeWidth = 1f }
            for (i in 0 until maxShell) {
                val r = cR + 10f + (i + 1) * 14f
                canvas.drawCircle(cx, cy, r, orbPaint)
                // Shell label
                val lblPaint = Paint().apply { color = Color.argb(80, 0, 200, 255); textSize = 8f; textAlign = Paint.Align.CENTER }
                canvas.drawText("n=${i+1}", cx, cy - r - 3f, lblPaint)
            }

            // Atom glow
            val glowPaint = Paint().apply {
                color = Color.argb(25, 57, 255, 20); style = Paint.Style.FILL
                setShadowLayer(15f, 0f, 0f, Color.argb(60, 57, 255, 20))
            }
            canvas.drawCircle(cx, cy, cR + 8f, glowPaint)

            // Atom body
            val atomGrad = Paint().apply { color = Color.argb(90, 57, 255, 20); style = Paint.Style.FILL }
            canvas.drawCircle(cx, cy, cR, atomGrad)
            canvas.drawCircle(cx, cy, cR, Paint().apply { color = Color.argb(150, 57, 255, 20); style = Paint.Style.STROKE; strokeWidth = 2f })

            // Symbol
            val symPaint = Paint().apply { color = Color.WHITE; textSize = cR * 0.75f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.MONOSPACE }
            canvas.drawText(s, cx, cy + cR * 0.28f, symPaint)

            // Element name below
            val elAdi = KimyaData.elementler[s]?.adi ?: ""
            val namePaint = Paint().apply { color = Color.argb(150, 200, 200, 200); textSize = 10f; textAlign = Paint.Align.CENTER }
            canvas.drawText(elAdi, cx, cy + cR + 16f, namePaint)

            // Electron dots on shells
            val ePaint = Paint().apply { color = Color.argb(200, 57, 255, 20); style = Paint.Style.FILL }
            for (i in 0 until maxShell) {
                val count = min(shells[i], 20)
                val r = cR + 10f + (i + 1) * 14f
                for (e in 0 until count) {
                    val angle = -90.0 + e * (360.0 / count)
                    val rad = Math.toRadians(angle)
                    val ex = cx + r * cos(rad).toFloat()
                    val ey = cy + r * sin(rad).toFloat()
                    canvas.drawCircle(ex, ey, 2.5f, ePaint)
                }
            }
        }

        val leftX = w * 0.22f
        val rightX = w * 0.78f
        drawAtomShells(leftX, ad1, no1, elKonf1)
        drawAtomShells(rightX, ad2, no2, elKonf2)

        // Bond line
        val bondPaint = Paint().apply { color = bagRenk; strokeWidth = 3f; style = Paint.Style.STROKE }
        val shellR = min(w * 0.10f, 38f) + 10f + 4 * 14f
        val bondStart = leftX + shellR
        val bondEnd = rightX - shellR
        canvas.drawLine(bondStart, cy, bondEnd, cy, bondPaint)

        // Bond type badge
        val badgePaint = Paint().apply { color = bagRenk; style = Paint.Style.FILL; isAntiAlias = true }
        val badgeW = 80f; val badgeH = 22f
        canvas.drawRoundRect(w / 2f - badgeW / 2f, cy - badgeH - 8f, w / 2f + badgeW / 2f, cy - 8f, 11f, 11f, badgePaint)
        val badgeTextPaint = Paint().apply { color = Color.BLACK; textSize = 11f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText(bagTipi, w / 2f, cy - 8f - 5f, badgeTextPaint)

        // Formula - large
        val formPaint = Paint().apply { color = Color.argb(220, 255, 255, 200); textSize = 22f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.MONOSPACE }
        canvas.drawText(formul, w / 2f, h - 20f, formPaint)

        // Compound name
        if (bilesikAdi.isNotEmpty()) {
            val adPaint = Paint().apply { color = Color.argb(160, 200, 255, 200); textSize = 12f; textAlign = Paint.Align.CENTER }
            canvas.drawText(bilesikAdi, w / 2f, h - 36f, adPaint)
        }

        // Bond region visualization
        val midX = (leftX + rightX) / 2f
        if (bagTipi.contains("Iyonik", true)) {
            // Electron transfer arrow
            val arrowPaint = Paint().apply { color = Color.rgb(255, 150, 50); strokeWidth = 2f; style = Paint.Style.STROKE }
            val arrowY = cy - 36f
            canvas.drawLine(midX - 20f, arrowY, midX + 20f, arrowY, arrowPaint)
            canvas.drawLine(midX + 20f, arrowY, midX + 12f, arrowY - 7f, arrowPaint)
            canvas.drawLine(midX + 20f, arrowY, midX + 12f, arrowY + 7f, arrowPaint)
            val etPaint = Paint().apply { color = Color.argb(160, 255, 200, 100); textSize = 9f; textAlign = Paint.Align.CENTER }
            canvas.drawText("e- transferi", midX, arrowY - 10f, etPaint)
            // Ion charges
            val chargePaint = Paint().apply { color = Color.argb(180, 255, 200, 100); textSize = 16f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText("+", leftX + 5f, cy - shellR - 4f, chargePaint)
            canvas.drawText("-", rightX - 5f, cy - shellR - 4f, chargePaint)
        } else {
            // Shared electron pairs
            val sePaint = Paint().apply { color = Color.argb(200, 57, 255, 20); style = Paint.Style.FILL }
            canvas.drawCircle(midX - 6f, cy - 4f, 3f, sePaint)
            canvas.drawCircle(midX + 6f, cy - 4f, 3f, sePaint)
            val ortPaint = Paint().apply { color = Color.argb(140, 200, 255, 200); textSize = 9f; textAlign = Paint.Align.CENTER }
            canvas.drawText("ortak e-", midX, cy - 18f, ortPaint)

            // Polarity (for kovalent non-metal diverse)
            val en1 = KimyaData.elektronegatiflikler[ad1] ?: 0.0
            val en2 = KimyaData.elektronegatiflikler[ad2] ?: 0.0
            if (abs(en1 - en2) > 0.4 && abs(en1 - en2) < 2.0) {
                val polarPaint = Paint().apply { color = Color.argb(160, 255, 200, 100); textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                if (en1 > en2) {
                    canvas.drawText("d-", leftX + 5f, cy - shellR - 4f, polarPaint)
                    canvas.drawText("d+", rightX - 5f, cy - shellR - 4f, polarPaint)
                } else {
                    canvas.drawText("d+", leftX + 5f, cy - shellR - 4f, polarPaint)
                    canvas.drawText("d-", rightX - 5f, cy - shellR - 4f, polarPaint)
                }
            }
        }
        canvas.restore()
    }

    private fun parseBasitKabuklar(elektronConf: String, atomNo: Int): List<Int> {
        var config = elektronConf.trim()
        var base = 0
        for ((core, cnt) in mapOf("[He]" to 2, "[Ne]" to 10, "[Ar]" to 18, "[Kr]" to 36, "[Xe]" to 54, "[Rn]" to 86)) {
            if (config.startsWith(core)) { base = cnt; config = config.removePrefix(core).trim(); break }
        }
        val shells = mutableListOf<Int>()
        var rem = base; var n = 1
        while (rem > 0) { val c = min(2 * n * n, rem); shells.add(c); rem -= c; n++ }
        val sub = mutableMapOf<Int, Int>()
        for (m in Regex("""(\d+)[spdI](\d+)""").findAll(config)) {
            val sn = m.groupValues[1].toInt(); val cnt = m.groupValues[2].toInt()
            sub[sn] = (sub[sn] ?: 0) + cnt
        }
        for ((sn, cnt) in sub) { while (shells.size < sn) shells.add(0); shells[sn-1] = shells[sn-1] + cnt }
        return shells
    }
}

class BilesikFragment : Fragment() {
    private var el1Idx = 0; private var d1Idx = 0
    private var el2Idx = 0; private var d2Idx = 0
    private val elementAdlari = KimyaData.elementler.values.map { it.adi }.toList()
    private val degerlikler = listOf(1, 2, 3, 4, -1, -2, -3)
    private lateinit var canvasView: BilesikCanvasView
    private var sonFormul: String = ""
    private var sonBagTipi: String = ""

    private val bilinenBilesikler = mapOf(
        "NaCl" to "Sodyum Klorur (Sofra Tuzu)", "H2O" to "Su (Dihidrojen Monoksit)",
        "FeO" to "Demir Oksit", "Fe2O3" to "Demir Oksit",
        "MgCl2" to "Magnezyum Klorur", "MgO" to "Magnezyum Oksit",
        "HCl" to "Hidrojen Klorur (Tuz Ruhu)", "CO2" to "Karbon Dioksit",
        "NH3" to "Amonyak", "CH4" to "Metan",
        "CaO" to "Kalsiyum Oksit (Kirec)", "CaCl2" to "Kalsiyum Klorur",
        "Na2O" to "Sodyum Oksit", "KCl" to "Potasyum Klorur",
        "SO2" to "Karbon Dioksit", "NO2" to "Azot Dioksit"
    )

    private fun bilesikAdiBul(formul: String): String {
        return bilinenBilesikler[formul] ?: run {
            val list = KimyaData.elementler.values.toList()
            val e1 = if (el1Idx < list.size) list[el1Idx] else return@run ""
            val e2 = if (el2Idx < list.size) list[el2Idx] else return@run ""
            val metal = listOf(e1, e2).filter { it.tur.contains("Metal") || it.tur == "Toprak Alkali" }
            val ametal = listOf(e1, e2).filter { it.tur == "Ametal" || it.tur == "Soy Gaz" }
            if (metal.isNotEmpty() && ametal.isNotEmpty()) {
                val mAd = metal[0].adi.replaceFirstChar { it.uppercase() }
                val aAd = ametal[0].adi.replaceFirstChar { if (it == 'i' || it == 'I') 'I' else it.uppercaseChar() }
                "$mAd $aAd"
            } else ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_bilesik, container, false)
        val el1 = v.findViewById<Spinner>(R.id.bilesik_el1)
        val d1 = v.findViewById<Spinner>(R.id.bilesik_d1)
        val el2 = v.findViewById<Spinner>(R.id.bilesik_el2)
        val d2 = v.findViewById<Spinner>(R.id.bilesik_d2)
        val sonuc = v.findViewById<TextView>(R.id.bilesik_sonuc)

        // Canvas
        val canvasPlaceholder = v.findViewById<View>(R.id.bilesik_canvas)
        val parent = canvasPlaceholder.parent as ViewGroup
        val idx = parent.indexOfChild(canvasPlaceholder)
        parent.removeView(canvasPlaceholder)
        canvasView = BilesikCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
        }
        parent.addView(canvasView, idx)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, elementAdlari)
        el1.adapter = adapter; el2.adapter = adapter
        val degAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            degerlikler.map { if (it > 0) "+$it" else "$it" })
        d1.adapter = degAdapter; d2.adapter = degAdapter

        el1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { el1Idx = pos }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        el2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { el2Idx = pos }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        d1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { d1Idx = pos }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        d2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { d2Idx = pos }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        fun olustur(): Triple<String, String, String>? {
            val list = KimyaData.elementler.values.toList()
            if (el1Idx >= list.size || el2Idx >= list.size) return null
            val e1 = list[el1Idx]; val e2 = list[el2Idx]
            val dv1 = degerlikler[d1Idx]; val dv2 = degerlikler[d2Idx]
            val d1abs = abs(dv1); val d2abs = abs(dv2)
            val c1 = d2abs; val c2 = d1abs
            val g = gcd(c1, c2)
            val k1 = c1 / g; val k2 = c2 / g
            val formul = when { k1 == 1 && k2 == 1 -> "${e1.semIol}${e2.semIol}"
                k1 == 1 -> "${e1.semIol}${e2.semIol}$k2"
                k2 == 1 -> "${e1.semIol}$k1${e2.semIol}"
                else -> "${e1.semIol}$k1${e2.semIol}$k2" }
            val bag = bagTipiBul(e1.semIol, e2.semIol)
            val ad = bilesikAdiBul(formul)
            sonFormul = formul; sonBagTipi = bag
            canvasView.atom1 = mapOf("sembol" to e1.semIol, "atomNo" to e1.atomNo, "elektron" to e1.elektron, "grup" to e1.grup)
            canvasView.atom2 = mapOf("sembol" to e2.semIol, "atomNo" to e2.atomNo, "elektron" to e2.elektron, "grup" to e2.grup)
            canvasView.bagTipi = bag; canvasView.formul = formul; canvasView.bilesikAdi = ad
            canvasView.invalidate()
            return Triple(formul, bag, ad)
        }

        fun bagAciklamasi(bag: String): String = when {
            bag.contains("Iyonik", true) -> "Iyonik bag: Metal (+) elektron verir, Ametal (-) alir. Kristal kafes olusur. Yuksek erime noktasi. Siviyken elektrik iletir."
            bag.contains("Metalik", true) -> "Metalik bag: Metal atomlari arasinda serbest elektron bulutu. Yuksek iletkenlik. Parlak ve islenebilir."
            else -> "Kovalent bag: Ametaller arasinda elektron ortaklasmasi. Molekuler yapi. Dusuk erime noktasi. Polar/apolar olabilir."
        }

        v.findViewById<Button>(R.id.bilesik_itn).setOnClickListener {
            val r = olustur() ?: return@setOnClickListener
            val list = KimyaData.elementler.values.toList()
            val e1 = list[el1Idx]; val e2 = list[el2Idx]
            val dv1 = degerlikler[d1Idx]; val dv2 = degerlikler[d2Idx]
            val adSatiri = if (r.third.isNotEmpty()) "\nBilesik Adi: ${r.third}\n" else "\n"
            val en1 = KimyaData.elektronegatiflikler[e1.semIol] ?: 0.0
            val en2 = KimyaData.elektronegatiflikler[e2.semIol] ?: 0.0
            val fark = abs(en1 - en2)
            val polarite = when {
                fark > 1.7 -> "Iyonik (EN farki > 1.7)"
                fark > 0.4 -> "Polar Kovalent"
                else -> "Apolar Kovalent"
            }
            sonuc.text = """
[OLUSAN BILESIK]
Formul: ${r.first}  |  Bag: ${r.second}$adSatiri
Polarite: $polarite (EN farki: ${"%.2f".format(fark)})

[BILESENLER]
${e1.adi} (${e1.semIol}): Z=${e1.atomNo}, ${e1.tur}, degerlik: ${if(dv1>0)"+"else""}$dv1
${e2.adi} (${e2.semIol}): Z=${e2.atomNo}, ${e2.tur}, degerlik: ${if(dv2>0)"+"else""}$dv2

[BAG ACIKLAMASI]
${bagAciklamasi(r.second)}
""".trimMargin()
        }

        v.findViewById<Button>(R.id.bilesik_rastgele).setOnClickListener {
            el1.setSelection(Random.nextInt(elementAdlari.size))
            d1.setSelection(Random.nextInt(degerlikler.size))
            el2.setSelection(Random.nextInt(elementAdlari.size))
            d2.setSelection(Random.nextInt(degerlikler.size))
            v.findViewById<Button>(R.id.bilesik_itn).performClick()
        }

        v.findViewById<Button>(R.id.bilesik_enerji).setOnClickListener {
            val list = KimyaData.elementler.values.toList()
            if (el1Idx >= list.size || el2Idx >= list.size) return@setOnClickListener
            val e1 = list[el1Idx]; val e2 = list[el2Idx]
            val ie1 = KimyaData.iyonlasmaEnerjileri[e1.semIol] ?: 0.0
            val ie2 = KimyaData.iyonlasmaEnerjileri[e2.semIol] ?: 0.0
            val en1 = KimyaData.elektronegatiflikler[e1.semIol] ?: 0.0
            val en2 = KimyaData.elektronegatiflikler[e2.semIol] ?: 0.0
            val fark = abs(en1 - en2)
            val bagYorum = when {
                fark > 1.7 -> "Iyonik bag"
                fark > 0.4 -> "Polar kovalent bag"
                else -> "Apolar kovalent bag"
            }
            sonuc.text = """
[ENERJI ve ELEKTRONEGATIFLIK]
Iyonlasma Enerjisi (kJ/mol):
  ${e1.semIol} (${e1.adi}): ${"%.1f".format(ie1)}
  ${e2.semIol} (${e2.adi}): ${"%.1f".format(ie2)}

Elektronegatiflik (Pauling):
  ${e1.semIol}: ${"%.2f".format(en1)}
  ${e2.semIol}: ${"%.2f".format(en2)}
  Fark: ${"%.2f".format(fark)}

Tahmini Bag: $bagYorum
""".trimMargin()
        }

        v.findViewById<Button>(R.id.bilesik_Iart).setOnClickListener {
            val r = olustur() ?: return@setOnClickListener
            val mK = KimyaData.molekulKutlesiHesapla(r.first)
            val adSatiri = if (r.third.isNotEmpty()) "Bilesik: ${r.third}\n" else ""
            sonuc.text = """
[KART - ${r.first}]
$adSatiri
Molekul Kutlesi: ${if (mK != null) "${"%.4f".format(mK)} g/mol" else "hesaplanamadi"}
Bag Tipi: ${r.second}
""".trimMargin()
        }

        fun hizliReaksiyon(el1S: String, el2S: String, d1Val: Int? = null, d2Val: Int? = null) {
            val i1 = elementAdlari.indexOfFirst { KimyaData.elementler[el1S]?.adi == it }
            val i2 = elementAdlari.indexOfFirst { KimyaData.elementler[el2S]?.adi == it }
            if (i1 >= 0) el1.setSelection(i1)
            if (i2 >= 0) el2.setSelection(i2)
            d1.setSelection(d1Val?.let { degerlikler.indexOf(it).coerceAtLeast(0) } ?: 0)
            d2.setSelection(d2Val?.let { degerlikler.indexOf(it).coerceAtLeast(0) } ?: degerlikler.indexOf(-1).coerceAtLeast(0))
            v.findViewById<Button>(R.id.bilesik_itn).performClick()
        }

        v.findViewById<Button>(R.id.bilesik_hizli1).setOnClickListener { hizliReaksiyon("Na", "Cl") }
        v.findViewById<Button>(R.id.bilesik_hizli2).setOnClickListener { hizliReaksiyon("H", "O") }
        v.findViewById<Button>(R.id.bilesik_hizli3).setOnClickListener { hizliReaksiyon("Fe", "O") }
        v.findViewById<Button>(R.id.bilesik_hizli4).setOnClickListener { hizliReaksiyon("Mg", "Cl") }
        v.findViewById<Button>(R.id.bilesik_hizli5).setOnClickListener { hizliReaksiyon("H", "Cl") }
        v.findViewById<Button>(R.id.bilesik_hizli6).setOnClickListener { hizliReaksiyon("C", "H") }
        v.findViewById<Button>(R.id.bilesik_hizli7).setOnClickListener { hizliReaksiyon("N", "H") }
        v.findViewById<Button>(R.id.bilesik_hizli8).setOnClickListener { hizliReaksiyon("Ca", "O") }

        v.findViewById<Button>(R.id.bilesik_sart_itn).setOnClickListener {
            if (sonFormul.isEmpty()) { sonuc.text = "Once bir bilesik olusturun (Olustur butonu)"; return@setOnClickListener }
            val t = v.findViewById<EditText>(R.id.bilesik_T).text.toString().toDoubleOrNull() ?: 298.0
            val p = v.findViewById<EditText>(R.id.bilesik_P).text.toString().toDoubleOrNull() ?: 1.0
            val mK = KimyaData.molekulKutlesiHesapla(sonFormul)
            if (mK == null) { sonuc.text = "Molekul kutlesi hesaplanamadi"; return@setOnClickListener }
            val atomSayisi = atomSayisiHesapla(sonFormul)
            val kutleEtkisi = min(mK / 10.0, 40.0)
            val (deltaH, deltaS) = if (sonBagTipi.contains("Iyonik", true)) {
                -120.0 - (35.0 * atomSayisi) - kutleEtkisi to -0.08
            } else {
                -40.0 - (18.0 * atomSayisi) - (kutleEtkisi / 2.0) to -0.03
            }
            val deltaG = deltaH - (t * deltaS)
            val R = 8.314
            val exponent = max(min((-deltaG * 1000.0) / (R * t), 60.0), -60.0)
            val dengeK = exp(exponent)
            val yon = if (deltaH < 0) "Ekzotermik (isi verir)" else "Endotermik (isi alir)"

            sonuc.text = """
[TERMODINAMIK - ${sonFormul}]
Sicaklik: ${"%.0f".format(t)} K  |  Basinc: ${"%.1f".format(p)} atm

Entalpi (DH): ${"%.2f".format(deltaH)} kJ/mol -> $yon
Entropi (DS): ${"%.4f".format(deltaS)} kJ/(mol.K)
Serbest Enerji (DG): ${"%.2f".format(deltaG)} kJ/mol
Denge Sabiti (K): ${"%.6e".format(dengeK)}

Atom Sayisi: $atomSayisi  |  Mol Kutlesi: ${"%.2f".format(mK)} g/mol
""".trimMargin()
        }

        v.findViewById<Button>(R.id.bilesik_ozelliI_itn).setOnClickListener {
            if (sonFormul.isEmpty()) { sonuc.text = "Once bir bilesik olusturun (Olustur butonu)"; return@setOnClickListener }
            val mK = KimyaData.molekulKutlesiHesapla(sonFormul)
            if (mK == null) { sonuc.text = "Molekul kutlesi hesaplanamadi"; return@setOnClickListener }
            val iyonik = sonBagTipi.contains("Iyonik", true)
            val erime = when {
                mK < 50 -> if (iyonik) "400-800 C" else "-200 ile -50 C"
                mK < 100 -> if (iyonik) "800-1500 C" else "-50 ile 50 C"
                else -> if (iyonik) ">1500 C" else "50-200 C"
            }
            val hal = when { mK < 30 -> "Gaz"; mK < 100 -> "Sivi"; else -> "Kati" }
            val yapi = if (iyonik) "Kristal Kafes" else "Molekuler"
            val iletkenlik = if (iyonik) "Siviyken iletken, katiken degil" else "Iletken degil"
            val cozunurluk = if (iyonik) "Suda iyi cozunur" else "Polar ise suda, apolar ise organik cozuculerde"
            val yogunluk = if (iyonik) "2-6 g/cm3" else "0.5-3 g/cm3"

            sonuc.text = """
[OZELLIKLER - ${sonFormul}]
Molekul Kutlesi: ${"%.2f".format(mK)} g/mol
Bag Turu: ${if (iyonik) "Iyonik Bag" else "Kovalent Bag"}
Yapi: $yapi  |  Hal (25 C): $hal
Erime Noktasi: $erime
Iletkenlik: $iletkenlik
Cozunurluk: $cozunurluk
Yogunluk: $yogunluk
""".trimMargin()
        }

        v.findViewById<Button>(R.id.bilesik_kullanim_itn).setOnClickListener {
            if (sonFormul.isEmpty()) { sonuc.text = "Once bir bilesik olusturun (Olustur butonu)"; return@setOnClickListener }
            val form = sonFormul
            val kullanim = when {
                "O" in form && listOf("Fe", "Cu", "Al", "Ti").any { it in form } ->
                    "Metalurji ve alasim\nSeramik ve cam\nBoya ve pigment"
                "Cl" in form -> "Su aritma\nPlastik (PVC)\nGida koruma"
                "S" in form -> "Gubre\nKaucuk\nIlac sanayi"
                "N" in form -> "Gubre ve patlayici\nBoya\nGida katki"
                "C" in form && "H" in form -> "Organik kimya\nYakit\nPlastik ve polimer"
                "Na" in form || "K" in form -> "Gida (tuz)\nSabun ve deterjan\nCam uretimi"
                "Ca" in form -> "Insaat\nGubre\nIlac"
                else -> "Laboratuvar\nArastirma\nEndustriyel sentez"
            }
            sonuc.text = """
[KULLANIM - ${sonFormul}]
$kullanim
""".trimMargin()
        }

        return v
    }

    private fun bagTipiBul(sym1: String, sym2: String): String {
        val e1 = KimyaData.elementler[sym1] ?: return "Kovalent"
        val e2 = KimyaData.elementler[sym2] ?: return "Kovalent"
        val tur1 = e1.tur; val tur2 = e2.tur
        val metal1 = tur1.contains("Metal") || tur1 == "Toprak Alkali" || tur1 == "Lantanit" || tur1 == "Aktinit"
        val metal2 = tur2.contains("Metal") || tur2 == "Toprak Alkali" || tur2 == "Lantanit" || tur2 == "Aktinit"
        return when {
            metal1 && (tur2 == "Ametal" || tur2 == "Soy Gaz") -> "Iyonik"
            metal2 && (tur1 == "Ametal" || tur1 == "Soy Gaz") -> "Iyonik"
            metal1 && metal2 -> "Metalik"
            else -> "Kovalent"
        }
    }

    private fun atomSayisiHesapla(formul: String): Int {
        var toplam = 0
        for (m in Regex("([A-Z][a-z]?)(\\d*)").findAll(formul)) {
            toplam += m.groupValues[2].toIntOrNull() ?: 1
        }
        return toplam
    }

    companion object {
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    }
}
