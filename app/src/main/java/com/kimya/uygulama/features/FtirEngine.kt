package com.kimya.uygulama.features

import android.graphics.Color
import kotlin.math.*
import kotlin.random.Random

data class FtirCompound(
    val name: String,
    val formula: String,
    val category: String,
    val groups: List<String>,
    val structure: MoleculeStructure?,
    val mw: String = "",
    val desc: String = ""
)

data class Atom(val symbol: String, val x: Float, val y: Float, val color: Int)
data class Bond(val from: Int, val to: Int, val order: Int = 1)
data class MoleculeStructure(val atoms: List<Atom>, val bonds: List<Bond>)

class FtirEngine {

    var selectedCompound: FtirCompound? = null
        private set

    var selectedGroups = mutableSetOf<String>()
        private set

    private var _resolution = 4
    val resolution: Int get() = _resolution
    private var _scanCount = 16
    val scanCount: Int get() = _scanCount

    var isScanning = false
        private set
    var scanProgress = 0f
        private set
    var hasResult = false
        private set

    var showInterferogram = false
        private set

    var snr = 0f
        private set

    // Spektrum verileri
    val wavenumbers = FloatArray(4000)
    val transmittance = FloatArray(4000)
    val absorbance = FloatArray(4000)
    val interferogram = FloatArray(256)

    // Animasyon için
    var mirrorPosition = 0f
    private var mirrorVelocity = 0f
    private var scanPhase = 0f

    // Banner
    var bannerMsg = ""
        private set
    private var bannerT = 0f

