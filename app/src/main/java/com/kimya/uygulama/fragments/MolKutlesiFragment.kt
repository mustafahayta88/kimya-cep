package com.kimya.uygulama.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData
import com.kimya.uygulama.utils.PdfExporter
import com.kimya.uygulama.viewmodel.KimyaViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class MoleculeView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var formulaText = ""
    private var atoms = emptyList<AtomPos>()
    private var bonds = emptyList<BondLine>()

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector

    private val bondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00F0FF.toInt()
        strokeWidth = 3f
    }
    private val atomFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val atomStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.WHITE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val elementColors = mapOf(
        "H" to 0xFF00F0FF.toInt(), "He" to 0xFFFF69B4.toInt(),
        "C" to 0xFF555555.toInt(), "N" to 0xFF3B82F6.toInt(),
        "O" to 0xFFEF4444.toInt(), "F" to 0xFF39FF14.toInt(),
        "Na" to 0xFFB388FF.toInt(), "Mg" to 0xFFFF8C00.toInt(),
        "Al" to 0xFFDA70D6.toInt(), "Si" to 0xFF8B7355.toInt(),
        "P" to 0xFFFFA500.toInt(), "S" to 0xFFEAB308.toInt(),
        "Cl" to 0xFF39FF14.toInt(), "K" to 0xFFFF6347.toInt(),
        "Ca" to 0xFFFF8C00.toInt(), "Fe" to 0xFFA0522D.toInt(),
        "Cu" to 0xFFCD853F.toInt(), "Zn" to 0xFF708090.toInt(),
        "Br" to 0xFF8B0000.toInt(), "Ag" to 0xFFC0C0C0.toInt(),
        "I" to 0xFF4B0082.toInt(), "Ba" to 0xFF228B22.toInt(),
        "Au" to 0xFFFFD700.toInt(), "Hg" to 0xFFA9A9A9.toInt(),
        "Pb" to 0xFF696969.toInt(), "Mn" to 0xFF9370DB.toInt(),
        "Cr" to 0xFF00CED1.toInt(), "Ni" to 0xFF50C878.toInt(),
        "Co" to 0xFF4169E1.toInt(), "Ti" to 0xFF808080.toInt()
    )

    private data class AtomPos(val symbol: String, val x: Float, val y: Float)
    private data class BondLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    fun setFormula(formula: String) {
        formulaText = formula
        calculatePositions()
        invalidate()
    }

    private fun calculatePositions() {
        atoms = emptyList()
        bonds = emptyList()
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || formulaText.isEmpty()) return

        val regex = Regex("([A-Z][a-z]?)(\\d*)")
        val pairs = mutableListOf<Pair<String, Int>>()
        for (m in regex.findAll(formulaText)) {
            val sym = m.groupValues[1]
            val cnt = m.groupValues[2].toIntOrNull() ?: 1
            pairs.add(sym to cnt)
        }
        if (pairs.isEmpty()) return

        val atomList = mutableListOf<String>()
        pairs.forEach { (sym, cnt) -> repeat(cnt) { atomList.add(sym) } }
        if (atomList.isEmpty()) return

        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) * 0.3f

        if (atomList.size == 1) {
            atoms = listOf(AtomPos(atomList[0], cx, cy))
            return
        }

        val counts = pairs.associate { it }
        val unique = pairs.map { it.first }.toSet()
        val centralSym = findCentral(unique, counts)
        val outer = atomList.filter { it != centralSym }

        val result = mutableListOf<AtomPos>()
        val bondResult = mutableListOf<BondLine>()

        result.add(AtomPos(centralSym, cx, cy))

        val isLinear3 = atomList.size == 3 && unique.size == 3

        when {
            isLinear3 -> {
                val spacing = r * 0.75f
                outer.forEachIndexed { i, sym ->
                    val x = cx + (if (i == 0) -spacing else spacing)
                    result.add(AtomPos(sym, x, cy))
                    bondResult.add(BondLine(cx, cy, x, cy))
                }
            }
            outer.size == 2 -> {
                val angles = listOf(-PI / 4, PI / 4)
                outer.forEachIndexed { i, sym ->
                    val a = angles[i]
                    val x = cx + r * cos(a).toFloat()
                    val y = cy + r * sin(a).toFloat()
                    result.add(AtomPos(sym, x, y))
                    bondResult.add(BondLine(cx, cy, x, y))
                }
            }
            outer.size == 3 -> {
                    val angles = listOf(-PI / 2, -PI / 2 + 2 * PI / 3, -PI / 2 + 4 * PI / 3)
                outer.forEachIndexed { i, sym ->
                    val a = angles[i]
                    val x = cx + r * cos(a).toFloat()
                    val y = cy + r * sin(a).toFloat()
                    result.add(AtomPos(sym, x, y))
                    bondResult.add(BondLine(cx, cy, x, y))
                }
            }
            else -> {
                val step = 2 * PI / outer.size
                outer.forEachIndexed { i, sym ->
                    val a = i.toDouble() * step - PI / 2
                    val x = cx + r * cos(a).toFloat()
                    val y = cy + r * sin(a).toFloat()
                    result.add(AtomPos(sym, x, y))
                    bondResult.add(BondLine(cx, cy, x, y))
                }
            }
        }

        atoms = result
        bonds = bondResult
    }

    private fun findCentral(unique: Set<String>, counts: Map<String, Int>): String {
        val singleNonH = unique.filter { it != "H" && (counts[it] ?: 0) == 1 }
        if (singleNonH.size == 1) return singleNonH.first()
        val nonH = unique.filter { it != "H" }.sortedBy { counts[it] }
        if (nonH.isNotEmpty()) return nonH.first()
        return unique.first()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (formulaText.isNotEmpty()) calculatePositions()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_MOVE -> { if (tMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }; lastTx = e.x; lastTy = e.y; invalidate() }
            MotionEvent.ACTION_UP -> { tMode = 0; return true }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (atoms.isEmpty()) return
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)

        for (b in bonds) {
            canvas.drawLine(b.x1, b.y1, b.x2, b.y2, bondPaint)
        }

        val radius = dp(22).toFloat()
        labelPaint.textSize = dp(15).toFloat()

        for (a in atoms) {
            val color = elementColors[a.symbol] ?: 0xFF00F0FF.toInt()
            atomFillPaint.color = color
            canvas.drawCircle(a.x, a.y, radius, atomFillPaint)
            canvas.drawCircle(a.x, a.y, radius, atomStrokePaint)

            val baseline = a.y - (labelPaint.fontMetrics.ascent + labelPaint.fontMetrics.descent) / 2f
            canvas.drawText(a.symbol, a.x, baseline, labelPaint)
        }
        canvas.restore()
    }
}

class MolKutlesiFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private var sonMkSonuc = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_molkutlesi, container, false)
        val formul = v.findViewById<EditText>(R.id.mI_formul)
        val sonuc = v.findViewById<TextView>(R.id.mI_sonuc)
        val bilgi = v.findViewById<TextView>(R.id.mI_bilesik_bilgi)
        val moleculeView = v.findViewById<MoleculeView>(R.id.mI_molecule_view)

        fun hesapla() {
            val f = formul.text.toString().trim()
            if (f.isEmpty()) {
                sonuc.text = "Formul girin"
                moleculeView.setFormula("")
                bilgi.text = ""
                return
            }
            val m = KimyaData.molekulKutlesiHesapla(f)
            if (m == null) {
                sonuc.text = "Gecersiz formul veya element bilinmiyor"
                moleculeView.setFormula("")
                bilgi.text = ""
                return
            }

            moleculeView.setFormula(f)

            val sb = StringBuilder()
            sb.append("  M($f) = ${"%.4f".format(m)} g/mol\n\n")
            val regex = Regex("([A-Z][a-z]?)(\\d*)")
            var cnt = 0
            for (match in regex.findAll(f)) {
                val s = match.groupValues[1]
                val c = match.groupValues[2].toIntOrNull() ?: 1
                val el = KimyaData.elementler[s]
                if (el != null) {
                    cnt++
                    sb.append("  ${el.adi}($s): ${el.kutle} x $c = ${"%.3f".format(el.kutle * c)}\n")
                }
            }
            if (cnt > 0) {
                sonMkSonuc = sb.toString()
                sonuc.text = sonMkSonuc
                vm.addHistory("Mol Kutlesi", "M($f) = ${"%.4f".format(m)} g/mol")
            }

            val bilesik = KimyaData.bilesikler.find { it.formulu == f }
            if (bilesik != null) {
                bilgi.text = """  Bilesik: ${bilesik.adi}
  Formul: ${bilesik.formulu}
  Tur: ${bilesik.tur}
  Ozellik: ${bilesik.ozellik}"""
            } else {
                bilgi.text = "  Bu formul icin kayitli bilesik bilgisi yok."
            }
        }

        v.findViewById<Button>(R.id.mI_hesapla).setOnClickListener { hesapla() }
        v.findViewById<Button>(R.id.mI_h2so4).setOnClickListener { formul.setText("H2SO4"); hesapla() }
        v.findViewById<Button>(R.id.mI_caco3).setOnClickListener { formul.setText("CaCO3"); hesapla() }
        v.findViewById<Button>(R.id.mI_naoh).setOnClickListener { formul.setText("NaOH"); hesapla() }

        v.findViewById<Button>(R.id.mI_paylas).setOnClickListener {
            if (sonMkSonuc.isEmpty()) {
                Toast.makeText(context, "Once hesaplama yapin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PdfExporter.shareText(requireContext(), "Mol Kutlesi", sonMkSonuc)
        }
        return v
    }
}
