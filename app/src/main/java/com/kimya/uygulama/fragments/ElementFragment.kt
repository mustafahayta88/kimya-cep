package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.ElementData
import com.kimya.uygulama.utils.KimyaData
import kotlin.math.*
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════
//  AtomPlaceholderView — Boş durumdaki animasyonlu atom
// ═══════════════════════════════════════════════════════════════
class AtomPlaceholderView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {
    private var t = 0f
    private val sparks = mutableListOf<Spark>()
    private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var color: Int, var size: Float)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        addUpdateListener { t = it.animatedValue as Float; updateSparks(); invalidate() }
    }
    init { animator.start() }

    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    private fun updateSparks() {
        val it = sparks.iterator(); while (it.hasNext()) { val s = it.next(); s.x += s.vx; s.y += s.vy; s.vx *= 0.96f; s.vy *= 0.96f; s.life -= 0.02f; if (s.life <= 0f) it.remove() }
        if (sparks.size < 30 && Random.nextFloat() < 0.3f) {
            val cx = width / 2f; val cy = height / 2f; val r = min(width, height) / 2f - 20f
            val ang = Random.nextFloat() * 2f * PI.toFloat(); val d = r * (0.3f + Random.nextFloat() * 0.7f)
            sparks += Spark(cx + cos(ang) * d, cy + sin(ang) * d * 0.4f, cos(ang + 1.5f) * 1.5f, sin(ang + 1.5f) * 1.5f, 1f, intArrayOf(0xFF4DD0E1.toInt(), 0xFF81C784.toInt(), 0xFFFFA500.toInt(), 0xFFFF44AA.toInt()).random(), 2f + Random.nextFloat() * 3f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val maxR = min(w, h) / 2f - 20f

        // Dış parlama
        val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, maxR * 1.3f,
                intArrayOf(Color.argb(0, 0, 240, 255), Color.argb(25, 0, 240, 255), Color.argb(0, 0, 240, 255)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, maxR * 1.3f, glowP)

        // Yörünge halkaları (5 katman)
        val ringData = listOf(
            Triple(0xFF4DD0E1.toInt(), 0f, 0.95f), Triple(0xFF81C784.toInt(), 60f, 0.78f),
            Triple(0xFFFF44AA.toInt(), -30f, 0.62f), Triple(0xFFFFA500.toInt(), 45f, 0.48f),
            Triple(0xFFB388FF.toInt(), -15f, 0.35f)
        )
        for ((col, ang, rf) in ringData) {
            val r = maxR * rf
            val tilt = 0.3f + 0.05f * sin(t * 3f + rf * 5f)
            canvas.save(); canvas.rotate(ang + t * (20f - rf * 15f), cx, cy)
            val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = Paint.Style.STROKE; strokeWidth = 1.5f; alpha = (100 + 50 * sin(t * 2f + rf * 3f)).toInt() }
            canvas.drawOval(cx - r, cy - r * tilt, cx + r, cy + r * tilt, rp)

            // Elektron
            val eAng = Math.toRadians((t * (360f + rf * 200f)).toDouble())
            val ex = cx + r * cos(eAng).toFloat()
            val ey = cy + r * tilt * sin(eAng).toFloat()
            val depth = sin(eAng).toFloat() * 0.5f + 0.5f
            val eR = 4f + depth * 3f
            val egp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((50 + depth * 80).toInt(), Color.red(col), Color.green(col), Color.blue(col)); style = Paint.Style.FILL }
            canvas.drawCircle(ex, ey, eR + 4f, egp)
            val ebp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(ex, ey, eR, intArrayOf(Color.WHITE, col), null, Shader.TileMode.CLAMP); style = Paint.Style.FILL }
            canvas.drawCircle(ex, ey, eR, ebp)
            canvas.restore()
        }

        // Parçacıklar
        for (s in sparks) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.color; alpha = (s.life * 200).toInt().coerceIn(0, 200); style = Paint.Style.FILL }
            canvas.drawCircle(s.x, s.y, s.size * s.life, sp)
        }

        // Çekirdek — 3D gradient + glow
        val nR = maxR * 0.12f
        val ngp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, nR * 2f, intArrayOf(Color.argb(180, 255, 80, 80), Color.argb(0, 255, 40, 40)), null, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, nR * 2f, ngp)
        val nbp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - nR * 0.3f, cy - nR * 0.3f, nR * 1.4f,
                intArrayOf(Color.rgb(255, 160, 160), Color.rgb(200, 50, 50), Color.rgb(120, 20, 20)), null, Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, nR, nbp)
        canvas.drawCircle(cx, cy, nR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f })

        // Çekirdek parlak nokta
        canvas.drawCircle(cx - nR * 0.25f, cy - nR * 0.25f, nR * 0.25f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF.toInt(); style = Paint.Style.FILL })

        // "e⁻" etiketi
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = nR * 0.8f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.MONOSPACE }
        canvas.drawText("e⁻", cx, cy + nR * 0.3f, tp)
    }
}

