package com.kimya.uygulama.utils

object Calculator {
    private val molKutleleri = mapOf(
        "NaOH" to 40.0, "HCl" to 36.46, "H2SO4" to 98.08, "NH3" to 17.03,
        "NaCl" to 58.44, "KOH" to 56.11, "HNO3" to 63.01, "CH3COOH" to 60.05,
        "NaHCO3" to 84.01, "CaCO3" to 100.09, "Ca(OH)2" to 74.09,
        "H3PO4" to 98.00, "AgNO3" to 169.87, "KI" to 166.00,
        "KMnO4" to 158.04, "FeCl3" to 162.20, "CuSO4" to 159.61
    )

    private val stoikiMol = mapOf(
        "H2SO4" to 98.08, "NaOH" to 40.0, "Na2SO4" to 142.04, "H2O" to 18.02,
        "HCl" to 36.46, "NaCl" to 58.44, "CaCO3" to 100.09, "CO2" to 44.01,
        "HNO3" to 63.01, "KOH" to 56.11, "H3PO4" to 98.00, "Na3PO4" to 163.94,
        "AgNO3" to 169.87, "AgCl" to 143.32, "CuSO4" to 159.61,
        "FeCl3" to 162.20, "KMnO4" to 158.04, "NH3" to 17.03
    )

    fun molariteHesapla(madde: String, miitar: Double, birim: String, hacim: Double, hacimBirim: String): String {
        val mK = molKutleleri[madde] ?: return "Bilinmeyen madde"
        val hacimL = if (hacimBirim == "mL") hacim / 1000.0 else hacim
        if (hacimL <= 0) return "Hacim pozitif olmalı"
        val mol = if (birim == "gram") miitar / mK else miitar
        val M = mol / hacimL
        return "Molarite = %.6f M".format(M)
    }

    fun stokiyometriHesapla(mol1: String, mol2: String, miitar: Double, birim: String): String {
        val I1 = stoikiMol[mol1] ?: return "$mol1 bulunamadı"
        val I2 = stoikiMol[mol2] ?: return "$mol2 bulunamadı"
        val mol = if (birim == "gram") miitar / I1 else miitar
        val m2 = mol * I2
        return "%s: %.4f gram (%.6f mol)".format(mol2, m2, mol)
    }

    fun seyreltmeHesapla(C1: Double, V1: Double, C2: Double, V2: Double, mod: String): String {
        val r = when (mod) {
            "C1" -> if (V1 <= 0) null else C2 * V2 / V1
            "V1" -> if (C1 <= 0) null else C2 * V2 / C1
            "C2" -> if (V2 <= 0) null else C1 * V1 / V2
            "V2" -> if (C2 <= 0) null else C1 * V1 / C2
            else -> null
        }
        if (r == null) return "Gecersiz deger"
        val fmt = if (r == r.toLong().toDouble()) "%.0f" else if (r < 0.01) "%.4f" else "%.2f"
        return "$mod = ${"$fmt".format(r)}"
    }

    fun yogunlukHesapla(kutle: Double, hacim: Double, yo: Double, mod: String): String {
        val r = when (mod) {
            "d" -> if (hacim <= 0) null else kutle / hacim
            "m" -> if (yo <= 0) null else yo * hacim
            "V" -> if (yo <= 0) null else kutle / yo
            else -> null
        }
        return if (r == null) "Geçersiz değer" else {
            val label = mapOf("d" to "Yoğunluk", "m" to "Kütle", "V" to "Hacim")[mod]
            "$label = %.6f".format(r)
        }
    }

    fun cozeltiHesapla(M1: Double, V1: Double, M2: Double, V2: Double, mod: String): String {
        val r = when (mod) {
            "M1" -> if (V1 <= 0) null else M2 * V2 / V1
            "V1" -> if (M1 <= 0) null else M2 * V2 / M1
            "M2" -> if (V2 <= 0) null else M1 * V1 / V2
            "V2" -> if (M2 <= 0) null else M1 * V1 / M2
            else -> null
        }
        return if (r == null) "Geçersiz değer" else "%s = %.6f".format(mod, r)
    }

    fun donusumHesapla(m: Double?, n: Double?, Ma: Double?, mod: String): String {
        val r = when (mod) {
            "m" -> if (n == null || Ma == null || Ma <= 0) null else n * Ma
            "n" -> if (m == null || Ma == null || Ma <= 0) null else m / Ma
            "Ma" -> if (m == null || n == null || n <= 0) null else m / n
            else -> null
        }
        return if (r == null) "Geçersiz değer" else {
            val label = mapOf("m" to "Kütle (g)", "n" to "Mol", "Ma" to "Mol Kütlesi (g/mol)")[mod]
            "$label = %.6f".format(r)
        }
    }

    data class RegResult(val a: Double, val b: Double, val r2: Double)

    fun regresyon(data: List<Pair<Double, Double>>): RegResult? {
        if (data.size < 2) return null
        val n = data.size
        val sumX = data.sumOf { it.first }
        val sumY = data.sumOf { it.second }
        val sumXY = data.sumOf { it.first * it.second }
        val sumX2 = data.sumOf { it.first * it.first }
        val denom = n * sumX2 - sumX * sumX
        if (denom == 0.0) return null
        val a = (n * sumXY - sumX * sumY) / denom
        val b = (sumY - a * sumX) / n
        val yMean = sumY / n
        val ssRes = data.sumOf { (it.second - (a * it.first + b)).let { it * it } }
        val ssTot = data.sumOf { (it.second - yMean).let { it * it } }
        val r2 = if (ssTot != 0.0) 1 - ssRes / ssTot else 0.0
        return RegResult(a, b, r2)
    }
}
