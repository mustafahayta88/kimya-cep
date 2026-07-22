package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.Calculator

class MolTriangleView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    var mVal: Double? = null
    var nVal: Double? = null
    var maVal: Double? = null

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

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

        val bg = Paint().apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, w, h, bg)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val cx = w / 2f
        val topY = h * 0.17f
        val botY = h * 0.80f
        val leftX = w * 0.18f
        val rightX = w * 0.82f

        val triPath = Path().apply {
            moveTo(cx, topY)
            lineTo(leftX, botY)
            lineTo(rightX, botY)
            close()
        }

        val fillPaint = Paint().apply { color = 0x1800F0FF.toInt(); style = Paint.Style.FILL }
        canvas.drawPath(triPath, fillPaint)

        val outlinePaint = Paint().apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f }
        canvas.drawPath(triPath, outlinePaint)

        val midY = (topY + botY) / 2f
        val frac = (midY - topY) / (botY - topY)
        val midLeftX = cx + (leftX - cx) * frac
        val midRightX = cx + (rightX - cx) * frac
        val divPaint = Paint().apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.8f; alpha = 120 }
        canvas.drawLine(midLeftX, midY, midRightX, midY, divPaint)

        val dotPaint = Paint().apply { style = Paint.Style.FILL }
        val labelPaint = Paint().apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        val valPaint = Paint().apply { textAlign = Paint.Align.CENTER }

        val cirRad = dp(20).toFloat()

        dotPaint.color = 0xFF00F0FF.toInt()
        canvas.drawCircle(cx, topY, cirRad, dotPaint)
        labelPaint.color = 0xFF0D1117.toInt(); labelPaint.textSize = 16f
        canvas.drawText("m", cx, topY + 6f, labelPaint)
        valPaint.color = 0xFF00F0FF.toInt(); valPaint.textSize = 12f
        if (mVal != null) canvas.drawText("%.2f g".format(mVal), cx, topY + cirRad + 18f, valPaint)

        dotPaint.color = 0xFF39FF14.toInt()
        canvas.drawCircle(leftX, botY, cirRad, dotPaint)
        labelPaint.color = 0xFF0D1117.toInt(); labelPaint.textSize = 16f
        canvas.drawText("n", leftX, botY + 6f, labelPaint)
        valPaint.color = 0xFF39FF14.toInt(); valPaint.textSize = 12f
        if (nVal != null) canvas.drawText("%.4f mol".format(nVal), leftX, botY + cirRad + 18f, valPaint)

        dotPaint.color = 0xFFB388FF.toInt()
        canvas.drawCircle(rightX, botY, cirRad, dotPaint)
        labelPaint.color = 0xFF0D1117.toInt(); labelPaint.textSize = 14f
        canvas.drawText("Ma", rightX, botY + 6f, labelPaint)
        valPaint.color = 0xFFB388FF.toInt(); valPaint.textSize = 12f
        if (maVal != null) canvas.drawText("%.2f g/mol".format(maVal), rightX, botY + cirRad + 18f, valPaint)

        val opPaint = Paint().apply { textAlign = Paint.Align.CENTER; color = 0xFF8B949E.toInt(); textSize = 11f }
        canvas.drawText("/ Ma", (cx + rightX) / 2f, (topY + botY) / 2f - 10f, opPaint)
        canvas.drawText("x Ma", (leftX + rightX) / 2f, botY + cirRad + 40f, opPaint)
        canvas.drawText("/ n", (cx + leftX) / 2f, (topY + botY) / 2f - 10f, opPaint)
        canvas.restore()
    }
}

class DonusumFragment : Fragment() {

    private var isUpdating = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_donusum, container, false)
        val m = v.findViewById<EditText>(R.id.don_m)
        val n = v.findViewById<EditText>(R.id.don_n)
        val ma = v.findViewById<EditText>(R.id.don_Ma)
        val sonuc = v.findViewById<TextView>(R.id.don_sonuc)

        val placeholder = v.findViewById<View>(R.id.don_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val triangle = MolTriangleView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
        }
        parent.addView(triangle, idx)

        fun updateTriangle() {
            triangle.mVal = m.text.toString().toDoubleOrNull()
            triangle.nVal = n.text.toString().toDoubleOrNull()
            triangle.maVal = ma.text.toString().toDoubleOrNull()
            triangle.invalidate()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true
                val mv = m.text.toString().toDoubleOrNull()
                val nv = n.text.toString().toDoubleOrNull()
                val mav = ma.text.toString().toDoubleOrNull()
                val filled = listOfNotNull(mv?.let { "m" }, nv?.let { "n" }, mav?.let { "Ma" })
                sonuc.text = if (filled.size == 2) {
                    val missing = listOf("m", "n", "Ma").first { it !in filled }
                    try {
                        Calculator.donusumHesapla(mv, nv, mav, missing)
                    } catch (e: Exception) { "Hata: ${e.message}" }
                } else if (filled.size == 3) {
                    "n = ${"%.4f".format(nv!!)} mol\nm = ${"%.2f".format(mv!!)} g\nMa = ${"%.2f".format(mav!!)} g/mol"
                } else {
                    "2 deger girin, bilinmeyen otomatik hesaplanir"
                }
                updateTriangle()
                isUpdating = false
            }
        }
        m.addTextChangedListener(watcher)
        n.addTextChangedListener(watcher)
        ma.addTextChangedListener(watcher)

        fun hesapla() {
            val mv = m.text.toString().toDoubleOrNull()
            val nv = n.text.toString().toDoubleOrNull()
            val mav = ma.text.toString().toDoubleOrNull()
            val filled = listOfNotNull(mv?.let { "m" }, nv?.let { "n" }, mav?.let { "Ma" })
            sonuc.text = if (filled.size < 2) "En az 2 deger girin"
            else if (filled.size == 2) {
                val missing = listOf("m", "n", "Ma").first { it !in filled }
                try {
                    Calculator.donusumHesapla(mv, nv, mav, missing)
                } catch (e: Exception) { "Hata: ${e.message}" }
            } else {
                "n = ${"%.4f".format(nv!!)} mol\nm = ${"%.2f".format(mv!!)} g\nMa = ${"%.2f".format(mav!!)} g/mol"
            }
            updateTriangle()
        }
        v.findViewById<Button>(R.id.don_itn_hesapla).setOnClickListener { hesapla() }

        fun applyPreset(mass: String, molarMass: String) {
            isUpdating = true
            m.setText(mass)
            ma.setText(molarMass)
            n.setText("")
            val mv = mass.toDoubleOrNull()
            val mav = molarMass.toDoubleOrNull()
            if (mv != null && mav != null && mav > 0) {
                val nv = mv / mav
                sonuc.text = "n = ${"%.4f".format(nv)} mol"
            }
            triangle.mVal = mv
            triangle.nVal = if (mv != null && mav != null && mav > 0) mv / mav else null
            triangle.maVal = mav
            triangle.invalidate()
            isUpdating = false
        }

        v.findViewById<Button>(R.id.don_preset_h2o).setOnClickListener { applyPreset("18", "18.02") }
        v.findViewById<Button>(R.id.don_preset_nacl).setOnClickListener { applyPreset("58.44", "58.44") }
        v.findViewById<Button>(R.id.don_preset_h2so4).setOnClickListener { applyPreset("98", "98.08") }

        return v
    }

    private fun dp(n: Int): Int = (n * requireContext().resources.displayMetrics.density).toInt()
}
