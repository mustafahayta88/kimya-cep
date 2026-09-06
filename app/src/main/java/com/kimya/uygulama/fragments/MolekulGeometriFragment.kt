package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextPaint
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import kotlin.math.*

data class VSEPRGeo(
    val name: String, val nameEn: String, val bondPairs: Int, val lonePairs: Int,
    val angle: String, val info: String, val polar: String,
    val example: String, val hybrid: String, val vseprType: String,
    val atoms: List<GAtom>, val bonds: List<Pair<Int, Int>>
)

data class GAtom(val x: Float, val y: Float, val z: Float, val symbol: String, val color: Int, val radius: Float, val isCenter: Boolean = false)

// ---------------------------------------------------------------------------
// 3B GÖRÜNTÜLEYİCİ — perspektif, derinlik sıralı, ışıklı küreler
// ---------------------------------------------------------------------------
class Geo3DView(context: Context) : View(context) {
    private var geo: VSEPRGeo? = null
    private var rotX = 0.35f; private var rotY = 0.5f
    var autoRotate = true
    private var zoom = 1f
    private var lastTx = 0f; private var lastTy = 0f
    private var tMode = 0
    private var pulse = 0f
    private var vRotX = 0f; private var vRotY = 0f
    private var inertiaActive = false
    private var prevMoveT = 0L; private var prevRotY = 0f; private var prevRotX = 0f
    private var startTx = 0f; private var startTy = 0f; private var accX = 0f; private var accY = 0f
    private var seciliIndex = -1
    var onAtomSelected: ((GAtom, Int) -> Unit)? = null
    private val sDetector: ScaleGestureDetector
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val atomColors = mapOf(
        "H" to 0xFFE8E8E8.toInt(), "O" to 0xFFFF3B30.toInt(), "N" to 0xFF3B82F6.toInt(),
        "F" to 0xFF4CD964.toInt(), "C" to 0xFF8E8E93.toInt(), "Cl" to 0xFF34C759.toInt(),
        "Br" to 0xFFB45309.toInt(), "S" to 0xFFFFD60A.toInt(), "P" to 0xFFFF9500.toInt(),
        "I" to 0xFFAF52DE.toInt(), "Xe" to 0xFF32ADE6.toInt(), "Be" to 0xFFA8E6CF.toInt(),
        "B" to 0xFFFFB5B5.toInt(), "He" to 0xFF4DD0E1.toInt(), "Ta" to 0xFF8E8E93.toInt(),
        "Mo" to 0xFF6B6B6B.toInt(), "Re" to 0xFF9E9EA3.toInt(), "W" to 0xFF7A7A80.toInt(),
        "Al" to 0xFFBFBFBF.toInt(), "Te" to 0xFFD89000.toInt(), "As" to 0xFF6B5CFF.toInt(),
        "Zr" to 0xFF8E8E93.toInt(), "Mn" to 0xFF9E9EA3.toInt()
    )
    private val atomR = mapOf("H" to 10f, "C" to 15f, "O" to 15f, "N" to 15f, "F" to 13f,
        "Cl" to 17f, "Br" to 19f, "S" to 18f, "P" to 18f, "I" to 21f, "Xe" to 20f, "He" to 20f)

    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sphereP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bondGlowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.DEFAULT_BOLD
    }
    private val selectRingP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val loneP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var running = true
    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (autoRotate) { rotY += 0.008f }
            else if (inertiaActive) {
                rotY += vRotY; rotX += vRotX
                rotX = rotX.coerceIn(-PI.toFloat() * 0.9f, PI.toFloat() * 0.9f)
                vRotX *= 0.94f; vRotY *= 0.94f
                if (abs(vRotX) < 0.0004f && abs(vRotY) < 0.0004f) {
                    inertiaActive = false
                    handler.postDelayed({ autoRotate = true }, 2000)
                }
            }
            pulse += 0.06f
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoom = (zoom * d.scaleFactor).coerceIn(0.35f, 4f); invalidate(); return true
            }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; startTx = e.x; startTy = e.y; accX = 0f; accY = 0f; tMode = 1; autoRotate = false; inertiaActive = false; prevMoveT = System.currentTimeMillis(); prevRotX = rotX; prevRotY = rotY }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (tMode == 1) {
                        val dx = e.x - lastTx; val dy = e.y - lastTy
                        if (dx * dx + dy * dy < 40f * 40f) selectAtom(e.x, e.y)
                        handler.postDelayed({ autoRotate = true }, 4000)
                    } else if (tMode == 2) {
                        inertiaActive = true
                    }
                    tMode = 0
                }
            }
            if (e.action == MotionEvent.ACTION_MOVE && e.pointerCount == 1) {
                if (tMode == 1) {
                    val dx = e.x - lastTx; val dy = e.y - lastTy
                    accX += dx; accY += dy
                    if (accX * accX + accY * accY > 16f) {
                        // toplam hareket eşiği aşılınca, hareketi baştan itibaren uygula (takılma hissi yok)
                        tMode = 2
                        val totX = e.x - startTx; val totY = e.y - startTy
                        rotY += totX * 0.018f; rotX += totY * 0.018f
                        rotX = rotX.coerceIn(-PI.toFloat() * 0.9f, PI.toFloat() * 0.9f)
                        prevRotY = rotY; prevRotX = rotX; prevMoveT = System.currentTimeMillis()
                        lastTx = e.x; lastTy = e.y
                        invalidate()
                    }
                } else if (tMode == 2) {
                    val dx = e.x - lastTx; val dy = e.y - lastTy
                    val now = System.currentTimeMillis()
                    val newY = rotY + dx * 0.018f
                    val newX = rotX + dy * 0.018f
                    val dt = (now - prevMoveT).coerceAtLeast(1L)
                    vRotY = ((newY - prevRotY) / dt * 16.7f) * 0.55f
                    vRotX = ((newX - prevRotX) / dt * 16.7f) * 0.55f
                    prevRotY = newY; prevRotX = newX; prevMoveT = now
                    rotY = newY; rotX = newX.coerceIn(-PI.toFloat() * 0.9f, PI.toFloat() * 0.9f)
                    lastTx = e.x; lastTy = e.y
                    invalidate()
                }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow(); running = true
        Choreographer.getInstance().postFrameCallback(frameCb)
    }
    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCb)
        super.onDetachedFromWindow()
    }

    fun setGeo(g: VSEPRGeo) { geo = g; rotX = 0.35f; rotY = 0.5f; zoom = 1f; seciliIndex = -1; invalidate() }

    private fun selectAtom(x: Float, y: Float) {
        val g = geo ?: return
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h * 0.47f
        val maxR = (g.atoms.map { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }.maxOrNull() ?: 50f).coerceAtLeast(40f)
        val sc = minOf(w, h) * 0.36f / maxR * zoom
        val cr = cos(rotX); val sr = sin(rotX)
        val cp = cos(rotY); val sp = sin(rotY)
        var best = -1; var bestD = Float.MAX_VALUE
        for (i in g.atoms.indices) {
            val a = g.atoms[i]
            val x1 = a.x * cp + a.z * sp
            val z1 = -a.x * sp + a.z * cp
            val y1 = a.y
            val y2 = y1 * cr - z1 * sr
            val sx = cx + x1 * sc; val sy = cy + y2 * sc
            val rr = a.radius * (minOf(w, h) * 0.0050f)
            val d = (sx - x) * (sx - x) + (sy - y) * (sy - y)
            val hitR = rr * 2.2f
            if (d < hitR * hitR && d < bestD) { best = i; bestD = d }
        }
        seciliIndex = best
        invalidate()
        if (best >= 0) onAtomSelected?.invoke(g.atoms[best], best)
    }

    private fun lighten(c: Int, f: Float): Int {
        val r = Color.red(c) + ((255 - Color.red(c)) * f).toInt()
        val g = Color.green(c) + ((255 - Color.green(c)) * f).toInt()
        val b = Color.blue(c) + ((255 - Color.blue(c)) * f).toInt()
        return Color.rgb(r, g, b)
    }
    private fun darken(c: Int, f: Float): Int {
        val r = (Color.red(c) * f).toInt()
        val g = (Color.green(c) * f).toInt()
        val b = (Color.blue(c) * f).toInt()
        return Color.rgb(r, g, b)
    }
    private fun labelColor(bg: Int): Int {
        val lum = 0.299f * Color.red(bg) + 0.587f * Color.green(bg) + 0.114f * Color.blue(bg)
        return if (lum > 150) Color.rgb(20, 26, 36) else Color.WHITE
    }

    private fun drawStars(canvas: Canvas, w: Float, h: Float) {
        val starP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until 26) {
            val sx = ((i * 137.51f).toInt() % 1000) / 1000f * w
            val sy = ((i * 229.11f).toInt() % 1000) / 1000f * h
            val tw = 0.6f + ((i * 13) % 30) / 40f
            val a = 16 + ((i * 27) % 28)
            starP.color = Color.argb(a, 165, 205, 255)
            canvas.drawCircle(sx, sy, tw, starP)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        // koyu lacivert-mor zemin
        val bg = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF0A0A1E.toInt(), 0xFF171233.toInt(), 0xFF0A0A1E.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = bg })

        // mor sis
        val neb = RadialGradient(w / 2f, h * 0.44f, minOf(w, h) * 0.7f,
            intArrayOf(0x1E8B5CF6.toInt(), 0x0F8B5CF6.toInt(), 0x00000000.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = neb })

        val g = geo ?: return
        val cx = w / 2f; val cy = h * 0.47f

        val cr = cos(rotX); val sr = sin(rotX)
        val cp = cos(rotY); val sp = sin(rotY)
        val maxR = (g.atoms.map { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }.maxOrNull() ?: 50f).coerceAtLeast(40f)

        val proj = g.atoms.map { a ->
            val x1 = a.x * cp + a.z * sp
            val z1 = -a.x * sp + a.z * cp
            val y1 = a.y
            val y2 = y1 * cr - z1 * sr
            val z2 = y1 * sr + z1 * cr
            // sabit boyut — perspektif büyütme/küçültme yok
            val sc = minOf(w, h) * 0.36f / maxR * zoom
            Triple(cx + x1 * sc, cy + y2 * sc, z2)
        }

        // koordinat eksenleri (x kırmızı-mor, y mor, z beyaz-mor)
        val axLen = minOf(w, h) * 0.34f
        val axP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f }
        axP.color = Color.argb(55, 167, 139, 250); canvas.drawLine(cx - axLen, cy, cx + axLen, cy, axP)
        axP.color = Color.argb(55, 190, 120, 255); canvas.drawLine(cx, cy - axLen, cx, cy + axLen, axP)
        axP.color = Color.argb(30, 200, 170, 255); canvas.drawLine(cx, cy, cx + axLen * 0.55f, cy + axLen * 0.4f, axP)

        // elips ızgara zemin (mor)
        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.argb(16, 167, 139, 250); strokeWidth = 1.2f }
        val rr = minOf(w, h) * 0.28f
        for (i in 1..3) {
            val er = rr * i / 3f
            canvas.drawOval(cx - er, h * 0.93f - er * 0.22f, cx + er, h * 0.93f + er * 0.22f, gp)
        }

        // tüm elementleri birbirine bağlayan şeffaf bağ ağı (atomların altında)
        if (proj.size > 1) {
            val webP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f; strokeCap = Paint.Cap.ROUND }
            val webOrder = (0 until proj.size).flatMap { i -> (i + 1 until proj.size).map { j -> i to j } }
                .sortedBy { (proj[it.first].third + proj[it.second].third) * 0.5f }
            for ((i, j) in webOrder) {
                val p1 = proj[i]; val p2 = proj[j]
                val zavg = (p1.third + p2.third) * 0.5f
                val alpha = ((0.35f + zavg * 0.12f) * 255).toInt().coerceIn(80, 180)
                webP.color = Color.argb(alpha, 167, 139, 250)
                canvas.drawLine(p1.first.toFloat(), p1.second.toFloat(), p2.first.toFloat(), p2.second.toFloat(), webP)
            }
        }

        // bağlar — stick model, çift çizgi (derinlik hissi)
        val bondOrder = g.bonds.withIndex().sortedBy {
            val p1 = proj[it.value.first]; val p2 = proj[it.value.second]
            (p1.third + p2.third) * 0.5f
        }
        for ((_, b) in bondOrder) {
            if (b.first >= proj.size || b.second >= proj.size) continue
            val p1 = proj[b.first]; val p2 = proj[b.second]
            val sx1 = p1.first.toFloat(); val sy1 = p1.second.toFloat()
            val sx2 = p2.first.toFloat(); val sy2 = p2.second.toFloat()
            val zavg = (p1.third + p2.third) * 0.5f
            val depthF = 1f
            val alpha = ((0.55f + zavg * 0.10f) * 255).toInt().coerceIn(90, 255)
            val base = Color.rgb(186, 176, 214)
            val wd = 2.6f * depthF
            bondGlowP.color = Color.argb(alpha / 4, 167, 139, 250)
            bondGlowP.strokeWidth = wd * 2.2f
            canvas.drawLine(sx1, sy1, sx2, sy2, bondGlowP)
            bondP.color = Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
            bondP.strokeWidth = wd
            canvas.drawLine(sx1, sy1, sx2, sy2, bondP)
            // uçlarda ince ikincil çizgi — silindir hissi
            val dx = sx2 - sx1; val dy = sy2 - sy1
            val len = sqrt(dx * dx + dy * dy)
            if (len > 1f) {
                val nx = -dy / len * wd * 0.9f; val ny = dx / len * wd * 0.9f
                val edgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                edgeP.color = Color.argb((alpha * 0.45f).toInt(), 120, 100, 160)
                edgeP.strokeWidth = 1.1f * depthF
                canvas.drawLine(sx1 + nx, sy1 + ny, sx2 + nx, sy2 + ny, edgeP)
                canvas.drawLine(sx1 - nx, sy1 - ny, sx2 - nx, sy2 - ny, edgeP)
            }
        }

        // merkez atom + açı yayları
        val centerIdx = g.atoms.indexOfFirst { it.isCenter }
        if (centerIdx >= 0 && centerIdx < proj.size) {
            val cproj = proj[centerIdx]
            val cSx = cproj.first.toFloat(); val cSy = cproj.second.toFloat()
            val cR = (atomR[g.atoms[centerIdx].symbol] ?: 15f) * (minOf(w, h) * 0.0050f)
            val bondAngles = mutableListOf<Float>()
            for (b in g.bonds) {
                val other = if (b.first == centerIdx) b.second else b.first
                if (other >= proj.size) continue
                val op = proj[other]
                val dx = op.first.toFloat() - cSx; val dy = op.second.toFloat() - cSy
                if (abs(dx) + abs(dy) > 1f) bondAngles.add(atan2(dy, dx))
            }
            bondAngles.sort()
            if (bondAngles.size >= 2) {
                val arcP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f }
                val arcR = cR * 1.9f
                for (i in bondAngles.indices) {
                    val a1 = bondAngles[i]
                    val a2 = bondAngles[(i + 1) % bondAngles.size]
                    var sweep = a2 - a1
                    while (sweep > PI) sweep -= 2 * PI.toFloat()
                    while (sweep < -PI) sweep += 2 * PI.toFloat()
                    if (abs(sweep) < 0.12f || abs(sweep) > PI * 0.95f) continue
                    arcP.color = Color.argb(140 + (60 * sin(pulse + i)).toInt(), 167, 139, 250)
                    canvas.drawArc(cSx - arcR, cSy - arcR, cSx + arcR, cSy + arcR, a1 * 180f / PI.toFloat(), sweep * 180f / PI.toFloat(), false, arcP)
                    // açı etiketi
                    val mid = a1 + sweep / 2f
                    val lx = cSx + cos(mid) * arcR * 1.45f; val ly = cSy + sin(mid) * arcR * 1.45f
                    val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                    lp.textSize = 15f; lp.color = Color.argb(200, 196, 181, 253)
                    canvas.drawText(g.angle, lx, ly, lp)
                }
            }
        }

        // atomlar — derinlik sıralı, küçük küreler (stick model); uzak önce, yakın son
        val sorted = proj.withIndex().sortedBy { it.value.third }
        for ((idx, p) in sorted) {
            if (idx >= g.atoms.size) continue
            val a = g.atoms[idx]
            val sx = p.first.toFloat(); val sy = p.second.toFloat(); val sz = p.third
            val r = atomR[a.symbol] ?: 15f
            val depthF = 1f
            val rad = r * (minOf(w, h) * 0.0040f) * depthF
            val baseC = a.color

            if (a.isCenter) {
                val cg = RadialGradient(sx, sy, rad * 1.7f,
                    intArrayOf(0x2E8B5CF6.toInt(), 0x00000000.toInt()), null, Shader.TileMode.CLAMP)
                canvas.drawCircle(sx, sy, rad * 1.7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = cg })
            }

            // yumuşak küre (az parlak — Lewis'ten farklı)
            val hiX = sx - rad * 0.35f; val hiY = sy - rad * 0.38f
            sphereP.shader = RadialGradient(hiX, hiY, rad * 1.5f,
                intArrayOf(lighten(baseC, 0.45f), baseC, darken(baseC, 0.25f)),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(sx, sy, rad, sphereP)

            // mor ince çerçeve
            rimP.color = Color.argb((70 + (60 * depthF).toInt()), 160, 140, 220)
            rimP.strokeWidth = 1.2f
            canvas.drawCircle(sx, sy, rad, rimP)

            if (idx == seciliIndex) {
                val ringR = rad + 5f + 2f * pulse
                selectRingP.color = Color.argb((170 + (85 * pulse).toInt()).coerceIn(0, 255), 167, 139, 250)
                selectRingP.strokeWidth = 3f
                canvas.drawCircle(sx, sy, ringR, selectRingP)
            }

            // yalnız çiftler — turuncu ikiz nokta
            val gLone = if (a.isCenter) g.lonePairs else 0
            if (gLone > 0) {
                var dirX = 0f; var dirY = 0f
                for (b in g.bonds) {
                    val other = if (b.first == idx) b.second else b.first
                    if (other >= proj.size) continue
                    val op = proj[other]
                    val dx = sx - op.first.toFloat(); val dy = sy - op.second.toFloat()
                    val l = sqrt(dx * dx + dy * dy)
                    if (l > 0.1f) { dirX += dx / l; dirY += dy / l }
                }
                if (dirX * dirX + dirY * dirY < 0.01f) { dirX = 0f; dirY = -1f }
                val il = sqrt(dirX * dirX + dirY * dirY); dirX /= il; dirY /= il
                val off = rad * 1.6f
                val dotR = rad * 0.18f
                val gap = dotR * 2.4f
                for (k in 0 until gLone) {
                    val a0 = (k - (gLone - 1) / 2f) * 1.15f
                    val ca = cos(a0); val sa = sin(a0)
                    val pxr = dirX * ca - dirY * sa
                    val pyr = dirX * sa + dirY * ca
                    val bx = sx + pxr * off
                    val by = sy + pyr * off
                    val halo = Paint(Paint.ANTI_ALIAS_FLAG)
                    halo.shader = RadialGradient(bx, by, rad * 0.95f,
                        intArrayOf(0x38FFB300.toInt(), 0x16FFB300.toInt(), 0x00000000.toInt()),
                        floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                    canvas.drawCircle(bx, by, rad * 0.95f, halo)
                    loneP.color = Color.argb(215, 255, 220, 150)
                    val tx = -pyr; val ty = pxr
                    canvas.drawCircle(bx + tx * gap, by + ty * gap, dotR, loneP)
                    canvas.drawCircle(bx - tx * gap, by - ty * gap, dotR, loneP)
                }
            }

            // sembol etiketi
            textP.textSize = rad * 0.85f
            canvas.drawText(a.symbol, sx, sy + textP.textSize * 0.36f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; color = Color.argb(160, 0, 0, 0); textSize = textP.textSize
                style = Paint.Style.STROKE; strokeWidth = rad * 0.16f; strokeJoin = Paint.Join.ROUND
            })
            textP.color = labelColor(baseC)
            canvas.drawText(a.symbol, sx, sy + textP.textSize * 0.36f, textP)
        }

        // açı rozeti (mor)
        val ap = Paint(Paint.ANTI_ALIAS_FLAG)
        ap.color = Color.argb(120, 30, 22, 60)
        canvas.drawRoundRect(w * 0.64f, 10f, w * 0.98f, 42f, 16f, 16f, ap)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        tp.textSize = 16f; tp.color = Color.rgb(167, 139, 250)
        canvas.drawText("∠ ${g.angle}", (w * 0.64f + w * 0.98f) / 2f, 31f, tp)
    }
}