    companion object {
        val C_C = Color.rgb(77, 208, 225)
        val C_O = Color.rgb(255, 80, 80)
        val C_N = Color.rgb(80, 160, 255)
        val C_H = Color.rgb(180, 190, 200)
        val C_S = Color.rgb(255, 220, 50)
        val C_CL = Color.rgb(100, 220, 100)

        private fun ring6(cx: Float, cy: Float, r: Float, withH: Boolean = true): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat(), C_C))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            if (withH) {
                for (i in 0 until 6) {
                    val ang = Math.toRadians(60.0 * i - 90.0)
                    a.add(Atom("H", cx + (cos(ang) * (r + 34)).toFloat(), cy + (sin(ang) * (r + 34)).toFloat(), C_H))
                    b.add(Bond(i, a.size - 1))
                }
            }
            return MoleculeStructure(a, b)
        }

        fun eth() = MoleculeStructure(
            listOf(Atom("C", -30f, 0f, C_C), Atom("C", 30f, 0f, C_C), Atom("O", 90f, 0f, C_O), Atom("H", 125f, -25f, C_H), Atom("H", -30f, -35f, C_H), Atom("H", -65f, 18f, C_H), Atom("H", -30f, 35f, C_H), Atom("H", 30f, -35f, C_H), Atom("H", 30f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(0, 4), Bond(0, 5), Bond(0, 6), Bond(1, 7), Bond(1, 8))
        )
        fun met() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("O", 50f, 0f, C_O), Atom("H", 85f, -22f, C_H), Atom("H", 0f, -36f, C_H), Atom("H", -32f, 18f, C_H), Atom("H", 0f, 36f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(0, 3), Bond(0, 4), Bond(0, 5))
        )
        fun ace() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("C", -45f, 28f, C_C), Atom("C", 45f, 28f, C_C), Atom("O", 0f, -45f, C_O)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3, 2))
        )
        fun benz() = ring6(0f, 0f, 38f)
        fun aniline() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("N", 0f, -72f, C_N)); b.add(Bond(0, a.size - 1))
            MoleculeStructure(a, b)
        }
        fun aceAcid() = MoleculeStructure(
            listOf(Atom("C", -28f, 0f, C_C), Atom("C", 28f, 0f, C_C), Atom("O", 28f, -45f, C_O), Atom("O", 82f, 18f, C_O), Atom("H", 115f, 0f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4))
        )
        fun hexMol() = MoleculeStructure(
            listOf(Atom("C", -75f, 0f, C_C), Atom("C", -30f, 0f, C_C), Atom("C", 15f, 0f, C_C), Atom("C", 60f, 0f, C_C), Atom("C", 105f, 0f, C_C), Atom("C", 150f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(4, 5))
        )
        fun aceAmide() = MoleculeStructure(
            listOf(Atom("C", -25f, 0f, C_C), Atom("C", 25f, 0f, C_C), Atom("O", 25f, -42f, C_O), Atom("N", 68f, 20f, C_N), Atom("H", 100f, 5f, C_H), Atom("H", 68f, 55f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(3, 5))
        )
        fun nitrobz() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("N", 0f, -72f, C_N)); b.add(Bond(0, a.size - 1))
            a.add(Atom("O", -25f, -100f, C_O)); b.add(Bond(a.size - 2, a.size - 1))
            a.add(Atom("O", 25f, -100f, C_O)); b.add(Bond(a.size - 3, a.size - 1))
            MoleculeStructure(a, b)
        }
        fun diethylEther() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", -10f, 0f, C_C), Atom("O", 30f, 0f, C_O), Atom("C", 70f, 0f, C_C), Atom("C", 110f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4))
        )
        fun dmso() = MoleculeStructure(
            listOf(Atom("C", -40f, 0f, C_C), Atom("S", 0f, 0f, C_S), Atom("O", 0f, -45f, C_O), Atom("C", 40f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3))
        )
        fun ethylAcetate() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", 0f, 0f, C_C), Atom("O", 0f, -40f, C_O), Atom("O", 50f, 15f, C_O), Atom("C", 90f, 0f, C_C), Atom("C", 130f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(4, 5))
        )

        fun isoPrOH() = MoleculeStructure(
            listOf(Atom("C", -40f, 0f, C_C), Atom("C", 20f, 0f, C_C), Atom("O", 80f, 0f, C_O), Atom("H", 115f, -22f, C_H), Atom("C", -40f, -50f, C_C), Atom("H", 20f, -40f, C_H), Atom("H", -75f, -70f, C_H), Atom("H", -40f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(0, 4), Bond(1, 5), Bond(4, 6), Bond(0, 7))
        )

        fun glycerol() = MoleculeStructure(
            listOf(Atom("C", -40f, 0f, C_C), Atom("C", 20f, 0f, C_C), Atom("C", 80f, 0f, C_C), Atom("O", -85f, 0f, C_O), Atom("H", -115f, -18f, C_H), Atom("O", 20f, -50f, C_O), Atom("H", 20f, -85f, C_H), Atom("O", 125f, 0f, C_O), Atom("H", 155f, -18f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(0, 3), Bond(3, 4), Bond(1, 5), Bond(5, 6), Bond(2, 7), Bond(7, 8))
        )

        fun phenol() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("O", 0f, -72f, C_O)); b.add(Bond(0, a.size - 1))
            a.add(Atom("H", 0f, -105f, C_H)); b.add(Bond(a.size - 2, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun benzaldehyde() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("H", 72f, 35f, C_H)); b.add(Bond(ci, ci + 2))
            MoleculeStructure(a, b)
        }

        fun acetaldehyde() = MoleculeStructure(
            listOf(Atom("C", -20f, 0f, C_C), Atom("C", 30f, 0f, C_C), Atom("O", 65f, -25f, C_O), Atom("H", 30f, 35f, C_H), Atom("H", -20f, -35f, C_H), Atom("H", -55f, 18f, C_H), Atom("H", -20f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(0, 4), Bond(0, 5), Bond(0, 6))
        )

        fun benzoicAcid() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 108f, 25f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("H", 140f, 25f, C_H)); b.add(Bond(ci + 2, ci + 3))
            MoleculeStructure(a, b)
        }

        fun tol() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("C", 0f, -72f, C_C)); b.add(Bond(0, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun hexene() = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_C), Atom("C", -15f, 0f, C_C), Atom("C", 30f, 0f, C_C), Atom("C", 75f, 0f, C_C), Atom("C", 120f, 0f, C_C), Atom("C", 165f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(2, 3), Bond(3, 4), Bond(4, 5))
        )

        fun diethylamine() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", -10f, 0f, C_C), Atom("N", 30f, 0f, C_N), Atom("H", 30f, -35f, C_H), Atom("C", 70f, 0f, C_C), Atom("C", 110f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(2, 4), Bond(4, 5))
        )

        fun acetonitrile() = MoleculeStructure(
            listOf(Atom("C", -25f, 0f, C_C), Atom("C", 25f, 0f, C_C), Atom("N", 68f, 0f, C_N), Atom("H", -25f, -35f, C_H), Atom("H", -60f, 18f, C_H), Atom("H", -25f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 3), Bond(0, 3), Bond(0, 4), Bond(0, 5))
        )

        fun chloroform() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("Cl", -40f, -35f, C_CL), Atom("Cl", 40f, -35f, C_CL), Atom("Cl", 0f, 40f, C_CL), Atom("H", 0f, -60f, C_H)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3), Bond(0, 4))
        )

        fun dichloromethane() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("Cl", -40f, -20f, C_CL), Atom("Cl", 40f, -20f, C_CL), Atom("H", -25f, 30f, C_H), Atom("H", 25f, 30f, C_H)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3), Bond(0, 4))
        )

        fun ccl4() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("Cl", -45f, -30f, C_CL), Atom("Cl", 45f, -30f, C_CL), Atom("Cl", -45f, 30f, C_CL), Atom("Cl", 45f, 30f, C_CL)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3), Bond(0, 4))
        )

        fun pyridine(): MoleculeStructure {
            val m = ring6(0f, 0f, 38f, false)
            val a = m.atoms.toMutableList()
            a[0] = Atom("N", a[0].x, a[0].y, C_N)
            return MoleculeStructure(a, m.bonds)
        }

        fun cyclopentanone() = MoleculeStructure(
            listOf(Atom("C", -30f, -15f, C_C), Atom("C", 0f, -35f, C_C), Atom("C", 30f, -15f, C_C), Atom("C", 20f, 15f, C_C), Atom("C", -20f, 15f, C_C), Atom("O", 0f, -68f, C_O)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(4, 0), Bond(1, 5, 2))
        )

        fun styrene() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, a.size - 1))
            a.add(Atom("C", 108f, 25f, C_C)); b.add(Bond(a.size - 2, a.size - 1, 2))
            MoleculeStructure(a, b)
        }

        fun butOH() = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_C), Atom("C", -15f, 0f, C_C), Atom("C", 30f, 0f, C_C), Atom("C", 75f, 0f, C_C), Atom("O", 120f, 0f, C_O), Atom("H", 155f, -22f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(4, 5))
        )

        fun rings(cx: Float = 0f, cy: Float = 0f, r: Float = 38f): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat(), C_C))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, 1))
            return MoleculeStructure(a, b)
        }

        fun cyclohexanol() = rings().let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("O", 0f, -72f, C_O)); b.add(Bond(0, a.size - 1))
            a.add(Atom("H", 0f, -105f, C_H)); b.add(Bond(a.size - 2, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun kresol() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("O", 0f, -72f, C_O)); b.add(Bond(0, a.size - 1))
            a.add(Atom("C", 66f, -30f, C_C)); b.add(Bond(1, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun mek() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", 0f, 0f, C_C), Atom("O", 0f, -42f, C_O), Atom("C", 50f, 0f, C_C), Atom("C", 95f, 0f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4))
        )

        fun acetophenone() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("C", 108f, 30f, C_C)); b.add(Bond(ci, ci + 2))
            MoleculeStructure(a, b)
        }

        fun cyclohexanone() = rings().let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("O", 62f, -36f, C_O)); b.add(Bond(1, a.size - 1, 2))
            MoleculeStructure(a, b)
        }

        fun formaldehyde() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("O", 0f, -40f, C_O), Atom("H", -35f, 20f, C_H), Atom("H", 35f, 20f, C_H)),
            listOf(Bond(0, 1, 2), Bond(0, 2), Bond(0, 3))
        )

        fun formicAcid() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("O", 0f, -40f, C_O), Atom("O", 50f, 15f, C_O), Atom("H", 85f, 5f, C_H), Atom("H", -35f, 20f, C_H)),
            listOf(Bond(0, 1, 2), Bond(0, 2), Bond(2, 3), Bond(0, 4))
        )

        fun propanoicAcid() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", 0f, 0f, C_C), Atom("C", 50f, 0f, C_C), Atom("O", 50f, -45f, C_O), Atom("O", 95f, 20f, C_O), Atom("H", 128f, 2f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3, 2), Bond(2, 4), Bond(4, 5))
        )

        fun methylBenzoate() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 108f, 25f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("C", 148f, 25f, C_C)); b.add(Bond(ci + 2, ci + 3))
            MoleculeStructure(a, b)
        }

        fun butylAcetate() = MoleculeStructure(
            listOf(Atom("C", -80f, 0f, C_C), Atom("C", -30f, 0f, C_C), Atom("O", -30f, -42f, C_O), Atom("O", 20f, -30f, C_O), Atom("C", 60f, -15f, C_C), Atom("C", 105f, -15f, C_C), Atom("C", 150f, -15f, C_C), Atom("C", 195f, -15f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(4, 5), Bond(5, 6), Bond(6, 7))
        )

        fun thf(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 5) {
                val ang = Math.toRadians(72.0 * i - 90.0)
                if (i == 0) a.add(Atom("O", (cos(ang) * 38f).toFloat(), (sin(ang) * 38f).toFloat(), C_O))
                else a.add(Atom("C", (cos(ang) * 38f).toFloat(), (sin(ang) * 38f).toFloat(), C_C))
            }
            for (i in 0 until 5) b.add(Bond(i, (i + 1) % 5, 1))
            return MoleculeStructure(a, b)
        }

        fun nmethylaniline() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("N", 0f, -72f, C_N)); b.add(Bond(0, a.size - 1))
            a.add(Atom("C", 0f, -110f, C_C)); b.add(Bond(a.size - 2, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun piperidine(): MoleculeStructure {
            val m = rings()
            val a = m.atoms.toMutableList()
            a[0] = Atom("N", a[0].x, a[0].y, C_N)
            return MoleculeStructure(a, m.bonds)
        }

        fun morpholine(): MoleculeStructure {
            val m = rings()
            val a = m.atoms.toMutableList()
            a[0] = Atom("N", a[0].x, a[0].y, C_N)
            a[3] = Atom("O", a[3].x, a[3].y, C_O)
            return MoleculeStructure(a, m.bonds)
        }

        fun dmf() = MoleculeStructure(
            listOf(Atom("C", -25f, 0f, C_C), Atom("C", 25f, 0f, C_C), Atom("O", 25f, -42f, C_O), Atom("N", 68f, 20f, C_N), Atom("C", 100f, 0f, C_C), Atom("C", 68f, 55f, C_C), Atom("H", -25f, -35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(3, 5), Bond(0, 6))
        )

        fun urea() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("O", 0f, -42f, C_O), Atom("N", -45f, 25f, C_N), Atom("N", 45f, 25f, C_N), Atom("H", -70f, 10f, C_H), Atom("H", -45f, 55f, C_H), Atom("H", 70f, 10f, C_H), Atom("H", 45f, 55f, C_H)),
            listOf(Bond(0, 1, 2), Bond(0, 2), Bond(0, 3), Bond(2, 4), Bond(2, 5), Bond(3, 6), Bond(3, 7))
        )

        fun benzonitrile() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, a.size - 1))
            a.add(Atom("N", 115f, 0f, C_N)); b.add(Bond(a.size - 2, a.size - 1, 3))
            MoleculeStructure(a, b)
        }

        fun acetylene() = MoleculeStructure(
            listOf(Atom("C", -25f, 0f, C_C), Atom("C", 25f, 0f, C_C), Atom("H", -60f, 0f, C_H), Atom("H", 60f, 0f, C_H)),
            listOf(Bond(0, 1, 3), Bond(0, 2), Bond(1, 3))
        )

        fun propargylAlcohol() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", -10f, 0f, C_C), Atom("C", 35f, 0f, C_C), Atom("O", 80f, 0f, C_O), Atom("H", 115f, -20f, C_H), Atom("H", -85f, 0f, C_H)),
            listOf(Bond(0, 1, 3), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(0, 5))
        )

        fun chlorobenzene() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("Cl", 0f, -75f, C_CL)); b.add(Bond(0, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun chlorobutane() = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_C), Atom("C", -15f, 0f, C_C), Atom("C", 30f, 0f, C_C), Atom("C", 75f, 0f, C_C), Atom("Cl", 120f, 0f, C_CL)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4))
        )

        fun cyclohexene(): MoleculeStructure {
            val m = rings()
            val b = m.bonds.toMutableList()
            b[0] = Bond(0, 1, 2)
            return MoleculeStructure(m.atoms, b)
        }

        fun oxylene() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("C", 0f, -72f, C_C)); b.add(Bond(0, a.size - 1))
            a.add(Atom("C", 66f, -30f, C_C)); b.add(Bond(1, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun benzylAlcohol() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 112f, 20f, C_O)); b.add(Bond(ci, ci + 1))
            a.add(Atom("H", 145f, 10f, C_H)); b.add(Bond(ci + 1, ci + 2))
            MoleculeStructure(a, b)
        }

        fun allylAlcohol() = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_C), Atom("C", -15f, 0f, C_C), Atom("C", 30f, 0f, C_C), Atom("O", 75f, 0f, C_O), Atom("H", 110f, -20f, C_H)),
            listOf(Bond(0, 1, 2), Bond(1, 2), Bond(2, 3), Bond(3, 4))
        )

        fun mibk() = MoleculeStructure(
            listOf(Atom("C", -70f, 0f, C_C), Atom("C", -20f, 0f, C_C), Atom("O", -20f, -42f, C_O), Atom("C", 30f, 0f, C_C), Atom("C", 80f, 0f, C_C), Atom("C", 80f, -50f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(3, 5))
        )

        fun benzophenone() = ring6(-55f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 17f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 53f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("C", 53f, 30f, C_C)); b.add(Bond(ci, ci + 2))
            MoleculeStructure(a, b)
        }

        fun propanal() = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_C), Atom("C", -10f, 0f, C_C), Atom("C", 40f, 0f, C_C), Atom("O", 75f, -25f, C_O), Atom("H", 40f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3, 2), Bond(2, 4))
        )

        fun cinnamaldehyde() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("C", 112f, 25f, C_C)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("C", 152f, 25f, C_C)); b.add(Bond(ci + 1, ci + 2))
            a.add(Atom("O", 187f, 5f, C_O)); b.add(Bond(ci + 2, ci + 3, 2))
            MoleculeStructure(a, b)
        }

        fun hexanoicAcid() = MoleculeStructure(
            listOf(Atom("C", -90f, 0f, C_C), Atom("C", -45f, 0f, C_C), Atom("C", 0f, 0f, C_C), Atom("C", 45f, 0f, C_C), Atom("C", 90f, 0f, C_C), Atom("O", 90f, -45f, C_O), Atom("O", 135f, 20f, C_O), Atom("H", 168f, 5f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(4, 5, 2), Bond(4, 6), Bond(6, 7))
        )

        fun salicylicAcid() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 108f, 25f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("H", 140f, 25f, C_H)); b.add(Bond(ci + 2, ci + 3))
            a.add(Atom("O", 62f, -42f, C_O)); b.add(Bond(1, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun ethylBenzoate() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 108f, 25f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("C", 148f, 25f, C_C)); b.add(Bond(ci + 2, ci + 3))
            a.add(Atom("C", 188f, 25f, C_C)); b.add(Bond(ci + 3, ci + 4))
            MoleculeStructure(a, b)
        }

        fun methylAcetate() = MoleculeStructure(
            listOf(Atom("C", -40f, 0f, C_C), Atom("C", 10f, 0f, C_C), Atom("O", 10f, -42f, C_O), Atom("O", 55f, 15f, C_O), Atom("C", 95f, 10f, C_C)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4))
        )

        fun dioxane(): MoleculeStructure {
            val m = rings()
            val a = m.atoms.toMutableList()
            a[0] = Atom("O", a[0].x, a[0].y, C_O)
            a[3] = Atom("O", a[3].x, a[3].y, C_O)
            return MoleculeStructure(a, m.bonds)
        }

        fun cyclohexylamine() = rings().let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("N", 62f, -36f, C_N)); b.add(Bond(1, a.size - 1))
            a.add(Atom("H", 95f, -50f, C_H)); b.add(Bond(a.size - 2, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun benzylamine() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            val ci = a.size
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, ci))
            a.add(Atom("N", 112f, 15f, C_N)); b.add(Bond(ci, ci + 1))
            a.add(Atom("H", 145f, 5f, C_H)); b.add(Bond(ci + 1, ci + 2))
            MoleculeStructure(a, b)
        }

        fun acetanilide() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("N", 0f, -72f, C_N)); b.add(Bond(0, a.size - 1))
            val ci = a.size
            a.add(Atom("C", 0f, -112f, C_C)); b.add(Bond(ci - 1, ci))
            a.add(Atom("O", -32f, -140f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("C", 36f, -136f, C_C)); b.add(Bond(ci, ci + 2))
            MoleculeStructure(a, b)
        }

        fun formamide() = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_C), Atom("O", 0f, -42f, C_O), Atom("N", 50f, 15f, C_N), Atom("H", -30f, 20f, C_H)),
            listOf(Bond(0, 1, 2), Bond(0, 2), Bond(0, 3))
        )

        fun acrylonitrile() = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_C), Atom("C", -5f, 0f, C_C), Atom("C", 40f, 0f, C_C), Atom("N", 83f, 0f, C_N)),
            listOf(Bond(0, 1, 2), Bond(1, 2), Bond(2, 3, 3))
        )

        fun dichloroethane() = MoleculeStructure(
            listOf(Atom("Cl", -70f, 0f, C_CL), Atom("C", -25f, 0f, C_C), Atom("C", 25f, 0f, C_C), Atom("Cl", 70f, 0f, C_CL)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3))
        )

        fun cyclopentane(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 5) {
                val ang = Math.toRadians(72.0 * i - 90.0)
                a.add(Atom("C", (cos(ang) * 38f).toFloat(), (sin(ang) * 38f).toFloat(), C_C))
            }
            for (i in 0 until 5) b.add(Bond(i, (i + 1) % 5, 1))
            return MoleculeStructure(a, b)
        }

        fun hexyne() = MoleculeStructure(
            listOf(Atom("C", -70f, 0f, C_C), Atom("C", -30f, 0f, C_C), Atom("C", 15f, 0f, C_C), Atom("C", 60f, 0f, C_C), Atom("C", 105f, 0f, C_C), Atom("C", 150f, 0f, C_C), Atom("H", -105f, 0f, C_H)),
            listOf(Bond(0, 1, 3), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(4, 5), Bond(0, 6))
        )

        fun ethylbenzene() = ring6(0f, 0f, 38f, false).let { m ->
            val a = m.atoms.toMutableList(); val b = m.bonds.toMutableList()
            a.add(Atom("C", 72f, 0f, C_C)); b.add(Bond(0, a.size - 1))
            a.add(Atom("C", 115f, 10f, C_C)); b.add(Bond(a.size - 2, a.size - 1))
            MoleculeStructure(a, b)
        }

        fun chloroaceticAcid() = MoleculeStructure(
            listOf(Atom("Cl", -70f, 0f, C_CL), Atom("C", -25f, 0f, C_C), Atom("C", 25f, 0f, C_C), Atom("O", 25f, -45f, C_O), Atom("O", 70f, 20f, C_O), Atom("H", 103f, 5f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3, 2), Bond(2, 4), Bond(4, 5))
        )

        val FUNCTIONAL_GROUPS = listOf(
            FunctionalGroup("oh_alcohol", "O-H Alkol", "Hidroksil", 3200f, 3600f, 3350f, 0.85f, 180f, Color.rgb(255, 80, 80), "Geniş"),
            FunctionalGroup("oh_acid", "O-H Asit", "Asit O-H", 2500f, 3300f, 3000f, 0.92f, 400f, Color.rgb(255, 60, 60), "Çok Geniş"),
            FunctionalGroup("nh_primary", "N-H 1° Amin", "1° Amin", 3250f, 3500f, 3400f, 0.55f, 80f, Color.rgb(80, 180, 255), "Çift Pik"),
            FunctionalGroup("nh_secondary", "N-H 2° Amin", "2° Amin", 3310f, 3350f, 3330f, 0.4f, 50f, Color.rgb(100, 200, 255), "Tek Pik"),
            FunctionalGroup("nh_amide", "N-H Amid", "Amid N-H", 3180f, 3350f, 3280f, 0.5f, 100f, Color.rgb(120, 160, 255), "Geniş"),
            FunctionalGroup("ch_alkane", "C-H Alkan", "Alkan C-H", 2845f, 2970f, 2920f, 0.7f, 50f, Color.rgb(77, 208, 225), "Keskin"),
            FunctionalGroup("ch_alkene", "C-H Alken", "=C-H", 3020f, 3100f, 3080f, 0.45f, 40f, Color.rgb(100, 220, 100), "Orta"),
            FunctionalGroup("ch_aro", "C-H Aromatik", "Ar C-H", 3000f, 3100f, 3050f, 0.4f, 35f, Color.rgb(180, 140, 255), "Zayıf"),
            FunctionalGroup("ch_aldehyde", "C-H Aldehit", "Aldehit C-H", 2720f, 2830f, 2780f, 0.35f, 40f, Color.rgb(200, 180, 100), "Çift Pik"),
            FunctionalGroup("ch_alkyne", "≡C-H Alkin", "Alkin ≡C-H", 3260f, 3330f, 3300f, 0.7f, 30f, Color.rgb(255, 220, 80), "Keskin"),
            FunctionalGroup("co_ketone", "C=O Keton", "Keton C=O", 1705f, 1725f, 1715f, 0.95f, 35f, Color.rgb(255, 200, 50), "Keskin"),
            FunctionalGroup("co_aldehyde", "C=O Aldehit", "Aldehit C=O", 1720f, 1740f, 1730f, 0.9f, 30f, Color.rgb(255, 180, 80), "Keskin"),
            FunctionalGroup("co_ester", "C=O Ester", "Ester C=O", 1735f, 1750f, 1740f, 0.88f, 30f, Color.rgb(255, 160, 100), "Keskin"),
            FunctionalGroup("co_acid", "C=O Asit", "Asit C=O", 1700f, 1725f, 1710f, 0.92f, 35f, Color.rgb(255, 140, 70), "Keskin"),
            FunctionalGroup("co_amide1", "C=O Amid I", "Amid I", 1630f, 1690f, 1660f, 0.85f, 40f, Color.rgb(220, 180, 255), "Keskin"),
            FunctionalGroup("co_conj", "C=O Konjuge", "Konjuge C=O", 1675f, 1710f, 1690f, 0.93f, 35f, Color.rgb(255, 185, 60), "Keskin"),
            FunctionalGroup("co_ring", "C=O Halka", "Halka C=O", 1735f, 1765f, 1750f, 0.95f, 35f, Color.rgb(255, 215, 90), "Keskin"),
            FunctionalGroup("cc_alkene", "C=C Alken", "Alken C=C", 1620f, 1680f, 1650f, 0.35f, 35f, Color.rgb(80, 255, 180), "Zayıf"),
            FunctionalGroup("cc_aro", "C=C Aromatik", "Ar C=C", 1450f, 1615f, 1500f, 0.45f, 60f, Color.rgb(160, 140, 220), "Orta"),
            FunctionalGroup("cc_alkyne", "C≡C Alkin", "Alkin C≡C", 2100f, 2260f, 2120f, 0.3f, 30f, Color.rgb(255, 255, 120), "Zayıf"),
            FunctionalGroup("cn_nitrile", "C≡N Nitril", "Nitril C≡N", 2210f, 2260f, 2250f, 0.5f, 25f, Color.rgb(150, 255, 150), "Orta"),
            FunctionalGroup("no2", "NO₂ Nitro", "Nitro", 1515f, 1570f, 1540f, 0.8f, 35f, Color.rgb(255, 80, 180), "Güçlü"),
            FunctionalGroup("co_alcohol", "C-O Alkol", "C-O", 1040f, 1175f, 1100f, 0.65f, 80f, Color.rgb(255, 150, 100), "Güçlü"),
            FunctionalGroup("co_ester_coc", "C-O-C Ester", "Ester C-O-C", 1150f, 1300f, 1240f, 0.75f, 70f, Color.rgb(255, 130, 80), "Güçlü"),
            FunctionalGroup("nh_bend", "N-H Bükülme", "Amid II", 1510f, 1570f, 1540f, 0.6f, 35f, Color.rgb(140, 180, 255), "Orta"),
            FunctionalGroup("c_cl", "C-Cl Klor", "Kloro", 550f, 850f, 700f, 0.7f, 100f, Color.rgb(180, 220, 180), "Güçlü")
        )

        val COOKBOOK_COMPOUNDS = listOf(
            FtirCompound("Etanol", "C₂H₅OH", "Alkol", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), eth(), "46.07", "O-H ~3350'de geniş bant, C-H ~2920, C-O ~1100. Birincil alkolün tipik izi."),
            FtirCompound("Metanol", "CH₃OH", "Alkol", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), met(), "32.04", "O-H ~3350 (geniş), C-H ~2920, C-O ~1050. En basit alkol."),
            FtirCompound("İzopropanol", "(CH₃)₂CHOH", "Alkol", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), isoPrOH(), "60.10", "O-H ~3350, C-H ~2920, C-O ~1150. İkincil alkol."),
            FtirCompound("Gliserol", "C₃H₈O₃", "Alkol", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), glycerol(), "92.09", "3 OH grubu: çok geniş O-H bandı, güçlü C-O 1000-1150."),
            FtirCompound("Fenol", "C₆H₅OH", "Aromatik", listOf("oh_alcohol", "ch_aro", "cc_aro", "co_alcohol"), phenol(), "94.11", "O-H ~3350 + aromatik C-H ~3050, C=C ~1500. Fenolik OH."),
            FtirCompound("Aseton", "CH₃COCH₃", "Keton", listOf("co_ketone", "ch_alkane"), ace(), "58.08", "C=O ~1715 (çok güçlü, keskin), C-H ~2920. Keton referansı."),
            FtirCompound("Siklopentanon", "C₅H₈O", "Keton", listOf("co_ring", "ch_alkane"), cyclopentanone(), "84.12", "C=O ~1750 (5-halka gerilimi yukarı kaydırır)."),
            FtirCompound("Benzaldehit", "C₆H₅CHO", "Aldehit", listOf("co_conj", "ch_aldehyde", "ch_aro", "cc_aro"), benzaldehyde(), "106.12", "C=O ~1705 (konjuge aldehit), Fermi ikilisi ~2720/2820."),
            FtirCompound("Asetaldehit", "CH₃CHO", "Aldehit", listOf("co_aldehyde", "ch_aldehyde", "ch_alkane"), acetaldehyde(), "44.05", "C=O ~1730, Fermi ikilisi ~2720/2820, C-H ~2920."),
            FtirCompound("Asetik Asit", "CH₃COOH", "Asit", listOf("oh_acid", "co_acid", "co_alcohol"), aceAcid(), "60.05", "2500-3300 arası çok geniş O-H, C=O ~1710, C-O ~1240."),
            FtirCompound("Benzoik Asit", "C₆H₅COOH", "Asit", listOf("oh_acid", "co_acid", "ch_aro", "cc_aro"), benzoicAcid(), "122.12", "Geniş asit O-H, C=O ~1690 (konjugasyon düşürür), C=C ~1500."),
            FtirCompound("Etil Asetat", "CH₃COOC₂H₅", "Ester", listOf("co_ester", "co_ester_coc", "ch_alkane"), ethylAcetate(), "88.11", "C=O ~1740, C-O-C ~1240 (güçlü). Ester ikilisi belirleyicidir."),
            FtirCompound("Benzen", "C₆H₆", "Aromatik", listOf("ch_aro", "cc_aro"), benz(), "78.11", "Aromatik C-H ~3050 (zayıf), C=C ~1500/1600. Aromatik iskelet."),
            FtirCompound("Toluen", "C₆H₅CH₃", "Aromatik", listOf("ch_aro", "cc_aro", "ch_alkane"), tol(), "92.14", "Aromatik ~3050 + alifatik C-H ~2920 bir arada, C=C ~1500."),
            FtirCompound("Piridin", "C₅H₅N", "Aromatik", listOf("ch_aro", "cc_aro"), pyridine(), "79.10", "Aromatik C-H, C=C/C=N ~1500-1600. Azotlu heteroaromatik."),
            FtirCompound("Stiren", "C₆H₅CH=CH₂", "Alken", listOf("ch_aro", "cc_aro", "ch_alkene", "cc_alkene"), styrene(), "104.15", "Aromatik + vinil: =C-H ~3080, C=C ~1630."),
            FtirCompound("1-Heksen", "C₆H₁₂", "Alken", listOf("ch_alkane", "ch_alkene", "cc_alkene"), hexene(), "84.16", "=C-H ~3080, C=C ~1650 (zayıf-orta), C-H ~2920."),
            FtirCompound("Hekzan", "C₆H₁₄", "Alkan", listOf("ch_alkane"), hexMol(), "86.18", "Sadece C-H ~2920/2850. Fonksiyonsuz referans spektrum."),
            FtirCompound("Anilin", "C₆H₅NH₂", "Amin", listOf("nh_primary", "ch_aro", "cc_aro"), aniline(), "93.13", "N-H ikilisi ~3400 (birincil amin), aromatik ~3050."),
            FtirCompound("Dietilamin", "(C₂H₅)₂NH", "Amin", listOf("nh_secondary", "ch_alkane"), diethylamine(), "73.14", "Tek N-H ~3330 (ikincil amin), C-H ~2920."),
            FtirCompound("Asetonitril", "CH₃CN", "Nitril", listOf("cn_nitrile", "ch_alkane"), acetonitrile(), "41.05", "C≡N ~2250 (keskin, karakteristik), C-H ~2920."),
            FtirCompound("Asetamid", "CH₃CONH₂", "Amid", listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"), aceAmide(), "59.07", "N-H ~3300, Amid I ~1660, Amid II ~1540."),
            FtirCompound("Nitrobenzen", "C₆H₅NO₂", "Nitro", listOf("no2", "ch_aro", "cc_aro"), nitrobz(), "123.11", "NO₂ ~1540 (güçlü) + ~1350, aromatik ~3050."),
            FtirCompound("Kloroform", "CHCl₃", "Halojen", listOf("c_cl", "ch_alkane"), chloroform(), "119.38", "C-Cl ~760 (güçlü), C-H ~3020."),
            FtirCompound("Diklorometan", "CH₂Cl₂", "Halojen", listOf("c_cl", "ch_alkane"), dichloromethane(), "84.93", "C-Cl ~700, C-H ~2920. Çözücü olarak da kullanılır."),
            FtirCompound("Karbon Tetraklorür", "CCl₄", "Halojen", listOf("c_cl"), ccl4(), "153.82", "C-Cl ~780. C-H piki yok — IR'de çözücü penceresi verir."),
            FtirCompound("Dietil Eter", "(C₂H₅)₂O", "Eter", listOf("ch_alkane", "co_alcohol"), diethylEther(), "74.12", "C-H ~2920, C-O ~1120. OH bandı yok."),
            FtirCompound("DMSO", "(CH₃)₂SO", "Sülfoksit", listOf("ch_alkane"), dmso(), "78.13", "S=O ~1050 (güçlü), C-H ~2920."),
            FtirCompound("1-Butanol", "C₄H₉OH", "Alkol", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), butOH(), "74.12", "O-H ~3350, C-H ~2920, C-O ~1070. Birincil alkol."),
            FtirCompound("Sikloheksanol", "C₆H₁₁OH", "Alkol", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), cyclohexanol(), "100.16", "O-H ~3350, C-H ~2930, C-O ~1060. Halka alkol."),
            FtirCompound("o-Krezol", "CH₃C₆H₄OH", "Aromatik", listOf("oh_alcohol", "ch_aro", "cc_aro", "ch_alkane", "co_alcohol"), kresol(), "108.14", "Fenolik O-H ~3350, aromatik ~3050, C=C ~1500."),
            FtirCompound("MEK", "CH₃COC₂H₅", "Keton", listOf("co_ketone", "ch_alkane"), mek(), "72.11", "C=O ~1715 (doymuş keton), C-H ~2920."),
            FtirCompound("Asetofenon", "C₆H₅COCH₃", "Keton", listOf("co_conj", "ch_aro", "cc_aro", "ch_alkane"), acetophenone(), "120.15", "C=O ~1690 (aromatik konjugasyon düşürür)."),
            FtirCompound("Sikloheksanon", "C₆H₁₀O", "Keton", listOf("co_ketone", "ch_alkane"), cyclohexanone(), "98.15", "C=O ~1715 (6-halka, gerilimsiz)."),
            FtirCompound("Formaldehit", "HCHO", "Aldehit", listOf("co_aldehyde"), formaldehyde(), "30.03", "C=O ~1740. En basit aldehit."),
            FtirCompound("Formik Asit", "HCOOH", "Asit", listOf("oh_acid", "co_acid"), formicAcid(), "46.03", "Geniş O-H 2500-3300, C=O ~1720."),
            FtirCompound("Propanoik Asit", "C₂H₅COOH", "Asit", listOf("oh_acid", "co_acid", "ch_alkane", "co_alcohol"), propanoicAcid(), "74.08", "Geniş asit O-H, C=O ~1710, C-O ~1240."),
            FtirCompound("Metil Benzoat", "C₆H₅COOCH₃", "Ester", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro"), methylBenzoate(), "136.15", "C=O ~1720 (konjuge ester), C-O ~1270."),
            FtirCompound("Butil Asetat", "CH₃COO(CH₂)₃CH₃", "Ester", listOf("co_ester", "co_ester_coc", "ch_alkane"), butylAcetate(), "116.16", "C=O ~1740, C-O-C ~1240."),
            FtirCompound("THF", "C₄H₈O", "Eter", listOf("ch_alkane", "co_alcohol"), thf(), "72.11", "C-H ~2920, C-O ~1070 (halka eter)."),
            FtirCompound("N-Metilanilin", "C₆H₅NHCH₃", "Amin", listOf("nh_secondary", "ch_aro", "cc_aro", "ch_alkane"), nmethylaniline(), "107.16", "Tek N-H ~3400 (sekonder aromatik amin)."),
            FtirCompound("Piperidin", "C₅H₁₁N", "Amin", listOf("nh_secondary", "ch_alkane"), piperidine(), "85.15", "N-H ~3300, C-H ~2920. Halka amin."),
            FtirCompound("Morfolin", "C₄H₉NO", "Amin", listOf("nh_secondary", "ch_alkane", "co_alcohol"), morpholine(), "87.12", "N-H ~3300, C-O ~1110."),
            FtirCompound("DMF", "(CH₃)₂NCHO", "Amid", listOf("co_amide1", "ch_alkane"), dmf(), "73.09", "C=O ~1666 (tersiyer amid), C-N ~1500."),
            FtirCompound("Üre", "CO(NH₂)₂", "Amid", listOf("nh_amide", "co_amide1", "nh_bend"), urea(), "60.06", "N-H ~3300, C=O ~1640, N-H eğilme ~1540."),
            FtirCompound("Benzonitril", "C₆H₅CN", "Nitril", listOf("cn_nitrile", "ch_aro", "cc_aro"), benzonitrile(), "103.12", "C≡N ~2230 (konjuge nitril), aromatik ~3050."),
            FtirCompound("Asetilen", "C₂H₂", "Alkin", listOf("ch_alkyne", "cc_alkyne"), acetylene(), "26.04", "≡C-H ~3300 (keskin), C≡C ~2120."),
            FtirCompound("Propargil Alkol", "HC≡CCH₂OH", "Alkin", listOf("oh_alcohol", "ch_alkyne", "cc_alkyne", "co_alcohol"), propargylAlcohol(), "56.06", "O-H ~3350 + ≡C-H ~3300 üst üste, C≡C ~2120."),
            FtirCompound("Klorobenzen", "C₆H₅Cl", "Halojen", listOf("c_cl", "ch_aro", "cc_aro"), chlorobenzene(), "112.56", "Aromatik ~3050, C=C ~1500/1600, C-Cl parmak izi ~700."),
            FtirCompound("1-Klorobütan", "C₄H₉Cl", "Halojen", listOf("c_cl", "ch_alkane"), chlorobutane(), "92.57", "C-Cl ~650-700, C-H ~2920."),
            FtirCompound("PVC", "(C₂H₃Cl)ₙ", "Polimer", listOf("c_cl", "ch_alkane"), null, "62.50", "C-Cl ~650, C-H ~2920. Polimer; kütle monomer birimindir."),
            FtirCompound("Sikloheksan", "C₆H₁₂", "Alkan", listOf("ch_alkane"), rings(), "84.16", "C-H ~2930/2850. Halka alkan."),
            FtirCompound("Sikloheksen", "C₆H₁₀", "Alken", listOf("ch_alkane", "ch_alkene", "cc_alkene"), cyclohexene(), "82.14", "=C-H ~3020, C=C ~1650."),
            FtirCompound("o-Ksilen", "C₆H₄(CH₃)₂", "Aromatik", listOf("ch_aro", "cc_aro", "ch_alkane"), oxylene(), "106.17", "Aromatik ~3050, C=C ~1500, metil C-H ~2920."),
            FtirCompound("Naftalin", "C₁₀H₈", "Aromatik", listOf("ch_aro", "cc_aro"), null, "128.17", "Aromatik C-H ~3050, çoklu C=C ~1500-1600."),
            FtirCompound("Glukoz", "C₆H₁₂O₆", "Karbonhidrat", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), null, "180.16", "Geniş O-H ~3400, güçlü C-O 1000-1100."),
            FtirCompound("Polietilen", "(C₂H₄)ₙ", "Polimer", listOf("ch_alkane"), null, "28.05", "C-H ~2920/2850/1470. Polimer; kütle monomer birimindir."),
            FtirCompound("Polistiren", "(C₈H₈)ₙ", "Polimer", listOf("ch_aro", "cc_aro", "ch_alkane"), null, "104.15", "Aromatik ~3025, C=C ~1600. Polimer; kütle monomer birimindir."),
            FtirCompound("Naylon 6,6", "(C₁₂H₂₂N₂O₂)ₙ", "Polimer", listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"), null, "226.32", "N-H ~3300, Amid I ~1640, Amid II ~1540."),
            FtirCompound("Benzil Alkol", "C₆H₅CH₂OH", "Alkol", listOf("oh_alcohol", "ch_aro", "cc_aro", "ch_alkane", "co_alcohol"), benzylAlcohol(), "108.14", "O-H ~3350, aromatik ~3050, C-O ~1020."),
            FtirCompound("Allil Alkol", "CH₂=CHCH₂OH", "Alkol", listOf("oh_alcohol", "ch_alkene", "cc_alkene", "co_alcohol", "ch_alkane"), allylAlcohol(), "58.08", "O-H ~3350, =C-H ~3080, C=C ~1650."),
            FtirCompound("MIBK", "(CH₃)₂CHCH₂COCH₃", "Keton", listOf("co_ketone", "ch_alkane"), mibk(), "100.16", "C=O ~1715 (doymuş keton), C-H ~2920."),
            FtirCompound("Benzofenon", "(C₆H₅)₂CO", "Keton", listOf("co_conj", "ch_aro", "cc_aro"), benzophenone(), "182.22", "C=O ~1690 (diaril keton, konjugasyon düşürür)."),
            FtirCompound("Propanal", "CH₃CH₂CHO", "Aldehit", listOf("co_aldehyde", "ch_aldehyde", "ch_alkane"), propanal(), "58.08", "C=O ~1730, Fermi ikilisi ~2720/2820."),
            FtirCompound("Sinemaldehit", "C₆H₅CH=CHCHO", "Aldehit", listOf("co_conj", "ch_aldehyde", "ch_alkene", "cc_alkene"), cinnamaldehyde(), "132.16", "C=O ~1680 (konjuge), =C-H ~3080."),
            FtirCompound("Heksanoik Asit", "C₅H₁₁COOH", "Asit", listOf("oh_acid", "co_acid", "ch_alkane", "co_alcohol"), hexanoicAcid(), "116.16", "Geniş asit O-H, C=O ~1710, C-O ~1240."),
            FtirCompound("Salisilik Asit", "HOC₆H₄COOH", "Asit", listOf("oh_acid", "co_acid", "oh_alcohol", "ch_aro", "cc_aro"), salicylicAcid(), "138.12", "Asit + fenol OH bir arada, C=O ~1680."),
            FtirCompound("Etil Benzoat", "C₆H₅COOC₂H₅", "Ester", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"), ethylBenzoate(), "150.17", "C=O ~1720 (konjuge ester), C-O ~1270."),
            FtirCompound("Metil Asetat", "CH₃COOCH₃", "Ester", listOf("co_ester", "co_ester_coc", "ch_alkane"), methylAcetate(), "74.08", "C=O ~1740, C-O-C ~1240."),
            FtirCompound("1,4-Dioksan", "C₄H₈O₂", "Eter", listOf("ch_alkane", "co_alcohol"), dioxane(), "88.11", "C-H ~2920, C-O ~1120 (iki eter bağı)."),
            FtirCompound("Sikloheksilamin", "C₆H₁₁NH₂", "Amin", listOf("nh_primary", "ch_alkane"), cyclohexylamine(), "99.18", "N-H ikilisi ~3350 (birincil amin)."),
            FtirCompound("Benzilamin", "C₆H₅CH₂NH₂", "Amin", listOf("nh_primary", "ch_aro", "cc_aro", "ch_alkane"), benzylamine(), "107.16", "N-H ikilisi ~3350, aromatik ~3050."),
            FtirCompound("Asetanilid", "C₆H₅NHCOCH₃", "Amid", listOf("nh_amide", "co_amide1", "ch_aro", "cc_aro", "ch_alkane"), acetanilide(), "135.17", "Amid N-H ~3300 (tek), Amid I ~1660."),
            FtirCompound("Formamid", "HCONH₂", "Amid", listOf("nh_amide", "co_amide1", "nh_bend"), formamide(), "45.04", "N-H ~3300, C=O ~1690. En basit amid."),
            FtirCompound("Akrilonitril", "CH₂=CHCN", "Nitril", listOf("cn_nitrile", "ch_alkene", "cc_alkene"), acrylonitrile(), "53.06", "C≡N ~2230, =C-H ~3080, C=C ~1620."),
            FtirCompound("m-Dinitrobenzen", "C₆H₄(NO₂)₂", "Nitro", listOf("no2", "ch_aro", "cc_aro"), null, "168.11", "NO₂ ~1540/1350 (çift nitro, çok güçlü)."),
            FtirCompound("1,2-Dikloroetan", "ClCH₂CH₂Cl", "Halojen", listOf("c_cl", "ch_alkane"), dichloroethane(), "98.96", "C-Cl ~650-700, C-H ~2920."),
            FtirCompound("Siklopentan", "C₅H₁₀", "Alkan", listOf("ch_alkane"), cyclopentane(), "70.13", "C-H ~2930/2850. 5-halka alkan."),
            FtirCompound("1-Heksin", "HC≡C(CH₂)₃CH₃", "Alkin", listOf("ch_alkyne", "cc_alkyne", "ch_alkane"), hexyne(), "82.14", "≡C-H ~3300 (keskin), C≡C ~2120."),
            FtirCompound("Etilbenzen", "C₆H₅C₂H₅", "Aromatik", listOf("ch_aro", "cc_aro", "ch_alkane"), ethylbenzene(), "106.17", "Aromatik ~3050, C=C ~1500, etil C-H ~2920."),
            FtirCompound("Sukroz", "C₁₂H₂₂O₁₁", "Karbonhidrat", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), null, "342.30", "Geniş O-H ~3400, güçlü C-O 1000-1150."),
            FtirCompound("Kloroasetik Asit", "ClCH₂COOH", "Asit", listOf("oh_acid", "co_acid", "c_cl", "ch_alkane"), chloroaceticAcid(), "94.50", "Asit O-H + C=O ~1720, C-Cl ~780.")
        )

        val SAMPLE_TYPES = listOf("KBr Pellet", "İnce Film", "ATR", "Çözelti")
    }

    data class FunctionalGroup(
        val id: String, val name: String, val nameTr: String,
        val wMin: Float, val wMax: Float, val peak: Float,
        val intensity: Float, val width: Float, val color: Int, val shape: String
    )

    fun selectCompound(c: FtirCompound?) {
        selectedCompound = c
        selectedGroups.clear()
        c?.groups?.forEach { selectedGroups.add(it) }
        resetSpectrum()
    }

    fun toggleGroup(id: String) {
        if (id in selectedGroups) selectedGroups.remove(id) else selectedGroups.add(id)
        resetSpectrum()
    }

    init {
        for (i in wavenumbers.indices) wavenumbers[i] = 4000f - i * 0.875f
        resetSpectrum()
    }

    fun setResolution(r: Int) { _resolution = r.coerceIn(1, 8) }
    fun setScanCount(n: Int) { _scanCount = n.coerceIn(1, 64) }
    fun toggleInterferogram() { showInterferogram = !showInterferogram }

    fun reset() {
        selectedCompound = null
        selectedGroups.clear()
        isScanning = false
        scanProgress = 0f
        hasResult = false
        mirrorPosition = 0f
        mirrorVelocity = 0f
        snr = 0f
        resetSpectrum()
        showBanner("Sıfırlandı")
    }

    fun startScan() {
        if (isScanning) return
        if (selectedCompound == null && selectedGroups.isEmpty()) {
            showBanner("Önce numune seç veya grup ekle!")
            return
        }
        isScanning = true
        hasResult = false
        scanProgress = 0f
        scanPhase = 0f
        mirrorPosition = -1f
        mirrorVelocity = 0f
        for (i in transmittance.indices) transmittance[i] = 1f
        showBanner("Tarama başlıyor... Çöz: ${_resolution} cm⁻¹, ${_scanCount} tarama")
    }

    fun bannerVisible(): Boolean = bannerT > 0f && bannerMsg.isNotEmpty()
    private fun showBanner(msg: String, secs: Float = 2.5f) { bannerMsg = msg; bannerT = secs }

    private fun resetSpectrum() {
        for (i in transmittance.indices) transmittance[i] = 1f
        for (i in interferogram.indices) interferogram[i] = 0f
    }

    fun tick(dt: Float) {
        if (bannerT > 0f) bannerT -= dt

        if (!isScanning) return

        // Ayna hareketi (sinusoidal tarama)
        val scanDuration = 3f + scanCount * 0.15f
        scanPhase += dt / scanDuration
        scanProgress = scanPhase.coerceIn(0f, 1f)

        // Ayna pozisyonu: -1 to 1 arası sinüs hareketi
        mirrorPosition = sin(scanPhase * PI.toFloat() * 2)
        mirrorVelocity = cos(scanPhase * PI.toFloat() * 2) * PI.toFloat() * 2 / scanDuration

        // İlerleyen spektrum güncelleme
        if (scanProgress < 1f) {
            updateSpectrumPartial()
            updateInterferogram()
        }

        if (scanProgress >= 1f) {
            isScanning = false
            mirrorPosition = 0f
            mirrorVelocity = 0f
            finalizeSpectrum()
            val snrVal = calculateSNR()
            snr = snrVal
            hasResult = true
            showBanner("Tarama tamamlandı. SNR: ${snrVal.toInt()} dB", 3f)
        }
    }

    private fun updateSpectrumPartial() {
        for (i in transmittance.indices) {
            val wn = 4000f - i * 0.875f // 4000 -> 500 cm⁻¹
            var t = 1f
            for (gId in selectedGroups) {
                val g = FUNCTIONAL_GROUPS.find { it.id == gId } ?: continue
                val diff = wn - g.peak
                val sigma = g.width / 2.355f
                val gauss = exp(-0.5f * (diff / sigma) * (diff / sigma))
                t -= g.intensity * gauss * scanProgress
            }
            // Baseline + atmospheric noise
            val baseNoise = (Random.nextFloat() - 0.5f) * 0.008f * (1f - scanProgress * 0.5f)
            t += baseNoise
            transmittance[i] = t.coerceIn(0.01f, 1f)
            absorbance[i] = -log10(t.coerceIn(0.001f, 1f))
        }
    }

    private fun updateInterferogram() {
        // Simüle interferogram: ayna pozisyonuna bağlı olarak
        for (i in interferogram.indices) {
            val opd = i * 0.5f * mirrorPosition // Optik yol farkı
            var signal = 0f
            for (gId in selectedGroups) {
                val g = FUNCTIONAL_GROUPS.find { it.id == gId } ?: continue
                val wn = g.peak
                val freq = wn * 2 * PI.toFloat() * opd / 10000f // normalize
                signal += g.intensity * cos(freq) * exp(-opd * 0.01f)
            }
            // Merkezi patlama + sarkaç
            val centerBurst = if (abs(mirrorPosition) < 0.1f) exp(-i * 0.3f) else 0f
            interferogram[i] = (signal + centerBurst) * scanProgress + (Random.nextFloat() - 0.5f) * 0.02f
        }
    }

    private fun finalizeSpectrum() {
        // Tam çözünürlükte final spektrum
        for (i in transmittance.indices) {
            val wn = 4000f - i * 0.875f
            var t = 1f
            for (gId in selectedGroups) {
                val g = FUNCTIONAL_GROUPS.find { it.id == gId } ?: continue
                val diff = wn - g.peak
                val sigma = g.width / 2.355f
                val gauss = exp(-0.5f * (diff / sigma) * (diff / sigma))
                t -= g.intensity * gauss
            }
            // Atmosferik CO2/H2O
            val atmo = atmosphericAbsorption(wn)
            t *= (1f - atmo)
            // Gürültü
            t += (Random.nextFloat() - 0.5f) * 0.005f
            transmittance[i] = t.coerceIn(0.01f, 1f)
            absorbance[i] = -log10(t.coerceIn(0.001f, 1f))
        }
    }

    private fun atmosphericAbsorption(wn: Float): Float {
        // CO2 ~2350, H2O ~1600, 3600
        var a = 0f
        val d1 = (wn - 2350f) / 20f // CO2
        val d2 = (wn - 1600f) / 50f // H2O bend
        val d3 = (wn - 3600f) / 100f // H2O stretch
        a += 0.02f * exp(-0.5f * d1 * d1)
        a += 0.015f * exp(-0.5f * d2 * d2)
        a += 0.01f * exp(-0.5f * d3 * d3)
        return a.coerceIn(0f, 0.1f)
    }

    private fun calculateSNR(): Float {
        if (selectedGroups.isEmpty()) return 0f
        var signal = 0f; var noise = 0f
        for (i in transmittance.indices) {
            val wn = 4000f - i * 0.875f
            for (gId in selectedGroups) {
                val g = FUNCTIONAL_GROUPS.find { it.id == gId } ?: continue
                if (abs(wn - g.peak) < g.width) signal += (1f - transmittance[i])
            }
            noise += (Random.nextFloat() - 0.5f) * 0.01f
        }
        if (signal <= 0f) return 0f
        return if (abs(noise) > 1e-6f) 20f * log10(signal / abs(noise).coerceAtLeast(1e-6f)) else 60f
    }

    fun suggestedStep(): String {
        return when {
            selectedCompound == null && selectedGroups.isEmpty() -> "1. ADIM: Numune seç veya fonksiyonel grup ekle"
            isScanning -> "Taranıyor... ${(scanProgress * 100).toInt()}% — ayna hareket ediyor"
            hasResult -> "Spektrum hazır — piklere dokunarak incele, TEKRAR TARA ile yenile"
            else -> "2. ADIM: Çözünürlük/tarama ayarla, sonra TARA'ya bas"
        }
    }

    fun stage(): Int {
        return when {
            selectedCompound == null && selectedGroups.isEmpty() -> 0
            isScanning -> 2
            hasResult -> 3
            else -> 1
        }
    }
}