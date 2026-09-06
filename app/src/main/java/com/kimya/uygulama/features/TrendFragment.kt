package com.kimya.uygulama.features

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Choreographer
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.Interpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.HelpDialog
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.KimyaData
import kotlin.math.abs

val trendOzellikler = listOf("İyonlaşma Enerjisi", "Elektronegatiflik", "Atom Yarıçapı", "Atom Kütlesi", "Metalik Karakter", "Değerlik Elektron")
val trendModlar = listOf("GÖSTERGE", "TERAZİ", "MEKANİZMA", "DÜELLO")

fun trendBirim(ozellik: String): String = when (ozellik) {
    "İyonlaşma Enerjisi" -> "kJ/mol"
    "Elektronegatiflik" -> ""
    "Atom Yarıçapı" -> "pm"
    "Atom Kütlesi" -> "g/mol"
    else -> ""
}

fun trendDeger(sembol: String, ozellik: String): Double? = when (ozellik) {
    "İyonlaşma Enerjisi" -> KimyaData.iyonlasmaEnerjileri[sembol]
    "Elektronegatiflik" -> KimyaData.elektronegatiflikler[sembol]?.takeIf { it > 0 }
    "Atom Yarıçapı" -> KimyaData.atomYaricapPm[sembol]?.toDouble()
    "Atom Kütlesi" -> KimyaData.elementler[sembol]?.kutle
    "Metalik Karakter" -> KimyaData.elementler[sembol]?.let { el ->
        when (el.tur) {
            "Soy Gaz" -> 1.0
            "Ametal" -> 2.0
            "Yari Metal" -> 5.0
            "Gecis Metali" -> 8.0
            else -> 10.0
        }
    }
    "Değerlik Elektron" -> KimyaData.elementler[sembol]?.let { el ->
        when {
            el.atomNo in 57..71 || el.atomNo in 89..103 -> 3.0
            el.grup in 1..2 -> el.grup.toDouble()
            el.grup in 3..12 -> 2.0
            else -> (el.grup - 10).toDouble()
        }
    }
    else -> null
}

fun trendFormat(v: Double, ozellik: String): String = when (ozellik) {
    "Elektronegatiflik" -> "%.2f".format(v)
    "Atom Kütlesi", "Atom Yarıçapı", "İyonlaşma Enerjisi" -> if (v >= 100) "%.0f".format(v) else "%.1f".format(v)
    else -> "%.0f".format(v)
}

private fun isiRengi(t: Float): Int {
    val stops = arrayOf(
        0f to 0xFF1A237E.toInt(), 0.25f to 0xFF2962FF.toInt(),
        0.5f to 0xFF81C784.toInt(), 0.75f to 0xFFFFEA00.toInt(), 1f to 0xFFFF1744.toInt()
    )
    val tt = t.coerceIn(0f, 1f)
    for (i in 0 until stops.size - 1) {
        val (t0, c0) = stops[i]
        val (t1, c1) = stops[i + 1]
        if (tt <= t1) {
            val f = if (t1 == t0) 0f else (tt - t0) / (t1 - t0)
            return Color.rgb(
                (Color.red(c0) + (Color.red(c1) - Color.red(c0)) * f).toInt(),
                (Color.green(c0) + (Color.green(c1) - Color.green(c0)) * f).toInt(),
                (Color.blue(c0) + (Color.blue(c1) - Color.blue(c0)) * f).toInt()
            )
        }
    }
    return stops.last().second
}

private fun elementRenk(sembol: String): Int {
    val el = KimyaData.elementler[sembol] ?: return Color.GRAY
    return KimyaData.elementRengi(el.tur)
}

// ---------------------------------------------------------------------------
// 1) MEKANİK GÖSTERGE (analog kadran)
// ---------------------------------------------------------------------------
private class IbretInterpolator : Interpolator {
    override fun getInterpolation(input: Float): Float {
        val t = input.coerceIn(0f, 1f)
        // Phase 1 (0-0.35): slow spool-up — needle barely moves, "motor çalışıyor"
        // Phase 2 (0.35-0.8): strong acceleration — needle rushes
        // Phase 3 (0.8-1.0): decelerate and settle with slight overshoot
        return when {
            t < 0.35f -> 0.10f * (t / 0.35f) * (t / 0.35f)
            t < 0.80f -> {
                val u = (t - 0.35f) / 0.45f
                0.10f + 0.85f * u * u * (3f - 2f * u)
            }
            else -> {
                val u = (t - 0.80f) / 0.20f
                val base = 0.95f + 0.11f * u
                val settle = 1.06f * Math.exp(-4.0 * (1.0 - t)).toFloat()
                if (u > 0.5f) base + 0.04f * (u - 0.5f) * settle else base
            }
        }
    }
}
class MekanikGaugeView(context: Context) : View(context) {
    var etiket: String = ""
        set(v) { field = v; invalidate() }
    var deger: Double = 0.0
        set(v) { field = v; invalidate() }
    var minD: Double = 0.0
        set(v) { field = v; invalidate() }
    var maxD: Double = 1.0
        set(v) { field = v; invalidate() }
    var birim: String = ""
        set(v) { field = v; invalidate() }
    var degerYok: Boolean = false
        set(v) { field = v; invalidate() }

