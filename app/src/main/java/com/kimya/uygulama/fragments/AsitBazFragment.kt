package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.PdfExporter
import com.kimya.uygulama.utils.KimyaData
import com.kimya.uygulama.viewmodel.KimyaViewModel

class phScaleView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    var phValue: Double? = null
    private val phColors = intArrayOf(
        0xFFFF0000.toInt(), 0xFFFF2200.toInt(), 0xFFFF4400.toInt(), 0xFFFF6600.toInt(),
        0xFFFF8800.toInt(), 0xFFFFAA00.toInt(), 0xFFFFCC00.toInt(), 0xFFEEFF00.toInt(),
        0xFFCCFF00.toInt(), 0xFFAAFF00.toInt(), 0xFF88FF00.toInt(), 0xFF44FF44.toInt(),
        0xFF00FF88.toInt(), 0xFF00FFCC.toInt(), 0xFF00EEFF.toInt(), 0xFF00AAFF.toInt(),
        0xFF0066FF.toInt(), 0xFF0044FF.toInt(), 0xFF2200FF.toInt(), 0xFF4400FF.toInt(),
        0xFF6600FF.toInt(),         0xFF8800FF.toInt()
    )

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
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
        if (w <= 0 || h <= 0) return
        val margin = 40f; val barW = w - margin * 2; val barH = 28f
        val barY = h / 2f - barH / 2f + 12f
        val segW = barW / 14f

        val bgPaint = Paint()
        bgPaint.color = Color.argb(30, 57, 255, 20)
        bgPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(margin - 2, barY - 2, margin + barW + 2, barY + barH + 2, 6f, 6f, bgPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        for (i in 0 until 14) {
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = phColors[i]
            canvas.drawRect(margin + i * segW, barY, margin + (i + 1) * segW, barY + barH, paint)
        }
        val borderPaint = Paint()
        borderPaint.color = Color.argb(80, 57, 255, 20)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1.5f
        canvas.drawRoundRect(margin - 2, barY - 2, margin + barW + 2, barY + barH + 2, 6f, 6f, borderPaint)

        val tickPaint = Paint()
        tickPaint.color = Color.argb(120, 255, 255, 255)
        tickPaint.strokeWidth = 1.5f
        val labelPaint = Paint()
        labelPaint.color = Color.argb(200, 230, 237, 243)
        labelPaint.textSize = 11f
        labelPaint.textAlign = Paint.Align.CENTER
        for (ph in 0..14) {
            val x = margin + ph * segW
            canvas.drawLine(x, barY - 5f, x, barY + barH + 5f, tickPaint)
            canvas.drawText("$ph", x, barY + barH + 18f, labelPaint)
        }

        val lblY = barY + barH + 34f
        val asitPaint = Paint()
        asitPaint.color = 0xFFFF4444.toInt(); asitPaint.textSize = 14f; asitPaint.textAlign = Paint.Align.LEFT; asitPaint.isFakeBoldText = true
        canvas.drawText("ASIT", margin, lblY, asitPaint)
        val notrPaint = Paint()
        notrPaint.color = 0xFF44FF44.toInt(); notrPaint.textSize = 14f; notrPaint.textAlign = Paint.Align.CENTER; notrPaint.isFakeBoldText = true
        canvas.drawText("NOTR", margin + barW / 2, lblY, notrPaint)
        val bazPaint = Paint()
        bazPaint.color = 0xFF4488FF.toInt(); bazPaint.textSize = 14f; bazPaint.textAlign = Paint.Align.RIGHT; bazPaint.isFakeBoldText = true
        canvas.drawText("BAZ", margin + barW, lblY, bazPaint)

        val ph = phValue
        if (ph != null && ph in 0.0..14.0) {
            val x = margin + (ph / 14.0 * barW).toFloat()
            val color = phColors[ph.toInt().coerceIn(0, 13)]

            val glowPaint = Paint()
            glowPaint.color = Color.argb(50, 57, 255, 20)
            glowPaint.strokeWidth = 14f
            glowPaint.isAntiAlias = true
            canvas.drawLine(x, barY - 2f, x, barY + barH + 2f, glowPaint)

            val linePaint = Paint()
            linePaint.color = 0xFF39FF14.toInt(); linePaint.strokeWidth = 3f; linePaint.isAntiAlias = true
            canvas.drawLine(x, barY - 3f, x, barY + barH + 3f, linePaint)

            val arrowPaint = Paint()
            arrowPaint.color = 0xFF39FF14.toInt(); arrowPaint.style = Paint.Style.FILL; arrowPaint.isAntiAlias = true
            val path = Path()
            val aH = 22f; val aW = 13f
            path.moveTo(x - aW, barY - 4f); path.lineTo(x + aW, barY - 4f); path.lineTo(x, barY - aH); path.close()
            canvas.drawPath(path, arrowPaint)

            val infoPaint = Paint()
            infoPaint.isFakeBoldText = true; infoPaint.textAlign = Paint.Align.CENTER
            infoPaint.color = color; infoPaint.textSize = 24f
            canvas.drawText("pH = ${"%.2f".format(ph)}", w / 2f, barY - 32f, infoPaint)

            infoPaint.textSize = 16f; infoPaint.color = Color.argb(230, 230, 237, 243)
            val tip = when { ph < 4 -> "Kuvvetli Asit"; ph < 7 -> "Zayif Asit"; ph == 7.0 -> "Notr"; ph < 10 -> "Zayif Baz"; else -> "Kuvvetli Baz" }
            canvas.drawText("($tip)", w / 2f, barY - 52f, infoPaint)
        } else {
            val infoPaint = Paint()
            infoPaint.color = Color.argb(150, 230, 237, 243); infoPaint.textSize = 14f; infoPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Bir deger girip Analiz'e basin", w / 2f, barY - 28f, infoPaint)
        }
        canvas.restore()
    }
}

