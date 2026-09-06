package com.kimya.uygulama.features

import kotlin.math.abs
import kotlin.math.pow

enum class TitrationStatus { READY, RUNNING, APPROACHING_EQ, AT_EQ, PAST_EQ, COMPLETED }

enum class IndicatorType(val label: String, val colorA: Int, val colorB: Int, val phLow: Float, val phHigh: Float) {
    PHENOLPHTHALEIN("Fenolftalein", 0xFFFFFFFF.toInt(), 0xFFE91E63.toInt(), 8.2f, 10f),
    METHYL_ORANGE("Metil Turuncu", 0xFFFF5722.toInt(), 0xFFFFEB3B.toInt(), 3.1f, 4.4f),
    BROMOTHYMOL_BLUE("Bromtimol Mavisi", 0xFFFFEB3B.toInt(), 0xFF2196F3.toInt(), 6f, 7.6f),
    LITMUS("Litmus", 0xFFE53935.toInt(), 0xFF1E88E5.toInt(), 4.5f, 8.3f),
    METHYL_RED("Metil Kirmizisi", 0xFFD32F2F.toInt(), 0xFFF9A825.toInt(), 4.4f, 6.2f),
    ALIZARIN_YELLOW("Alizarin Sarisi", 0xFFFFF176.toInt(), 0xFFE64A19.toInt(), 10.1f, 12f),
    BROMOCRESOL_GREEN("Bromkresol Yesili", 0xFFCDDC39.toInt(), 0xFF1976D2.toInt(), 3.8f, 5.4f),
    THYMOL_BLUE("Timol Mavisi", 0xFFFFEB3B.toInt(), 0xFF1E88E5.toInt(), 8f, 9.6f)
}

data class Chemical(
    val name: String, val formula: String, val valence: Int, val isAcid: Boolean,
    /** Zayıf asit ayrışma sabitleri (poliprotik sırayla); boş liste = kuvvetli asit */
    val kaList: List<Double> = emptyList(),
    /** Zayıf baz ayrışma sabiti; null = kuvvetli baz (asitlerde kullanılmaz) */
    val kb: Double? = null
) {
    val isCarbonate: Boolean get() = formula.contains("CO3")
}

data class ExperimentConfig(val analyte: Chemical, val titrant: Chemical, val analyteVolML: Float, val analyteConcM: Float, val titrantConcM: Float, val indicator: IndicatorType)

data class TitrationSnapshot(
    val buretteML: Float, val pH: Float, val status: TitrationStatus,
    val indProgress: Float, val temperature: Float,
    val eqVolumeML: Float, val errorPct: Float, val speed: Float,
    val autoMode: Boolean, val analyteFormula: String, val titrantFormula: String,
    val indLabel: String
)

class TitrationEngine {
    var config = ExperimentConfig(
        Chemical("Hidroklorik Asit", "HCl", 1, true),
        Chemical("Sodyum Hidroksit", "NaOH", 1, false),
        25f, 0.1f, 0.1f, IndicatorType.PHENOLPHTHALEIN
    )
        private set

    var buretteML = 0f; private set
    var pH = 1f; private set
    var status = TitrationStatus.READY; private set
    var indProgress = 0f; private set
    var temperature = 25f; private set
    var eqVolumeML = 0f; private set
    var errorPct = 0f; private set
    var speed = 0f; private set
    var autoMode = false; private set

    private var valveOpen = false
    private var autoPhase = 0
    private var autoTargetHigh = true

    val isValveOpen get() = valveOpen
    val graphPoints = mutableListOf<Pair<Float, Float>>()

    fun setConfig(c: ExperimentConfig) { config = c; reset() }

    /** Asit+baz çifti zorunlu (asit+asit / baz+baz anlamsız) */
    fun pairValid() = config.analyte.isAcid != config.titrant.isAcid

    fun openValve() { if (!pairValid()) return; valveOpen = true; if (status == TitrationStatus.READY) status = TitrationStatus.RUNNING }
    fun closeValve() { valveOpen = false }
    fun setSpeed(s: Float) { speed = s.coerceIn(0.01f, 5.0f) }