    private val dens = context.resources.displayMetrics.density
    private var anim = 0f
    var gecikme: Long = 0L
    private var calisiyor = false
    private var baslaNano = 0L
    private val ibret = IbretInterpolator()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!calisiyor) return
            val el = (frameTimeNanos - baslaNano) / 1_000_000f
            if (el <= 0f) {
                anim = 0f
                invalidate()
                postFrame()
                return
            }
            val t = (el / 1800f).coerceIn(0f, 1f)
            anim = ibret.getInterpolation(t) * 1.06f
            invalidate()
            if (t < 1f) postFrame()
        }
    }
    private fun postFrame() {
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post { oynat() }
    }
    override fun onDetachedFromWindow() {
        calisiyor = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onDetachedFromWindow()
    }
    fun oynat() {
        calisiyor = true
        anim = 0f
        invalidate()
        baslaNano = System.nanoTime() + gecikme * 1_000_000L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        postFrame()
    }

    private fun ang(t: Float): Double = Math.toRadians(135.0 + 270.0 * t)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f
        val cy = h * 0.42f
        val r = (minOf(w, h * 0.8f) / 2f - 4f * dens).coerceAtLeast(20f * dens)
        val t = if (degerYok) 0f else ((deger - minD) / (maxD - minD).coerceAtLeast(1e-9)).toFloat().coerceIn(0f, 1f)

        // bezel
        val sweep = Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 3f * dens
            shader = SweepGradient(cx, cy, intArrayOf(0xFF4A525C.toInt(), 0xFF23272E.toInt(), 0xFF4A525C.toInt(), 0xFF23272E.toInt()), null)
        }
        canvas.drawCircle(cx, cy, r + 2f * dens, sweep)
        canvas.drawCircle(cx, cy, r, Paint().apply { style = Paint.Style.FILL; color = 0xFF14181C.toInt() })
        canvas.drawCircle(cx, cy, r, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * dens; color = 0xFF3A4047.toInt() })

        // ticks
        val tick = Paint().apply { color = 0xFF7E878F.toInt(); strokeWidth = 1.1f * dens }
        val tickMaj = Paint().apply { color = 0xFFC6CDD6.toInt(); strokeWidth = 1.7f * dens }
        val inR = r * 0.8f; val outR = r * 0.92f
        for (i in 0..10) {
            val f = i / 10f
            val a = ang(f)
            val cosA = Math.cos(a).toFloat(); val sinA = Math.sin(a).toFloat()
            canvas.drawLine(cx + inR * cosA, cy + inR * sinA, cx + outR * cosA, cy + outR * sinA, if (i % 5 == 0) tickMaj else tick)
        }
        // scale labels
        val lbl = Paint().apply { color = 0xFF9AA3AC.toInt(); textSize = 6.5f * dens; textAlign = Paint.Align.CENTER }
        fun drawLabel(f: Float, yazi: String) {
            val a = ang(f)
            val lx = cx + Math.cos(a).toFloat() * r * 0.62f
            val ly = cy + Math.sin(a).toFloat() * r * 0.62f + 2f * dens
            canvas.drawText(yazi, lx, ly, lbl)
        }
        drawLabel(0f, trendFormat(minD, etiket))
        drawLabel(0.5f, trendFormat((minD + maxD) / 2, etiket))
        drawLabel(1f, trendFormat(maxD, etiket))

        // progress arc (sweeps together with the animated needle)
        val arcRect = RectF(cx - r + 2f * dens, cy - r + 2f * dens, cx + r - 2f * dens, cy + r - 2f * dens)
        canvas.drawArc(arcRect, 135f, 270f, false, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f * dens; color = 0xFF2A2F36.toInt() })
        if (!degerYok) {
            val tArc = (t * anim).coerceIn(0f, 1f)
            canvas.drawArc(arcRect, 135f, 270f * tArc, false, Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 3f * dens; color = isiRengi(tArc); strokeCap = Paint.Cap.ROUND
            })
        }

        // needle
        val aN = ang(t * anim)
        val nx = cx + Math.cos(aN).toFloat() * r * 0.74f
        val ny = cy + Math.sin(aN).toFloat() * r * 0.74f
        canvas.drawLine(cx, cy, nx, ny, Paint().apply { color = 0xFFF2C14E.toInt(); strokeWidth = 2.4f * dens; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND })
        canvas.drawCircle(cx, cy, 4.6f * dens, Paint().apply { color = 0xFFF2C14E.toInt() })
        canvas.drawCircle(cx, cy, 4.6f * dens, Paint().apply { style = Paint.Style.STROKE; color = 0xFF3A4047.toInt(); strokeWidth = 1f * dens })
        canvas.drawLine(cx - 2.5f * dens, cy, cx + 2.5f * dens, cy, Paint().apply { color = 0xFF3A4047.toInt(); strokeWidth = 0.8f * dens })
        canvas.drawLine(cx, cy - 2.5f * dens, cx, cy + 2.5f * dens, Paint().apply { color = 0xFF3A4047.toInt(); strokeWidth = 0.8f * dens })

        // value readout + label (counts up with the needle)
        val read = Paint().apply { color = 0xFF4DD0E1.toInt(); textSize = 11f * dens; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        val lab = Paint().apply { color = 0xFFAAB2BB.toInt(); textSize = 8f * dens; textAlign = Paint.Align.CENTER }
        val gosterilen = if (degerYok) null else minD + (deger - minD) * anim.coerceIn(0f, 1f)
        val degerStr = if (gosterilen == null) "--" else "${trendFormat(gosterilen, etiket)}${if (birim.isNotEmpty()) " " + birim else ""}"
        canvas.drawText(degerStr, cx, h - 16f * dens, read)
        canvas.drawText(etiket, cx, h - 4f * dens, lab)
    }
}

// ---------------------------------------------------------------------------
// 2) MEKANİK TERAZİ (karşılaştırma makinesi)
// ---------------------------------------------------------------------------
class MekanikTeraziView(context: Context) : View(context) {
    var solSym = ""
        set(v) { field = v; invalidate() }
    var sagSym = ""
        set(v) { field = v; invalidate() }
    var solD = 0.0
        set(v) { field = v; invalidate() }
    var sagD = 0.0
        set(v) { field = v; invalidate() }

