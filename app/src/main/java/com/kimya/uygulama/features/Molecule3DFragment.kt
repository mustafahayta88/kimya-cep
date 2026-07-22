package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

data class Atom3D(val symbol: String, var x: Float, var y: Float, var z: Float, val color: Int, val radius: Float)
data class Bond3D(val from: Int, val to: Int, val order: Int = 1)

class Molecule3DView(context: Context) : View(context) {
    private var molIndex = 0
    private var rotX = 0f; private var rotY = 0f
    private var autoRotate = true
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val bond2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val bond3P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val elP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); style = Paint.Style.FILL }

    var onMoleculeChange: ((Int) -> Unit)? = null

    private val molecules = listOf(
        listOf(Atom3D("O", 0f, 0f, 0f, 0xFFFF0000.toInt(), 18f), Atom3D("H", 20f, -15f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", 20f, 15f, 0f, 0xFFFFFFFF.toInt(), 12f)),
        listOf(Atom3D("C", 0f, 0f, 0f, 0xFF6B6B6B.toInt(), 16f), Atom3D("O", 25f, 0f, 0f, 0xFFFF0000.toInt(), 18f), Atom3D("O", -25f, 0f, 0f, 0xFFFF0000.toInt(), 18f)),
        listOf(Atom3D("C", 0f, 0f, 0f, 0xFF6B6B6B.toInt(), 16f), Atom3D("H", 22f, 18f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", -22f, 18f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", 0f, -24f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", 0f, 0f, 22f, 0xFFFFFFFF.toInt(), 12f)),
        listOf(Atom3D("N", 0f, 0f, 0f, 0xFF3050F8.toInt(), 16f), Atom3D("H", 20f, 14f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", -16f, 18f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", -4f, -20f, 0f, 0xFFFFFFFF.toInt(), 12f)),
        listOf(Atom3D("C", 0f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("C", 22f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("H", -18f, 14f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -18f, -14f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 40f, 14f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 40f, -14f, 0f, 0xFFFFFFFF.toInt(), 10f)),
        listOf(Atom3D("C", 0f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("C", 22f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("H", -18f, 15f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -18f, -15f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 40f, 15f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 40f, -15f, 0f, 0xFFFFFFFF.toInt(), 10f)),
        listOf(
            Atom3D("C", 0f, -18f, 0f, 0xFF6B6B6B.toInt(), 13f), Atom3D("C", 16f, -9f, 2f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("C", 16f, 9f, -2f, 0xFF6B6B6B.toInt(), 13f), Atom3D("C", 0f, 18f, 0f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("C", -16f, 9f, 2f, 0xFF6B6B6B.toInt(), 13f), Atom3D("C", -16f, -9f, -2f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("H", 0f, -30f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 27f, -15f, 3f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", 27f, 15f, -3f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 0f, 30f, 0f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", -27f, 15f, 3f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -27f, -15f, -3f, 0xFFFFFFFF.toInt(), 10f)
        ),
        listOf(
            Atom3D("C", -35f, 8f, 0f, 0xFF6B6B6B.toInt(), 12f), Atom3D("C", -20f, -8f, 1f, 0xFF6B6B6B.toInt(), 12f),
            Atom3D("C", -5f, 8f, -1f, 0xFF6B6B6B.toInt(), 12f), Atom3D("C", 10f, -8f, 0f, 0xFF6B6B6B.toInt(), 12f),
            Atom3D("C", 25f, 8f, 1f, 0xFF6B6B6B.toInt(), 12f), Atom3D("C", 40f, -8f, -1f, 0xFF6B6B6B.toInt(), 12f),
            Atom3D("C", 55f, 8f, 0f, 0xFF6B6B6B.toInt(), 12f), Atom3D("C", 70f, -8f, 1f, 0xFF6B6B6B.toInt(), 12f),
            Atom3D("H", -42f, 20f, 1f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", -42f, 0f, -4f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", -27f, -20f, 3f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", -27f, 0f, 5f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", -12f, 20f, -3f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", -12f, 0f, -5f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", 3f, -20f, 2f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", 3f, 0f, 4f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", 18f, 20f, 3f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", 18f, 0f, -3f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", 33f, -20f, -3f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", 33f, 0f, 5f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", 48f, 20f, 2f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", 48f, 0f, -4f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", 63f, -20f, 3f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", 63f, 0f, 5f, 0xFFFFFFFF.toInt(), 9f),
            Atom3D("H", 77f, 0f, -3f, 0xFFFFFFFF.toInt(), 9f), Atom3D("H", 77f, -16f, 4f, 0xFFFFFFFF.toInt(), 9f)
        ),
        listOf(
            Atom3D("C", -12f, 8f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("C", 12f, -8f, 0f, 0xFF6B6B6B.toInt(), 14f),
            Atom3D("O", 32f, 4f, 0f, 0xFFFF0000.toInt(), 16f), Atom3D("H", 44f, -4f, 0f, 0xFFFFFFFF.toInt(), 12f),
            Atom3D("H", -22f, -4f, 10f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -22f, -4f, -10f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", -18f, 22f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 8f, -22f, 10f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", 8f, -22f, -10f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 18f, -12f, 14f, 0xFFFFFFFF.toInt(), 10f)
        ),
        listOf(
            Atom3D("C", 0f, 15f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("C", 0f, -10f, 0f, 0xFF6B6B6B.toInt(), 14f),
            Atom3D("O", 0f, -30f, 0f, 0xFFFF0000.toInt(), 16f), Atom3D("C", 22f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f),
            Atom3D("H", -14f, 24f, 10f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -14f, 24f, -10f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", 8f, 26f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 32f, -8f, 10f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", 32f, -8f, -10f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 26f, 14f, 0f, 0xFFFFFFFF.toInt(), 10f)
        ),
        listOf(
            Atom3D("C", 0f, 10f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("C", 20f, -5f, 0f, 0xFF6B6B6B.toInt(), 14f),
            Atom3D("O", 35f, 5f, 0f, 0xFFFF0000.toInt(), 16f), Atom3D("O", 20f, -22f, 0f, 0xFFFF0000.toInt(), 16f),
            Atom3D("H", 45f, -2f, 0f, 0xFFFFFFFF.toInt(), 12f),
            Atom3D("H", -14f, 0f, 12f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -14f, 0f, -12f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", -4f, 26f, 0f, 0xFFFFFFFF.toInt(), 10f)
        ),
        listOf(
            Atom3D("C", 0f, 8f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("O", 20f, -6f, 0f, 0xFFFF0000.toInt(), 16f),
            Atom3D("H", 32f, 2f, 0f, 0xFFFFFFFF.toInt(), 12f),
            Atom3D("H", -14f, -2f, 12f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -14f, -2f, -12f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", -6f, 24f, 0f, 0xFFFFFFFF.toInt(), 10f)
        ),
        listOf(
            Atom3D("C", -12f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f), Atom3D("C", 12f, 0f, 0f, 0xFF6B6B6B.toInt(), 14f),
            Atom3D("H", -26f, 0f, 0f, 0xFFFFFFFF.toInt(), 12f), Atom3D("H", 26f, 0f, 0f, 0xFFFFFFFF.toInt(), 12f)
        ),
        listOf(
            Atom3D("C", 0f, -18f, 0f, 0xFF6B6B6B.toInt(), 13f), Atom3D("C", 16f, -9f, 2f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("C", 16f, 9f, -2f, 0xFF6B6B6B.toInt(), 13f), Atom3D("C", 0f, 18f, 0f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("C", -16f, 9f, 2f, 0xFF6B6B6B.toInt(), 13f), Atom3D("C", -16f, -9f, -2f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("C", 0f, -32f, 0f, 0xFF6B6B6B.toInt(), 13f),
            Atom3D("H", 27f, -15f, 3f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 27f, 15f, -3f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", 0f, 30f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", -27f, 15f, 3f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", -27f, -15f, -3f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", -12f, -38f, 0f, 0xFFFFFFFF.toInt(), 10f), Atom3D("H", 8f, -40f, 8f, 0xFFFFFFFF.toInt(), 10f),
            Atom3D("H", 8f, -40f, -8f, 0xFFFFFFFF.toInt(), 10f)
        )
    )
    private val molBonds = listOf(
        listOf(Bond3D(0, 1), Bond3D(0, 2)),
        listOf(Bond3D(0, 1, 2), Bond3D(0, 2, 2)),
        listOf(Bond3D(0, 1), Bond3D(0, 2), Bond3D(0, 3), Bond3D(0, 4)),
        listOf(Bond3D(0, 1), Bond3D(0, 2), Bond3D(0, 3)),
        listOf(Bond3D(0, 1), Bond3D(0, 2), Bond3D(0, 3), Bond3D(1, 4), Bond3D(1, 5)),
        listOf(Bond3D(0, 1, 2), Bond3D(0, 2), Bond3D(0, 3), Bond3D(1, 4), Bond3D(1, 5)),
        listOf(Bond3D(0, 1), Bond3D(1, 2, 2), Bond3D(2, 3), Bond3D(3, 4, 2), Bond3D(4, 5), Bond3D(5, 0, 2),
            Bond3D(0, 6), Bond3D(1, 7), Bond3D(2, 8), Bond3D(3, 9), Bond3D(4, 10), Bond3D(5, 11)),
        listOf(Bond3D(0, 1), Bond3D(1, 2), Bond3D(2, 3), Bond3D(3, 4), Bond3D(4, 5), Bond3D(5, 6), Bond3D(6, 7),
            Bond3D(0, 8), Bond3D(0, 9), Bond3D(1, 10), Bond3D(1, 11), Bond3D(2, 12), Bond3D(2, 13),
            Bond3D(3, 14), Bond3D(3, 15), Bond3D(4, 16), Bond3D(4, 17), Bond3D(5, 18), Bond3D(5, 19),
            Bond3D(6, 20), Bond3D(6, 21), Bond3D(7, 22), Bond3D(7, 23), Bond3D(7, 24)),
        listOf(Bond3D(0, 1), Bond3D(1, 2), Bond3D(2, 3),
            Bond3D(0, 4), Bond3D(0, 5), Bond3D(0, 6), Bond3D(1, 7), Bond3D(1, 8), Bond3D(1, 9)),
        listOf(Bond3D(0, 1), Bond3D(1, 2, 2), Bond3D(0, 3),
            Bond3D(0, 4), Bond3D(0, 5), Bond3D(0, 6), Bond3D(3, 7), Bond3D(3, 8), Bond3D(3, 9)),
        listOf(Bond3D(0, 1), Bond3D(1, 2, 2), Bond3D(1, 3), Bond3D(2, 4),
            Bond3D(0, 5), Bond3D(0, 6), Bond3D(0, 7)),
        listOf(Bond3D(0, 1), Bond3D(1, 2), Bond3D(0, 3), Bond3D(0, 4), Bond3D(0, 5)),
        listOf(Bond3D(0, 1, 3), Bond3D(0, 2), Bond3D(1, 3)),
        listOf(Bond3D(0, 1), Bond3D(1, 2, 2), Bond3D(2, 3), Bond3D(3, 4, 2), Bond3D(4, 5), Bond3D(5, 0, 2),
            Bond3D(0, 6), Bond3D(1, 7), Bond3D(2, 8), Bond3D(3, 9), Bond3D(4, 10), Bond3D(5, 11),
            Bond3D(6, 12), Bond3D(6, 13), Bond3D(6, 14))
    )
    private val molNames = listOf(
        "Su (H2O)", "Karbondioksit (CO2)", "Metan (CH4)", "Amonyak (NH3)",
        "Etan (C2H6)", "Eten (C2H4)", "Benzen (C6H6)", "Oktan (C8H18)",
        "Etanol (C2H5OH)", "Aseton (C3H6O)", "Asetik Asit (CH3COOH)",
        "Metanol (CH3OH)", "Asetilen (C2H2)", "Toluen (C7H8)"
    )

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    fun setMolecule(i: Int) { molIndex = i.coerceIn(0, molecules.size - 1); rotX = 0f; rotY = 0f; invalidate(); onMoleculeChange?.invoke(molIndex) }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1 }
            MotionEvent.ACTION_MOVE -> { if (tMode == 1 && e.pointerCount == 1) { rotY += (e.x - lastTx) * 0.01f; rotX += (e.y - lastTy) * 0.01f; autoRotate = false; invalidate() }; lastTx = e.x; lastTy = e.y }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_UP -> { if (tMode == 1 && abs(e.x - lastTx) < 10f && abs(e.y - lastTy) < 10f) { autoRotate = !autoRotate }; tMode = 0 }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        canvas.save()
        canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f)
        canvas.translate(panX / zoomScale, panY / zoomScale)
        val cx = w / 2f; val cy = h * 0.45f
        labelP.textSize = h * 0.035f

        if (autoRotate) { rotY += 0.02f; postInvalidateOnAnimation() }

        val atoms = molecules[molIndex]
        val bonds = molBonds[molIndex]
        val sc = w * 0.012f

        val proj = atoms.map { a ->
            var x = a.x * cos(rotY) - a.z * sin(rotY)
            var y = a.x * sin(rotY) * sin(rotX) + a.y * cos(rotX) + a.z * cos(rotY) * sin(rotX)
            val z = a.x * sin(rotY) * cos(rotX) - a.y * sin(rotX) + a.z * cos(rotY) * cos(rotX)
            Triple(x * sc + cx, y * sc + cy, z)
        }

        for (b in bonds) {
            val p1 = proj[b.from]; val p2 = proj[b.to]
            val sx = p1.first.toFloat(); val sy = p1.second.toFloat()
            val ox = p2.first.toFloat(); val oy = p2.second.toFloat()
            val bp = when (b.order) { 2 -> bond2P; 3 -> bond3P; else -> bondP }
            canvas.drawLine(sx, sy, ox, oy, bp)
        }

        val sorted = proj.withIndex().sortedBy { -it.value.third }
        for ((idx, p) in sorted) {
            val sx = p.first.toFloat(); val sy = p.second.toFloat(); val sz = p.third
            val r = atoms[idx].radius * sc / 18f * (1f + sz * 0.001f).coerceIn(0.6f, 1.4f)

            val a = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = atoms[idx].color; style = Paint.Style.FILL }
            canvas.drawCircle(sx, sy, r, a)
            canvas.drawCircle(sx, sy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f })
            elP.textSize = r * 1.2f
            canvas.drawText(atoms[idx].symbol, sx, sy + elP.textSize / 3f, elP)

            glowP.alpha = ((1f + sz * 0.005f).coerceIn(0.3f, 1f) * 40).toInt().coerceIn(10, 60)
            canvas.drawCircle(sx, sy, r * 2f, glowP)
        }

        labelP.textSize = h * 0.04f
        canvas.drawText(molNames[molIndex], cx, h * 0.89f, labelP)
        canvas.restore()
    }
}

class Molecule3DFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ll = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0D1117.toInt())
        }
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(8, 8, 8, 4)
        }
        headerRow.addView(TextView(requireContext()).apply {
            text = "3B Molekul Goruntuleyici"; setTextColor(0xFF00F0FF.toInt())
            textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val helpBtn3d = android.widget.Button(requireContext()).apply {
            text = "?"; textSize = 20f; setTextColor(-0x1)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
            layoutParams = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt())
        }
        headerRow.addView(helpBtn3d)
        ll.addView(headerRow)
        val view = Molecule3DView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (550 * resources.displayMetrics.density).toInt())
        }
        ll.addView(view)

        val btnNames = listOf("H2O", "CO2", "CH4", "NH3", "C2H6", "C2H4", "C6H6", "C8H18", "EtOH", "Aseton", "AcOH", "MeOH", "C2H2", "Toluen")
        val hScroll = HorizontalScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val btnRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(4, 4, 4, 4)
        }
        val btnIds = mutableListOf<Button>()
        btnNames.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 11f; setTextColor(-0x1); setPadding(10, 4, 10, 4)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { setMargins(3, 0, 3, 0) }
                setOnClickListener { btnIds.forEach { it.alpha = 0.5f }; alpha = 1f; view.setMolecule(i) }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        hScroll.addView(btnRow); ll.addView(hScroll)

        val info = arrayOf(
            "Su: V-sekli, 104.5°, polar, evrensel cozucu",
            "CO2: Dogrusal, 180°, apolar, sera gazi",
            "Metan: Dortyuzlu, 109.5°, apolar, dogal gaz",
            "Amonyak: Ucgen piramit, 107.3°, polar, gubre",
            "Etan: C-C tek bag, alkan, dogal gaz bileseni",
            "Eten: C=C cift bag, duzlemsel, alken",
            "Benzen: Halkali, 120°, aromatik, reaktif",
            "Oktan: 8 C'li alkan, benzin bileseni",
            "Etanol: Alkol, hidroksil (-OH) grubu icerir",
            "Aseton: Keton, C=O, polarl cozucu",
            "Asetik Asit: Karboksilik asit, sirke asidi",
            "Metanol: En basit alkol, toksik, endustriyel cozucu",
            "Asetilen: C=C uc bag, dogrusal, kaynak gazı",
            "Toluen: Metil benzen, aromatik, boya cozucusu"
        )
        val infoDetail = TextView(requireContext()).apply {
            setTextColor(0xFF00F0FF.toInt()); textSize = 14f; gravity = android.view.Gravity.CENTER
            setPadding(8, 4, 8, 8)
        }
        ll.addView(TextView(requireContext()).apply {
            text = "Surukleyin: dondur | Dokunun: otomatik donus | Cift parmak: yaklastir"
            setTextColor(0xFFAAAAAA.toInt()); textSize = 12f; gravity = android.view.Gravity.CENTER; setPadding(8, 8, 8, 4)
        })
        ll.addView(infoDetail)

        btnIds[0].alpha = 1f
        view.onMoleculeChange = { infoDetail.text = info[it] }
        view.setMolecule(0)
        helpBtn3d.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("3B Molekul Goruntuleyici")
                .setMessage("Molekulleri 3 boyutlu olarak inceleyebilirsiniz.\n\n" +
                    "- Molekul secmek icin alttaki dugmelere dokunun\n" +
                    "- Parmağinizi surukleyerek molekulu dondurebilirsiniz\n" +
                    "- Dokundugunuzda otomatik donus baslar\n" +
                    "- Cift parmakla yakinsastirip uzaklastirabilirsiniz\n\n" +
                    "Her molekulin aciklamasi ekranda gosterilir.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return ll
    }
}
