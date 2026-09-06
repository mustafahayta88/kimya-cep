package com.kimya.uygulama.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import com.kimya.uygulama.utils.KimyaData
import com.kimya.uygulama.utils.PdfExporter
import com.kimya.uygulama.utils.ReactionBalancer
import com.kimya.uygulama.viewmodel.KimyaViewModel
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════
//  ReactionSceneView — Parçacık sistemi v2
//  Zoom, pan, çarpışma efekti, elektron transferi, enerji dalgası
// ═══════════════════════════════════════════════════════════════
class ReactionSceneView(context: Context) : View(context) {

    private data class Particle(
        val formula: String, val color: Int, val radius: Float,
        var x: Float = 0f, var y: Float = 0f,
        var vx: Float = 0f, var vy: Float = 0f,
        var targetX: Float = 0f, var targetY: Float = 0f,
        var alpha: Int = 255, var scale: Float = 1f,
        var label: String = formula
    )

    private val eColor = mapOf(
        "H" to 0xFFE0E0E0.toInt(), "O" to 0xFFFF5252.toInt(), "N" to 0xFF448AFF.toInt(),
        "C" to 0xFF616161.toInt(), "Na" to 0xFFB388FF.toInt(), "Cl" to 0xFF69F0AE.toInt(),
        "Fe" to 0xFFFFA726.toInt(), "S" to 0xFFFFEE58.toInt(), "Ca" to 0xFF26C6DA.toInt(),
        "Al" to 0xFFB0BEC5.toInt(), "K" to 0xFFCE93D8.toInt(), "Mg" to 0xFFA5D6A7.toInt(),
        "P" to 0xFFFF80AB.toInt(), "Br" to 0xFFA1887F.toInt(), "F" to 0xFF80DEEA.toInt(),
        "Li" to 0xFFFF8A80.toInt(), "Ba" to 0xFF80CBC4.toInt(), "I" to 0xFF9575CD.toInt()
    )

