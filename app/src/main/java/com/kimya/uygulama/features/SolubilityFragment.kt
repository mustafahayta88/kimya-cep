package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.*
import kotlin.random.Random

class SolubilityView(context: Context) : View(context) {

    data class Solute(val name: String, val formula: String, val r: Int, val g: Int, val b: Int, val max20: Float, val coeff: Float)

    private val solutes = listOf(
        Solute("NaCl", "NaCl", 79, 195, 247, 36f, 0.02f),
        Solute("Şeker", "C₁₂H₂₂O₁₁", 255, 213, 79, 200f, 0.05f),
        Solute("Bakır Sülfat", "CuSO₄", 41, 121, 255, 20f, 0.08f),
        Solute("Potasyum Nitrat", "KNO₃", 105, 240, 174, 32f, 0.15f),
        Solute("Kalsiyum Hidroksit", "Ca(OH)₂", 255, 152, 0, 2f, -0.01f),
        Solute("Kalsiyum Karbonat", "CaCO₃", 206, 147, 216, 1.5f, -0.005f)
    )

    var soluteIdx = 0; var temperature = 20f; var addedGrams = 0f
    var running = false; var showInfo = false; var dropAnim = -1f; var stirring = false
    val dissolvingParticles = mutableListOf<Triple<Float, Float, Float>>()
    val fallingCrystals = mutableListOf<Triple<Float, Float, Float>>()

    private var animTime = 0f; private var wavePhase = 0f; private var stirAngle = 0f
    var precipitateHeight = 0f // 0..1, how much precipitate at bottom
    private var precipitateShake = 0f
    private val bubbles = mutableListOf<Triple<Float, Float, Float>>()
    private val splashParts = mutableListOf<Triple<Float, Float, Float>>()
    private val ripples = mutableListOf<Triple<Float, Float, Float>>()
    private var saturationPulse = 0f; private var shakeAmount = 0f
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector
    private val handler = Handler(Looper.getMainLooper())

    private val animRun = object : Runnable {
        override fun run() {
            if (!running) return
            animTime += 0.016f; wavePhase += if (stirring) 0.12f else 0.05f
            if (stirring) stirAngle = (stirAngle + 20f) % 360f
            if (shakeAmount > 0f) shakeAmount *= 0.88f
            if (precipitateShake > 0f) precipitateShake *= 0.9f
            updateAnim(); invalidate()
            handler.postDelayed(this, 25)
        }
    }

    private fun solR() = solutes[soluteIdx].r
    private fun solG() = solutes[soluteIdx].g
    private fun solB() = solutes[soluteIdx].b
    private fun solColor(a: Int = 255) = Color.argb(a, solR(), solG(), solB())
    private fun maxSol(): Float = (solutes[soluteIdx].max20 + solutes[soluteIdx].coeff * (temperature - 20f)).coerceAtLeast(0.001f)
    private fun dissolveRatio(): Float = (addedGrams / maxSol()).coerceIn(0f, 1.5f)
    private fun isSaturated() = dissolveRatio() >= 1f
    private fun excessGrams() = (addedGrams - maxSol()).coerceAtLeast(0f)

