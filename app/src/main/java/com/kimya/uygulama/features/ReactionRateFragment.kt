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

class ReactionRateView(context: Context) : View(context) {

    private var temperature = 25f; private var concA = 1f; private var concB = 1f; private var catalyst = false
    private var time = 0f; private var running = false; private var showInfo = false
    private val dataPoints = mutableListOf<Pair<Float, Float>>()
    private var animTime = 0f
    private val molecules = mutableListOf<Triple<Float, Float, Int>>() // x, y, type (0=A,1=B,2=C)
    private val collisionFx = mutableListOf<Triple<Float, Float, Float>>() // x, y, life
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector
    private val handler = Handler(Looper.getMainLooper())

    private val animRun = object : Runnable {
        override fun run() {
            if (!running) return
            animTime += 0.016f
            time += 0.1f
            val k = 0.05f * exp((temperature - 25f) * 0.03f) * if (catalyst) 3f else 1f
            val rate = k * concA * concB
            dataPoints.add(Pair(time, rate))
            updateMolecules(k, rate)
            if (time >= 20f) { running = false; invalidate(); return }
            invalidate(); handler.postDelayed(this, 50)
        }
    }

    init {
        isClickable = true; isFocusable = true
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

    fun setTemp(v: Int) { temperature = v.toFloat(); invalidate() }
    fun setConcA(v: Int) { concA = v / 100f; invalidate() }
    fun setConcB(v: Int) { concB = v / 100f; invalidate() }
    fun toggleCatalyst() { catalyst = !catalyst; invalidate() }
    fun toggleInfo() { showInfo = !showInfo; invalidate() }
    fun start() {
        if (!running) {
            running = true; dataPoints.clear(); time = 0f; molecules.clear(); collisionFx.clear()
            // Spawn initial molecules
            for (i in 0..(concA * 15).toInt()) molecules.add(Triple(Random.nextFloat(), Random.nextFloat(), 0))
            for (i in 0..(concB * 15).toInt()) molecules.add(Triple(Random.nextFloat(), Random.nextFloat(), 1))
            handler.post(animRun)
        }
    }
    fun stop() { running = false }
    fun reset() { running = false; dataPoints.clear(); time = 0f; molecules.clear(); collisionFx.clear(); invalidate() }

    private fun updateMolecules(k: Float, rate: Float) {
        // Move molecules
        val speed = 0.008f + k * 0.1f
        val it = molecules.iterator()
        while (it.hasNext()) {
            val m = it.next()
            val nx = m.first + (Random.nextFloat() - 0.5f) * speed
            val ny = m.second + (Random.nextFloat() - 0.5f) * speed
            if (m.third < 2) { // Reactants
                molecules[molecules.indexOf(m)] = Triple(nx.coerceIn(0.05f, 0.95f), ny.coerceIn(0.05f, 0.95f), m.third)
            }
        }
        // Collision → product
        if (Random.nextFloat() < rate * 0.3f) {
            val aMols = molecules.filter { it.third == 0 }
            val bMols = molecules.filter { it.third == 1 }
            if (aMols.isNotEmpty() && bMols.isNotEmpty()) {
                val a = aMols.random(); val b = bMols.random()
                val ax = molecules.indexOf(a); val bx = molecules.indexOf(b)
                if (ax >= 0 && bx >= 0) {
                    val mx2 = (a.first + b.first) / 2f; val my2 = (a.second + b.second) / 2f
                    molecules.removeAt(maxOf(ax, bx)); molecules.removeAt(minOf(ax, bx))
                    molecules.add(Triple(mx2, my2, 2))
                    collisionFx.add(Triple(mx2, my2, 1f))
                }
            }
        }
        // Fade collision effects
        val cit = collisionFx.iterator()
        while (cit.hasNext()) { val c = cit.next(); val nl = c.third - 0.05f; if (nl <= 0f) cit.remove() else { val i = collisionFx.indexOf(c); if (i >= 0) collisionFx[i] = Triple(c.first, c.second, nl) } }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(Color.rgb(10, 14, 23))
        val w = width.toFloat(); val h = height.toFloat()
        val mx = Matrix(); mx.postScale(zoomScale, zoomScale, w / 2, h / 2); mx.postTranslate(panX, panY)
        c.save(); c.concat(mx)

        val k = 0.05f * exp((temperature - 25f) * 0.03f) * if (catalyst) 3f else 1f
        val initialRate = k * concA * concB
        val maxRate = maxOf(initialRate * 1.5f, 0.01f)

        // === GRAPH ===
        val gLeft = w * 0.08f; val gRight = w * 0.62f; val gTop = h * 0.03f; val gBot = h * 0.38f
        val gw = gRight - gLeft; val gh = gBot - gTop
        val cardP = Paint(Paint.ANTI_ALIAS_FLAG); cardP.color = Color.rgb(22, 27, 34); cardP.isAntiAlias = true
        val csP = Paint(Paint.ANTI_ALIAS_FLAG); csP.style = Paint.Style.STROKE; csP.strokeWidth = 2f; csP.color = Color.rgb(48, 54, 61); csP.isAntiAlias = true
        c.drawRoundRect(gLeft, gTop, gRight, gBot, 10f, 10f, cardP); c.drawRoundRect(gLeft, gTop, gRight, gBot, 10f, 10f, csP)

        // Title
        val tp = Paint(Paint.ANTI_ALIAS_FLAG); tp.color = Color.rgb(0, 240, 255); tp.textSize = 14f; tp.textAlign = Paint.Align.CENTER; tp.isFakeBoldText = true; tp.isAntiAlias = true
        c.drawText("Reaksiyon Hızı Grafiği", gLeft + gw / 2, gTop + 18f, tp)

        // Grid
        val gridP = Paint(Paint.ANTI_ALIAS_FLAG); gridP.style = Paint.Style.STROKE; gridP.strokeWidth = 1f; gridP.color = Color.argb(30, 255, 255, 255)
        for (i in 1..4) { val gy2 = gTop + 25f + (gh - 40f) * i / 5f; c.drawLine(gLeft + 8, gy2, gRight - 8, gy2, gridP) }
        for (i in 0..4) { val gx2 = gLeft + gw * i / 4f; c.drawLine(gx2, gBot - 4, gx2, gBot + 4, gridP) }

        // Axis labels
        val ap = Paint(Paint.ANTI_ALIAS_FLAG); ap.textSize = 10f; ap.textAlign = Paint.Align.CENTER; ap.color = Color.rgb(120, 120, 120); ap.isAntiAlias = true
        c.drawText("Zaman (s)", gLeft + gw / 2, gBot + 18f, ap)
        c.save(); c.rotate(-90f, gLeft - 16f, gTop + gh / 2); c.drawText("Hız (mol/L·s)", gLeft - 16f, gTop + gh / 2, ap); c.restore()
        for (i in 0..4) c.drawText("${i * 5}", gLeft + gw * i / 4f, gBot + 12f, ap)

        // Rate curve with gradient fill
        if (dataPoints.size > 1) {
            val path = Path(); val fillPath = Path()
            fillPath.moveTo(gLeft + 8, gBot - 4)
            for ((i, pt) in dataPoints.withIndex()) {
                val px = gLeft + 8 + (gw - 16) * pt.first / 20f
                val py = gBot - 4 - (gh - 30) * (pt.second / maxRate).coerceIn(0f, 1f)
                if (i == 0) { path.moveTo(px, py); fillPath.lineTo(px, py) } else { path.lineTo(px, py); fillPath.lineTo(px, py) }
            }
            fillPath.lineTo(gLeft + 8 + (gw - 16) * dataPoints.last().first / 20f, gBot - 4); fillPath.close()
            // Fill gradient
            val fillP = Paint(Paint.ANTI_ALIAS_FLAG); fillP.style = Paint.Style.FILL; fillP.isAntiAlias = true
            fillP.shader = LinearGradient(0f, gTop + 25f, 0f, gBot - 4, Color.argb(60, 0, 200, 100), Color.argb(10, 0, 200, 100), Shader.TileMode.CLAMP)
            c.drawPath(fillPath, fillP)
            // Line
            val lineP = Paint(Paint.ANTI_ALIAS_FLAG); lineP.style = Paint.Style.STROKE; lineP.strokeWidth = 3f; lineP.color = Color.rgb(0, 200, 100); lineP.isAntiAlias = true; lineP.pathEffect = CornerPathEffect(5f)
            c.drawPath(path, lineP)
        }

        // === MOLECULE VIEW ===
        val mvLeft = w * 0.08f; val mvTop = h * 0.42f; val mvW = w * 0.54f; val mvH = h * 0.35f
        c.drawRoundRect(mvLeft, mvTop, mvLeft + mvW, mvTop + mvH, 12f, 12f, cardP); c.drawRoundRect(mvLeft, mvTop, mvLeft + mvW, mvTop + mvH, 12f, 12f, csP)
        tp.textSize = 13f; c.drawText("Moleküler Görünüm", mvLeft + mvW / 2, mvTop + 16f, tp)

        // Collision effects
        val cfxP = Paint(Paint.ANTI_ALIAS_FLAG); cfxP.style = Paint.Style.FILL
        for (fx in collisionFx) {
            val fx2 = mvLeft + 8 + (mvW - 16) * fx.first; val fy = mvTop + 22 + (mvH - 30) * fx.second
            cfxP.color = Color.argb((fx.third * 200).toInt(), 255, 255, 100)
            c.drawCircle(fx2, fy, (1f - fx.third) * 20f, cfxP)
            cfxP.color = Color.argb((fx.third * 100).toInt(), 255, 200, 50)
            c.drawCircle(fx2, fy, (1f - fx.third) * 30f, cfxP)
        }

        // Molecules
        val molP = Paint(Paint.ANTI_ALIAS_FLAG); molP.style = Paint.Style.FILL; molP.isAntiAlias = true
        for (m in molecules) {
            val mx3 = mvLeft + 8 + (mvW - 16) * m.first; val my = mvTop + 22 + (mvH - 30) * m.second
            val color = when (m.third) { 0 -> Color.rgb(100, 180, 255); 1 -> Color.rgb(255, 180, 50); else -> Color.rgb(100, 255, 150) }
            molP.color = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
            c.drawCircle(mx3, my, 12f, molP)
            molP.color = color; c.drawCircle(mx3, my, 6f, molP)
            molP.color = Color.argb(120, 255, 255, 255); c.drawCircle(mx3 - 1.5f, my - 1.5f, 2f, molP)
        }

        // Legend
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.textSize = 10f; lp.textAlign = Paint.Align.LEFT; lp.isAntiAlias = true
        val legY = mvTop + mvH - 8f
        lp.color = Color.rgb(100, 180, 255); c.drawCircle(mvLeft + 12, legY - 3, 4f, lp)
        lp.color = Color.rgb(200, 200, 200); c.drawText("A", mvLeft + 20, legY, lp)
        lp.color = Color.rgb(255, 180, 50); c.drawCircle(mvLeft + 42, legY - 3, 4f, lp)
        lp.color = Color.rgb(200, 200, 200); c.drawText("B", mvLeft + 50, legY, lp)
        lp.color = Color.rgb(100, 255, 150); c.drawCircle(mvLeft + 72, legY - 3, 4f, lp)
        lp.color = Color.rgb(200, 200, 200); c.drawText("Ürün", mvLeft + 80, legY, lp)

        // === FORMULA CARD ===
        val fcLeft = w * 0.66f; val fcTop = h * 0.42f; val fcW = w * 0.3f; val fcH = h * 0.17f
        c.drawRoundRect(fcLeft, fcTop, fcLeft + fcW, fcTop + fcH, 10f, 10f, cardP); c.drawRoundRect(fcLeft, fcTop, fcLeft + fcW, fcTop + fcH, 10f, 10f, csP)
        tp.textSize = 12f; c.drawText("Formül", fcLeft + fcW / 2, fcTop + 16f, tp)
        val fp = Paint(Paint.ANTI_ALIAS_FLAG); fp.textSize = 13f; fp.textAlign = Paint.Align.CENTER; fp.color = Color.rgb(200, 230, 255); fp.isAntiAlias = true
        c.drawText("r = k · [A] · [B]", fcLeft + fcW / 2, fcTop + 38f, fp)
        fp.color = Color.rgb(255, 200, 100)
        c.drawText("k = ${"%.4f".format(k)}", fcLeft + fcW / 2, fcTop + 58f, fp)
        if (catalyst) { fp.color = Color.rgb(255, 100, 100); c.drawText("⚡ Katalizör aktif (3x)", fcLeft + fcW / 2, fcTop + 75f, fp) }

        // === ENERGY DIAGRAM ===
        val edLeft = w * 0.66f; val edTop = h * 0.62f; val edW = w * 0.3f; val edH = h * 0.15f
        c.drawRoundRect(edLeft, edTop, edLeft + edW, edTop + edH, 10f, 10f, cardP); c.drawRoundRect(edLeft, edTop, edLeft + edW, edTop + edH, 10f, 10f, csP)
        tp.textSize = 12f; c.drawText("Enerji Diyagramı", edLeft + edW / 2, edTop + 16f, tp)
        // Energy curve
        val ep = Paint(Paint.ANTI_ALIAS_FLAG); ep.style = Paint.Style.STROKE; ep.strokeWidth = 2.5f; ep.isAntiAlias = true
        val ePath = Path()
        val ea = if (catalyst) 0.4f else 1f
        for (i in 0..40) {
            val t = i / 40f
            val ex = edLeft + 10 + (edW - 20) * t
            val ey = edTop + edH - 10 - (edH - 25) * (0.3f + ea * 0.5f * sin(t * PI.toFloat()) * (1f - t * 0.4f))
            if (i == 0) ePath.moveTo(ex, ey) else ePath.lineTo(ex, ey)
        }
        ep.color = if (catalyst) Color.rgb(255, 100, 100) else Color.rgb(100, 200, 255)
        c.drawPath(ePath, ep)
        fp.textSize = 10f; fp.color = Color.rgb(150, 150, 150)
        c.drawText("Reaktanlar", edLeft + 15, edTop + edH - 5, fp)
        c.drawText("Ürünler", edLeft + edW - 15, edTop + edH - 5, fp)

        // === RESULTS CARD ===
        val rcLeft = w * 0.08f; val rcTop = h * 0.8f; val rcW = w * 0.88f; val rcH = h * 0.17f
        c.drawRoundRect(rcLeft, rcTop, rcLeft + rcW, rcTop + rcH, 10f, 10f, cardP); c.drawRoundRect(rcLeft, rcTop, rcLeft + rcW, rcTop + rcH, 10f, 10f, csP)

        // Values
        val vp = Paint(Paint.ANTI_ALIAS_FLAG); vp.textAlign = Paint.Align.CENTER; vp.isFakeBoldText = true; vp.isAntiAlias = true
        vp.textSize = 16f; vp.color = Color.rgb(0, 240, 255)
        c.drawText("Hız = ${"%.4f".format(initialRate)} mol/L·s", rcLeft + rcW / 2, rcTop + 24f, vp)
        vp.textSize = 12f; vp.color = Color.rgb(200, 200, 200)
        c.drawText("k = ${"%.4f".format(k)} | T = ${temperature.toInt()}°C | [A] = ${"%.2f".format(concA)} M | [B] = ${"%.2f".format(concB)} M", rcLeft + rcW / 2, rcTop + 46f, vp)
        if (catalyst) { vp.color = Color.rgb(255, 100, 100); c.drawText("⚡ Katalizör: Hız 3x arttı!", rcLeft + rcW / 2, rcTop + 65f, vp) }

        // Info panel
        if (showInfo) drawInfo(c, w, h)
        c.restore()
    }

    private fun drawInfo(c: Canvas, w: Float, h: Float) {
        val px = w * 0.03f; val py = h * 0.02f; val pw = w * 0.94f; val ph = h * 0.96f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 39); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.rgb(0, 200, 255); isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 22f; hp.textAlign = Paint.Align.CENTER; hp.color = Color.rgb(0, 240, 255); hp.isFakeBoldText = true; hp.isAntiAlias = true
        c.drawText("Reaksiyon Hızı Simülatörü", w / 2f, ty, hp); ty += 38f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.textSize = 16f; lp.textAlign = Paint.Align.LEFT; lp.isAntiAlias = true
        val lines = listOf(
            Pair("═══ NEDİR? ═══", Color.rgb(0, 240, 255)),
            Pair("Reaksiyon hızı: Kimyasal bir reaksiyonun", Color.rgb(220, 220, 220)),
            Pair("ne kadar hızlı gerçekleştiğini ölçer.", Color.rgb(220, 220, 220)),
            Pair("Birim: mol / (L·s)", Color.rgb(220, 220, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ HIZ YASASI ═══", Color.rgb(0, 240, 255)),
            Pair("r = k · [A] · [B]", Color.rgb(255, 200, 100)),
            Pair("k: Hız sabiti (sıcaklıkla değişir)", Color.rgb(180, 200, 220)),
            Pair("[A], [B]: Madde derişimleri", Color.rgb(180, 200, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ ETKİLEYEN FAKTÖRLER ═══", Color.rgb(0, 240, 255)),
            Pair("• Sıcaklık↑ → Hız↑ (Arrhenius)", Color.rgb(170, 204, 255)),
            Pair("• Derişim↑ → Hız↑", Color.rgb(170, 204, 255)),
            Pair("• Katalizör → Hız↑ (Ea↓)", Color.rgb(255, 150, 150)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ ARRHENIUS ═══", Color.rgb(0, 240, 255)),
            Pair("k = A · e^(-Ea/RT)", Color.rgb(255, 200, 100)),
            Pair("Ea: Aktivasyon enerjisi", Color.rgb(180, 200, 220)),
            Pair("T: Sıcaklık (Kelvin)", Color.rgb(180, 200, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", Color.rgb(0, 240, 255)),
            Pair("1. Sıcaklık ayarlayın", Color.rgb(200, 230, 255)),
            Pair("2. [A] ve [B] derişimlerini ayarlayın", Color.rgb(200, 230, 255)),
            Pair("3. Katalizörü açıp kapatın", Color.rgb(200, 230, 255)),
            Pair("4. Başlat ile deneyi başlatın", Color.rgb(200, 230, 255))
        )
        for ((line, color) in lines) { if (line.isEmpty()) { ty += 6f; continue }; lp.color = color; c.drawText(line, px + 18f, ty, lp); ty += 22f }
        lp.textAlign = Paint.Align.CENTER
    }
}

class ReactionRateFragment : Fragment() {
    private lateinit var rv: ReactionRateView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext(); rv = ReactionRateView(ctx)
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10, 14, 23)); setPadding(12, 12, 12, 12) }

        // Top bar
        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 8) }
        top.addView(TextView(ctx).apply { text = "Reaksiyon Hızı"; textSize = 22f; setTextColor(Color.rgb(0, 240, 255)); setTypeface(null, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val helpBtn = TextView(ctx).apply { text = "?"; textSize = 26f; setTextColor(Color.rgb(0, 240, 255)); setPadding(20, 8, 20, 8); setBackgroundColor(Color.rgb(20, 30, 50)) }
        helpBtn.setOnClickListener { rv.toggleInfo() }
        top.addView(helpBtn)
        root.addView(top)

        // Canvas
        root.addView(rv, LinearLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.55f).toInt())

        // Controls
        fun makeLabel(text: String, color: Int = Color.rgb(170, 170, 170)): TextView = TextView(ctx).apply { this.text = text; textSize = 13f; setTextColor(color); setPadding(0, 6, 0, 0) }
        val tempLabel = makeLabel("Sıcaklık: 25°C")
        root.addView(tempLabel)
        root.addView(SeekBar(ctx).apply { max = 100; progress = 25; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { rv.setTemp(p); tempLabel.text = "Sıcaklık: ${p}°C" }
            override fun onStartTrackingTouch(sb: SeekBar?) {} override fun onStopTrackingTouch(sb: SeekBar?) {}
        }) })

        val concALabel = makeLabel("[A]: 1.00 M")
        root.addView(concALabel)
        root.addView(SeekBar(ctx).apply { max = 200; progress = 100; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { rv.setConcA(p); concALabel.text = "[A]: ${"%.2f".format(p / 100f)} M" }
            override fun onStartTrackingTouch(sb: SeekBar?) {} override fun onStopTrackingTouch(sb: SeekBar?) {}
        }) })

