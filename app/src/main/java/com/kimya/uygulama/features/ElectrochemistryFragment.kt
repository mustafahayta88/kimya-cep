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

class CellView(context: Context) : View(context) {

    data class Electrode(val symbol: String, val name: String, val color: Int, val potential: Float, val ionName: String, val ionColor: Int, val halfReaction: String)

    private val electrodes = listOf(
        Electrode("Zn", "Çinko", 0xFF8B8B8B.toInt(), -0.76f, "Zn²⁺", 0xFF6688CC.toInt(), "Zn → Zn²⁺ + 2e⁻"),
        Electrode("Fe", "Demir", 0xFFA0522D.toInt(), -0.44f, "Fe²⁺", 0xFFAABB44.toInt(), "Fe → Fe²⁺ + 2e⁻"),
        Electrode("Ni", "Nikel", 0xFFB0C4DE.toInt(), -0.26f, "Ni²⁺", 0xFF44CC88.toInt(), "Ni → Ni²⁺ + 2e⁻"),
        Electrode("Cu", "Bakır", 0xFFCD7F32.toInt(), 0.34f, "Cu²⁺", 0xFF4488FF.toInt(), "Cu²⁺ + 2e⁻ → Cu"),
        Electrode("Ag", "Gümüş", 0xFFC0C0C0.toInt(), 0.80f, "Ag⁺", 0xFFAAAAAA.toInt(), "Ag⁺ + e⁻ → Ag"),
        Electrode("Pt", "Platin", 0xFFE5E4E2.toInt(), 1.20f, "H⁺", 0xFFCC6666.toInt(), "2H⁺ + 2e⁻ → H₂")
    )

    var anodeIdx = 0; var cathodeIdx = 3
    var running = false; var showInfo = false
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector
    private val handler = Handler(Looper.getMainLooper())

    // Animation state
    private var animTime = 0f
    private val electrons = mutableListOf<Float>()
    private val metalFlakes = mutableListOf<Triple<Float, Float, Float>>() // x, y, phase
    private val bubbles = mutableListOf<Pair<Float, Float>>() // x, life
    private val depositFlakes = mutableListOf<Pair<Float, Float>>() // x, size
    private var anodeShrink = 0f
    private var cathodeGrow = 0f