    private val reactants = mutableListOf<Particle>()
    private val products = mutableListOf<Particle>()
    private var phase = 0f
    private var reactionT = 0f
    private var isAnimating = false
    private var flashR = 0f; private var flashA = 0f
    private var waveR = 0f; private var waveA = 0f
    private var sparkles = mutableListOf<Triple<Float, Float, Float>>()
    private var pendingReaction = false
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sd = resources.displayMetrics.scaledDensity
    private val sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.4f, 3f); invalidate(); return true
        }
    })

    var reactant1 = ""; var reactant2 = ""
    var product1 = ""; var product2 = ""
    var condition = ""; var reactionType = ""
    var coefficients = intArrayOf()

    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E14.toInt() }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF12171F.toInt(); strokeWidth = 0.5f }
    private val flashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val waveP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = 0xFF4DD0E1.toInt() }
    private val molP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val molTxtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val molOutP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A1218.toInt(); textAlign = Paint.Align.CENTER; style = Paint.Style.STROKE; strokeWidth = 3f; isFakeBoldText = true }
    private val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4DD0E1.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val arrowFillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4DD0E1.toInt(); style = Paint.Style.FILL }
    private val typeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val condP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF81C784.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E7681.toInt(); textAlign = Paint.Align.CENTER }
    private val sparkP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val electronP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFEE58.toInt(); style = Paint.Style.FILL }
    private val electronPath = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFEE58.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) }
    private val hintP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E7681.toInt(); textAlign = Paint.Align.CENTER }
    private val energyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FF6B6B.toInt(); style = Paint.Style.FILL }

    private var phaseAnimator: ValueAnimator? = null

    init {
        isClickable = true; isFocusable = true
        phaseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3500; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() { phaseAnimator?.cancel(); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pendingReaction) { pendingReaction = false; startReaction() }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_MOVE -> {
                if (tMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }
                lastTx = e.x; lastTy = e.y; invalidate()
            }
            MotionEvent.ACTION_UP -> { tMode = 0; return true }
        }
        return true
    }

    private fun colorFor(f: String): Int {
        val el = f.replace(Regex("[0-9]"), "").take(2)
        return eColor[el] ?: eColor[el.take(1)] ?: 0xFFB388FF.toInt()
    }

    private fun makeParticles(formulas: String): List<Particle> {
        return formulas.split("+").filter { it.isNotBlank() }.map { f ->
            val txt = f.trim()
            Particle(txt, colorFor(txt), 26f * sd)
        }
    }

    fun startReaction() {
        if (reactant1.isEmpty()) return
        if (width <= 0 || height <= 0) { pendingReaction = true; return }
        val w = width.toFloat(); val h = height.toFloat(); val cx = w * 0.5f; val cy = h * 0.5f

        reactants.clear(); products.clear(); sparkles.clear(); reactionT = 0f

        val allR = mutableListOf<String>()
        if (reactant1.isNotEmpty()) allR.addAll(reactant1.split("+").filter { it.isNotBlank() }.map { it.trim() })
        if (reactant2.isNotEmpty()) allR.addAll(reactant2.split("+").filter { it.isNotBlank() }.map { it.trim() })
        val allP = mutableListOf<String>()
        if (product1.isNotEmpty()) allP.addAll(product1.split("+").filter { it.isNotBlank() }.map { it.trim() })
        if (product2.isNotEmpty()) allP.addAll(product2.split("+").filter { it.isNotBlank() }.map { it.trim() })

        val r = 24f * sd
        val leftX = w * 0.2f; val rightX = w * 0.8f

        allR.forEachIndexed { i, f ->
            val p = Particle(f, colorFor(f), r)
            p.x = leftX; p.y = if (allR.size == 1) cy else h * (0.28f + i * 0.44f / max(1, allR.size - 1))
            p.targetX = leftX; p.targetY = p.y
            reactants.add(p)
        }
        allP.forEachIndexed { i, f ->
            val p = Particle(f, colorFor(f), r)
            p.x = cx; p.y = cy
            p.targetX = rightX; p.targetY = if (allP.size == 1) cy else h * (0.28f + i * 0.44f / max(1, allP.size - 1))
            p.alpha = 0; p.scale = 0.3f
            products.add(p)
        }

        for (i in 0..23) {
            val angle = (i.toFloat() / 24f) * 2f * PI.toFloat()
            sparkles.add(Triple(cx, cy, angle))
        }

        isAnimating = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500; interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                reactionT = it.animatedValue as Float
                updatePositions()
                invalidate()
                if (reactionT >= 1f) isAnimating = false
            }
            start()
        }
    }

    private fun updatePositions() {
        val t = reactionT; val w = width.toFloat(); val h = height.toFloat(); val cx = w * 0.5f; val cy = h * 0.5f
        when {
            t < 0.25f -> {
                val p = t / 0.25f
                val shake = sin(p * PI * 8).toFloat() * 6f * (1f - p)
                reactants.forEach { it.x = it.targetX + shake; it.scale = 1f + 0.08f * sin(p * PI * 4).toFloat() }
                flashR = 0f; flashA = 0f; waveR = 0f; waveA = 0f
            }
            t < 0.45f -> {
                val p = (t - 0.25f) / 0.2f
                flashR = 140f * p; flashA = 255f * (1f - p * p)
                waveR = 180f * p; waveA = 200f * (1f - p)
                sparkles.forEachIndexed { i, sp ->
                    val angle = sp.third; val dist = 20f + 120f * p
                    sparkles[i] = Triple(cx + cos(angle.toDouble()).toFloat() * dist, cy + sin(angle.toDouble()).toFloat() * dist, angle)
                }
                reactants.forEach { it.scale = 1f + 0.3f * (1f - p) }
            }
            t < 0.85f -> {
                val p = (t - 0.45f) / 0.4f; val ease = 1f - (1f - p).pow(3)
                reactants.forEach { it.alpha = (255 * (1f - ease)).toInt().coerceIn(0, 255); it.scale = 1f - 0.7f * ease }
                products.forEach {
                    it.x = it.targetX * ease; it.y = it.targetY * (0.5f + 0.5f * ease)
                    it.alpha = (255 * ease).toInt(); it.scale = 0.3f + 0.7f * ease
                }
                flashR = 0f; flashA = 0f; waveR = 0f; waveA = 0f
            }
            else -> {
                val p = ((t - 0.85f) / 0.15f).coerceIn(0f, 1f)
                val ease = 1f - (1f - p).pow(2)
                reactants.forEach { it.alpha = (255 * ease).toInt(); it.scale = 0.5f + 0.5f * ease }
                products.forEach { it.x = it.targetX; it.y = it.targetY; it.alpha = 255; it.scale = 1f }
                flashR = 0f; flashA = 0f; waveR = 0f; waveA = 0f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat(); val cx = w / 2f; val cy = h / 2f

        canvas.drawRect(0f, 0f, w, h, bgP)
        val gs = w * 0.05f
        for (gx in 0..(w / gs).toInt()) for (gy in 0..(h / gs).toInt()) canvas.drawCircle(gx * gs, gy * gs, 0.5f, gridP)

        canvas.save(); canvas.scale(zoomScale, zoomScale, cx, cy); canvas.translate(panX / zoomScale, panY / zoomScale)

        if (reactant1.isEmpty() && product1.isEmpty()) {
            hintP.textSize = 13f * sd
            hintP.alpha = (140 + 80 * sin(phase * PI * 2)).toInt().coerceIn(80, 255)
            canvas.drawText("Tepkimeyi girip", cx, cy - 12f, hintP)
            canvas.drawText("DENKLEŞTİR'e basın", cx, cy + 12f, hintP)
            arrowP.alpha = 100; arrowP.pathEffect = DashPathEffect(floatArrayOf(8f, 5f), phase * 16f)
            canvas.drawLine(cx - 30f, cy + 32f, cx + 30f, cy + 32f, arrowP)
            arrowP.pathEffect = null; arrowP.alpha = 255
            canvas.restore(); return
        }

        // Ok çizimi
        if (reactant1.isNotEmpty()) {
            val arrowWave = sin(phase * PI * 4).toFloat() * 3f
            canvas.drawLine(cx - 50f + arrowWave, cy, cx + 50f + arrowWave, cy, arrowP)
            val aPath = Path().apply { moveTo(cx + 50f + arrowWave, cy); lineTo(cx + 36f + arrowWave, cy - 9f); lineTo(cx + 36f + arrowWave, cy + 9f); close() }
            canvas.drawPath(aPath, arrowFillP)
        }

        // Durum etiketi
        if (condition.isNotEmpty()) { condP.textSize = 9f * sd; canvas.drawText(condition, cx, cy - 24f, condP) }
        if (reactionType.isNotEmpty()) {
            typeP.textSize = 8f * sd; val tw = typeP.measureText(reactionType) + 14f
            canvas.drawRoundRect(RectF(cx - tw / 2, 2f, cx + tw / 2, 22f), 8f, 8f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFA500.toInt(); style = Paint.Style.FILL })
            canvas.drawText(reactionType, cx, 15f, typeP)
        }

        // Flaş + dalga
        if (flashA > 1f) { flashP.color = flashA.toInt().shl(24) or 0xFFFFFF; canvas.drawCircle(cx, cy, flashR, flashP) }
        if (waveA > 1f) { waveP.alpha = waveA.toInt(); canvas.drawCircle(cx, cy, waveR, waveP); waveP.alpha = 255 }

        // Kıvılcımlar
        if (reactionT in 0.25f..0.5f) {
            for (sp in sparkles) {
                val a = ((1f - (reactionT - 0.25f) / 0.25f) * 255).toInt().coerceIn(0, 255)
                sparkP.color = a.shl(24) or 0xFFEE58.toInt()
                canvas.drawCircle(sp.first, sp.second, 2f * sd, sparkP)
            }
        }

        // Tüm parçacıklar
        val allP = reactants + products
        for (m in allP) {
            if (m.alpha < 5) continue
            val idleY = sin((phase * PI * 2 + m.targetX * 0.015).toFloat()) * 2.5f
            val drawY = m.y + idleY
            val s = m.scale

            // Glow
            val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(m.x, drawY, m.radius * s * 2.2f,
                    intArrayOf(m.color and 0x00FFFFFF or 0x22000000, 0x00000000), null, Shader.TileMode.CLAMP)
                alpha = m.alpha
            }
            canvas.drawCircle(m.x, drawY, m.radius * s * 2.2f, glowP)

            // Gölge
            canvas.drawCircle(m.x + 2f * sd, drawY + 3f * sd, m.radius * s,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000.toInt(); style = Paint.Style.FILL })

            // Ana küre
            molP.color = m.color; molP.alpha = m.alpha; molP.shader = RadialGradient(
                m.x - m.radius * s * 0.2f, drawY - m.radius * s * 0.2f, m.radius * s * 1.3f,
                intArrayOf(0xFFFFFFFF.toInt(), m.color, m.color and 0x00FFFFFF or 0xCC000000.toInt()), null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(m.x, drawY, m.radius * s, molP)
            molP.shader = null

            // Highlight
            molP.color = 0x44FFFFFF.toInt(); molP.alpha = m.alpha
            canvas.drawCircle(m.x - m.radius * s * 0.25f, drawY - m.radius * s * 0.25f, m.radius * s * 0.28f, molP)

            // Metin
            val textSize = if (m.formula.length > 3) 9f * sd else 11f * sd
            molTxtP.textSize = textSize; molTxtP.alpha = m.alpha
            molOutP.textSize = textSize; molOutP.alpha = m.alpha
            canvas.drawText(m.formula, m.x, drawY + textSize * 0.35f, molOutP)
            canvas.drawText(m.formula, m.x, drawY + textSize * 0.35f, molTxtP)
        }

        // Etiketler
        labelP.textSize = 8f * sd
        if (reactant1.isNotEmpty()) canvas.drawText("REAKTİFLER", w * 0.2f, h - 6f, labelP)
        if (product1.isNotEmpty()) canvas.drawText("ÜRÜNLER", w * 0.8f, h - 6f, labelP)

        // Katsayılar
        if (coefficients.isNotEmpty() && isAnimating.not()) {
            val coeffP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF81C784.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true; textSize = 14f * sd }
            val rCoeffs = coefficients.takeWhile { it != 0 }
            reactants.forEachIndexed { i, m ->
                if (i < rCoeffs.size && rCoeffs[i] > 1) {
                    canvas.drawText("${rCoeffs[i]}", m.x - m.radius * sd - 12f, m.y + 5f, coeffP)
                }
            }
        }

        canvas.restore()
    }
}

