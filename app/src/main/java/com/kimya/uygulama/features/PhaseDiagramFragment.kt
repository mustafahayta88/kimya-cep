package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class PhaseDiagramView(context: Context) : View(context) {
    private var substance = 0
    private var cursorX = 0f; private var cursorY = 0f
    private var showCursor = false
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val regionP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textAlign = Paint.Align.CENTER }
    private val phaseP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val cursorP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f; pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f) }

    // Substance data: name, tripleT, tripleP, critT, critP, solid_name, liquid_name, gas_name
    private val subData = arrayOf(
        arrayOf("Su", 0.01, 0.006, 374.0, 218.0, "Buz (Kati)", "Su (Sivi)", "Buhar (Gaz)"),
        arrayOf("CO2", -56.6, 5.11, 31.0, 73.0, "Kuru Buz (Kati)", "Sivi CO2", "CO2 (Gaz)")
    )
    private val subNames = listOf("Su (H2O)", "Karbondioksit (CO2)")

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    fun setSubstance(i: Int) { substance = i.coerceIn(0, 1); invalidate() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_MOVE -> { if (tMode == 1 && zoomScale <= 1.05f) { cursorX = e.x; cursorY = e.y; showCursor = true; invalidate() }; if (tMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }; lastTx = e.x; lastTy = e.y; invalidate() }
            MotionEvent.ACTION_UP -> { tMode = 0; return true }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val data = subData[substance]
        val tMin = -80.0; val tMax = (data[3] as Double) + 20.0
        val pMin = 0.0; val pMax = maxOf((data[4] as Double) * 1.2, 10.0)

        val dLeft = w * 0.12f; val dRight = w * 0.88f; val dTop = h * 0.03f; val dBot = h * 0.70f
        val dw = dRight - dLeft; val dh = dBot - dTop

        // Helper: coordinate mapping functions
        fun tToX(t: Double) = dLeft + dw * ((t - tMin) / (tMax - tMin)).toFloat()
        fun pToY(p: Double) = dBot - dh * ((p - pMin) / (pMax - pMin)).toFloat()

        // Draw phase regions (semi-transparent)
        val n = 200
        val pathSolid = Path(); val pathLiquid = Path(); val pathGas = Path()
        var prevTS = tMin; var prevTG = tMin; var prevTL = tMin
        var firstS = true; var firstL = true; var firstG = true

        for (i in 0..n) {
            val frac = i / n.toDouble()
            val t = tMin + (tMax - tMin) * frac
            // Sublimation curve (S-G)
            val pSG = pMin + (data[4] as Double) * 0.12 * exp((t - (data[1] as Double)) * 0.02)
            // Vaporization curve (L-G)
            val pLG = pMin + (data[4] as Double) * 0.25 * exp((t - (data[1] as Double)) * 0.05)
            // Melting curve (S-L) - nearly vertical
            val pSL = pMin + (data[4] as Double) * (0.002 + max(0.0, (t - (data[1] as Double)) * 0.008))

            val x = tToX(t)
            val ySG = pToY(pSG); val yLG = pToY(pLG); val ySL = pToY(pSL)

            // Solid region: top-left, bounded by SG and SL
            // Liquid region: middle, bounded by SL and LG
            // Gas region: bottom, bounded by SG and LG
        }

        // Draw boundary curves
        val pathSG = Path(); val pathLG = Path(); val pathSL = Path()
        for (i in 0..n) {
            val frac = i / n.toDouble()
            val t = tMin + (tMax - tMin) * frac
            val pSG = pMin + (data[4] as Double) * 0.12 * exp((t - (data[1] as Double)) * 0.02)
            val pLG = pMin + (data[4] as Double) * 0.25 * exp((t - (data[1] as Double)) * 0.05)
            val pSL = pMin + (data[4] as Double) * (0.002 + max(0.0, (t - (data[1] as Double)) * 0.008))

            val x = tToX(t); val ySG = pToY(pSG); val yLG = pToY(pLG); val ySL = pToY(pSL)
            if (i == 0) { pathSG.moveTo(x, ySG); pathLG.moveTo(x, yLG); pathSL.moveTo(x, ySL) }
            else { pathSG.lineTo(x, ySG); pathLG.lineTo(x, yLG); pathSL.lineTo(x, ySL) }
        }
        canvas.drawPath(pathSG, lineP)
        canvas.drawPath(pathLG, lineP)
        canvas.drawPath(pathSL, lineP)

        // Fill phase regions
        // Solid: above SG and left/down of SL
        regionP.color = 0x226666FF.toInt()
        val solidPath = Path()
        solidPath.addPath(pathSG); solidPath.addPath(pathSL)
        // Actually, easier: just draw labeled regions without filling

        // Phase labels with region backgrounds
        phaseP.textSize = h * 0.045f

        // Solid label top-left
        val solidX = dLeft + dw * 0.15f; val solidY = dTop + dh * 0.15f
        regionP.color = 0x22FF4444.toInt()
        canvas.drawRoundRect(solidX - w * 0.06f, solidY - h * 0.03f, solidX + w * 0.06f, solidY + h * 0.03f, 8f, 8f, regionP)
        canvas.drawText(data[5] as String, solidX, solidY + h * 0.01f, phaseP)

        // Liquid label middle
        val liqX = dLeft + dw * 0.45f; val liqY = dTop + dh * 0.55f
        regionP.color = 0x2244FF44.toInt()
        canvas.drawRoundRect(liqX - w * 0.06f, liqY - h * 0.03f, liqX + w * 0.06f, liqY + h * 0.03f, 8f, 8f, regionP)
        canvas.drawText(data[6] as String, liqX, liqY + h * 0.01f, phaseP)

        // Gas label bottom-right
        val gasX = dLeft + dw * 0.75f; val gasY = dTop + dh * 0.25f
        regionP.color = 0x22FFFF44.toInt()
        canvas.drawRoundRect(gasX - w * 0.06f, gasY - h * 0.03f, gasX + w * 0.06f, gasY + h * 0.03f, 8f, 8f, regionP)
        canvas.drawText(data[7] as String, gasX, gasY + h * 0.01f, phaseP)

        // Triple point
        val tpX = tToX(data[1] as Double); val tpY = pToY(data[2] as Double)
        canvas.drawCircle(tpX, tpY, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); style = Paint.Style.FILL })
        textP.textSize = h * 0.02f
        canvas.drawText("Uclu Nokta (${"%.1f".format(data[1] as Double)}°C, ${"%.3f".format(data[2] as Double)} atm)", tpX, tpY - 14f, textP)

        // Critical point
        val crX = tToX(data[3] as Double); val crY = pToY(data[4] as Double)
        canvas.drawCircle(crX, crY, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); style = Paint.Style.FILL })
        textP.textSize = h * 0.02f; textP.color = 0xFFFF0000.toInt()
        canvas.drawText("Kritik Nokta (${"%.0f".format(data[3] as Double)}°C)", crX, crY + 14f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        // Axis labels
        textP.textSize = h * 0.022f
        for (i in 0..5) {
            val frac = i / 5.0
            val t = tMin + (tMax - tMin) * frac; val x = tToX(t)
            canvas.drawText("${"%.0f".format(t)}", x, dBot + 14f, textP)
            val p = pMin + (pMax - pMin) * frac; val y = pToY(p)
            canvas.drawText("${"%.1f".format(p)}", dLeft - 24f, y + 4f, textP)
        }
        canvas.drawText("Sicaklik (°C)", dLeft + dw / 2f, dBot + 32f, textP)
        canvas.save(); canvas.rotate(-90f, dLeft - 30f, dTop + dh / 2f); canvas.drawText("Basinc (atm)", dLeft - 30f, dTop + dh / 2f, textP); canvas.restore()

        // Cursor crosshair and readout
        if (showCursor && cursorX >= dLeft && cursorX <= dRight && cursorY >= dTop && cursorY <= dBot) {
            canvas.drawLine(cursorX, dTop, cursorX, dBot, cursorP)
            canvas.drawLine(dLeft, cursorY, dRight, cursorY, cursorP)

            val cT = tMin + (tMax - tMin) * (cursorX - dLeft) / dw
            val cP = pMin + (pMax - pMin) * (dBot - cursorY) / dh

            // Determine phase
            var phase = "Bilinmiyor"
            val pSGB = pMin + (data[4] as Double) * 0.12 * exp((cT - (data[1] as Double)) * 0.02)
            val pLGB = pMin + (data[4] as Double) * 0.25 * exp((cT - (data[1] as Double)) * 0.05)
            val pSLB = pMin + (data[4] as Double) * (0.002 + max(0.0, (cT - (data[1] as Double)) * 0.008))
            phase = when {
                cT < (data[1] as Double) -> if (cP > pSGB) "Kati" else "Gaz"
                else -> when {
                    cP > pSLB -> "Kati"
                    cP > pLGB -> "Sivi"
                    else -> "Gaz"
                }
            }

            labelP.textSize = h * 0.028f
            canvas.drawText("T = ${"%.1f".format(cT)}°C | P = ${"%.3f".format(cP)} atm", w / 2f, h * 0.77f, labelP)
            valP.textSize = h * 0.025f; valP.color = 0xFF00F0FF.toInt()
            canvas.drawText("Faz: $phase", w / 2f, h * 0.81f, valP)
            valP.color = 0xFFFFA500.toInt()
        } else {
            textP.textSize = h * 0.025f
            canvas.drawText("Parmaginizi diyagramda gezdirin -> sicaklik/basinca gore fazi gorun", w / 2f, h * 0.78f, textP)
        }

        // Explanation
        textP.textSize = h * 0.02f; textP.color = 0xFF666666.toInt()
        canvas.drawText("Faz diyagrami: Bir maddenin hangi sicaklik ve basincta kati, sivi veya gaz oldugunu gosterir", w / 2f, h * 0.86f, textP)
        canvas.drawText("Cizgiler = faz siniri | Uclu nokta = uc faz birden | Kritik nokta = sivi-gaz ayrimin bittigi nokta", w / 2f, h * 0.89f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        labelP.textSize = h * 0.03f
        canvas.drawText(subNames[substance], w / 2f, h * 0.93f, labelP)
        canvas.restore()
    }

    companion object {
        private val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    }
}

class PhaseDiagramFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ll = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0D1117.toInt()) }
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(8, 8, 8, 4)
        }
        headerRow.addView(TextView(requireContext()).apply {
            text = "Faz Diyagrami"; setTextColor(0xFF00F0FF.toInt())
            textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val helpBtnPhase = android.widget.Button(requireContext()).apply {
            text = "?"; textSize = 20f; setTextColor(-0x1)
            backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
            layoutParams = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt())
        }
        headerRow.addView(helpBtnPhase)
        ll.addView(headerRow)
        val view = PhaseDiagramView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (650 * resources.displayMetrics.density).toInt())
        }
        ll.addView(view)
        val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 4, 8, 4) }
        listOf("Su (H2O)", "CO2").forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; setTextColor(-0x1); setPadding(8, 4, 8, 4)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(6, 4, 6, 4) }
                setOnClickListener { view.setSubstance(i) }; row.addView(this)
            }
        }
        ll.addView(row)
        ll.addView(TextView(requireContext()).apply {
            text = "Ne ise yarar? → Bir maddeyi belirli T ve P'de hangi fazda oldugunu bulmak icin. Ornek: Su 100°C'de 1 atm'de kaynar (sivi->gaz)"
            setTextColor(0xFFAAAAAA.toInt()); textSize = 12f;             gravity = android.view.Gravity.CENTER; setPadding(8, 4, 8, 8)
        })
        helpBtnPhase.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Faz Diyagrami")
                .setMessage("Bir maddenin sicaklik ve basinc deger gore hangi fazda oldugunu gosterir.\n\n" +
                    "- Su ve CO2 icin faz diyagramlari mevcuttur\n" +
                    "- Diyagram uzerinde gezinerek faz sinirlarini gorebilirsiniz\n" +
                    "- Katı/sivi/gaz bolgeleri renklerle ayirtilmistir\n" +
                    "- Uct noktalar ve kritik nokta isaretlenmistir\n\n" +
                    "Ornek: Su 100°C'de 1 atm'de kaynar (sivi->gaz donusumu).")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return ll
    }
}
