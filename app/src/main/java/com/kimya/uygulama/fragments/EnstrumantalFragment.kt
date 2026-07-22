package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R

class KalibrasyonView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    var noktalar: List<Pair<Double, Double>> = emptyList()
    var regLine: Pair<Double, Double>? = null
    var tappedPoint: Pair<Float, Float>? = null
    var onPointTapped: ((Double, Double) -> Unit)? = null

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
        if (w <= 0 || h <= 0 || noktalar.isEmpty()) {
            val p = Paint().apply { color = Color.argb(100,139,148,158); textSize = 14f; textAlign = Paint.Align.CENTER }
            canvas.drawText("Standart ekleyerek veri girin", w/2, h/2, p); return
        }
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        val maxK = (noktalar.maxOfOrNull { it.first } ?: 1.0) * 1.1
        val maxA = (noktalar.maxOfOrNull { it.second } ?: 1.0) * 1.1
        val left = 56f; val top = 50f; val right = w - 20f; val bottom = h - 46f
        val gw = right - left; val gh = bottom - top

        val axisPaint = Paint().apply { color = Color.argb(180,230,237,243); strokeWidth = 2f; textSize = 11f; textAlign = Paint.Align.CENTER }

        // Grid lines
        val gridPaint = Paint().apply {
            color = Color.argb(30, 230, 237, 243); strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        for (i in 0..8) {
            val frac = i / 8.0
            val gx = left + (frac * gw).toFloat()
            canvas.drawLine(gx, top, gx, bottom, gridPaint)
            val gy = bottom - (frac * gh).toFloat()
            canvas.drawLine(left, gy, right, gy, gridPaint)
        }

        canvas.drawLine(left, bottom, right, bottom, axisPaint)
        canvas.drawLine(left, bottom, left, top, axisPaint)

        val tickPaint = Paint().apply { color = Color.argb(140,230,237,243); textSize = 9f; textAlign = Paint.Align.CENTER; strokeWidth = 1.5f }
        for (i in 0..8) {
            val frac = i / 8.0
            val x = left + (frac * gw).toFloat()
            val kVal = frac * maxK
            canvas.drawLine(x, bottom + 4f, x, bottom - 4f, tickPaint)
            tickPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("${"%.2f".format(kVal)}", x, bottom + 16f, tickPaint)
        }
        for (i in 0..8) {
            val frac = i / 8.0
            val y = bottom - (frac * gh).toFloat()
            val aVal = frac * maxA
            canvas.drawLine(left - 4f, y, left + 4f, y, tickPaint)
            tickPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${"%.3f".format(aVal)}", left - 8f, y + 3f, tickPaint)
        }

        val labelPaint = Paint().apply { color = Color.argb(200, 230, 237, 243); textSize = 10f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("Konsantrasyon (ppm)", w / 2f, bottom + 34f, labelPaint)
        canvas.save(); canvas.rotate(-90f, 14f, (top + bottom) / 2f)
        canvas.drawText("Absorbans", 14f, (top + bottom) / 2f + 5f, labelPaint)
        canvas.restore()

        val dotColors = intArrayOf(0xFF00F0FF.toInt(), 0xFF44FFBB.toInt(), 0xFF88FF66.toInt(), 0xFFBBFF22.toInt(), 0xFF39FF14.toInt())
        var ci = 0
        for (n in noktalar) {
            val x = left + (n.first / maxK).toFloat() * gw
            val y = bottom - (n.second / maxA).toFloat() * gh
            val dotPaint = Paint().apply { isAntiAlias = true; color = dotColors[ci % dotColors.size]; style = Paint.Style.FILL }
            // Glow around point
            val glowP = Paint().apply { isAntiAlias = true; color = Color.argb(40, 0, 240, 255); style = Paint.Style.FILL }
            canvas.drawCircle(x, y, 9f, glowP)
            canvas.drawCircle(x, y, 6f, dotPaint)
            // White center
            val centerPaint = Paint().apply { isAntiAlias = true; color = Color.argb(180, 255, 255, 255); style = Paint.Style.FILL }
            canvas.drawCircle(x, y, 2.5f, centerPaint)
            ci++
        }

        if (regLine != null) {
            val (slope, intercept) = regLine!!
            val x1 = 0.0; val y1 = slope * x1 + intercept
            val x2 = maxK; val y2 = slope * x2 + intercept
            val rx1 = left; val ry1 = bottom - (y1 / maxA).toFloat() * gh
            val rx2 = left + (x2 / maxK).toFloat() * gw; val ry2 = bottom - (y2 / maxA).toFloat() * gh

            // Glow for regression line
            val glowLine = Paint().apply { color = Color.argb(60, 0, 240, 255); strokeWidth = 8f; isAntiAlias = true }
            canvas.drawLine(rx1, ry1.coerceIn(top, bottom), rx2, ry2.coerceIn(top, bottom), glowLine)

            val regPaint = Paint().apply { color = 0xFF00F0FF.toInt(); strokeWidth = 3f; isAntiAlias = true; isDither = true }
            canvas.drawLine(rx1, ry1.coerceIn(top, bottom), rx2, ry2.coerceIn(top, bottom), regPaint)

            val r2 = rKare()
            val topTextPaint = Paint().apply { color = 0xFF00F0FF.toInt(); textSize = 12f; textAlign = Paint.Align.LEFT; isFakeBoldText = true }
            canvas.drawText("y = ${"%.4f".format(slope)}x + ${"%.4f".format(intercept)}", left + 4f, 20f, topTextPaint)
            topTextPaint.color = 0xFF39FF14.toInt()
            canvas.drawText("R² = ${"%.4f".format(r2)}", left + 4f, 38f, topTextPaint)
        }

        if (tappedPoint != null) {
            val (tx, ty) = tappedPoint!!
            val crossPaint = Paint().apply { color = 0xB3FFEE00.toInt(); strokeWidth = 1.5f }
            canvas.drawLine(tx, top, tx, bottom, crossPaint)
            canvas.drawLine(left, ty, right, ty, crossPaint)
            val xData = ((tx - left) / gw * maxK)
            val yData = ((bottom - ty) / gh * maxA)
            val textPaint = Paint().apply { color = 0xFFFFEE00.toInt(); textSize = 11f; textAlign = Paint.Align.LEFT }
            val label = "${"%.2f".format(xData)} ppm, ${"%.3f".format(yData)} Abs"
            canvas.drawText(label, (tx + 10f).coerceAtMost(right - 100f), (ty - 10f).coerceAtLeast(top + 14f), textPaint)
        }
        canvas.restore()
    }

    private fun rKare(): Double {
        if (noktalar.size < 2) return 0.0
        val ym = noktalar.map { it.second }.average()
        val ssRes = noktalar.sumOf { (it.second - (regLine!!.first * it.first + regLine!!.second)).pow(2) }
        val ssTot = noktalar.sumOf { (it.second - ym).pow(2) }
        return if (ssTot == 0.0) 0.0 else 1.0 - ssRes / ssTot
    }

    private fun Double.pow(e: Int): Double = Math.pow(this, e.toDouble())
}