    private val dens = context.resources.displayMetrics.density
    private var tilt = 0f
    private var sway = 0f
    private var tiltBasla = 0f
    private var tiltHedef = 0f
    private var tiltCalisiyor = false
    private var swayCalisiyor = false
    private var tiltBaslaNano = 0L
    private var swayBaslaNano = 0L
    private val overshoot = OvershootInterpolator(1.6f)
    private val swayCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!swayCalisiyor) return
            val el = (frameTimeNanos - swayBaslaNano) / 1_000_000f
            sway = (Math.sin(el / 1400f * Math.PI * 2.0)).toFloat() * 0.018f
            invalidate()
            if (swayCalisiyor) Choreographer.getInstance().postFrameCallback(this)
        }
    }
    private val tiltCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!tiltCalisiyor) return
            val el = (frameTimeNanos - tiltBaslaNano) / 1_000_000f
            if (el <= 0f) { tilt = tiltBasla; invalidate(); Choreographer.getInstance().postFrameCallback(this); return }
            val f = overshoot.getInterpolation((el / 1000f).coerceIn(0f, 1f))
            tilt = tiltBasla + (tiltHedef - tiltBasla) * f
            invalidate()
            if (el < 1000f) Choreographer.getInstance().postFrameCallback(this)
        }
    }
    init { swayCalisiyor = true; swayBaslaNano = System.nanoTime(); Choreographer.getInstance().postFrameCallback(swayCb) }
    override fun onDetachedFromWindow() {
        swayCalisiyor = false; tiltCalisiyor = false
        Choreographer.getInstance().removeFrameCallback(swayCb)
        Choreographer.getInstance().removeFrameCallback(tiltCb)
        super.onDetachedFromWindow()
    }

    fun setOran(s1: String, d1: Double, s2: String, d2: Double) {
        solSym = s1; sagSym = s2; solD = d1; sagD = d2
        tiltBasla = tilt
        tiltHedef = when {
            d1 > d2 -> -1f
            d2 > d1 -> 1f
            else -> 0f
        }
        tiltCalisiyor = true
        tiltBaslaNano = System.nanoTime()
        Choreographer.getInstance().removeFrameCallback(tiltCb)
        Choreographer.getInstance().postFrameCallback(tiltCb)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f
        val baseY = h - 8f * dens
        val pivotY = h * 0.34f
        val pivotX = cx
        val beamW = w * 0.72f

        // rivets
        val rv = Paint().apply { color = 0xFF3A4047.toInt() }
        val rvs = Paint().apply { style = Paint.Style.STROKE; color = 0xFF565D66.toInt(); strokeWidth = 1f * dens }
        for (corner in arrayOf(8f to 8f, w - 8f to 8f, 8f to h - 8f, w - 8f to h - 8f)) {
            canvas.drawCircle(corner.first * dens, corner.second, 2.5f * dens, rv)
            canvas.drawCircle(corner.first * dens, corner.second, 2.5f * dens, rvs)
        }

        // base + pillar
        val metal = Paint().apply { color = 0xFF3A3F47.toInt() }
        canvas.drawRoundRect(RectF(cx - w * 0.34f, baseY - 5f * dens, cx + w * 0.34f, baseY + 3f * dens), 3f * dens, 3f * dens, metal)
        canvas.drawRoundRect(RectF(cx - 4f * dens, pivotY + 6f * dens, cx + 4f * dens, baseY - 5f * dens), 2f * dens, 2f * dens, metal)

        // pivot screw
        val pivot = Paint().apply { color = 0xFFF2C14E.toInt() }
        canvas.drawCircle(pivotX, pivotY, 6f * dens, pivot)
        canvas.drawCircle(pivotX, pivotY, 6f * dens, Paint().apply { style = Paint.Style.STROKE; color = 0xFF565D66.toInt(); strokeWidth = 1.5f * dens })

        // beam endpoints (rotate around pivot by tilt + sway)
        val angDeg = (tilt + sway) * 14f
        val rad = Math.toRadians(angDeg.toDouble())
        val cosA = Math.cos(rad); val sinA = Math.sin(rad)
        fun rot(x: Float, y: Float): Pair<Float, Float> {
            val dx = x - pivotX; val dy = y - pivotY
            return Pair((pivotX + dx * cosA - dy * sinA).toFloat(), (pivotY + dx * sinA + dy * cosA).toFloat())
        }
        val lx0 = pivotX - beamW / 2f
        val rx0 = pivotX + beamW / 2f
        val (bxl, byl) = rot(lx0, pivotY)
        val (bxr, byr) = rot(rx0, pivotY)

        // beam
        val beam = Paint().apply { color = 0xFFE8B64E.toInt(); strokeWidth = 5f * dens; strokeCap = Paint.Cap.ROUND }
        canvas.drawLine(bxl, byl, bxr, byr, beam)
        canvas.drawLine(bxl, byl, bxr, byr, Paint().apply { color = 0xFF565D66.toInt(); strokeWidth = 1f * dens; style = Paint.Style.STROKE })

        // pans (move with beam rotation so they rise/lower)
        val chain = Paint().apply { color = 0xFF8A919C.toInt(); strokeWidth = 1.6f * dens }
        val panR = 30f * dens
        val panY = h * 0.62f
        val panLx = pivotX - beamW / 2f
        val panRx = pivotX + beamW / 2f
        // rotate pan anchor points around pivot the same as the beam
        val (panLa, panLy) = rot(panLx, panY)
        val (panRa, panRy) = rot(panRx, panY)
        // left pan
        canvas.drawLine(bxl, byl, panLa, panLy, chain)
        canvas.drawArc(RectF(panLa - panR, panLy - panR, panLa + panR, panLy + panR), 180f, 180f, false,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f * dens; color = 0xFFE8B64E.toInt() })
        canvas.drawArc(RectF(panLa - panR + 3f * dens, panLy - panR + 3f * dens, panLa + panR - 3f * dens, panLy + panR - 3f * dens), 180f, 180f, false,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f * dens; color = 0xFF8A919C.toInt() })
        // right pan
        canvas.drawLine(bxr, byr, panRa, panRy, chain)
        canvas.drawArc(RectF(panRa - panR, panRy - panR, panRa + panR, panRy + panR), 180f, 180f, false,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f * dens; color = 0xFFE8B64E.toInt() })
        canvas.drawArc(RectF(panRa - panR + 3f * dens, panRy - panR + 3f * dens, panRa + panR - 3f * dens, panRy + panR - 3f * dens), 180f, 180f, false,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f * dens; color = 0xFF8A919C.toInt() })

        // symbols on pans
        val symPaint = Paint().apply { isFakeBoldText = true; textSize = 16f * dens; textAlign = Paint.Align.CENTER }
        if (solSym.isNotEmpty()) {
            symPaint.color = elementRenk(solSym)
            canvas.drawText(solSym, panLa, panLy - 12f * dens, symPaint)
        }
        if (sagSym.isNotEmpty()) {
            symPaint.color = elementRenk(sagSym)
            canvas.drawText(sagSym, panRa, panRy - 12f * dens, symPaint)
        }

        // base label
        val baseL = Paint().apply { color = 0xFF6E767F.toInt(); textSize = 8f * dens; textAlign = Paint.Align.CENTER }
        canvas.drawText("KARŞILAŞTIRMA MAKİNESİ", cx, baseY - 14f * dens, baseL)
    }
}

