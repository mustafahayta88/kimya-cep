package com.kimya.uygulama.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.util.Locale

class BirimFragment : Fragment() {

    private val kategoriler = mapOf(
        "Uzunluk" to mapOf("km" to 1000.0, "hm" to 100.0, "dam" to 10.0, "m" to 1.0, "dm" to 0.1, "cm" to 0.01, "mm" to 0.001),
        "Kutle" to mapOf("kg" to 1000.0, "hg" to 100.0, "dag" to 10.0, "g" to 1.0, "dg" to 0.1, "cg" to 0.01, "mg" to 0.001, "lb" to 453.592, "oz" to 28.3495),
        "Hacim" to mapOf("kL" to 1000.0, "hL" to 100.0, "daL" to 10.0, "L" to 1.0, "dL" to 0.1, "cL" to 0.01, "mL" to 0.001),
        "Sicaklik" to null,
        "Basinc" to mapOf("atm" to 1.0, "bar" to 0.986923, "kPa" to 0.00986923, "mmHg" to 1.0 / 760.0, "psi" to 0.068046),
        "Enerji" to mapOf("kWh" to 3_600_000.0, "kJ" to 1000.0, "kcal" to 4184.0, "J" to 1.0, "cal" to 4.184),
        "Konsantrasyon" to mapOf("M" to 1.0, "mM" to 0.001, "uM" to 1e-6, "nM" to 1e-9, "ppm" to 1e-6, "% (w/v)" to 0.1),
        "Alan" to mapOf("km2" to 1_000_000.0, "ha" to 10000.0, "m2" to 1.0, "cm2" to 1e-4, "mm2" to 1e-6, "ft2" to 0.092903, "in2" to 0.00064516, "ac" to 4046.86),
        "Yogunluk" to mapOf("g/mL" to 1.0, "kg/m3" to 0.001, "g/L" to 0.001, "lb/ft3" to 0.0160185)
    )

