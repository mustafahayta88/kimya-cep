package com.kimya.uygulama.features

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R

class MoleculeDrawerView(context: Context) : View(context) {
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
        return v
    }
}