// ---------------------------------------------------------------------------
// 3) MEKANİK DİŞLİ ŞERİDİ (periyodiklik simülatörü)
// ---------------------------------------------------------------------------
class MekanikDisliView(context: Context) : View(context) {
    var ozellik = "İyonlaşma Enerjisi"
        set(v) { field = v; invalidate() }
    var elemanlar: List<String> = emptyList()
        set(v) { field = v; requestLayout(); invalidate() }
    var calisiyor = true
        set(v) { field = v; invalidate() }
    var hiz = 1f
        set(v) { field = v }

    private val dens = context.resources.displayMetrics.density
    private val adim get() = 42f * dens
    private var acis = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val spin = object : Runnable {
        override fun run() {
            if (calisiyor) { acis += 1.6f * hiz; invalidate() }
            handler.postDelayed(this, 16)
        }
    }
    init { handler.post(spin) }
    override fun onDetachedFromWindow() { handler.removeCallbacks(spin); super.onDetachedFromWindow() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        setMeasuredDimension((maxOf(elemanlar.size, 1) * adim).toInt(), (112f * dens).toInt())
    }

    private fun disliCiz(canvas: Canvas, cx: Float, cy: Float, r: Float, aci: Float, renk: Int) {
        val teeth = (8 + (r / (22f * dens))).toInt().coerceIn(8, 14)
        val toothLen = r * 0.26f
        val toothW = r * 0.16f
        val body = Paint().apply { color = Color.argb(200, Color.red(renk), Color.green(renk), Color.blue(renk)) }
        val edge = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * dens; color = Color.argb(120, 255, 255, 255) }
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(aci)
        for (k in 0 until teeth) {
            canvas.save(); canvas.rotate(k * (360f / teeth))
            canvas.drawRoundRect(RectF(-toothW, -r - toothLen, toothW, -r + toothLen * 0.5f), 2f * dens, 2f * dens, body)
            canvas.drawRoundRect(RectF(-toothW, -r - toothLen, toothW, -r + toothLen * 0.5f), 2f * dens, 2f * dens, edge)
            canvas.restore()
        }
        canvas.drawCircle(0f, 0f, r, body)
        canvas.drawCircle(0f, 0f, r, edge)
        if (r > 16f * dens) {
            val spoke = Paint().apply { color = Color.argb(150, 255, 255, 255); strokeWidth = 1.4f * dens }
            for (k in 0..2) {
                val a = k * 120f + aci * 0.5f
                val rad = Math.toRadians(a.toDouble())
                val x2 = Math.cos(rad).toFloat() * r * 0.8f
                val y2 = Math.sin(rad).toFloat() * r * 0.8f
                canvas.drawLine(0f, 0f, x2, y2, spoke)
            }
        }
        canvas.drawCircle(0f, 0f, r * 0.3f, Paint().apply { color = Color.argb(230, Color.red(renk), Color.green(renk), Color.blue(renk)) })
        canvas.drawCircle(0f, 0f, r * 0.14f, Paint().apply { color = 0xFF14181C.toInt() })
        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (elemanlar.isEmpty()) return
        var mn = Double.POSITIVE_INFINITY; var mx = Double.NEGATIVE_INFINITY
        val vals = HashMap<String, Double>()
        for (s in elemanlar) trendDeger(s, ozellik)?.let { vals[s] = it; if (it < mn) mn = it; if (it > mx) mx = it }
        val fark = (mx - mn).takeIf { it > 0 } ?: 1.0

        val symPaint = Paint().apply { color = 0xFFC6CDD6.toInt(); textSize = 7f * dens; textAlign = Paint.Align.CENTER }
        val perPaint = Paint().apply { color = 0xFF8A919C.toInt(); textSize = 8f * dens; textAlign = Paint.Align.CENTER }
        val h = height.toFloat()
        var oncekiPeriyot = 0
        for (i in elemanlar.indices) {
            val s = elemanlar[i]
            val el = KimyaData.elementler[s]
            val cx = i * adim + adim / 2f
            val cy = h * 0.55f
            val v = vals[s]
            val t = if (v != null) ((v - mn) / fark).toFloat() else 0f
            val r = (9f + t * 26f) * dens
            val renk = if (el != null) KimyaData.elementRengi(el.tur) else Color.GRAY
            val dir = if (i % 2 == 0) 1f else -1f
            disliCiz(canvas, cx, cy, r, acis * dir, renk)
            if (el != null && el.periyot != oncekiPeriyot) {
                canvas.drawText("${el.periyot}.", cx, 10f * dens, perPaint)
                canvas.drawLine(cx, 14f * dens, cx, 18f * dens, Paint().apply { color = 0xFF3A4047.toInt(); strokeWidth = 1f * dens })
                oncekiPeriyot = el.periyot
            }
            canvas.drawText(s, cx, h - 4f * dens, symPaint)
        }
    }
}

// ---------------------------------------------------------------------------
// FRAGMENT
// ---------------------------------------------------------------------------
class TrendFragment : Fragment() {
    private var ozellik = "İyonlaşma Enerjisi"
    private var mod = "GÖSTERGE"
    private var seciliElement = "Fe"
    private var teraziSlot = "A"
    private var teraziA = "Na"
    private var teraziB = "Fe"

    private var skor = 0; private var seri = 0; private var rekor = 0
    private var duelA: String? = null; private var duelB: String? = null
    private var duelDogru: String? = null; private var duelKilitli = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var infoText: TextView
    private lateinit var container: FrameLayout
    private lateinit var modArea: FrameLayout
    private lateinit var ozellikSatiriView: View

    private val populerElementler = listOf("H", "He", "Li", "Be", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "Cr", "Fe", "Co", "Ni", "Cu", "Zn", "Br", "Ag", "I", "Xe", "Cs", "Pt", "Au", "Hg", "Pb", "U")

