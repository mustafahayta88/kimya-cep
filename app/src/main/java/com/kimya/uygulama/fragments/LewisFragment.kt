package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

data class LewisMolecule(
    val name: String, val formula: String, val info: String,
    val center: Pair<Float, Float>,
    val atoms: List<Triple<Float, Float, String>>,
    val bonds: List<Pair<Int, Int>>,
    val bondOrders: List<Int> = List(bonds.size) { 1 },
    val lonePairCounts: List<Int>,
    val centerAtom: Int = 0,
    val resonance: Boolean = false,
    val charge: String = ""
)

class LewisCanvasView(context: Context) : View(context) {
    private var molecule: LewisMolecule? = null

    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt() }
    private val atomBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF222222.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val bondP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 36f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val loneP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.FILL }
    private val lonePairLineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x44000000.toInt(); style = Paint.Style.FILL }
    private val chargeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF3333.toInt(); textSize = 24f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val resonanceP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x4439FF14.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f; pathEffect = null }

    private val atomColors = mapOf(
        "H" to 0xFFCCCCCC.toInt(), "O" to 0xFFFF3333.toInt(), "N" to 0xFF3050F8.toInt(),
        "F" to 0xFF90E050.toInt(), "C" to 0xFF555555.toInt(), "Cl" to 0xFF1FF01F.toInt(),
        "Br" to 0xFF8B2500.toInt(), "S" to 0xFFFFFF30.toInt(), "P" to 0xFFFF8000.toInt(),
        "I" to 0xFF9400D4.toInt(), "Xe" to 0xFF00BFFF.toInt(), "Be" to 0xFFC0FF00.toInt(),
        "B" to 0xFFffb5b5.toInt(), "Na" to 0xFFAB5CF2.toInt()
    )

    fun setMolecule(mol: LewisMolecule) { molecule = mol; invalidate() }

    private fun getAtomPaint(el: String): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = atomColors[el] ?: 0xFF6B6B6B.toInt()
            style = Paint.Style.FILL
        }
        return p
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)

        val mol = molecule ?: return
        val scale = minOf(w / 420f, h / 420f)
        val ox = w / 2f - mol.center.first * scale
        val oy = h / 2f - mol.center.second * scale

        for ((bondIdx, bond) in mol.bonds.withIndex()) {
            if (bond.first >= mol.atoms.size || bond.second >= mol.atoms.size) continue
            val (x1, y1, _) = mol.atoms[bond.first]
            val (x2, y2, _) = mol.atoms[bond.second]
            val sx1 = x1 * scale + ox; val sy1 = y1 * scale + oy
            val sx2 = x2 * scale + ox; val sy2 = y2 * scale + oy
            val order = mol.bondOrders.getOrElse(bondIdx) { 1 }
            val dx = sx2 - sx1; val dy = sy2 - sy1
            val len = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (len == 0f) continue
            val nx = -dy / len; val ny = dx / len

            when (order) {
                1 -> canvas.drawLine(sx1, sy1, sx2, sy2, bondP)
                2 -> {
                    val off = 4f * scale / 1.3f
                    canvas.drawLine(sx1 + nx * off, sy1 + ny * off, sx2 + nx * off, sy2 + ny * off, bondP)
                    canvas.drawLine(sx1 - nx * off, sy1 - ny * off, sx2 - nx * off, sy2 - ny * off, bondP)
                }
                3 -> {
                    val off = 5f * scale / 1.3f
                    canvas.drawLine(sx1, sy1, sx2, sy2, bondP)
                    canvas.drawLine(sx1 + nx * off, sy1 + ny * off, sx2 + nx * off, sy2 + ny * off, bondP)
                    canvas.drawLine(sx1 - nx * off, sy1 - ny * off, sx2 - nx * off, sy2 - ny * off, bondP)
                }
            }
        }

        for (i in mol.atoms.indices) {
            val (x, y, el) = mol.atoms[i]
            val sx = x * scale + ox; val sy = y * scale + oy
            val baseR = when (el) { "H" -> 20f; "O", "N", "C" -> 30f; "S", "P" -> 34f; else -> 32f }
            val r = baseR * scale / 1.3f

            canvas.drawCircle(sx + 3f, sy + 3f, r, shadowP)

            val paint = getAtomPaint(el)
            canvas.drawCircle(sx, sy, r, paint)
            canvas.drawCircle(sx, sy, r, atomBorder)

            textP.textSize = r * 0.85f
            val displayEl = if (el == "Cl") "Cl" else if (el == "Br") "Br" else el
            canvas.drawText(displayEl, sx, sy + textP.textSize * 0.35f, textP)

            val lpc = mol.lonePairCounts.getOrElse(i) { 0 }
            if (lpc > 0) {
                val dotR = r * 0.28f
                val offset = r + 12f * scale / 1.3f
                when (lpc) {
                    1 -> {
                        canvas.drawCircle(sx, sy - offset, dotR, loneP)
                        canvas.drawCircle(sx + dotR * 1.8f, sy - offset, dotR, loneP)
                    }
                    2 -> {
                        canvas.drawCircle(sx, sy - offset, dotR, loneP)
                        canvas.drawCircle(sx + dotR * 1.8f, sy - offset, dotR, loneP)
                        canvas.drawCircle(sx, sy + offset, dotR, loneP)
                        canvas.drawCircle(sx + dotR * 1.8f, sy + offset, dotR, loneP)
                    }
                    3 -> {
                        canvas.drawCircle(sx - offset, sy, dotR, loneP)
                        canvas.drawCircle(sx - offset + dotR * 1.8f, sy, dotR, loneP)
                        canvas.drawCircle(sx + offset, sy, dotR, loneP)
                        canvas.drawCircle(sx + offset + dotR * 1.8f, sy, dotR, loneP)
                        canvas.drawCircle(sx, sy - offset, dotR, loneP)
                        canvas.drawCircle(sx + dotR * 1.8f, sy - offset, dotR, loneP)
                    }
                }
            }
        }

        if (mol.resonance) {
            labelP.textSize = 18f
            canvas.drawText("⚡ Rezonans yapıları mevcut", w / 2f, h - 12f, labelP)
        }

        if (mol.charge.isNotEmpty()) {
            chargeP.textSize = 22f
            canvas.drawText(mol.charge, w - 30f, 30f, chargeP)
        }
    }
}

