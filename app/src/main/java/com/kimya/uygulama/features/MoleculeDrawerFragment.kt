package com.kimya.uygulama.features

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R

class MoleculeDrawerView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }
    private val paths = mutableListOf<Pair<Path, Int>>()
    private var currentPath: Path? = null
    private var currentColor = 0xFF00F0FF.toInt()

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        sDetector.onTouchEvent(event)
        val x = event.x; val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath = Path().apply { moveTo(x, y) }
                paths.add(currentPath!! to currentColor)
            }
            MotionEvent.ACTION_MOVE -> currentPath?.lineTo(x, y)
            MotionEvent.ACTION_UP -> currentPath?.lineTo(x, y)
        }
        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for ((path, color) in paths) {
            paint.color = color
            canvas.drawPath(path, paint)
        }
    }

    fun clear() { paths.clear(); currentPath = null; invalidate() }
    fun setColor(color: Int) { currentColor = color }
}

class MoleculeDrawerFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_molecule_drawer, container, false)
        val placeholder = v.findViewById<View>(R.id.mol_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        val drawView = MoleculeDrawerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 400)
        }
        parent.addView(drawView, idx)

        v.findViewById<Button>(R.id.mol_clear).setOnClickListener { drawView.clear() }
        v.findViewById<Button>(R.id.mol_red).setOnClickListener { drawView.setColor(0xFFFF4444.toInt()) }
        v.findViewById<Button>(R.id.mol_green).setOnClickListener { drawView.setColor(0xFF44FF44.toInt()) }
        v.findViewById<Button>(R.id.mol_blue).setOnClickListener { drawView.setColor(0xFF4488FF.toInt()) }
        v.findViewById<Button>(R.id.mol_white).setOnClickListener { drawView.setColor(0xFFFFFFFF.toInt()) }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Molekul Ciz")
                .setMessage("Kendi molekullerinizi cizebilirsiniz!\n\n" +
                    "- Parmaginizi surukleyerek cizim yapin\n" +
                    "- Renk secmek icin alttaki dugmelere dokunun\n" +
                    "- Temizle dugmesiyle ekrani sifirlayin\n" +
                    "- Yakinsastirmak icin iki parmak kullanin\n\n" +
                    "Serbest cizim ile istediginiz molekulu cizebilirsiniz.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }
}