    private val globalAralik: Map<String, Pair<Double, Double>> by lazy {
        trendOzellikler.associateWith { p ->
            val v = KimyaData.elementler.values.mapNotNull { trendDeger(it.semIol, p) }
            (v.minOrNull() ?: 0.0) to (v.maxOrNull() ?: 1.0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_trend, container, false)
        infoText = v.findViewById(R.id.trend_info)
        this.container = v.findViewById(R.id.trend_container)
        infoText.text = modAciklama()
        val sv = v as? android.widget.ScrollView ?: return v
        val titleTv = ((((sv.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? TextView) ?: return v
        AnimUtils.gradientTitle(titleTv)

        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            HelpDialog.show(requireContext(), "Periyodik Analiz Makinesi", """
                <b>Ne işe yarar?</b> Periyodik özellikler, mekanik bir analiz makinesi gibi keşfedilir.<br/><br/>
                <b>1. GÖSTERGE:</b> Bir element seç — 6 özelliği analog kadranlarda (ibreli) gör. Kadran dolgusu o özelliğin tüm elementler arasındaki konumunu gösterir.<br/>
                <b>2. TERAZİ:</b> İki element seç; hangisinin seçili özellikte değeri yüksekse terazi ona doğru yatır. Sol/Sağ düğmeleriyle hangi kefeye koyacağını seç.<br/>
                <b>3. MEKANİZMA:</b> Her element bir dişli; dişli boyu seçili özelliğin değeriyle orantılı. Dişliler dönerken periyodikliği izle (ör. yarıçap: periyot içinde küçülür, grupta büyür).<br/>
                <b>4. DÜELLO:</b> İki elementten hangisinin değeri daha yüksek — bilmeceli oyun, skor ve seri takibi.<br/><br/>
                <i>Not: Atom yarıçapı değerleri kovalent yarıçap yaklaşımıdır (tahmini).</i>
            """.trimIndent())
        }

        val content = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        ozellikSatiriView = ozellikSatiri()
        content.addView(ozellikSatiriView)
        content.addView(modSatiri())
        modArea = FrameLayout(requireContext())
        content.addView(modArea)
        this.container.addView(content)

        showMod()
        return v
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun panel(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(Color.argb(10, 255, 255, 255))
            setStroke(1, Color.argb(30, 255, 255, 255))
        }
    }

    private fun chipBak(text: String, renk: Int, secili: Boolean): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 9f
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(3), dp(6), dp(3))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(if (secili) Color.BLACK else Color.argb(170, 210, 210, 210))
            background = GradientDrawable().apply {
                cornerRadius = dp(4).toFloat()
                if (secili) setColor(renk)
                else {
                    setColor(Color.argb(24, 255, 255, 255))
                    setStroke(1, Color.argb(60, 255, 255, 255))
                }
            }
        }
    }

