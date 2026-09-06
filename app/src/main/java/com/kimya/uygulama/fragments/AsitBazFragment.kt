package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import com.kimya.uygulama.utils.KimyaData
import com.kimya.uygulama.utils.PdfExporter
import com.kimya.uygulama.viewmodel.KimyaViewModel

// ─── pH renk tablosu (14 adimda smooth gradient) ───
private val PH_COLORS = intArrayOf(
    0xFFFF1744.toInt(), 0xFFFF3D00.toInt(), 0xFFFF6D00.toInt(), 0xFFFF9100.toInt(),
    0xFFFFC400.toInt(), 0xFFFFEA00.toInt(), 0xFFC6FF00.toInt(), 0xFF76FF03.toInt(),
    0xFF81C784.toInt(), 0xFF1DE9B6.toInt(), 0xFF4DD0E1.toInt(), 0xFF00B0FF.toInt(),
    0xFF2979FF.toInt(), 0xFF651FFF.toInt()
)

private fun phColorAt(ph: Float): Int {
    val idx = ph.toInt().coerceIn(0, 13)
    val f = ph - idx
    val c1 = PH_COLORS[idx]; val c2 = PH_COLORS[(idx + 1).coerceAtMost(13)]
    return Color.rgb(
        (Color.red(c1) * (1 - f) + Color.red(c2) * f).toInt(),
        (Color.green(c1) * (1 - f) + Color.green(c2) * f).toInt(),
        (Color.blue(c1) * (1 - f) + Color.blue(c2) * f).toInt()
    )
}

// ─── BUBBLE ───
private data class Bubble(
    val xBase: Float, val speedMul: Float, val radiusBase: Float,
    var y: Float = 0f, var wobblePhase: Float = 0f
)

// ─── ION PARTICLE ───
private data class IonParticle(
    var x: Float, var y: Float,
    val vx: Float, val vy: Float,
    val radius: Float, val isHydronium: Boolean,
    var life: Float = 1f
)

// ═══════════════════════════════════════════════════════════════
//  PhBeakerView — Gercekci bardak + sivi + kabarciklar + iyonlar
// ═══════════════════════════════════════════════════════════════
class PhBeakerView(context: Context) : View(context) {

    var phValue: Double? = null
    private var fillLevel = 0f
    private var targetFill = 0f
    private var wavePhase = 0f
    private var animT = 0f

    private val bubbles = Array(22) { i ->
        Bubble(
            xBase = 0.08f + (i * 0.041f) + (((i * 7 + 3) % 11) / 120f),
            speedMul = 0.6f + (i % 5) * 0.18f,
            radiusBase = 1.6f + (i % 4) * 0.6f
        )
    }

