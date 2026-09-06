package com.kimya.uygulama

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.kimya.uygulama.features.*
import com.kimya.uygulama.fragments.*
import com.kimya.uygulama.utils.HelpDialog
import com.kimya.uygulama.utils.AnimUtils

data class ToolItem(
    val title: String,
    val desc: String,
    val fragment: Fragment,
    val colorRes: Int,
    val help: String = ""
)

class DashboardFragment : Fragment() {

    /** Araca özel sembol (emoji): her araç neyse onu gösterir */
    private fun emojiFor(title: String): String = when (title) {
        "Element" -> "⚛️"
        "Bileşik" -> "🧪"
        "Periyodik" -> "🔲"
        "Trend" -> "📈"
        "Lewis Yapısı" -> "🔵"
        "Molekül Geometrisi" -> "🔺"
        "Atom Kütlesi" -> "⚖️"
        "Gaz Yasaları" -> "🎈"
        "Çözelti" -> "💧"
        "Molarite" -> "🧂"
        "Seyreltme" -> "💦"
        "Stokiyometri" -> "🧮"
        "Birim Dönüşüm" -> "🔄"
        "Mol Hesap" -> "🔢"
        "Termodinamik" -> "🌡️"
        "Kinematik" -> "🏎️"
        "Kimyasal Denge" -> "↔️"
        "Elektrokimya" -> "🔌"
        "Çözeltiler" -> "💠"
        "Reaksiyon" -> "💥"
        "Asit/Baz Hesaplayıcı" -> "🍋"
        "Redox" -> "🔁"
        "Organik" -> "🌿"
        "Org. Reaksiyon" -> "🔗"
        "İzomerlik" -> "👯"
        "Polimerler" -> "⛓️"
        "Petrol" -> "🛢️"
        "Biyomolekül" -> "🧬"
        "Lab. Güvenlik" -> "⚠️"
        "Notlar" -> "📝"
        "Kronometre" -> "⏱️"
        "Quiz" -> "❓"
        "Geçmiş" -> "🕘"
        "Kimyasal Bağ" -> "🧲"
        "Molekül Çizici" -> "✏️"
        "Titrasyon" -> "💉"
        "Reaksiyon Hızı" -> "⏩"
        "Faz Diyagramı" -> "📊"
        "Alev Testi" -> "🔥"
        "AAS Simülatörü" -> "🔬"
        "Pil Simülatörü" -> "🔋"
        "Kalibrasyon Eğrisi" -> "📏"
        "Çözünürlük" -> "🍬"
        "FTIR Simülatörü" -> "🌈"
        "Optik Lab" -> "🔦"
        else -> "🧪"
    }



    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** Aktif temadan renk çözer (açık/koyu tema uyumu) */
    private fun attrColor(attr: Int): Int {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val kesfet = listOf(
            ToolItem("Element", "Sembol, atom no, kütle", ElementFragment(), R.color.cat_kesfet,
                "Periyodik tablodaki her elementin özelliklerini inceleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Aramak istediğiniz elementin adını veya sembolünü girin.<br/>2. Sembol, atom numarası, atom kütlesi ve elektronegatiflik gibi bilgileri görün.<br/>3. Soldaki liste veya arama ile hızlıca gezinin."),
            ToolItem("Bileşik", "Ad, formül, mol kütlesi", BilesikFragment(), R.color.cat_kesfet,
                "Kimyasal bileşiklerin formülünü ve mol kütlesini hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bileşik listesinden bir madde seçin.<br/>2. Formülü ve mol kütlesi otomatik görüntülenir.<br/>3. Özel bir bileşik için formülü elle girebilirsiniz."),
            ToolItem("Periyodik", "İnteraktif tablo", PeriyodikFragment(), R.color.cat_kesfet,
                "Periyodik tabloda elementleri keşfedin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bir elemente dokunun, bilgileri görün.<br/>2. Renk kodları ile grup ve periyodu anlayın.<br/>3. Metaller, ametaller ve yarı metaller renklerle ayrılır."),
            ToolItem("Trend", "Periyodik özellikler", TrendFragment(), R.color.cat_kesfet,
                "6 periyodik özelliği 4 modla keşfedin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Özellik seç: iyonlaşma enerjisi, elektronegatiflik, yarıçap, kütle, metalik karakter, değerlik.<br/>2. HARİTA: tablo, özelliğe göre renklenir; kutuya dokununca detay gelir.<br/>3. GRAFİK: 118 elementin eğrisinde parmağınızı gezdirin.<br/>4. KARŞILAŞTIR: 2-4 element seçip yan yana kıyaslayın.<br/>5. DÜELLO: hangi element daha yüksek, bilmeceli oyun!"),
            ToolItem("Lewis Yapısı", "Elektron noktalı yapı", LewisFragment(), R.color.cat_kesfet,
                "Moleküllerin Lewis elektron-nokta yapısını çizin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Formülü girin veya seçin.<br/>2. Değerlik elektronları toplamını görün.<br/>3. Bağ ve ortaklanmamış elektron çiftleri otomatik yerleşir."),
            ToolItem("Molekül Geometrisi", "VSEPR, açılar, polarite", MolekulGeometriFragment(), R.color.cat_kesfet,
                "VSEPR teorisine göre molekül geometrisini belirleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Merkez atom ve bağ sayısını girin.<br/>2. Bağ açısı ve geometri adı otomatik hesaplanır.<br/>3. Polarite değerlendirmesini görün."),
        )
        val hesapla = listOf(
            ToolItem("Atom Kütlesi", "Element ve bilesik kutle hesaplama", MolKutlesiFragment(), R.color.cat_hesapla,
                "Element ve bileşiklerin atom/mol kütlesini hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bileşik formülünü girin (ör. H2SO4).<br/>2. Her atomun kütlesi toplanır.<br/>3. Sonuç g/mol olarak görüntülenir."),
            ToolItem("Gaz Yasaları", "PV=nRT hesapları", GazFragment(), R.color.cat_hesapla,
                "İdeal gaz yasasını kullanarak P, V, n veya T değerlerini hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bilinen üç değeri girin (basınç, hacim, mol, sıcaklık).<br/>2. Hesaplanacak değeri seçin.<br/>3. R sabiti ile sonuç otomatik bulunur."),
            ToolItem("Çözelti", "Çözelti hazırlama", CozeltiFragment(), R.color.cat_hesapla,
                "Bir çözeltinin hazırlanması için gereken madde miktarını hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Çözünen ve çözücüyü seçin.<br/>2. Hacim ve derişimi girin.<br/>3. Gereken kütle/gram miktarı hesaplanır."),
            ToolItem("Molarite", "Molarite hesaplama", MolariteFragment(), R.color.cat_hesapla,
                "Molarite (M) hesaplayın: M = n / V.<br/><br/><b>Nasıl kullanılır?</b><br/>1. M, n veya V alanlarından ikisini doldurun.<br/>2. Hesapla butonuna basın.<br/>3. Eksik değer otomatik bulunur."),
            ToolItem("Seyreltme", "Seyreltme oranları", SeyreltmeFragment(), R.color.cat_hesapla,
                "M1.V1 = M2.V2 formülü ile seyreltme hesabı yapın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Başlangıç derişim ve hacmi girin.<br/>2. Hedef derişimi seçin.<br/>3. Eklenmesi gereken su hacmi hesaplanır."),
            ToolItem("Stokiyometri", "Mol oran hesapları", StokiyometriFragment(), R.color.cat_hesapla,
                "Kimyasal denklemlerde mol oranlarını hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Dengeli denklemi girin.<br/>2. Bilinen mol/kütle miktarını yazın.<br/>3. Diğer maddelerin mol ve kütleleri bulunur."),
            ToolItem("Birim Dönüşüm", "Birim çevirme", BirimFragment(), R.color.cat_hesapla,
                "Kütle, hacim, enerji ve basınç birimlerini dönüştürün.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Dönüştürmek istediğiniz değeri girin.<br/>2. Kaynak ve hedef birimi seçin.<br/>3. Sonuç anında dönüştürülür."),
            ToolItem("Mol Hesap", "Mol, kutle, hacim hesaplama", DonusumFragment(), R.color.neon_lime,
                "Mol, kütle ve hacim arasında dönüşüm yapın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Madde miktarını ve türünü girin.<br/>2. n = m/M ve n = V/22.4 formüllerini kullanın.<br/>3. Sonuç mol olarak verilir."),
            ToolItem("Termodinamik", "ΔG = ΔH − TΔS hesaplama", TermodinamikFragment(), R.color.cat_hesapla,
                "Gibbs serbest enerjisi hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. ΔH, ΔS ve sıcaklık değerlerini girin.<br/>2. ΔG = ΔH − TΔS formülü uygulanır.<br/>3. Spontanelik hakkında bilgi alın (ΔG<0 ise spontan)."),
            ToolItem("Kinematik", "Hız yasaları, yarım ömür", KinematikFragment(), R.color.cat_hesapla,
                "Reaksiyon hız yasaları ve yarı ömür hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Reaksiyon derecesini seçin.<br/>2. Hız sabiti (k) ve derişimleri girin.<br/>3. Hız veya yarı ömür hesaplanır."),
            ToolItem("Kimyasal Denge", "Kp, Kc, Le Chatelier", KimyasalDengeFragment(), R.color.cat_hesapla,
                "Denge sabiti Kc/Kp hesaplayın ve Le Chatelier ilkesini inceleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Dengeli denklemi girin.<br/>2. Denge derişimlerini yazın.<br/>3. Kc veya Kp hesaplanır; basınç/sıcaklık etkisi gösterilir."),
            ToolItem("Elektrokimya", "Pil, Nernst, Faraday", ElektrokimyaFragment(), R.color.cat_hesapla,
                "Elektrokimyasal hücre hesapları yapın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Yükseltgenme ve indirgenme yarı tepkimelerini girin.<br/>2. E° hücre potansiyelini hesaplayın.<br/>3. Nernst denklemi ile derişime bağlı potansiyel bulun."),
            ToolItem("Çözeltiler", "Raoult, donma/kaynama", CozeltiRaoultFragment(), R.color.cat_hesapla,
                "Raoult yasası ve koligatif özellikleri hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Çözücü ve çözünen miktarlarını girin.<br/>2. Buhar basıncı düşüşü hesaplanır.<br/>3. Donma noktası düşmesi / kaynama noktası yükselmesi görülür."),
        )
        val tepkime = listOf(
            ToolItem("Reaksiyon", "Kimyasal dengeleme", ReaksiyonFragment(), R.color.cat_tepkime,
                "Kimyasal denklemleri dengeleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Tepkime denklemini girin (ör. H2 + O2 → H2O).<br/>2. Dengele butonuna basın.<br/>3. Katsayılar otomatik hesaplanır ve doğrulanır."),
            ToolItem("Asit/Baz Hesaplayıcı", "pH, pOH hesapları", AsitBazFragment(), R.color.cat_tepkime,
                "pH, pOH, [H+] ve [OH-] hesaplayın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Hesaplamak istediğiniz değeri girin.<br/>2. Türü seçin (asit veya baz).<br/>3. İlgili değerler otomatik hesaplanır."),
            ToolItem("Redox", "Yükseltgenme/indirgenme", RedoxFragment(), R.color.cat_tepkime,
                "Redoks tepkimelerinde yükseltgenme sayılarını bulun.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bileşiği girin.<br/>2. Her elementin yükseltgenme sayısı gösterilir.<br/>3. Yükseltgenen ve indirgenen türler belirlenir."),
            ToolItem("Organik", "Organik kimya araçları", OrganicFragment(), R.color.cat_tepkime,
                "Organik bileşikleri adlandırın ve sınıflandırın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bileşik listesinden seçin veya formül girin.<br/>2. Fonksiyonel grup ve sınıfı görün.<br/>3. IUPAC adı önerilir."),
            ToolItem("Org. Reaksiyon", "Yer değiştirme, katılma", ReactionsFragment(), R.color.cat_tepkime,
                "Organik tepkime türlerini öğrenin: katılma, yer değiştirme, polimerizasyon.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bir tepkime türü seçin.<br/>2. Örnek tepkime gösterilir.<br/>3. Mekanizma adım adım açıklanır."),
            ToolItem("İzomerlik", "Yapı, geometrik, optik", IsomerismFragment(), R.color.cat_tepkime,
                "Yapısal, geometrik ve optik izomerleri inceleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bir molekül seçin.<br/>2. İzomer türlerini görün.<br/>3. Farklı izomerlerin yapılarını karşılaştırın."),
            ToolItem("Polimerler", "Katılma, yoğunlaşma", PolymersFragment(), R.color.cat_tepkime,
                "Polimerizasyon türlerini öğrenin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Polimer türünü seçin (katılma/yoğunlaşma).<br/>2. Monomer yapısını görün.<br/>3. Polimer zinciri adım adım oluşturulur."),
            ToolItem("Petrol", "Hidrokarbonlar", PetroleumFragment(), R.color.cat_tepkime,
                "Hidrokarbon türlerini ve petrol ürünlerini inceleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Hidrokarbon sınıfını seçin.<br/>2. Yapı ve örnekleri görün.<br/>3. Rafinasyon ürünleri tablosunu inceleyin."),
            ToolItem("Biyomolekül", "Karbonhidrat, protein", BiomoleculesFragment(), R.color.cat_tepkime,
                "Karbonhidrat, protein, yağ ve nükleik asitleri keşfedin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Biyomolekül türünü seçin.<br/>2. Yapı taşlarını ve örneklerini görün.<br/>3. Görevlerini öğrenin."),
        )
        val araclar = listOf(
            ToolItem("Lab. Güvenlik", "Guvenlik rehberi: sembol, ekipman, acil durum", LabSafetyFragment(), R.color.cat_araclar,
                "Laboratuvar güvenlik rehberi: semboller, ekipman, acil durum.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Sekme seç: Semboller, Ekipman, Acil Durum, Kurallar.<br/>2. Sembol kartına dokun, tanım + önlem + acil durumu oku.<br/>3. Deneye başlamadan ekipman ve kuralları gözden geçir."),
            ToolItem("Notlar", "Hızlı not alma", NotFragment(), R.color.cat_araclar,
                "Kimya ders notlarınızı hızlıca alın ve kaydedin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Yeni not butonuna basın.<br/>2. Başlık ve içerik girin.<br/>3. Kaydet butonu ile notunuz saklanır."),
            ToolItem("Kronometre", "Reaksiyon süreölçer", TimerFragment(), R.color.cat_araclar,
                "Reaksiyon sürelerini ölçün.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Başlat butonuna basın.<br/>2. Süre geri sayar.<br/>3. Durdur / Sıfırla ile kontrol edin."),
            ToolItem("Quiz", "Kendini test et", QuizFragment(), R.color.cat_araclar,
                "Kimya bilginizi test edin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bir kategori seçin.<br/>2. Soruları cevaplayın.<br/>3. Sonuç ve doğru cevapları görün."),
            ToolItem("Geçmiş", "İşlem geçmişi", HistoryFragment(), R.color.cat_araclar,
                "Önceden yaptığınız hesaplamaları görüntüleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Geçmiş listenizi inceleyin.<br/>2. Kayıtlı hesaplamayı tekrar görüntüleyin.<br/>3. Sil butonu ile kayıtları temizleyin."),
            ToolItem("Molekül Çizici", "Serbest molekül çizimi", MoleculeDrawerFragment(), R.color.cat_araclar,
                "Serbest molekül çizim alanı.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Parmağınızla çizin.<br/>2. İki parmakla kaydırıp yakınlaştırın.<br/>3. Temizle ile sıfırlayın."),
        )

        val simulasyon = listOf(
            ToolItem("Kimyasal Bağ", "Bağ oluşumu simülasyonu", ChemicalBondFragment(), R.color.cat_kesfet,
                "Kovalent, iyonik ve metalik bag olusumunu animasyonla izleyin.<br/><br/><b>Nasil kullanilir?</b><br/>1. Bag turunu secin (Kovalent/Iyonik/Metalik).<br/>2. Animasyonu izleyin.<br/>3. Atomlara dokunarak bilgi alin."),
            ToolItem("Titrasyon", "pH titrasyon simulasyonu", TitrasyonFragment(), R.color.cat_kesfet,
                "Asit-baz titrasyonunu sanal ortamda gerçekleştirin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Asit ve baz türlerini seçin.<br/>2. Burette hacmini ayarlayın.<br/>3. Titre edin; pH eğrisi ve dönüm noktası otomatik çizilir."),
            ToolItem("Reaksiyon Hızı", "Hiz simulasyonu", ReactionRateFragment(), R.color.cat_hesapla,
                "Reaksiyon hızını derişim ve sıcaklık ile simüle edin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Reaksiyonu seçin.<br/>2. Sıcaklık ve derişimi ayarlayın.<br/>3. Hız grafiğini anlık izleyin."),
            ToolItem("Faz Diyagramı", "Maddenin halleri", PhaseDiagramFragment(), R.color.cat_hesapla,
                "Sıcaklık-basınç faz diyagramını inceleyin.<br/><br/><b>Nasıl kullanır?</b><br/>1. Madde seçin.<br/>2. Sıcaklık/basıncı ayarlayın.<br/>3. Maddenin hangi fazda olduğunu görün."),
            ToolItem("Alev Testi", "Flame test simulasyonu", FlameTestFragment(), R.color.cat_tepkime,
                "Farklı elementlerin alev renklerini simüle edin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bir element seçin (Li, Na, K, Cu vb.).<br/>2. Tel alev üzerine getirilir.<br/>3. Karakteristik alev rengini ve nedenini görün."),
            ToolItem("AAS Simülatörü", "Atomik absorpsiyon spektroskopisi", AASSimulatorFragment(), R.color.cat_tepkime,
                "Atomik Absorpsiyon Spektrometresi ile metal analizi yapın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Üstteki rehberi takip edin: Gaz aç → Alev yak → Lamba ayarla → Blank → Ölç.<br/>2. Her adımın yanındaki <b>?</b> yardım butonu adımı açıklar.<br/>3. SELECT SAMPLE ile element ve konsantrasyon seçip MEASURE ile ölçün."),
            ToolItem("Pil Simülatörü", "Galvanik hücre, elektron akışı", ElectrochemistryFragment(), R.color.cat_hesapla,
                "Galvanik hücrede elektron akışını simüle edin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Anot ve katot metalleri seçin.<br/>2. Hücre potansiyeli hesaplanır.<br/>3. Elektron akış yönünü görsel olarak izleyin."),
            ToolItem("Kalibrasyon Eğrisi", "Spektroskopi analizi", EnstrumantalFragment(), R.color.cat_araclar,
                "Standart çözeltilerle kalibrasyon eğrisi oluşturun.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Standart derişimleri girin.<br/>2. Absorbans değerlerini ekleyin.<br/>3. Doğrusal eğri çizilir, bilinmeyen derişim bulunur."),
            ToolItem("Çözünürlük", "Çözünürlük ve doygunluk", SolubilityFragment(), R.color.cat_hesapla,
                "Madde çözünürlüğünü ve doygunluğu inceleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Maddeyi ve sıcaklığı seçin.<br/>2. Eklenen madde miktarını ayarlayın.<br/>3. Doygun/çözünmüş durumu görün."),
            ToolItem("FTIR Simülatörü", "Kızılötesi spektrum analizi", FTIRSimulatorFragment(), R.color.cat_tepkime,
                "Organik bileşiklerin FTIR spektrumunu inceleyin.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Bir bileşik seçin.<br/>2. Spektrumdaki pikleri inceleyin.<br/>3. Her pik için fonksiyonel grup eşleşmesi gösterilir."),
            ToolItem("Optik Lab", "Işın, ayna, prizma sandbox", OpticsFragment(), R.color.cat_araclar,
                "Serbest optik laboratuvarı: ışıkla deney yapın.<br/><br/><b>Nasıl kullanılır?</b><br/>1. Alttan IŞIK, AYNA, PRİZMA, FİLTRE, MERCEK, ENGEL, YARI AYNA, PLAKA veya NOKTA ekleyin.<br/>2. Nesneye dokunup sürükleyin; üstteki halkayla döndürün.<br/>3. Yansıma, kırılma, odaklama ve renklere ayrılmayı anlık izleyin."),
        )
        val tumAraclar = kesfet + hesapla + tepkime + araclar + simulasyon
        val cats = listOf(
            "Keşfet" to kesfet, "Hesapla" to hesapla, "Tepkime" to tepkime,
            "Araçlar" to araclar, "Simülasyon" to simulasyon
        )

        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        v.findViewById<TextView>(R.id.dash_hello).apply {
            visibility = View.VISIBLE
            text = when (hour) {
                in 6..11 -> "Günaydın"
                in 12..17 -> "İyi günler"
                in 18..22 -> "İyi akşamlar"
                else -> "İyi geceler"
            }
        }
        v.findViewById<TextView>(R.id.dash_date).apply {
            visibility = View.VISIBLE
            text = java.text.SimpleDateFormat("d MMMM EEEE", java.util.Locale("tr", "TR")).format(java.util.Date()) +
                " • ${tumAraclar.size} araç"
        }
        // günün bilgisi (sade satır)
        (v.findViewById<TextView>(R.id.dash_date).parent as? LinearLayout)?.let { parent ->
            val info = TextView(requireContext()).apply {
                text = "💡 " + dailyFact()
                setTextColor(attrColor(android.R.attr.textColorSecondary))
                textSize = 12f
                setPadding(0, dp(8), 0, dp(4))
            }
            parent.addView(info, parent.indexOfChild(v.findViewById(R.id.dash_date)) + 1)
        }

        buildRails(v, cats)
        loadRecents(v, tumAraclar.associateBy { it.title })

        return v
    }