// ═══════════════════════════════════════════════════════════════
//  AtomModelView — 3D elektron modeli (dokunarak döndürme)
// ═══════════════════════════════════════════════════════════════
class AtomModelView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {
    var semIol: String? = null; var atomNo: Int = 0; var elektronConfig: String? = null; var nucColor: Int = 0xFFFF6060.toInt()
    private var t = 0f; private var rotX = 0.35f; private var rotY = 0f; private var autoRot = true
    private var lastTx = 0f; private var lastTy = 0f; private var drag = false
    private val sparks = mutableListOf<Spark>()
    private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var color: Int, var size: Float)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2500; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        addUpdateListener { t = it.animatedValue as Float; updateSparks(); if (autoRot) { rotY += 0.008f; postInvalidateOnAnimation() }; invalidate() }
    }
    init { isClickable = true; isFocusable = true; animator.start() }

    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    private fun updateSparks() {
        val it = sparks.iterator(); while (it.hasNext()) { val s = it.next(); s.x += s.vx; s.y += s.vy; s.vx *= 0.95f; s.vy *= 0.95f; s.life -= 0.025f; if (s.life <= 0f) it.remove() }
    }

    private fun emitSparks(x: Float, y: Float, color: Int, n: Int) {
        for (i in 0 until n) { val a = Random.nextFloat() * 2f * PI.toFloat(); val s = 1.5f + Random.nextFloat() * 4f
            sparks += Spark(x, y, cos(a) * s, sin(a) * s, 1f, color, 2f + Random.nextFloat() * 2f) }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; drag = true; autoRot = false; (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> { if (drag && e.pointerCount == 1) { rotY += (e.x - lastTx) * 0.02f; rotX += (e.y - lastTy) * 0.02f; rotX = rotX.coerceIn(-1.4f, 1.4f); invalidate() }; lastTx = e.x; lastTy = e.y }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false); drag = false; autoRot = true; postInvalidateOnAnimation() }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sym = semIol ?: return; val ec = elektronConfig ?: return
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2f; val cy = h / 2f
        val maxR = min(w, h) / 2f - 24f
        val shells = parseElectronShells(ec, atomNo); if (shells.isEmpty()) return

        val sint = sin(rotX).toFloat(); val cost = cos(rotX).toFloat()

        // Arka plan parlama
        val bgG = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, maxR,
                intArrayOf(Color.argb(15, Color.red(nucColor), Color.green(nucColor), Color.blue(nucColor)), Color.argb(0, 0, 0, 0)), null, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, maxR, bgG)

        // Parçacıklar (çekirdek etrafında)
        if (sparks.size < 20 && Random.nextFloat() < 0.15f) {
            val ang = Random.nextFloat() * 2f * PI.toFloat(); val d = maxR * 0.12f
            emitSparks(cx + cos(ang) * d, cy + sin(ang) * d, nucColor, 3)
        }
        for (s in sparks) { val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.color; alpha = (s.life * 220).toInt().coerceIn(0, 220); style = Paint.Style.FILL }; canvas.drawCircle(s.x, s.y, s.size * s.life, sp) }

        val nucR = maxR * 0.1f + 12f

        // Yörünge halkaları + elektronlar
        val shellColors = intArrayOf(
            0xFF00C8FF.toInt(), 0xFF00FFB4.toInt(), 0xFFB464FF.toInt(),
            0xFFFFC832.toInt(), 0xFFFF6496.toInt(), 0xFF64FFC8.toInt(), 0xFFC8C8FF.toInt()
        )
        for (i in shells.indices) {
            val r = maxR * (i + 1).toFloat() / (shells.size + 1).toFloat() + nucR + 10f
            val col = shellColors[i % shellColors.size]
            val rY = r * cost
            // Halka çizimi
            val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = Paint.Style.STROKE; strokeWidth = 1.8f; alpha = (120 + 30 * sin(t * 2f + i)).toInt() }
            canvas.drawOval(cx - r, cy - rY, cx + r, cy + rY, ringP)
            // Elektronlar
            val cnt = min(shells[i], 32); val spd = 1.2f + i * 0.5f
            for (e in 0 until cnt) {
                val ang = Math.toRadians((-90.0 + e * (360.0 / cnt) + t * 360.0 * spd + Math.toDegrees(rotY.toDouble())))
                val ex = cx + r * cos(ang).toFloat()
                val ey = cy + rY * sin(ang).toFloat()
                val depth = (sin(ang)).toFloat() * 0.5f + 0.5f
                val eR = 3.5f + depth * 3.5f
                // Glow
                val egp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb((40 + depth * 70).toInt(), 57, 255, 20); style = Paint.Style.FILL }
                canvas.drawCircle(ex, ey, eR + 4f, egp)
                // Elektron gövdesi
                val ebp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(ex, ey, eR, intArrayOf(Color.WHITE, Color.rgb(57, 255, 20)), null, Shader.TileMode.CLAMP); style = Paint.Style.FILL }
                canvas.drawCircle(ex, ey, eR, ebp)
            }
            // Katman etiketi
            val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, Color.red(col), Color.green(col), Color.blue(col)); textSize = 11f; textAlign = Paint.Align.CENTER }
            canvas.drawText("n=${i+1} (${shells[i]}e⁻)", cx, cy - abs(rY) - 10f, lp)
        }

        // Çekirdek — 3D glow + gradient
        val ngp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, nucR * 2f,
                intArrayOf(Color.argb(160, Color.red(nucColor), Color.green(nucColor), Color.blue(nucColor)),
                           Color.argb(0, Color.red(nucColor), Color.green(nucColor), Color.blue(nucColor))),
                null, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, nucR * 2f, ngp)
        val nbp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - nucR * 0.3f, cy - nucR * 0.3f, nucR * 1.3f,
                intArrayOf(
                    Color.rgb((Color.red(nucColor) + 60).coerceAtMost(255), (Color.green(nucColor) + 60).coerceAtMost(255), (Color.blue(nucColor) + 60).coerceAtMost(255)),
                    Color.rgb((Color.red(nucColor) - 50).coerceAtLeast(0), (Color.green(nucColor) - 50).coerceAtLeast(0), (Color.blue(nucColor) - 50).coerceAtLeast(0))),
                null, Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, nucR, nbp)
        canvas.drawCircle(cx, cy, nucR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f })
        // Parlam nokta
        canvas.drawCircle(cx - nucR * 0.2f, cy - nucR * 0.2f, nucR * 0.2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF.toInt(); style = Paint.Style.FILL })
        // Sembol
        val stp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = nucR * 0.85f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.MONOSPACE }
        canvas.drawText(sym, cx, cy + nucR * 0.35f, stp)
        // Z
        val ztp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAABBCC.toInt(); textSize = 13f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        canvas.drawText("Z=$atomNo", cx, cy + nucR + 24f, ztp)
    }

    private fun parseElectronShells(config: String, atomNo: Int): List<Int> {
        var cfg = config.trim(); var base = 0
        for ((core, cnt) in mapOf("[He]" to 2, "[Ne]" to 10, "[Ar]" to 18, "[Kr]" to 36, "[Xe]" to 54, "[Rn]" to 86)) {
            if (cfg.startsWith(core)) { base = cnt; cfg = cfg.removePrefix(core).trim(); break }
        }
        val shells = mutableListOf<Int>(); var rem = base; var n = 1
        while (rem > 0) { val c = min(2 * n * n, rem); shells.add(c); rem -= c; n++ }
        val sub = mutableMapOf<Int, Int>()
        for (m in Regex("""(\d+)[spd](\d+)""").findAll(cfg)) { val s = m.groupValues[1].toInt(); val c = m.groupValues[2].toInt(); sub[s] = (sub[s] ?: 0) + c }
        for ((sn, cnt) in sub) { while (shells.size < sn) shells.add(0); shells[sn-1] = shells[sn-1] + cnt }
        return shells
    }
}

