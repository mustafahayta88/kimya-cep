package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

data class VSEPRGeo(
    val name: String, val nameEn: String, val bondPairs: Int, val lonePairs: Int,
    val angle: String, val info: String, val polar: String,
    val example: String, val hybrid: String, val vseprType: String,
    val atoms: List<GAtom>, val bonds: List<Pair<Int, Int>>
)

data class GAtom(val x: Float, val y: Float, val z: Float, val symbol: String, val color: Int, val radius: Float, val isCenter: Boolean = false)

class Geo3DView(context: Context) : View(context) {
    private var geo: VSEPRGeo? = null
    private var rotX = 0.3f; private var rotY = 0.4f
    private var autoRotate = true
    private var zoomScale = 1f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt() }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; isAntiAlias = true }
    private val elP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF.toInt(); style = Paint.Style.FILL }
    private val loneP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL; isAntiAlias = true }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x15FFFFFF.toInt(); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val angleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 20f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    init {
        isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    fun setGeo(g: VSEPRGeo) { geo = g; rotX = 0.3f; rotY = 0.4f; zoomScale = 1f; invalidate() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                lastTx = e.x; lastTy = e.y; tMode = 1; autoRotate = false
                (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (tMode == 1 && e.pointerCount == 1) {
                    rotY += (e.x - lastTx) * 0.04f
                    rotX += (e.y - lastTy) * 0.04f
                    rotX = rotX.coerceIn(-PI.toFloat(), PI.toFloat())
                    invalidate()
                }; lastTx = e.x; lastTy = e.y
            }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                (parent as? ViewGroup)?.requestDisallowInterceptTouchEvent(false)
                if (tMode == 1) { autoRotate = true; postInvalidateOnAnimation() }
                tMode = 0
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        canvas.save()
        canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f)
        val cx = w / 2f; val cy = h * 0.45f
        val sc = w * 0.012f

        if (autoRotate) { rotY += 0.02f; postInvalidateOnAnimation() }

        val g = geo ?: return

        for (i in -4..4) {
            val f = i.toFloat() / 4f
            canvas.drawLine(cx + f * sc * 5, cy - 4 * sc, cx + f * sc * 5, cy + 4 * sc, gridP)
            canvas.drawLine(cx - 4 * sc, cy + f * sc * 5, cx + 4 * sc, cy + f * sc * 5, gridP)
        }

        val proj = g.atoms.map { a ->
            var x = a.x * cos(rotY) - a.z * sin(rotY)
            var y = a.x * sin(rotY) * sin(rotX) + a.y * cos(rotX) + a.z * cos(rotY) * sin(rotX)
            val z = a.x * sin(rotY) * cos(rotX) - a.y * sin(rotX) + a.z * cos(rotY) * cos(rotX)
            Triple(x * sc + cx, y * sc + cy, z)
        }

        for ((a, b) in g.bonds) {
            if (a < proj.size && b < proj.size) {
                val p1 = proj[a]; val p2 = proj[b]
                canvas.drawLine(p1.first.toFloat(), p1.second.toFloat(), p2.first.toFloat(), p2.second.toFloat(), bondP)
            }
        }

        val sorted = proj.withIndex().sortedBy { -it.value.third }
        for ((idx, p) in sorted) {
            if (idx >= g.atoms.size) continue
            val atom = g.atoms[idx]
            val sx = p.first.toFloat(); val sy = p.second.toFloat(); val sz = p.third
            val r = atom.radius * sc / 18f * (1f + sz * 0.001f).coerceIn(0.6f, 1.4f)

            if (atom.isCenter) {
                canvas.drawCircle(sx, sy, r * 2f, glowP.apply { alpha = 40 })
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = atom.color; style = Paint.Style.FILL }
            canvas.drawCircle(sx, sy, r, paint)
            canvas.drawCircle(sx, sy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f })

            elP.textSize = r * 1.1f
            canvas.drawText(atom.symbol, sx, sy + elP.textSize / 3f, elP)

            if (atom.isCenter && g.lonePairs > 0) {
                val loneAngle = Math.toRadians((-90.0 + idx * 45))
                val lx = sx + (r * 2.5f * cos(loneAngle)).toFloat()
                val ly = sy + (r * 2.5f * sin(loneAngle)).toFloat()
                canvas.drawCircle(lx, ly, r * 0.3f, loneP)
                canvas.drawCircle(lx + r * 0.5f, ly, r * 0.3f, loneP)
            }
        }

        angleP.textSize = 20f
        canvas.drawText(g.angle, cx, h - 20f, angleP)
        canvas.restore()
    }
}

