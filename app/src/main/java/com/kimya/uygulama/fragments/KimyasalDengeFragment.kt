package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog

class EquilibriumView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    private var kpValue: Double? = null
    private var animAngle = 0f
    private var targetAngle = 0f
    private var t = 0f
    private var leMode: String? = null // "conc","press","temp"
    private val bgPaint = Paint().apply { color = 0xFF0D1117.toInt() }
    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }
    fun setKp(value: Double?) {
        kpValue = value
        leMode = null
        targetAngle = when {
            value == null || value.isNaN() || value.isInfinite() -> 0f
            value > 1000 -> 14f
            value < 0.001 -> -14f
            value > 1 -> (Math.log10(value) * 4).toFloat().coerceIn(0f, 14f)
            value < 1 -> (-Math.log10(1/value) * 4).toFloat().coerceIn(-14f, 0f)
            else -> 0f
        }
        ValueAnimator.ofFloat(animAngle, targetAngle).apply { duration = 700; interpolator = DecelerateInterpolator(); addUpdateListener { animAngle = it.animatedValue as Float; invalidate() }; start() }
    }
    fun setLeMode(mode: String) {
        leMode = mode
        // animate tilt to indicate shift: conc -> right, press -> depending, temp -> left
        targetAngle = when(mode){
            "conc" -> 10f
            "press" -> -10f
            "temp" -> 8f
            else -> 0f
        }
        ValueAnimator.ofFloat(animAngle, targetAngle).apply { duration = 500; interpolator = DecelerateInterpolator(); addUpdateListener { animAngle = it.animatedValue as Float; invalidate() }; start()
            // return to center after 1.2s
            postDelayed({ ValueAnimator.ofFloat(animAngle, 0f).apply { duration = 600; interpolator = DecelerateInterpolator(); addUpdateListener { animAngle = it.animatedValue as Float; invalidate() }; start() } }, 1200)
        }
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w<=0||h<=0) return
        t += 0.02f; if(t>1f) t-=1f
        canvas.drawRect(0f,0f,w,h,bgPaint)
        val cx = w/2f; val topY = h*0.28f; val baseY = h*0.88f
        val pillarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A3441.toInt(); strokeWidth = 4f }
        canvas.drawLine(cx, topY, cx, baseY, pillarPaint)
        canvas.drawLine(cx-22f, baseY, cx+22f, baseY, pillarPaint.apply { strokeWidth = 6f })
        canvas.save()
        canvas.rotate(animAngle, cx, topY)
        val beamLen = w*0.34f
        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 5f; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(cx-beamLen, topY, cx+beamLen, topY, beamPaint)
        val panW = w*0.18f; val panH = 8f
        val panFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF161B22.toInt(); style = Paint.Style.FILL }
        val panStrokeCyan = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4DD0E1.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
        val panStrokeLime = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF81C784.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
        val lx = cx-beamLen; val ly = topY
        canvas.drawLine(lx, ly, lx, ly+28f, pillarPaint.apply { strokeWidth = 2f })
        canvas.drawRoundRect(lx-panW/2f, ly+28f, lx+panW/2f, ly+28f+panH, 4f,4f, panFill)
        canvas.drawRoundRect(lx-panW/2f, ly+28f, lx+panW/2f, ly+28f+panH, 4f,4f, panStrokeCyan)
        val rx = cx+beamLen; val ry = topY
        canvas.drawLine(rx, ry, rx, ry+28f, pillarPaint)
        canvas.drawRoundRect(rx-panW/2f, ry+28f, rx+panW/2f, ry+28f+panH, 4f,4f, panFill)
        canvas.drawRoundRect(rx-panW/2f, ry+28f, rx+panW/2f, ry+28f+panH, 4f,4f, panStrokeLime)
        canvas.restore()
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4DD0E1.toInt(); textSize = 11f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("Reaktif", cx-beamLen, topY+52f, labelPaint)
        labelPaint.color = 0xFF81C784.toInt()
        canvas.drawText("Ürün", cx+beamLen, topY+52f, labelPaint)
        // Kp or Le mode text
        if (leMode != null) {
            val txt = when(leMode){ "conc"->"Konsantrasyon ↑ → SAĞA"; "press"->"Basınç ↑ → AZ mol"; "temp"->"Sıcaklık ↑ → K değişir"; else->"" }
            canvas.drawText(txt, cx, baseY-8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB388FF.toInt(); textSize = 11f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.CENTER; isFakeBoldText = true })
        } else kpValue?.let {
            val txt = String.format(java.util.Locale.US, "Kp=%.4g", it)
            val col = when { it>1000 -> 0xFF81C784.toInt(); it<0.001 -> 0xFF4DD0E1.toInt(); else -> 0xFFE6EDF3.toInt() }
            val vp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; textSize = 13f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText(txt, cx, baseY-6f, vp)
            val note = when { it>1000 -> "Ürün baskın"; it<0.001 -> "Reaktif baskın"; else -> "Dengede" }
            canvas.drawText(note, cx, baseY+12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textSize = 10f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.CENTER })
        } ?: run {
            canvas.drawText("Kp = Kc(RT)^Δn", cx, baseY+6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB388FF.toInt(); textSize = 12f * resources.displayMetrics.scaledDensity; textAlign = Paint.Align.CENTER; isFakeBoldText = true })
        }
        if (isAttachedToWindow) postInvalidateOnAnimation()
    }
}

