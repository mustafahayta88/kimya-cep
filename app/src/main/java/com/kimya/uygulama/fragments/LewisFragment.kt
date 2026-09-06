package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextPaint
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// VERİ MODELLERİ
// ---------------------------------------------------------------------------
data class LewisAtom(val el: String, val x: Float, val y: Float, val z: Float, val lonePairs: Int = 0, val fc: Int = 0)
data class LewisBond(val from: Int, val to: Int, val order: Int = 1)
data class LewisMolecule(
    val name: String, val formula: String,
    val info: String = "",
    val valence: Int = 0,
    val geometry: String = "", val angle: String = "", val polar: String = "", val hybrid: String = "",
    val atoms: List<LewisAtom> = emptyList(), val bonds: List<LewisBond> = emptyList(),
    val resonance: Boolean = false, val charge: String = ""
)

// ---------------------------------------------------------------------------
// 3B GÖRÜNTÜLEYİCİ — gelişmiş render
// ---------------------------------------------------------------------------
class Lewis3DView(context: Context) : View(context) {
    private var molecule: LewisMolecule? = null

    private var rotX = 0f
    private var rotY = 0f
    var autoRotate = true
    private var zoom = 1f
    private var lastTx = 0f; private var lastTy = 0f
    private var tMode = 0
    private var pulse = 0f
    var showLonePairs = true
    private val sDetector: ScaleGestureDetector
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    private val atomColors = mapOf(
        "H" to 0xFFE8E8E8.toInt(), "O" to 0xFFFF3B30.toInt(), "N" to 0xFF3B82F6.toInt(),
        "F" to 0xFF4CD964.toInt(), "C" to 0xFF8E8E93.toInt(), "Cl" to 0xFF34C759.toInt(),
        "Br" to 0xFFB45309.toInt(), "S" to 0xFFFFD60A.toInt(), "P" to 0xFFFF9500.toInt(),
        "I" to 0xFFAF52DE.toInt(), "Xe" to 0xFF32ADE6.toInt(), "Be" to 0xFFA8E6CF.toInt(),
        "B" to 0xFFFFB5B5.toInt(), "Na" to 0xFF5AC8FA.toInt()
    )
    private val atomR = mapOf("H" to 10f, "C" to 15f, "O" to 15f, "N" to 15f, "F" to 13f,
        "Cl" to 17f, "Br" to 19f, "S" to 18f, "P" to 18f, "I" to 21f, "Xe" to 20f, "Be" to 12f, "B" to 14f)

    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sphereP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bondGlowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val textP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; isFakeBoldText = true; color = Color.WHITE
    }
    private val labelP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; isFakeBoldText = true; color = Color.rgb(16, 22, 32)
    }
    private val loneP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val loneHaloP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fcP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true; color = Color.rgb(255, 100, 100) }
    private val dashP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }

    private var running = true
    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (autoRotate) { rotY += 0.008f; rotX = 0.35f * sin(rotY * 0.55f) }
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
                MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1 }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tMode = 0
                    handler.postDelayed({ autoRotate = true }, 4000)
                }
            }
            if (e.action == MotionEvent.ACTION_MOVE && tMode == 1 && e.pointerCount == 1) {
                val dx = e.x - lastTx; val dy = e.y - lastTy
                if (abs(dx) > 4 || abs(dy) > 4) tMode = 2
                if (tMode == 2) {
                    rotY += dx * 0.007f; rotX += dy * 0.007f
                    autoRotate = false
                    lastTx = e.x; lastTy = e.y
                    invalidate()
                }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        Choreographer.getInstance().postFrameCallback(frameCb)
    }
    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCb)
        super.onDetachedFromWindow()
    }

    fun setMolecule(mol: LewisMolecule) {
        molecule = mol; rotX = 0.32f; rotY = 0.45f; invalidate()
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

    private fun drawStars(canvas: Canvas, w: Float, h: Float) {
        val starP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until 24) {
            val sx = ((i * 137.51f).toInt() % 1000) / 1000f * w
            val sy = ((i * 229.11f).toInt() % 1000) / 1000f * h
            val tw = 0.6f + ((i * 13) % 30) / 40f
            val a = 18 + ((i * 27) % 30)
            starP.color = Color.argb(a, 165, 205, 255)
            canvas.drawCircle(sx, sy, tw, starP)
        }
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.argb(14, 0, 224, 255); strokeWidth = 1.2f }
        val r = minOf(w, h) * 0.30f
        for (i in 1..3) {
            val rr = r * i / 3f
            canvas.drawOval(cx - rr, cy - rr * 0.28f, cx + rr, cy + rr * 0.28f, gp)
        }
        for (i in -2..2) {
            canvas.drawLine(cx - r, cy + i * r * 0.14f, cx + r, cy + i * r * 0.14f, gp)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val bg = LinearGradient(0f, 0f, 0f, h, intArrayOf(0xFF090D18.toInt(), 0xFF101A2C.toInt(), 0xFF090D18.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = bg })

        // radyal turkuaz sis
        val neb = RadialGradient(w / 2f, h * 0.44f, minOf(w, h) * 0.7f,
            intArrayOf(0x1400E0FF.toInt(), 0x0A00E0FF.toInt(), 0x00000000.toInt()),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, Paint().apply { shader = neb })

        drawStars(canvas, w, h)

        val mol = molecule ?: return
        val cx = w / 2f; val cy = h * 0.46f

        // perspektif izdüşüm
        val cr = cos(rotX); val sr = sin(rotX)
        val cp = cos(rotY); val sp = sin(rotY)
        val maxR = (mol.atoms.map { sqrt(it.x * it.x + it.y * it.y + it.z * it.z) }.maxOrNull() ?: 50f).coerceAtLeast(40f)
        val proj = mol.atoms.map { a ->
            val x1 = a.x * cp + a.z * sp
            val z1 = -a.x * sp + a.z * cp
            val y1 = a.y
            val y2 = y1 * cr - z1 * sr
            val z2 = y1 * sr + z1 * cr
            val persp = 1.0f / (1.0f + z2 * 0.008f)
            val sc = minOf(w, h) * 0.27f / maxR * zoom * persp
            Triple(cx + x1 * sc, cy + y2 * sc, z2)
        }

        drawGrid(canvas, w, h, cx, h * 0.88f)

        // bağlar — derinlik sıralı
        val bondOrder = mol.bonds.withIndex().sortedByDescending {
            val p1 = proj[it.value.from]; val p2 = proj[it.value.to]
            (p1.third + p2.third) * 0.5f
        }
        for ((_, b) in bondOrder) {
            if (b.from >= proj.size || b.to >= proj.size) continue
            val p1 = proj[b.from]; val p2 = proj[b.to]
            val sx1 = p1.first.toFloat(); val sy1 = p1.second.toFloat()
            val sx2 = p2.first.toFloat(); val sy2 = p2.second.toFloat()
            val zavg = (p1.third + p2.third) * 0.5f
            val depthF = (1.0f + zavg * 0.045f).coerceIn(0.5f, 1.5f)
            val alpha = ((0.35f + depthF * 0.45f) * 255).toInt()
            val base = if (b.order == 2) Color.rgb(0, 210, 255) else if (b.order == 3) Color.rgb(255, 110, 190) else Color.rgb(178, 192, 210)
            val wd = 3.4f * depthF

            // bağ parlaması
            bondGlowP.color = Color.argb(alpha / 4, Color.red(base), Color.green(base), Color.blue(base))
            bondGlowP.strokeWidth = wd * 2.0f
            canvas.drawLine(sx1, sy1, sx2, sy2, bondGlowP)

            bondP.color = Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
            bondP.strokeWidth = wd
            if (b.order == 1) {
                canvas.drawLine(sx1, sy1, sx2, sy2, bondP)
            } else {
                val dx = sx2 - sx1; val dy = sy2 - sy1
                val len = sqrt(dx * dx + dy * dy)
                if (len < 1f) continue
                val nx = -dy / len * 3.2f * depthF; val ny = dx / len * 3.2f * depthF
                if (b.order == 2) {
                    canvas.drawLine(sx1 + nx, sy1 + ny, sx2 + nx, sy2 + ny, bondP)
                    canvas.drawLine(sx1 - nx, sy1 - ny, sx2 - nx, sy2 - ny, bondP)
                } else {
                    canvas.drawLine(sx1, sy1, sx2, sy2, bondP)
                    canvas.drawLine(sx1 + nx * 1.3f, sy1 + ny * 1.3f, sx2 + nx * 1.3f, sy2 + ny * 1.3f, bondP)
                    canvas.drawLine(sx1 - nx * 1.3f, sy1 - ny * 1.3f, sx2 - nx * 1.3f, sy2 - ny * 1.3f, bondP)
                }
            }
            if (mol.resonance && b.order >= 2) {
                dashP.color = Color.argb((130 + (80 * sin(pulse)).toInt()).coerceIn(70, 230), 255, 205, 90)
                dashP.strokeWidth = wd * 0.9f
                dashP.pathEffect = DashPathEffect(floatArrayOf(9f, 7f), -pulse * 28f)
                canvas.drawLine(sx1, sy1, sx2, sy2, dashP)
                dashP.pathEffect = null
            }
        }

        // atomlar — derinlik sıralı
        val sorted = proj.withIndex().sortedByDescending { it.value.third }
        for ((idx, p) in sorted) {
            val a = mol.atoms[idx]
            val sx = p.first.toFloat(); val sy = p.second.toFloat(); val sz = p.third
            val r = atomR[a.el] ?: 15f
            val depthF = (1.0f + sz * 0.04f).coerceIn(0.6f, 1.5f)
            val rad = r * (minOf(w, h) * 0.0065f) * zoom * depthF
            val baseC = atomColors[a.el] ?: 0xFF8E8E93.toInt()

            // dış parıltı
            val glowA = (14 + (12 * sin(pulse + idx * 0.7f)).toInt()).coerceIn(8, 32)
            glowP.color = Color.argb(glowA, Color.red(baseC), Color.green(baseC), Color.blue(baseC))
            canvas.drawCircle(sx, sy, rad * 1.5f, glowP)

            // ışıklı küre (speküler vurgu)
            val hiX = sx - rad * 0.38f; val hiY = sy - rad * 0.42f
            sphereP.shader = RadialGradient(hiX, hiY, rad * 1.6f,
                intArrayOf(lighten(baseC, 0.65f), baseC, darken(baseC, 0.3f)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(sx, sy, rad, sphereP)

            // dış halka
            rimP.color = Color.argb((50 + (50 * depthF).toInt()), 255, 255, 255)
            rimP.strokeWidth = 1.0f
            canvas.drawCircle(sx, sy, rad, rimP)

            // yalnız çiftler — orbital bulutu + sade ikiz nokta
            if (showLonePairs && a.lonePairs > 0) {
                var dirX = 0f; var dirY = 0f
                for (b in mol.bonds) {
                    val other = if (b.from == idx) b.to else b.from
                    if (other >= proj.size) continue
                    val op = proj[other]
                    val dx = sx - op.first.toFloat(); val dy = sy - op.second.toFloat()
                    val l = sqrt(dx * dx + dy * dy)
                    if (l > 0.1f) { dirX += dx / l; dirY += dy / l }
                }
                if (dirX * dirX + dirY * dirY < 0.01f) { dirX = 0f; dirY = -1f }
                val il = sqrt(dirX * dirX + dirY * dirY); dirX /= il; dirY /= il

                val off = rad * 1.55f
                val dotR = rad * 0.17f
                val gap = dotR * 2.4f
                val breath = 0.9f + 0.1f * sin(pulse * 0.8f + idx)

                for (k in 0 until a.lonePairs) {
                    val a0 = (k - (a.lonePairs - 1) / 2f) * 1.15f
                    val ca = cos(a0); val sa = sin(a0)
                    // bağ karşıtı yön etrafında açılı radyal
                    val pxr = dirX * ca - dirY * sa
                    val pyr = dirX * sa + dirY * ca
                    val bx = sx + pxr * off
                    val by = sy + pyr * off

                    // orbital bulutu (yumuşak, sade)
                    loneHaloP.shader = RadialGradient(bx, by, rad * 0.95f * breath,
                        intArrayOf(0x3800B8E6.toInt(), 0x1600B8E6.toInt(), 0x00000000.toInt()),
                        floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                    canvas.drawCircle(bx, by, rad * 0.95f * breath, loneHaloP)
                    loneHaloP.shader = null

                    // ikiz nokta — bulutun üzerinde
                    val tx = -pyr; val ty = pxr
                    loneP.color = Color.argb(215, 225, 245, 255)
                    canvas.drawCircle(bx + tx * gap, by + ty * gap, dotR, loneP)
                    canvas.drawCircle(bx - tx * gap, by - ty * gap, dotR, loneP)
                }
            }

            // sembol etiketi
            textP.textSize = rad * 0.62f
            labelP.textSize = rad * 0.62f
            // gölgeli okunabilirlik: önce koyu, sonra renkli üst
            canvas.drawText(a.el, sx, sy + textP.textSize * 0.36f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; color = Color.argb(160, 0, 0, 0); textSize = textP.textSize
                style = Paint.Style.STROKE; strokeWidth = rad * 0.14f; strokeJoin = Paint.Join.ROUND
            })
            textP.color = labelColor(baseC)
            canvas.drawText(a.el, sx, sy + textP.textSize * 0.36f, textP)

            if (a.fc != 0) {
                fcP.textSize = rad * 0.72f
                canvas.drawText(if (a.fc > 0) "+${a.fc}" else "${a.fc}", sx + rad * 0.95f, sy - rad * 0.7f, fcP)
            }
        }
    }

    private fun labelColor(bg: Int): Int {
        val lum = 0.299f * Color.red(bg) + 0.587f * Color.green(bg) + 0.114f * Color.blue(bg)
        return if (lum > 150) Color.rgb(20, 26, 36) else Color.WHITE
    }
}

// ---------------------------------------------------------------------------
// FRAGMENT
// ---------------------------------------------------------------------------
class LewisFragment : Fragment() {

    private val molecules = buildMolecules()

    private var currentIndex = 0
    private var mod = "YAPI"
    private lateinit var container: FrameLayout
    private lateinit var modArea: FrameLayout
    private lateinit var tvName: TextView
    private lateinit var tvFormula: TextView
    private lateinit var chipProps: LinearLayout
    private lateinit var moleculeView: Lewis3DView
    private lateinit var btnYapi: Button
    private lateinit var btnRehber: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lewis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.lewis_container)
        val sv = view as android.widget.ScrollView
        val titleTv = (((sv.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? TextView
        titleTv?.let { AnimUtils.gradientTitle(it) }
        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            HelpDialog.show(requireContext(), "Lewis Yapısı Stüdyosu", """
                <b>Ne işe yarar?</b> Moleküllerin Lewis (nokta) elektron yapısını 3B, döndürülebilir bir stüdyoda inceler.<br/><br/>
                <b>3B YAPI:</b> Molekülü parmağınızla sürükleyerek döndürün, çift parmakla yakınlaşın. Yalnız çiftler mavi-turkuaz noktalar olarak titreşir.<br/>
                <b>REHBER:</b> Seçili molekül için Lewis yapısının adım adım nasıl kurulduğunu gösterir.<br/><br/>
                <b>Çizim kuralları:</b><br/>
                <b>1.</b> Toplam değerlik elektronu sayılır.<br/>
                <b>2.</b> En az elektronegatif atom merkeze konur.<br/>
                <b>3.</b> Atomlar tek bağla bağlanır (her bağ 2 elektron).<br/>
                <b>4.</b> Kalan elektronlar yalnız çift olarak yerleştirilir (oktet kuralı).<br/>
                <b>5.</b> Oktet eksikse çoklu bağ kurulur.<br/><br/>
                <b>Renkler:</b> gri C, kızıl O, mavi N, yeşil Cl/F, sarı S, turuncu P, açık gri H.<br/>
                <b>Mavi çift çizgi</b> = çift bağ, <b>pembe üçlü çizgi</b> = üçlü bağ.<br/>
                <b>⚡ Rezonans:</b> Elektron delokalizasyonu animasyonlu kesikli çizgiyle gösterilir.<br/>
                Toplam ${molecules.size} molekül.
            """.trimIndent())
        }

        val content = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        content.addView(baslikSatiri())
        chipProps = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(8))
        }
        content.addView(chipProps)
        content.addView(modSatiri())
        modArea = FrameLayout(requireContext())
        content.addView(modArea)
        container.addView(content)

        showMod()
        guncelleBilgi()
        showMolecule()
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun rounded(bg: Int, corner: Int, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = corner.toFloat()
            setColor(bg)
            if (stroke > 0) setStroke(stroke, strokeColor)
        }
    }

    private fun baslikSatiri(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(6))
            val prev = Button(requireContext()).apply {
                text = "‹"; textSize = 30f; setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                background = rounded(0xFF1A2740.toInt(), dp(24), 1, 0xFF2A3D5F.toInt())
            }
            val next = Button(requireContext()).apply {
                text = "›"; textSize = 30f; setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                background = rounded(0xFF1A2740.toInt(), dp(24), 1, 0xFF2A3D5F.toInt())
            }
            prev.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            next.layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            val mid = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            tvName = TextView(requireContext()).apply {
                textSize = 19f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD; setLetterSpacing(0.02f)
            }
            tvFormula = TextView(requireContext()).apply {
                textSize = 13f; setTextColor(Color.rgb(0, 224, 255)); gravity = Gravity.CENTER
                setTypeface(Typeface.DEFAULT_BOLD)
            }
            mid.addView(tvName)
            mid.addView(tvFormula)
            mid.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            prev.setOnClickListener { AnimUtils.press(prev); currentIndex = (currentIndex - 1 + molecules.size) % molecules.size; showMolecule() }
            next.setOnClickListener { AnimUtils.press(next); currentIndex = (currentIndex + 1) % molecules.size; showMolecule() }
            addView(prev); addView(mid); addView(next)
        }
    }

    private fun modSatiri(): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(6))
            background = rounded(0xFF121C30.toInt(), dp(14), 1, 0xFF22314E.toInt())
        }
        btnYapi = Button(requireContext()).apply {
            text = "3B YAPI"; textSize = 13f; setAllCaps(false)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(0xFF00C2D6.toInt(), dp(12), 0)
            setTextColor(Color.rgb(4, 16, 22))
        }
        btnRehber = Button(requireContext()).apply {
            text = "REHBER"; textSize = 13f; setAllCaps(false)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(0xFF121C30.toInt(), dp(12), 0)
            setTextColor(Color.rgb(190, 208, 232))
        }
        val lp1 = LinearLayout.LayoutParams(0, dp(42), 1f); lp1.setMargins(dp(3), dp(3), dp(2), dp(3))
        val lp2 = LinearLayout.LayoutParams(0, dp(42), 1f); lp2.setMargins(dp(2), dp(3), dp(3), dp(3))
        btnYapi.layoutParams = lp1
        btnRehber.layoutParams = lp2
        btnYapi.setOnClickListener { mod = "YAPI"; showMod() }
        btnRehber.setOnClickListener { mod = "REHBER"; showMod() }
        row.addView(btnYapi); row.addView(btnRehber)
        return row
    }

    private fun chipBtn(t: String, active: Boolean): Button {
        return Button(requireContext()).apply {
            text = t; textSize = 11.5f; setAllCaps(false)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(if (active) 0xFF00C2D6.toInt() else 0xFF1A2740.toInt(), dp(16), if (active) 0 else 1, if (active) 0 else 0xFF2A3D5F.toInt())
            setTextColor(if (active) Color.rgb(4, 16, 22) else Color.rgb(200, 216, 238))
        }
    }

    private fun propChip(t: String, color: Int): TextView {
        val tv = TextView(requireContext()).apply {
            text = t; textSize = 11.5f; setTextColor(Color.rgb(220, 232, 248))
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = rounded(0xFF16223A.toInt(), dp(13), 1, color)
        }
        return tv
    }

    private fun showMod() {
        modArea.removeAllViews()
        btnYapi.background = rounded(if (mod == "YAPI") 0xFF00C2D6.toInt() else 0xFF121C30.toInt(), dp(12), 0)
        btnYapi.setTextColor(if (mod == "YAPI") Color.rgb(4, 16, 22) else Color.rgb(190, 208, 232))
        btnRehber.background = rounded(if (mod == "REHBER") 0xFF00C2D6.toInt() else 0xFF121C30.toInt(), dp(12), 0)
        btnRehber.setTextColor(if (mod == "REHBER") Color.rgb(4, 16, 22) else Color.rgb(190, 208, 232))
        if (mod == "YAPI") showYapi() else showRehber()
    }

    private fun showYapi() {
        modArea.removeAllViews()
        val v = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val mv = Lewis3DView(requireContext())
        mv.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(340))
        v.addView(mv)
        moleculeView = mv
        v.addView(chipRow())
        modArea.addView(v)
        moleculeView.setMolecule(molecules[currentIndex])
    }

    private fun chipRow(): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(4))
        }
        val chA = chipBtn("⟳ Otomatik Döndür", true)
        val chB = chipBtn("◉ Yalnız Çiftler", true)
        chA.layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply { setMargins(0, 0, dp(5), 0) }
        chB.layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
        chA.setOnClickListener {
            moleculeView.autoRotate = !moleculeView.autoRotate
            it.background = rounded(if (moleculeView.autoRotate) 0xFF00C2D6.toInt() else 0xFF1A2740.toInt(), dp(16), if (moleculeView.autoRotate) 0 else 1, if (moleculeView.autoRotate) 0 else 0xFF2A3D5F.toInt())
            (it as Button).setTextColor(if (moleculeView.autoRotate) Color.rgb(4, 16, 22) else Color.rgb(200, 216, 238))
        }
        chB.setOnClickListener {
            moleculeView.showLonePairs = !moleculeView.showLonePairs
            moleculeView.invalidate()
            it.background = rounded(if (moleculeView.showLonePairs) 0xFF00C2D6.toInt() else 0xFF1A2740.toInt(), dp(16), if (moleculeView.showLonePairs) 0 else 1, if (moleculeView.showLonePairs) 0 else 0xFF2A3D5F.toInt())
            (it as Button).setTextColor(if (moleculeView.showLonePairs) Color.rgb(4, 16, 22) else Color.rgb(200, 216, 238))
        }
        row.addView(chA); row.addView(chB)
        return row
    }

    private fun showRehber() {
        modArea.removeAllViews()
        val mol = molecules[currentIndex]
        val v = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        v.setPadding(0, dp(4), 0, 0)

        val baslik = TextView(requireContext()).apply {
            text = "Adım Adım Lewis Kurulumu"; textSize = 15f; setTextColor(Color.WHITE)
            setTypeface(Typeface.DEFAULT_BOLD); setPadding(dp(4), 0, 0, dp(8))
        }
        v.addView(baslik)

        val steps = buildSteps(mol)
        for ((i, s) in steps.withIndex()) {
            val kart = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                background = rounded(0xFF121C30.toInt(), dp(10), 1, 0xFF22314E.toInt())
            }
            val num = TextView(requireContext()).apply {
                text = "${i + 1}"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(if (mol.resonance && i == 3) 0xFFFFB300.toInt() else 0xFF00C2D6.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, dp(4), dp(10), dp(4)) }
                setTextColor(Color.rgb(4, 16, 22))
            }
            val txt = TextView(requireContext()).apply {
                text = android.text.Html.fromHtml(s, android.text.Html.FROM_HTML_MODE_LEGACY)
                textSize = 13f; setTextColor(Color.rgb(222, 232, 246)); setLineSpacing(0f, 1.15f)
            }
            txt.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            kart.addView(num); kart.addView(txt)
            val klp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            klp.setMargins(0, 0, 0, dp(6))
            kart.layoutParams = klp
            v.addView(kart)
        }

        val not = TextView(requireContext()).apply {
            text = "\n${mol.info.ifEmpty { "${mol.formula} — ${mol.geometry}, ${mol.angle}, ${mol.polar}, hibrit: ${mol.hybrid}" }}"
            textSize = 12f; setTextColor(Color.argb(200, 170, 190, 215)); setLineSpacing(0f, 1.2f); setPadding(dp(4), dp(2), dp(4), 0)
        }
        v.addView(not)
        modArea.addView(v)
    }

    private fun showMolecule() {
        val mol = molecules[currentIndex]
        tvName.text = mol.name
        tvFormula.text = mol.formula
        if (mod == "YAPI") moleculeView.setMolecule(mol) else showRehber()
        guncelleBilgi()
    }

    private fun guncelleBilgi() {
        val mol = molecules[currentIndex]
        chipProps.removeAllViews()
        val props = listOf(
            "📐 ${mol.geometry.ifEmpty { "—" }}",
            "∠ ${mol.angle.ifEmpty { "—" }}",
            if (mol.polar.isNotEmpty()) "🧲 ${mol.polar}" else "Yük: ${mol.charge}",
            "⚗ ${mol.hybrid.ifEmpty { "—" }}"
        )
        for ((i, p) in props.withIndex()) {
            val colors = intArrayOf(0xFF00C2D6.toInt(), 0xFF4DD0E1.toInt(), 0xFFFFB300.toInt(), 0xFF7C4DFF.toInt())
            val c = chipProps.context
            val tv = TextView(c).apply {
                text = p; textSize = 11f; setTextColor(Color.rgb(222, 232, 248))
                gravity = Gravity.CENTER; setPadding(dp(8), dp(3), dp(8), dp(3))
                background = rounded(0xFF16223A.toInt(), dp(11), 1, colors[i % 4])
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, dp(4), 0)
            tv.layoutParams = lp
            chipProps.addView(tv)
        }
    }

    private fun buildSteps(mol: LewisMolecule): List<String> {
        val merkez = merkezAtom(mol)
        return listOf(
            "Değerlik elektronlarını topla: ${mol.formula} için toplam <b>${mol.valence}</b> değerlik elektronu.",
            "En az elektronegatif atomu merkeze koy: <b>${merkez}</b> (uygun merkez atom).",
            "Atomları tek bağla bağla: <b>${mol.bonds.size}</b> bağ = <b>${mol.bonds.size * 2}</b> elektron kullanıldı.",
            if (mol.resonance)
                "Rezonans: Çoklu bağ yerini değiştirir — elektronlar delokalizedir (kesikli çizgi)."
            else
                "Kalan ${mol.valence - mol.bonds.size * 2} elektronu yalnız çift olarak yerleştir (oktet kuralı).",
            "Geometri: <b>${mol.geometry}</b> (${mol.angle}) • ${mol.polar} • Hibritleşme: ${mol.hybrid}${if (mol.charge.isNotEmpty()) " • Yük: ${mol.charge}" else ""}"
        )
    }

    private fun merkezAtom(mol: LewisMolecule): String {
        var best = 0; var bestDeg = -1
        for (i in mol.atoms.indices) {
            val deg = mol.bonds.count { it.from == i || it.to == i }
            if (deg > bestDeg) { bestDeg = deg; best = i }
        }
        return mol.atoms[best].el
    }

    // ------------------------------------------------------------------
    // MOLEKÜL VERİTABANI (3B koordinatlar)
    // ------------------------------------------------------------------
    private fun buildMolecules(): List<LewisMolecule> {
        val m = mutableListOf<LewisMolecule>()

        // H₂O
        m += LewisMolecule("Su", "H₂O", "Toplam değerlik elektronu: 8 • Merkez O, 2 bağ + 2 yalnız çift • Bükülü (104.5°) • Polar • sp³",
            8, "Bükülü", "104.5°", "Polar", "sp³",
            atoms = listOf(
                LewisAtom("O", 0f, 0f, 0f, lonePairs = 2),
                LewisAtom("H", 42f, 32f, 0f), LewisAtom("H", -42f, 32f, 0f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2)))

        // CO₂
        m += LewisMolecule("Karbondioksit", "CO₂", "Toplam değerlik elektronu: 16 • Merkez C, 2 çift bağ • Doğrusal (180°) • Apolar • sp",
            16, "Doğrusal", "180°", "Apolar", "sp",
            atoms = listOf(
                LewisAtom("O", -55f, 0f, 0f, lonePairs = 2),
                LewisAtom("C", 0f, 0f, 0f),
                LewisAtom("O", 55f, 0f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(1, 2, 2)))

        // NH₃
        m += LewisMolecule("Amonyak", "NH₃", "Toplam değerlik elektronu: 8 • Merkez N, 3 bağ + 1 yalnız çift • Üçgen piramit (107°) • Polar • sp³",
            8, "Üçgen Piramit", "107°", "Polar", "sp³",
            atoms = listOf(
                LewisAtom("N", 0f, 0f, 14f, lonePairs = 1),
                LewisAtom("H", 46f, -18f, -6f), LewisAtom("H", -38f, -18f, -6f), LewisAtom("H", -8f, 36f, -6f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3)))

        // CH₄
        m += LewisMolecule("Metan", "CH₄", "Toplam değerlik elektronu: 8 • Merkez C, 4 bağ • Dört yüzlü (109.5°) • Apolar • sp³",
            8, "Dört Yüzlü", "109.5°", "Apolar", "sp³",
            atoms = listOf(
                LewisAtom("C", 0f, 0f, 0f),
                LewisAtom("H", 38f, 38f, 38f), LewisAtom("H", 38f, -38f, -38f),
                LewisAtom("H", -38f, 38f, -38f), LewisAtom("H", -38f, -38f, 38f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4)))

        // HF
        m += LewisMolecule("Hidrojen Florür", "HF", "Toplam değerlik elektronu: 8 • Doğrusal • Polar • Çok güçlü bağ (567 kJ/mol)",
            8, "Doğrusal", "180°", "Polar", "—",
            atoms = listOf(
                LewisAtom("H", -30f, 0f, 0f),
                LewisAtom("F", 30f, 0f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1)))

        // HCl
        m += LewisMolecule("Hidrojen Klorür", "HCl", "Toplam değerlik elektronu: 8 • Doğrusal • Polar",
            8, "Doğrusal", "180°", "Polar", "—",
            atoms = listOf(
                LewisAtom("H", -30f, 0f, 0f),
                LewisAtom("Cl", 30f, 0f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1)))

        // CCl₄
        m += LewisMolecule("Karbon Tetraklorür", "CCl₄", "Toplam değerlik elektronu: 32 • Merkez C, 4 bağ • Dört yüzlü • Apolar • sp³",
            32, "Dört Yüzlü", "109.5°", "Apolar", "sp³",
            atoms = listOf(
                LewisAtom("C", 0f, 0f, 0f),
                LewisAtom("Cl", 38f, 38f, 38f, lonePairs = 3), LewisAtom("Cl", 38f, -38f, -38f, lonePairs = 3),
                LewisAtom("Cl", -38f, 38f, -38f, lonePairs = 3), LewisAtom("Cl", -38f, -38f, 38f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4)))

        // O₂
        m += LewisMolecule("Oksijen", "O₂", "Toplam değerlik elektronu: 12 • Çift bağ (O=O) • Apolar • Paramanyetik",
            12, "Doğrusal", "—", "Apolar", "—",
            atoms = listOf(
                LewisAtom("O", -30f, 0f, 0f, lonePairs = 2),
                LewisAtom("O", 30f, 0f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2)))

        // N₂
        m += LewisMolecule("Azot", "N₂", "Toplam değerlik elektronu: 10 • Üçlü bağ (N≡N) • Apolar • Çok güçlü bağ (945 kJ/mol)",
            10, "Doğrusal", "180°", "Apolar", "sp",
            atoms = listOf(
                LewisAtom("N", -30f, 0f, 0f, lonePairs = 1),
                LewisAtom("N", 30f, 0f, 0f, lonePairs = 1)),
            bonds = listOf(LewisBond(0, 1, 3)))

        // SO₂
        m += LewisMolecule("Kükürt Dioksit", "SO₂", "Toplam değerlik elektronu: 18 • Merkez S, çift bağ + yalnız çift • Bükülü (119°) • Polar • Rezonans",
            18, "Bükülü", "119°", "Polar", "sp²", resonance = true,
            atoms = listOf(
                LewisAtom("S", 0f, 0f, 0f, lonePairs = 1),
                LewisAtom("O", 45f, 30f, 0f, lonePairs = 2),
                LewisAtom("O", -45f, 30f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(0, 2, 2)))

        // HCN
        m += LewisMolecule("Hidrojen Siyanür", "HCN", "Toplam değerlik elektronu: 10 • C≡N üçlü bağ • Doğrusal (180°) • Polar",
            10, "Doğrusal", "180°", "Polar", "sp",
            atoms = listOf(
                LewisAtom("H", -60f, 0f, 0f),
                LewisAtom("C", 0f, 0f, 0f),
                LewisAtom("N", 55f, 0f, 0f, lonePairs = 1)),
            bonds = listOf(LewisBond(0, 1), LewisBond(1, 2, 3)))

        // NO₃⁻
        m += LewisMolecule("Nitrat İyonu", "NO₃⁻", "Toplam değerlik elektronu: 24 • Merkez N • Düzenli üçgen (120°) • Rezonans • Yük: -1",
            24, "Düzenli Üçgen", "120°", "—", "sp²", resonance = true, charge = "−1",
            atoms = listOf(
                LewisAtom("N", 0f, 0f, 0f),
                LewisAtom("O", 48f, 0f, 0f, lonePairs = 2),
                LewisAtom("O", -24f, 42f, 0f, lonePairs = 2),
                LewisAtom("O", -24f, -42f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(0, 2), LewisBond(0, 3)))

        // O₃
        m += LewisMolecule("Ozon", "O₃", "Toplam değerlik elektronu: 18 • Merkez O • Bükülü (117°) • Polar • Rezonans",
            18, "Bükülü", "117°", "Polar", "sp²", resonance = true,
            atoms = listOf(
                LewisAtom("O", -40f, 20f, 0f, lonePairs = 2),
                LewisAtom("O", 0f, 0f, 0f, lonePairs = 1),
                LewisAtom("O", 40f, 20f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(1, 2, 2)))

        // NO₂
        m += LewisMolecule("Azot Dioksit", "NO₂", "Toplam değerlik elektronu: 17 • Merkez N, çift bağ + tek elektron • Bükülü (134°) • Radikal • Rezonans",
            17, "Bükülü", "134°", "Polar", "sp²", resonance = true,
            atoms = listOf(
                LewisAtom("N", 0f, 0f, 0f, lonePairs = 0, fc = 1),
                LewisAtom("O", 45f, 28f, 0f, lonePairs = 2),
                LewisAtom("O", -45f, 28f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(0, 2, 2)))

        // PCl₅
        m += LewisMolecule("Fosfor Pentaklorür", "PCl₅", "Toplam değerlik elektronu: 40 • Merkez P, 5 bağ • Trigonal-bipiramit • Apolar • sp³d",
            40, "Trigonal Bipiramit", "90°/120°", "Apolar", "sp³d",
            atoms = listOf(
                LewisAtom("P", 0f, 0f, 0f),
                LewisAtom("Cl", 50f, 0f, 0f, lonePairs = 3),
                LewisAtom("Cl", -25f, 43f, 0f, lonePairs = 3),
                LewisAtom("Cl", -25f, -43f, 0f, lonePairs = 3),
                LewisAtom("Cl", 0f, 0f, 50f, lonePairs = 3),
                LewisAtom("Cl", 0f, 0f, -50f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4), LewisBond(0, 5)))

        // SF₆
        m += LewisMolecule("Kükürt Heksaflorür", "SF₆", "Toplam değerlik elektronu: 48 • Merkez S, 6 bağ • Oktahedral • Apolar • sp³d²",
            48, "Oktahedral", "90°", "Apolar", "sp³d²",
            atoms = listOf(
                LewisAtom("S", 0f, 0f, 0f),
                LewisAtom("F", 50f, 0f, 0f, lonePairs = 3), LewisAtom("F", -50f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", 0f, 50f, 0f, lonePairs = 3), LewisAtom("F", 0f, -50f, 0f, lonePairs = 3),
                LewisAtom("F", 0f, 0f, 50f, lonePairs = 3), LewisAtom("F", 0f, 0f, -50f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4), LewisBond(0, 5), LewisBond(0, 6)))

        // XeF₂
        m += LewisMolecule("Ksenon Diflorür", "XeF₂", "Toplam değerlik elektronu: 22 • Merkez Xe, 2 bağ + 3 yalnız çift • Doğrusal • Apolar • sp³d",
            22, "Doğrusal", "180°", "Apolar", "sp³d",
            atoms = listOf(
                LewisAtom("Xe", 0f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", -50f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", 50f, 0f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2)))

        // CO₃²⁻
        m += LewisMolecule("Karbonat İyonu", "CO₃²⁻", "Toplam değerlik elektronu: 24 • Merkez C • Düzenli üçgen (120°) • Rezonans • Yük: -2",
            24, "Düzenli Üçgen", "120°", "—", "sp²", resonance = true, charge = "−2",
            atoms = listOf(
                LewisAtom("C", 0f, 0f, 0f),
                LewisAtom("O", 48f, 0f, 0f, lonePairs = 2),
                LewisAtom("O", -24f, 42f, 0f, lonePairs = 2),
                LewisAtom("O", -24f, -42f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(0, 2), LewisBond(0, 3)))

        // SO₄²⁻
        m += LewisMolecule("Sülfat İyonu", "SO₄²⁻", "Toplam değerlik elektronu: 32 • Merkez S, 4 bağ • Dört yüzlü • Apolar (simetrik) • Yük: -2",
            32, "Dört Yüzlü", "109.5°", "Apolar", "sp³", charge = "−2",
            atoms = listOf(
                LewisAtom("S", 0f, 0f, 0f),
                LewisAtom("O", 38f, 38f, 38f, lonePairs = 2), LewisAtom("O", 38f, -38f, -38f, lonePairs = 2),
                LewisAtom("O", -38f, 38f, -38f, lonePairs = 2), LewisAtom("O", -38f, -38f, 38f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4)))

        // NH₄⁺
        m += LewisMolecule("Amonyum İyonu", "NH₄⁺", "Toplam değerlik elektronu: 8 • Merkez N, 4 bağ • Dört yüzlü • Apolar (simetrik) • Yük: +1",
            8, "Dört Yüzlü", "109.5°", "Apolar", "sp³", charge = "+1",
            atoms = listOf(
                LewisAtom("N", 0f, 0f, 0f),
                LewisAtom("H", 38f, 38f, 38f), LewisAtom("H", 38f, -38f, -38f),
                LewisAtom("H", -38f, 38f, -38f), LewisAtom("H", -38f, -38f, 38f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4)))

        // BeCl₂
        m += LewisMolecule("Berilyum Klorür", "BeCl₂", "Toplam değerlik elektronu: 16 • Merkez Be, 2 bağ • Doğrusal (180°) • Apolar • sp",
            16, "Doğrusal", "180°", "Apolar", "sp",
            atoms = listOf(
                LewisAtom("Be", 0f, 0f, 0f),
                LewisAtom("Cl", -50f, 0f, 0f, lonePairs = 3),
                LewisAtom("Cl", 50f, 0f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2)))

        // BF₃
        m += LewisMolecule("Bor Triflorür", "BF₃", "Toplam değerlik elektronu: 24 • Merkez B, 3 bağ • Düzenli üçgen (120°) • Apolar • sp² (oktet eksik — elektron eksikliği)",
            24, "Düzenli Üçgen", "120°", "Apolar", "sp²",
            atoms = listOf(
                LewisAtom("B", 0f, 0f, 0f),
                LewisAtom("F", 48f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", -24f, 42f, 0f, lonePairs = 3),
                LewisAtom("F", -24f, -42f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3)))

        // ClF₃
        m += LewisMolecule("Klor Triflorür", "ClF₃", "Toplam değerlik elektronu: 28 • Merkez Cl, 3 bağ + 2 yalnız çift • T-şekilli • Polar • sp³d",
            28, "T-şekilli", "87.5°", "Polar", "sp³d",
            atoms = listOf(
                LewisAtom("Cl", 0f, 0f, 0f, lonePairs = 2),
                LewisAtom("F", -50f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", 50f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", 0f, -48f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3)))

        // XeF₄
        m += LewisMolecule("Ksenon Tetraflorür", "XeF₄", "Toplam değerlik elektronu: 36 • Merkez Xe, 4 bağ + 2 yalnız çift • Kare-düzlem • Apolar (simetrik) • sp³d²",
            36, "Kare-düzlem", "90°", "Apolar", "sp³d²",
            atoms = listOf(
                LewisAtom("Xe", 0f, 0f, 0f, lonePairs = 2),
                LewisAtom("F", -50f, 0f, 0f, lonePairs = 3), LewisAtom("F", 50f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", 0f, -50f, 0f, lonePairs = 3), LewisAtom("F", 0f, 50f, 0f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4)))

        // BrF₅
        m += LewisMolecule("Brom Pentaflorür", "BrF₅", "Toplam değerlik elektronu: 42 • Merkez Br, 5 bağ + 1 yalnız çift • Kare-piramit • Polar • sp³d²",
            42, "Kare-piramit", "90°", "Polar", "sp³d²",
            atoms = listOf(
                LewisAtom("Br", 0f, 0f, 0f, lonePairs = 1),
                LewisAtom("F", -50f, 0f, 0f, lonePairs = 3), LewisAtom("F", 50f, 0f, 0f, lonePairs = 3),
                LewisAtom("F", 0f, -50f, 0f, lonePairs = 3), LewisAtom("F", 0f, 50f, 0f, lonePairs = 3),
                LewisAtom("F", 0f, 0f, 48f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4), LewisBond(0, 5)))

        // H₂S
        m += LewisMolecule("Hidrojen Sülfür", "H₂S", "Toplam değerlik elektronu: 8 • Merkez S, 2 bağ + 2 yalnız çift • Bükülü (92°) • Polar • sp³",
            8, "Bükülü", "92°", "Polar", "sp³",
            atoms = listOf(
                LewisAtom("S", 0f, 0f, 0f, lonePairs = 2),
                LewisAtom("H", 40f, 34f, 0f), LewisAtom("H", -40f, 34f, 0f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2)))

        // PH₃
        m += LewisMolecule("Fosfin", "PH₃", "Toplam değerlik elektronu: 8 • Merkez P, 3 bağ + 1 yalnız çift • Üçgen piramit (93.5°) • Polar • sp³",
            8, "Üçgen Piramit", "93.5°", "Polar", "sp³",
            atoms = listOf(
                LewisAtom("P", 0f, 0f, 12f, lonePairs = 1),
                LewisAtom("H", 44f, -18f, -6f), LewisAtom("H", -36f, -18f, -6f), LewisAtom("H", -8f, 34f, -6f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3)))

        // CH₃Cl
        m += LewisMolecule("Klorometan", "CH₃Cl", "Toplam değerlik elektronu: 14 • Merkez C • Dört yüzlü • Polar • sp³",
            14, "Dört Yüzlü", "109.5°", "Polar", "sp³",
            atoms = listOf(
                LewisAtom("C", 0f, 0f, 0f),
                LewisAtom("H", 38f, 38f, 38f), LewisAtom("H", 38f, -38f, -38f), LewisAtom("H", -38f, 38f, -38f),
                LewisAtom("Cl", -38f, -38f, 38f, lonePairs = 3)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(0, 3), LewisBond(0, 4)))

        // H₂O₂
        m += LewisMolecule("Hidrojen Peroksit", "H₂O₂", "Toplam değerlik elektronu: 14 • O-O bağı • Bükülü (O-H) • Polar • sp³",
            14, "Bükülü", "94.8°", "Polar", "sp³",
            atoms = listOf(
                LewisAtom("O", -25f, 0f, 0f, lonePairs = 2),
                LewisAtom("O", 25f, 0f, 0f, lonePairs = 2),
                LewisAtom("H", -42f, 30f, 0f), LewisAtom("H", 42f, 30f, 0f)),
            bonds = listOf(LewisBond(0, 1), LewisBond(0, 2), LewisBond(1, 3)))

        // CO
        m += LewisMolecule("Karbon Monoksit", "CO", "Toplam değerlik elektronu: 10 • C≡O üçlü bağ • Doğrusal • Polar (zayıf) • Çok güçlü bağ",
            10, "Doğrusal", "180°", "Polar", "sp",
            atoms = listOf(
                LewisAtom("C", -30f, 0f, 0f, lonePairs = 1),
                LewisAtom("O", 30f, 0f, 0f, lonePairs = 1)),
            bonds = listOf(LewisBond(0, 1, 3)))

        // SO₃
        m += LewisMolecule("Kükürt Trioksit", "SO₃", "Toplam değerlik elektronu: 24 • Merkez S • Düzenli üçgen (120°) • Apolar • Rezonans • sp²",
            24, "Düzenli Üçgen", "120°", "Apolar", "sp²", resonance = true,
            atoms = listOf(
                LewisAtom("S", 0f, 0f, 0f),
                LewisAtom("O", 48f, 0f, 0f, lonePairs = 2),
                LewisAtom("O", -24f, 42f, 0f, lonePairs = 2),
                LewisAtom("O", -24f, -42f, 0f, lonePairs = 2)),
            bonds = listOf(LewisBond(0, 1, 2), LewisBond(0, 2, 2), LewisBond(0, 3, 2)))

        return m
    }
}
