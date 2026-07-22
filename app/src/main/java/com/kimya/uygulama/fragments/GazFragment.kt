package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData
import kotlin.random.Random
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class GasContainerView(context: Context) : View(context) {
    var pressure: Double? = null
    var volume: Double? = null
    var temp: Double? = null
    var moles: Double? = null

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B949E.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val pistonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB388FF.toInt()
        style = Paint.Style.FILL
    }
    private val pistonLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF39FF14.toInt()
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt()
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val formulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF39FF14.toInt()
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt()
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB388FF.toInt()
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF3333.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    private val arrowHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF3333.toInt()
        style = Paint.Style.FILL
    }
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val particlePos = Array(10) { Pair(Random.nextFloat(), Random.nextFloat()) }
    private val particleVel = Array(10) { Pair((Random.nextFloat() - 0.5f) * 0.008f, (Random.nextFloat() - 0.5f) * 0.008f) }
    private val particleColors = intArrayOf(
        0xFF00F0FF.toInt(), 0xFF39FF14.toInt(), 0xFFB388FF.toInt(), 0xFFFF3333.toInt(),
        0xFFFFA500.toInt(), 0xFFFFFF00.toInt(), 0xFF00F0FF.toInt(), 0xFF39FF14.toInt(),
        0xFFB388FF.toInt(), 0xFFFFA500.toInt()
    )

    private var animator: ValueAnimator? = null

    init {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                for (i in 0 until 10) {
                    var (x, y) = particlePos[i]
                    var (vx, vy) = particleVel[i]
                    x += vx
                    y += vy
                    if (x < 0.05f || x > 0.95f) { vx = -vx; x = x.coerceIn(0.05f, 0.95f) }
                    if (y < 0.05f || y > 0.95f) { vy = -vy; y = y.coerceIn(0.05f, 0.95f) }
                    particlePos[i] = x to y
                    particleVel[i] = vx to vy
                }
                invalidate()
            }
        }
    }

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator?.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
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
        val w = width.toFloat()
        val h = height.toFloat()

        val vol = volume ?: 22.4
        val pistonRatio = (vol / 50.0).coerceIn(0.25, 0.9)
        val cTop = h * 0.22f
        val cBot = h * 0.78f
        val cHeight = cBot - cTop
        val pistonY = cTop + cHeight * (1f - pistonRatio.toFloat())

        val left = w * 0.10f
        val right = w * 0.90f

        // Gradient fill for depth
        val grad = LinearGradient(left, cTop, left + (right - left) * 0.5f, cBot,
            0x22161B22.toInt(), 0x080D1117.toInt(), Shader.TileMode.CLAMP)
        gradientPaint.shader = grad
        canvas.drawRect(left, cTop, right, cBot, gradientPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        canvas.drawRect(left, cTop, right, cBot, wallPaint)
        canvas.drawLine(left, cBot, right, cBot, wallPaint)

        // Piston with line detail
        canvas.drawRect(left, pistonY, right, pistonY + 14f, pistonPaint)
        canvas.drawLine(left, pistonY + 7f, right, pistonY + 7f, pistonLinePaint)

        // Particles with animation
        for (i in 0 until 10) {
            val px = left + 12f + particlePos[i].first * (right - left - 24f)
            val py = pistonY + 12f + particlePos[i].second * (cBot - pistonY - 24f)
            if (py > pistonY + 4f && py < cBot - 4f) {
                particlePaint.color = particleColors[i]
                canvas.drawCircle(px, py, 6f, particlePaint)
            }
        }

        // Right-side pressure arrow
        val arrowX = w * 0.955f
        val arrowTop = pistonY + 20f
        val arrowBot = cBot - 10f
        if (arrowBot > arrowTop) {
            canvas.drawLine(arrowX, arrowTop, arrowX, arrowBot, arrowPaint)
            val ap = Path()
            ap.moveTo(arrowX, arrowTop)
            ap.lineTo(arrowX - 8f, arrowTop + 14f)
            ap.lineTo(arrowX + 8f, arrowTop + 14f)
            ap.close()
            canvas.drawPath(ap, arrowHeadPaint)
            canvas.drawText("P", arrowX, (arrowTop + arrowBot) / 2f + labelPaint.textSize / 3f, labelPaint)
        }

        // Formula at top
        canvas.drawText("PV = nRT", w / 2f, h * 0.16f, formulaPaint)

        // Values below container
        val pStr = if (pressure != null) "P = ${"%.2f".format(pressure)} atm" else "P = ?"
        val vStr = if (volume != null) "V = ${"%.1f".format(volume)} L" else "V = ?"
        val nStr = if (moles != null) "n = ${"%.3f".format(moles)} mol" else "n = ?"
        val tStr = if (temp != null) "T = ${"%.1f".format(temp)} K" else "T = ?"
        val colW = (right - left) / 4f
        canvas.drawText(pStr, left + colW * 0.5f, cBot + 36f, valuePaint)
        canvas.drawText(vStr, left + colW * 1.5f, cBot + 36f, valuePaint)
        canvas.drawText(nStr, left + colW * 2.5f, cBot + 36f, valuePaint)
        canvas.drawText(tStr, left + colW * 3.5f, cBot + 36f, valuePaint)
        canvas.restore()
    }
}

