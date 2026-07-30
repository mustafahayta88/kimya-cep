package com.kimya.uygulama.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class AASSimulatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class ElementInfo(
        val symbol: String, val name: String, val atomicNumber: Int,
        val wavelength: String, val wavelengthNm: Float,
        val hclColor: Int, val flameColor: Int,
        val sensitivity: Float, val detectionLimit: String,
        val description: String
    )

    companion object {
        val ELEMENTS = listOf(
            ElementInfo("Cu", "Bakır", 29, "324.8 nm", 324.8f,
                Color.rgb(0, 240, 200), Color.rgb(30, 220, 140), 0.08f, "0.001 ppm", "Bakır analizi"),
            ElementInfo("Zn", "Çinko", 30, "213.9 nm", 213.9f,
                Color.rgb(100, 180, 255), Color.rgb(120, 200, 255), 0.015f, "0.0005 ppm", "UV absorpsiyon"),
            ElementInfo("Fe", "Demir", 26, "248.3 nm", 248.3f,
                Color.rgb(255, 160, 60), Color.rgb(255, 180, 80), 0.04f, "0.003 ppm", "Demir tayini"),
            ElementInfo("Pb", "Kurşun", 82, "217.0 nm", 217.0f,
                Color.rgb(180, 200, 240), Color.rgb(200, 220, 255), 0.06f, "0.01 ppm", "Çevre analizi"),
            ElementInfo("Cd", "Kadmiyum", 48, "228.8 nm", 228.8f,
                Color.rgb(220, 220, 220), Color.rgb(240, 240, 240), 0.003f, "0.0001 ppm", "Ultra hassas"),
            ElementInfo("Mn", "Manganez", 25, "279.5 nm", 279.5f,
                Color.rgb(200, 140, 255), Color.rgb(220, 170, 255), 0.025f, "0.002 ppm", "Manganez"),
            ElementInfo("Ni", "Nikel", 28, "232.0 nm", 232.0f,
                Color.rgb(120, 220, 120), Color.rgb(150, 250, 150), 0.05f, "0.005 ppm", "Nikel tayini"),
            ElementInfo("Cr", "Krom", 24, "357.9 nm", 357.9f,
                Color.rgb(160, 240, 160), Color.rgb(190, 255, 190), 0.035f, "0.003 ppm", "Krom analizi"),
            ElementInfo("Ca", "Kalsiyum", 20, "422.7 nm", 422.7f,
                Color.rgb(255, 180, 80), Color.rgb(255, 200, 110), 0.06f, "0.005 ppm", "Kalsiyum"),
            ElementInfo("Na", "Sodyum", 11, "589.0 nm", 589.0f,
                Color.rgb(255, 220, 60), Color.rgb(255, 240, 90), 0.01f, "0.001 ppm", "Sodyum"),
            ElementInfo("K", "Potasyum", 19, "766.5 nm", 766.5f,
                Color.rgb(220, 140, 255), Color.rgb(240, 170, 255), 0.02f, "0.005 ppm", "Potasyum"),
            ElementInfo("Mg", "Magnezyum", 12, "285.2 nm", 285.2f,
                Color.rgb(240, 240, 140), Color.rgb(255, 255, 160), 0.005f, "0.0005 ppm", "Magnezyum"),
            ElementInfo("Ag", "Gümüş", 47, "328.1 nm", 328.1f,
                Color.rgb(220, 220, 240), Color.rgb(200, 200, 255), 0.07f, "0.001 ppm", "Gümüş analizi"),
            ElementInfo("Au", "Altın", 79, "242.8 nm", 242.8f,
                Color.rgb(255, 215, 80), Color.rgb(255, 200, 60), 0.09f, "0.01 ppm", "Altın tayini")
        )
    }

    // State parameters
    var currentElement = ELEMENTS[0]
        private set
    var sampleId = "Sample_01"
    var sampleConcentration = 6.17f // ppm

    var airValveOpen = false
    var airPressure = 0.0f // bar
    var airFlow = 5.0f // L/min

    var c2h2ValveOpen = false
    var c2h2Pressure = 0.0f // bar
    var c2h2Flow = 1.2f // L/min

    var lampCurrent = 0.0f // mA
    var wavelengthNm = 324.8f
    var slitWidthNm = 0.7f
    var readModeAbs = true
    var autoBaselineOffset = 0f

    var flameIgnited = false
    var burnerHead = "5 cm"
    var fuelType = "Air-Acetylene"

    var isMeasuring = false
    var isAspirating = false

    var currentAbsorbance = 0.000f
        private set
    var currentConcentrationReadout = 0.0f
        private set
    var currentTransmittancePercent = 100.0f
        private set

    // Logs
    val systemLogs = mutableListOf<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Callbacks
    var onSelectSampleRequested: (() -> Unit)? = null

    // Touch hit regions
    private val hitAirValve = RectF()
    private val hitC2h2Valve = RectF()
    private val hitAirKnob = RectF()
    private val hitC2h2Knob = RectF()
    private val hitLampCurrentKnob = RectF()
    private val hitWavelengthUp = RectF()
    private val hitWavelengthDown = RectF()
    private val hitSlitUp = RectF()
    private val hitSlitDown = RectF()
    private val hitReadModeAbs = RectF()
    private val hitReadModePctT = RectF()
    private val hitBaselineAuto = RectF()
    private val hitIgnite = RectF()
    private val hitExtinguish = RectF()
    private val hitSelectSample = RectF()
    private val hitBurnerHead = RectF()
    private val hitFuel = RectF()
    private val hitBlank = RectF()
    private val hitMeasure = RectF()
    private val hitStop = RectF()

    // Drag tracking
    private var activeDragTarget: String? = null
    private var lastTouchY = 0f

    // Visual animation clock
    private var animTime = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            animTime += 0.05f
            updatePhysics()
            invalidate()
            handler.postDelayed(this, 30L)
        }
    }

    // Paints
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ledTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    private val gaugePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val tempRect = RectF()

    private var textScale = 1.0f
    private fun sp(v: Float) = v * textScale

    init {
        addLog("System initialized.")
        addLog("Lamp selected: ${currentElement.symbol}")
        addLog("Wavelength set to ${String.format("%.1f", wavelengthNm)} nm")
        addLog("System is OFF - Open valves to start")
    }

    fun addLog(msg: String) {
        val timeStr = timeFormat.format(Date())
        systemLogs.add("[$timeStr] $msg")
        if (systemLogs.size > 15) {
            systemLogs.removeAt(0)
        }
    }

    fun setElement(element: ElementInfo, conc: Float = 6.17f, sampleName: String = "Sample_01") {
        currentElement = element
        wavelengthNm = element.wavelengthNm
        sampleConcentration = conc
        sampleId = sampleName
        addLog("Sample selected: $sampleId (${element.symbol})")
        addLog("Resonant line: ${element.wavelength}")
        invalidate()
    }

    private fun updatePhysics() {
        val targetAirP = if (airValveOpen) 4.5f else 0.0f
        airPressure += (targetAirP - airPressure) * 0.12f

        val targetC2h2P = if (c2h2ValveOpen) 0.8f else 0.0f
        c2h2Pressure += (targetC2h2P - c2h2Pressure) * 0.12f

        if (flameIgnited && (airPressure < 1.5f || c2h2Pressure < 0.3f)) {
            flameIgnited = false
            addLog("Flame extinguished: Low gas pressure!")
        }

        val flameTemp = if (flameIgnited) 2300f + (c2h2Flow / (airFlow + 0.1f)) * 300f else 25f
        val ready = flameIgnited && airPressure > 2.0f && c2h2Pressure > 0.4f && lampCurrent > 1.0f

        if (ready) {
            val wavelengthMatch = 1.0f - (abs(wavelengthNm - currentElement.wavelengthNm) / 10f).coerceIn(0f, 1f)
            val slitFactor = (slitWidthNm / 0.7f).coerceIn(0.5f, 1.5f)
            val lampFactor = (lampCurrent / 10.0f).coerceIn(0.2f, 1.3f)

            var rawAbs = currentElement.sensitivity * sampleConcentration * wavelengthMatch * slitFactor * lampFactor
            rawAbs += (Random.nextFloat() - 0.5f) * 0.003f
            rawAbs = (rawAbs - autoBaselineOffset).coerceIn(0.000f, 2.500f)

            if (isMeasuring || isAspirating) {
                currentAbsorbance += (rawAbs - currentAbsorbance) * 0.18f
            } else {
                currentAbsorbance += (0.001f - currentAbsorbance) * 0.1f
            }

            currentTransmittancePercent = 10f.pow(-currentAbsorbance) * 100f
            currentConcentrationReadout = if (currentElement.sensitivity > 0) currentAbsorbance / currentElement.sensitivity else 0f
        } else {
            currentAbsorbance += (0.000f - currentAbsorbance) * 0.2f
            currentTransmittancePercent = 100f
            currentConcentrationReadout = 0f
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = y
                activeDragTarget = null

                if (hitAirValve.contains(x, y)) {
                    airValveOpen = !airValveOpen
                    addLog("Air valve: " + (if (airValveOpen) "OPEN" else "CLOSED"))
                    invalidate(); return true
                }
                if (hitC2h2Valve.contains(x, y)) {
                    c2h2ValveOpen = !c2h2ValveOpen
                    addLog("C₂H₂ valve: " + (if (c2h2ValveOpen) "OPEN" else "CLOSED"))
                    invalidate(); return true
                }
                if (hitAirKnob.contains(x, y)) { activeDragTarget = "AIR_KNOB"; return true }
                if (hitC2h2Knob.contains(x, y)) { activeDragTarget = "C2H2_KNOB"; return true }
                if (hitLampCurrentKnob.contains(x, y)) { activeDragTarget = "LAMP_KNOB"; return true }

                if (hitWavelengthUp.contains(x, y)) {
                    wavelengthNm = (wavelengthNm + 0.1f).coerceAtMost(900.0f)
                    addLog("Wavelength: ${String.format("%.1f", wavelengthNm)} nm")
                    invalidate(); return true
                }
                if (hitWavelengthDown.contains(x, y)) {
                    wavelengthNm = (wavelengthNm - 0.1f).coerceAtLeast(190.0f)
                    addLog("Wavelength: ${String.format("%.1f", wavelengthNm)} nm")
                    invalidate(); return true
                }
                if (hitSlitUp.contains(x, y)) {
                    slitWidthNm = when (slitWidthNm) {
                        0.2f -> 0.7f; 0.7f -> 1.4f; 1.4f -> 2.0f; else -> 2.0f
                    }
                    addLog("Slit width: $slitWidthNm nm")
                    invalidate(); return true
                }
                if (hitSlitDown.contains(x, y)) {
                    slitWidthNm = when (slitWidthNm) {
                        2.0f -> 1.4f; 1.4f -> 0.7f; 0.7f -> 0.2f; else -> 0.2f
                    }
                    addLog("Slit width: $slitWidthNm nm")
                    invalidate(); return true
                }
                if (hitReadModeAbs.contains(x, y)) { readModeAbs = true; invalidate(); return true }
                if (hitReadModePctT.contains(x, y)) { readModeAbs = false; invalidate(); return true }

                if (hitBaselineAuto.contains(x, y)) {
                    autoBaselineOffset += currentAbsorbance
                    addLog("Baseline auto-corrected.")
                    invalidate(); return true
                }
                if (hitIgnite.contains(x, y)) {
                    if (airPressure >= 2.0f && c2h2Pressure >= 0.4f) {
                        flameIgnited = true
                        addLog("Flame ignited successfully.")
                    } else {
                        addLog("Ignition failed: Check gas pressure!")
                    }
                    invalidate(); return true
                }
                if (hitExtinguish.contains(x, y)) {
                    flameIgnited = false
                    addLog("Flame extinguished.")
                    invalidate(); return true
                }
                if (hitSelectSample.contains(x, y)) { onSelectSampleRequested?.invoke(); return true }
                if (hitBurnerHead.contains(x, y)) {
                    burnerHead = if (burnerHead == "5 cm") "10 cm" else "5 cm"
                    addLog("Burner head: $burnerHead")
                    invalidate(); return true
                }
                if (hitFuel.contains(x, y)) {
                    fuelType = if (fuelType == "Air-Acetylene") "N₂O-Acetylene" else "Air-Acetylene"
                    addLog("Fuel gas: $fuelType")
                    invalidate(); return true
                }
                if (hitBlank.contains(x, y)) {
                    autoBaselineOffset = 0f
                    isAspirating = false
                    isMeasuring = false
                    addLog("Blank measured: Absorbance zeroed.")
                    invalidate(); return true
                }
                if (hitMeasure.contains(x, y)) {
                    isMeasuring = true
                    isAspirating = true
                    addLog("Measuring sample $sampleId...")
                    invalidate(); return true
                }
                if (hitStop.contains(x, y)) {
                    isMeasuring = false
                    isAspirating = false
                    addLog("Measurement stopped.")
                    invalidate(); return true
                }

            }
            MotionEvent.ACTION_MOVE -> {
                val dy = lastTouchY - y
                lastTouchY = y

                when (activeDragTarget) {
                    "AIR_KNOB" -> { airFlow = (airFlow + dy * 0.05f).coerceIn(0.0f, 10.0f); invalidate() }
                    "C2H2_KNOB" -> { c2h2Flow = (c2h2Flow + dy * 0.02f).coerceIn(0.0f, 3.0f); invalidate() }
                    "LAMP_KNOB" -> { lampCurrent = (lampCurrent + dy * 0.1f).coerceIn(0.0f, 15.0f); invalidate() }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { activeDragTarget = null }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        textScale = (w / 800f).coerceIn(1.0f, 2.2f)

        bgPaint.color = Color.rgb(8, 10, 15)
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val topBarH = h * 0.075f
        val contentH = h - topBarH

        drawTopHeaderBar(canvas, 0f, 0f, w, topBarH)
        drawMainContentArea(canvas, 0f, topBarH, w, contentH)
    }

    // -------------------------------------------------------------------------------------
    // TOP HEADER BAR (Skeuomorphic Chrome & LED Bar)
    // -------------------------------------------------------------------------------------
    private fun drawTopHeaderBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        panelPaint.shader = LinearGradient(x, y, x, y + h,
            intArrayOf(Color.rgb(32, 40, 52), Color.rgb(18, 22, 30)), null, Shader.TileMode.CLAMP)
        rect.set(x, y, x + w, y + h)
        canvas.drawRect(rect, panelPaint)
        panelPaint.shader = null

        borderPaint.color = Color.rgb(60, 75, 95)
        borderPaint.strokeWidth = 2f
        canvas.drawLine(x, y + h, x + w, y + h, borderPaint)

        val airOk = airPressure >= 2.0f
        val c2h2Ok = c2h2Pressure >= 0.4f
        val lampOk = lampCurrent > 1.0f
        val ready = airOk && c2h2Ok && lampOk && flameIgnited

        val leds = listOf(
            Pair("POWER", true),
            Pair("AIR", airOk),
            Pair("C₂H₂", c2h2Ok),
            Pair("LAMP", lampOk),
            Pair("FLAME", flameIgnited),
            Pair("READY", ready)
        )

        val ledSpacing = w * 0.155f
        val ledRadius = h * 0.22f
        var startLedX = w / 2f - (leds.size * ledSpacing) / 2f

        textPaint.textSize = h * 0.34f
        textPaint.isFakeBoldText = true
        textPaint.textAlign = Paint.Align.CENTER

        for ((name, active) in leds) {
            val cx = startLedX + ledRadius
            val cy = y + h * 0.48f

            gaugePaint.style = Paint.Style.FILL
            gaugePaint.color = Color.rgb(70, 82, 100)
            canvas.drawCircle(cx, cy, ledRadius + 2.5f, gaugePaint)

            if (active) {
                gaugePaint.shader = RadialGradient(cx, cy, ledRadius * 2.2f,
                    intArrayOf(Color.argb(120, 0, 255, 120), Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, ledRadius * 2.2f, gaugePaint)
                gaugePaint.shader = null

                gaugePaint.color = Color.rgb(0, 255, 100)
                canvas.drawCircle(cx, cy, ledRadius, gaugePaint)

                gaugePaint.color = Color.WHITE
                canvas.drawCircle(cx - ledRadius * 0.3f, cy - ledRadius * 0.3f, ledRadius * 0.32f, gaugePaint)
            } else {
                gaugePaint.color = Color.rgb(15, 45, 25)
                canvas.drawCircle(cx, cy, ledRadius, gaugePaint)
            }

            textPaint.color = if (active) Color.WHITE else Color.rgb(120, 140, 160)
            canvas.drawText(name, cx, cy + ledRadius + h * 0.18f, textPaint)

            startLedX += ledSpacing
        }


    }

    // -------------------------------------------------------------------------------------
    // MAIN CONTENT AREA
    // -------------------------------------------------------------------------------------
    private fun drawMainContentArea(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val pL = w * 0.205f
        val pR = w * 0.245f
        val pCenter = w - pL - pR

        val leftTopH = h * 0.66f
        val leftBotH = h - leftTopH

        drawGasControlsPanel(canvas, x + 2f, y + 2f, pL - 4f, leftTopH - 4f)

        drawSampleIntroductionPanel(canvas, x + 2f, y + leftTopH, pL - 4f, leftBotH - 2f)

        val centerTopH = h * 0.72f
        drawPhotorealisticCenterInstrument(canvas, x + pL + 1f, y + 2f, pCenter - 2f, centerTopH - 4f)

        drawCenterBottomControls(canvas, x + pL + 1f, y + centerTopH, pCenter - 2f, h - centerTopH - 2f)

        drawRightPanel(canvas, x + pL + pCenter + 1f, y + 2f, pR - 3f, h - 4f)
    }

    // -------------------------------------------------------------------------------------
    // 1. GAS CONTROLS PANEL (Pipes, Metallic Valves & Gauges)
    // -------------------------------------------------------------------------------------
    private fun drawGasControlsPanel(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val innerY = y + h * 0.035f
        val sectionH = (h - h * 0.04f) / 3f

        // --- AIR SECTION ---
        textPaint.color = Color.rgb(0, 210, 255)
        textPaint.textSize = sp(16f)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true
        canvas.drawText("AIR", x + 12f, innerY + 18f, textPaint)

        // Metallic Pipe
        drawMetallicPipe(canvas, x + 20f, innerY + sectionH * 0.52f, x + w - 20f, innerY + sectionH * 0.52f, 8f)

        // Blue Glossy 3D Valve
        val valveRadius = sectionH * 0.32f
        val airValveCx = x + w * 0.28f
        val airValveCy = innerY + sectionH * 0.52f
        hitAirValve.set(airValveCx - valveRadius, airValveCy - valveRadius, airValveCx + valveRadius, airValveCy + valveRadius)
        draw3DValveWheel(canvas, airValveCx, airValveCy, valveRadius, Color.rgb(0, 140, 255), airValveOpen)

        // Air Pressure Gauge
        val gaugeCx = x + w * 0.72f
        val gaugeCy = innerY + sectionH * 0.45f
        val gaugeR = sectionH * 0.38f
        draw3DPressureGauge(canvas, gaugeCx, gaugeCy, gaugeR, airPressure, 10.0f, "bar")

        // Digital readout
        rect.set(gaugeCx - 24f, gaugeCy + gaugeR + 3f, gaugeCx + 24f, gaugeCy + gaugeR + 19f)
        panelPaint.color = Color.BLACK
        canvas.drawRoundRect(rect, 3f, 3f, panelPaint)
        borderPaint.color = Color.rgb(40, 60, 80)
        canvas.drawRoundRect(rect, 3f, 3f, borderPaint)

        ledTextPaint.color = if (airValveOpen) Color.rgb(0, 255, 100) else Color.rgb(100, 100, 100)
        ledTextPaint.textSize = sp(15f)
        ledTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(String.format("%.1f bar", airPressure), rect.centerX(), rect.centerY() + 4f, ledTextPaint)

        // --- C2H2 SECTION ---
        val c2h2Y = innerY + sectionH
        textPaint.color = Color.rgb(255, 90, 80)
        textPaint.textSize = sp(12f)
        canvas.drawText("ACETYLENE (C₂H₂)", x + 12f, c2h2Y + 14f, textPaint)

        drawMetallicPipe(canvas, x + 20f, c2h2Y + sectionH * 0.52f, x + w - 20f, c2h2Y + sectionH * 0.52f, 8f)

        val c2h2ValveCx = x + w * 0.28f
        val c2h2ValveCy = c2h2Y + sectionH * 0.52f
        hitC2h2Valve.set(c2h2ValveCx - valveRadius, c2h2ValveCy - valveRadius, c2h2ValveCx + valveRadius, c2h2ValveCy + valveRadius)
        draw3DValveWheel(canvas, c2h2ValveCx, c2h2ValveCy, valveRadius, Color.rgb(230, 40, 30), c2h2ValveOpen)

        val gauge2Cx = x + w * 0.72f
        val gauge2Cy = c2h2Y + sectionH * 0.45f
        draw3DPressureGauge(canvas, gauge2Cx, gauge2Cy, gaugeR, c2h2Pressure, 3.0f, "bar")

        rect.set(gauge2Cx - 24f, gauge2Cy + gaugeR + 3f, gauge2Cx + 24f, gauge2Cy + gaugeR + 19f)
        panelPaint.color = Color.BLACK
        canvas.drawRoundRect(rect, 3f, 3f, panelPaint)
        canvas.drawRoundRect(rect, 3f, 3f, borderPaint)

        ledTextPaint.color = if (c2h2ValveOpen) Color.rgb(0, 255, 100) else Color.rgb(100, 100, 100)
        canvas.drawText(String.format("%.1f bar", c2h2Pressure), rect.centerX(), rect.centerY() + 4f, ledTextPaint)

        // --- FLOW ADJUST SECTION ---
        val flowY = innerY + sectionH * 2.0f
        textPaint.color = Color.rgb(180, 200, 220)
        textPaint.textSize = sp(11f)
        canvas.drawText("FLOW ADJUST", x + 12f, flowY + 12f, textPaint)

        val knobR = sectionH * 0.28f
        val knob1Cx = x + w * 0.30f
        val knob1Cy = flowY + sectionH * 0.55f
        hitAirKnob.set(knob1Cx - knobR, knob1Cy - knobR, knob1Cx + knobR, knob1Cy + knobR)
        draw3DKnob(canvas, knob1Cx, knob1Cy, knobR, airFlow / 10.0f, "AIR FLOW", Color.rgb(0, 180, 255))

        val knob2Cx = x + w * 0.70f
        val knob2Cy = flowY + sectionH * 0.55f
        hitC2h2Knob.set(knob2Cx - knobR, knob2Cy - knobR, knob2Cx + knobR, knob2Cy + knobR)
        draw3DKnob(canvas, knob2Cx, knob2Cy, knobR, c2h2Flow / 3.0f, "C₂H₂ FLOW", Color.rgb(255, 80, 70))
    }

    private fun drawMetallicPipe(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float) {
        panelPaint.shader = LinearGradient(x1, y1 - thickness / 2f, x1, y1 + thickness / 2f,
            intArrayOf(Color.rgb(50, 60, 75), Color.rgb(200, 215, 235), Color.rgb(30, 40, 55)),
            null, Shader.TileMode.CLAMP)
        rect.set(x1, y1 - thickness / 2f, x2, y2 + thickness / 2f)
        canvas.drawRect(rect, panelPaint)
        panelPaint.shader = null
    }

    private fun draw3DValveWheel(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, open: Boolean) {
        // Shadow
        gaugePaint.style = Paint.Style.FILL
        gaugePaint.color = Color.rgb(10, 12, 16)
        canvas.drawCircle(cx + 2f, cy + 2f, radius, gaugePaint)

        // Outer Wheel Ring Gradient
        val activeColor = if (open) color else Color.rgb(70, 75, 85)
        gaugePaint.shader = RadialGradient(cx, cy, radius,
            intArrayOf(activeColor, Color.rgb(20, 25, 32)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, gaugePaint)
        gaugePaint.shader = null

        // Specular Arc
        borderPaint.color = Color.argb(120, 255, 255, 255)
        borderPaint.strokeWidth = 2f
        tempRect.set(cx - radius * 0.85f, cy - radius * 0.85f, cx + radius * 0.85f, cy + radius * 0.85f)
        canvas.drawArc(tempRect, 200f, 100f, false, borderPaint)

        // Spokes
        borderPaint.color = Color.rgb(210, 225, 240)
        borderPaint.strokeWidth = 3f
        val angleOffset = if (open) animTime * 2f else 0f
        for (i in 0 until 4) {
            val ang = i * (Math.PI / 2.0) + angleOffset
            val x2 = cx + cos(ang).toFloat() * radius * 0.82f
            val y2 = cy + sin(ang).toFloat() * radius * 0.82f
            canvas.drawLine(cx, cy, x2, y2, borderPaint)
        }

        // Center Chrome Nut
        gaugePaint.shader = LinearGradient(cx - radius * 0.3f, cy - radius * 0.3f, cx + radius * 0.3f, cy + radius * 0.3f,
            intArrayOf(Color.rgb(240, 250, 255), Color.rgb(120, 135, 155)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius * 0.32f, gaugePaint)
        gaugePaint.shader = null
    }

    private fun draw3DPressureGauge(canvas: Canvas, cx: Float, cy: Float, radius: Float, valVal: Float, maxVal: Float, unit: String) {
        // Outer Chrome Bezel
        gaugePaint.style = Paint.Style.FILL
        gaugePaint.shader = LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
            intArrayOf(Color.rgb(230, 240, 255), Color.rgb(100, 115, 135), Color.rgb(200, 215, 235)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, gaugePaint)

        // Inner Dark Face
        gaugePaint.shader = RadialGradient(cx, cy, radius * 0.88f,
            intArrayOf(Color.rgb(28, 34, 44), Color.rgb(12, 15, 20)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius * 0.88f, gaugePaint)
        gaugePaint.shader = null

        // Ticks & Numbers
        borderPaint.color = Color.WHITE
        borderPaint.strokeWidth = 1.5f
        val startAng = 135.0
        val sweepAng = 270.0

        for (i in 0..10) {
            val frac = i / 10.0
            val ang = Math.toRadians(startAng + frac * sweepAng)
            val innerR = radius * (if (i % 5 == 0) 0.65f else 0.75f)
            val outerR = radius * 0.84f

            val x1 = cx + cos(ang).toFloat() * innerR
            val y1 = cy + sin(ang).toFloat() * innerR
            val x2 = cx + cos(ang).toFloat() * outerR
            val y2 = cy + sin(ang).toFloat() * outerR
            canvas.drawLine(x1, y1, x2, y2, borderPaint)
        }

        // 3D Red Needle
        val currentFrac = (valVal / maxVal).coerceIn(0f, 1f)
        val needleAng = Math.toRadians(startAng + currentFrac * sweepAng)
        val needleLen = radius * 0.72f

        borderPaint.color = Color.RED
        borderPaint.strokeWidth = 2.5f
        val nx = cx + cos(needleAng).toFloat() * needleLen
        val ny = cy + sin(needleAng).toFloat() * needleLen
        canvas.drawLine(cx, cy, nx, ny, borderPaint)

        // Center Cap
        gaugePaint.color = Color.rgb(220, 230, 245)
        canvas.drawCircle(cx, cy, radius * 0.16f, gaugePaint)

        // Glass Reflection Arc
        borderPaint.color = Color.argb(80, 255, 255, 255)
        borderPaint.strokeWidth = 3f
        tempRect.set(cx - radius * 0.8f, cy - radius * 0.8f, cx + radius * 0.8f, cy + radius * 0.8f)
        canvas.drawArc(tempRect, 200f, 80f, false, borderPaint)
    }

    private fun draw3DKnob(canvas: Canvas, cx: Float, cy: Float, radius: Float, frac: Float, label: String, activeColor: Int) {
        // Knob Body Shadow
        gaugePaint.style = Paint.Style.FILL
        gaugePaint.color = Color.rgb(10, 12, 16)
        canvas.drawCircle(cx + 2f, cy + 2f, radius, gaugePaint)

        // Knurled Body Gradient
        gaugePaint.shader = RadialGradient(cx, cy, radius,
            intArrayOf(Color.rgb(75, 88, 105), Color.rgb(25, 30, 40)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, gaugePaint)
        gaugePaint.shader = null

        // Indicator Line
        val startAng = 135.0
        val sweepAng = 270.0
        val ang = Math.toRadians(startAng + frac.coerceIn(0f, 1f) * sweepAng)

        borderPaint.color = activeColor
        borderPaint.strokeWidth = 3.5f
        val px = cx + cos(ang).toFloat() * radius * 0.80f
        val py = cy + sin(ang).toFloat() * radius * 0.80f
        canvas.drawLine(cx, cy, px, py, borderPaint)

        textPaint.color = Color.rgb(160, 180, 200)
        textPaint.textSize = sp(9.5f)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, cx, cy + radius + 11f, textPaint)
    }

    // -------------------------------------------------------------------------------------
    // 2. SAMPLE INTRODUCTION PANEL
    // -------------------------------------------------------------------------------------
    private fun drawSampleIntroductionPanel(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val innerY = y + h * 0.04f
        val segW = w / 4f

        val items = listOf("SAMPLE", "NEBULIZER", "SPRAY CHAMBER", "DRAIN")
        textPaint.textSize = sp(8.5f)
        textPaint.color = Color.rgb(180, 200, 220)
        textPaint.textAlign = Paint.Align.CENTER

        for (i in 0 until 4) {
            val cx = x + segW * (i + 0.5f)
            val cy = innerY + (h - 22f) * 0.42f

            drawIntroductionIcon(canvas, cx, cy, segW * 0.35f, i)
            canvas.drawText(items[i], cx, cy + segW * 0.45f + 8f, textPaint)

            if (i < 3) {
                val arrowX = cx + segW * 0.38f
                borderPaint.color = if (isAspirating) Color.rgb(0, 240, 255) else Color.rgb(80, 100, 120)
                borderPaint.strokeWidth = 2f
                canvas.drawLine(arrowX, cy, arrowX + segW * 0.24f, cy, borderPaint)

                if (isAspirating) {
                    val dropOffset = (animTime * 15f) % (segW * 0.24f)
                    gaugePaint.color = Color.rgb(0, 240, 255)
                    canvas.drawCircle(arrowX + dropOffset, cy, 2.5f, gaugePaint)
                }
            }
        }
    }

    private fun drawIntroductionIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, type: Int) {
        panelPaint.style = Paint.Style.FILL
        panelPaint.color = Color.rgb(28, 36, 48)
        rect.set(cx - size, cy - size, cx + size, cy + size)
        canvas.drawRoundRect(rect, 4f, 4f, panelPaint)

        borderPaint.color = Color.rgb(65, 82, 105)
        borderPaint.strokeWidth = 1.5f
        canvas.drawRoundRect(rect, 4f, 4f, borderPaint)

        gaugePaint.style = Paint.Style.FILL
        when (type) {
            0 -> {
                gaugePaint.color = Color.rgb(0, 160, 240)
                rect.set(cx - size * 0.6f, cy - size * 0.2f, cx + size * 0.6f, cy + size * 0.7f)
                canvas.drawRect(rect, gaugePaint)
            }
            1 -> {
                gaugePaint.color = Color.rgb(190, 205, 225)
                canvas.drawCircle(cx, cy, size * 0.5f, gaugePaint)
            }
            2 -> {
                gaugePaint.color = Color.argb(120, 200, 230, 255)
                canvas.drawCircle(cx, cy, size * 0.65f, gaugePaint)
            }
            3 -> {
                gaugePaint.color = Color.rgb(0, 120, 200)
                rect.set(cx - size * 0.5f, cy + size * 0.1f, cx + size * 0.5f, cy + size * 0.7f)
                canvas.drawRect(rect, gaugePaint)
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // 3. PHOTOREALISTIC CENTER INSTRUMENT BODY
    // -------------------------------------------------------------------------------------
    private fun drawPhotorealisticCenterInstrument(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        // Brushed Silver Metallic Chassis Frame
        panelPaint.shader = LinearGradient(x, y, x, y + h,
            intArrayOf(Color.rgb(235, 242, 250), Color.rgb(175, 188, 205), Color.rgb(110, 122, 138), Color.rgb(200, 212, 228)),
            null, Shader.TileMode.CLAMP)
        rect.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 8f, 8f, panelPaint)
        panelPaint.shader = null

        borderPaint.color = Color.rgb(90, 102, 118)
        borderPaint.strokeWidth = 3f
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val segW = w / 3f
        val pad = h * 0.012f

        // --- COMPARTMENT A: HOLLOW CATHODE LAMP ---
        rect.set(x + pad, y + pad, x + segW - pad, y + h - pad)
        panelPaint.color = Color.rgb(20, 25, 34)
        canvas.drawRoundRect(rect, 6f, 6f, panelPaint)

        textPaint.color = Color.rgb(200, 215, 230)
        textPaint.textSize = sp(10.5f)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        canvas.drawText("HOLLOW CATHODE LAMP", rect.centerX(), y + h * 0.035f, textPaint)

        drawPhotorealisticLamp(canvas, rect.centerX(), y + h * 0.30f, rect.width() * 0.78f, h * 0.28f)

        val lampPanelY = y + h * 0.56f
        textPaint.textSize = sp(8.5f)
        textPaint.color = Color.rgb(150, 170, 190)
        canvas.drawText("LAMP CURRENT", rect.centerX(), lampPanelY, textPaint)

        val dispW = (rect.width() * 0.55f).coerceAtMost(84f)
        val dispY = lampPanelY + h * 0.03f
        rect.set(rect.centerX() - dispW / 2f, dispY, rect.centerX() + dispW / 2f, dispY + h * 0.038f)
        panelPaint.color = Color.BLACK
        canvas.drawRoundRect(rect, 4f, 4f, panelPaint)

        ledTextPaint.color = Color.rgb(0, 255, 100)
        ledTextPaint.textSize = sp(14f)
        ledTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(String.format("%.1f mA", lampCurrent), rect.centerX(), rect.centerY() + 4.5f, ledTextPaint)

        val knobCx = rect.centerX()
        val knobCy = dispY + h * 0.038f + h * 0.07f
        val knobR = (h * 0.07f).coerceIn(26f, 50f)
        hitLampCurrentKnob.set(knobCx - knobR, knobCy - knobR, knobCx + knobR, knobCy + knobR)
        draw3DKnob(canvas, knobCx, knobCy, knobR, lampCurrent / 15.0f, "", Color.rgb(0, 255, 100))

        // --- COMPARTMENT B: FLAME & BURNER CHAMBER ---
        rect.set(x + segW + pad, y + pad, x + segW * 2f - pad, y + h - pad)
        panelPaint.color = Color.rgb(12, 15, 20)
        canvas.drawRoundRect(rect, 6f, 6f, panelPaint)

        borderPaint.color = Color.rgb(55, 68, 85)
        borderPaint.strokeWidth = 2f
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val flameCx = rect.centerX()
        val flameBaseY = y + h * 0.72f
        drawBurnerAssemblyAndFlame(canvas, flameCx, flameBaseY, rect.width() * 0.8f, h * 0.65f)

        if (lampCurrent > 1.0f) {
            val beamY = flameBaseY - h * 0.28f
            flamePaint.style = Paint.Style.STROKE
            flamePaint.strokeWidth = 3f
            flamePaint.color = currentElement.hclColor
            canvas.drawLine(x + pad, beamY, x + w - pad, beamY, flamePaint)
        }

        // --- COMPARTMENT C: CONTROL PANEL ---
        rect.set(x + segW * 2f + pad, y + pad, x + w - pad, y + h - pad)
        panelPaint.shader = LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
            intArrayOf(Color.rgb(230, 240, 252), Color.rgb(190, 202, 218)), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, 6f, 6f, panelPaint)
        panelPaint.shader = null

        val innerPad = rect.width() * 0.04f
        val ctrlContentTop = rect.top
        val titleH = h * 0.035f
        val subtitleH = h * 0.025f
        val titleAreaH = titleH + subtitleH + h * 0.015f

        textPaint.color = Color.rgb(20, 30, 45)
        textPaint.textSize = sp(12f)
        textPaint.isFakeBoldText = true
        canvas.drawText("AAS-2000", rect.centerX(), ctrlContentTop + titleH, textPaint)
        textPaint.textSize = sp(8f)
        textPaint.isFakeBoldText = false
        canvas.drawText("DUAL BEAM FLAME AAS", rect.centerX(), ctrlContentTop + titleH + subtitleH, textPaint)

        val ctrlY = ctrlContentTop + titleAreaH
        val availableH = rect.height() - titleAreaH - h * 0.01f
        val itemH = availableH / 4f

        drawDigitalControlBox(canvas, rect.left + innerPad, ctrlY, rect.width() - innerPad * 2f, itemH,
            "WAVELENGTH", String.format("%.1f nm", wavelengthNm), hitWavelengthUp, hitWavelengthDown)

        drawDigitalControlBox(canvas, rect.left + innerPad, ctrlY + itemH, rect.width() - innerPad * 2f, itemH,
            "SLIT WIDTH", String.format("%.1f nm", slitWidthNm), hitSlitUp, hitSlitDown)

        val modeY = ctrlY + itemH * 2.0f
        textPaint.textSize = sp(8f)
        textPaint.color = Color.rgb(40, 50, 65)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("READ MODE", rect.left + innerPad, modeY + itemH * 0.25f, textPaint)

        val btnW = (rect.width() - innerPad * 3f) / 2f
        val toggleH = itemH * 0.35f
        hitReadModeAbs.set(rect.left + innerPad, modeY + itemH * 0.35f, rect.left + innerPad + btnW, modeY + itemH * 0.35f + toggleH)
        hitReadModePctT.set(rect.left + innerPad * 2f + btnW, modeY + itemH * 0.35f, rect.left + innerPad * 2f + btnW * 2f, modeY + itemH * 0.35f + toggleH)

        drawToggleButton(canvas, hitReadModeAbs, "ABS", readModeAbs)
        drawToggleButton(canvas, hitReadModePctT, "%T", !readModeAbs)

        val baseY = ctrlY + itemH * 3.0f
        textPaint.textSize = sp(8f)
        canvas.drawText("BASELINE CORRECTION", rect.left + innerPad, baseY + itemH * 0.25f, textPaint)

        hitBaselineAuto.set(rect.left + innerPad, baseY + itemH * 0.35f, rect.right - innerPad, baseY + itemH * 0.35f + toggleH)
        draw3DButton(canvas, hitBaselineAuto, "AUTO", Color.rgb(50, 62, 78), Color.WHITE)
    }

    private fun drawPhotorealisticLamp(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        // Glass Capsule Outer
        panelPaint.style = Paint.Style.FILL
        panelPaint.color = Color.argb(90, 210, 230, 255)
        rect.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        canvas.drawRoundRect(rect, 10f, 10f, panelPaint)

        borderPaint.color = Color.rgb(170, 190, 215)
        borderPaint.strokeWidth = 1.5f
        canvas.drawRoundRect(rect, 10f, 10f, borderPaint)

        // End Caps
        panelPaint.color = Color.rgb(180, 192, 208)
        canvas.drawRect(cx - w / 2f - 7f, cy - h / 2f + 3f, cx - w / 2f, cy + h / 2f - 3f, panelPaint)
        canvas.drawRect(cx + w / 2f, cy - h / 2f + 3f, cx + w / 2f + 7f, cy + h / 2f - 3f, panelPaint)

        // Internal Plasma Discharge Glow
        if (lampCurrent > 0.5f) {
            val alpha = (lampCurrent / 15.0f * 220).toInt().coerceIn(40, 255)
            gaugePaint.style = Paint.Style.FILL
            gaugePaint.color = Color.argb(alpha, Color.red(currentElement.hclColor), Color.green(currentElement.hclColor), Color.blue(currentElement.hclColor))
            canvas.drawCircle(cx, cy, h * 0.40f, gaugePaint)

            gaugePaint.color = Color.argb(255, 255, 255, 220)
            canvas.drawCircle(cx, cy, h * 0.18f, gaugePaint)
        }
    }

    private fun drawBurnerAssemblyAndFlame(canvas: Canvas, cx: Float, baseY: Float, w: Float, h: Float) {
        panelPaint.style = Paint.Style.FILL
        panelPaint.color = Color.rgb(160, 175, 195)
        rect.set(cx - 10f, baseY - h * 0.4f, cx + 10f, baseY)
        canvas.drawRect(rect, panelPaint)

        rect.set(cx - w * 0.38f, baseY - h * 0.42f, cx + w * 0.38f, baseY - h * 0.38f)
        canvas.drawRect(rect, panelPaint)

        if (flameIgnited) {
            val flameH = h * 0.65f
            val burnerTop = baseY - h * 0.42f
            val flameTopY = burnerTop - flameH
            val flickerX = sin(animTime * 4.7f) * w * 0.04f + sin(animTime * 7.3f) * w * 0.02f
            val flickerW = 1f + sin(animTime * 5.1f) * 0.06f + cos(animTime * 3.8f) * 0.04f

            // --- GLOW (outer aura) ---
            gaugePaint.shader = RadialGradient(cx + flickerX, burnerTop - flameH * 0.35f, w * 0.55f,
                intArrayOf(Color.argb(40, 255, 180, 60), Color.argb(15, 255, 120, 20), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx + flickerX, burnerTop - flameH * 0.35f, w * 0.55f, gaugePaint)
            gaugePaint.shader = null

            // --- OUTER FLAME (yellow-orange, large) ---
            path.reset()
            path.moveTo(cx - w * 0.34f * flickerW, burnerTop)
            path.cubicTo(
                cx - w * 0.48f * flickerW, burnerTop - flameH * 0.35f,
                cx - w * 0.20f + flickerX, burnerTop - flameH * 0.75f,
                cx + flickerX * 1.2f, flameTopY + flameH * 0.12f
            )
            path.cubicTo(
                cx + w * 0.20f + flickerX, burnerTop - flameH * 0.75f,
                cx + w * 0.48f * flickerW, burnerTop - flameH * 0.35f,
                cx + w * 0.34f * flickerW, burnerTop
            )
            path.close()

            flamePaint.shader = LinearGradient(cx, burnerTop, cx, flameTopY,
                intArrayOf(Color.argb(200, 255, 160, 30), Color.argb(180, 255, 200, 60), Color.argb(100, 255, 240, 150)),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, flamePaint)
            flamePaint.shader = null

            // --- MIDDLE FLAME (bright yellow, narrower) ---
            path.reset()
            path.moveTo(cx - w * 0.22f * flickerW, burnerTop)
            path.cubicTo(
                cx - w * 0.30f * flickerW, burnerTop - flameH * 0.40f,
                cx - w * 0.12f + flickerX * 0.8f, burnerTop - flameH * 0.82f,
                cx + flickerX * 0.9f, flameTopY + flameH * 0.22f
            )
            path.cubicTo(
                cx + w * 0.12f + flickerX * 0.8f, burnerTop - flameH * 0.82f,
                cx + w * 0.30f * flickerW, burnerTop - flameH * 0.40f,
                cx + w * 0.22f * flickerW, burnerTop
            )
            path.close()

            flamePaint.shader = LinearGradient(cx, burnerTop, cx, flameTopY,
                intArrayOf(Color.argb(230, 255, 220, 80), Color.argb(255, 255, 255, 200), Color.argb(180, 255, 255, 240)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, flamePaint)
            flamePaint.shader = null

            // --- INNER BLUE CORE (hot zone) ---
            path.reset()
            path.moveTo(cx - w * 0.12f * flickerW, burnerTop)
            path.cubicTo(
                cx - w * 0.16f * flickerW, burnerTop - flameH * 0.30f,
                cx - w * 0.06f + flickerX * 0.5f, burnerTop - flameH * 0.55f,
                cx + flickerX * 0.5f, burnerTop - flameH * 0.48f
            )
            path.cubicTo(
                cx + w * 0.06f + flickerX * 0.5f, burnerTop - flameH * 0.55f,
                cx + w * 0.16f * flickerW, burnerTop - flameH * 0.30f,
                cx + w * 0.12f * flickerW, burnerTop
            )
            path.close()

            flamePaint.shader = LinearGradient(cx, burnerTop, cx, burnerTop - flameH * 0.55f,
                intArrayOf(Color.argb(200, 20, 120, 255), Color.argb(160, 40, 180, 255), Color.argb(80, 100, 220, 255)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawPath(path, flamePaint)
            flamePaint.shader = null

            // --- HOTTEST CORE (bright white-blue) ---
            path.reset()
            path.moveTo(cx - w * 0.06f, burnerTop)
            path.cubicTo(
                cx - w * 0.08f, burnerTop - flameH * 0.20f,
                cx - w * 0.03f + flickerX * 0.3f, burnerTop - flameH * 0.35f,
                cx + flickerX * 0.3f, burnerTop - flameH * 0.30f
            )
            path.cubicTo(
                cx + w * 0.03f + flickerX * 0.3f, burnerTop - flameH * 0.35f,
                cx + w * 0.08f, burnerTop - flameH * 0.20f,
                cx + w * 0.06f, burnerTop
            )
            path.close()
            flamePaint.color = Color.argb(160, 200, 230, 255)
            canvas.drawPath(path, flamePaint)

            // --- ELEMENT COLOR TINT (when aspirating) ---
            if (isAspirating) {
                gaugePaint.shader = RadialGradient(cx + flickerX, burnerTop - flameH * 0.45f, w * 0.25f,
                    intArrayOf(Color.argb(120, Color.red(currentElement.flameColor), Color.green(currentElement.flameColor), Color.blue(currentElement.flameColor)),
                        Color.argb(40, Color.red(currentElement.flameColor), Color.green(currentElement.flameColor), Color.blue(currentElement.flameColor)),
                        Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP)
                canvas.drawCircle(cx + flickerX, burnerTop - flameH * 0.45f, w * 0.25f, gaugePaint)
                gaugePaint.shader = null
            }

            // --- HEAT SHIMMER (subtle wavy lines above flame) ---
            if (lampCurrent > 1.0f) {
                flamePaint.style = Paint.Style.STROKE
                flamePaint.strokeWidth = 1f
                for (i in 0 until 3) {
                    val shimmerY = flameTopY - h * 0.03f - i * h * 0.025f
                    val shimmerAlpha = (60 - i * 20).coerceAtLeast(10)
                    flamePaint.color = Color.argb(shimmerAlpha, 150, 180, 200)
                    path.reset()
                    path.moveTo(cx - w * 0.15f, shimmerY)
                    for (j in 0..8) {
                        val px = cx - w * 0.15f + (w * 0.3f) * (j / 8f)
                        val py = shimmerY + sin(animTime * 6f + j * 1.2f + i * 0.8f) * h * 0.012f
                        path.lineTo(px, py)
                    }
                    canvas.drawPath(path, flamePaint)
                }
                flamePaint.style = Paint.Style.FILL
            }
        }
    }

    private fun drawDigitalControlBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, valStr: String, upRect: RectF, downRect: RectF) {
        textPaint.textSize = sp(8f)
        textPaint.color = Color.rgb(40, 50, 65)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = false
        canvas.drawText(label, x + 2f, y + 9f, textPaint)

        val boxW = w * 0.70f
        rect.set(x, y + 13f, x + boxW, y + h - 3f)
        panelPaint.color = Color.BLACK
        canvas.drawRoundRect(rect, 4f, 4f, panelPaint)

        ledTextPaint.color = Color.rgb(0, 255, 100)
        ledTextPaint.textSize = sp(11.5f)
        ledTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(valStr, rect.centerX(), rect.centerY() + 4f, ledTextPaint)

        val btnX = x + boxW + 4f
        val btnW = w - boxW - 4f
        val btnH = (h - 17f) / 2f

        upRect.set(btnX, y + 13f, btnX + btnW, y + 13f + btnH)
        downRect.set(btnX, y + 17f + btnH, btnX + btnW, y + h - 3f)

        draw3DButton(canvas, upRect, "▲", Color.rgb(55, 68, 85), Color.WHITE)
        draw3DButton(canvas, downRect, "▼", Color.rgb(55, 68, 85), Color.WHITE)
    }

    private fun drawToggleButton(canvas: Canvas, rectF: RectF, text: String, active: Boolean) {
        panelPaint.color = if (active) Color.rgb(0, 200, 100) else Color.rgb(65, 75, 90)
        canvas.drawRoundRect(rectF, 4f, 4f, panelPaint)

        textPaint.color = if (active) Color.BLACK else Color.rgb(200, 210, 220)
        textPaint.textSize = sp(9.5f)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        canvas.drawText(text, rectF.centerX(), rectF.centerY() + 3.5f, textPaint)
    }

    private fun draw3DButton(canvas: Canvas, rectF: RectF, text: String, bgColor: Int, textColor: Int) {
        panelPaint.shader = LinearGradient(rectF.left, rectF.top, rectF.left, rectF.bottom,
            intArrayOf(bgColor, Color.rgb((Color.red(bgColor) * 0.7f).toInt(), (Color.green(bgColor) * 0.7f).toInt(), (Color.blue(bgColor) * 0.7f).toInt())), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rectF, 4f, 4f, panelPaint)
        panelPaint.shader = null

        borderPaint.color = Color.argb(80, 255, 255, 255)
        borderPaint.strokeWidth = 1f
        canvas.drawRoundRect(rectF, 4f, 4f, borderPaint)

        textPaint.color = textColor
        textPaint.textSize = sp(9.5f)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        canvas.drawText(text, rectF.centerX(), rectF.centerY() + 3.5f, textPaint)
    }

    // -------------------------------------------------------------------------------------
    // 4. CENTER BOTTOM CONTROLS
    // -------------------------------------------------------------------------------------
    private fun drawCenterBottomControls(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val segW = w / 3f

        // --- Box 1: SAMPLE ---
        rect.set(x, y, x + segW - 3f, y + h)
        drawSkeuomorphicPanelBox(canvas, rect.left, rect.top, rect.width(), rect.height(), "SAMPLE")

        val sampleCy = rect.centerY()
        val boxLeft = x + w * 0.01f
        val sampleBoxW = (w / 3f) * 0.15f
        panelPaint.color = Color.rgb(28, 36, 48)
        rect.set(boxLeft, sampleCy - h * 0.18f, boxLeft + sampleBoxW, sampleCy + h * 0.18f)
        canvas.drawRoundRect(rect, 4f, 4f, panelPaint)
        gaugePaint.color = Color.rgb(0, 140, 220)
        canvas.drawRect(boxLeft + h * 0.02f, sampleCy - h * 0.04f, boxLeft + sampleBoxW - h * 0.02f, sampleCy + h * 0.16f, gaugePaint)

        textPaint.textSize = sp(10f)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = false
        val textX = boxLeft + sampleBoxW + h * 0.04f
        canvas.drawText("Sample ID: $sampleId", textX, sampleCy - h * 0.06f, textPaint)
        canvas.drawText("Element: ${currentElement.symbol}", textX, sampleCy + h * 0.10f, textPaint)

        hitSelectSample.set(x + 6f, y + h * 0.65f, x + segW - 8f, y + h - 4f)
        draw3DButton(canvas, hitSelectSample, "SELECT SAMPLE", Color.rgb(40, 60, 85), Color.WHITE)

        // --- Box 2: BURNER ---
        val x2 = x + segW + 2f
        rect.set(x2, y, x2 + segW - 3f, y + h)
        drawSkeuomorphicPanelBox(canvas, rect.left, rect.top, rect.width(), rect.height(), "BURNER")

        val burnInnerX = x2 + w * 0.01f
        val burnBtnW = segW - w * 0.03f
        textPaint.textSize = sp(9f)
        textPaint.color = Color.rgb(160, 180, 200)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("BURNER HEAD", burnInnerX, y + h * 0.28f, textPaint)
        hitBurnerHead.set(burnInnerX, y + h * 0.33f, burnInnerX + burnBtnW, y + h * 0.49f)
        draw3DButton(canvas, hitBurnerHead, burnerHead, Color.rgb(45, 55, 70), Color.WHITE)

        canvas.drawText("FUEL", burnInnerX, y + h * 0.58f, textPaint)
        hitFuel.set(burnInnerX, y + h * 0.63f, burnInnerX + burnBtnW, y + h * 0.80f)
        draw3DButton(canvas, hitFuel, fuelType, Color.rgb(45, 55, 70), Color.WHITE)

        // --- Box 3: IGNITION ---
        val x3 = x + segW * 2f + 2f
        rect.set(x3, y, x3 + segW, y + h)
        drawSkeuomorphicPanelBox(canvas, rect.left, rect.top, rect.width(), rect.height(), "IGNITION")

        val ignInnerX = x3 + w * 0.01f
        val ignBtnW = segW - w * 0.03f
        hitIgnite.set(ignInnerX, y + h * 0.28f, ignInnerX + ignBtnW, y + h * 0.50f)
        draw3DButton(canvas, hitIgnite, "IGNITE", if (flameIgnited) Color.rgb(220, 100, 20) else Color.rgb(180, 80, 20), Color.WHITE)

        hitExtinguish.set(ignInnerX, y + h * 0.55f, ignInnerX + ignBtnW, y + h - h * 0.05f)
        draw3DButton(canvas, hitExtinguish, "EXTINGUISH", Color.rgb(60, 70, 80), Color.rgb(200, 210, 220))
    }

    // -------------------------------------------------------------------------------------
    // 5. RIGHT PANEL (MEASUREMENT, SPECTRUM, LOG, MEASURE ACTIONS)
    // -------------------------------------------------------------------------------------
    private fun drawRightPanel(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val gap = h * 0.003f
        val boxH1 = h * 0.22f
        val boxH2 = h * 0.28f
        val boxH3 = h * 0.32f
        val boxH4 = h - boxH1 - boxH2 - boxH3 - gap * 3f

        // --- Box 1: MEASUREMENT ---
        drawSkeuomorphicPanelBox(canvas, x, y, w, boxH1, "MEASUREMENT")

        textPaint.textSize = sp(8f)
        textPaint.color = Color.rgb(160, 180, 200)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = false

        val labelStr = if (readModeAbs) "ABSORBANCE" else "TRANSMITTANCE"
        val valStr = if (readModeAbs) String.format("%.3f A", currentAbsorbance) else String.format("%.1f %%T", currentTransmittancePercent)

        canvas.drawText(labelStr, x + 10f, y + boxH1 * 0.18f, textPaint)
        ledTextPaint.color = Color.rgb(0, 255, 100)
        ledTextPaint.textSize = sp(17.5f)
        ledTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(valStr, x + w - 10f, y + boxH1 * 0.22f, ledTextPaint)

        canvas.drawText("CONCENTRATION", x + 10f, y + boxH1 * 0.52f, textPaint)
        ledTextPaint.textSize = sp(17.5f)
        canvas.drawText(String.format("%.2f ppm", currentConcentrationReadout), x + w - 10f, y + boxH1 * 0.56f, ledTextPaint)

        // --- Box 2: SPECTRUM ---
        val y2 = y + boxH1 + gap
        drawSkeuomorphicPanelBox(canvas, x, y2, w, boxH2, "SPECTRUM")
        drawMiniSpectrumChart(canvas, x + 6f, y2 + 17f, w - 12f, boxH2 - 22f)

        // --- Box 3: SYSTEM LOG ---
        val y3 = y2 + boxH2 + gap
        drawSkeuomorphicPanelBox(canvas, x, y3, w, boxH3, "SYSTEM LOG")
        drawTerminalLog(canvas, x + 5f, y3 + 17f, w - 10f, boxH3 - 21f)

        // --- Box 4: MEASURE CONTROLS ---
        val y4 = y3 + boxH3 + gap
        drawSkeuomorphicPanelBox(canvas, x, y4, w, boxH4, "MEASURE")

        val btnW = (w - 20f) / 3f
        val btnH = boxH4 - 22f
        val btnY = y4 + 17f

        hitBlank.set(x + 5f, btnY, x + 5f + btnW, btnY + btnH)
        hitMeasure.set(x + 10f + btnW, btnY, x + 10f + btnW * 2f, btnY + btnH)
        hitStop.set(x + 15f + btnW * 2f, btnY, x + w - 5f, btnY + btnH)

        draw3DButton(canvas, hitBlank, "BLANK", Color.rgb(0, 90, 180), Color.WHITE)
        draw3DButton(canvas, hitMeasure, "MEASURE", Color.rgb(0, 160, 80), Color.WHITE)
        draw3DButton(canvas, hitStop, "STOP", Color.rgb(180, 40, 40), Color.WHITE)
    }

    private fun drawMiniSpectrumChart(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        panelPaint.color = Color.BLACK
        rect.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 3f, 3f, panelPaint)

        borderPaint.color = Color.rgb(20, 50, 30)
        borderPaint.strokeWidth = 1f
        for (i in 1..3) {
            val gy = y + h * (i / 4f)
            canvas.drawLine(x, gy, x + w, gy, borderPaint)
        }
        for (i in 1..4) {
            val gx = x + w * (i / 5f)
            canvas.drawLine(gx, y, gx, y + h, borderPaint)
        }

        path.reset()
        val numPoints = 60
        val centerIndex = ((wavelengthNm - 190f) / (800f - 190f) * numPoints).toInt().coerceIn(5, numPoints - 5)

        for (i in 0..numPoints) {
            val px = x + w * (i / numPoints.toFloat())
            val dist = abs(i - centerIndex)
            val peakH = currentAbsorbance * h * 0.70f * exp(-(dist * dist) / 12f)
            val py = (y + h - 4f) - peakH

            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }

        flamePaint.style = Paint.Style.STROKE
        flamePaint.strokeWidth = 2f
        flamePaint.color = Color.rgb(0, 255, 100)
        canvas.drawPath(path, flamePaint)

        textPaint.textSize = sp(7f)
        textPaint.color = Color.rgb(120, 140, 160)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("190", x + 2f, y + h - 2f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("800 nm", x + w - 2f, y + h - 2f, textPaint)
    }

    private fun drawTerminalLog(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        panelPaint.color = Color.BLACK
        rect.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 3f, 3f, panelPaint)

        textPaint.textSize = sp(8.5f)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = false

        val maxLines = (h / 11.5f).toInt().coerceAtLeast(1)
        val visibleLogs = systemLogs.takeLast(maxLines)

        var logY = y + 9.5f
        for (line in visibleLogs) {
            textPaint.color = if (line.contains("READY") || line.contains("ignited")) Color.rgb(0, 255, 120)
            else if (line.contains("failed") || line.contains("extinguished")) Color.rgb(255, 100, 100)
            else Color.rgb(160, 200, 160)

            canvas.drawText(line, x + 4f, logY, textPaint)
            logY += 10.5f
        }
    }

    // -------------------------------------------------------------------------------------
    // 6. BOTTOM STATUS BAR
    // -------------------------------------------------------------------------------------
    private fun drawBottomStatusBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        panelPaint.color = Color.rgb(14, 18, 25)
        rect.set(x, y, x + w, y + h)
        canvas.drawRect(rect, panelPaint)

        borderPaint.color = Color.rgb(35, 45, 60)
        borderPaint.strokeWidth = 1.5f
        canvas.drawLine(x, y, x + w, y, borderPaint)


    }

    private fun drawSkeuomorphicPanelBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, title: String) {
        panelPaint.shader = LinearGradient(x, y, x, y + h,
            intArrayOf(Color.rgb(24, 30, 42), Color.rgb(14, 18, 25)), null, Shader.TileMode.CLAMP)
        rect.set(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 5f, 5f, panelPaint)
        panelPaint.shader = null

        borderPaint.color = Color.rgb(48, 60, 80)
        borderPaint.strokeWidth = 1.5f
        canvas.drawRoundRect(rect, 5f, 5f, borderPaint)

        textPaint.color = Color.rgb(170, 190, 215)
        textPaint.textSize = sp(9f)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.isFakeBoldText = true
        canvas.drawText(title, x + 8f, y + 12.5f, textPaint)
    }
}