// ═══════════════════════════════════════════════════════════════
//  EnergyProfileView — Aktivasyon enerjisi diyagramı
// ═══════════════════════════════════════════════════════════════
class EnergyProfileView(context: Context) : View(context) {
    var isExothermic = true
    var animProgress = 0f
    private val sd = resources.displayMetrics.scaledDensity

    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E14.toInt() }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF12171F.toInt(); strokeWidth = 0.5f }
    private val curveP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; isAntiAlias = true }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val levelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f) }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val axisP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A3441.toInt(); strokeWidth = 1.5f }

    fun animateIn() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200; interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animProgress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)

        val gs = w * 0.06f
        for (gx in 0..(w / gs).toInt()) for (gy in 0..(h / gs).toInt()) canvas.drawCircle(gx * gs, gy * gs, 0.5f, gridP)

        val padL = w * 0.15f; val padR = w * 0.08f; val padT = h * 0.15f; val padB = h * 0.25f
        val plotW = w - padL - padR; val plotH = h - padT - padB

        // Eksenler
        canvas.drawLine(padL, padT, padL, padT + plotH, axisP)
        canvas.drawLine(padL, padT + plotH, padL + plotW, padT + plotH, axisP)

        // Eksen etiketleri
        labelP.textSize = 8f * sd; labelP.color = 0xFF6E7681.toInt()
        canvas.drawText("Enerji", padL - 8f, padT - 4f, labelP.apply { textAlign = Paint.Align.LEFT })
        canvas.drawText("Tepkime İlerlemesi →", padL + plotW / 2f, padT + plotH + 18f, labelP.apply { textAlign = Paint.Align.CENTER })

        // Energypath
        val rE = if (isExothermic) 0.7f else 0.35f
        val pE = if (isExothermic) 0.35f else 0.7f
        val peak = 0.15f

        val path = Path()
        val pts = mutableListOf<Pair<Float, Float>>()
        for (i in 0..100) {
            val t = i / 100f; val tx = padL + t * plotW
            val curveT = t * PI.toFloat()
            val baseE = rE + (pE - rE) * t
            val bump = peak * sin(curveT).toFloat() * (1f - 0.3f * abs(t - 0.5f))
            val ty = padT + plotH * (1f - (baseE + bump)).coerceIn(0f, 1f)
            if (animProgress * 1.2f < t) break
            pts.add(tx to ty)
            if (i == 0) path.moveTo(tx, ty) else path.lineTo(tx, ty)
        }

        // Glow
        glowP.color = if (isExothermic) 0x44FF6B6B.toInt() else 0x44448AFF.toInt()
        canvas.drawPath(path, glowP)

        // Ana çizgi
        curveP.color = if (isExothermic) 0xFFFF6B6B.toInt() else 0xFF448AFF.toInt()
        canvas.drawPath(path, curveP)

        // Gradient doldurma
        if (pts.size > 2) {
            val fillPath = Path(path)
            fillPath.lineTo(pts.last().first, padT + plotH)
            fillPath.lineTo(pts.first().first, padT + plotH)
            fillPath.close()
            val topColor = if (isExothermic) 0x22FF6B6B.toInt() else 0x22448AFF.toInt()
            fillP.shader = LinearGradient(0f, padT, 0f, padT + plotH,
                intArrayOf(topColor, 0x00000000), null, Shader.TileMode.CLAMP)
            canvas.drawPath(fillPath, fillP); fillP.shader = null
        }

        // Seviye çizgileri
        levelP.color = 0xFF81C784.toInt(); levelP.alpha = 120
        val reactantY = padT + plotH * (1f - rE)
        canvas.drawLine(padL, reactantY, padL + plotW * 0.3f, reactantY, levelP)
        labelP.textSize = 8f * sd; labelP.color = 0xFF81C784.toInt(); labelP.alpha = 200
        canvas.drawText("Reaktifler", padL + plotW * 0.15f, reactantY - 6f, labelP)

        levelP.color = 0xFFFFA500.toInt()
        val productY = padT + plotH * (1f - pE)
        canvas.drawLine(padL + plotW * 0.7f, productY, padL + plotW, productY, levelP)
        labelP.color = 0xFFFFA500.toInt()
        canvas.drawText("Ürünler", padL + plotW * 0.85f, productY - 6f, labelP)

        // Aktivasyon enerjisi oku
        if (animProgress > 0.4f) {
            val peakX = padL + plotW * 0.5f
            val peakY = padT + plotH * (1f - (rE + peak * 0.7f))
            val eaP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFEE58.toInt(); textAlign = Paint.Align.CENTER; textSize = 8f * sd; isFakeBoldText = true }
            canvas.drawText("Ea (Aktivasyon)", peakX, peakY - 10f, eaP)
            val arrP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFEE58.toInt(); strokeWidth = 1.5f; style = Paint.Style.STROKE }
            canvas.drawLine(peakX, peakY - 4f, peakX, reactantY + 4f, arrP)
            canvas.drawLine(peakX - 4f, reactantY + 10f, peakX, reactantY + 4f, arrP)
            canvas.drawLine(peakX + 4f, reactantY + 10f, peakX, reactantY + 4f, arrP)
        }

        // ΔH etiketi
        if (animProgress > 0.7f) {
            val dhP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            val dhX = padL + plotW * 0.82f
            val dhText = if (isExothermic) "ΔH < 0 (Ekzo)" else "ΔH > 0 (Endo)"
            dhP.color = if (isExothermic) 0xFFFF6B6B.toInt() else 0xFF448AFF.toInt()
            canvas.drawText(dhText, dhX, (reactantY + productY) / 2f, dhP)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  YieldBarView v2 — Gradient verim çubuğu
// ═══════════════════════════════════════════════════════════════
class YieldBarView(context: Context) : View(context) {
    var yieldPercent: Double? = null
    private var shimmer = 0f; private var animYield = 0f
    private val sd = resources.displayMetrics.scaledDensity

    private var shimmerAnimator: ValueAnimator? = null

    init {
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE
            addUpdateListener { shimmer = it.animatedValue as Float; if (yieldPercent != null) { animYield += ((yieldPercent!!.toFloat() - animYield) * 0.06f); invalidate() } }
            start()
        }
    }

    override fun onDetachedFromWindow() { shimmerAnimator?.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val barH = h * 0.36f; val top = (h - barH) / 2f - 6f; val left = 16f; val right = w - 16f

        canvas.drawRoundRect(RectF(left, top, right, top + barH), 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A1F2E.toInt(); style = Paint.Style.FILL })

        val pct = animYield.coerceIn(0f, 100f); val fillRight = left + (right - left) * (pct / 100f)
        if (fillRight > left) {
            val gradient = LinearGradient(left, 0f, right, 0f, intArrayOf(0xFFFF3333.toInt(), 0xFFFFA500.toInt(), 0xFFFFFF00.toInt(), 0xFF81C784.toInt()), floatArrayOf(0f, 0.33f, 0.66f, 1f), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(left, top, fillRight, top + barH), 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient })
            val sx = left + (right - left) * shimmer
            canvas.drawRoundRect(RectF(sx - 30f, top, sx + 30f, top + barH), 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF.toInt(); style = Paint.Style.FILL })
        }

        val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 14f * sd; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        val pctText = if (yieldPercent != null) "${"%.1f".format(pct)}%" else "Verim Bekleniyor..."
        canvas.drawText(pctText, w / 2f, top + barH / 2f + txtP.textSize / 3f, txtP)

        val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6E7681.toInt(); textSize = 8f * sd; textAlign = Paint.Align.CENTER }
        canvas.drawText("0%", left, top + barH + 18f, labelP)
        canvas.drawText("50%", w / 2f, top + barH + 18f, labelP)
        canvas.drawText("100%", right, top + barH + 18f, labelP)
    }
}

