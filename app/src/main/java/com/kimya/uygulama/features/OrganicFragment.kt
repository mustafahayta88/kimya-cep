package com.kimya.uygulama.features

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils
import com.kimya.uygulama.utils.HelpDialog
import com.kimya.uygulama.viewmodel.KimyaViewModel
import android.view.ScaleGestureDetector
import kotlin.math.*
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════
//  MoleculeCanvas — Sıfırdan yazılmış molekül inşa alanı
// ═══════════════════════════════════════════════════════════════
class MoleculeCanvas(context: Context) : View(context) {

    // ── Veri modelleri ──
    data class Atom(var id: Int, var col: Int, var row: Int, var elem: String, var anim: Float = 0f)
    data class Bond(var a: Int, var b: Int, var type: Int, var anim: Float = 0f)
    data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var col: Int)

    // ── Durum ──
    val atoms = mutableListOf<Atom>()
    val bonds = mutableListOf<Bond>()
    private val sparks = mutableListOf<Spark>()
    private var uid = 0
    var selectedAtomId = -1
    var connectFrom = -1
    var currentElem = "C"
    var currentBondType = 1
    var isConnectMode = false
    var showH = true
    var onChange: (() -> Unit)? = null

    // ── Undo/Redo ──
    private data class Snap(val atoms: List<Atom>, val bonds: List<Bond>, val uid: Int)
    private val undoStack = mutableListOf<Snap>()
    private val redoStack = mutableListOf<Snap>()
    private fun snap() { undoStack.add(Snap(atoms.map { it.copy() }, bonds.map { it.copy() }, uid)); if (undoStack.size > 50) undoStack.removeAt(0); redoStack.clear() }
    fun undo() { if (undoStack.isEmpty()) return; redoStack.add(Snap(atoms.map { it.copy() }, bonds.map { it.copy() }, uid)); val s = undoStack.removeAt(undoStack.size - 1); atoms.clear(); atoms.addAll(s.atoms); bonds.clear(); bonds.addAll(s.bonds); uid = s.uid; selectedAtomId = -1; connectFrom = -1; invalidate(); onChange?.invoke() }
    fun redo() { if (redoStack.isEmpty()) return; undoStack.add(Snap(atoms.map { it.copy() }, bonds.map { it.copy() }, uid)); val s = redoStack.removeAt(redoStack.size - 1); atoms.clear(); atoms.addAll(s.atoms); bonds.clear(); bonds.addAll(s.bonds); uid = s.uid; selectedAtomId = -1; connectFrom = -1; invalidate(); onChange?.invoke() }

    // ── Grid ──
    private var cell = 0f; private var ox = 0f; private var oy = 0f
    var cols = 14; var rows = 20
    private fun recalc() { cell = min(width.toFloat() / cols, height.toFloat() / rows); ox = (width - cell * cols) / 2f; oy = (height - cell * rows) / 2f }
    private fun g2p(c: Int, r: Int): Pair<Float, Float> { if (cell <= 0f) recalc(); return Pair(ox + c * cell + cell / 2, oy + r * cell + cell / 2) }
    private fun p2g(x: Float, y: Float): Pair<Int, Int> { if (cell <= 0f) recalc(); return Pair(((x - ox) / cell).toInt().coerceIn(0, cols - 1), ((y - oy) / cell).toInt().coerceIn(0, rows - 1)) }
    private fun atomAt(c: Int, r: Int): Atom? = atoms.find { it.col == c && it.row == r }
    private fun atomR() = (cell * 0.38f).coerceAtMost(40f)

    // ── Renkler ──
    private val eCol = mapOf(
        "C" to 0xFF5C6BC0.toInt(), "H" to 0xFF90A4AE.toInt(), "O" to 0xFFEF5350.toInt(),
        "N" to 0xFF42A5F5.toInt(), "S" to 0xFFFFCA28.toInt(), "F" to 0xFF66BB6A.toInt(),
        "Cl" to 0xFF26A69A.toInt(), "Br" to 0xFFAB47BC.toInt(), "I" to 0xFF7E57C2.toInt(),
        "OH" to 0xFFEC407A.toInt(), "COOH" to 0xFFFF7043.toInt(), "NH2" to 0xFF5C6BC0.toInt(),
        "CHO" to 0xFFFFA726.toInt(), "R" to 0xFF8D6E63.toInt()
    )
    private val eCol2 = mapOf(
        "C" to 0xFF7986CB.toInt(), "H" to 0xFFB0BEC5.toInt(), "O" to 0xFFEF9A9A.toInt(),
        "N" to 0xFF90CAF9.toInt(), "S" to 0xFFFFE082.toInt(), "F" to 0xFFA5D6A7.toInt(),
        "Cl" to 0xFF80CBC4.toInt(), "Br" to 0xFFCE93D8.toInt(), "I" to 0xFFB39DDB.toInt(),
        "OH" to 0xFFF48FB1.toInt(), "COOH" to 0xFFFFAB91.toInt(), "NH2" to 0xFF90CAF9.toInt(),
        "CHO" to 0xFFFFCC80.toInt(), "R" to 0xFFBCAAA4.toInt()
    )
    // Koyu gölge renkleri
    private val eColDk = mapOf(
        "C" to 0xFF3949AB.toInt(), "H" to 0xFF607D8B.toInt(), "O" to 0xFFC62828.toInt(),
        "N" to 0xFF1565C0.toInt(), "S" to 0xFFF9A825.toInt(), "F" to 0xFF2E7D32.toInt(),
        "Cl" to 0xFF00796B.toInt(), "Br" to 0xFF7B1FA2.toInt(), "I" to 0xFF512DA8.toInt(),
        "OH" to 0xFFAD1457.toInt(), "COOH" to 0xFFD84315.toInt(), "NH2" to 0xFF283593.toInt(),
        "CHO" to 0xFFEF6C00.toInt(), "R" to 0xFF5D4037.toInt()
    )
    private val valence = mapOf("C" to 4, "H" to 1, "O" to 2, "N" to 3, "S" to 2, "F" to 1, "Cl" to 1, "Br" to 1, "I" to 1, "OH" to 1, "COOH" to 1, "NH2" to 1, "CHO" to 1, "R" to 1)
    private val weights = mapOf("C" to 12.011, "H" to 1.008, "O" to 15.999, "N" to 14.007, "S" to 32.065, "Cl" to 35.453, "Br" to 79.904, "F" to 18.998, "I" to 126.904, "OH" to 17.007, "COOH" to 45.017, "NH2" to 16.023, "CHO" to 29.019, "R" to 0.0)

    // ── Paint'ler ──
    private val bgP = Paint().apply { color = 0xFF0A0E14.toInt() }
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF151D28.toInt() }
    private val bondP1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF667788.toInt(); strokeWidth = 7f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bondP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00DDFF.toInt(); strokeWidth = 6f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bondP3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF44AA.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val selP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF81C784.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val pendP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val hP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF667788.toInt(); textAlign = Paint.Align.LEFT; isFakeBoldText = true }
    private val glP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000.toInt(); style = Paint.Style.FILL }
    private val spP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private var pulse = 0f
    private var zoom = 1f; private var panX = 0f; private var panY = 0f
    private var lastX = 0f; private var lastY = 0f; private var dragId = -1
    private val scDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(det: ScaleGestureDetector) = true
        override fun onScale(det: ScaleGestureDetector): Boolean { zoom *= det.scaleFactor; zoom = zoom.coerceIn(0.4f, 3f); invalidate(); return true }
    })

    init {
        isClickable = true; isFocusable = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
            addUpdateListener { pulse = it.animatedValue as Float; updateSparks(); invalidate() }; start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); recalc() }

    // ── Açık API ──
    fun hCount(a: Atom): Int = max(0, (valence[a.elem] ?: 4) - bonds.filter { it.a == a.id || it.b == a.id }.sumOf { it.type })
    fun formula(): String {
        val m = mutableMapOf<String, Int>(); var eh = 0
        for (a in atoms) when (a.elem) {
            "OH" -> { m["O"] = (m["O"] ?: 0) + 1; eh++ }
            "COOH" -> { m["C"] = (m["C"] ?: 0) + 1; m["O"] = (m["O"] ?: 0) + 2; eh++ }
            "NH2" -> { m["N"] = (m["N"] ?: 0) + 1; eh += 2 }
            "CHO" -> { m["C"] = (m["C"] ?: 0) + 1; m["O"] = (m["O"] ?: 0) + 1; eh++ }
            "R", "H" -> {} else -> m[a.elem] = (m[a.elem] ?: 0) + 1
        }
        var th = eh; for (a in atoms) if (a.elem !in listOf("H", "OH", "COOH", "NH2", "CHO", "R")) th += hCount(a)
        m["H"] = th
        return listOf("C", "H", "N", "O", "S", "F", "Cl", "Br", "I").filter { it in m }.joinToString("") { e -> val c = m[e]!!; if (c == 1) e else "$e$c" }
    }
    fun molWeight(): Double { val m = mutableMapOf<String, Int>(); for (a in atoms) m[a.elem] = (m[a.elem] ?: 0) + 1; return m.entries.sumOf { (e, c) -> (weights[e] ?: 0.0) * c } }

    fun getProperties(): Map<String, String> {
        val props = mutableMapOf<String, String>()
        val fgs = functionalGroups()
        val mw = molWeight()

        // Agirlik
        props["Agirlik"] = "${"%.2f".format(mw)} g/mol"

        // Agirlik durumu (yaklasik erime/kaynama noktalari)
        val mp = when {
            fgs.any { it == "Karboksilik Asit" } -> "80-120 C (yaklasik)"
            fgs.any { it == "Alkol" } -> ">-100 C (yaklasik)"
            fgs.any { it == "Aromatik" } -> "Yuksek (katı)"
            mw < 100 -> "Dusuk (gaz/sivi)"
            mw < 200 -> "Orta (sivi)"
            else -> "Yuksek (katı)"
        }
        props["Erime Noktasi"] = mp

        // Polarite
        val isPolar = fgs.any { it in listOf("Alkol", "Karboksilik Asit", "Amin", "Amin (2ry)", "Amin (3ry)") }
        val hasHalogen = fgs.any { it == "Halojen" }
        props["Polarite"] = when {
            fgs.any { it == "Karboksilik Asit" } -> "Cok polar (H-baglantisi)"
            fgs.any { it == "Alkol" } && fgs.any { it == "Amin" } -> "Cok polar (H-baglantisi)"
            fgs.any { it == "Alkol" } -> "Polar (H-baglantisi)"
            fgs.any { it in listOf("Amin", "Amin (2ry)", "Amin (3ry)") } -> "Polar (H-baglantisi)"
            hasHalogen && fgs.any { it == "Alken" || it == "Aromatik" } -> "Az polar"
            fgs.any { it == "Aromatik" } -> "Apolar"
            isPolar -> "Polar"
            else -> "Apolar"
        }

        // Cozunurluk
        props["Suda Cozunurluk"] = when {
            fgs.any { it == "Karboksilik Asit" && mw < 150 } -> "Iyi cozunur"
            fgs.any { it == "Alkol" && mw < 120 } -> "Iyi cozunur"
            fgs.any { it == "Amin" && mw < 100 } -> "Iyi cozunur"
            fgs.any { it == "Aromatik" } -> "Cozunmez"
            hasHalogen -> "Az cozunur"
            fgs.any { it == "Alkol" } -> "Kismen cozunur"
            else -> "Cozunmez"
        }

        // Oda sicakliginda durum
        props["Oda Sıcakliginda"] = when {
            mw < 50 -> "Gaz"
            mw < 120 && !fgs.any { it in listOf("Karboksilik Asit", "Aromatik") } -> "Sivi"
            fgs.any { it == "Aromatik" } -> "Katı"
            fgs.any { it == "Karboksilik Asit" && mw < 130 } -> "Katı"
            else -> "Sivi"
        }

        // Kimyasal sinif
        val classes = mutableListOf<String>()
        if (fgs.any { it == "Alkol" }) classes += "Alkol"
        if (fgs.any { it == "Karboksilik Asit" }) classes += "Karboksilik Asit"
        if (fgs.any { it == "Keton" }) classes += "Keton"
        if (fgs.any { it == "Aldehit" }) classes += "Aldehit"
        if (fgs.any { it in listOf("Amin", "Amin (2ry)", "Amin (3ry)") }) classes += "Amin"
        if (fgs.any { it == "Eter" }) classes += "Eter"
        if (fgs.any { it == "Alken" }) classes += "Alken"
        if (fgs.any { it == "Alkin" }) classes += "Alkin"
        if (fgs.any { it == "Aromatik" }) classes += "Aromatik"
        if (fgs.any { it == "Halojen" }) classes += "Halojen"
        if (classes.isEmpty()) classes += "Alkan"
        props["Kimyasal Sinif"] = classes.joinToString(", ")

        return props
    }

    fun functionalGroups(): List<String> {
        val g = mutableListOf<String>()
        val adj = mutableMapOf<Int, MutableList<Int>>()
        for (a in atoms) adj[a.id] = mutableListOf()
        for (b in bonds) { adj[b.a]?.add(b.b); adj[b.b]?.add(b.a) }

        // O atomu etrafindaki baglantilari kontrol et
        for (a in atoms.filter { it.elem == "O" }) {
            val nb = adj[a.id] ?: continue
            val ne = nb.map { atoms.find { x -> x.id == it }?.elem ?: "" }
            // C=O + C-OH -> Karboksilik Asit
            val hasDoubleC = bonds.any { (it.a == a.id || it.b == a.id) && it.type == 2 && atoms.find { x -> x.id == if (it.a == a.id) it.b else it.a }?.elem == "C" }
            if (hasDoubleC) {
                // Bu O bir C'ye bagli mi ve o C'ye baska bir O-H bagli mi?
                val parentC = nb.find { atoms.find { x -> x.id == it }?.elem == "C" }
                if (parentC != null) {
                    val cNeighbors = adj[parentC] ?: mutableListOf()
                    val hasOH = cNeighbors.any { atoms.find { x -> x.id == it }?.elem == "OH" }
                    val hasOtherO = cNeighbors.any { it != a.id && atoms.find { x -> x.id == it }?.elem == "O" && !hasOH }
                    if (hasOH) { g += "Karboksilik Asit"; continue }
                    // C=O + C-C -> Ketona veya Aldehide
                    val cCount = cNeighbors.count { atoms.find { x -> x.id == it }?.elem == "C" }
                    if (cCount >= 2) g += "Keton"
                    else if (cCount == 1 && !hasOH) g += "Aldehit"
                }
                continue
            }
            // C-O-C -> Eter
            if (ne.count { it == "C" } == 2 && nb.size == 2) g += "Eter"
            // C-OH -> Alkol
            if (ne.any { it == "C" } && ne.any { it == "H" }) g += "Alkol"
        }

        // N atomu
        for (a in atoms.filter { it.elem == "N" }) {
            val nb = adj[a.id] ?: continue
            val ne = nb.map { atoms.find { x -> x.id == it }?.elem ?: "" }
            val cCount = ne.count { it == "C" }
            if (cCount >= 1 && ne.all { it in listOf("C", "H") }) {
                if (cCount >= 2) g += "Amin (3ry)"
                else if (cCount == 1 && nb.size == 2) g += "Amin (2ry)"
                else g += "Amin"
            }
        }

        // Halojen
        for (a in atoms.filter { it.elem in listOf("Cl", "Br", "F", "I") }) {
            g += "Halojen"
        }

        // C=C, C#C
        for (a in atoms.filter { it.elem == "C" }) {
            val nb = adj[a.id] ?: continue
            val hasCCdouble = bonds.any { (it.a == a.id || it.b == a.id) && it.type == 2 && atoms.find { x -> x.id == if (it.a == a.id) it.b else it.a }?.elem == "C" }
            val hasCCtriple = bonds.any { (it.a == a.id || it.b == a.id) && it.type == 3 && atoms.find { x -> x.id == if (it.a == a.id) it.b else it.a }?.elem == "C" }
            if (hasCCtriple) g += "Alkin"
            else if (hasCCdouble) g += "Alken"
        }

        // Benzen halkasi tespiti
        val cIds = atoms.filter { it.elem == "C" }.map { it.id }.toSet()
        if (cIds.size >= 6) {
            val adjC = mutableMapOf<Int, MutableList<Int>>()
            for (id in cIds) adjC[id] = mutableListOf()
            for (b in bonds) { if (b.a in adjC && b.b in adjC) { adjC[b.a]!!.add(b.b); adjC[b.b]!!.add(b.a) } }
            // Halka olusturan 6 karbonlu yapı var mi?
            fun hasCycle6(start: Int, path: List<Int>): Boolean {
                if (path.size == 6) return adjC[start]?.any { it == path[0] } == true
                for (nb in adjC[start] ?: emptyList()) { if (nb !in path && path.size < 6) { if (hasCycle6(nb, path + nb)) return true } }
                return false
            }
            if (cIds.any { hasCycle6(it, listOf(it)) }) {
                val hasDouble = bonds.filter { it.a in cIds && it.b in cIds }.any { it.type == 2 }
                if (hasDouble) g += "Aromatik"
                else g += "Sikloalkan"
            }
        }

        return g.distinct()
    }

    fun iupacName(): String {
        val cIds = atoms.filter { it.elem == "C" }.map { it.id }.toSet()
        if (cIds.isEmpty()) return "Yok"; if (cIds.size == 1) return "Metan"; if (cIds.size == 2) return "Ethan"
        val adj = mutableMapOf<Int, MutableList<Int>>(); for (id in cIds) adj[id] = mutableListOf()
        for (b in bonds) { if (b.a in adj && b.b in adj) { adj[b.a]!!.add(b.b); adj[b.b]!!.add(b.a) } }
        fun dfs(cur: Int, vis: Set<Int>): List<Int> { var best = listOf(cur); for (nb in adj[cur]?.filter { it !in vis } ?: emptyList()) { val p = dfs(nb, vis + cur); if (p.size + 1 > best.size) best = listOf(cur) + p }; return best }
        var chain = emptyList<Int>(); for (s in adj.keys) { val p = dfs(s, setOf(s)); if (p.size > chain.size) chain = p }
        val n = chain.size; val chainSet = chain.toSet()
        val root = listOf("", "", "et", "prop", "but", "pent", "hekz", "hept", "okt", "non", "dek").getOrElse(n) { "C$n" }
        var hasD = false; var hasT = false; var dp = -1; var tp = -1
        for (i in 0 until n - 1) { val b = bonds.find { (it.a == chain[i] && it.b == chain[i+1]) || (it.a == chain[i+1] && it.b == chain[i]) }; when (b?.type) { 2 -> { hasD = true; if (dp < 0) dp = i+1 }; 3 -> { hasT = true; if (tp < 0) tp = i+1 } } }
        val suf = when { hasT -> "${min(tp, n - tp)}in"; hasD -> "${min(dp, n - dp)}en"; else -> "an" }
        val gs = functionalGroups()
        val gSuf = when { gs.any { "Karboksilik" in it } -> "-oik asit"; gs.any { "Alkol" in it } -> "-ol"; gs.any { "Aldehit" in it } -> "-al"; gs.any { "Keton" in it } -> "-on"; gs.any { "Amin" in it } -> "-amin"; else -> "" }
        val isCyclic = cIds.size >= 3 && bonds.filter { it.a in cIds && it.b in cIds }.let { cb -> cb.size == cIds.size && adj.values.all { it.size == 2 } }
        val isArom = isCyclic && bonds.filter { it.a in cIds && it.b in cIds }.any { it.type == 2 }
        if (isCyclic) return if (isArom) "Benzen" else "Siklo${root}${suf}${gSuf}".replaceFirstChar { it.uppercase() }
        val branches = mutableListOf<Pair<Int, Int>>()
        for (cid in adj.keys.filter { it !in chainSet }) { val at = adj[cid]?.firstOrNull { it in chainSet } ?: continue; val ci = chain.indexOf(at); if (ci < 0) continue; var sz = 0; val vis = mutableSetOf(cid); val q = mutableListOf(cid); while (q.isNotEmpty()) { val c = q.removeAt(0); sz++; q.addAll(adj[c]?.filter { it !in vis && it !in chainSet }?.onEach { vis.add(it) } ?: emptyList()) }; branches.add(ci + 1 to sz) }
        val bn = mapOf(1 to "metil", 2 to "etil", 3 to "propil", 4 to "butil")
        if (branches.isEmpty()) return "${root}${suf}${gSuf}".replaceFirstChar { it.uppercase() }
        val parts = mutableListOf<String>()
        for ((sz, list) in branches.groupBy { it.second }.toSortedMap()) { val ps = list.sortedBy { it.first }.map { it.first.toString() }; val pre = when (list.size) { 1 -> ""; 2 -> "di"; 3 -> "tri"; else -> "tetra" }; parts.add("${ps.joinToString("-")}-${pre}${bn[sz] ?: "C$sz"}") }
        return "${parts.joinToString("-")}-${root}${suf}${gSuf}".replaceFirstChar { it.uppercase() }
    }

    fun loadPreset(name: String) {
        snap(); atoms.clear(); bonds.clear(); uid = 0; selectedAtomId = -1; connectFrom = -1
        fun a(c: Int, r: Int, e: String): Int { val at = Atom(uid++, c, r, e, 1f); atoms.add(at); return at.id }
        fun b(f: Int, t: Int, tp: Int = 1) { bonds.add(Bond(f, t, tp, 1f)) }
        val cx = cols / 2; val cy = rows / 2
        when (name) {
            "Metan" -> { a(cx, cy, "C") }
            "Etan" -> { val c1=a(cx-1,cy,"C"); val c2=a(cx+1,cy,"C"); b(c1,c2) }
            "Propan" -> { val c1=a(cx-3,cy,"C"); val c2=a(cx,cy,"C"); val c3=a(cx+3,cy,"C"); b(c1,c2); b(c2,c3) }
            "Butan" -> { val c1=a(cx-4,cy,"C"); val c2=a(cx-1,cy,"C"); val c3=a(cx+2,cy,"C"); val c4=a(cx+5,cy,"C"); b(c1,c2); b(c2,c3); b(c3,c4) }
            "Pentan" -> { val c1=a(cx-5,cy,"C"); val c2=a(cx-2,cy,"C"); val c3=a(cx+1,cy,"C"); val c4=a(cx+4,cy,"C"); val c5=a(cx+7,cy,"C"); b(c1,c2); b(c2,c3); b(c3,c4); b(c4,c5) }
            "Etilen" -> { val c1=a(cx-1,cy,"C"); val c2=a(cx+1,cy,"C"); b(c1,c2,2) }
            "Propilen" -> { val c1=a(cx-2,cy,"C"); val c2=a(cx+1,cy,"C"); val c3=a(cx+3,cy-2,"C"); b(c1,c2,2); b(c2,c3) }
            "Etanol" -> { val c1=a(cx-2,cy,"C"); val c2=a(cx+1,cy,"C"); val oh=a(cx+3,cy,"OH"); b(c1,c2); b(c2,oh) }
            "Propanol" -> { val c1=a(cx-3,cy,"C"); val c2=a(cx,cy,"C"); val c3=a(cx+3,cy,"C"); val oh=a(cx+5,cy,"OH"); b(c1,c2); b(c2,c3); b(c3,oh) }
            "Asetik Asit" -> { val c1=a(cx-2,cy,"C"); val c2=a(cx+1,cy,"C"); val o=a(cx+3,cy-2,"O"); val oh=a(cx+3,cy+2,"OH"); b(c1,c2); b(c2,o,2); b(c2,oh) }
            "Aseton" -> { val c1=a(cx-2,cy,"C"); val c2=a(cx+1,cy,"C"); val c3=a(cx+4,cy,"C"); val o=a(cx+1,cy-3,"O"); b(c1,c2); b(c2,c3); b(c2,o,2) }
            "Asetaldehit" -> { val c1=a(cx-2,cy,"C"); val c2=a(cx+1,cy,"C"); val o=a(cx+3,cy-2,"O"); b(c1,c2); b(c2,o,2) }
            "Benzen" -> { val ids=(0..5).map { i -> val ang=i*60.0; val c=(cx+2*cos(Math.toRadians(ang))).toInt(); val r=(cy+2*sin(Math.toRadians(ang))).toInt(); a(c,r,"C") }; for(i in ids.indices) b(ids[i], ids[(i+1)%6], if(i%2==0)2 else 1) }
            "Toluen" -> { val ids=(0..5).map { i -> val ang=i*60.0; val c=(cx+2*cos(Math.toRadians(ang))).toInt(); val r=(cy+2*sin(Math.toRadians(ang))).toInt(); a(c,r,"C") }; for(i in ids.indices) b(ids[i], ids[(i+1)%6], if(i%2==0)2 else 1); val mc=ids[0]; val ma=a(cx,cy+3,"C"); b(mc,ma) }
            "Fenol" -> { val ids=(0..5).map { i -> val ang=i*60.0; val c=(cx+2*cos(Math.toRadians(ang))).toInt(); val r=(cy+2*sin(Math.toRadians(ang))).toInt(); a(c,r,"C") }; for(i in ids.indices) b(ids[i], ids[(i+1)%6], if(i%2==0)2 else 1); val oh=a(cx,cy+3,"OH"); b(ids[0],oh) }
            "Siklohekzan" -> { val ids=(0..5).map { i -> val ang=i*60.0; val c=(cx+2*cos(Math.toRadians(ang))).toInt(); val r=(cy+2*sin(Math.toRadians(ang))).toInt(); a(c,r,"C") }; for(i in ids.indices) b(ids[i], ids[(i+1)%6]) }
            "Glukoz" -> { val c1=a(cx-4,cy,"C"); val c2=a(cx-2,cy,"C"); val c3=a(cx,cy,"C"); val c4=a(cx+2,cy,"C"); val c5=a(cx+4,cy,"C"); val c6=a(cx+6,cy,"C"); b(c1,c2); b(c2,c3); b(c3,c4); b(c4,c5); b(c5,c6); b(c2,a(cx-2,cy-3,"O")); b(c3,a(cx,cy-3,"O")); b(c4,a(cx+2,cy-3,"O")) }
            "Kafein" -> { val n1=a(cx-2,cy-1,"N"); val c1=a(cx,cy-1,"C"); val n2=a(cx+2,cy-1,"N"); val c2=a(cx+4,cy-1,"C"); val n3=a(cx+2,cy+1,"N"); val c3=a(cx,cy+1,"C"); b(n1,c1); b(c1,n2); b(n2,c2); b(c2,n3); b(n3,c3); b(c3,n1); b(c1,c3,2); val o1=a(cx,cy-3,"O"); val o2=a(cx+4,cy-3,"O"); b(c1,o1,2); b(c2,o2,2) }
            "Nikotin" -> { val n1=a(cx-2,cy-1,"N"); val c1=a(cx,cy-1,"C"); val c2=a(cx+2,cy-1,"C"); val c3=a(cx+4,cy-1,"C"); val n2=a(cx+2,cy+1,"N"); val c4=a(cx,cy+1,"C"); b(n1,c1); b(c1,c2); b(c2,c3); b(c3,n2); b(n2,c4); b(c4,n1); b(c1,c4,2); val mc=a(cx+6,cy,"C"); b(c3,mc) }
        }
        invalidate(); onChange?.invoke()
    }

    fun randomPreset() {
        val presets = listOf("Metan","Etan","Propan","Butan","Pentan","Etilen","Propilen","Etanol","Propanol","Asetik Asit","Aseton","Asetaldehit","Benzen","Toluen","Fenol","Siklohekzan","Glukoz","Kafein","Nikotin")
        loadPreset(presets[Random.nextInt(presets.size)])
    }

    // ── Parçacık ──
    private fun emitSparks(x: Float, y: Float, color: Int, n: Int) {
        for (i in 0 until n) { val a = Random.nextFloat() * 2f * PI.toFloat(); val s = 2f + Random.nextFloat() * 5f
            sparks += Spark(x, y, cos(a) * s, sin(a) * s, 1f, color) }
    }
    private fun updateSparks() { val it = sparks.iterator(); while (it.hasNext()) { val s = it.next(); s.x += s.vx; s.y += s.vy; s.vx *= 0.94f; s.vy *= 0.94f; s.life -= 0.035f; if (s.life <= 0f) it.remove() } }

    // ── Çizim ──
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); recalc()

        // Arka plan — karanlik gradyan
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgP)
        val bgGrad = RadialGradient(width / 2f, height / 2f, width * 0.6f,
            intArrayOf(0xFF0D1B2A.toInt(), 0xFF080F18.toInt(), 0xFF050A10.toInt()), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = bgGrad })

        // Hex grid — altıgen desen
        val hexSize = cell * 0.42f
        for (r in 0 until rows) for (c in 0 until cols) {
            val (x, y) = g2p(c, r)
            val wave = sin((x * 0.008f + y * 0.006f + pulse * 2f)) * 0.5f + 0.5f
            val nearAtom = atomAt(c, r) != null
            val nearBond = bonds.any { bond ->
                val a1 = atoms.find { it.id == bond.a }
                val a2 = atoms.find { it.id == bond.b }
                (a1 != null && a1.col == c && a1.row == r) || (a2 != null && a2.col == c && a2.row == r)
            }
            gridP.alpha = when {
                nearAtom -> 80
                nearBond -> 50
                else -> (8 + (12 * wave).toInt()).coerceIn(5, 25)
            }
            val dotR = if (nearAtom) 2.5f else if (nearBond) 1.8f else 0.8f
            canvas.drawCircle(x, y, dotR, gridP)
        }

        canvas.save(); canvas.scale(zoom, zoom, width / 2f, height / 2f); canvas.translate(panX / zoom, panY / zoom)

        // Connect mode: seçili atomdan canvas'a çizgi
        if (isConnectMode && connectFrom >= 0) {
            val from = atoms.find { it.id == connectFrom }
            if (from != null) {
                val (fx, fy) = g2p(from.col, from.row)
                // Dış halo
                pendP.alpha = (60 + 40 * sin(pulse * 6)).toInt()
                pendP.strokeWidth = 2f
                canvas.drawCircle(fx, fy, atomR() + 20f + pulse * 6f, pendP)
                // İç halo
                pendP.alpha = (120 + 80 * sin(pulse * 4)).toInt()
                pendP.strokeWidth = 3f
                canvas.drawCircle(fx, fy, atomR() + 10f + pulse * 3f, pendP)
                // Inner pulse
                val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(fx, fy, atomR() + 25f,
                        intArrayOf(0x44FFA500.toInt(), 0x11FFA500.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
                }
                canvas.drawCircle(fx, fy, atomR() + 25f + pulse * 8f, glowP)
            }
        }

        // Bağlar — glow efektli
        for (bond in bonds) {
            val a1 = atoms.find { it.id == bond.a } ?: continue
            val a2 = atoms.find { it.id == bond.b } ?: continue
            val (x1, y1) = g2p(a1.col, a1.row)
            val (x2, y2) = g2p(a2.col, a2.row)
            val dx = x2 - x1; val dy = y2 - y1; val len = sqrt(dx * dx + dy * dy); if (len < 1f) continue
            val off = atomR() * 0.3f; val px = -dy / len * off; val py = dx / len * off
            val prog = bond.anim.coerceIn(0f, 1f)
            val ex = x1 + dx * prog; val ey = y1 + dy * prog

            // Bond glow
            val bondGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val glowCol = when (bond.type) { 2 -> 0x3300DDFF.toInt(); 3 -> 0x33FF44AA.toInt(); else -> 0x22667788.toInt() }
                shader = RadialGradient((x1 + x2) / 2, (y1 + y2) / 2, len / 2 + 15f,
                    intArrayOf(glowCol, 0x00000000), null, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle((x1 + x2) / 2, (y1 + y2) / 2, len / 2 + 15f, bondGlow)

            when (bond.type) {
                1 -> {
                    bondP1.strokeWidth = 7f
                    canvas.drawLine(x1, y1, ex, ey, bondP1)
                    // Highlight
                    bondP1.strokeWidth = 2f; bondP1.alpha = 80
                    canvas.drawLine(x1 - 1f, y1 - 1f, ex - 1f, ey - 1f, bondP1)
                    bondP1.alpha = 255; bondP1.strokeWidth = 7f
                }
                2 -> {
                    canvas.drawLine(x1 + px, y1 + py, ex + px * prog, ey + py * prog, bondP2)
                    canvas.drawLine(x1 - px, y1 - py, ex - px * prog, ey - py * prog, bondP2)
                }
                3 -> {
                    canvas.drawLine(x1, y1, ex, ey, bondP1)
                    canvas.drawLine(x1 + px * 1.4f, y1 + py * 1.4f, ex + px * 1.4f * prog, ey + py * 1.4f * prog, bondP2)
                    canvas.drawLine(x1 - px * 1.4f, y1 - py * 1.4f, ex - px * 1.4f * prog, ey - py * 1.4f * prog, bondP2)
                }
            }
            if (bond.anim < 1f) { bond.anim += 0.1f; invalidate() }
        }

        // Parçacıklar
        for (s in sparks) {
            spP.color = s.col; spP.alpha = (s.life * 200).toInt().coerceIn(0, 200)
            canvas.drawCircle(s.x, s.y, 3f * s.life, spP)
            // Spark trail
            spP.alpha = (s.life * 80).toInt().coerceIn(0, 80)
            canvas.drawCircle(s.x - s.vx * 2f, s.y - s.vy * 2f, 2f * s.life, spP)
        }

        // Atomlar — 3B küre efekti
        val r = atomR(); val ts = r * 1.15f; txtP.textSize = ts
        for (atom in atoms) {
            val (ax, ay) = g2p(atom.col, atom.row)
            val sc = 0.12f + 0.88f * atom.anim.coerceIn(0f, 1f); val dr = r * sc
            val c1 = eCol[atom.elem] ?: 0xFF777777.toInt()
            val c2 = eCol2[atom.elem] ?: 0xFFAAAAAA.toInt()
            val cDk = eColDk[atom.elem] ?: 0xFF333333.toInt()

            // Outer glow
            val isTarget = isConnectMode && connectFrom >= 0 && atom.id != connectFrom && !bonds.any { (it.a == connectFrom && it.b == atom.id) || (it.a == atom.id && it.b == connectFrom) }
            val gr = dr + when {
                isTarget -> 20f + pulse * 10f
                atom.id == connectFrom -> 22f + pulse * 12f
                atom.id == selectedAtomId -> 16f + pulse * 6f
                else -> 8f + pulse * 3f
            }
            val glowColor = when {
                atom.id == connectFrom -> 0x55FFA500.toInt()
                isTarget -> 0x4400FF88.toInt()
                atom.id == selectedAtomId -> 0x4439FF14.toInt()
                else -> (c2.toInt().toLong() or 0x20000000L).toInt()
            }
            glP.shader = RadialGradient(ax, ay, gr, intArrayOf(glowColor, 0x00000000), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(ax, ay, gr, glP)

            // Gölge — atomun altında yumuşak
            val shGrad = RadialGradient(ax + 3f, ay + 5f, dr * 1.2f,
                intArrayOf(0x55000000.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
            val shGlowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = shGrad }
            canvas.drawCircle(ax + 3f, ay + 5f, dr * 1.2f, shGlowP)

            // Body —3B küre: highlight -> ana renk -> koyu kenar
            val bodyShader = RadialGradient(ax - dr * 0.32f, ay - dr * 0.35f, dr * 1.7f,
                intArrayOf(0xFFFFFFFF.toInt(), c2, c1, cDk),
                floatArrayOf(0f, 0.2f, 0.55f, 1f), Shader.TileMode.CLAMP)
            val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = bodyShader; style = Paint.Style.FILL }
            canvas.drawCircle(ax, ay, dr, bodyP)

            // Edge ring
            val edgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33000000.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f }
            canvas.drawCircle(ax, ay, dr, edgeP)

            // Specular highlight — üst sol
            val specP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(ax - dr * 0.3f, ay - dr * 0.35f, dr * 0.38f,
                    intArrayOf(0xBBFFFFFF.toInt(), 0x44FFFFFF.toInt(), 0x00000000), floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(ax - dr * 0.3f, ay - dr * 0.35f, dr * 0.38f, specP)

            // Secondary specular — alt sağ
            val spec2P = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(ax + dr * 0.22f, ay + dr * 0.28f, dr * 0.18f,
                    intArrayOf(0x44FFFFFF.toInt(), 0x00000000), null, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(ax + dr * 0.22f, ay + dr * 0.28f, dr * 0.18f, spec2P)

            // Seçim / hedef halkası
            if (atom.id == selectedAtomId) {
                selP.strokeWidth = 2.5f; selP.alpha = (150 + pulse * 105).toInt()
                canvas.drawCircle(ax, ay, dr + 6f + pulse * 4f, selP)
                selP.alpha = (80 + pulse * 60).toInt()
                canvas.drawCircle(ax, ay, dr + 2f + pulse * 2f, selP)
            }
            if (atom.id == connectFrom) {
                pendP.strokeWidth = 3f; pendP.alpha = (200 + pulse * 55).toInt()
                canvas.drawCircle(ax, ay, dr + 10f + pulse * 6f, pendP)
            }

            // Etiket — element sembolü
            if (atom.anim > 0.35f) {
                // Element sembolü
                val labelColor = when (atom.elem) {
                    "C" -> 0xFFFFFFFF.toInt()
                    "O" -> 0xFFFFCDD2.toInt()
                    "N" -> 0xFFBBDEFB.toInt()
                    "S" -> 0xFFFFF9C4.toInt()
                    "F", "Cl" -> 0xFFC8E6C9.toInt()
                    "Br" -> 0xFFE1BEE7.toInt()
                    "I" -> 0xFFD1C4E9.toInt()
                    "H" -> 0xFFCFD8DC.toInt()
                    else -> 0xFFFFFFFF.toInt()
                }
                // Shadow
                txtP.textSize = ts * sc; txtP.color = 0x55000000.toInt()
                canvas.drawText(atom.elem, ax + 1f, ay + ts * sc * 0.37f + 1f, txtP)
                // Label
                txtP.color = labelColor
                canvas.drawText(atom.elem, ax, ay + ts * sc * 0.37f, txtP)

                // H sayısı — badge şeklinde, okunabilir
                if (showH && atom.elem !in listOf("H", "OH", "COOH", "NH2", "CHO", "R")) {
                    val hc = hCount(atom)
                    if (hc > 0) {
                        val hx = ax + dr * 0.8f; val hy = ay - dr * 0.7f
                        val badgeSize = ts * 0.48f * sc

                        // Badge arka plan — yuvarlak köşeli
                        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF1A237E.toInt(); style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(hx - badgeSize * 0.9f, hy - badgeSize * 0.65f,
                            hx + badgeSize * 0.9f, hy + badgeSize * 0.55f, badgeSize * 0.3f, badgeSize * 0.3f, badgeBg)

                        // Badge kenarlık
                        val badgeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = 0xFF5C6BC0.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f
                        }
                        canvas.drawRoundRect(hx - badgeSize * 0.9f, hy - badgeSize * 0.65f,
                            hx + badgeSize * 0.9f, hy + badgeSize * 0.55f, badgeSize * 0.3f, badgeSize * 0.3f, badgeBorder)

                        // H harfi — beyaz, küçük
                        hP.textSize = badgeSize * 0.75f; hP.color = 0xFFB0BEC5.toInt()
                        hP.textAlign = Paint.Align.LEFT
                        canvas.drawText("H", hx - badgeSize * 0.55f, hy + badgeSize * 0.2f, hP)

                        // Sayı — turuncu, kalın
                        hP.textSize = badgeSize * 0.85f; hP.color = 0xFFFF9800.toInt()
                        hP.textAlign = Paint.Align.LEFT
                        val numX = hx - badgeSize * 0.55f + hP.measureText("H") * 0.85f
                        canvas.drawText("$hc", numX, hy + badgeSize * 0.2f, hP)
                        hP.textAlign = Paint.Align.LEFT
                    }
                }
            }
        }
        canvas.restore()
    }

    // ── Dokunma ──
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        scDetector.onTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = ev.x; lastY = ev.y
                // Atom kontrolü — canvas transformasyonu uygula
                val cx = (ev.x - width / 2f) / zoom + width / 2f - panX / zoom
                val cy = (ev.y - height / 2f) / zoom + height / 2f - panY / zoom
                val hit = atoms.find { val (ax, ay) = g2p(it.col, it.row); sqrt((ax - cx).pow(2) + (ay - cy).pow(2)) < atomR() + 18f }

                if (isConnectMode) {
                    if (hit != null) {
                        if (connectFrom < 0) {
                            connectFrom = hit.id; selectedAtomId = hit.id
                            val (hx, hy) = g2p(hit.col, hit.row); emitSparks(hx, hy, 0xFFFFA500.toInt(), 10)
                            invalidate(); return true
                        }
                        if (hit.id == connectFrom) {
                            connectFrom = -1; selectedAtomId = -1; invalidate(); return true
                        }
                        if (!bonds.any { (it.a == connectFrom && it.b == hit.id) || (it.a == hit.id && it.b == connectFrom) }) {
                            snap()
                            bonds.add(Bond(connectFrom, hit.id, currentBondType, 0f))
                            val (fx, fy) = g2p(atoms.find { it.id == connectFrom }?.col ?: 0, atoms.find { it.id == connectFrom }?.row ?: 0)
                            emitSparks((fx + cx) / 2, (fy + cy) / 2, 0xFF00DDFF.toInt(), 20)
                            onChange?.invoke()
                        }
                        connectFrom = -1; selectedAtomId = -1; invalidate(); return true
                    }
                    return true
                }
                // Normal mod
                if (hit != null) {
                    selectedAtomId = hit.id; dragId = hit.id
                    val (hx, hy) = g2p(hit.col, hit.row); lastX = hx; lastY = hy
                    invalidate(); return true
                }
                // Boş alana dokunma → atom ekle
                val (gc, gr) = p2g(cx, cy)
                if (atomAt(gc, gr) == null) {
                    snap(); val at = Atom(uid++, gc, gr, currentElem, 0f); atoms.add(at)
                    val (nx, ny) = g2p(gc, gr); emitSparks(nx, ny, eCol[currentElem] ?: 0xFF777777.toInt(), 12)
                    ValueAnimator.ofFloat(0f, 1f).apply { duration = 350; interpolator = OvershootInterpolator(2.5f)
                        addUpdateListener { at.anim = it.animatedValue as Float; invalidate() }; start() }
                    onChange?.invoke()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragId >= 0 && !isConnectMode) {
                    val atom = atoms.find { it.id == dragId } ?: return true
                    val cx = (ev.x - width / 2f) / zoom + width / 2f - panX / zoom
                    val cy = (ev.y - height / 2f) / zoom + height / 2f - panY / zoom
                    val (gc, gr) = p2g(cx, cy)
                    if (gc != atom.col || gr != atom.row) { atom.col = gc; atom.row = gr; invalidate() }
                    return true
                }
                if (zoom != 1f || panX != 0f || panY != 0f) {
                    panX += ev.x - lastX; panY += ev.y - lastY; lastX = ev.x; lastY = ev.y; invalidate(); return true
                }
            }
            MotionEvent.ACTION_UP -> { dragId = -1; lastX = ev.x; lastY = ev.y }
        }
        return true
    }

    // Public fonksiyonlar
    fun removeSelected() { if (selectedAtomId < 0) return; snap(); atoms.removeAll { it.id == selectedAtomId }; bonds.removeAll { it.a == selectedAtomId || it.b == selectedAtomId }; selectedAtomId = -1; connectFrom = -1; invalidate(); onChange?.invoke() }
    fun clearAll() { if (atoms.isEmpty()) return; snap(); atoms.clear(); bonds.clear(); uid = 0; selectedAtomId = -1; connectFrom = -1; invalidate(); onChange?.invoke() }
    fun addRing(benzen: Boolean) {
        snap(); val cx = cols / 2; val cy = rows / 2; val ids = mutableListOf<Int>()
        for (i in 0..5) { val ang = i * 60.0; val c = (cx + 2 * cos(Math.toRadians(ang))).toInt().coerceIn(0, cols - 1); val r = (cy + 2 * sin(Math.toRadians(ang))).toInt().coerceIn(0, rows - 1)
            if (atomAt(c, r) != null) continue; val at = Atom(uid++, c, r, "C", 1f); atoms.add(at); ids.add(at.id) }
        for (i in ids.indices) bonds.add(Bond(ids[i], ids[(i + 1) % ids.size], if (benzen && i % 2 == 0) 2 else 1, 1f))
        invalidate(); onChange?.invoke()
    }
}

