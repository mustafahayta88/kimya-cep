package com.kimya.uygulama.utils

import kotlin.math.abs

object ReactionBalancer {

    data class BalancedReaction(
        val reaktifler: List<Pair<String, Int>>,
        val urunler: List<Pair<String, Int>>,
        val tip: String = "",
        val deltaH: Double? = null,
        /** false = otomatik denkleştirilemedi, katsayılar güvenilmez */
        val denkMi: Boolean = true
    )

    /** Ortak ayrıştırıcıyı kullanır (parantez destekler); hatalı formülde boş döner */
    fun parseBilesif(formul: String): Map<String, Int> {
        return KimyaData.formulAyristir(formul.trim()) ?: emptyMap()
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
        val finalCoeffs = smallestIntegers(coeffs)
        if (finalCoeffs != null && atomlarDenkMi(reaktifBilesikler, urunBilesiIler, finalCoeffs, nReact)) {
            return BalancedReaction(
                reaktifler = reaktifler.zip(finalCoeffs.take(nReact)),
                urunler = urunler.zip(finalCoeffs.drop(nReact)),
                tip = tipBul(reaktifler, urunler),
                denkMi = true
            )
        }

        // Denkleştirilemedi: 1'li katsayı + UYARI bayrağı (sessizce doğruymuş gibi gösterme!)
        return BalancedReaction(
            reaktifler = reaktifler.zip(List(nReact) { 1 }),
            urunler = urunler.zip(List(nProd) { 1 }),
            tip = tipBul(reaktifler, urunler),
            denkMi = false
        )
    }

    /** Çözümü doğrulanmış en küçük tam sayı katsayılara çevirir; olmazsa null */
    private fun smallestIntegers(coeffs: DoubleArray?): List<Int>? {
        if (coeffs == null || coeffs.any { it <= 1e-9 }) return null
        val minPos = coeffs.minOrNull()!!
        val normed = coeffs.map { it / minPos }
        for (m in 1..12) {
            val scaled = normed.map { it * m }
            if (scaled.all { abs(it - kotlin.math.round(it)) < 0.02 && it < 500 }) {
                return scaled.map { kotlin.math.round(it).toInt() }
            }
        }
        return null
    }

    private fun atomlarDenkMi(
        reaktifBilesikler: List<Map<String, Int>>, urunBilesiIler: List<Map<String, Int>>,
        coeffs: List<Int>, nReact: Int
    ): Boolean {
        val sol = mutableMapOf<String, Int>()
        val sag = mutableMapOf<String, Int>()
        reaktifBilesikler.forEachIndexed { i, mp ->
            for ((el, n) in mp) sol[el] = (sol[el] ?: 0) + n * coeffs[i]
        }
        urunBilesiIler.forEachIndexed { i, mp ->
            for ((el, n) in mp) sag[el] = (sag[el] ?: 0) + n * coeffs[nReact + i]
        }
        return (sol.keys + sag.keys).all { (sol[it] ?: 0) == (sag[it] ?: 0) }
    }

    private fun solveLinearSystem(matrix: Array<DoubleArray>, nVars: Int): DoubleArray? {
        val m = matrix.size
        val n = nVars
        val augmented = Array(m + 1) { i ->
            if (i < m) matrix[i].copyOf(n + 1) else DoubleArray(n + 1)
        }

        augmented[m][n - 1] = 1.0 // sabitleme: son katsayı = 1 (ölçek çapası)
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

    private fun tipBul(reaktifler: List<String>, urunler: List<String>): String {
        val all = reaktifler + urunler
        return when {
            "O2" in reaktifler && all.any { "CO2" in it || "H2O" in it } -> "Yanma"
            reaktifler.size == 1 && urunler.size > 1 -> "Bozunma"
            reaktifler.size > 1 && urunler.size == 1 -> "Sentez"
            ("HCl" in reaktifler && "NaOH" in reaktifler) || ("H2SO4" in reaktifler && "NaOH" in reaktifler) -> "Nötrleşme"
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
        val uyariStr = if (!r.denkMi) "  ⚠ otomatik denkleştirilemedi" else ""
        val entalpiStr = if (r.deltaH != null) "  ΔH=${r.deltaH} kJ/mol" else ""
        return "$reaktifStr -> $urunStr$tipStr$entalpiStr$uyariStr"
    }

    // findLCM/gcd kaldırıldı: smallestIntegers + atomlarDenkMi kullanılıyor

    // ÖLÜ KOD (findLCM ile birlikte kaldırıldı) -- private fun gcdX(a: Int, I: Int): Int = if (I == 0) a else gcd(I, a % I)
}