    private val animRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            animTime += 0.02f
            updateAnimation()
            invalidate()
            handler.postDelayed(this, 25)
        }
    }

    // Paints
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E17.toInt() }
    private val glassP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = 0xFF5599BB.toInt(); isAntiAlias = true }
    private val liquidP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val electrodeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val wireP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD700.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    private val electronP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00FF88.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val electronGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCCCC.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF3.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val voltP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); textSize = 36f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val statusP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val saltBridgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAA7744.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val flakeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val bubbleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = 0x88FFFFFF.toInt(); isAntiAlias = true }
    private val depositP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val infoBgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD111827.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val infoHeadP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 22f; isFakeBoldText = true; isAntiAlias = true }
    private val infoTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF3.toInt(); textSize = 20f; isAntiAlias = true }
    private val stepP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); textSize = 22f; isFakeBoldText = true; isAntiAlias = true }
    private val anodeLabelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4444.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val cathodeLabelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4488FF.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val reactionP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 18f; textAlign = Paint.Align.CENTER; isAntiAlias = true }

    init {
        isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.5f, 3f); invalidate(); return true
            }
        })
        repeat(12) { electrons.add(it / 12f) }
    }

    fun setElectrodes(a: Int, c: Int) { anodeIdx = a.coerceIn(0, 5); cathodeIdx = c.coerceIn(0, 5); reset() }
    fun toggleAnimation() { running = !running; if (running) handler.post(animRunnable); else handler.removeCallbacks(animRunnable); invalidate() }
    fun isRunning() = running
    fun toggleInfo() { showInfo = !showInfo; panY = 0f; panX = 0f; invalidate() }

    fun reset() {
        running = false; animTime = 0f; zoomScale = 1f; panX = 0f; panY = 0f
        handler.removeCallbacks(animRunnable)
        electrons.clear(); repeat(12) { electrons.add(it / 12f) }
        metalFlakes.clear(); bubbles.clear(); depositFlakes.clear()
        anodeShrink = 0f; cathodeGrow = 0f; invalidate()
    }

    private fun updateAnimation() {
        // Electrons move along wire
        for (i in electrons.indices) {
            electrons[i] += 0.01f
            if (electrons[i] > 1f) electrons[i] -= 1f
        }
        // Metal flakes fall from anode
        if (Random.nextFloat() < 0.15f) {
            metalFlakes.add(Triple(Random.nextFloat() * 0.4f + 0.3f, 0f, 0f))
        }
        val fit = metalFlakes.iterator()
        while (fit.hasNext()) {
            val f = fit.next()
            val newY = f.second + 0.008f
            if (newY > 1f) fit.remove()
            else metalFlakes[metalFlakes.indexOf(f)] = Triple(f.first, newY, f.third + 0.02f)
        }
        // Bubbles at anode
        if (Random.nextFloat() < 0.08f) {
            bubbles.add(Pair(Random.nextFloat() * 0.15f + 0.15f, 0f))
        }
        val bit = bubbles.iterator()
        while (bit.hasNext()) {
            val b = bit.next()
            val newLife = b.second + 0.015f
            if (newLife > 1f) bit.remove()
            else bubbles[bubbles.indexOf(b)] = Pair(b.first, newLife)
        }
        // Cathode grows
        if (Random.nextFloat() < 0.05f) {
            depositFlakes.add(Pair(Random.nextFloat() * 0.8f + 0.1f, Random.nextFloat() * 0.5f + 0.5f))
        }
        if (depositFlakes.size > 30) depositFlakes.removeAt(0)
        // Electrode size change
        anodeShrink = (anodeShrink + 0.0002f).coerceAtMost(0.15f)
        cathodeGrow = (cathodeGrow + 0.0001f).coerceAtMost(0.1f)
    }

    fun getVoltage(): Float {
        val a = electrodes[anodeIdx]; val c = electrodes[cathodeIdx]
        return (c.potential - a.potential).coerceIn(0f, 3f)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (showInfo) {
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> { lastTy = e.y; touchMode = 1; return true }
                MotionEvent.ACTION_MOVE -> { if (touchMode == 1) { panY += e.y - lastTy; panY = panY.coerceIn(-500f, 0f); lastTy = e.y; invalidate() } }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { touchMode = 0 }
            }
            return true
        }
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; touchMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { touchMode = 2 }
            MotionEvent.ACTION_MOVE -> { if (touchMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }; lastTx = e.x; lastTy = e.y; invalidate() }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { touchMode = 0 }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        if (showInfo) { drawInfoPanel(canvas, w, h); return }

        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        // Title
        canvas.drawText("Galvanik Hücre", w * 0.5f, 38f, titleP)

        // Voltage display
        val voltage = getVoltage()
        canvas.drawText("%.2f V".format(voltage), w * 0.5f, 75f, voltP)

        // Layout
        val bW = w * 0.30f; val bH = h * 0.34f
        val bY = h * 0.42f
        val lX = w * 0.08f; val rX = w * 0.62f

        // ─── ANOT SIDE ───
        drawAnodeSide(canvas, lX, bY, bW, bH, w, h)
        // ─── KATOT SIDE ───
        drawCathodeSide(canvas, rX, bY, bW, bH, w, h)
        // ─── WIRE + VOLT METER ───
        drawWireAndMeter(canvas, lX + bW * 0.5f, bY - 40f, rX + bW * 0.5f, bY - 40f, w, h)
        // ─── SALT BRIDGE ───
        drawSaltBridge(canvas, lX + bW, bY + 10f, rX, bY + 10f)

        // ─── STATUS TEXT (animated) ───
        if (running) drawStatusText(canvas, w, h)

        canvas.restore()
    }

    private fun drawAnodeSide(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, sw: Float, sh: Float) {
        val anode = electrodes[anodeIdx]

        // Labels
        canvas.drawText("⊕ ANOT (−)", x + w * 0.5f, y - 65f, anodeLabelP)
        canvas.drawText("Yükseltgenme", x + w * 0.5f, y - 42f, textP.apply { textSize = 16f; color = 0xFFFF8888.toInt() })

        // Beaker glass
        glassP.color = 0xFF5599BB.toInt()
        canvas.drawRoundRect(x, y, x + w, y + h, 10f, 10f, glassP)

        // Solution
        val solAlpha = if (running) (40 + animTime * 10).toInt().coerceAtMost(100) else 40
        liquidP.color = Color.argb(solAlpha, Color.red(anode.ionColor), Color.green(anode.ionColor), Color.blue(anode.ionColor))
        canvas.drawRoundRect(x + 4f, y + h * 0.3f, x + w - 4f, y + h - 4f, 8f, 8f, liquidP)

        // Solution label
        textP.textSize = 16f; textP.color = 0xFFAABBCC.toInt()
        canvas.drawText("${anode.ionName} çözeltisi", x + w * 0.5f, y + h * 0.65f, textP)
        textP.textSize = 24f; textP.color = 0xFFCCCCCC.toInt()

        // Electrode (shrinks during animation)
        val eW = 22f - anodeShrink * 40f
        val eX = x + w * 0.5f - eW / 2f
        electrodeP.color = anode.color
        canvas.drawRoundRect(eX, y - 35f, eX + eW, y + h + 10f, 4f, 4f, electrodeP)
        // Highlight
        electrodeP.color = Color.argb(80, 255, 255, 255)
        canvas.drawRoundRect(eX + 2f, y - 35f, eX + eW * 0.35f, y + h + 10f, 4f, 4f, electrodeP)

        // Metal flakes falling from anode
        if (running) {
            for (flake in metalFlakes) {
                val fx = x + w * flake.first
                val fy = y + h * 0.3f + h * 0.6f * flake.second
                flakeP.color = anode.color
                canvas.drawCircle(fx, fy, 4f, flakeP)
            }
            // Bubbles
            for (bub in bubbles) {
                val bx = x + w * bub.first
                val by = y + h * 0.7f - h * 0.4f * bub.second
                canvas.drawCircle(bx, by, 5f - bub.second * 3f, bubbleP)
            }
        }

        // Electrode name
        labelP.textSize = 18f
        canvas.drawText(anode.symbol, x + w * 0.5f, y + h + 30f, labelP)
        labelP.textSize = 22f

        // Half reaction
        reactionP.textSize = 16f
        canvas.drawText(anode.halfReaction, x + w * 0.5f, y + h + 52f, reactionP)
        reactionP.textSize = 18f
    }

    private fun drawCathodeSide(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, sw: Float, sh: Float) {
        val cathode = electrodes[cathodeIdx]

        // Labels
        canvas.drawText("⊖ KATOT (+)", x + w * 0.5f, y - 65f, cathodeLabelP)
        canvas.drawText("İndirgenme", x + w * 0.5f, y - 42f, textP.apply { textSize = 16f; color = 0xFF8888FF.toInt() })

        // Beaker glass
        glassP.color = 0xFF5599BB.toInt()
        canvas.drawRoundRect(x, y, x + w, y + h, 10f, 10f, glassP)

        // Solution
        liquidP.color = Color.argb(60, Color.red(cathode.ionColor), Color.green(cathode.ionColor), Color.blue(cathode.ionColor))
        canvas.drawRoundRect(x + 4f, y + h * 0.3f, x + w - 4f, y + h - 4f, 8f, 8f, liquidP)

        // Solution label
        textP.textSize = 16f; textP.color = 0xFFAABBCC.toInt()
        canvas.drawText("${cathode.ionName} çözeltisi", x + w * 0.5f, y + h * 0.65f, textP)
        textP.textSize = 24f; textP.color = 0xFFCCCCCC.toInt()

        // Electrode (grows during animation)
        val eW = 22f + cathodeGrow * 40f
        val eX = x + w * 0.5f - eW / 2f
        electrodeP.color = cathode.color
        canvas.drawRoundRect(eX, y - 35f, eX + eW, y + h + 10f, 4f, 4f, electrodeP)
        electrodeP.color = Color.argb(80, 255, 255, 255)
        canvas.drawRoundRect(eX + 2f, y - 35f, eX + eW * 0.35f, y + h + 10f, 4f, 4f, electrodeP)

        // Deposit on electrode
        if (running) {
            for (dep in depositFlakes) {
                val dx = eX + eW * dep.first
                val dy = y + h * dep.second
                depositP.color = cathode.color
                canvas.drawCircle(dx, dy, 3f + dep.second * 2f, depositP)
            }
        }

        labelP.textSize = 18f
        canvas.drawText(cathode.symbol, x + w * 0.5f, y + h + 30f, labelP)
        labelP.textSize = 22f

        reactionP.textSize = 16f
        canvas.drawText(cathode.halfReaction, x + w * 0.5f, y + h + 52f, reactionP)
        reactionP.textSize = 18f
    }

    private fun drawWireAndMeter(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, sw: Float, sh: Float) {
        val topY = y1 - 90f
        // Wire
        canvas.drawLine(x1, y1, x1, topY, wireP)
        canvas.drawLine(x1, topY, x2, topY, wireP)
        canvas.drawLine(x2, topY, x2, y2, wireP)

        // Voltmeter circle
        val vmX = (x1 + x2) * 0.5f
        canvas.drawCircle(vmX, topY, 26f, Paint().apply { color = 0xFF1A1A2E.toInt(); style = Paint.Style.FILL })
        canvas.drawCircle(vmX, topY, 26f, Paint().apply { color = 0xFFFF0080.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f })
        canvas.drawText("V", vmX, topY + 9f, voltP.apply { textSize = 24f })
        voltP.textSize = 36f

        // Electron flow
        if (running) {
            for (phase in electrons) {
                val px: Float; val py: Float
                when {
                    phase < 0.25f -> { val t = phase / 0.25f; px = x1; py = y1 + (topY - y1) * t }
                    phase < 0.75f -> { val t = (phase - 0.25f) / 0.5f; px = x1 + (x2 - x1) * t; py = topY }
                    else -> { val t = (phase - 0.75f) / 0.25f; px = x2; py = topY + (y2 - topY) * t }
                }
                electronGlow.color = 0x4400FF88.toInt()
                canvas.drawCircle(px, py, 14f, electronGlow)
                canvas.drawCircle(px, py, 6f, electronP)
            }
            // Arrow
            textP.textSize = 16f; textP.color = 0xFF00FF88.toInt()
            canvas.drawText("e⁻ →→→→", vmX, topY - 35f, textP)
            textP.textSize = 24f; textP.color = 0xFFCCCCCC.toInt()
        }
    }

    private fun drawSaltBridge(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path()
        path.moveTo(x1, y1)
        path.cubicTo(x1 + (x2 - x1) * 0.3f, y1 - 50f, x1 + (x2 - x1) * 0.7f, y2 - 50f, x2, y2)
        canvas.drawPath(path, saltBridgeP)

        textP.textSize = 14f; textP.color = 0xFFCC9966.toInt()
        canvas.drawText("Tuz Köprüsü (KCl)", (x1 + x2) * 0.5f, y1 - 65f, textP)
        textP.textSize = 24f; textP.color = 0xFFCCCCCC.toInt()

        if (running) {
            textP.textSize = 12f; textP.color = 0xFF886644.toInt()
            canvas.drawText("K⁺ →→  ←← Cl⁻", (x1 + x2) * 0.5f, y1 - 80f, textP)
            textP.textSize = 24f; textP.color = 0xFFCCCCCC.toInt()
        }
    }

    private fun drawStatusText(canvas: Canvas, w: Float, h: Float) {
        val anode = electrodes[anodeIdx]; val cathode = electrodes[cathodeIdx]
        val y = h * 0.88f

        statusP.textSize = 18f
        canvas.drawText("${anode.symbol} çözülüyor → ${anode.ionName} + e⁻", w * 0.5f, y, statusP)
        canvas.drawText("${cathode.ionName} çöküyor → ${cathode.symbol}", w * 0.5f, y + 28f, statusP.apply { color = 0xFF4488FF.toInt() })
        statusP.color = 0xFF39FF14.toInt()
    }

    private fun drawInfoPanel(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRoundRect(16f, 16f, w - 16f, h - 16f, 20f, 20f, infoBgP)
        canvas.save(); canvas.translate(0f, panY)

        var y = 65f; val left = 36f; val cx = w / 2f

        canvas.drawText("⚡ Galvanik Hücre Rehberi", cx, y, infoHeadP.apply { textSize = 30f; color = 0xFFFFD700.toInt() }); y += 50f

        canvas.drawText("Galvanik hücre nedir?", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 36f
        canvas.drawText("Kimyasal enerjiyi elektrik enerjisine", left, y, infoTextP.apply { textSize = 22f }); y += 30f
        canvas.drawText("çeviren cihazdır. Kalem pil, telefon", left, y, infoTextP); y += 30f
        canvas.drawText("bataryası hep bu prensiple çalışır.", left, y, infoTextP); y += 45f

        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f

        canvas.drawText("⚙ Nasıl Çalışır?", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 40f

        val steps = listOf(
            "➊ ANOT (−): Metal çözülür",
            "   Zn → Zn²⁺ + 2e⁻",
            "   Metal atomları iyonlaşır, elektron bırakır.",
            "",
            "➋ TEL: Elektronlar akar",
            "   Anottan katota doğru akım oluşur.",
            "   Bu enerji ampul, telefon şarj eder.",
            "",
            "➌ KATOT (+): Metal çöker",
            "   Cu²⁺ + 2e⁻ → Cu",
            "   Çözeltideki iyonlar metal oluşturur.",
            "",
            "➍ TUZ KÖPRÜSÜ: Denge sağlar",
            "   K⁺ ve Cl⁻ iyonları hareket eder.",
            "   Elektriksel yük dengesi korunur."
        )
        for (step in steps) {
            if (step.isEmpty()) { y += 14f; continue }
            if (step.startsWith("➊") || step.startsWith("➋") || step.startsWith("➌") || step.startsWith("➍")) {
                canvas.drawText(step, left, y, stepP.apply { textSize = 22f; color = 0xFFFF0080.toInt() }); y += 34f
            } else {
                canvas.drawText(step, left + 8f, y, infoTextP.apply { textSize = 20f }); y += 28f
            }
        }
        y += 15f
        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f

        canvas.drawText("📐 Formüller", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        canvas.drawText("E° hücre = E° katot − E° anot", left + 8f, y, infoTextP.apply { textSize = 22f; color = 0xFF39FF14.toInt() }); y += 32f
        canvas.drawText("Nernst: E = E° − (0,0592/n)·logQ", left + 8f, y, infoTextP.apply { textSize = 22f; color = 0xFF39FF14.toInt() }); y += 32f
        canvas.drawText("ΔG° = −nFE°", left + 8f, y, infoTextP.apply { textSize = 22f; color = 0xFF39FF14.toInt() }); y += 45f

        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f

        canvas.drawText("📊 Elektrot Potansiyelleri", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        val pots = listOf(
            "Li⁺/Li    −3.04 V", "Zn²⁺/Zn   −0.76 V", "Fe²⁺/Fe   −0.44 V",
            "Ni²⁺/Ni   −0.26 V", "H⁺/H₂      0.00 V", "Cu²⁺/Cu   +0.34 V",
            "Ag⁺/Ag    +0.80 V", "Au³⁺/Au   +1.50 V"
        )
        for (p in pots) { canvas.drawText(p, left + 8f, y, infoTextP.apply { textSize = 20f; color = 0xFFCCCCCC.toInt() }); y += 28f }

        y += 15f
        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f

        canvas.drawText("🔍 Örnek Hesaplama", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        canvas.drawText("Zn + Cu²⁺ → Zn²⁺ + Cu", left + 8f, y, infoTextP.apply { textSize = 22f }); y += 32f
        canvas.drawText("E° = 0.34 − (−0.76) = 1.10 V ✓", left + 16f, y, infoTextP.apply { textSize = 22f; color = 0xFF39FF14.toInt() }); y += 32f
        canvas.drawText("Pozitif → Tepkime spontane, enerji üretir", left + 16f, y, infoTextP.apply { textSize = 18f; color = 0xFFFFA500.toInt() }); y += 45f

        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f

        canvas.drawText("🌍 Gerçek Hayat", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        val exs = listOf("• Kalem Pili: Zn−MnO₂", "• Telefon: Li-ion batarya", "• Araba Aküsü: Pb−PbO₂", "• Paslanma: İstenmeyen hücre")
        for (e in exs) { canvas.drawText(e, left + 8f, y, infoTextP.apply { textSize = 20f }); y += 28f }

        y += 20f
        textP.textSize = 14f; textP.color = 0xFF555555.toInt()
        canvas.drawText("Kaydırmak için sürükleyin • Kapatmak için ?", cx, y + 10f, textP)
        textP.textSize = 24f; textP.color = 0xFFCCCCCC.toInt()

        canvas.restore()
    }
}

class ElectrochemistryFragment : Fragment() {
    private lateinit var cellView: CellView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0A0E17.toInt()); setPadding(24, 24, 24, 24) }

        val topBar = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 12) }
        topBar.addView(TextView(requireContext()).apply { text = "Pil Simülatörü"; setTextColor(0xFF00F0FF.toInt()); textSize = 18f; paint.isFakeBoldText = true; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
        topBar.addView(TextView(requireContext()).apply {
            text = "?"; setTextColor(0xFF000000.toInt()); textSize = 18f; paint.isFakeBoldText = true; setPadding(28, 8, 28, 8)
            background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xFFFF0080.toInt()) }
            setOnClickListener { cellView.toggleInfo() }
        })
        v.addView(topBar)

        cellView = CellView(requireContext())
        v.addView(cellView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val spinners = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 4) }
        val anodeSpin = Spinner(requireContext()); val cathodeSpin = Spinner(requireContext())
        val names = arrayOf("Zn (−0.76V)", "Fe (−0.44V)", "Ni (−0.26V)", "Cu (+0.34V)", "Ag (+0.80V)", "Pt (+1.20V)")
        anodeSpin.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        cathodeSpin.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        anodeSpin.setSelection(0); cathodeSpin.setSelection(3)
        anodeSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { cellView.setElectrodes(pos, cellView.cathodeIdx) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        cathodeSpin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { cellView.setElectrodes(cellView.anodeIdx, pos) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinners.addView(anodeSpin, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        spinners.addView(cathodeSpin, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(spinners)

        val btns = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 0); gravity = Gravity.CENTER }
        btns.addView(Button(requireContext()).apply {
            text = "▶ Başlat"; setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF00F0FF.toInt())
            setOnClickListener { cellView.toggleAnimation(); text = if (cellView.isRunning()) "⏸ Durdur" else "▶ Başlat" }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btns.addView(Button(requireContext()).apply {
            text = "↺ Sıfırla"; setTextColor(0xFF00F0FF.toInt()); setBackgroundColor(0xFF1A1A2E.toInt()); setPadding(24, 0, 24, 0)
            setOnClickListener { cellView.reset(); anodeSpin.setSelection(0); cathodeSpin.setSelection(3); btns.getChildAt(0).let { (it as Button).text = "▶ Başlat" } }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(btns)

        return v
    }
}
