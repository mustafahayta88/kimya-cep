package com.kimya.uygulama.features

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

data class CellMetal(
    val symbol: String,
    val name: String,
    val potential: Float,
    val color: Int,
    val ionColor: Int,
    val ion: String,
    val n: Int,
    val molarMass: Float
)

enum class CellMode { GALVANIC, ELECTROLYSIS }

class CellEngine {
    var leftIdx = 0; private set
    var rightIdx = 3; private set
    var mode = CellMode.GALVANIC; private set
    var supplyV = 3f; private set
    var concL = 1f; private set
    var concR = 1f; private set
    var rLoad = 10f; private set
    var tempK = 298f; private set
    var depthL = 1f; private set
    var depthR = 1f; private set
    var stirL = 0f; private set
    var stirR = 0f; private set
    var paused = false; private set

    var wireL = true; private set
    var wireR = true; private set

    // Sarj durumu: 0 dolu -> 1 bitik
    var discharge = 0f; private set
    var current = 0f; private set
    var emfOC = 1.1f; private set
    var eLoad = 0f; private set
    var gasH2 = 0f; private set
    var gasO2 = 0f; private set
    // Faraday takibi
    var runTime = 0f; private set
    var chargeC = 0f; private set
    private var vSum = 0f
    private var vCount = 0
    val avgV: Float get() = if (vCount > 0) vSum / vCount else 0f
    // Anotta cozunen / katotta biriken kutle (gram).
    // Yük işaretlidir (galvanik +, elektroliz −): elektrolizde kütle ters yönde işler.
    fun dissolvedGrams(): Float {
        val q = if (mode == CellMode.GALVANIC) chargeC else -chargeC
        if (q <= 0f) return 0f
        val a = anode
        return q * a.molarMass / (a.n * FARADAY)
    }
    fun platedGrams(): Float {
        val q = if (mode == CellMode.GALVANIC) chargeC else -chargeC
        if (q <= 0f) return 0f
        val c = cathode
        return q * c.molarMass / (c.n * FARADAY)
    }

    var bannerMsg = ""; private set
    private var bannerT = 0f
    private var lowWarned = false
    fun bannerVisible(): Boolean = bannerT > 0f && bannerMsg.isNotEmpty()
    private fun showBanner(msg: String, secs: Float = 2.5f) { bannerMsg = msg; bannerT = secs }

    val anode: CellMetal get() = if (METALS[leftIdx].potential <= METALS[rightIdx].potential) METALS[leftIdx] else METALS[rightIdx]
    val cathode: CellMetal get() = if (METALS[leftIdx].potential <= METALS[rightIdx].potential) METALS[rightIdx] else METALS[leftIdx]
    val anodeLeft: Boolean get() = METALS[leftIdx].potential <= METALS[rightIdx].potential
    val sameMetal: Boolean get() = abs(METALS[rightIdx].potential - METALS[leftIdx].potential) < 0.01f
    val deadCell: Boolean get() = sameMetal && abs(concL - concR) < 0.05f

    fun setLeft(i: Int) { leftIdx = i.coerceIn(0, METALS.size - 1); onConfigChanged() }
    fun setRight(i: Int) { rightIdx = i.coerceIn(0, METALS.size - 1); onConfigChanged() }
    fun setMode(m: CellMode) {
        mode = m
        showBanner(if (m == CellMode.GALVANIC) "Galvanik pil modu" else "Elektroliz modu: guc kaynagi baglandi")
    }
    fun setSupplyV(v: Float) { supplyV = v.coerceIn(0.5f, 6f) }
    fun setConcL(c: Float) { concL = c.coerceIn(0.1f, 2f); freshIfIdle() }
    fun setConcR(c: Float) { concR = c.coerceIn(0.1f, 2f); freshIfIdle() }
    fun setLoad(r: Float) { rLoad = r.coerceIn(2f, 50f) }
    fun setTempC(t: Float) { tempK = (t + 273f).coerceIn(273f, 373f) }
    fun setDepthL(d: Float) { depthL = d.coerceIn(0.35f, 1f) }
    fun setDepthR(d: Float) { depthR = d.coerceIn(0.35f, 1f) }
    fun tapStir(side: Int) {
        if (side == 0) stirL = 3f else stirR = 3f
        showBanner("Karistirildi: polarizasyon dagildi, akim artti!")
    }
    fun togglePause() { paused = !paused; showBanner(if (paused) "Duraklatildi" else "Devam ediyor") }
    fun toggleWireL() { wireL = !wireL; showBanner(if (wireL) "Sol kablo baglandi" else "Sol kablo sokuldu!") }
    fun toggleWireR() { wireR = !wireR; showBanner(if (wireR) "Sag kablo baglandi" else "Sag kablo sokuldu!") }

    private fun freshIfIdle() {
        if (discharge < 0.02f) {
            discharge = 0f; current = 0f; eLoad = 0f
        }
    }

    private fun onConfigChanged() {
        if (deadCell) showBanner("Ayni metal + esit derisim: pil calismaz! Derisimleri farklilastir (derisim pili).")
    }