class MolekulGeometriFragment : Fragment() {

    private val geos = listOf(
        VSEPRGeo("Doğrusal", "Linear", 2, 0, "180°",
            "2 bağ çifti, 0 yalnız çift\nsp hibritleşme\n\nÖrnekler: CO₂, BeCl₂, CS₂",
            "Polar olmayan", "CO₂, BeCl₂", "sp", "AX₂",
            listOf(GAtom(0f, 0f, 0f, "C", 0xFF6B6B6B.toInt(), 16f, true),
                   GAtom(25f, 0f, 0f, "O", 0xFFFF3333.toInt(), 18f),
                   GAtom(-25f, 0f, 0f, "O", 0xFFFF3333.toInt(), 18f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("Düzenli üçgen", "Trigonal Planar", 3, 0, "120°",
            "3 bağ çifti, 0 yalnız çift\nsp² hibritleşme\n\nÖrnekler: BF₃, AlCl₃, NO₃⁻",
            "Polar olmayan", "BF₃, AlCl₃", "sp²", "AX₃",
            listOf(GAtom(0f, 0f, 0f, "B", 0xFFFFB5B5.toInt(), 16f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF90E050.toInt(), 17f),
                   GAtom(19f, 11f, 0f, "F", 0xFF90E050.toInt(), 17f),
                   GAtom(-19f, 11f, 0f, "F", 0xFF90E050.toInt(), 17f)),
            listOf(0 to 1, 0 to 2, 0 to 3)),
        VSEPRGeo("Bükülü", "Bent (2p)", 2, 1, "<120°",
            "2 bağ çifti, 1 yalnız çift\nsp² hibritleşme\n\nÖrnekler: SO₂, O₃",
            "Polar molekül", "SO₂, O₃", "sp²", "AX₂E",
            listOf(GAtom(0f, 0f, 0f, "S", 0xFFFFFF30.toInt(), 18f, true),
                   GAtom(20f, -14f, 0f, "O", 0xFFFF3333.toInt(), 17f),
                   GAtom(-20f, -14f, 0f, "O", 0xFFFF3333.toInt(), 17f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("Trigonal-piramit", "Trigonal Pyramidal", 3, 1, "<109.5°",
            "3 bağ çifti, 1 yalnız çift\nsp³ hibritleşme\n\nÖrnekler: NH₃, PCl₃, AsH₃",
            "Polar molekül", "NH₃, PCl₃", "sp³", "AX₃E",
            listOf(GAtom(0f, -5f, 0f, "N", 0xFF3050F8.toInt(), 16f, true),
                   GAtom(20f, 12f, 0f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-16f, 16f, 0f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-4f, 12f, -18f, "H", 0xFFFFFFFF.toInt(), 12f)),
            listOf(0 to 1, 0 to 2, 0 to 3)),
        VSEPRGeo("Dörtgen", "Tetrahedral", 4, 0, "109.5°",
            "4 bağ çifti, 0 yalnız çift\nsp³ hibritleşme\n\nÖrnekler: CH₄, CCl₄, NH₄⁺",
            "Polar olmayan", "CH₄, CCl₄", "sp³", "AX₄",
            listOf(GAtom(0f, 0f, 0f, "C", 0xFF6B6B6B.toInt(), 16f, true),
                   GAtom(18f, 18f, 18f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-18f, -18f, 18f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-18f, 18f, -18f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(18f, -18f, -18f, "H", 0xFFFFFFFF.toInt(), 12f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Bükülü (4p)", "Bent (4p)", 2, 2, "<109.5°",
            "2 bağ çifti, 2 yalnız çift\nsp³ hibritleşme\n\nÖrnekler: H₂O, H₂S, OF₂",
            "Polar molekül", "H₂O, H₂S", "sp³", "AX₂E₂",
            listOf(GAtom(0f, 0f, 0f, "O", 0xFFFF3333.toInt(), 18f, true),
                   GAtom(20f, -14f, 0f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-20f, -14f, 0f, "H", 0xFFFFFFFF.toInt(), 12f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("T-şekilli", "T-Shaped", 3, 2, "<90°",
            "3 bağ çifti, 2 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: ClF₃, BrF₃",
            "Polar molekül", "ClF₃, BrF₃", "sp³d", "AX₃E₂",
            listOf(GAtom(0f, 0f, 0f, "Cl", 0xFF1FF01F.toInt(), 18f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3)),
        VSEPRGeo("Testere", "Seesaw", 4, 1, "90°/120°",
            "4 bağ çifti, 1 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: SF₄, TeCl₄",
            "Polar molekül", "SF₄, TeCl₄", "sp³d", "AX₄E",
            listOf(GAtom(0f, 0f, 0f, "S", 0xFFFFFF30.toInt(), 18f, true),
                   GAtom(0f, -20f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(20f, 8f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-10f, 8f, 17f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-10f, 8f, -17f, "F", 0xFF90E050.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Trigonal-bipyramit", "Trigonal Bipyramidal", 5, 0, "90°/120°",
            "5 bağ çifti, 0 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: PCl₅, AsF₅",
            "Polar olmayan", "PCl₅, AsF₅", "sp³d", "AX₅",
            listOf(GAtom(0f, 0f, 0f, "P", 0xFFFF8000.toInt(), 18f, true),
                   GAtom(0f, -28f, 0f, "Cl", 0xFF1FF01F.toInt(), 15f),
                   GAtom(0f, 28f, 0f, "Cl", 0xFF1FF01F.toInt(), 15f),
                   GAtom(26f, 0f, 0f, "Cl", 0xFF1FF01F.toInt(), 15f),
                   GAtom(-13f, 0f, 22f, "Cl", 0xFF1FF01F.toInt(), 15f),
                   GAtom(-13f, 0f, -22f, "Cl", 0xFF1FF01F.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5)),
        VSEPRGeo("Kare-düzlem", "Square Planar", 4, 2, "90°",
            "4 bağ çifti, 2 yalnız çift\nsp³d² hibritleşme\n\nÖrnekler: XeF₄, ICl₄⁻",
            "Polar olmayan", "XeF₄, ICl₄⁻", "sp³d²", "AX₄E₂",
            listOf(GAtom(0f, 0f, 0f, "Xe", 0xFF00BFFF.toInt(), 20f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(0f, 22f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Kare-piramit", "Square Pyramidal", 5, 1, "<90°",
            "5 bağ çifti, 1 yalnız çift\nsp³d² hibritleşme\n\nÖrnekler: BrF₅, ClF₅",
            "Polar molekül", "BrF₅, ClF₅", "sp³d²", "AX₅E",
            listOf(GAtom(0f, 0f, 0f, "Br", 0xFF8B2500.toInt(), 18f, true),
                   GAtom(0f, -22f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(0f, 0f, 22f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(0f, 22f, 0f, "F", 0xFF90E050.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5)),
        VSEPRGeo("Oktahedral", "Octahedral", 6, 0, "90°",
            "6 bağ çifti, 0 yalnız çift\nsp³d² hibritleşme\n\nÖrnekler: SF₆, PF₆⁻",
            "Polar olmayan", "SF₆, PF₆⁻", "sp³d²", "AX₆",
            listOf(GAtom(0f, 0f, 0f, "S", 0xFFFFFF30.toInt(), 18f, true),
                   GAtom(0f, -24f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(0f, 24f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(24f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-24f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(0f, 0f, 24f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(0f, 0f, -24f, "F", 0xFF90E050.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6)),
        VSEPRGeo("Doğrusal (3ç)", "Linear (3lp)", 2, 3, "180°",
            "2 bağ çifti, 3 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: XeF₂, I₃⁻",
            "Polar olmayan", "XeF₂, I₃⁻", "sp³d", "AX₂E₃",
            listOf(GAtom(0f, 0f, 0f, "Xe", 0xFF00BFFF.toInt(), 20f, true),
                   GAtom(28f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f),
                   GAtom(-28f, 0f, 0f, "F", 0xFF90E050.toInt(), 15f)),
            listOf(0 to 1, 0 to 2)),
        VSEPRGeo("Pentagonal-bipyramit", "Pentagonal Bipyramidal", 7, 0, "72°/90°",
            "7 bağ çifti, 0 yalnız çift\nsp³d³ hibritleşme\n\nÖrnekler: IF₇, ZrF₇³⁻\n5 equatorial + 2 axial",
            "Polar olmayan", "IF₇", "sp³d³", "AX₇",
            listOf(GAtom(0f, 0f, 0f, "I", 0xFF9400D4.toInt(), 18f, true),
                   GAtom(0f, -28f, 0f, "F", 0xFF90E050.toInt(), 14f),
                   GAtom(0f, 28f, 0f, "F", 0xFF90E050.toInt(), 14f),
                   GAtom(26f, 0f, 0f, "F", 0xFF90E050.toInt(), 14f),
                   GAtom(8f, 0f, 24f, "F", 0xFF90E050.toInt(), 14f),
                   GAtom(-21f, 0f, 15f, "F", 0xFF90E050.toInt(), 14f),
                   GAtom(-21f, 0f, -15f, "F", 0xFF90E050.toInt(), 14f),
                   GAtom(8f, 0f, -24f, "F", 0xFF90E050.toInt(), 14f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7)),
        VSEPRGeo("Trigonal-prizma", "Trigonal Prismatic", 6, 0, "90°",
            "6 bağ çifti, 0 yalnız çift\nd³sp² hibritleşme\n\nÖrnekler: Mo(SMe₂)₆, W(CH₃)₆\nPrizmatik geometri",
            "Polar olmayan", "Mo(SMe₂)₆", "d³sp²", "AX₆ (prizma)",
            listOf(GAtom(0f, 0f, 0f, "Mo", 0xFF6B6B6B.toInt(), 18f, true),
                   GAtom(18f, -10f, 15f, "S", 0xFFFFFF30.toInt(), 15f),
                   GAtom(-18f, -10f, 15f, "S", 0xFFFFFF30.toInt(), 15f),
                   GAtom(0f, -10f, -20f, "S", 0xFFFFFF30.toInt(), 15f),
                   GAtom(18f, 10f, 15f, "S", 0xFFFFFF30.toInt(), 15f),
                   GAtom(-18f, 10f, 15f, "S", 0xFFFFFF30.toInt(), 15f),
                   GAtom(0f, 10f, -20f, "S", 0xFFFFFF30.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6)),
        VSEPRGeo("Kare-antiprizma", "Square Antiprismatic", 8, 0, "Variable",
            "8 bağ çifti, 0 yalnız çift\nd⁴sp³ hibritleşme\n\nÖrnekler: TaF₈³⁻, ZrF₈⁴⁻\n2 kare yüzey birbirine 45° döndürülmüş",
            "Polar olmayan", "TaF₈³⁻", "d⁴sp³", "AX₈ (antiprizma)",
            listOf(GAtom(0f, 0f, 0f, "Ta", 0xFF6B6B6B.toInt(), 18f, true),
                   GAtom(16f, -16f, 16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(-16f, -16f, 16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(-16f, 16f, 16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(16f, 16f, 16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(22f, 0f, -16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(0f, -22f, -16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(-22f, 0f, -16f, "F", 0xFF90E050.toInt(), 13f),
                   GAtom(0f, 22f, -16f, "F", 0xFF90E050.toInt(), 13f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8)),
        VSEPRGeo("Dodekahedral", "Dodecahedral", 8, 0, "Variable",
            "8 bağ çifti, 0 yalnız çift\n\nÖrnekler: Mo(CN)₈⁴⁻\n8 köşeli dodekaedron",
            "Polar olmayan", "Mo(CN)₈⁴⁻", "d⁴sp³", "AX₈ (dodekaedron)",
            listOf(GAtom(0f, 0f, 0f, "Mo", 0xFF6B6B6B.toInt(), 18f, true),
                   GAtom(20f, -12f, 10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(-20f, -12f, 10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(-20f, 12f, -10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(20f, 12f, -10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(12f, -20f, -10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(-12f, -20f, -10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(-12f, 20f, 10f, "C", 0xFF555555.toInt(), 13f),
                   GAtom(12f, 20f, 10f, "C", 0xFF555555.toInt(), 13f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8)),
        VSEPRGeo("Tricapped-prizma", "Tricapped Trigonal Prismatic", 9, 0, "Variable",
            "9 bağ çifti, 0 yalnız çift\n\nÖrnekler: ReH₉²⁻, TcH₉²⁻\n9 bağlı en karmaşık geometri",
            "Polar olmayan", "ReH₉²⁻", "d⁴sp³", "AX₉",
            listOf(GAtom(0f, 0f, 0f, "Re", 0xFF6B6B6B.toInt(), 18f, true),
                   GAtom(18f, -10f, 14f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-18f, -10f, 14f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(0f, -10f, -18f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(18f, 10f, 14f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(-18f, 10f, 14f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(0f, 10f, -18f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(0f, -24f, 0f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(0f, 24f, 0f, "H", 0xFFFFFFFF.toInt(), 12f),
                   GAtom(0f, 0f, 26f, "H", 0xFFFFFFFF.toInt(), 12f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7, 0 to 8, 0 to 9)),
        VSEPRGeo("Testere (3ç)", "Seesaw (3lp)", 4, 3, "Variable",
            "4 bağ çifti, 3 yalnız çift\nsp³d hibritleşme\n\nÖrnekler: ClO₄⁻(tahmini)\nKarmaşık yalnız çift düzeni",
            "Değişken", "—", "sp³d", "AX₄E₃",
            listOf(GAtom(0f, 0f, 0f, "Cl", 0xFF1FF01F.toInt(), 18f, true),
                   GAtom(0f, -22f, 0f, "O", 0xFFFF3333.toInt(), 15f),
                   GAtom(22f, 0f, 0f, "O", 0xFFFF3333.toInt(), 15f),
                   GAtom(-22f, 0f, 0f, "O", 0xFFFF3333.toInt(), 15f),
                   GAtom(0f, 0f, 22f, "O", 0xFFFF3333.toInt(), 15f)),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4)),
        VSEPRGeo("Küre", "Spherical", 0, 0, "—",
            "Tüm elektronlar yalnız çift\nHiç bağ yok\n\nÖrnekler: Nadir gazlar (He, Ne, Ar)\nTam simetrik küresel yapı",
            "Polar olmayan", "He, Ne, Ar", "—", "AX₀",
            listOf(GAtom(0f, 0f, 0f, "He", 0xFF00F0FF.toInt(), 22f, true)),
            emptyList())
    )

    private var currentIndex = 0
    private lateinit var geoView: Geo3DView
    private lateinit var tvName: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvCounter: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_molekul_geometri, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geoView = Geo3DView(requireContext())
        tvName = view.findViewById(R.id.tv_geo_name)
        tvInfo = view.findViewById(R.id.tv_geo_info)
        tvCounter = view.findViewById(R.id.tv_geo_counter)

        val placeholder = view.findViewById<View>(R.id.geo_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        parent.addView(geoView, idx, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (300 * resources.displayMetrics.density).toInt()))

        view.findViewById<Button>(R.id.btn_geo_prev).setOnClickListener {
            currentIndex = (currentIndex - 1 + geos.size) % geos.size; showGeo()
        }
        view.findViewById<Button>(R.id.btn_geo_next).setOnClickListener {
            currentIndex = (currentIndex + 1) % geos.size; showGeo()
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Molekül Geometrisi Aracı Yardımı")
                .setMessage(buildString {
                    appendLine("Bu araç VSEPR geometrilerini 3 Boyutta gösterir.")
                    appendLine()
                    appendLine("3B Etkileşim:")
                    appendLine("• Sürükle: Molekülü her eksende döndür")
                    appendLine("• Çift parmak: Yakınlaştır/uzaklaştır")
                    appendLine("• Parmağını kaldır → otomatik döndürme başlar")
                    appendLine()
                    appendLine("Toplam ${geos.size} geometri mevcut.")
                })
                .setPositiveButton("Anladım", null)
                .show()
        }

        showGeo()
    }

    private fun showGeo() {
        val g = geos[currentIndex]
        tvName.text = "${g.name} (${g.nameEn})"
        tvInfo.text = "${g.info}\n\nVSEPR: ${g.vseprType}\nHibritleşme: ${g.hybrid}\nPolarite: ${g.polar}"
        tvCounter.text = "${currentIndex + 1} / ${geos.size}"
        geoView.setGeo(g)
    }
}
