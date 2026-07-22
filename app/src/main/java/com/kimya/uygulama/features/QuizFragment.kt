package com.kimya.uygulama.features

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.viewmodel.KimyaViewModel

class QuizFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()

    data class Soru(val soru: String, val secenekler: List<String>, val dogru: Int, val kategori: String, val zorluk: String)

    private val tumSorular = listOf(
        Soru("Suyun formülü nedir?", listOf("H2O", "CO2", "NaCl", "CH4", "H2SO4"), 0, "Genel", "Kolay"),
        Soru("Asitlerin pH değeri?", listOf("0-7", "7-14", "14-100", "0-14", "7"), 0, "Genel", "Kolay"),
        Soru("H sembolü hangi elementtir?", listOf("Helyum", "Hidrojen", "Hafniyum", "Holmiyum", "Hafnium"), 1, "Genel", "Kolay"),
        Soru("Bir mol kaç tanecik içerir?", listOf("6.02x10²³", "3.01x10²³", "1x10²³", "12x10²³", "6.02x10²²"), 0, "Genel", "Kolay"),
        Soru("Organik bileşiklerin temel elementi?", listOf("Oksijen", "Azot", "Karbon", "Hidrojen", "Kükürt"), 2, "Organik", "Kolay"),
        Soru("Periyodik tabloda kaç periyot var?", listOf("5", "7", "8", "10", "6"), 1, "Genel", "Kolay"),
        Soru("Turnusol hangi tür belirtecidir?", listOf("Asit-baz", "Redox", "Kompleks", "Çökeltme", "Gaz"), 0, "Analitik", "Kolay"),
        Soru("İdeal gaz yasası nedir?", listOf("PV=nRT", "E=mc²", "F=ma", "V=IR", "pH=-log[H⁺]"), 0, "Fizikokimya", "Kolay"),
        Soru("Karbon atomu kaç bağ yapar?", listOf("2", "3", "4", "5", "6"), 2, "Organik", "Kolay"),
        Soru("Güçlü asit hangisidir?", listOf("CH₃COOH", "HF", "H₂CO₃", "HCl", "HNO₂"), 3, "Genel", "Orta"),
        Soru("En hafif element?", listOf("Helyum", "Hidrojen", "Lityum", "Berilyum", "Karbon"), 1, "Genel", "Kolay"),
        Soru("Oksijenin atom numarası?", listOf("6", "7", "8", "9", "10"), 2, "Genel", "Kolay"),
        Soru("Doymuş hidrokarbon hangisidir?", listOf("C₂H₄", "C₂H₂", "CH₄", "C₆H₆", "C₂H₆"), 2, "Organik", "Orta"),
        Soru("Öz ısı nedir?", listOf("Isı", "Sıcaklık", "1g'ın 1°C ısınması", "Entalpi", "Entropi"), 2, "Fizikokimya", "Orta"),
        Soru("NaOH hangi tür bileşiktir?", listOf("Asit", "Baz", "Tuz", "Organik", "Gaz"), 1, "Anorganik", "Kolay"),
        Soru("Monomerlerin uç uca eklenmesi?", listOf("Kondenzasyon", "Katılma", "Çapraz bağ", "Kraking", "Polimerizasyon"), 1, "Organik", "Orta"),
        Soru("DNA'nın yapı taşı nedir?", listOf("Amino asit", "Nükleotid", "Yağ asidi", "Monosakkarit", "Protein"), 1, "Biyokimya", "Orta"),
        Soru("Hangisi karbonhidrat değildir?", listOf("Glikoz", "Sakkaroz", "Kolesterol", "Selüloz", "Nişasta"), 2, "Biyokimya", "Orta"),
        Soru("pH=7 olan çözelti nedir?", listOf("Asidik", "Bazik", "Nötr", "Tampon", "Doymuş"), 2, "Analitik", "Kolay"),
        Soru("Proton sayısına ne denir?", listOf("Kütle no", "Atom no", "İzotop", "Nöutron no", "Elektron no"), 1, "Genel", "Kolay"),
        Soru("Polimerizasyonda katkılama?", listOf("Kondenzasyon", "Katılma", "Çapraz bağ", "Kraking", "Bölünme"), 1, "Organik", "Orta"),
        Soru("Suyun donma noktası?", listOf("-1°C", "0°C", "1°C", "100°C", "25°C"), 1, "Fizikokimya", "Kolay"),
        Soru("Metanın formülü?", listOf("C₂H₆", "C₃H₈", "CH₄", "C₄H₁₀", "C₂H₄"), 2, "Organik", "Kolay"),
        Soru("Yarı metal hangisidir?", listOf("Sodyum", "Klor", "Silisyum", "Argon", "Bakır"), 2, "Anorganik", "Orta"),
        Soru("Hızı artıran madde?", listOf("Katalizör", "Reaktant", "Ürün", "Çözücü", "İnhibitör"), 0, "Fizikokimya", "Kolay"),
        Soru("Elektron alan madde?", listOf("Yükseltgenen", "İndirgenen", "Nötral", "İyonlaşan", "Katalizör"), 1, "Anorganik", "Orta"),
        Soru("Hangisi alkoldür?", listOf("CH₃COOH", "C₂H₅OH", "C₆H₆", "CH₃CHO", "HCOOH"), 1, "Organik", "Kolay"),
        Soru("Amonyak formülü?", listOf("NH₃", "NO₂", "N₂O", "N₂H₄", "HNO₃"), 0, "Anorganik", "Kolay"),
        Soru("Motor yakıtı nedir?", listOf("Benzin", "Su", "Hava", "Kum", "Demir"), 0, "Organik", "Kolay"),
        Soru("Proteinlerin yapı taşı?", listOf("Nükleotid", "Amino asit", "Yağ asidi", "Şeker", "Vitamin"), 1, "Biyokimya", "Kolay"),
        Soru("Grup aynı olanların ortak özelliği?", listOf("Kütlesi", "Kimyasal özellik", "Nöutron sayısı", "Erime noktası", "Rengi"), 1, "Genel", "Orta"),
        Soru("Oda sıcaklığında sıvı element?", listOf("Cıva", "Demir", "Oksijen", "Karbon", "Altın"), 0, "Anorganik", "Orta"),
        Soru("SO₂'de S yükseltgenme basamağı?", listOf("+2", "+3", "+4", "+6", "+1"), 2, "Anorganik", "Orta"),
        Soru("Yenilenebilir enerji kaynağı?", listOf("Kömür", "Petrol", "Doğalgaz", "Güneş", "Uranyum"), 3, "Fizikokimya", "Kolay"),
        Soru("En yüksek elektronegatiflik?", listOf("Klor", "Oksijen", "Flor", "Azot", "İyod"), 2, "Genel", "Orta"),
        Soru("Zayıf baz hangisidir?", listOf("NaOH", "KOH", "NH₃", "Ca(OH)₂", "Ba(OH)₂"), 2, "Anorganik", "Orta"),
        Soru("Alkanların genel formülü?", listOf("CₙH₂ₙ", "CₙH₂ₙ₊₂", "CₙH₂ₙ₋₂", "CₙHₙ", "CₙH₂ₙ₊₁"), 1, "Organik", "Orta"),
        Soru("Çözeltide çözünen miktarına?", listOf("Hacim", "Derişim", "Sıcaklık", "Basınç", "Yoğunluk"), 1, "Analitik", "Orta"),
        Soru("Esterleşme sonucu açığa çıkan?", listOf("H₂", "CO₂", "H₂O", "O₂", "N₂"), 2, "Organik", "Orta"),
        Soru("C₆H₁₂O₆ formülü kime ait?", listOf("Sakkaroz", "Glikoz", "Selüloz", "Nişasta", "Fruktoz"), 1, "Biyokimya", "Orta"),
        Soru("Elektrolit olmayan?", listOf("NaCl", "HCl", "Şeker", "KOH", "H₂SO₄"), 2, "Analitik", "Orta"),
        Soru("Nükleer reaksiyonda korunan?", listOf("Kütle", "Enerji", "Kütle+Enerji", "Sıcaklık", "Hız"), 2, "Fizikokimya", "Zor"),
        Soru("Sera etkisi yapan gaz?", listOf("O₂", "CO₂", "N₂", "H₂", "Ar"), 1, "Fizikokimya", "Kolay"),
        Soru("İzotopta aynı olan?", listOf("Kütle no", "Proton sayısı", "Nötron sayısı", "Elektron sayısı", "Hacim"), 1, "Genel", "Orta"),
        Soru("Aromatik bileşik?", listOf("C₂H₄", "C₆H₆", "CH₄", "C₂H₂", "C₂H₆"), 1, "Organik", "Orta"),
        Soru("Biyopolimer hangisidir?", listOf("PET", "Selüloz", "PVC", "Polistiren", "Naylon"), 1, "Biyokimya", "Zor"),
        Soru("Petrol ayrıştırma yöntemi?", listOf("Kraking", "Damıtma", "Süzme", "Elektroliz", "Kristallendirme"), 1, "Organik", "Orta"),
        Soru("1 mol su kaç gram?", listOf("16g", "17g", "18g", "20g", "36g"), 2, "Genel", "Kolay"),
        Soru("İndirgen madde hangisidir?", listOf("O₂", "Cl₂", "Na", "F₂", "Br₂"), 2, "Anorganik", "Orta"),
        Soru("Kovalent bağda elektronlar?", listOf("Bir atom verir", "Ortak paylaşılır", "Serbest", "Yok", "Yutulur"), 1, "Anorganik", "Kolay"),
        Soru("Fotosentez ürünü?", listOf("CO₂", "H₂O", "Glikoz", "NaCl", "O₂"), 2, "Biyokimya", "Kolay"),
        Soru("Çekirdekteki parçacıklar?", listOf("Proton+Nöutron", "Proton+Elektron", "Nöutron+Elektron", "Sadece Proton", "Sadece Nöutron"), 0, "Genel", "Kolay"),
        Soru("Termoplastik olmayan?", listOf("PE", "PP", "PVC", "Bakalit", "PS"), 3, "Organik", "Zor"),
        Soru("Yanma tepkimesi türü?", listOf("Sentez", "Analiz", "Yükseltgenme", "Çökelme", "Deşarj"), 2, "Anorganik", "Orta"),
        Soru("Avogadro sayısı?", listOf("6.02×10²³", "3.14×10²³", "1.66×10²⁴", "9.11×10⁻³¹", "6.02×10²²"), 0, "Genel", "Kolay"),
        Soru("Lewis asidi nedir?", listOf("Proton verici", "Elektron çifti alıcı", "OH⁻ verici", "H⁺ verici", "Elektron verici"), 1, "Anorganik", "Zor"),
        Soru("İç enerji değişimi?", listOf("ΔH", "ΔU", "ΔG", "ΔS", "ΔP"), 1, "Fizikokimya", "Zor"),
        Soru("Elektromotor kuvvet birimi?", listOf("Volt", "Amper", "Ohm", "Watt", "Joule"), 0, "Fizikokimya", "Orta"),
        Soru("Hangisi amfoterdir?", listOf("NaOH", "Al(OH)₃", "HCl", "NaCl", "KOH"), 1, "Anorganik", "Zor"),
        Soru("Hidroliz nedir?", listOf("Su ile ayrışma", "Su ile birleşme", "Isı ile ayrışma", "Elektrik ile ayrışma", "Basınç ile ayrışma"), 0, "Anorganik", "Orta"),
        Soru("En çok bulunan gaz?", listOf("O₂", "N₂", "CO₂", "Ar", "H₂"), 1, "Genel", "Kolay"),
        Soru("Kararlı izotop?", listOf("¹⁴C", "¹²C", "³H", "¹³¹I", "²³⁵U"), 1, "Genel", "Orta"),
        Soru("Periyodik tabloda en çok element?", listOf("Geçiş metal", "Soy gaz", "Halogren", "Alkali metal", "Toprak alkali"), 0, "Genel", "Orta"),
        Soru("Koagülasyon nedir?", listOf("Çökelme", "Süspansiyon", "Emülsiyon", "Jel", "Kolloit"), 0, "Analitik", "Zor"),
        Soru("缓冲 çözelti nedir?", listOf("pH değişmeyen", "Asidik", "Bazik", "Nötr", "Doymuş"), 0, "Analitik", "Zor"),
        Soru("En çok elektronegatif?", listOf("Fr", "Cs", "F", "O", "Cl"), 2, "Genel", "Orta"),
        Soru("Redoks tepkimesinde indirgenen?", listOf("Oksijenlenir", "İndirgenir", "Değişmez", "Yükseltgenir", "Hidroliz olur"), 1, "Anorganik", "Orta"),
        Soru("Polietilen monomeri?", listOf("CH₂=CH₂", "CH≡CH", "CH₂=CHCl", "C₆H₆", "CH₂=CHCH₃"), 0, "Organik", "Orta"),
        Soru("Kaç tane noble gaz var?", listOf("5", "6", "7", "8", "9"), 1, "Genel", "Kolay"),
        Soru("Katalizör ne yapar?", listOf("Hızı artırır", "Dengeler", "Enerji verir", "Soğutur", "Isıtır"), 0, "Fizikokimya", "Kolay"),
        Soru("Sülfürik asit formülü?", listOf("HCl", "HNO₃", "H₂SO₄", "H₃PO₄", "H₂CO₃"), 2, "Anorganik", "Kolay"),
        Soru("Benzin içindeki hidrokarbon?", listOf("Metan", "Etan", "Okten", "Asetilen", "Benzen"), 2, "Organik", "Orta"),
        Soru("Amino asit sayısı?", listOf("10", "20", "30", "40", "50"), 1, "Biyokimya", "Orta"),
        Soru("Standart sıcaklık?", listOf("0°C", "25°C", "100°C", "37°C", "20°C"), 0, "Fizikokimya", "Kolay"),
        Soru("En yaygın metal?", listOf("Demir", "Bakır", "Alüminyum", "Altın", "Gümüş"), 0, "Genel", "Orta"),
        Soru("Kovalent bağ güçlü mü?", listOf("Çok güçlü", "Orta", "Zayıf", "Çok zayıf", "Yok"), 1, "Anorganik", "Orta"),
        Soru("pH=-log hangisinin konsantrasyonu?", listOf("OH⁻", "H⁺", "e⁻", "Na⁺", "Cl⁻"), 1, "Analitik", "Orta"),
        Soru("En az yoğun gaz?", listOf("O₂", "CO₂", "H₂", "N₂", "Ar"), 2, "Fizikokimya", "Orta"),
        Soru("Kimyasal denge sabiti Kp?", listOf("Basınç", "Derişim", "Sıcaklık", "Hız", "Enerji"), 0, "Fizikokimya", "Orta"),
        Soru("Fotosentez denklemi?", listOf("6CO₂+6H₂O→C₆H₁₂O₆+6O₂", "C₆H₁₂O₆→6CO₂+6H₂O", "H₂O→H₂+O₂", "N₂+3H₂→2NH₃", "2H₂+O₂→2H₂O"), 0, "Biyokimya", "Orta"),
        Soru("Kaç tane alkaloid biliyorsunuz?", listOf("5", "10", "20", "50", "100'den fazla"), 4, "Biyokimya", "Zor"),
        Soru("Asit-baz tepkimesi sonucu?", listOf("Tuz+Su", "Gaz", "Çökelti", "Isı", "Işık"), 0, "Anorganik", "Kolay"),
        Soru("Molarite birimi?", listOf("mol/L", "g/mL", "g/L", "mol/kg", "mL/mol"), 0, "Analitik", "Kolay"),
        Soru("Elektriksel iletkenlik en yüksek?", listOf("Cam", "Tahta", "Bakır", "Plastik", "Kauçuk"), 2, "Fizikokimya", "Kolay"),
        Soru("İzomeri nedir?", listOf("Aynı formül farklı yapı", "Farklı formül", "Aynı yapı", "İzotop", "İyon"), 0, "Organik", "Orta"),
        Soru("En sert element?", listOf("Demir", "Elmas", "Tungsten", "Krom", "Platin"), 1, "Genel", "Orta"),
        Soru("Gaz halindeki suya ne denir?", listOf("Buhar", "Sıvı su", "Buz", "Bulut", "Yağmur"), 0, "Fizikokimya", "Kolay"),
        Soru("Organik asit örneği?", listOf("HCl", "H₂SO₄", "CH₃COOH", "HNO₃", "HF"), 2, "Organik", "Kolay"),
        Soru("Kaç tane amino asit protein yapar?", listOf("Hepsi", "20 tanesi", "Sadece 10'u", "50 tanesi", "Hiçbiri"), 1, "Biyokimya", "Zor")
    )

    private var sorular = tumSorular.toList()
    private var currentQ = 0
    private var score = 0
    private var dogruAdet = 0
    private var yanlisAdet = 0
    private var sure = 20
    private var sureAktif = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var progressBar: ProgressBar
    private lateinit var timerText: TextView
    private lateinit var questionText: TextView
    private lateinit var resultText: TextView
    private lateinit var optionContainer: LinearLayout
    private lateinit var nextBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_quiz, container, false)
        questionText = v.findViewById(R.id.quiz_question)
        resultText = v.findViewById(R.id.quiz_result)
        nextBtn = v.findViewById(R.id.quiz_next)
        val restartBtn = v.findViewById<Button>(R.id.quiz_restart)

        optionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        (resultText.parent as ViewGroup).addView(optionContainer, (resultText.parent as ViewGroup).indexOfChild(resultText))

        progressBar = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16)
            max = 10
            progress = 0
            progressTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_lime)
        }
        val qParent = questionText.parent as ViewGroup
        qParent.addView(progressBar, qParent.indexOfChild(questionText))

        timerText = TextView(requireContext()).apply {
            text = "⏱ 20s"
            setTextColor(0xFFFFA500.toInt())
            textSize = 16f
        }
        qParent.addView(timerText)

        showCategorySelection()

        nextBtn.setOnClickListener {
            currentQ++
            if (currentQ >= sorular.size) {
                showResults()
            } else {
                loadQuestion()
            }
        }

        restartBtn.setOnClickListener {
            currentQ = 0; score = 0; dogruAdet = 0; yanlisAdet = 0
            showCategorySelection()
        }

        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Kimya Quiz")
                .setMessage(buildString {
                    appendLine("Kimya bilginizi test edin!")
                    appendLine()
                    appendLine("• Kategori seçin: Genel, Organik, Anorganik, Fizikokimya, Analitik, Biyokimya veya Hepsi")
                    appendLine("• Her soru için 20 saniye süreniz var")
                    appendLine("• 5 seçenek arasından doğru olanı seçin")
                    appendLine("• Doğru=yeşil, Yanlış=kırmızı gösterilir")
                    appendLine("• Süre dolduğunda otomatik geçilir")
                    appendLine()
                    appendLine("Toplam ${tumSorular.size} soru mevcut.")
                })
                .setPositiveButton("Anladım", null)
                .show()
        }

        return v
    }

    private fun showCategorySelection() {
        questionText.text = "Kategori Seçin"
        timerText.text = ""
        resultText.text = ""
        nextBtn.isEnabled = false
        progressBar.progress = 0
        optionContainer.removeAllViews()

        val kategoriler = listOf("Hepsi" to tumSorular.size, "Genel" to 25, "Organik" to 15, "Anorganik" to 15, "Fizikokimya" to 12, "Analitik" to 8, "Biyokimya" to 10)
        for ((kat, count) in kategoriler) {
            val btn = Button(requireContext()).apply {
                text = "$kat ($count soru)"
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(16, 12, 16, 12)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_cyan)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 4, 0, 4) }
                setOnClickListener { startQuiz(kat) }
            }
            optionContainer.addView(btn)
        }
    }

    private fun startQuiz(kategori: String) {
        sorular = if (kategori == "Hepsi") tumSorular.shuffled().take(20)
        else tumSorular.filter { it.kategori == kategori }.shuffled().take(15)
        currentQ = 0; score = 0; dogruAdet = 0; yanlisAdet = 0
        progressBar.max = sorular.size
        loadQuestion()
    }

    private fun loadQuestion() {
        sureAktif = false
        if (currentQ >= sorular.size) { showResults(); return }

        val s = sorular[currentQ]
        questionText.text = "${currentQ + 1}/${sorular.size} • [${s.kategori}] ${s.soru}"
        resultText.text = ""
        nextBtn.isEnabled = false
        progressBar.progress = currentQ
        optionContainer.removeAllViews()

        val colors = listOf(0xFF1A73E8.toInt(), 0xFFEA4335.toInt(), 0xFF34A853.toInt(), 0xFFFBBC05.toInt(), 0xFF9C27B0.toInt())
        for ((i, secenek) in s.secenekler.withIndex()) {
            val btn = Button(requireContext()).apply {
                text = "${('A' + i)}) $secenek"
                textSize = 14f
                setTextColor(Color.WHITE)
                setPadding(16, 14, 16, 14)
                setBackgroundColor(colors[i % colors.size])
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 6, 0, 6) }
                setOnClickListener { checkAnswer(i) }
            }
            optionContainer.addView(btn)
        }
        startTimer()
    }

    private fun checkAnswer(selected: Int) {
        sureAktif = false
        val s = sorular[currentQ]
        val dogruMu = selected == s.dogru
        if (dogruMu) { score++; dogruAdet++ } else { yanlisAdet++ }

        for (i in 0 until optionContainer.childCount) {
            val btn = optionContainer.getChildAt(i) as Button
            btn.isEnabled = false
            when {
                i == s.dogru -> btn.setBackgroundColor(0xFF00C853.toInt())
                i == selected && !dogruMu -> btn.setBackgroundColor(0xFFFF1744.toInt())
            }
        }

        val feedback = if (dogruMu) "✓ Doğru!" else "✗ Yanlış! Cevap: ${s.secenekler[s.dogru]}"
        resultText.text = feedback
        resultText.setTextColor(if (dogruMu) 0xFF00FF00.toInt() else 0xFFFF4444.toInt())

        if (!dogruMu) {
            try {
                val vb = requireContext().getSystemService(Vibrator::class.java)
                vb?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Exception) {}
        }

        nextBtn.isEnabled = true
        progressBar.progress = currentQ + 1
    }

    private fun startTimer() {
        sure = 20; sureAktif = true
        timerText.text = "⏱ ${sure}s"
        timerText.setTextColor(if (sure <= 5) 0xFFFF4444.toInt() else 0xFFFFA500.toInt())
        handler.post(object : Runnable {
            override fun run() {
                if (!sureAktif) return
                sure--
                timerText.text = "⏱ ${sure}s"
                timerText.setTextColor(if (sure <= 5) 0xFFFF4444.toInt() else 0xFFFFA500.toInt())
                if (sure <= 0) {
                    sureAktif = false
                    yanlisAdet++
                    for (i in 0 until optionContainer.childCount) {
                        val btn = optionContainer.getChildAt(i) as Button
                        btn.isEnabled = false
                        if (i == sorular[currentQ].dogru) btn.setBackgroundColor(0xFF00C853.toInt())
                    }
                    resultText.text = "⏱ Süre doldu! Cevap: ${sorular[currentQ].secenekler[sorular[currentQ].dogru]}"
                    resultText.setTextColor(0xFFFFA500.toInt())
                    nextBtn.isEnabled = true
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun showResults() {
        sureAktif = false
        questionText.text = "Quiz Tamamlandı!"
        timerText.text = ""
        optionContainer.removeAllViews()
        nextBtn.isEnabled = false

        val yuzde = if (sorular.isNotEmpty()) (score * 100 / sorular.size) else 0
        val notHarf = when {
            yuzde >= 90 -> "AA"
            yuzde >= 80 -> "BA"
            yuzde >= 70 -> "BB"
            yuzde >= 60 -> "CB"
            yuzde >= 50 -> "CC"
            else -> "FF"
        }
        val emoji = when {
            yuzde >= 90 -> "🏆"
            yuzde >= 70 -> "👍"
            yuzde >= 50 -> "📖"
            else -> "💪"
        }

        resultText.text = buildString {
            appendLine("$emoji SONUÇ")
            appendLine()
            appendLine("Skor: $score/${sorular.size} ($yuzde%)")
            appendLine("Not: $notHarf")
            appendLine()
            appendLine("✓ Doğru: $dogruAdet")
            appendLine("✗ Yanlış: $yanlisAdet")
            appendLine()
            appendLine("Kategori: ${sorular.first().kategori}")
        }
        resultText.setTextColor(0xFF00F0FF.toInt())
        resultText.textSize = 16f

        vm.addHistory("Quiz", "Skor: $score/${sorular.size} ($yuzde%) - $notHarf")
    }

    override fun onDestroyView() {
        sureAktif = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}
