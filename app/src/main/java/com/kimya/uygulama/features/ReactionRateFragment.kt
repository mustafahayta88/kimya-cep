package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class ReactionRateView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null) : View(context, attrs) {
    private var temp = 25f; private var concA = 1f; private var concB = 1f
    private var time = 0f; private var running = false
    private val points = mutableListOf<Pair<Float, Float>>()
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); strokeWidth = 1f }
    private val rateP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val arrP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val flaskStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f; isAntiAlias = true }
    private val flaskFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
    private val graphBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A1A2E.toInt(); style = Paint.Style.FILL }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val runnable = object : Runnable { override fun run() {
        if (!running) return@run
        time += 0.1f
        val k = 0.05f * exp((temp - 25f) * 0.03f)
        val rate = k * concA * concB
        points.add(Pair(time, rate))
        if (time >= 20f) { running = false; invalidate(); return@run }
        invalidate(); handler.postDelayed(this, 50)
    }}

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    fun setTemp(v: Int) { temp = v.toFloat(); invalidate() }
    fun setConcA(v: Int) { concA = v / 100f; invalidate() }
    fun setConcB(v: Int) { concB = v / 100f; invalidate() }
    fun start() { if (!running) { running = true; points.clear(); time = 0f; handler.post(runnable) } }
    fun stop() { running = false }
    fun reset() { running = false; points.clear(); time = 0f; invalidate() }

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        textP.textSize = h * 0.022f; valP.textSize = h * 0.035f

        val k = 0.05f * exp((temp - 25f) * 0.03f)
        val initialRate = k * concA * concB
        val maxRate = maxOf(initialRate * 1.5f, 0.001f)

        // Graph area
        val gLeft = w * 0.1f; val gRight = w * 0.9f; val gTop = h * 0.04f; val gBot = h * 0.42f
        val gw = gRight - gLeft; val gh = gBot - gTop
        canvas.drawRoundRect(gLeft, gTop, gRight, gBot, 6f, 6f, graphBg)
        canvas.drawRoundRect(gLeft, gTop, gRight, gBot, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f })

        // Y-axis labels
        textP.textSize = h * 0.02f
        for (i in 0..4) {
            val x = gLeft + gw * i / 4f; canvas.drawLine(x, gBot, x, gBot + 4f, gridP); canvas.drawText("${i * 5}", x - 8f, gBot + 16f, textP)
        }
        canvas.drawText("Zaman (s)", gLeft + gw / 2f, gBot + 32f, textP)
        canvas.save(); canvas.rotate(-90f, gLeft - 22f, gTop + gh / 2f); canvas.drawText("Hiz (mol/L.s)", gLeft - 22f, gTop + gh / 2f, textP); canvas.restore()

        // Horizontal grid lines
        for (i in 1..3) {
            val y = gBot - gh * i / 4f; canvas.drawLine(gLeft, y, gLeft + 4f, y, gridP)
        }

        // Rate curve (clamped to graph bounds)
        if (points.isNotEmpty()) {
            val path = Path()
            for ((i, pt) in points.withIndex()) {
                val px = gLeft + gw * pt.first / 20f
                val py = gBot - gh * (pt.second / maxRate).coerceIn(0f, 1f)
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, rateP)

            valP.textSize = h * 0.03f
            canvas.drawText("Hiz = ${"%.4f".format(initialRate)} mol/L.s", w / 2f, h * 0.47f, valP)
            textP.textSize = h * 0.02f
            canvas.drawText("k = ${"%.4f".format(k)} | T = ${temp.toInt()}°C | [A]: ${"%.2f".format(concA)} M | [B]: ${"%.2f".format(concB)} M", w / 2f, h * 0.51f, textP)
        }

        // Reaction vessel pair
        val boxSize = h * 0.13f; val boxTop = h * 0.56f
        val boxGap = w * 0.06f; val totalW = boxSize * 2 + boxGap; val startX = (w - totalW) / 2f

        // Flask for A
        canvas.drawRoundRect(startX, boxTop, startX + boxSize, boxTop + boxSize, 4f, 4f, flaskStroke)
        flaskFill.color = 0x4400F0FF.toInt()
        val aFill = concA * boxSize * 0.7f
        canvas.drawRect(startX + 4f, boxTop + boxSize - aFill, startX + boxSize - 4f, boxTop + boxSize - 4f, flaskFill)
        textP.textSize = boxSize * 0.14f
        canvas.drawText("A: ${"%.2f".format(concA)} M", startX + boxSize / 2f, boxTop + boxSize + 20f, textP)

        // + symbol
        valP.textSize = boxSize * 0.5f; valP.color = 0xFF00F0FF.toInt()
        canvas.drawText("+", startX + boxSize + boxGap / 2f, boxTop + boxSize * 0.7f, valP)

        // Flask for B
        val bX = startX + boxSize + boxGap
        canvas.drawRoundRect(bX, boxTop, bX + boxSize, boxTop + boxSize, 4f, 4f, flaskStroke)
        flaskFill.color = 0x4439FF14.toInt()
        val bFill = concB * boxSize * 0.7f
        canvas.drawRect(bX + 4f, boxTop + boxSize - bFill, bX + boxSize - 4f, boxTop + boxSize - 4f, flaskFill)
        canvas.drawText("B: ${"%.2f".format(concB)} M", bX + boxSize / 2f, boxTop + boxSize + 20f, textP)

        // Arrow and product flask
        val prodX = startX + boxSize * 2 + boxGap * 2
        valP.textSize = boxSize * 0.3f; valP.color = 0xFF39FF14.toInt()
        canvas.drawText("->", (startX + totalW) / 2f, boxTop + boxSize * 0.5f, valP)
        valP.color = 0xFFFFA500.toInt()

        // Arrhenius plot (corner)
        val arrSize = boxSize * 0.7f; val arrLeft = w * 0.05f; val arrTop = boxTop
        canvas.drawRoundRect(arrLeft, arrTop, arrLeft + arrSize, arrTop + arrSize, 4f, 4f, graphBg)
        canvas.drawRoundRect(arrLeft, arrTop, arrLeft + arrSize, arrTop + arrSize, 4f, 4f, flaskStroke)
        textP.textSize = arrSize * 0.12f
        canvas.drawText("ln(k) vs 1/T", arrLeft + arrSize / 2f, arrTop + arrSize * 0.15f, textP)
        // Draw simplified Arrhenius
        val arrPath = Path()
        for (i in 0..40) {
            val tVal = 10f + i * 80f / 40f
            val invT = 1f / (tVal + 273f)
            val lnk = ln(0.05f) - (30000f / 8.314f) * (invT - 1f / 298f)
            val px = arrLeft + arrSize * 0.8f * (i / 40f)
            val py = arrTop + arrSize * 0.75f * (1f - (lnk + 8f) / 10f)
            if (i == 0) arrPath.moveTo(px, py) else arrPath.lineTo(px.coerceIn(arrLeft, arrLeft + arrSize), py.coerceIn(arrTop, arrTop + arrSize))
        }
        canvas.drawPath(arrPath, arrP)

        // Energy bar on right
        val barSize = boxSize * 0.7f; val barLeft = w * 0.88f; val barTop2 = boxTop
        canvas.drawRoundRect(barLeft, barTop2, barLeft + w * 0.07f, barTop2 + barSize, 4f, 4f, graphBg)
        canvas.drawRoundRect(barLeft, barTop2, barLeft + w * 0.07f, barTop2 + barSize, 4f, 4f, flaskStroke)
        val enerFrac = (temp / 100f).coerceIn(0f, 1f)
        val barFillColor = Color.HSVToColor(floatArrayOf((1f - enerFrac) * 240f, 0.8f, 0.6f))
        canvas.drawRect(barLeft + 3f, barTop2 + barSize - enerFrac * barSize, barLeft + w * 0.07f - 3f, barTop2 + barSize - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barFillColor; style = Paint.Style.FILL })
        textP.textSize = w * 0.02f
        canvas.drawText("${temp.toInt()}°C", barLeft + w * 0.035f, barTop2 + barSize + 16f, textP)

        canvas.restore()
    }
}

class ReactionRateFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_reaction_rate, container, false)
        val view = v.findViewById<ReactionRateView>(R.id.rate_canvas)
        v.findViewById<SeekBar>(R.id.temp_seek).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { view.setTemp(p); v.findViewById<TextView>(R.id.temp_label).text = "Sicaklik: ${p}°C" }
            override fun onStartTrackingTouch(sb: SeekBar) {} override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        v.findViewById<SeekBar>(R.id.concA_seek).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { view.setConcA(p); v.findViewById<TextView>(R.id.concA_label).text = "[A]: ${"%.1f".format(p / 100f)} M" }
            override fun onStartTrackingTouch(sb: SeekBar) {} override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        v.findViewById<SeekBar>(R.id.concB_seek).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) { view.setConcB(p); v.findViewById<TextView>(R.id.concB_label).text = "[B]: ${"%.1f".format(p / 100f)} M" }
            override fun onStartTrackingTouch(sb: SeekBar) {} override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        v.findViewById<Button>(R.id.btn_start).setOnClickListener { view.start() }
        v.findViewById<Button>(R.id.btn_stop).setOnClickListener { view.stop() }
        v.findViewById<Button>(R.id.btn_reset).setOnClickListener { view.reset() }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reaksiyon Hizi")
                .setMessage("Reaksiyon hizi, kimyasal bir reaksiyonun ne kadar hizli gerceklestigini olcer.\n\n" +
                    "Bu bolumde:\n" +
                    "- Sicaklik, konsantrasyon ve katalizor etkisini inceleyebilirsiniz\n" +
                    "- Seeker'lari hareket ettirerek parametreleri degistirin\n" +
                    "- Grafige bakarak hizi gozlemleyin\n" +
                    "- Baslat/Durdur/Sifirla dugmeleriyle deney yapabilirsiniz\n\n" +
                    "Arrhenius denklemi ve aktivasyon enerjisi hakkinda bilgi alabilirsiniz.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }
}
