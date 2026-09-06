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
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import com.kimya.uygulama.utils.KimyaData

class SolutionView(context: Context, attrs: android.util.AttributeSet? = null) : View(context, attrs) {
    private var fillPercent = 0f
    private var targetFill = 0f
    private var volumeLabel = ""
    private var fillAnimator: ValueAnimator? = null

    private val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4DD0E1.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE
    }
    private val liquidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val meniscusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4DD0E1.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE
    }
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF81C784.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE
    }
    private val markTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 24f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.LEFT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 30f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }

    private fun dp(n: Int): Float = (n * context.resources.displayMetrics.density).toFloat()

    fun setLiquidLevel(percent: Float, label: String) {
        targetFill = percent.coerceIn(0f, 1f)
        volumeLabel = label
        fillAnimator?.cancel()
        fillAnimator = ValueAnimator.ofFloat(fillPercent, targetFill).apply {
            duration = 800
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { fillPercent = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f

        val beakerW = w * 0.5f
        val beakerH = h * 0.62f
        val left = cx - beakerW / 2f
        val right = cx + beakerW / 2f
        val top = h * 0.12f
        val bottom = top + beakerH
        val corner = dp(6).toInt()

        canvas.drawRoundRect(left, top, right, bottom, corner.toFloat(), corner.toFloat(), glassPaint)

        for (i in 0..4) {
            val t = i.toFloat() / 4f
            val y = bottom - beakerH * t
            val markLen = if (i % 2 == 0) beakerW * 0.18f else beakerW * 0.1f
            canvas.drawLine(left + dp(8), y, left + dp(8) + markLen, y, markPaint)
            if (i % 2 == 0 && t > 0) {
                canvas.drawText("${((1 - t) * 100).toInt()}%", left + dp(14) + markLen, y + dp(4), markTextPaint)
            }
        }

        if (fillPercent > 0.01f) {
            val liquidY = bottom - beakerH * fillPercent
            val lGrad = LinearGradient(left, liquidY, left, bottom,
                0x4400F0FF.toInt(), 0x7700F0FF.toInt(), Shader.TileMode.CLAMP)
            liquidPaint.shader = lGrad
            canvas.drawRoundRect(left + dp(3), liquidY + dp(3), right - dp(3), bottom - dp(3),
                corner.toFloat() / 2, corner.toFloat() / 2, liquidPaint)

            val mPath = Path()
            mPath.moveTo(left + dp(8), liquidY + dp(6))
            mPath.quadTo(cx, liquidY, right - dp(8), liquidY + dp(6))
            canvas.drawPath(mPath, meniscusPaint)
        }

        if (volumeLabel.isNotEmpty()) {
            canvas.drawText(volumeLabel, cx, bottom + dp(26), labelPaint)
        }
    }
}

class CozeltiFragment : Fragment() {
    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun animateValue(tv: TextView, target: Double, start: Double = 0.0) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener { a -> tv.text = (start + (target - start) * (a.animatedValue as Float)).let { "%.4f".format(it) } }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { tv.text = "%.4f".format(target) }
            })
            start()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_cozelti, container, false)

        val hedefM = v.findViewById<EditText>(R.id.coz_hedef_M)
        val hedefV = v.findViewById<EditText>(R.id.coz_hedef_V)
        val mK = v.findViewById<EditText>(R.id.coz_mK)
        val yuzde = v.findViewById<EditText>(R.id.coz_yuzde)
        val yogunluk = v.findViewById<EditText>(R.id.coz_yogunluk)
        val sonuc = v.findViewById<TextView>(R.id.coz_sonuc)
        val resultCard = v.findViewById<View>(R.id.coz_result_card)
        val resultNote = v.findViewById<TextView>(R.id.coz_result_note)
        val resultValue = v.findViewById<TextView>(R.id.coz_result_value)

        val dcmYuzde = v.findViewById<EditText>(R.id.dcm_yuzde)
        val dcmYogunluk = v.findViewById<EditText>(R.id.dcm_yogunluk)
        val dcmmK = v.findViewById<EditText>(R.id.dcm_mK)
        val dcmSonuc = v.findViewById<TextView>(R.id.dcm_sonuc)
        val dcmResultCard = v.findViewById<View>(R.id.dcm_result_card)
        val dcmResultNote = v.findViewById<TextView>(R.id.dcm_result_note)
        val dcmResultValue = v.findViewById<TextView>(R.id.dcm_result_value)

        val solutionView = v.findViewById<SolutionView>(R.id.solution_view)

        val chips = listOf(
            v.findViewById<TextView>(R.id.preset_1M_NaCl),
            v.findViewById<TextView>(R.id.preset_01M_HCl),
            v.findViewById<TextView>(R.id.preset_2M_H2SO4),
            v.findViewById<TextView>(R.id.preset_05M_NaOH)
        )
        fun selectChip(c: TextView) { chips.forEach { it.isSelected = it === c } }

        fun hazirla(animate: Boolean) {
            val M = hedefM.text.toString().toDoubleOrNull()
            val V = hedefV.text.toString().toDoubleOrNull()
            val mID = mK.text.toString().toDoubleOrNull()
            if (M == null || V == null || mID == null || M <= 0 || V <= 0 || mID <= 0) {
                resultCard.visibility = View.VISIBLE
                resultNote.text = "uyari"
                sonuc.visibility = View.VISIBLE
                sonuc.text = "M, V ve Ma alanlarini doldurun (pozitif degerler)"
                return
            }
            val n = M * V
            val gereKutle = n * mID
            resultCard.visibility = View.VISIBLE
            resultNote.text = "gerekli kutle"
            if (animate) animateValue(resultValue, gereKutle) else resultValue.text = "%.4f".format(gereKutle)
            sonuc.visibility = View.VISIBLE
            sonuc.text = "Mol: ${"%.4f".format(n)} mol | Hacim: ${"%.2f".format(V)} L"

            val y = yuzde.text.toString().toDoubleOrNull()
            val d = yogunluk.text.toString().toDoubleOrNull()
            if (y != null && d != null && y > 0 && d > 0) {
                val Vstok = gereKutle / (d * y / 100.0)
                val cozucu = maxOf(0.0, V * 1000 - Vstok)
                sonuc.text = sonuc.text.toString() + "\nStok: ${"%.2f".format(Vstok)} mL alinip ${"%.2f".format(cozucu)} mL cozucu ile tamamlanir" + if (Vstok > V * 1000) "\nUyari: stok hacmi hedefi aşıyor" else ""
            }
            AnimUtils.popIn(resultCard)
            AnimUtils.flash(sonuc)
            solutionView.setLiquidLevel((V / 2.0).toFloat(), "${"%.2f".format(V)} L")
        }

        v.findViewById<TextView>(R.id.coz_hazirla).setOnClickListener {
            AnimUtils.press(it); hazirla(true)
        }

        v.findViewById<TextView>(R.id.preset_1M_NaCl).setOnClickListener {
            AnimUtils.press(it); selectChip(it as TextView)
            hedefM.setText("1.0"); hedefV.setText("1.0"); mK.setText("58.44")
            hazirla(true)
        }
        v.findViewById<TextView>(R.id.preset_01M_HCl).setOnClickListener {
            AnimUtils.press(it); selectChip(it as TextView)
            hedefM.setText("0.1"); hedefV.setText("0.5"); mK.setText("36.46")
            hazirla(true)
        }
        v.findViewById<TextView>(R.id.preset_2M_H2SO4).setOnClickListener {
            AnimUtils.press(it); selectChip(it as TextView)
            hedefM.setText("2.0"); hedefV.setText("0.25"); mK.setText("98.08")
            hazirla(true)
        }
        v.findViewById<TextView>(R.id.preset_05M_NaOH).setOnClickListener {
            AnimUtils.press(it); selectChip(it as TextView)
            hedefM.setText("0.5"); hedefV.setText("1.0"); mK.setText("40.0")
            hazirla(true)
        }

        v.findViewById<TextView>(R.id.dcm_hesapla).setOnClickListener {
            AnimUtils.press(it)
            val y = dcmYuzde.text.toString().toDoubleOrNull()
            val d = dcmYogunluk.text.toString().toDoubleOrNull()
            val mID = dcmmK.text.toString().toDoubleOrNull()
            if (y == null || d == null || mID == null || y <= 0 || d <= 0 || mID <= 0) {
                dcmResultCard.visibility = View.VISIBLE
                dcmResultNote.text = "uyari"
                dcmSonuc.visibility = View.VISIBLE
                dcmSonuc.text = "Tum alanlari doldurun (pozitif degerler)"
                return@setOnClickListener
            }
            val M = KimyaData.yogunluktanMolarite(y, d, mID)
            if (M == null || M.isNaN() || M.isInfinite()) {
                dcmResultCard.visibility = View.VISIBLE
                dcmResultNote.text = "hata"
                dcmSonuc.visibility = View.VISIBLE
                dcmSonuc.text = "Hesaplama hatasi"
            } else {
                dcmResultCard.visibility = View.VISIBLE
                dcmResultNote.text = "molarite"
                if (true) animateValue(dcmResultValue, M) else dcmResultValue.text = "%.4f".format(M)
                dcmSonuc.visibility = View.VISIBLE
                dcmSonuc.text = "%${"%.1f".format(y)} cozelti, d=${"%.3f".format(d)} g/mL, Ma=${"%.2f".format(mID)} g/mol"
                AnimUtils.popIn(dcmResultCard)
                AnimUtils.flash(dcmSonuc)
            }
        }

        v.findViewById<TextView>(R.id.coz_help).setOnClickListener {
            HelpDialog.showGuide(
                requireContext(),
                "Cozelti Hazirlama",
                "Molarite (M) ve gerekli kutle hesaplama.",
                listOf(
                    "M = n / V formulu: M molarite, n mol sayisi, V hacim (L).",
                    "Gerekli kutle: m = M x V x Ma (Ma: molar kutle).",
                    "Stok cozeltiden hazirlama: yuzde ve yogunluk girin, gerekli stok hacmi hesaplanir.",
                    "Yogunluktan molarite: % yuzde, yogunluk ve Ma ile M hesaplanir."
                )
            )
        }

        AnimUtils.gradientTitle(v.findViewById(R.id.coz_baslik))
        AnimUtils.slideUpFade(v)
        return v
    }
}

