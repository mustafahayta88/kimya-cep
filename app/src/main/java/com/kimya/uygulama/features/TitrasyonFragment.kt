package com.kimya.uygulama.features

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.*

// ─── Data classes ──────────────────────────────────────────────────────────
enum class TitrationType(val label: String, val acid: String, val base: String, val acidKa: Float? = null, val baseKb: Float? = null) {
    SA_SB("Guclu Asit - Guclu Baz", "HCl", "NaOH"),
    SA_WB("Guclu Asit - Zayif Baz", "HCl", "NH3", baseKb = 1.8e-5f),
    WA_SB("Zayif Asit - Guclu Baz", "CH3COOH", "NaOH", acidKa = 1.8e-5f),
    WA_WB("Zayif Asit - Zayif Baz", "CH3COOH", "NH3", acidKa = 1.8e-5f, baseKb = 1.8e-5f)
}

enum class Indicator(val label: String) {
    FENOL("Fenolftalein"), METIL_ORANJ("Metil Oranj"), BROM_TIMOL("Bromtimol Mavisi")
}

private val ACID_OPTIONS = listOf("HCl", "HNO3", "H2SO4", "CH3COOH")
private val BASE_OPTIONS = listOf("NaOH", "KOH", "NH3")
private val CONC_OPTIONS = listOf(0.01f, 0.05f, 0.1f, 0.5f, 1f)
private val VOL_OPTIONS = listOf(10f, 25f, 50f, 100f)

// ─── Chemistry Engine ──────────────────────────────────────────────────────
private fun log10(v: Float) = ln(v) / ln(10f)

private fun calcPH(
    volTitrant: Float, titrantConc: Float, analyteConc: Float,
    analyteVol: Float, type: TitrationType, isAcidAnalyte: Boolean
): Float {
    val molAnalyte = analyteConc * analyteVol / 1000f
    val molTitrant = titrantConc * volTitrant / 1000f
    val totalVol = (analyteVol + volTitrant) / 1000f
    val molH: Float; val molOH: Float
    if (isAcidAnalyte) { molH = molAnalyte; molOH = molTitrant }
    else { molOH = molAnalyte; molH = molTitrant }
    val excessH = molH - molOH; val excessOH = molOH - molH

    return when (type) {
        TitrationType.SA_SB -> when {
            volTitrant < 0.001f -> -log10(analyteConc)
            excessH > 0 -> -log10(excessH / totalVol)
            excessOH > 0 -> 14f + log10(excessOH / totalVol)
            else -> 7f
        }
        TitrationType.SA_WB -> when {
            volTitrant < 0.001f -> -log10(analyteConc)
            excessH > 0 -> -log10(excessH / totalVol)
            molOH >= molH -> {
                if (type.baseKb != null) {
                    val Cb = excessOH / totalVol
                    if (Cb > 0) { val Kb = type.baseKb; 7f + 0.5f * (-log10(Kb) + log10(Cb)) }
                    else { val saltConc = molAnalyte / totalVol; val Ka = 1e-14f / type.baseKb!!; 7f - 0.5f * (-log10(Ka) - log10(saltConc)) }
                } else 14f + log10(excessOH / totalVol)
            }
            else -> -log10(excessH / totalVol)
        }
        TitrationType.WA_SB -> when {
            volTitrant < 0.001f -> { if (type.acidKa != null) 0.5f * (-log10(type.acidKa)) - 0.5f * log10(analyteConc) else 3f }
            molOH < molH -> { val Ka = type.acidKa ?: 1.8e-5f; -log10(Ka) + log10(molOH / (molH - molOH)) }
            molOH >= molH -> {
                if (molOH > molH) { val Cb = excessOH / totalVol; 14f + log10(Cb) }
                else { val saltConc = molH / totalVol; val Kb = 1e-14f / (type.acidKa ?: 1.8e-5f); 7f + 0.5f * (-log10(Kb) + log10(saltConc)) }
            }
            else -> 7f
        }
        TitrationType.WA_WB -> when {
            volTitrant < 0.001f -> { if (type.acidKa != null) 0.5f * (-log10(type.acidKa)) - 0.5f * log10(analyteConc) else 5f }
            excessH > 0 && excessOH <= 0 -> {
                val rem = molH - molOH; val form = molOH
                if (rem > 0 && form > 0) { val Ka = type.acidKa ?: 1.8e-5f; -log10(Ka) + log10(form / rem) } else 7f
            }
            else -> { val pKa = -log10(type.acidKa ?: 1.8e-5f); val pKb = -log10(type.baseKb ?: 1.8e-5f); 7f + 0.5f * (pKa - pKb) }
        }
    }
}

// ─── Custom View ──────────────────────────────────────────────────────────
class TitrationView(context: Context) : View(context) {

    // State
    private var volume = 0f
    private var animating = false
    private var dropProgress = -1f
    private var dropCount = 0
    private var wavePhase = 0f
    private var stirAngle = 0f
    private var splashPhase = -1f

    // Stopcock & erlen positions (set during onDraw)
    private var stopcockCY = 0f; private var stopcockR = 0f
    private var erlenBodyTop = 0f; private var erlenBodyBot = 0f
    private var erlenBodyLeft = 0f; private var erlenBodyRight = 0f
    private var erlenNeckL = 0f; private var erlenNeckR = 0f; private var erlenNeckH = 0f; private var erlenFlaskTopY = 0f