    private val hydroniumIons = mutableListOf<IonParticle>()
    private val hydroxideIons = mutableListOf<IonParticle>()
    private var currentHConc = 1e-7
    private var currentOHConc = 1e-7

    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E14.toInt() }
    private val glassBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF.toInt(); style = Paint.Style.FILL }
    private val glassStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val glassHighlight1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x18FFFFFF.toInt(); style = Paint.Style.FILL }
    private val glassHighlight2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x0CFFFFFF.toInt(); style = Paint.Style.FILL }
    private val markP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); strokeWidth = 1f }
    private val markTxtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); textSize = 7f; textAlign = Paint.Align.RIGHT }
    private val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x12FFFFFF.toInt(); style = Paint.Style.FILL }
    private val lipP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f; strokeCap = Paint.Cap.ROUND }
    private val bubbleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val probeBodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); style = Paint.Style.FILL }
    private val probeTipP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val probeLedP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val wireP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2A2A.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val phValueP = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true; textAlign = Paint.Align.CENTER }
    private val phLabelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textAlign = Paint.Align.CENTER }
    private val sideP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E7681.toInt(); textAlign = Paint.Align.CENTER }
    private val ionP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val hintP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E7681.toInt(); textAlign = Paint.Align.CENTER }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF12171F.toInt(); strokeWidth = 0.5f }

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.4f, 3f); invalidate(); return true }
    })

    private var beakerAnimator: ValueAnimator? = null

    init {
        isClickable = true; isFocusable = true
        beakerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
            addUpdateListener {
                animT += 0.025f; wavePhase += 0.07f
                fillLevel += (targetFill - fillLevel) * 0.055f
                for (b in bubbles) { b.y += 0.004f * b.speedMul; b.wobblePhase += 0.06f; if (b.y > 1f) b.y = 0f }
                for (ion in hydroniumIons) { ion.x += ion.vx * (Math.sin((animT * 3 + ion.y * 5).toDouble()).toFloat() * 0.7f); ion.y += ion.vy * (Math.cos((animT * 2.5 + ion.x * 4).toDouble()).toFloat() * 0.5f) }
                for (ion in hydroxideIons) { ion.x += ion.vx * (Math.cos((animT * 2.8 + ion.y * 6).toDouble()).toFloat() * 0.6f); ion.y += ion.vy * (Math.sin((animT * 3.2 + ion.x * 3).toDouble()).toFloat() * 0.8f) }
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() { beakerAnimator?.cancel(); super.onDetachedFromWindow() }

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

    private fun generateIons(pH: Float) {
        val hConc = Math.pow(10.0, -pH.toDouble()).toFloat()
        val ohConc = (1e-7f / Math.pow(10.0, -(pH - 7).toDouble()).toFloat()).coerceIn(1e-14f, 1f)
        currentHConc = Math.pow(10.0, -pH.toDouble())
        currentOHConc = 1e-14 / currentHConc
        val hCount = ((-Math.log10(hConc.toDouble()) * -2 + 15).toInt()).coerceIn(3, 25)
        val ohCount = ((-Math.log10(ohConc.toDouble()) * -2 + 15).toInt()).coerceIn(3, 25)
        val rnd = java.util.Random()
        hydroniumIons.clear()
        for (i in 0 until hCount) {
            hydroniumIons.add(IonParticle(x = 0.15f + rnd.nextFloat() * 0.7f, y = 0.25f + rnd.nextFloat() * 0.6f, vx = (rnd.nextFloat() - 0.5f) * 0.003f, vy = (rnd.nextFloat() - 0.5f) * 0.002f, radius = 2.5f + rnd.nextFloat() * 1.5f, isHydronium = true))
        }
        hydroxideIons.clear()
        for (i in 0 until ohCount) {
            hydroxideIons.add(IonParticle(x = 0.15f + rnd.nextFloat() * 0.7f, y = 0.25f + rnd.nextFloat() * 0.6f, vx = (rnd.nextFloat() - 0.5f) * 0.003f, vy = (rnd.nextFloat() - 0.5f) * 0.002f, radius = 2.5f + rnd.nextFloat() * 1.5f, isHydronium = false))
        }
    }

    fun animateToPh(ph: Double) {
        phValue = ph; targetFill = 0.92f
        generateIons(ph.toFloat()); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val sd = resources.displayMetrics.scaledDensity; val cx = w / 2f

        canvas.drawRect(0f, 0f, w, h, bgP)
        val gs = w * 0.06f
        for (gx in 0..(w / gs).toInt()) for (gy in 0..(h / gs).toInt()) canvas.drawCircle(gx * gs, gy * gs, 0.8f, gridP)

        canvas.save(); canvas.scale(zoomScale, zoomScale, cx, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val ph = phValue
        if (ph == null) {
            hintP.textSize = 13f * sd; hintP.alpha = (160 + 80 * Math.sin((animT * 2).toDouble()).toFloat()).toInt().coerceIn(100, 255)
            canvas.drawText("pH degerini girip", cx, h / 2f - 12f, hintP)
            canvas.drawText("Analiz Et'e basin", cx, h / 2f + 16f, hintP)
            hintP.alpha = 100; hintP.textSize = 10f * sd; canvas.drawText("↓", cx, h / 2f + 40f, hintP)
            canvas.restore(); return
        }

        val pH = ph.toFloat().coerceIn(0f, 14f); val mc = phColorAt(pH)
        val bkW = w * 0.44f; val bkH = h * 0.7f; val bkL = cx - bkW / 2f; val bkT = h * 0.2f; val bkB = bkT + bkH; val bkR = bkW * 0.06f

        canvas.drawRoundRect(RectF(bkL + 5f, bkT + 7f, bkL + bkW + 5f, bkB + 7f), bkR, bkR, shadowP)
        canvas.drawRoundRect(RectF(bkL, bkT, bkL + bkW, bkB), bkR, bkR, glassBody)
        canvas.drawRoundRect(RectF(bkL + 3f, bkT + 10f, bkL + 8f, bkB - 15f), 3f, 3f, glassHighlight1)
        canvas.drawRoundRect(RectF(bkL + 11f, bkT + 20f, bkL + 14f, bkB - 25f), 2f, 2f, glassHighlight2)
        canvas.drawRoundRect(RectF(bkL, bkT, bkL + bkW, bkB), bkR, bkR, glassStroke)
        canvas.drawLine(bkL - 5f, bkT, bkL + 10f, bkT, lipP)

        val dens = resources.displayMetrics.density; markTxtP.textSize = 7f * dens
        for (i in 1..5) {
            val my = bkB - bkH * (i / 6f); val ml = if (i % 2 == 0) bkW * 0.14f else bkW * 0.07f
            canvas.drawLine(bkL + 3f, my, bkL + 3f + ml, my, markP)
            if (i % 2 == 0) canvas.drawText("${i * 20}%", bkL + 3f + ml + 3f, my + 3f, markTxtP)
        }

        val liqTopFrac = 0.06f + fillLevel * 0.68f; val liqTop = bkB - bkH * liqTopFrac; val liqBot = bkB - 2f
        val liqColor = mc and 0x00FFFFFF.toInt() or 0x99000000.toInt()
        val liqGrad = LinearGradient(0f, liqTop, 0f, liqBot, liqColor, (mc and 0x00FFFFFF.toInt() or 0x55000000.toInt()), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(bkL + 2f, liqTop + 8f, bkL + bkW - 2f, liqBot), bkR - 1f, bkR - 1f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = liqGrad })

        if (fillLevel > 0.02f) {
            val meniscusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = liqColor; style = Paint.Style.FILL }
            val mPath = Path(); mPath.moveTo(bkL + 2f, liqBot); mPath.lineTo(bkL + 2f, liqTop + 10f)
            val ww = bkW - 4f; val w1 = liqTop + 8f + 3.5f * Math.sin(wavePhase.toDouble()).toFloat(); val w2 = liqTop + 11f + 2.8f * Math.cos(wavePhase * 1.4).toFloat(); val w3 = liqTop + 9f + 3f * Math.sin(wavePhase * 0.8 + 1.2).toFloat()
            mPath.cubicTo(bkL + 2f + ww * 0.22f, w1, bkL + 2f + ww * 0.48f, w2, bkL + 2f + ww * 0.72f, w3)
            mPath.cubicTo(bkL + 2f + ww * 0.88f, liqTop + 10f, bkL + ww, liqTop + 9f, bkL + bkW - 2f, liqTop + 10f)
            mPath.lineTo(bkL + bkW - 2f, liqBot); mPath.close(); canvas.drawPath(mPath, meniscusPaint)
        }

        if (fillLevel > 0.02f) {
            for (ion in hydroniumIons) {
                val ix = bkL + 6f + ion.x * (bkW - 12f); val iy = liqTop + 12f + ion.y * (liqBot - liqTop - 16f)
                if (iy < liqTop + 8f || iy > liqBot - 4f) continue
                canvas.drawCircle(ix, iy, ion.radius * 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(ix, iy, ion.radius * 2.5f, intArrayOf(0x44FF4444.toInt(), 0x00FF4444.toInt()), null, Shader.TileMode.CLAMP) })
                ionP.color = 0xFFFF5252.toInt(); canvas.drawCircle(ix, iy, ion.radius, ionP)
                if (ion.radius > 3f) { val ionLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 5f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true }; canvas.drawText("H3O+", ix, iy + ion.radius + 7f, ionLbl) }
            }
            for (ion in hydroxideIons) {
                val ix = bkL + 6f + ion.x * (bkW - 12f); val iy = liqTop + 12f + ion.y * (liqBot - liqTop - 16f)
                if (iy < liqTop + 8f || iy > liqBot - 4f) continue
                canvas.drawCircle(ix, iy, ion.radius * 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(ix, iy, ion.radius * 2.5f, intArrayOf(0x444488FF.toInt(), 0x004488FF.toInt()), null, Shader.TileMode.CLAMP) })
                ionP.color = 0xFF448AFF.toInt(); canvas.drawCircle(ix, iy, ion.radius, ionP)
                if (ion.radius > 3f) { val ionLbl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 5f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true }; canvas.drawText("OH-", ix, iy + ion.radius + 7f, ionLbl) }
            }
        }

        if (fillLevel > 0.02f) {
            for (b in bubbles) {
                val bx = bkL + 5f + b.xBase * (bkW - 10f); val by = liqBot - b.y * (liqTopFrac * bkH)
                if (by < liqTop + 6f || by > liqBot - 2f) continue
                val wobble = 2f * Math.sin(b.wobblePhase.toDouble()).toFloat(); val br = b.radiusBase * (1f + 0.15f * Math.sin((b.y * 4).toDouble()).toFloat())
                bubbleP.color = Color.argb((28 + (b.y * 45).toInt()).coerceAtMost(70), 255, 255, 255); canvas.drawCircle(bx + wobble, by, br, bubbleP)
                bubbleP.color = Color.argb(18, 255, 255, 255); canvas.drawCircle(bx + wobble - br * 0.2f, by - br * 0.2f, br * 0.35f, bubbleP)
            }
        }

        val condP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for ((fx, fy) in listOf(0.08f to 0.15f, 0.92f to 0.22f, 0.05f to 0.45f, 0.95f to 0.38f, 0.12f to 0.65f, 0.88f to 0.55f)) {
            val dx = bkL + fx * bkW; val dy = bkT + fy * bkH; val dropR = 1.5f + fy * 1.2f
            if (fx < 0.18f || fx > 0.82f) {
                condP.color = Color.argb((15 + (20 * Math.sin((animT * 0.5 + fy * 3).toDouble())).toInt()).coerceIn(8, 35), 200, 220, 240); canvas.drawCircle(dx, dy, dropR, condP)
            }
        }

        val prX = bkL + bkW * 0.8f; val prTop = bkT - h * 0.14f; val prBot = liqTop + 14f; val prW = 7f
        canvas.drawRoundRect(RectF(prX - prW / 2f, prTop, prX + prW / 2f, prBot), 3f, 3f, probeBodyP)
        canvas.drawRect(RectF(prX - prW / 2f + 1f, prTop + 5f, prX, prBot - 3f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); style = Paint.Style.FILL })
        probeTipP.color = mc; canvas.drawRoundRect(RectF(prX - prW / 2f - 1.5f, prBot - 12f, prX + prW / 2f + 1.5f, prBot + 3f), 4f, 4f, probeTipP)
        if (fillLevel > 0.02f) { canvas.drawCircle(prX, prBot, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(prX, prBot, 14f, intArrayOf(mc and 0x00FFFFFF.toInt() or 0x44000000.toInt(), 0x00000000.toInt()), null, Shader.TileMode.CLAMP) }) }
        val ledOn = fillLevel > 0.02f; probeLedP.color = if (ledOn) 0xFF81C784.toInt() else 0xFF333333.toInt(); canvas.drawCircle(prX, prTop + 10f, 3f, probeLedP)
        if (ledOn) { canvas.drawCircle(prX, prTop + 10f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(prX, prTop + 10f, 8f, intArrayOf(0x5539FF14.toInt(), 0x00000000.toInt()), null, Shader.TileMode.CLAMP) }) }

        val wPath = Path(); wPath.moveTo(prX, prTop)
        wPath.cubicTo(prX + w * 0.12f, prTop - 8f, prX + w * 0.22f, prTop + 12f, bkL + bkW + w * 0.08f, prTop - 3f)
        canvas.drawPath(wPath, wireP)

        // UST: Asit/Baz/Notr etiketi (beherin ustunde, probe seviyesinde)
        val tipText = when { pH < 4 -> "KUVVETLI ASIT"; pH < 7 -> "ZAYIF ASIT"; pH == 7f -> "NOTR"; pH < 10 -> "ZAYIF BAZ"; else -> "KUVVETLI BAZ" }
        val topLabelY = bkT - h * 0.1f
        val topLabelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mc; textSize = 11f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true; letterSpacing = 0.1f }
        canvas.drawText(tipText, cx, topLabelY, topLabelP)

        // ALT: pH sayisi (metrenin altinda, beherin hemen altinda)
        val phNumY = bkB + 22f
        phValueP.textSize = w * 0.08f; phValueP.color = mc
        phValueP.setShadowLayer(12f, 0f, 0f, mc and 0x00FFFFFF.toInt() or 0x66000000.toInt())
        canvas.drawText("${"%.2f".format(ph)}", cx, phNumY, phValueP)
        val phSubP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textSize = 8f * sd; textAlign = Paint.Align.CENTER }
        canvas.drawText("pH Degeri", cx, phNumY + 12f * sd, phSubP)

        // Side labels (beherin icinde, alt kisim)
        val sideX1 = bkL + 8f; val sideX2 = bkL + bkW - 8f; val sideY = bkB - 20f
        sideP.textSize = 6.5f * sd
        canvas.drawText("[H+]", sideX1, sideY, sideP.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("[OH-]", sideX2, sideY, sideP.apply { textAlign = Paint.Align.RIGHT })
        sideP.textSize = 6f * sd; sideP.color = mc
        canvas.drawText("${"%.1e".format(currentHConc)}", sideX1, sideY + 9f * sd, sideP)
        canvas.drawText("${"%.1e".format(currentOHConc)}", sideX2, sideY + 9f * sd, sideP)
        sideP.color = 0xFF6E7681.toInt()

        canvas.drawOval(cx - bkW * 0.45f, bkB - 3f, cx + bkW * 0.45f, bkB + 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, bkB + 5f, bkW * 0.45f, intArrayOf(mc and 0x00FFFFFF.toInt() or 0x18000000.toInt(), 0x00000000.toInt()), null, Shader.TileMode.CLAMP) })

        // INDICATOR (buyuk ve net)
        val indY = phNumY + 26f * sd
        val indicator = when { pH < 4 -> "Metil Oranj"; pH > 8.2 -> "Fenolftalein"; else -> "Evrensel Indikator" }
        val indP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB0C4DE.toInt(); textSize = 9.5f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("Indikator: $indicator", cx, indY, indP)

        // SAFETY (en alt, buyuk ve net)
        val safetyY = indY + 14f * sd
        val safetyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 10f * sd; isFakeBoldText = true }
        val safety = when { pH <= 2 || pH >= 12 -> { safetyP.color = 0xFFFF5252.toInt(); "KOROZIF - Eldiven ve gozluk sart" }; pH < 4 || pH > 10 -> { safetyP.color = 0xFFFFC400.toInt(); "DIKKAT: Dikkatli kullanin" }; else -> { safetyP.color = 0xFF81C784.toInt(); "GUVENLI KULLANIM" } }
        canvas.drawText(safety, cx, safetyY, safetyP)
        canvas.restore()
    }
}

