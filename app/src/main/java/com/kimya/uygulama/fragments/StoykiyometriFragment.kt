package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.Calculator

class StoichiometryView(context: Context) : View(context) {
    var mol1: String? = null
    var mol2: String? = null
    var reaksiyon: String? = null

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

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

        val bg = Paint().apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, w, h, bg)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        val boxW = w * 0.28f; val boxH = h * 0.32f
        val box1Left = w * 0.08f; val boxTop = h * 0.10f
        val box2Left = w * 0.64f
        val boxCenterY = boxTop + boxH / 2f

        val boxPaint = Paint().apply { color = 0xFF161B22.toInt(); style = Paint.Style.FILL }
        val border1Paint = Paint().apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f }
        val border2Paint = Paint().apply { color = 0xFF39FF14.toInt(); style = Paint.Style.STROKE; strokeWidth = 2.5f }
        val textPaint = Paint().apply { color = 0xFFE6EDF3.toInt(); textSize = 18f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }

        canvas.drawRoundRect(box1Left, boxTop, box1Left + boxW, boxTop + boxH, 10f, 10f, boxPaint)
        canvas.drawRoundRect(box1Left, boxTop, box1Left + boxW, boxTop + boxH, 10f, 10f, border1Paint)
        val label1 = mol1 ?: "A"
        canvas.drawText(label1, box1Left + boxW / 2f, boxCenterY + 6f, textPaint)

        canvas.drawRoundRect(box2Left, boxTop, box2Left + boxW, boxTop + boxH, 10f, 10f, boxPaint)
        canvas.drawRoundRect(box2Left, boxTop, box2Left + boxW, boxTop + boxH, 10f, 10f, border2Paint)
        val label2 = mol2 ?: "B"
        canvas.drawText(label2, box2Left + boxW / 2f, boxCenterY + 6f, textPaint)

        // Arrow
        val arrowY = boxCenterY
        val arrowLeft = box1Left + boxW + 4f
        val arrowRight = box2Left - 4f
        val arrowPaint = Paint().apply { color = 0xFFB388FF.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
        canvas.drawLine(arrowLeft, arrowY, arrowRight, arrowY, arrowPaint)

        val arrowPath = Path().apply {
            moveTo(arrowRight, arrowY)
            lineTo(arrowRight - 14f, arrowY - 7f)
            moveTo(arrowRight, arrowY)
            lineTo(arrowRight - 14f, arrowY + 7f)
        }
        canvas.drawPath(arrowPath, arrowPaint)

        // Ratio label above arrow
        if (mol1 != null && mol2 != null) {
            val oranPaint = Paint().apply { color = 0xFFB388FF.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
            canvas.drawText("$mol2 / $mol1", (arrowLeft + arrowRight) / 2f, arrowY - 12f, oranPaint)
        }

        // Reaction equation
        val eqPaint = Paint().apply { color = 0xFF39FF14.toInt(); textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText(reaksiyon ?: "", w / 2f, h * 0.55f, eqPaint)

        // Formula at bottom
        val formulaPaint = Paint().apply { color = 0xFF00F0FF.toInt(); textSize = 13f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("n$label2 = n$label1 x (katsayi$label2 / katsayi$label1)", w / 2f, h * 0.88f, formulaPaint)
        canvas.restore()
    }
}

class StokiyometriFragment : Fragment() {
    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_stoykiyometri, container, false)
        val mol1 = v.findViewById<EditText>(R.id.sto_mol1)
        val mol2 = v.findViewById<EditText>(R.id.sto_mol2)
        val miitar = v.findViewById<EditText>(R.id.sto_miitar)
        val birim = v.findViewById<Spinner>(R.id.sto_birim)
        val sonuc = v.findViewById<TextView>(R.id.sto_sonuc)

        val placeholder = v.findViewById<View>(R.id.sto_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val stoichView = StoichiometryView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(200))
        }
        parent.addView(stoichView, idx)

        ArrayAdapter.createFromResource(requireContext(), R.array.sto_birim, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            birim.adapter = it
        }

        v.findViewById<Button>(R.id.sto_hesapla).setOnClickListener {
            try {
                val m = miitar.text.toString().toDoubleOrNull() ?: 0.0
                sonuc.text = Calculator.stokiyometriHesapla(mol1.text.toString(), mol2.text.toString(), m, birim.selectedItem.toString())
                stoichView.mol1 = mol1.text.toString()
                stoichView.mol2 = mol2.text.toString()
                stoichView.invalidate()
            } catch (e: Exception) {
                sonuc.text = "Hata: ${e.message}"
            }
        }

        v.findViewById<Button>(R.id.sto_preset1).setOnClickListener {
            mol1.setText("H2"); mol2.setText("HCl")
            stoichView.mol1 = "H2"; stoichView.mol2 = "HCl"
            stoichView.reaksiyon = "H2 + Cl2 -> 2HCl"
            stoichView.invalidate()
        }
        v.findViewById<Button>(R.id.sto_preset2).setOnClickListener {
            mol1.setText("H2"); mol2.setText("H2O")
            stoichView.mol1 = "H2"; stoichView.mol2 = "H2O"
            stoichView.reaksiyon = "2H2 + O2 -> 2H2O"
            stoichView.invalidate()
        }
        v.findViewById<Button>(R.id.sto_preset3).setOnClickListener {
            mol1.setText("NaOH"); mol2.setText("NaCl")
            stoichView.mol1 = "NaOH"; stoichView.mol2 = "NaCl"
            stoichView.reaksiyon = "NaOH + HCl -> NaCl + H2O"
            stoichView.invalidate()
        }
        return v
    }
}