class EnstrumantalFragment : Fragment() {
    private val blankList = mutableListOf<Double>()
    private val stdNoktalar = mutableListOf<Pair<Double, Double>>()
    private var regLine: Pair<Double, Double>? = null
    private lateinit var chartView: KalibrasyonView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_enstrumantal, container, false)
        val blankDurum = v.findViewById<TextView>(R.id.ins_blank_durum)
        val blankButon = v.findViewById<Button>(R.id.ins_blank_btn)
        val blankInput = v.findViewById<EditText>(R.id.ins_blank)
        val stdKons = v.findViewById<EditText>(R.id.ins_std_kons)
        val stdAbs = v.findViewById<EditText>(R.id.ins_std_abs)
        val stdButon = v.findViewById<Button>(R.id.ins_std_btn)
        val stdList = v.findViewById<TextView>(R.id.ins_std_list)
        val temizle = v.findViewById<Button>(R.id.ins_temizle)
        val konsInput = v.findViewById<EditText>(R.id.ins_kons)
        val konsBtn = v.findViewById<Button>(R.id.ins_kons_btn)
        val absInput = v.findViewById<EditText>(R.id.ins_abs)
        val absBtn = v.findViewById<Button>(R.id.ins_abs_btn)
        val sonuc = v.findViewById<TextView>(R.id.ins_sonuc)

        val chartContainer = v.findViewById<FrameLayout>(R.id.chart_container)
        chartView = KalibrasyonView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        chartContainer.addView(chartView)

        val insCoord = v.findViewById<TextView>(R.id.ins_coord)
        chartView.onPointTapped = { xData, yData ->
            insCoord.text = if (xData < 0) "Secim temizlendi" else "${"%.2f".format(xData)} ppm, ${"%.3f".format(yData)} Abs"
        }

        fun updateRegression() {
            if (stdNoktalar.size < 2) { regLine = null; chartView.regLine = null; chartView.invalidate(); return }
            val n = stdNoktalar.size
            val sx = stdNoktalar.sumOf { it.first }; val sy = stdNoktalar.sumOf { it.second }
            val sxx = stdNoktalar.sumOf { it.first * it.first }; val sxy = stdNoktalar.sumOf { it.first * it.second }
            val slope = if (n * sxx - sx * sx != 0.0) (n * sxy - sx * sy) / (n * sxx - sx * sx) else 0.0
            val intercept = (sy - slope * sx) / n
            regLine = slope to intercept
            chartView.noktalar = stdNoktalar.toList()
            chartView.regLine = regLine
            chartView.invalidate()
        }

        fun renderStdList() {
            val sb = StringBuilder()
            stdNoktalar.forEachIndexed { i, (k, a) -> sb.append("${i+1}: $k ppm -> $a Abs\n") }
            stdList.text = if (sb.isEmpty()) "Henuz standart eklenmedi" else sb.toString()
        }

        fun addStd(k: Double, a: Double) {
            stdNoktalar.add(k to a)
            renderStdList()
            updateRegression()
        }

        blankButon.setOnClickListener {
            val b = blankInput.text.toString().toDoubleOrNull()
            if (b == null) { blankDurum.text = "Gecersiz deger"; return@setOnClickListener }
            blankList.add(b)
            blankDurum.text = "Blank: ort=${"%.4f".format(blankList.average())} n=${blankList.size}"
            blankInput.setText("")
        }

        stdButon.setOnClickListener {
            val k = stdKons.text.toString().toDoubleOrNull()
            val a = stdAbs.text.toString().toDoubleOrNull()
            if (k == null || a == null) { Toast.makeText(context, "Gecersiz", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            addStd(k, a)
            stdKons.setText(""); stdAbs.setText("")
        }

        v.findViewById<Button>(R.id.ins_preset_1).setOnClickListener {
            stdKons.setText("1"); stdAbs.setText("0.1")
            addStd(1.0, 0.1)
            stdKons.setText(""); stdAbs.setText("")
        }
        v.findViewById<Button>(R.id.ins_preset_2).setOnClickListener {
            stdKons.setText("2"); stdAbs.setText("0.2")
            addStd(2.0, 0.2)
            stdKons.setText(""); stdAbs.setText("")
        }
        v.findViewById<Button>(R.id.ins_preset_5).setOnClickListener {
            stdKons.setText("5"); stdAbs.setText("0.5")
            addStd(5.0, 0.5)
            stdKons.setText(""); stdAbs.setText("")
        }
        v.findViewById<Button>(R.id.ins_preset_all).setOnClickListener {
            stdNoktalar.clear()
            val demo = listOf(1.0 to 0.1, 2.0 to 0.2, 3.0 to 0.3, 4.0 to 0.4, 5.0 to 0.5)
            demo.forEach { (k, a) -> stdNoktalar.add(k to a) }
            renderStdList()
            updateRegression()
            sonuc.text = "Demo standartlar eklendi (1-5 ppm)"
        }

        temizle.setOnClickListener {
            stdNoktalar.clear(); renderStdList(); regLine = null
            chartView.noktalar = emptyList(); chartView.regLine = null; chartView.tappedPoint = null; chartView.invalidate()
            sonuc.text = "Standartlar temizlendi."
        }

        konsBtn.setOnClickListener {
            val k = konsInput.text.toString().toDoubleOrNull()
            if (k == null || regLine == null) { sonuc.text = "Once kalibrasyon yapin"; return@setOnClickListener }
            val (slope, intercept) = regLine!!
            sonuc.text = "Abs = ${"%.4f".format(slope * k + intercept)}"
        }

        absBtn.setOnClickListener {
            val a = absInput.text.toString().toDoubleOrNull()
            if (a == null || regLine == null) { sonuc.text = "Once kalibrasyon yapin"; return@setOnClickListener }
            val (slope, intercept) = regLine!!
            if (slope == 0.0) { sonuc.text = "Egim sifir, hesaplanamaz"; return@setOnClickListener }
            sonuc.text = "Kons = ${"%.4f".format((a - intercept) / slope)} ppm"
        }

        return v
    }
}
