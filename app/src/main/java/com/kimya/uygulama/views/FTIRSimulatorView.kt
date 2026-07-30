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

    data class CookbookCompound(
        val name: String, val formula: String,
        val groups: List<String>, val description: String
    )

    data class ThemeColors(
        val bg: Int = Color.rgb(13, 17, 23),
        val surface: Int = Color.rgb(22, 27, 34),
        val primary: Int = Color.rgb(0, 240, 255),
        val text: Int = Color.rgb(230, 237, 243),
        val muted: Int = Color.rgb(139, 148, 158),
        val accent: Int = Color.rgb(57, 255, 20),
        val line: Int = Color.rgb(48, 54, 61)
    )

    companion object {
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
                "Geniş O-H (3350), C-H (2920), C-O (1100) - Alkolün klasik spektrumu"),
            CookbookCompound("Metanol", "CH₃OH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "O-H (3350), C-H (2920), C-O (1050)"),
            CookbookCompound("İzopropanol", "(CH₃)₂CHOH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "O-H (3350), C-H (2920), C-O (1150)"),
            CookbookCompound("1-Butanol", "C₄H₉OH",
                listOf("oh_alcohol", "ch_alkane", "co_alcohol"),
                "O-H (3350), C-H (2920), C-O (1070)"),
            CookbookCompound("Fenol", "C₆H₅OH",
                listOf("oh_alcohol", "ch_aro", "cc_aro", "co_alcohol"),
                "O-H (3350), Aromatik C-H (3050), C=C (1500), C-O (1200)"),
            CookbookCompound("Gliserol", "C₃H₈O₃",
                listOf("oh_alcohol", "co_alcohol", "ch_alkane"),
                "3x O-H (3350), C-O (1100) - Çok geniş O-H bandı"),
            CookbookCompound("Aseton", "CH₃COCH₃",
                listOf("co_ketone", "ch_alkane"),
                "Güçlü C=O (1715), C-H (2920) - Keton referansı"),
            CookbookCompound("2-Butanon (MEK)", "CH₃COC₂H₅",
                listOf("co_ketone", "ch_alkane"),
                "C=O (1715), C-H (2920)"),
            CookbookCompound("Sikloheksanon", "C₆H₁₀O",
                listOf("co_ketone", "ch_alkane"),
                "C=O (1715), C-H (2920) - Halkalı keton"),
            CookbookCompound("Asetofenon", "C₆H₅COCH₃",
                listOf("co_ketone", "ch_aro", "cc_aro", "ch_alkane"),
                "C=O (1715), Aromatik C=C (1500, 1600), C-H (2920)"),
            CookbookCompound("Benzaldehit", "C₆H₅CHO",
                listOf("co_aldehyde", "ch_aldehyde", "ch_aro", "cc_aro"),
                "C=O (1730), C-H Fermi çift (2720, 2820), Aromatik (3050, 1500)"),
            CookbookCompound("Asetaldehit", "CH₃CHO",
                listOf("co_aldehyde", "ch_aldehyde", "ch_alkane"),
                "C=O (1730), C-H Fermi çift (2720, 2820), C-H (2920)"),
            CookbookCompound("Formaldehit", "HCHO",
                listOf("co_aldehyde"),
                "C=O (1745) - En basit aldehit"),
            CookbookCompound("Asetik Asit", "CH₃COOH",
                listOf("oh_acid", "co_acid", "co_alcohol"),
                "Çok geniş O-H (2500-3300), C=O (1710), C-O (1240)"),
            CookbookCompound("Propiyonik Asit", "C₂H₅COOH",
                listOf("oh_acid", "co_acid", "ch_alkane", "co_alcohol"),
                "Geniş O-H, C=O (1710), C-H (2920), C-O (1240)"),
            CookbookCompound("Benzoik Asit", "C₆H₅COOH",
                listOf("oh_acid", "co_acid", "ch_aro", "cc_aro"),
                "Geniş O-H, C=O (1690), Aromatik C=C (1500)"),
            CookbookCompound("Etil Asetat", "CH₃COOC₂H₅",
                listOf("co_ester", "co_ester_coc", "ch_alkane"),
                "C=O (1740), C-O-C (1240), C-H (2920)"),
            CookbookCompound("Metil Benzoat", "C₆H₅COOCH₃",
                listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro"),
                "C=O (1724), C-O-C (1275), Aromatik C=C (1500)"),
            CookbookCompound("Etil Benzoat", "C₆H₅COOC₂H₅",
                listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"),
                "C=O (1720), C-O-C (1270), Aromatik + Alkil C-H"),
            CookbookCompound("Benzen", "C₆H₆",
                listOf("ch_aro", "cc_aro"),
                "Aromatik C-H (3050), C=C halka (1500, 1600)"),
            CookbookCompound("Toluen", "C₆H₅CH₃",
                listOf("ch_aro", "cc_aro", "ch_alkane"),
                "Aromatik C-H (3050), C=C (1500, 1600), CH₃ (2920)"),
            CookbookCompound("Hekzan", "C₆H₁₄",
                listOf("ch_alkane"),
                "Sadece C-H (2920, 2850) absorpsiyonları - Basit spektrum"),
            CookbookCompound("Sikloheksan", "C₆H₁₂",
                listOf("ch_alkane"),
                "C-H (2920, 2850) - Halkalı alkan"),
            CookbookCompound("1-Heksen", "C₆H₁₂",
                listOf("ch_alkane", "ch_alkene", "cc_alkene"),
                "C-H alkane (2920), =C-H (3080), C=C (1650)"),
            CookbookCompound("Stiren", "C₆H₅CH=CH₂",
                listOf("ch_aro", "cc_aro", "ch_alkene", "cc_alkene"),
                "Aromatik + Alken C-H, C=C (1630), Aromatik C=C (1500)"),
            CookbookCompound("Anilin", "C₆H₅NH₂",
                listOf("nh_primary", "ch_aro", "cc_aro"),
                "N-H çift pik (3400), Aromatik C-H (3050), C=C (1500)"),
            CookbookCompound("Dietilamin", "(C₂H₅)₂NH",
                listOf("nh_secondary", "ch_alkane", "co_alcohol"),
                "N-H tek pik (3330), C-H (2920)"),
            CookbookCompound("Asetonitril", "CH₃CN",
                listOf("cn_nitrile", "ch_alkane"),
                "C≡N (2250), C-H (2920)"),
            CookbookCompound("Benzonitril", "C₆H₅CN",
                listOf("cn_nitrile", "ch_aro", "cc_aro"),
                "C≡N (2230), Aromatik C-H (3050), C=C (1500)"),
            CookbookCompound("Kloroform", "CHCl₃",
                listOf("c_cl", "ch_alkane"),
                "C-Cl (760), C-H (3020) - Çözücü"),
            CookbookCompound("Diklorometan", "CH₂Cl₂",
                listOf("c_cl", "ch_alkane"),
                "C-Cl (700), C-H (2920)"),
            CookbookCompound("Karbon Tetraklorür", "CCl₄",
                listOf("c_cl"),
                "C-Cl (780, 820) - Sadece C-Cl absorpsiyonu"),
            CookbookCompound("Asetamid", "CH₃CONH₂",
                listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"),
                "N-H (3300), Amid I (1660), Amid II (1540), C-H (2920)"),
            CookbookCompound("Naylon 6,6", "(C₁₂H₂₂N₂O₂)ₙ",
                listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"),
                "N-H (3300), Amid I (1660), Amid II (1540), C-H (2920)"),
            CookbookCompound("Nitrobenzen", "C₆H₅NO₂",
                listOf("no2", "ch_aro", "cc_aro"),
                "NO₂ (1540), Aromatik C-H (3050), C=C (1500)"),
            CookbookCompound("Dietil Eter", "(C₂H₅)₂O",
                listOf("ch_alkane", "co_alcohol"),
                "C-H (2920), C-O (1120) - Eter bandı"),
            CookbookCompound("Tetrahidrofuran (THF)", "C₄H₈O",
                listOf("ch_alkane", "co_alcohol"),
                "C-H (2920), C-O (1070) - Halkalı eter"),
            CookbookCompound("Difenil Metan", "(C₆H₅)₂CH₂",
                listOf("ch_aro", "cc_aro", "ch_alkane"),
                "Aromatik C-H (3050), C=C (1500, 1600), CH₂ (2920)"),
            CookbookCompound("Kağıt (Selüloz)", "(C₆H₁₀O₅)ₙ",
                listOf("oh_alcohol", "co_alcohol", "ch_alkane"),
                "Geniş O-H (3350), C-O (1050), C-H (2920) - Selüloz iskeleti"),
            CookbookCompound("PET Plastik", "(C₁₀H₈O₄)ₙ",
                listOf("co_ester", "co_ester_coc", "ch_aro", "cc_aro", "ch_alkane"),
                "C=O (1720), C-O-C (1240), Aromatik C=C (1500), C-H (2920)"),
            CookbookCompound("Polietilen (PE)", "(C₂H₄)ₙ",
                listOf("ch_alkane"),
                "Yoğun C-H (2920, 2850, 1470, 1370) - Sadece alkan bantları"),
            CookbookCompound("Polipropilen (PP)", "(C₃H₆)ₙ",
                listOf("ch_alkane"),
                "C-H (2920, 2840, 1378) - Metil dalgalanması belirgin"),
            CookbookCompound("PVC", "(C₂H₃Cl)ₙ",
                listOf("ch_alkane", "c_cl"),
                "C-H (2920), C-Cl (690, 615) - Polivinil klorür"),
            CookbookCompound("Polistiren (PS)", "(C₈H₈)ₙ",
                listOf("ch_aro", "cc_aro", "ch_alkane"),
                "Aromatik C-H (3025), C=C (1600, 1492, 1452), C-H (2920)"),
            CookbookCompound("Naylon (PA6)", "(C₆H₁₁NO)ₙ",
                listOf("nh_amide", "co_amide1", "nh_bend", "ch_alkane"),
                "N-H (3300), Amid I (1640), Amid II (1540), C-H (2930)"),
            CookbookCompound("Lastik (Doğal Kauçuk)", "(C₅H₈)ₙ",
                listOf("ch_alkane", "ch_alkene", "cc_alkene"),
                "C-H (2920), =C-H (3040), C=C (1660) - Poliizopren"),
            CookbookCompound("Naylon Çorap (Elastan)", "(C₁₅H₂₂N₂O₂)ₙ",
                listOf("nh_amide", "co_amide1", "co_alcohol", "ch_alkane"),
                "N-H (3330), Amid I (1700), Amid II (1540), C-O (1100)"),
            CookbookCompound("Şeker (Sucroz)", "C₁₂H₂₂O₁₁",
                listOf("oh_alcohol", "co_alcohol", "ch_alkane"),
                "Yoğun O-H (3400), C-O (1000-1100), C-H (2920)"),
            CookbookCompound("Tuz (NaCl)", "NaCl",
                listOf("c_cl"),
                "Na-Cl absorpsiyonu (600 civarı) - Basit inorganik tuz"),
            CookbookCompound("Sirke (Asetik Asit Ç.)", "CH₃COOH + H₂O",
                listOf("oh_acid", "co_acid", "oh_alcohol", "co_alcohol"),
                "Geniş O-H, C=O (1710), C-O (1240) - Seyreltik asit"),
            CookbookCompound("Yağ (Trigliserit)", "C₅₅H₉₈O₆",
                listOf("ch_alkane", "co_ester"),
                "C-H (2920, 2850), C=O (1745), C-O (1160) - Uzun zincirli ester"),
            CookbookCompound("E Vitamini", "C₂₉H₅₀O₂",
                listOf("oh_alcohol", "ch_alkane", "ch_aro", "cc_aro"),
                "O-H (3400), C-H (2920), Aromatik C=C (1500)")
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

    private var time = 0f
    private var themeColors = ThemeColors()
    private val spectrumData = FloatArray(2000)
    private val interferogramData = FloatArray(200)

    // Zoom & Pan
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var touchMode = 0
    private val sDetector: ScaleGestureDetector

    // Cursor
    private var cursorX = 0f; private var cursorY = 0f; private var showCursor = false
    private var tapTime = 0L; private var lastTapTime = 0L

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            time += 0.03f
            updateData()
            invalidate()
            handler.postDelayed(this, 16L)
        }
    }

    init {
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean { return true }
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val factor = d.scaleFactor
                val pivotX = d.focusX; val pivotY = d.focusY
                // Zoom around the pinch point
                val newScale = (zoomScale * factor).coerceIn(0.5f, 4f)
                val scaleChange = newScale / zoomScale
                // Adjust pan so the pivot point stays fixed
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
                2 -> { val dx = e.x - lastTx; val dy = e.y - lastTy; if (abs(dx) > 5 || abs(dy) > 5) touchMode = 1; if (touchMode == 1) { panX += dx; panY += dy; lastTx = e.x; lastTy = e.y } }
                1, 3 -> {
                    if (touchMode == 0 && System.currentTimeMillis() - tapTime < 300) {
                        // Double tap detection
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            // Double tap: reset zoom and pan
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
    private val tinyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f; textAlign = Paint.Align.CENTER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val peakLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 1f
    }
    private val peakBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val peakLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
    }
    private val peakWnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f; textAlign = Paint.Align.CENTER
    }
    private val peakDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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

        canvas.save()
        canvas.scale(zoomScale, zoomScale, w / 2, h / 2)
        canvas.translate(panX / zoomScale, panY / zoomScale)

        if (showInterferogram) {
            drawInterferogram(canvas, w, h)
        } else {
            drawSchematic(canvas, w, h * 0.06f)
            drawSpectrum(canvas, 0f, h * 0.07f, w, h * 0.93f)
        }

        canvas.restore()

        // Info overlay
        if (showInfo) drawInfo(canvas, w, h)
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

        spectrumPaint.color = themeColors.accent; spectrumPaint.strokeWidth = 1.8f; spectrumPaint.style = Paint.Style.STROKE; spectrumPaint.strokeCap = Paint.Cap.ROUND
        path.reset()
        for (i in interferogramData.indices) {
            val x = plotL + plotW * i / interferogramData.size
            val y = plotT + plotH * 0.5f - interferogramData[i] * plotH * 0.45f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, spectrumPaint)

        labelPaint.textSize = 13f; labelPaint.color = themeColors.primary; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("İnterferogram", plotL + plotW / 2f, plotT - 4f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSchematic(canvas: Canvas, w: Float, h: Float) {
        val cy = h * 0.55f; val dotR = h * 0.2f; val seg = w / 7f
        val irPulse = (0.6f + sin(time * 3f) * 0.2f).coerceIn(0.4f, 0.9f); val irAlpha = (irPulse * 255).toInt()

        laserPaint.strokeWidth = 2f; laserPaint.color = Color.argb(irAlpha, 255, 130, 60)
        canvas.drawLine(seg * 0.4f, cy, seg * 6.6f, cy, laserPaint)

        val xS = seg * 0.5f
        glowPaint.shader = RadialGradient(xS, cy, dotR, intArrayOf(Color.argb(irAlpha, 255, 120, 40), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(xS, cy, dotR, glowPaint); glowPaint.shader = null
        labelPaint.textSize = 7f; labelPaint.color = themeColors.primary; canvas.drawText("Kaynak", xS, cy + h * 0.4f, labelPaint)

        val xB = seg * 1.8f
        path.reset(); path.moveTo(xB, cy - dotR * 0.8f); path.lineTo(xB + dotR * 0.8f, cy)
        path.lineTo(xB, cy + dotR * 0.8f); path.lineTo(xB - dotR * 0.8f, cy); path.close()
        boxPaint.color = colorWithAlpha(themeColors.accent, 90); canvas.drawPath(path, boxPaint)
        labelPaint.textSize = 6f; labelPaint.color = themeColors.accent; canvas.drawText("Bölücü", xB, cy + h * 0.4f, labelPaint)

        val xM = seg * 2.6f
        rect.set(xM - dotR * 0.9f, cy - 2f, xM + dotR * 0.9f, cy + 2f)
        boxPaint.color = themeColors.muted; canvas.drawRect(rect, boxPaint)
        labelPaint.textSize = 6f; labelPaint.color = themeColors.muted; canvas.drawText("Aynalar", xM, cy + h * 0.4f, labelPaint)

        val xSm = seg * 3.8f
        rect.set(xSm - dotR, cy - dotR * 0.7f, xSm + dotR, cy + dotR * 0.7f)
        boxPaint.color = Color.argb(40, 180, 180, 180); canvas.drawRoundRect(rect, 3f, 3f, boxPaint)
        labelPaint.textSize = 6f; labelPaint.color = themeColors.text; canvas.drawText("Numune", xSm, cy + h * 0.4f, labelPaint)

        val xD = seg * 5.5f
        rect.set(xD - dotR * 0.8f, cy - dotR * 0.6f, xD + dotR * 0.8f, cy + dotR * 0.6f)
        boxPaint.color = themeColors.surface; canvas.drawRoundRect(rect, 4f, 4f, boxPaint)
        if (selectedGroups.isNotEmpty() && !isScanning) {
            val dA = (90 + sin(time * 4f) * 40).toInt().coerceIn(50, 180)
            glowPaint.shader = RadialGradient(xD, cy, dotR * 0.7f, intArrayOf(colorWithAlpha(themeColors.primary, dA), Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(xD, cy, dotR * 0.7f, glowPaint); glowPaint.shader = null
        }
        labelPaint.textSize = 6f; labelPaint.color = themeColors.primary; canvas.drawText("Dedektör", xD, cy + h * 0.4f, labelPaint)

        if (isScanning) {
            val barW = w * 0.35f; val barH = 3f; val barX = (w - barW) / 2f; val barY = h - 4f
            rect.set(barX, barY, barX + barW, barY + barH)
            boxPaint.color = darken(themeColors.surface, 0.5f); canvas.drawRoundRect(rect, 1f, 1f, boxPaint)
            rect.set(barX, barY, barX + barW * scanProgress, barY + barH)
            boxPaint.color = themeColors.primary; canvas.drawRoundRect(rect, 1f, 1f, boxPaint)
        }
    }

    private fun drawSpectrum(canvas: Canvas, left: Float, top: Float, w: Float, h: Float) {
        val mL = 38f; val mR = 8f; val mT = 16f; val mB = 28f
        val pL = left + mL; val pT = top + mT; val pR = left + w - mR; val pB = top + h - mB
        val pW = pR - pL; val pH = pB - pT

        rect.set(pL, pT, pR, pB)
        boxPaint.color = darken(themeColors.bg, 0.85f); canvas.drawRoundRect(rect, 4f, 4f, boxPaint)
        linePaint.color = themeColors.line; linePaint.strokeWidth = 1f; canvas.drawRoundRect(rect, 4f, 4f, linePaint)

        // Grid
        gridPaint.color = colorWithAlpha(themeColors.line, 50); gridPaint.strokeWidth = 0.8f
        for (pct in listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)) canvas.drawLine(pL, pT + pH * pct, pR, pT + pH * pct, gridPaint)
        for (wn in listOf(4000f, 3500f, 3000f, 2500f, 2000f, 1500f, 1000f, 500f)) {
            val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawLine(x, pT, x, pB, gridPaint)
        }

        // Labels
        smallLabelPaint.color = themeColors.muted; smallLabelPaint.textSize = 11f; smallLabelPaint.textAlign = Paint.Align.CENTER
        for (wn in listOf(4000f, 3500f, 3000f, 2500f, 2000f, 1500f, 1000f, 500f)) {
            val x = pL + pW * (1f - (wn - 400f) / 3600f); canvas.drawText("${wn.toInt()}", x, pB + 14f, smallLabelPaint)
        }
        smallLabelPaint.textAlign = Paint.Align.RIGHT
        for (t in listOf(100, 80, 60, 40, 20, 0)) {
            val y = pT + pH * (1f - t / 100f); canvas.drawText("$t", pL - 4f, y + 4f, smallLabelPaint)
        }

        // Axis titles
        labelPaint.textSize = 11f; labelPaint.color = themeColors.muted; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Dalga Sayısı (cm⁻¹)", pL + pW / 2f, pB + 24f, labelPaint)
        canvas.save(); canvas.rotate(-90f, pL - 30f, pT + pH / 2f)
        canvas.drawText("İletim (%)", pL - 30f, pT + pH / 2f, labelPaint); canvas.restore()

        if (selectedGroups.isEmpty()) {
            labelPaint.textSize = 14f; labelPaint.color = themeColors.muted
            canvas.drawText("Fonksiyonel grup seçin veya Kitap'tan hazır bileşik seçin", pL + pW / 2f, pT + pH / 2f, labelPaint)
            labelPaint.textAlign = Paint.Align.LEFT; return
        }

        // Spectrum fill gradient
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; isAntiAlias = true }
        fillPaint.shader = LinearGradient(0f, pT, 0f, pB, Color.argb(30, 0, 200, 255), Color.argb(5, 0, 200, 255), Shader.TileMode.CLAMP)
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

        // Spectrum line
        spectrumPaint.color = themeColors.primary; spectrumPaint.strokeWidth = 2.5f
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

        // Peak labels with lines
        val labelPositions = mutableListOf<Pair<Float, Float>>()
        for (group in FUNCTIONAL_GROUPS) {
            if (!selectedGroups.contains(group.id)) continue
            val x = pL + pW * (1f - (group.peakCenter - 400f) / 3600f)
            val idx = ((4000f - group.peakCenter) / 3600f * spectrumData.size).toInt().coerceIn(0, spectrumData.size - 1)
            val y = pT + pH * (1f - spectrumData[idx])
            if (x < pL || x > pR) continue

            // Vertical line from peak to label area
            peakLinePaint.color = colorWithAlpha(group.color, 100); peakLinePaint.strokeWidth = 1f
            peakLinePaint.pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
            canvas.drawLine(x, y, x, pT + 2f, peakLinePaint)
            peakLinePaint.pathEffect = null

            peakDotPaint.color = group.color; canvas.drawCircle(x, y, 5f, peakDotPaint)
            peakDotPaint.color = Color.argb(60, Color.red(group.color), Color.green(group.color), Color.blue(group.color))
            canvas.drawCircle(x, y, 9f, peakDotPaint)

            val shortName = group.nameTr; val wnText = "${group.peakCenter.toInt()} cm⁻¹"
            peakLabelPaint.textSize = 11f; peakLabelPaint.color = group.color
            peakWnPaint.textSize = 9f; peakWnPaint.color = colorWithAlpha(Color.WHITE, 200)

            val nameW = peakLabelPaint.measureText(shortName); val wnW = peakWnPaint.measureText(wnText)
            val boxW = maxOf(nameW, wnW) + 10f; val boxH = 26f
            var labelY = pT + 2f; var tries = 0
            while (tries < 12) {
                val collision = labelPositions.any { (lx, ly) -> abs(lx - x) < boxW * 0.85f && abs(ly - labelY) < boxH + 2f }
                if (!collision) break
                labelY += boxH + 3f; tries++
            }
            labelPositions.add(Pair(x, labelY))

            peakBgPaint.color = Color.argb(220, 10, 13, 20)
            labelBgRect.set(x - boxW / 2f, labelY, x + boxW / 2f, labelY + boxH)
            canvas.drawRoundRect(labelBgRect, 3f, 3f, peakBgPaint)

            canvas.drawText(shortName, x, labelY + 11f, peakLabelPaint)
            canvas.drawText(wnText, x, labelY + 21f, peakWnPaint)
        }

        // Cursor crosshair
        if (showCursor && cursorX >= pL && cursorX <= pR && cursorY >= pT && cursorY <= pB) {
            val cursorP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(60, 255, 255, 255); pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f) }
            canvas.drawLine(cursorX, pT, cursorX, pB, cursorP)
            canvas.drawLine(pL, cursorY, pR, cursorY, cursorP)
            val cWn = 4000f - (cursorX - pL) / pW * 3600f
            val cT = 100f - (cursorY - pT) / pH * 100f
            val infoBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 22, 27, 34); isAntiAlias = true }
            canvas.drawRoundRect(pL + 4f, pB + 16f, pR - 4f, pB + 32f, 6f, 6f, infoBg)
            smallLabelPaint.textSize = 10f; smallLabelPaint.color = themeColors.primary; smallLabelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("WN: ${"%.0f".format(cWn)} cm⁻¹  |  T: ${"%.1f".format(cT)}%", pL + pW / 2, pB + 28f, smallLabelPaint)
        }

        // Title
        labelPaint.textSize = 13f; labelPaint.color = themeColors.primary; labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("FT-IR Spektrumu", pL + pW / 2f, top + 12f, labelPaint)
        labelPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawInfo(c: Canvas, w: Float, h: Float) {
        val px = w * 0.03f; val py = 8f; val pw = w * 0.94f; val ph = h - 16f
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 39); isAntiAlias = true })
        c.drawRoundRect(px, py, px + pw, py + ph, 20f, 20f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2f; color = Color.rgb(0, 200, 255); isAntiAlias = true })
        var ty = py + 40f
        val hp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f; textAlign = Paint.Align.CENTER; color = Color.rgb(0, 240, 255); isFakeBoldText = true; isAntiAlias = true }
        c.drawText("FT-IR Simülatörü", w / 2f, ty, hp); ty += 38f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; textAlign = Paint.Align.LEFT; isAntiAlias = true }
        val lines = listOf(
            Pair("═══ NEDİR? ═══", Color.rgb(0, 240, 255)),
            Pair("Fourier Dönüşümle Kızılötesi Spektroskopisi", Color.rgb(220, 220, 220)),
            Pair("Moleküllerin fonksiyonel gruplarını belirler.", Color.rgb(220, 220, 220)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ NASIL ÇALIŞIR? ═══", Color.rgb(0, 240, 255)),
            Pair("1. IR Kaynak: Kızılötesi ışık yayar", Color.rgb(170, 204, 255)),
            Pair("2. Michelson İnterferometresi: Işığı ikiye böler", Color.rgb(170, 204, 255)),
            Pair("3. Sabit ve Hareketli Aynalar", Color.rgb(170, 204, 255)),
            Pair("4. Numune: IR ışığını emer", Color.rgb(170, 204, 255)),
            Pair("5. Dedektör: Geçen ışığı ölçer", Color.rgb(170, 204, 255)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ BÖLGELER ═══", Color.rgb(0, 240, 255)),
            Pair("Fonksiyonel Bölge: 4000-1500 cm⁻¹", Color.rgb(100, 255, 160)),
            Pair("Parmak İzi: 1500-400 cm⁻¹", Color.rgb(255, 200, 100)),
            Pair("", Color.TRANSPARENT),
            Pair("═══ KULLANIM ═══", Color.rgb(0, 240, 255)),
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
