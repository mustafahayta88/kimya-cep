package com.kimya.uygulama.utils

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class ChartView(context: Context) : View(context) {
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B949E"); textSize = 28f }
    private val stdPoints = mutableListOf<Pair<Float, Float>>()
    private val samplePoints = mutableListOf<Triple<String, Float, Float>>()
    private var lineA = 0.0; private var lineB = 0.0; private var lineR2 = 0.0; private var hasLine = false
    private var marginL = 120f; private var marginR = 30f; private var marginT = 40f; private var marginB = 60f
    private var minC = 0.0; private var maxC = 1.0; private var minA = 0.0; private var maxA = 1.0
    var onTap: ((kons: Double, aIs: Double) -> Unit)? = null

    fun setData(std: List<Pair<Double, Double>>, samples: List<Triple<String, Double, Double>>, reg: Calculator.RegResult?) {
        stdPoints.clear(); std.forEach { stdPoints.add(Pair(it.first.toFloat(), it.second.toFloat())) }
        samplePoints.clear(); samples.forEach { samplePoints.add(Triple(it.first, it.second.toFloat(), it.third.toFloat())) }
        if (reg != null) { lineA = reg.a; lineB = reg.b; lineR2 = reg.r2; hasLine = true }
        else hasLine = false
        // Scale
        val allX = std.map { it.first } + samples.map { it.second }
        val allY = std.map { it.second } + samples.map { it.third }
        if (allX.isEmpty()) return
        minC = (allX.minOrNull() ?: 0.0) * 0.85; maxC = (allX.maxOrNull() ?: 1.0) * 1.15
        minA = 0.0; maxA = (allY.maxOrNull() ?: 1.0) * 1.2
        if (maxA <= 0) maxA = 1.0; if (maxC <= minC) maxC = minC + 1.0
        invalidate()
    }

    private fun mapX(x: Double): Float = marginL + ((x - minC) / (maxC - minC)).toFloat() * (width - marginL - marginR)
    private fun mapY(y: Double): Float = height - marginB - ((y - minA) / (maxA - minA)).toFloat() * (height - marginT - marginB)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#0D1117"))
        canvas.save(); canvas.scale(zoomScale, zoomScale, width / 2f, height / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        // Axes
        paint.color = Color.parseColor("#30363D"); paint.strokeWidth = 2f
        canvas.drawLine(marginL, marginT, marginL, height - marginB, paint)
        canvas.drawLine(marginL, height - marginB, width - marginR, height - marginB, paint)
        textPaint.color = Color.parseColor("#8B949E")
        canvas.drawText("Abs", marginL + 10f, marginT + textPaint.textSize, textPaint)
        canvas.drawText("Kons (ppm)", width - marginR - 150f, height - marginB + 40f, textPaint)
        // TicIs
        val nTicks = 5; paint.strokeWidth = 1f; paint.color = Color.parseColor("#30363D")
        for (i in 0 until nTicks) {
            val frac = i.toFloat() / (nTicks - 1)
            val xv = minC + (maxC - minC) * frac; val yv = minA + (maxA - minA) * frac
            val px = mapX(xv); val py = mapY(yv)
            canvas.drawLine(px, height - marginB, px, height - marginB + 8f, paint)
            textPaint.textSize = 22f; canvas.drawText("%.1f".format(xv), px - 20f, height - marginB + 30f, textPaint)
            canvas.drawLine(marginL, py, marginL - 8f, py, paint)
            canvas.drawText("%.2f".format(yv), marginL - 80f, py + 8f, textPaint)
        }
        // Regression line
        if (hasLine) {
            paint.color = Color.parseColor("#FF0080"); paint.strokeWidth = 3f
            val x1 = mapX(minC); val y1 = mapY(lineA * minC + lineB)
            val x2 = mapX(maxC); val y2 = mapY(lineA * maxC + lineB)
            canvas.drawLine(x1, y1, x2, y2, paint)
            textPaint.color = Color.parseColor("#FFEE00"); textPaint.textSize = 26f
            canvas.drawText("R² = %.4f".format(lineR2), width - marginR - 200f, marginT + 30f, textPaint)
        }
        // Standard points
        for ((cx, ay) in stdPoints) {
            val px = mapX(cx.toDouble()); val py = mapY(ay.toDouble())
            paint.color = Color.parseColor("#39FF14"); paint.style = Paint.Style.FILL
            canvas.drawCircle(px, py, 8f, paint)
            paint.style = Paint.Style.STROKE; paint.color = Color.WHITE; paint.strokeWidth = 2f
            canvas.drawCircle(px, py, 8f, paint)
        }
        // Sample points (red diamonds)
        for ((name, cx, ay) in samplePoints) {
            val px = mapX(cx.toDouble()); val py = mapY(ay.toDouble())
            paint.color = Color.parseColor("#FF3333"); paint.style = Paint.Style.FILL
            val path = Path()
            path.moveTo(px, py - 10f); path.lineTo(px + 10f, py)
            path.lineTo(px, py + 10f); path.lineTo(px - 10f, py); path.close()
            canvas.drawPath(path, paint)
            paint.color = Color.WHITE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f
            canvas.drawPath(path, paint)
            textPaint.color = Color.parseColor("#FF3333"); textPaint.textSize = 22f
            canvas.drawText(name, px + 14f, py + 8f, textPaint)
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        sDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x; val y = event.y
            if (x < marginL || x > width - marginR || y < marginT || y > height - marginB) return true
            val kons = minC + (x - marginL) / (width - marginL - marginR) * (maxC - minC)
            val absV = minA + (height - marginB - y) / (height - marginT - marginB) * (maxA - minA)
            onTap?.invoke(kons, absV)
        }
        return true
    }
}
