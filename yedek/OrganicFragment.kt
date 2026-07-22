package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class MoleculeBuilderView(context: Context) : View(context) {
    data class Atom(val id: Int, var x: Float, var y: Float, var element: String)
    data class Bond(val from: Int, val to: Int, var type: Int)
    data class Snap(val atoms: List<Atom>, val bonds: List<Bond>, val nextId: Int)

    val atoms = mutableListOf<Atom>(); val bonds = mutableListOf<Bond>()
    private var nextId = 0
    var selectedIds = mutableListOf<Int>()
    var currentElement = "C"; var currentBondType = 1
    var isConnectMode = false; var connectPendingId = -1; var showH = true
    private val undoStack = mutableListOf<Snap>()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1E2A3A.toInt(); style = Paint.Style.FILL }
    private val atomPaints = mutableMapOf<String, Paint>()
    private val bondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE }
    private val bondPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE }
    private val bondPaint3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val pendingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private fun atomR() = (cellSize * 0.33f).coerceAtMost(38f)
    private fun textS() = atomR() * 1.3f
    private val valence = mapOf("C" to 4, "H" to 1, "O" to 2, "N" to 3, "Cl" to 1, "Br" to 1, "OH" to 1, "R" to 1, "I" to 1, "F" to 1)
    private val atomWeights = mapOf("C" to 12.011, "H" to 1.008, "O" to 15.999, "N" to 14.007, "Cl" to 35.453, "Br" to 79.904, "OH" to 17.007, "R" to 0.0, "I" to 126.904, "F" to 18.998)

    init {
        setAtomColor("C", 0xFF6B6B6B.toInt()); setAtomColor("H", 0xFFFFFFFF.toInt())
        setAtomColor("O", 0xFFFF0000.toInt()); setAtomColor("N", 0xFF3050F8.toInt())
        setAtomColor("Cl", 0xFF1FC01F.toInt()); setAtomColor("Br", 0xFFA52525.toInt())
        setAtomColor("OH", 0xFFFF69B4.toInt()); setAtomColor("R", 0xFFFFA500.toInt())
        setAtomColor("I", 0xFF4B0082.toInt()); setAtomColor("F", 0xFF00FF00.toInt())
    }

    private fun setAtomColor(elem: String, color: Int) {
        atomPaints[elem] = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    }

    private var cellSize = 0f; private var offsetX = 0f; private var offsetY = 0f
    var cols = 14; var rows = 22

    private fun recalcGrid() {
        cellSize = min(width.toFloat() / cols, height.toFloat() / rows)
        offsetX = (width - cellSize * cols) / 2f; offsetY = (height - cellSize * rows) / 2f
    }

    fun snapToGrid(x: Float, y: Float): Pair<Int, Int> {
        if (cellSize <= 0f) recalcGrid()
        return Pair(((x - offsetX) / cellSize).toInt().coerceIn(0, cols - 1), ((y - offsetY) / cellSize).toInt().coerceIn(0, rows - 1))
    }
    fun gridToPos(col: Int, row: Int): Pair<Float, Float> {
        if (cellSize <= 0f) recalcGrid()
        return Pair(offsetX + col * cellSize + cellSize / 2, offsetY + row * cellSize + cellSize / 2)
    }

    fun saveState() { undoStack.add(Snap(atoms.map { it.copy() }, bonds.map { it.copy() }, nextId)); if (undoStack.size > 30) undoStack.removeAt(0) }
    fun undo() {
        if (undoStack.isEmpty()) return; val s = undoStack.removeAt(undoStack.size - 1)
        atoms.clear(); atoms.addAll(s.atoms.map { it.copy() }); bonds.clear(); bonds.addAll(s.bonds.map { it.copy() })
        nextId = s.nextId; selectedIds.clear(); connectPendingId = -1; invalidate()
    }

    fun addAtomAtGrid(col: Int, row: Int, elem: String): Atom? {
        val (x, y) = gridToPos(col, row); if (atoms.any { abs(it.x - x) < 1f && abs(it.y - y) < 1f }) return null
        saveState(); val atom = Atom(nextId++, x, y, elem); atoms.add(atom); invalidate(); return atom
    }
    fun removeAtom(id: Int) {
        saveState(); atoms.removeAll { it.id == id }; bonds.removeAll { it.from == id || it.to == id }
        selectedIds.removeAll { it == id }; if (connectPendingId == id) connectPendingId = -1; invalidate()
    }
    fun clearAll() { if (atoms.isEmpty()) return; saveState(); atoms.clear(); bonds.clear(); nextId = 0; selectedIds.clear(); connectPendingId = -1; invalidate() }

    fun getHCount(atom: Atom): Int = max(0, (valence[atom.element] ?: 4) - bonds.filter { it.from == atom.id || it.to == atom.id }.sumOf { it.type })

    fun getMolecularWeight(): Double {
        val counts = mutableMapOf<String, Int>()
        for (a in atoms) counts[a.element] = (counts[a.element] ?: 0) + 1
        return counts.entries.sumOf { (e, c) -> (atomWeights[e] ?: 0.0) * c }
    }

    fun getFormula(): String {
        val elem = mutableMapOf<String, Int>(); var ohN = 0; var totalH = 0
        for (a in atoms) {
            when (a.element) { "OH" -> { ohN++; elem["O"] = (elem["O"] ?: 0) + 1 }; "R" -> {}; "H" -> totalH++; else -> elem[a.element] = (elem[a.element] ?: 0) + 1 }
        }
        for (a in atoms) { if (a.element !in listOf("H","OH","R")) totalH += getHCount(a) }
        totalH += ohN
        elem["H"] = totalH
        val cN = elem["C"] ?: 0; val hN = elem["H"] ?: 0
        val mol = listOf("C","H","O","N","Cl","Br").filter { it in elem }.joinToString("") { e -> val c = elem[e]!!; if (c == 1) e else "$e$c" }
        if (ohN > 0 && cN > 0) { val hMain = hN - ohN; return "$mol ($cN" + "H${hMain}${if (ohN == 1) "OH" else "(OH)$ohN"})" }
        return mol
    }

    fun generateRing(col: Int, row: Int, benzen: Boolean = false) {
        saveState()
        val pts = listOf(0 to 0, 3 to 0, 5 to 2, 4 to 5, 1 to 5, -1 to 2)
        val ids = mutableListOf<Int>()
        for ((dc, dr) in pts) {
            val c = (col + dc).coerceIn(0, cols - 1); val r = (row + dr).coerceIn(0, rows - 1)
            val (x, y) = gridToPos(c, r)
            if (atoms.any { abs(it.x - x) < 1f && abs(it.y - y) < 1f }) continue
            val atom = Atom(nextId++, x, y, "C"); atoms.add(atom); ids.add(atom.id)
        }
        for (i in ids.indices) {
            val j = (i + 1) % ids.size
            val typ = if (benzen && i % 2 == 0) 2 else 1
            bonds.add(Bond(ids[i], ids[j], typ))
        }
        invalidate()
    }

    fun loadExample(name: String) {
        saveState(); atoms.clear(); bonds.clear(); nextId = 0; selectedIds.clear(); connectPendingId = -1
        val cx = cols / 2; val cy = rows / 2
        fun p(c: Int, r: Int) = gridToPos((cx + c).coerceIn(0, cols-1), (cy + r).coerceIn(0, rows-1))
        fun a(c: Int, r: Int, e: String): Int { val (x, y) = p(c, r); val at = Atom(nextId++, x, y, e); atoms.add(at); return at.id }
        fun b(f: Int, t: Int, tp: Int = 1) { bonds.add(Bond(f, t, tp)) }
        when (name) {
            "Etanol" -> { val c1 = a(-2, 0, "C"); val c2 = a(2, 0, "C"); val oh = a(5, 3, "OH"); b(c1, c2); b(c2, oh) }
            "AsetikAsit" -> { val c1 = a(-2, 0, "C"); val c2 = a(2, 0, "C"); val o1 = a(5, -2, "O"); val oh = a(5, 3, "OH"); b(c1, c2); b(c2, o1, 2); b(c2, oh) }
            "MetilAmin" -> { val c1 = a(0, 0, "C"); val n1 = a(3, 2, "N"); b(c1, n1) }
            "Propan" -> { val c1 = a(-3, 0, "C"); val c2 = a(0, 0, "C"); val c3 = a(3, 0, "C"); b(c1, c2); b(c2, c3) }
            "Siklohekzan" -> generateRing(cx - 2, cy - 2, false)
            "Benzen" -> generateRing(cx - 2, cy - 2, true)
        }
        invalidate()
    }

    fun isCyclic(): Boolean {
        val cIds = atoms.filter { it.element == "C" }.map { it.id }.toSet(); if (cIds.size < 3) return false
        val cBonds = bonds.filter { it.from in cIds && it.to in cIds }; val deg = mutableMapOf<Int, Int>()
        for (id in cIds) deg[id] = 0; for (b in cBonds) { deg[b.from] = (deg[b.from] ?: 0) + 1; deg[b.to] = (deg[b.to] ?: 0) + 1 }
        return deg.values.all { it == 2 } && cBonds.size == cIds.size
    }

    fun cAdj(): Map<Int, List<Int>> {
        val cIds = atoms.filter { it.element == "C" }.map { it.id }.toSet(); val adj = mutableMapOf<Int, MutableList<Int>>()
        for (id in cIds) adj[id] = mutableListOf(); for (b in bonds) { if (b.from in adj && b.to in adj) { adj[b.from]!!.add(b.to); adj[b.to]!!.add(b.from) } }; return adj
    }

    fun longestChain(): List<Int> {
        val adj = cAdj(); if (adj.isEmpty()) return emptyList(); if (adj.size <= 2) return adj.keys.toList()
        fun dfs(cur: Int, vis: Set<Int>): List<Int> { var best = listOf(cur); for (nb in adj[cur]?.filter { it !in vis } ?: emptyList()) { val p = dfs(nb, vis + cur); if (p.size + 1 > best.size) best = listOf(cur) + p }; return best }
        var best = emptyList<Int>(); for (s in adj.keys) { val p = dfs(s, setOf(s)); if (p.size > best.size) best = p }; return best
    }

    fun generateName(): String {
        val chain = longestChain(); if (chain.isEmpty()) return "Karbon atomu yok"
        val n = chain.size; if (n == 1) return "Metan"; if (n == 2) return "Etan"
        val cyclic = isCyclic()
        val root = listOf("", "", "et", "prop", "but", "pent", "hekz", "hept", "okt", "non", "dek").getOrElse(n) { "C${n}" }
        val adj = cAdj(); val chainSet = chain.toSet()
        var hasDouble = false; var hasTriple = false; var dbPos = -1; var tbPos = -1
        for (i in 0 until n - 1) { val b = bonds.find { (it.from == chain[i] && it.to == chain[i+1]) || (it.from == chain[i+1] && it.to == chain[i]) }
            when (b?.type) { 2 -> { hasDouble = true; if (dbPos < 0) dbPos = i + 1 }; 3 -> { hasTriple = true; if (tbPos < 0) tbPos = i + 1 } } }
        val suf = when { hasTriple -> { val p = min(tbPos, n - tbPos); "${p}in" }; hasDouble -> { val p = min(dbPos, n - dbPos); "${p}en" }; else -> "an" }

        if (!cyclic) {
            val branches = mutableListOf<Pair<Int, Int>>()
            for (cid in adj.keys.filter { it !in chainSet }) { val attach = adj[cid]?.firstOrNull { it in chainSet } ?: continue; val chainIdx = chain.indexOf(attach); if (chainIdx < 0) continue
                var size = 0; val vis = mutableSetOf(cid); val q = mutableListOf(cid); while (q.isNotEmpty()) { val cur = q.removeAt(0); size++; q.addAll(adj[cur]?.filter { it !in vis && it !in chainSet }?.onEach { vis.add(it) } ?: emptyList()) }; branches.add(chainIdx + 1 to size) }
            if (branches.isEmpty()) return "${root}${suf}".replaceFirstChar { it.uppercase() }
            val revChain = chain.reversed(); val revBranches = mutableListOf<Pair<Int, Int>>()
            for (cid in adj.keys.filter { it !in chainSet }) { val attach = adj[cid]?.firstOrNull { it in chainSet } ?: continue; val chainIdx = revChain.indexOf(attach); if (chainIdx < 0) continue
                var size = 0; val vis = mutableSetOf(cid); val q = mutableListOf(cid); while (q.isNotEmpty()) { val cur = q.removeAt(0); size++; q.addAll(adj[cur]?.filter { it !in vis && it !in chainSet }?.onEach { vis.add(it) } ?: emptyList()) }; revBranches.add(chainIdx + 1 to size) }
            val fwd = branches.sortedBy { it.first }; val bwd = revBranches.sortedBy { it.first }
            val useFwd = compareLocants(fwd.map { it.first }, bwd.map { it.first })
            val finalBranches = if (useFwd) fwd else bwd
            val branchNames = mapOf(1 to "metil", 2 to "etil", 3 to "propil", 4 to "butil", 5 to "pentil", 6 to "hekil")
            val branchParts = mutableListOf<String>()
            for ((size, list) in finalBranches.groupBy { it.second }.toSortedMap()) { val ps = list.sortedBy { it.first }.map { it.first.toString() }; val pre = when (list.size) { 1 -> ""; 2 -> "di"; 3 -> "tri"; 4 -> "tetra"; else -> "poly" }; branchParts.add("${ps.joinToString("-")}-${pre}${branchNames[size] ?: "C${size}"}") }
            return "${branchParts.joinToString("-")}-${root}${suf}".replaceFirstChar { it.uppercase() }
        }
        return "Siklo${root}${suf}".replaceFirstChar { it.uppercase() }
    }

    private fun compareLocants(a: List<Int>, b: List<Int>): Boolean { for (i in 0 until min(a.size, b.size)) { if (a[i] != b[i]) return a[i] < b[i] }; return a.size <= b.size }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { super.onSizeChanged(w, h, oldw, oldh); recalcGrid() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); recalcGrid(); canvas.drawColor(0xFF0D1117.toInt())
        for (r in 0 until rows) for (c in 0 until cols) { val (x, y) = gridToPos(c, r); canvas.drawCircle(x, y, 1.5f, gridPaint) }
        for (bond in bonds) {
            val a1 = atoms.find { it.id == bond.from } ?: continue; val a2 = atoms.find { it.id == bond.to } ?: continue
            val dx = a2.x - a1.x; val dy = a2.y - a1.y; val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
            val bo = atomR() * 0.25f; val px = -dy / len * bo; val py = dx / len * bo
            val bp = when (bond.type) { 2 -> bondPaint2; 3 -> bondPaint3; else -> bondPaint }
            when (bond.type) { 1 -> canvas.drawLine(a1.x, a1.y, a2.x, a2.y, bp)
                2 -> { canvas.drawLine(a1.x + px, a1.y + py, a2.x + px, a2.y + py, bp); canvas.drawLine(a1.x - px, a1.y - py, a2.x - px, a2.y - py, bp) }
                3 -> { canvas.drawLine(a1.x, a1.y, a2.x, a2.y, bondPaint); canvas.drawLine(a1.x + px, a1.y + py, a2.x + px, a2.y + py, bondPaint2); canvas.drawLine(a1.x - px, a1.y - py, a2.x - px, a2.y - py, bondPaint2) } }
        }
        val r = atomR(); val ts = textS(); textPaint.textSize = ts
        for (atom in atoms) {
            val ar = if (atom.element == "H") r * 0.6f else r; val paint = atomPaints[atom.element] ?: atomPaints["C"]!!
            if (atom.id in selectedIds) canvas.drawCircle(atom.x, atom.y, ar + 6f, selectPaint)
            if (atom.id == connectPendingId) canvas.drawCircle(atom.x, atom.y, ar + 8f, pendingPaint)
            canvas.drawCircle(atom.x, atom.y, ar, paint)
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
            canvas.drawCircle(atom.x, atom.y, ar, border)
            val label = atom.element; val showLabel = if (label == "OH") "OH" else if (label == "R") "R" else label
            if (atom.element != "H") {
                canvas.drawText(showLabel, atom.x, atom.y + ts / 3f, textPaint)
                if (showH && atom.element !in listOf("OH", "R")) { val hc = getHCount(atom); if (hc > 0) {
                    val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCCCC.toInt(); textSize = ts * 0.45f; textAlign = Paint.Align.LEFT; isFakeBoldText = true }
                    canvas.drawText(if (hc == 1) "H" else "H${hc}", atom.x + ar * 0.6f, atom.y - ar * 0.35f + 5f, hp) } }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tx = event.x; val ty = event.y; val hitR = atomR()
            val hit = atoms.find { sqrt((it.x - tx) * (it.x - tx) + (it.y - ty) * (it.y - ty)) < hitR + 10f }
            if (isConnectMode) {
                if (hit != null) { if (connectPendingId < 0) { connectPendingId = hit.id; selectedIds.clear(); selectedIds.add(hit.id); invalidate(); return true }
                    val a = connectPendingId; val b = hit.id; if (a != b && !bonds.any { (it.from == a && it.to == b) || (it.from == b && it.to == a) }) { saveState(); bonds.add(Bond(a, b, currentBondType)); invalidate() }
                    connectPendingId = b; selectedIds.clear(); selectedIds.add(b); invalidate(); return true }
                return true
            }
            if (hit != null) { selectedIds.add(hit.id); if (selectedIds.size > 2) selectedIds.removeAt(0); invalidate(); return true }
            val (col, row) = snapToGrid(tx, ty); addAtomAtGrid(col, row, currentElement); return true
        }
        return false
    }
}