// ═══════════════════════════════════════════════════════════════
//  Element3DView — 3 boyutlu küre element kartı (doğal tonlar)
// ═══════════════════════════════════════════════════════════════
class Element3DView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {
    var elemSym = "C"; var elemColor = 0xFF777777.toInt(); var elemZ = 6; var elemName = "Karbon"
    private var animT = 0f; private var pulse = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2400; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animT = it.animatedValue as Float; pulse = sin(animT * PI.toFloat() * 1.5f); invalidate() }
    }
    init { animator.start() }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    private fun darken(color: Int, factor: Float): Int {
        return Color.rgb(
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        )
    }
    private fun brighten(color: Int, amount: Int): Int {
        return Color.rgb(
            (Color.red(color) + amount).coerceIn(0, 255),
            (Color.green(color) + amount).coerceIn(0, 255),
            (Color.blue(color) + amount).coerceIn(0, 255)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat(); val r = min(w, h) / 2f - 10f
        val cx = w / 2f; val cy = h / 2f + 2f

        // Gölge (yere düşen)
        val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy + r + 8f, r * 0.7f,
                intArrayOf(0x30000000, 0x08000000, 0x00000000), null, Shader.TileMode.CLAMP)
        }
        canvas.drawOval(cx - r * 0.7f, cy + r + 2f, cx + r * 0.7f, cy + r + 16f, shadowP)

        // Dış ambient glow — çok hafif
        val agp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, r * 1.25f,
                intArrayOf(0x00000000, Color.argb((8 + pulse * 4).toInt(), Color.red(elemColor), Color.green(elemColor), Color.blue(elemColor)), 0x00000000),
                floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r * 1.25f, agp)

        // Ana küre — koyu taban, üstü aydınlık (doğal 3D)
        val darkBase = darken(elemColor, 0.35f)
        val midTone = darken(elemColor, 0.7f)
        val lightTone = brighten(elemColor, 25)
        val highlight = brighten(elemColor, 55)

        val sphereP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - r * 0.22f, cy - r * 0.28f, r * 1.15f,
                intArrayOf(highlight, lightTone, midTone, darkBase),
                floatArrayOf(0f, 0.2f, 0.55f, 1f), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, sphereP)

        // Kenar gölgesi (alt kısım koyulaşma)
        val edgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy + r * 0.15f, r,
                intArrayOf(0x00000000, 0x00000000, Color.argb(40, 0, 0, 0)),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, edgeP)

        // İnce kenar çizgisi
        canvas.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(25, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1f })

        // Üst highlight — yumuşak, geniş
        val hlP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - r * 0.2f, cy - r * 0.3f, r * 0.55f,
                intArrayOf(Color.argb(50, 255, 255, 255), Color.argb(0, 255, 255, 255)),
                null, Shader.TileMode.CLAMP)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - r * 0.2f, cy - r * 0.3f, r * 0.55f, hlP)

        // Küçük parlak nokta
        canvas.drawCircle(cx - r * 0.15f, cy - r * 0.22f, r * 0.08f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 255, 255, 255); style = Paint.Style.FILL })

        // Sembol — koyu gölge + açık metin
        val shadowTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 0, 0, 0); textSize = r * 0.62f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText(elemSym, cx + 1f, cy + r * 0.22f + 1.5f, shadowTxt)
        val stp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(230, 255, 255, 255); textSize = r * 0.62f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText(elemSym, cx, cy + r * 0.22f, stp)

        // Atom no — üstte, sade
        val zp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(100, 255, 255, 255); textSize = r * 0.19f; textAlign = Paint.Align.CENTER }
        canvas.drawText("$elemZ", cx, cy - r * 0.42f, zp)
    }
}

