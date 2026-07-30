package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.math.*

data class Atom3D(val symbol: String, var x: Float, var y: Float, var z: Float, val color: Int, val radius: Float)
data class Bond3D(val from: Int, val to: Int, val order: Int = 1)

class Molecule3DView(context: Context) : View(context) {
    private var molIndex = 0
    private var rotX = 0f; private var rotY = 0f
    private var autoRotate = true
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private var showInfo = false
    private val sDetector: ScaleGestureDetector
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    data class MolInfo(val name: String, val formula: String, val geometry: String, val angle: String, val polar: String, val desc: String)

    val molInfos = listOf(
        MolInfo("Su", "H₂O", "Bükülü", "104.5°", "Polar", "Yağları çözer, tüm canlılar için temel molekül"),
        MolInfo("Karbondioksit", "CO₂", "Doğrusal", "180°", "Apolar", "Sera gazı, fotosentezinham maddesi"),
        MolInfo("Metan", "CH₄", "Dört yüzlü", "109.5°", "Apolar", "Doğal gazın ana bileşeni"),
        MolInfo("Amonyak", "NH₃", "Üçgen piramit", "107.3°", "Polar", "Gübre üretiminde kullanılır"),
        MolInfo("Etan", "C₂H₆", "Dört yüzlü (C)", "109.5°", "Apolar", "En basit iki karbonlu alkan"),
        MolInfo("Eten", "C₂H₄", "Düzensel", "120°", "Apolar", "Polietilen üretimi için kullanılır"),
        MolInfo("Benzen", "C₆H₆", "Halkalı düzlemsel", "120°", "Apolar", "Aromatik bileşiklerin temel yapıtaşı"),
        MolInfo("Oktan", "C₈H₁₈", "Uzun zincir", "109.5°", "Apolar", "Benzin ana bileşeni, oktan sayısı"),
        MolInfo("Etanol", "C₂H₅OH", "Karışık", "—", "Polar", "İçki ve dezenfektan yapımında kullanılır"),
        MolInfo("Aseton", "CₙH₆O", "Düzensel", "120°", "Polar", "Oje çözücü, endüstriyel çözücü"),
        MolInfo("Asetik Asit", "CH₃COOH", "Karışık", "—", "Polar", "Sirkanın ana bileşeni, %5 asetik asit"),
        MolInfo("Metanol", "CH₃OH", "Dört yüzlü (C)", "109.5°", "Polar", "Endüstriyel çözücü, toksik"),
        MolInfo("Asetilen", "C₂H₂", "Doğrusal", "180°", "Apolar", "Kaynak ve kesme gazı"),
        MolInfo("Toluen", "C₇H₈", "Halkalı", "120°", "Apolar", "Boya ve yapıştırıcı çözücü")
    )

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