class OrganicFragment : Fragment() {
    private lateinit var builder: MoleculeBuilderView; private lateinit var nameResult: TextView
    private lateinit var formulaResult: TextView; private lateinit var molDetail: TextView; private lateinit var infoText: TextView
    private lateinit var connectBtn: Button; private lateinit var hToggleBtn: Button
    private var exampleIdx = 0; private val examples = listOf("Etanol", "AsetikAsit", "MetilAmin", "Propan", "Siklohekzan", "Benzen")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_organic, container, false)
        val placeholder = v.findViewById<View>(R.id.org_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        builder = MoleculeBuilderView(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (550 * resources.displayMetrics.density).toInt()) }
        parent.addView(builder, idx)

        nameResult = v.findViewById(R.id.org_name_result); formulaResult = v.findViewById(R.id.org_formula_result)
        molDetail = v.findViewById(R.id.org_mol_detail); infoText = v.findViewById(R.id.org_info)
        infoText.text = "C eklemek icin tuvale dokun. Bagla modunda once bir atoma sonra digerine dokunarak bag olustur."

        fun updateElem(activeId: Int) {
            listOf(R.id.org_elem_c, R.id.org_elem_h, R.id.org_elem_o, R.id.org_elem_n, R.id.org_elem_cl, R.id.org_elem_br, R.id.org_elem_oh, R.id.org_elem_r).forEach { id ->
                v.findViewById<Button>(id).apply { isEnabled = id != activeId; alpha = if (id == activeId) 1f else 0.5f } }
        }
        v.findViewById<Button>(R.id.org_elem_c).isEnabled = false
        listOf(R.id.org_elem_c to "C", R.id.org_elem_h to "H", R.id.org_elem_o to "O", R.id.org_elem_n to "N",
            R.id.org_elem_cl to "Cl", R.id.org_elem_br to "Br", R.id.org_elem_oh to "OH", R.id.org_elem_r to "R")
            .forEach { (id, elem) -> v.findViewById<Button>(id).setOnClickListener { builder.currentElement = elem; updateElem(id) } }

        fun updateBond(activeId: Int) {
            listOf(R.id.org_bond_single, R.id.org_bond_double, R.id.org_bond_triple).forEach { id ->
                v.findViewById<Button>(id).apply { isEnabled = id != activeId; backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (id == activeId) R.color.neon_cyan else R.color.line) } }
        }
        v.findViewById<Button>(R.id.org_bond_single).isEnabled = false
        v.findViewById<Button>(R.id.org_bond_single).setOnClickListener { builder.currentBondType = 1; updateBond(R.id.org_bond_single) }
        v.findViewById<Button>(R.id.org_bond_double).setOnClickListener { builder.currentBondType = 2; updateBond(R.id.org_bond_double) }
        v.findViewById<Button>(R.id.org_bond_triple).setOnClickListener { builder.currentBondType = 3; updateBond(R.id.org_bond_triple) }

        v.findViewById<Button>(R.id.org_ring).setOnClickListener { builder.generateRing(builder.cols / 2 - 2, builder.rows / 2 - 2, false) }
        v.findViewById<Button>(R.id.org_benzene).setOnClickListener { builder.generateRing(builder.cols / 2 - 2, builder.rows / 2 - 2, true) }
        v.findViewById<Button>(R.id.org_example).setOnClickListener { builder.loadExample(examples[exampleIdx]); exampleIdx = (exampleIdx + 1) % examples.size }

        connectBtn = v.findViewById(R.id.org_connect)
        connectBtn.setOnClickListener {
            builder.isConnectMode = !builder.isConnectMode; if (!builder.isConnectMode) builder.connectPendingId = -1
            connectBtn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (builder.isConnectMode) R.color.neon_cyan else R.color.neon_pink)
            connectBtn.setText(if (builder.isConnectMode) "Bagla:ON" else "Bagla"); builder.invalidate()
        }
        v.findViewById<Button>(R.id.org_del).setOnClickListener { if (builder.selectedIds.isNotEmpty()) builder.removeAtom(builder.selectedIds.last()) }
        v.findViewById<Button>(R.id.org_undo).setOnClickListener { builder.undo() }
        v.findViewById<Button>(R.id.org_clear).setOnClickListener { builder.clearAll(); nameResult.text = ""; formulaResult.text = ""; molDetail.text = "" }

        hToggleBtn = v.findViewById(R.id.org_toggle_h)
        hToggleBtn.setOnClickListener { builder.showH = !builder.showH; hToggleBtn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (builder.showH) R.color.neon_cyan else R.color.line)
            hToggleBtn.setText(if (builder.showH) "H+" else "H-"); builder.invalidate() }

        v.findViewById<Button>(R.id.org_adlandir).setOnClickListener {
            val name = builder.generateName(); nameResult.text = "Adi: $name"
            formulaResult.text = "Formul: ${builder.getFormula()}"
            val mw = builder.getMolecularWeight(); val ohCount = builder.atoms.count { it.element == "OH" }
            var detail = "Mol Kutlesi: ${"%.3f".format(mw)} g/mol"
            if (builder.isCyclic()) { val hasDB = builder.bonds.any { it.type == 2 && builder.atoms.any { a -> a.id == it.from || a.id == it.to } }
                detail += " | Aromatik? " + if (hasDB) "Evet (Huckel)" else "Hayir (Sikloalkan)" }
            if (ohCount > 0) detail += " | Alkol (-OH)"
            if (builder.atoms.any { it.element == "R" }) detail += " | Radikal grup (R)"
            molDetail.text = detail
            val cat = when { "in" in name -> "Alkin"; "en" in name -> "Alken"; "an" in name -> "Alkan"; else -> "" }
            infoText.text = "IUPAC: $name | $cat"
            builder.invalidate()
        }
        return v
    }
}
