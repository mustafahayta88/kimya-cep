package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.*

class PhaseDiagramView(context: Context) : View(context) {
    private var substance = 0
    private var cursorX = 0f; private var cursorY = 0f
    private var showCursor = false
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private var animTime = 0f
    private var showInfo = false
    private val sDetector: ScaleGestureDetector
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private data class SubData(val name: String, val tripleT: Double, val tripleP: Double, val critT: Double, val critP: Double, val solid: String, val liquid: String, val gas: String, val formula: String)

    private val substances = listOf(
        SubData("Su (H2O)", 0.01, 0.006, 374.0, 218.0, "Buz", "Su", "Buhar", "H2O"),
        SubData("Karbondioksit (CO₂)", -56.6, 5.11, 31.0, 73.0, "Kuru Buz", "Sivi CO₂", "CO₂ Gazı", "CO₂")
    )

    init {
        isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                0 -> { lastTx = e.x; lastTy = e.y; tMode = 1 }
                2 -> { val dx = e.x - lastTx; val dy = e.y - lastTy; if (abs(dx) > 5 || abs(dy) > 5) tMode = 2; if (tMode == 2) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y; invalidate() } }
                1, 3 -> { tMode = 0; if (cursorX > 0 && cursorY > 0) { showCursor = true } }
            }
            if (e.action == MotionEvent.ACTION_MOVE && tMode == 1 && zoomScale <= 1.05f) {
                cursorX = e.x; cursorY = e.y; showCursor = true; invalidate()
            }
            true
        }
    }

    fun setSubstance(i: Int) { substance = i.coerceIn(0, substances.size - 1); cursorX = 0f; cursorY = 0f; showCursor = false; invalidate() }
    fun toggleInfo() { showInfo = !showInfo; invalidate() }

    private fun tToX(t: Double, dLeft: Float, dw: Float, tMin: Double, tMax: Double) = dLeft + dw * ((t - tMin) / (tMax - tMin)).toFloat()
    private fun pToY(p: Double, dTop: Float, dh: Float, pMin: Double, pMax: Double) = (dTop + dh - dh * ((p - pMin) / (pMax - pMin))).toFloat()
    private fun curveSG(t: Double, d: SubData) = d.critP * 0.12 * exp((t - d.tripleT) * 0.02)
    private fun curveLG(t: Double, d: SubData) = d.critP * 0.25 * exp((t - d.tripleT) * 0.05)
    private fun curveSL(t: Double, d: SubData) = d.critP * (0.002 + max(0.0, (t - d.tripleT) * 0.008))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.rgb(10, 14, 23))

        // Header
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 20f; hp.textAlign = Paint.Align.CENTER; hp.color = Color.rgb(0, 240, 255); hp.isFakeBoldText = true; hp.isAntiAlias = true
        canvas.drawText("Faz Diyagramı Simülatörü", w / 2f, 28f, hp)

        // Main diagram area
        val dLeft = w * 0.1f; val dRight = w * 0.92f; val dTop = 45f; val dBot = h * 0.68f
        val dw = dRight - dLeft; val dh = dBot - dTop

        val d = substances[substance]
        val tMin = if (substance == 0) -80.0 else -100.0; val tMax = d.critT + 30.0
        val pMin = 0.0; val pMax = d.critP * 1.3

        // Background card
        val cardP = Paint(Paint.ANTI_ALIAS_FLAG); cardP.color = Color.rgb(22, 27, 34); cardP.isAntiAlias = true
        val csP = Paint(Paint.ANTI_ALIAS_FLAG); csP.style = Paint.Style.STROKE; csP.strokeWidth = 2f; csP.color = Color.rgb(48, 54, 61); csP.isAntiAlias = true
        canvas.drawRoundRect(dLeft, dTop, dRight, dBot, 12f, 12f, cardP)
        canvas.drawRoundRect(dLeft, dTop, dRight, dBot, 12f, 12f, csP)

        // Grid lines
        val gridP = Paint(Paint.ANTI_ALIAS_FLAG); gridP.style = Paint.Style.STROKE; gridP.strokeWidth = 1f; gridP.color = Color.argb(20, 255, 255, 255)
        for (i in 1..5) { val gx = dLeft + dw * i / 5f; canvas.drawLine(gx, dTop + 1, gx, dBot - 1, gridP) }
        for (i in 1..4) { val gy = dTop + dh * i / 5f; canvas.drawLine(dLeft + 1, gy, dRight - 1, gy, gridP) }

        // Fill phase regions
        fun tToX(t: Double) = tToX(t, dLeft, dw, tMin, tMax)
        fun pToY(p: Double) = pToY(p, dTop, dh, pMin, pMax)
        fun fSG(t: Double) = curveSG(t, d)
        fun fLG(t: Double) = curveLG(t, d)
        fun fSL(t: Double) = curveSL(t, d)

        // Solid region fill
        val solidFill = Paint(Paint.ANTI_ALIAS_FLAG); solidFill.style = Paint.Style.FILL; solidFill.isAntiAlias = true
        solidFill.color = Color.argb(25, 100, 100, 255)
        val solidPath = Path()
        solidPath.moveTo(tToX(tMin), pToY(pMax))
        for (i in 0..200) { val t = tMin + (tMax - tMin) * i / 200.0; solidPath.lineTo(tToX(t), pToY(fSG(t))) }
        solidPath.lineTo(tToX(tMin), pToY(pMin)); solidPath.close()
        canvas.drawPath(solidPath, solidFill)

        // Liquid region fill
        solidFill.color = Color.argb(25, 100, 255, 100)
        val liquidPath = Path()
        liquidPath.moveTo(tToX(d.tripleT), pToY(fSL(d.tripleT)))
        for (i in 0..200) { val t = tMin + (tMax - tMin) * i / 200.0; liquidPath.lineTo(tToX(t), pToY(fLG(t))) }
        for (i in 200 downTo 0) { val t = tMin + (tMax - tMin) * i / 200.0; liquidPath.lineTo(tToX(t), pToY(fSL(t))) }
        liquidPath.close()
        canvas.drawPath(liquidPath, solidFill)

        // Gas region fill
        solidFill.color = Color.argb(25, 255, 255, 100)
        val gasPath = Path()
        gasPath.moveTo(tToX(tMin), pToY(0.0))
        for (i in 0..200) { val t = tMin + (tMax - tMin) * i / 200.0; gasPath.lineTo(tToX(t), pToY(fSG(t))) }
        for (i in 200 downTo 0) { val t = tMin + (tMax - tMin) * i / 200.0; gasPath.lineTo(tToX(t), pToY(fLG(t))) }
        gasPath.close()
        canvas.drawPath(gasPath, solidFill)

        // Draw boundary curves
        val lineP = Paint(Paint.ANTI_ALIAS_FLAG); lineP.style = Paint.Style.STROKE; lineP.strokeWidth = 3f; lineP.isAntiAlias = true; lineP.pathEffect = CornerPathEffect(3f)
        val pathSG = Path(); val pathLG = Path(); val pathSL = Path()
        val n = 200
        for (i in 0..n) {
            val t = tMin + (tMax - tMin) * i / n.toDouble()
            val x = tToX(t)
            val ySG = pToY(fSG(t)); val yLG = pToY(fLG(t)); val ySL = pToY(fSL(t))
            if (i == 0) { pathSG.moveTo(x, ySG); pathLG.moveTo(x, yLG); pathSL.moveTo(x, ySL) }
            else { pathSG.lineTo(x, ySG); pathLG.lineTo(x, yLG); pathSL.lineTo(x, ySL) }
        }
        lineP.color = Color.rgb(100, 150, 255); canvas.drawPath(pathSG, lineP)
        lineP.color = Color.rgb(100, 255, 150); canvas.drawPath(pathLG, lineP)
        lineP.color = Color.rgb(255, 150, 100); canvas.drawPath(pathSL, lineP)

        // Phase labels
        val tp = Paint(Paint.ANTI_ALIAS_FLAG); tp.textSize = 18f; tp.textAlign = Paint.Align.CENTER; tp.isFakeBoldText = true; tp.isAntiAlias = true
        tp.color = Color.rgb(130, 160, 255); canvas.drawText(d.solid, dLeft + dw * 0.12f, dTop + dh * 0.25f, tp)
        tp.color = Color.rgb(100, 255, 160); canvas.drawText(d.liquid, dLeft + dw * 0.4f, dTop + dh * 0.55f, tp)
        tp.color = Color.rgb(255, 240, 100); canvas.drawText(d.gas, dLeft + dw * 0.72f, dTop + dh * 0.22f, tp)

        // Triple point
        val tpX = tToX(d.tripleT); val tpY = pToY(d.tripleP)
        val dotP = Paint(Paint.ANTI_ALIAS_FLAG); dotP.style = Paint.Style.FILL; dotP.isAntiAlias = true
        dotP.color = Color.rgb(255, 0, 128); canvas.drawCircle(tpX, tpY, 8f, dotP)
        dotP.color = Color.argb(80, 255, 0, 128); canvas.drawCircle(tpX, tpY, 14f, dotP)
        val dp = Paint(Paint.ANTI_ALIAS_FLAG); dp.textSize = 12f; dp.textAlign = Paint.Align.CENTER; dp.color = Color.rgb(255, 100, 180); dp.isAntiAlias = true
        canvas.drawText("Üçlü Nokta", tpX, tpY - 18f, dp)
        canvas.drawText("${"%.1f".format(d.tripleT)}°C, ${"%.3f".format(d.tripleP)} atm", tpX, tpY - 6f, dp)

        // Critical point
        val crX = tToX(d.critT); val crY = pToY(d.critP)
        dotP.color = Color.rgb(255, 50, 50); canvas.drawCircle(crX, crY, 8f, dotP)
        dotP.color = Color.argb(80, 255, 50, 50); canvas.drawCircle(crX, crY, 14f, dotP)
        dp.color = Color.rgb(255, 80, 80)
        canvas.drawText("Kritik Nokta", crX, crY - 18f, dp)
        canvas.drawText("${"%.0f".format(d.critT)}°C, ${"%.0f".format(d.critP)} atm", crX, crY - 6f, dp)

        // Axis labels
        val ap = Paint(Paint.ANTI_ALIAS_FLAG); ap.textSize = 11f; ap.textAlign = Paint.Align.CENTER; ap.color = Color.rgb(140, 140, 140); ap.isAntiAlias = true
        for (i in 0..5) { val t = tMin + (tMax - tMin) * i / 5.0; canvas.drawText("${"%.0f".format(t)}", tToX(t), dBot + 14f, ap) }
        for (i in 0..4) { val p = pMin + (pMax - pMin) * i / 4.0; canvas.drawText("${"%.1f".format(p)}", dLeft - 28f, pToY(p) + 4f, ap) }
        canvas.drawText("Sıcaklık (°C)", dLeft + dw / 2, dBot + 28f, ap)
        canvas.save(); canvas.rotate(-90f, dLeft - 32f, dTop + dh / 2); canvas.drawText("Basınç (atm)", dLeft - 32f, dTop + dh / 2, ap); canvas.restore()

        // Cursor crosshair
        if (showCursor && cursorX >= dLeft && cursorX <= dRight && cursorY >= dTop && cursorY <= dBot) {
            val cursorP = Paint(Paint.ANTI_ALIAS_FLAG); cursorP.style = Paint.Style.STROKE; cursorP.strokeWidth = 1.5f; cursorP.color = Color.argb(80, 255, 255, 255); cursorP.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f); cursorP.isAntiAlias = true
            canvas.drawLine(cursorX, dTop, cursorX, dBot, cursorP)
            canvas.drawLine(dLeft, cursorY, dRight, cursorY, cursorP)
            val cT = tMin + (tMax - tMin) * (cursorX - dLeft) / dw
            val cP = pMin + (pMax - pMin) * (dBot - cursorY) / dh
            var phase = when {
                cT < d.tripleT -> if (cP > fSG(cT)) "Katı" else "Gaz"
                else -> when { cP > fSL(cT) -> "Katı"; cP > fLG(cT) -> "Sıvı"; else -> "Gaz" }
            }
            val infoBg = Paint(Paint.ANTI_ALIAS_FLAG); infoBg.color = Color.argb(180, 22, 27, 34); infoBg.isAntiAlias = true
            canvas.drawRoundRect(dLeft + 4f, dBot + 32f, dRight - 4f, dBot + 68f, 8f, 8f, infoBg)
            dp.textSize = 13f; dp.color = Color.rgb(0, 240, 255)
            canvas.drawText("T = ${"%.1f".format(cT)}°C  |  P = ${"%.3f".format(cP)} atm  |  Faz: $phase", dLeft + dw / 2, dBot + 52f, dp)
        } else {
            ap.textSize = 12f; ap.color = Color.rgb(100, 100, 100)
            canvas.drawText("Parmağınızı diyagramda gezdirin — sıcaklık/basınca göre fazı görün", dLeft + dw / 2, dBot + 50f, ap)
        }

        // Info panel
        if (showInfo) drawInfo(canvas, w, h)
    }

    private fun drawInfo(c: Canvas, w: Float, h: Float) {
        val px = w * 0.03f; val py = 8f; val pw = w * 0.94f; val ph = h - 16f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 39); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.rgb(0, 200, 255); isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 22f; hp.textAlign = Paint.Align.CENTER; hp.color = Color.rgb(0, 240, 255); hp.isFakeBoldText = true; hp.isAntiAlias = true
        c.drawText("Faz Diyagramı Simülatörü", w / 2f, ty, hp); ty += 38f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.textSize = 16f; lp.textAlign = Paint.Align.LEFT; lp.isAntiAlias = true
        val lines = listOf(
            Pair("═══ NEDİR? ═══", Color.rgb(0, 240, 255)),
            Pair("Faz diyagramı, bir maddenin hangi sıcaklık", Color.rgb(220, 220, 220)),
            Pair("ve basınçta katı, sıvı veya gaz olduğunu gösterir.", Color.rgb(220, 220, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ FAZLAR ═══", Color.rgb(0, 240, 255)),
            Pair("Katı: Düzenli moleküler yapı, sabit şekil", Color.rgb(130, 160, 255)),
            Pair("Sıvı: Serbest moleküler hareket, belirli hacim", Color.rgb(100, 255, 160)),
            Pair("Gaz: Serbest moleküler hareket, belirsiz hacim", Color.rgb(255, 240, 100)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ SINIR NOKTALARI ═══", Color.rgb(0, 240, 255)),
            Pair("Üçlü Nokta: 3 fazın bir arada olduğu T ve P", Color.rgb(255, 100, 180)),
            Pair("Kritik Nokta: Sıvı-gaz ayrımının bittiği T ve P", Color.rgb(255, 80, 80)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", Color.rgb(0, 240, 255)),
            Pair("1. Madde seçin (Su veya CO₂)", Color.rgb(200, 230, 255)),
            Pair("2. Diyagramda parmağınızı gezdirin", Color.rgb(200, 230, 255)),
            Pair("3. T ve P değerlerine göre fazı görün", Color.rgb(200, 230, 255)),
            Pair("4. Çimdikleme ile yakınlaştırın", Color.rgb(200, 230, 255))
        )
        for ((line, color) in lines) { if (line.isEmpty()) { ty += 6f; continue }; lp.color = color; c.drawText(line, px + 18f, ty, lp); ty += 22f }
    }
}

class PhaseDiagramFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10, 14, 23)); setPadding(12, 12, 12, 12) }
        val view = PhaseDiagramView(ctx)

        // Top bar
        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 4) }
        top.addView(TextView(ctx).apply { text = "Faz Diyagramı"; textSize = 22f; setTextColor(Color.rgb(0, 240, 255)); setTypeface(null, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val helpBtn = TextView(ctx).apply { text = "?"; textSize = 26f; setTextColor(Color.rgb(0, 240, 255)); setPadding(20, 8, 20, 8); setBackgroundColor(Color.rgb(20, 30, 50)) }
        helpBtn.setOnClickListener { view.toggleInfo() }
        top.addView(helpBtn)
        root.addView(top)

        root.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.65f).toInt())

        // Substance buttons
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 4) }
        fun subBtn(text: String, idx: Int): TextView = TextView(ctx).apply {
            this.text = text; textSize = 14f; setTextColor(Color.WHITE); setPadding(24, 12, 24, 12)
            setBackgroundColor(if (idx == 0) Color.rgb(0, 120, 200) else Color.rgb(0, 100, 160))
            setOnClickListener { view.setSubstance(idx) }
        }
        row.addView(subBtn("Su (H₂O)", 0))
        row.addView(subBtn("CO₂", 1).apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 12 })
        root.addView(row)

        // Bottom info
        root.addView(TextView(ctx).apply {
            text = "Üçlü noktada katı, sıvı ve gaz bir arada bulunur. Kritik noktanın ötesinde sıvı-gaz ayrımı ortadan kalkar."
            textSize = 11f; setTextColor(Color.rgb(100, 100, 100)); gravity = Gravity.CENTER; setPadding(0, 8, 0, 0)
        })

        return root
    }
}