// ---------------------------------------------------------------------------
// FRAGMENT
// ---------------------------------------------------------------------------
class MolekulGeometriFragment : Fragment() {

    private val geos = listOf(
        VSEPRGeo("Doğrusal", "Linear", 2, 0, "180°",
            "2 bağ çifti, 0 yalnız çift\nsp hibritleşme\n\nÖrnekler: CO₂, BeCl₂, CS₂",
            "Polar olmayan", "CO₂, BeCl₂", "sp", "AX₂",
            listOf(GAtom(0f, 0f, 0f, "C", 0xFF6B6B6B.toInt(), 16f, true),
                   GAtom(25f, 0f, 0f, "O", 0xFFFF3B30.toInt(), 18f),
                   GAtom(-25f, 0f, 0f, "O", 0xFFFF3B30.toInt(), 18f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("Düzenli üçgen", "Trigonal Planar", 3, 0, "120°",
            "3 bağ çifti, 0 yalnız çift\nsp² hibritleşme\n\nÖrnekler: BF₃, AlCl₃, NO₃⁻",
            "Polar olmayan", "BF₃, AlCl₃", "sp²", "AX₃",
            listOf(GAtom(0f, 0f, 0f, "B", 0xFFFFB5B5.toInt(), 16f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF4CD964.toInt(), 17f),
                   GAtom(19f, 11f, 0f, "F", 0xFF4CD964.toInt(), 17f),
                   GAtom(-19f, 11f, 0f, "F", 0xFF4CD964.toInt(), 17f)),
            listOf(0 to 1, 0 to 2, 0 to 3)),
        VSEPRGeo("Bükülü", "Bent (2p)", 2, 1, "<120°",
            "2 bağ çifti, 1 yalnız çift\nsp² hibritleşme\n\nÖrnekler: SO₂, O₃",
            "Polar molekül", "SO₂, O₃", "sp²", "AX₂E",
            listOf(GAtom(0f, 0f, 0f, "S", 0xFFFFD60A.toInt(), 18f, true),
                   GAtom(20f, -14f, 0f, "O", 0xFFFF3B30.toInt(), 17f),
                   GAtom(-20f, -14f, 0f, "O", 0xFFFF3B30.toInt(), 17f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("Trigonal-piramit", "Trigonal Pyramidal", 3, 1, "<109.5°",
            "3 bağ çifti, 1 yalnız çift\nsp³ hibritleşme\n\nÖrnekler: NH₃, PCl₃, AsH₃",
            "Polar molekül", "NH₃, PCl₃", "sp³", "AX₃E",
            listOf(GAtom(0f, -5f, 0f, "N", 0xFF3B82F6.toInt(), 16f, true),
                   GAtom(20f, 12f, 0f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-16f, 16f, 0f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-4f, 12f, -18f, "H", 0xFFE8E8E8.toInt(), 12f)),
            listOf(0 to 1, 0 to 2, 0 to 3)),
        VSEPRGeo("Dörtgen", "Tetrahedral", 4, 0, "109.5°",
            "4 bağ çifti, 0 yalnız çift\nsp³ hibritleşme\n\nÖrnekler: CH₄, CCl₄, NH₄⁺",
            "Polar olmayan", "CH₄, CCl₄", "sp³", "AX₄",
            listOf(GAtom(0f, 0f, 0f, "C", 0xFF8E8E93.toInt(), 16f, true),
                   GAtom(18f, 18f, 18f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-18f, -18f, 18f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-18f, 18f, -18f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(18f, -18f, -18f, "H", 0xFFE8E8E8.toInt(), 12f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Bükülü (4p)", "Bent (4p)", 2, 2, "<109.5°",
            "2 bağ çifti, 2 yalnız çift\nsp³ hibritleşme\n\nÖrnekler: H₂O, H₂S, OF₂",
            "Polar molekül", "H₂O, H₂S", "sp³", "AX₂E₂",
            listOf(GAtom(0f, 0f, 0f, "O", 0xFFFF3B30.toInt(), 18f, true),
                   GAtom(20f, -14f, 0f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-20f, -14f, 0f, "H", 0xFFE8E8E8.toInt(), 12f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("T-şekilli", "T-Shaped", 3, 2, "<90°",
            "3 bağ çifti, 2 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: ClF₃, BrF₃",
            "Polar molekül", "ClF₃, BrF₃", "sp³d", "AX₃E₂",
            listOf(GAtom(0f, 0f, 0f, "Cl", 0xFF34C759.toInt(), 18f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3)),
        VSEPRGeo("Testere", "Seesaw", 4, 1, "90°/120°",
            "4 bağ çifti, 1 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: SF₄, TeCl₄",
            "Polar molekül", "SF₄, TeCl₄", "sp³d", "AX₄E",
            listOf(GAtom(0f, 0f, 0f, "S", 0xFFFFD60A.toInt(), 18f, true),
                   GAtom(0f, -20f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(20f, 8f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-10f, 8f, 17f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-10f, 8f, -17f, "F", 0xFF4CD964.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Trigonal-bipyramit", "Trigonal Bipyramidal", 5, 0, "90°/120°",
            "5 bağ çifti, 0 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: PCl₅, AsF₅",
            "Polar olmayan", "PCl₅, AsF₅", "sp³d", "AX₅",
            listOf(GAtom(0f, 0f, 0f, "P", 0xFFFF9500.toInt(), 18f, true),
                   GAtom(0f, -28f, 0f, "Cl", 0xFF34C759.toInt(), 15f),
                   GAtom(0f, 28f, 0f, "Cl", 0xFF34C759.toInt(), 15f),
                   GAtom(26f, 0f, 0f, "Cl", 0xFF34C759.toInt(), 15f),
                   GAtom(-13f, 0f, 22f, "Cl", 0xFF34C759.toInt(), 15f),
                   GAtom(-13f, 0f, -22f, "Cl", 0xFF34C759.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5)),
        VSEPRGeo("Kare-düzlem", "Square Planar", 4, 2, "90°",
            "4 bağ çifti, 2 yalnız çift\nsp³d² hibritleşme\n\nÖrnekler: XeF₄, ICl₄⁻",
            "Polar olmayan", "XeF₄, ICl₄⁻", "sp³d²", "AX₄E₂",
            listOf(GAtom(0f, 0f, 0f, "Xe", 0xFF32ADE6.toInt(), 20f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(0f, 22f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Kare-piramit", "Square Pyramidal", 5, 1, "<90°",
            "5 bağ çifti, 1 yalnız çift\nsp³d² hibritleşme\n\nÖrnekler: BrF₅, ClF₅",
            "Polar molekül", "BrF₅, ClF₅", "sp³d²", "AX₅E",
            listOf(GAtom(0f, 0f, 0f, "Br", 0xFFB45309.toInt(), 18f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(0f, 0f, 22f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(0f, 22f, 0f, "F", 0xFF4CD964.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5)),
        VSEPRGeo("Oktahedral", "Octahedral", 6, 0, "90°",
            "6 bağ çifti, 0 yalnız çift\nsp³d² hibritleşme\n\nÖrnekler: SF₆, PF₆⁻",
            "Polar olmayan", "SF₆, PF₆⁻", "sp³d²", "AX₆",
            listOf(GAtom(0f, 0f, 0f, "S", 0xFFFFD60A.toInt(), 18f, true),
                   GAtom(0f, -24f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(0f, 24f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(24f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-24f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(0f, 0f, 24f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(0f, 0f, -24f, "F", 0xFF4CD964.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6)),
        VSEPRGeo("Doğrusal (3ç)", "Linear (3lp)", 2, 3, "180°",
            "2 bağ çifti, 3 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: XeF₂, I₃⁻",
            "Polar olmayan", "XeF₂, I₃⁻", "sp³d", "AX₂E₃",
            listOf(GAtom(0f, 0f, 0f, "Xe", 0xFF32ADE6.toInt(), 20f, true),
                   GAtom(28f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f),
                   GAtom(-28f, 0f, 0f, "F", 0xFF4CD964.toInt(), 15f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("Pentagonal-bipyramit", "Pentagonal Bipyramidal", 7, 0, "72°/90°",
            "7 bağ çifti, 0 yalnız çift\nsp³d³ hibritleşme\n\nÖrnekler: IF₇, ZrF₇³⁻\n5 equatorial + 2 axial",
            "Polar olmayan", "IF₇", "sp³d³", "AX₇",
            listOf(GAtom(0f, 0f, 0f, "I", 0xFFAF52DE.toInt(), 18f, true),
                   GAtom(0f, -28f, 0f, "F", 0xFF4CD964.toInt(), 14f),
                   GAtom(0f, 28f, 0f, "F", 0xFF4CD964.toInt(), 14f),
                   GAtom(26f, 0f, 0f, "F", 0xFF4CD964.toInt(), 14f),
                   GAtom(8f, 0f, 24f, "F", 0xFF4CD964.toInt(), 14f),
                   GAtom(-21f, 0f, 15f, "F", 0xFF4CD964.toInt(), 14f),
                   GAtom(-21f, 0f, -15f, "F", 0xFF4CD964.toInt(), 14f),
                   GAtom(8f, 0f, -24f, "F", 0xFF4CD964.toInt(), 14f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7)),
        VSEPRGeo("Trigonal-prizma", "Trigonal Prismatic", 6, 0, "90°",
            "6 bağ çifti, 0 yalnız çift\nd³sp² hibritleşme\n\nÖrnekler: Mo(SMe₂)₆, W(CH₃)₆\nPrizmatik geometri",
            "Polar olmayan", "Mo(SMe₂)₆", "d³sp²", "AX₆ (prizma)",
            listOf(GAtom(0f, 0f, 0f, "Mo", 0xFF6B6B6B.toInt(), 18f, true),
                   GAtom(18f, -10f, 15f, "S", 0xFFFFD60A.toInt(), 15f),
                   GAtom(-18f, -10f, 15f, "S", 0xFFFFD60A.toInt(), 15f),
                   GAtom(0f, -10f, -20f, "S", 0xFFFFD60A.toInt(), 15f),
                   GAtom(18f, 10f, 15f, "S", 0xFFFFD60A.toInt(), 15f),
                   GAtom(-18f, 10f, 15f, "S", 0xFFFFD60A.toInt(), 15f),
                   GAtom(0f, 10f, -20f, "S", 0xFFFFD60A.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6)),
        VSEPRGeo("Kare-antiprizma", "Square Antiprismatic", 8, 0, "Variable",
            "8 bağ çifti, 0 yalnız çift\nd⁴sp³ hibritleşme\n\nÖrnekler: TaF₈³⁻, ZrF₈⁴⁻\n2 kare yüzey birbirine 45° döndürülmüş",
            "Polar olmayan", "TaF₈³⁻", "d⁴sp³", "AX₈ (antiprizma)",
            listOf(GAtom(0f, 0f, 0f, "Ta", 0xFF8E8E93.toInt(), 18f, true),
                   GAtom(16f, -16f, 16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(-16f, -16f, 16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(-16f, 16f, 16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(16f, 16f, 16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(22f, 0f, -16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(0f, -22f, -16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(-22f, 0f, -16f, "F", 0xFF4CD964.toInt(), 13f),
                   GAtom(0f, 22f, -16f, "F", 0xFF4CD964.toInt(), 13f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8)),
        VSEPRGeo("Dodekahedral", "Dodecahedral", 8, 0, "Variable",
            "8 bağ çifti, 0 yalnız çift\n\nÖrnekler: Mo(CN)₈⁴⁻\n8 köşeli dodekaedron",
            "Polar olmayan", "Mo(CN)₈⁴⁻", "d⁴sp³", "AX₈ (dodekaedron)",
            listOf(GAtom(0f, 0f, 0f, "Mo", 0xFF6B6B6B.toInt(), 18f, true),
                   GAtom(20f, -12f, 10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(-20f, -12f, 10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(-20f, 12f, -10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(20f, 12f, -10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(12f, -20f, -10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(-12f, -20f, -10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(-12f, 20f, 10f, "C", 0xFF8E8E93.toInt(), 13f),
                   GAtom(12f, 20f, 10f, "C", 0xFF8E8E93.toInt(), 13f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8)),
        VSEPRGeo("Tricapped-prizma", "Tricapped Trigonal Prismatic", 9, 0, "Variable",
            "9 bağ çifti, 0 yalnız çift\n\nÖrnekler: ReH₉²⁻, TcH₉²⁻\n9 bağlı en karmaşık geometri",
            "Polar olmayan", "ReH₉²⁻", "d⁴sp³", "AX₉",
            listOf(GAtom(0f, 0f, 0f, "Re", 0xFF9E9EA3.toInt(), 18f, true),
                   GAtom(18f, -10f, 14f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-18f, -10f, 14f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(0f, -10f, -18f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(18f, 10f, 14f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(-18f, 10f, 14f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(0f, 10f, -18f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(0f, -24f, 0f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(0f, 24f, 0f, "H", 0xFFE8E8E8.toInt(), 12f),
                   GAtom(0f, 0f, 26f, "H", 0xFFE8E8E8.toInt(), 12f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8, 0 to 9)),
        VSEPRGeo("Testere (3ç)", "Seesaw (3lp)", 4, 3, "Variable",
            "4 bağ çifti, 3 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: ClO₄⁻(tahmini)\nKarmaşık yalnız çift düzeni",
            "Değişken", "—", "sp³d", "AX₄E₃",
            listOf(GAtom(0f, 0f, 0f, "Cl", 0xFF34C759.toInt(), 18f, true),
                   GAtom(0f, -22f, 0f, "O", 0xFFFF3B30.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "O", 0xFFFF3B30.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "O", 0xFFFF3B30.toInt(), 15f),
                   GAtom(0f, 0f, 22f, "O", 0xFFFF3B30.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Küre", "Spherical", 0, 0, "—",
            "Tüm elektronlar yalnız çift\nHiç bağ yok\n\nÖrnekler: Nadir gazlar (He, Ne, Ar)\nTam simetrik küresel yapı",
            "Polar olmayan", "He, Ne, Ar", "—", "AX₀",
            listOf(GAtom(0f, 0f, 0f, "He", 0xFF4DD0E1.toInt(), 22f, true)),
            emptyList())
    )

    private var currentIndex = 0
    private lateinit var geoView: Geo3DView
    private lateinit var root: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_molekul_geometri, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        root = view
        val container = view.findViewById<FrameLayout>(R.id.geo_container)
        val sv = view as android.widget.ScrollView
        val titleTv = (((sv.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? TextView
        titleTv?.let { AnimUtils.gradientTitle(it) }

        val column = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(column, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        geoView = Geo3DView(requireContext())
        column.addView(geoView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (340 * resources.displayMetrics.density).toInt()))

        // navigasyon çubuğu
        val navRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val prevBtn = navButton(R.drawable.ic_nav_prev)
        val nextBtn = navButton(R.drawable.ic_nav_next)
        val tvName = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFE6EEF8.toInt()); textSize = 17f; setTypeface(null, android.graphics.Typeface.BOLD)
        }
        prevBtn.setOnClickListener { AnimUtils.press(prevBtn); currentIndex = (currentIndex - 1 + geos.size) % geos.size; showGeo(tvName) }
        nextBtn.setOnClickListener { AnimUtils.press(nextBtn); currentIndex = (currentIndex + 1) % geos.size; showGeo(tvName) }
        navRow.addView(prevBtn); navRow.addView(tvName); navRow.addView(nextBtn)
        column.addView(navRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // özellik çipleri
        val chipsRow = TextView(requireContext()).apply {
            setTextColor(0xFFBFD0E8.toInt()); textSize = 12f
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.bg_lewis_info)
            setPadding(30, 14, 30, 14)
        }
        column.addView(chipsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // bilgi paneli
        val info = TextView(requireContext()).apply {
            setTextColor(0xFFBFD0E8.toInt()); textSize = 13f
            setLineSpacing(4f, 1f)
            setBackgroundResource(R.drawable.bg_lewis_info)
            setPadding(30, 18, 30, 18)
        }
        column.addView(info, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        geoView.onAtomSelected = { atom, idx ->
            val g = geos[currentIndex]
            val bagSayisi = g.bonds.count { it.first == idx || it.second == idx }
            val merkez = if (atom.isCenter) "  [Merkez atom]" else ""
            info.text = "${atom.symbol} atomu$merkez\nBu atoma $bagSayisi bağ bağlı"
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            HelpDialog.show(requireContext(), "Molekül Geometrisi", """
                <b>Ne işe yarar?</b> VSEPR geometrilerini 3 boyutlu, döndürülebilir gösterir.<br/><br/>
                <b>3B Etkileşim:</b><br/>
                • Sürükle: Molekülü döndür / çevir.<br/>
                • Çift parmak: Yakınlaştır / uzaklaştır.<br/>
                • Parmağını kaldır → otomatik döndürme başlar.<br/><br/>
                <b>Atom seçme:</b> Bir atoma dokun — turkuaz halkayla vurgulanır ve alt panelde o atomun bağ sayısı görünür.<br/><br/>
                <b>Renkler:</b> Koyu gri C, kırmızı O, mavi N, beyaz H, yeşil F, sarı S, turuncu P, mor I, camgöbeği Xe.<br/><br/>
                ◀ ▶ butonlarıyla toplam ${geos.size} geometri arasında gez.
            """.trimIndent())
        }

        showGeo(tvName)
    }

    private fun navButton(resId: Int): ImageButton {
        val b = ImageButton(requireContext())
        b.setImageResource(resId)
        b.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(0xFFA78BFA.toInt()); cornerRadius = 24f
        }
        b.setPadding(30, 30, 30, 30)
        b.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        b.layoutParams = LinearLayout.LayoutParams(96, 96)
        b.elevation = 0f
        return b
    }

    private fun showGeo(tvName: TextView) {
        val g = geos[currentIndex]
        tvName.text = "${g.name} (${g.nameEn})"
        val container = root.findViewById<FrameLayout>(R.id.geo_container)
        val column = container.getChildAt(0) as LinearLayout
        val chipsRow = column.getChildAt(2) as TextView
        chipsRow.text = "📐 ${g.name}      ∠ ${g.angle}      🧲 ${g.polar}      ⚗ ${g.hybrid}"
        val info = column.getChildAt(3) as TextView
        info.text = "${g.info}\n\nVSEPR: ${g.vseprType}   •   Bağ: ${g.bondPairs}   Yalnız: ${g.lonePairs}\n\nÖrnek: ${g.example}"
        geoView.setGeo(g)
    }
}