    init {
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.5f, 3f); return true }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                0 -> { lastTx = e.x; lastTy = e.y; touchMode = 0 }
                2 -> { val dx = e.x - lastTx; val dy = e.y - lastTy; if (abs(dx) > 5 || abs(dy) > 5) touchMode = 1; if (touchMode == 1) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y } }
                1, 3 -> touchMode = 0
            }
            true
        }
    }

    fun start() { running = true; handler.post(animRun) }
    fun stop() { running = false; handler.removeCallbacks(animRun) }

    fun addSalt(grams: Float) {
        addedGrams += grams; dropAnim = 0f; shakeAmount = 4f; precipitateShake = 2f
        val count = (grams / 1.5f).toInt().coerceIn(4, 18)
        for (k in 0..count) fallingCrystals.add(Triple(0.38f + Random.nextFloat() * 0.24f, 0.03f + Random.nextFloat() * 0.04f, Random.nextFloat() * 5f + 2f))
        ripples.add(Triple(0.5f, 0f, 1f))
        for (k in 0..5) splashParts.add(Triple(0.5f + (Random.nextFloat() - 0.5f) * 0.35f, 0.14f, Random.nextFloat() * 0.3f))
    }

    private fun updateAnim() {
        val dr = dissolveRatio(); val sat = isSaturated()
        if (sat) saturationPulse = (saturationPulse + 0.04f) % (2f * PI).toFloat()

        // Precipitate height grows when saturated, shrinks when not
        val targetPrecip = if (sat) (excessGrams() / maxSol()).coerceIn(0f, 0.35f) else 0f
        precipitateHeight += (targetPrecip - precipitateHeight) * 0.03f

        if (dropAnim >= 0f) { dropAnim += 0.038f; if (dropAnim >= 1f) dropAnim = -1f }

        // Falling crystals
        val fit = fallingCrystals.iterator()
        while (fit.hasNext()) {
            val fc = fit.next()
            val speed = if (stirring) 0.016f else 0.01f
            val newY = fc.second + speed
            val swirl = if (stirring) sin(stirAngle * PI.toFloat() / 180f + fc.first * 5f) * 0.012f else 0f
            val newX = fc.first + sin(animTime * 2f + fc.first * 10f) * 0.003f + swirl
            val shrink = if (!sat) 0.994f else 1f; val newSize = fc.third * shrink
            if (newY > 0.88f || newSize < 0.3f) {
                fit.remove()
                if (!sat) for (k in 0..5) dissolvingParticles.add(Triple(newX + (Random.nextFloat() - 0.5f) * 0.12f, newY - 0.02f, 0f))
            } else { val i = fallingCrystals.indexOf(fc); if (i >= 0) fallingCrystals[i] = Triple(newX.coerceIn(0.12f, 0.88f), newY, newSize) }
        }

        // Dissolving particles
        val dit = dissolvingParticles.iterator()
        while (dit.hasNext()) {
            val p = dit.next(); val n = p.third + (if (stirring) 0.02f else 0.01f)
            if (n > 1f) dit.remove() else {
                val i = dissolvingParticles.indexOf(p)
                val sx = if (stirring) cos(animTime * 3f + p.second * 5f) * 0.015f else sin(animTime * 1.5f + p.first * 8f) * 0.006f
                val sy = if (stirring) sin(animTime * 2f + p.first * 4f) * 0.01f else cos(animTime * 1.2f + p.second * 6f) * 0.003f
                if (i >= 0) dissolvingParticles[i] = Triple(p.first + sx, p.second + sy, n)
            }
        }

        // Bubbles
        if (temperature > 20f && Random.nextFloat() < (if (!sat && dr > 0f) 0.1f else 0.04f) * (temperature / 80f))
            bubbles.add(Triple(0.28f + Random.nextFloat() * 0.44f, 0.85f, 0f))
        if (stirring && Random.nextFloat() < 0.07f) bubbles.add(Triple(0.28f + Random.nextFloat() * 0.44f, 0.7f + Random.nextFloat() * 0.15f, 0f))
        val bit = bubbles.iterator()
        while (bit.hasNext()) { val b = bit.next(); val n = b.second - if (stirring) 0.014f else 0.008f; val l = b.third + 0.022f; if (l > 1f || n < 0.05f) bit.remove() else { val i = bubbles.indexOf(b); if (i >= 0) bubbles[i] = Triple(b.first + sin(l * 4f) * 0.007f, n, l) } }

        // Ripples
        val rit = ripples.iterator()
        while (rit.hasNext()) { val r = rit.next(); val nr = r.second + 0.03f; val na = r.third - 0.02f; if (na <= 0f) rit.remove() else { val i = ripples.indexOf(r); if (i >= 0) ripples[i] = Triple(r.first, nr, na) } }

        val spit = splashParts.iterator()
        while (spit.hasNext()) { val s = spit.next(); val n = s.third + 0.05f; if (n > 1f) spit.remove() else { val i = splashParts.indexOf(s); if (i >= 0) splashParts[i] = Triple(s.first, s.second, n) } }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(Color.rgb(10, 14, 23))
        val cw = width.toFloat(); val ch = height.toFloat()
        val mx = Matrix()
        val shX = if (shakeAmount > 0.1f) sin(animTime * 40f) * shakeAmount else 0f
        mx.postScale(zoomScale, zoomScale, cw / 2, ch / 2); mx.postTranslate(panX + shX, panY)
        c.save(); c.concat(mx)

        val bx = cw * 0.2f; val by = ch * 0.1f; val bw = cw * 0.45f; val bh = ch * 0.68f
        val dr = dissolveRatio().coerceAtMost(1f)
        val liqTop = by + bh * 0.06f; val liqBot = by + bh - 3f
        val sr = solR(); val sg = solG(); val sb = solB()
        val precipBot = liqBot
        val precipTop = liqBot - (liqBot - liqTop) * precipitateHeight

        // === GLOW ===
        val gp = Paint(Paint.ANTI_ALIAS_FLAG); gp.maskFilter = BlurMaskFilter(45f, BlurMaskFilter.Blur.NORMAL); gp.color = Color.argb(30, sr, sg, sb)
        c.drawCircle(bx + bw / 2, by + bh * 0.5f, bw, gp)

        // === GLASS 3D ===
        // Glass body
        val glassBg = Paint(Paint.ANTI_ALIAS_FLAG); glassBg.style = Paint.Style.FILL; glassBg.color = Color.argb(25, 80, 160, 200)
        c.drawRoundRect(bx, by, bx + bw, by + bh, 10f, 10f, glassBg)
        // Glass left edge highlight
        val glassHL = Paint(Paint.ANTI_ALIAS_FLAG); glassHL.style = Paint.Style.STROKE; glassHL.strokeWidth = 2f; glassHL.color = Color.argb(100, 180, 230, 255); glassHL.isAntiAlias = true
        c.drawLine(bx + 1, by + 5, bx + 1, by + bh - 5, glassHL)
        // Glass right edge
        val glassR = Paint(Paint.ANTI_ALIAS_FLAG); glassR.style = Paint.Style.STROKE; glassR.strokeWidth = 1.5f; glassR.color = Color.argb(50, 100, 180, 220); glassR.isAntiAlias = true
        c.drawLine(bx + bw - 1, by + 5, bx + bw - 1, by + bh - 5, glassR)

        // === LIQUID ===
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.style = Paint.Style.FILL
        val wr = 100; val wg = 180; val wb = 240; val bl = dr * 1.3f
        lp.color = Color.rgb((wr + (sr - wr) * bl).toInt().coerceIn(0, 255), (wg + (sg - wg) * bl).toInt().coerceIn(0, 255), (wb + (sb - wb) * bl).toInt().coerceIn(0, 255))
        c.drawRect(bx + 5, liqTop, bx + bw - 5, liqBot, lp)

        // Liquid gradient overlay (darker at bottom)
        val lgp = Paint(Paint.ANTI_ALIAS_FLAG); lgp.style = Paint.Style.FILL
        lgp.shader = LinearGradient(bx, liqTop, bx, liqBot, Color.argb(0, 0, 0, 0), Color.argb(60, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawRect(bx + 5, liqTop, bx + bw - 5, liqBot, lgp)

        // Saturation pulse
        if (isSaturated()) {
            val pa = (abs(sin(saturationPulse)) * 55).toInt()
            c.drawRect(bx + 5, liqTop, bx + bw - 5, liqBot, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb(pa, sr, sg, sb) })
        }

        // Surface shine
        val shp = Paint(Paint.ANTI_ALIAS_FLAG); shp.style = Paint.Style.FILL; shp.color = Color.argb(90, sr, sg, sb)
        c.drawRect(bx + 5, liqTop, bx + bw - 5, liqTop + 12f, shp)
        // Surface highlight line
        c.drawLine(bx + 8, liqTop + 2, bx + bw - 8, liqTop + 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f; color = Color.argb(120, 255, 255, 255); isAntiAlias = true })

        // === WAVE ===
        val wp = Paint(Paint.ANTI_ALIAS_FLAG); wp.style = Paint.Style.FILL; wp.color = Color.argb(100, sr, sg, sb)
        val wave = Path(); wave.moveTo(bx + 5, liqTop)
        val wAmp = if (stirring) 9f else 5f
        for (x in 0..(bw - 10).toInt() step 3) wave.lineTo(bx + 5 + x.toFloat(), liqTop + sin(wavePhase + x * 0.07f) * wAmp + cos(wavePhase * 0.6f + x * 0.05f) * 3f)
        wave.lineTo(bx + bw - 5, liqTop + 18f); wave.lineTo(bx + 5, liqTop + 18f); wave.close(); c.drawPath(wave, wp)

        // === RIPPLES ===
        val rp = Paint(Paint.ANTI_ALIAS_FLAG); rp.style = Paint.Style.STROKE; rp.strokeWidth = 2.5f
        for (r in ripples) { val rx = bx + 5 + (bw - 10) * r.first; rp.color = Color.argb((r.third * 220).toInt(), sr, sg, sb); c.drawOval(rx - r.second * 70f, liqTop + 18f - r.second * 12f, rx + r.second * 70f, liqTop + 18f + r.second * 12f, rp) }

        // === PRECIPITATE LAYER ===
        if (precipitateHeight > 0.005f) {
            // Precipitate body — white/gray (real precipitate color)
            val precipP = Paint(Paint.ANTI_ALIAS_FLAG); precipP.style = Paint.Style.FILL
            precipP.shader = LinearGradient(bx, precipTop, bx, precipBot, Color.argb(40, 220, 220, 220), Color.argb(210, 200, 200, 200), Shader.TileMode.CLAMP)
            val pShake = sin(animTime * 8f) * precipitateShake * 2f
            c.drawRect(bx + 5 + pShake, precipTop, bx + bw - 5 + pShake, precipBot, precipP)

            // Precipitate texture — grainy
            val grainP = Paint(Paint.ANTI_ALIAS_FLAG); grainP.style = Paint.Style.FILL
            for (i in 0..20) {
                val gx = bx + 8 + (bw - 16) * (i / 20f) + sin(i * 1.7f) * 4f
                val gy = precipTop + 3 + (precipBot - precipTop - 6) * (0.5f + cos(i * 2.3f) * 0.4f)
                grainP.color = Color.argb(80 + (i % 3) * 20, 180, 180, 180)
                c.drawCircle(gx, gy, 2f + sin(i.toFloat()) * 1f, grainP)
            }

            // Precipitate surface — wavy top
            val ptPath = Path(); ptPath.moveTo(bx + 5, precipTop)
            for (x in 0..(bw - 10).toInt() step 4) {
                val py = precipTop + sin(animTime * 1.5f + x * 0.1f) * 3f + cos(x * 0.15f) * 2f
                ptPath.lineTo(bx + 5 + x.toFloat(), py)
            }
            ptPath.lineTo(bx + bw - 5, precipTop + 8f); ptPath.lineTo(bx + 5, precipTop + 8f); ptPath.close()
            precipP.shader = null; precipP.color = Color.argb(190, 210, 210, 210)
            c.drawPath(ptPath, precipP)

            // Sparkle crystals on surface
            val sparkP = Paint(Paint.ANTI_ALIAS_FLAG); sparkP.style = Paint.Style.FILL
            for (i in 0..10) {
                val sx = bx + 10 + (bw - 20) * (i / 10f) + sin(animTime * 0.8f + i * 1.5f) * 4f
                val sy = precipTop + 4f + cos(animTime * 0.6f + i * 2f) * 2f
                sparkP.color = Color.argb(180, 255, 255, 255)
                c.drawCircle(sx, sy, 1.5f, sparkP)
                sparkP.color = Color.argb(100, 200, 200, 200)
                c.drawCircle(sx + 1f, sy + 1f, 1f, sparkP)
            }

            // Label
            val plp = Paint(Paint.ANTI_ALIAS_FLAG); plp.textSize = 12f; plp.textAlign = Paint.Align.CENTER; plp.color = Color.argb(220, 80, 80, 80); plp.isFakeBoldText = true; plp.isAntiAlias = true
            c.drawText("ÇÖKELTİ", bx + bw / 2, precipBot - 6f, plp)
        }

        // === MEASUREMENT LINES ===
        val mlP = Paint(Paint.ANTI_ALIAS_FLAG); mlP.style = Paint.Style.STROKE
        for (i in 1..9) { val ly = liqTop + (liqBot - liqTop) * i / 10f; mlP.strokeWidth = if (i % 5 == 0) 2.5f else 1f; mlP.color = if (i % 5 == 0) Color.rgb(136, 221, 255) else Color.argb(80, 136, 221, 255); c.drawLine(bx + 5, ly, bx + 5 + if (i % 5 == 0) 15f else 7f, ly, mlP) }

        // === STIR ROD ===
        if (stirring) {
            val rodP = Paint(Paint.ANTI_ALIAS_FLAG); rodP.strokeWidth = 6f; rodP.strokeCap = Paint.Cap.ROUND; rodP.color = Color.rgb(160, 160, 160); rodP.isAntiAlias = true
            val rodX = bx + bw * 0.5f + cos(stirAngle * PI.toFloat() / 180f) * bw * 0.2f
            c.drawLine(rodX, liqTop - 10f, rodX + sin(stirAngle * PI.toFloat() / 180f) * 20f, liqBot - 10f, rodP)
            // Vortex circles
            val vP = Paint(Paint.ANTI_ALIAS_FLAG); vP.style = Paint.Style.STROKE; vP.strokeWidth = 2f; vP.isAntiAlias = true
            for (i in 1..4) { val vr = 10f * i + sin(animTime * 5f) * 4f; vP.color = Color.argb(30 - i * 5, 255, 255, 255); c.drawOval(bx + bw * 0.5f - vr, liqBot - 30f - vr * 0.25f, bx + bw * 0.5f + vr, liqBot - 30f + vr * 0.25f, vP) }
        }

        // === FALLING CRYSTALS ===
        val fcp = Paint(Paint.ANTI_ALIAS_FLAG); fcp.style = Paint.Style.FILL
        for (fc in fallingCrystals) {
            val fcx = bx + 5 + (bw - 10) * fc.first; val fcy = liqTop + 18 + (liqBot - liqTop - 18) * fc.second; val s = fc.third
            // Glow
            fcp.color = Color.argb(40, sr, sg, sb); c.drawCircle(fcx, fcy, s + 5f, fcp)
            // Crystal body
            fcp.color = Color.argb(230, sr, sg, sb)
            val p = Path(); p.moveTo(fcx, fcy - s); p.lineTo(fcx + s * 0.8f, fcy); p.lineTo(fcx, fcy + s * 0.5f); p.lineTo(fcx - s * 0.8f, fcy); p.close(); c.drawPath(p, fcp)
            // Highlight
            fcp.color = Color.argb(120, 255, 255, 255); c.drawCircle(fcx - s * 0.2f, fcy - s * 0.3f, s * 0.2f, fcp)
        }

        // === DISSOLVING PARTICLES ===
        val dp = Paint(Paint.ANTI_ALIAS_FLAG); dp.style = Paint.Style.FILL
        for (p in dissolvingParticles) {
            val px = bx + 10 + (bw - 20) * p.first
            val py = liqTop + 18 + (liqBot - liqTop - 18) * (0.1f + p.second * 0.85f)
            val a = ((1f - p.third) * 255).toInt().coerceIn(0, 255); val spread = p.third * 30f
            // Outer glow
            dp.color = Color.argb(a / 3, sr, sg, sb); c.drawCircle(px, py, 9f + p.third * 12f, dp)
            // Core
            dp.color = Color.argb(a, sr, sg, sb)
            c.drawCircle(px + sin(animTime * 2f + p.first * 10f) * spread, py + cos(animTime * 1.8f + p.second * 8f) * spread * 0.4f, 4.5f + (1f - p.third) * 3.5f, dp)
        }

        // === BUBBLES ===
        val bp = Paint(Paint.ANTI_ALIAS_FLAG); bp.style = Paint.Style.STROKE; bp.strokeWidth = 2f
        for (b in bubbles) {
            val bx2 = bx + 10 + (bw - 20) * b.first; val by2 = liqTop + 18 + (liqBot - liqTop - 18) * b.second
            val a = ((1f - b.third) * 220).toInt().coerceIn(0, 220); val br = 2.5f + b.third * 7f
            bp.color = Color.argb(a, 200, 240, 255); c.drawCircle(bx2, by2, br, bp)
            // Highlight
            bp.color = Color.argb(a / 2, 255, 255, 255); c.drawCircle(bx2 - br * 0.3f, by2 - br * 0.3f, br * 0.2f, bp)
            bp.color = Color.argb(a, 200, 240, 255)
        }

        // === SPLASH ===
        val sip = Paint(Paint.ANTI_ALIAS_FLAG); sip.style = Paint.Style.FILL
        for (s in splashParts) {
            val sx = bx + bw * 0.5f + (s.first - 0.5f) * bw * 0.5f * s.third * 5f; val sy = liqTop - s.third * 40f * (1f - s.third)
            sip.color = Color.argb(((1f - s.third) * 240).toInt(), sr, sg, sb); c.drawCircle(sx, sy, 5.5f * (1f - s.third), sip)
        }

        // === DROP ===
        if (dropAnim >= 0f) {
            val dx = bx + bw * 0.5f; val dy = by - 40f + (liqTop - by + 40f) * dropAnim; val ds = 15f * (1f - dropAnim * 0.12f)
            // Trail
            val dg = Paint(Paint.ANTI_ALIAS_FLAG); dg.style = Paint.Style.FILL
            for (i in 0..5) { dg.color = Color.argb(25 - i * 4, sr, sg, sb); c.drawCircle(dx, dy - i * 8f, ds + 7f - i, dg) }
            // Body
            dg.color = Color.rgb(sr, sg, sb); c.drawCircle(dx, dy, ds, dg)
            // Crystal inside
            val dcP = Paint(Paint.ANTI_ALIAS_FLAG); dcP.style = Paint.Style.FILL; dcP.color = Color.argb(200, 255, 255, 255)
            val dcs = ds * 0.35f; val dp2 = Path(); dp2.moveTo(dx, dy - dcs); dp2.lineTo(dx + dcs * 0.8f, dy); dp2.lineTo(dx, dy + dcs * 0.5f); dp2.lineTo(dx - dcs * 0.8f, dy); dp2.close(); c.drawPath(dp2, dcP)
            c.drawCircle(dx - ds * 0.25f, dy - ds * 0.25f, ds * 0.22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb(160, 255, 255, 255) })
        }

        // === GLASS FRONT ===
        c.drawRoundRect(bx, by, bx + bw, by + bh, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f; color = Color.rgb(136, 221, 255); isAntiAlias = true })
        // Rim
        val rimP = Paint(Paint.ANTI_ALIAS_FLAG); rimP.style = Paint.Style.STROKE; rimP.strokeWidth = 4f; rimP.color = Color.rgb(180, 240, 255); rimP.isAntiAlias = true
        c.drawLine(bx - 3, by, bx + bw + 3, by, rimP)

        // ====== RIGHT PANELS ======
        val gx = bx + bw + 25f; val gy = by; val gw = cw - gx - 15f
        val cardP = Paint(Paint.ANTI_ALIAS_FLAG); cardP.color = Color.rgb(22, 27, 34); cardP.isAntiAlias = true
        val csP = Paint(Paint.ANTI_ALIAS_FLAG); csP.style = Paint.Style.STROKE; csP.strokeWidth = 2f; csP.color = Color.rgb(48, 54, 61); csP.isAntiAlias = true

        // Graph
        val gh = ch * 0.3f
        c.drawRoundRect(gx, gy, gx + gw, gy + gh, 12f, 12f, cardP); c.drawRoundRect(gx, gy, gx + gw, gy + gh, 12f, 12f, csP)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG); tp.color = Color.rgb(0, 240, 255); tp.textSize = 14f; tp.textAlign = Paint.Align.CENTER; tp.isFakeBoldText = true; tp.isAntiAlias = true
        c.drawText("Çözünürlük Grafiği", gx + gw / 2, gy + 20f, tp)
        val glp = Paint(Paint.ANTI_ALIAS_FLAG); glp.style = Paint.Style.STROKE; glp.strokeWidth = 1f; glp.color = Color.argb(50, 255, 255, 255); glp.isAntiAlias = true
        for (i in 1..4) c.drawLine(gx + 12, gy + 30f + (gh - 50f) * i / 5f, gx + gw - 12, gy + 30f + (gh - 50f) * i / 5f, glp)
        val scp = Paint(Paint.ANTI_ALIAS_FLAG); scp.textSize = 11f; scp.textAlign = Paint.Align.CENTER; scp.color = Color.rgb(119, 119, 119); scp.isAntiAlias = true
        c.drawText("0°C", gx + 12, gy + gh - 3, scp); c.drawText("100°C", gx + gw - 12, gy + gh - 3, scp)
        val glp2 = Paint(Paint.ANTI_ALIAS_FLAG); glp2.style = Paint.Style.STROKE; glp2.strokeWidth = 3f; glp2.color = solColor(); glp2.isAntiAlias = true; glp2.pathEffect = CornerPathEffect(8f)
        val gPath = Path()
        for (t in 0..100 step 3) { val mx2 = solutes[soluteIdx].max20 + solutes[soluteIdx].coeff * (t - 20f); val gpX = gx + 12 + (gw - 24) * t / 100f; val gpY = gy + gh - 22 - ((gh - 42) * mx2.coerceAtLeast(0f) / (solutes[soluteIdx].max20 * 1.5f).coerceAtLeast(1f)).coerceAtMost(gh - 42f); if (t == 0) gPath.moveTo(gpX, gpY) else gPath.lineTo(gpX, gpY) }
        c.drawPath(gPath, glp2)
        val curMax = maxSol()
        val dotX = gx + 12 + (gw - 24) * temperature / 100f; val dotY = gy + gh - 22 - ((gh - 42) * curMax.coerceAtLeast(0f) / (solutes[soluteIdx].max20 * 1.5f).coerceAtLeast(1f)).coerceAtMost(gh - 42f)
        val dtp = Paint(Paint.ANTI_ALIAS_FLAG); dtp.style = Paint.Style.FILL; dtp.color = Color.rgb(255, 68, 68); dtp.isAntiAlias = true; c.drawCircle(dotX, dotY, 7f, dtp); dtp.color = Color.argb(70, 255, 68, 68); c.drawCircle(dotX, dotY, 12f, dtp)
        val vlp = Paint(Paint.ANTI_ALIAS_FLAG); vlp.textSize = 12f; vlp.textAlign = Paint.Align.CENTER; vlp.color = solColor(); vlp.isAntiAlias = true; c.drawText("%.1fg".format(curMax), dotX, dotY - 16f, vlp)

        // === SATURATION GAUGE (vertical thermometer) ===
        val gaugeX = gx + gw + 15f; val gaugeW = 28f; val gaugeH = ch * 0.6f; val gaugeY = gy
        // Gauge body
        c.drawRoundRect(gaugeX, gaugeY, gaugeX + gaugeW, gaugeY + gaugeH, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 35, 42); isAntiAlias = true })
        c.drawRoundRect(gaugeX, gaugeY, gaugeX + gaugeW, gaugeY + gaugeH, 14f, 14f, csP)
        // Level marks
        val markP = Paint(Paint.ANTI_ALIAS_FLAG); markP.strokeWidth = 1f; markP.color = Color.rgb(80, 80, 80)
        for (i in 1..4) { val my = gaugeY + gaugeH * i / 5f; c.drawLine(gaugeX + 3, my, gaugeX + gaugeW - 3, my, markP) }
        // Fill
        val fillH = gaugeH * dr.coerceAtMost(1f)
        val gaugeFill = Paint(Paint.ANTI_ALIAS_FLAG); gaugeFill.style = Paint.Style.FILL; gaugeFill.isAntiAlias = true
        val gaugeColor = if (dr >= 1f) Color.rgb(255, 50, 50) else if (dr > 0.75f) Color.rgb(255, 140, 30) else if (dr > 0.5f) Color.rgb(255, 210, 50) else if (dr > 0.25f) Color.rgb(100, 220, 100) else Color.rgb(80, 160, 220)
        gaugeFill.color = gaugeColor
        if (fillH > 0) c.drawRoundRect(gaugeX + 4, gaugeY + gaugeH - fillH, gaugeX + gaugeW - 4, gaugeY + gaugeH - 4, 10f, 10f, gaugeFill)
        // Glow on fill
        if (fillH > 0) { gaugeFill.color = Color.argb(40, Color.red(gaugeColor), Color.green(gaugeColor), Color.blue(gaugeColor)); c.drawRoundRect(gaugeX + 2, gaugeY + gaugeH - fillH - 2, gaugeX + gaugeW - 2, gaugeY + gaugeH - 2, 12f, 12f, gaugeFill) }
        // Labels
        val glp3 = Paint(Paint.ANTI_ALIAS_FLAG); glp3.textSize = 9f; glp3.textAlign = Paint.Align.CENTER; glp3.isAntiAlias = true
        glp3.color = Color.rgb(100, 100, 100)
        c.drawText("100%", gaugeX + gaugeW / 2, gaugeY + 10f, glp3)
        c.drawText("50%", gaugeX + gaugeW / 2, gaugeY + gaugeH / 2 + 4f, glp3)
        c.drawText("0%", gaugeX + gaugeW / 2, gaugeY + gaugeH - 2f, glp3)
        // Current level indicator
        val indY = gaugeY + gaugeH - fillH
        val indP = Paint(Paint.ANTI_ALIAS_FLAG); indP.style = Paint.Style.FILL; indP.color = Color.WHITE; indP.isAntiAlias = true
        c.drawCircle(gaugeX - 5f, indY, 4f, indP)
        c.drawLine(gaugeX - 1f, indY, gaugeX + 4f, indY, Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2f; color = gaugeColor; isAntiAlias = true })
        // Level text
        val lvlP = Paint(Paint.ANTI_ALIAS_FLAG); lvlP.textSize = 10f; lvlP.textAlign = Paint.Align.CENTER; lvlP.isFakeBoldText = true; lvlP.color = gaugeColor; lvlP.isAntiAlias = true
        val lvlText = if (dr >= 1f) "DOYGUN" else if (dr > 0.75f) "YAKIN" else if (dr > 0.5f) "İYİ" else if (dr > 0.25f) "AZ" else "BOŞ"
        c.drawText(lvlText, gaugeX + gaugeW / 2, gaugeY - 8f, lvlP)
        c.drawText("%%%d".format((dr * 100).toInt().coerceAtMost(100)), gaugeX + gaugeW / 2, gaugeY + gaugeH + 16f, lvlP)

        // Info card
        val iy = gy + gh + 12f; val ih = ch * 0.18f
        c.drawRoundRect(gx, iy, gx + gw, iy + ih, 12f, 12f, cardP); c.drawRoundRect(gx, iy, gx + gw, iy + ih, 12f, 12f, csP)
        tp.textSize = 15f; c.drawText(solutes[soluteIdx].name, gx + gw / 2, iy + 24f, tp)
        val ip = Paint(Paint.ANTI_ALIAS_FLAG); ip.textSize = 13f; ip.textAlign = Paint.Align.LEFT; ip.color = Color.rgb(187, 187, 187); ip.isAntiAlias = true
        c.drawText("Formül: ${solutes[soluteIdx].formula}", gx + 10, iy + 48f, ip)
        c.drawText("Max (20°C): ${"%.2f".format(solutes[soluteIdx].max20)}g / 100mL", gx + 10, iy + 68f, ip)
        c.drawText("Sıcaklık: ${if (solutes[soluteIdx].coeff > 0) "+" else ""}${"%.2f".format(solutes[soluteIdx].coeff)} g/°C", gx + 10, iy + 88f, ip)
        ip.textAlign = Paint.Align.CENTER

        // ====== BOTTOM CARDS ======
        val cy2 = by + bh + 16f; val cH = 48f; val cW = (cw - 40f) / 3f
        val crP = Paint(Paint.ANTI_ALIAS_FLAG); crP.style = Paint.Style.STROKE; crP.strokeWidth = 2f; crP.isAntiAlias = true
        val stp = Paint(Paint.ANTI_ALIAS_FLAG); stp.textSize = 11f; stp.textAlign = Paint.Align.CENTER; stp.color = Color.rgb(153, 153, 153); stp.isAntiAlias = true
        val bp2 = Paint(Paint.ANTI_ALIAS_FLAG); bp2.textSize = 18f; bp2.textAlign = Paint.Align.CENTER; bp2.isFakeBoldText = true; bp2.isAntiAlias = true

        crP.color = solColor(); c.drawRoundRect(10f, cy2, 10f + cW, cy2 + cH, 10f, 10f, cardP); c.drawRoundRect(10f, cy2, 10f + cW, cy2 + cH, 10f, 10f, crP)
        c.drawText("Çözünen", 10f + cW / 2, cy2 + 14f, stp); bp2.color = solColor(); c.drawText("%.1fg".format(addedGrams), 10f + cW / 2, cy2 + 36f, bp2)

        crP.color = Color.rgb(255, 136, 68); c.drawRoundRect(20f + cW, cy2, 20f + cW * 2, cy2 + cH, 10f, 10f, cardP); c.drawRoundRect(20f + cW, cy2, 20f + cW * 2, cy2 + cH, 10f, 10f, crP)
        c.drawText("Maksimum", 20f + cW * 1.5f, cy2 + 14f, stp); bp2.color = Color.rgb(255, 136, 68); c.drawText("%.1fg".format(curMax), 20f + cW * 1.5f, cy2 + 36f, bp2)

        val pct = (dr * 100f).toInt().coerceAtMost(999)
        val pctC = if (isSaturated()) Color.rgb(255, 68, 68) else if (dr > 0.7f) Color.rgb(255, 170, 51) else Color.rgb(68, 255, 136)
        crP.color = pctC; c.drawRoundRect(30f + cW * 2, cy2, 30f + cW * 3, cy2 + cH, 10f, 10f, cardP); c.drawRoundRect(30f + cW * 2, cy2, 30f + cW * 3, cy2 + cH, 10f, 10f, crP)
        c.drawText("Oran", 30f + cW * 2.5f, cy2 + 14f, stp); bp2.color = pctC; c.drawText("%%%d".format(pct), 30f + cW * 2.5f, cy2 + 36f, bp2)

        // Status badge
        val stText = if (isSaturated()) "● DOYGUN — ÇÖKELTİ VAR" else if (dr > 0.7f) "● YAKIN DOYUM" else if (dr > 0.01f) "● ÇÖZÜNÜYOR" else "○ BOŞ"
        val stColor = if (isSaturated()) Color.rgb(255, 68, 68) else if (dr > 0.7f) Color.rgb(255, 170, 51) else if (dr > 0.01f) Color.rgb(68, 255, 136) else Color.rgb(102, 102, 102)
        val stP = Paint(Paint.ANTI_ALIAS_FLAG); stP.textSize = 14f; stP.textAlign = Paint.Align.CENTER; stP.isFakeBoldText = true; stP.color = stColor; stP.isAntiAlias = true
        val stBg = Paint(Paint.ANTI_ALIAS_FLAG); stBg.color = Color.argb(60, Color.red(stColor), Color.green(stColor), Color.blue(stColor)); stBg.isAntiAlias = true
        val stW = stP.measureText(stText) + 30f
        c.drawRoundRect(cw / 2f - stW / 2f, cy2 + cH + 8f, cw / 2f + stW / 2f, cy2 + cH + 32f, 16f, 16f, stBg)
        c.drawText(stText, cw / 2f, cy2 + cH + 24f, stP)

        if (showInfo) drawInfo(c, cw, ch)
        c.restore()
    }

    private fun drawInfo(c: Canvas, cw: Float, ch: Float) {
        val px = cw * 0.03f; val py = ch * 0.02f; val pw = cw * 0.94f; val ph = ch * 0.96f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 39); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.rgb(0, 200, 255); isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 24f; hp.textAlign = Paint.Align.CENTER; hp.color = Color.rgb(0, 240, 255); hp.isFakeBoldText = true; hp.isAntiAlias = true
        c.drawText("Çözünürlük Simülatörü", cw / 2f, ty, hp); ty += 40f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.textSize = 16f; lp.textAlign = Paint.Align.LEFT; lp.isAntiAlias = true
        val lines = listOf(
            Pair("═══ NEDİR? ═══", Color.rgb(0, 240, 255)),
            Pair("Çözünürlük: Bir maddenin belirli sıcaklıkta", Color.rgb(220, 220, 220)),
            Pair("çözücüde çözünebileceği maksimum miktar.", Color.rgb(220, 220, 220)),
            Pair("Birim: gram / 100 mL su", Color.rgb(220, 220, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ SICAKLIK ETKİSİ ═══", Color.rgb(0, 240, 255)),
            Pair("• Çoğu katı: Sıcaklık↑ = Çözünürlük↑", Color.rgb(170, 204, 255)),
            Pair("• Bazıları: Sıcaklık↑ = Çözünürlük↓", Color.rgb(255, 200, 150)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ DOYGUNLUK ═══", Color.rgb(0, 240, 255)),
            Pair("• Doymamış: Daha fazla çözünebilir 🟢", Color.rgb(68, 255, 136)),
            Pair("• Doymuş: Maksimum çözünmüş 🟡", Color.rgb(255, 170, 51)),
            Pair("• Aşırı doymuş: Çökelir 🔴", Color.rgb(255, 68, 68)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ FORMÜL ═══", Color.rgb(0, 240, 255)),
            Pair("Çözünürlük = kütle / hacim × 100", Color.rgb(200, 230, 255)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ MADDELER ═══", Color.rgb(0, 240, 255)),
            Pair("• NaCl: 36g  • Şeker: 200g", Color.rgb(150, 200, 255)),
            Pair("• CuSO₄: 20g  • KNO₃: 32g", Color.rgb(150, 200, 255)),
            Pair("• Ca(OH)₂: 2g  • CaCO₃: 1.5g", Color.rgb(255, 180, 130)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", Color.rgb(0, 240, 255)),
            Pair("1. Madde seçin  2. Sıcaklığı ayarlayın", Color.rgb(200, 230, 255)),
            Pair("3. Tuz ekleyin  4. Karıştırın", Color.rgb(200, 230, 255)),
            Pair("5. Çökelmeyi gözlemleyin", Color.rgb(200, 230, 255))
        )
        for ((line, color) in lines) { if (line.isEmpty()) { ty += 6f; continue }; lp.color = color; c.drawText(line, px + 18f, ty, lp); ty += 22f }
        lp.textAlign = Paint.Align.CENTER
    }
}

class SolubilityFragment : Fragment() {
    private lateinit var sv: SolubilityView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext(); sv = SolubilityView(ctx)
        val root = FrameLayout(ctx).apply { setBackgroundColor(Color.rgb(10, 14, 23)); addView(sv, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }

        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(20, 16, 20, 8); gravity = Gravity.CENTER_VERTICAL }
        val sp = Spinner(ctx).apply { adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf("NaCl", "Şeker", "CuSO₄", "KNO₃", "Ca(OH)₂", "CaCO₃")) }
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { sv.soluteIdx = pos; sv.addedGrams = 0f; sv.dissolvingParticles.clear(); sv.fallingCrystals.clear(); sv.precipitateHeight = 0f }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        top.addView(sp, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val hb = TextView(ctx).apply { text = "?"; textSize = 26f; setTextColor(Color.rgb(0, 240, 255)); setPadding(20, 8, 20, 8); setBackgroundColor(Color.rgb(20, 30, 50)) }
        hb.setOnClickListener { sv.showInfo = !sv.showInfo; sv.invalidate() }
        top.addView(hb); root.addView(top)

        val bot = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 20); gravity = Gravity.BOTTOM }
        val tRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        tRow.addView(TextView(ctx).apply { text = "Sıcaklık:"; setTextColor(Color.rgb(204, 204, 204)); textSize = 14f })
        val tBar = SeekBar(ctx).apply { max = 100; progress = 20 }
        val tLbl = TextView(ctx).apply { text = "20°C"; setTextColor(Color.rgb(0, 240, 255)); textSize = 14f; setPadding(10, 0, 0, 0) }
        tBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { sv.temperature = p.toFloat(); tLbl.text = "${p}°C"; sv.invalidate() }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        tRow.addView(tBar, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); tRow.addView(tLbl)
        bot.addView(tRow)

        val bRow1 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 8, 0, 0) }
        fun btn(t: String, bg: Int, act: () -> Unit): TextView = TextView(ctx).apply { text = t; textSize = 13f; setTextColor(Color.WHITE); setBackgroundColor(bg); setPadding(22, 10, 22, 10); gravity = Gravity.CENTER; setOnClickListener { act() } }
        bRow1.addView(btn("+1g", Color.rgb(0, 170, 110)) { sv.addSalt(1f) })
        bRow1.addView(btn("+2g", Color.rgb(0, 190, 120)) { sv.addSalt(2f) }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 6 })
        bRow1.addView(btn("+5g", Color.rgb(0, 204, 136)) { sv.addSalt(5f) }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 6 })
        bRow1.addView(btn("+10g", Color.rgb(0, 136, 204)) { sv.addSalt(10f) }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 6 })
        bot.addView(bRow1)

        val bRow2 = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 6, 0, 0) }
        bRow2.addView(btn("Karıştır", Color.rgb(180, 100, 0)) { sv.stirring = !sv.stirring; sv.invalidate() })
        bRow2.addView(btn("Sıfırla", Color.rgb(85, 85, 85)) { sv.addedGrams = 0f; sv.stirring = false; sv.dissolvingParticles.clear(); sv.fallingCrystals.clear(); sv.precipitateHeight = 0f; sv.invalidate() }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 8 })
        bot.addView(bRow2)
        root.addView(bot)
        return root
    }
    override fun onResume() { super.onResume(); sv.start() }
    override fun onPause() { super.onPause(); sv.stop() }
}