    // Settings
    var titrType = TitrationType.SA_SB
    private var _indicator = Indicator.FENOL
    var acidName = "HCl"; var baseName = "NaOH"
    var acidConc = 0.1f; var baseConc = 0.1f; private var _analyteVol = 50f
    var speedMul = 1
    var isAcidAnalyte = true
    var isAutoMode = false
    private var manualHold = false
    private var holdTimer = 0L

    // Curve data
    private val curvePoints = mutableListOf<Pair<Float, Float>>()

    // Paints
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E17.toInt() }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val valV = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }

    // Glass paints
    private val glassP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val glassStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }
    private val glassHigh = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FFFFFF.toInt(); isAntiAlias = true }
    private val glassDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000.toInt(); isAntiAlias = true }

    // Liquid paint
    private val liqP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }

    // Drop paint
    private val dropP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00E5FF.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val dropGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x3300E5FF.toInt(); style = Paint.Style.FILL; isAntiAlias = true }

    // Graph paints
    private val graphBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); isAntiAlias = true }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF222222.toInt(); strokeWidth = 0.8f }
    private val curveP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val curveFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1839FF14.toInt(); style = Paint.Style.FILL }
    private val faintP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); strokeWidth = 1.2f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val eqP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 1.5f; pathEffect = DashPathEffect(floatArrayOf(8f, 5f), 0f); style = Paint.Style.STROKE }
    private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val dotGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FF0080.toInt(); style = Paint.Style.FILL; isAntiAlias = true }

    // Sound
    private var soundPool: SoundPool? = null
    private var tickSoundId = 0
    private var loaded = false

    // Handlers
    private val handler = Handler(Looper.getMainLooper())
    private var autoRunnable: Runnable? = null
    private var holdRunnable: Runnable? = null

    private val eqVol: Float get() = if (isAcidAnalyte) acidConc * _analyteVol / baseConc else baseConc * _analyteVol / acidConc
    private val currentPH: Float get() = calcPH(volume, if (isAcidAnalyte) baseConc else acidConc, if (isAcidAnalyte) acidConc else baseConc, _analyteVol, titrType, isAcidAnalyte)

    init {
        isClickable = true; isFocusable = true
        try {
            val attr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attr).build()
            soundPool?.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded = true }
        } catch (_: Exception) {}
    }

    // ─── Public API ───────────────────────────────────────────────────────
    fun reset() {
        animating = false; isAutoMode = false; manualHold = false
        volume = 0f; dropProgress = -1f; dropCount = 0; splashPhase = -1f
        curvePoints.clear(); handler.removeCallbacksAndMessages(null); invalidate()
    }

    private var lastDropTime = 0L
    fun addDrop() {
        val timePerDrop = (350L / speedMul).coerceAtLeast(40L)
        val now = System.currentTimeMillis()
        if (now - lastDropTime < timePerDrop) return
        lastDropTime = now
        if (volume >= eqVol * 2.5f) return

        dropProgress = 0f; dropCount++
        val dropVol = 0.05f * speedMul
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = (280L / speedMul).coerceAtLeast(40L)
            addUpdateListener { a ->
                dropProgress = a.animatedFraction
                wavePhase = sin(a.animatedFraction * PI.toFloat() * 4f) * 0.8f
                stirAngle = (stirAngle + 0.15f * speedMul) % 360f
                if (a.animatedFraction >= 0.95f) {
                    dropProgress = -1f; splashPhase = 0f
                    volume = minOf(volume + dropVol, eqVol * 2.5f)
                    curvePoints.add(Pair(volume, currentPH))
                    if (loaded && tickSoundId > 0) { try { soundPool?.play(tickSoundId, 0.4f, 0.4f, 1, 0, 1f) } catch (_: Exception) {} }
                }
                invalidate()
            }
            start()
        }
        // Splash fade
        if (splashPhase >= 0f) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300L
                addUpdateListener { splashPhase = it.animatedFraction; invalidate() }
                start()
            }
        }
    }

    fun startAuto() {
        if (animating) return; animating = true; isAutoMode = true
        val delay = (280L / speedMul).coerceAtLeast(30L)
        autoRunnable = object : Runnable {
            override fun run() {
                if (!animating || volume >= eqVol * 2.5f) { animating = false; isAutoMode = false; return }
                addDrop(); handler.postDelayed(this, delay)
            }
        }
        handler.post(autoRunnable!!)
    }

    fun stop() { animating = false; isAutoMode = false; manualHold = false; handler.removeCallbacksAndMessages(null) }
    fun startManual() {
        manualHold = true
        holdRunnable = object : Runnable {
            override fun run() {
                if (!manualHold || volume >= eqVol * 2.5f) return
                addDrop(); handler.postDelayed(this, (180L / speedMul).coerceAtLeast(30L))
            }
        }; handler.post(holdRunnable!!)
    }
    fun stopManual() { manualHold = false; holdRunnable?.let { handler.removeCallbacks(it) } }

    fun setSpeed(s: Int) { speedMul = s }
    fun setAcid(name: String, conc: Float) { acidName = name; acidConc = conc; reset() }
    fun setBase(name: String, conc: Float) { baseName = name; baseConc = conc; reset() }
    fun setAnalyteVol(v: Float) { _analyteVol = v; reset() }
    fun setType(t: TitrationType) {
        titrType = t; isAcidAnalyte = true
        when (t) {
            TitrationType.SA_SB -> { acidName = "HCl"; baseName = "NaOH" }
            TitrationType.SA_WB -> { acidName = "HCl"; baseName = "NH3" }
            TitrationType.WA_SB -> { acidName = "CH3COOH"; baseName = "NaOH" }
            TitrationType.WA_WB -> { acidName = "CH3COOH"; baseName = "NH3" }
        }; reset()
    }
    fun setIndicator(ind: Indicator) { _indicator = ind; invalidate() }

    // ─── Touch ────────────────────────────────────────────────────────────
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val cx = width * 0.25f
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                if (abs(e.x - cx) < stopcockR * 3f && abs(e.y - stopcockCY) < stopcockR * 3f) {
                    startManual(); return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { stopManual() }
        }
        return true
    }

    // ─── Draw ─────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)

        val ph = currentPH
        val maxVol = eqVol * 2.5f
        val phHue = (1f - ph / 14f) * 240f

        // ═══ LEFT SIDE: LAB SETUP ══════════════════════════════════════════
        val labCX = w * 0.25f

        // ─── Burette ──────────────────────────────────────────────────────
        val burW = w * 0.065f
        val burLeft = labCX - burW / 2f
        val burTop = h * 0.015f
        val burH = h * 0.26f
        val burBot = burTop + burH

        // Clamp/stand
        drawClamp(canvas, labCX, burTop - h * 0.01f, w * 0.14f)

        // Burette glass body
        val burGrad = LinearGradient(burLeft, 0f, burLeft + burW, 0f,
            intArrayOf(0xFF3A5A7C.toInt(), 0xFF6AAACE.toInt(), 0xCCFFFFFF.toInt(), 0xFF6AAACE.toInt(), 0xFF3A5A7C.toInt()),
            floatArrayOf(0f, 0.15f, 0.35f, 0.7f, 1f), Shader.TileMode.CLAMP)
        glassP.shader = burGrad
        canvas.drawRoundRect(burLeft, burTop, burLeft + burW, burBot, 3f, 3f, glassP)
        glassP.shader = null
        canvas.drawRoundRect(burLeft, burTop, burLeft + burW, burBot, 3f, 3f, glassStroke)

        // Specular highlight
        canvas.drawRect(burLeft + burW * 0.18f, burTop + 6f, burLeft + burW * 0.30f, burBot - 6f, glassHigh)

        // Graduation marks
        textP.textSize = h * 0.012f; textP.color = 0xFF999999.toInt()
        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF777777.toInt(); strokeWidth = 0.8f }
        val gradPaintMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); strokeWidth = 1.2f }
        for (i in 0..50) {
            val y = burTop + burH * i / 50f
            val isMajor = i % 10 == 0
            val isMid = i % 5 == 0
            val len = when { isMajor -> burW * 0.25f; isMid -> burW * 0.15f; else -> burW * 0.08f }
            val p = if (isMajor) gradPaintMajor else gradPaint
            canvas.drawLine(burLeft + burW - len, y, burLeft + burW - 1f, y, p)
            if (isMajor) {
                canvas.drawText("${i / 2}", burLeft + burW + 4f, y + 4f, textP)
            }
        }

        // Liquid inside burette
        val maxV = maxVol.coerceAtLeast(1f)
        val liqLevel = (1f - volume / maxV).coerceIn(0f, 1f)
        val burLiqTop = burTop + burH * (1f - liqLevel)
        val burLiqBot = burBot - 2f
        if (burLiqBot > burLiqTop && liqLevel < 0.99f) {
            val liqColor = if (isAcidAnalyte) 0xFF0088CC.toInt() else 0xFFCC6600.toInt()
            val liqColorDk = if (isAcidAnalyte) 0xFF005588.toInt() else 0xFF884400.toInt()
            val liqGrad = LinearGradient(0f, burLiqTop, 0f, burLiqBot, liqColor, liqColorDk, Shader.TileMode.CLAMP)
            liqP.shader = liqGrad
            canvas.drawRect(burLeft + 2f, burLiqTop, burLeft + burW - 2f, burLiqBot, liqP)
            liqP.shader = null
            // Meniscus curve
            val meniscus = Path().apply {
                moveTo(burLeft + 2f, burLiqTop)
                quadTo(labCX, burLiqTop - burW * 0.06f, burLeft + burW - 2f, burLiqTop)
            }
            canvas.drawPath(meniscus, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); strokeWidth = 1.2f; style = Paint.Style.STROKE; isAntiAlias = true })
        }

        // Burette label
        val titrantName = if (isAcidAnalyte) baseName else acidName
        val titrantConc = if (isAcidAnalyte) baseConc else acidConc
        textP.textSize = h * 0.016f; textP.color = 0xFF00CCFF.toInt()
        canvas.drawText("$titrantName ${"%.2f".format(titrantConc)} M", labCX, burBot + 13f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        // ─── Tip (narrow glass capillary) ─────────────────────────────────
        val tipW = w * 0.016f; val tipH = h * 0.04f
        val tipLeft = labCX - tipW / 2f
        val tipGrad = LinearGradient(tipLeft, 0f, tipLeft + tipW, 0f,
            intArrayOf(0xFF5599BB.toInt(), 0xAAFFFFFF.toInt(), 0xFF447799.toInt()),
            floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
        glassP.shader = tipGrad
        canvas.drawRoundRect(tipLeft, burBot, tipLeft + tipW, burBot + tipH, 2f, 2f, glassP)
        glassP.shader = null
        canvas.drawRoundRect(tipLeft, burBot, tipLeft + tipW, burBot + tipH, 2f, 2f, glassStroke)

        // ─── Stopcock (T-valve) ───────────────────────────────────────────
        stopcockCY = burBot + tipH * 0.5f; stopcockR = w * 0.024f
        val stopR = stopcockR
        // Horizontal bar
        canvas.drawRoundRect(labCX - stopR * 1.8f, stopcockCY - stopR * 0.35f, labCX + stopR * 1.8f, stopcockCY + stopR * 0.35f, 3f, 3f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFDDDDDD.toInt(); style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawRoundRect(labCX - stopR * 1.8f, stopcockCY - stopR * 0.35f, labCX + stopR * 1.8f, stopcockCY + stopR * 0.35f, 3f, 3f, glassStroke)
        // Center knob
        val knobColor = if (manualHold) 0xFFFF4444.toInt() else 0xFFEEEEEE.toInt()
        canvas.drawCircle(labCX, stopcockCY, stopR * 0.55f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = knobColor; style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawCircle(labCX, stopcockCY, stopR * 0.55f, glassStroke)
        if (manualHold) {
            canvas.drawCircle(labCX, stopcockCY, stopR * 0.85f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FF4444.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true })
        }

        // ─── Drop animation ───────────────────────────────────────────────
        if (dropProgress in 0f..1f) {
            val dropStartY = burBot + tipH
            val dropEndY = burBot + tipH + h * 0.10f
            val dropY = dropStartY + (dropEndY - dropStartY) * dropProgress
            val sz = w * 0.013f * (1f + sin(dropProgress * PI.toFloat()) * 0.2f)
            // Glow
            canvas.drawCircle(labCX, dropY, sz * 2.5f, dropGlow)
            // Drop body (teardrop)
            val dropPath = Path().apply {
                moveTo(labCX, dropY - sz * 1.2f)
                cubicTo(labCX + sz * 1.5f, dropY - sz * 0.3f, labCX + sz, dropY + sz * 0.8f, labCX, dropY + sz)
                cubicTo(labCX - sz, dropY + sz * 0.8f, labCX - sz * 1.5f, dropY - sz * 0.3f, labCX, dropY - sz * 1.2f)
            }
            canvas.drawPath(dropPath, dropP)
            // Highlight
            canvas.drawCircle(labCX - sz * 0.3f, dropY - sz * 0.2f, sz * 0.3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x88FFFFFF.toInt(); style = Paint.Style.FILL; isAntiAlias = true })
        }

        // ─── Erlenmeyer Flask ─────────────────────────────────────────────
        val flaskTopY = burBot + tipH + h * 0.11f
        val neckW = w * 0.045f; val neckH = h * 0.04f
        val bodyTopY = flaskTopY + neckH
        val bodyW = w * 0.30f; val bodyH = h * 0.20f
        val bodyBotY = bodyTopY + bodyH
        val bodyLeft = labCX - bodyW / 2f; val bodyRight = labCX + bodyW / 2f
        val neckL = labCX - neckW / 2f; val neckR = labCX + neckW / 2f

        erlenBodyTop = bodyTopY; erlenBodyBot = bodyBotY; erlenBodyLeft = bodyLeft; erlenBodyRight = bodyRight
        erlenNeckL = neckL; erlenNeckR = neckR; erlenNeckH = neckH; erlenFlaskTopY = flaskTopY

        // Shadow
        canvas.drawOval(labCX - bodyW * 0.35f, bodyBotY - 2f, labCX + bodyW * 0.35f, bodyBotY + 6f, glassDark)

        // Neck
        val neckGrad = LinearGradient(neckL, 0f, neckR, 0f,
            intArrayOf(0xFF4477AA.toInt(), 0xBBFFFFFF.toInt(), 0xFF4477AA.toInt()),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        glassP.shader = neckGrad
        canvas.drawRect(neckL, flaskTopY, neckR, bodyTopY, glassP)
        glassP.shader = null
        canvas.drawRect(neckL, flaskTopY, neckR, bodyTopY, glassStroke)
        canvas.drawRect(neckL + neckW * 0.15f, flaskTopY + 2f, neckL + neckW * 0.28f, bodyTopY - 2f, glassHigh)

        // Body (trapezoid)
        val bodyPath = Path().apply {
            moveTo(neckL, bodyTopY)
            lineTo(neckR, bodyTopY)
            lineTo(bodyRight, bodyBotY)
            lineTo(bodyLeft, bodyBotY)
            close()
        }
        val bodyGrad = LinearGradient(bodyLeft, 0f, bodyRight, 0f,
            intArrayOf(0xFF3A6699.toInt(), 0xFF77BBEE.toInt(), 0xBBFFFFFF.toInt(), 0xFF77BBEE.toInt(), 0xFF2A4466.toInt()),
            floatArrayOf(0f, 0.12f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP)
        glassP.shader = bodyGrad
        canvas.drawPath(bodyPath, glassP)
        glassP.shader = null
        canvas.drawPath(bodyPath, glassStroke)

        // Left highlight
        val hlPath = Path().apply {
            moveTo(neckL + 3f, bodyTopY + 4f)
            lineTo(bodyLeft + (bodyRight - bodyLeft) * 0.08f, bodyBotY - 4f)
            lineTo(bodyLeft + 4f, bodyBotY - 4f)
            lineTo(neckL + 2f, bodyTopY + 4f)
            close()
        }
        canvas.drawPath(hlPath, glassHigh)

        // ─── Liquid in flask (clipped to trapezoid) ───────────────────────
        val liqH = h * 0.10f + (volume / maxV) * h * 0.06f
        val liqTopY = bodyBotY - liqH + wavePhase * 2f
        val indicatorColor = getIndicatorColor(ph)
        val isFenolPink = _indicator == Indicator.FENOL && ph > 8.2f

        if (liqTopY < bodyBotY && liqTopY >= bodyTopY) {
            // Liquid level is within the trapezoid body
            val t = (liqTopY - bodyTopY) / (bodyBotY - bodyTopY) // 0=top, 1=bottom
            val liqL = neckL + (bodyLeft - neckL) * t
            val liqR = neckR + (bodyRight - neckR) * t

            val fillColor: Int
            val fillColorLt: Int
            if (isFenolPink) {
                fillColor = 0xFFDD1177.toInt()  // Deep pink
                fillColorLt = 0xFFEE4499.toInt() // Light pink
            } else if (_indicator == Indicator.METIL_ORANJ || _indicator == Indicator.BROM_TIMOL) {
                fillColor = Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.65f, 0.45f))
                fillColorLt = Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.45f, 0.55f))
            } else {
                fillColor = Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.65f, 0.45f))
                fillColorLt = Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.45f, 0.55f))
            }
            val fillGrad = LinearGradient(0f, liqTopY, 0f, bodyBotY, fillColorLt, fillColor, Shader.TileMode.CLAMP)
            liqP.shader = fillGrad

            // Clip to flask body path
            canvas.save()
            canvas.clipPath(bodyPath)
            val fillPath = Path().apply {
                moveTo(liqL, liqTopY)
                lineTo(liqR, liqTopY)
                lineTo(bodyRight + 2f, bodyBotY + 2f)
                lineTo(bodyLeft - 2f, bodyBotY + 2f)
                close()
            }
            canvas.drawPath(fillPath, liqP)
            liqP.shader = null

            // Surface wave
            val wavePath = Path().apply {
                moveTo(liqL + 4f, liqTopY + 1f)
                quadTo(labCX, liqTopY - 4f + wavePhase, liqR - 4f, liqTopY + 1f)
            }
            canvas.drawPath(wavePath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); strokeWidth = 1.5f; style = Paint.Style.STROKE; isAntiAlias = true })
            canvas.restore()
        } else if (liqTopY < bodyTopY) {
            // Liquid level is in the neck
            val fillColor: Int = if (isFenolPink) 0xFFDD1177.toInt() else Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.65f, 0.45f))
            val fillColorLt: Int = if (isFenolPink) 0xFFEE4499.toInt() else Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.45f, 0.55f))
            val fillGrad = LinearGradient(0f, liqTopY, 0f, bodyBotY, fillColorLt, fillColor, Shader.TileMode.CLAMP)
            liqP.shader = fillGrad

            canvas.save()
            canvas.clipPath(bodyPath)
            canvas.drawRect(neckL + 2f, liqTopY, neckR - 2f, bodyBotY + 2f, liqP)
            liqP.shader = null
            canvas.restore()
        }

        // Splash effect
        if (splashPhase in 0f..1f) {
            val splashAlpha = ((1f - splashPhase) * 180).toInt()
            val splashR = w * 0.018f * (1f + splashPhase * 2f)
            val splashY = if (liqTopY < bodyTopY) liqTopY + 2f else liqTopY + 3f
            canvas.drawCircle(labCX, splashY, splashR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(splashAlpha, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true
            })
        }

        // Stir bar
        val barCY = bodyBotY - 8f
        canvas.save()
        canvas.rotate(stirAngle, labCX, barCY)
        val barW2 = w * 0.04f
        val barGrad = LinearGradient(0f, barCY - 2.5f, 0f, barCY + 2.5f,
            intArrayOf(0xFFBBBBBB.toInt(), 0xFF777777.toInt(), 0xFF444444.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(labCX - barW2, barCY - 2.5f, labCX + barW2, barCY + 2.5f, 2f, 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = barGrad; style = Paint.Style.FILL })
        canvas.drawRoundRect(labCX - barW2, barCY - 2.5f, labCX + barW2, barCY + 2.5f, 2f, 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.5f })
        canvas.restore()

        // Flask label
        textP.textSize = h * 0.014f; textP.color = 0xFF888888.toInt()
        canvas.drawText("${titrType.label}", labCX, bodyBotY + 13f, textP)

        // Indicator dot (shows current indicator color)
        val indDotColor = if (isFenolPink) 0xFFDD1177.toInt() else indicatorColor
        canvas.drawCircle(labCX - w * 0.065f, bodyTopY + 5f, w * 0.011f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = indDotColor; style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawCircle(labCX - w * 0.065f, bodyTopY + 5f, w * 0.011f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 0.8f; isAntiAlias = true })
        textP.textSize = h * 0.013f; textP.color = indDotColor
        canvas.drawText(_indicator.label, labCX - w * 0.065f, bodyTopY - 5f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        // ─── pH Meter ─────────────────────────────────────────────────────
        val meterLeft = w * 0.02f; val meterTopY = bodyBotY + 20f
        val meterW = w * 0.46f; val meterH = h * 0.050f
        canvas.drawRoundRect(meterLeft, meterTopY, meterLeft + meterW, meterTopY + meterH, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111827.toInt(); style = Paint.Style.FILL; isAntiAlias = true })
        canvas.drawRoundRect(meterLeft, meterTopY, meterLeft + meterW, meterTopY + meterH, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 0.6f, 0.5f)); style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true })
        // pH value
        valV.textSize = h * 0.038f
        valV.color = Color.HSVToColor(floatArrayOf(phHue.coerceIn(0f, 300f), 1f, 1f))
        canvas.drawText("pH  ${"%.2f".format(ph)}", meterLeft + meterW * 0.35f, meterTopY + meterH * 0.72f, valV)
        // Mini pH bar
        val barX = meterLeft + meterW * 0.68f; val barY = meterTopY + meterH * 0.25f
        val phBarW = meterW * 0.28f; val phBarH = meterH * 0.5f
        canvas.drawRoundRect(barX, barY, barX + phBarW, barY + phBarH, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF222222.toInt(); style = Paint.Style.FILL })
        val phFrac = (ph / 14f).coerceIn(0f, 1f)
        canvas.drawRoundRect(barX, barY, barX + phBarW * phFrac, barY + phBarH, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.HSVToColor(floatArrayOf((1f - phFrac) * 240f, 1f, 0.8f)); style = Paint.Style.FILL })

        // Info text
        textP.textSize = h * 0.017f; textP.color = 0xFF00CCFF.toInt()
        val anName = if (isAcidAnalyte) acidName else baseName; val anConc = if (isAcidAnalyte) acidConc else baseConc
        canvas.drawText("${anName} ${"%.1f".format(anConc)} M  ·  ${"%.0f".format(_analyteVol)} mL", labCX, meterTopY + meterH + 15f, textP)
        textP.color = 0xFFFFA500.toInt()
        canvas.drawText("Eklenen: ${"%.1f".format(volume)} mL  ·  Esd: ${"%.1f".format(eqVol)} mL", labCX, meterTopY + meterH + 32f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        // ─── RIGHT SIDE: pH GRAPH ─────────────────────────────────────────
        val gLeft = w * 0.55f; val gRight = w * 0.97f
        val gTop = h * 0.04f; val gBot = h * 0.58f
        val gw = gRight - gLeft; val gh = gBot - gTop

        canvas.drawRoundRect(gLeft, gTop, gRight, gBot, 10f, 10f, graphBg)
        canvas.drawRoundRect(gLeft, gTop, gRight, gBot, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00E5FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true })

        // Grid
        textP.textSize = h * 0.015f; textP.color = 0xFF666666.toInt()
        for (i in 0..7) {
            val y = gBot - gh * i / 7f
            canvas.drawLine(gLeft + 1f, y, gRight - 1f, y, gridP)
            textP.textAlign = Paint.Align.RIGHT
            canvas.drawText("${i * 2}", gLeft - 4f, y + 4f, textP)
        }
        textP.textAlign = Paint.Align.CENTER
        for (i in 0..5) {
            val x = gLeft + gw * i / 5f
            canvas.drawLine(x, gBot - 1f, x, gBot + 3f, gridP)
            canvas.drawText("${"%.0f".format(i * maxVol / 5f)}", x, gBot + 15f, textP)
        }

        // Axis labels
        textP.textSize = h * 0.016f; textP.color = 0xFF888888.toInt()
        canvas.drawText("Hacim (mL)", gLeft + gw / 2f, gBot + 28f, textP)
        canvas.save(); canvas.rotate(-90f, gLeft - 16f, gTop + gh / 2f)
        canvas.drawText("pH", gLeft - 16f, gTop + gh / 2f, textP); canvas.restore()

        // Title
        textP.textSize = h * 0.019f; textP.color = 0xFF39FF14.toInt()
        canvas.drawText("Titrasyon Egrisi", gLeft + gw / 2f, gTop - 4f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        // Reference curve (faint)
        val refPath = Path()
        for (i in 0..200) {
            val v = i * maxVol / 200f
            val p = calcPH(v, if (isAcidAnalyte) baseConc else acidConc, if (isAcidAnalyte) acidConc else baseConc, _analyteVol, titrType, isAcidAnalyte)
            val px = gLeft + gw * v / maxVol; val py = gBot - gh * (p / 14f)
            if (i == 0) refPath.moveTo(px, py) else refPath.lineTo(px, py)
        }
        canvas.drawPath(refPath, faintP)

        // Actual curve
        if (curvePoints.isNotEmpty()) {
            val path = Path()
            for ((i, pt) in curvePoints.withIndex()) {
                val px = gLeft + gw * pt.first / maxVol; val py = gBot - gh * (pt.second / 14f)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, curveP)
            if (curvePoints.size > 1) {
                val last = curvePoints.last()
                val fp = Path(path).apply {
                    lineTo(gLeft + gw * last.first / maxVol, gBot); lineTo(gLeft, gBot); close()
                }
                canvas.drawPath(fp, curveFill)
            }
            val cx = gLeft + gw * volume / maxVol; val cy = gBot - gh * (ph / 14f)
            canvas.drawCircle(cx, cy, 7f, dotGlow)
            canvas.drawCircle(cx, cy, 4f, dotP)
        }

        // Equivalence line
        val eqX = gLeft + gw * eqVol / maxVol
        canvas.drawLine(eqX, gTop, eqX, gBot, eqP)
        textP.textSize = h * 0.016f; textP.color = 0xFFFF0080.toInt()
        canvas.drawText("Esd: ${"%.1f".format(eqVol)} mL", eqX, gBot + 40f, textP)
        textP.color = 0xFFAAAAAA.toInt()

        // Info panel (bottom right)
        val infoLeft = w * 0.55f; val infoTopY = gBot + 48f
        canvas.drawRoundRect(infoLeft, infoTopY, w * 0.97f, infoTopY + h * 0.10f, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111827.toInt(); style = Paint.Style.FILL; isAntiAlias = true })
        textP.textSize = h * 0.016f; textP.color = 0xFF00CCFF.toInt()
        canvas.drawText("${acidName} + ${baseName}", infoLeft + gw / 2f, infoTopY + 16f, textP)
        textP.color = 0xFF888888.toInt()
        canvas.drawText("Esd pH: ${"%.1f".format(calcPH(eqVol, if (isAcidAnalyte) baseConc else acidConc, if (isAcidAnalyte) acidConc else baseConc, _analyteVol, titrType, isAcidAnalyte))}  |  Damlalar: $dropCount", infoLeft + gw / 2f, infoTopY + 34f, textP)
    }

    // ─── Helper: Draw clamp/stand ──────────────────────────────────────────
    private fun drawClamp(canvas: Canvas, cx: Float, top: Float, totalW: Float) {
        val standPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
        val standStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }

        // Vertical rod
        val rodW = 6f; val rodH = height * 0.32f
        canvas.drawRoundRect(cx - totalW / 2f - rodW * 2f, top, cx - totalW / 2f - rodW, top + rodH, 2f, 2f, standPaint)
        canvas.drawRoundRect(cx - totalW / 2f - rodW * 2f, top, cx - totalW / 2f - rodW, top + rodH, 2f, 2f, standStroke)

        // Base
        val baseW = totalW * 0.7f; val baseH = 8f
        val baseX = cx - totalW / 2f - rodW * 2f - baseW * 0.1f
        canvas.drawRoundRect(baseX, top + rodH - baseH / 2f, baseX + baseW, top + rodH + baseH / 2f, 3f, 3f, standPaint)
        canvas.drawRoundRect(baseX, top + rodH - baseH / 2f, baseX + baseW, top + rodH + baseH / 2f, 3f, 3f, standStroke)

        // Clamp arm
        val armX = cx - totalW / 2f - rodW * 2f + rodW / 2f
        canvas.drawRoundRect(armX, top + 4f, cx - 2f, top + 10f, 2f, 2f, standPaint)
        canvas.drawRoundRect(armX, top + 4f, cx - 2f, top + 10f, 2f, 2f, standStroke)
        // Clamp grip
        canvas.drawRoundRect(cx - 4f, top - 2f, cx + 4f, top + 14f, 2f, 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF777777.toInt(); style = Paint.Style.FILL; isAntiAlias = true })
    }

    // ─── Indicator Color ───────────────────────────────────────────────────
    private fun getIndicatorColor(ph: Float): Int = when (_indicator) {
        Indicator.FENOL -> if (ph > 8.2f) 0xFFDD1177.toInt() else 0xFFCCCCCC.toInt()
        Indicator.METIL_ORANJ -> when {
            ph < 3.1f -> 0xFFFF3333.toInt()
            ph > 4.4f -> 0xFFEECC00.toInt()
            else -> { val f = (ph - 3.1f) / 1.3f; Color.argb(255, (255 * (1f - f * 0.5f)).toInt(), (255 * f).toInt(), 0) }
        }
        Indicator.BROM_TIMOL -> when {
            ph < 6.0f -> 0xFFEEEE00.toInt()
            ph > 7.6f -> 0xFF3333FF.toInt()
            else -> 0xFF33CC33.toInt()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { soundPool?.release() } catch (_: Exception) {}
        handler.removeCallbacksAndMessages(null)
    }
}