class KimyasalDengeFragment : Fragment() {
    private lateinit var scene: EquilibriumView
    private lateinit var resultCard: LinearLayout
    private lateinit var leResultCard: LinearLayout
    private fun parse(s: String): Double? = s.replace(",",".").trim().toDoubleOrNull()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_kimyasal_denge, container, false)
        AnimUtils.gradientTitle(v.findViewById(R.id.den_baslik))
        return v
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scene = view.findViewById(R.id.den_scene)
        val etKc = view.findViewById<EditText>(R.id.et_kc)
        val etDeltaN = view.findViewById<EditText>(R.id.et_delta_n)
        val etTemp = view.findViewById<EditText>(R.id.et_temp_kc)
        val tvResult = view.findViewById<TextView>(R.id.tv_kp_kc_result)
        val tvLeResult = view.findViewById<TextView>(R.id.tv_le_result)
        resultCard = view.findViewById(R.id.kp_result_card)
        leResultCard = view.findViewById(R.id.le_result_card)
        val btnKpKc = view.findViewById<View>(R.id.btn_kp_kc)
        val btnLeConc = view.findViewById<View>(R.id.btn_le_conc)
        val btnLePressure = view.findViewById<View>(R.id.btn_le_pressure)
        val btnLeTemp = view.findViewById<View>(R.id.btn_le_temp)

        // Kp presets
        view.findViewById<View>(R.id.den_preset1)?.setOnClickListener { AnimUtils.press(it); etKc.setText("2"); etDeltaN.setText("1"); etTemp.setText("298"); btnKpKc.performClick() }
        view.findViewById<View>(R.id.den_preset2)?.setOnClickListener { AnimUtils.press(it); etKc.setText("0.1"); etDeltaN.setText("-2"); etTemp.setText("298"); btnKpKc.performClick() }
        view.findViewById<View>(R.id.den_preset3)?.setOnClickListener { AnimUtils.press(it); etKc.setText("0.01"); etDeltaN.setText("0"); etTemp.setText("298"); btnKpKc.performClick() }

        btnKpKc.setOnClickListener {
            AnimUtils.press(it)
            val kc = parse(etKc.text.toString()); val dn = parse(etDeltaN.text.toString()); val t = parse(etTemp.text.toString())
            if (kc==null||dn==null||t==null){ Toast.makeText(context,"Tüm değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (t<=0||kc<0){ resultCard.visibility = View.VISIBLE; AnimUtils.popIn(resultCard); tvResult.text = "Geçersiz giriş: T pozitif olmalı ve Kc negatif olmamalı"; AnimUtils.flash(tvResult); return@setOnClickListener }
            val R = 0.08206; val kp = kc * Math.pow(R*t, dn)
            resultCard.visibility = View.VISIBLE; AnimUtils.popIn(resultCard)
            tvResult.text = buildString {
                append("Kp = Kc × (RT)^Δn\n")
                append("Kp = ${String.format(java.util.Locale.US,"%.4f",kc)} × (0.08206×${String.format(java.util.Locale.US,"%.0f",t)})^${String.format(java.util.Locale.US,"%.0f",dn)}\n")
                append("Kp = ${String.format(java.util.Locale.US,"%.4f",kc)} × ${String.format(java.util.Locale.US,"%.4f",Math.pow(R*t, dn))}\n")
                append("Kp = ${String.format(java.util.Locale.US,"%.6f",kp)}\n\n")
                if(kp>1000) append("Ürünler baskın (sağa kaymış)") else if(kp<0.001) append("Reaktifler baskın (sola kaymış)") else append("Reaktif ve ürünler dengede")
            }
            AnimUtils.flash(tvResult); scene.setKp(kp)
            // scroll to result
            resultCard.post { (view as? android.widget.ScrollView)?.smoothScrollTo(0, resultCard.top) }
        }

        fun showLe(text: String, mode: String) {
            leResultCard.visibility = View.VISIBLE
            tvLeResult.text = text
            AnimUtils.popIn(leResultCard)
            scene.setLeMode(mode)
            leResultCard.post {
                try {
                    (view as? android.widget.ScrollView)?.smoothScrollTo(0, leResultCard.top - 50)
                } catch (_: Exception) {}
            }
        }

        btnLeConc.setOnClickListener {
            AnimUtils.press(it)
            showLe("KONSANTRASYON:\n\n▸ [Reaktif] ↑ → SAĞA (ürün)\n▸ [Reaktif] ↓ → SOLA\n▸ [Ürün] ↑ → SOLA\n▸ [Ürün] ↓ → SAĞA\n\nK sabit kalır, konum değişir.", "conc")
        }
        btnLePressure.setOnClickListener {
            AnimUtils.press(it)
            showLe("BASINÇ:\n\n▸ P ↑ → gaz mol AZ tarafa\n▸ P ↓ → gaz mol ÇOK tarafa\n▸ Δn=0 → etkisiz\n\nÖr: N₂+3H₂⇌2NH₃ Δn=-2 → P↑ SAĞA", "press")
        }
        btnLeTemp.setOnClickListener {
            AnimUtils.press(it)
            showLe("SICAKLIK:\n\n▸ Ekzotermik (ΔH<0): T↑ → K↓ SOLA\n  T↓ → K↑ SAĞA\n\n▸ Endotermik (ΔH>0): T↑ → K↑ SAĞA\n  T↓ → K↓ SOLA\n\nSadece sıcaklık K'yı değiştirir", "temp")
        }

        view.findViewById<View>(R.id.btn_help).setOnClickListener {
            AnimUtils.press(it)
            HelpDialog.showGuide(requireContext(),"Kimyasal Denge","Kp=Kc(RT)^Δn ve Le Chatelier.", listOf(
                "Kc, Δn, T girin → Kp Hesapla (presetlerle hızlı).",
                "Le Chatelier için Kons./Basınç/Sıcaklık chip'lerine basın — terazi animasyonla kayar.",
                "Kp>1000 ürün, Kp<0.001 reaktif baskın.",
                "Canvas terazi + Le modunda ok yönünü gösterir."
            ))
        }

        val header = view.findViewById<View>(R.id.den_baslik).parent.parent as View
        val sceneV = view.findViewById<View>(R.id.den_scene).parent as View
        header.alpha=0f; sceneV.alpha=0f
        header.post{ AnimUtils.slideUpFade(header,0L); AnimUtils.slideUpFade(sceneV,70L) }
        // initial le result hidden
        leResultCard.visibility = View.GONE
    }
}