// ═══════════════════════════════════════════════════════════════
//  PhScaleView — Intерактив pH skala cubugu
// ═══════════════════════════════════════════════════════════════
class PhScaleView(context: Context) : View(context) {
    var phValue: Double? = null
    var onPhTouched: ((Double) -> Unit)? = null
    private var indicatorX = 0f; private var animT = 0f
    private var scaleAnimator: ValueAnimator? = null

    init {
        isClickable = true; isFocusable = true
        scaleAnimator = ValueAnimator.ofFloat(0f, 1f).apply { duration = 1800L; repeatCount = ValueAnimator.INFINITE; addUpdateListener { animT += 0.02f; invalidate() }; start() }
    }

    override fun onDetachedFromWindow() { scaleAnimator?.cancel(); super.onDetachedFromWindow() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { handleTouch(e.x); return true }
        }
        return true
    }

    private fun handleTouch(x: Float) {
        val w = width.toFloat(); val margin = 24f; val barW = w - margin * 2f
        val ph = ((x - margin) / barW * 14.0).coerceIn(0.0, 14.0)
        onPhTouched?.invoke(ph)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat(); if (w <= 0 || h <= 0) return
        val sd = resources.displayMetrics.scaledDensity
        canvas.drawRect(0f, 0f, w, h, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E14.toInt() })
        val margin = 24f; val barW = w - margin * 2f; val barH = 12f; val barY = h * 0.42f

        val cols = IntArray(28); val pos = FloatArray(28)
        for (i in 0..27) { val p = i * 14f / 27f; cols[i] = phColorAt(p); pos[i] = i / 27f }
        canvas.drawRoundRect(RectF(margin, barY, margin + barW, barY + barH), 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(margin, 0f, margin + barW, 0f, cols, pos, Shader.TileMode.CLAMP) })
        canvas.drawRoundRect(RectF(margin, barY, margin + barW, barY + barH), 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f })

        val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); strokeWidth = 1f }
        val lP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textSize = 8.5f * sd; textAlign = Paint.Align.CENTER }
        for (ph in 0..14) { val x = margin + (ph / 14f) * barW; canvas.drawLine(x, barY + barH + 2f, x, barY + barH + 6f, tP); canvas.drawText("$ph", x, barY + barH + 17f, lP) }

        val zP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        zP.color = 0xFFFF4444.toInt(); canvas.drawText("ASIT", margin + barW * 0.15f, barY - 5f, zP)
        zP.color = 0xFF76FF03.toInt(); canvas.drawText("NOTR", margin + barW * 0.5f, barY - 5f, zP)
        zP.color = 0xFF4488FF.toInt(); canvas.drawText("BAZ", margin + barW * 0.85f, barY - 5f, zP)

        val ph = phValue
        if (ph != null && ph in 0.0..14.0) {
            val targetX = margin + (ph / 14.0 * barW).toFloat(); indicatorX += (targetX - indicatorX) * 0.14f
            val mc = phColorAt(ph.toFloat()); val pulse = 0.55f + 0.45f * Math.sin(animT * 4.0).toFloat()
            canvas.drawCircle(indicatorX, barY + barH / 2f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(indicatorX, barY + barH / 2f, 16f, intArrayOf(mc and 0x00FFFFFF.toInt() or ((0x44 * pulse).toInt().shl(24).coerceAtMost(0xFF000000.toInt())), 0x00000000.toInt()), null, Shader.TileMode.CLAMP) })
            canvas.drawLine(indicatorX, barY - 3f, indicatorX, barY + barH + 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF81C784.toInt(); strokeWidth = 2.5f; style = Paint.Style.STROKE })
            val tri = Path(); tri.moveTo(indicatorX - 5f, barY + barH + 4f); tri.lineTo(indicatorX + 5f, barY + barH + 4f); tri.lineTo(indicatorX, barY + barH + 13f); tri.close()
            canvas.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF81C784.toInt(); style = Paint.Style.FILL })
            canvas.drawText("%.2f".format(ph), indicatorX, barY - 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = mc; textSize = 11f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true })
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  TitrationEgrisiView — Animasyonlu titrasyon egrisi
// ═══════════════════════════════════════════════════════════════
class TitrationEgrisiView(context: Context) : View(context) {
    var eqPoint: Float = 7f; var eqVolume: Float = 25f
    var isStrongAcid = true
    var animProgress = 0f
    private val sd = resources.displayMetrics.scaledDensity
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E14.toInt() }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF12171F.toInt(); strokeWidth = 0.5f }
    private val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A3441.toInt(); strokeWidth = 1.5f }
    private val curveP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; isAntiAlias = true }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun animateIn() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500; interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animProgress = it.animatedValue as Float; invalidate() }; start()
        }
    }

    private fun titrationPH(vol: Float): Float {
        val frac = (vol / eqVolume).coerceIn(0.001f, 10f)
        return if (isStrongAcid) {
            if (frac < 0.99f) (2f + 3f * frac).coerceIn(1f, 6.5f)
            else if (frac < 1.01f) eqPoint
            else (14f - 12f / frac).coerceIn(7.5f, 13f)
        } else {
            if (frac < 0.99f) (3f + 2.5f * frac).coerceIn(2f, 6.5f)
            else if (frac < 1.01f) eqPoint
            else (14f - 10f / frac).coerceIn(7.5f, 12.5f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat(); if (w <= 0 || h <= 0) return
        canvas.drawRect(0f, 0f, w, h, bgP)
        val gs = w * 0.06f
        for (gx in 0..(w / gs).toInt()) for (gy in 0..(h / gs).toInt()) canvas.drawCircle(gx * gs, gy * gs, 0.5f, gridP)

        val padL = w * 0.16f; val padR = w * 0.1f; val padT = h * 0.14f; val padB = h * 0.26f
        val plotW = w - padL - padR; val plotH = h - padT - padB

        canvas.drawLine(padL, padT, padL, padT + plotH, axisP)
        canvas.drawLine(padL, padT + plotH, padL + plotW, padT + plotH, axisP)
        labelP.textSize = 8f * sd; labelP.color = 0xFF6E7681.toInt()
        canvas.drawText("pH", padL - 8f, padT - 4f, labelP.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("Baz (mL) ->", padL + plotW / 2f, padT + plotH + 16f, labelP.apply { textAlign = Paint.Align.CENTER })

        // Tick marks
        val tickP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FFFFFF.toInt(); strokeWidth = 1f }
        val tickLP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E7681.toInt(); textSize = 7f * sd; textAlign = Paint.Align.CENTER }
        for (ph in listOf(0f, 4f, 7f, 10f, 14f)) {
            val y = padT + plotH * (1f - ph / 14f)
            canvas.drawLine(padL - 4f, y, padL, y, tickP)
            canvas.drawText("%.0f".format(ph), padL - 7f, y + 3f, tickLP.apply { textAlign = Paint.Align.RIGHT })
        }
        for (v in listOf(0f, 10f, 20f, 30f, 40f, 50f)) {
            val x = padL + (v / 50f) * plotW
            canvas.drawLine(x, padT + plotH, x, padT + plotH + 4f, tickP)
            canvas.drawText("%.0f".format(v), x, padT + plotH + 13f, tickLP)
        }

        // Curve
        val path = Path(); val pts = mutableListOf<Pair<Float, Float>>()
        val maxDraw = (animProgress * 100).toInt()
        for (i in 0..maxDraw) {
            val vol = i * 50f / 100f; val ph = titrationPH(vol)
            val tx = padL + (vol / 50f) * plotW; val ty = padT + plotH * (1f - ph / 14f)
            pts.add(tx to ty)
            if (i == 0) path.moveTo(tx, ty) else path.lineTo(tx, ty)
        }
        glowP.color = 0x4400F0FF.toInt(); canvas.drawPath(path, glowP)
        curveP.color = 0xFF4DD0E1.toInt(); canvas.drawPath(path, curveP)

        // Fill
        if (pts.size > 2) {
            val fillPath = Path(path); fillPath.lineTo(pts.last().first, padT + plotH); fillPath.lineTo(pts.first().first, padT + plotH); fillPath.close()
            fillP.shader = LinearGradient(0f, padT, 0f, padT + plotH, intArrayOf(0x2200F0FF.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
            canvas.drawPath(fillPath, fillP); fillP.shader = null
        }

        // Equivalence point dot
        if (animProgress > 0.5f) {
            val eqX = padL + (eqVolume / 50f) * plotW; val eqY = padT + plotH * (1f - eqPoint / 14f)
            dotP.color = 0xFFFF6B6B.toInt(); canvas.drawCircle(eqX, eqY, 5f * sd, dotP)
            canvas.drawCircle(eqX, eqY, 10f * sd, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(eqX, eqY, 10f * sd, intArrayOf(0x55FF6B6B.toInt(), 0x00000000.toInt()), null, Shader.TileMode.CLAMP) })
            labelP.textSize = 8f * sd; labelP.color = 0xFFFF6B6B.toInt()
            canvas.drawText("Eslesme Noktasi", eqX, eqY - 12f * sd, labelP)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Fragment
// ═══════════════════════════════════════════════════════════════
class AsitBazFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private var asitindex = 0; private var bazIndex = 0
    private var sonPhSonuc = ""

    private data class CardRefs(val card: LinearLayout, val nameTv: TextView, val checkTv: TextView)
    private val asitCardsList = mutableListOf<CardRefs>()
    private val bazCardsList = mutableListOf<CardRefs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_asitbaz, container, false)
        val deger = v.findViewById<EditText>(R.id.ab_deger)
        val tur = v.findViewById<Spinner>(R.id.ab_tur)
        val sonuc = v.findViewById<TextView>(R.id.ab_sonuc)
        val katSonuc = v.findViewById<TextView>(R.id.ab_katalog_sonuc)
        val dens = resources.displayMetrics.density

        // Beaker custom view
        val bkPh = v.findViewById<View>(R.id.ab_beaker_view)
        val bkPar = bkPh.parent as ViewGroup; val bkI = bkPar.indexOfChild(bkPh); bkPar.removeView(bkPh)
        val beakerView = PhBeakerView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (220 * dens).toInt()) }
        bkPar.addView(beakerView, bkI)

        // Scale custom view
        val scPh = v.findViewById<View>(R.id.ab_scale_view)
        val scPar = scPh.parent as ViewGroup; val scI = scPar.indexOfChild(scPh); scPar.removeView(scPh)
        val scaleView = PhScaleView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (64 * dens).toInt()) }
        scPar.addView(scaleView, scI)

        // Titration curve custom view
        val titPh = v.findViewById<View>(R.id.ab_titration_placeholder)
        val titPar = titPh.parent as ViewGroup; val titI = titPar.indexOfChild(titPh); titPar.removeView(titPh)
        val titrationView = TitrationEgrisiView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (160 * dens).toInt()) }
        titPar.addView(titrationView, titI)

        tur.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, arrayOf("pH", "pOH", "[H+]", "[OH-]"))

        fun analizYap() {
            try {
                val d = deger.text.toString().replace(",", ".").toDoubleOrNull() ?: return
                val t = tur.selectedItem.toString()
                val r = KimyaData.phHesapla(d, t)
                if (r.containsKey("hata")) { sonuc.text = r["hata"] as String; return }
                val ph = r["pH"] as Double
                beakerView.animateToPh(ph)
                scaleView.phValue = ph; scaleView.invalidate()
                titrationView.isStrongAcid = ph < 7; titrationView.animProgress = 0f; titrationView.animateIn()

                val indicator = when { ph < 4 -> "Metil Oranj (kirmizi -> sari)"; ph > 8.2 -> "Fenolftalein (renksiz -> pembe)"; else -> "Evrensel Indikator" }
                val guvenlik = when { ph <= 2 || ph >= 12 -> "Korozif - eldiven ve gozluk sart"; else -> "Standart onlemler yeterli" }
                sonPhSonuc = "pH = ${"%.2f".format(ph)}\npOH = ${"%.2f".format(r["pOH"] as Double)}\n[H+] = ${"%.4e".format(r["[H+]"] as Double)}\n[OH-] = ${"%.4e".format(r["[OH-]"] as Double)}\nTur: ${r["tur"]}\nIndikator: $indicator\nGuvenlik: $guvenlik"
                sonuc.text = sonPhSonuc
                v.findViewById<View>(R.id.ab_result_card)?.let { it.visibility = View.VISIBLE; AnimUtils.popIn(it) }
                vm.addHistory("pH Analizi", "pH=${"%.2f".format(ph)} (${r["tur"]})")
            } catch (e: Exception) { sonuc.text = "Hata: ${e.message}" }
        }

        v.findViewById<View>(R.id.ab_analiz)?.setOnClickListener { AnimUtils.press(it); analizYap() }
        scaleView.onPhTouched = { tappedPh -> deger.setText("%.1f".format(tappedPh)); tur.setSelection(0); analizYap() }

        fun preset(pH: Double) { deger.setText("%.0f".format(pH)); tur.setSelection(0); analizYap() }
        v.findViewById<View>(R.id.ab_p1)?.setOnClickListener { AnimUtils.press(it); preset(1.0) }
        v.findViewById<View>(R.id.ab_p4)?.setOnClickListener { AnimUtils.press(it); preset(4.0) }
        v.findViewById<View>(R.id.ab_p7)?.setOnClickListener { AnimUtils.press(it); preset(7.0) }
        v.findViewById<View>(R.id.ab_p9)?.setOnClickListener { AnimUtils.press(it); preset(9.0) }
        v.findViewById<View>(R.id.ab_p13)?.setOnClickListener { AnimUtils.press(it); preset(13.0) }

        // Henderson-Hasselbalch
        v.findViewById<View>(R.id.ab_hh_btn)?.setOnClickListener { btn ->
            AnimUtils.press(btn)
            val cBase = v.findViewById<EditText>(R.id.ab_hh_cbase)?.text.toString().replace(",", ".").toDoubleOrNull()
            val cAcid = v.findViewById<EditText>(R.id.ab_hh_cacid)?.text.toString().replace(",", ".").toDoubleOrNull()
            val pKa = v.findViewById<EditText>(R.id.ab_hh_pka)?.text.toString().replace(",", ".").toDoubleOrNull()
            val hhResult = v.findViewById<TextView>(R.id.ab_hh_result)
            if (cBase == null || cAcid == null || pKa == null || cAcid <= 0 || cBase <= 0) { hhResult.text = "Tum degerleri girin (derisimler pozitif olmali)"; hhResult.visibility = View.VISIBLE; return@setOnClickListener }
            val ph = pKa + Math.log10(cBase / cAcid)
            val ratio = cBase / cAcid
            val bufferCapacity = when { ratio in 0.1..10.0 -> "Iyi tampon"; ratio < 0.1 -> "Asit agirlikli"; else -> "Baz agirlikli" }
            hhResult.text = "Henderson-Hasselbalch:\npH = pKa + log([B]/[A])\npH = ${"%.2f".format(pKa)} + log(${"%.2f".format(cBase)}/${"%.2f".format(cAcid)})\npH = ${"%.2f".format(ph)}\n\nOran [B]/[A] = ${"%.3f".format(ratio)}\nTampon durumu: $bufferCapacity"
            hhResult.visibility = View.VISIBLE; AnimUtils.popIn(hhResult)
            beakerView.animateToPh(ph); scaleView.phValue = ph; scaleView.invalidate()
        }

        fun updateSelection() {
            // asit/baz secimi spinner ile yapildigindan boyle kaldi
        }

        // Katalog: Spinner ile asit/baz secimi
        val asitAdlari = KimyaData.asitler.map { "${it.adi} (${it.formulu})" }.toTypedArray()
        val bazAdlari = KimyaData.bazlar.map { "${it.adi} (${it.formulu})" }.toTypedArray()

        val asitSpinner = v.findViewById<Spinner>(R.id.ab_asit_spinner)
        val bazSpinner = v.findViewById<Spinner>(R.id.ab_baz_spinner)
        val asitDetail = v.findViewById<TextView>(R.id.ab_asit_detail)
        val bazDetail = v.findViewById<TextView>(R.id.ab_baz_detail)

        asitSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, asitAdlari)
        bazSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, bazAdlari)

        asitSpinner?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                asitindex = pos; val a = KimyaData.asitler[pos]
                asitDetail?.text = "Tur: ${a.tur}\nFormul: ${a.formulu}\npH: ${a.pH}\n\nOzellik:\n${a.ozellik}\n\nKullanim:\n${a.kullanim}\n\nGuvenlik: ${a.guvenlik}"
                asitDetail?.visibility = View.VISIBLE; AnimUtils.popIn(asitDetail)
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
        bazSpinner?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                bazIndex = pos; val b = KimyaData.bazlar[pos]
                bazDetail?.text = "Tur: ${b.tur}\nFormul: ${b.formulu}\npH: ${b.pH}\n\nOzellik:\n${b.ozellik}\n\nKullanim:\n${b.kullanim}\n\nGuvenlik: ${b.guvenlik}"
                bazDetail?.visibility = View.VISIBLE; AnimUtils.popIn(bazDetail)
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        // Asit "Kullan" butonu
        v.findViewById<View>(R.id.ab_asit_kullan)?.setOnClickListener { btn ->
            AnimUtils.press(btn)
            val a = KimyaData.asitler[asitindex]
            deger.setText(a.pH.split("-").first().trim().toDoubleOrNull()?.let { "%.1f".format(it) } ?: "3.0")
            tur.setSelection(0); analizYap()
        }
        // Baz "Kullan" butonu
        v.findViewById<View>(R.id.ab_baz_kullan)?.setOnClickListener { btn ->
            AnimUtils.press(btn)
            val b = KimyaData.bazlar[bazIndex]
            deger.setText(b.pH.split("-").last().trim().toDoubleOrNull()?.let { "%.1f".format(it) } ?: "12.0")
            tur.setSelection(0); analizYap()
        }

        v.findViewById<View>(R.id.ab_notrlesme)?.setOnClickListener { btn ->
            AnimUtils.press(btn)
            val asit = KimyaData.asitler[asitindex]; val baz = KimyaData.bazlar[bazIndex]
            val aK = asit.tur.contains("Kuvvetli"); val bK = baz.tur.contains("Kuvvetli")
            val tahmini = when { aK && bK -> "~7 (Notr)"; aK && !bK -> ">7 (hafif bazik)"; !aK && bK -> "<7 (hafif asidik)"; else -> "~7" }
            katSonuc.text = "Notrlesme\n${asit.formulu} + ${baz.formulu} -> Tuz + H2O\n${asit.adi} + ${baz.adi}\nTahmini pH: $tahmini"
            katSonuc.visibility = View.VISIBLE; AnimUtils.popIn(katSonuc)
        }

        v.findViewById<View>(R.id.ab_paylas)?.setOnClickListener {
            if (sonPhSonuc.isEmpty()) { Toast.makeText(context, "Once analiz yapin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PdfExporter.shareText(requireContext(), "pH Analizi", sonPhSonuc)
        }

        v.findViewById<View>(R.id.ab_help)?.setOnClickListener {
            AnimUtils.press(it)
            HelpDialog.showGuide(requireContext(), "pH ve Asit-Baz Analizi", "pH, bir cozeltinin asitlik/bazlik derecesini olcer (0-14).", listOf(
                "Deger girin, Analiz Et'e basin - bardak animasyonlanir.",
                "Skalaya dokunarak hizlica pH ayarlayabilirsiniz.",
                "Kirmizi noktalar H3O+, mavi noktalar OH- iyonlaridir.",
                "Titrasyon egrisi eslesme noktasini gosterir.",
                "Henderson-Hasselbalch ile tampon cozelti pH'si hesaplayin.",
                "Katalogdan madde secip ▸ Kullan ile hizlica deneyin."
            ))
        }

        // Staggered entrance animations
        val staggerIds = listOf(R.id.ab_header, R.id.ab_scene_card, R.id.ab_chips_card, R.id.ab_input_card, R.id.ab_titration_card, R.id.ab_hh_card, R.id.ab_catalog_card)
        staggerIds.forEachIndexed { idx, id ->
            v.findViewById<View>(id)?.let { it.alpha = 0f; it.translationY = 40f * dens; it.post { AnimUtils.slideUpFade(it, idx * 80L) } }
        }
        v.findViewById<TextView>(R.id.ab_title)?.let { AnimUtils.gradientTitle(it) }

        return v
    }
}

