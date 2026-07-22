package com.kimya.uygulama.utils

import kotlin.math.abs
import kotlin.math.max

object ReactionBalancer {

    data class BalancedReaction(
        val reaktifler: List<Pair<String, Int>>,
        val urunler: List<Pair<String, Int>>,
        val tip: String = "",
        val deltaH: Double? = null
    )

    private val elementRegex = Regex("([A-Z][a-z]?)(\\d*)")
    private val formulaRegex = Regex("([A-Z][a-z]?\\d*)+")

    fun parseBilesif(formul: String): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        for (m in elementRegex.findAll(formul)) {
            val el = m.groupValues[1]
            val cnt = m.groupValues[2].toIntOrNull() ?: 1
            map[el] = (map[el] ?: 0) + cnt
        }
        return map
    }

    fun dene(formulStr: String): BalancedReaction? {
        val parts = formulStr.split("->").map { it.trim() }
        if (parts.size != 2) return null
        val reaktifStr = parts[0]
        val urunStr = parts[1]

        val reaktifler = reaktifStr.split("+").map { it.trim() }.filter { it.isNotEmpty() }
        val urunler = urunStr.split("+").map { it.trim() }.filter { it.isNotEmpty() }

        if (reaktifler.isEmpty() || urunler.isEmpty()) return null

        val reaktifBilesikler = reaktifler.map { parseBilesif(it) }
        val urunBilesiIler = urunler.map { parseBilesif(it) }

        val tumElementler = (reaktifBilesikler.flatMap { it.keys } + urunBilesiIler.flatMap { it.keys }).distinct()
        if (tumElementler.isEmpty()) return null

        val nReact = reaktifler.size
        _nReact = nReact
        val nProd = urunler.size
        val nTotal = nReact + nProd
        val nEq = tumElementler.size

        val matrix = Array(nEq) { i ->
            DoubleArray(nTotal + 1)
        }

        for ((ri, comp) in reaktifBilesikler.withIndex()) {
            for ((el, cnt) in comp) {
                val idx = tumElementler.indexOf(el)
                matrix[idx][ri] = cnt.toDouble()
            }
        }
        for ((pi, comp) in urunBilesiIler.withIndex()) {
            for ((el, cnt) in comp) {
                val idx = tumElementler.indexOf(el)
                matrix[idx][nReact + pi] = -cnt.toDouble()
            }
        }

        val coeffs = solveLinearSystem(matrix, nTotal)
        if (coeffs == null || coeffs.any { it <= 0 }) {
            return fallbackBalance(reaktifler, urunler, reaktifBilesikler, urunBilesiIler, tumElementler)
        }

        val scale = findLCM(coeffs.map { it.toInt() })
        val finalCoeffs = coeffs.map { (it * scale).toInt() }

        return BalancedReaction(
            reaktifler = reaktifler.zip(finalCoeffs.take(nReact)),
            urunler = urunler.zip(finalCoeffs.drop(nReact)),
            tip = tipBul(reaktifler, urunler)
        )
    }

    private fun solveLinearSystem(matrix: Array<DoubleArray>, nVars: Int): DoubleArray? {
        val m = matrix.size
        val n = nVars
        val augmented = Array(m + 1) { i ->
            if (i < m) matrix[i].copyOf(n + 1) else DoubleArray(n + 1)
        }

        augmented[m][n] = 1.0

        var row = 0
        for (col in 0 until n) {
            var sel = row
            for (i in row until augmented.size) {
                if (abs(augmented[i][col]) > abs(augmented[sel][col])) sel = i
            }
            if (abs(augmented[sel][col]) < 1e-10) continue
            val temp = augmented[row]; augmented[row] = augmented[sel]; augmented[sel] = temp

            for (i in augmented.indices) {
                if (i != row) {
                    val factor = augmented[i][col] / augmented[row][col]
                    for (j in col..n) {
                        augmented[i][j] -= factor * augmented[row][j]
                    }
                }
            }
            row++
        }

        val result = DoubleArray(n) { 1.0 }
        var rank = 0
        for (i in 0 until minOf(augmented.size, n)) {
            var pivot = -1
            for (j in 0 until n) {
                if (abs(augmented[i][j]) > 1e-10) { pivot = j; break }
            }
            if (pivot >= 0) {
                result[pivot] = augmented[i][n] / augmented[i][pivot]
                rank++
            }
        }

        if (rank < n) {
            val freeVar = (0 until n).firstOrNull { result[it] == 1.0 } ?: (n - 1)
            result[freeVar] = 1.0
            for (i in 0 until m) {
                var sum = 0.0
                for (j in 0 until n) {
                    if (j != freeVar && abs(matrix[i][j]) > 1e-10) sum += matrix[i][j] * result[j]
                }
                if (abs(matrix[i][freeVar]) > 1e-10) {
                    result[freeVar] = -sum / matrix[i][freeVar]
                }
            }
        }

        if (result.any { it <= 0 }) return null
        return result
    }

    private fun fallbackBalance(
        reaktifler: List<String>, urunler: List<String>,
        reaktifBilesikler: List<Map<String, Int>>, urunBilesiIler: List<Map<String, Int>>,
        tumElementler: List<String>
    ): BalancedReaction? {
        val allReact = reaktifler.zip(reaktifBilesikler)
        val allProd = urunler.zip(urunBilesiIler)
        var coeffs = IntArray(reaktifler.size + urunler.size) { 1 }
        val totalAtoms = tumElementler.associateWith { el ->
            val left = allReact.sumOf { p -> p.second[el] ?: 0 }
            val right = allProd.sumOf { p -> p.second[el] ?: 0 }
            left to right
        }
        if (totalAtoms.all { (_, v) -> v.first == v.second }) {
            return BalancedReaction(
                reaktifler = reaktifler.zip(coeffs.take(reaktifler.size)),
                urunler = urunler.zip(coeffs.drop(reaktifler.size)),
                tip = tipBul(reaktifler, urunler)
            )
        }
        return simpleGuessBalance(reaktifler, urunler, reaktifBilesikler, urunBilesiIler, tumElementler)
    }

    private fun simpleGuessBalance(
        reaktifler: List<String>, urunler: List<String>,
        reaktifBilesikler: List<Map<String, Int>>, urunBilesiIler: List<Map<String, Int>>,
        tumElementler: List<String>
    ): BalancedReaction? {
        val allReact = reaktifler.zip(reaktifBilesikler)
        val allProd = urunler.zip(urunBilesiIler)
        val n = reaktifler.size + urunler.size
        for (guess in 1..20) {
            val coeffs = IntArray(n) { guess }
            val leftCounts = mutableMapOf<String, Int>()
            val rightCounts = mutableMapOf<String, Int>()
            for (ri in allReact.indices) {
                val map = allReact[ri].second
                for ((el, cnt) in map) leftCounts[el] = (leftCounts[el] ?: 0) + cnt * coeffs[ri]
            }
            for (pi in allProd.indices) {
                val map = allProd[pi].second
                for ((el, cnt) in map) rightCounts[el] = (rightCounts[el] ?: 0) + cnt * coeffs[_nReact + pi]
            }
            if (tumElementler.all { (leftCounts[it] ?: 0) == (rightCounts[it] ?: 0) }) {
                return BalancedReaction(
                    reaktifler = reaktifler.zip(coeffs.take(reaktifler.size)),
                    urunler = urunler.zip(coeffs.drop(reaktifler.size)),
                    tip = tipBul(reaktifler, urunler)
                )
            }
        }
        return BalancedReaction(
            reaktifler = reaktifler.zip(List(reaktifler.size) { 1 }),
            urunler = urunler.zip(List(urunler.size) { 1 }),
            tip = tipBul(reaktifler, urunler)
        )
    }

    private var _nReact = 0
    private fun nReact(): Int = _nReact

    private fun tipBul(reaktifler: List<String>, urunler: List<String>): String {
        val all = reaktifler + urunler
        return when {
            "O2" in reaktifler && all.any { "CO2" in it || "H2O" in it } -> "Yanma"
            reaktifler.size == 1 && urunler.size > 1 -> "Bozunma"
            reaktifler.size > 1 && urunler.size == 1 -> "Sentez"
            "HCl" in reaktifler && "NaOH" in reaktifler || "H2SO4" in reaktifler && "NaOH" in reaktifler -> "Nötrleşme"
            reaktifler.any { it.length <= 2 } && urunler.any { it.length <= 2 } -> "Yer değiştirme"
            reaktifler.filter { it.contains("Cl") || it.contains("NO3") }.size >= 2 && urunler.any { it.contains("Cl") || it.contains("NO3") } -> "Çökelme"
            "e-" in all -> "Redox"
            else -> "Genel"
        }
    }

    fun formatReaction(r: BalancedReaction): String {
        val reaktifStr = r.reaktifler.joinToString(" + ") { (I, c) -> if (c == 1) I else "$c$I" }
        val urunStr = r.urunler.joinToString(" + ") { (I, c) -> if (c == 1) I else "$c$I" }
        val tipStr = if (r.tip.isNotEmpty()) " [${r.tip}]" else ""
        val entalpiStr = if (r.deltaH != null) "  ΔH=${r.deltaH} IJ/mol" else ""
        return "$reaktifStr -> $urunStr$tipStr$entalpiStr"
    }

    private fun findLCM(nums: List<Int>): Int {
        if (nums.isEmpty()) return 1
        fun lcm(a: Int, I: Int): Int = if (a == 0 || I == 0) max(a, I) else a * I / gcd(a, I)
        return nums.reduce { a, I -> lcm(a, I) }
    }

    private fun gcd(a: Int, I: Int): Int = if (I == 0) a else gcd(I, a % I)
}