class GazFragment : Fragment() {
    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_gaz, container, false)
        val P = v.findViewById<EditText>(R.id.gaz_P)
        val V = v.findViewById<EditText>(R.id.gaz_V)
        val n = v.findViewById<EditText>(R.id.gaz_n)
        val T = v.findViewById<EditText>(R.id.gaz_T)
        val sonuc = v.findViewById<TextView>(R.id.gaz_sonuc)
        val mIKutle = v.findViewById<EditText>(R.id.gaz_mI_kutle)
        val mIV = v.findViewById<EditText>(R.id.gaz_mI_V)
        val mIT = v.findViewById<EditText>(R.id.gaz_mI_T)
        val mIP = v.findViewById<EditText>(R.id.gaz_mI_P)
        val mISonuc = v.findViewById<TextView>(R.id.gaz_mI_sonuc)

        val gasPlaceholder = v.findViewById<View>(R.id.gaz_canvas_placeholder)
        val gasParent = gasPlaceholder.parent as ViewGroup
        val gasIdx = gasParent.indexOfChild(gasPlaceholder)
        gasParent.removeView(gasPlaceholder)
        val gasView = GasContainerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
        }
        gasParent.addView(gasView, gasIdx)

        v.findViewById<Button>(R.id.gaz_hesapla).setOnClickListener {
            try {
                val p = P.text.toString().toDoubleOrNull()
                val vv = V.text.toString().toDoubleOrNull()
                val nn = n.text.toString().toDoubleOrNull()
                val tt = T.text.toString().toDoubleOrNull()
                val eksik = when {
                    p == null -> "Basinc (P)"
                    vv == null -> "Hacim (V)"
                    nn == null -> "Mol (n)"
                    tt == null -> "Sicaklik (T)"
                    else -> { sonuc.text = "Bir alani bos birakin"; return@setOnClickListener }
                }
                val r = KimyaData.idealGaz(p, vv, nn, tt)
                if (r == null) sonuc.text = "Gecersiz deger"
                else {
                    sonuc.text = "$eksik = ${"%.6f".format(r)}"
                    gasView.pressure = p ?: r
                    gasView.volume = vv ?: r
                    gasView.moles = nn ?: r
                    gasView.temp = tt ?: r
                    gasView.invalidate()
                }
            } catch (e: Exception) { sonuc.text = "Hata: ${e.message}" }
        }

        v.findViewById<Button>(R.id.gaz_stp).setOnClickListener {
            P.setText("1"); V.setText("22.4"); n.setText("1"); T.setText("273.15")
            gasView.pressure = 1.0; gasView.volume = 22.4; gasView.moles = 1.0; gasView.temp = 273.15
            gasView.invalidate()
        }
        v.findViewById<Button>(R.id.gaz_oda).setOnClickListener {
            P.setText("1"); V.setText("24.5"); n.setText("1"); T.setText("298")
            gasView.pressure = 1.0; gasView.volume = 24.5; gasView.moles = 1.0; gasView.temp = 298.0
            gasView.invalidate()
        }
        v.findViewById<Button>(R.id.gaz_preset1).setOnClickListener {
            P.setText("1"); V.setText("49.2"); n.setText("2"); T.setText("300")
            gasView.pressure = 1.0; gasView.volume = 49.2; gasView.moles = 2.0; gasView.temp = 300.0
            gasView.invalidate()
        }
        v.findViewById<Button>(R.id.gaz_preset2).setOnClickListener {
            P.setText("1"); V.setText("11.2"); n.setText("0.5"); T.setText("273.15")
            gasView.pressure = 1.0; gasView.volume = 11.2; gasView.moles = 0.5; gasView.temp = 273.15
            gasView.invalidate()
        }

        v.findViewById<Button>(R.id.gaz_mI_itn).setOnClickListener {
            try {
                val k = mIKutle.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val vv = mIV.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val tt = mIT.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val pp = mIP.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val mK = KimyaData.gazMolKutlesi(k, vv, tt, pp)
                if (mK == null) mISonuc.text = "Gecersiz deger"
                else mISonuc.text = "Mol Kutlesi = ${"%.4f".format(mK)} g/mol"
            } catch (e: Exception) { mISonuc.text = "Hata: ${e.message}" }
        }
        return v
    }
}
