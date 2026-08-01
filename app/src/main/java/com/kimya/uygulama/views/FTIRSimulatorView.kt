package com.kimya.uygulama.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.*

class FTIRSimulatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class FunctionalGroup(
        val id: String, val name: String, val nameTr: String,
        val wavenumberMin: Float, val wavenumberMax: Float,
        val peakCenter: Float, val peakIntensity: Float,
        val peakWidth: Float, val color: Int,
        val shape: String, val description: String,
        val exampleCompound: String
    )
    data class Atom(val symbol: String, val x: Float, val y: Float, val color: Int)
    data class Bond(val from: Int, val to: Int, val order: Int = 1)
    data class MoleculeStructure(val atoms: List<Atom>, val bonds: List<Bond>)
    data class CookbookCompound(
        val name: String, val formula: String,
        val groups: List<String>, val description: String,
        val molecularWeight: String = "", val category: String = "",
        val structure: MoleculeStructure? = null
    )
    data class ThemeColors(
        val bg: Int = Color.rgb(10, 14, 20), val surface: Int = Color.rgb(18, 24, 32),
        val primary: Int = Color.rgb(0, 240, 200), val text: Int = Color.rgb(220, 230, 240),
        val muted: Int = Color.rgb(120, 140, 160), val accent: Int = Color.rgb(57, 255, 20),
        val line: Int = Color.rgb(30, 40, 50)
    )

    companion object {
        val C_GREEN = Color.rgb(0, 220, 160)
        val C_CYAN = Color.rgb(0, 240, 200)
        val C_O = Color.rgb(255, 80, 80)
        val C_N = Color.rgb(80, 160, 255)
        val C_H = Color.rgb(180, 190, 200)
        val C_CL = Color.rgb(100, 220, 100)

        private fun ring6(cx: Float, cy: Float, r: Float, withH: Boolean = true): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat(), C_GREEN))
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

        fun eth(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -30f, 0f, C_GREEN), Atom("C", 30f, 0f, C_GREEN), Atom("O", 90f, 0f, C_O), Atom("H", 125f, -25f, C_H), Atom("H", -30f, -35f, C_H), Atom("H", -65f, 18f, C_H), Atom("H", -30f, 35f, C_H), Atom("H", 30f, -35f, C_H), Atom("H", 30f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(0, 4), Bond(0, 5), Bond(0, 6), Bond(1, 7), Bond(1, 8))
        )
        fun met(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_GREEN), Atom("O", 50f, 0f, C_O), Atom("H", 85f, -22f, C_H), Atom("H", 0f, -36f, C_H), Atom("H", -32f, 18f, C_H), Atom("H", 0f, 36f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(0, 3), Bond(0, 4), Bond(0, 5))
        )
        fun isoPrOH(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -40f, 0f, C_GREEN), Atom("C", 20f, 0f, C_GREEN), Atom("O", 80f, 0f, C_O), Atom("H", 115f, -22f, C_H), Atom("C", -40f, -50f, C_GREEN), Atom("H", 20f, -40f, C_H), Atom("H", -80f, -30f, C_H), Atom("H", -40f, -85f, C_H), Atom("H", 20f, 35f, C_H), Atom("H", -40f, 35f, C_H), Atom("H", -80f, 20f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(0, 4), Bond(1, 5), Bond(4, 6), Bond(4, 7), Bond(1, 8), Bond(0, 9), Bond(0, 10))
        )
        fun butOH(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_GREEN), Atom("C", -15f, 0f, C_GREEN), Atom("C", 30f, 0f, C_GREEN), Atom("C", 75f, 0f, C_GREEN), Atom("O", 120f, 0f, C_O), Atom("H", 155f, -22f, C_H), Atom("H", -60f, -35f, C_H), Atom("H", -60f, 35f, C_H), Atom("H", -15f, -35f, C_H), Atom("H", 30f, -35f, C_H), Atom("H", 75f, -35f, C_H), Atom("H", 75f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(4, 5), Bond(0, 6), Bond(0, 7), Bond(1, 8), Bond(2, 9), Bond(3, 10), Bond(3, 11))
        )
        fun glycerol(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -40f, 0f, C_GREEN), Atom("C", 20f, 0f, C_GREEN), Atom("C", 80f, 0f, C_GREEN), Atom("O", -85f, 0f, C_O), Atom("H", -115f, -18f, C_H), Atom("O", 20f, -50f, C_O), Atom("H", 20f, -85f, C_H), Atom("O", 125f, 0f, C_O), Atom("H", 155f, -18f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(0, 3), Bond(3, 4), Bond(1, 5), Bond(5, 6), Bond(2, 7), Bond(7, 8))
        )
        fun ace(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_GREEN), Atom("C", -45f, 28f, C_GREEN), Atom("C", 45f, 28f, C_GREEN), Atom("O", 0f, -45f, C_O), Atom("H", -45f, 62f, C_H), Atom("H", -80f, 10f, C_H), Atom("H", -45f, -8f, C_H), Atom("H", 45f, 62f, C_H), Atom("H", 80f, 10f, C_H), Atom("H", 45f, -8f, C_H)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3, 2), Bond(1, 4), Bond(1, 5), Bond(1, 6), Bond(2, 7), Bond(2, 8), Bond(2, 9))
        )
        fun mek(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_GREEN), Atom("C", 0f, 0f, C_GREEN), Atom("C", 50f, 28f, C_GREEN), Atom("C", 50f, -28f, C_GREEN), Atom("O", 0f, -45f, C_O), Atom("H", -50f, -35f, C_H), Atom("H", -85f, 18f, C_H), Atom("H", -50f, 35f, C_H), Atom("H", 50f, 62f, C_H), Atom("H", 85f, 10f, C_H), Atom("H", 85f, -45f, C_H), Atom("H", 50f, -62f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(1, 3), Bond(1, 4, 2), Bond(0, 5), Bond(0, 6), Bond(0, 7), Bond(2, 8), Bond(2, 9), Bond(3, 10), Bond(3, 11))
        )
        fun cycHexanone(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", (cos(ang) * 38).toFloat(), (sin(ang) * 38).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6))
            a.add(Atom("O", 0f, -78f, C_O)); b.add(Bond(0, 6, 2))
            for (i in 1 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 68).toFloat(), (sin(ang) * 68).toFloat(), C_H))
                b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun acetophenone(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", 72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("C", 72f, 40f, C_GREEN)); b.add(Bond(ci, ci + 2))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            a.add(Atom("H", 72f, 75f, C_H)); b.add(Bond(ci + 2, a.size - 1))
            a.add(Atom("H", 105f, 55f, C_H)); b.add(Bond(ci + 2, a.size - 1))
            a.add(Atom("H", 40f, 55f, C_H)); b.add(Bond(ci + 2, a.size - 1))
            return MoleculeStructure(a, b)
        }
        fun benzaldehyde(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", 72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("H", 72f, 35f, C_H)); b.add(Bond(ci, ci + 2))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun acetaldehyde(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -20f, 0f, C_GREEN), Atom("C", 30f, 0f, C_GREEN), Atom("O", 65f, -25f, C_O), Atom("H", 30f, 35f, C_H), Atom("H", -20f, -35f, C_H), Atom("H", -55f, 18f, C_H), Atom("H", -20f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(0, 4), Bond(0, 5), Bond(0, 6))
        )
        fun formaldehyde(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_GREEN), Atom("O", 0f, -40f, C_O), Atom("H", -35f, 20f, C_H), Atom("H", 35f, 20f, C_H)),
            listOf(Bond(0, 1, 2), Bond(0, 2), Bond(0, 3))
        )
        fun aceAcid(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -28f, 0f, C_GREEN), Atom("C", 28f, 0f, C_GREEN), Atom("O", 28f, -45f, C_O), Atom("O", 82f, 18f, C_O), Atom("H", 115f, 0f, C_H), Atom("H", -28f, -35f, C_H), Atom("H", -60f, 18f, C_H), Atom("H", -28f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(0, 5), Bond(0, 6), Bond(0, 7))
        )
        fun propanoicAcid(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_GREEN), Atom("C", 0f, 0f, C_GREEN), Atom("C", 50f, 0f, C_GREEN), Atom("O", 50f, -45f, C_O), Atom("O", 95f, 20f, C_O), Atom("H", 128f, 2f, C_H), Atom("H", -50f, -35f, C_H), Atom("H", -85f, 18f, C_H), Atom("H", -50f, 35f, C_H), Atom("H", 0f, -35f, C_H), Atom("H", 0f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3, 2), Bond(2, 4), Bond(4, 5), Bond(0, 6), Bond(0, 7), Bond(0, 8), Bond(1, 9), Bond(1, 10))
        )
        fun benzoicAcid(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", 72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 72f, 40f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("H", 105f, 55f, C_H)); b.add(Bond(ci + 2, ci + 3))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun ethylAcetate(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -50f, 0f, C_GREEN), Atom("C", 0f, 0f, C_GREEN), Atom("O", 0f, -45f, C_O), Atom("C", 50f, 0f, C_GREEN), Atom("O", 50f, 45f, C_O), Atom("C", 95f, 0f, C_GREEN), Atom("H", -50f, -35f, C_H), Atom("H", -85f, 18f, C_H), Atom("H", -50f, 35f, C_H), Atom("H", 50f, -35f, C_H), Atom("H", 95f, -35f, C_H), Atom("H", 95f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(4, 5), Bond(0, 6), Bond(0, 7), Bond(0, 8), Bond(3, 9), Bond(5, 10), Bond(5, 11))
        )
        fun methylBenzoate(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", 72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 72f, 40f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("C", 110f, 55f, C_GREEN)); b.add(Bond(ci + 2, ci + 3))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            a.add(Atom("H", 110f, 90f, C_H)); b.add(Bond(ci + 3, a.size - 1))
            a.add(Atom("H", 140f, 40f, C_H)); b.add(Bond(ci + 3, a.size - 1))
            a.add(Atom("H", 85f, 70f, C_H)); b.add(Bond(ci + 3, a.size - 1))
            return MoleculeStructure(a, b)
        }
        fun ethylBenzoate(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", 72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("O", 108f, -25f, C_O)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("O", 72f, 40f, C_O)); b.add(Bond(ci, ci + 2))
            a.add(Atom("C", 110f, 55f, C_GREEN)); b.add(Bond(ci + 2, ci + 3))
            a.add(Atom("C", 145f, 35f, C_GREEN)); b.add(Bond(ci + 3, ci + 4))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            a.add(Atom("H", 110f, 90f, C_H)); b.add(Bond(ci + 3, a.size - 1))
            a.add(Atom("H", 145f, 70f, C_H)); b.add(Bond(ci + 4, a.size - 1))
            a.add(Atom("H", 175f, 50f, C_H)); b.add(Bond(ci + 4, a.size - 1))
            a.add(Atom("H", 145f, 0f, C_H)); b.add(Bond(ci + 4, a.size - 1))
            return MoleculeStructure(a, b)
        }
        fun benz(): MoleculeStructure = ring6(0f, 0f, 42f)
        fun tol(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", -72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("H", -100f, -22f, C_H)); b.add(Bond(ci, ci + 1))
            a.add(Atom("H", -100f, 22f, C_H)); b.add(Bond(ci, ci + 2))
            a.add(Atom("H", -72f, 38f, C_H)); b.add(Bond(ci, ci + 3))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun hexMol(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val x = (i - 2.5f) * 38f; val y = if (i % 2 == 0) -14f else 14f
                a.add(Atom("C", x, y, C_GREEN)); if (i > 0) b.add(Bond(i - 1, i))
            }
            a.add(Atom("H", -95f, -45f, C_H)); b.add(Bond(0, 6))
            a.add(Atom("H", -95f, 16f, C_H)); b.add(Bond(0, 7))
            a.add(Atom("H", 95f, -45f, C_H)); b.add(Bond(5, 8))
            a.add(Atom("H", 95f, 16f, C_H)); b.add(Bond(5, 9))
            for (i in 1 until 5) {
                val x = (i - 2.5f) * 38f
                a.add(Atom("H", x, if (i % 2 == 0) -48f else 48f, C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun cyclohexane(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", (cos(ang) * 38).toFloat(), (sin(ang) * 38).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 68).toFloat(), (sin(ang) * 68).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun hexene(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val x = (i - 2.5f) * 38f; val y = if (i % 2 == 0) -14f else 14f
                a.add(Atom("C", x, y, C_GREEN)); if (i > 0) b.add(Bond(i - 1, i, if (i == 1) 2 else 1))
            }
            a.add(Atom("H", -95f, -45f, C_H)); b.add(Bond(0, 6))
            a.add(Atom("H", -95f, 16f, C_H)); b.add(Bond(0, 7))
            a.add(Atom("H", 95f, -45f, C_H)); b.add(Bond(5, 8))
            a.add(Atom("H", 95f, 16f, C_H)); b.add(Bond(5, 9))
            for (i in 2 until 5) {
                val x = (i - 2.5f) * 38f
                a.add(Atom("H", x, if (i % 2 == 0) -48f else 48f, C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun styrene(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", -72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("C", -108f, -25f, C_GREEN)); b.add(Bond(ci, ci + 1, 2))
            a.add(Atom("H", -72f, 35f, C_H)); b.add(Bond(ci, ci + 2))
            a.add(Atom("H", -108f, -60f, C_H)); b.add(Bond(ci + 1, ci + 3))
            a.add(Atom("H", -140f, -10f, C_H)); b.add(Bond(ci + 1, ci + 4))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun aniline(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("N", 0f, -78f, C_N)); b.add(Bond(0, ci))
            a.add(Atom("H", -25f, -105f, C_H)); b.add(Bond(ci, ci + 1))
            a.add(Atom("H", 25f, -105f, C_H)); b.add(Bond(ci, ci + 2))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun diethylamine(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -60f, 0f, C_GREEN), Atom("C", -20f, 0f, C_GREEN), Atom("N", 20f, 0f, C_N), Atom("H", 55f, -20f, C_H), Atom("C", 60f, 0f, C_GREEN), Atom("C", 100f, 0f, C_GREEN), Atom("H", -60f, -35f, C_H), Atom("H", -95f, 18f, C_H), Atom("H", -60f, 35f, C_H), Atom("H", 100f, -35f, C_H), Atom("H", 135f, 18f, C_H), Atom("H", 100f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(2, 4), Bond(4, 5), Bond(0, 6), Bond(0, 7), Bond(0, 8), Bond(5, 9), Bond(5, 10), Bond(5, 11))
        )
        fun acetonitrile(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -30f, 0f, C_GREEN), Atom("C", 20f, 0f, C_GREEN), Atom("N", 65f, 0f, C_N), Atom("H", -30f, -35f, C_H), Atom("H", -65f, 18f, C_H), Atom("H", -30f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 3), Bond(0, 3), Bond(0, 4), Bond(0, 5))
        )
        fun benzonitrile(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("C", 72f, 0f, C_GREEN)); b.add(Bond(0, ci))
            a.add(Atom("N", 108f, 0f, C_N)); b.add(Bond(ci, ci + 1, 3))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun chloroform(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_GREEN), Atom("Cl", -40f, -30f, C_CL), Atom("Cl", 40f, -30f, C_CL), Atom("Cl", 0f, 40f, C_CL), Atom("H", 0f, -45f, C_H)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3), Bond(0, 4))
        )
        fun dichloromethane(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_GREEN), Atom("Cl", -35f, -25f, C_CL), Atom("Cl", 35f, -25f, C_CL), Atom("H", -25f, 30f, C_H), Atom("H", 25f, 30f, C_H)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3), Bond(0, 4))
        )
        fun ccl4(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", 0f, 0f, C_GREEN), Atom("Cl", -40f, -30f, C_CL), Atom("Cl", 40f, -30f, C_CL), Atom("Cl", -40f, 30f, C_CL), Atom("Cl", 40f, 30f, C_CL)),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3), Bond(0, 4))
        )
        fun acetamide(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -25f, 0f, C_GREEN), Atom("C", 25f, 0f, C_GREEN), Atom("O", 25f, -42f, C_O), Atom("N", 68f, 20f, C_N), Atom("H", 100f, 5f, C_H), Atom("H", 68f, 55f, C_H), Atom("H", -25f, -35f, C_H), Atom("H", -60f, 18f, C_H), Atom("H", -25f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(3, 5), Bond(0, 6), Bond(0, 7), Bond(0, 8))
        )
        fun nitrobenzene(): MoleculeStructure {
            val r = ring6(0f, 0f, 38f, false)
            val a = r.atoms.toMutableList(); val b = r.bonds.toMutableList()
            val ci = a.size; a.add(Atom("N", 0f, -78f, C_N)); b.add(Bond(0, ci))
            a.add(Atom("O", -28f, -105f, C_O)); b.add(Bond(ci, ci + 1))
            a.add(Atom("O", 28f, -105f, C_O)); b.add(Bond(ci, ci + 2, 2))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 70).toFloat(), (sin(ang) * 70).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun diethylEther(): MoleculeStructure = MoleculeStructure(
            listOf(Atom("C", -55f, 0f, C_GREEN), Atom("C", -15f, 0f, C_GREEN), Atom("O", 25f, 0f, C_O), Atom("C", 65f, 0f, C_GREEN), Atom("C", 105f, 0f, C_GREEN), Atom("H", -55f, -35f, C_H), Atom("H", -90f, 18f, C_H), Atom("H", -55f, 35f, C_H), Atom("H", 105f, -35f, C_H), Atom("H", 140f, 18f, C_H), Atom("H", 105f, 35f, C_H)),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(3, 4), Bond(0, 5), Bond(0, 6), Bond(0, 7), Bond(4, 8), Bond(4, 9), Bond(4, 10))
        )
        fun thf(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 4) {
                val ang = Math.toRadians(90.0 * i - 45.0)
                a.add(Atom("C", (cos(ang) * 35).toFloat(), (sin(ang) * 35).toFloat(), C_GREEN))
            }
            a.add(Atom("O", 0f, -55f, C_O))
            for (i in 0 until 4) b.add(Bond(i, (i + 1) % 4))
            b.add(Bond(3, 4)); b.add(Bond(4, 0))
            for (i in 0 until 4) {
                val ang = Math.toRadians(90.0 * i - 45.0)
                a.add(Atom("H", (cos(ang) * 60).toFloat(), (sin(ang) * 60).toFloat() - 5f, C_H)); b.add(Bond(i, a.size - 1))
                a.add(Atom("H", (cos(ang) * 20).toFloat(), (sin(ang) * 60).toFloat() + 10f, C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun diphenylmethane(): MoleculeStructure {
            val r1 = ring6(-40f, 0f, 30f, false); val r2 = ring6(40f, 0f, 30f, false)
            val a = (r1.atoms + r2.atoms).toMutableList(); val b = (r1.bonds + r2.bonds).toMutableList()
            val ci = a.size; a.add(Atom("C", 0f, 0f, C_GREEN)); b.add(Bond(0, ci)); b.add(Bond(6, ci))
            a.add(Atom("H", 0f, 30f, C_H)); b.add(Bond(ci, a.size - 1))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", -40f + (cos(ang) * 55).toFloat(), (sin(ang) * 55).toFloat(), C_H)); b.add(Bond(i, a.size - 1))
            }
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", 40f + (cos(ang) * 55).toFloat(), (sin(ang) * 55).toFloat(), C_H)); b.add(Bond(6 + i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }

        val FUNCTIONAL_GROUPS = listOf(
            FunctionalGroup("oh_alcohol", "O-H (Alkol)", "Hidroksil", 3200f, 3600f, 3350f, 0.85f, 180f, Color.rgb(255, 80, 80), "Geniş", "Geniş pik. Alkol ve fenol.", "Etanol"),
            FunctionalGroup("oh_acid", "O-H (Asit)", "Asit O-H", 2500f, 3300f, 3000f, 0.92f, 400f, Color.rgb(255, 60, 60), "Çok Geniş", "Çok geniş. Asit belirleyici.", "Asetik Asit"),
            FunctionalGroup("nh_primary", "N-H (1° Amin)", "1° Amin", 3250f, 3500f, 3400f, 0.55f, 80f, Color.rgb(80, 180, 255), "Çift Pik", "İkiz pik. Birincil amin.", "Anilin"),
            FunctionalGroup("nh_secondary", "N-H (2° Amin)", "2° Amin", 3310f, 3350f, 3330f, 0.4f, 50f, Color.rgb(100, 200, 255), "Tek Pik", "Tek zayıf pik.", "Dietilamin"),
            FunctionalGroup("nh_amide", "N-H (Amid)", "Amid N-H", 3180f, 3350f, 3280f, 0.5f, 100f, Color.rgb(120, 160, 255), "Geniş", "Amid bantları ile görülür.", "Asetamid"),
            FunctionalGroup("ch_alkane", "C-H (Alkan)", "Alkan C-H", 2845f, 2970f, 2920f, 0.7f, 50f, Color.rgb(200, 200, 80), "Keskin", "Güçlü pik. sp³ C-H.", "Hekzan"),
            FunctionalGroup("ch_alkene", "C-H (Alken)", "=C-H", 3020f, 3100f, 3080f, 0.45f, 40f, Color.rgb(100, 220, 100), "Orta", "sp² C-H. Alken belirtisi.", "1-Heksen"),
            FunctionalGroup("ch_aro", "C-H (Aromatik)", "Ar C-H", 3000f, 3100f, 3050f, 0.4f, 35f, Color.rgb(180, 140, 255), "Zayıf", "Aromatik C-H.", "Benzen"),
            FunctionalGroup("ch_aldehyde", "C-H (Aldehit)", "Aldehit C-H", 2720f, 2830f, 2780f, 0.35f, 40f, Color.rgb(200, 180, 100), "Çift Pik", "Fermi çift pik.", "Benzaldehit"),
            FunctionalGroup("ch_aldehyde2", "C-H (Aldehit 2)", "Fermi Çift", 2720f, 2820f, 2720f, 0.3f, 30f, Color.rgb(220, 190, 90), "Keskin", "Fermi alt pik.", "Benzaldehit"),
            FunctionalGroup("ch_alkyne", "≡C-H (Alkin)", "Alkin ≡C-H", 3260f, 3330f, 3300f, 0.7f, 30f, Color.rgb(255, 220, 80), "Keskin", "Güçlü pik.", "Asetilen"),
            FunctionalGroup("co_ketone", "C=O (Keton)", "Keton C=O", 1705f, 1725f, 1715f, 0.95f, 35f, Color.rgb(255, 200, 50), "Keskin", "Çok güçlü pik.", "Aseton"),
            FunctionalGroup("co_aldehyde", "C=O (Aldehit)", "Aldehit C=O", 1720f, 1740f, 1730f, 0.9f, 30f, Color.rgb(255, 180, 80), "Keskin", "Güçlü pik.", "Benzaldehit"),
            FunctionalGroup("co_ester", "C=O (Ester)", "Ester C=O", 1735f, 1750f, 1740f, 0.88f, 30f, Color.rgb(255, 160, 100), "Keskin", "C-O ile ester.", "Etil Asetat"),
            FunctionalGroup("co_acid", "C=O (Asit)", "Asit C=O", 1700f, 1725f, 1710f, 0.92f, 35f, Color.rgb(255, 140, 70), "Keskin", "O-H ile asit.", "Asetik Asit"),
            FunctionalGroup("co_amide1", "C=O (Amid I)", "Amid I", 1630f, 1690f, 1660f, 0.85f, 40f, Color.rgb(220, 180, 255), "Keskin", "Amid I bandı.", "Asetamid"),
            FunctionalGroup("cc_alkene", "C=C (Alken)", "Alken C=C", 1620f, 1680f, 1650f, 0.35f, 35f, Color.rgb(80, 255, 180), "Zayıf", "Değişken şiddet.", "1-Heksen"),
            FunctionalGroup("cc_aro", "C=C (Aromatik)", "Ar C=C", 1450f, 1615f, 1500f, 0.45f, 60f, Color.rgb(160, 140, 220), "Orta", "Çoklu pikler.", "Benzen"),
            FunctionalGroup("cc_alkyne", "C≡C (Alkin)", "Alkin C≡C", 2100f, 2260f, 2150f, 0.3f, 25f, Color.rgb(255, 255, 100), "Zayıf", "Zayıf pik.", "Asetilen"),
            FunctionalGroup("cn_nitrile", "C≡N (Nitril)", "Nitril C≡N", 2210f, 2260f, 2250f, 0.5f, 25f, Color.rgb(150, 255, 150), "Orta", "Karakteristik pik.", "Asetonitril"),
            FunctionalGroup("no2", "NO₂ (Nitro)", "Nitro", 1515f, 1570f, 1540f, 0.8f, 35f, Color.rgb(255, 80, 180), "Güçlü", "NO₂ gerilmesi.", "Nitrobenzen"),
            FunctionalGroup("co_alcohol", "C-O (Alkol)", "C-O", 1040f, 1175f, 1100f, 0.65f, 80f, Color.rgb(255, 150, 100), "Güçlü", "Alkol/ester/eter.", "Etanol"),
            FunctionalGroup("co_ester_coc", "C-O-C (Ester)", "Ester C-O-C", 1150f, 1300f, 1240f, 0.75f, 70f, Color.rgb(255, 130, 80), "Güçlü", "Ester C-O.", "Etil Asetat"),
            FunctionalGroup("nh_bend", "N-H Bükülme", "Amid II", 1510f, 1570f, 1540f, 0.6f, 35f, Color.rgb(140, 180, 255), "Orta", "Amid II bandı.", "Asetamid"),
            FunctionalGroup("c_cl", "C-Cl (Klor)", "Kloro", 550f, 850f, 700f, 0.7f, 100f, Color.rgb(180, 220, 180), "Güçlü", "Halojenli bileşik.", "Kloroform")
        )

        val COOKBOOK_COMPOUNDS = listOf(
            CookbookCompound("Etanol", "C₂H₅OH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1100)", "46.07", "Alkol", eth()),
            CookbookCompound("Metanol", "CH₃OH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1050)", "32.04", "Alkol", met()),
            CookbookCompound("İzopropanol", "(CH₃)₂CHOH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1150)", "60.10", "Alkol", isoPrOH()),
            CookbookCompound("1-Butanol", "C₄H₉OH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1070)", "74.12", "Alkol", butOH()),
            CookbookCompound("Fenol", "C₆H₅OH", listOf("oh_alcohol", "ch_aro", "cc_aro", "co_alcohol"), "O-H (3350), Aromatik (3050), C=C (1500)", "94.11", "Aromatik", null),
            CookbookCompound("Gliserol", "C₃H₈O₃", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), "3x O-H (3350), C-O (1100)", "92.09", "Alkol", glycerol()),
            CookbookCompound("Aseton", "CH₃COCH₃", listOf("co_ketone", "ch_alkane"), "C=O (1715), C-H (2920)", "58.08", "Keton", ace()),
            CookbookCompound("2-Butanon (MEK)", "CH₃COC₂H₅", listOf("co_ketone", "ch_alkane"), "C=O (1715), C-H (2920)", "72.11", "Keton", mek()),
            CookbookCompound("Sikloheksanon", "C₆H₁₀O", listOf("co_ketone", "ch_alkane"), "C=O (1715), C-H (2920)", "98.14", "Keton", cycHexanone()),
            CookbookCompound("Asetofenon", "C₆H₅COCH₃", listOf("co_ketone", "ch_aro", "cc_aro", "ch_alkane"), "C=O (1715), Aromatik (1500)", "120.15", "Keton", acetophenone()),
            CookbookCompound("Benzaldehit", "C₆H₅CHO", listOf("co_aldehyde", "ch_aldehyde", "ch_aro", "cc_aro"), "C=O (1730), Fermi çift (2720,2820)", "106.12", "Aldehit", benzaldehyde()),
            CookbookCompound("Asetaldehit", "CH₃CHO", listOf("co_aldehyde", "ch_aldehyde", "ch_alkane"), "C=O (1730), Fermi çift (2720,2820)", "44.05", "Aldehit", acetaldehyde()),
            CookbookCompound("Formaldehit", "HCHO", listOf("co_aldehyde"), "C=O (1745)", "30.03", "Aldehit", formaldehyde()),
            CookbookCompound("Asetik Asit", "CH₃COOH", listOf("oh_acid", "co_acid", "co_alcohol"), "O-H (2500-3300), C=O (1710), C-O (1240)", "60.05", "Asit", aceAcid()),
            CookbookCompound("Propiyonik Asit", "C₂H₅COOH", listOf("oh_acid", "co_acid", "ch_alkane", "co_alcohol"), "O-H, C=O (1710), C-H (2920)", "74.08", "Asit", propanoicAcid()),
            CookbookCompound("Benzoik Asit", "C₆H₅COOH", listOf("oh_acid", "co_acid", "ch_aro", "cc_aro"), "O-H, C=O (1690), Aromatik C=C (1500)", "122.12", "Asit", benzoicAcid()),
            CookbookCompound("Etil Asetat", "CH₃COOC₂H₅", listOf("co_ester", "co_ester_coc", "ch_alkane"), "C=O (1740), C-O-C (1240)", "88.11", "Ester", ethylAcetate()),
            CookbookCompound("Metil Benzoat", "C₆H₅COOCH₃", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro"), "C=O (1724), C-O-C (1275)", "136.15", "Ester", methylBenzoate()),
            CookbookCompound("Etil Benzoat", "C₆H₅COOC₂H₅", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"), "C=O (1720), C-O-C (1270)", "150.17", "Ester", ethylBenzoate()),
            CookbookCompound("Benzen", "C₆H₆", listOf("ch_aro", "cc_aro"), "Aromatik C-H (3050), C=C (1500, 1600)", "78.11", "Aromatik", benz()),
            CookbookCompound("Toluen", "C₆H₅CH₃", listOf("ch_aro", "cc_aro", "ch_alkane"), "Aromatik (3050), C=C (1500)", "92.14", "Aromatik", tol()),
            CookbookCompound("Hekzan", "C₆H₁₄", listOf("ch_alkane"), "C-H (2920, 2850)", "86.18", "Alkan", hexMol()),
            CookbookCompound("Sikloheksan", "C₆H₁₂", listOf("ch_alkane"), "C-H (2920, 2850)", "84.16", "Alkan", cyclohexane()),
            CookbookCompound("1-Heksen", "C₆H₁₂", listOf("ch_alkane", "ch_alkene", "cc_alkene"), "C-H (2920), =C-H (3080), C=C (1650)", "84.16", "Alken", hexene()),
            CookbookCompound("Stiren", "C₆H₅CH=CH₂", listOf("ch_aro", "cc_aro", "ch_alkene", "cc_alkene"), "Aromatik + Alken, C=C (1630)", "104.15", "Alken", styrene()),
            CookbookCompound("Anilin", "C₆H₅NH₂", listOf("nh_primary", "ch_aro", "cc_aro"), "N-H çift (3400), Aromatik (3050)", "93.13", "Amin", aniline()),
            CookbookCompound("Dietilamin", "(C₂H₅)₂NH", listOf("nh_secondary", "ch_alkane"), "N-H tek (3330), C-H (2920)", "73.14", "Amin", diethylamine()),
            CookbookCompound("Asetonitril", "CH₃CN", listOf("cn_nitrile", "ch_alkane"), "C≡N (2250), C-H (2920)", "41.05", "Nitril", acetonitrile()),
            CookbookCompound("Benzonitril", "C₆H₅CN", listOf("cn_nitrile", "ch_aro", "cc_aro"), "C≡N (2230), Aromatik (3050)", "103.12", "Nitril", benzonitrile()),
            CookbookCompound("Kloroform", "CHCl₃", listOf("c_cl", "ch_alkane"), "C-Cl (760), C-H (3020)", "119.38", "Halojen", chloroform()),
            CookbookCompound("Diklorometan", "CH₂Cl₂", listOf("c_cl", "ch_alkane"), "C-Cl (700), C-H (2920)", "84.93", "Halojen", dichloromethane()),
            CookbookCompound("Karbon Tetraklorür", "CCl₄", listOf("c_cl"), "C-Cl (780, 820)", "153.82", "Halojen", ccl4()),
            CookbookCompound("Asetamid", "CH₃CONH₂", listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"), "N-H (3300), Amid I (1660), Amid II (1540)", "59.07", "Amid", acetamide()),
            CookbookCompound("Nitrobenzen", "C₆H₅NO₂", listOf("no2", "ch_aro", "cc_aro"), "NO₂ (1540), Aromatik (3050)", "123.11", "Nitro", nitrobenzene()),
            CookbookCompound("Dietil Eter", "(C₂H₅)₂O", listOf("ch_alkane", "co_alcohol"), "C-H (2920), C-O (1120)", "74.12", "Eter", diethylEther()),
            CookbookCompound("THF", "C₄H₈O", listOf("ch_alkane", "co_alcohol"), "C-H (2920), C-O (1070)", "72.11", "Eter", thf()),
            CookbookCompound("Difenil Metan", "(C₆H₅)₂CH₂", listOf("ch_aro", "cc_aro", "ch_alkane"), "Aromatik (3050), C=C (1500)", "166.22", "Aromatik", diphenylmethane()),
            CookbookCompound("Naylon 6,6", "(C₁₂H₂₂N₂O₂)ₙ", listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"), "N-H (3300), Amid I (1660)", "226.32", "Polimer"),
            CookbookCompound("Polietilen", "(C₂H₄)ₙ", listOf("ch_alkane"), "C-H (2920, 2850, 1470)", "—", "Polimer"),
            CookbookCompound("Polistiren", "(C₈H₈)ₙ", listOf("ch_aro", "cc_aro", "ch_alkane"), "Aromatik (3025), C=C (1600)", "—", "Polimer"),
            CookbookCompound("Şeker (Sucroz)", "C₁₂H₂₂O₁₁", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), "O-H (3400), C-O (1000-1100)", "342.30", "Karbonhidrat")
        )

        val SAMPLE_TYPES = listOf(
            "KBr Pellet" to "Katı numuneler için KBr",
            "İnce Film" to "Sıvı numuneler için NaCl",
            "ATR" to "Tam yansıma tekniği",
            "Çözelti" to "CCl₄ içinde seyreltme"
        )
    }

    var selectedGroups = mutableSetOf<String>(); private set
    var resolution = 4; private set
    var scanCount = 16; private set
    var sampleType = SAMPLE_TYPES[0].first; private set
    var isScanning = false; private set
    var scanProgress = 0f; private set
    var showInterferogram = false
    var showInfo = false
    var currentCompound: CookbookCompound? = null; private set

    private var time = 0f
    private var themeColors = ThemeColors()
    private val spectrumData = FloatArray(2000)
    private val interferogramData = FloatArray(200)
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector
    private var cursorX = 0f; private var cursorY = 0f; private var showCursor = false
    private var tapTime = 0L; private var lastTapTime = 0L
    private var scanLineX = 0f; private var scanLineActive = false
    private var animProgress = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            time += 0.025f
            if (scanLineActive) { scanLineX += 0.015f; if (scanLineX >= 1f) { scanLineActive = false; scanLineX = 1f } }
            if (animProgress < 1f) animProgress = (animProgress + 0.02f).coerceAtMost(1f)
            updateData(); invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    init {
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean = true
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val ns = (zoomScale * d.scaleFactor).coerceIn(0.5f, 4f); val sc = ns / zoomScale
                panX = d.focusX - (d.focusX - panX) * sc; panY = d.focusY - (d.focusY - panY) * sc
                zoomScale = ns; invalidate(); return true
            }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                0 -> { lastTx = e.x; lastTy = e.y; touchMode = 0; tapTime = System.currentTimeMillis() }
                2 -> { val dx = e.x - lastTx; val dy = e.y - lastTy; if (abs(dx) > 5 || abs(dy) > 5) touchMode = 1; if (touchMode == 1) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y } }
                1, 3 -> { if (touchMode == 0) { val now = System.currentTimeMillis(); if (now - lastTapTime < 300) { zoomScale = 1f; panX = 0f; panY = 0f; invalidate() }; lastTapTime = now }; touchMode = 0; showCursor = false }
            }
            if (e.action == MotionEvent.ACTION_MOVE && touchMode == 0) { cursorX = e.x; cursorY = e.y; showCursor = true }
            true
        }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val spectrumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.5f }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; textAlign = Paint.Align.CENTER }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val atomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val atomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val bondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val peakLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
    private val peakBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val peakNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; textAlign = Paint.Align.CENTER; isFakeBoldText = true; color = Color.WHITE }
    private val peakDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    private val path = Path()
    private val rect = RectF()
    private val labelBgRect = RectF()

    fun toggleGroup(g: String) { if (selectedGroups.contains(g)) selectedGroups.remove(g) else selectedGroups.add(g); generateSpectrum(); invalidate() }
    fun selectPreset(c: CookbookCompound) { selectedGroups.clear(); selectedGroups.addAll(c.groups); currentCompound = c; animProgress = 0f; scanLineX = 0f; scanLineActive = true; generateSpectrum(); invalidate() }
    fun setResolution(r: Int) { resolution = r.coerceIn(1, 8); generateSpectrum() }
    fun setScanCount(c: Int) { scanCount = c.coerceIn(1, 128) }
    fun setSampleType(t: String) { sampleType = t }
    fun toggleInfo() { showInfo = !showInfo; invalidate() }
    fun startScan() { isScanning = true; scanProgress = 0f; generateSpectrum(); invalidate() }
    fun setThemeColors(c: ThemeColors) { themeColors = c; bgPaint.color = c.bg; generateSpectrum(); invalidate() }

    private fun updateData() {
        if (isScanning) { scanProgress += 0.004f; if (scanProgress >= 1f) { scanProgress = 1f; isScanning = false; generateSpectrum() } }
        val zpd = interferogramData.size / 2
        for (i in interferogramData.indices) {
            val opd = (i - zpd).toFloat(); val env = exp(-(opd * opd) / (3000f + scanCount * 50f))
            interferogramData[i] = env * cos(opd * 0.08f + time * 2f) + env * 0.4f * cos(opd * 0.12f + time * 1.3f) + (Math.random().toFloat() - 0.5f) * 0.01f
        }
    }

    private fun generateSpectrum() {
        for (i in spectrumData.indices) spectrumData[i] = 0.5f
        val nl = 0.15f + (8 - resolution) * 0.1f
        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            spectrumData[i] += 0.5f * sin(wn / 4000f * PI.toFloat()) * 0.02f + 0.015f * sin(wn / 800f * PI.toFloat()) * sin(wn / 1200f * PI.toFloat())
            spectrumData[i] -= 0.12f * exp(-((wn - 2349f).pow(2)) / 450f)
            for (j in intArrayOf(1595, 1650, 3750, 3660, 3600)) spectrumData[i] -= 0.04f * exp(-((wn - j.toFloat()).pow(2)) / 800f)
        }
        for (g in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(g.id)) continue
            val hw = g.peakWidth / resolution; val lor = g.peakIntensity > 0.7f
            for (i in spectrumData.indices) {
                val wn = 4000f - i * (3600f / spectrumData.size); val d = wn - g.peakCenter
                spectrumData[i] -= (if (lor) g.peakIntensity / (1f + d * d / (hw * hw)) else g.peakIntensity * exp(-d * d / (2f * hw * hw))) * 0.45f
            }
        }
        for (i in spectrumData.indices) { spectrumData[i] += (Math.random().toFloat() - 0.5f) * nl * 0.012f; spectrumData[i] = spectrumData[i].coerceIn(0.02f, 0.98f) }
    }

    fun getPeakTable(): List<Triple<String, Float, Float>> {
        val p = mutableListOf<Triple<String, Float, Float>>()
        for (g in FUNCTIONAL_GROUPS) { if (!selectedGroups.contains(g.id)) continue; val idx = ((4000f - g.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1); p.add(Triple(g.name, g.peakCenter, spectrumData[idx] * 100f)) }
        return p.sortedByDescending { it.second }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); handler.post(ticker) }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacksAndMessages(null) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.5f; color = Color.argb(12, 0, 255, 200) }
        var sx = 0f; while (sx < w) { canvas.drawLine(sx, 0f, sx, h, gp); sx += 30f }
        var sy = 0f; while (sy < h) { canvas.drawLine(0f, sy, w, sy, gp); sy += 30f }

        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2, h / 2); canvas.translate(panX / zoomScale, panY / zoomScale)
        if (showInterferogram) { drawInterferogram(canvas, w, h) } else {
            val c = currentCompound
            if (c != null) {
                drawCompoundHeader(canvas, w, h * 0.13f)
                if (c.structure != null) drawMolecule(canvas, 0f, h * 0.13f, w, h * 0.30f, c)
                drawSpectrum(canvas, 0f, if (c.structure != null) h * 0.43f else h * 0.14f, w, if (c.structure != null) h * 0.57f else h * 0.86f)
            } else drawSpectrum(canvas, 0f, 0f, w, h)
        }
        canvas.restore()

        if (scanLineActive) { val x = w * scanLineX; scanLinePaint.color = Color.argb(200, 0, 255, 200); canvas.drawLine(x, 0f, x, h, scanLinePaint) }
        if (showInfo) drawInfo(canvas, w, h)
    }

    private fun drawCompoundHeader(canvas: Canvas, w: Float, h: Float) {
        val c = currentCompound ?: return; val pad = 14f; val a = (animProgress * 255).toInt().coerceIn(0, 255)
        val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; textAlign = Paint.Align.LEFT; isFakeBoldText = true; color = Color.argb(a, 255, 255, 255); typeface = Typeface.MONOSPACE }
        canvas.drawText(c.name.uppercase(), pad, pad + 16f, tP)
        val fP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; textAlign = Paint.Align.LEFT; color = Color.argb(a, 0, 240, 200); typeface = Typeface.MONOSPACE }
        canvas.drawText(c.formula, pad, pad + 34f, fP)
        val iP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; textAlign = Paint.Align.LEFT; color = Color.argb(a, 120, 140, 160); typeface = Typeface.MONOSPACE }
        var ix = pad
        if (c.molecularWeight.isNotEmpty()) { canvas.drawText("${c.molecularWeight} g/mol", ix, pad + 50f, iP); ix += iP.measureText("${c.molecularWeight} g/mol") + 16f }
        if (c.category.isNotEmpty()) canvas.drawText(c.category, ix, pad + 50f, iP)
        val sep = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(30, 0, 255, 200) }
        canvas.drawLine(pad, h - 3f, w - pad, h - 3f, sep)
    }

    private fun drawMolecule(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, compound: CookbookCompound) {
        val s = compound.structure ?: return; val cx = left + w / 2f; val cy = top + h / 2f
        val scale = minOf(w, h) / 300f * animProgress; val rot = time * 0.25f; val br = 1f + sin(time * 0.6f) * 0.02f
        val alpha = (animProgress * 255).toInt().coerceIn(0, 255)
        for (bnd in s.bonds) {
            if (bnd.from >= s.atoms.size || bnd.to >= s.atoms.size) continue
            val a1 = s.atoms[bnd.from]; val a2 = s.atoms[bnd.to]
            val x1 = cx + (a1.x * cos(rot) - a1.y * sin(rot)) * scale * br
            val y1 = cy + (a1.x * sin(rot) + a1.y * cos(rot)) * scale * br
            val x2 = cx + (a2.x * cos(rot) - a2.y * sin(rot)) * scale * br
            val y2 = cy + (a2.x * sin(rot) + a2.y * cos(rot)) * scale * br
            bondPaint.strokeWidth = 2.5f; bondPaint.color = Color.argb(alpha, 0, 200, 150)
            canvas.drawLine(x1, y1, x2, y2, bondPaint)
            if (bnd.order == 2) { val dx = x2 - x1; val dy = y2 - y1; val len = sqrt(dx * dx + dy * dy); if (len > 0) { val nx = -dy / len * 4f; val ny = dx / len * 4f; bondPaint.strokeWidth = 1.5f; bondPaint.color = Color.argb(alpha / 2, 0, 200, 150); canvas.drawLine(x1 + nx, y1 + ny, x2 + nx, y2 + ny, bondPaint) } }
        }
        for (at in s.atoms) {
            val ax = cx + (at.x * cos(rot) - at.y * sin(rot)) * scale * br
            val ay = cy + (at.x * sin(rot) + at.y * cos(rot)) * scale * br
            val r = when (at.symbol) { "C" -> 11f; "O" -> 13f; "N" -> 13f; "Cl" -> 15f; "H" -> 7f; else -> 11f } * scale
            val gr = r * 2.8f
            glowPaint.shader = RadialGradient(ax, ay, gr, intArrayOf(Color.argb(alpha / 4, Color.red(at.color), Color.green(at.color), Color.blue(at.color))), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(ax, ay, gr, glowPaint); glowPaint.shader = null
            atomPaint.color = Color.argb(alpha, Color.red(at.color), Color.green(at.color), Color.blue(at.color)); canvas.drawCircle(ax, ay, r, atomPaint)
            atomPaint.color = Color.argb(alpha / 2, Color.red(at.color), Color.green(at.color), Color.blue(at.color)); canvas.drawCircle(ax, ay, r * 1.3f, atomPaint)
            if (at.symbol != "C") { atomTextPaint.textSize = (10f * scale).coerceIn(7f, 14f); atomTextPaint.color = Color.argb(alpha, 255, 255, 255); canvas.drawText(at.symbol, ax, ay + 4f * scale, atomTextPaint) }
        }
    }

    private fun drawInterferogram(canvas: Canvas, w: Float, h: Float) {
        val mL = 38f; val mR = 8f; val mT = 16f; val mB = 28f; val pL = mL; val pT = mT; val pR = w - mR; val pB = h - mB; val pW = pR - pL; val pH = pB - pT
        rect.set(pL, pT, pR, pB); boxPaint.color = darken(themeColors.bg, 0.9f); canvas.drawRoundRect(rect, 4f, 4f, boxPaint)
        linePaint.color = themeColors.line; canvas.drawRoundRect(rect, 4f, 4f, linePaint)
        spectrumPaint.color = C_GREEN; spectrumPaint.strokeWidth = 1.8f; path.reset()
        for (i in interferogramData.indices) { val x = pL + pW * i / interferogramData.size; val y = pT + pH * 0.5f - interferogramData[i] * pH * 0.45f; if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        canvas.drawPath(path, spectrumPaint)
        labelPaint.textSize = 13f; labelPaint.color = C_CYAN; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("İnterferogram", pL + pW / 2f, pT - 4f, labelPaint); labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSpectrum(canvas: Canvas, left: Float, top: Float, w: Float, h: Float) {
        val mL = 42f; val mR = 10f; val mT = 18f; val mB = 30f; val pL = left + mL; val pT = top + mT; val pR = left + w - mR; val pB = top + h - mB; val pW = pR - pL; val pH = pB - pT
        rect.set(pL, pT, pR, pB); boxPaint.color = darken(themeColors.bg, 0.85f); canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        linePaint.color = Color.argb(50, 0, 255, 200); canvas.drawRoundRect(rect, 6f, 6f, linePaint)
        gridPaint.color = Color.argb(20, 0, 255, 200); gridPaint.strokeWidth = 0.6f
        for (pct in listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)) canvas.drawLine(pL, pT + pH * pct, pR, pT + pH * pct, gridPaint)
        for (wn in listOf(4000f, 3000f, 2000f, 1000f, 500f)) { val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawLine(x, pT, x, pB, gridPaint) }
        smallLabelPaint.color = themeColors.muted; smallLabelPaint.textSize = 9f; smallLabelPaint.textAlign = Paint.Align.CENTER
        for (wn in listOf(4000f, 3000f, 2000f, 1000f, 500f)) { val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawText("${wn.toInt()}", x, pB + 13f, smallLabelPaint) }
        smallLabelPaint.textAlign = Paint.Align.RIGHT
        for (t in listOf(100, 80, 60, 40, 20, 0)) { val y = pT + pH * (1f - t / 100f); canvas.drawText("$t", pL - 6f, y + 4f, smallLabelPaint) }
        labelPaint.textSize = 9f; labelPaint.color = themeColors.muted; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Wavenumber (cm⁻¹)", pL + pW / 2f, pB + 25f, labelPaint)
        canvas.save(); canvas.rotate(-90f, pL - 30f, pT + pH / 2f); canvas.drawText("Transmittance (%T)", pL - 30f, pT + pH / 2f, labelPaint); canvas.restore()
        if (selectedGroups.isEmpty()) { labelPaint.textSize = 12f; labelPaint.color = themeColors.muted; canvas.drawText("Fonksiyonel grup seçin veya Library'den bileşik seçin", pL + pW / 2f, pT + pH / 2f, labelPaint); labelPaint.textAlign = Paint.Align.LEFT; return }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        fillPaint.shader = LinearGradient(0f, pT, 0f, pB, Color.argb(30, 0, 255, 200), Color.argb(3, 0, 255, 200), Shader.TileMode.CLAMP)
        val fp = Path(); fp.moveTo(pL, pB); var ff = true
        for (i in spectrumData.indices) { val wn = 4000f - i * (3600f / spectrumData.size); val x = pL + pW * (1f - (wn - 400f) / 3600f); val y = pT + pH * (1f - spectrumData[i]); if (x < pL || x > pR) continue; if (ff) { fp.lineTo(x, y); ff = false } else fp.lineTo(x, y) }
        fp.lineTo(pR, pB); fp.close(); canvas.drawPath(fp, fillPaint)

        spectrumPaint.color = C_CYAN; spectrumPaint.strokeWidth = 2f; path.reset(); var first = true
        for (i in spectrumData.indices) { val wn = 4000f - i * (3600f / spectrumData.size); val x = pL + pW * (1f - (wn - 400f) / 3600f); val y = pT + pH * (1f - spectrumData[i]); if (x < pL || x > pR) continue; if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y) }
        canvas.drawPath(path, spectrumPaint)

        val lp = mutableListOf<Pair<Float, Float>>(); var pIdx = 1
        for (g in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(g.id)) continue
            val x = pL + pW * (1f - (g.peakCenter - 400f) / 3600f); val idx = ((4000f - g.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1); val y = pT + pH * (1f - spectrumData[idx])
            if (x < pL || x > pR) continue
            peakLinePaint.color = colorWithAlpha(g.color, 70); peakLinePaint.pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f); canvas.drawLine(x, y, x, pT + 4f, peakLinePaint); peakLinePaint.pathEffect = null
            peakDotPaint.color = g.color; canvas.drawCircle(x, y, 5f, peakDotPaint)
            peakDotPaint.color = Color.argb(40, Color.red(g.color), Color.green(g.color), Color.blue(g.color)); canvas.drawCircle(x, y, 11f, peakDotPaint)
            val nw = peakNumPaint.measureText("$pIdx") + 10f; var ly = pT + 4f; var tries = 0
            while (tries < 15) { if (!lp.any { abs(it.first - x) < nw * 0.9f && abs(it.second - ly) < 22f }) break; ly += 22f; tries++ }
            lp.add(Pair(x, ly)); peakBgPaint.color = Color.argb(220, 10, 14, 20); labelBgRect.set(x - nw / 2f, ly, x + nw / 2f, ly + 20f); canvas.drawRoundRect(labelBgRect, 4f, 4f, peakBgPaint)
            peakNumPaint.textSize = 12f; canvas.drawText("$pIdx", x, ly + 14f, peakNumPaint); pIdx++
        }
        if (pIdx > 1) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; typeface = Typeface.MONOSPACE; textAlign = Paint.Align.LEFT }
            var tx = pL; var pn = 1; val ty = top + h - 4f
            for (g in FUNCTIONAL_GROUPS) { if (!selectedGroups.contains(g.id)) continue; tp.color = g.color; val txt = "$pn. ${g.nameTr} ${g.peakCenter.toInt()}"; canvas.drawText(txt, tx, ty, tp); tx += tp.measureText(txt) + 14f; if (tx > pR - 40f) { tx = pL }; pn++ }
        }
        if (showCursor && cursorX >= pL && cursorX <= pR && cursorY >= pT && cursorY <= pB) {
            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(50, 255, 255, 255); pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f) }
            canvas.drawLine(cursorX, pT, cursorX, pB, cp); canvas.drawLine(pL, cursorY, pR, cursorY, cp)
            val cWn = 4000f - (cursorX - pL) / pW * 3600f; val cT = 100f - (cursorY - pT) / pH * 100f
            val ibg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 10, 14, 20) }
            canvas.drawRoundRect(pL + 4f, pB + 14f, pR - 4f, pB + 28f, 4f, 4f, ibg)
            smallLabelPaint.textSize = 9f; smallLabelPaint.color = C_CYAN; smallLabelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("WN: ${"%.0f".format(cWn)} cm⁻¹  |  T: ${"%.1f".format(cT)}%", pL + pW / 2, pB + 25f, smallLabelPaint)
        }
        labelPaint.textSize = 11f; labelPaint.color = C_CYAN; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("FT-IR SPEKTRUMU", pL + pW / 2f, top + 13f, labelPaint); labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawInfo(c: Canvas, w: Float, h: Float) {
        val px = w * 0.03f; val py = 8f; val pw = w * 0.94f; val ph = h - 16f
        c.drawRoundRect(px, py, px + pw, py + ph, 16f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(10, 14, 20); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 16f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = C_CYAN; isAntiAlias = true })
        var ty = py + 36f; val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; textAlign = Paint.Align.CENTER; color = C_CYAN; isFakeBoldText = true; typeface = Typeface.MONOSPACE }
        c.drawText("FT-IR SIMULATOR", w / 2f, ty, hp); ty += 32f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; textAlign = Paint.Align.LEFT; isAntiAlias = true }
        for ((l, cl) in listOf("═══ WHAT IS IT? ═══" to C_CYAN, "Fourier Transform Infrared Spectroscopy" to Color.WHITE, "Identifies functional groups." to Color.WHITE, "" to Color.TRANSPARENT, "═══ USAGE ═══" to C_CYAN, "1. Select groups or Library compound" to Color.rgb(200, 230, 255), "2. Press SCAN" to Color.rgb(200, 230, 255), "3. Pinch to zoom" to Color.rgb(200, 230, 255), "4. Touch to see WN/T" to Color.rgb(200, 230, 255))) { if (l.isEmpty()) { ty += 4f; continue }; lp.color = cl; c.drawText(l, px + 14f, ty, lp); ty += 20f }
    }

    private fun colorWithAlpha(c: Int, a: Int): Int = Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
    private fun darken(c: Int, f: Float): Int = Color.rgb((Color.red(c) * f).toInt().coerceIn(0, 255), (Color.green(c) * f).toInt().coerceIn(0, 255), (Color.blue(c) * f).toInt().coerceIn(0, 255))
}
