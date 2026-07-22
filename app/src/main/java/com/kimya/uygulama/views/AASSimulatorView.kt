package com.kimya.uygulama.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
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
                Color.rgb(0, 200, 160), Color.rgb(30, 220, 140), 0.08f, "0.001 ppm",
                "Bakır analizi için en yaygın kullanılan dalga boyu"),
            ElementInfo("Zn", "Çinko", 30, "213.9 nm", 213.9f,
                Color.rgb(80, 150, 220), Color.rgb(100, 180, 240), 0.015f, "0.0005 ppm",
                "UV bölgesinde absorpsiyon yapar, hassas ölçüm"),
            ElementInfo("Fe", "Demir", 26, "248.3 nm", 248.3f,
                Color.rgb(220, 140, 50), Color.rgb(255, 170, 70), 0.04f, "0.003 ppm",
                "Demir tayininde kullanılan temel dalga boyu"),
            ElementInfo("Pb", "Kurşun", 82, "217.0 nm", 217.0f,
                Color.rgb(160, 180, 220), Color.rgb(190, 210, 240), 0.06f, "0.01 ppm",
                "Çevre analizlerinde kurşun tayini"),
            ElementInfo("Cd", "Kadmiyum", 48, "228.8 nm", 228.8f,
                Color.rgb(200, 200, 200), Color.rgb(230, 230, 230), 0.003f, "0.0001 ppm",
                "Ultra düşük seviye kadmiyum tayini"),
            ElementInfo("Mn", "Manganez", 25, "279.5 nm", 279.5f,
                Color.rgb(180, 120, 220), Color.rgb(200, 150, 240), 0.025f, "0.002 ppm",
                "Manganez analizi için kullanılır"),
            ElementInfo("Ni", "Nikel", 28, "232.0 nm", 232.0f,
                Color.rgb(100, 200, 100), Color.rgb(130, 230, 130), 0.05f, "0.005 ppm",
                "Nikel tayininde kullanılır"),
            ElementInfo("Cr", "Krom", 24, "357.9 nm", 357.9f,
                Color.rgb(140, 220, 140), Color.rgb(170, 240, 170), 0.035f, "0.003 ppm",
                "Krom analizi için karakteristik çizgi"),
            ElementInfo("Ca", "Kalsiyum", 20, "422.7 nm", 422.7f,
                Color.rgb(255, 160, 60), Color.rgb(255, 180, 90), 0.06f, "0.005 ppm",
                "Sular ve biyolojik numunelerde kalsiyum"),
            ElementInfo("Na", "Sodyum", 11, "589.0 nm", 589.0f,
                Color.rgb(255, 200, 40), Color.rgb(255, 220, 70), 0.01f, "0.001 ppm",
                "Sodyum tayini, çok duyarlı analiz çizgisi"),
            ElementInfo("K", "Potasyum", 19, "766.5 nm", 766.5f,
                Color.rgb(200, 120, 255), Color.rgb(220, 150, 255), 0.02f, "0.005 ppm",
                "Kızılötesi bölgede absorpsiyon yapar"),
            ElementInfo("Mg", "Magnezyum", 12, "285.2 nm", 285.2f,
                Color.rgb(220, 220, 120), Color.rgb(240, 240, 140), 0.005f, "0.0005 ppm",
                "Magnezyum tayini için çok hassas çizgi")
        )
    }

    var currentElement = ELEMENTS[0]
        private set
    var flameOn = false
    var sampleInserted = false
    var acetyleneFlow = 0f
    var airFlow = 0f
    var sampleConcentration = 0f
    var currentAbsorbance = 0f
        private set
    var currentConcentration = 0f
        private set
    var signalPercent = 0f
        private set

    private var time = 0f
    private var targetAbsorbance = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            time += 0.04f
            updatePhysics()
            invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(12, 14, 18)
    }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(160, 170, 185)
        textSize = 18f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val smallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(120, 130, 145)
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val lightBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    fun selectElement(symbol: String) {
        currentElement = ELEMENTS.find { it.symbol == symbol } ?: ELEMENTS[0]
    }

    private fun updatePhysics() {
        val flameTemp = calculateFlameTemp()

        if (flameOn && flameTemp > 500f && sampleInserted) {
            val atomization = (flameTemp / 2800f).coerceIn(0f, 1f)
            targetAbsorbance = currentElement.sensitivity * sampleConcentration * atomization
            targetAbsorbance = targetAbsorbance.coerceIn(0f, 2f)
        } else {
            targetAbsorbance = 0f
        }

        val noise = if (flameOn) (Random.nextFloat() - 0.5f) * 0.008f else 0f
        currentAbsorbance += (targetAbsorbance - currentAbsorbance) * 0.08f + noise
        currentAbsorbance = currentAbsorbance.coerceIn(0f, 2f)

        currentConcentration = if (currentElement.sensitivity > 0f && currentAbsorbance > 0.001f) {
            currentAbsorbance / currentElement.sensitivity
        } else 0f

        signalPercent = ((1f - pow(10f, -currentAbsorbance)) * 100f).coerceIn(0f, 99.9f)
    }

    private fun calculateFlameTemp(): Float {
        if (!flameOn) return 0f
        val airRatio = (airFlow / 100f).coerceIn(0f, 1f)
        val c2h2Ratio = (acetyleneFlow / 100f).coerceIn(0f, 1f)
        val ratio = if (airRatio + c2h2Ratio > 0f) c2h2Ratio / (airRatio + c2h2Ratio) else 0f
        val idealRatio = 0.35f
        val efficiency = 1f - abs(ratio - idealRatio) * 3f
        return (2300f * efficiency.coerceIn(0.3f, 1f) * (airRatio + c2h2Ratio) * 0.5f + 800f * c2h2Ratio)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val centerY = h * 0.5f
        val segW = w / 7f

        drawHCL(canvas, segW * 0.5f, centerY, segW, h)
        drawLightBeam(canvas, segW * 1f, centerY, segW * 1.5f, h)
        drawNebulizer(canvas, segW * 2f, centerY, segW, h)
        drawFlameBurner(canvas, segW * 3.2f, centerY, segW, h)
        drawMonochromator(canvas, segW * 4.5f, centerY, segW, h)
        drawDetector(canvas, segW * 5.5f, centerY, segW, h)
        drawDisplay(canvas, segW * 6.5f, centerY, segW, h)

        drawFlowArrows(canvas, centerY, w, h)
    }

    private fun drawHCL(canvas: Canvas, cx: Float, cy: Float, segW: Float, h: Float) {
        val bw = segW * 0.35f
        val bh = h * 0.32f
        rect.set(cx - bw, cy - bh, cx + bw, cy + bh)

        boxPaint.shader = LinearGradient(
            cx - bw, cy - bh, cx + bw, cy + bh,
            intArrayOf(Color.rgb(35, 40, 50), Color.rgb(55, 62, 75), Color.rgb(40, 45, 55)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)

        val glowColor = if (flameOn && airFlow > 10f && acetyleneFlow > 10f) currentElement.hclColor else Color.rgb(40, 45, 55)
        boxPaint.shader = RadialGradient(cx, cy, bw * 0.6f,
            intArrayOf(colorWithAlpha(glowColor, 120), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, bw * 0.6f, boxPaint)
        boxPaint.shader = null

        labelPaint.textSize = 16f
        labelPaint.color = Color.rgb(160, 170, 185)
        canvas.drawText("HCL", cx, cy - bh * 0.55f, labelPaint)

        smallLabelPaint.textSize = 13f
        smallLabelPaint.color = glowColor
        canvas.drawText(currentElement.symbol, cx, cy + bh * 0.05f, smallLabelPaint)

        labelPaint.textSize = 12f
        labelPaint.color = Color.rgb(100, 110, 125)
        canvas.drawText(currentElement.wavelength, cx, cy + bh * 0.65f, labelPaint)
    }

    private fun drawLightBeam(canvas: Canvas, x1: Float, cy: Float, x2: Float, h: Float) {
        if (!flameOn || airFlow < 10f || acetyleneFlow < 10f) return

        val beamAlpha = (60 + sin(time * 4f) * 20).toInt().coerceIn(30, 120)
        val beamColor = colorWithAlpha(currentElement.hclColor, beamAlpha)

        lightBeamPaint.shader = LinearGradient(x1, cy - 3f, x2, cy + 3f,
            intArrayOf(beamColor, colorWithAlpha(currentElement.hclColor, beamAlpha + 40), beamColor),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        lightBeamPaint.strokeWidth = 4f
        lightBeamPaint.style = Paint.Style.FILL
        canvas.drawRect(x1, cy - 2f, x2, cy + 2f, lightBeamPaint)

        val pulseAlpha = (40 + sin(time * 6f) * 30).toInt().coerceIn(20, 80)
        lightBeamPaint.shader = LinearGradient(x1, cy - 8f, x2, cy + 8f,
            intArrayOf(Color.TRANSPARENT, colorWithAlpha(currentElement.hclColor, pulseAlpha), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(x1, cy - 8f, x2, cy + 8f, lightBeamPaint)
        lightBeamPaint.shader = null

        for (i in 0..4) {
            val px = x1 + (x2 - x1) * (i / 4f) + sin(time * 3f + i) * 2f
            lightBeamPaint.color = colorWithAlpha(currentElement.hclColor, (beamAlpha * 0.6f).toInt())
            canvas.drawCircle(px, cy, 1.5f, lightBeamPaint)
        }
    }

    private fun drawNebulizer(canvas: Canvas, cx: Float, cy: Float, segW: Float, h: Float) {
        val bw = segW * 0.35f
        val bh = h * 0.2f
        rect.set(cx - bw, cy - bh, cx + bw, cy + bh)

        boxPaint.shader = LinearGradient(
            cx - bw, cy - bh, cx + bw, cy + bh,
            intArrayOf(Color.rgb(40, 50, 60), Color.rgb(65, 75, 90), Color.rgb(45, 55, 65)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        boxPaint.shader = null

        labelPaint.textSize = 14f
        labelPaint.color = Color.rgb(160, 170, 185)
        canvas.drawText("Nebulizatör", cx, cy - bh * 0.15f, labelPaint)

        if (sampleInserted && flameOn) {
            val sprayAlpha = (100 + sin(time * 8f) * 50).toInt().coerceIn(50, 200)
            for (i in 0..6) {
                val angle = time * 2f + i * 0.9f
                val dist = bw * 0.3f + (i * bw * 0.08f)
                val sx = cx + cos(angle) * dist * 0.4f
                val sy = cy + bh * 0.15f + sin(angle * 1.3f) * dist * 0.3f
                flamePaint.color = colorWithAlpha(Color.rgb(200, 220, 255), sprayAlpha / (i + 1))
                canvas.drawCircle(sx, sy, (3 - i * 0.3f).coerceAtLeast(1f), flamePaint)
            }
        }

        smallLabelPaint.textSize = 12f
        smallLabelPaint.color = if (sampleInserted) Color.rgb(130, 200, 130) else Color.rgb(100, 110, 125)
        canvas.drawText(if (sampleInserted) "Aktif" else "Beklemede", cx, cy + bh * 0.75f, smallLabelPaint)
    }

    private fun drawFlameBurner(canvas: Canvas, cx: Float, cy: Float, segW: Float, h: Float) {
        val burnerW = segW * 0.12f
        val burnerH = h * 0.2f
        val burnerTop = cy + burnerH * 0.1f

        rect.set(cx - burnerW, burnerTop, cx + burnerW, burnerTop + burnerH)
        boxPaint.shader = LinearGradient(
            cx - burnerW, burnerTop, cx + burnerW, burnerTop,
            intArrayOf(Color.rgb(80, 80, 90), Color.rgb(140, 140, 155), Color.rgb(100, 100, 115)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 3f, 3f, boxPaint)

        rect.set(cx - burnerW * 2.5f, burnerTop + burnerH, cx + burnerW * 2.5f, burnerTop + burnerH + h * 0.015f)
        boxPaint.shader = LinearGradient(
            cx - burnerW * 2.5f, burnerTop + burnerH, cx + burnerW * 2.5f, burnerTop + burnerH,
            intArrayOf(Color.rgb(70, 70, 80), Color.rgb(130, 130, 145), Color.rgb(90, 90, 100)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 2f, 2f, boxPaint)
        boxPaint.shader = null

        if (flameOn && acetyleneFlow > 5f && airFlow > 5f) {
            drawFlame(canvas, cx, burnerTop, segW, h)
        }

        labelPaint.textSize = 13f
        labelPaint.color = Color.rgb(160, 170, 185)
        canvas.drawText("Atomizer", cx, burnerTop + burnerH + h * 0.05f, labelPaint)

        val temp = calculateFlameTemp()
        if (flameOn) {
            smallLabelPaint.textSize = 11f
            smallLabelPaint.color = if (temp > 1500) Color.rgb(255, 160, 60) else Color.rgb(100, 110, 125)
            canvas.drawText("${temp.toInt()}°C", cx, burnerTop + burnerH + h * 0.08f, smallLabelPaint)
        }
    }

    private fun drawFlame(canvas: Canvas, cx: Float, baseY: Float, segW: Float, h: Float) {
        val flameH = h * 0.28f * ((acetyleneFlow + airFlow) / 200f).coerceIn(0.2f, 1f)
        val flameW = segW * 0.18f
        val temp = calculateFlameTemp()

        val innerColor = if (temp > 2000) Color.rgb(100, 140, 255) else Color.rgb(30, 100, 255)
        val midColor = currentElement.flameColor
        val outerColor = colorWithAlpha(currentElement.flameColor, 80)

        val fx = sin(time * 3.7f) * 2f + cos(time * 7f) * 1f
        val fy = sin(time * 2.3f) * h * 0.005f

        path.reset()
        flamePaint.shader = RadialGradient(cx, baseY - flameH * 0.5f, flameH * 0.6f,
            intArrayOf(outerColor, Color.TRANSPARENT),
            floatArrayOf(0.3f, 1f), Shader.TileMode.CLAMP)
        buildFlamePath(path, cx, baseY, cx + fx, baseY - flameH + fy, flameW * 2.5f)
        canvas.drawPath(path, flamePaint)

        flamePaint.shader = LinearGradient(cx, baseY, cx, baseY - flameH,
            intArrayOf(midColor, colorWithAlpha(midColor, 200), colorWithAlpha(midColor, 60), Color.TRANSPARENT),
            floatArrayOf(0f, 0.2f, 0.6f, 1f), Shader.TileMode.CLAMP)
        path.reset()
        buildFlamePath(path, cx, baseY, cx + fx * 0.5f, baseY - flameH * 0.88f + fy * 0.6f, flameW * 1.2f)
        canvas.drawPath(path, flamePaint)

        flamePaint.shader = LinearGradient(cx, baseY, cx, baseY - flameH * 0.5f,
            intArrayOf(Color.WHITE, colorWithAlpha(innerColor, 200), colorWithAlpha(innerColor, 50), Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP)
        path.reset()
        buildFlamePath(path, cx, baseY, cx + fx * 0.2f, baseY - flameH * 0.45f + fy * 0.3f, flameW * 0.4f)
        canvas.drawPath(path, flamePaint)

        flamePaint.shader = null
    }

    private fun buildFlamePath(p: Path, cx: Float, baseY: Float, tipX: Float, tipY: Float, halfW: Float) {
        p.moveTo(cx, baseY)
        p.cubicTo(
            cx - halfW * 0.85f, baseY - (baseY - tipY) * 0.35f,
            tipX - halfW * 0.35f, tipY + (baseY - tipY) * 0.18f,
            tipX, tipY
        )
        p.cubicTo(
            tipX + halfW * 0.35f, tipY + (baseY - tipY) * 0.18f,
            cx + halfW * 0.85f, baseY - (baseY - tipY) * 0.35f,
            cx, baseY
        )
        p.close()
    }

    private fun drawMonochromator(canvas: Canvas, cx: Float, cy: Float, segW: Float, h: Float) {
        val bw = segW * 0.35f
        val bh = h * 0.25f
        rect.set(cx - bw, cy - bh, cx + bw, cy + bh)

        boxPaint.shader = LinearGradient(
            cx - bw, cy - bh, cx + bw, cy + bh,
            intArrayOf(Color.rgb(35, 42, 55), Color.rgb(58, 68, 85), Color.rgb(40, 48, 60)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 8f, 8f, boxPaint)
        boxPaint.shader = null

        val prismCx = cx
        val prismCy = cy
        val prismSize = bw * 0.5f
        path.reset()
        path.moveTo(prismCx, prismCy - prismSize)
        path.lineTo(prismCx - prismSize * 0.8f, prismCy + prismSize * 0.5f)
        path.lineTo(prismCx + prismSize * 0.8f, prismCy + prismSize * 0.5f)
        path.close()
        boxPaint.color = colorWithAlpha(Color.rgb(100, 150, 255), 60)
        canvas.drawPath(path, boxPaint)
        boxPaint.color = Color.rgb(120, 170, 255)
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 1.5f
        canvas.drawPath(path, boxPaint)
        boxPaint.style = Paint.Style.FILL

        labelPaint.textSize = 14f
        labelPaint.color = Color.rgb(160, 170, 185)
        canvas.drawText("Monokromatör", cx, cy - bh * 0.75f, labelPaint)

        smallLabelPaint.textSize = 11f
        smallLabelPaint.color = currentElement.hclColor
        canvas.drawText(currentElement.wavelength, cx, cy + bh * 0.85f, smallLabelPaint)
    }

    private fun drawDetector(canvas: Canvas, cx: Float, cy: Float, segW: Float, h: Float) {
        val bw = segW * 0.3f
        val bh = h * 0.2f
        rect.set(cx - bw, cy - bh, cx + bw, cy + bh)

        boxPaint.shader = LinearGradient(
            cx - bw, cy - bh, cx + bw, cy + bh,
            intArrayOf(Color.rgb(45, 35, 50), Color.rgb(70, 55, 80), Color.rgb(50, 40, 55)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        boxPaint.shader = null

        if (flameOn && airFlow > 10f && acetyleneFlow > 10f) {
            val detectAlpha = (80 + signalPercent * 1.2f).toInt().coerceIn(40, 200)
            boxPaint.color = colorWithAlpha(Color.rgb(180, 100, 255), detectAlpha)
            canvas.drawCircle(cx, cy, bw * 0.4f, boxPaint)
        }

        labelPaint.textSize = 14f
        labelPaint.color = Color.rgb(160, 170, 185)
        canvas.drawText("PMT", cx, cy - bh * 0.55f, labelPaint)

        smallLabelPaint.textSize = 11f
        smallLabelPaint.color = Color.rgb(130, 100, 160)
        canvas.drawText("Dedektör", cx, cy + bh * 0.6f, smallLabelPaint)
    }

    private fun drawDisplay(canvas: Canvas, cx: Float, cy: Float, segW: Float, h: Float) {
        val bw = segW * 0.4f
        val bh = h * 0.3f
        rect.set(cx - bw, cy - bh, cx + bw, cy + bh)

        boxPaint.shader = LinearGradient(
            cx - bw, cy - bh, cx + bw, cy + bh,
            intArrayOf(Color.rgb(15, 18, 25), Color.rgb(25, 30, 40), Color.rgb(18, 22, 30)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, 10f, 10f, boxPaint)
        boxPaint.shader = null

        val screenRect = RectF(cx - bw * 0.85f, cy - bh * 0.75f, cx + bw * 0.85f, cy + bh * 0.75f)
        boxPaint.color = Color.rgb(8, 12, 18)
        canvas.drawRoundRect(screenRect, 6f, 6f, boxPaint)

        valuePaint.textSize = 16f
        valuePaint.color = Color.rgb(0, 240, 255)
        valuePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("A = ${String.format("%.4f", currentAbsorbance)}", cx, cy - bh * 0.25f, valuePaint)

        valuePaint.textSize = 13f
        valuePaint.color = Color.rgb(57, 255, 20)
        canvas.drawText("${String.format("%.2f", currentConcentration)} ppm", cx, cy + bh * 0.1f, valuePaint)

        valuePaint.textSize = 11f
        valuePaint.color = Color.rgb(255, 0, 128)
        canvas.drawText(currentElement.symbol + " " + currentElement.wavelength, cx, cy + bh * 0.4f, valuePaint)

        labelPaint.textSize = 14f
        labelPaint.color = Color.rgb(160, 170, 185)
        canvas.drawText("Ekran", cx, cy + bh * 0.95f, labelPaint)
    }

    private fun drawFlowArrows(canvas: Canvas, cy: Float, w: Float, h: Float) {
        if (!flameOn || acetyleneFlow < 5f) return

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        arrowPaint.color = colorWithAlpha(Color.rgb(50, 200, 50), 120)
        path.reset()
        path.moveTo(w * 0.15f, cy + h * 0.22f)
        path.lineTo(w * 0.28f, cy + h * 0.1f)
        canvas.drawPath(path, arrowPaint)

        arrowPaint.color = colorWithAlpha(Color.rgb(100, 180, 255), 120)
        path.reset()
        path.moveTo(w * 0.28f, cy + h * 0.22f)
        path.lineTo(w * 0.38f, cy + h * 0.1f)
        canvas.drawPath(path, arrowPaint)
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