// ═══════════════════════════════════════════════════════════════
//  ReaksiyonFragment
// ═══════════════════════════════════════════════════════════════
class ReaksiyonFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private var sonDengeli = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_reaksiyon, container, false)
        val reaktif = v.findViewById<EditText>(R.id.rea_reaktif)
        val urun = v.findViewById<EditText>(R.id.rea_urun)
        val sonuc = v.findViewById<TextView>(R.id.rea_sonuc)
        val verimT = v.findViewById<EditText>(R.id.verim_teorik)
        val verimG = v.findViewById<EditText>(R.id.verim_gercek)
        val verimY = v.findViewById<EditText>(R.id.verim_yuzde)
        val verimSonuc = v.findViewById<TextView>(R.id.verim_sonuc)
        val resultCard = v.findViewById<View>(R.id.rea_result_card)

        // Scene custom view'ı ekle
        val schemePlaceholder = v.findViewById<View>(R.id.rea_scheme_placeholder)
        val schemeParent = schemePlaceholder.parent as ViewGroup; val schemeIdx = schemeParent.indexOfChild(schemePlaceholder); schemeParent.removeView(schemePlaceholder)
        val sceneView = ReactionSceneView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (200 * resources.displayMetrics.density).toInt()) }
        schemeParent.addView(sceneView, schemeIdx)

        // Enerji profili custom view'ı ekle
        val energyPlaceholder = v.findViewById<View>(R.id.rea_energy_placeholder)
        val energyParent = energyPlaceholder.parent as ViewGroup; val energyIdx = energyParent.indexOfChild(energyPlaceholder); energyParent.removeView(energyPlaceholder)
        val energyView = EnergyProfileView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (140 * resources.displayMetrics.density).toInt()) }
        energyParent.addView(energyView, energyIdx)

        // Yield custom view'ı ekle
        val yieldPlaceholder = v.findViewById<View>(R.id.rea_yield_placeholder)
        val yieldParent = yieldPlaceholder.parent as ViewGroup; val yieldIdx = yieldParent.indexOfChild(yieldPlaceholder); yieldParent.removeView(yieldPlaceholder)
        val yieldView = YieldBarView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (70 * resources.displayMetrics.density).toInt()) }
        yieldParent.addView(yieldView, yieldIdx)

        // Help
        v.findViewById<View>(R.id.rea_help)?.setOnClickListener {
            AnimUtils.press(it)
            HelpDialog.showGuide(requireContext(), "Reaksiyon Dengeleme", "Kimyasal tepkimeleri dengeleyin, enerji profilini görüntüleyin.", listOf(
                "Reaktif ve ürün formüllerini girin (örn: H2+O2 → H2O).",
                "DENKLEŞTİR'e basın — katsayılar otomatik hesaplanır.",
                "Sahne animasyonunu izleyin — çarpışma ve ürün oluşumu.",
                "Enerji profili exotermik/endotermik gösterir.",
                "Örnek tepkime chip'leri ile hızlı deneyin.",
                "Verim için 2 değer girin, 3. otomatik hesaplanır."
            ))
        }

        // Tip kartlari - chip tiklama
        val tipLabels = listOf("Sentez\nA+B-C", "Bozunma\nA-B+C", "Yanma\nCxHy+O2", "Notrlesme\nAsit+Baz", "Yer Degis.\nAB+CD")
        val tipPresets = listOf("A+B" to "C", "A" to "B+C", "CH4+O2" to "CO2+H2O", "HCl+NaOH" to "NaCl+H2O", "Na+Cl2" to "NaCl")

        v.findViewById<HorizontalScrollView>(R.id.rea_tip_scroll)?.post {
            for (idx in tipPresets.indices) {
                val chipId = resources.getIdentifier("rea_tip_$idx", "id", requireContext().packageName)
                v.findViewById<TextView>(chipId)?.setOnClickListener { btn ->
                    AnimUtils.press(btn)
                    // Çiftin tamamı alanlara yazılır: "A+B" → "C", "CH4+O2" → "CO2+H2O"
                    reaktif.setText(tipPresets[idx].first)
                    urun.setText(tipPresets[idx].second)
                    reaksiyonYap(reaktif, urun, sonuc, resultCard, sceneView, energyView)
                }
            }
        }

        // Preset tepkimeler
        data class PresetInfo(val id: String, val r: String, val u: String, val isExo: Boolean)
        val presets = listOf(
            PresetInfo("rea_su", "H2+O2", "H2O", true),
            PresetInfo("rea_tuz", "Na+Cl2", "NaCl", true),
            PresetInfo("rea_amonyak", "N2+H2", "NH3", false),
            PresetInfo("rea_hcl", "H2+Cl2", "HCl", true),
            PresetInfo("rea_kirec", "Ca+O2", "CaO", true),
            PresetInfo("rea_yanma", "CH4+O2", "CO2+H2O", true),
            PresetInfo("rea_demir", "Fe+O2", "Fe2O3", true),
            PresetInfo("rea_aluminyum", "Al+O2", "Al2O3", true)
        )
        presets.forEach { p ->
            v.findViewById<View>(resources.getIdentifier(p.id, "id", requireContext().packageName))?.setOnClickListener { btn ->
                AnimUtils.press(btn)
                reaktif.setText(p.r); urun.setText(p.u)
                energyView.isExothermic = p.isExo; energyView.animProgress = 0f; energyView.animateIn()
                reaksiyonYap(reaktif, urun, sonuc, resultCard, sceneView, energyView)
            }
        }

        // DENKLEŞTİR butonu
        v.findViewById<View>(R.id.rea_dengele)?.setOnClickListener { btn ->
            AnimUtils.press(btn)
            reaksiyonYap(reaktif, urun, sonuc, resultCard, sceneView, energyView)
        }

        // Paylaş
        v.findViewById<View>(R.id.rea_paylas)?.setOnClickListener {
            AnimUtils.press(it)
            if (sonDengeli.isEmpty()) { Toast.makeText(context, "Önce reaksiyon dengeleyin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PdfExporter.shareText(requireContext(), "Reaksiyon", sonDengeli)
        }

        // Verim hesapla
        v.findViewById<View>(R.id.verim_hesapla)?.setOnClickListener { btn ->
            AnimUtils.press(btn)
            try {
                val t = verimT.text.toString().toDoubleOrNull(); val g = verimG.text.toString().toDoubleOrNull(); val y = verimY.text.toString().toDoubleOrNull()
                val sb = StringBuilder()
                when {
                    t != null && g != null && t > 0 -> { val yuzde = g / t * 100; sb.append("Verim = ${"%.2f".format(yuzde)}%\n"); sb.append(when { yuzde >= 95 -> "Mükemmel verim!"; yuzde >= 80 -> "İyi verim"; yuzde >= 60 -> "Orta verim"; else -> "Düşük verim — optimizasyon gerekli" }); yieldView.yieldPercent = yuzde; yieldView.invalidate() }
                    t != null && y != null && t > 0 -> { val gercek = t * y / 100; sb.append("Gerçek Verim = ${"%.4f".format(gercek)} g"); yieldView.yieldPercent = y; yieldView.invalidate() }
                    g != null && y != null && y > 0 -> { val teorik = g / (y / 100); sb.append("Teorik Verim = ${"%.4f".format(teorik)} g"); yieldView.yieldPercent = y; yieldView.invalidate() }
                    else -> sb.append("En az 2 değer girin")
                }
                verimSonuc.text = sb.toString(); verimSonuc.visibility = View.VISIBLE; AnimUtils.popIn(verimSonuc)
            } catch (e: Exception) { verimSonuc.text = "Hata: ${e.message}"; verimSonuc.visibility = View.VISIBLE }
        }

        // Verim preset'leri
        v.findViewById<View>(R.id.verim_preset1)?.setOnClickListener { AnimUtils.press(it); verimT.setText("10"); verimG.setText("8"); verimY.setText(""); v.findViewById<View>(R.id.verim_hesapla).performClick() }
        v.findViewById<View>(R.id.verim_preset2)?.setOnClickListener { AnimUtils.press(it); verimT.setText("5"); verimG.setText("4.5"); verimY.setText(""); v.findViewById<View>(R.id.verim_hesapla).performClick() }
        v.findViewById<View>(R.id.verim_preset3)?.setOnClickListener { AnimUtils.press(it); verimT.setText("20"); verimY.setText("75"); verimG.setText(""); v.findViewById<View>(R.id.verim_hesapla).performClick() }

        // Gradient title
        v.findViewById<TextView>(R.id.rea_baslik)?.let { AnimUtils.gradientTitle(it) }

        // Giriş animasyonları (staggered)
        val staggerIds = listOf(
            R.id.rea_header, R.id.rea_scene_card, R.id.rea_tip_card,
            R.id.rea_input_card, R.id.rea_energy_card, R.id.rea_verim_card
        )
        staggerIds.forEachIndexed { idx, id ->
            v.findViewById<View>(id)?.let {
                it.alpha = 0f; it.translationY = 40f * resources.displayMetrics.density
                it.post { AnimUtils.slideUpFade(it, idx * 80L) }
            }
        }

        return v
    }

    private fun reaksiyonYap(reaktif: EditText, urun: EditText, sonuc: TextView, resultCard: View?, sceneView: ReactionSceneView, energyView: EnergyProfileView) {
        var r = reaktif.text.toString().trim().replace(" ", "")
        var u = urun.text.toString().trim().replace(" ", "")

        if (r.isEmpty() && u.isEmpty()) { sonuc.text = "Reaktif ve ürün girin"; resultCard?.visibility = View.VISIBLE; return }

        // Otomatik bulma
        if (r.isEmpty() || u.isEmpty()) {
            val found = findReactionFor(r, u)
            if (found != null) {
                if (r.isEmpty()) { r = found.first; reaktif.setText(r) }
                if (u.isEmpty()) { u = found.second; urun.setText(u) }
                Toast.makeText(context, "Otomatik: ${found.first} → ${found.second}", Toast.LENGTH_SHORT).show()
            } else {
                sonuc.text = "Bilinmeyen tepkime\nÖrnek: H2+O2 → H2O veya Na+Cl2 → NaCl"
                resultCard?.visibility = View.VISIBLE; if (resultCard != null) AnimUtils.popIn(resultCard)
                return
            }
        }

        // Dengele
        val dengeli = ReactionBalancer.dene("$r->$u")
        val dengeliStr = if (dengeli != null) ReactionBalancer.formatReaction(dengeli) else KimyaData.reaksiyonDengele(r, u)

        // Tip tespiti
        val tip = when {
            "O2" in r && ("CO2" in u || "H2O" in u) -> "Yanma"
            r.split("+").size == 1 && u.split("+").size > 1 -> "Bozunma"
            r.split("+").size > 1 && u.split("+").size == 1 -> "Sentez"
            ("HCl" in r || "H2SO4" in r) && "NaOH" in r -> "Nötrleşme"
            else -> "Genel"
        }

        // Enerji tespiti
        val isExo = tip == "Yanma" || tip == "Sentez" || "O2" in r || "Cl2" in r
        energyView.isExothermic = isExo; energyView.animProgress = 0f; energyView.animateIn()

        sonDengeli = "$dengeliStr\n\nTip: $tip\nEnerji: ${if (isExo) "Ekzotermik (ΔH < 0)" else "Endotermik (ΔH > 0)"}"
        sonuc.text = sonDengeli; resultCard?.visibility = View.VISIBLE
        if (resultCard != null) AnimUtils.popIn(resultCard)
        vm.addHistory("Reaksiyon Dengele", "$r -> $u")

        // Sahne
        val reaktifList = r.split("+").filter { it.isNotBlank() }.map { it.trim() }
        val urunList = u.split("+").filter { it.isNotBlank() }.map { it.trim() }
        sceneView.reactant1 = reaktifList.joinToString("+")
        sceneView.reactant2 = ""
        sceneView.product1 = urunList.joinToString("+")
        sceneView.product2 = ""
        sceneView.condition = if (isExo) "⚡ Isı" else ""
        sceneView.reactionType = tip
        if (dengeli != null) {
            sceneView.coefficients = (dengeli.reaktifler.map { it.second } + dengeli.urunler.map { it.second }).toIntArray()
        }
        sceneView.startReaction()
    }

    private fun findReactionFor(reaktifStr: String, urunStr: String): Pair<String, String>? {
        val knownReactions = listOf(
            "H2+O2" to "H2O", "Na+Cl2" to "NaCl", "Ca+O2" to "CaO", "H2+Cl2" to "HCl",
            "N2+H2" to "NH3", "CH4+O2" to "CO2+H2O", "C+O2" to "CO2", "S+O2" to "SO2",
            "Fe+O2" to "Fe2O3", "Al+O2" to "Al2O3", "H2+Br2" to "HBr", "Na+O2" to "Na2O",
            "K+Cl2" to "KCl", "Mg+O2" to "MgO", "Fe+S" to "FeS", "Cu+O2" to "CuO",
            "Zn+O2" to "ZnO", "P+O2" to "P2O5", "Li+O2" to "Li2O", "Ba+O2" to "BaO2"
        )
        fun normalize(s: String) = s.split("+").map { it.trim() }.filter { it.isNotEmpty() }.sorted().joinToString("+")

        val rNorm = normalize(reaktifStr); val uNorm = normalize(urunStr)
        for ((kr, ku) in knownReactions) {
            if (rNorm.isNotEmpty() && uNorm.isEmpty() && normalize(kr) == rNorm) return kr to ku
            if (uNorm.isNotEmpty() && rNorm.isEmpty() && normalize(ku) == uNorm) return kr to ku
            if (rNorm.isNotEmpty() && uNorm.isEmpty()) {
                val rSet = rNorm.split("+").toSet(); val krSet = normalize(kr).split("+").toSet()
                if (rSet == krSet) return kr to ku
            }
            if (uNorm.isNotEmpty() && rNorm.isEmpty()) {
                val uSet = uNorm.split("+").toSet(); val kuSet = normalize(ku).split("+").toSet()
                if (uSet == kuSet) return kr to ku
            }
        }
        if (rNorm.isNotEmpty() && uNorm.isEmpty() && rNorm.split("+").size == 2) {
            val parts = rNorm.split("+"); val e1 = parts[0].replace(Regex("[0-9]"), ""); val e2 = parts[1].replace(Regex("[0-9]"), "")
            val predicted = try { KimyaData.etkilesim(e1, e2, null, null) } catch (_: Exception) { "" }
            // "İyonik bileşik: Mg3N2" → "Mg3N2" (Türkçe önek regex'e takılmasın);
            // kütlesi hesaplanabilen gerçek formülse kabul et
            val m = predicted.substringAfter(":", "").trim().replace(" ", "")
            if (m.isNotEmpty() && m != rNorm.replace("+", "") && KimyaData.molekulKutlesiHesapla(m) != null) return rNorm to m
        }
        return null
    }
}