// ─── Fragment ──────────────────────────────────────────────────────────────
class TitrasyonFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext()).apply { setBackgroundColor(0xFF0A0E17.toInt()) }
        val ll = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, 0, 16) }

        // Header with ? help button
        val headerRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16, 12, 16, 0); gravity = Gravity.CENTER_VERTICAL }
        TextView(requireContext()).apply { text = "Titrasyon Simulasyonu"; setTextColor(0xFF00E5FF.toInt()); textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f); headerRow.addView(this) }
        Button(requireContext()).apply {
            text = "?"; setTextColor(-0x1); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD)
            backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF9C27B0.toInt())
            layoutParams = LinearLayout.LayoutParams(44, 44); setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Titrasyon Simulasyonu")
                    .setMessage("Bu araç bir asit-baz titrasyonunu canlandırır.\n\nNasıl kullanılır:\n• Asit ve baz türünü seçin\n• Derisimi ve hacmi ayarlayın\n• İndikatör seçin (Fenolftalein en yaygın)\n• 'Otomatik' ile otomatik damlatma başlatın\n• Veya büreten (musluğa) basılı tutarak manuel damlatın\n\nEkranın solunda:\n• Bürette asit veya baz sıvısı var\n• Aşağıya doğru damlalar düşer\n• Erlenmeyer flasks içindeki sıvı rengi değişir\n\nEkranın sağında:\n• pH eğrisi gerçek zamanlı çizilir\n• Pembe kesikli çizgi = eşdeğer noktası\n\nRenk dönüşümleri:\n• Fenolftalein: renksiz → pembe (pH > 8.2)\n• Metil Oranj: kırmızı → sarı (pH 3.1-4.4)\n• Bromtimol: sarı → mavi (pH 6.0-7.6)\n\nEşdeğer noktası: Tepkideki asit ve bazın birbirini tam nötrlediği nokta.")
                    .setPositiveButton("Anladım", null)
                    .show()
            }
            headerRow.addView(this)
        }
        ll.addView(headerRow)

        val view = TitrationView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (700 * resources.displayMetrics.density).toInt())
        }
        ll.addView(view)

        // Speed
        val speedRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        TextView(requireContext()).apply { text = "Hiz:"; setTextColor(-0x1); layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply { gravity = Gravity.CENTER_VERTICAL; setMargins(4, 0, 8, 0) }; speedRow.addView(this) }
        listOf("x1" to 1, "x5" to 5, "x10" to 10).forEach { (label, mul) ->
            Button(requireContext()).apply { text = label; setTextColor(-0x1); textSize = 11f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF455A64.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(3, 2, 3, 2) }; setOnClickListener { view.setSpeed(mul) }; speedRow.addView(this) }
        }
        ll.addView(speedRow)

        // Type
        val typeRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        for (t in TitrationType.values()) {
            Button(requireContext()).apply {
                text = when (t) { TitrationType.SA_SB -> "SA-SB"; TitrationType.SA_WB -> "SA-WB"; TitrationType.WA_SB -> "WA-SB"; TitrationType.WA_WB -> "WA-WB" }
                setTextColor(-0x1); textSize = 10f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1976D2.toInt())
                layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(2, 2, 2, 2) }; setOnClickListener { view.setType(t) }; typeRow.addView(this)
            }
        }
        ll.addView(typeRow)

        // Acid
        val acidRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        TextView(requireContext()).apply { text = "Asit:"; setTextColor(-0x1); layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply { gravity = Gravity.CENTER_VERTICAL; setMargins(4, 0, 4, 0) }; acidRow.addView(this) }
        ACID_OPTIONS.forEach { name ->
            Button(requireContext()).apply { text = name; setTextColor(-0x1); textSize = 11f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFD32F2F.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(2, 2, 2, 2) }; setOnClickListener { view.setAcid(name, view.acidConc) }; acidRow.addView(this) }
        }
        ll.addView(acidRow)

        // Base
        val baseRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        TextView(requireContext()).apply { text = "Baz:"; setTextColor(-0x1); layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply { gravity = Gravity.CENTER_VERTICAL; setMargins(4, 0, 4, 0) }; baseRow.addView(this) }
        BASE_OPTIONS.forEach { name ->
            Button(requireContext()).apply { text = name; setTextColor(-0x1); textSize = 11f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF1976D2.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(2, 2, 2, 2) }; setOnClickListener { view.setBase(name, view.baseConc) }; baseRow.addView(this) }
        }
        ll.addView(baseRow)

        // Conc
        val concRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        TextView(requireContext()).apply { text = "Derisim:"; setTextColor(-0x1); layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply { gravity = Gravity.CENTER_VERTICAL; setMargins(4, 0, 4, 0) }; concRow.addView(this) }
        CONC_OPTIONS.forEach { c ->
            Button(requireContext()).apply { text = "${"%.2f".format(c)} M"; setTextColor(-0x1); textSize = 11f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF388E3C.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(2, 2, 2, 2) }; setOnClickListener { view.setAcid(view.acidName, c); view.setBase(view.baseName, c) }; concRow.addView(this) }
        }
        ll.addView(concRow)

        // Volume
        val volRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        TextView(requireContext()).apply { text = "Hacim:"; setTextColor(-0x1); layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply { gravity = Gravity.CENTER_VERTICAL; setMargins(4, 0, 4, 0) }; volRow.addView(this) }
        VOL_OPTIONS.forEach { v ->
            Button(requireContext()).apply { text = "${"%.0f".format(v)} mL"; setTextColor(-0x1); textSize = 11f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF7B1FA2.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(2, 2, 2, 2) }; setOnClickListener { view.setAnalyteVol(v) }; volRow.addView(this) }
        }
        ll.addView(volRow)

        // Indicator
        val indRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8, 2, 8, 2) }
        TextView(requireContext()).apply { text = "Indikator:"; setTextColor(-0x1); layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT).apply { gravity = Gravity.CENTER_VERTICAL; setMargins(4, 0, 4, 0) }; indRow.addView(this) }
        Indicator.values().forEach { ind ->
            Button(requireContext()).apply { text = ind.label; setTextColor(-0x1); textSize = 10f; backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFA000.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(2, 2, 2, 2) }; setOnClickListener { view.setIndicator(ind) }; indRow.addView(this) }
        }
        ll.addView(indRow)

        // Actions
        val actRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12, 6, 12, 6) }
        Button(requireContext()).apply { text = "Otomatik"; setTextColor(-0x1); backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(4, 4, 4, 4) }; setOnClickListener { view.startAuto() }; actRow.addView(this) }
        Button(requireContext()).apply { text = "Durdur"; setTextColor(-0x1); backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(4, 4, 4, 4) }; setOnClickListener { view.stop() }; actRow.addView(this) }
        Button(requireContext()).apply { text = "Sifirla"; setTextColor(-0x1); backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF455A64.toInt()); layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply { setMargins(4, 4, 4, 4) }; setOnClickListener { view.reset() }; actRow.addView(this) }
        ll.addView(actRow)

        // Help
        ll.addView(TextView(requireContext()).apply {
            text = "Musluga basili tut = manuel damlatma | Hiz x1/x5/x10 | Asit-baz turunu, indikatoru, derisimi ve hacmi degistirin"
            setTextColor(0xFF666666.toInt()); textSize = 11f; gravity = Gravity.CENTER; setPadding(12, 2, 12, 8)
        })

        scroll.addView(ll)
        return scroll
    }
}
