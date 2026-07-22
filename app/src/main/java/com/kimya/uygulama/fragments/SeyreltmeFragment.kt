package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.Calculator

class DilutionView(context: Context) : View(context) {
    var c1: Double? = null
    var v1: Double? = null
    var c2: Double? = null
    var v2: Double? = null
    var animFrac1: Float = 0.40f
    var animFrac2: Float = 0.65f

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

        val bg = Paint().apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, w, h, bg)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val bLeft1 = w * 0.04f; val bRight1 = w * 0.40f
        val bLeft2 = w * 0.60f; val bRight2 = w * 0.96f
        val bTop = h * 0.25f; val bBottom = h * 0.82f
        val bH = bBottom - bTop

        fun drawBeaker(l: Float, r: Float, fillFrac: Float, clabel: String, vlabel: String) {
            val rectPath = Path().apply {
                moveTo(l - 4f, bTop)
                lineTo(l, bBottom)
                lineTo(r, bBottom)
                lineTo(r + 4f, bTop)
                close()
            }
            val fillPaint = Paint().apply { color = 0xFF161B22.toInt(); style = Paint.Style.FILL }
            canvas.drawPath(rectPath, fillPaint)

            val borderPaint = Paint().apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
            canvas.drawPath(rectPath, borderPaint)

            val fillTop = bTop + bH * (1f - fillFrac)
            val It = (fillTop - bTop) / bH
            val Il = l - 4f + 4f * It
            val Ir = r + 4f - 4f * It
            val fillPath = Path().apply {
                moveTo(Il, fillTop)
                lineTo(l, bBottom)
                lineTo(r, bBottom)
                lineTo(Ir, fillTop)
                close()
            }
            val liqPaint = Paint().apply { color = 0x6600F0FF.toInt(); style = Paint.Style.FILL }
            canvas.drawPath(fillPath, liqPaint)

            val laIelPaint = Paint().apply { color = 0xFFE6EDF3.toInt(); textSize = 11f; textAlign = Paint.Align.CENTER }
            canvas.drawText(clabel, (l + r) / 2f, bTop - 8f, laIelPaint)
            canvas.drawText(vlabel, (l + r) / 2f, bBottom + 18f, laIelPaint)
        }

        val c1Str = if (c1 != null) "C1 = ${"%.2f".format(c1)}" else "C1"
        val v1Str = if (v1 != null) "V1 = ${"%.0f".format(v1)} mL" else "V1 (mL)"
        val c2Str = if (c2 != null) "C2 = ${"%.2f".format(c2)}" else "C2"
        val v2Str = if (v2 != null) "V2 = ${"%.0f".format(v2)} mL" else "V2 (mL)"

        drawBeaker(bLeft1, bRight1, animFrac1, c1Str, v1Str)
        drawBeaker(bLeft2, bRight2, animFrac2, c2Str, v2Str)

        val arrowPaint = Paint().apply { color = 0xFF00F0FF.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
        val ay = bTop + bH * 0.45f
        val ax1 = bRight1 + 6f; val ax2 = bLeft2 - 6f
        canvas.drawLine(ax1, ay, ax2, ay, arrowPaint)
        val aPath = Path().apply {
            moveTo(ax2, ay)
            lineTo(ax2 - 10f, ay - 5f)
            moveTo(ax2, ay)
            lineTo(ax2 - 10f, ay + 5f)
        }
        canvas.drawPath(aPath, arrowPaint)

        val formulaPaint = Paint().apply {
            color = 0xFFB388FF.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        canvas.drawText("C1 . V1 = C2 . V2", w / 2f, h * 0.13f, formulaPaint)
        canvas.restore()
    }
}

class SeyreltmeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_seyreltme, container, false)
        val C1 = v.findViewById<EditText>(R.id.sey_C1)
        val V1 = v.findViewById<EditText>(R.id.sey_V1)
        val C2 = v.findViewById<EditText>(R.id.sey_C2)
        val V2 = v.findViewById<EditText>(R.id.sey_V2)
        val sonuc = v.findViewById<TextView>(R.id.sey_sonuc)

        val placeholder = v.findViewById<View>(R.id.sey_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val dilutionView = DilutionView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 180)
        }
        parent.addView(dilutionView, idx)

        fun getTargetFrac(value: Double?): Float {
            return value?.let { (it / 2000.0).toFloat().coerceIn(0.15f, 0.85f) } ?: 0.40f
        }

        fun animateDilution() {
            val target1 = getTargetFrac(dilutionView.v1)
            val target2 = getTargetFrac(dilutionView.v2)
            val start1 = dilutionView.animFrac1
            val start2 = dilutionView.animFrac2
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 400
                interpolator = DecelerateInterpolator()
                addUpdateListener { anim ->
                    val t = anim.animatedFraction
                    dilutionView.animFrac1 = start1 + (target1 - start1) * t
                    dilutionView.animFrac2 = start2 + (target2 - start2) * t
                    dilutionView.invalidate()
                }
                start()
            }
        }

        fun updateDilution() {
            try { dilutionView.c1 = C1.text.toString().toDouble() } catch (_: Exception) {}
            try { dilutionView.v1 = V1.text.toString().toDouble() } catch (_: Exception) {}
            try { dilutionView.c2 = C2.text.toString().toDouble() } catch (_: Exception) {}
            try { dilutionView.v2 = V2.text.toString().toDouble() } catch (_: Exception) {}
            animateDilution()
        }

        fun autoHesapla() {
            val c1v = C1.text.toString().toDoubleOrNull()
            val v1v = V1.text.toString().toDoubleOrNull()
            val c2v = C2.text.toString().toDoubleOrNull()
            val v2v = V2.text.toString().toDoubleOrNull()
            val dolu = listOfNotNull(c1v?.let{"C1"}, v1v?.let{"V1"}, c2v?.let{"C2"}, v2v?.let{"V2"})
            if (dolu.size < 3) { sonuc.text = "3 deger girin, bilinmeyen otomatik hesaplanir"; return }
            val bos = listOf("C1", "V1", "C2", "V2").firstOrNull { it !in dolu }
            if (bos == null) { sonuc.text = "Tum degerler girildi"; return }
            try {
                sonuc.text = Calculator.seyreltmeHesapla(c1v ?: 0.0, v1v ?: 0.0, c2v ?: 0.0, v2v ?: 0.0, bos)
                updateDilution()
            } catch (e: Exception) { sonuc.text = "Hata: ${e.message}" }
        }

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { autoHesapla() }
        }
        C1.addTextChangedListener(watcher)
        V1.addTextChangedListener(watcher)
        C2.addTextChangedListener(watcher)
        V2.addTextChangedListener(watcher)

        v.findViewById<Button>(R.id.sey_preset_2x).setOnClickListener {
            C1.setText("2"); V1.setText("100"); C2.setText("1"); V2.setText("200")
        }
        v.findViewById<Button>(R.id.sey_preset_10x).setOnClickListener {
            C1.setText("10"); V1.setText("50"); C2.setText("1"); V2.setText("500")
        }
        v.findViewById<Button>(R.id.sey_preset_1m_01m).setOnClickListener {
            C1.setText("1"); V1.setText("100"); C2.setText("0.1"); V2.setText("1000")
        }

        return v
    }
}