// ═══════════════════════════════════════════════════════════════
//  ElementFragment
// ═══════════════════════════════════════════════════════════════
class ElementFragment : Fragment() {

    private val quickElements = listOf("H", "He", "C", "N", "O", "Na", "Mg", "Al", "Si", "S", "Cl", "K", "Ca", "Fe", "Cu", "Ag", "Au")
    private val categoryInfo = listOf(
        Triple("Alkali Metal", 0xFFFF6347.toInt(), "M+"),
        Triple("Toprak Alkali", 0xFFFF8C00.toInt(), "M2+"),
        Triple("Geçiş Metali", 0xFF00CED1.toInt(), "Mn+"),
        Triple("Yarı Metal", 0xFFDA70D6.toInt(), "~"),
        Triple("Ametal", 0xFF81C784.toInt(), "-"),
        Triple("Soy Gaz", 0xFFFF69B4.toInt(), "0"),
        Triple("Lantanit", 0xFF32CD32.toInt(), "Ln"),
        Triple("Aktinit", 0xFFFFA500.toInt(), "Ac")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_element, container, false)
        val arama = v.findViewById<EditText>(R.id.el_arama)
        val emptyState = v.findViewById<LinearLayout>(R.id.el_empty)
        val detailState = v.findViewById<LinearLayout>(R.id.el_detail)
        val propBars = v.findViewById<LinearLayout>(R.id.el_prop_bars)
        val legendRow = v.findViewById<LinearLayout>(R.id.el_legend)
        val quickGrid = v.findViewById<GridLayout>(R.id.el_quick_grid)