class phMetreView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    var phValue: Double? = null
    private val segmentColors = intArrayOf(
        0xFFFF0000.toInt(), 0xFFFF2200.toInt(), 0xFFFF4400.toInt(), 0xFFFF6600.toInt(),
        0xFFFF8800.toInt(), 0xFFFFAA00.toInt(), 0xFFFFCC00.toInt(), 0xFF88CC00.toInt(),
        0xFF44CC44.toInt(), 0xFF00CC88.toInt(), 0xFF00AACC.toInt(), 0xFF0088CC.toInt(),
        0xFF0066CC.toInt(), 0xFF4444CC.toInt()
    )

    private fun phColor(pH: Float): Int {
        val idx = (pH / 1f).toInt().coerceIn(0, 13)
        return segmentColors[idx]
    }

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
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
        if (w <= 0 || h <= 0) return
        val ph = phValue ?: return
        val pH = ph.toFloat().coerceIn(0f, 14f)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val leftMargin = 10f; val rightMargin = 10f; val topMargin = 14f; val bottomMargin = 14f
        val colW = (w - leftMargin - rightMargin) / 3f

        val meterLeft = leftMargin + 6f
        val meterW = colW - 12f
        val meterTop = topMargin + 22f
        val meterH = h - topMargin - bottomMargin - 36f
        val segH = meterH / 14f
        for (i in 0 until 14) {
            val p = Paint().apply { style = Paint.Style.FILL; color = segmentColors[i] }
            canvas.drawRect(meterLeft, meterTop + i * segH, meterLeft + meterW, meterTop + (i + 1) * segH, p)
        }
        val border = Paint().apply { style = Paint.Style.STROKE; color = Color.argb(100, 255, 255, 255); strokeWidth = 1f }
        canvas.drawRect(meterLeft, meterTop, meterLeft + meterW, meterTop + meterH, border)

        val segIdx = (pH / 1f).toInt().coerceIn(0, 13)
        val segFrac = (pH % 1f) / 1f
        val pointerY = meterTop + segIdx * segH + segFrac * segH
        val ptrX = meterLeft + meterW
        val ptrPath = Path()
        ptrPath.moveTo(ptrX, pointerY - 8f); ptrPath.lineTo(ptrX + 14f, pointerY); ptrPath.lineTo(ptrX, pointerY + 8f); ptrPath.close()
        canvas.drawPath(ptrPath, Paint().apply { style = Paint.Style.FILL; color = 0xFF39FF14.toInt(); isAntiAlias = true })

        val lbl = Paint().apply { color = Color.argb(200, 230, 237, 243); textSize = 10f; textAlign = Paint.Align.CENTER }
        canvas.drawText("0", meterLeft + meterW / 2f, meterTop + meterH + 14f, lbl)
        canvas.drawText("14", meterLeft + meterW / 2f, meterTop - 5f, lbl)
        canvas.drawText("pH", meterLeft + meterW / 2f, meterTop - 20f, lbl)

        val mtick = Paint().apply { color = Color.argb(80, 255, 255, 255); strokeWidth = 1f }
        for (i in 0..14) {
            val my = meterTop + (i / 14f) * meterH
            canvas.drawLine(meterLeft + meterW + 3f, my, meterLeft + meterW + 8f, my, mtick)
        }

        val centerX = leftMargin + colW + colW / 2f
        val textColor = phColor(pH)
        val bigText = Paint().apply {
            color = textColor; isFakeBoldText = true; textAlign = Paint.Align.CENTER
            textSize = Math.min(colW * 0.26f, 38f)
        }
        canvas.drawText("pH = ${"%.2f".format(ph)}", centerX, h / 2f + bigText.textSize / 3f, bigText)

        val tubeLeft = leftMargin + colW * 2f + 6f
        val tubeW = colW - 12f
        val tubeTop = topMargin + 4f
        val tubeH = h - topMargin - bottomMargin - 8f
        val radius = tubeW / 2f

        val tubeBody = RectF(tubeLeft, tubeTop, tubeLeft + tubeW, tubeTop + tubeH - 4f)
        val tubePath = Path()
        tubePath.addRoundRect(tubeBody, floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius), Path.Direction.CW)
        canvas.drawPath(tubePath, Paint().apply { style = Paint.Style.STROKE; color = Color.argb(180, 200, 210, 220); strokeWidth = 2.5f; isAntiAlias = true })

        val liquidH = tubeH * 0.65f
        val liquidBottom = tubeTop + tubeH - 4f
        val liquidTop = liquidBottom - liquidH
        val baseColor = textColor and 0x00FFFFFF.toInt() or 0x88000000.toInt()

        val liquidBody = RectF(tubeLeft + 2f, liquidTop + 6f, tubeLeft + tubeW - 2f, liquidBottom)
        val wavePaint = Paint().apply { style = Paint.Style.FILL; color = baseColor; isAntiAlias = true }
        val wavePath = Path()
        val waveY = liquidTop + 6f
        wavePath.moveTo(tubeLeft + 2f, liquidBottom)
        wavePath.lineTo(tubeLeft + 2f, waveY)
        val ww = tubeW - 4f
        wavePath.cubicTo(tubeLeft + 2f + ww * 0.2f, waveY - 5f, tubeLeft + 2f + ww * 0.4f, waveY + 5f, tubeLeft + 2f + ww * 0.6f, waveY - 4f)
        wavePath.cubicTo(tubeLeft + 2f + ww * 0.8f, waveY + 4f, tubeLeft + 2f + ww * 0.9f, waveY - 2f, tubeLeft + tubeW - 2f, waveY)
        wavePath.lineTo(tubeLeft + tubeW - 2f, liquidBottom)
        wavePath.close()
        canvas.drawPath(wavePath, wavePaint)

        val bubblePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val bubblePositions = listOf(
            0.25f to 0.3f, 0.55f to 0.5f, 0.75f to 0.2f,
            0.4f to 0.6f, 0.65f to 0.7f, 0.3f to 0.8f,
            0.8f to 0.4f, 0.5f to 0.85f, 0.15f to 0.5f, 0.7f to 0.55f
        )
        for ((fracX, fracY) in bubblePositions) {
            val bx = tubeLeft + 2f + fracX * (tubeW - 4f)
            val by = liquidBottom - fracY * liquidH
            val bubbleR = (1.5f + fracY * 2.5f).coerceAtMost(4.5f)
            bubblePaint.color = Color.argb((30 + (fracY * 50).toInt()).coerceAtMost(90), 255, 255, 255)
            canvas.drawCircle(bx, by, bubbleR, bubblePaint)
            bubblePaint.color = Color.argb(15, 255, 255, 255)
            canvas.drawCircle(bx - bubbleR * 0.3f, by - bubbleR * 0.3f, bubbleR * 0.4f, bubblePaint)
        }

        val (label, lblCol) = when {
            pH < 6.5f -> "ASIT" to 0xFFFF4444.toInt()
            pH > 7.5f -> "BAZ" to 0xFF4488FF.toInt()
            else -> "NOTR" to 0xFF44FF44.toInt()
        }
        val labelPaint = Paint().apply { color = lblCol; textSize = Math.min(tubeH * 0.15f, 14f); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText(label, tubeLeft + tubeW / 2f, tubeTop + tubeH / 2f + labelPaint.textSize / 3f, labelPaint)
        canvas.restore()
    }
}

class AsitBazFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private var asitindex = 0
    private var bazIndex = 0
    private var sonPhSonuc = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_asitbaz, container, false)
        val deger = v.findViewById<EditText>(R.id.ab_deger)
        val tur = v.findViewById<Spinner>(R.id.ab_tur)
        val sonuc = v.findViewById<TextView>(R.id.ab_sonuc)
        val asitList = v.findViewById<ListView>(R.id.ab_asit_list)
        val bazList = v.findViewById<ListView>(R.id.ab_baz_list)
        val katSonuc = v.findViewById<TextView>(R.id.ab_katalog_sonuc)

        val placeholder = v.findViewById<View>(R.id.ab_scale_view)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val scaleView = phScaleView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(130))
        }
        parent.addView(scaleView, idx)

        val meterPlaceholder = v.findViewById<View>(R.id.ab_meter_placeholder)
        val meterParent = meterPlaceholder.parent as ViewGroup
        val meterIdx = meterParent.indexOfChild(meterPlaceholder)
        meterParent.removeView(meterPlaceholder)
        val meterView = phMetreView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180))
        }
        meterParent.addView(meterView, meterIdx)

        tur.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            arrayOf("pH", "pOH", "[H+]", "[OH-]"))

        v.findViewById<Button>(R.id.ab_analiz).setOnClickListener {
            try {
                val d = deger.text.toString().toDoubleOrNull() ?: 0.0
                val t = tur.selectedItem.toString()
                val r = KimyaData.phHesapla(d, t)
                if (r.containsKey("hata")) { sonuc.text = r["hata"] as String; return@setOnClickListener }
                val ph = r["pH"] as Double
                scaleView.phValue = ph
                scaleView.invalidate()
                meterView.phValue = ph
                meterView.invalidate()
                val indicator = when {
                    ph < 4 -> "Metil oranj (kirmizi -> sari)"
                    ph > 8.2 -> "Fenolftalein (renksiz -> pembe)"
                    else -> "Evrensel indikator"
                }
                val guvenlik = when {
                    ph <= 2 || ph >= 12 -> "Korozif olabilir, eldiven ve gozluk kullanin"
                    else -> "Standart laboratuvar onlemleri yeterli"
                }
                sonPhSonuc = """|pH = ${"%.2f".format(ph)}
                    |pOH = ${"%.2f".format(r["pOH"] as Double)}
                    |[H+] = ${"%.6e".format(r["[H+]"] as Double)}
                    |[OH-] = ${"%.6e".format(r["[OH-]"] as Double)}
                    |Tur: ${r["tur"]}
                    |Indikator: $indicator
                    |Guvenlik: $guvenlik""".trimMargin()
                sonuc.text = sonPhSonuc
                vm.addHistory("pH Analizi", "pH=${"%.2f".format(ph)} (${r["tur"]})")
            } catch (e: Exception) { sonuc.text = "Hata: ${e.message}" }
        }

        v.findViewById<Button>(R.id.ab_asit).setOnClickListener { deger.setText("1"); tur.setSelection(0) }
        v.findViewById<Button>(R.id.ab_notr).setOnClickListener { deger.setText("7"); tur.setSelection(0) }
        v.findViewById<Button>(R.id.ab_baz).setOnClickListener { deger.setText("13"); tur.setSelection(0) }
        v.findViewById<Button>(R.id.ab_preset_zayif_asit).setOnClickListener { deger.setText("5"); tur.setSelection(0) }
        v.findViewById<Button>(R.id.ab_preset_zayif_baz).setOnClickListener { deger.setText("9"); tur.setSelection(0) }

        v.findViewById<Button>(R.id.ab_notrlesme).setOnClickListener {
            val asit = KimyaData.asitler[asitindex]
            val baz = KimyaData.bazlar[bazIndex]
            val asitKuvvetli = asit.tur.contains("Kuvvetli")
            val bazKuvvetli = baz.tur.contains("Kuvvetli")
            val tahminiPh = when {
                asitKuvvetli && bazKuvvetli -> "~7 (Notr)"
                asitKuvvetli && !bazKuvvetli -> ">7 (zayif bazik)"
                !asitKuvvetli && bazKuvvetli -> "<7 (zayif asidik)"
                else -> "~7 (zayif asit + zayif baz)"
            }
            katSonuc.text = """|Notrlesme Reaksiyonu:
                |${asit.formulu} + ${baz.formulu} -> Tuz + Su
                |${asit.adi} + ${baz.adi}
                |Tahmini pH: $tahminiPh""".trimMargin()
        }

        asitList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
            KimyaData.asitler.map { "${it.adi} (${it.formulu})" })
        bazList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
            KimyaData.bazlar.map { "${it.adi} (${it.formulu})" })

        asitList.setOnItemClickListener { _, _, pos, _ -> asitindex = pos }
        bazList.setOnItemClickListener { _, _, pos, _ -> bazIndex = pos }

        v.findViewById<Button>(R.id.ab_paylas).setOnClickListener {
            if (sonPhSonuc.isEmpty()) { Toast.makeText(context, "Once analiz yapin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PdfExporter.shareText(requireContext(), "pH Analizi", sonPhSonuc)
        }

        v.findViewById<Button>(R.id.ab_asit_detay).setOnClickListener {
            val a = KimyaData.asitler[asitindex]
            katSonuc.text = """|Asit: ${a.adi} (${a.formulu})
                |Tur: ${a.tur} | pH: ${a.pH}
                |Kullanim: ${a.kullanim}
                |Guvenlik: ${a.guvenlik}""".trimMargin()
        }

        v.findViewById<Button>(R.id.ab_baz_detay).setOnClickListener {
            val b = KimyaData.bazlar[bazIndex]
            katSonuc.text = """|Baz: ${b.adi} (${b.formulu})
                |Tur: ${b.tur} | pH: ${b.pH}
                |Kullanim: ${b.kullanim}
                |Guvenlik: ${b.guvenlik}""".trimMargin()
        }
        return v
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
}