class LewisFragment : Fragment() {

    private val molecules = listOf(
        LewisMolecule("H₂O", "H₂O",
            "Toplam valans elektronu: 8\nMerkez atom: O (2 bağ, 2 yalnız çift)\nB geometrisi (104.5°)\nPolar molekül\n\nHibritleşme: sp³",
            0f to 0f,
            listOf(Triple(0f, -80f, "O"), Triple(-70f, 40f, "H"), Triple(70f, 40f, "H")),
            listOf(0 to 1, 0 to 2), listOf(1, 1), listOf(2, 0, 0), 0),
        LewisMolecule("CO₂", "CO₂",
            "Toplam valans elektronu: 16\nMerkez atom: C (2 çift bağ)\nDoğrusal geometri (180°)\nPolar olmayan molekül\n\nHibritleşme: sp",
            0f to 0f,
            listOf(Triple(-100f, 0f, "O"), Triple(0f, 0f, "C"), Triple(100f, 0f, "O")),
            listOf(0 to 1, 1 to 2), listOf(2, 1, 2), listOf(2, 0, 2), 1),
        LewisMolecule("NH₃", "NH₃",
            "Toplam valans elektronu: 8\nMerkez atom: N (3 bağ, 1 yalnız çift)\nPiramit geometrisi (107°)\nPolar molekül\n\nHibritleşme: sp³",
            0f to 0f,
            listOf(Triple(0f, -80f, "N"), Triple(-70f, 40f, "H"), Triple(70f, 40f, "H"), Triple(0f, 90f, "H")),
            listOf(0 to 1, 0 to 2, 0 to 3), listOf(1, 1, 1, 1), listOf(1, 0, 0, 0), 0),
        LewisMolecule("CH₄", "CH₄",
            "Toplam valans elektronu: 8\nMerkez atom: C (4 bağ)\nDörtgen geometri (109.5°)\nPolar olmayan molekül\n\nHibritleşme: sp³",
            0f to 0f,
            listOf(Triple(0f, -80f, "C"), Triple(-80f, 20f, "H"), Triple(80f, 20f, "H"), Triple(0f, 90f, "H")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4), listOf(1, 1, 1, 1, 1), listOf(0, 0, 0, 0, 0), 0),
        LewisMolecule("HF", "HF",
            "Toplam valans elektronu: 8\nDoğrusal (2 atom)\nPolar molekül\nH-F bağı çok güçlü (567 kJ/mol)",
            0f to 0f,
            listOf(Triple(-50f, 0f, "H"), Triple(50f, 0f, "F")),
            listOf(0 to 1), listOf(1, 1), listOf(0, 3), 0),
        LewisMolecule("HCl", "HCl",
            "Toplam valans elektronu: 8\nDoğrusal (2 atom)\nPolar molekül",
            0f to 0f,
            listOf(Triple(-60f, 0f, "H"), Triple(60f, 0f, "Cl")),
            listOf(0 to 1), listOf(1, 1), listOf(0, 3), 0),
        LewisMolecule("CCl₄", "CCl₄",
            "Toplam valans elektronu: 32\nMerkez atom: C (4 bağ)\nDörtgen geometri\nPolar olmayan molekül",
            0f to 0f,
            listOf(Triple(0f, -80f, "C"), Triple(-80f, 0f, "Cl"), Triple(80f, 0f, "Cl"), Triple(0f, 80f, "Cl")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4), listOf(1, 1, 1, 1, 1), listOf(0, 3, 3, 3, 0), 0),
        LewisMolecule("O₂", "O₂",
            "Toplam valans elektronu: 12\nÇift bağ (O=O)\nPolar olmayan molekül\nParamanyetik",
            0f to 0f,
            listOf(Triple(-55f, 0f, "O"), Triple(55f, 0f, "O")),
            listOf(0 to 1), listOf(2), listOf(2, 2), 0),
        LewisMolecule("N₂", "N₂",
            "Toplam valans elektronu: 10\nÜçlü bağ (N≡N)\nPolar olmayan molekül\nÇok güçlü bağ (945 kJ/mol)",
            0f to 0f,
            listOf(Triple(-55f, 0f, "N"), Triple(55f, 0f, "N")),
            listOf(0 to 1), listOf(3), listOf(1, 1), 0),
        LewisMolecule("SO₂", "SO₂",
            "Toplam valans elektronu: 18\nMerkez atom: S (çift bağ + yalnız çift)\nBükülü geometri (119°)\nPolar molekül\nRezonans yapıları var",
            0f to 0f,
            listOf(Triple(-100f, 30f, "O"), Triple(0f, 0f, "S"), Triple(100f, 30f, "O")),
            listOf(0 to 1, 1 to 2), listOf(2, 1, 2), listOf(2, 1, 2), 1, resonance = true),
        LewisMolecule("HCN", "HCN",
            "Toplam valans elektronu: 10\nMerkez atom: C (3lü bağ N ile)\nDoğrusal geometri\nPolar molekül",
            0f to 0f,
            listOf(Triple(-80f, 0f, "H"), Triple(0f, 0f, "C"), Triple(80f, 0f, "N")),
            listOf(0 to 1, 1 to 2), listOf(1, 1, 1), listOf(0, 0, 1), 1),
        LewisMolecule("NO₃⁻", "NO₃⁻",
            "Toplam valans elekt.: 24\nMerkez atom: N\nDüzenli üçgen geometri (120°)\nRezonans yapıları var\nYük: -1",
            0f to 0f,
            listOf(Triple(0f, -80f, "N"), Triple(-80f, 50f, "O"), Triple(80f, 50f, "O"), Triple(0f, 80f, "O")),
            listOf(0 to 1, 0 to 2, 0 to 3), listOf(2, 1, 1, 1), listOf(1, 2, 2, 2), 0, resonance = true, charge = "-1"),
        LewisMolecule("O₃", "O₃",
            "Toplam valans elektronu: 18\nMerkez atom: O (çift bağ + tek bağ)\nBükülü geometri (117°)\nPolar molekül\nRezonans yapıları var",
            0f to 0f,
            listOf(Triple(-90f, 20f, "O"), Triple(0f, 0f, "O"), Triple(90f, 20f, "O")),
            listOf(0 to 1, 1 to 2), listOf(2, 1, 2), listOf(2, 1, 2), 1, resonance = true),
        LewisMolecule("NO₂", "NO₂",
            "Toplam valans elektronu: 17\nMerkez atom: N (çift bağ + tek elektron)\nBükülü geometri (134°)\nRadikal molekül",
            0f to 0f,
            listOf(Triple(-80f, 20f, "O"), Triple(0f, 0f, "N"), Triple(80f, 20f, "O")),
            listOf(0 to 1, 1 to 2), listOf(2, 1, 2), listOf(2, 0, 2), 1, resonance = true),
        LewisMolecule("PCl₅", "PCl₅",
            "Toplam valans elektronu: 40\nMerkez atom: P (5 bağ)\nTrigonal-bipyramit geometri\nPolar olmayan molekül\n\nHibritleşme: sp³d",
            0f to 0f,
            listOf(Triple(0f, -80f, "P"), Triple(-90f, -20f, "Cl"), Triple(90f, -20f, "Cl"),
                   Triple(-60f, 60f, "Cl"), Triple(60f, 60f, "Cl"), Triple(0f, 100f, "Cl")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), listOf(1,1,1,1,1,1),
            listOf(0, 3, 3, 3, 3, 3), 0),
        LewisMolecule("SF₆", "SF₆",
            "Toplam valans elekt.: 48\nMerkez atom: S (6 bağ)\nOktahedral geometri\nPolar olmayan molekül\n\nHibritleşme: sp³d²",
            0f to 0f,
            listOf(Triple(0f, -90f, "S"), Triple(-80f, 0f, "F"), Triple(80f, 0f, "F"),
                   Triple(0f, 90f, "F"), Triple(-60f, -60f, "F"), Triple(60f, -60f, "F")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5, 0 to 6), listOf(1,1,1,1,1,1,1),
            listOf(0, 3, 3, 3, 3, 3, 3), 0),
        LewisMolecule("XeF₂", "XeF₂",
            "Toplam valans elekt.: 22\nMerkez atom: Xe (2 bağ, 3 yalnız çift)\nDoğrusal geometri\nPolar olmayan molekül\n\nHibritleşme: sp³d",
            0f to 0f,
            listOf(Triple(0f, 0f, "Xe"), Triple(-90f, 0f, "F"), Triple(90f, 0f, "F")),
            listOf(0 to 1, 0 to 2), listOf(1, 1, 1), listOf(3, 3, 3), 0),
        LewisMolecule("CO₃²⁻", "CO₃²⁻",
            "Toplam valans elekt.: 24\nMerkez atom: C\nDüzenli üçgen geometri (120°)\nRezonans yapıları var\nYük: -2",
            0f to 0f,
            listOf(Triple(0f, -80f, "C"), Triple(-80f, 50f, "O"), Triple(80f, 50f, "O"), Triple(0f, 80f, "O")),
            listOf(0 to 1, 0 to 2, 0 to 3), listOf(2, 1, 1, 1), listOf(0, 2, 2, 2), 0, resonance = true, charge = "-2"),
        LewisMolecule("SO₄²⁻", "SO₄²⁻",
            "Toplam valans elekt.: 32\nMerkez atom: S\nDörtgen geometri\nPolar olmayan (simetrik)\nYük: -2",
            0f to 0f,
            listOf(Triple(0f, -80f, "S"), Triple(-80f, 0f, "O"), Triple(80f, 0f, "O"),
                   Triple(0f, -30f, "O"), Triple(0f, 80f, "O")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), listOf(2, 1, 1, 1, 1),
            listOf(0, 2, 2, 2, 2, 2), 0, charge = "-2"),
        LewisMolecule("NH₄⁺", "NH₄⁺",
            "Toplam valans elekt.: 8\nMerkez atom: N (4 bağ)\nDörtgen geometri\nPolar olmayan (simetrik)\nYük: +1",
            0f to 0f,
            listOf(Triple(0f, -80f, "N"), Triple(-80f, 20f, "H"), Triple(80f, 20f, "H"),
                   Triple(0f, -40f, "H"), Triple(0f, 80f, "H")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), listOf(1, 1, 1, 1, 1),
            listOf(1, 0, 0, 0, 0, 0), 0, charge = "+1"),
        LewisMolecule("BeCl₂", "BeCl₂",
            "Toplam valans elekt.: 16\nMerkez atom: Be (2 bağ)\nDoğrusal geometri (180°)\nPolar olmayan molekül\n\nHibritleşme: sp",
            0f to 0f,
            listOf(Triple(0f, 0f, "Be"), Triple(-90f, 0f, "Cl"), Triple(90f, 0f, "Cl")),
            listOf(0 to 1, 0 to 2), listOf(1, 1, 1), listOf(0, 3, 3), 0),
        LewisMolecule("BF₃", "BF₃",
            "Toplam valans elekt.: 24\nMerkez atom: B (3 bağ)\nDüzenli üçgen geometri (120°)\nPolar olmayan molekül\n\nHibritleşme: sp²",
            0f to 0f,
            listOf(Triple(0f, -80f, "B"), Triple(-80f, 50f, "F"), Triple(80f, 50f, "F"), Triple(0f, 90f, "F")),
            listOf(0 to 1, 0 to 2, 0 to 3), listOf(1, 1, 1, 1), listOf(0, 3, 3, 3), 0),
        LewisMolecule("ClF₃", "ClF₃",
            "Toplam valans elekt.: 28\nMerkez atom: Cl (3 bağ, 2 yalnız çift)\nT-şekilli geometri\nPolar molekül\n\nHibritleşme: sp³d",
            0f to 0f,
            listOf(Triple(0f, 0f, "Cl"), Triple(-90f, 0f, "F"), Triple(90f, 0f, "F"), Triple(0f, -80f, "F")),
            listOf(0 to 1, 0 to 2, 0 to 3), listOf(1, 1, 1, 1), listOf(2, 3, 3, 3), 0),
        LewisMolecule("XeF₄", "XeF₄",
            "Toplam valans elekt.: 36\nMerkez atom: Xe (4 bağ, 2 yalnız çift)\nKare-düzlem geometri\nPolar olmayan (simetrik)\n\nHibritleşme: sp³d²",
            0f to 0f,
            listOf(Triple(0f, 0f, "Xe"), Triple(-90f, 0f, "F"), Triple(90f, 0f, "F"),
                   Triple(0f, -90f, "F"), Triple(0f, 90f, "F")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4), listOf(1, 1, 1, 1, 1),
            listOf(0, 3, 3, 3, 3, 2), 0),
        LewisMolecule("BrF₅", "BrF₅",
            "Toplam valans elekt.: 42\nMerkez atom: Br (5 bağ, 1 yalnız çift)\nKare-piramit geometri\nPolar molekül\n\nHibritleşme: sp³d²",
            0f to 0f,
            listOf(Triple(0f, -60f, "Br"), Triple(-80f, 10f, "F"), Triple(80f, 10f, "F"),
                   Triple(-60f, 70f, "F"), Triple(60f, 70f, "F"), Triple(0f, 100f, "F")),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 0 to 5), listOf(1, 1, 1, 1, 1, 1),
            listOf(0, 3, 3, 3, 3, 3, 2), 0),
        LewisMolecule("HCN", "HCN (Detaylı)",
            "Toplam valans elektronu: 10\nC≡N üçlü bağ\nC-H tek bağ\nDoğrusal (180°)\nPolar molekül\n\nDipol moment: 2.98 D",
            0f to 0f,
            listOf(Triple(-80f, 0f, "H"), Triple(0f, 0f, "C"), Triple(80f, 0f, "N")),
            listOf(0 to 1, 1 to 2), listOf(1, 3, 1), listOf(0, 0, 1), 1)
    )

    private var currentIndex = 0
    private lateinit var lewisView: LewisCanvasView
    private lateinit var tvName: TextView
    private lateinit var tvFormula: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvCounter: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lewis, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lewisView = LewisCanvasView(requireContext())
        tvName = view.findViewById(R.id.tv_lewis_name)
        tvFormula = view.findViewById(R.id.tv_lewis_formula)
        tvInfo = view.findViewById(R.id.tv_lewis_info)
        tvCounter = view.findViewById(R.id.tv_lewis_counter)

        val placeholder = view.findViewById<View>(R.id.lewis_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup
        val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        parent.addView(lewisView, idx, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (280 * resources.displayMetrics.density).toInt()))

        view.findViewById<Button>(R.id.btn_lewis_prev).setOnClickListener {
            currentIndex = (currentIndex - 1 + molecules.size) % molecules.size; showMolecule()
        }
        view.findViewById<Button>(R.id.btn_lewis_next).setOnClickListener {
            currentIndex = (currentIndex + 1) % molecules.size; showMolecule()
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            val msg = StringBuilder()
            msg.appendLine("Bu araç moleküllerin Lewis elektron yapısını gösterir.")
            msg.appendLine()
            msg.appendLine("Nasıl çizilir:")
            msg.appendLine("1. Toplam valans elektronu sayılır")
            msg.appendLine("2. Merkez atom belirlenir (en az elektronegatif)")
            msg.appendLine("3. Bağlar oluşturulur (her bağ 2 elektron)")
            msg.appendLine("4. Kalan elektronlar yalnız çift olarak yerleştirilir")
            msg.appendLine("5. Octet kuralı kontrol edilir (8 elektron)")
            msg.appendLine()
            msg.appendLine("Renkler:")
            msg.appendLine("• Gri: C atomu  • Kızıl: O atomu  • Mavi: N atomu")
            msg.appendLine("• Açık gri: H atomu  • Yeşil: Cl/F atomu  • Sarı: S atomu")
            msg.appendLine("• Turkuaz noktalar: Yalnız elektron çiftleri")
            msg.appendLine("• Çift çizgi: Çift bağ  • Üçlü çizgi: Üçlü bağ")
            msg.appendLine()
            msg.appendLine("Özel durumlar:")
            msg.appendLine("• ⚡ Rezonans: Birden fazla yapı")
            msg.appendLine("• Yük: İyonik yük gösterimi")
            msg.appendLine()
            msg.appendLine("Toplam ${molecules.size} molekül mevcut.")
            AlertDialog.Builder(requireContext())
                .setTitle("Lewis Yapısı Aracı Yardımı")
                .setMessage(msg.toString())
                .setPositiveButton("Anladım", null)
                .show()
        }

        showMolecule()
    }

    private fun showMolecule() {
        val mol = molecules[currentIndex]
        tvName.text = mol.name
        tvFormula.text = mol.formula
        tvInfo.text = mol.info
        tvCounter.text = "${currentIndex + 1} / ${molecules.size}"
        lewisView.setMolecule(mol)
    }
}
