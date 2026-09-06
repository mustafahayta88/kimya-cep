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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.viewmodel.KimyaViewModel

data class Soru(val soru: String, val secenekler: List<String>, val dogru: Int, val kategori: String, val zorluk: String)

val tumSorular = listOf(
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
        Soru("Kovalent bağda elektronlar?", listOf("Bir atom verir", "Ortak paylaşılır", "Serbest", "Yok", "Yutulur"), 0, "Anorganik", "Kolay"),
        Soru("Fotosentez ürünü?", listOf("CO₂", "H₂O", "Glikoz", "NaCl", "O₂"), 2, "Biyokimya", "Kolay"),
        Soru("Çekirdekteki parçacıklar?", listOf("Proton+Nöutron", "Proton+Elektron", "Nöutron+Elektron", "Sadece Proton", "Sadece Nöutron"), 0, "Genel", "Kolay"),
        Soru("Termoplastik olmayan?", listOf("PE", "PP", "PVC", "Bakalit", "PS"), 3, "Organik", "Zor"),
        Soru("Yanma tepkimesi türü?", listOf("Sentez", "Analiz", "Yükseltgenme", "Çökelme", "Deşarj"), 2, "Anorganik", "Orta"),
        Soru("Lewis asidi nedir?", listOf("Proton verici", "Elektron çifti alıcı", "OH⁻ verici", "H⁺ verici", "Elektron verici"), 1, "Anorganik", "Zor"),
        Soru("İç enerji değişimi?", listOf("ΔH", "ΔU", "ΔG", "ΔS", "ΔP"), 1, "Fizikokimya", "Zor"),
        Soru("Elektromotor kuvvet birimi?", listOf("Volt", "Amper", "Ohm", "Watt", "Joule"), 0, "Fizikokimya", "Orta"),
        Soru("Hangisi amfoterdir?", listOf("NaOH", "Al(OH)₃", "HCl", "NaCl", "KOH"), 1, "Anorganik", "Zor"),
        Soru("Hidroliz nedir?", listOf("Su ile ayrışma", "Su ile birleşme", "Isı ile ayrışma", "Elektrik ile ayrışma", "Basınç ile ayrışma"), 0, "Anorganik", "Orta"),
        Soru("En çok bulunan gaz?", listOf("O₂", "N₂", "CO₂", "Ar", "H₂"), 1, "Genel", "Kolay"),
        Soru("Kararlı izotop?", listOf("¹⁴C", "¹²C", "³H", "¹³¹I", "²³⁵U"), 1, "Genel", "Orta"),
        Soru("Periyodik tabloda en çok element?", listOf("Geçiş metal", "Soy gaz", "Halojen", "Alkali metal", "Toprak alkali"), 0, "Genel", "Orta"),
        Soru("Koagülasyon nedir?", listOf("Çökelme", "Süspansiyon", "Emülsiyon", "Jel", "Kolloit"), 0, "Analitik", "Zor"),
        Soru("Tampon çözelti nedir?", listOf("pH değişmeyen", "Asidik", "Bazik", "Nötr", "Doymuş"), 0, "Analitik", "Zor"),
        Soru("En çok elektronegatif?", listOf("Fr", "Cs", "F", "O", "Cl"), 2, "Genel", "Orta"),
        Soru("Redoks tepkimesinde indirgenen?", listOf("Oksijenlenir", "İndirgenir", "Değişmez", "Yükseltgenir", "Hidroliz olur"), 1, "Anorganik", "Orta"),
        Soru("Kaç tane soy gaz var?", listOf("5", "6", "7", "8", "9"), 1, "Genel", "Kolay"),
        Soru("Katalizör ne yapar?", listOf("Hızı artırır", "Dengeler", "Enerji verir", "Soğutur", "Isıtır"), 0, "Fizikokimya", "Kolay"),
        Soru("Sülfürik asit formülü?", listOf("HCl", "HNO₃", "H₂SO₄", "H₃PO₄", "H₂CO₃"), 2, "Anorganik", "Kolay"),
        Soru("Benzin içindeki hidrokarbon?", listOf("Metan", "Etan", "Okten", "Asetilen", "Benzen"), 2, "Organik", "Orta"),
        Soru("Amino asit sayısı?", listOf("10", "20", "30", "40", "50"), 1, "Biyokimya", "Orta"),
        Soru("Standart sıcaklık?", listOf("0°C", "25°C", "100°C", "37°C", "20°C"), 0, "Fizikokimya", "Kolay"),
        Soru("Yer kabuğunda en bol metal?", listOf("Alüminyum", "Demir", "Bakır", "Altın", "Gümüş"), 0, "Genel", "Orta"),
        Soru("Kovalent bağ güçlü mü?", listOf("Çok güçlü", "Orta", "Zayıf", "Çok zayıf", "Yok"), 0, "Anorganik", "Orta"),
        Soru("pH=-log hangisinin konsantrasyonu?", listOf("OH⁻", "H⁺", "e⁻", "Na⁺", "Cl⁻"), 1, "Analitik", "Orta"),
        Soru("En az yoğun gaz?", listOf("O₂", "CO₂", "H₂", "N₂", "Ar"), 2, "Fizikokimya", "Orta"),
        Soru("Kimyasal denge sabiti Kp?", listOf("Basınç", "Derişim", "Sıcaklık", "Hız", "Enerji"), 0, "Fizikokimya", "Orta"),
        Soru("Fotosentez denklemi?", listOf("6CO₂+6H₂O→C₆H₁₂O₆+6O₂", "C₆H₁₂O₆→6CO₂+6H₂O", "H₂O→H₂+O₂", "N₂+3H₂→2NH₃", "2H₂+O₂→2H₂O"), 0, "Biyokimya", "Orta"),
        Soru("Hangisi alkaloitlere örnektir?", listOf("Morfin", "Glukoz", "Etanol", "Asetik asit", "Üre"), 0, "Biyokimya", "Orta"),
        Soru("Asit-baz tepkimesi sonucu?", listOf("Tuz+Su", "Gaz", "Çökelti", "Isı", "Işık"), 0, "Anorganik", "Kolay"),
        Soru("Molarite birimi?", listOf("mol/L", "g/mL", "g/L", "mol/kg", "mL/mol"), 0, "Analitik", "Kolay"),
        Soru("Elektriksel iletkenlik en yüksek?", listOf("Cam", "Tahta", "Bakır", "Plastik", "Kauçuk"), 2, "Fizikokimya", "Kolay"),
        Soru("İzomeri nedir?", listOf("Aynı formül farklı yapı", "Farklı formül", "Aynı yapı", "İzotop", "İyon"), 0, "Organik", "Orta"),
        Soru("En sert element?", listOf("Demir", "Elmas", "Tungsten", "Krom", "Platin"), 1, "Genel", "Orta"),
        Soru("Gaz halindeki suya ne denir?", listOf("Buhar", "Sıvı su", "Buz", "Bulut", "Yağmur"), 0, "Fizikokimya", "Kolay"),
        Soru("Organik asit örneği?", listOf("HCl", "H₂SO₄", "CH₃COOH", "HNO₃", "HF"), 2, "Organik", "Kolay"),
        Soru("Kaç tane amino asit protein yapar?", listOf("Hepsi", "20 tanesi", "Sadece 10'u", "50 tanesi", "Hiçbiri"), 1, "Biyokimya", "Zor"),
        Soru("Aynı proton, farklı nötron sayısına sahip atomlara ne denir?", listOf("İzotop", "İzobar", "İzoton", "İzomer", "Allotrop"), 0, "Genel", "Orta"),
        Soru("Hangisi alaşımdır?", listOf("Bronz (bakır + kalay)", "Saf demir", "Elmas", "Kuvars", "Sofra tuzu"), 0, "Genel", "Kolay"),
        Soru("Hal değişimi sırasında sıcaklık nasıl davranır?", listOf("Sabit kalır", "Sürekli artar", "Sürekli azalır", "Önce artar sonra azalır", "Negatif olur"), 0, "Genel", "Kolay"),
        Soru("Avogadro sayısı kaçtır?", listOf("6,02x10^23", "3,14", "9,81", "1,6x10^-19", "2,99x10^8"), 0, "Genel", "Orta"),
        Soru("Alkanların genel formülü hangisidir?", listOf("CnH2n+2", "CnH2n", "CnH2n-2", "CnHn", "CnH3n"), 0, "Organik", "Kolay"),
        Soru("Benzen halkasında kaç karbon atomu bulunur?", listOf("6", "5", "8", "4", "12"), 0, "Organik", "Kolay"),
        Soru("Etanolün fonksiyonel grubu hangisidir?", listOf("-OH (alkol)", "-COOH (karboksil)", "-CHO (aldehit)", "-NH2 (amin)", "-NO2 (nitro)"), 0, "Organik", "Orta"),
        Soru("Asit + alkol tepkimesi hangi ürünleri verir?", listOf("Ester ve su", "Eter ve tuz", "Aldehit ve alkol", "Keton ve gaz", "Alkan ve su"), 0, "Organik", "Zor"),
        Soru("Kompleks bileşiklerde merkez iyon genellikle hangisidir?", listOf("Geçiş metal iyonu", "Alkali metal iyonu", "Toprak alkali iyonu", "Halojen iyonu", "Soygaz"), 0, "Anorganik", "Zor"),
        Soru("Nötralizasyon tepkimesinin ürünleri hangileridir?", listOf("Tuz ve su", "Asit ve baz", "Gaz ve metal", "Metal ve oksit", "Karbon ve tuz"), 0, "Anorganik", "Kolay"),
        Soru("Sert suya neden olan iyonlar hangileridir?", listOf("Ca2+ ve Mg2+", "Na+ ve K+", "Cl- ve Br-", "SO4 ve NO3", "Al3+ ve Fe2+"), 0, "Anorganik", "Orta"),
        Soru("Hem asit hem baz ile tepkime verebilen (amfoter) oksit hangisidir?", listOf("ZnO", "Na2O", "K2O", "CaO", "MgO"), 0, "Anorganik", "Zor"),
        Soru("Entropi neyin ölçüsüdür?", listOf("Düzensizliğin", "Sıcaklığın", "Basıncın", "Kütlenin", "Hacmin"), 0, "Fizikokimya", "Orta"),
        Soru("Katalizör tepkimede hangisini değiştirir?", listOf("Aktivasyon enerjisini", "Ürün miktarını", "Denge sabitini", "Tepkime ısısını", "Mol sayısını"), 0, "Fizikokimya", "Zor"),
        Soru("Bir sıvının buhar basıncını en çok etkileyen nedir?", listOf("Sıcaklık", "Renk", "Yoğunluktan bağımsız kütle", "Hacim", "Çözünürlük"), 0, "Fizikokimya", "Kolay"),
        Soru("Gay-Lussac yasasına göre sabit hacimde basınç ile sıcaklık nasıl ilişkilidir?", listOf("Doğru orantılı", "Ters orantılı", "İlişkisiz", "Kareyle orantılı", "Logaritmik"), 0, "Fizikokimya", "Orta"),
        Soru("Titrasyonun bittiği ana ne denir?", listOf("Eşdeğer nokta", "Denge noktası", "Erime noktası", "Kaynama noktası", "Kritik nokta"), 0, "Analitik", "Kolay"),
        Soru("Kantitatif (nicel) analizin amacı nedir?", listOf("Maddenin miktarını belirlemek", "Maddenin rengini bulmak", "Maddenin kokusunu bulmak", "Maddenin tadını izlemek", "Maddenin halini görmek"), 0, "Analitik", "Orta"),
        Soru("Gravimetrik analiz hangi ölçüme dayanır?", listOf("Kütle ölçümüne", "Hacim ölçümüne", "Renk ölçümüne", "Ses ölçümüne", "Basınç ölçümüne"), 0, "Analitik", "Orta"),
        Soru("Absorpsiyon spektroskopisi hangi yasayı kullanır?", listOf("Beer-Lambert", "Avogadro", "Dalton", "Charles", "Raoult"), 0, "Analitik", "Zor"),
        Soru("RNA'da bulunup DNA'da normalde bulunmayan baz hangisidir?", listOf("Timin", "Adenin", "Guanin", "Sitozin", "Urasil"), 4, "Biyokimya", "Orta"),
        Soru("Hayvan hücrelerinde glikozun depo formu hangisidir?", listOf("Glikojen", "Laktoz", "Selüloz", "Sükroz", "Maltoz"), 0, "Biyokimya", "Orta"),
        Soru("Enzimlerin yapıtaşı hangisidir?", listOf("Protein", "Karbonhidrat", "Lipit", "Nükleotit", "Metal iyonu"), 0, "Biyokimya", "Kolay"),
        Soru("Çekirdekteki proton sayısına ne denir?", listOf("Atom numarası", "Kütle numarası", "Nötron sayısı", "İzotop sayısı", "Yarı ömür"), 0, "Nükleer", "Kolay"),
        Soru("Alfa ışıması hangi parçacığı yayar?", listOf("Helyum çekirdeği", "Elektron", "Foton", "Nötron", "Pozitron"), 0, "Nükleer", "Orta"),
        Soru("Beta ışımasında hangi parçacık yayılır?", listOf("Elektron veya pozitron", "Helyum çekirdeği", "Gama fotonu", "Nötron", "Alfa parçacığı"), 0, "Nükleer", "Orta"),
        Soru("Bir yarı ömür geçtikten sonra radyoaktif çekirdeklerin ne kadarı kalır?", listOf("%50", "%25", "%75", "%100", "%10"), 0, "Nükleer", "Kolay"),
        Soru("Nükleer füzyon hangisidir?", listOf("Hafif çekirdeklerin birleşmesi", "Ağır çekirdeklerin bölünmesi", "Elektron kaybetme", "Madde kazanma", "Proton çekme"), 0, "Nükleer", "Kolay"),

        Soru("Polietilen monomeri nedir?", listOf("CH2=CH2", "CH2=CHCl", "C6H6", "CH3OH", "CH2=CHCH3"), 0, "Polimer", "Kolay"),
        Soru("PVC'nin tam adı nedir?", listOf("Polivinil klorür", "Polietilen tereftalat", "Polipropilen", "Polistiren", "Polibütilen"), 0, "Polimer", "Kolay"),
        Soru("Naylon hangi tür polimerdir?", listOf("Poliomid", "Poliester", "Poliüretan", "Poliakril", "Polisiloksan"), 0, "Polimer", "Orta"),
        Soru("Termoplastik ne zaman şekil alır?", listOf("Isıtılınca", "Soğutulunca", "Basınç altında", "Işık altında", "Kimyasal madde ile"), 0, "Polimer", "Kolay"),
        Soru("Bakalit hangi tür polimerdir?", listOf("Termoset", "Termoplastik", "Elastomer", "Doğal polimer", "Biyouyumlu polimer"), 0, "Polimer", "Orta"),
        Soru("Polimerizasyon türlerinden katılama polimerizasyonu hangisini içerir?", listOf("Çift bağın açılması", "Su çıkarması", "Gaz çıkarması", "İyon alışverişi", "Redoks"), 0, "Polimer", "Orta"),
        Soru("Kauçuk hangi tür polimerdir?", listOf("Elastomer", "Termoset", "Termoplastik", "Reçine", "Fiber"), 0, "Polimer", "Kolay"),
        Soru("Polietilen termoplastik midir?", listOf("Evet", "Hayır", "Bazen", "Sadece LDPE", "Sadece HDPE"), 0, "Polimer", "Zor"),
        Soru("Addisyon polimerizasyonunda hangisi açılır?", listOf("Çift veya üçlü bağ", "Tekli bağ", "Halka yapısı", "Aromatik halka", "İyonik bağ"), 0, "Polimer", "Orta"),
        Soru("Selüloz hangi polimerden oluşur?", listOf("Glikoz", "Fruktoz", "Galaktoz", "Sakkaroz", "Maltoz"), 0, "Polimer", "Orta"),
        Soru("Polistiren monomeri nedir?", listOf("Stiren (vinil benzen)", "etilen", "propilen", "toluen", "fenol"), 0, "Polimer", "Orta"),
        Soru("Polimer zincirindeki tekrarlayan birime ne denir?", listOf("Monomer", "Rezonans", "İzomer", "Dimer", "Trimer"), 0, "Polimer", "Kolay"),
        Soru("Naylon 6,6 hangi monomerlerden oluşur?", listOf("Adipik asit ve heksametilendiamin", "Stiren ve butadien", "Etilen glikol ve tereftalik asit", "Kaprolaktam", "Fenol ve formaldehit"), 0, "Polimer", "Zor"),
        Soru("PET hangi alanda yaygın kullanılır?", listOf("Plastik şişe", "Cam", "Metal", "Ahşap", "Kağıt"), 0, "Polimer", "Kolay"),

        Soru("Petrolün bileşimi ağırlıklı olarak nedir?", listOf("Hidrokarbonlar", "Oksijen", "Azot", "Su", "Metaller"), 0, "Petrol", "Kolay"),
        Soru("Damıtma yöntemiyle petrol ayrıştırılırken ilk çıkan ürün nedir?", listOf("Benzin", "Mazot", "Asfalt", "Gazyağı", "Motorin"), 0, "Petrol", "Orta"),
        Soru("Kraking işlemi ne yapar?", listOf("Uzun zincirli hidrokarbonları kırar", "Kısa zincirleri birleştirir", "Gaz üretir", "Su üretir", "Metal ayrıştırır"), 0, "Petrol", "Orta"),
        Soru("Doğalgazın ana bileşeni nedir?", listOf("Metan", "Etan", "Propan", "Bütan", "Pentan"), 0, "Petrol", "Kolay"),
        Soru("Reforming işlemi ne işe yarar?", listOf("Oktan sayısını artırır", "Dizel üretir", "Asfalt üretir", "Gaz üretir", "Plastik üretir"), 0, "Petrol", "Orta"),
        Soru("Petrokimya products hangisidir?", listOf("Plastik, lastik, ilaç", "Benzin", "Mazot", "Asfalt", "Gazyağı"), 0, "Petrol", "Kolay"),
        Soru("Ham petrolün rengi genellikle nedir?", listOf("Siyah/kahverengi", "Şeffaf", "Mavi", "Yeşil", "Kırmızı"), 0, "Petrol", "Kolay"),
        Soru("Benzin içindeki oktan sayısı neyi gösterir?", listOf("Vuruntuya dayanıklılık", "Yoğunluk", "Kaynama noktası", "Erime noktası", "Renk"), 0, "Petrol", "Orta"),
        Soru("LPG hangi gazların karışımıdır?", listOf("Propan ve bütan", "Metan ve etan", "Oksijen ve azot", "Hidrojen ve oksijen", "Karbon dioksit ve su"), 0, "Petrol", "Kolay"),
        Soru("Petrol rafinerisinde en çok üretilen ürün nedir?", listOf("Benzin ve dizel", "Asfalt", "Plastik", "Gaz", "Kimyasallar"), 0, "Petrol", "Orta"),
        Soru("Hidrocracking işlemi nedir?", listOf("Hidrojenle kırma", "Isıyla kırma", "Basınçla kırma", "Enzimle kırma", "Işıkla kırma"), 0, "Petrol", "Zor"),
        Soru("Ham petrolden sülfür nasıl alınır?", listOf("Hidrodesülfürizasyon", "Damıtma", "Kristallendirme", "Elektroliz", "Filtrasyon"), 0, "Petrol", "Zor"),
        Soru("Bitümen hangi işlemlerden sonra kalır?", listOf("Damıtma sonrası", "Kraking sonrası", "Reforming sonrası", "Polimerizasyon sonrası", "Nötralizasyon sonrası"), 0, "Petrol", "Orta"),
        Soru("Petrolün-density-sg değeri genellikle nedir?", listOf("0.8-0.9 g/cm3", "1.0-1.2 g/cm3", "0.5-0.6 g/cm3", "1.5-2.0 g/cm3", "0.1-0.3 g/cm3"), 0, "Petrol", "Zor"),
        Soru("Gazyağı hangi amaçla kullanılır?", listOf("Isıtma ve aydınlatma", "Uçak yakıtı", "Plastik üretimi", "İlaç üretimi", "Gıda"), 0, "Petrol", "Orta"),

        Soru("Proteinlerin yapı taşı olan amino asit sayısı?", listOf("20", "10", "30", "50", "100"), 0, "Biyomolekül", "Kolay"),
        Soru("DNA'nın çift sarmal yapısını kim keşfetti?", listOf("Watson ve Crick", "Darwin", "Mendel", "Pasteur", "Koch"), 0, "Biyomolekül", "Kolay"),
        Soru("Glikozun moleküler formülü nedir?", listOf("C6H12O6", "C12H22O11", "C2H5OH", "CH3COOH", "C6H5OH"), 0, "Biyomolekül", "Kolay"),
        Soru("Enzimlerin yapısındaki protein olmayan kısma ne denir?", listOf("Kofaktör", "Substrat", "Ürün", "İnhibitör", "Apoenzim"), 0, "Biyomolekül", "Orta"),
        Soru("RNA'da baz eşleşmesi nasıldır?", listOf("A-U, G-C", "A-T, G-C", "A-U, G-G", "A-C, T-G", "A-A, T-T"), 0, "Biyomolekül", "Orta"),
        Soru("Yağ asitlerinin sınıflandırılmasında çift bağ sayısına bakılır. Doymamış yağ asidinde?", listOf("En az 1 çift bağ var", "Çift bağ yok", "2 veya daha fazla çift bağ", "Sadece tekli bağ", "Halka yapısı"), 0, "Biyomolekül", "Orta"),
        Soru("ATP hangi enerjiyi taşır?", listOf("Hücre içi enerji", "Işık enerjisi", "Kinetik enerji", "Potansiyel enerji", "Isı enerjisi"), 0, "Biyomolekül", "Kolay"),
        Soru("Hücre zarındaki fosfolipitler hangi yapıdadır?", listOf("Çift tabakalı", "Tek tabakalı", "Üç tabakalı", "Küresel", "Doğrusal"), 0, "Biyomolekül", "Orta"),
        Soru("Katalizör enzimlerin hangi özelliğini değiştirir?", listOf("Aktivasyon enerjisini düşürür", "Substrat miktarını artırır", "Ürün sayısını artırır", "pH'ı değiştirir", "Sıcaklığı artırır"), 0, "Biyomolekül", "Orta"),
        Soru("Nükleik asitler hangi monomerlerden oluşur?", listOf("Nükleotitler", "Amino asitler", "Monosakkaritler", "Yağ asitleri", "Steroidler"), 0, "Biyomolekül", "Kolay"),
        Soru("Hangisi bir monosakkarit değildir?", listOf("Sakkaroz", "Glikoz", "Fruktoz", "Galaktoz", "Riboz"), 0, "Biyomolekül", "Orta"),
        Soru("Protein denatürasyonu ne demektir?", listOf("Yapı bozulması", "Yeni bağ oluşumu", "Hızlanma", "Soğuma", "Kristalleşme"), 0, "Biyomolekül", "Orta"),
        Soru("Lipitlerin ana işlevi nedir?", listOf("Enerji depolama", "Enzimatik reaksiyon", "Genetik bilgi", "Taşıma", "Yapısal destek"), 0, "Biyomolekül", "Kolay"),
        Soru("Vitamin C hangi asittir?", listOf("Askorbik asit", "Folik asit", "Pantotenik asit", "Nikotinik asit", "Retinoik asit"), 0, "Biyomolekül", "Orta"),
        Soru("Mitoz bölünme sonunda hücre sayısı nasıl değişir?", listOf("İki katına çıkar", "Yarıya iner", "Aynı kalır", "Üç katına çıkar", "Dörde katlanır"), 0, "Biyomolekül", "Kolay"),

        Soru("Sodyum elementinin atom numarası?", listOf("11", "12", "10", "13", "14"), 0, "Genel", "Kolay"),
        Soru("Periyodik tabloda Alkali metal grubu kaçıncı gruptur?", listOf("1", "2", "13", "14", "17"), 0, "Genel", "Kolay"),
        Soru("İzotoplar atomik kütle farkını neden oluşturur?", listOf("Farklı nöutron sayısı", "Farklı proton sayısı", "Farklı elektron sayısı", "Farklı bağ sayısı", "Farklı orbital"), 0, "Genel", "Orta"),
        Soru("Kovalent bağda elektronlar nasıl davranır?", listOf("Ortak paylaşılır", "Bir atom verir", "Bir atom alır", "Yok olur", "Oluşur"), 0, "Genel", "Kolay"),
        Soru("Periyodik tabloda hangi grup soy gazları içerir?", listOf("Grup 18", "Grup 1", "Grup 2", "Grup 17", "Grup 16"), 0, "Genel", "Kolay"),
        Soru("Kimyasal bağ oluşumunda enerji nasıl davranır?", listOf("Serbest kalır", "Yutulur", "Değişmez", "Dönüşür", "Kaybolur"), 0, "Genel", "Orta"),
        Soru("Elektronegatiflik en yüksek olan element?", listOf("Flor", "Oksijen", "Klor", "Azot", "İyod"), 0, "Genel", "Orta"),
        Soru("İyonik bağ genellikle hangi tür elementler arasında oluşur?", listOf("Metal ve ametal", "İki metal", "İki ametal", "Soy gaz", "Geçiş metal"), 0, "Genel", "Kolay"),
        Soru("Atomun çekirdeğinde hangi parçacıklar bulunur?", listOf("Proton ve nöutron", "Proton ve elektron", "Nöutron ve elektron", "Sadece proton", "Sadece nöutron"), 0, "Genel", "Kolay"),
        Soru("Elektron konfigürasyonunda s orbitali en fazla kaç elektron alır?", listOf("2", "6", "10", "14", "8"), 0, "Genel", "Orta"),

        Soru("Metanolün yapısı nedir?", listOf("CH3OH", "C2H5OH", "CH3COOH", "HCHO", "CH3OCH3"), 0, "Organik", "Kolay"),
        Soru("Alkenlerin genel formülü nedir?", listOf("CnH2n", "CnH2n+2", "CnH2n-2", "CnHn", "CnH3n"), 0, "Organik", "Orta"),
        Soru("Benzin hangi tür hidrokarbon içerir?", listOf("Aromatik", "Alifatik", "Sikloalkan", "Alkin", "Aldehit"), 0, "Organik", "Orta"),
        Soru("Fenol hangi fonksiyonel gruba sahiptir?", listOf("Hidroksil (-OH) aromatik halkada", "Karboksil", "Amin", "Aldehit", "Keton"), 0, "Organik", "Orta"),
        Soru("Katalizör organik tepkimede ne yapar?", listOf("Aktivasyon enerjisini düşürür", "Ürün değişir", "Reaktan ekler", "Ters yöne çevirir", "Isı üretir"), 0, "Organik", "Kolay"),
        Soru("Aseton hangi fonksiyonel gruba sahiptir?", listOf("Keton", "Aldehit", "Alkol", "Asit", "Eter"), 0, "Organik", "Kolay"),
        Soru("Esterleşme tepkimesinde hangi ürün oluşur?", listOf("Ester ve su", "Alkol ve asit", "Aldehit ve su", "Keton ve gaz", "Alkan ve su"), 0, "Organik", "Orta"),
        Soru("Doymuş hidrokarbonlarda kaç tür C-C bağı vardır?", listOf("Sadece tekli bağ", "Çift ve üçlü bağ", "Halka yapısı", "Aromatik bağ", "Çapraz bağ"), 0, "Organik", "Kolay"),
        Soru("Grignard reaktifi hangi elementi içerir?", listOf("Magnezyum", "Sodyum", "Demir", "Bakır", "Kalsiyum"), 0, "Organik", "Zor"),

        Soru("Asitlerin ortak özelliği nedir?", listOf("H+ iyonu verir", "OH- iyonu verir", "Elektron verir", "Elektron alır", "Nötrdür"), 0, "Anorganik", "Kolay"),
        Soru("Kalsiyum karbonat hangi asitle tepkimeye girer?", listOf("HCl", "H2SO4", "HNO3", "HF", "H3PO4"), 0, "Anorganik", "Orta"),
        Soru("Oksitlerin sınıflandırması nedir?", listOf("Asidik ve bazik oksit", "Organik ve inorganik", "Katı ve sıvı", "Beyaz ve siyah", "Sıcak ve soğuk"), 0, "Anorganik", "Orta"),
        Soru("Demir(III) oksit hangi renktedir?", listOf("Kırmızı-kahverengi", "Siyah", "Beyaz", "Mavi", "Yeşil"), 0, "Anorganik", "Orta"),
        Soru("Bakır sülfat çözeltisi hangi renktedir?", listOf("Mavi", "Yeşil", "Kırmızı", "Sarı", "Beyaz"), 0, "Anorganik", "Kolay"),
        Soru("Amonyak hangi asit ile tuz oluşturur?", listOf("HCl (Hidroklorik asit)", "H2SO4", "HNO3", "CH3COOH", "H3PO4"), 0, "Anorganik", "Orta"),
        Soru("Gümüş klorür hangi renkte çökelir?", listOf("Beyaz", "Siyah", "Kırmızı", "Mavi", "Sarı"), 0, "Anorganik", "Zor"),
        Soru("Potasyum permanganat hangi renktedir?", listOf("Mor", "Mavi", "Yeşil", "Sarı", "Beyaz"), 0, "Anorganik", "Orta"),
        Soru("Lityum hangi grubun ilk elementidir?", listOf("Alkali metaller", "Toprak alkali", "Halojenler", "Soy gazlar", "Geçiş metalleri"), 0, "Anorganik", "Kolay"),
        Soru("Kükürt dioksit hangi probleme neden olur?", listOf("Asit yağmuru", "Sera etkisi", "Ozon deliği", "Küresel ısınma", "Su kirliliği"), 0, "Anorganik", "Orta"),

        Soru("Gaz yasalarında ideal gaz kabulü nedir?", listOf("Tanecikler arası etkileşim yok", "Tanecikler büyük", "Tanecikler yavaş", "Tanecikler yüklü", "Tanecikler manyetik"), 0, "Fizikokimya", "Orta"),
        Soru("Entalpi değişimi pozitif ise tepkime nasıldır?", listOf("Endotermik", "Ekzotermik", "Nötr", "Katalitik", "Hızlı"), 0, "Fizikokimya", "Orta"),
        Soru("Denge sabiti Kc hangi durumda büyük olur?", listOf("Ürün fazlaysa", "Reaktan fazlaysa", "İkisi eşitse", "Sıcaklık düşükse", "Basınç yüksekse"), 0, "Fizikokimya", "Orta"),
        Soru("Hız sabiti temperature nasıl bağlıdır?", listOf("Artar", "Azalır", "Değişmez", "Orantılı", "Ters orantılı"), 0, "Fizikokimya", "Orta"),
        Soru("Elektrokimya hücrede elektrik enerjisi nereden gelir?", listOf("Kimyasal tepkime", "Isı", "Işık", "Ses", "Basınç"), 0, "Fizikokimya", "Kolay"),
        Soru("Clark Normalite çözeltisi nedir?", listOf("Ca(OH)2 çözeltisi", "NaOH çözeltisi", "HCl çözeltisi", "H2SO4 çözeltisi", "NaCl çözeltisi"), 0, "Fizikokimya", "Zor"),
        Soru("Dinamik denge nedir?", listOf("İleri ve geri hız eşit", "Reaksiyon durmuş", "Sıcaklık sabit", "Basınç sabit", "Hacim sabit"), 0, "Fizikokimya", "Orta"),
        Soru("Le Chatelier prensibi ne söyler?", listOf("Denge bozulursa sistem karşı koyar", "Sistem her zaman bozulur", "Denge değişmez", "Hız artar", "Sıcaklık düşer"), 0, "Fizikokimya", "Orta"),
        Soru("Kolloit çözeltide parçacık boyutu nedir?", listOf("1-1000 nm", "0.1-1 nm", ">1000 nm", "<0.1 nm", "10-100 nm"), 0, "Fizikokimya", "Zor"),
        Soru("Kimyasal termodinamikte G serbest enerji nedir?", listOf("Kullanılabilir enerji", "Toplam enerji", "Isı enerjisi", "Potansiyel enerji", "Kinetik enerji"), 0, "Fizikokimya", "Orta"),

        Soru("Spektrofotometride absorbans nedir?", listOf("Işık soğurması", "Işık saçılması", "Işık kırılması", "Işık yansıması", "Işık üretimi"), 0, "Analitik", "Orta"),
        Soru("pH-metre hangi prensiple çalışır?", listOf("Elektromotor kuvvet ölçümü", "Renk ölçümü", "Kütle ölçümü", "Hacim ölçümü", "Sıcaklık ölçümü"), 0, "Analitik", "Orta"),
        Soru("Analitik kimyada kalibrasyon ne demektir?", listOf("Cihazın doğrulanması", "Numune alma", "Sonuç hesaplama", "Cam yıkama", "Grafik çizme"), 0, "Analitik", "Orta"),
        Soru("Titrasyonda indikatör ne işe yarar?", listOf("Eşdeğer noktasını gösterir", "Hızı artırır", "Sıcaklığı ölçer", "pH'ı değiştirir", "Çözeltileri karıştırır"), 0, "Analitik", "Kolay"),
        Soru("Gravimetrik analizde çökelme hangi amaçla yapılır?", listOf("Maddenin ayrılmasını sağlamak", "Renk değişimi", "pH ölçümü", "Sıcaklık ölçümü", "Basınç ölçümü"), 0, "Analitik", "Zor"),
        Soru("HPLC'nin açılımı nedir?", listOf("High Performance Liquid Chromatography", "High Pressure Liquid Chromatography", "High Performance Liquid Chemical", "High Precision Liquid Chromatography", "High Power Liquid Chromatography"), 0, "Analitik", "Zor"),

        Soru("Mitoz bölünme kaç aşamadan oluşur?", listOf("4 (Profaz, Metafaz, Anafaz, Telofaz)", "2", "3", "5", "6"), 0, "Biyomolekül", "Orta"),
        Soru("Protein sentezinde mRNA hangi organdan çıkar?", listOf(" çekirdek", "Mitokondri", "Ribozom", "Endoplazmik retikulum", "Golgi"), 0, "Biyomolekül", "Orta"),
        Soru("Fotosentezde ışık enerjisi hangi molekülde depolanır?", listOf("ATP ve NADPH", "ADP", "Glikoz", "Yağ asidi", "Protein"), 0, "Biyomolekül", "Orta"),
        Soru("Hücre solunumunda glikozun ilk parçalandığı yer neresidir?", listOf("Sitoplazma (glikoliz)", "Mitokondri", "Çekirdek", "Ribozom", "Golgi"), 0, "Biyomolekül", "Orta"),
        Soru("DNA replikasyonunda hangi enzim kullanılır?", listOf("DNA polimeraz", "RNA polimeraz", "Lipaz", "Proteaz", "Amilaz"), 0, "Biyomolekül", "Orta"),
        Soru("Kan grubu belirleyen molekül nedir?", listOf("Antijen", "Antikor", "Enzim", "Hormon", "Vitamin"), 0, "Biyomolekül", "Orta"),

        Soru("Hangi element periyodik tablonun en altında yer alır?", listOf("Oganesson (Og)", "Fermiyum", "Nöbeliyum", "Lawrenciyum", "Rutherfordiyum"), 0, "Genel", "Zor"),
        Soru("Atom numarası 79 olan element hangisidir?", listOf("Altın (Au)", "Gümüş (Ag)", "Bakır (Cu)", "Platin (Pt)", "Demir (Fe)"), 0, "Genel", "Orta"),
        Soru("Soy gazların ortak özelliği nedir?", listOf("reaktif olmamaları", "Renksiz olmaları", "Gaz halinde olmaları", "Ağır olmaları", "İyonik bağ yapmamaları"), 0, "Genel", "Kolay"),
        Soru("İzomer kavramı ne anlama gelir?", listOf("Aynı formül, farklı yapı", "Farklı formül, aynı yapı", "Aynı kütle, farklı hacim", "Farklı renk, aynı yapı", "Aynı atom, farklı kütle"), 0, "Genel", "Orta"),
        Soru("Elektron ilgisi nedir?", listOf("Elektron alımı sırasında açığa çıkan enerji", "Elektron verimi", "Proton alımı", "Nöutron alışverişi", "Işık soğurması"), 0, "Genel", "Zor"),
        Soru("İyonlaşma enerjisi periyodik tablonda nasıl değişir?", listOf("Soldan sağa artar, yukarıdan aşağıya azalır", "Soldan sağa azalır", "Yukarıdan aşağıya artar", "Değişmez", "Rastgele"), 0, "Genel", "Orta"),
        Soru("Kristal yapısında yüzey merkezli küp (FCC) birim hücrede kaç atom vardır?", listOf("4", "2", "8", "6", "1"), 0, "Genel", "Zor"),
        Soru("Alaşımların özelliği nedir?", listOf("Birden fazla metalin karışımı", "Saf metal", "Ametal karışımı", "Gaz karışımı", "Sıvı karışımdır"), 0, "Genel", "Kolay"),
        Soru("Hangi element en iyi ısı iletkenidir?", listOf("Gümüş", "Altın", "Bakır", "Demir", "Alüminyum"), 0, "Genel", "Orta"),
        Soru("Süperatif element hangisidir?", listOf("Kaliforniyum", "Hidrojen", "Oksijen", "Karbon", "Demir"), 0, "Genel", "Zor")
    )

class QuizFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()

    private var sorular = tumSorular.toList()
    private var currentQ = 0
    private var score = 0
    private var dogruAdet = 0
    private var yanlisAdet = 0
    private var streak = 0
    private var maxStreak = 0
    private var sure = 20
    private var sureAktif = false
    private var selectedDifficulty = "Hepsi"
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var progressBar: ProgressBar
    private lateinit var timerText: TextView
    private lateinit var questionText: TextView
    private lateinit var resultText: TextView
    private lateinit var streakText: TextView
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

        streakText = TextView(requireContext()).apply {
            text = "Seri: 0"
            setTextColor(0xFFFFD700.toInt())
            textSize = 14f
            setPadding(8, 4, 8, 4)
        }
        (resultText.parent as ViewGroup).addView(streakText, (resultText.parent as ViewGroup).indexOfChild(optionContainer) + 1)

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
                    appendLine("• Kategori seçin: Genel, Organik, Anorganik, Fizikokimya, Analitik, Biyokimya, Nükleer, Polimer, Petrol, Biyomolekül veya Hepsi")
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
        questionText.text = "Kategori Secin"
        timerText.text = ""
        resultText.text = ""
        streakText.text = ""
        nextBtn.isEnabled = false
        progressBar.progress = 0
        optionContainer.removeAllViews()

        val kategoriler = listOf("Genel", "Organik", "Anorganik", "Fizikokimya", "Analitik", "Biyokimya", "Nükleer", "Polimer", "Petrol", "Biyomolekül")
        val zorluklar = listOf("Hepsi", "Kolay", "Orta", "Zor")

        val diffLabel = TextView(requireContext()).apply {
            text = "Zorluk Secin:"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 13f
            setPadding(0, 8, 0, 4)
        }
        optionContainer.addView(diffLabel)

        val diffRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        for (z in zorluklar) {
            val btn = Button(requireContext()).apply {
                text = z
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(12, 8, 12, 8)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(),
                    if (z == "Hepsi") R.color.neon_cyan else R.color.neon_lime)
                alpha = if (z == selectedDifficulty) 1f else 0.5f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    selectedDifficulty = z
                    showCategorySelection()
                }
            }
            diffRow.addView(btn)
        }
        optionContainer.addView(diffRow)

        val catLabel = TextView(requireContext()).apply {
            text = "Kategori Secin:"
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 13f
            setPadding(0, 12, 0, 4)
        }
        optionContainer.addView(catLabel)

        for (kat in kategoriler) {
            val filteredByDiff = tumSorular.filter { q ->
                (selectedDifficulty == "Hepsi" || q.zorluk == selectedDifficulty)
            }
            val count = filteredByDiff.count { it.kategori == kat }
            if (count == 0) continue
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

        val allBtn = Button(requireContext()).apply {
            val totalFiltered = tumSorular.count { q -> selectedDifficulty == "Hepsi" || q.zorluk == selectedDifficulty }
            text = "Hepsi ($totalFiltered soru)"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_lime)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 4) }
            setOnClickListener { startQuiz("Hepsi") }
        }
        optionContainer.addView(allBtn)
    }

    private fun startQuiz(kategori: String) {
        val filtered = tumSorular.filter { q ->
            val matchDiff = selectedDifficulty == "Hepsi" || q.zorluk == selectedDifficulty
            val matchCat = kategori == "Hepsi" || q.kategori == kategori
            matchDiff && matchCat
        }.shuffled()
        sorular = filtered.take(if (kategori == "Hepsi") 25 else 15)
        if (sorular.isEmpty()) {
            Toast.makeText(context, "Bu kategoride secili zorlukta soru yok", Toast.LENGTH_SHORT).show()
            return
        }
        currentQ = 0; score = 0; dogruAdet = 0; yanlisAdet = 0; streak = 0; maxStreak = 0
        progressBar.max = sorular.size
        streakText.text = "Seri: 0"
        loadQuestion()
    }

    private fun loadQuestion() {
        sureAktif = false
        if (currentQ >= sorular.size) { showResults(); return }

        val s = sorular[currentQ]
        questionText.text = "${currentQ + 1}/${sorular.size} [${s.kategori}/${s.zorluk}] ${s.soru}"
        resultText.text = ""
        streakText.text = "Seri: $streak"
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
        if (dogruMu) { score++; dogruAdet++; streak++; if (streak > maxStreak) maxStreak = streak }
        else { yanlisAdet++; streak = 0 }

        for (i in 0 until optionContainer.childCount) {
            val btn = optionContainer.getChildAt(i) as Button
            btn.isEnabled = false
            when {
                i == s.dogru -> btn.setBackgroundColor(0xFF81C784.toInt())
                i == selected && !dogruMu -> btn.setBackgroundColor(0xFFFF1744.toInt())
            }
        }

        val feedback = if (dogruMu) {
            if (streak >= 3) "Dogru! Seri: $streak" else "Dogru!"
        } else "Yanlis! Cevap: ${s.secenekler[s.dogru]}"
        resultText.text = feedback
        resultText.setTextColor(if (dogruMu) 0xFF81C784.toInt() else 0xFFFF4444.toInt())
        streakText.text = "Seri: $streak"

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
                        if (i == sorular[currentQ].dogru) btn.setBackgroundColor(0xFF81C784.toInt())
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
        questionText.text = "Quiz Tamamlandi!"
        timerText.text = ""
        streakText.text = ""
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

        val ilkSoru = sorular.firstOrNull()
        if (ilkSoru == null) { resultText.text = "Soru bulunamadı"; return }

        resultText.text = buildString {
            appendLine("SONUC")
            appendLine()
            appendLine("Skor: $score/${sorular.size} ($yuzde%)")
            appendLine("Not: $notHarf")
            appendLine()
            appendLine("Dogru: $dogruAdet")
            appendLine("Yanlis: $yanlisAdet")
            appendLine()
            appendLine("En uzun seri: $maxStreak")
            appendLine("Kategori: ${ilkSoru.kategori}")
            appendLine("Zorluk: $selectedDifficulty")
        }
        resultText.setTextColor(0xFF4DD0E1.toInt())
        resultText.textSize = 16f

        vm.addHistory("Quiz", "Skor: $score/${sorular.size} ($yuzde%) - $notHarf - Seri: $maxStreak")
    }

    override fun onDestroyView() {
        sureAktif = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }
}