    private fun ozellikSatiri(): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, 0)
        }
        val scroll = HorizontalScrollView(requireContext()).apply {
            addView(row)
            isHorizontalScrollBarEnabled = false
        }
        for (p in trendOzellikler) {
            val c = chipBak(p, 0xFF4DD0E1.toInt(), p == ozellik)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26))
            lp.setMargins(0, 0, dp(3), 0)
            c.layoutParams = lp
            c.setOnClickListener {
                ozellik = p
                infoText.text = modAciklama()
                for (i in 0 until row.childCount) {
                    val b = row.getChildAt(i) as TextView
                    b.setTextColor(if (b.text == p) Color.BLACK else Color.argb(170, 210, 210, 210))
                    (b.background as GradientDrawable).setColor(if (b.text == p) 0xFF4DD0E1.toInt() else Color.argb(24, 255, 255, 255))
                }
                showMod()
            }
            row.addView(c)
        }
        return scroll
    }

    private fun modSatiri(): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)
        }
        for (m in trendModlar) {
            val c = chipBak(m, 0xFFB388FF.toInt(), m == mod)
            val lp = LinearLayout.LayoutParams(0, dp(30), 1f)
            lp.setMargins(0, 0, dp(3), 0)
            c.layoutParams = lp
            c.setOnClickListener {
                AnimUtils.press(c)
                mod = m
                for (i in 0 until row.childCount) {
                    val b = row.getChildAt(i) as TextView
                    b.setTextColor(if (b.text == m) Color.BLACK else Color.argb(170, 210, 210, 210))
                    (b.background as GradientDrawable).setColor(if (b.text == m) 0xFFB388FF.toInt() else Color.argb(24, 255, 255, 255))
                }
                showMod()
            }
            row.addView(c)
        }
        return row
    }

    private fun showMod() {
        modArea.removeAllViews()
        ozellikSatiriView.visibility = if (mod == "GÖSTERGE") View.GONE else View.VISIBLE
        infoText.text = modAciklama()
        when (mod) {
            "TERAZİ" -> showTerazi()
            "MEKANİZMA" -> showMekanizma()
            "DÜELLO" -> showDuel()
            else -> showGosterge()
        }
    }

    private fun modAciklama(): String = when (mod) {
        "TERAZİ" -> "Kefelere iki element koy; seçili özellikte değeri büyük olan tarafa yatır. Sol / Sağ düğmesi hangi kefeyi dolduracağını seçer."
        "MEKANİZMA" -> "Her element bir dişli: dişli büyüklüğü seçili özelliğin değeriyle orantılı. Periyodikliği dönen mekanizmada izle."
        "DÜELLO" -> "İki elementten hangisinin değeri daha yüksek? Bil ve skorunu yükselt."
        else -> "Bir element seç; altı periyodik özellik analog kadranlarda işlenir. Kadran dolgusu, değerin tüm elementler arasındaki yerini gösterir."
    }

    private fun elementSecici(chipRowHedef: LinearLayout, onSec: (String) -> Unit): View {
        fun chipSatiri(liste: List<String>, buyukluk: Int, yaziBoy: Float): View {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            val scroll = HorizontalScrollView(requireContext()).apply {
                addView(row)
                isHorizontalScrollBarEnabled = false
            }
            for (s in liste) {
                val c = TextView(requireContext()).apply {
                    text = s; textSize = yaziBoy; gravity = Gravity.CENTER; setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    setPadding(dp(5), dp(2), dp(5), dp(2)); setTextColor(Color.WHITE)
                    background = GradientDrawable().apply {
                        cornerRadius = dp(4).toFloat(); setColor(elementRenk(s))
                    }
                    setOnClickListener { onSec(s) }
                }
                val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(buyukluk))
                lp.setMargins(0, 0, dp(2), 0)
                c.layoutParams = lp
                row.addView(c)
            }
            return scroll
        }
        val kolon = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        kolon.addView(chipSatiri(populerElementler, 22, 9.5f))
        kolon.addView(chipSatiri(KimyaData.elementler.keys.sortedBy { KimyaData.elementler[it]?.atomNo ?: Int.MAX_VALUE }, 20, 8f))
        return kolon
    }

    // ------------------------------------------------------------------
    // GÖSTERGE modu
    // ------------------------------------------------------------------
    private fun showGosterge() {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val kontrol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(4))
        }
        val arama = EditText(requireContext()).apply {
            hint = "Sembol veya ad ara (örn. Au, Demir)"
            textSize = 12f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(120, 200, 200, 200))
            setSingleLine(true)
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat(); setColor(Color.argb(16, 255, 255, 255)); setStroke(1, Color.argb(50, 255, 255, 255))
            }
            setPadding(dp(8), 0, dp(8), 0)
        }
        val lpArama = LinearLayout.LayoutParams(0, dp(38), 1f)
        lpArama.setMargins(0, 0, dp(4), 0)
        arama.layoutParams = lpArama
        arama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                if (q.length >= 2) {
                    KimyaData.elementBul(q)?.let { el -> seciliElement = el.semIol; guncelleGostergeler(root) }
                }
            }
        })
        kontrol.addView(arama)

        val rast = Button(requireContext()).apply {
            text = "RASTGELE"; textSize = 11f; setTextColor(Color.BLACK)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(0xFFF2C14E.toInt()) }
            setOnClickListener {
                seciliElement = populerElementler.random()
                arama.setText("")
                guncelleGostergeler(root)
            }
        }
        val lpRast = LinearLayout.LayoutParams(dp(84), dp(38))
        rast.layoutParams = lpRast
        kontrol.addView(rast)
        root.addView(kontrol)

        val chipRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        root.addView(elementSecici(chipRow) { s ->
            seciliElement = s
            arama.setText("")
            guncelleGostergeler(root)
        })
        root.addView(chipRow)

        val kart = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = panel()
        }
        root.addView(kart)
        kart.post { guncelleGostergeler(root) }

        val info = TextView(requireContext()).apply {
            textSize = 12f; setTextColor(Color.argb(180, 200, 200, 200)); setPadding(0, dp(6), 0, 0)
        }
        root.addView(info)

        modArea.addView(root)
        guncelleGostergeler(root)
    }

    private fun guncelleGostergeler(root: LinearLayout) {
        val el = KimyaData.elementler[seciliElement] ?: return
        if (root.childCount < 5) return
        val kart = root.getChildAt(3) as? LinearLayout ?: return
        val info = root.getChildAt(4) as? TextView ?: return
        kart.removeAllViews()

        val head = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(6))
        }
        head.addView(TextView(requireContext()).apply {
            text = el.semIol; textSize = 26f; setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            setPadding(dp(10), dp(2), dp(10), dp(2))
            background = GradientDrawable().apply { cornerRadius = dp(7).toFloat(); setColor(elementRenk(el.semIol)) }
        })
        head.addView(TextView(requireContext()).apply {
            text = "  ${el.adi}  (Z=${el.atomNo})\n  ${el.tur} • ${el.durum}"
            textSize = 13f; setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
        })
        kart.addView(head)

        val rows = listOf(
            trendOzellikler.subList(0, 2),
            trendOzellikler.subList(2, 4),
            trendOzellikler.subList(4, 6)
        )
        var kac = 0
        for (ikili in rows) {
            val satir = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            for (p in ikili) {
                val gauge = MekanikGaugeView(requireContext()).apply {
                    etiket = p
                    birim = trendBirim(p)
                    gecikme = kac * 220L
                    kac++
                    val (gmin, gmax) = globalAralik[p] ?: (0.0 to 1.0)
                    minD = gmin; maxD = gmax
                    val v = trendDeger(el.semIol, p)
                    if (v == null) { degerYok = true; deger = gmin }
                    else { degerYok = false; deger = v }
                }
                val lp = LinearLayout.LayoutParams(0, dp(150), 1f)
                lp.setMargins(0, dp(4), dp(4), 0)
                gauge.layoutParams = lp
                satir.addView(gauge)
            }
            kart.addView(satir)
        }
        info.text = "${el.adi} (${el.semIol}) hakkında:\n${el.kimyasal}\nKullanım: ${el.kullanim}"
    }

    // ------------------------------------------------------------------
    // TERAZİ modu
    // ------------------------------------------------------------------
    private fun showTerazi() {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val terazi = MekanikTeraziView(requireContext())
        val lpTer = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(230))
        terazi.layoutParams = lpTer
        root.addView(terazi)

        val slotRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val solBtn = Button(requireContext()).apply {
            text = "SOL KEFE"; textSize = 11f; setTextColor(if (teraziSlot == "A") Color.BLACK else Color.WHITE)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(if (teraziSlot == "A") 0xFF4DD0E1.toInt() else Color.argb(20, 255, 255, 255)) }
        }
        val sagBtn = Button(requireContext()).apply {
            text = "SAĞ KEFE"; textSize = 11f; setTextColor(if (teraziSlot == "B") Color.BLACK else Color.WHITE)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(if (teraziSlot == "B") 0xFF4DD0E1.toInt() else Color.argb(20, 255, 255, 255)) }
        }
        solBtn.setOnClickListener { teraziSlot = "A"; guncelleTerazi(terazi, root); refreshTeraziSlot(solBtn, sagBtn, root) }
        sagBtn.setOnClickListener { teraziSlot = "B"; guncelleTerazi(terazi, root); refreshTeraziSlot(solBtn, sagBtn, root) }
        val lpSol = LinearLayout.LayoutParams(0, dp(36), 1f); lpSol.setMargins(0, 0, dp(4), 0)
        solBtn.layoutParams = lpSol
        slotRow.addView(solBtn)
        val lpSag = LinearLayout.LayoutParams(0, dp(36), 1f); lpSag.setMargins(dp(4), 0, dp(4), 0)
        sagBtn.layoutParams = lpSag
        slotRow.addView(sagBtn)
        val rastBtn = Button(requireContext()).apply {
            text = "RASTGELE"; textSize = 11f; setTextColor(Color.BLACK)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(0xFFF2C14E.toInt()) }
            setOnClickListener {
                teraziA = populerElementler.random(); teraziB = populerElementler.random()
                if (teraziA == teraziB) teraziB = populerElementler.random()
                guncelleTerazi(terazi, root); refreshTeraziSlot(solBtn, sagBtn, root)
            }
        }
        val lpRast2 = LinearLayout.LayoutParams(0, dp(36), 1f)
        rastBtn.layoutParams = lpRast2
        slotRow.addView(rastBtn)
        root.addView(slotRow)

        val chipRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        root.addView(elementSecici(chipRow) { s ->
            if (teraziSlot == "A") teraziA = s else teraziB = s
            guncelleTerazi(terazi, root); refreshTeraziSlot(solBtn, sagBtn, root)
        })
        root.addView(chipRow)

        val sonuc = TextView(requireContext()).apply {
            textSize = 13f; gravity = Gravity.CENTER; setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.WHITE); setPadding(0, dp(6), 0, 0)
        }
        root.addView(sonuc)

        modArea.addView(root)
        guncelleTerazi(terazi, root)
        refreshTeraziSlot(solBtn, sagBtn, root)
    }

    private fun refreshTeraziSlot(solBtn: Button, sagBtn: Button, root: LinearLayout) {
        val secili = teraziSlot == "A"
        solBtn.setTextColor(if (secili) Color.BLACK else Color.WHITE)
        solBtn.background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(if (secili) 0xFF4DD0E1.toInt() else Color.argb(20, 255, 255, 255)) }
        sagBtn.setTextColor(if (!secili) Color.BLACK else Color.WHITE)
        sagBtn.background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(if (!secili) 0xFF4DD0E1.toInt() else Color.argb(20, 255, 255, 255)) }
        if (root.childCount < 5) return
        val sonuc = root.getChildAt(4) as? TextView ?: return
        val da = trendDeger(teraziA, ozellik); val db = trendDeger(teraziB, ozellik)
        val birim = trendBirim(ozellik).let { if (it.isNotEmpty()) " $it" else "" }
        val metin = when {
            da == null && db == null -> "${teraziA} ve ${teraziB} için veri yok"
            da == null -> "${teraziB} daha yüksek (${teraziA} için veri yok)"
            db == null -> "${teraziA} daha yüksek (${teraziB} için veri yok)"
            da > db -> "${teraziA} > ${teraziB}  (${trendFormat(da, ozellik)}$birim > ${trendFormat(db, ozellik)}$birim)"
            db > da -> "${teraziB} > ${teraziA}  (${trendFormat(db, ozellik)}$birim > ${trendFormat(da, ozellik)}$birim)"
            else -> "Eşit (${trendFormat(da, ozellik)}$birim)"
        }
        sonuc.text = metin
    }

    private fun guncelleTerazi(terazi: MekanikTeraziView, root: LinearLayout) {
        terazi.setOran(teraziA, trendDeger(teraziA, ozellik) ?: 0.0, teraziB, trendDeger(teraziB, ozellik) ?: 0.0)
    }

    // ------------------------------------------------------------------
    // MEKANİZMA modu
    // ------------------------------------------------------------------
    private fun showMekanizma() {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val disliler = KimyaData.elementler.values.sortedBy { it.atomNo }.map { it.semIol }

        val kontrol = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(6))
        }
        val durdur = Button(requireContext()).apply {
            text = "❚❚"; textSize = 12f; setTextColor(Color.WHITE)
            background = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(Color.argb(30, 255, 255, 255)) }
        }
        val lpDur = LinearLayout.LayoutParams(dp(44), dp(34)); lpDur.setMargins(0, 0, dp(6), 0)
        durdur.layoutParams = lpDur
        kontrol.addView(durdur)

        val hizRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val dv = MekanikDisliView(requireContext()).apply {
            this.ozellik = this@TrendFragment.ozellik
            elemanlar = disliler
        }
        for (hz in listOf(0.5f, 1f, 2f)) {
            val c = chipBak(if (hz == 1f) "1x" else "${hz}x", 0xFF81C784.toInt(), hz == 1f)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26))
            lp.setMargins(0, 0, dp(3), 0)
            c.layoutParams = lp
            c.setOnClickListener {
                dv.hiz = hz
                for (i in 0 until hizRow.childCount) {
                    val b = hizRow.getChildAt(i) as TextView
                    b.setTextColor(if (b.text == "${hz}x") Color.BLACK else Color.argb(170, 210, 210, 210))
                    (b.background as GradientDrawable).setColor(if (b.text == "${hz}x") 0xFF81C784.toInt() else Color.argb(24, 255, 255, 255))
                }
            }
            hizRow.addView(c)
        }
        val lpHz = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        hizRow.layoutParams = lpHz
        kontrol.addView(hizRow)

        val durumTv = TextView(requireContext()).apply {
            text = "DÖNÜYOR"; textSize = 10f; setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setTextColor(Color.rgb(0, 230, 118)); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(34))
        }
        kontrol.addView(durumTv)
        root.addView(kontrol)

        val lpDv = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112))
        dv.layoutParams = lpDv
        val disliScroll = HorizontalScrollView(requireContext()).apply {
            addView(dv)
            isHorizontalScrollBarEnabled = false
        }
        root.addView(disliScroll)

        val not = TextView(requireContext()).apply {
            text = "Dişli boyutu \"${ozellik}\" değeriyle orantılı. 118 element atom numarasına göre dizili."
            textSize = 11f; setTextColor(Color.argb(160, 200, 200, 200)); setPadding(0, dp(6), 0, 0)
        }
        root.addView(not)

        durdur.setOnClickListener {
            dv.calisiyor = !dv.calisiyor
            durumTv.text = if (dv.calisiyor) "DÖNÜYOR" else "DURDU"
            durumTv.setTextColor(if (dv.calisiyor) Color.rgb(0, 230, 118) else Color.rgb(255, 140, 60))
        }

        modArea.addView(root)
    }

    // ------------------------------------------------------------------
    // DÜELLO modu
    // ------------------------------------------------------------------
    private fun showDuel() {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val skorRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(6))
        }
        fun skorHuc(text: String, renk: Int): TextView {
            return TextView(requireContext()).apply {
                this.text = text
                textSize = 12f; setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                setTextColor(renk); gravity = Gravity.CENTER
                setPadding(dp(4), dp(4), dp(4), dp(4))
                background = GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat(); setColor(Color.argb(18, 255, 255, 255)); setStroke(1, Color.argb(30, 255, 255, 255))
                }
                val lp = LinearLayout.LayoutParams(0, dp(34), 1f)
                lp.setMargins(0, 0, dp(3), 0)
                layoutParams = lp
            }
        }
        val skorTv = skorHuc("Skor: 0", Color.rgb(0, 229, 255))
        val seriTv = skorHuc("Seri: 0", Color.rgb(0, 230, 118))
        val rekorTv = skorHuc("Rekor: 0", Color.rgb(255, 64, 129))
        skorRow.addView(skorTv); skorRow.addView(seriTv); skorRow.addView(rekorTv)
        root.addView(skorRow)

        val soruTv = TextView(requireContext()).apply {
            textSize = 14f; setTypeface(Typeface.DEFAULT, Typeface.BOLD); setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(soruTv)

        val btnRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val aBtn = duelButonu(); val bBtn = duelButonu()
        val lpA = LinearLayout.LayoutParams(0, dp(120), 1f); lpA.setMargins(0, 0, dp(4), 0)
        val lpB = LinearLayout.LayoutParams(0, dp(120), 1f); lpB.setMargins(dp(4), 0, 0, 0)
        aBtn.layoutParams = lpA; bBtn.layoutParams = lpB
        btnRow.addView(aBtn); btnRow.addView(bBtn)
        root.addView(btnRow)

        val sonucTv = TextView(requireContext()).apply {
            textSize = 12f; gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0); text = " "
        }
        root.addView(sonucTv)

        modArea.addView(root)
        yeniDuel(soruTv, aBtn, bBtn, sonucTv, skorTv, seriTv, rekorTv)
    }

    private fun duelButonu(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.argb(16, 255, 255, 255))
                setStroke(2, Color.argb(70, 255, 255, 255))
            }
            isClickable = true
        }
    }

    private fun yeniDuel(soruTv: TextView, aBtn: LinearLayout, bBtn: LinearLayout, sonucTv: TextView, skorTv: TextView, seriTv: TextView, rekorTv: TextView) {
        val birim = trendBirim(ozellik).let { if (it.isNotEmpty()) " ($it)" else "" }
        soruTv.text = "Hangisinin $ozellik$birim değeri daha YÜKSEK?"

        val adaylar = KimyaData.elementler.values.filter { trendDeger(it.semIol, ozellik) != null }
        if (adaylar.size < 2) return
        var a: com.kimya.uygulama.utils.ElementData? = null
        var b: com.kimya.uygulama.utils.ElementData? = null
        var guard = 0
        do {
            a = adaylar.random(); b = adaylar.random()
            guard++
        } while (guard < 300 && (a == b || abs(trendDeger(a.semIol, ozellik)!! - trendDeger(b.semIol, ozellik)!!) < 1e-9))
        val av = a ?: return
        val bv = b ?: return
        duelA = av.semIol; duelB = bv.semIol
        val dav = trendDeger(duelA ?: return, ozellik) ?: return
        val dbv = trendDeger(duelB ?: return, ozellik) ?: return
        duelDogru = if (dav > dbv) duelA else duelB

        fun doldur(btn: LinearLayout, sym: String) {
            btn.removeAllViews()
            val el = KimyaData.elementler[sym] ?: return
            btn.background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat(); setColor(Color.argb(16, 255, 255, 255)); setStroke(2, Color.argb(70, 255, 255, 255))
            }
            btn.addView(TextView(btn.context).apply {
                text = el.semIol; textSize = 34f; setTypeface(Typeface.DEFAULT, Typeface.BOLD); setTextColor(elementRenk(sym))
            })
            btn.addView(TextView(btn.context).apply {
                text = el.adi; textSize = 13f; setTypeface(Typeface.DEFAULT, Typeface.BOLD); setTextColor(Color.WHITE)
            })
            btn.addView(TextView(btn.context).apply {
                text = el.tur; textSize = 10f; setTextColor(Color.argb(160, 200, 200, 200))
            })
        }
        doldur(aBtn, duelA!!); doldur(bBtn, duelB!!)
        sonucTv.text = " "
        duelKilitli = false

        fun cevap(sym: String) {
            if (duelKilitli) return
            duelKilitli = true
            val dogruMu = sym == duelDogru
            val dogruSym = duelDogru ?: return
            val daStr = duelA ?: return
            val dbStr = duelB ?: return
            val va = trendDeger(daStr, ozellik) ?: return; val vb = trendDeger(dbStr, ozellik) ?: return
            val vibr = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (dogruMu) {
                skor++; seri++; if (seri > rekor) rekor = seri
                try { vibr?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
                sonucTv.setTextColor(Color.rgb(0, 230, 118))
                sonucTv.text = "DOĞRU! +1  |  ${duelA} = ${trendFormat(va, ozellik)}   ${duelB} = ${trendFormat(vb, ozellik)}"
            } else {
                seri = 0
                try { vibr?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
                sonucTv.setTextColor(Color.rgb(255, 80, 80))
                sonucTv.text = "YANLIŞ! Doğru: ${dogruSym} (${trendFormat(trendDeger(dogruSym, ozellik)!!, ozellik)})\nSen: $sym (${trendFormat(if (sym == duelA) va else vb, ozellik)})"
            }
            fun vurgula(btn: LinearLayout, btnSym: String) {
                val renk = when {
                    btnSym == dogruSym -> Color.rgb(0, 230, 118)
                    btnSym == sym -> Color.rgb(255, 80, 80)
                    else -> Color.rgb(0, 229, 255)
                }
                btn.background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat(); setColor(Color.argb(22, 255, 255, 255)); setStroke(3, renk)
                }
            }
            vurgula(aBtn, duelA!!)
            vurgula(bBtn, duelB!!)
            skorTv.text = "Skor: $skor"; seriTv.text = "Seri: $seri"; rekorTv.text = "Rekor: $rekor"
            handler.postDelayed({ if (!isAdded) return@postDelayed; duelKilitli = false; yeniDuel(soruTv, aBtn, bBtn, sonucTv, skorTv, seriTv, rekorTv) }, 1500)
        }

        aBtn.setOnClickListener { cevap(duelA!!) }
        bBtn.setOnClickListener { cevap(duelB!!) }
    }
}

