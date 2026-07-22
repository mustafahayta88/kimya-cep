package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.min

class TrendChartView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }
    var trendTipi: String = "Iyonlasma Enerjisi"
    var gosterim: String = "Periyot 2"
    var seciliIndex: Int = -1
    var onElementClick: ((String) -> Unit)? = null

    val periyotElementleri = mapOf(
        "Periyot 2" to listOf("Li", "Be", "B", "C", "N", "O", "F", "Ne"),
        "Periyot 3" to listOf("Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar"),
        "Periyot 4" to listOf("K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr"),
        "Grup 1" to listOf("H", "Li", "Na", "K", "Rb", "Cs"),
        "Grup 17" to listOf("F", "Cl", "Br", "I"),
        "Grup 18" to listOf("He", "Ne", "Ar", "Kr"),
    )

    val atomYaricapTahmin = mapOf(
        "H" to 53, "He" to 31, "Li" to 167, "Be" to 112, "B" to 87,
        "C" to 67, "N" to 56, "O" to 48, "F" to 42, "Ne" to 38,
        "Na" to 190, "Mg" to 145, "Al" to 118, "Si" to 111, "P" to 98,
        "S" to 88, "Cl" to 79, "Ar" to 71,
        "K" to 243, "Ca" to 194, "Sc" to 184, "Ti" to 176, "V" to 171,
        "Cr" to 166, "Mn" to 161, "Fe" to 156, "Co" to 152, "Ni" to 149,
        "Cu" to 145, "Zn" to 142, "Ga" to 136, "Ge" to 125, "As" to 114,
        "Se" to 103, "Br" to 94, "Kr" to 88,
        "Rb" to 265, "Sr" to 219, "I" to 115, "Cs" to 298, "Ba" to 253,
        "Au" to 144, "Hg" to 151, "Pb" to 175, "Pt" to 139, "Ag" to 144,
        "Cd" to 151, "Sn" to 140, "U" to 138
    )

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

        val elems = periyotElementleri[gosterim] ?: return
        if (elems.isEmpty()) return

        // Background
        val bgPaint = Paint().apply { color = Color.argb(12, 57, 255, 20); style = Paint.Style.FILL }
        canvas.drawRoundRect(4f, 4f, w - 4f, h - 4f, 12f, 12f, bgPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        // Title
        val titlePaint = Paint().apply { color = Color.argb(180, 255, 255, 200); textSize = 13f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("$trendTipi - $gosterim", w / 2f, 22f, titlePaint)

        // Gather values
        val values = elems.mapNotNull { s ->
            val v = when (trendTipi) {
                "Iyonlasma Enerjisi" -> KimyaData.iyonlasmaEnerjileri[s]
                "Elektronegatiflik" -> KimyaData.elektronegatiflikler[s]
                "Atom Kutlesi" -> KimyaData.elementler[s]?.kutle
                "Atom Yaricapi" -> atomYaricapTahmin[s]?.toDouble()
                "Metalik Karakter" -> {
                    val el = KimyaData.elementler[s]
                    if (el != null) {
                        when {
                            el.tur.contains("Metal") || el.tur == "Toprak Alkali" -> 10.0
                            el.tur == "Yari Metal" -> 5.0
                            else -> 1.0
                        }
                    } else null
                }
                else -> null
            }
            if (v != null && v > 0) s to v else null
        }

        if (values.isEmpty()) {
            val noPaint = Paint().apply { color = Color.argb(100, 200, 200, 200); textSize = 14f; textAlign = Paint.Align.CENTER }
            canvas.drawText("Bu kume icin veri yok", w / 2f, h / 2f, noPaint)
            return
        }

        val maxVal = values.maxOf { it.second }
        val minVal = values.minOf { it.second }
        val fark = maxVal - minVal
        val chartTop = 32f; val chartBottom = h - 28f; val chartH = chartBottom - chartTop
        val barCount = values.size
        val barW = min(w * 0.7f / barCount, 50f)
        val gap = if (barCount > 1) (w * 0.85f - barW * barCount) / (barCount - 1) else 0f
        val startX = (w - (barW * barCount + gap * (barCount - 1))) / 2f

        // Baseline
        val basePaint = Paint().apply { color = Color.argb(60, 200, 200, 200); strokeWidth = 1f }
        canvas.drawLine(startX, chartBottom, startX + barW * barCount + gap * (barCount - 1), chartBottom, basePaint)

        val barPaint = Paint().apply { style = Paint.Style.FILL }
        val barStroke = Paint().apply { color = Color.argb(60, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1f }
        val labelPaint = Paint().apply { color = Color.argb(180, 200, 200, 200); textSize = 10f; textAlign = Paint.Align.CENTER }
        val valPaint = Paint().apply { textSize = 9f; textAlign = Paint.Align.CENTER }

        for ((i, pair) in values.withIndex()) {
            val (sembol, value) = pair
            val x = startX + i * (barW + gap)
            val barH = if (fark > 0) ((value - minVal) / fark * chartH).toFloat() else chartH * 0.5f
            val y = chartBottom - barH

            val el = KimyaData.elementler[sembol]
            val renk = if (el != null) KimyaData.elementRengi(el.tur) else Color.GRAY
            val highlight = i == seciliIndex

            // Bar
            barPaint.color = if (highlight) Color.argb(220, Color.red(renk), Color.green(renk), Color.blue(renk))
                else Color.argb(140, Color.red(renk), Color.green(renk), Color.blue(renk))
            canvas.drawRoundRect(x, y, x + barW, chartBottom, 3f, 3f, barPaint)

            if (highlight) {
                val hlPaint = Paint().apply { color = Color.argb(60, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 2f }
                canvas.drawRoundRect(x - 2f, y - 2f, x + barW + 2f, chartBottom + 2f, 5f, 5f, hlPaint)
            }

            // Symbol label
            canvas.drawText(sembol, x + barW / 2f, chartBottom + 16f, labelPaint)

            // Value label
            val displayVal = if (trendTipi == "Elektronegatiflik") "${"%.2f".format(value)}"
                else if (trendTipi == "Iyonlasma Enerjisi") "${value.toInt()}"
                else "${"%.1f".format(value)}"
            valPaint.color = Color.argb(180, 255, 255, 200)
            canvas.drawText(displayVal, x + barW / 2f, y - 6f, valPaint)
        }

        // Max/min labels
        val infoPaint = Paint().apply { color = Color.argb(80, 200, 200, 200); textSize = 9f; textAlign = Paint.Align.LEFT }
        canvas.drawText("En yuksek: ${maxVal.let { if (trendTipi == "Elektronegatiflik") "${"%.2f".format(it)}" else "${"%.1f".format(it)}" }}", 8f, h - 6f, infoPaint)
        val maxEl = KimyaData.elementler[values.maxByOrNull { it.second }?.first ?: ""]
        if (maxEl != null) {
            canvas.drawText("(${maxEl.adi})", 8f + 120f, h - 6f, infoPaint)
        }
        canvas.restore()
    }

    val gosterimListesi: List<String>
        get() = periyotElementleri.keys.toList()
}

class TrendFragment : Fragment() {
    private var trendTipi = "Iyonlasma Enerjisi"
    private var gosterim = "Periyot 2"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_trend, container, false)

        // Canvas
        val canvasPlaceholder = v.findViewById<View>(R.id.trend_canvas)
        val parent = canvasPlaceholder.parent as ViewGroup
        val idx = parent.indexOfChild(canvasPlaceholder)
        parent.removeView(canvasPlaceholder)
        val chartView = TrendChartView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 260)
        }
        parent.addView(chartView, idx)

        val infoText = v.findViewById<TextView>(R.id.trend_info)

        // Trend selector
        val trendRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, dp(4), 0, 0)
        }
        val trendBolum = listOf("Iyonlasma Enerjisi", "Elektronegatiflik", "Atom Yaricapi", "Atom Kutlesi", "Metalik Karakter")
        for (t in trendBolum) {
            val btn = TextView(requireContext()).apply {
                text = t; textSize = 10f; gravity = android.view.Gravity.CENTER
                setPadding(dp(4), dp(3), dp(4), dp(3))
                setTextColor(if (t == trendTipi) Color.WHITE else Color.argb(150, 200, 200, 200))
                setOnClickListener {
                    trendTipi = t; chartView.trendTipi = t; chartView.seciliIndex = -1
                    chartView.invalidate(); infoText.text = trendAciklama(t, gosterim)
                    // Update button colors
                    for (i in 0 until trendRow.childCount) {
                        val c = trendRow.getChildAt(i)
                        if (c is TextView) c.setTextColor(if (c.text == t) Color.WHITE else Color.argb(150, 200, 200, 200))
                    }
                }
                val lp = LinearLayout.LayoutParams(0, dp(30), 1f)
                lp.setMargins(dp(1), 0, dp(1), 0)
                layoutParams = lp
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.argb(30, 57, 255, 20)); cornerRadius = dp(4).toFloat()
                    setStroke(1, Color.argb(40, 255, 255, 255))
                }
                gravity = android.view.Gravity.CENTER
            }
            trendRow.addView(btn)
        }
        parent.addView(trendRow, parent.indexOfChild(chartView) + 1)

        // Gomiterim (period/group) selector - scrollable
        val gosterimRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, dp(3), 0, 0)
        }
        val gosterimler = chartView.gosterimListesi
        for (g in gosterimler) {
            val btn = TextView(requireContext()).apply {
                text = g; textSize = 10f; gravity = android.view.Gravity.CENTER
                setPadding(dp(6), dp(3), dp(6), dp(3))
                setTextColor(if (g == gosterim) Color.WHITE else Color.argb(150, 200, 200, 200))
                setOnClickListener {
                    gosterim = g; chartView.gosterim = g; chartView.seciliIndex = -1
                    chartView.invalidate(); infoText.text = trendAciklama(trendTipi, g)
                    for (i in 0 until gosterimRow.childCount) {
                        val c = gosterimRow.getChildAt(i)
                        if (c is TextView) c.setTextColor(if (c.text == g) Color.WHITE else Color.argb(150, 200, 200, 200))
                    }
                }
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26))
                lp.setMargins(dp(1), 0, dp(1), 0)
                layoutParams = lp
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.argb(20, 0, 200, 255)); cornerRadius = dp(4).toFloat()
                    setStroke(1, Color.argb(30, 255, 255, 255))
                }
                gravity = android.view.Gravity.CENTER
            }
            gosterimRow.addView(btn)
        }

        val gosterimScroll = HorizontalScrollView(requireContext()).apply {
            addView(gosterimRow)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        parent.addView(gosterimScroll, parent.indexOfChild(trendRow) + 1)

        // Chart bar click
        chartView.onElementClick = { s ->
            val el = KimyaData.elementler[s]
            if (el != null) {
                val ie = KimyaData.iyonlasmaEnerjileri[s] ?: 0.0
                val en = KimyaData.elektronegatiflikler[s] ?: 0.0
                infoText.text = """
[${el.semIol}] ${el.adi} (Z=${el.atomNo})
${el.tur} | ${el.durum} | Grup ${el.grup}
Iyonlasma Enerjisi: ${"%.1f".format(ie)} kJ/mol
Elektronegatiflik: ${"%.2f".format(en)}
Atom Kutlesi: ${el.kutle} g/mol
Degerlik: ${el.valans.joinToString(", ")}
                """.trimMargin()
            }
        }

        // Touch on chart to find which bar
        chartView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val elems = chartView.periyotElementleri[chartView.gosterim] ?: return@setOnTouchListener true
                val values = elems.mapNotNull { s ->
                    val v = when (chartView.trendTipi) {
                        "Iyonlasma Enerjisi" -> KimyaData.iyonlasmaEnerjileri[s]
                        "Elektronegatiflik" -> KimyaData.elektronegatiflikler[s]
                        "Atom Kutlesi" -> KimyaData.elementler[s]?.kutle
                        "Atom Yaricapi" -> (chartView.atomYaricapTahmin[s] ?: 0).toDouble()
                        else -> null
                    }
                    if (v != null && v > 0) s to v else null
                }
                if (values.isEmpty()) return@setOnTouchListener true

                val w = chartView.width.toFloat()
                val barCount = values.size
                val barW = min(w * 0.7f / barCount, 50f)
                val gap = if (barCount > 1) (w * 0.85f - barW * barCount) / (barCount - 1) else 0f
                val startX = (w - (barW * barCount + gap * (barCount - 1))) / 2f

                val tx = event.x
                for (i in values.indices) {
                    val bx = startX + i * (barW + gap)
                    if (tx >= bx && tx <= bx + barW) {
                        chartView.seciliIndex = i
                        chartView.invalidate()
                        chartView.onElementClick?.invoke(values[i].first)
                        break
                    }
                }
            }
            true
        }

        infoText.text = trendAciklama(trendTipi, gosterim)
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Periyodik Trend Analizi")
                .setMessage("Periyodik tablodaki elementlerin ozelliklerindeki trendleri inceler.\n\n" +
                    "Bu bolumde:\n" +
                    "- Iyonlasma enerjisi, elektronegatiflik, atom yaricapi\n" +
                    "- Atom kutlesi, metalik karakter trendleri\n" +
                    "- Her trend icin aciklama ve gorsel grafik\n" +
                    "- Elemente dokunarak detayli bilgi alabilirsiniz\n\n" +
                    "Periyot ve grup boyunca degisimleri gozlemleyin.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }

    private fun trendAciklama(trend: String, gosterim: String): String = when (trend) {
        "Iyonlasma Enerjisi" -> "Iyonlasma Enerjisi: Bir atomdan bir elektron koparmak icin gereken minimum enerji.\nPeriyot boyunca SOLDAN SAGA artar (cekirdek yuku artar).\nGrupta YUKARIDAN ASAGIYA azalir (elektron uzaklasir)."
        "Elektronegatiflik" -> "Elektronegatiflik: Bir atomun bag elektronlarini cekme egilimi.\nPeriyot boyunca SOLDAN SAGA artar.\nGrupta YUKARIDAN ASAGIYA azalir.\nEn yuksek: F (3.98). En dusuk: Cs (0.79)."
        "Atom Kutlesi" -> "Atom Kutlesi: Bir atomun ortalama kutlesi (g/mol).\nPeriyot boyunca SOLDAN SAGA artar (proton+notron sayisi artar).\nGrupta YUKARIDAN ASAGIYA artar (yeni enerji seviyesi eklenir)."
        "Atom Yaricapi" -> "Atom Yaricapi: Bir atomun cekirdegi ile en dis elektronu arasindaki mesafe.\nPeriyot boyunca SOLDAN SAGA azalir (cekirdek cekimi artar).\nGrupta YUKARIDAN ASAGIYA artar (yeni kabuk eklenir)."
        "Metalik Karakter" -> "Metalik Karakter: Elementin metal ozelligi gosterme egilimi.\nPeriyot boyunca SOLDAN SAGA azalir.\nGrupta YUKARIDAN ASAGIYA artar.\nEn metalik: Fr. En az metalik: F."
        else -> ""
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
}