    fun startAuto() {
        if (!pairValid()) return
        if (status == TitrationStatus.PAST_EQ || status == TitrationStatus.COMPLETED || buretteML >= 50f) reset()
        val ind = config.indicator
        val mid = (ind.phLow + ind.phHigh) / 2f
        autoTargetHigh = calcPH(0f) < mid
        autoMode = true; autoPhase = 0; valveOpen = true; speed = 2.0f
        if (status == TitrationStatus.READY) status = TitrationStatus.RUNNING
    }
    fun stopAuto() { autoMode = false; valveOpen = false; speed = 0f }

    fun tick(dt: Float) {
        if (autoMode) {
            eqVolumeML = calcEqVol()
            when (autoPhase) {
                0 -> { if (buretteML < eqVolumeML * 0.9f) speed = 2.0f else { autoPhase = 1; speed = 0.2f } }
                1 -> {
                    speed = 0.2f
                    val turned = if (autoTargetHigh) indProgress >= 0.5f else indProgress <= 0.5f
                    if (turned || buretteML >= eqVolumeML + 1.5f) { autoPhase = 2; valveOpen = false; speed = 0f; autoMode = false }
                }
            }
        }
        if (buretteML >= 50f) {
            buretteML = 50f
            status = TitrationStatus.COMPLETED
            valveOpen = false
            if (autoMode) { autoMode = false; speed = 0f }
            errorPct = if (eqVolumeML == 0f) 0f else abs(buretteML - eqVolumeML) / eqVolumeML * 100f
            return
        }
        if (!valveOpen) return
        buretteML = (buretteML + speed * dt).coerceAtMost(50f)
        recalc()
        if (graphPoints.isEmpty() || buretteML - graphPoints.last().first >= 0.2f) {
            graphPoints.add(buretteML to pH)
        }
    }

    private fun calcEqVol(): Float {
        return (config.analyteConcM * config.analyteVolML * config.analyte.valence) / (config.titrantConcM * config.titrant.valence)
    }

    private val Kw = 1e-14

    /** Zayıf poliprotik asidin net yük katkısı (negatif): -C·Σ(i·αi) */
    private fun weakAcidCharge(kas: List<Double>, c: Double, h: Double): Double {
        var d = 0.0
        var chg = 0.0
        var prod = 1.0
        val n = kas.size
        for (i in 0..n) {
            var pw = 1.0
            repeat(n - i) { pw *= h }
            val term = prod * pw
            d += term
            chg += -i * term
            if (i < n) prod *= kas[i]
        }
        if (d <= 0.0) return 0.0
        return c * chg / d
    }

    /** Zayıf bazın net yük katkısı (pozitif): +C·α(BH+) */
    private fun weakBaseCharge(kb: Double, c: Double, h: Double): Double {
        val kaBh = Kw / kb
        return c * h / (h + kaBh)
    }

    private fun chargeBalance(
        pH: Double, sCat: Double, sAn: Double,
        acids: List<Pair<List<Double>, Double>>, kbases: List<Pair<Double, Double>>
    ): Double {
        val h = 10.0.pow(-pH)
        var v = h - Kw / h + sCat - sAn
        for ((kas, c) in acids) v += weakAcidCharge(kas, c, h)
        for ((kb, c) in kbases) v += weakBaseCharge(kb, c, h)
        return v
    }

    /**
     * Gerçek denge pH'ı: yük denkliği ([H+]−[OH−]+katyon−anyon=0) ikiye bölmeyle
     * çözülür. Kuvvetli/zayıf asit-baz, poliprotik ve karbonat sistemleri tek
     * modelde birleşir; tampon bölgesi ve eşdeğerlik pH'ı kendiliğinden çıkar.
     */
    fun calcPH(volML: Float): Float {
        val vA = config.analyteVolML / 1000.0
        val vT = volML / 1000.0
        val V = vA + vT
        if (V <= 0.0) return 7f
        var sCat = 0.0
        var sAn = 0.0
        val acids = mutableListOf<Pair<List<Double>, Double>>()
        val kbases = mutableListOf<Pair<Double, Double>>()
        for ((chem, c) in listOf(
            config.analyte to config.analyteConcM * vA / V,
            config.titrant to config.titrantConcM * vT / V
        )) {
            if (c <= 0.0) continue
            if (chem.isAcid) {
                if (chem.kaList.isEmpty()) sAn += chem.valence * c
                else acids.add(chem.kaList to c)
            } else {
                if (chem.isCarbonate) {
                    // Karbonat: katyon + karbonik asit sistemi (2 uç nokta kendiliğinden çıkar)
                    sCat += chem.valence * c
                    acids.add(listOf(4.3e-7, 5.6e-11) to c)
                } else if (chem.kb != null) {
                    kbases.add(chem.kb to c)
                } else {
                    sCat += chem.valence * c
                }
            }
        }
        var lo = -1.0
        var hi = 15.0
        repeat(100) {
            val mid = (lo + hi) / 2
            if (chargeBalance(mid, sCat, sAn, acids, kbases) > 0) lo = mid else hi = mid
        }
        return ((lo + hi) / 2).toFloat()
    }