        val concBLabel = makeLabel("[B]: 1.00 M")
        root.addView(concBLabel)
        root.addView(SeekBar(ctx).apply { max = 200; progress = 100; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) { rv.setConcB(p); concBLabel.text = "[B]: ${"%.2f".format(p / 100f)} M" }
            override fun onStartTrackingTouch(sb: SeekBar?) {} override fun onStopTrackingTouch(sb: SeekBar?) {}
        }) })

        // Buttons
        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 0) }
        fun btn(t: String, bg: Int, act: () -> Unit): TextView = TextView(ctx).apply { text = t; textSize = 14f; setTextColor(Color.WHITE); setBackgroundColor(bg); setPadding(20, 12, 20, 12); gravity = Gravity.CENTER; setOnClickListener { act() } }
        btnRow.addView(btn("Başlat", Color.rgb(0, 180, 100)) { rv.start() })
        btnRow.addView(btn("Durdur", Color.rgb(200, 80, 60)) { rv.stop() }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 8 })
        btnRow.addView(btn("Sıfırla", Color.rgb(85, 85, 85)) { rv.reset() }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 8 })
        btnRow.addView(btn("Katalizör", Color.rgb(180, 100, 0)) { rv.toggleCatalyst() }.apply { (layoutParams as? LinearLayout.LayoutParams)?.marginStart = 8 })
        root.addView(btnRow)

        // Formula
        root.addView(TextView(ctx).apply { text = "A + B → C | r = k[A][B] | Arrhenius: k = Ae^(-Ea/RT)"; textSize = 11f; setTextColor(Color.rgb(100, 100, 100)); gravity = Gravity.CENTER; setPadding(0, 8, 0, 0) })

        return root
    }
    override fun onResume() { super.onResume() }
    override fun onPause() { super.onPause(); rv.stop() }
}