    private val catDots = mapOf(
        "Keşfet" to R.color.cat_kesfet,
        "Hesapla" to R.color.cat_hesapla,
        "Tepkime" to R.color.cat_tepkime,
        "Araçlar" to R.color.cat_araclar,
        "Simülasyon" to R.color.cat_tepkime
    )

    private fun surfaceCard(radius: Int = 16): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radius).toFloat()
            setColor(attrColor(com.google.android.material.R.attr.colorSurface))
        }
    }

    private fun pressScale(v: View) {
        v.setOnTouchListener { vv, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> vv.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    vv.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
            }
            false
        }
    }

    /** İki rengi karıştırır (oran: ikinci rengin ağırlığı 0..1) */
    private fun blend(a: Int, b: Int, ratio: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * ratio).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * ratio).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * ratio).toInt()
    )

    private val dailyFacts = listOf(
        "Su 100°C'de kaynar ama rakım yükseldikçe kaynama noktası düşer.",
        "Altın havada kararmaz; asal metaldir.",
        "Elmas ve grafit aynı elementtir: karbon.",
        "Bir çay kaşığı nötron yıldızı milyarlarca ton gelir.",
        "Cıva oda sıcaklığında sıvı olan tek metaldir.",
        "Lityum en hafif metaldir, sudan bile hafiftir.",
        "pH 1 birim değiştiğinde asitlik 10 kat değişir.",
        "Vücudundaki atomların çoğu yıldızlarda üretildi.",
        "Helyum asla katılaşmaz; mutlak sıfırda bile sıvı kalır.",
        "Bir damla suda milyarlarca molekül vardır.",
        "Titanyum çelik kadar güçlü ama %45 daha hafiftir.",
        "Mendeleyev tabloyu elementleri tartarak değil dizerek kurdu.",
        "Ozon tabakası Güneş'in zararlı morötesini süzer.",
        "Demir çekirdekteki basınçla erimez; katı iç çekirdek oluşturur."
    )

    private fun dailyFact(): String {
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        return dailyFacts[day % dailyFacts.size]
    }

    private fun buildRails(v: View, cats: List<Pair<String, List<ToolItem>>>) {
        val slot = v.findViewById<LinearLayout>(R.id.cats_slot)
        slot.removeAllViews()
        var animOf = 0
        for ((cat, items) in cats) {
            if (items.isEmpty()) continue
            val cc = ContextCompat.getColor(requireContext(), catDots[cat] ?: R.color.cat_kesfet)
            val head = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(13).toFloat()
                    setColor(blend(attrColor(com.google.android.material.R.attr.colorSurface), cc, 0.16f))
                    setStroke(dp(1), blend(attrColor(com.google.android.material.R.attr.colorSurface), cc, 0.40f))
                }
                setPadding(dp(12), dp(7), dp(12), dp(7))
            }
            head.addView(TextView(requireContext()).apply {
                text = "●"
                setTextColor(cc)
                textSize = 11f
                setPadding(0, 0, dp(6), 0)
            })
            head.addView(TextView(requireContext()).apply {
                text = cat
                setTextColor(attrColor(com.google.android.material.R.attr.colorOnSurface))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            })
            head.addView(TextView(requireContext()).apply {
                text = "${items.size}"
                setTextColor(attrColor(android.R.attr.textColorSecondary))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(6), 0, 0, 0)
            })
            slot.addView(head, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(14), 0, dp(8)) })
            val hsv = android.widget.HorizontalScrollView(requireContext()).apply {
                isHorizontalScrollBarEnabled = false
            }
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            for ((i, item) in items.withIndex()) {
                row.addView(railCard(item, animOf++), LinearLayout.LayoutParams(dp(118), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, if (i == items.size - 1) 0 else dp(10), 0)
                })
            }
            hsv.addView(row)
            slot.addView(hsv)
        }
    }

    private fun railCard(item: ToolItem, animIndex: Int): LinearLayout {
        val cc = ContextCompat.getColor(requireContext(), item.colorRes)
        val surface = attrColor(com.google.android.material.R.attr.colorSurface)
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(blend(surface, cc, 0.14f), blend(surface, cc, 0.05f))
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20).toFloat()
            }
            elevation = dp(3).toFloat()
            isClickable = true
            isFocusable = true
        }
        val inner = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        inner.addView(TextView(requireContext()).apply {
            text = emojiFor(item.title)
            textSize = 30f
            gravity = Gravity.CENTER
        })
        inner.addView(TextView(requireContext()).apply {
            text = item.title
            setTextColor(attrColor(com.google.android.material.R.attr.colorOnSurface))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            maxLines = 2
            minLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, 0)
        })
        inner.addView(TextView(requireContext()).apply {
            text = item.desc
            setTextColor(attrColor(android.R.attr.textColorSecondary))
            textSize = 10f
            gravity = Gravity.CENTER
            maxLines = 2
            minLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        card.addView(inner, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        pressScale(card)
        card.setOnClickListener { saveRecent(item.title); openFragment(item.fragment) }
        card.setOnLongClickListener {
            val body = if (item.help.isNotBlank()) item.help else item.desc
            HelpDialog.show(requireContext(), item.title, body)
            true
        }
        card.alpha = 0f
        card.translationY = dp(18).toFloat()
        card.animate().alpha(1f).translationY(0f)
            .setStartDelay(minOf(animIndex * 25, 450).toLong())
            .setDuration(280).start()
        return card
    }

    // Not: eski bento ızgara kaldırıldı; yerine raylar (buildRails) kullanılıyor

    private fun openFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_slide_up,
                R.anim.fragment_fade_out,
                R.anim.fragment_fade_in,
                R.anim.fragment_slide_down
            )
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun saveRecent(title: String) {
        val prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        val current = prefs.getString("recent_tools", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        val updated = (listOf(title) + current).distinct().take(6)
        prefs.edit().putString("recent_tools", updated.joinToString(",")).apply()
    }

    private fun loadRecents(root: View, toolByTitle: Map<String, ToolItem>) {
        val prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        val titles = prefs.getString("recent_tools", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        val titleView = root.findViewById<TextView>(R.id.recent_title)
        val scroll = root.findViewById<View>(R.id.recent_scroll)
        val row = root.findViewById<LinearLayout>(R.id.recent_row)
        row.removeAllViews()
        val shown = titles.mapNotNull { toolByTitle[it] }.take(6)
        if (shown.isEmpty()) {
            titleView.visibility = View.GONE
            scroll.visibility = View.GONE
            return
        }
        titleView.visibility = View.VISIBLE
        scroll.visibility = View.VISIBLE
        for (item in shown) {
            val chip = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(8), dp(14), dp(8))
                background = surfaceCard()
                elevation = dp(2).toFloat()
            }
            val iv = TextView(requireContext()).apply {
                text = emojiFor(item.title)
                textSize = 22f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
            }
            val tv = TextView(requireContext()).apply {
                text = item.title
                setTextColor(attrColor(com.google.android.material.R.attr.colorOnSurface))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(8), 0, 0, 0)
            }
            chip.addView(iv)
            chip.addView(tv)
            chip.setOnClickListener { openFragment(item.fragment) }
            chip.setOnLongClickListener {
                val body = if (item.help.isNotBlank()) item.help else item.desc
                HelpDialog.show(requireContext(), item.title, body)
                true
            }
            row.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, dp(8), 0) })
        }
    }
}
