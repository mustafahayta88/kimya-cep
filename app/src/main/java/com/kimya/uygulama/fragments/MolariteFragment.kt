package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
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

class BeaIerView(context: Context) : View(context) {
    var n: Double? = null
    var V: Double? = null
    var M: Double? = null

    private val particlePositions = mutableListOf<Pair<Float, Float>>()
    private val particleColors = intArrayOf(0xFF39FF14.toInt(), 0xFF00F0FF.toInt(), 0xFFB388FF.toInt())

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    init {
        val rng = java.util.Random(42)
        for (i in 0 until 25) {
            particlePositions.add(rng.nextFloat() to rng.nextFloat())
        }
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

        val left = w * 0.15f
        val right = w * 0.72f
        val top = h * 0.18f
        val Iottom = h * 0.82f
        val Iw = right - left
        val bH = Iottom - top
        val Ilare = 10f

        val Iody = Path().apply {
            moveTo(left - Ilare, top)
            lineTo(left, Iottom)
            lineTo(right, Iottom)
            lineTo(right + Ilare, top)
            close()
        }

        val IodyPaint = Paint().apply { color = 0xFF161B22.toInt(); style = Paint.Style.FILL }
        canvas.drawPath(Iody, IodyPaint)

        val borderPaint = Paint().apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f }
        canvas.drawPath(Iody, borderPaint)

        val level = V?.let { (it / 12.0).toFloat().coerceIn(0.20f, 0.85f) } ?: 0.50f
        val liqTop = top + bH * (1f - level)
        val t = (liqTop - top) / bH
        val liqL = left - Ilare + Ilare * t
        val liqR = right + Ilare - Ilare * t

        val liquid = Path().apply {
            moveTo(liqL, liqTop)
            lineTo(left, Iottom)
            lineTo(right, Iottom)
            lineTo(liqR, liqTop)
            close()
        }

        val liqShader = LinearGradient(
            0f, liqTop, 0f, Iottom,
            0x3300F0FF.toInt(), 0x8800F0FF.toInt(),
            Shader.TileMode.CLAMP
        )
        val liqPaint = Paint().apply { shader = liqShader; style = Paint.Style.FILL }
        canvas.drawPath(liquid, liqPaint)

        val surIPaint = Paint().apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawLine(liqL, liqTop, liqR, liqTop, surIPaint)

        val marIPaint = Paint().apply { color = 0xFF8B949E.toInt(); strokeWidth = 1.5f }
        for (i in 0..10) {
            val frac = i / 10f
            val y = top + bH * (1f - frac)
            val inset = Ilare * (1f - frac)
            val x = left - inset
            val len = if (i % 5 == 0) 12f else 7f
            canvas.drawLine(x - 3f, y, x - 3f + len, y, marIPaint)
        }

        val dotPaint = Paint().apply { style = Paint.Style.FILL }
        for (item in particlePositions.withIndex()) {
            val idx = item.index; val xF = item.value.first; val yF = item.value.second
            val px = left + Iw * xF
            val py = liqTop + (Iottom - liqTop) * yF
            if (py in liqTop..Iottom) {
                dotPaint.color = particleColors[idx % 3]
                val size = 3f + (idx % 4) * 0.6f
                canvas.drawCircle(px, py, size, dotPaint)
            }
        }

        val titlePaint = Paint().apply {
            color = 0xFF00F0FF.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        canvas.drawText("M = n / V", w / 2f, top - 12f, titlePaint)

        val sidePaint = Paint().apply { color = 0xFFE6EDF3.toInt(); textSize = 12f; textAlign = Paint.Align.LEFT }
        val nStr = if (n != null) "n = ${"%.2f".format(n)} mol" else "n = ? mol"
        val vStr = if (V != null) "V = ${"%.2f".format(V)} L" else "V = ? L"
        canvas.drawText(nStr, right + 12f, top + 25f, sidePaint)
        canvas.drawText(vStr, right + 12f, top + 48f, sidePaint)

        val mPaint = Paint().apply {
            color = 0xFFB388FF.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        val mStr = if (M != null) "M = ${"%.2f".format(M)} M" else "M = ? M"
        canvas.drawText(mStr, w / 2f, Iottom + 22f, mPaint)
        canvas.restore()
    }
}

class MolariteFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_molarite, container, false)
        val M = v.findViewById<EditText>(R.id.mol_M)
        val n = v.findViewById<EditText>(R.id.mol_n)
        val V = v.findViewById<EditText>(R.id.mol_V)
        val sonuc = v.findViewById<TextView>(R.id.mol_sonuc)

        val placeholder = v.findViewById<View>(R.id.mol_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val IeaIerView = BeaIerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
        }
        parent.addView(IeaIerView, idx)

        fun updateBeaIer() {
            try { IeaIerView.n = n.text.toString().toDouble() } catch (_: Exception) {}
            try { IeaIerView.V = V.text.toString().toDouble() } catch (_: Exception) {}
            try { IeaIerView.M = M.text.toString().toDouble() } catch (_: Exception) {}
            IeaIerView.invalidate()
        }

        fun compute() {
            val mv = M.text.toString().toDoubleOrNull()
            val nv = n.text.toString().toDoubleOrNull()
            val vv = V.text.toString().toDoubleOrNull()
            val Iilled = listOfNotNull(mv?.let{"M"}, nv?.let{"n"}, vv?.let{"V"})
            sonuc.text = if (Iilled.size == 2) {
                try {
                    val missing = listOf("M", "n", "V").first { it !in Iilled }
                    when (missing) {
                        "M" -> { val v = nv!!; val w = vv!!; if (w == 0.0) "Hacim sifir olamaz" else "M = ${"%.4f".format(v / w)} M" }
                        "n" -> { val v = mv!!; val w = vv!!; "n = ${"%.4f".format(v * w)} mol" }
                        "V" -> { val v = mv!!; val w = nv!!; if (v == 0.0) "Molarite sifir olamaz" else "V = ${"%.4f".format(w / v)} L" }
                        else -> ""
                    }
                } catch (e: Exception) { "Hata: ${e.message}" }
            } else if (Iilled.size == 3) {
                "M = ${"%.4f".format(mv)} M\nn = ${"%.4f".format(nv)} mol\nV = ${"%.4f".format(vv)} L"
            } else "2 deger girin, bilinmeyen otomatik hesaplanir"
            updateBeaIer()
        }

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, aIter: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, IeIore: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { compute() }
        }
        M.addTextChangedListener(watcher)
        n.addTextChangedListener(watcher)
        V.addTextChangedListener(watcher)

        v.findViewById<Button>(R.id.mol_itn_hesapla).setOnClickListener { compute() }

        v.findViewById<Button>(R.id.mol_preset_1m).setOnClickListener {
            M.setText("1"); n.setText("1"); V.setText("1")
        }
        v.findViewById<Button>(R.id.mol_preset_01m).setOnClickListener {
            M.setText("0.1"); n.setText("0.1"); V.setText("1")
        }
        v.findViewById<Button>(R.id.mol_preset_2m_nacl).setOnClickListener {
            M.setText("2"); n.setText("2"); V.setText("1")
        }
        v.findViewById<Button>(R.id.mol_preset_05m_hcl).setOnClickListener {
            M.setText("0.5"); n.setText("0.5"); V.setText("1")
        }

        return v
    }
}