        fun propBar(label: String, value: Double, maxVal: Double, renk: Int): View {
            val lay = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 6, 0, 6); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            val txt = TextView(requireContext()).apply { text = label; setTextColor(0xFFCCCCCC.toInt()); textSize = 12f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val valTxt = TextView(requireContext()).apply { text = if (value == value.toLong().toDouble()) value.toLong().toString() else "%.3f".format(value); setTextColor(renk); textSize = 12f; typeface = Typeface.MONOSPACE }
            row.addView(txt); row.addView(valTxt); lay.addView(row)
            val barBg = LinearLayout(requireContext()).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 10); setPadding(0, 3, 0, 0); background = GradientDrawable().apply { setColor(0x33FFFFFF.toInt()); cornerRadius = 5f } }
            val fraksiyon = if (maxVal > 0) (value / maxVal).toFloat().coerceIn(0f, 1f) else 0f
            val maxPx = (220 * requireContext().resources.displayMetrics.density).toInt()
            val barW = (fraksiyon * maxPx).toInt().coerceAtMost(maxPx)
            val barFill = View(requireContext()).apply { layoutParams = LinearLayout.LayoutParams(0, 8); background = GradientDrawable().apply { setColor(renk); cornerRadius = 5f } }
            barBg.addView(barFill); lay.addView(barBg)
            ValueAnimator.ofInt(0, barW).apply { duration = 600L; startDelay = 80L; addUpdateListener { val w = it.animatedValue as Int; (barFill.layoutParams as LinearLayout.LayoutParams).width = w; barFill.requestLayout() }; start() }
            return lay
        }

        fun showElement(q: String) {
            val el = KimyaData.elementBul(q) ?: return
            emptyState.visibility = View.GONE; detailState.visibility = View.VISIBLE
            val atomCanvas = v.findViewById<AtomModelView>(R.id.el_atom_canvas)
            atomCanvas.semIol = el.semIol; atomCanvas.atomNo = el.atomNo; atomCanvas.elektronConfig = el.elektron; atomCanvas.nucColor = KimyaData.elementRengi(el.tur); atomCanvas.invalidate()
            val color = KimyaData.elementRengi(el.tur)
            v.findViewById<TextView>(R.id.el_sym_badge).apply { text = el.semIol; background = GradientDrawable().apply { setColor(color); cornerRadius = 16f } }
            v.findViewById<TextView>(R.id.el_name).text = "${el.adi} (${el.semIol})"
            v.findViewById<TextView>(R.id.el_type_badge).apply { text = el.tur; background = GradientDrawable().apply { setColor(color and 0x55FFFFFF.toInt()); cornerRadius = 10f; setStroke(1, color) } }
            val grupAdlari = mapOf(1 to "Alkali Metaller", 2 to "Toprak Alkali", 3 to "Skandiyum Grubu", 4 to "Titan Grubu", 5 to "Vanadyum Grubu", 6 to "Krom Grubu", 7 to "Manganez Grubu", 8 to "Demir Grubu", 9 to "Kobalt Grubu", 10 to "Nikel Grubu", 11 to "Bakır Grubu", 12 to "Çinko Grubu", 13 to "Bor Grubu", 14 to "Karbon Grubu", 15 to "Azot Grubu", 16 to "Oksijen Grubu", 17 to "Halojenler", 18 to "Soy Gazlar")
            v.findViewById<TextView>(R.id.el_subtitle).text = "${grupAdlari[el.grup] ?: ""} | ${el.periyot}. Periyot | ${el.durum}"
            v.findViewById<TextView>(R.id.el_stat_z).text = "${el.atomNo}"; v.findViewById<TextView>(R.id.el_stat_mass).text = "%.2f".format(el.kutle); v.findViewById<TextView>(R.id.el_stat_group).text = "${el.grup}"; v.findViewById<TextView>(R.id.el_stat_period).text = "${el.periyot}"

            propBars.removeAllViews()
            // Atomik & temel özellikler
            if (el.iyonlasmaEnerjisi > 0) propBars.addView(propBar("İyonlaşma Enerjisi (kJ/mol)", el.iyonlasmaEnerjisi, 2500.0, 0xFFFFB400.toInt()))
            if (el.elektronegatiflik > 0) propBars.addView(propBar("Elektronegatiflik (Pauling)", el.elektronegatiflik, 4.0, 0xFF00C8FF.toInt()))
            propBars.addView(propBar("Atom Kütlesi (g/mol)", el.kutle, 300.0, 0xFF81C784.toInt()))
            propBars.addView(propBar("Periyot", el.periyot.toDouble(), 7.0, 0xFFB388FF.toInt()))
            propBars.addView(propBar("Grup", el.grup.toDouble(), 18.0, 0xFFFF0080.toInt()))
            // Fiziksel özellikler
            if (el.erime != 0.0) propBars.addView(propBar("Erime Noktası (°C)", el.erime, 4000.0, 0xFFFF6B6B.toInt()))
            if (el.kaynama != 0.0) propBars.addView(propBar("Kaynama Noktası (°C)", el.kaynama, 6000.0, 0xFF4ECDC4.toInt()))
            if (el.yogunluk > 0) propBars.addView(propBar("Yoğunluk (g/cm³)", el.yogunluk, 23.0, 0xFFA8E6CF.toInt()))
            if (el.ozelIsi > 0) propBars.addView(propBar("Özel Isı (J/g·K)", el.ozelIsi, 15.0, 0xFFFFD93D.toInt()))
            if (el.isilIletkenlik > 0) propBars.addView(propBar("Isıl İletkenlik (W/m·K)", el.isilIletkenlik, 430.0, 0xFF6C5CE7.toInt()))
            if (el.katilik > 0) propBars.addView(propBar("Katılık (Mohs)", el.katilik, 10.0, 0xFFFF8A5C.toInt()))

            v.findViewById<TextView>(R.id.el_electron_config).text = el.elektron
            val valRow = v.findViewById<LinearLayout>(R.id.el_valence_row); valRow.removeAllViews()
            for (v2 in el.valans) { val chip = TextView(requireContext()).apply { text = if (v2 > 0) "+$v2" else "$v2"; textSize = 12f; setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE; setPadding(10, 4, 10, 4); background = GradientDrawable().apply { setColor(0x4400F0FF.toInt()); cornerRadius = 8f; setStroke(1, 0xFF4DD0E1.toInt()) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(4, 2, 4, 2) } }; valRow.addView(chip) }

            // Fiziksel açıklama
            val fizikselText = buildString {
                if (el.manyetik.isNotEmpty()) appendLine("Manyetik: ${el.manyetik}")
                if (el.kirkRefraksiyon > 0) appendLine("Kırılma İndeksi: ${"%.3f".format(el.kirkRefraksiyon)}")
                if (el.fiziksel.isNotEmpty()) appendLine(el.fiziksel)
                if (el.bulunsenYil > 0) appendLine("Bulunuş Yılı: ${el.bulunsenYil}")
                else if (el.bulunsenYil == 0) appendLine("Bulunuş Yılı: Antik Çağ")
                if (el.adinHikayesi.isNotEmpty()) appendLine("Adın Hikayesi: ${el.adinHikayesi}")
            }
            v.findViewById<TextView>(R.id.el_kimyasal_text).text = fizikselText.trimEnd()

            // Kimyasal özellikler — bölümlere ayırarak göster
            val kimText = el.kimyasal
            val bolumler = mutableListOf<Pair<String, String>>()
            // Yükseltgenme
            val yuksMatch = Regex("Yükseltgenme:\\s*([^\\.]+)\\.").find(kimText)
            if (yuksMatch != null) bolumler.add("Yükseltgenme Bas." to yuksMatch.groupValues[1].trim())
            // Bileşikler
            val bilMatch = Regex("Bileşikleri:\\s*([^\\.]+)\\.").find(kimText)
            if (bilMatch != null) bolumler.add("Ana Bileşikler" to bilMatch.groupValues[1].trim())
            // Tepkime
            val tepMatch = Regex("(Suda[^\\.]+\\.\\s*)").find(kimText)
            val tepMatch2 = Regex("(Asitlerle[^\\.]+\\.\\s*)").find(kimText)
            val tepMatch3 = Regex("(Yanarak[^\\.]+\\.\\s*)").find(kimText)
            val tepkimeText = buildString {
                tepMatch?.let { append(it.groupValues[1]) }
                tepMatch2?.let { append(it.groupValues[1]) }
                tepMatch3?.let { append(it.groupValues[1]) }
            }.trim()
            if (tepkimeText.isNotEmpty()) bolumler.add("Tepkime Davranışı" to tepkimeText)
            // Kalan tüm metin
            val cleanText = kimText
                .replace(Regex("Yükseltgenme:\\s*[^\\.]+\\.\\s*"), "")
                .replace(Regex("Bileşikleri:\\s*[^\\.]+\\.\\s*"), "")
                .replace(Regex("Suda[^\\.]+\\.\\s*"), "")
                .replace(Regex("Asitlerle[^\\.]+\\.\\s*"), "")
                .replace(Regex("Yanarak[^\\.]+\\.\\s*"), "")
                .trim()
            if (cleanText.isNotEmpty()) bolumler.add("Detay Bilgi" to cleanText)

            val kimFormatted = buildString {
                for ((baslik, icerik) in bolumler) {
                    appendLine("▸ $baslik")
                    appendLine("  $icerik")
                    appendLine()
                }
            }.trimEnd()
            v.findViewById<TextView>(R.id.el_kullanim_text).text = kimFormatted.ifEmpty { kimText }
        }

        // Kategori legend
        for ((name, color, _) in categoryInfo) { val chip = TextView(requireContext()).apply { text = name; textSize = 10f; setTextColor(Color.WHITE); setPadding(12, 4, 12, 4); background = GradientDrawable().apply { setColor(color and 0x33FFFFFF.toInt()); cornerRadius = 12f; setStroke(1, color) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(4, 2, 4, 2) } }; legendRow.addView(chip) }

        // Hızlı element grid'i — 3D küre kartlar
        for ((idx, sym) in quickElements.withIndex()) {
            val el = KimyaData.elementler[sym] ?: continue
            val color = KimyaData.elementRengi(el.tur)
            val card = Element3DView(requireContext()).apply {
                elemSym = sym; elemColor = color; elemZ = el.atomNo; elemName = el.adi
                layoutParams = GridLayout.LayoutParams().apply { width = 0; height = (64 * resources.displayMetrics.density).toInt(); columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(4, 4, 4, 4) }
                setOnClickListener { AnimUtils.press(this); showElement(sym) }
                alpha = 0f; postDelayed({ AnimUtils.slideUpFade(this, 0); alpha = 0f }, (idx * 50).toLong())
            }
            quickGrid.addView(card)
        }

        v.findViewById<Button>(R.id.el_ara_btn).setOnClickListener { showElement(arama.text.toString().trim()) }
        arama.setOnEditorActionListener { _, _, _ -> showElement(arama.text.toString().trim()); true }
        v.findViewById<Button>(R.id.el_back_btn).setOnClickListener { detailState.visibility = View.GONE; emptyState.visibility = View.VISIBLE; v.findViewById<AtomModelView>(R.id.el_atom_canvas).semIol = null; v.findViewById<AtomModelView>(R.id.el_atom_canvas).invalidate() }
        v.findViewById<Button>(R.id.el_listele_btn).setOnClickListener {
            emptyState.visibility = View.GONE; detailState.visibility = View.GONE
            val ac = v.findViewById<AtomModelView>(R.id.el_atom_canvas); ac.semIol = null; ac.invalidate()
            v.findViewById<TextView>(R.id.el_kimyasal_text).text = KimyaData.elementler.values.sortedBy { it.atomNo }.joinToString("\n") { "${it.atomNo}. ${it.semIol} - ${it.adi} (${"%.2f".format(it.kutle)} g/mol) | ${it.tur}" }
            v.findViewById<TextView>(R.id.el_kullanim_text).text = ""; v.findViewById<TextView>(R.id.el_electron_config).text = ""
            v.findViewById<LinearLayout>(R.id.el_valence_row).removeAllViews(); propBars.removeAllViews()
            v.findViewById<TextView>(R.id.el_sym_badge).text = "#"; v.findViewById<TextView>(R.id.el_name).text = "TÜM ELEMENTLER"
            v.findViewById<TextView>(R.id.el_type_badge).apply { text = "118 Element"; background = GradientDrawable().apply { setColor(0x4400F0FF.toInt()); cornerRadius = 10f } }
            v.findViewById<TextView>(R.id.el_subtitle).text = "Periyodik tablodaki tüm elementler"
            v.findViewById<TextView>(R.id.el_stat_z).text = "118"; v.findViewById<TextView>(R.id.el_stat_mass).text = "-"; v.findViewById<TextView>(R.id.el_stat_group).text = "-"; v.findViewById<TextView>(R.id.el_stat_period).text = "-"
            detailState.visibility = View.VISIBLE
        }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle("Periyodik Tablo").setMessage(buildString {
                appendLine("118 elementin detaylı bilgisi."); appendLine()
                appendLine("• Arama çubuğundan element ara (ad, sembol veya atom no)")
                appendLine("• Popüler elementlere hızlı erişim (3D küre kartlar)"); appendLine()
                appendLine("ATOM MODELİ:"); appendLine("• Elektron katmanlarını yörünge olarak gösterir"); appendLine("• Parmağınla sürükle → atom 3B döner"); appendLine("• Bırakınca otomatik dönmeye devam eder"); appendLine("• Çekirdek rengi element türüne göre değişir"); appendLine()
                appendLine("3B KÜRE KARTLARI:"); appendLine("• Her element kendi rengiyle 3B küre olarak gösterilir"); appendLine("• Gradient ile derinlik efekti"); appendLine("• Titreşen parlama animasyonu"); appendLine()
                appendLine("• Fiziksel özellikler animasyonlu çubuklarla"); appendLine("• '118 Elementi Listele' ile tüm tabloyu gör")
            }).setPositiveButton("Anladım", null).show()
        }

        // Header animasyonu
        v.findViewById<TextView>(R.id.el_baslik)?.let { AnimUtils.gradientTitle(it) }
        listOf(R.id.el_header to 0L, R.id.el_search_card to 60L, R.id.el_empty to 120L).forEach { (id, d) -> v.findViewById<View>(id)?.let { it.alpha = 0f; it.post { AnimUtils.slideUpFade(it, d) } } }
        return v
    }
}