// ═══════════════════════════════════════════════════════════════
//  OrganicFragment
// ═══════════════════════════════════════════════════════════════
class OrganicFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private lateinit var canvas: MoleculeCanvas
    private lateinit var nameTV: TextView; private lateinit var formulaTV: TextView
    private lateinit var detailTV: TextView; private lateinit var groupTV: TextView
    private lateinit var infoTV: TextView
    private val exList = listOf("Metan","Etan","Propan","Butan","Pentan","Etilen","Propilen","Etanol","Propanol","Asetik Asit","Aseton","Asetaldehit","Benzen","Toluen","Fenol","Siklohekzan","Glukoz","Kafein","Nikotin")
    private var exIdx = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_organic, container, false)

        // Canvas'ı ekle
        val ph = v.findViewById<View>(R.id.org_canvas_placeholder)
        val par = ph.parent as ViewGroup; val i = par.indexOfChild(ph); par.removeView(ph)
        canvas = MoleculeCanvas(requireContext()).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (440 * resources.displayMetrics.density).toInt()); onChange = { refresh() } }
        par.addView(canvas, i)

        nameTV = v.findViewById(R.id.org_name_result); formulaTV = v.findViewById(R.id.org_formula_result)
        detailTV = v.findViewById(R.id.org_mol_detail); groupTV = v.findViewById(R.id.org_group_badge)
        infoTV = v.findViewById(R.id.org_info)

        // Element seçimi
        val elems = listOf(R.id.org_elem_c to "C", R.id.org_elem_h to "H", R.id.org_elem_o to "O", R.id.org_elem_n to "N",
            R.id.org_elem_s to "S", R.id.org_elem_cl to "Cl", R.id.org_elem_br to "Br", R.id.org_elem_f to "F",
            R.id.org_elem_oh to "OH", R.id.org_elem_cooh to "COOH", R.id.org_elem_nh2 to "NH2", R.id.org_elem_cho to "CHO")
        fun hl(actId: Int) { elems.forEach { (id, _) -> v.findViewById<TextView>(id)?.alpha = if (id == actId) 1f else 0.45f } }
        v.findViewById<TextView>(R.id.org_elem_c)?.alpha = 1f
        elems.forEach { (id, el) -> v.findViewById<TextView>(id)?.setOnClickListener { canvas.currentElem = el; hl(id); AnimUtils.press(it) } }

        // Bağ tipi
        fun bhl(actId: Int) { listOf(R.id.org_bond_single, R.id.org_bond_double, R.id.org_bond_triple).forEach { id -> v.findViewById<TextView>(id)?.alpha = if (id == actId) 1f else 0.45f } }
        v.findViewById<TextView>(R.id.org_bond_single)?.setOnClickListener { canvas.currentBondType = 1; bhl(R.id.org_bond_single); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_bond_double)?.setOnClickListener { canvas.currentBondType = 2; bhl(R.id.org_bond_double); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_bond_triple)?.setOnClickListener { canvas.currentBondType = 3; bhl(R.id.org_bond_triple); AnimUtils.press(it) }

        // Araçlar
        v.findViewById<TextView>(R.id.org_ring)?.setOnClickListener { canvas.addRing(false); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_benzene)?.setOnClickListener { canvas.addRing(true); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_example)?.setOnClickListener { canvas.loadPreset(exList[exIdx]); exIdx = (exIdx + 1) % exList.size; AnimUtils.press(it); refresh() }
        v.findViewById<TextView>(R.id.org_random)?.setOnClickListener { canvas.randomPreset(); AnimUtils.press(it); refresh() }

        // Aksiyonlar
        val conBtn = v.findViewById<TextView>(R.id.org_connect) ?: return v
        conBtn.setOnClickListener {
            canvas.isConnectMode = !canvas.isConnectMode; if (!canvas.isConnectMode) canvas.connectFrom = -1
            conBtn.text = if (canvas.isConnectMode) "BAGLA: ACIK" else "Bagla"
            conBtn.setTextColor(if (canvas.isConnectMode) 0xFF81C784.toInt() else 0xFF4DD0E1.toInt())
            canvas.invalidate(); AnimUtils.press(it)
        }
        v.findViewById<TextView>(R.id.org_del)?.setOnClickListener { canvas.removeSelected(); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_undo)?.setOnClickListener { canvas.undo(); refresh(); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_redo)?.setOnClickListener { canvas.redo(); refresh(); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_clear)?.setOnClickListener { canvas.clearAll(); nameTV.text = ""; formulaTV.text = ""; detailTV.text = ""; groupTV.text = ""; infoTV.text = "Atom ekle, bagla, adlandir"; AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_toggle_h)?.setOnClickListener { canvas.showH = !canvas.showH; canvas.invalidate(); AnimUtils.press(it) }
        v.findViewById<TextView>(R.id.org_adlandir)?.setOnClickListener { refresh(); AnimUtils.press(it) }

        v.findViewById<View>(R.id.btn_help)?.setOnClickListener {
            HelpDialog.showGuide(requireContext(), "Organik Kimya", "Molekul olusturup adlandirin.",
                listOf("Bos alana dokunarak atom ekle.", "Bagla modunda iki atoma dokunarak bag olustur.", "Halka/Benzen ile siklik olustur.", "Cekerek atom tasi.", "Fonksiyonel gruplar otomatik tespit edilir."))
        }
        v.findViewById<TextView>(R.id.org_baslik)?.let { AnimUtils.gradientTitle(it) }
        listOf(R.id.org_header to 0L, R.id.org_scene_card to 60L, R.id.org_elems_card to 120L, R.id.org_bonds_card to 180L, R.id.org_tools_card to 240L, R.id.org_actions_card to 300L, R.id.org_adlandir_card to 360L).forEach { (id, d) -> v.findViewById<View>(id)?.let { it.alpha = 0f; it.post { AnimUtils.slideUpFade(it, d) } } }
        return v
    }

    private fun refresh() {
        val n = canvas.iupacName(); val f = canvas.formula(); val mw = canvas.molWeight()
        nameTV.text = n; formulaTV.text = f
        groupTV.text = canvas.functionalGroups().joinToString(" + ")
        var d = "${"%.3f".format(mw)} g/mol"
        if (canvas.functionalGroups().isNotEmpty()) d += " | ${canvas.functionalGroups().size} grup"
        d += " | ${canvas.atoms.size} atom, ${canvas.bonds.size} bag"
        detailTV.text = d

        // Ozellik karti
        val props = canvas.getProperties()
        val sb = StringBuilder()
        for ((k, v) in props) sb.appendLine("${k}: ${v}")
        infoTV.text = sb.toString().trim()
        canvas.invalidate()
    }
}

