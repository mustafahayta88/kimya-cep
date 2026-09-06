package com.kimya.uygulama.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class SolubilityView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class SoluteData(
        val name: String, val formula: String, val category: String,
        val color: Int, val colorLight: Int, val colorDark: Int,
        val maxSol20: Float, val tempCoeff: Float,
        val desc: String, val dangerLevel: Int
    )

    private val solutes = listOf(
        SoluteData("Sodyum Klorur", "NaCl", "İyonik", Color.rgb(100, 180, 255), Color.rgb(150, 210, 255), Color.rgb(60, 130, 200), 36f, 0.02f, "Sofra tuzu", 0),
        SoluteData("Bakır Sülfat", "CuSO₄·5H₂O", "Kristal", Color.rgb(40, 120, 220), Color.rgb(100, 170, 255), Color.rgb(20, 80, 160), 20f, 0.08f, "Mavi kristal", 1),
        SoluteData("Potasyum Nitrat", "KNO₃", "Tuz", Color.rgb(80, 200, 160), Color.rgb(140, 230, 190), Color.rgb(40, 140, 100), 32f, 0.15f, "Gübre, barut", 1),
        SoluteData("Şeker (Sukroz)", "C₁₂H₂₂O₁₁", "Organik", Color.rgb(255, 200, 60), Color.rgb(255, 220, 120), Color.rgb(200, 150, 20), 200f, 0.05f, "Gıda", 0),
        SoluteData("Kalsiyum Hidroksit", "Ca(OH)₂", "Baz", Color.rgb(255, 140, 40), Color.rgb(255, 180, 100), Color.rgb(200, 100, 20), 0.165f, -0.0008f, "Kireç suyu", 2),
        SoluteData("Kalsiyum Karbonat", "CaCO₃", "Tuz", Color.rgb(180, 160, 200), Color.rgb(210, 190, 220), Color.rgb(130, 110, 150), 0.0013f, -0.0001f, "Tebeşir, mermer", 1),
        SoluteData("Sodyum Sülfat", "Na₂SO₄", "Tuz", Color.rgb(200, 180, 140), Color.rgb(230, 210, 180), Color.rgb(150, 130, 90), 19.5f, 0.04f, "Glauber tuzu", 1),
        SoluteData("Baryum Klorür", "BaCl₂", "Tuz", Color.rgb(220, 200, 100), Color.rgb(240, 220, 150), Color.rgb(170, 150, 60), 35.8f, 0.03f, "Zehirli!", 3)
    )

    var viewMode = 0 // 0=beaker, 1=graph, 2=data
    var soluteIdx = 0; var temperature = 25f; var addedGrams = 0f
    var showInfo = false; var stirring = false; var heating = false; var cooling = false
    var evaporating = false; var supersaturating = false; var seedCrystal = false
    var precipitateHeight = 0f; var graphAnimProgress = 0f

    private var t = 0f; private var wavePhase = 0f; private var stirAngle = 0f
    private var zoom = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private var saturationPulse = 0f; private var shakeAmount = 0f
    private var precipitateShake = 0f
    private var steamTimer = 0f; private var crystalGrowPhase = 0f
    private var nucleationTimer = 0f; private var supersaturateGlow = 0f
    private var seedDropAnim = -1f; private var seedX = 0f; private var seedY = 0f
    private var tempAnimTarget = 25f; private var tempAnimCurrent = 25f
    private var evaporateTimer = 0f; private var liquidVolume = 1f
    private var beakerFillAnim = 0f

    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, var maxLife: Float, var size: Float, var type: Int)
    private data class Crystal(var x: Float, var y: Float, var size: Float, var growth: Float,
        var rotation: Float, var hue: Float, var dissolved: Boolean = false)
    private data class Bubble(var x: Float, var y: Float, var r: Float, var speed: Float, var wobble: Float)
    private data class Steam(var x: Float, var y: Float, var life: Float, var size: Float)
    private data class Star(var x: Float, var y: Float, var size: Float, var phase: Float)

    private val particles = mutableListOf<Particle>()
    private val crystals = mutableListOf<Crystal>()
    private val bubbles = mutableListOf<Triple<Float, Float, Float>>()
    private val steamParticles = mutableListOf<Steam>()
    private val fallingCrystals = mutableListOf<Triple<Float, Float, Float>>()
    private val dissolvingParticles = mutableListOf<Triple<Float, Float, Float>>()
    private val ripples = mutableListOf<Triple<Float, Float, Float>>()
    private val splashParts = mutableListOf<Triple<Float, Float, Float>>()
    private val stars = mutableListOf<Star>()

    private val hd = Handler(Looper.getMainLooper())
    private val sDetector: ScaleGestureDetector

    private val animRunnable = object : Runnable {
        override fun run() {
            t += 0.016f; wavePhase += if (stirring) 0.14f else 0.05f
            if (stirring) stirAngle = (stirAngle + 22f) % 360f
            if (shakeAmount > 0.1f) shakeAmount *= 0.88f
            if (precipitateShake > 0.1f) precipitateShake *= 0.9f
            if (seedDropAnim >= 0f) seedDropAnim += 0.035f
            if (seedDropAnim > 1f) seedDropAnim = -1f
            if (heating) { temperature += 0.15f; if (temperature > 100f) temperature = 100f }
            if (cooling) { temperature -= 0.15f; if (temperature < 0f) temperature = 0f }
            if (evaporating) {
                evaporateTimer += 0.02f
                liquidVolume -= 0.0008f
                if (liquidVolume < 0.2f) liquidVolume = 0.2f
                if (Random.nextFloat() < 0.15f) steamParticles.add(Steam(
                    0.3f + Random.nextFloat() * 0.4f, 0.15f, 1f, 4f + Random.nextFloat() * 6f))
            }
            if (supersaturating && dissolveRatio() < 1.5f) {
                addedGrams += 0.3f; supersaturateGlow = min(supersaturateGlow + 0.02f, 1f)
            } else supersaturateGlow *= 0.97f
            crystalGrowPhase += 0.03f
            beakerFillAnim += (liquidVolume - beakerFillAnim) * 0.05f
            graphAnimProgress = min(graphAnimProgress + 0.02f, 1f)
            updateSimulation(); invalidate()
            hd.postDelayed(this, 20)
        }
    }

    fun sol() = solutes[soluteIdx.coerceIn(solutes.indices)]
    fun maxSol(): Float = (sol().maxSol20 + sol().tempCoeff * (temperature - 20f)).coerceAtLeast(0.0001f)
    fun dissolveRatio(): Float = (addedGrams / maxSol()).coerceIn(0f, 3f)
    fun isSaturated(): Boolean = dissolveRatio() >= 1f
    fun isSupersaturated(): Boolean = dissolveRatio() > 1.2f
    private fun excessGrams(): Float = (addedGrams - maxSol()).coerceAtLeast(0f)

    init {
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoom *= d.scaleFactor; zoom = zoom.coerceIn(0.4f, 3f); return true
            }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; touchMode = 0 }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.x - lastTx; val dy = e.y - lastTy
                    if (abs(dx) > 5 || abs(dy) > 5) touchMode = 1
                    if (touchMode == 1) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touchMode = 0
            }
            true
        }
        for (i in 0..20) stars.add(Star(Random.nextFloat(), Random.nextFloat(),
            1f + Random.nextFloat() * 2f, Random.nextFloat() * 6.28f))
    }

    fun startAnim() { hd.post(animRunnable) }
    fun stopAnim() { hd.removeCallbacksAndMessages(null) }

    fun addSolute(grams: Float) {
        addedGrams += grams; shakeAmount = 4f; precipitateShake = 2f
        val count = (grams * 2f).toInt().coerceIn(6, 25)
        for (k in 0..count) fallingCrystals.add(Triple(
            0.35f + Random.nextFloat() * 0.3f, 0.02f + Random.nextFloat() * 0.03f,
            Random.nextFloat() * 6f + 3f))
        ripples.add(Triple(0.5f, 0f, 1f))
        for (k in 0..8) splashParts.add(Triple(
            0.5f + (Random.nextFloat() - 0.5f) * 0.4f, 0.13f, Random.nextFloat() * 0.3f))
        if (isSupersaturated()) supersaturateGlow = min(supersaturateGlow + 0.3f, 1f)
    }

    fun resetSimulation() {
        addedGrams = 0f; stirring = false; heating = false; cooling = false
        evaporating = false; supersaturating = false; seedCrystal = false
        precipitateHeight = 0f; liquidVolume = 1f; supersaturateGlow = 0f
        fallingCrystals.clear(); dissolvingParticles.clear(); particles.clear()
        crystals.clear(); bubbles.clear(); steamParticles.clear(); ripples.clear()
        splashParts.clear(); graphAnimProgress = 0f; invalidate()
    }

    fun dropSeedCrystal() {
        if (!isSupersaturated()) return
        seedCrystal = true; seedDropAnim = 0f; seedX = 0.5f; seedY = 0.05f
        shakeAmount = 6f
        for (k in 0..30) {
            val angle = Random.nextFloat() * 6.28f
            val dist = Random.nextFloat() * 0.15f
            crystals.add(Crystal(0.5f + cos(angle) * dist, 0.3f + sin(angle) * dist * 0.5f,
                2f + Random.nextFloat() * 3f, 0f, Random.nextFloat() * 360f, Random.nextFloat()))
        }
    }

    private fun updateSimulation() {
        val dr = dissolveRatio(); val sat = isSaturated()
        if (sat) saturationPulse = (saturationPulse + 0.04f) % (2f * PI).toFloat()

        val targetPrecip = if (sat) (excessGrams() / maxSol()).coerceIn(0f, 0.4f) else 0f
        precipitateHeight += (targetPrecip - precipitateHeight) * 0.03f

        val fcIt = fallingCrystals.iterator()
        while (fcIt.hasNext()) {
            val fc = fcIt.next()
            val speed = if (stirring) 0.018f else 0.012f
            val newY = fc.second + speed
            val swirl = if (stirring) sin(stirAngle * PI.toFloat() / 180f + fc.first * 5f) * 0.015f else 0f
            val newX = fc.first + sin(t * 2f + fc.first * 10f) * 0.003f + swirl
            val shrink = if (!sat) 0.993f else 1f; val newSize = fc.third * shrink
            if (newY > 0.88f || newSize < 0.3f) {
                fcIt.remove()
                if (!sat) for (k in 0..6) dissolvingParticles.add(Triple(
                    newX + (Random.nextFloat() - 0.5f) * 0.14f, newY - 0.02f, 0f))
            } else {
                val i = fallingCrystals.indexOf(fc)
                if (i >= 0) fallingCrystals[i] = Triple(newX.coerceIn(0.12f, 0.88f), newY, newSize)
            }
        }

        val dpIt = dissolvingParticles.iterator()
        while (dpIt.hasNext()) {
            val p = dpIt.next(); val n = p.third + (if (stirring) 0.022f else 0.012f)
            if (n > 1f) dpIt.remove() else {
                val i = dissolvingParticles.indexOf(p)
                val sx = if (stirring) cos(t * 3f + p.second * 5f) * 0.018f else sin(t * 1.5f + p.first * 8f) * 0.008f
                val sy = if (stirring) sin(t * 2f + p.first * 4f) * 0.012f else cos(t * 1.2f + p.second * 6f) * 0.004f
                if (i >= 0) dissolvingParticles[i] = Triple(p.first + sx, p.second + sy, n)
            }
        }

        if (temperature > 25f && Random.nextFloat() < 0.08f * (temperature / 80f))
            bubbles.add(Triple(0.3f + Random.nextFloat() * 0.4f, 0.85f, 0f))
        if (stirring && Random.nextFloat() < 0.08f) bubbles.add(Triple(
            0.3f + Random.nextFloat() * 0.4f, 0.7f + Random.nextFloat() * 0.15f, 0f))
        val bIt = bubbles.iterator()
        while (bIt.hasNext()) {
            val b = bIt.next(); val ny = b.second - if (stirring) 0.016f else 0.009f
            val nl = b.third + 0.024f
            if (nl > 1f || ny < 0.05f) bIt.remove() else {
                val i = bubbles.indexOf(b)
                if (i >= 0) bubbles[i] = Triple(b.first + sin(nl * 4f) * 0.008f, ny, nl)
            }
        }

        val rIt = ripples.iterator()
        while (rIt.hasNext()) {
            val r = rIt.next(); val nr = r.second + 0.035f; val na = r.third - 0.022f
            if (na <= 0f) rIt.remove() else {
                val i = ripples.indexOf(r)
                if (i >= 0) ripples[i] = Triple(r.first, nr, na)
            }
        }

        val sIt = splashParts.iterator()
        while (sIt.hasNext()) {
            val s = sIt.next(); val n = s.third + 0.055f
            if (n > 1f) sIt.remove() else {
                val i = splashParts.indexOf(s)
                if (i >= 0) splashParts[i] = Triple(s.first, s.second, n)
            }
        }

        val stIt = steamParticles.iterator()
        while (stIt.hasNext()) {
            val s = stIt.next(); val nl = s.life - 0.02f
            if (nl <= 0f) stIt.remove() else {
                val i = steamParticles.indexOf(steamParticles.find { it === s })
                if (i >= 0) steamParticles[i] = Steam(s.x + sin(t * 2f + s.x * 3f) * 0.003f,
                    s.y - 0.005f, nl, s.size * 1.01f)
            }
        }

        val cIt = crystals.iterator()
        while (cIt.hasNext()) {
            val c = cIt.next()
            if (!c.dissolved && sat) c.growth = min(c.growth + 0.003f, 1f)
            if (!sat && !c.dissolved) { c.size -= 0.02f; if (c.size < 0.5f) c.dissolved = true }
        }
        if (seedCrystal && seedDropAnim >= 0f && seedDropAnim < 1f) {
            seedY = 0.05f + seedDropAnim * 0.5f
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val W = width.toFloat(); val H = height.toFloat()
        c.drawColor(Color.rgb(10, 14, 22))

        if (viewMode == 0) drawBeakerMode(c, W, H)
        else if (viewMode == 1) drawGraphMode(c, W, H)
        else drawDataMode(c, W, H)

        if (showInfo) drawInfoOverlay(c, W, H)
    }

    private fun drawBeakerMode(c: Canvas, W: Float, H: Float) {
        val mx = Matrix()
        val shX = if (shakeAmount > 0.2f) sin(t * 40f) * shakeAmount else 0f
        mx.postScale(zoom, zoom, W / 2, H / 2); mx.postTranslate(panX + shX, panY)
        c.save(); c.concat(mx)

        val bx = W * 0.18f; val by = H * 0.08f; val bw = W * 0.48f; val bh = H * 0.72f
        val liqTop = by + bh * 0.08f; val liqBot = by + bh - 3f
        val dr = dissolveRatio().coerceAtMost(1.5f)
        val col = sol(); val precipBot = liqBot
        val precipTop = liqBot - (liqBot - liqTop) * precipitateHeight

        drawBackground(c, bx, by, bw, bh, col)
        drawBeaker3D(c, bx, by, bw, bh, col, liqTop, liqBot, dr)
        drawLiquid(c, bx, by, bw, bh, col, liqTop, liqBot, dr)
        drawWave(c, bx, by, bw, liqTop, col)
        drawRipples(c, bx, by, bw, liqTop, col)
        drawPrecipitate(c, bx, by, bw, precipTop, precipBot, col)
        drawMeasurementLines(c, bx, by, bw, liqTop, liqBot)
        drawStirRod(c, bx, by, bw, liqTop, liqBot)
        drawFallingCrystals(c, bx, by, bw, liqTop, liqBot, col)
        drawDissolvingParticles(c, bx, by, bw, liqTop, liqBot, col)
        drawBubbles(c, bx, by, bw, liqTop, liqBot)
        drawSteam(c, bx, by, bw, liqTop)
        drawSplash(c, bx, by, bw, liqTop, col)
        drawDrop(c, bx, by, bw, liqTop, col)
        drawSeedCrystal(c, bx, by, bw, liqTop, liqBot)
        drawCrystalGrowth(c, bx, by, bw, liqTop, liqBot, col)
        drawBeakerFront(c, bx, by, bw, bh, col)

        drawRightPanel(c, W, H, bx + bw + 20f, by, W - bx - bw - 35f, bh)
        drawBottomCards(c, W, H, bx, by, bw, bh, col, dr)

        c.restore()
    }

    private fun drawBackground(c: Canvas, bx: Float, by: Float, bw: Float, bh: Float, col: SoluteData) {
        val gp = Paint(Paint.ANTI_ALIAS_FLAG); gp.maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.NORMAL)
        gp.color = Color.argb(25, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
        c.drawCircle(bx + bw / 2, by + bh * 0.5f, bw, gp)
        for (s in stars) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG); sp.style = Paint.Style.FILL
            val a = ((sin(t * 0.5f + s.phase) + 1f) * 0.5f * 150f).toInt()
            sp.color = Color.argb(a, 120, 200, 255)
            c.drawCircle(bx + bw * s.x, by + bh * s.y, s.size, sp)
        }
    }

    private fun drawBeaker3D(c: Canvas, bx: Float, by: Float, bw: Float, bh: Float,
        col: SoluteData, liqTop: Float, liqBot: Float, dr: Float) {
        val glassBg = Paint(Paint.ANTI_ALIAS_FLAG); glassBg.style = Paint.Style.FILL
        glassBg.color = Color.argb(18, 80, 160, 200)
        c.drawRoundRect(bx, by, bx + bw, by + bh, 12f, 12f, glassBg)

        val glassHL = Paint(Paint.ANTI_ALIAS_FLAG); glassHL.style = Paint.Style.STROKE
        glassHL.strokeWidth = 2.5f; glassHL.color = Color.argb(90, 180, 230, 255); glassHL.isAntiAlias = true
        c.drawLine(bx + 1.5f, by + 8f, bx + 1.5f, by + bh - 8f, glassHL)

        val glassR = Paint(Paint.ANTI_ALIAS_FLAG); glassR.style = Paint.Style.STROKE
        glassR.strokeWidth = 1.5f; glassR.color = Color.argb(40, 100, 180, 220); glassR.isAntiAlias = true
        c.drawLine(bx + bw - 1.5f, by + 8f, bx + bw - 1.5f, by + bh - 8f, glassR)

        val rimP = Paint(Paint.ANTI_ALIAS_FLAG); rimP.style = Paint.Style.STROKE; rimP.strokeWidth = 5f
        rimP.color = Color.rgb(77, 208, 225); rimP.isAntiAlias = true
        c.drawLine(bx - 4f, by, bx + bw + 4f, by, rimP)

        val outlineP = Paint(Paint.ANTI_ALIAS_FLAG); outlineP.style = Paint.Style.STROKE
        outlineP.strokeWidth = 4f; outlineP.color = Color.rgb(77, 208, 225); outlineP.isAntiAlias = true
        c.drawRoundRect(bx, by, bx + bw, by + bh, 12f, 12f, outlineP)
    }

    private fun drawLiquid(c: Canvas, bx: Float, by: Float, bw: Float, bh: Float,
        col: SoluteData, liqTop: Float, liqBot: Float, dr: Float) {
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.style = Paint.Style.FILL
        val wr = 90; val wg = 170; val wb = 230; val bl = dr.coerceAtMost(1f)
        val r = (wr + (Color.red(col.color) - wr) * bl).toInt().coerceIn(0, 255)
        val g = (wg + (Color.green(col.color) - wg) * bl).toInt().coerceIn(0, 255)
        val b = (wb + (Color.blue(col.color) - wb) * bl).toInt().coerceIn(0, 255)
        lp.color = Color.rgb(r, g, b)
        c.drawRect(bx + 5f, liqTop + 15f, bx + bw - 5f, liqBot, lp)

        val lgp = Paint(Paint.ANTI_ALIAS_FLAG); lgp.style = Paint.Style.FILL
        lgp.shader = LinearGradient(bx, liqTop + 15f, bx, liqBot,
            Color.argb(0, 0, 0, 0), Color.argb(50, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(bx + 5f, liqTop + 15f, bx + bw - 5f, liqBot, lgp)

        if (isSaturated()) {
            val pa = (abs(sin(saturationPulse)) * 60).toInt()
            c.drawRect(bx + 5f, liqTop + 15f, bx + bw - 5f, liqBot,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL
                    color = Color.argb(pa, Color.red(col.color), Color.green(col.color), Color.blue(col.color)) })
        }

        if (supersaturateGlow > 0.1f) {
            val ga = (supersaturateGlow * 80).toInt()
            c.drawRect(bx + 5f, liqTop + 15f, bx + bw - 5f, liqBot,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL
                    color = Color.argb(ga, 255, 50, 150) })
        }

        val shp = Paint(Paint.ANTI_ALIAS_FLAG); shp.style = Paint.Style.FILL
        shp.color = Color.argb(80, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
        c.drawRect(bx + 5f, liqTop + 15f, bx + bw - 5f, liqTop + 28f, shp)
        c.drawLine(bx + 8f, liqTop + 17f, bx + bw - 8f, liqTop + 17f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f
                color = Color.argb(110, 255, 255, 255); isAntiAlias = true })
    }

    private fun drawWave(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, col: SoluteData) {
        val wp = Paint(Paint.ANTI_ALIAS_FLAG); wp.style = Paint.Style.FILL
        wp.color = Color.argb(90, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
        val wave = Path(); wave.moveTo(bx + 5f, liqTop + 15f)
        val wAmp = if (stirring) 10f else 5f
        for (x in 0..(bw - 10).toInt() step 3)
            wave.lineTo(bx + 5f + x.toFloat(), liqTop + 15f + sin(wavePhase + x * 0.07f) * wAmp
                + cos(wavePhase * 0.6f + x * 0.05f) * 3f)
        wave.lineTo(bx + bw - 5f, liqTop + 32f); wave.lineTo(bx + 5f, liqTop + 32f); wave.close()
        c.drawPath(wave, wp)
    }

    private fun drawRipples(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, col: SoluteData) {
        val rp = Paint(Paint.ANTI_ALIAS_FLAG); rp.style = Paint.Style.STROKE; rp.strokeWidth = 2.5f
        for (r in ripples) {
            val rx = bx + 5f + (bw - 10f) * r.first
            rp.color = Color.argb((r.third * 200).toInt(), Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            c.drawOval(rx - r.second * 80f, liqTop + 32f - r.second * 14f,
                rx + r.second * 80f, liqTop + 32f + r.second * 14f, rp)
        }
    }

    private fun drawPrecipitate(c: Canvas, bx: Float, by: Float, bw: Float,
        precipTop: Float, precipBot: Float, col: SoluteData) {
        if (precipitateHeight < 0.005f) return
        val pp = Paint(Paint.ANTI_ALIAS_FLAG); pp.style = Paint.Style.FILL
        val pShake = sin(t * 8f) * precipitateShake * 2f
        pp.shader = LinearGradient(bx, precipTop, bx, precipBot,
            Color.argb(35, 200, 200, 200), Color.argb(200, 180, 180, 180), Shader.TileMode.CLAMP)
        c.drawRect(bx + 5f + pShake, precipTop, bx + bw - 5f + pShake, precipBot, pp)

        val grainP = Paint(Paint.ANTI_ALIAS_FLAG); grainP.style = Paint.Style.FILL
        for (i in 0..25) {
            val gx = bx + 8f + (bw - 16f) * (i / 25f) + sin(i * 1.7f) * 5f
            val gy = precipTop + 3f + (precipBot - precipTop - 6f) * (0.5f + cos(i * 2.3f) * 0.4f)
            grainP.color = Color.argb(70 + (i % 3) * 25, 170, 170, 170)
            c.drawCircle(gx, gy, 2.5f + sin(i.toFloat()) * 1.5f, grainP)
        }

        val ptPath = Path(); ptPath.moveTo(bx + 5f, precipTop)
        for (x in 0..(bw - 10).toInt() step 4) {
            val py = precipTop + sin(t * 1.5f + x * 0.1f) * 3f + cos(x * 0.15f) * 2f
            ptPath.lineTo(bx + 5f + x.toFloat(), py)
        }
        ptPath.lineTo(bx + bw - 5f, precipTop + 10f); ptPath.lineTo(bx + 5f, precipTop + 10f); ptPath.close()
        pp.shader = null; pp.color = Color.argb(180, 200, 200, 200)
        c.drawPath(ptPath, pp)

        val sparkP = Paint(Paint.ANTI_ALIAS_FLAG); sparkP.style = Paint.Style.FILL
        for (i in 0..12) {
            val sx = bx + 10f + (bw - 20f) * (i / 12f) + sin(t * 0.8f + i * 1.5f) * 5f
            val sy = precipTop + 5f + cos(t * 0.6f + i * 2f) * 3f
            sparkP.color = Color.argb(190, 255, 255, 255); c.drawCircle(sx, sy, 2f, sparkP)
            sparkP.color = Color.argb(100, 200, 200, 200); c.drawCircle(sx + 1f, sy + 1f, 1.5f, sparkP)
        }

        val plp = Paint(Paint.ANTI_ALIAS_FLAG); plp.textSize = 12f; plp.textAlign = Paint.Align.CENTER
        plp.color = Color.argb(220, 70, 70, 70); plp.isFakeBoldText = true; plp.isAntiAlias = true
        c.drawText("ÇÖKELTİ", bx + bw / 2f, precipBot - 8f, plp)
    }

    private fun drawMeasurementLines(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, liqBot: Float) {
        val mlP = Paint(Paint.ANTI_ALIAS_FLAG); mlP.style = Paint.Style.STROKE
        for (i in 1..9) {
            val ly = liqTop + (liqBot - liqTop) * i / 10f
            mlP.strokeWidth = if (i % 5 == 0) 2.5f else 1f
            mlP.color = if (i % 5 == 0) Color.rgb(77, 208, 225) else Color.argb(70, 130, 220, 255)
            c.drawLine(bx + 5f, ly, bx + 5f + if (i % 5 == 0) 18f else 8f, ly, mlP)
        }
    }

    private fun drawStirRod(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, liqBot: Float) {
        if (!stirring) return
        val rodP = Paint(Paint.ANTI_ALIAS_FLAG); rodP.strokeWidth = 7f; rodP.strokeCap = Paint.Cap.ROUND
        rodP.color = Color.rgb(160, 160, 160); rodP.isAntiAlias = true
        val rodX = bx + bw * 0.5f + cos(stirAngle * PI.toFloat() / 180f) * bw * 0.22f
        c.drawLine(rodX, liqTop - 12f, rodX + sin(stirAngle * PI.toFloat() / 180f) * 22f,
            liqBot - 12f, rodP)

        val vP = Paint(Paint.ANTI_ALIAS_FLAG); vP.style = Paint.Style.STROKE; vP.strokeWidth = 2f; vP.isAntiAlias = true
        for (i in 1..5) {
            val vr = 12f * i + sin(t * 5f) * 5f
            vP.color = Color.argb(28 - i * 4, 255, 255, 255)
            c.drawOval(bx + bw * 0.5f - vr, liqBot - 35f - vr * 0.28f,
                bx + bw * 0.5f + vr, liqBot - 35f + vr * 0.28f, vP)
        }
    }

    private fun drawFallingCrystals(c: Canvas, bx: Float, by: Float, bw: Float,
        liqTop: Float, liqBot: Float, col: SoluteData) {
        val fcp = Paint(Paint.ANTI_ALIAS_FLAG); fcp.style = Paint.Style.FILL
        for (fc in fallingCrystals) {
            val fcx = bx + 5f + (bw - 10f) * fc.first
            val fcy = liqTop + 18f + (liqBot - liqTop - 18f) * fc.second; val s = fc.third
            fcp.color = Color.argb(35, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            c.drawCircle(fcx, fcy, s + 6f, fcp)
            fcp.color = Color.argb(220, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            val p = Path(); p.moveTo(fcx, fcy - s); p.lineTo(fcx + s * 0.8f, fcy)
            p.lineTo(fcx, fcy + s * 0.5f); p.lineTo(fcx - s * 0.8f, fcy); p.close()
            c.drawPath(p, fcp)
            fcp.color = Color.argb(130, 255, 255, 255)
            c.drawCircle(fcx - s * 0.2f, fcy - s * 0.3f, s * 0.22f, fcp)
        }
    }

    private fun drawDissolvingParticles(c: Canvas, bx: Float, by: Float, bw: Float,
        liqTop: Float, liqBot: Float, col: SoluteData) {
        val dp = Paint(Paint.ANTI_ALIAS_FLAG); dp.style = Paint.Style.FILL
        for (p in dissolvingParticles) {
            val px = bx + 10f + (bw - 20f) * p.first
            val py = liqTop + 18f + (liqBot - liqTop - 18f) * (0.1f + p.second * 0.85f)
            val a = ((1f - p.third) * 240f).toInt().coerceIn(0, 240)
            val spread = p.third * 35f
            dp.color = Color.argb(a / 3, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            c.drawCircle(px, py, 10f + p.third * 14f, dp)
            dp.color = Color.argb(a, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            c.drawCircle(px + sin(t * 2f + p.first * 10f) * spread,
                py + cos(t * 1.8f + p.second * 8f) * spread * 0.4f,
                5f + (1f - p.third) * 4f, dp)
        }
    }

    private fun drawBubbles(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, liqBot: Float) {
        val bp = Paint(Paint.ANTI_ALIAS_FLAG); bp.style = Paint.Style.STROKE; bp.strokeWidth = 2f
        for (b in bubbles) {
            val bx2 = bx + 10f + (bw - 20f) * b.first
            val by2 = liqTop + 18f + (liqBot - liqTop - 18f) * b.second
            val a = ((1f - b.third) * 210f).toInt().coerceIn(0, 210)
            val br = 3f + b.third * 8f
            bp.color = Color.argb(a, 200, 240, 255); c.drawCircle(bx2, by2, br, bp)
            bp.color = Color.argb(a / 2, 255, 255, 255)
            c.drawCircle(bx2 - br * 0.3f, by2 - br * 0.3f, br * 0.22f, bp)
        }
    }

    private fun drawSteam(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float) {
        val sp = Paint(Paint.ANTI_ALIAS_FLAG); sp.style = Paint.Style.FILL
        for (s in steamParticles) {
            val sx = bx + bw * s.x; val sy = by + liqTop * s.y
            val a = (s.life * 120f).toInt().coerceIn(0, 120)
            sp.color = Color.argb(a, 200, 220, 240)
            c.drawCircle(sx, sy, s.size * s.life, sp)
            sp.color = Color.argb(a / 2, 220, 240, 255)
            c.drawCircle(sx + s.size, sy - s.size * 0.5f, s.size * s.life * 0.6f, sp)
        }
    }

    private fun drawSplash(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, col: SoluteData) {
        val sip = Paint(Paint.ANTI_ALIAS_FLAG); sip.style = Paint.Style.FILL
        for (s in splashParts) {
            val sx = bx + bw * 0.5f + (s.first - 0.5f) * bw * 0.5f * s.third * 6f
            val sy = liqTop - s.third * 45f * (1f - s.third)
            sip.color = Color.argb(((1f - s.third) * 230f).toInt(),
                Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            c.drawCircle(sx, sy, 6f * (1f - s.third), sip)
        }
    }

    private fun drawDrop(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, col: SoluteData) {
        if (seedDropAnim < 0f) return
        val dx = bx + bw * 0.5f; val dy = by - 45f + (liqTop - by + 45f) * seedDropAnim
        val ds = 16f * (1f - seedDropAnim * 0.1f)
        val dg = Paint(Paint.ANTI_ALIAS_FLAG); dg.style = Paint.Style.FILL
        for (i in 0..6) { dg.color = Color.argb(22 - i * 3, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            c.drawCircle(dx, dy - i * 9f, ds + 8f - i, dg) }
        dg.color = Color.rgb(Color.red(col.color), Color.green(col.color), Color.blue(col.color))
        c.drawCircle(dx, dy, ds, dg)
        val dcP = Paint(Paint.ANTI_ALIAS_FLAG); dcP.style = Paint.Style.FILL; dcP.color = Color.argb(200, 255, 255, 255)
        val dcs = ds * 0.38f; val dp2 = Path(); dp2.moveTo(dx, dy - dcs); dp2.lineTo(dx + dcs * 0.8f, dy)
        dp2.lineTo(dx, dy + dcs * 0.5f); dp2.lineTo(dx - dcs * 0.8f, dy); dp2.close(); c.drawPath(dp2, dcP)
    }

    private fun drawSeedCrystal(c: Canvas, bx: Float, by: Float, bw: Float, liqTop: Float, liqBot: Float) {
        if (!seedCrystal || seedDropAnim < 0f) return
        val sx = bx + bw * seedX; val sy = by + liqTop + (liqBot - liqTop) * seedY
        val sp = Paint(Paint.ANTI_ALIAS_FLAG); sp.style = Paint.Style.FILL
        sp.color = Color.argb(200, 255, 220, 100)
        val size = 5f + seedDropAnim * 4f
        val p = Path(); p.moveTo(sx, sy - size); p.lineTo(sx + size * 0.8f, sy)
        p.lineTo(sx, sy + size * 0.5f); p.lineTo(sx - size * 0.8f, sy); p.close(); c.drawPath(p, sp)
        sp.color = Color.argb(80, 255, 255, 200); c.drawCircle(sx, sy, size + 8f, sp)
        sp.color = Color.argb(150, 255, 255, 255); c.drawCircle(sx - size * 0.2f, sy - size * 0.3f, size * 0.25f, sp)
    }

    private fun drawCrystalGrowth(c: Canvas, bx: Float, by: Float, bw: Float,
        liqTop: Float, liqBot: Float, col: SoluteData) {
        if (crystals.isEmpty()) return
        val cp = Paint(Paint.ANTI_ALIAS_FLAG); cp.style = Paint.Style.FILL
        for (cr in crystals) {
            if (cr.dissolved) continue
            val crx = bx + bw * cr.x; val cry = by + liqTop + (liqBot - liqTop) * cr.y
            val s = cr.size * (1f + cr.growth * 0.5f)
            cp.color = Color.argb(180, Color.red(col.color), Color.green(col.color), Color.blue(col.color))
            val p = Path()
            for (i in 0..5) {
                val ang = Math.toRadians((60.0 * i + cr.rotation).toDouble())
                val px = crx + (cos(ang) * s).toFloat(); val py = cry + (sin(ang) * s).toFloat()
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close(); c.drawPath(p, cp)
            cp.color = Color.argb(100, 255, 255, 255)
            c.drawCircle(crx - s * 0.2f, cry - s * 0.2f, s * 0.18f, cp)
        }
    }

    private fun drawBeakerFront(c: Canvas, bx: Float, by: Float, bw: Float, bh: Float, col: SoluteData) {
        val outlineP = Paint(Paint.ANTI_ALIAS_FLAG); outlineP.style = Paint.Style.STROKE
        outlineP.strokeWidth = 4f; outlineP.color = Color.rgb(77, 208, 225); outlineP.isAntiAlias = true
        c.drawRoundRect(bx, by, bx + bw, by + bh, 12f, 12f, outlineP)
        val rimP = Paint(Paint.ANTI_ALIAS_FLAG); rimP.style = Paint.Style.STROKE; rimP.strokeWidth = 4.5f
        rimP.color = Color.rgb(100, 220, 240); rimP.isAntiAlias = true
        c.drawLine(bx - 4f, by, bx + bw + 4f, by, rimP)
    }

    private fun drawRightPanel(c: Canvas, W: Float, H: Float, px: Float, py: Float, pw: Float, ph: Float) {
        if (pw < 30f) return
        val cardP = Paint(Paint.ANTI_ALIAS_FLAG);         cardP.color = Color.rgb(22, 27, 34); cardP.isAntiAlias = true
        val csP = Paint(Paint.ANTI_ALIAS_FLAG); csP.style = Paint.Style.STROKE; csP.strokeWidth = 2f
        csP.color = Color.rgb(48, 54, 61); csP.isAntiAlias = true
        val gh = ph * 0.32f
        c.drawRoundRect(px, py, px + pw, py + gh, 12f, 12f, cardP)
        c.drawRoundRect(px, py, px + pw, py + gh, 12f, 12f, csP)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG); tp.color = Color.rgb(77, 208, 225); tp.textSize = 13f
        tp.textAlign = Paint.Align.CENTER; tp.isFakeBoldText = true; tp.isAntiAlias = true
        c.drawText("Çözünürlük Grafiği", px + pw / 2, py + 18f, tp)

        val glp = Paint(Paint.ANTI_ALIAS_FLAG); glp.style = Paint.Style.STROKE; glp.strokeWidth = 1f
        glp.color = Color.argb(40, 255, 255, 255); glp.isAntiAlias = true
        for (i in 1..4) c.drawLine(px + 10f, py + 28f + (gh - 48f) * i / 5f,
            px + pw - 10f, py + 28f + (gh - 48f) * i / 5f, glp)

        val scp = Paint(Paint.ANTI_ALIAS_FLAG); scp.textSize = 10f; scp.textAlign = Paint.Align.CENTER
        scp.color = Color.rgb(100, 130, 160); scp.isAntiAlias = true
        c.drawText("0°C", px + 10f, py + gh - 2f, scp)
        c.drawText("100°C", px + pw - 10f, py + gh - 2f, scp)

        val maxVal = sol().maxSol20 * 1.5f
        val glp2 = Paint(Paint.ANTI_ALIAS_FLAG); glp2.style = Paint.Style.STROKE; glp2.strokeWidth = 3f
        glp2.color = sol().color; glp2.isAntiAlias = true; glp2.pathEffect = CornerPathEffect(8f)
        val gPath = Path()
        for (temp in 0..100 step 3) {
            val mx2 = sol().maxSol20 + sol().tempCoeff * (temp - 20f)
            val gpX = px + 10f + (pw - 20f) * temp / 100f
            val gpY = py + gh - 20f - ((gh - 40f) * mx2.coerceAtLeast(0f) / maxVal.coerceAtLeast(1f)).coerceAtMost(gh - 40f)
            if (temp == 0) gPath.moveTo(gpX, gpY) else gPath.lineTo(gpX, gpY)
        }
        c.drawPath(gPath, glp2)

        val curMax = maxSol()
        val dotX = px + 10f + (pw - 20f) * temperature / 100f
        val dotY = py + gh - 20f - ((gh - 40f) * curMax.coerceAtLeast(0f) / maxVal.coerceAtLeast(1f)).coerceAtMost(gh - 40f)
        val dtp = Paint(Paint.ANTI_ALIAS_FLAG); dtp.style = Paint.Style.FILL
        dtp.color = Color.rgb(255, 68, 68); dtp.isAntiAlias = true; c.drawCircle(dotX, dotY, 7f, dtp)
        dtp.color = Color.argb(60, 255, 68, 68); c.drawCircle(dotX, dotY, 13f, dtp)

        val iy = py + gh + 10f; val ih = ph * 0.2f
        c.drawRoundRect(px, iy, px + pw, iy + ih, 12f, 12f, cardP)
        c.drawRoundRect(px, iy, px + pw, iy + ih, 12f, 12f, csP)
        tp.textSize = 14f; c.drawText(sol().name, px + pw / 2, iy + 22f, tp)
        val ip = Paint(Paint.ANTI_ALIAS_FLAG); ip.textSize = 11f; ip.textAlign = Paint.Align.LEFT
        ip.color = Color.rgb(150, 175, 200); ip.isAntiAlias = true
        c.drawText("Formül: ${sol().formula}", px + 8f, iy + 42f, ip)
        c.drawText("Max (20°C): ${"%.2f".format(sol().maxSol20)}g / 100mL", px + 8f, iy + 58f, ip)
        c.drawText("Kategori: ${sol().category}", px + 8f, iy + 74f, ip)

        val sy2 = iy + ih + 10f; val sh2 = ph * 0.22f
        c.drawRoundRect(px, sy2, px + pw, sy2 + sh2, 12f, 12f, cardP)
        c.drawRoundRect(px, sy2, px + pw, sy2 + sh2, 12f, 12f, csP)
        val statusText = when {
            isSupersaturated() -> "AŞIRI DOYUM"
            isSaturated() -> "DOYUM NOKTASI"
            dissolveRatio() > 0.7f -> "YAKIN DOYUM"
            dissolveRatio() > 0.01f -> "ÇÖZÜNÜYOR"
            else -> "BOŞ"
        }
        val statusColor = when {
            isSupersaturated() -> Color.rgb(255, 50, 150)
            isSaturated() -> Color.rgb(255, 170, 50)
            dissolveRatio() > 0.7f -> Color.rgb(255, 210, 50)
            dissolveRatio() > 0.01f -> Color.rgb(77, 208, 225)
            else -> Color.rgb(100, 100, 100)
        }
        tp.textSize = 14f; tp.color = statusColor
        c.drawText(statusText, px + pw / 2, sy2 + 25f, tp)
        tp.color = Color.rgb(77, 208, 225)
        val statusDesc = when {
            isSupersaturated() -> "Kristal tohumu düşürün!"
            isSaturated() -> "Daha fazla çözünmez"
            dissolveRatio() > 0.7f -> "Az kaldı"
            dissolveRatio() > 0.01f -> "Devam ediyor..."
            else -> "Tuz ekleyin"
        }
        tp.textSize = 11f; c.drawText(statusDesc, px + pw / 2, sy2 + 45f, tp)
    }

    private fun drawBottomCards(c: Canvas, W: Float, H: Float, bx: Float, by: Float,
        bw: Float, bh: Float, col: SoluteData, dr: Float) {
        val cy2 = by + bh + 14f; val cH = 44f; val cW = (W - 40f) / 3f
        val crP = Paint(Paint.ANTI_ALIAS_FLAG); crP.style = Paint.Style.STROKE; crP.strokeWidth = 2f; crP.isAntiAlias = true
        val stp = Paint(Paint.ANTI_ALIAS_FLAG); stp.textSize = 10f; stp.textAlign = Paint.Align.CENTER
        stp.color = Color.rgb(130, 155, 180); stp.isAntiAlias = true
        val bp2 = Paint(Paint.ANTI_ALIAS_FLAG); bp2.textSize = 17f; bp2.textAlign = Paint.Align.CENTER
        bp2.isFakeBoldText = true; bp2.isAntiAlias = true

        crP.color = col.color; c.drawRoundRect(10f, cy2, 10f + cW, cy2 + cH, 10f, 10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(22, 27, 34) })
        c.drawRoundRect(10f, cy2, 10f + cW, cy2 + cH, 10f, 10f, crP)
        c.drawText("Çözünen", 10f + cW / 2, cy2 + 13f, stp); bp2.color = col.color
        c.drawText("${"%.1f".format(addedGrams)}g", 10f + cW / 2, cy2 + 34f, bp2)

        crP.color = Color.rgb(255, 136, 68); c.drawRoundRect(20f + cW, cy2, 20f + cW * 2, cy2 + cH, 10f, 10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(22, 27, 34) })
        c.drawRoundRect(20f + cW, cy2, 20f + cW * 2, cy2 + cH, 10f, 10f, crP)
        c.drawText("Maksimum", 20f + cW * 1.5f, cy2 + 13f, stp); bp2.color = Color.rgb(255, 136, 68)
        c.drawText("${"%.1f".format(maxSol())}g", 20f + cW * 1.5f, cy2 + 34f, bp2)

        val pct = (dr * 100f).toInt().coerceIn(0, 300)
        val pctC = if (isSupersaturated()) Color.rgb(255, 50, 150) else if (isSaturated()) Color.rgb(255, 68, 68)
        else if (dr > 0.7f) Color.rgb(255, 170, 51) else Color.rgb(77, 208, 225)
        crP.color = pctC; c.drawRoundRect(30f + cW * 2, cy2, 30f + cW * 3, cy2 + cH, 10f, 10f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(22, 27, 34) })
        c.drawRoundRect(30f + cW * 2, cy2, 30f + cW * 3, cy2 + cH, 10f, 10f, crP)
        c.drawText("Oran", 30f + cW * 2.5f, cy2 + 13f, stp); bp2.color = pctC
        c.drawText("%%%d".format(pct), 30f + cW * 2.5f, cy2 + 34f, bp2)

        val stText = when {
            isSupersaturated() -> "● AŞIRI DOYUM — TOHUM KRİSTAL DÜŞÜRÜN"
            isSaturated() -> "● DOYGUN — ÇÖKELTİ VAR"
            dissolveRatio() > 0.7f -> "● YAKIN DOYUM"
            dissolveRatio() > 0.01f -> "● ÇÖZÜNÜYOR"
            else -> "○ BOŞ"
        }
        val stColor = when {
            isSupersaturated() -> Color.rgb(255, 50, 150)
            isSaturated() -> Color.rgb(255, 68, 68)
            dissolveRatio() > 0.7f -> Color.rgb(255, 170, 51)
            dissolveRatio() > 0.01f -> Color.rgb(77, 208, 225)
            else -> Color.rgb(100, 100, 100)
        }
        val stP = Paint(Paint.ANTI_ALIAS_FLAG); stP.textSize = 13f; stP.textAlign = Paint.Align.CENTER
        stP.isFakeBoldText = true; stP.color = stColor; stP.isAntiAlias = true
        val stBg = Paint(Paint.ANTI_ALIAS_FLAG)
        stBg.color = Color.argb(55, Color.red(stColor), Color.green(stColor), Color.blue(stColor))
        stBg.isAntiAlias = true
        val stW = stP.measureText(stText) + 30f
        c.drawRoundRect(W / 2f - stW / 2f, cy2 + cH + 8f, W / 2f + stW / 2f, cy2 + cH + 32f, 16f, 16f, stBg)
        c.drawText(stText, W / 2f, cy2 + cH + 24f, stP)
    }

    private fun drawGraphMode(c: Canvas, W: Float, H: Float) {
        val pad = 40f; val gw = W - pad * 2; val gh = H - pad * 2 - 60f
        val cardP = Paint(Paint.ANTI_ALIAS_FLAG); cardP.color = Color.rgb(14, 22, 14); cardP.isAntiAlias = true
        c.drawRoundRect(pad - 10f, pad - 10f, W - pad + 10f, pad + gh + 10f, 16f, 16f, cardP)
        val csP = Paint(Paint.ANTI_ALIAS_FLAG); csP.style = Paint.Style.STROKE; csP.strokeWidth = 2f
        csP.color = Color.rgb(48, 54, 61); csP.isAntiAlias = true
        c.drawRoundRect(pad - 10f, pad - 10f, W - pad + 10f, pad + gh + 10f, 16f, 16f, csP)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG); tp.color = Color.rgb(77, 208, 225); tp.textSize = 18f
        tp.textAlign = Paint.Align.CENTER; tp.isFakeBoldText = true; tp.isAntiAlias = true
        c.drawText("TÜM MADDELER — ÇÖZÜNÜRLÜK GRAFİĞİ", W / 2, pad + 20f, tp)

        val glp = Paint(Paint.ANTI_ALIAS_FLAG); glp.style = Paint.Style.STROKE; glp.strokeWidth = 1f
        glp.color = Color.argb(30, 255, 255, 255); glp.isAntiAlias = true
        for (i in 0..5) { val y = pad + 40f + (gh - 60f) * i / 5f; c.drawLine(pad, y, pad + gw, y, glp) }
        for (i in 0..10) { val x = pad + gw * i / 10f; c.drawLine(x, pad + 40f, x, pad + gh - 20f, glp) }

        val scp = Paint(Paint.ANTI_ALIAS_FLAG); scp.textSize = 11f; scp.textAlign = Paint.Align.CENTER
        scp.color = Color.rgb(100, 130, 160); scp.isAntiAlias = true
        c.drawText("0°C", pad + 5f, pad + gh + 5f, scp)
        c.drawText("100°C", pad + gw - 5f, pad + gh + 5f, scp)
        scp.textAlign = Paint.Align.RIGHT
        c.drawText("250g", pad - 8f, pad + 45f, scp)
        c.drawText("0g", pad - 8f, pad + gh - 18f, scp)

        val maxVal = 250f
        for (sol in solutes) {
            val gp = Paint(Paint.ANTI_ALIAS_FLAG); gp.style = Paint.Style.STROKE; gp.strokeWidth = 2.5f
            gp.color = sol.color; gp.isAntiAlias = true; gp.pathEffect = CornerPathEffect(6f)
            val gPath = Path(); var started = false
            for (temp in 0..100 step 2) {
                val mx2 = sol.maxSol20 + sol.tempCoeff * (temp - 20f)
                val gpX = pad + gw * temp / 100f
                val gpY = pad + gh - 20f - ((gh - 60f) * mx2.coerceAtLeast(0f) / maxVal).coerceAtMost(gh - 60f)
                if (!started) { gPath.moveTo(gpX, gpY); started = true } else gPath.lineTo(gpX, gpY)
            }
            c.drawPath(gPath, gp)
        }

        val curX = pad + gw * temperature / 100f
        val curMax = maxSol()
        val curY = pad + gh - 20f - ((gh - 60f) * curMax.coerceAtLeast(0f) / maxVal).coerceAtMost(gh - 60f)
        val dtp = Paint(Paint.ANTI_ALIAS_FLAG); dtp.style = Paint.Style.FILL
        dtp.color = Color.rgb(255, 68, 68); dtp.isAntiAlias = true
        c.drawCircle(curX, curY, 8f, dtp); dtp.color = Color.argb(50, 255, 68, 68)
        c.drawCircle(curX, curY, 14f, dtp)
        c.drawLine(curX, pad + 40f, curX, pad + gh - 20f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f
                color = Color.argb(40, 255, 255, 255); pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f) })

        val ly = pad + gh + 25f
        for ((i, sol) in solutes.withIndex()) {
            val lx = pad + (i % 4) * (gw / 4f); val ly2 = ly + (i / 4) * 20f
            val legP = Paint(Paint.ANTI_ALIAS_FLAG); legP.style = Paint.Style.FILL
            legP.color = sol.color; c.drawCircle(lx + 6f, ly2, 5f, legP)
            legP.textSize = 10f; legP.textAlign = Paint.Align.LEFT; legP.color = sol.color
            c.drawText("${sol.formula} (${sol.name})", lx + 16f, ly2 + 4f, legP)
        }
    }

    private fun drawDataMode(c: Canvas, W: Float, H: Float) {
        val pad = 20f; val cardP = Paint(Paint.ANTI_ALIAS_FLAG); cardP.color = Color.rgb(14, 22, 14); cardP.isAntiAlias = true
        val csP = Paint(Paint.ANTI_ALIAS_FLAG); csP.style = Paint.Style.STROKE; csP.strokeWidth = 1.5f
        csP.color = Color.rgb(48, 54, 61); csP.isAntiAlias = true

        val tp = Paint(Paint.ANTI_ALIAS_FLAG); tp.color = Color.rgb(77, 208, 225); tp.textSize = 16f
        tp.textAlign = Paint.Align.CENTER; tp.isFakeBoldText = true; tp.isAntiAlias = true
        c.drawText("MADDE VERİLERİ", W / 2, pad + 20f, tp)

        val headerH = 30f; val rowH = 50f; val colW = (W - pad * 2) / 5f
        val headerY = pad + 35f
        c.drawRoundRect(pad, headerY, W - pad, headerY + headerH, 8f, 8f, cardP)

        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 11f; hp.textAlign = Paint.Align.CENTER
        hp.color = Color.rgb(77, 208, 225); hp.isFakeBoldText = true; hp.isAntiAlias = true
        val headers = listOf("Madde", "Formül", "Max(g)", "Kategori", "Tehlike")
        for ((i, h) in headers.withIndex()) c.drawText(h, pad + colW * (i + 0.5f), headerY + 20f, hp)

        val rp = Paint(Paint.ANTI_ALIAS_FLAG); rp.textSize = 12f; rp.textAlign = Paint.Align.CENTER
        rp.isAntiAlias = true
        for ((idx, sol) in solutes.withIndex()) {
            val ry = headerY + headerH + idx * rowH
            c.drawRoundRect(pad, ry, W - pad, ry + rowH - 2f, 6f, 6f, cardP)
            if (idx == soluteIdx) c.drawRoundRect(pad, ry, W - pad, ry + rowH - 2f, 6f, 6f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f
                    color = sol.color; isAntiAlias = true })
            rp.color = sol.color; c.drawText(sol.name, pad + colW * 0.5f, ry + 20f, rp)
            rp.color = Color.rgb(180, 210, 230); c.drawText(sol.formula, pad + colW * 1.5f, ry + 20f, rp)
            rp.color = Color.rgb(255, 200, 100); c.drawText("${"%.2f".format(sol.maxSol20)}", pad + colW * 2.5f, ry + 20f, rp)
            rp.color = Color.rgb(150, 200, 150); c.drawText(sol.category, pad + colW * 3.5f, ry + 20f, rp)
            val dangerColor = when (sol.dangerLevel) { 0 -> Color.rgb(77, 208, 225); 1 -> Color.rgb(255, 200, 50)
                2 -> Color.rgb(255, 140, 50); else -> Color.rgb(255, 50, 50) }
            rp.color = dangerColor
            val dangerText = when (sol.dangerLevel) { 0 -> "Güvenli"; 1 -> "Dikkat"; 2 -> "Uyarı"; else -> "Tehlikeli!" }
            c.drawText(dangerText, pad + colW * 4.5f, ry + 20f, rp)
            rp.textSize = 9f; rp.color = Color.rgb(120, 150, 120)
            c.drawText(sol.desc, pad + colW * 0.5f, ry + 38f, rp); rp.textSize = 12f
        }
    }

    private fun drawInfoOverlay(c: Canvas, W: Float, H: Float) {
        val px = W * 0.03f; val py = H * 0.02f; val pw = W * 0.94f; val ph = H * 0.96f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(10, 16, 26); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f
                color = Color.rgb(77, 208, 225); isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 22f; hp.textAlign = Paint.Align.CENTER
        hp.color = Color.rgb(77, 208, 225); hp.isFakeBoldText = true; hp.isAntiAlias = true
        c.drawText("Çözünürlük Simülatörü", W / 2f, ty, hp); ty += 38f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.textSize = 14f; lp.textAlign = Paint.Align.LEFT; lp.isAntiAlias = true
        val lines = listOf(
            Pair("═══ ÇÖZÜNÜRLÜK NEDİR? ═══", Color.rgb(77, 208, 225)),
            Pair("Bir maddenin belirli sıcaklıkta", Color.rgb(180, 210, 230)),
            Pair("çözücüde çözünebileceği maksimum miktar.", Color.rgb(180, 210, 230)),
            Pair("Birim: gram / 100 mL su", Color.rgb(180, 210, 230)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ SICAKLIK ETKİSİ ═══", Color.rgb(77, 208, 225)),
            Pair("• Çoğu katı: Sıcaklık↑ = Çözünürlük↑", Color.rgb(150, 200, 230)),
            Pair("• Bazıları: Sıcaklık↑ = Çözünürlük↓", Color.rgb(255, 200, 130)),
            Pair("• Ca(OH)₂ ve CaCO₃: Ters orantılı", Color.rgb(255, 160, 100)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ DOYGUNLUK DURUMLARI ═══", Color.rgb(77, 208, 225)),
            Pair("• Doymamış: Daha fazla çözünebilir", Color.rgb(77, 208, 225)),
            Pair("• Doymuş: Maksimum çözünmüş", Color.rgb(255, 170, 50)),
            Pair("• Aşırı doymuş: Kararsız durum", Color.rgb(255, 50, 150)),
            Pair("• Çökelme: Fazla madde çöker", Color.rgb(200, 200, 200)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ SİMÜLASYON ÖZELLİKLERİ ═══", Color.rgb(77, 208, 225)),
            Pair("• Isıtma/Soğutma: Sıcaklık ayarı", Color.rgb(150, 200, 255)),
            Pair("• Karıştırma: Çözünme hızlanır", Color.rgb(150, 200, 255)),
            Pair("• Buharlaştırma: Su azalır", Color.rgb(255, 180, 130)),
            Pair("• Aşırı Doyurma: Süperdoygunluk", Color.rgb(255, 100, 200)),
            Pair("• Tohum Kristal: Kristalleşme başlatır", Color.rgb(255, 220, 100)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", Color.rgb(77, 208, 225)),
            Pair("1. Madde seçin  2. Sıcaklığı ayarlayın", Color.rgb(180, 210, 230)),
            Pair("3. Tuz ekleyin  4. Karıştırın", Color.rgb(180, 210, 230)),
            Pair("5. Grafiği veya veriyi inceleyin", Color.rgb(180, 210, 230))
        )
        for ((line, color) in lines) {
            if (line.isEmpty()) { ty += 6f; continue }; lp.color = color
            c.drawText(line, px + 18f, ty, lp); ty += 22f
        }
    }
}