    fun buildCompPts(): List<Pair<Float, Float>> = (0..50).map { it.toFloat() to calcPH(it.toFloat()) }

    private fun recalc() {
        eqVolumeML = calcEqVol()
        pH = calcPH(buretteML)
        val ind = config.indicator
        indProgress = when { pH < ind.phLow -> 0f; pH > ind.phHigh -> 1f; else -> (pH - ind.phLow) / (ind.phHigh - ind.phLow) }
        temperature = 25f + (buretteML / 50f) * 3f
        when {
            buretteML < eqVolumeML - 1f -> status = TitrationStatus.RUNNING
            abs(buretteML - eqVolumeML) < 0.3f -> status = TitrationStatus.AT_EQ
            abs(buretteML - eqVolumeML) <= 1f -> status = TitrationStatus.APPROACHING_EQ
            buretteML > eqVolumeML + 1f -> status = TitrationStatus.PAST_EQ
        }
        if (buretteML >= 50f) status = TitrationStatus.COMPLETED
        if (status == TitrationStatus.COMPLETED || status == TitrationStatus.PAST_EQ) errorPct = if (eqVolumeML == 0f) 0f else abs(buretteML - eqVolumeML) / eqVolumeML * 100f
    }

    fun snapshot() = TitrationSnapshot(buretteML, pH, status, indProgress, temperature, eqVolumeML, errorPct, speed, autoMode, config.analyte.formula, config.titrant.formula, config.indicator.label)

    fun reset() {
        buretteML = 0f; pH = calcPH(0f); status = TitrationStatus.READY; indProgress = 0f
        temperature = 25f; errorPct = 0f; valveOpen = false; speed = 0f; autoMode = false; autoPhase = 0; autoTargetHigh = true; graphPoints.clear()
    }

    companion object {
        val ANALYTES = listOf(
            Chemical("Hidroklorik Asit", "HCl", 1, true),
            Chemical("Sülfürik Asit", "H2SO4", 2, true, kaList = listOf(1e6, 0.012)),
            Chemical("Asetik Asit", "CH3COOH", 1, true, kaList = listOf(1.8e-5)),
            Chemical("Nitrik Asit", "HNO3", 1, true),
            Chemical("Fosforik Asit", "H3PO4", 3, true, kaList = listOf(7.5e-3, 6.2e-8, 4.8e-13)),
            Chemical("Okzalik Asit", "H2C2O4", 2, true, kaList = listOf(5.6e-2, 5.4e-5)),
            Chemical("Sitrik Asit", "C6H8O7", 3, true, kaList = listOf(7.4e-4, 1.7e-5, 4.0e-7)),
            Chemical("Kireç Taşı", "CaCO3", 2, false),
            Chemical("Soda Külü", "Na2CO3", 2, false)
        )
        val TITRANTS = listOf(
            Chemical("Sodyum Hidroksit", "NaOH", 1, false),
            Chemical("Potasyum Hidroksit", "KOH", 1, false),
            Chemical("Amonyak", "NH3", 1, false, kb = 1.8e-5),
            Chemical("Baryum Hidroksit", "Ba(OH)2", 2, false),
            Chemical("Hidroklorik Asit", "HCl", 1, true),
            Chemical("Sülfürik Asit", "H2SO4", 2, true, kaList = listOf(1e6, 0.012))
        )
    }
}