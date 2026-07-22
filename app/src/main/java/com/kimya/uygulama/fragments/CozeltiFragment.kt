package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class SolutionView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var fillPercent = 0f
    private var volumeLabel = ""

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    private fun dp(n: Int): Int = (n * context.resources.displayMetrics.density).toInt()

    fun setLiquidLevel(percent: Float, label: String) {
        fillPercent = percent.coerceIn(0f, 1f)
        volumeLabel = label
        invalidate()
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
        val cx = w / 2f

        val beakerW = w * 0.55f
        val beakerH = h * 0.65f
        val left = cx - beakerW / 2f
        val right = cx + beakerW / 2f
        val top = h * 0.1f
        val bottom = top + beakerH

        paint.style = Paint.Style.FILL
        paint.color = 0x0D00F0FF.toInt()
        canvas.drawRoundRect(left, top, right, bottom, dp(6).toFloat(), dp(6).toFloat(), paint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(3).toFloat()
        paint.color = 0xFF00F0FF.toInt()
        canvas.drawRoundRect(left, top, right, bottom, dp(6).toFloat(), dp(6).toFloat(), paint)

        val lipExt = dp(8).toFloat()
        val lipH = dp(4).toFloat()
        paint.strokeWidth = dp(3).toFloat()
        canvas.drawLine(left - lipExt, top, left - lipExt, top + lipH, paint)
        canvas.drawLine(left - lipExt, top + lipH, right + lipExt, top + lipH, paint)
        canvas.drawLine(right + lipExt, top, right + lipExt, top + lipH, paint)

        paint.color = 0xFF39FF14.toInt()
        paint.strokeWidth = dp(2).toFloat()
        val numMarks = 4
        for (i in 0..numMarks) {
            val t = i.toFloat() / numMarks
            val y = bottom - beakerH * t
            val longMark = i % 2 == 0
            val markLen = if (longMark) beakerW * 0.2f else beakerW * 0.12f
            canvas.drawLine(left + dp(8).toFloat(), y, left + dp(8).toFloat() + markLen, y, paint)
            if (longMark && t > 0) {
                paint.textSize = dp(10).toFloat()
                paint.style = Paint.Style.FILL
                paint.color = 0xFFE6EDF3.toInt()
                canvas.drawText("${((1 - t) * 100).toInt()}%", left + dp(12).toFloat() + markLen, y + dp(4).toFloat(), paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2).toFloat()
                paint.color = 0xFF39FF14.toInt()
            }
        }

        if (fillPercent > 0) {
            val liquidY = bottom - beakerH * fillPercent
            paint.style = Paint.Style.FILL
            paint.color = 0x6600F0FF.toInt()
            canvas.drawRoundRect(left + dp(3).toFloat(), liquidY, right - dp(3).toFloat(), bottom - dp(3).toFloat(), dp(4).toFloat(), dp(4).toFloat(), paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2).toFloat()
            paint.color = 0xFF00F0FF.toInt()
            canvas.drawLine(left + dp(6).toFloat(), liquidY, right - dp(6).toFloat(), liquidY, paint)
        }

        if (volumeLabel.isNotEmpty()) {
            paint.color = 0xFFE6EDF3.toInt()
            paint.textSize = dp(14).toFloat()
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(volumeLabel, cx, bottom + dp(28).toFloat(), paint)
        }
        canvas.restore()
    }
}

class CozeltiFragment : Fragment() {
    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_cozelti, container, false)

        val hedefM = v.findViewById<EditText>(R.id.coz_hedef_M)
        val hedefV = v.findViewById<EditText>(R.id.coz_hedef_V)
        val mK = v.findViewById<EditText>(R.id.coz_mK)
        val yuzde = v.findViewById<EditText>(R.id.coz_yuzde)
        val yogunluk = v.findViewById<EditText>(R.id.coz_yogunluk)
        val sonuc = v.findViewById<TextView>(R.id.coz_sonuc)
        val dcmYuzde = v.findViewById<EditText>(R.id.dcm_yuzde)
        val dcmYogunluk = v.findViewById<EditText>(R.id.dcm_yogunluk)
        val dcmmK = v.findViewById<EditText>(R.id.dcm_mK)
        val dcmSonuc = v.findViewById<TextView>(R.id.dcm_sonuc)
        val solutionView = v.findViewById<SolutionView>(R.id.solution_view)

        v.findViewById<Button>(R.id.preset_1M_NaCl).setOnClickListener {
            hedefM.setText("1.0"); hedefV.setText("1.0"); mK.setText("58.44")
        }
        v.findViewById<Button>(R.id.preset_01M_HCl).setOnClickListener {
            hedefM.setText("0.1"); hedefV.setText("0.5"); mK.setText("36.46")
        }
        v.findViewById<Button>(R.id.preset_2M_H2SO4).setOnClickListener {
            hedefM.setText("2.0"); hedefV.setText("0.25"); mK.setText("98.08")
        }
        v.findViewById<Button>(R.id.preset_05M_NaOH).setOnClickListener {
            hedefM.setText("0.5"); hedefV.setText("1.0"); mK.setText("40.0")
        }

        v.findViewById<Button>(R.id.coz_hazirla).setOnClickListener {
            try {
                val M = hedefM.text.toString().toDoubleOrNull() ?: 0.0
                val V = hedefV.text.toString().toDoubleOrNull() ?: 0.0
                val mID = mK.text.toString().toDoubleOrNull() ?: 0.0
                if (M <= 0 || V <= 0 || mID <= 0) {
                    sonuc.text = "Hedef M, V ve mK pozitif olmali"; return@setOnClickListener
                }
                val n = M * V
                val gereIliKutle = n * mID
                val sI = StringBuilder()
                sI.append("Gerekli Cozunen: ${"%.4f".format(gereIliKutle)} g\n")
                sI.append("Mol sayisi: ${"%.4f".format(n)} mol\n")
                sI.append("Hacim: ${"%.4f".format(V)} L")

                val y = yuzde.text.toString().toDoubleOrNull()
                val d = yogunluk.text.toString().toDoubleOrNull()
                if (y != null && d != null && y > 0 && d > 0) {
                    val VstoI = gereIliKutle / (d * y / 100.0)
                    sI.append("\n\nStok Cozeltiden:\n${"%.4f".format(VstoI)} mL alinip ${"%.4f".format(V * 1000 - VstoI)} mL cozucu ile tamamlanir")
                }
                sonuc.text = sI.toString()

                val maxVol = 2.0
                solutionView.setLiquidLevel((V / maxVol).toFloat(), "${"%.2f".format(V)} L")
            } catch (e: Exception) { sonuc.text = "Hata: ${e.message}" }
        }

        v.findViewById<Button>(R.id.dcm_hesapla).setOnClickListener {
            try {
                val y = dcmYuzde.text.toString().toDoubleOrNull() ?: 0.0
                val d = dcmYogunluk.text.toString().toDoubleOrNull() ?: 0.0
                val mID = dcmmK.text.toString().toDoubleOrNull() ?: 0.0
                if (y <= 0 || d <= 0 || mID <= 0) { dcmSonuc.text = "Tum alanlari doldurun"; return@setOnClickListener }
                val M = KimyaData.yogunluktanMolarite(y, d, mID)
                if (M == null) dcmSonuc.text = "Hesaplama hatasi"
                else dcmSonuc.text = "M = ${"%.4f".format(M)} M\n(%${"%.1f".format(y)} cozelti, d=${"%.3f".format(d)} g/mL, mK=${"%.2f".format(mID)} g/mol)"
            } catch (e: Exception) { dcmSonuc.text = "Hata: ${e.message}" }
        }
        return v
    }
}
