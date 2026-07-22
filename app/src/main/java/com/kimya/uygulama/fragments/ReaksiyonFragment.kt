package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.PdfExporter
import com.kimya.uygulama.utils.KimyaData
import com.kimya.uygulama.utils.ReactionBalancer
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.kimya.uygulama.viewmodel.KimyaViewModel

class ReactionSchemeView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    var reactant1 = ""; var reactant2 = ""
    var product1 = ""; var product2 = ""
    var condition = ""; var reactionType = ""

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB388FF.toInt(); style = Paint.Style.FILL
    }
    private val circlePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0080.toInt(); style = Paint.Style.FILL
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 30; color = 0xFF00F0FF.toInt() }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8B949E.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 34f; textAlign = Paint.Align.CENTER
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE
    }
    private val arrowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt(); style = Paint.Style.FILL
    }
    private val condPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF39FF14.toInt(); textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFA500.toInt(); textSize = 18f; textAlign = Paint.Align.CENTER
    }

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
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f

        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        if (reactant1.isEmpty() && product1.isEmpty()) {
            canvas.drawText("Reaktif ve urun girip\nDengele'ye basin", cx, cy, typePaint)
            return
        }

        val hasR2 = reactant2.isNotEmpty(); val hasP2 = product2.isNotEmpty()
        val leftX = w * 0.18f; val rightX = w * 0.82f
        val circleR = minOf(w * 0.08f, h * 0.25f, 45f)
        val gap = circleR * 2.2f
        val arrowL = cx - 35f; val arrowR = cx + 35f

        val gradient = RadialGradient(leftX, cy, circleR + 20f,
            intArrayOf(0x33B388FF.toInt(), 0x00B388FF.toInt()), null, Shader.TileMode.CLAMP)
        glowPaint.shader = gradient
        canvas.drawCircle(leftX, cy, circleR + 20f, glowPaint)

        if (hasR2) {
            val r1y = cy - gap / 2f; val r2y = cy + gap / 2f
            canvas.drawCircle(leftX, r1y, circleR, circlePaint)
            canvas.drawText(reactant1, leftX, r1y + circleR + textPaint.textSize + 4f, textPaint)
            canvas.drawCircle(leftX, r2y, circleR, circlePaint2)
            canvas.drawText(reactant2, leftX, r2y + circleR + textPaint.textSize + 4f, textPaint)
            canvas.drawLine(leftX + circleR, r1y, arrowL, cy - 15f, linePaint)
            canvas.drawLine(leftX + circleR, r2y, arrowL, cy + 15f, linePaint)
        } else {
            canvas.drawCircle(leftX, cy, circleR, circlePaint)
            canvas.drawText(reactant1, leftX, cy + circleR + textPaint.textSize + 4f, textPaint)
            canvas.drawLine(leftX + circleR, cy, arrowL, cy, linePaint)
        }

        canvas.drawLine(arrowL, cy, arrowR, cy, arrowPaint)
        val aPath = Path()
        aPath.moveTo(arrowR, cy); aPath.lineTo(arrowR - 14f, cy - 10f)
        aPath.lineTo(arrowR - 14f, cy + 10f); aPath.close()
        canvas.drawPath(aPath, arrowFill)

        if (condition.isNotEmpty()) canvas.drawText(condition, cx, cy - 22f, condPaint)
        if (reactionType.isNotEmpty()) {
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x3300F0FF.toInt(); style = Paint.Style.FILL
            }
            val tw = typePaint.measureText(reactionType) + 16f
            canvas.drawRoundRect(RectF(cx - tw / 2, 6f, cx + tw / 2, 6f + 24f), 12f, 12f, bg)
            canvas.drawText(reactionType, cx, 6f + 18f, typePaint)
        }

        if (hasP2) {
            val p1y = cy - gap / 2f; val p2y = cy + gap / 2f
            canvas.drawCircle(rightX, p1y, circleR, circlePaint)
            canvas.drawText(product1, rightX, p1y + circleR + textPaint.textSize + 4f, textPaint)
            canvas.drawCircle(rightX, p2y, circleR, circlePaint2)
            canvas.drawText(product2, rightX, p2y + circleR + textPaint.textSize + 4f, textPaint)
            canvas.drawLine(arrowR, cy - 15f, rightX - circleR, p1y, linePaint)
            canvas.drawLine(arrowR, cy + 15f, rightX - circleR, p2y, linePaint)
        } else {
            canvas.drawCircle(rightX, cy, circleR, circlePaint)
            canvas.drawText(product1, rightX, cy + circleR + textPaint.textSize + 4f, textPaint)
            canvas.drawLine(arrowR, cy, rightX - circleR, cy, linePaint)
        }
        canvas.restore()
    }
}

