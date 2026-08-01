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
        val molecularWeight: String = "",
        val category: String = "",
        val structure: MoleculeStructure? = null
    )

    data class ThemeColors(
        val bg: Int = Color.rgb(10, 14, 20),
        val surface: Int = Color.rgb(18, 24, 32),
        val primary: Int = Color.rgb(0, 240, 200),
        val text: Int = Color.rgb(220, 230, 240),
        val muted: Int = Color.rgb(120, 140, 160),
        val accent: Int = Color.rgb(57, 255, 20),
        val line: Int = Color.rgb(30, 40, 50)
    )

    companion object {
        val C_GREEN = Color.rgb(0, 220, 160)
        val C_CYAN = Color.rgb(0, 240, 200)
        val C_O = Color.rgb(255, 80, 80)
        val C_N = Color.rgb(80, 160, 255)
        val C_H = Color.rgb(180, 190, 200)
        val C_BOND = Color.rgb(0, 200, 150)

        fun eth(): MoleculeStructure = MoleculeStructure(
            listOf(
                Atom("C", -30f, 0f, C_GREEN), Atom("C", 30f, 0f, C_GREEN),
                Atom("O", 90f, 0f, C_O), Atom("H", 125f, -25f, C_H),
                Atom("H", -30f, -35f, C_H), Atom("H", -65f, 18f, C_H),
                Atom("H", -30f, 35f, C_H), Atom("H", 30f, -35f, C_H), Atom("H", 30f, 35f, C_H)
            ),
            listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(0, 4), Bond(0, 5), Bond(0, 6), Bond(1, 7), Bond(1, 8))
        )
        fun met(): MoleculeStructure = MoleculeStructure(
            listOf(
                Atom("C", 0f, 0f, C_GREEN), Atom("O", 50f, 0f, C_O),
                Atom("H", 85f, -22f, C_H), Atom("H", 0f, -36f, C_H),
                Atom("H", -32f, 18f, C_H), Atom("H", 0f, 36f, C_H)
            ),
            listOf(Bond(0, 1), Bond(1, 2), Bond(0, 3), Bond(0, 4), Bond(0, 5))
        )
        fun ace(): MoleculeStructure = MoleculeStructure(
            listOf(
                Atom("C", 0f, 0f, C_GREEN), Atom("C", -45f, 28f, C_GREEN),
                Atom("C", 45f, 28f, C_GREEN), Atom("O", 0f, -45f, C_O),
                Atom("H", -45f, 62f, C_H), Atom("H", -80f, 10f, C_H),
                Atom("H", -45f, -8f, C_H), Atom("H", 45f, 62f, C_H),
                Atom("H", 80f, 10f, C_H), Atom("H", 45f, -8f, C_H)
            ),
            listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3, 2), Bond(1, 4), Bond(1, 5), Bond(1, 6), Bond(2, 7), Bond(2, 8), Bond(2, 9))
        )
        fun benz(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", (cos(ang) * 42).toFloat(), (sin(ang) * 42).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 76).toFloat(), (sin(ang) * 76).toFloat(), C_H))
                b.add(Bond(i, i + 6))
            }
            return MoleculeStructure(a, b)
        }
        fun aceAcid(): MoleculeStructure = MoleculeStructure(
            listOf(
                Atom("C", -28f, 0f, C_GREEN), Atom("C", 28f, 0f, C_GREEN),
                Atom("O", 28f, -45f, C_O), Atom("O", 82f, 18f, C_O),
                Atom("H", 115f, 0f, C_H), Atom("H", -28f, -35f, C_H),
                Atom("H", -60f, 18f, C_H), Atom("H", -28f, 35f, C_H)
            ),
            listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(0, 5), Bond(0, 6), Bond(0, 7))
        )
        fun tol(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", (cos(ang) * 38).toFloat(), (sin(ang) * 38).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            a.add(Atom("C", -68f, 0f, C_GREEN)); b.add(Bond(0, 6))
            a.add(Atom("H", -95f, -22f, C_H)); b.add(Bond(6, 7))
            a.add(Atom("H", -95f, 22f, C_H)); b.add(Bond(6, 8))
            a.add(Atom("H", -68f, 38f, C_H)); b.add(Bond(6, 9))
            for (i in 1 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 68).toFloat(), (sin(ang) * 68).toFloat(), C_H))
                b.add(Bond(i, a.size - 1))
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
                val x = (i - 2.5f) * 38f; val y1 = if (i % 2 == 0) -48f else 48f
                a.add(Atom("H", x, y1, C_H)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun phenMol(): MoleculeStructure {
            val a = mutableListOf<Atom>(); val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("C", (cos(ang) * 38).toFloat(), (sin(ang) * 38).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            a.add(Atom("O", 0f, -75f, C_O)); b.add(Bond(0, 6))
            a.add(Atom("H", 0f, -108f, C_H)); b.add(Bond(6, 7))
            for (i in 1 until 6) {
                val ang = Math.toRadians(60.0 * i - 90.0)
                a.add(Atom("H", (cos(ang) * 68).toFloat(), (sin(ang) * 68).toFloat(), C_H))
                b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }

        val FUNCTIONAL_GROUPS = listOf(
            FunctionalGroup("oh_alcohol", "O-H (Alkol)", "Hidroksil", 3200f, 3600f, 3350f, 0.85f, 180f, Color.rgb(255, 80, 80), "Geniş", "H-bağlama nedeniyle geniş pik. Alkol ve fenol için karakteristiktir.", "Etanol, Metanol"),
            FunctionalGroup("oh_acid", "O-H (Asit)", "Karboksilik Asit", 2500f, 3300f, 3000f, 0.92f, 400f, Color.rgb(255, 60, 60), "Çok Geniş", "Çok geniş, C-H ile örtüşür.", "Asetik Asit"),
            FunctionalGroup("nh_primary", "N-H (1° Amin)", "Birincil Amin", 3250f, 3500f, 3400f, 0.55f, 80f, Color.rgb(80, 180, 255), "Çift Pik", "İkiz pik. Birincil amin belirtisi.", "Anilin"),
            FunctionalGroup("nh_secondary", "N-H (2° Amin)", "İkincil Amin", 3310f, 3350f, 3330f, 0.4f, 50f, Color.rgb(100, 200, 255), "Tek Pik", "Tek zayıf pik. İkincil amin.", "Dietilamin"),
            FunctionalGroup("nh_amide", "N-H (Amid)", "Amid N-H", 3180f, 3350f, 3280f, 0.5f, 100f, Color.rgb(120, 160, 255), "Geniş", "Amid I ve II bantları ile görülür.", "Asetamid"),
            FunctionalGroup("ch_alkane", "C-H (Alkan)", "Alkan C-H", 2845f, 2970f, 2920f, 0.7f, 50f, Color.rgb(200, 200, 80), "Keskin", "Güçlü, keskin pik. sp³ C-H gerilmesi.", "Hekzan"),
            FunctionalGroup("ch_alkene", "C-H (Alken)", "Alken =C-H", 3020f, 3100f, 3080f, 0.45f, 40f, Color.rgb(100, 220, 100), "Orta", "sp² C-H gerilmesi. Alken belirtisi.", "1-Heksen"),
            FunctionalGroup("ch_aro", "C-H (Aromatik)", "Aromatik C-H", 3000f, 3100f, 3050f, 0.4f, 35f, Color.rgb(180, 140, 255), "Zayıf", "Aromatik halka C-H gerilmesi.", "Benzen"),
            FunctionalGroup("ch_aldehyde", "C-H (Aldehit)", "Aldehit C-H", 2720f, 2830f, 2780f, 0.35f, 40f, Color.rgb(200, 180, 100), "Çift Pik", "Fermi çift pik. Aldehit belirleyici.", "Benzaldehit"),
            FunctionalGroup("ch_aldehyde2", "C-H (Aldehit 2)", "Fermi Çift", 2720f, 2820f, 2720f, 0.3f, 30f, Color.rgb(220, 190, 90), "Keskin", "Fermi rezonans çifti alt pik.", "Benzaldehit"),
            FunctionalGroup("ch_alkyne", "≡C-H (Alkin)", "Terminal Alkin", 3260f, 3330f, 3300f, 0.7f, 30f, Color.rgb(255, 220, 80), "Keskin", "Güçlü, keskin pik.", "Asetilen"),
            FunctionalGroup("co_ketone", "C=O (Keton)", "Keton Karbonil", 1705f, 1725f, 1715f, 0.95f, 35f, Color.rgb(255, 200, 50), "Keskin", "Çok güçlü pik. Keton belirleyici.", "Aseton"),
            FunctionalGroup("co_aldehyde", "C=O (Aldehit)", "Aldehit C=O", 1720f, 1740f, 1730f, 0.9f, 30f, Color.rgb(255, 180, 80), "Keskin", "Güçlü pik. C-H ile aldehiti doğrular.", "Benzaldehit"),
            FunctionalGroup("co_ester", "C=O (Ester)", "Ester Karbonil", 1735f, 1750f, 1740f, 0.88f, 30f, Color.rgb(255, 160, 100), "Keskin", "Güçlü pik. C-O ile ester doğrulanır.", "Etil Asetat"),
            FunctionalGroup("co_acid", "C=O (Asit)", "Asit C=O", 1700f, 1725f, 1710f, 0.92f, 35f, Color.rgb(255, 140, 70), "Keskin", "Güçlü pik. O-H ile asit doğrulanır.", "Asetik Asit"),
            FunctionalGroup("co_amide1", "C=O (Amid I)", "Amid I", 1630f, 1690f, 1660f, 0.85f, 40f, Color.rgb(220, 180, 255), "Keskin", "Amid I bandı (C=O gerilmesi).", "Asetamid"),
            FunctionalGroup("cc_alkene", "C=C (Alken)", "Alken C=C", 1620f, 1680f, 1650f, 0.35f, 35f, Color.rgb(80, 255, 180), "Zayıf", "Simetrik olmayan alkenlerde görünür.", "1-Heksen"),
            FunctionalGroup("cc_aro", "C=C (Aromatik)", "Aromatik C=C", 1450f, 1615f, 1500f, 0.45f, 60f, Color.rgb(160, 140, 220), "Orta", "Çoklu pikler. Aromatik halka belirtisi.", "Benzen"),
            FunctionalGroup("cc_alkyne", "C≡C (Alkin)", "Alkin C≡C", 2100f, 2260f, 2150f, 0.3f, 25f, Color.rgb(255, 255, 100), "Zayıf", "Zayıf pik.", "Asetilen"),
            FunctionalGroup("cn_nitrile", "C≡N (Nitril)", "Nitril C≡N", 2210f, 2260f, 2250f, 0.5f, 25f, Color.rgb(150, 255, 150), "Orta", "Karakteristik pik.", "Asetonitril"),
            FunctionalGroup("no2", "NO₂ (Nitro)", "Nitro", 1515f, 1570f, 1540f, 0.8f, 35f, Color.rgb(255, 80, 180), "Güçlü", "Güçlü pik. NO₂ gerilmesi.", "Nitrobenzen"),
            FunctionalGroup("co_alcohol", "C-O (Alkol)", "C-O Gerilmesi", 1040f, 1175f, 1100f, 0.65f, 80f, Color.rgb(255, 150, 100), "Güçlü", "Alkol, ester ve eterlerde.", "Etanol"),
            FunctionalGroup("co_ester_coc", "C-O-C (Ester)", "Ester C-O-C", 1150f, 1300f, 1240f, 0.75f, 70f, Color.rgb(255, 130, 80), "Güçlü", "Ester C-O gerilmesi.", "Etil Asetat"),
            FunctionalGroup("nh_bend", "N-H Bükülme", "Amid II", 1510f, 1570f, 1540f, 0.6f, 35f, Color.rgb(140, 180, 255), "Orta", "Amid II bandı.", "Asetamid"),
            FunctionalGroup("c_cl", "C-Cl (Klor)", "Kloro", 550f, 850f, 700f, 0.7f, 100f, Color.rgb(180, 220, 180), "Güçlü", "Halojenli bileşikler.", "Kloroform")
        )

        val COOKBOOK_COMPOUNDS = listOf(
            CookbookCompound("Etanol", "C₂H₅OH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1100)", "46.07", "Alkol", eth()),
            CookbookCompound("Metanol", "CH₃OH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1050)", "32.04", "Alkol", met()),
            CookbookCompound("İzopropanol", "(CH₃)₂CHOH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1150)", "60.10", "Alkol"),
            CookbookCompound("1-Butanol", "C₄H₉OH", listOf("oh_alcohol", "ch_alkane", "co_alcohol"), "O-H (3350), C-H (2920), C-O (1070)", "74.12", "Alkol"),
            CookbookCompound("Fenol", "C₆H₅OH", listOf("oh_alcohol", "ch_aro", "cc_aro", "co_alcohol"), "O-H (3350), Aromatik (3050), C=C (1500), C-O (1200)", "94.11", "Aromatik", phenMol()),
            CookbookCompound("Gliserol", "C₃H₈O₃", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), "3x O-H (3350), C-O (1100)", "92.09", "Alkol"),
            CookbookCompound("Aseton", "CH₃COCH₃", listOf("co_ketone", "ch_alkane"), "C=O (1715), C-H (2920)", "58.08", "Keton", ace()),
            CookbookCompound("2-Butanon", "CH₃COC₂H₅", listOf("co_ketone", "ch_alkane"), "C=O (1715), C-H (2920)", "72.11", "Keton"),
            CookbookCompound("Sikloheksanon", "C₆H₁₀O", listOf("co_ketone", "ch_alkane"), "C=O (1715), C-H (2920)", "98.14", "Keton"),
            CookbookCompound("Asetofenon", "C₆H₅COCH₃", listOf("co_ketone", "ch_aro", "cc_aro", "ch_alkane"), "C=O (1715), Aromatik (1500), C-H (2920)", "120.15", "Keton"),
            CookbookCompound("Benzaldehit", "C₆H₅CHO", listOf("co_aldehyde", "ch_aldehyde", "ch_aro", "cc_aro"), "C=O (1730), Fermi çift (2720,2820), Aromatik", "106.12", "Aldehit"),
            CookbookCompound("Asetaldehit", "CH₃CHO", listOf("co_aldehyde", "ch_aldehyde", "ch_alkane"), "C=O (1730), Fermi çift (2720,2820)", "44.05", "Aldehit"),
            CookbookCompound("Formaldehit", "HCHO", listOf("co_aldehyde"), "C=O (1745)", "30.03", "Aldehit"),
            CookbookCompound("Asetik Asit", "CH₃COOH", listOf("oh_acid", "co_acid", "co_alcohol"), "O-H (2500-3300), C=O (1710), C-O (1240)", "60.05", "Asit", aceAcid()),
            CookbookCompound("Propiyonik Asit", "C₂H₅COOH", listOf("oh_acid", "co_acid", "ch_alkane", "co_alcohol"), "O-H, C=O (1710), C-H (2920)", "74.08", "Asit"),
            CookbookCompound("Benzoik Asit", "C₆H₅COOH", listOf("oh_acid", "co_acid", "ch_aro", "cc_aro"), "O-H, C=O (1690), Aromatik C=C (1500)", "122.12", "Asit"),
            CookbookCompound("Etil Asetat", "CH₃COOC₂H₅", listOf("co_ester", "co_ester_coc", "ch_alkane"), "C=O (1740), C-O-C (1240), C-H (2920)", "88.11", "Ester"),
            CookbookCompound("Metil Benzoat", "C₆H₅COOCH₃", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro"), "C=O (1724), C-O-C (1275), Aromatik", "136.15", "Ester"),
            CookbookCompound("Etil Benzoat", "C₆H₅COOC₂H₅", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"), "C=O (1720), C-O-C (1270)", "150.17", "Ester"),
            CookbookCompound("Benzen", "C₆H₆", listOf("ch_aro", "cc_aro"), "Aromatik C-H (3050), C=C (1500, 1600)", "78.11", "Aromatik", benz()),
            CookbookCompound("Toluen", "C₆H₅CH₃", listOf("ch_aro", "cc_aro", "ch_alkane"), "Aromatik (3050), C=C (1500), CH₃ (2920)", "92.14", "Aromatik", tol()),
            CookbookCompound("Hekzan", "C₆H₁₄", listOf("ch_alkane"), "C-H (2920, 2850)", "86.18", "Alkan", hexMol()),
            CookbookCompound("Sikloheksan", "C₆H₁₂", listOf("ch_alkane"), "C-H (2920, 2850)", "84.16", "Alkan"),
            CookbookCompound("1-Heksen", "C₆H₁₂", listOf("ch_alkane", "ch_alkene", "cc_alkene"), "C-H (2920), =C-H (3080), C=C (1650)", "84.16", "Alken"),
            CookbookCompound("Stiren", "C₆H₅CH=CH₂", listOf("ch_aro", "cc_aro", "ch_alkene", "cc_alkene"), "Aromatik + Alken C-H, C=C (1630)", "104.15", "Alken"),
            CookbookCompound("Anilin", "C₆H₅NH₂", listOf("nh_primary", "ch_aro", "cc_aro"), "N-H çift (3400), Aromatik (3050)", "93.13", "Amin"),
            CookbookCompound("Dietilamin", "(C₂H₅)₂NH", listOf("nh_secondary", "ch_alkane"), "N-H tek (3330), C-H (2920)", "73.14", "Amin"),
            CookbookCompound("Asetonitril", "CH₃CN", listOf("cn_nitrile", "ch_alkane"), "C≡N (2250), C-H (2920)", "41.05", "Nitril"),
            CookbookCompound("Benzonitril", "C₆H₅CN", listOf("cn_nitrile", "ch_aro", "cc_aro"), "C≡N (2230), Aromatik (3050)", "103.12", "Nitril"),
            CookbookCompound("Kloroform", "CHCl₃", listOf("c_cl", "ch_alkane"), "C-Cl (760), C-H (3020)", "119.38", "Halojen"),
            CookbookCompound("Diklorometan", "CH₂Cl₂", listOf("c_cl", "ch_alkane"), "C-Cl (700), C-H (2920)", "84.93", "Halojen"),
            CookbookCompound("Karbon Tetraklorür", "CCl₄", listOf("c_cl"), "C-Cl (780, 820)", "153.82", "Halojen"),
            CookbookCompound("Asetamid", "CH₃CONH₂", listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"), "N-H (3300), Amid I (1660), Amid II (1540)", "59.07", "Amid"),
            CookbookCompound("Naylon 6,6", "(C₁₂H₂₂N₂O₂)ₙ", listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"), "N-H (3300), Amid I (1660), Amid II (1540)", "226.32", "Polimer"),
            CookbookCompound("Nitrobenzen", "C₆H₅NO₂", listOf("no2", "ch_aro", "cc_aro"), "NO₂ (1540), Aromatik (3050)", "123.11", "Nitro"),
            CookbookCompound("Dietil Eter", "(C₂H₅)₂O", listOf("ch_alkane", "co_alcohol"), "C-H (2920), C-O (1120)", "74.12", "Eter"),
            CookbookCompound("THF", "C₄H₈O", listOf("ch_alkane", "co_alcohol"), "C-H (2920), C-O (1070)", "72.11", "Eter"),
            CookbookCompound("Difenil Metan", "(C₆H₅)₂CH₂", listOf("ch_aro", "cc_aro", "ch_alkane"), "Aromatik (3050), C=C (1500)", "166.22", "Aromatik"),
            CookbookCompound("Kağıt (Selüloz)", "(C₆H₁₀O₅)ₙ", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), "O-H (3350), C-O (1050), C-H (2920)", "—", "Polimer"),
            CookbookCompound("PET Plastik", "(C₁₀H₈O₄)ₙ", listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"), "C=O (1720), C-O-C (1240), Aromatik", "—", "Polimer"),
            CookbookCompound("Polietilen", "(C₂H₄)ₙ", listOf("ch_alkane"), "C-H (2920, 2850, 1470)", "—", "Polimer"),
            CookbookCompound("Polistiren", "(C₈H₈)ₙ", listOf("ch_aro", "cc_aro", "ch_alkane"), "Aromatik (3025), C=C (1600)", "—", "Polimer"),
            CookbookCompound("Şeker (Sucroz)", "C₁₂H₂₂O₁₁", listOf("oh_alcohol", "co_alcohol", "ch_alkane"), "O-H (3400), C-O (1000-1100)", "342.30", "Karbonhidrat"),
            CookbookCompound("Yağ (Trigliserit)", "C₅₅H₉₈O₆", listOf("ch_alkane", "co_ester"), "C-H (2920), C=O (1745)", "—", "Lipit"),
            CookbookCompound("E Vitamini", "C₂₉H₅₀O₂", listOf("oh_alcohol", "ch_alkane", "ch_aro", "cc_aro"), "O-H (3400), C-H (2920), Aromatik", "430.71", "Vitamin")
        )

        val SAMPLE_TYPES = listOf(
            "KBr Pellet" to "Katı numuneler için KBr ile sıkıştırma",
            "İnce Film" to "Sıvı numuneler için NaCl plakları arasında",
            "ATR" to "Tam yansıma tekniği",
            "Çözelti" to "Kuvvetli çözücüde seyreltme"
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
                val ns = (zoomScale * d.scaleFactor).coerceIn(0.5f, 4f)
                val sc = ns / zoomScale
                panX = d.focusX - (d.focusX - panX) * sc
                panY = d.focusY - (d.focusY - panY) * sc
                zoomScale = ns; invalidate(); return true
            }
        })
        setOnTouchListener { _, e ->
            sDetector.onTouchEvent(e)
            if (e.pointerCount == 1) when (e.action) {
                0 -> { lastTx = e.x; lastTy = e.y; touchMode = 0; tapTime = System.currentTimeMillis() }
                2 -> {
                    val dx = e.x - lastTx; val dy = e.y - lastTy
                    if (abs(dx) > 5 || abs(dy) > 5) touchMode = 1
                    if (touchMode == 1) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y }
                }
                1, 3 -> {
                    if (touchMode == 0) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) { zoomScale = 1f; panX = 0f; panY = 0f; invalidate() }
                        lastTapTime = now
                    }
                    touchMode = 0; showCursor = false
                }
            }
            if (e.action == MotionEvent.ACTION_MOVE && touchMode == 0) {
                cursorX = e.x; cursorY = e.y; showCursor = true
            }
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

    fun toggleGroup(groupId: String) {
        if (selectedGroups.contains(groupId)) selectedGroups.remove(groupId) else selectedGroups.add(groupId)
        generateSpectrum(); invalidate()
    }
    fun selectPreset(compound: CookbookCompound) {
        selectedGroups.clear(); selectedGroups.addAll(compound.groups)
        currentCompound = compound; animProgress = 0f; scanLineX = 0f; scanLineActive = true
        generateSpectrum(); invalidate()
    }
    fun setResolution(res: Int) { resolution = res.coerceIn(1, 8); generateSpectrum() }
    fun setScanCount(count: Int) { scanCount = count.coerceIn(1, 128) }
    fun setSampleType(type: String) { sampleType = type }
    fun toggleInfo() { showInfo = !showInfo; invalidate() }
    fun startScan() { isScanning = true; scanProgress = 0f; generateSpectrum(); invalidate() }
    fun setThemeColors(colors: ThemeColors) { themeColors = colors; bgPaint.color = colors.bg; generateSpectrum(); invalidate() }

    private fun updateData() {
        if (isScanning) {
            scanProgress += 0.004f
            if (scanProgress >= 1f) { scanProgress = 1f; isScanning = false; generateSpectrum() }
        }
        val zpd = interferogramData.size / 2
        for (i in interferogramData.indices) {
            val opd = (i - zpd).toFloat()
            val env = exp(-(opd * opd) / (3000f + scanCount * 50f))
            interferogramData[i] = env * cos(opd * 0.08f + time * 2f) + env * 0.4f * cos(opd * 0.12f + time * 1.3f) + (Math.random().toFloat() - 0.5f) * 0.01f
        }
    }

    private fun generateSpectrum() {
        for (i in spectrumData.indices) spectrumData[i] = 0.5f
        val nl = 0.15f + (8 - resolution) * 0.1f
        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            spectrumData[i] += 0.5f * sin(wn / 4000f * PI.toFloat()) * 0.02f + 0.015f * sin(wn / 800f * PI.toFloat()) * sin(wn / 1200f * PI.toFloat())
            spectrumData[i] -= 0.12f * exp(-((wn - 2349f).pow(2)) / 450f) - 0.1f * exp(-((wn - 2361f).pow(2)) / 450f)
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
        for (i in spectrumData.indices) {
            spectrumData[i] += (Math.random().toFloat() - 0.5f) * nl * 0.012f
            spectrumData[i] = spectrumData[i].coerceIn(0.02f, 0.98f)
        }
    }

    fun getPeakTable(): List<Triple<String, Float, Float>> {
        val p = mutableListOf<Triple<String, Float, Float>>()
        for (g in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(g.id)) continue
            val idx = ((4000f - g.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1)
            p.add(Triple(g.name, g.peakCenter, spectrumData[idx] * 100f))
        }
        return p.sortedByDescending { it.second }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); handler.post(ticker) }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacksAndMessages(null) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        drawGrid(w, h, canvas)

        canvas.save()
        canvas.scale(zoomScale, zoomScale, w / 2, h / 2)
        canvas.translate(panX / zoomScale, panY / zoomScale)

        if (showInterferogram) {
            drawInterferogram(canvas, w, h)
        } else {
            val cmp = currentCompound
            if (cmp != null && cmp.structure != null) {
                drawCompoundHeader(canvas, w, h * 0.13f)
                drawMolecule(canvas, 0f, h * 0.13f, w, h * 0.30f, cmp)
                drawSpectrum(canvas, 0f, h * 0.43f, w, h * 0.57f)
            } else if (cmp != null) {
                drawCompoundHeader(canvas, w, h * 0.09f)
                drawSpectrum(canvas, 0f, h * 0.10f, w, h * 0.90f)
            } else {
                drawSpectrum(canvas, 0f, 0f, w, h)
            }
        }
        canvas.restore()

        if (scanLineActive) {
            val x = w * scanLineX
            scanLinePaint.color = Color.argb(200, 0, 255, 200)
            canvas.drawLine(x, 0f, x, h, scanLinePaint)
            val g = LinearGradient(x - 50f, 0f, x, 0f, Color.TRANSPARENT, Color.argb(40, 0, 255, 200), Shader.TileMode.CLAMP)
            val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = g }
            canvas.drawRect(x - 50f, 0f, x, h, gp); gp.shader = null
        }
        if (showInfo) drawInfo(canvas, w, h)
    }

    private fun drawGrid(w: Float, h: Float, canvas: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.5f; color = Color.argb(12, 0, 255, 200) }
        val step = 30f
        var x = 0f; while (x < w) { canvas.drawLine(x, 0f, x, h, p); x += step }
        var y = 0f; while (y < h) { canvas.drawLine(0f, y, w, y, p); y += step }
    }

    private fun drawCompoundHeader(canvas: Canvas, w: Float, h: Float) {
        val c = currentCompound ?: return
        val pad = 14f
        val alpha = (animProgress * 255).toInt().coerceIn(0, 255)

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; textAlign = Paint.Align.LEFT; isFakeBoldText = true; color = Color.argb(alpha, 255, 255, 255); typeface = Typeface.MONOSPACE }
        canvas.drawText(c.name.uppercase(), pad, pad + 16f, titleP)

        val formulaP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; textAlign = Paint.Align.LEFT; color = Color.argb(alpha, 0, 240, 200); typeface = Typeface.MONOSPACE }
        canvas.drawText(c.formula, pad, pad + 34f, formulaP)

        val infoP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; textAlign = Paint.Align.LEFT; color = Color.argb(alpha, 120, 140, 160); typeface = Typeface.MONOSPACE }
        var ix = pad
        if (c.molecularWeight.isNotEmpty()) { canvas.drawText("${c.molecularWeight} g/mol", ix, pad + 50f, infoP); ix += infoP.measureText("${c.molecularWeight} g/mol") + 16f }
        if (c.category.isNotEmpty()) { canvas.drawText(c.category, ix, pad + 50f, infoP) }

        val sep = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(30, 0, 255, 200) }
        canvas.drawLine(pad, h - 3f, w - pad, h - 3f, sep)
    }

    private fun drawMolecule(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, compound: CookbookCompound) {
        val s = compound.structure ?: return
        val cx = left + w / 2f; val cy = top + h / 2f
        val scale = minOf(w, h) / 300f * animProgress
        val rot = time * 0.25f; val br = 1f + sin(time * 0.6f) * 0.02f
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
            if (bnd.order == 2) {
                val dx = x2 - x1; val dy = y2 - y1; val len = sqrt(dx * dx + dy * dy)
                if (len > 0) { val nx = -dy / len * 4f; val ny = dx / len * 4f
                    bondPaint.strokeWidth = 1.5f; bondPaint.color = Color.argb(alpha / 2, 0, 200, 150)
                    canvas.drawLine(x1 + nx, y1 + ny, x2 + nx, y2 + ny, bondPaint) }
            }
        }

        for (at in s.atoms) {
            val ax = cx + (at.x * cos(rot) - at.y * sin(rot)) * scale * br
            val ay = cy + (at.x * sin(rot) + at.y * cos(rot)) * scale * br
            val r = when (at.symbol) { "C" -> 11f; "O" -> 13f; "N" -> 13f; "H" -> 7f; "Cl" -> 15f; else -> 11f } * scale

            val gr = r * 2.8f
            glowPaint.shader = RadialGradient(ax, ay, gr, intArrayOf(Color.argb(alpha / 4, Color.red(at.color), Color.green(at.color), Color.blue(at.color))), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(ax, ay, gr, glowPaint); glowPaint.shader = null

            atomPaint.color = Color.argb(alpha, Color.red(at.color), Color.green(at.color), Color.blue(at.color))
            canvas.drawCircle(ax, ay, r, atomPaint)
            atomPaint.color = Color.argb(alpha / 2, Color.red(at.color), Color.green(at.color), Color.blue(at.color))
            canvas.drawCircle(ax, ay, r * 1.3f, atomPaint)

            if (at.symbol != "C") {
                atomTextPaint.textSize = (10f * scale).coerceIn(7f, 14f)
                atomTextPaint.color = Color.argb(alpha, 255, 255, 255)
                canvas.drawText(at.symbol, ax, ay + 4f * scale, atomTextPaint)
            }
        }
    }

    private fun drawInterferogram(canvas: Canvas, w: Float, h: Float) {
        val mL = 38f; val mR = 8f; val mT = 16f; val mB = 28f
        val pL = mL; val pT = mT; val pR = w - mR; val pB = h - mB
        val pW = pR - pL; val pH = pB - pT

        rect.set(pL, pT, pR, pB); boxPaint.color = darken(themeColors.bg, 0.9f); canvas.drawRoundRect(rect, 4f, 4f, boxPaint)
        linePaint.color = themeColors.line; linePaint.strokeWidth = 1f; canvas.drawRoundRect(rect, 4f, 4f, linePaint)

        gridPaint.color = colorWithAlpha(themeColors.line, 50); gridPaint.strokeWidth = 0.8f
        canvas.drawLine(pL, pT + pH * 0.5f, pR, pT + pH * 0.5f, gridPaint)
        canvas.drawLine(pL + pW * 0.5f, pT, pL + pW * 0.5f, pB, gridPaint)

        spectrumPaint.color = C_GREEN; spectrumPaint.strokeWidth = 1.8f
        path.reset()
        for (i in interferogramData.indices) {
            val x = pL + pW * i / interferogramData.size; val y = pT + pH * 0.5f - interferogramData[i] * pH * 0.45f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, spectrumPaint)

        labelPaint.textSize = 13f; labelPaint.color = C_CYAN; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("İnterferogram", pL + pW / 2f, pT - 4f, labelPaint); labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSpectrum(canvas: Canvas, left: Float, top: Float, w: Float, h: Float) {
        val mL = 42f; val mR = 10f; val mT = 18f; val mB = 30f
        val pL = left + mL; val pT = top + mT; val pR = left + w - mR; val pB = top + h - mB
        val pW = pR - pL; val pH = pB - pT

        rect.set(pL, pT, pR, pB); boxPaint.color = darken(themeColors.bg, 0.85f); canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        linePaint.color = Color.argb(50, 0, 255, 200); linePaint.strokeWidth = 1f; canvas.drawRoundRect(rect, 6f, 6f, linePaint)

        gridPaint.color = Color.argb(20, 0, 255, 200); gridPaint.strokeWidth = 0.6f
        for (pct in listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)) canvas.drawLine(pL, pT + pH * pct, pR, pT + pH * pct, gridPaint)
        for (wn in listOf(4000f, 3500f, 3000f, 2500f, 2000f, 1500f, 1000f, 500f)) {
            val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawLine(x, pT, x, pB, gridPaint)
        }

        smallLabelPaint.color = themeColors.muted; smallLabelPaint.textSize = 9f; smallLabelPaint.textAlign = Paint.Align.CENTER
        for (wn in listOf(4000f, 3000f, 2000f, 1000f, 500f)) {
            val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawText("${wn.toInt()}", x, pB + 13f, smallLabelPaint)
        }
        smallLabelPaint.textAlign = Paint.Align.RIGHT
        for (t in listOf(100, 80, 60, 40, 20, 0)) {
            val y = pT + pH * (1f - t / 100f); canvas.drawText("$t", pL - 6f, y + 4f, smallLabelPaint)
        }

        labelPaint.textSize = 9f; labelPaint.color = themeColors.muted; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Wavenumber (cm⁻¹)", pL + pW / 2f, pB + 25f, labelPaint)
        canvas.save(); canvas.rotate(-90f, pL - 30f, pT + pH / 2f)
        canvas.drawText("Transmittance (%T)", pL - 30f, pT + pH / 2f, labelPaint); canvas.restore()

        if (selectedGroups.isEmpty()) {
            labelPaint.textSize = 12f; labelPaint.color = themeColors.muted; labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Fonksiyonel grup seçin veya Library'den bileşik seçin", pL + pW / 2f, pT + pH / 2f, labelPaint)
            labelPaint.textAlign = Paint.Align.LEFT; return
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        fillPaint.shader = LinearGradient(0f, pT, 0f, pB, Color.argb(30, 0, 255, 200), Color.argb(3, 0, 255, 200), Shader.TileMode.CLAMP)
        val fp = Path(); fp.moveTo(pL, pB)
        var ff = true
        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            val x = pL + pW * (1f - (wn - 400f) / 3600f); val y = pT + pH * (1f - spectrumData[i])
            if (x < pL || x > pR) continue
            if (ff) { fp.lineTo(x, y); ff = false } else fp.lineTo(x, y)
        }
        fp.lineTo(pR, pB); fp.close(); canvas.drawPath(fp, fillPaint)

        spectrumPaint.color = C_CYAN; spectrumPaint.strokeWidth = 2f; spectrumPaint.style = Paint.Style.STROKE; spectrumPaint.strokeCap = Paint.Cap.ROUND; spectrumPaint.strokeJoin = Paint.Join.ROUND
        path.reset(); var first = true
        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            val x = pL + pW * (1f - (wn - 400f) / 3600f); val y = pT + pH * (1f - spectrumData[i])
            if (x < pL || x > pR) continue
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
        canvas.drawPath(path, spectrumPaint)

        val labelPos = mutableListOf<Pair<Float, Float>>()
        var pIdx = 1
        for (g in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(g.id)) continue
            val x = pL + pW * (1f - (g.peakCenter - 400f) / 3600f)
            val idx = ((4000f - g.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1)
            val y = pT + pH * (1f - spectrumData[idx])
            if (x < pL || x > pR) continue

            peakLinePaint.color = colorWithAlpha(g.color, 70); peakLinePaint.strokeWidth = 1f
            peakLinePaint.pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
            canvas.drawLine(x, y, x, pT + 4f, peakLinePaint); peakLinePaint.pathEffect = null

            val dotR = 5f; peakDotPaint.color = g.color; canvas.drawCircle(x, y, dotR, peakDotPaint)
            peakDotPaint.color = Color.argb(40, Color.red(g.color), Color.green(g.color), Color.blue(g.color)); canvas.drawCircle(x, y, dotR + 6f, peakDotPaint)

            val numText = "$pIdx"; val numW = peakNumPaint.measureText(numText) + 10f; val numH = 20f
            var ly = pT + 4f; var tries = 0
            while (tries < 15) {
                if (!labelPos.any { abs(it.first - x) < numW * 0.9f && abs(it.second - ly) < numH + 2f }) break
                ly += numH + 2f; tries++
            }
            labelPos.add(Pair(x, ly))
            peakBgPaint.color = Color.argb(220, 10, 14, 20)
            labelBgRect.set(x - numW / 2f, ly, x + numW / 2f, ly + numH)
            canvas.drawRoundRect(labelBgRect, 4f, 4f, peakBgPaint)
            peakNumPaint.textSize = 12f; canvas.drawText(numText, x, ly + 14f, peakNumPaint)
            pIdx++
        }

        val tableY = pB + 26f
        if (tableY + 12f < top + h && pIdx > 1) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; typeface = Typeface.MONOSPACE; textAlign = Paint.Align.LEFT }
            var tx = pL; var pNum = 1
            for (g in FUNCTIONAL_GROUPS) {
                if (!selectedGroups.contains(g.id)) continue
                tp.color = g.color; canvas.drawText("$pNum. ${g.nameTr} ${g.peakCenter.toInt()}", tx, top + h - 4f, tp)
                tx += tp.measureText("$pNum. ${g.nameTr} ${g.peakCenter.toInt()}") + 14f
                if (tx > pR - 40f) { tx = pL; tp.color = g.color }
                pNum++
            }
        }

        if (showCursor && cursorX >= pL && cursorX <= pR && cursorY >= pT && cursorY <= pB) {
            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(50, 255, 255, 255); pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f) }
            canvas.drawLine(cursorX, pT, cursorX, pB, cp); canvas.drawLine(pL, cursorY, pR, cursorY, cp)
            val cWn = 4000f - (cursorX - pL) / pW * 3600f; val cT = 100f - (cursorY - pT) / pH * 100f
            val ibg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 10, 14, 20); isAntiAlias = true }
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
        var ty = py + 36f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; textAlign = Paint.Align.CENTER; color = C_CYAN; isFakeBoldText = true; isAntiAlias = true; typeface = Typeface.MONOSPACE }
        c.drawText("FT-IR SIMULATOR", w / 2f, ty, hp); ty += 32f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; textAlign = Paint.Align.LEFT; isAntiAlias = true }
        for ((line, clr) in listOf(
            "═══ WHAT IS IT? ═══" to C_CYAN, "Fourier Transform Infrared Spectroscopy" to Color.WHITE,
            "Identifies functional groups of molecules." to Color.WHITE, "" to Color.TRANSPARENT,
            "═══ HOW IT WORKS ═══" to C_CYAN, "1. IR Source: Emits infrared light" to Color.rgb(170, 220, 255),
            "2. Michelson Interferometer: Splits beam" to Color.rgb(170, 220, 255),
            "3. Fixed & Moving Mirrors" to Color.rgb(170, 220, 255),
            "4. Sample: Absorbs IR light" to Color.rgb(170, 220, 255),
            "5. Detector: Measures transmitted light" to Color.rgb(170, 220, 255), "" to Color.TRANSPARENT,
            "═══ REGIONS ═══" to C_CYAN, "Functional: 4000-1500 cm⁻¹" to Color.rgb(100, 255, 160),
            "Fingerprint: 1500-400 cm⁻¹" to Color.rgb(255, 200, 100), "" to Color.TRANSPARENT,
            "═══ USAGE ═══" to C_CYAN, "1. Select functional groups or Library compound" to Color.rgb(200, 230, 255),
            "2. Press SCAN to generate spectrum" to Color.rgb(200, 230, 255),
            "3. Pinch to zoom, double-tap to reset" to Color.rgb(200, 230, 255),
            "4. Touch to see WN and T values" to Color.rgb(200, 230, 255)
        )) { if (line.isEmpty()) { ty += 4f; continue }; lp.color = clr; c.drawText(line, px + 14f, ty, lp); ty += 20f }
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    private fun darken(color: Int, f: Float): Int =
        Color.rgb((Color.red(color) * f).toInt().coerceIn(0, 255), (Color.green(color) * f).toInt().coerceIn(0, 255), (Color.blue(color) * f).toInt().coerceIn(0, 255))
}