    private val kategoriKeys = kategoriler.keys.toList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_birim, container, false)

        val kategoriSpinner = v.findViewById<Spinner>(R.id.birim_kategori)
        val kaynakSpinner = v.findViewById<Spinner>(R.id.birim_kaynak)
        val hedefSpinner = v.findViewById<Spinner>(R.id.birim_hedef)
        val degerInput = v.findViewById<EditText>(R.id.birim_deger)
        val donusturBtn = v.findViewById<Button>(R.id.birim_donustur)
        val sonucText = v.findViewById<TextView>(R.id.birim_sonuc)
        val bannerText = v.findViewById<TextView>(R.id.birim_banner)
        val formulText = v.findViewById<TextView>(R.id.birim_formul)
        val carpanText = v.findViewById<TextView>(R.id.birim_carpan)
        val convView = v.findViewById<ConversionView>(R.id.birim_conv_view)

        fun styleAdapter(items: List<String>): ArrayAdapter<String> =
            object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = super.getView(position, convertView, parent) as TextView
                    tv.setTextColor(0xFFE6EDF3.toInt())
                    tv.textSize = 14f
                    return tv
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val tv = super.getDropDownView(position, convertView, parent) as TextView
                    tv.setTextColor(0xFFE6EDF3.toInt())
                    tv.setBackgroundColor(0xFF161B22.toInt())
                    return tv
                }
            }

        kategoriSpinner.adapter = styleAdapter(kategoriKeys)

        kategoriSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val units = kategoriler[kategoriKeys[position]]
                val birimler = if (units != null) units.keys.toList() else listOf("C", "F", "K")
                kaynakSpinner.adapter = styleAdapter(birimler)
                hedefSpinner.adapter = styleAdapter(birimler)
                bannerText.text = ""
                sonucText.text = ""
                formulText.text = ""
                carpanText.text = ""
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        kategoriSpinner.setSelection(0)

        donusturBtn.setOnClickListener {
            val deger = degerInput.text.toString().toDoubleOrNull()
            if (deger == null) {
                sonucText.text = "Gecersiz deger"
                bannerText.text = ""
                return@setOnClickListener
            }
            val kaynak = kaynakSpinner.selectedItem?.toString() ?: ""
            val hedef = hedefSpinner.selectedItem?.toString() ?: ""
            val katAdi = kategoriKeys[kategoriSpinner.selectedItemPosition]

            var sonuc: Double
            var not: String
            var formul: String
            var carpan: Double

            if (katAdi == "Sicaklik") {
                sonuc = sicaklikDonustur(deger, kaynak, hedef)
                formul = sicaklikFormul(kaynak, hedef)
                not = "Sicaklik ofset formulu uygulandi."
                carpan = 1.0
            } else {
                val birimler = kategoriler[katAdi] ?: run {
                    sonucText.text = "Hata"
                    bannerText.text = ""
                    return@setOnClickListener
                }
                val iF = birimler[kaynak]
                val hF = birimler[hedef]
                if (iF == null || hF == null) {
                    sonucText.text = "Bilinmeyen birim!"
                    bannerText.text = ""
                    return@setOnClickListener
                }
                carpan = iF / hF
                sonuc = (deger * iF) / hF
                not = "Carpan: ${formatSayi(iF)} -> ${formatSayi(hF)}"
                formul = "$kaynak -> $hedef: carpan = ${formatSayi(iF)} / ${formatSayi(hF)} = ${formatSayi(carpan)}"
            }
            if (sonuc.isNaN() || sonuc.isInfinite()) {
                bannerText.text = "Hata: gecersiz donusum"
                sonucText.text = ""
                formulText.text = ""
                carpanText.text = ""
            } else {
                bannerText.text = "${formatSayi(deger)} $kaynak = ${formatSayi(sonuc)} $hedef"
                sonucText.text = "$kaynak: ${formatSayi(deger)}  ->  $hedef: ${formatSayi(sonuc)}"
                formulText.text = "Formul: $formul"
                carpanText.text = not
            }
            convView.setConversion(kaynak, hedef, carpan, formul)
            convView.resetAnimation()
        }

        v.findViewById<Button>(R.id.birim_preset1).setOnClickListener {
            kategoriSpinner.setSelection(3)
            kaynakSpinner.setSelection(0)
            hedefSpinner.setSelection(2)
            degerInput.setText("25")
            donusturBtn.performClick()
        }
        v.findViewById<Button>(R.id.birim_preset2).setOnClickListener {
            kategoriSpinner.setSelection(4)
            kaynakSpinner.setSelection(0)
            hedefSpinner.setSelection(2)
            degerInput.setText("1")
            donusturBtn.performClick()
        }
        v.findViewById<Button>(R.id.birim_preset3).setOnClickListener {
            kategoriSpinner.setSelection(5)
            kaynakSpinner.setSelection(1)
            hedefSpinner.setSelection(3)
            degerInput.setText("1")
            donusturBtn.performClick()
        }
        v.findViewById<Button>(R.id.birim_preset4).setOnClickListener {
            kategoriSpinner.setSelection(6)
            kaynakSpinner.setSelection(0)
            hedefSpinner.setSelection(1)
            degerInput.setText("1")
            donusturBtn.performClick()
        }
        v.findViewById<Button>(R.id.birim_preset5).setOnClickListener {
            kategoriSpinner.setSelection(1)
            kaynakSpinner.setSelection(3)
            hedefSpinner.setSelection(6)
            degerInput.setText("1")
            donusturBtn.performClick()
        }
        v.findViewById<Button>(R.id.birim_preset6).setOnClickListener {
            kategoriSpinner.setSelection(2)
            kaynakSpinner.setSelection(3)
            hedefSpinner.setSelection(6)
            degerInput.setText("1")
            donusturBtn.performClick()
        }

        return v
    }

    private fun sicaklikDonustur(deger: Double, kaynak: String, hedef: String): Double {
        val celsius = when (kaynak) {
            "C" -> deger
            "F" -> (deger - 32) * 5 / 9
            "K" -> deger - 273.15
            else -> return Double.NaN
        }
        return when (hedef) {
            "C" -> celsius
            "F" -> celsius * 9 / 5 + 32
            "K" -> celsius + 273.15
            else -> Double.NaN
        }
    }

    private fun sicaklikFormul(kaynak: String, hedef: String): String {
        return when {
            kaynak == "C" && hedef == "F" -> "(C * 9/5) + 32 = F"
            kaynak == "C" && hedef == "K" -> "C + 273.15 = K"
            kaynak == "F" && hedef == "C" -> "(F - 32) * 5/9 = C"
            kaynak == "F" && hedef == "K" -> "(F - 32) * 5/9 + 273.15 = K"
            kaynak == "K" && hedef == "C" -> "K - 273.15 = C"
            kaynak == "K" && hedef == "F" -> "(K - 273.15) * 9/5 + 32 = F"
            else -> "Dogrusal donusum"
        }
    }

    private fun formatSayi(v: Double): String {
        return when {
            v == 0.0 -> "0"
            v == v.toLong().toDouble() -> v.toLong().toString()
            kotlin.math.abs(v) < 1e-3 || kotlin.math.abs(v) > 1e6 -> String.format(Locale.US, "%.4e", v)
            else -> String.format(Locale.US, "%.4f", v)
        }
    }
}

