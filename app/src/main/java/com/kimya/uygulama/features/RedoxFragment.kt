package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.abs
import kotlin.math.min
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData
import com.kimya.uygulama.viewmodel.KimyaViewModel

class RedoxArrowView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }
    var element1 = ""; var element2 = ""
    var en1 = 0.0; var en2 = 0.0; var bagTuru = ""

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val glowPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 30; color = 0xFFB388FF.toInt(); style = Paint.Style.FILL }
    private val glowPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 30; color = 0xFFFF0080.toInt(); style = Paint.Style.FILL }
    private val atomPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB388FF.toInt(); style = Paint.Style.FILL }
    private val atomPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(14f, 7f), 0f) }
    private val arrowHead = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6EDF3.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val infoP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCCCC.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val barBgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E2A3A.toInt(); style = Paint.Style.FILL }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val enLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val oxP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.FILL }

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
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        val cx = w / 2f; val cy = h * 0.32f

        if (element1.isEmpty() || element2.isEmpty()) {
            subP.textSize = h * 0.06f; canvas.drawText("Iki element girerek Redoks analizi yapin", cx, h * 0.5f, subP)
            return
        }

        val c = canvas; val r = min(w * 0.12f, 72f)
        val leftX = w * 0.22f; val rightX = w * 0.78f
        textP.textSize = h * 0.08f; subP.textSize = h * 0.05f; infoP.textSize = h * 0.045f
        enLabel.textSize = h * 0.045f; labelP.textSize = h * 0.04f; oxP.textSize = h * 0.06f

        c.drawCircle(leftX, cy, r + 24f, glowPaint1); c.drawCircle(leftX, cy, r, atomPaint1); c.drawCircle(leftX, cy, r, borderPaint)
        c.drawCircle(rightX, cy, r + 24f, glowPaint2); c.drawCircle(rightX, cy, r, atomPaint2); c.drawCircle(rightX, cy, r, borderPaint)

        c.drawText(element1, leftX, cy + r + textP.textSize + 4f, textP)
        c.drawText(element2, rightX, cy + r + textP.textSize + 4f, textP)

        if (en1 > 0 && en2 > 0) {
            val arrowY = cy - r - 36f
            if (en1 < en2) {
                c.drawLine(leftX + r + 5f, cy, cx, arrowY, arrowPaint)
                c.drawLine(cx, arrowY, rightX - r - 5f, cy, arrowPaint)
                val path = Path(); path.moveTo(rightX - r - 5f, cy); path.lineTo(rightX - r - 5f - 18f, cy - 10f); path.lineTo(rightX - r - 5f - 18f, cy + 10f); path.close()
                c.drawPath(path, arrowHead)
                c.drawText("e⁻ akisi (yukseltgenme)", cx, arrowY - 10f, infoP)
                c.drawText("(en dusukten yuksege)", cx, arrowY + infoP.textSize + 2f, subP)
            } else {
                c.drawLine(rightX - r - 5f, cy, cx, arrowY, arrowPaint)
                c.drawLine(cx, arrowY, leftX + r + 5f, cy, arrowPaint)
                val path = Path(); path.moveTo(leftX + r + 5f, cy); path.lineTo(leftX + r + 5f + 18f, cy - 10f); path.lineTo(leftX + r + 5f + 18f, cy + 10f); path.close()
                c.drawPath(path, arrowHead)
                c.drawText("e⁻ akisi (yukseltgenme)", cx, arrowY - 10f, infoP)
                c.drawText("(en dusukten yuksege)", cx, arrowY + infoP.textSize + 2f, subP)
            }

            val ay2 = cy + r + textP.textSize + 16f
            val yu = if (en1 < en2) element1 else element2
            val ind = if (en1 > en2) element1 else element2
            oxP.textSize = h * 0.055f
            c.drawText("Yukseltgenen: ${yu} (e⁻ verir)", leftX, ay2, oxP)
            c.drawText("Indirgenen: ${ind} (e⁻ alir)", rightX, ay2, oxP)

            val barY = h * 0.72f; val barH = 24f
            val barL = w * 0.08f; val barR = w * 0.92f; val barW = barR - barL
            c.drawRoundRect(RectF(barL, barY, barR, barY + barH), 12f, 12f, barBgP)
            val barFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(barL, 0f, barR, 0f, intArrayOf(0xFF39FF14.toInt(), 0xFF00F0FF.toInt(), 0xFFFF0080.toInt(), 0xFFFF3333.toInt()), null, Shader.TileMode.CLAMP)
                style = Paint.Style.FILL
            }
            c.drawRoundRect(RectF(barL, barY, barR, barY + barH), 12f, 12f, barFill)

            val maxEn = 4.0; val p1 = (en1 / maxEn).toFloat(); val p2 = (en2 / maxEn).toFloat()
            val m1X = barL + barW * p1; val m2X = barL + barW * p2
            val markP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
            c.drawCircle(m1X, barY + barH / 2, 10f, markP)
            c.drawText("${element1} ${"%.1f".format(en1)}", m1X, barY - 8f, enLabel)
            c.drawCircle(m2X, barY + barH / 2, 10f, markP)
            c.drawText("${element2} ${"%.1f".format(en2)}", m2X, barY - 8f, enLabel)

            c.drawText("Dusuk EN (e⁻ verir)", barL, barY + barH + 28f, labelP)
            c.drawText("Yuksek EN (e⁻ alir)", barR, barY + barH + 28f, labelP)

            val fark = abs(en1 - en2)
            c.drawText("EN Farki: ${"%.2f".format(fark)} | ${bagTuru}", cx, barY + barH + 52f, infoP)
        }
        canvas.restore()
    }
}

class RedoxFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private val presets = listOf(
        Pair("Na", "Cl"), Pair("H", "O"), Pair("C", "O"), Pair("H", "F"),
        Pair("K", "Br"), Pair("Li", "F"), Pair("N", "O"), Pair("Mg", "O"),
        Pair("S", "O"), Pair("Ca", "F"), Pair("Si", "O"), Pair("Al", "O")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_redox, container, false)
        val r1 = v.findViewById<EditText>(R.id.redox_r1)
        val r2 = v.findViewById<EditText>(R.id.redox_r2)
        val sonuc = v.findViewById<TextView>(R.id.redox_sonuc)
        val yuksTV = v.findViewById<TextView>(R.id.redox_yuks)
        val indTV = v.findViewById<TextView>(R.id.redox_ind)
        val detayTV = v.findViewById<TextView>(R.id.redox_detay)

        val placeholder = v.findViewById<View>(R.id.redox_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val redoxView = RedoxArrowView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (350 * resources.displayMetrics.density).toInt())
        }
        parent.addView(redoxView, idx)

        val presetRow = v.findViewById<LinearLayout>(R.id.redox_presets)
        for ((e1, e2) in presets) {
            Button(requireContext()).apply {
                text = "$e1-$e2"; textSize = 11f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.line)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    r1.setText(e1); r2.setText(e2)
                    analizEt(redoxView, sonuc, yuksTV, indTV, detayTV, r1, r2)
                }
                presetRow.addView(this)
            }
        }

        v.findViewById<Button>(R.id.redox_analiz).setOnClickListener { analizEt(redoxView, sonuc, yuksTV, indTV, detayTV, r1, r2) }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Redoks Analizi")
                .setMessage("Redoks (oksidasyon-induksiyon) reaksiyonlari elektron transferini icerir.\n\n" +
                    "Bu bolumde:\n" +
                    "- Iki element girerek redoks analizi yapabilirsiniz\n" +
                    "- Yukselme/indirgenme degerleri gosterilir\n" +
                    "- Elektron transfer yonu gorsel olarak aciklanir\n" +
                    "- Ornek reaksiyonlari hazir olarak bulabilirsiniz\n\n" +
                    "Preset dugmeleri ile hazir analizleri hizlica gorebilirsiniz.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }

    private fun analizEt(redoxView: RedoxArrowView, sonuc: TextView, yuksTV: TextView, indTV: TextView, detayTV: TextView, r1: EditText, r2: EditText) {
        val s1 = r1.text.toString().trim(); val s2 = r2.text.toString().trim()
        if (s1.isEmpty() || s2.isEmpty()) { sonuc.text = "Iki element giriniz"; return }
        val d1 = KimyaData.elementler[s1]; val d2 = KimyaData.elementler[s2]
        if (d1 == null || d2 == null) { sonuc.text = "Element bulunamadi"; return }
        val en1 = d1.elektronegatiflik; val en2 = d2.elektronegatiflik; val fark = abs(en1 - en2)
        val bagTuru = when {
            fark == 0.0 -> "Apolar Kovalent (Ayni element)"
            fark < 0.5 -> "Apolar Kovalent (EN farki < 0.5)"
            fark < 1.7 -> "Polar Kovalent (EN farki 0.5-1.7)"
            else -> "Iyonik (EN farki > 1.7, elektron devri)"
        }
        val yuksAdi = if (en1 < en2) d1.adi else d2.adi
        val indAdi = if (en1 > en2) d1.adi else d2.adi
        val yuksSem = if (en1 < en2) d1.semIol else d2.semIol
        val indSem = if (en1 > en2) d1.semIol else d2.semIol

        redoxView.element1 = d1.semIol; redoxView.element2 = d2.semIol
        redoxView.en1 = en1; redoxView.en2 = en2; redoxView.bagTuru = bagTuru; redoxView.invalidate()

        sonuc.text = """|REDOKS ANALIZI
            |${d1.adi} (${d1.semIol}, EN: ${en1}) - ${d2.adi} (${d2.semIol}, EN: ${en2})
            |EN Farki: ${"%.2f".format(fark)} | Bag Turu: ${bagTuru}
            |Yukseltgenen: ${yuksAdi} (e⁻ verir, EN dusuk)
            |Indirgenen: ${indAdi} (e⁻ alir, EN yuksek)""".trimMargin()

        val elFarki = if (en1 < en2) abs(en1 - en2) else abs(en2 - en1)
        val yuksYari = "${yuksSem} → ${yuksSem}^+ + e⁻"
        val indYari = "${indSem} + e⁻ → ${indSem}^-"
        yuksTV.text = "Yukseltgenme Yari Tepkimesi: ${yuksYari}"
        indTV.text = "Indirgenme Yari Tepkimesi: ${indYari}"

        val yukselm = if (en1 < en2) d1 else d2
        val indir = if (en1 > en2) d1 else d2
        detayTV.text = """|Detay: ${yukselm.adi} (EN ${yukselm.elektronegatiflik}) elektron verir → yukseltgenir
            |${indir.adi} (EN ${indir.elektronegatiflik}) elektron alir → indirgenir
            |Yukseltgen madde: ${indir.adi} (elektron alan)
            |Indirgen madde: ${yukselm.adi} (elektron veren)
            |Grup: ${d1.grup}/${d2.grup} | Periyot: ${d1.periyot}/${d2.periyot}""".trimMargin()

        vm.addHistory("Redox Analizi", "$s1 - $s2 | Fark: ${"%.2f".format(fark)} | $bagTuru")
    }
}
