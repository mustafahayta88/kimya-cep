package com.kimya.uygulama.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class FlameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class SaltInfo(
        val key: String, val name: String, val formula: String,
        val colorInner: Int, val colorMid: Int, val colorOuter: Int,
        val sparks: Boolean = false,
        val wavelength: String, val description: String
    )

    companion object {
        val SALTS = listOf(
            SaltInfo("none", "Normal", "C₂H₂+Hava",
                Color.rgb(30, 120, 255), Color.rgb(80, 160, 255), Color.rgb(120, 190, 255),
                wavelength = "-", description = "Asetilen + Hava normal alevi"),
            SaltInfo("Li", "Lityum", "LiCl",
                Color.rgb(220, 30, 30), Color.rgb(255, 60, 40), Color.rgb(255, 90, 70),
                wavelength = "671 nm", description = "Koyu kirmizi alev"),
            SaltInfo("Na", "Sodyum", "NaCl",
                Color.rgb(220, 160, 0), Color.rgb(255, 210, 20), Color.rgb(255, 235, 60),
                wavelength = "589 nm", description = "Yogun sari alev"),
            SaltInfo("K", "Potasyum", "KCl",
                Color.rgb(150, 60, 220), Color.rgb(185, 110, 255), Color.rgb(215, 160, 255),
                wavelength = "766 nm", description = "Mor-lila alev"),
            SaltInfo("Ca", "Kalsiyum", "CaCl₂",
                Color.rgb(230, 110, 20), Color.rgb(255, 150, 50), Color.rgb(255, 180, 90),
                wavelength = "423 nm", description = "Turuncu-kirmizi alev"),
            SaltInfo("Sr", "Stronsiyum", "SrCl₂",
                Color.rgb(190, 15, 15), Color.rgb(235, 40, 35), Color.rgb(255, 70, 55),
                wavelength = "461 nm", description = "Koyu kirmizi alev"),
            SaltInfo("Ba", "Baryum", "BaCl₂",
                Color.rgb(110, 195, 40), Color.rgb(150, 230, 70), Color.rgb(190, 255, 110),
                wavelength = "554 nm", description = "Sari-yesil alev"),
            SaltInfo("Cu", "Bakir", "CuCl₂",
                Color.rgb(0, 155, 110), Color.rgb(30, 200, 145), Color.rgb(70, 235, 175),
                wavelength = "325 nm", description = "Yesil-mavi alev"),
            SaltInfo("Fe", "Demir", "FeCl₃",
                Color.rgb(230, 140, 45), Color.rgb(255, 175, 75), Color.rgb(255, 210, 120),
                sparks = true, wavelength = "372 nm", description = "Turuncu kivilcimli alev"),
            SaltInfo("Mn", "Manganez", "MnCl₂",
                Color.rgb(90, 180, 55), Color.rgb(125, 215, 85), Color.rgb(165, 245, 125),
                wavelength = "403 nm", description = "Sari-yesil alev"),
            SaltInfo("Pb", "Kursun", "Pb(NO₃)₂",
                Color.rgb(160, 190, 245), Color.rgb(195, 220, 255), Color.rgb(230, 242, 255),
                wavelength = "406 nm", description = "Mavi-beyaz alev"),
            SaltInfo("Zn", "Cinko", "ZnCl₂",
                Color.rgb(80, 140, 190), Color.rgb(120, 175, 220), Color.rgb(160, 210, 245),
                wavelength = "214 nm", description = "Mavi-yesil alev")
        )
        private val SALT_MAP = SALTS.associateBy { it.key }
    }

    private var currentSalt = SALTS[0]
    private var time = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            time += 0.045f
            updateSparks()
            invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    data class Spark(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, var maxLife: Float, var size: Float
    )
    private val sparks = mutableListOf<Spark>()

    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val burnerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 180, 190); textSize = 20f; textAlign = Paint.Align.CENTER
    }
    private val path = Path()
    private val path2 = Path()

    fun selectSalt(key: String) {
        currentSalt = SALT_MAP[key] ?: SALTS[0]
    }

    fun getCurrentSalt(): SaltInfo = currentSalt

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
        val cx = w * 0.45f
        val burnerTopY = h * 0.6f
        val burnerW = w * 0.045f
        val burnerH = h * 0.26f
        val baseY = burnerTopY + burnerH

        drawBurner(canvas, cx, burnerTopY, burnerW, burnerH, baseY, w, h)
        drawFlame(canvas, cx, burnerTopY, w, h)
        if (currentSalt.sparks) drawSparks(canvas, cx, burnerTopY, w, h)
        drawWire(canvas, cx, burnerTopY, w, h)
        drawGasTank(canvas, cx, burnerTopY, burnerH, w, h)
    }

    private fun drawBurner(canvas: Canvas, cx: Float, topY: Float, bw: Float, bh: Float, baseY: Float, w: Float, h: Float) {
        burnerPaint.style = Paint.Style.FILL

        burnerPaint.shader = LinearGradient(
            cx - bw * 1.2f, topY, cx + bw * 1.2f, topY,
            intArrayOf(Color.rgb(90, 90, 100), Color.rgb(160, 160, 175), Color.rgb(120, 120, 135)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(cx - bw, topY + 4f, cx + bw, baseY, 5f, 5f, burnerPaint)

        burnerPaint.shader = null
        burnerPaint.color = Color.rgb(55, 55, 65)
        canvas.drawOval(cx - bw * 0.55f, topY + bh * 0.12f, cx + bw * 0.55f, topY + bh * 0.22f, burnerPaint)

        burnerPaint.color = Color.rgb(70, 70, 80)
        canvas.drawOval(cx - bw * 0.55f, topY + bh * 0.28f, cx + bw * 0.55f, topY + bh * 0.38f, burnerPaint)

        burnerPaint.shader = LinearGradient(
            cx - bw * 3.2f, baseY, cx + bw * 3.2f, baseY,
            intArrayOf(Color.rgb(80, 80, 90), Color.rgb(145, 145, 160), Color.rgb(100, 100, 115)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(cx - bw * 3f, baseY, cx + bw * 3f, baseY + h * 0.018f, 3f, 3f, burnerPaint)

        burnerPaint.shader = LinearGradient(
            cx + bw, topY + bh * 0.68f, cx + bw * 2.8f, topY + bh * 0.68f,
            intArrayOf(Color.rgb(90, 90, 100), Color.rgb(135, 135, 150)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(cx + bw, topY + bh * 0.68f, cx + bw * 2.8f, topY + bh * 0.82f, 3f, 3f, burnerPaint)

        burnerPaint.shader = null
        burnerPaint.color = Color.rgb(50, 50, 55)
        burnerPaint.strokeWidth = 3.5f
        burnerPaint.style = Paint.Style.STROKE
        val hoseX1 = cx + bw * 2.8f
        val hoseY1 = topY + bh * 0.75f
        val hoseX2 = cx + bw * 4.5f
        val hoseY2 = baseY + h * 0.04f
        path.reset()
        path.moveTo(hoseX1, hoseY1)
        path.cubicTo(hoseX1 + w * 0.06f, hoseY1, hoseX2 - w * 0.02f, hoseY2, hoseX2, hoseY2)
        canvas.drawPath(path, burnerPaint)
        burnerPaint.style = Paint.Style.FILL
    }

    private fun drawFlame(canvas: Canvas, cx: Float, baseY: Float, w: Float, h: Float) {
        val s = currentSalt
        val fH = h * 0.34f
        val fW = w * 0.055f

        val fx = sin(time * 3.7f) * 3.5f + sin(time * 7.3f) * 1.8f + cos(time * 11.1f) * 0.9f
        val fy = sin(time * 2.1f) * h * 0.012f + sin(time * 5.3f) * h * 0.006f
        val fw = cos(time * 4.7f) * 2.5f

        val tipX = cx + fx
        val tipY = baseY - fH + fy
        val curW = fW + fw

        flamePaint.shader = RadialGradient(
            cx, baseY - fH * 0.5f, fH * 0.75f,
            intArrayOf(colorWithAlpha(s.colorOuter, 50), Color.TRANSPARENT),
            floatArrayOf(0.25f, 1f), Shader.TileMode.CLAMP
        )
        path.reset()
        buildFlamePath(path, cx, baseY, tipX, tipY, curW * 3f)
        canvas.drawPath(path, flamePaint)

        flamePaint.shader = LinearGradient(
            cx, baseY, cx, tipY,
            intArrayOf(s.colorOuter, colorWithAlpha(s.colorOuter, 160), colorWithAlpha(s.colorOuter, 40), Color.TRANSPARENT),
            floatArrayOf(0f, 0.25f, 0.65f, 1f), Shader.TileMode.CLAMP
        )
        path.reset()
        buildFlamePath(path, cx, baseY, tipX, tipY, curW * 1.8f)
        canvas.drawPath(path, flamePaint)

        val midTipY = baseY - fH * 0.88f + fy * 0.6f
        flamePaint.shader = LinearGradient(
            cx, baseY, cx, midTipY,
            intArrayOf(colorWithAlpha(s.colorMid, 220), colorWithAlpha(s.colorMid, 180), colorWithAlpha(s.colorMid, 60), Color.TRANSPARENT),
            floatArrayOf(0f, 0.2f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        path.reset()
        buildFlamePath(path, cx, baseY, tipX + fx * 0.4f, midTipY, curW * 1.1f)
        canvas.drawPath(path, flamePaint)

        val innerTipY = baseY - fH * 0.52f + fy * 0.3f
        flamePaint.shader = LinearGradient(
            cx, baseY, cx, innerTipY,
            intArrayOf(Color.WHITE, colorWithAlpha(s.colorInner, 230), colorWithAlpha(s.colorInner, 80), Color.TRANSPARENT),
            floatArrayOf(0f, 0.3f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        path.reset()
        buildFlamePath(path, cx, baseY, cx + fx * 0.2f, innerTipY, curW * 0.48f)
        canvas.drawPath(path, flamePaint)

        flamePaint.shader = RadialGradient(
            cx, baseY - fH * 0.08f, fH * 0.06f,
            intArrayOf(Color.WHITE, colorWithAlpha(Color.WHITE, 0)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        flamePaint.alpha = 180
        canvas.drawCircle(cx, baseY - fH * 0.08f, fH * 0.06f, flamePaint)
        flamePaint.alpha = 255
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

    private fun updateSparks() {
        sparks.removeAll { it.life <= 0f }
        if (currentSalt.sparks && Random.nextFloat() < 0.5f) {
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w * 0.45f
            val bTop = h * 0.6f
            sparks.add(Spark(
                x = cx + (Random.nextFloat() - 0.5f) * 24f,
                y = bTop - h * 0.18f - Random.nextFloat() * h * 0.08f,
                vx = (Random.nextFloat() - 0.5f) * 7f,
                vy = -Random.nextFloat() * 5f - 2f,
                life = 1f, maxLife = 1f,
                size = Random.nextFloat() * 4f + 1.5f
            ))
        }
        val toRemove = mutableListOf<Spark>()
        sparks.forEach { s ->
            s.x += s.vx; s.y += s.vy; s.vy += 0.28f; s.life -= 0.022f
            if (s.life <= 0f) toRemove.add(s)
        }
        sparks.removeAll(toRemove)
    }

    private fun drawSparks(canvas: Canvas, cx: Float, burnerTopY: Float, w: Float, h: Float) {
        sparks.forEach { s ->
            val ratio = (s.life / s.maxLife).coerceIn(0f, 1f)
            val r = (220 + ratio * 35).toInt().coerceAtMost(255)
            val g = (140 + ratio * 115).toInt().coerceAtMost(255)
            val b = (30 + ratio * 70).toInt().coerceAtMost(255)
            flamePaint.color = Color.rgb(r, g, b)
            flamePaint.alpha = (ratio * 255).toInt()
            canvas.drawCircle(s.x, s.y, s.size * ratio, flamePaint)
            flamePaint.alpha = (ratio * 100).toInt()
            canvas.drawCircle(s.x, s.y, s.size * ratio * 2.5f, flamePaint)
        }
        flamePaint.alpha = 255
    }

    private fun drawWire(canvas: Canvas, cx: Float, burnerTopY: Float, w: Float, h: Float) {
        val startX = w * 0.88f
        val startY = burnerTopY - h * 0.1f
        val endX = cx + w * 0.015f
        val endY = burnerTopY - h * 0.18f
        val midX = (startX + endX) / 2f
        val midY = startY - h * 0.03f

        wirePaint.style = Paint.Style.STROKE
        wirePaint.strokeWidth = 2.5f
        wirePaint.color = Color.rgb(150, 150, 160)
        path.reset()
        path.moveTo(startX, startY)
        path.quadTo(midX, midY, endX, endY)
        canvas.drawPath(path, wirePaint)

        wirePaint.style = Paint.Style.FILL
        if (currentSalt.key != "none") {
            wirePaint.color = Color.rgb(255, 190, 80)
            canvas.drawCircle(endX, endY, 5.5f, wirePaint)
            wirePaint.color = Color.rgb(255, 230, 140)
            canvas.drawCircle(endX, endY, 3f, wirePaint)
            wirePaint.color = Color.rgb(240, 240, 240)
            canvas.drawCircle(endX, endY - 4f, 2.5f, wirePaint)
        } else {
            wirePaint.color = Color.rgb(130, 130, 140)
            canvas.drawCircle(endX, endY, 4f, wirePaint)
        }

        wirePaint.style = Paint.Style.FILL
    }

    private fun drawGasTank(canvas: Canvas, cx: Float, burnerTopY: Float, burnerH: Float, w: Float, h: Float) {
        val tankX = cx + w * 0.22f
        val tankTopY = burnerTopY + burnerH * 0.3f
        val tankW = w * 0.055f
        val tankH = h * 0.16f

        burnerPaint.shader = LinearGradient(
            tankX - tankW, tankTopY, tankX + tankW, tankTopY,
            intArrayOf(Color.rgb(30, 80, 30), Color.rgb(50, 130, 50), Color.rgb(35, 90, 35)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        burnerPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(tankX - tankW * 0.7f, tankTopY, tankX + tankW * 0.7f, tankTopY + tankH, 8f, 8f, burnerPaint)

        burnerPaint.shader = null
        burnerPaint.color = Color.rgb(40, 110, 40)
        canvas.drawRoundRect(tankX - tankW * 0.4f, tankTopY - tankH * 0.08f, tankX + tankW * 0.4f, tankTopY + tankH * 0.05f, 4f, 4f, burnerPaint)

        textPaint.color = Color.rgb(200, 255, 200)
        textPaint.textSize = w * 0.035f
        canvas.drawText("C₂H₂", tankX, tankTopY + tankH * 0.55f, textPaint)

        smallTextPaint.textSize = w * 0.022f
        smallTextPaint.color = Color.rgb(180, 180, 190)
        canvas.drawText("Asetilen", tankX, tankTopY + tankH * 0.72f, smallTextPaint)

        burnerPaint.color = Color.rgb(50, 50, 55)
        burnerPaint.strokeWidth = 2.5f
        burnerPaint.style = Paint.Style.STROKE
        val hX = cx + w * 0.045f * 2.8f
        val hY = burnerTopY + burnerH * 0.75f
        path.reset()
        path.moveTo(tankX - tankW * 0.7f, tankTopY + tankH * 0.5f)
        path.cubicTo(tankX - tankW * 1.2f, tankTopY + tankH * 0.5f, hX + w * 0.04f, hY, hX, hY)
        canvas.drawPath(path, burnerPaint)
        burnerPaint.style = Paint.Style.FILL

        val airLabelX = cx - w * 0.1f
        val airLabelY = burnerTopY + burnerH * 0.17f
        smallTextPaint.textSize = w * 0.024f
        smallTextPaint.color = Color.rgb(140, 180, 220)
        canvas.drawText("Hava", airLabelX, airLabelY, smallTextPaint)
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
