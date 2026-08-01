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

    data class MoleculeStructure(
        val atoms: List<Atom>,
        val bonds: List<Bond>,
        val centerOffsetX: Float = 0f,
        val centerOffsetY: Float = 0f
    )

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
        val C_O_RED = Color.rgb(255, 80, 80)
        val C_N_BLUE = Color.rgb(80, 160, 255)
        val C_H_GRAY = Color.rgb(160, 170, 180)
        val C_CL_GREEN = Color.rgb(100, 220, 100)
        val C_BOND = Color.rgb(0, 200, 150)

        fun eth(): MoleculeStructure {
            val a = listOf(
                Atom("C", -30f, 0f, C_GREEN), Atom("C", 30f, 0f, C_GREEN),
                Atom("O", 90f, 0f, C_O_RED), Atom("H", 130f, -30f, C_H_GRAY),
                Atom("H", -30f, -40f, C_H_GRAY), Atom("H", -70f, 20f, C_H_GRAY),
                Atom("H", -30f, 40f, C_H_GRAY), Atom("H", 30f, -40f, C_H_GRAY),
                Atom("H", 30f, 40f, C_H_GRAY)
            )
            val b = listOf(Bond(0, 1), Bond(1, 2), Bond(2, 3), Bond(0, 4), Bond(0, 5), Bond(0, 6), Bond(1, 7), Bond(1, 8))
            return MoleculeStructure(a, b)
        }
        fun acetone(): MoleculeStructure {
            val a = listOf(
                Atom("C", 0f, 0f, C_GREEN), Atom("C", -50f, 30f, C_GREEN),
                Atom("C", 50f, 30f, C_GREEN), Atom("O", 0f, -50f, C_O_RED),
                Atom("H", -50f, 70f, C_H_GRAY), Atom("H", -90f, 10f, C_H_GRAY),
                Atom("H", -50f, -10f, C_H_GRAY), Atom("H", 50f, 70f, C_H_GRAY),
                Atom("H", 90f, 10f, C_H_GRAY), Atom("H", 50f, -10f, C_H_GRAY)
            )
            val b = listOf(Bond(0, 1), Bond(0, 2), Bond(0, 3, 2), Bond(1, 4), Bond(1, 5), Bond(1, 6), Bond(2, 7), Bond(2, 8), Bond(2, 9))
            return MoleculeStructure(a, b)
        }
        fun benzene(): MoleculeStructure {
            val a = mutableListOf<Atom>()
            val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                a.add(Atom("C", (cos(angle) * 45).toFloat(), (sin(angle) * 45).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                a.add(Atom("H", (cos(angle) * 80).toFloat(), (sin(angle) * 80).toFloat(), C_H_GRAY))
                b.add(Bond(i, i + 6))
            }
            return MoleculeStructure(a, b)
        }
        fun aceticAcid(): MoleculeStructure {
            val a = listOf(
                Atom("C", -30f, 0f, C_GREEN), Atom("C", 30f, 0f, C_GREEN),
                Atom("O", 30f, -50f, C_O_RED), Atom("O", 90f, 20f, C_O_RED),
                Atom("H", 130f, 0f, C_H_GRAY), Atom("H", -30f, -40f, C_H_GRAY),
                Atom("H", -70f, 20f, C_H_GRAY), Atom("H", -30f, 40f, C_H_GRAY)
            )
            val b = listOf(Bond(0, 1), Bond(1, 2, 2), Bond(1, 3), Bond(3, 4), Bond(0, 5), Bond(0, 6), Bond(0, 7))
            return MoleculeStructure(a, b)
        }
        fun ethanol(): MoleculeStructure = eth()
        fun methanol(): MoleculeStructure {
            val a = listOf(
                Atom("C", 0f, 0f, C_GREEN), Atom("O", 50f, 0f, C_O_RED),
                Atom("H", 90f, -25f, C_H_GRAY), Atom("H", 0f, -40f, C_H_GRAY),
                Atom("H", -35f, 20f, C_H_GRAY), Atom("H", 0f, 40f, C_H_GRAY)
            )
            val b = listOf(Bond(0, 1), Bond(1, 2), Bond(0, 3), Bond(0, 4), Bond(0, 5))
            return MoleculeStructure(a, b)
        }
        fun phenol(): MoleculeStructure {
            val a = mutableListOf<Atom>()
            val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                a.add(Atom("C", (cos(angle) * 40).toFloat(), (sin(angle) * 40).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            a.add(Atom("O", 0f, -80f, C_O_RED)); b.add(Bond(0, 6))
            a.add(Atom("H", 0f, -115f, C_H_GRAY)); b.add(Bond(6, 7))
            for (i in 1 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                a.add(Atom("H", (cos(angle) * 72).toFloat(), (sin(angle) * 72).toFloat(), C_H_GRAY))
                b.add(Bond(i, i + 7))
            }
            return MoleculeStructure(a, b)
        }
        fun acetoneMol(): MoleculeStructure = acetone()
        fun benzeneMol(): MoleculeStructure = benzene()
        fun toluene(): MoleculeStructure {
            val a = mutableListOf<Atom>()
            val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                a.add(Atom("C", (cos(angle) * 40).toFloat(), (sin(angle) * 40).toFloat(), C_GREEN))
            }
            for (i in 0 until 6) b.add(Bond(i, (i + 1) % 6, if (i % 2 == 0) 2 else 1))
            a.add(Atom("C", -72f, 0f, C_GREEN)); b.add(Bond(0, 6))
            a.add(Atom("H", -100f, -25f, C_H_GRAY)); b.add(Bond(6, 7))
            a.add(Atom("H", -100f, 25f, C_H_GRAY)); b.add(Bond(6, 8))
            a.add(Atom("H", -72f, 40f, C_H_GRAY)); b.add(Bond(6, 9))
            for (i in 1 until 6) {
                val angle = Math.toRadians((60.0 * i - 90.0))
                a.add(Atom("H", (cos(angle) * 70).toFloat(), (sin(angle) * 70).toFloat(), C_H_GRAY))
                b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }
        fun hexane(): MoleculeStructure {
            val a = mutableListOf<Atom>()
            val b = mutableListOf<Bond>()
            for (i in 0 until 6) {
                val x = (i - 2.5f) * 40f; val y = if (i % 2 == 0) -15f else 15f
                a.add(Atom("C", x, y, C_GREEN))
                if (i > 0) b.add(Bond(i - 1, i))
            }
            a.add(Atom("H", -100f, -50f, C_H_GRAY)); b.add(Bond(0, 6))
            a.add(Atom("H", -100f, 20f, C_H_GRAY)); b.add(Bond(0, 7))
            a.add(Atom("H", 100f, -50f, C_H_GRAY)); b.add(Bond(5, 8))
            a.add(Atom("H", 100f, 20f, C_H_GRAY)); b.add(Bond(5, 9))
            for (i in 1 until 5) {
                val x = (i - 2.5f) * 40f
                val y1 = if (i % 2 == 0) -50f else 50f
                a.add(Atom("H", x, y1, C_H_GRAY)); b.add(Bond(i, a.size - 1))
            }
            return MoleculeStructure(a, b)
        }

        val FUNCTIONAL_GROUPS = listOf(
            FunctionalGroup("oh_alcohol", "O-H (Alkol)", "Hidroksil",
                3200f, 3600f, 3350f, 0.85f, 180f,
                Color.rgb(255, 80, 80), "Geniş",
                "H-bağlama nedeniyle geniş pik. Alkol ve fenol için karakteristiktir.",
                "Etanol, Metanol"),
            FunctionalGroup("oh_acid", "O-H (Asit)", "Karboksilik Asit O-H",
                2500f, 3300f, 3000f, 0.92f, 400f,
                Color.rgb(255, 60, 60), "Çok Geniş",
                "Çok geniş, C-H absorpsiyonu ile örtüşür. Asit türü için belirleyici.",
                "Asetik Asit, Propiyonik Asit"),
            FunctionalGroup("nh_primary", "N-H (1° Amin)", "Birincil Amin",
                3250f, 3500f, 3400f, 0.55f, 80f,
                Color.rgb(80, 180, 255), "Çift Pik",
                "İkiz pik (asimetrik + simetrik N-H gerilmesi). Birincil amin belirtisi.",
                "Anilin, Metilamin"),
            FunctionalGroup("nh_secondary", "N-H (2° Amin)", "İkincil Amin",
                3310f, 3350f, 3330f, 0.4f, 50f,
                Color.rgb(100, 200, 255), "Tek Pik",
                "Tek zayıf pik. İkincil amin belirtisi.",
                "Dietilamin, Piperazin"),
            FunctionalGroup("nh_amide", "N-H (Amid)", "Amid N-H",
                3180f, 3350f, 3280f, 0.5f, 100f,
                Color.rgb(120, 160, 255), "Geniş",
                "Geniş pik. Amid I ve amid II bantları ile birlikte görülür.",
                "Asetamid, Üre"),
            FunctionalGroup("ch_alkane", "C-H (Alkan)", "Alkan C-H",
                2845f, 2970f, 2920f, 0.7f, 50f,
                Color.rgb(200, 200, 80), "Keskin",
                "Güçlü, keskin pik. sp³ C-H gerilmesi. Hemen her organik bileşikte bulunur.",
                "Hekzan, Sikloheksan"),
            FunctionalGroup("ch_alkene", "C-H (Alken)", "Alken =C-H",
                3020f, 3100f, 3080f, 0.45f, 40f,
                Color.rgb(100, 220, 100), "Orta",
                "sp² C-H gerilmesi. Alken varlığını gösterir.",
                "1-Heksen, Etilen"),
            FunctionalGroup("ch_aro", "C-H (Aromatik)", "Aromatik C-H",
                3000f, 3100f, 3050f, 0.4f, 35f,
                Color.rgb(180, 140, 255), "Zayıf-Orta",
                "Aromatik halka C-H gerilmesi. Benzen türevleri için karakteristiktir.",
                "Benzen, Toluen"),
            FunctionalGroup("ch_aldehyde", "C-H (Aldehit)", "Aldehit C-H",
                2720f, 2830f, 2780f, 0.35f, 40f,
                Color.rgb(200, 180, 100), "Çift Pik",
                "Fermi çift pik (2720 ve 2820 cm⁻¹). Aldehit belirleyici.",
                "Benzaldehit, Formaldehit"),
            FunctionalGroup("ch_aldehyde2", "C-H (Aldehit 2)", "Aldehit C-H (Fermi)",
                2720f, 2820f, 2720f, 0.3f, 30f,
                Color.rgb(220, 190, 90), "Keskin",
                "Fermi rezonans çiftinin alt pik. Aldehit onayı.",
                "Benzaldehit"),
            FunctionalGroup("ch_alkyne", "≡C-H (Alkin)", "Terminal Alkin ≡C-H",
                3260f, 3330f, 3300f, 0.7f, 30f,
                Color.rgb(255, 220, 80), "Keskin",
                "Güçlü, keskin pik. Terminal alkin belirtisi.",
                "Asetilen, Propin"),
            FunctionalGroup("co_ketone", "C=O (Keton)", "Keton Karbonil",
                1705f, 1725f, 1715f, 0.95f, 35f,
                Color.rgb(255, 200, 50), "Keskin",
                "Çok güçlü, keskin pik. Keton belirleyici absorpsiyon.",
                "Aseton, sikloheksanon"),
            FunctionalGroup("co_aldehyde", "C=O (Aldehit)", "Aldehit Karbonil",
                1720f, 1740f, 1730f, 0.9f, 30f,
                Color.rgb(255, 180, 80), "Keskin",
                "Güçlü pik. C-H Fermi çift pik ile birlikte aldehiti doğrular.",
                "Benzaldehit, Asetaldehit"),
            FunctionalGroup("co_ester", "C=O (Ester)", "Ester Karbonil",
                1735f, 1750f, 1740f, 0.88f, 30f,
                Color.rgb(255, 160, 100), "Keskin",
                "Güçlü pik. C-O bandı (1150-1300) ile birlikte esteri doğrular.",
                "Etil Asetat, Metil Benzoat"),
            FunctionalGroup("co_acid", "C=O (Asit)", "Karboksilik Asit C=O",
                1700f, 1725f, 1710f, 0.92f, 35f,
                Color.rgb(255, 140, 70), "Keskin",
                "Güçlü pik. O-H bandı (2500-3300) ile birlikte asiti doğrular.",
                "Asetik Asit, Benzoik Asit"),
            FunctionalGroup("co_amide1", "C=O (Amid I)", "Amid I Bandı",
                1630f, 1690f, 1660f, 0.85f, 40f,
                Color.rgb(220, 180, 255), "Keskin",
                "Amid I bandı (C=O gerilmesi). Amid türü için karakteristiktir.",
                "Asetamid, Nylon"),
            FunctionalGroup("cc_alkene", "C=C (Alken)", "Alken C=C",
                1620f, 1680f, 1650f, 0.35f, 35f,
                Color.rgb(80, 255, 180), "Zayıf-Orta",
                "Değişken şiddet. Simetrik olmayan alkenlerde görünür.",
                "1-Heksen, Stiren"),
            FunctionalGroup("cc_aro", "C=C (Aromatik)", "Aromatik Halka C=C",
                1450f, 1615f, 1500f, 0.45f, 60f,
                Color.rgb(160, 140, 220), "Orta",
                "Çoklu pikler (1450, 1500, 1580, 1600). Aromatik halka belirtisi.",
                "Benzen, Naftalin"),
            FunctionalGroup("cc_alkyne", "C≡C (Alkin)", "Alkin C≡C",
                2100f, 2260f, 2150f, 0.3f, 25f,
                Color.rgb(255, 255, 100), "Zayıf",
                "Zayıf pik. Terminal alkinlerde daha belirgin.",
                "Asetilen, 1-Butin"),
            FunctionalGroup("cn_nitrile", "C≡N (Nitril)", "Nitril C≡N",
                2210f, 2260f, 2250f, 0.5f, 25f,
                Color.rgb(150, 255, 150), "Orta-Keskin",
                "Karakteristik pik. Nitril belirleyici absorpsiyon.",
                "Asetonitril, Benzonitril"),
            FunctionalGroup("no2", "NO₂ (Nitro)", "Nitro",
                1515f, 1570f, 1540f, 0.8f, 35f,
                Color.rgb(255, 80, 180), "Güçlü",
                "Güçlü pik (asimetrik NO₂ gerilmesi). 1345-1385'te simetrik pik ile çift.",
                "Nitrobenzen, TNT"),
            FunctionalGroup("co_alcohol", "C-O (Alkol)", "C-O Gerilmesi",
                1040f, 1175f, 1100f, 0.65f, 80f,
                Color.rgb(255, 150, 100), "Güçlü",
                "Güçlü pik. Alkol, ester ve eterlerde bulunur.",
                "Etanol, İzopropanol"),
            FunctionalGroup("co_ester_coc", "C-O-C (Ester)", "Ester C-O-C",
                1150f, 1300f, 1240f, 0.75f, 70f,
                Color.rgb(255, 130, 80), "Güçlü",
                "Ester C-O gerilmesi. C=O bandı ile birlikte esteri doğrular.",
                "Etil Asetat, Metil Metakrilat"),
            FunctionalGroup("nh_bend", "N-H Bükülme", "Amid II Bandı",
                1510f, 1570f, 1540f, 0.6f, 35f,
                Color.rgb(140, 180, 255), "Orta-Güçlü",
                "Amid II bandı (N-H bükülme + C-N gerilmesi). Amid belirtisi.",
                "Asetamid, Protein"),
            FunctionalGroup("c_cl", "C-Cl (Klor)", "Kloro",
                550f, 850f, 700f, 0.7f, 100f,
                Color.rgb(180, 220, 180), "Güçlü",
                "Güçlü pik. Halojenli bileşikler için karakteristiktir.",
                "Kloroform, Diklorometan")
        )

        val COOKBOOK_COMPOUNDS = listOf(
            CookbookCompound("Etanol", "C₂H₅OH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "Geniş O-H (3350), C-H (2920), C-O (1100) - Alkolün klasik spektrumu",
                "46.07 g/mol", "Alkol", eth()),
            CookbookCompound("Metanol", "CH₃OH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "O-H (3350), C-H (2920), C-O (1050)",
                "32.04 g/mol", "Alkol", methanol()),
            CookbookCompound("İzopropanol", "(CH₃)₂CHOH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "O-H (3350), C-H (2920), C-O (1150)",
                "60.10 g/mol", "Alkol"),
            CookbookCompound("1-Butanol", "C₄H₉OH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "O-H (3350), C-H (2920), C-O (1070)",
                "74.12 g/mol", "Alkol"),
            CookbookCompound("Fenol", "C₆H₅OH",
                listOf("oh_alcohol", "ch_aro", "cc_aro", "co_alcohol"),
                "O-H (3350), Aromatik C-H (3050), C=C (1500), C-O (1200)",
                "94.11 g/mol", "Aromatik", phenol()),
            CookbookCompound("Gliserol", "C₃H₈O₃",
                listOf("oh_alcohol", "co_alcohol", "ch_alkane"),
                "3x O-H (3350), C-O (1100) - Çok geniş O-H bandı",
                "92.09 g/mol", "Alkol"),
            CookbookCompound("Aseton", "CH₃COCH₃",
                listOf("co_ketone", "ch_alkane"),
                "Güçlü C=O (1715), C-H (2920) - Keton referansı",
                "58.08 g/mol", "Keton", acetone()),
            CookbookCompound("2-Butanon (MEK)", "CH₃COC₂H₅",
                listOf("co_ketone", "ch_alkane"),
                "C=O (1715), C-H (2920)",
                "72.11 g/mol", "Keton"),
            CookbookCompound("Sikloheksanon", "C₆H₁₀O",
                listOf("co_ketone", "ch_alkane"),
                "C=O (1715), C-H (2920) - Halkalı keton",
                "98.14 g/mol", "Keton"),
            CookbookCompound("Asetofenon", "C₆H₅COCH₃",
                listOf("co_ketone", "ch_aro", "cc_aro", "ch_alkane"),
                "C=O (1715), Aromatik C=C (1500, 1600), C-H (2920)",
                "120.15 g/mol", "Keton"),
            CookbookCompound("Benzaldehit", "C₆H₅CHO",
                listOf("co_aldehyde", "ch_aldehyde", "ch_aro", "cc_aro"),
                "C=O (1730), C-H Fermi çift (2720, 2820), Aromatik (3050, 1500)",
                "106.12 g/mol", "Aldehit"),
            CookbookCompound("Asetaldehit", "CH₃CHO",
                listOf("co_aldehyde", "ch_aldehyde", "ch_alkane"),
                "C=O (1730), C-H Fermi çift (2720, 2820), C-H (2920)",
                "44.05 g/mol", "Aldehit"),
            CookbookCompound("Formaldehit", "HCHO",
                listOf("co_aldehyde"),
                "C=O (1745) - En basit aldehit",
                "30.03 g/mol", "Aldehit"),
            CookbookCompound("Asetik Asit", "CH₃COOH",
                listOf("oh_acid", "co_acid", "co_alcohol"),
                "Çok geniş O-H (2500-3300), C=O (1710), C-O (1240)",
                "60.05 g/mol", "Asit", aceticAcid()),
            CookbookCompound("Propiyonik Asit", "C₂H₅COOH",
                listOf("oh_acid", "co_acid", "ch_alkane", "co_alcohol"),
                "Geniş O-H, C=O (1710), C-H (2920), C-O (1240)",
                "74.08 g/mol", "Asit"),
            CookbookCompound("Benzoik Asit", "C₆H₅COOH",
                listOf("oh_acid", "co_acid", "ch_aro", "cc_aro"),
                "Geniş O-H, C=O (1690), Aromatik C=C (1500)",
                "122.12 g/mol", "Asit"),
            CookbookCompound("Etil Asetat", "CH₃COOC₂H₅",
                listOf("co_ester", "co_ester_coc", "ch_alkane"),
                "C=O (1740), C-O-C (1240), C-H (2920)",
                "88.11 g/mol", "Ester"),
            CookbookCompound("Metil Benzoat", "C₆H₅COOCH₃",
                listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro"),
                "C=O (1724), C-O-C (1275), Aromatik C=C (1500)",
                "136.15 g/mol", "Ester"),
            CookbookCompound("Etil Benzoat", "C₆H₅COOC₂H₅",
                listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"),
                "C=O (1720), C-O-C (1270), Aromatik + Alkil C-H",
                "150.17 g/mol", "Ester"),
            CookbookCompound("Benzen", "C₆H₆",
                listOf("ch_aro", "cc_aro"),
                "Aromatik C-H (3050), C=C halka (1500, 1600)",
                "78.11 g/mol", "Aromatik", benzene()),
            CookbookCompound("Toluen", "C₆H₅CH₃",
                listOf("ch_aro", "cc_aro", "ch_alkane"),
                "Aromatik C-H (3050), C=C (1500, 1600), CH₃ (2920)",
                "92.14 g/mol", "Aromatik", toluene()),
            CookbookCompound("Hekzan", "C₆H₁₄",
                listOf("ch_alkane"),
                "Sadece C-H (2920, 2850) absorpsiyonları - Basit spektrum",
                "86.18 g/mol", "Alkan", hexane()),
            CookbookCompound("Sikloheksan", "C₆H₁₂",
                listOf("ch_alkane"),
                "C-H (2920, 2850) - Halkalı alkan",
                "84.16 g/mol", "Alkan"),
            CookbookCompound("1-Heksen", "C₆H₁₂",
                listOf("ch_alkane", "ch_alkene", "cc_alkene"),
                "C-H alkane (2920), =C-H (3080), C=C (1650)",
                "84.16 g/mol", "Alken"),
            CookbookCompound("Stiren", "C₆H₅CH=CH₂",
                listOf("ch_aro", "cc_aro", "ch_alkene", "cc_alkene"),
                "Aromatik + Alken C-H, C=C (1630), Aromatik C=C (1500)",
                "104.15 g/mol", "Alken"),
            CookbookCompound("Anilin", "C₆H₅NH₂",
                listOf("nh_primary", "ch_aro", "cc_aro"),
                "N-H çift pik (3400), Aromatik C-H (3050), C=C (1500)",
                "93.13 g/mol", "Amin"),
            CookbookCompound("Dietilamin", "(C₂H₅)₂NH",
                listOf("nh_secondary", "ch_alkane", "co_alcohol"),
                "N-H tek pik (3330), C-H (2920)",
                "73.14 g/mol", "Amin"),
            CookbookCompound("Asetonitril", "CH₃CN",
                listOf("cn_nitrile", "ch_alkane"),
                "C≡N (2250), C-H (2920)",
                "41.05 g/mol", "Nitril"),
            CookbookCompound("Benzonitril", "C₆H₅CN",
                listOf("cn_nitrile", "ch_aro", "cc_aro"),
                "C≡N (2230), Aromatik C-H (3050), C=C (1500)",
                "103.12 g/mol", "Nitril"),
            CookbookCompound("Kloroform", "CHCl₃",
                listOf("c_cl", "ch_alkane"),
                "C-Cl (760), C-H (3020) - Çözücü",
                "119.38 g/mol", "Halojen"),
            CookbookCompound("Diklorometan", "CH₂Cl₂",
                listOf("c_cl", "ch_alkane"),
                "C-Cl (700), C-H (2920)",
                "84.93 g/mol", "Halojen"),
            CookbookCompound("Karbon Tetraklorür", "CCl₄",
                listOf("c_cl"),
                "C-Cl (780, 820) - Sadece C-Cl absorpsiyonu",
                "153.82 g/mol", "Halojen"),
            CookbookCompound("Asetamid", "CH₃CONH₂",
                listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"),
                "N-H (3300), Amid I (1660), Amid II (1540), C-H (2920)",
                "59.07 g/mol", "Amid"),
            CookbookCompound("Naylon 6,6", "(C₁₂H₂₂N₂O₂)ₙ",
                listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"),
                "N-H (3300), Amid I (1660), Amid II (1540), C-H (2920)",
                "226.32 g/mol", "Polimer"),
            CookbookCompound("Nitrobenzen", "C₆H₅NO₂",
                listOf("no2", "ch_aro", "cc_aro"),
                "NO₂ (1540), Aromatik C-H (3050), C=C (1500)",
                "123.11 g/mol", "Nitro"),
            CookbookCompound("Dietil Eter", "(C₂H₅)₂O",
                listOf("ch_alkane", "co_alcohol"),
                "C-H (2920), C-O (1120) - Eter bandı",
                "74.12 g/mol", "Eter"),
            CookbookCompound("Tetrahidrofuran (THF)", "C₄H₈O",
                listOf("ch_alkane", "co_alcohol"),
                "C-H (2920), C-O (1070) - Halkalı eter",
                "72.11 g/mol", "Eter"),
            CookbookCompound("Difenil Metan", "(C₆H₅)₂CH₂",
                listOf("ch_aro", "cc_aro", "ch_alkane"),
                "Aromatik C-H (3050), C=C (1500, 1600), CH₂ (2920)",
                "166.22 g/mol", "Aromatik"),
            CookbookCompound("Kağıt (Selüloz)", "(C₆H₁₀O₅)ₙ",
                listOf("oh_alcohol", "co_alcohol", "ch_alkane"),
                "Geniş O-H (3350), C-O (1050), C-H (2920) - Selüloz iskeleti",
                "—", "Polimer"),
            CookbookCompound("PET Plastik", "(C₁₀H₈O₄)ₙ",
                listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"),
                "C=O (1720), C-O-C (1240), Aromatik C=C (1500), C-H (2920)",
                "—", "Polimer"),
            CookbookCompound("Polietilen (PE)", "(C₂H₄)ₙ",
                listOf("ch_alkane"),
                "Yoğun C-H (2920, 2850, 1470, 1370) - Sadece alkan bantları",
                "—", "Polimer"),
            CookbookCompound("Polipropilen (PP)", "(C₃H₆)ₙ",
                listOf("ch_alkane"),
                "C-H (2920, 2840, 1378) - Metil dalgalanması belirgin",
                "—", "Polimer"),
            CookbookCompound("PVC", "(C₂H₃Cl)ₙ",
                listOf("ch_alkane", "c_cl"),
                "C-H (2920), C-Cl (690, 615) - Polivinil klorür",
                "—", "Polimer"),
            CookbookCompound("Polistiren (PS)", "(C₈H₈)ₙ",
                listOf("ch_aro", "cc_aro", "ch_alkane"),
                "Aromatik C-H (3025), C=C (1600, 1492, 1452), C-H (2920)",
                "—", "Polimer"),
            CookbookCompound("Naylon (PA6)", "(C₆H₁₁NO)ₙ",
                listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"),
                "N-H (3300), Amid I (1640), Amid II (1540), C-H (2930)",
                "—", "Polimer"),
            CookbookCompound("Lastik (Doğal Kauçuk)", "(C₅H₈)ₙ",
                listOf("ch_alkane", "ch_alkene", "cc_alkene"),
                "C-H (2920), =C-H (3040), C=C (1660) - Poliizopren",
                "—", "Polimer"),
            CookbookCompound("Naylon Çorap (Elastan)", "(C₁₅H₂₂N₂O₂)ₙ",
                listOf("nh_amide", "co_amide1", "co_alcohol", "ch_alkane"),
                "N-H (3330), Amid I (1700), Amid II (1540), C-O (1100)",
                "—", "Polimer"),
            CookbookCompound("Şeker (Sucroz)", "C₁₂H₂₂O₁₁",
                listOf("oh_alcohol", "co_alcohol", "ch_alkane"),
                "Yoğun O-H (3400), C-O (1000-1100), C-H (2920)",
                "342.30 g/mol", "Karbonhidrat"),
            CookbookCompound("Tuz (NaCl)", "NaCl",
                listOf("c_cl"),
                "Na-Cl absorpsiyonu (600 civarı) - Basit inorganik tuz",
                "58.44 g/mol", "Inorganik"),
            CookbookCompound("Sirke (Asetik Asit Ç.)", "CH₃COOH + H₂O",
                listOf("oh_acid", "co_acid", "oh_alcohol", "co_alcohol"),
                "Geniş O-H, C=O (1710), C-O (1240) - Seyreltik asit",
                "—", "Çözelti"),
            CookbookCompound("Yağ (Trigliserit)", "C₅₅H₉₈O₆",
                listOf("ch_alkane", "co_ester"),
                "C-H (2920, 2850), C=O (1745), C-O (1160) - Uzun zincirli ester",
                "—", "Lipit"),
            CookbookCompound("E Vitamini", "C₂₉H₅₀O₂",
                listOf("oh_alcohol", "ch_alkane", "ch_aro", "cc_aro"),
                "O-H (3400), C-H (2920), Aromatik C=C (1500)",
                "430.71 g/mol", "Vitamin")
        )

        val SAMPLE_TYPES = listOf(
            "KBr Pellet" to "Katı numuneler için KBr ile sıkıştırma",
            "İnce Film" to "Sıvı numuneler için NaCl plakları arasında",
            "ATR" to "Tam yansıma tekniği",
            "Çözelti" to "Kuvvetli çözücüde seyreltme (CCl₄)"
        )
    }

    var selectedGroups = mutableSetOf<String>()
        private set
    var resolution = 4
        private set
    var scanCount = 16
        private set
    var sampleType = SAMPLE_TYPES[0].first
        private set
    var isScanning = false
        private set
    var scanProgress = 0f
        private set
    var showInterferogram = false
    var showInfo = false

    var currentCompound: CookbookCompound? = null
        private set
    var compoundName: String = ""
        private set

    private var time = 0f
    private var themeColors = ThemeColors()
    private val spectrumData = FloatArray(2000)
    private val interferogramData = FloatArray(200)

    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector
    private var cursorX = 0f; private var cursorY = 0f; private var showCursor = false
    private var tapTime = 0L; private var lastTapTime = 0L

    private var scanLineX = 0f
    private var scanLineActive = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            time += 0.025f
            updateData()
            if (scanLineActive) {
                scanLineX += 2f
                if (scanLineX > 1f) { scanLineActive = false; scanLineX = 0f }
            }
            invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    init {
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean = true
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val factor = d.scaleFactor
                val pivotX = d.focusX; val pivotY = d.focusY
                val newScale = (zoomScale * factor).coerceIn(0.5f, 4f)
                val scaleChange = newScale / zoomScale
                panX = pivotX - (pivotX - panX) * scaleChange
                panY = pivotY - (pivotY - panY) * scaleChange
                zoomScale = newScale
                invalidate()
                return true
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
                    if (touchMode == 0 && System.currentTimeMillis() - tapTime < 300) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            zoomScale = 1f; panX = 0f; panY = 0f; invalidate()
                        }
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
    private val spectrumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; strokeCap = Paint.Cap.ROUND
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 0.5f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val smallLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f; textAlign = Paint.Align.CENTER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val atomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val atomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val bondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val peakLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f
    }
    private val peakBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val peakLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val peakWnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f; textAlign = Paint.Align.CENTER
    }
    private val peakDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val path = Path()
    private val rect = RectF()
    private val labelBgRect = RectF()

    fun toggleGroup(groupId: String) {
        if (selectedGroups.contains(groupId)) selectedGroups.remove(groupId)
        else selectedGroups.add(groupId)
        generateSpectrum(); invalidate()
    }

    fun selectPreset(compound: CookbookCompound) {
        selectedGroups.clear(); selectedGroups.addAll(compound.groups)
        currentCompound = compound; compoundName = compound.name
        scanLineX = 0f; scanLineActive = true
        generateSpectrum(); invalidate()
    }

    fun setResolution(res: Int) { resolution = res.coerceIn(1, 8); generateSpectrum() }
    fun setScanCount(count: Int) { scanCount = count.coerceIn(1, 128) }
    fun setSampleType(type: String) { sampleType = type }
    fun toggleInfo() { showInfo = !showInfo; invalidate() }

    fun startScan() {
        isScanning = true; scanProgress = 0f; generateSpectrum(); invalidate()
    }

    fun setThemeColors(colors: ThemeColors) {
        themeColors = colors; bgPaint.color = colors.bg; generateSpectrum(); invalidate()
    }

    private fun updateData() {
        if (isScanning) {
            scanProgress += 0.004f
            if (scanProgress >= 1f) { scanProgress = 1f; isScanning = false; generateSpectrum() }
        }
        val zpd = interferogramData.size / 2
        for (i in interferogramData.indices) {
            val opd = (i - zpd).toFloat()
            val envelope = exp(-(opd * opd) / (3000f + scanCount * 50f))
            var signal = envelope * cos(opd * 0.08f + time * 2f)
            signal += envelope * 0.4f * cos(opd * 0.12f + time * 1.3f)
            signal += envelope * 0.2f * cos(opd * 0.04f + time * 3f)
            interferogramData[i] = signal + (Math.random().toFloat() - 0.5f) * 0.01f
        }
    }

    private fun generateSpectrum() {
        for (i in spectrumData.indices) spectrumData[i] = 0.5f
        val noiseLevel = 0.15f + (8 - resolution) * 0.1f

        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            val baselineCurve = 0.5f * sin(wn / 4000f * PI.toFloat()) * 0.02f
            val slowDrift = 0.015f * sin(wn / 800f * PI.toFloat()) * sin(wn / 1200f * PI.toFloat())
            spectrumData[i] += baselineCurve + slowDrift

            val co2Center1 = 2349f; val co2Center2 = 2361f; val co2W = 15f; val co2Depth = 0.12f
            spectrumData[i] -= co2Depth * exp(-((wn - co2Center1).pow(2)) / (2f * co2W * co2W))
            spectrumData[i] -= co2Depth * 0.8f * exp(-((wn - co2Center2).pow(2)) / (2f * co2W * co2W))

            val h2oCenters = floatArrayOf(1595f, 1650f, 3750f, 3660f, 3600f)
            val h2oWidths = floatArrayOf(20f, 25f, 40f, 30f, 35f)
            val h2oDepths = floatArrayOf(0.06f, 0.04f, 0.08f, 0.05f, 0.03f)
            for (j in h2oCenters.indices) {
                val d = wn - h2oCenters[j]
                spectrumData[i] -= h2oDepths[j] * exp(-(d * d) / (2f * h2oWidths[j] * h2oWidths[j]))
            }
        }

        for (group in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(group.id)) continue
            val halfW = group.peakWidth / resolution
            val useLorentzian = group.peakIntensity > 0.7f
            for (i in spectrumData.indices) {
                val wn = 4000f - i * (3600f / spectrumData.size)
                val dist = wn - group.peakCenter
                val peak = if (useLorentzian) {
                    group.peakIntensity / (1f + (dist * dist) / (halfW * halfW))
                } else {
                    group.peakIntensity * exp(-(dist * dist) / (2f * halfW * halfW))
                }
                spectrumData[i] -= peak * 0.45f
            }
        }

        for (i in spectrumData.indices) {
            spectrumData[i] += (Math.random().toFloat() - 0.5f) * noiseLevel * 0.012f
            if (Math.random().toFloat() > 0.997f) spectrumData[i] += (Math.random().toFloat() - 0.5f) * 0.03f
            spectrumData[i] = spectrumData[i].coerceIn(0.02f, 0.98f)
        }
    }

    fun getPeakTable(): List<Triple<String, Float, Float>> {
        val peaks = mutableListOf<Triple<String, Float, Float>>()
        for (group in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(group.id)) continue
            val idx = ((4000f - group.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1)
            val transmittance = spectrumData[idx] * 100f
            peaks.add(Triple(group.name, group.peakCenter, transmittance))
        }
        return peaks.sortedByDescending { it.second }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); handler.post(ticker) }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handler.removeCallbacksAndMessages(null) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        drawHolographicOverlay(canvas, w, h)

        canvas.save()
        canvas.scale(zoomScale, zoomScale, w / 2, h / 2)
        canvas.translate(panX / zoomScale, panY / zoomScale)

        if (showInterferogram) {
            drawInterferogram(canvas, w, h)
        } else {
            val compound = currentCompound
            if (compound != null && compound.structure != null) {
                drawCompoundInfo(canvas, w, h * 0.12f)
                drawMolecule(canvas, 0f, h * 0.12f, w, h * 0.30f, compound)
                drawSpectrum(canvas, 0f, h * 0.42f, w, h * 0.58f)
            } else if (compound != null) {
                drawCompoundInfo(canvas, w, h * 0.08f)
                drawSpectrum(canvas, 0f, h * 0.09f, w, h * 0.91f)
            } else {
                drawSpectrum(canvas, 0f, 0f, w, h)
            }
        }

        canvas.restore()

        if (scanLineActive) drawScanLine(canvas, w, h)
        if (showInfo) drawInfo(canvas, w, h)
    }

    private fun drawHolographicOverlay(canvas: Canvas, w: Float, h: Float) {
        val scanAlpha = (15 + sin(time * 0.5f) * 10).toInt().coerceIn(5, 25)
        val grad = LinearGradient(0f, 0f, 0f, h,
            Color.argb(scanAlpha, 0, 255, 200), Color.argb(0, 0, 255, 200),
            Shader.TileMode.CLAMP)
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = grad; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, w, h, overlayPaint)
        overlayPaint.shader = null

        val lineY = (h * 0.5f + sin(time * 0.3f) * h * 0.3f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1f
            color = Color.argb(20, 0, 255, 200)
        }
        canvas.drawLine(0f, lineY, w, lineY, linePaint)
    }

    private fun drawScanLine(canvas: Canvas, w: Float, h: Float) {
        val x = w * scanLineX
        scanLinePaint.color = Color.argb(180, 0, 255, 200)
        scanLinePaint.pathEffect = null
        canvas.drawLine(x, 0f, x, h, scanLinePaint)

        val glowGrad = LinearGradient(x - 40f, 0f, x, 0f,
            Color.TRANSPARENT, Color.argb(60, 0, 255, 200), Shader.TileMode.CLAMP)
        val glowPaintLocal = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = glowGrad }
        canvas.drawRect(x - 40f, 0f, x, h, glowPaintLocal)
        glowPaintLocal.shader = null
    }

    private fun drawCompoundInfo(canvas: Canvas, w: Float, h: Float) {
        val compound = currentCompound ?: return
        val pad = 16f

        labelPaint.textSize = 22f; labelPaint.color = Color.WHITE; labelPaint.textAlign = Paint.Align.LEFT
        labelPaint.isFakeBoldText = true
        canvas.drawText(compound.name.uppercase(), pad, pad + 18f, labelPaint)

        labelPaint.textSize = 13f; labelPaint.color = C_CYAN
        canvas.drawText(compound.formula, pad, pad + 38f, labelPaint)

        var infoX = pad
        val infoY = pad + 56f
        smallLabelPaint.textAlign = Paint.Align.LEFT; smallLabelPaint.textSize = 11f

        if (compound.molecularWeight.isNotEmpty()) {
            smallLabelPaint.color = C_GREEN
            canvas.drawText(compound.molecularWeight, infoX, infoY, smallLabelPaint)
            infoX += smallLabelPaint.measureText(compound.molecularWeight) + 20f
        }
        if (compound.category.isNotEmpty()) {
            smallLabelPaint.color = themeColors.muted
            canvas.drawText(compound.category, infoX, infoY, smallLabelPaint)
        }

        val descY = infoY + 18f
        smallLabelPaint.color = themeColors.muted; smallLabelPaint.textSize = 10f
        val descText = compound.description
        if (descText.length > 80) {
            canvas.drawText(descText.substring(0, 80) + "...", pad, descY, smallLabelPaint)
        } else {
            canvas.drawText(descText, pad, descY, smallLabelPaint)
        }

        val lineY = h - 2f
        linePaint.color = Color.argb(40, 0, 255, 200); linePaint.strokeWidth = 1f
        canvas.drawLine(pad, lineY, w - pad, lineY, linePaint)
    }

    private fun drawMolecule(canvas: Canvas, left: Float, top: Float, w: Float, h: Float, compound: CookbookCompound) {
        val structure = compound.structure ?: return
        val cx = left + w / 2f + structure.centerOffsetX
        val cy = top + h / 2f + structure.centerOffsetY
        val scale = minOf(w, h) / 280f

        val rotAngle = time * 0.3f
        val breathe = 1f + sin(time * 0.8f) * 0.03f

        for (bond in structure.bonds) {
            if (bond.from >= structure.atoms.size || bond.to >= structure.atoms.size) continue
            val a1 = structure.atoms[bond.from]; val a2 = structure.atoms[bond.to]
            val x1 = cx + (a1.x * cos(rotAngle) - a1.y * sin(rotAngle)) * scale * breathe
            val y1 = cy + (a1.x * sin(rotAngle) + a1.y * cos(rotAngle)) * scale * breathe
            val x2 = cx + (a2.x * cos(rotAngle) - a2.y * sin(rotAngle)) * scale * breathe
            val y2 = cy + (a2.x * sin(rotAngle) + a2.y * cos(rotAngle)) * scale * breathe

            bondPaint.strokeWidth = 3f
            bondPaint.color = Color.argb(120, 0, 200, 150)
            canvas.drawLine(x1, y1, x2, y2, bondPaint)

            if (bond.order == 2) {
                val dx = x2 - x1; val dy = y2 - y1
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    val nx = -dy / len * 4f; val ny = dx / len * 4f
                    bondPaint.strokeWidth = 2f
                    bondPaint.color = Color.argb(80, 0, 200, 150)
                    canvas.drawLine(x1 + nx, y1 + ny, x2 + nx, y2 + ny, bondPaint)
                }
            }
        }

        for (atom in structure.atoms) {
            val ax = cx + (atom.x * cos(rotAngle) - atom.y * sin(rotAngle)) * scale * breathe
            val ay = cy + (atom.x * sin(rotAngle) + atom.y * cos(rotAngle)) * scale * breathe
            val radius = when (atom.symbol) {
                "C" -> 10f; "O" -> 12f; "N" -> 12f; "H" -> 6f; "Cl" -> 14f; else -> 10f
            } * scale

            val glowR = radius * 2.5f
            glowPaint.shader = RadialGradient(ax, ay, glowR,
                intArrayOf(colorWithAlpha(atom.color, 60), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(ax, ay, glowR, glowPaint)
            glowPaint.shader = null

            atomPaint.color = Color.argb(200, Color.red(atom.color), Color.green(atom.color), Color.blue(atom.color))
            canvas.drawCircle(ax, ay, radius, atomPaint)

            atomPaint.color = atom.color
            canvas.drawCircle(ax, ay, radius * 0.7f, atomPaint)

            if (atom.symbol != "C") {
                atomTextPaint.textSize = (11f * scale).coerceIn(8f, 16f)
                atomTextPaint.color = Color.WHITE
                canvas.drawText(atom.symbol, ax, ay + 4f * scale, atomTextPaint)
            }
        }
    }

    private fun drawInterferogram(canvas: Canvas, w: Float, h: Float) {
        val marginL = 38f; val marginR = 8f; val marginT = 16f; val marginB = 28f
        val plotL = marginL; val plotT = marginT; val plotR = w - marginR; val plotB = h - marginB
        val plotW = plotR - plotL; val plotH = plotB - plotT

        rect.set(plotL, plotT, plotR, plotB)
        boxPaint.color = darken(themeColors.bg, 0.9f); canvas.drawRoundRect(rect, 4f, 4f, boxPaint)
        linePaint.color = themeColors.line; linePaint.strokeWidth = 1f; canvas.drawRoundRect(rect, 4f, 4f, linePaint)

        gridPaint.color = colorWithAlpha(themeColors.line, 50); gridPaint.strokeWidth = 0.8f
        canvas.drawLine(plotL, plotT + plotH * 0.5f, plotR, plotT + plotH * 0.5f, gridPaint)
        canvas.drawLine(plotL + plotW * 0.5f, plotT, plotL + plotW * 0.5f, plotB, gridPaint)

        smallLabelPaint.color = themeColors.muted; smallLabelPaint.textSize = 11f; smallLabelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("0", plotL + plotW * 0.5f, plotB + 14f, smallLabelPaint)
        canvas.drawText("-OPD", plotL + 5f, plotB + 14f, smallLabelPaint)
        canvas.drawText("+OPD", plotR - 5f, plotB + 14f, smallLabelPaint)
        smallLabelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("ZPD", plotL - 4f, plotT + plotH * 0.5f + 4f, smallLabelPaint)

        spectrumPaint.color = C_GREEN; spectrumPaint.strokeWidth = 1.8f; spectrumPaint.style = Paint.Style.STROKE; spectrumPaint.strokeCap = Paint.Cap.ROUND
        path.reset()
        for (i in interferogramData.indices) {
            val x = plotL + plotW * i / interferogramData.size
            val y = plotT + plotH * 0.5f - interferogramData[i] * plotH * 0.45f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, spectrumPaint)

        labelPaint.textSize = 13f; labelPaint.color = C_CYAN; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("İnterferogram", plotL + plotW / 2f, plotT - 4f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSpectrum(canvas: Canvas, left: Float, top: Float, w: Float, h: Float) {
        val mL = 42f; val mR = 10f; val mT = 18f; val mB = 30f
        val pL = left + mL; val pT = top + mT; val pR = left + w - mR; val pB = top + h - mB
        val pW = pR - pL; val pH = pB - pT

        rect.set(pL, pT, pR, pB)
        boxPaint.color = darken(themeColors.bg, 0.85f); canvas.drawRoundRect(rect, 6f, 6f, boxPaint)
        linePaint.color = Color.argb(60, 0, 255, 200); linePaint.strokeWidth = 1f; canvas.drawRoundRect(rect, 6f, 6f, linePaint)

        gridPaint.color = Color.argb(25, 0, 255, 200); gridPaint.strokeWidth = 0.6f
        for (pct in listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)) canvas.drawLine(pL, pT + pH * pct, pR, pT + pH * pct, gridPaint)
        for (wn in listOf(4000f, 3500f, 3000f, 2500f, 2000f, 1500f, 1000f, 500f)) {
            val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawLine(x, pT, x, pB, gridPaint)
        }

        smallLabelPaint.color = themeColors.muted; smallLabelPaint.textSize = 10f; smallLabelPaint.textAlign = Paint.Align.CENTER
        for (wn in listOf(4000f, 3500f, 3000f, 2500f, 2000f, 1500f, 1000f, 500f)) {
            val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawText("${wn.toInt()}", x, pB + 14f, smallLabelPaint)
        }
        smallLabelPaint.textAlign = Paint.Align.RIGHT
        for (t in listOf(100, 80, 60, 40, 20, 0)) {
            val y = pT + pH * (1f - t / 100f); canvas.drawText("$t", pL - 6f, y + 4f, smallLabelPaint)
        }

        labelPaint.textSize = 10f; labelPaint.color = themeColors.muted; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Wavenumber (cm⁻¹)", pL + pW / 2f, pB + 26f, labelPaint)
        canvas.save(); canvas.rotate(-90f, pL - 32f, pT + pH / 2f)
        canvas.drawText("Transmittance (%T)", pL - 32f, pT + pH / 2f, labelPaint); canvas.restore()

        if (selectedGroups.isEmpty()) {
            labelPaint.textSize = 13f; labelPaint.color = themeColors.muted; labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Fonksiyonel grup seçin veya Kitap'tan hazır bileşik seçin", pL + pW / 2f, pT + pH / 2f, labelPaint)
            labelPaint.textAlign = Paint.Align.LEFT; return
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        fillPaint.shader = LinearGradient(0f, pT, 0f, pB, Color.argb(35, 0, 255, 200), Color.argb(5, 0, 255, 200), Shader.TileMode.CLAMP)
        val fillPath = Path()
        fillPath.moveTo(pL, pB)
        var firstFill = true
        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            val x = pL + pW * (1f - (wn - 400f) / 3600f)
            val y = pT + pH * (1f - spectrumData[i])
            if (x < pL || x > pR) continue
            if (firstFill) { fillPath.lineTo(x, y); firstFill = false } else fillPath.lineTo(x, y)
        }
        fillPath.lineTo(pR, pB); fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        spectrumPaint.color = C_CYAN; spectrumPaint.strokeWidth = 2.5f
        spectrumPaint.style = Paint.Style.STROKE; spectrumPaint.strokeCap = Paint.Cap.ROUND; spectrumPaint.strokeJoin = Paint.Join.ROUND
        path.reset(); var first = true
        for (i in spectrumData.indices) {
            val wn = 4000f - i * (3600f / spectrumData.size)
            val x = pL + pW * (1f - (wn - 400f) / 3600f)
            val y = pT + pH * (1f - spectrumData[i])
            if (x < pL || x > pR) continue
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
        }
        canvas.drawPath(path, spectrumPaint)

        val labelPositions = mutableListOf<Pair<Float, Float>>()
        var peakIdx = 1
        for (group in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(group.id)) continue
            val x = pL + pW * (1f - (group.peakCenter - 400f) / 3600f)
            val idx = ((4000f - group.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1)
            val y = pT + pH * (1f - spectrumData[idx])
            if (x < pL || x > pR) continue

            peakLinePaint.color = colorWithAlpha(group.color, 80); peakLinePaint.strokeWidth = 1f
            peakLinePaint.pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
            canvas.drawLine(x, y, x, pT + 2f, peakLinePaint)
            peakLinePaint.pathEffect = null

            peakDotPaint.color = group.color; canvas.drawCircle(x, y, 6f, peakDotPaint)
            peakDotPaint.color = Color.argb(50, Color.red(group.color), Color.green(group.color), Color.blue(group.color))
            canvas.drawCircle(x, y, 12f, peakDotPaint)

            val idxText = "$peakIdx"
            peakLabelPaint.textSize = 11f; peakLabelPaint.color = Color.WHITE
            val idxW = peakLabelPaint.measureText(idxText) + 10f
            val idxH = 18f
            var labelY = pT + 2f; var tries = 0
            while (tries < 12) {
                val collision = labelPositions.any { (lx, ly) -> abs(lx - x) < idxW * 0.9f && abs(ly - labelY) < idxH + 2f }
                if (!collision) break
                labelY += idxH + 2f; tries++
            }
            labelPositions.add(Pair(x, labelY))

            peakBgPaint.color = Color.argb(200, 10, 14, 20)
            labelBgRect.set(x - idxW / 2f, labelY, x + idxW / 2f, labelY + idxH)
            canvas.drawRoundRect(labelBgRect, 4f, 4f, peakBgPaint)
            canvas.drawText(idxText, x, labelY + 13f, peakLabelPaint)

            peakIdx++
        }

        if (showCursor && cursorX >= pL && cursorX <= pR && cursorY >= pT && cursorY <= pB) {
            val cursorP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(60, 255, 255, 255)
                pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
            }
            canvas.drawLine(cursorX, pT, cursorX, pB, cursorP)
            canvas.drawLine(pL, cursorY, pR, cursorY, cursorP)
            val cWn = 4000f - (cursorX - pL) / pW * 3600f
            val cT = 100f - (cursorY - pT) / pH * 100f
            val infoBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 18, 24, 32); isAntiAlias = true }
            canvas.drawRoundRect(pL + 4f, pB + 16f, pR - 4f, pB + 32f, 6f, 6f, infoBg)
            smallLabelPaint.textSize = 10f; smallLabelPaint.color = C_CYAN; smallLabelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("WN: ${"%.0f".format(cWn)} cm⁻¹  |  T: ${"%.1f".format(cT)}%", pL + pW / 2, pB + 28f, smallLabelPaint)
        }

        labelPaint.textSize = 12f; labelPaint.color = C_CYAN; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("FT-IR Spektrumu", pL + pW / 2f, top + 14f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawInfo(c: Canvas, w: Float, h: Float) {
        val px = w * 0.03f; val py = 8f; val pw = w * 0.94f; val ph = h - 16f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(10, 14, 20); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = C_CYAN; isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; textAlign = Paint.Align.CENTER; color = C_CYAN; isFakeBoldText = true; isAntiAlias = true }
        c.drawText("FT-IR Simülatörü", w / 2f, ty, hp); ty += 38f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; textAlign = Paint.Align.LEFT; isAntiAlias = true }
        val lines = listOf(
            Pair("═══ NEDİR? ═══", C_CYAN),
            Pair("Fourier Dönüşümle Kızılötesi Spektroskopisi", Color.rgb(220, 220, 220)),
            Pair("Moleküllerin fonksiyonel gruplarını belirler.", Color.rgb(220, 220, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ NASIL ÇALIŞIR? ═══", C_CYAN),
            Pair("1. IR Kaynak: Kızılötesi ışık yayar", Color.rgb(170, 204, 255)),
            Pair("2. Michelson İnterferometresi: Işığı ikiye böler", Color.rgb(170, 204, 255)),
            Pair("3. Sabit ve Hareketli Aynalar", Color.rgb(170, 204, 255)),
            Pair("4. Numune: IR ışığını emer", Color.rgb(170, 204, 255)),
            Pair("5. Dedektör: Geçen ışığı ölçer", Color.rgb(170, 204, 255)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ BÖLGELER ═══", C_CYAN),
            Pair("Fonksiyonel Bölge: 4000-1500 cm⁻¹", Color.rgb(100, 255, 160)),
            Pair("Parmak İzi: 1500-400 cm⁻¹", Color.rgb(255, 200, 100)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", C_CYAN),
            Pair("1. Fonksiyonel grupları seçin veya Kitap'tan", Color.rgb(200, 230, 255)),
            Pair("2. Tarama Başlat ile spektrum oluşturun", Color.rgb(200, 230, 255)),
            Pair("3. Çimdikleme ile yakınlaştırın", Color.rgb(200, 230, 255)),
            Pair("4. ParmağınızlaWN ve T değerlerini görün", Color.rgb(200, 230, 255))
        )
        for ((line, color) in lines) { if (line.isEmpty()) { ty += 6f; continue }; lp.color = color; c.drawText(line, px + 18f, ty, lp); ty += 22f }
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun darken(color: Int, factor: Float): Int =
        Color.rgb((Color.red(color) * factor).toInt().coerceIn(0, 255), (Color.green(color) * factor).toInt().coerceIn(0, 255), (Color.blue(color) * factor).toInt().coerceIn(0, 255))
}