    init {
        isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                0 -> { lastTx = e.x; lastTy = e.y; tMode = 1 }
                2 -> { val dx = e.x - lastTx; val dy = e.y - lastTy; if (abs(dx) > 5 || abs(dy) > 5) tMode = 2; if (tMode == 2) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y; invalidate() } }
                1, 3 -> { tMode = 0 }
            }
            if (e.action == MotionEvent.ACTION_MOVE && tMode == 1 && e.pointerCount == 1) {
                rotY += (e.x - lastTx) * 0.008f; rotX += (e.y - lastTy) * 0.008f; autoRotate = false; invalidate()
            }
            if (e.action == 1) { tMode = 0; handler.postDelayed({ autoRotate = true; invalidate() }, 3000) }
            true
        }
    }

    fun setMolecule(i: Int) { molIndex = i.coerceIn(0, molecules.size - 1); rotX = 0f; rotY = 0f; invalidate(); onMoleculeChange?.invoke(molIndex) }
    fun toggleInfo() { showInfo = !showInfo; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.rgb(10, 14, 23))

        // Header
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 20f; hp.textAlign = Paint.Align.CENTER; hp.color = Color.rgb(0, 240, 255); hp.isFakeBoldText = true; hp.isAntiAlias = true
        canvas.drawText("3D Molekül Görüntüleyici", w / 2f, 28f, hp)

        canvas.save()
        canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f)
        canvas.translate(panX / zoomScale, panY / zoomScale)
        val cx = w / 2f; val cy = h * 0.44f

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

        // Draw bonds
        for (b in bonds) {
            val p1 = proj[b.from]; val p2 = proj[b.to]
            val sx = p1.first.toFloat(); val sy = p1.second.toFloat()
            val ox = p2.first.toFloat(); val oy = p2.second.toFloat()
            val bondColor = when (b.order) { 2 -> Color.rgb(0, 200, 255); 3 -> Color.rgb(255, 0, 128); else -> Color.rgb(140, 150, 160) }
            val bondW = when (b.order) { 3 -> 4f; else -> 3f }
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bondColor; strokeWidth = bondW; style = Paint.Style.STROKE; isAntiAlias = true }
            if (b.order == 2) {
                val dx = ox - sx; val dy = oy - sy; val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
                val nx = -dy / len * 3f; val ny = dx / len * 3f
                canvas.drawLine(sx + nx, sy + ny, ox + nx, oy + ny, bp)
                canvas.drawLine(sx - nx, sy - ny, ox - nx, oy - ny, bp)
            } else if (b.order == 3) {
                val dx = ox - sx; val dy = oy - sy; val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
                val nx = -dy / len * 4f; val ny = dx / len * 4f
                canvas.drawLine(sx, sy, ox, oy, bp)
                canvas.drawLine(sx + nx, sy + ny, ox + nx, oy + ny, bp)
                canvas.drawLine(sx - nx, sy - ny, ox - nx, oy - ny, bp)
            } else {
                canvas.drawLine(sx, sy, ox, oy, bp)
            }
        }

        // Draw atoms (sorted by depth)
        val sorted = proj.withIndex().sortedBy { -it.value.third }
        for ((idx, p) in sorted) {
            val sx = p.first.toFloat(); val sy = p.second.toFloat(); val sz = p.third
            val r = atoms[idx].radius * sc / 18f * (1f + sz * 0.001f).coerceIn(0.6f, 1.4f)

            // Glow
            val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
            glowP.color = Color.argb(((1f + sz * 0.005f).coerceIn(0.3f, 1f) * 35).toInt().coerceIn(10, 50), Color.red(atoms[idx].color), Color.green(atoms[idx].color), Color.blue(atoms[idx].color))
            canvas.drawCircle(sx, sy, r * 2.2f, glowP)

            // Atom sphere (gradient)
            val sphereP = Paint(Paint.ANTI_ALIAS_FLAG)
            sphereP.shader = RadialGradient(sx - r * 0.3f, sy - r * 0.3f, r * 1.5f,
                intArrayOf(Color.argb(255, minOf(255, Color.red(atoms[idx].color) + 80), minOf(255, Color.green(atoms[idx].color) + 80), minOf(255, Color.blue(atoms[idx].color) + 80)),
                    atoms[idx].color, Color.argb(255, maxOf(0, Color.red(atoms[idx].color) - 60), maxOf(0, Color.green(atoms[idx].color) - 60), maxOf(0, Color.blue(atoms[idx].color) - 60))),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            sphereP.isAntiAlias = true
            canvas.drawCircle(sx, sy, r, sphereP)

            // Outline
            canvas.drawCircle(sx, sy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 30, 30); style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true })

            // Highlight
            val hlP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
            hlP.color = Color.argb(120, 255, 255, 255)
            canvas.drawCircle(sx - r * 0.25f, sy - r * 0.25f, r * 0.3f, hlP)

            // Symbol
            val elP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = r * 1.1f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
            canvas.drawText(atoms[idx].symbol, sx, sy + elP.textSize * 0.35f, elP)
        }

        // Molecule name
        val np = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = h * 0.04f; textAlign = Paint.Align.CENTER; color = Color.rgb(0, 240, 255); isFakeBoldText = true; isAntiAlias = true }
        canvas.drawText("${molInfos[molIndex].name} (${molInfos[molIndex].formula})", cx, h * 0.88f, np)

        // Info
        val ip = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = h * 0.025f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        ip.color = Color.rgb(200, 200, 200)
        canvas.drawText("Geometri: ${molInfos[molIndex].geometry} | Açı: ${molInfos[molIndex].angle} | ${molInfos[molIndex].polar}", cx, h * 0.92f, ip)

        canvas.restore()

        // Info panel overlay
        if (showInfo) drawInfo(canvas, w, h)
    }

    private fun drawInfo(c: Canvas, w: Float, h: Float) {
        val px = w * 0.03f; val py = 8f; val pw = w * 0.94f; val ph = h - 16f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 39); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.rgb(0, 200, 255); isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG); hp.textSize = 22f; hp.textAlign = Paint.Align.CENTER; hp.color = Color.rgb(0, 240, 255); hp.isFakeBoldText = true; hp.isAntiAlias = true
        c.drawText("3D Molekül Görüntüleyici", w / 2f, ty, hp); ty += 38f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG); lp.textSize = 16f; lp.textAlign = Paint.Align.LEFT; lp.isAntiAlias = true
        val lines = listOf(
            Pair("═══ NEDİR? ═══", Color.rgb(0, 240, 255)),
            Pair("Molekulleri 3 boyutta görüntüleyin,", Color.rgb(220, 220, 220)),
            Pair("bağ türlerini ve geometriyi öğrenin.", Color.rgb(220, 220, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ GEOMETRİLER ═══", Color.rgb(0, 240, 255)),
            Pair("Doğrusal: 180° (CO₂, C₂H₂)", Color.rgb(170, 204, 255)),
            Pair("Düzlemsel üçgen: 120° (C₂H₄, C₆H₆)", Color.rgb(170, 204, 255)),
            Pair("Dört yüzlü: 109.5° (CH₄, C₂H₆)", Color.rgb(170, 204, 255)),
            Pair("Bükülü: 104.5° (H₂O)", Color.rgb(170, 204, 255)),
            Pair("Üçgen piramit: 107.3° (NH₃)", Color.rgb(170, 204, 255)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ BAĞ TÜRLERİ ═══", Color.rgb(0, 240, 255)),
            Pair("— Tek bağ: 1 sigma bağı", Color.rgb(140, 150, 160)),
            Pair("═ Çift bağ: 1 sigma + 1 pi bağı", Color.rgb(0, 200, 255)),
            Pair("≡ Üçlü bağ: 1 sigma + 2 pi bağı", Color.rgb(255, 0, 128)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", Color.rgb(0, 240, 255)),
            Pair("1. Alt düğümlerden molekül seçin", Color.rgb(200, 230, 255)),
            Pair("2. Kaydırarak döndürün (otomatik döner)", Color.rgb(200, 230, 255)),
            Pair("3. Çimdikleme ile yakınlaştırın", Color.rgb(200, 230, 255))
        )
        for ((line, color) in lines) { if (line.isEmpty()) { ty += 6f; continue }; lp.color = color; c.drawText(line, px + 18f, ty, lp); ty += 22f }
    }
}

class Molecule3DFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10, 14, 23)); setPadding(12, 12, 12, 12) }
        val view = Molecule3DView(ctx)

        // Top bar
        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 4) }
        top.addView(TextView(ctx).apply { text = "3D Molekül"; textSize = 22f; setTextColor(Color.rgb(0, 240, 255)); setTypeface(null, android.graphics.Typeface.BOLD) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val helpBtn = TextView(ctx).apply { text = "?"; textSize = 26f; setTextColor(Color.rgb(0, 240, 255)); setPadding(20, 8, 20, 8); setBackgroundColor(Color.rgb(20, 30, 50)) }
        helpBtn.setOnClickListener { view.toggleInfo() }
        top.addView(helpBtn)
        root.addView(top)

        root.addView(view, LinearLayout.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.58f).toInt())

        // Molecule buttons
        val hScroll = HorizontalScrollView(ctx).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        val btnRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4, 4, 4, 4) }
        val btnNames = listOf("H₂O", "CO₂", "CH₄", "NH₃", "C₂H₆", "C₂H₄", "C₆H₆", "C₈H₁₈", "EtOH", "Aseton", "AcOH", "MeOH", "C₂H₂", "Toluen")
        val btnIds = mutableListOf<TextView>()
        btnNames.forEachIndexed { i, name ->
            val btn = TextView(ctx).apply {
                text = name; textSize = 11f; setTextColor(Color.WHITE); setPadding(14, 8, 14, 8)
                setBackgroundColor(Color.rgb(40, 50, 70))
                setOnClickListener { btnIds.forEach { it.setBackgroundColor(Color.rgb(40, 50, 70)) }; setBackgroundColor(Color.rgb(0, 100, 180)); view.setMolecule(i) }
            }
            btnIds.add(btn); btnRow.addView(btn)
        }
        hScroll.addView(btnRow); root.addView(hScroll)

        // Info label
        val infoLabel = TextView(ctx).apply { textSize = 12f; setTextColor(Color.rgb(150, 170, 200)); gravity = Gravity.CENTER; setPadding(0, 6, 0, 0) }
        root.addView(infoLabel)

        // Atom legend
        val legend = TextView(ctx).apply {
            text = "C = Karbon | H = Hidrojen | O = Oksijen | N = Azot"; textSize = 10f; setTextColor(Color.rgb(100, 100, 100)); gravity = Gravity.CENTER; setPadding(0, 4, 0, 0)
        }
        root.addView(legend)

        btnIds[0].setBackgroundColor(Color.rgb(0, 100, 180))
        view.onMoleculeChange = { idx ->
            val info = view.molInfos[idx]
            infoLabel.text = "${info.name} — Geometri: ${info.geometry}, Açı: ${info.angle}, ${info.polar}\n${info.desc}"
        }
        view.setMolecule(0)

        return root
    }
}
