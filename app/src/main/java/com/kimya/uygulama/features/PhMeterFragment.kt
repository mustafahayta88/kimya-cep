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

class PhMeterView(context: Context) : View(context) {

    private var currentPH = 7.0f
    private var targetPH = 7.0f
    var stirring = false
    private var stirAngle = 0f
    private var dropAnim = -1f
    private var showInfo = false
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector
    private val handler = Handler(Looper.getMainLooper())

    private val bubbles = mutableListOf<Pair<Float, Float>>()
    private val stirParticles = mutableListOf<Triple<Float, Float, Float>>()

    private val animRunnable = object : Runnable {
        override fun run() {
            if (abs(currentPH - targetPH) > 0.01f) currentPH += (targetPH - currentPH) * 0.08f
            else currentPH = targetPH
            if (stirring) {
                stirAngle = (stirAngle + 12f) % 360f
                if (Random.nextFloat() < 0.3f) stirParticles.add(Triple(0.5f + (Random.nextFloat() - 0.5f) * 0.6f, 0.5f + (Random.nextFloat() - 0.5f) * 0.4f, 0f))
            }
            val pit = bubbles.iterator()
            while (pit.hasNext()) { val b = pit.next(); val n = b.second + 0.02f; if (n > 1f) pit.remove() else bubbles[bubbles.indexOf(b)] = Pair(b.first, n) }
            val sit = stirParticles.iterator()
            while (sit.hasNext()) { val p = sit.next(); val n = p.third + 0.03f; if (n > 1f) sit.remove() else stirParticles[stirParticles.indexOf(p)] = Triple(p.first, p.second, n) }
            invalidate()
            handler.postDelayed(this, 30)
        }
    }

    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A0E17.toInt() }
    private val glassP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = 0xFF5599BB.toInt(); isAntiAlias = true }
    private val liquidP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCCCC.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF3.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val phDisplayP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 60f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
    private val phLabelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); textSize = 18f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val probeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val probeTipP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAADDFF.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val stirP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true; strokeCap = Paint.Cap.ROUND }
    private val bubbleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true }
    private val dropP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val scaleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val flakeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val infoBgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD111827.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val infoHeadP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 22f; isFakeBoldText = true; isAntiAlias = true }
    private val infoTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF3.toInt(); textSize = 20f; isAntiAlias = true }
    private val stepP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); textSize = 22f; isFakeBoldText = true; isAntiAlias = true }

    init {
        isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.5f, 3f); invalidate(); return true }
        })
        handler.post(animRunnable)
    }

    fun addAcid() { targetPH = (targetPH - 0.3f).coerceAtLeast(0f); dropAnim = 0f; if (Random.nextFloat() < 0.5f) bubbles.add(Pair(0.4f + Random.nextFloat() * 0.2f, 0f)); invalidate() }
    fun addBase() { targetPH = (targetPH + 0.3f).coerceAtMost(14f); dropAnim = 0f; if (Random.nextFloat() < 0.5f) bubbles.add(Pair(0.5f + Random.nextFloat() * 0.2f, 0f)); invalidate() }
    fun toggleStir() { stirring = !stirring; if (!stirring) stirParticles.clear(); invalidate() }
    fun reset() { currentPH = 7f; targetPH = 7f; stirring = false; stirAngle = 0f; dropAnim = -1f; bubbles.clear(); stirParticles.clear(); invalidate() }
    fun toggleInfo() { showInfo = !showInfo; panY = 0f; invalidate() }
    fun getPH() = currentPH

    private fun getPHColor(ph: Float): Int {
        return when { ph < 2 -> 0xFFFF0000.toInt(); ph < 4 -> 0xFFFF6600.toInt(); ph < 6 -> 0xFFDDAA00.toInt(); ph < 8 -> 0xFF44BB44.toInt(); ph < 10 -> 0xFF0088CC.toInt(); ph < 12 -> 0xFF3344AA.toInt(); else -> 0xFF6622CC.toInt() }
    }
    private fun getPHLabel(ph: Float): String {
        return when { ph < 1 -> "Çok Güçlü Asit"; ph < 3 -> "Güçlü Asit"; ph < 5 -> "Zayıf Asit"; ph < 6.5 -> "Hafif Asidik"; ph < 7.5 -> "Nötr"; ph < 9 -> "Hafif Bazik"; ph < 11 -> "Zayıf Baz"; ph < 13 -> "Güçlü Baz"; else -> "Çok Güçlü Baz" }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (showInfo) {
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> { lastTy = e.y; touchMode = 1; return true }
                MotionEvent.ACTION_MOVE -> { if (touchMode == 1) { panY += e.y - lastTy; panY = panY.coerceIn(-400f, 0f); lastTy = e.y; invalidate() } }
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

        canvas.drawText("pH Metre Simülasyonu", w * 0.5f, 38f, titleP)

        val phColor = getPHColor(currentPH)
        phDisplayP.color = phColor
        canvas.drawText("%.1f".format(currentPH), w * 0.5f, 100f, phDisplayP)
        canvas.drawText(getPHLabel(currentPH), w * 0.5f, 125f, phLabelP)
        canvas.drawText("pH = −log[H⁺]", w * 0.5f, 148f, phLabelP.apply { textSize = 16f; color = 0xFF666666.toInt() })
        phLabelP.textSize = 18f; phLabelP.color = 0xFF888888.toInt()

        drawPHScale(canvas, w, 160f)

        val bW = w * 0.45f; val bH = h * 0.38f; val bX = w * 0.275f; val bY = h * 0.35f
        drawBeaker(canvas, bX, bY, bW, bH)
        drawProbe(canvas, bX + bW * 0.5f, bY, bH)
        if (stirring) drawStirrer(canvas, bX + bW * 0.5f, bY, bH)
        if (dropAnim in 0f..1f) { drawDrop(canvas, bX + bW * 0.5f, bY - 30f, bY + bH * 0.3f); dropAnim += 0.05f }

        textP.textSize = 16f; textP.color = 0xFFFF6666.toInt()
        canvas.drawText("Asit Ekle", bX + bW * 0.25f, bY + bH + 30f, textP)
        textP.color = 0xFF6688FF.toInt()
        canvas.drawText("Baz Ekle", bX + bW * 0.75f, bY + bH + 30f, textP)
        textP.textSize = 22f; textP.color = 0xFFCCCCCC.toInt()

        canvas.restore()
    }

    private fun drawPHScale(canvas: Canvas, w: Float, y: Float) {
        val barX = 40f; val barW = w - 80f; val barH = 20f
        val colors = intArrayOf(0xFFFF0000.toInt(), 0xFFFF6600.toInt(), 0xFFDDAA00.toInt(), 0xFF44BB44.toInt(), 0xFF0088CC.toInt(), 0xFF3344AA.toInt(), 0xFF6622CC.toInt())
        val positions = floatArrayOf(0f, 0.17f, 0.33f, 0.5f, 0.67f, 0.83f, 1f)
        val shader = LinearGradient(barX, y, barX + barW, y, colors, positions, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(barX, y, barX + barW, y + barH, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        for (i in 0..14) { val x = barX + barW * i / 14f; scaleP.color = 0xFF444444.toInt(); canvas.drawText("$i", x, y + barH + 18f, scaleP) }
        val markerX = barX + barW * currentPH / 14f
        canvas.drawCircle(markerX, y - 4f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL })
        canvas.drawCircle(markerX, y - 4f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF000000.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f })
        canvas.drawText("%.1f".format(currentPH), markerX, y - 16f, scaleP.apply { color = 0xFFFFFFFF.toInt(); textSize = 16f; isFakeBoldText = true })
        scaleP.textSize = 14f; scaleP.isFakeBoldText = false
    }

    private fun drawBeaker(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val phColor = getPHColor(currentPH)
        liquidP.color = Color.argb(70, Color.red(phColor), Color.green(phColor), Color.blue(phColor))
        canvas.drawRoundRect(x + 4f, y + h * 0.25f, x + w - 4f, y + h - 4f, 8f, 8f, liquidP)

        val waveP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(100, Color.red(phColor), Color.green(phColor), Color.blue(phColor)); style = Paint.Style.FILL }
        val wavePath = Path()
        wavePath.moveTo(x + 4f, y + h * 0.25f)
        for (i in 0..20) { val wx = x + 4f + (w - 8f) * i / 20f; val wy = y + h * 0.25f + sin(System.currentTimeMillis() / 300f + i * 0.5f).toFloat() * 3f; wavePath.lineTo(wx, wy) }
        wavePath.lineTo(x + w - 4f, y + h); wavePath.lineTo(x + 4f, y + h); wavePath.close()
        canvas.drawPath(wavePath, waveP)

        canvas.drawRoundRect(x, y, x + w, y + h, 10f, 10f, glassP)

        textP.textSize = 16f; textP.color = 0xFFAABBCC.toInt()
        canvas.drawText("Çözelti", x + w * 0.5f, y + h * 0.6f, textP)
        textP.textSize = 22f; textP.color = 0xFFCCCCCC.toInt()

        for (bub in bubbles) { val bx = x + w * bub.first; val by = y + h * 0.7f - h * 0.4f * bub.second; bubbleP.color = Color.argb((150 * (1f - bub.second)).toInt(), 255, 255, 255); canvas.drawCircle(bx, by, 4f - bub.second * 2f, bubbleP) }
        for (p in stirParticles) { val px = x + w * p.first; val py = y + h * p.second; flakeP.color = Color.argb((180 * (1f - p.third)).toInt(), 200, 200, 200); canvas.drawCircle(px, py, 3f, flakeP) }
    }

    private fun drawProbe(canvas: Canvas, centerX: Float, beakerY: Float, beakerH: Float) {
        val probeTop = beakerY - 80f; val probeBottom = beakerY + beakerH * 0.55f
        canvas.drawLine(centerX, probeTop, centerX, probeTop - 30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); strokeWidth = 6f; style = Paint.Style.STROKE; isAntiAlias = true })
        probeP.color = 0xFF444444.toInt()
        canvas.drawRoundRect(centerX - 10f, probeTop - 30f, centerX + 10f, probeBottom, 4f, 4f, probeP)
        probeTipP.color = 0xFFAADDFF.toInt()
        canvas.drawRoundRect(centerX - 6f, probeBottom - 20f, centerX + 6f, probeBottom + 10f, 3f, 3f, probeTipP)
        val phColor = getPHColor(currentPH)
        canvas.drawCircle(centerX, probeBottom + 5f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, Color.red(phColor), Color.green(phColor), Color.blue(phColor)); style = Paint.Style.FILL })
    }

    private fun drawStirrer(canvas: Canvas, centerX: Float, beakerY: Float, beakerH: Float) {
        val stirY = beakerY + beakerH * 0.7f; val stirLen = 30f
        val rad = Math.toRadians(stirAngle.toDouble()).toFloat()
        val dx = cos(rad) * stirLen; val dy = sin(rad) * stirLen * 0.3f
        canvas.drawLine(centerX - dx, stirY - dy, centerX + dx, stirY + dy, stirP)
        canvas.drawCircle(centerX, stirY, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); style = Paint.Style.FILL })
    }

    private fun drawDrop(canvas: Canvas, x: Float, fromY: Float, toY: Float) {
        val progress = dropAnim.coerceIn(0f, 1f); val dy = fromY + (toY - fromY) * progress
        dropP.color = if (targetPH < currentPH) 0xFFFF4444.toInt() else 0xFF4488FF.toInt()
        val dropPath = Path()
        dropPath.moveTo(x, dy - 8f); dropPath.cubicTo(x - 6f, dy, x - 6f, dy + 8f, x, dy + 10f); dropPath.cubicTo(x + 6f, dy + 8f, x + 6f, dy, x, dy - 8f)
        canvas.drawPath(dropPath, dropP)
    }

    private fun drawInfoPanel(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRoundRect(16f, 16f, w - 16f, h - 16f, 20f, 20f, infoBgP)
        canvas.save(); canvas.translate(0f, panY)
        var y = 65f; val left = 36f; val cx = w / 2f
        canvas.drawText("🧪 pH Metre Rehberi", cx, y, infoHeadP.apply { textSize = 30f; color = 0xFFFFD700.toInt() }); y += 50f
        canvas.drawText("pH nedir?", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 36f
        canvas.drawText("Bir çözeltilerin asidik veya bazik", left, y, infoTextP.apply { textSize = 22f }); y += 30f
        canvas.drawText("düzeyini ölçen değerdir. 0-14", left, y, infoTextP); y += 30f
        canvas.drawText("arasında değişir.", left, y, infoTextP); y += 45f
        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f
        canvas.drawText("📐 Formül", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        canvas.drawText("pH = −log[H⁺]", left + 8f, y, infoTextP.apply { textSize = 24f; color = 0xFF39FF14.toInt() }); y += 34f
        canvas.drawText("pOH = −log[OH⁻]", left + 8f, y, infoTextP.apply { textSize = 24f; color = 0xFF39FF14.toInt() }); y += 34f
        canvas.drawText("pH + pOH = 14", left + 8f, y, infoTextP.apply { textSize = 24f; color = 0xFF39FF14.toInt() }); y += 34f
        canvas.drawText("[H⁺] × [OH⁻] = 10⁻¹⁴", left + 8f, y, infoTextP.apply { textSize = 22f; color = 0xFFAABBCC.toInt() }); y += 45f
        canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f
        canvas.drawText("🎨 pH Renkleri", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        val colorInfo = listOf("0-2  : Kırmızı → Çok Güçlü Asit", "2-4  : Turuncu → Güçlü Asit", "4-6  : Sarı → Zayıf Asit", "6-7  : Açık Yeşil → Hafif Asidik", "7    : Yeşil → Nötr (Su)", "7-8  : Açık Mavi → Hafif Bazik", "8-10 : Mavi → Zayıf Baz", "10-12: Koyu Mavi → Güçlü Baz", "12-14: Mor → Çok Güçlü Baz")
        for (ci in colorInfo) { canvas.drawText(ci, left + 8f, y, infoTextP.apply { textSize = 20f }); y += 28f }
        y += 15f; canvas.drawLine(left, y, w - 36f, y, Paint().apply { color = 0xFF333333.toInt(); strokeWidth = 1f }); y += 25f
        canvas.drawText("🌍 Gerçek Hayat", left, y, infoHeadP.apply { textSize = 26f; color = 0xFF00F0FF.toInt() }); y += 38f
        val examples = listOf("• Mide Asidi: pH ≈ 1.5-3.5", "• Limon Suyu: pH ≈ 2-3", "• Sirke: pH ≈ 2.4", "• Süt: pH ≈ 6.5-6.7", "• Dist. Su: pH ≈ 7.0", "• Kan: pH ≈ 7.35-7.45", "• Deniz Suyu: pH ≈ 8.1", "• Amonyak: pH ≈ 11-12", "• Kaynak Suyu: pH ≈ 13")
        for (ex in examples) { canvas.drawText(ex, left + 8f, y, infoTextP.apply { textSize = 20f }); y += 28f }
        y += 20f; textP.textSize = 14f; textP.color = 0xFF555555.toInt()
        canvas.drawText("Kaydırmak için sürükleyin • Kapatmak için ?", cx, y + 10f, textP)
        textP.textSize = 22f; textP.color = 0xFFCCCCCC.toInt()
        canvas.restore()
    }
}

class PhMeterFragment : Fragment() {
    private lateinit var phView: PhMeterView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0A0E17.toInt()); setPadding(24, 24, 24, 24) }

        val topBar = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 8) }
        topBar.addView(TextView(requireContext()).apply { text = "pH Metre"; setTextColor(0xFF00F0FF.toInt()); textSize = 18f; paint.isFakeBoldText = true; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) })
        topBar.addView(TextView(requireContext()).apply {
            text = "?"; setTextColor(0xFF000000.toInt()); textSize = 18f; paint.isFakeBoldText = true; setPadding(28, 8, 28, 8)
            background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(0xFFFF0080.toInt()) }
            setOnClickListener { phView.toggleInfo() }
        })
        v.addView(topBar)

        phView = PhMeterView(requireContext())
        v.addView(phView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val btnRow1 = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4); gravity = Gravity.CENTER }
        btnRow1.addView(Button(requireContext()).apply { text = "🧪 Asit Ekle (HCl)"; setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFFFF4444.toInt()); setOnClickListener { phView.addAcid() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnRow1.addView(Button(requireContext()).apply { text = "🧪 Baz Ekle (NaOH)"; setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF4488FF.toInt()); setOnClickListener { phView.addBase() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(btnRow1)

        val btnRow2 = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 0); gravity = Gravity.CENTER }
        val btnStir = Button(requireContext()).apply { text = "🌀 Karıştır"; setTextColor(0xFF000000.toInt()); setBackgroundColor(0xFF44BB44.toInt()); setOnClickListener { phView.toggleStir(); text = if (phView.stirring) "⏹ Durdur" else "🌀 Karıştır" } }
        btnRow2.addView(btnStir, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        btnRow2.addView(Button(requireContext()).apply { text = "↺ Sıfırla"; setTextColor(0xFF00F0FF.toInt()); setBackgroundColor(0xFF1A1A2E.toInt()); setPadding(24, 0, 24, 0); setOnClickListener { phView.reset(); btnStir.text = "🌀 Karıştır" } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        v.addView(btnRow2)

        return v
    }
}