class YieldBarView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    var yieldPercent: Double? = null

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2D2D2D.toInt(); style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE6EDF3.toInt(); textSize = 34f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textSize = 22f; textAlign = Paint.Align.CENTER }

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
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val barH = h * 0.42f; val top = (h - barH) / 2f - 8f; val left = 20f; val right = w - 20f

        val bgRect = RectF(left, top, right, top + barH)
        canvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val pct = yieldPercent?.coerceIn(0.0, 100.0) ?: 0.0
        val fillRight = left + (right - left) * (pct / 100.0).toFloat()

        val gradient = LinearGradient(left, 0f, right, 0f,
            intArrayOf(0xFFFF3333.toInt(), 0xFFFFA500.toInt(), 0xFFFFFF00.toInt(), 0xFF39FF14.toInt()),
            floatArrayOf(0f, 0.33f, 0.66f, 1f), Shader.TileMode.CLAMP)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        fillPaint.shader = gradient
        val fillRect = RectF(left, top, fillRight, top + barH)
        canvas.drawRoundRect(fillRect, 10f, 10f, fillPaint)

        val pctText = if (yieldPercent != null) "${"%.1f".format(pct)}%" else "Verim Bekleniyor..."
        canvas.drawText(pctText, w / 2f, top + barH / 2f + textPaint.textSize / 3f, textPaint)

        val labelY = top + barH + 28f
        canvas.drawText("0%", left, labelY, labelPaint)
        canvas.drawText("50%", w / 2f, labelY, labelPaint)
        canvas.drawText("100%", right, labelY, labelPaint)
        canvas.restore()
    }
}

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

        val schemePlaceholder = v.findViewById<View>(R.id.rea_scheme_placeholder)
        val schemeParent = schemePlaceholder.parent as ViewGroup
        val schemeIdx = schemeParent.indexOfChild(schemePlaceholder)
        schemeParent.removeView(schemePlaceholder)
        val schemeView = ReactionSchemeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (160 * resources.displayMetrics.density).toInt())
        }
        schemeParent.addView(schemeView, schemeIdx)

        val yieldPlaceholder = v.findViewById<View>(R.id.rea_yield_placeholder)
        val yieldParent = yieldPlaceholder.parent as ViewGroup
        val yieldIdx = yieldParent.indexOfChild(yieldPlaceholder)
        yieldParent.removeView(yieldPlaceholder)
        val yieldView = YieldBarView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (80 * resources.displayMetrics.density).toInt())
        }
        yieldParent.addView(yieldView, yieldIdx)

        v.findViewById<Button>(R.id.verim_hesapla).setOnClickListener {
            try {
                val t = verimT.text.toString().toDoubleOrNull()
                val g = verimG.text.toString().toDoubleOrNull()
                val y = verimY.text.toString().toDoubleOrNull()
                val sb = StringBuilder()
                when {
                    t != null && g != null && t > 0 -> {
                        val yuzde = g / t * 100
                        sb.append("Verim = ${"%.2f".format(yuzde)}%\n")
                        sb.append(when { yuzde >= 95 -> "Mukemmel"; yuzde >= 80 -> "Iyi"; yuzde >= 60 -> "Orta"; else -> "Dusuk" })
                        yieldView.yieldPercent = yuzde; yieldView.invalidate()
                    }
                    t != null && y != null && t > 0 -> {
                        val gercek = t * y / 100
                        sb.append("Gercek Verim = ${"%.4f".format(gercek)} g")
                        yieldView.yieldPercent = y; yieldView.invalidate()
                    }
                    g != null && y != null && y > 0 -> {
                        val teorik = g / (y / 100)
                        sb.append("Teorik Verim = ${"%.4f".format(teorik)} g")
                        yieldView.yieldPercent = y; yieldView.invalidate()
                    }
                    else -> sb.append("En az 2 deger girin")
                }
                verimSonuc.text = sb.toString()
            } catch (e: Exception) { verimSonuc.text = "Hata: ${e.message}" }
        }

        fun reaksiyonYap() {
            val r = reaktif.text.toString().trim().replace(" ", "")
            val u = urun.text.toString().trim().replace(" ", "")
            if (r.isEmpty() || u.isEmpty()) { sonuc.text = "Reaktif ve urun girin"; return }

            val dengeli = ReactionBalancer.dene("$r->$u")
            val dengeliStr = if (dengeli != null) {
                ReactionBalancer.formatReaction(dengeli)
            } else {
                KimyaData.reaksiyonDengele(r, u)
            }
            val tip = when {
                "O2" in r -> "Yanma"
                "Cl2" in r || "F2" in r -> "Sentez"
                "H2" in r && "O2" in r -> "Su Olusumu"
                "OH" in r || "H" in r && "H2O" in u -> "Notrlesme"
                else -> "Genel"
            }
            val enerji = if ("O2" in r || "Cl2" in r) "Ekzotermik" else "Sarta Bagli"
            sonDengeli = "$dengeliStr\nTip: $tip | Enerji: $enerji"
            sonuc.text = sonDengeli
            vm.addHistory("Reaksiyon Dengele", "$r -> $u")

            val reaktifler = r.split("+").filter { it.isNotBlank() }
            val urunler = u.split("+").filter { it.isNotBlank() }
            schemeView.reactant1 = reaktifler.getOrElse(0) { "" }
            schemeView.reactant2 = reaktifler.getOrElse(1) { "" }
            schemeView.product1 = urunler.getOrElse(0) { "" }
            schemeView.product2 = urunler.getOrElse(1) { "" }
            schemeView.condition = if ("O2" in r || "Cl2" in r) "Isi" else ""
            schemeView.reactionType = tip
            schemeView.invalidate()
        }

        v.findViewById<Button>(R.id.rea_dengele).setOnClickListener { reaksiyonYap() }
        v.findViewById<Button>(R.id.rea_paylas).setOnClickListener {
            if (sonDengeli.isEmpty()) { Toast.makeText(context, "Once reaksiyon dengeleyin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PdfExporter.shareText(requireContext(), "Reaksiyon", sonDengeli)
        }

        fun hazirReaksiyon(r: String, u: String) { reaktif.setText(r); urun.setText(u); reaksiyonYap() }
        v.findViewById<Button>(R.id.rea_su).setOnClickListener { hazirReaksiyon("H2+O2", "H2O") }
        v.findViewById<Button>(R.id.rea_tuz).setOnClickListener { hazirReaksiyon("Na+Cl2", "NaCl") }
        v.findViewById<Button>(R.id.rea_amonyak).setOnClickListener { hazirReaksiyon("N2+H2", "NH3") }
        v.findViewById<Button>(R.id.rea_hcl).setOnClickListener { hazirReaksiyon("H2+Cl2", "HCl") }
        v.findViewById<Button>(R.id.rea_kirec).setOnClickListener { hazirReaksiyon("Ca+O2", "CaO") }
        return v
    }
}