class ConversionView @JvmOverloads constructor(
    context: Context, attrs: android.util.AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    private var kaynakBirim = ""
    private var hedefBirim = ""
    private var guncelCarpan = 1.0
    private var guncelFormul = ""

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private val boxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF161B22.toInt()
        style = Paint.Style.FILL
    }
    private val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val labelText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt()
        textSize = 36f
    }
    private val arrowLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt()
        strokeWidth = 3f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val arrowHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt()
        style = Paint.Style.FILL
    }
    private val factorText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB388FF.toInt()
        textSize = 28f
    }
    private val formulaText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF39FF14.toInt()
        textSize = 24f
    }

    private var animProgress = 0f
    private var animator: ValueAnimator? = null

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    init {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = LinearInterpolator()
            addUpdateListener {
                animProgress = it.animatedFraction
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animProgress = 1f
                    invalidate()
                }
            })
        }
    }

    fun setConversion(kaynak: String, hedef: String, carpan: Double, formul: String) {
        kaynakBirim = kaynak
        hedefBirim = hedef
        guncelCarpan = carpan
        guncelFormul = formul
    }

    fun resetAnimation() {
        animProgress = 0f
        animator?.cancel()
        animator?.start()
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
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        val boxW = dp(100).toFloat()
        val boxH = dp(50).toFloat()
        val cy = h / 2f

        val srcX = dp(16).toFloat()
        drawBox(canvas, srcX, cy, boxW, boxH, kaynakBirim.ifEmpty { "?" })

        val tgtX = w - boxW - dp(16).toFloat()
        drawBox(canvas, tgtX, cy, boxW, boxH, hedefBirim.ifEmpty { "?" })

        val lineStartX = srcX + boxW + dp(8)
        val lineEndX = tgtX - dp(8)
        val lineLen = lineEndX - lineStartX
        val drawLen = lineLen * animProgress

        val path = Path()
        path.moveTo(lineStartX, cy)
        path.lineTo(lineStartX + drawLen, cy)
        canvas.drawPath(path, arrowLine)

        if (animProgress > 0.1f) {
            val headX = lineStartX + drawLen
            val headPath = Path().apply {
                moveTo(headX, cy)
                lineTo(headX - dp(10), cy - dp(6))
                lineTo(headX - dp(10), cy + dp(6))
                close()
            }
            canvas.drawPath(headPath, arrowHead)
        }

        val carpanStr = if (guncelCarpan == 1.0) "" else "x${formatSayi(guncelCarpan)}"
        if (carpanStr.isNotEmpty() && animProgress > 0.3f) {
            val alpha = ((animProgress - 0.3f) / 0.7f * 255).toInt().coerceIn(0, 255)
            factorText.alpha = alpha
            val cx = (lineStartX + lineEndX) / 2f
            canvas.drawText(carpanStr, cx - factorText.measureText(carpanStr) / 2f, cy - dp(20), factorText)
        }

        if (guncelFormul.isNotEmpty() && animProgress > 0.5f) {
            val alpha = ((animProgress - 0.5f) / 0.5f * 255).toInt().coerceIn(0, 255)
            formulaText.alpha = alpha
            val fText = guncelFormul
            canvas.drawText(fText, w / 2f - formulaText.measureText(fText) / 2f, h - dp(16), formulaText)
        }
        canvas.restore()
    }

    private fun drawBox(canvas: Canvas, left: Float, cy: Float, w: Float, h: Float, label: String) {
        val rect = RectF(left, cy - h / 2, left + w, cy + h / 2)
        canvas.drawRoundRect(rect, 8f, 8f, boxFill)
        canvas.drawRoundRect(rect, 8f, 8f, boxStroke)
        canvas.drawText(label, left + w / 2 - labelText.measureText(label) / 2, cy + 12f, labelText)
    }

    private fun formatSayi(v: Double): String {
        return when {
            v == 0.0 -> "0"
            v == v.toLong().toDouble() -> v.toLong().toString()
            kotlin.math.abs(v) < 1e-3 || kotlin.math.abs(v) > 1e6 -> String.format(Locale.US, "%.4e", v)
            else -> String.format(Locale.US, "%.4f", v)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
