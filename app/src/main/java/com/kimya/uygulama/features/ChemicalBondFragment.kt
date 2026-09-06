package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*
import kotlin.random.Random

class BondSimView(context: Context) : View(context) {

    private var bondType = 0
    private var animPhase = 0f
    private var animSpeed = 0.02f
    private var isPlaying = true
    private var selectedAtom = -1
    private var temperature = 0.3f
    private var isBroken = false
    private var breakPhase = 0f
    private var touchCount = 0
    private var isDragging = false
    private var dragAtom = -1
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var currentDistance = 0f
    private var restDistance = 0f
    private var forceValue = 0f
    private var bondEnergyKJ = 0f
    private var bondLengthPM = 0
    private var showCloud = false
    private var showComparison = false
    private var compBondType = 1
    private var breakable = true
    private var vibrPhase = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val particles = mutableListOf<Particle>()
    private val trailPoints = mutableListOf<Pair<Float, Float>>()

    data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float,
                        var life: Float, var color: Int, var radius: Float)

    data class AtomInfo(val symbol: String, val name: String, val color: Int, val radius: Float,
                        val electronegativity: Float, val valence: Int, val desc: String,
                        val atomicNumber: Int)

    private val atoms = mapOf(
        "H" to AtomInfo("H", "Hidrojen", 0xFF90CAF9.toInt(), 22f, 2.20f, 1, "En hafif element. 1 elektron.", 1),
        "Li" to AtomInfo("Li", "Lityum", 0xFFFF8A80.toInt(), 28f, 0.98f, 1, "Alkali metal. Kolay electron verir.", 3),
        "Na" to AtomInfo("Na", "Sodyum", 0xFFFFAB40.toInt(), 32f, 0.93f, 1, "Alkali metal. Tuzda bulunur.", 11),
        "Cl" to AtomInfo("Cl", "Klor", 0xFF69F0AE.toInt(), 30f, 3.16f, 1, "Halojen. 1 elektron alir.", 17),
        "O" to AtomInfo("O", "Oksijen", 0xFFFF5252.toInt(), 26f, 3.44f, 2, "2 elektron alir. Yasam icin gerekli.", 8),
        "C" to AtomInfo("C", "Karbon", 0xFFB0BEC5.toInt(), 26f, 2.55f, 4, "4 bag yapar. Organik kimya temeli.", 6),
        "N" to AtomInfo("N", "Azot", 0xFF448AFF.toInt(), 26f, 3.04f, 3, "Atmosferin %78'i. 3 bag yapar.", 7),
        "F" to AtomInfo("F", "Flor", 0xFFB9F6CA.toInt(), 24f, 3.98f, 1, "En yuksek elektronegatiflik.", 9),
        "Mg" to AtomInfo("Mg", "Magnezyum", 0xFFCE93D8.toInt(), 30f, 1.31f, 2, "Toprak alkali. 2 elektron verir.", 12),
        "Fe" to AtomInfo("Fe", "Demir", 0xFFFFAB40.toInt(), 34f, 1.83f, 3, "Gecis metali. Kafes yapisi.", 26),
        "S" to AtomInfo("S", "Kukurt", 0xFFFFFF00.toInt(), 28f, 2.58f, 2, "2 bag yapar. Proteinslerde onemli.", 16)
    )

    data class BondConfig(
        val name: String, val shortDesc: String,
        val atom1: String, val atom2: String,
        val type: String, val detail: String,
        val electrons: Int, val electronColor: Int,
        val energyDesc: String,
        val bondEnergy: Float, val bondLength: Int,
        val whyFormed: String,
        val restDistFactor: Float = 1f
    )

    val bondConfigs = listOf(
        BondConfig("Kovalent Bag (H2)", "Ortak elektron paylasimi",
            "H", "H", "Kovalent",
            "Hidrojen atomlari 1'er elektronunu paylasir.\nOrtak elektron cifti ile guclu bag olusur.",
            2, 0xFF4DD0E1.toInt(), "Dusuk enerji, kararli bag",
            436f, 74, "Hidrojenin 1 elektronluk boslugunu doldurmak icin paylasim yapar.",
            1f),
        BondConfig("Kovalent Bag (H2O)", "Coklu paylasim",
            "H", "O", "Kovalent",
            "Oksijen 2 hidrojen ile bag kurar.\nHer bagda 2 elektron paylasilir.",
            4, 0xFF4DD0E1.toInt(), "Orta enerji, polar bag",
            459f, 96, "Oksijenin 2, hidrojenin 1 elektronluk boslugunu doldurmak icin.",
            0.9f),
        BondConfig("Iyonik Bag (NaCl)", "Elektron transferi",
            "Na", "Cl", "Iyonik",
            "Sodyum 1 elektronunu klore verir.\nNa+ ve Cl- iyonlari olusur.",
            1, 0xFFFFAB40.toInt(), "Yuksek enerji, guclu bag",
            786f, 236, "Dusuk EN'li Na, yuksek EN'li Cl'ye elektron verir. Fark buyukse iyonik bag.",
            1.2f),
        BondConfig("Iyonik Bag (MgO)", "Cift elektron transferi",
            "Mg", "O", "Iyonik",
            "Magnezyum 2 elektronunu oksijene verir.\nMg2+ ve O2- iyonlari olusur.",
            2, 0xFFFFAB40.toInt(), "Cok yuksek enerji",
            3850f, 210, "Mg2+, O2- yuksek yuk farki nedeniyle cok guclu iyonik bag.",
            1.1f),
        BondConfig("Metalik Bag (Fe)", "Elektron denizi",
            "Fe", "Fe", "Metalik",
            "Demir atomlari kafes olusturur.\nSerbest elektronlar her yeri kaplar.",
            6, 0xFFCE93D8.toInt(), "Dusuk enerji, esnek bag",
            425f, 248, "Dusuk EN farki, elektronlar serbestce hareket eder.",
            1.3f),
        BondConfig("Koordinasyon Bagi", "Donor-akceptor",
            "N", "H", "Kovalent",
            "Azot teklesmis elektron ciftini paylasir.\nGecis metalleri ile kompleks olusturur.",
            2, 0xFF69F0AE.toInt(), "Orta enerji, yonelimli bag",
            386f, 101, "Azotun teklesmis cifti, H'nin bos orbitallerine yonelir.",
            0.95f)
    )

    var onAtomTap: ((String, String, String) -> Unit)? = null
    var onBondChange: ((String, String, String, String) -> Unit)? = null
    var onPlayStateChange: ((Boolean) -> Unit)? = null
    var onForceUpdate: ((Float, Float) -> Unit)? = null
    var onBreakStateChange: ((Boolean) -> Unit)? = null
    var onWhyTap: ((String) -> Unit)? = null

    private val animRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                animPhase += animSpeed * (1f + temperature)
                if (animPhase > 1f) animPhase -= 1f
                vibrPhase += animSpeed * temperature * 2f
                if (breakable && !isBroken && temperature > 0.7f && currentDistance > restDistance * 1.3f) {
                    isBroken = true
                    breakPhase = 0f
                    onBreakStateChange?.invoke(true)
                    spawnBreakParticles()
                }
            }
            if (isBroken) {
                breakPhase += 0.02f
                if (breakPhase > 1f) breakPhase = 1f
            }
            updateParticles()
            invalidate()
            postDelayed(this, 30)
        }
    }

    init {
        isClickable = true
        isFocusable = true
        setOnTouchListener { _, e ->
            val w2 = width.toFloat()
            val h2 = height.toFloat()
            val cx = w2 / 2f
            val cy = h2 * 0.42f
            val sc = w2 / 400f
            val cfg = bondConfigs[bondType]

            if (isBroken) return@setOnTouchListener true

            val gap = restDistance

            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (cfg.type == "Metalik") return@setOnTouchListener true
                    val x1 = cx - gap
                    val x2 = cx + gap
                    val a1 = atoms[cfg.atom1] ?: return@setOnTouchListener true
                    val a2 = atoms[cfg.atom2] ?: return@setOnTouchListener true

                    val d1 = sqrt((e.x - x1).toDouble().pow(2.0) + (e.y - cy).toDouble().pow(2.0)).toFloat()
                    val d2 = sqrt((e.x - x2).toDouble().pow(2.0) + (e.y - cy).toDouble().pow(2.0)).toFloat()

                    when {
                        d1 < a1.radius * sc * 2f -> {
                            dragAtom = 0; isDragging = true; dragOffsetX = x1 - e.x; dragOffsetY = cy - e.y
                            selectedAtom = 0; spawnParticles(e.x, e.y, a1.color); onAtomTap?.invoke(a1.name, a1.symbol, a1.desc)
                        }
                        d2 < a2.radius * sc * 2f -> {
                            dragAtom = 1; isDragging = true; dragOffsetX = x2 - e.x; dragOffsetY = cy - e.y
                            selectedAtom = 1; spawnParticles(e.x, e.y, a2.color); onAtomTap?.invoke(a2.name, a2.symbol, a2.desc)
                        }
                        else -> {
                            selectedAtom = -1; touchCount++; spawnParticles(e.x, e.y, cfg.electronColor)
                        }
                    }
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging && dragAtom >= 0) {
                        val newX = e.x + dragOffsetX
                        val newY = e.y + dragOffsetY
                        currentDistance = abs(newX - cx) * 2f
                        forceValue = calcForce(currentDistance, restDistance, cfg.bondEnergy)
                        onForceUpdate?.invoke(forceValue, currentDistance)
                        trailPoints.add(Pair(e.x + dragOffsetX, e.y + dragOffsetY))
                        if (trailPoints.size > 40) trailPoints.removeAt(0)
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        isDragging = false; dragAtom = -1
                        if (currentDistance < restDistance * 0.6f) currentDistance = restDistance * 0.6f
                        trailPoints.clear()
                        invalidate()
                    }
                }
            }
            true
        }
        handler.post(animRunnable)
    }

    private fun calcForce(current: Float, rest: Float, energy: Float): Float {
        if (rest == 0f) return 0f
        val ratio = current / rest
        return when {
            ratio < 1f -> energy * (1f - ratio) * 0.5f
            ratio > 1.5f -> -energy * (ratio - 1.5f) * 2f
            else -> energy * (ratio - 1f) * 0.3f
        }
    }

    private fun spawnParticles(x: Float, y: Float, color: Int) {
        for (i in 0 until 10) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 1.5f + Random.nextFloat() * 4f
            particles.add(Particle(x, y, cos(angle) * speed, sin(angle) * speed,
                1f, color, 3f + Random.nextFloat() * 5f))
        }
    }

    private fun spawnBreakParticles() {
        val w2 = width.toFloat()
        val h2 = height.toFloat()
        val cx = w2 / 2f
        val cy = h2 * 0.42f
        val cfg = bondConfigs[bondType]
        val a2 = atoms[cfg.atom2] ?: return
        for (i in 0 until 30) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = 2f + Random.nextFloat() * 6f
            particles.add(Particle(cx, cy, cos(angle) * speed, sin(angle) * speed,
                1f, a2.color, 4f + Random.nextFloat() * 6f))
        }
    }

    private fun updateParticles() {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx; p.y += p.vy; p.life -= 0.025f; p.vx *= 0.97f; p.vy *= 0.97f
            if (p.life <= 0f) iter.remove()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Boyut bilinince denge mesafesini yeniden hesapla (açılışta width=0 olabilir)
        if (w > 0) {
            restDistance = 100f * (w / 400f) * bondConfigs[bondType].restDistFactor
            invalidate()
        }
    }

    fun setBondType(i: Int) {
        bondType = i.coerceIn(0, bondConfigs.size - 1)
        animPhase = 0f; selectedAtom = -1; isBroken = false; breakPhase = 0f
        currentDistance = 0f; forceValue = 0f; isDragging = false; dragAtom = -1
        particles.clear(); trailPoints.clear()
        val cfg = bondConfigs[bondType]
        bondEnergyKJ = cfg.bondEnergy; bondLengthPM = cfg.bondLength
        restDistance = 100f * (width.toFloat() / 400f) * cfg.restDistFactor
        onBondChange?.invoke(cfg.name, cfg.detail, cfg.energyDesc, cfg.whyFormed)
        onBreakStateChange?.invoke(false)
        invalidate()
    }

    fun setTemperature(t: Float) { temperature = t.coerceIn(0f, 1f) }
    fun togglePlay() { isPlaying = !isPlaying; onPlayStateChange?.invoke(isPlaying) }
    fun toggleCloud() { showCloud = !showCloud; invalidate() }
    fun toggleComparison() { showComparison = !showComparison; invalidate() }
    fun setCompBondType(i: Int) { compBondType = i.coerceIn(0, bondConfigs.size - 1); invalidate() }

    fun resetBond() {
        isBroken = false; breakPhase = 0f; currentDistance = 0f; forceValue = 0f
        isDragging = false; dragAtom = -1; particles.clear(); trailPoints.clear()
        selectedAtom = -1; touchCount = 0
        onBreakStateChange?.invoke(false)
        invalidate()
    }

    fun breakBond() {
        if (isBroken) return
        isBroken = true; breakPhase = 0f
        spawnBreakParticles()
        onBreakStateChange?.invoke(true)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.rgb(10, 14, 23))

        val bgP = Paint(Paint.ANTI_ALIAS_FLAG)
        bgP.shader = RadialGradient(w / 2f, h / 2f, w * 0.5f,
            intArrayOf(Color.rgb(18, 25, 40), Color.rgb(10, 14, 23)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgP)

        val cfg = bondConfigs[bondType]
        val a1 = atoms[cfg.atom1]!!
        val a2 = atoms[cfg.atom2]
        val sc = w / 400f
        val cx = w / 2f
        val cy = h * 0.42f

        drawTempBar(canvas, w, h, sc)
        drawForceMeter(canvas, w, h, sc)

        if (showComparison) {
            drawComparisonMode(canvas, cx, cy, sc, a1, a2, cfg, w, h)
        } else {
            drawMainSim(canvas, cx, cy, sc, a1, a2, cfg, w, h)
        }

        for (p in particles) {
            val pp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true; color = p.color; alpha = (p.life * 200).toInt().coerceIn(0, 200) }
            canvas.drawCircle(p.x, p.y, p.radius * p.life, pp)
        }

        val np = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f * sc; textAlign = Paint.Align.CENTER; color = Color.rgb(77, 208, 225); isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText(cfg.name, cx, h * 0.82f, np)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(170, 190, 210) }
        canvas.drawText(cfg.shortDesc, cx, h * 0.86f, tp)

        val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = cfg.electronColor }
        canvas.drawText("Paylasilan: ${cfg.electrons}e- | EN fark: %.1f | Bag enerjisi: %.0f kJ/mol".format(
            abs((atoms[cfg.atom1]?.electronegativity ?: 0f) - (atoms[cfg.atom2]?.electronegativity ?: 0f)),
            cfg.bondEnergy), cx, h * 0.90f, ep)

        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(100, 120, 140) }
        canvas.drawText("Surdur + sicaklik ayarla | Dokunma: $touchCount", cx, h * 0.94f, lp)
    }

    private fun drawMainSim(canvas: Canvas, cx: Float, cy: Float, sc: Float, a1: AtomInfo, a2: AtomInfo?, cfg: BondConfig, w: Float, h: Float) {
        when (cfg.type) {
            "Kovalent" -> drawCovalent(canvas, cx, cy, sc, a1, a2!!, cfg)
            "Iyonik" -> drawIonic(canvas, cx, cy, sc, a1, a2!!, cfg)
            "Metalik" -> drawMetallic(canvas, cx, cy, sc, a1, cfg)
        }
    }

    private fun drawComparisonMode(canvas: Canvas, cx: Float, cy: Float, sc: Float, a1: AtomInfo, a2: AtomInfo?, cfg: BondConfig, w: Float, h: Float) {
        val dividerY = h * 0.50f
        val divP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 55, 70); strokeWidth = 1.5f; pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f) }
        canvas.drawLine(20f, dividerY, w - 20f, dividerY, divP)

        val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(100, 140, 180) }
        canvas.drawText("--- KARŞILAŞTIRMA ---", cx, dividerY - 4f, labelP)

        val halfH = h * 0.45f
        val topCy = halfH * 0.5f + 20f

        canvas.save()
        canvas.clipRect(0f, 0f, w, dividerY - 8f)
        when (cfg.type) {
            "Kovalent" -> drawCovalent(canvas, cx, topCy, sc * 0.75f, a1, a2!!, cfg)
            "Iyonik" -> drawIonic(canvas, cx, topCy, sc * 0.75f, a1, a2!!, cfg)
            "Metalik" -> drawMetallic(canvas, cx, topCy, sc * 0.75f, a1, cfg)
        }
        canvas.restore()

        val cfg2 = bondConfigs[compBondType]
        val a2a = atoms[cfg2.atom1]!!
        val a2b = atoms[cfg2.atom2]
        val botCy = dividerY + (h - dividerY) * 0.5f

        canvas.save()
        canvas.clipRect(0f, dividerY + 8f, w, h)
        when (cfg2.type) {
            "Kovalent" -> drawCovalent(canvas, cx, botCy, sc * 0.75f, a2a, a2b!!, cfg2)
            "Iyonik" -> drawIonic(canvas, cx, botCy, sc * 0.75f, a2a, a2b!!, cfg2)
            "Metalik" -> drawMetallic(canvas, cx, botCy, sc * 0.75f, a2a, cfg2)
        }
        canvas.restore()

        val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(77, 208, 225) }
        canvas.drawText(cfg.name, cx, dividerY - 12f, cp)
        canvas.drawText(cfg2.name, cx, dividerY + 18f, cp)

        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(206, 147, 216) }
        canvas.drawText("Enerji: ${cfg.bondEnergy} kJ/mol | Uzunluk: ${cfg.bondLength} pm", cx, dividerY - 2f, sp)
        canvas.drawText("Enerji: ${cfg2.bondEnergy} kJ/mol | Uzunluk: ${cfg2.bondLength} pm", cx, dividerY + 28f, sp)
    }

    private fun drawTempBar(canvas: Canvas, w: Float, h: Float, sc: Float) {
        val barX = w - 22f * sc
        val barY = h * 0.04f
        val barW = 8f * sc
        val barH = h * 0.65f

        val bgBarP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 40, 55); style = Paint.Style.FILL }
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 4f, 4f, bgBarP)

        val fillH = barH * temperature
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        val tempColor = if (temperature < 0.5f) Color.rgb(68, 138, 255) else if (temperature < 0.75f) Color.rgb(255, 171, 64) else Color.rgb(255, 82, 82)
        fillP.color = tempColor
        canvas.drawRoundRect(barX, barY + barH - fillH, barX + barW, barY + barH, 4f, 4f, fillP)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(120, 140, 160) }
        canvas.save()
        canvas.rotate(-90f, barX + barW / 2f, barY + barH / 2f)
        canvas.drawText("SICAKLIK", barX + barW / 2f, barY + barH / 2f, tp)
        canvas.restore()

        val tempLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = tempColor }
        canvas.drawText("${(temperature * 3000).toInt()}K", barX + barW / 2f, barY + barH + 14f, tempLabel)
    }

    private fun drawForceMeter(canvas: Canvas, w: Float, h: Float, sc: Float) {
        val barX = 10f * sc
        val barY = h * 0.04f
        val barW = 8f * sc
        val barH = h * 0.65f

        val bgBarP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 40, 55); style = Paint.Style.FILL }
        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, 4f, 4f, bgBarP)

        val normalizedForce = (forceValue / 500f).coerceIn(-1f, 1f)
        val fillH = barH * abs(normalizedForce) / 2f
        val fillColor = if (normalizedForce > 0) Color.rgb(105, 240, 174) else Color.rgb(255, 82, 82)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fillColor; style = Paint.Style.FILL; isAntiAlias = true }
        val midY = barY + barH / 2f
        canvas.drawRoundRect(barX, midY - fillH, barX + barW, midY + fillH, 4f, 4f, fillP)

        val mp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 80, 100); strokeWidth = 1f; style = Paint.Style.STROKE }
        canvas.drawLine(barX, midY, barX + barW, midY, mp)

        val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = fillColor }
        canvas.save()
        canvas.rotate(-90f, barX + barW / 2f, barY + barH / 2f)
        canvas.drawText("KUVVET", barX + barW / 2f, barY + barH / 2f, fp)
        canvas.restore()

        val forceLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(120, 140, 160) }
        canvas.drawText("%.1f rel.".format(abs(forceValue)), barX + barW / 2f, barY + barH + 14f, forceLabel)
    }

    private fun drawAtom(canvas: Canvas, cx: Float, cy: Float, atom: AtomInfo, sc: Float, label: String, selected: Boolean, vibrX: Float = 0f, vibrY: Float = 0f) {
        val r = atom.radius * sc
        val finalCx = cx + vibrX
        val finalCy = cy + vibrY

        if (showCloud) {
            val cloudP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
            for (i in 0 until 5) {
                val cloudR = r * (1.8f + i * 0.4f)
                cloudP.color = Color.argb((15 - i * 3).coerceAtLeast(3), Color.red(atom.color), Color.green(atom.color), Color.blue(atom.color))
                canvas.drawCircle(finalCx, finalCy, cloudR, cloudP)
            }
        }

        val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        glowP.color = Color.argb(25, Color.red(atom.color), Color.green(atom.color), Color.blue(atom.color))
        canvas.drawCircle(finalCx, finalCy, r * 2.2f, glowP)

        val sphereP = Paint(Paint.ANTI_ALIAS_FLAG)
        sphereP.shader = RadialGradient(finalCx - r * 0.3f, finalCy - r * 0.3f, r * 1.5f,
            intArrayOf(
                Color.argb(255, minOf(255, Color.red(atom.color) + 90), minOf(255, Color.green(atom.color) + 90), minOf(255, Color.blue(atom.color) + 90)),
                atom.color,
                Color.argb(255, maxOf(0, Color.red(atom.color) - 70), maxOf(0, Color.green(atom.color) - 70), maxOf(0, Color.blue(atom.color) - 70))
            ),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        sphereP.isAntiAlias = true
        canvas.drawCircle(finalCx, finalCy, r, sphereP)

        val hlP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        hlP.color = Color.argb(140, 255, 255, 255)
        canvas.drawCircle(finalCx - r * 0.25f, finalCy - r * 0.25f, r * 0.28f, hlP)

        val elP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = r * 0.85f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText(label, finalCx, finalCy + elP.textSize * 0.35f, elP)

        val anP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(150, 170, 200) }
        canvas.drawText("${atom.atomicNumber}", finalCx, finalCy + r + 12f, anP)

        if (selected) {
            val selP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(77, 208, 225); style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }
            canvas.drawCircle(finalCx, finalCy, r + 8f, selP)
            selP.strokeWidth = 1.5f; selP.alpha = 80
            canvas.drawCircle(finalCx, finalCy, r + 14f, selP)

            val infoP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true; color = Color.rgb(77, 208, 225) }
            canvas.drawText(atom.name, finalCx, finalCy - r - 16f, infoP)
            canvas.drawText("EN: ${atom.electronegativity} | Val: ${atom.valence} | Z: ${atom.atomicNumber}", finalCx, finalCy - r - 6f,
                infoP.apply { textSize = 7.5f * sc; color = Color.rgb(150, 170, 200) })
        }
    }

    private fun drawCovalent(canvas: Canvas, cx: Float, cy: Float, sc: Float, a1: AtomInfo, a2: AtomInfo, cfg: BondConfig) {
        val gap = restDistance
        val vibrAmp = temperature * 2f * sc
        val vx1 = sin(vibrPhase * 3f) * vibrAmp
        val vx2 = sin(vibrPhase * 3f + PI.toFloat()) * vibrAmp

        val breakOff = if (isBroken) breakPhase * 120f * sc else 0f
        val x1 = cx - gap + vx1 - breakOff
        val x2 = cx + gap + vx2 + breakOff

        if (!isBroken) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 90, 100); strokeWidth = 4f * sc; style = Paint.Style.STROKE; isAntiAlias = true }
            canvas.drawLine(x1, cy, x2, cy, bp)
        } else {
            val crackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 82, 82); strokeWidth = 2f * sc; style = Paint.Style.STROKE; isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) }
            canvas.drawLine(cx - 15f * sc, cy, cx + 15f * sc, cy, crackP)
            val sparkP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 200, 50); style = Paint.Style.FILL; isAntiAlias = true }
            for (i in 0 until 5) {
                val sx = cx + cos(breakPhase * 10f + i * 1.2f) * 20f * sc * breakPhase
                val sy = cy + sin(breakPhase * 10f + i * 1.5f) * 20f * sc * breakPhase
                sparkP.alpha = ((1f - breakPhase) * 200).toInt().coerceIn(0, 200)
                canvas.drawCircle(sx, sy, 3f * sc * (1f - breakPhase), sparkP)
            }
        }

        val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        val eRadius = 5f * sc
        val numElectrons = cfg.electrons
        val eSpacing = 16f * sc

        if (!isBroken) {
            for (i in 0 until numElectrons) {
                val phase = animPhase * 2f * PI.toFloat()
                val startX = cx - eSpacing * (numElectrons - 1) / 2f + i * eSpacing + (vx1 + vx2) / 2f
                val orbitR = 22f * sc
                val angle = phase + i * (2f * PI.toFloat() / numElectrons)
                val ex = startX + cos(angle) * orbitR * 0.3f
                val ey = cy + sin(angle) * orbitR
                ep.color = cfg.electronColor; ep.alpha = (180 + sin(angle) * 75).toInt().coerceIn(100, 255)
                canvas.drawCircle(ex, ey, eRadius, ep)
                val trailP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true; color = cfg.electronColor; alpha = 40 }
                canvas.drawCircle(ex, ey, eRadius * 3f, trailP)
            }
            val midP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(77, 208, 225); textSize = 10f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true }
            canvas.drawText("paylasim bolgesi", cx, cy - 35f * sc, midP)
        } else {
            val breakP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 82, 82); textSize = 12f * sc; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
            canvas.drawText("BAG KOPDU!", cx, cy - 35f * sc, breakP)
            for (i in 0 until numElectrons) {
                val angle = animPhase * 4f + i * 2f
                val ex = cx + cos(angle) * (30f + breakPhase * 60f) * sc
                val ey = cy + sin(angle) * (30f + breakPhase * 60f) * sc
                ep.color = cfg.electronColor; ep.alpha = ((1f - breakPhase) * 180).toInt().coerceIn(0, 180)
                canvas.drawCircle(ex, ey, eRadius * (1f - breakPhase * 0.5f), ep)
            }
        }

        val lenP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 170, 200); textSize = 8f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        canvas.drawText("${cfg.bondLength} pm", cx, cy + 42f * sc, lenP)

        drawAtom(canvas, x1, cy, a1, sc, a1.symbol, selectedAtom == 0, vx1)
        drawAtom(canvas, x2, cy, a2, sc, a2.symbol, selectedAtom == 1, vx2)
    }

    private fun drawIonic(canvas: Canvas, cx: Float, cy: Float, sc: Float, a1: AtomInfo, a2: AtomInfo, cfg: BondConfig) {
        val gap = restDistance * 1.1f
        val vibrAmp = temperature * 2f * sc
        val vx1 = sin(vibrPhase * 3f) * vibrAmp
        val vx2 = sin(vibrPhase * 3f + PI.toFloat()) * vibrAmp
        val breakOff = if (isBroken) breakPhase * 120f * sc else 0f
        val x1 = cx - gap + vx1 - breakOff
        val x2 = cx + gap + vx2 + breakOff

        val transferProgress = if (isBroken) 1f else (sin(animPhase * PI.toFloat() * 2f) + 1f) / 2f

        val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        val electronCount = cfg.electrons
        val eRadius = 5f * sc

        if (!isBroken) {
            for (i in 0 until electronCount) {
                val startX = x1 + a1.radius * sc * 0.5f
                val endX = x2 - a2.radius * sc * 0.5f
                val ex = startX + (endX - startX) * transferProgress
                val ey = cy + (i - (electronCount - 1) / 2f) * 14f * sc + sin(animPhase * 4f + i) * 5f * sc
                ep.color = cfg.electronColor; ep.alpha = 220
                canvas.drawCircle(ex, ey, eRadius, ep)
                val trailP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true; color = cfg.electronColor; alpha = 45 }
                canvas.drawCircle(ex, ey, eRadius * 3.5f, trailP)
            }
            val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cfg.electronColor; strokeWidth = 2f * sc; style = Paint.Style.STROKE; isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(8f * sc, 6f * sc), 0f) }
            canvas.drawLine(x1 + a1.radius * sc, cy - 32f * sc, x2 - a2.radius * sc, cy - 32f * sc, arrowP)
            val arrowHead = Path()
            val ahx = x2 - a2.radius * sc
            arrowHead.moveTo(ahx, cy - 32f * sc); arrowHead.lineTo(ahx - 10f * sc, cy - 38f * sc); arrowHead.lineTo(ahx - 10f * sc, cy - 26f * sc); arrowHead.close()
            canvas.drawPath(arrowHead, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cfg.electronColor; style = Paint.Style.FILL; isAntiAlias = true })
        } else {
            val crackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 82, 82); strokeWidth = 2f * sc; style = Paint.Style.STROKE; isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) }
            canvas.drawLine(cx - 15f * sc, cy, cx + 15f * sc, cy, crackP)
            val sparkP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 200, 50); style = Paint.Style.FILL; isAntiAlias = true }
            for (i in 0 until 5) {
                val sx = cx + cos(breakPhase * 10f + i * 1.2f) * 20f * sc * breakPhase
                val sy = cy + sin(breakPhase * 10f + i * 1.5f) * 20f * sc * breakPhase
                sparkP.alpha = ((1f - breakPhase) * 200).toInt().coerceIn(0, 200)
                canvas.drawCircle(sx, sy, 3f * sc * (1f - breakPhase), sparkP)
            }
        }

        val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cfg.electronColor; textSize = 10f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        if (!isBroken) canvas.drawText("elektron transferi", cx, cy - 42f * sc, labelP)

        // Yük, aktarılan elektron sayısından gelir (NaCl: ±1, MgO: ±2)
        val ch = if (cfg.electrons <= 1) "" else "${cfg.electrons}"
        drawAtom(canvas, x1, cy, a1, sc, "${a1.symbol}${ch}+", selectedAtom == 0, vx1)
        drawAtom(canvas, x2, cy, a2, sc, "${a2.symbol}${ch}-", selectedAtom == 1, vx2)

        val chargeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f * sc; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
        chargeP.color = Color.rgb(255, 82, 82); canvas.drawText("+", x1, cy - a1.radius * sc - 8f, chargeP)
        chargeP.color = Color.rgb(68, 138, 255); canvas.drawText("-", x2, cy - a2.radius * sc - 8f, chargeP)

        if (!isBroken && transferProgress > 0.8f) {
            val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 171, 64); strokeWidth = 3f * sc; style = Paint.Style.STROKE; isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), animPhase * 20f) }
            canvas.drawLine(x1 + a1.radius * sc + 5f, cy, x2 - a2.radius * sc - 5f, cy, bondP)
        }

        val statusP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        if (isBroken) {
            statusP.color = Color.rgb(255, 82, 82); canvas.drawText("IONIK BAG KOPDU!", cx, cy + 45f * sc, statusP)
        } else {
            statusP.color = if (transferProgress > 0.5f) Color.rgb(255, 171, 64) else Color.rgb(150, 170, 200)
            canvas.drawText(if (transferProgress > 0.5f) "Transfer tamamlandi" else "Transfer devam ediyor...", cx, cy + 45f * sc, statusP)
        }

        val lenP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 170, 200); textSize = 8f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        canvas.drawText("${cfg.bondLength} pm", cx, cy + 55f * sc, lenP)
    }

    private fun drawMetallic(canvas: Canvas, cx: Float, cy: Float, sc: Float, atom: AtomInfo, cfg: BondConfig) {
        val cols = 5; val rows = 3
        val spacingX = 60f * sc; val spacingY = 55f * sc
        val offsetX = cx - (cols - 1) * spacingX / 2f; val offsetY = cy - (rows - 1) * spacingY / 2f

        val vibrAmp = temperature * 1.5f * sc
        val breakSpread = if (isBroken) breakPhase * 40f * sc else 0f

        val latticeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (isBroken) Color.rgb(80, 40, 40) else Color.rgb(40, 50, 65); strokeWidth = 1.5f * sc; style = Paint.Style.STROKE; isAntiAlias = true }

        if (!isBroken) {
            for (r in 0 until rows) {
                for (c in 0 until cols - 1) {
                    val x1 = offsetX + c * spacingX + sin(vibrPhase * 2f + r + c) * vibrAmp
                    val y1 = offsetY + r * spacingY + cos(vibrPhase * 2.5f + r + c) * vibrAmp
                    val x2 = offsetX + (c + 1) * spacingX + sin(vibrPhase * 2f + r + c + 1) * vibrAmp
                    canvas.drawLine(x1, y1, x2, y1, latticeP)
                }
            }
            for (c in 0 until cols) {
                for (r in 0 until rows - 1) {
                    val x1 = offsetX + c * spacingX + sin(vibrPhase * 2f + r + c) * vibrAmp
                    val y1 = offsetY + r * spacingY + cos(vibrPhase * 2.5f + r + c) * vibrAmp
                    val y2 = offsetY + (r + 1) * spacingY + cos(vibrPhase * 2.5f + r + c + 1) * vibrAmp
                    canvas.drawLine(x1, y1, x1, y2, latticeP)
                }
            }
        } else {
            val crackP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 82, 82); strokeWidth = 2f * sc; style = Paint.Style.STROKE; isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) }
            canvas.drawLine(cx - 30f * sc, cy - 20f * sc, cx + 30f * sc, cy + 20f * sc, crackP)
            canvas.drawLine(cx - 20f * sc, cy + 20f * sc, cx + 20f * sc, cy - 20f * sc, crackP)
            val sparkP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 200, 50); style = Paint.Style.FILL; isAntiAlias = true }
            for (i in 0 until 8) {
                val sx = cx + cos(breakPhase * 8f + i * 0.8f) * 30f * sc * breakPhase
                val sy = cy + sin(breakPhase * 8f + i) * 30f * sc * breakPhase
                sparkP.alpha = ((1f - breakPhase) * 200).toInt().coerceIn(0, 200)
                canvas.drawCircle(sx, sy, 3f * sc * (1f - breakPhase), sparkP)
            }
        }

        val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        val freeRadius = 4f * sc
        for (i in 0 until cfg.electrons * 8) {
            val angle = animPhase * 2f + i * 1.7f
            val orbitCX = offsetX + (i % cols) * spacingX + sin(vibrPhase * 2f + i) * vibrAmp + if (isBroken) cos(angle) * breakSpread else 0f
            val orbitCY = offsetY + (i / cols % rows) * spacingY + cos(vibrPhase * 2.5f + i) * vibrAmp + if (isBroken) sin(angle) * breakSpread else 0f
            val orbitR = 25f * sc
            val ex = orbitCX + cos(angle + i * 0.5f) * orbitR
            val ey = orbitCY + sin(angle + i * 0.7f) * orbitR
            val inBounds = ex > offsetX - spacingX - breakSpread && ex < offsetX + (cols - 1) * spacingX + spacingX + breakSpread &&
                           ey > offsetY - spacingY - breakSpread && ey < offsetY + (rows - 1) * spacingY + spacingY + breakSpread
            if (inBounds) {
                ep.color = cfg.electronColor; ep.alpha = (100 + sin(angle + i) * 50).toInt().coerceIn(60, 180)
                canvas.drawCircle(ex, ey, freeRadius, ep)
                val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true; color = cfg.electronColor; alpha = 25 }
                canvas.drawCircle(ex, ey, freeRadius * 3f, glowP)
            }
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val ax = offsetX + c * spacingX + sin(vibrPhase * 2f + r + c) * vibrAmp + if (isBroken) (c - 2) * breakSpread * 0.3f else 0f
                val ay = offsetY + r * spacingY + cos(vibrPhase * 2.5f + r + c) * vibrAmp + if (isBroken) (r - 1) * breakSpread * 0.3f else 0f
                drawAtom(canvas, ax, ay, atom, sc * 0.65f, atom.symbol, false)
            }
        }

        if (!isBroken) {
            val arrowColor = 0xFFCE93D8.toInt()
            val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = arrowColor; strokeWidth = 1.5f * sc; style = Paint.Style.STROKE; isAntiAlias = true; pathEffect = DashPathEffect(floatArrayOf(5f, 5f), animPhase * 15f) }
            for (c in 0 until cols - 1) {
                val sx = offsetX + c * spacingX + spacingX * 0.5f
                val sy = offsetY + (rows / 2) * spacingY
                canvas.drawLine(sx, sy - 5f, sx + spacingX * 0.6f, sy - 5f, arrowP)
                val ah = Path()
                ah.moveTo(sx + spacingX * 0.6f, sy - 5f); ah.lineTo(sx + spacingX * 0.6f - 6f, sy - 9f); ah.lineTo(sx + spacingX * 0.6f - 6f, sy - 1f); ah.close()
                canvas.drawPath(ah, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = arrowColor; style = Paint.Style.FILL; isAntiAlias = true })
            }
        }

        val infoP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f * sc; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        if (isBroken) {
            infoP.color = Color.rgb(255, 82, 82); canvas.drawText("METALIK BAG KOPDU! Kafes parcalandi", cx, offsetY + rows * spacingY + 20f * sc, infoP)
        } else {
            infoP.color = Color.rgb(206, 147, 216); canvas.drawText("Kafes + Serbest elektronlar | Akis yonu ->", cx, offsetY + rows * spacingY + 20f * sc, infoP)
        }
        canvas.drawText("${cfg.bondLength} pm", cx, offsetY + rows * spacingY + 32f * sc, infoP.apply { textSize = 8f * sc; color = Color.rgb(150, 170, 200) })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}

class ChemicalBondFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chemical_bond, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        val container = view.findViewById<FrameLayout>(R.id.bond_container)
        val simView = BondSimView(ctx)
        container.addView(simView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

        val btnRow = view.findViewById<LinearLayout>(R.id.bond_btn_row)
        val infoText = view.findViewById<TextView>(R.id.bond_info)
        val detailText = view.findViewById<TextView>(R.id.bond_detail)
        val playBtn = view.findViewById<Button>(R.id.bond_play)
        val energyText = view.findViewById<TextView>(R.id.bond_energy)
        val tempBar = view.findViewById<SeekBar>(R.id.bond_temp_bar)
        val tempLabel = view.findViewById<TextView>(R.id.bond_temp_label)
        val cloudBtn = view.findViewById<Button>(R.id.bond_cloud)
        val compBtn = view.findViewById<Button>(R.id.bond_comp)
        val compRow = view.findViewById<LinearLayout>(R.id.bond_comp_row)
        val breakBtn = view.findViewById<Button>(R.id.bond_break)
        val whyBtn = view.findViewById<Button>(R.id.bond_why)
        val resetBtn = view.findViewById<Button>(R.id.bond_reset)
        val forceText = view.findViewById<TextView>(R.id.bond_force)

        val btns = mutableListOf<TextView>()
        val labels = listOf("Kovalent H2", "Kovalent H2O", "Iyonik NaCl", "Iyonik MgO", "Metalik Fe", "Koordinasyon")

        for ((i, label) in labels.withIndex()) {
            val btn = TextView(ctx).apply {
                text = label; textSize = 11f; setTextColor(Color.WHITE)
                setPadding(16, 10, 16, 10); setBackgroundColor(Color.rgb(30, 40, 55))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 6, 0) }
                setOnClickListener {
                    btns.forEach { it.setBackgroundColor(Color.rgb(30, 40, 55)); it.alpha = 0.6f }
                    setBackgroundColor(Color.rgb(50, 100, 140)); alpha = 1f
                    simView.setBondType(i)
                    breakBtn?.text = "Kopar"
                    breakBtn?.setOnClickListener { simView.breakBond() }
                }
            }
            btns.add(btn); btnRow.addView(btn)
        }
        btns[0].setBackgroundColor(Color.rgb(50, 100, 140)); btns[0].alpha = 1f

        val compBtns = mutableListOf<TextView>()
        for ((i, label) in labels.withIndex()) {
            val btn = TextView(ctx).apply {
                text = label; textSize = 10f; setTextColor(Color.WHITE)
                setPadding(12, 8, 12, 8); setBackgroundColor(Color.rgb(25, 35, 50)); alpha = 0.6f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 4, 0) }
                setOnClickListener {
                    compBtns.forEach { it.setBackgroundColor(Color.rgb(25, 35, 50)); it.alpha = 0.6f }
                    setBackgroundColor(Color.rgb(80, 60, 120)); alpha = 1f
                    simView.setCompBondType(i)
                }
            }
            compBtns.add(btn); compRow.addView(btn)
        }
        compBtns[1].setBackgroundColor(Color.rgb(80, 60, 120)); compBtns[1].alpha = 1f

        playBtn.setOnClickListener { simView.togglePlay() }

        tempBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val t = progress / 100f
                simView.setTemperature(t)
                tempLabel?.text = "Sicaklik: ${(t * 3000).toInt()}K"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        var cloudOn = false
        cloudBtn?.setOnClickListener {
            cloudOn = !cloudOn
            simView.toggleCloud()
            cloudBtn.text = if (cloudOn) "Bulut: Acik" else "Bulut: Kapali"
        }

        var compOn = false
        compBtn?.setOnClickListener {
            compOn = !compOn
            simView.toggleComparison()
            compBtn.text = if (compOn) "Karsilastirma: Acik" else "Karsilastirma: Kapali"
            compRow?.visibility = if (compOn) View.VISIBLE else View.GONE
            btnRow?.visibility = if (compOn) View.GONE else View.VISIBLE
        }
        compRow?.visibility = View.GONE

        breakBtn?.setOnClickListener {
            simView.breakBond()
        }

        whyBtn?.setOnClickListener {
            val currentCfg = simView.bondConfigs[0]
            android.app.AlertDialog.Builder(ctx)
                .setTitle("Neden bu bag?")
                .setMessage(currentCfg.whyFormed)
                .setPositiveButton("Tamam", null)
                .show()
        }

        resetBtn?.setOnClickListener { simView.resetBond() }

        simView.onBondChange = { name, detail, energy, why ->
            infoText.text = name
            detailText.text = detail
            energyText.text = "Enerji: $energy"
            whyBtn?.setOnClickListener {
                android.app.AlertDialog.Builder(ctx)
                    .setTitle("Neden bu bag?")
                    .setMessage(why)
                    .setPositiveButton("Tamam", null)
                    .show()
            }
        }

        simView.onAtomTap = { name, symbol, desc ->
            infoText.text = "$name ($symbol)"
            detailText.text = desc
        }

        simView.onPlayStateChange = { playing ->
            playBtn.text = if (playing) "Durdur" else "Baslat"
        }

        simView.onForceUpdate = { force, dist ->
            forceText?.text = "Kuvvet: %.1f (göreli) | Mesafe: %.0f pm".format(abs(force), dist)
        }

        simView.onBreakStateChange = { broken ->
            if (broken) {
                detailText.text = "BAG KOPDU! Enerji aciga cikti."
                energyText.text = "Kopma! Bag kopma enerjisi aciga cikti."
                breakBtn?.text = "Sifirla"
                breakBtn?.setOnClickListener {
                    simView.resetBond()
                    breakBtn?.text = "Kopar"
                    breakBtn?.setOnClickListener {
                        simView.breakBond()
                    }
                }
            }
        }

        simView.setBondType(0)
    }
}