    fun reset() {
        lowWarned = false
        discharge = 0f; current = 0f; gasH2 = 0f; gasO2 = 0f
        runTime = 0f; chargeC = 0f; vSum = 0f; vCount = 0
        wireL = true; wireR = true
        depthL = 1f; depthR = 1f; stirL = 0f; stirR = 0f; paused = false
        bannerMsg = ""
        showBanner("Pil yenilendi (%100)")
    }

    fun circuitComplete(): Boolean = wireL && wireR

    private fun internalR(): Float = 3f / ((concL + concR) / 2f).coerceAtLeast(0.1f)

    fun tempSpeedF(): Float = kotlin.math.sqrt(tempK / 298f)

    // Nernst (derisim terimli): E = E0 - (k/n)·log([anot]^a/[katot]^c),
    // n = iki metalin elektron sayısının EKOK'u (örn. Zn|Ag hücresinde n=2)
    fun nernstE(): Float {
        val e0 = abs(METALS[rightIdx].potential - METALS[leftIdx].potential)
        val (cA, cC) = if (anodeLeft) Pair(concL, concR) else Pair(concR, concL)
        val oxA = (cA * (1f + discharge)).coerceAtLeast(1e-4f)
        val redC = (cC * (1f - discharge)).coerceAtLeast(1e-4f)
        val nA = anode.n
        val nC = cathode.n
        val n = nA * nC / gcdInt(nA, nC)
        val aCoef = n / nA
        val cCoef = n / nC
        val k = 2.303f * 8.314f * tempK / (n * FARADAY)
        val q = oxA.toDouble().pow(aCoef) / redC.toDouble().pow(cCoef)
        return (e0 - k * log10(q).toFloat()).coerceAtLeast(0f)
    }

    private fun gcdInt(a: Int, b: Int): Int = if (b == 0) a else gcdInt(b, a % b)

    fun tick(dt: Float) {
        if (stirL > 0f) stirL -= dt
        if (stirR > 0f) stirR -= dt
        if (paused) return
        emfOC = nernstE()
        val rInt = internalR()
        val areaAvg = ((depthL + depthR) / 2f).coerceIn(0.35f, 1f)
        val stirBoost = if (stirL > 0f || stirR > 0f) 1.35f else 1f
        if (mode == CellMode.GALVANIC) {
            if (circuitComplete() && discharge < 1f) {
                current = emfOC / (rLoad + rInt) * areaAvg * stirBoost
                eLoad = current * rLoad
                discharge = (discharge + current * dt / 5.5f).coerceAtMost(1f)
                if (discharge >= 1f) {
                    current = 0f; eLoad = 0f
                    showBanner("Pil bitti! SIFIRLA ile yenile.")
                }
            } else {
                current = 0f
                eLoad = emfOC
            }
        } else {
            // Elektroliz: dis guc kaynagi
            if (circuitComplete()) {
                val iRev = ((supplyV - emfOC) / (rLoad + rInt)).coerceAtLeast(0f) * areaAvg
                if (supplyV <= emfOC + 1e-6f && !lowWarned) {
                    lowWarned = true
                    showBanner("Güç yetersiz: besleme gerilimi EMF değerini aşmalı!")
                }
                if (supplyV > emfOC + 1e-6f) lowWarned = false
                current = -iRev
                eLoad = supplyV
                discharge = (discharge - iRev * dt / 9f).coerceAtLeast(0f)
                gasH2 += iRev * dt * 1.1f
                gasO2 += iRev * dt * 0.55f
            } else {
                current = 0f
                eLoad = 0f
            }
        }
        // Faraday takibi (akim akan her an; işaretli: galvanik +, elektroliz −)
        if (abs(current) > 0.0005f) {
            val q = current * dt
            chargeC += q
            runTime += dt
            if (mode == CellMode.GALVANIC && circuitComplete()) {
                vSum += eLoad
                vCount++
            }
        }
        if (bannerT > 0f) bannerT -= dt
    }

    fun bulbPower(): Float {
        if (mode != CellMode.GALVANIC || !circuitComplete()) return 0f
        return ((eLoad * eLoad / rLoad) / 0.15f).coerceIn(0f, 1f)
    }

    fun anodeConcFrac(): Float = (1f + discharge).coerceIn(0f, 2f) / 2f
    fun cathodeConcFrac(): Float = (1f - discharge).coerceIn(0f, 1f)

    companion object {
        val METALS = listOf(
            CellMetal("Zn", "Cinko", -0.76f, Color.rgb(139, 139, 139), Color.rgb(102, 136, 204), "Zn2+", 2, 65.4f),
            CellMetal("Fe", "Demir", -0.44f, Color.rgb(160, 82, 45), Color.rgb(170, 187, 68), "Fe2+", 2, 55.8f),
            CellMetal("Ni", "Nikel", -0.26f, Color.rgb(176, 196, 222), Color.rgb(68, 204, 136), "Ni2+", 2, 58.7f),
            CellMetal("Cu", "Bakır", 0.34f, Color.rgb(205, 127, 50), Color.rgb(68, 136, 255), "Cu2+", 2, 63.5f),
            CellMetal("Ag", "Gumus", 0.80f, Color.rgb(192, 192, 192), Color.rgb(170, 170, 170), "Ag+", 1, 107.9f)
        )
        const val FARADAY = 96485f
    }
}
