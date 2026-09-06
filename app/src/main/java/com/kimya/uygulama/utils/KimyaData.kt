package com.kimya.uygulama.utils

import kotlin.math.log10
import kotlin.math.pow

data class ElementData(
    val atomNo: Int, val semIol: String, val adi: String, val kutle: Double,
    val grup: Int, val periyot: Int, val metal: Boolean, val valans: List<Int>,
    val elektron: String, val durum: String,
    val erime: Double = 0.0, val kaynama: Double = 0.0, val yogunluk: Double = 0.0,
    val ozelIsi: Double = 0.0, val isilIletkenlik: Double = 0.0, val katilik: Double = 0.0,
    val manyetik: String = "", val kirkRefraksiyon: Double = 0.0,
    val iyonlasmaEnerjisi: Double = 0.0, val elektronegatiflik: Double = 0.0,
    val bulunsenYil: Int = 0, val adinHikayesi: String = "",
    val fiziksel: String = "", val kimyasal: String = "", val kullanim: String = ""
) {
    val tur: String get() = when {
        atomNo in 57..71 -> "Lantanit"
        atomNo in 89..103 -> "Aktinit"
        atomNo in intArrayOf(2, 10, 18, 36, 54, 86, 118) -> "Soy Gaz"
        !metal -> "Ametal"
        metal && grup in 3..12 -> "Gecis Metali"
        metal && grup == 1 -> "Alkali Metal"
        metal && grup == 2 -> "Toprak Alkali"
        metal && grup in 13..16 -> "Yari Metal"
        else -> "Metal"
    }
}

data class BilesikData(val adi: String, val formulu: String, val bilesenler: List<Pair<String, Int>>, val tur: String, val ozellik: String) {
    val molekulKutlesi: Double get() = KimyaData.elementler.let { els ->
        bilesenler.sumOf { (s, m) -> (els[s]?.kutle ?: 0.0) * m }
    }
}

data class AsitData(val adi: String, val formulu: String, val tur: String, val pH: String, val kullanim: String, val guvenlik: String,
    val ozellik: String = "", val bolum: String = "", val uretim: String = "", val doga: String = "")
data class BazData(val adi: String, val formulu: String, val tur: String, val pH: String, val kullanim: String, val guvenlik: String,
    val ozellik: String = "", val bolum: String = "", val uretim: String = "", val doga: String = "")

object KimyaData {
    val elementler: Map<String, ElementData> by lazy { elementlerOlustur() }

    val bilesikler: List<BilesikData> by lazy {
        listOf(
            BilesikData("Su", "H2O", listOf("H" to 2, "O" to 1), "Kovalent", "Sivi, cozucu"),
            BilesikData("Karbondioksit", "CO2", listOf("C" to 1, "O" to 2), "Kovalent", "Gaz"),
            BilesikData("Sodyum Klorur", "NaCl", listOf("Na" to 1, "Cl" to 1), "Iyonik", "Beyaz Kristal"),
            BilesikData("Kalsiyum Oksit", "CaO", listOf("Ca" to 1, "O" to 1), "Iyonik", "Beyaz toz"),
            BilesikData("Demir(III) Oksit", "Fe2O3", listOf("Fe" to 2, "O" to 3), "Iyonik", "Kirmizi kahve"),
            BilesikData("Amonyak", "NH3", listOf("N" to 1, "H" to 3), "Kovalent", "Gaz, keskin kokulu"),
            BilesikData("Metan", "CH4", listOf("C" to 1, "H" to 4), "Kovalent", "Gaz"),
            BilesikData("Sodyum Hidroksit", "NaOH", listOf("Na" to 1, "O" to 1, "H" to 1), "Iyonik", "Beyaz Katı"),
            BilesikData("Sulfurik Asit", "H2SO4", listOf("H" to 2, "S" to 1, "O" to 4), "Asit", "Yagli sivi"),
            BilesikData("Glikoz", "C6H12O6", listOf("C" to 6, "H" to 12, "O" to 6), "Kovalent", "Beyaz Kristal")
        )
    }

    val asitler: List<AsitData> = listOf(
        AsitData("Hidroklorik Asit", "HCl", "Kuvvetli Asit", "1-2", "Metal temizleme, sindirim", "Oldukca asindirici",
            "Renksiz, keskin kokulu sivi. Suda tamamen iyonlasir. H+ ve Cl- iyonlarini olusturur.",
            "Mide asidi olarak sindirimde, metal yuzey temizlemede, pH regulasyonunda, deri tabaklamada kullanilir.",
            "H2 gaz ile Cl2 gaz 300C'de birlestirilerek veya NaCl + H2SO4 ile endustriyel uretim.",
            "Mide oz suyunda dogal olarak bulunur. Yanardag gazlarinda ve tuz magaralarinda exists eder."),
        AsitData("Sulfurik Asit", "H2SO4", "Kuvvetli Asit", "0-1", "Pil uretimi, gubre, rafineri", "Cok asindirici, yanici",
            "Yogun, yagli kivamda renksiz sivi. Guclu dehidrasyon etkisi var. Su ile siddetli isinma.",
            "Gubre uretiminde, akuli pillerde, petrol rafinerisinde, maden islemede, deterjan uretiminde kullanilir.",
            "Kontak prosesi: SO2 -> SO3 -> H2SO4. Yasirlama kulesinde H2SO4 emilir.",
            "Asit yagmurlarinin ana bileseni. Vulkanik bolgelerde dogal olarak bulunur."),
        AsitData("Nitrik Asit", "HNO3", "Kuvvetli Asit", "1-2", "Gubre, patlayici, metal gravur", "Yakici, duman acici",
            "Renksiz-kirmizi, duman atan sivi. Guclu oksitleyici. Proteine sari renk verir (xantoproteik reaksiyon).",
            "Gubre (amonyum nitrat) uretiminde, patlayici yapiminda, metal gravurde, ila sanayiinde kullanilir.",
            "Ostwald prosesi: NH3 -> NO -> NO2 -> HNO3. Platin katalizor kullanilir.",
            "Gok gurultusunde yildirim N2 ve O2'yi birlestirerek dogal nitrik asit uretir."),
        AsitData("Perklorik Asit", "HClO4", "Kuvvetli Asit", "0-1", "Analitik kimya, roket yaktisi", "Patlayici riski, asindirici",
            "Renksiz, kokusuz sivi. En guclu mineral asitlerden biri. Cok guclu oksitleyici.",
            "Analitik kimyada titrelerde, roket yakiti propelantlarinda, kauuk vulkanizasyonunda kullanilir.",
            "NaClO4 + H2SO4 ile. Perklorat tuzlarinin asitlendirilmesiyle.",
            "Atmosferde cok dusuk miktarda bulunur. Perklir gollerinde perklorat tuzlari olarak exists eder."),
        AsitData("Hidrobromik Asit", "HBr", "Kuvvetli Asit", "0-1", "Organik sentez, foto film", "Asindirici, dumanli",
            "Renksiz, duman atan sivi. Guclu asit. Bromur iyonlari olusturur.",
            "Organik bileklerin sentezinde, foto film uretiminde, ila yapiminda kullanilir.",
            "H2 gaz ile Br2 gazin 500C'de reaksiyonuyla veya KBr + H2SO4 ile.",
            "Deniz suyunda dusuk konsantrasyonda bulunur. Vulkanik bolgelerde gaz olarak cikar."),
        AsitData("Hidroiyodik Asit", "HI", "Kuvvetli Asit", "0-1", "Organik sentez, analiz", "Asindirici, oksidasyona egilimli",
            "Renksiz, duman atan sivi. Kolayca oksitlenerek I2'ye donusur (sari-kahverengi).",
            "Organik sentezde, analitik kimyada, tibbi preparatlarda kullanilir.",
            "H2 + I2 reaksiyonuyla veya NaI + H2SO4 ile uretilir.",
            "Deniz yosunlarinda yuksek miktarda bulunur. Tiroid bezinde dogal olarak exists eder."),
        AsitData("Asetik Asit", "CH3COOH", "Zayif Asit", "3-5", "Sirke, cozucu, temizlik", "Tahris edici, keskin kokulu",
            "Renksiz, keskin kokulu sivi. %5-8 cozeltisi sirke olarak bilinir. Zayif asit oldugu icin sadece kismen iyonlasir.",
            "Gida sektorunde sirke olarak, cozucu olarak, sentezlerde, temizlik urunlerinde, ila yapiminda kullanilir.",
            "Etanolun asetik asit bakterileriyle (Acetobacter) fermantasyonuyla. Endustriyelde metanol karbonilasyonuyla.",
            "Meyve asitlerinde dogal olarak bulunur. Fermante edilmis gidalarda (sirke, yogurt) exists eder."),
        AsitData("Fosforik Asit", "H3PO4", "Orta Guclu Asit", "2-4", "Gubre, gida katki, pas temizleme", "Tahris edici",
            "Renksiz, hafif viskoz sivi. Triprotik asit. H3PO4, H2PO4-, HPO4--, PO4--- dengeleri.",
            "Gubre (super fosfat) uretiminde, iceceklerde (kola), gida katki maddesi olarak, pas temizlemede kullanilir.",
            "Fosfat kayalarinin H2SO4 ile islenmesiyle veya P2O5 + 3H2O -> 2H3PO4.",
            "Canli hucrelerde ATP'nin yapisiinda bulunur. Kemik ve dis yapisinda exists eder."),
        AsitData("Hidroflorik Asit", "HF", "Zayif Asit", "3-5", "Cam asindirma, deri temizleme", "COK TEHLIKELI, deriye nufuz",
            "Renksiz, keskin kokulu sivi. Zayif asit olmasina ragmen cok tehlikeli. Cami asindirir.",
            "Cam asindirmada, yari iletken temizlemede, deri tabaklamada, uranyum islemede kullanilir.",
            "CaF2 + H2SO4 ile veya H2 + F2 -> 2HF (kontrollu kosullarda).",
            "Florit mineralinde dogal olarak bulunur. Dis macunlarinda fluoror olarak eklenir."),
        AsitData("Karbonik Asit", "H2CO3", "Zayif Asit", "4-6", "Dogal maden suyu, icecek", "Zararsiz, dogal",
            "CO2'nin suda cozunmesiyle olusan zayif asit. Dengede: CO2 + H2O <-> H2CO3.",
            "Maden sularinda, gazli iceceklerde, kan pH regulasyonunda, fotograflarda kullanilir.",
            "CO2 gazinin suya basincl altinda cozunmesiyle. Kapal sistemde uretilir.",
            "Kanimizda pH tampon sistemi olarak bulunur. Yagmur sularinda CO2 cozunerek olusur."),
        AsitData("Sitrik Asit", "C6H8O7", "Zayif Asit", "2-4", "Gida, temizlik, kozmetik", "Zararsiz, dogal",
            "Beyaz kristal toz, ekşi tatta. Trikarboksilik asit. Guclu komplekslastirici.",
            "Gida katki maddesi olarak (E330), temizlik urunlerinde, kozmetikte, ila sanayiinde kullanilir.",
            "Aspergillus niger mantariyla sitrik asit fermantasyonuyla veya limon suyundan kristallendirmeyle.",
            "Narenciyede (limon, portakal, greyfurt) dogal olarak bol miktarda bulunur."),
        AsitData("Laktik Asit", "C3H6O3", "Zayif Asit", "3-5", "Gida, kozmetik, eczacilik", "Zararsiz, dogal",
            "Renksiz-seffaf sivi veya beyaz katı. Alfa-Hidroksi asit (AHA). Hafif ekşi tatta.",
            "Yogurt, peynir, turşu uretiminde, kozmetikte (cilt bakimi), eczacilikta, biyoplastik uretiminde kullanilir.",
            "Laktik asit bakterileri (Lactobacillus) tarafindan laktoz fermantasyonuyla uretilir.",
            "Insan kas hucrelerinde egzersiz sirasinda, yogurttta, turşuda dogal olarak bulunur."),
        AsitData("Okzalik Asit", "H2C2O4", "Zayif Asit", "2-3", "Leke cikarma, ahşap temizleme", "Zehirli, asindirici",
            "Beyaz kristal toz. En guclu organik asitlerden biri. Guclu indirgeyici.",
            "Leke cikarmada, ahşap temizlemede, metal parlatmada, analyze edici reaktif olarak kullanilir.",
            "Sodyum formattan oksidasyonla veya kakao yan urununden elde edilir.",
            "Ispanak, kereviz, kakao gibi bitkilerde dogal olarak bulunur. Bobrek tasi olusumuna katkida bulunur."),
        AsitData("Benzoik Asit", "C6H5COOH", "Zayif Asit", "4-5", "Gida koruyucu, antifungal", "Hafif tahris edici",
            "Beyaz pullu kristal. Aromatik karboksilik asit. Suda dusuk cozunurluk.",
            "Gida koruyucu olarak (E210), antifungal olarak, boya uretiminde, parfum endustrisinde kullanilir.",
            "Toluenin oksidasyonuyla veya katalitik hidrolizle uretilir.",
            "Yaban mersini, kuzuuzu gibi meyvelerde bulunur. Rezinede (benzoin) exists eder."),
        AsitData("Tartarik Asit", "C4H6O6", "Zayif Asit", "2-3", "Sarap, gida, eczacilik", "Zararsiz, dogal",
            "Beyaz kristal toz. Ekşi tatta. Sarap asidi olarak da bilinir. Cift yonlu optik aktivite.",
            "Sarap uretiminde, gida katki maddesi olarak (E334), eczacilikta, seker uretiminde kullanilir.",
            "Sarap cokeltisinden (tartar) kalsium tartratin asitlenmesiyle elde edilir.",
            "Uzumde, sarapta, muzda, kivi meyvesinde dogal olarak bulunur.")
    )

    val bazlar: List<BazData> = listOf(
        BazData("Sodyum Hidroksit", "NaOH", "Kuvvetli Baz", "13-14", "Sabun, kagit, biyodizel", "Asindirici, yakici",
            "Beyaz katı, nem cekici. Suda cok cozunur. Guclu baz. Yaglarla sabunlama reaksiyonu yapar.",
            "Sabun uretiminde (sabunlama), kagit uretiminde, biyodizel uretiminde, kanal temizlemede, gida islemede kullanilir.",
            "NaCl tuzunun elektrolizi ile (klor-alkali prosesi). 2NaCl + 2H2O -> 2NaOH + H2 + Cl2.",
            "Dogada saf halde bulunmaz. Ancak sodyum karbonat (soda) mineralinden elde edilir."),
        BazData("Potasyum Hidroksit", "KOH", "Kuvvetli Baz", "13-14", "Sabun, pil, gubre", "Asindirici",
            "Beyaz katı, nem cekici. NaOH ile benzer ozellikler. Potasyum sabunlari sivi sabun yapiminda kullanilir.",
            "Sivi sabun uretiminde, pil (alkali pil) uretiminde, gubre uretiminde, yumuşaklik yapiminda kullanilir.",
            "KCl tuzunun elektrolizi ile veya K2CO3 + Ca(OH)2 -> 2KOH + CaCO3.",
            "Dogada saf halde bulunmaz. Volkanik kil extraksion ile elde edilebilir."),
        BazData("Baryum Hidroksit", "Ba(OH)2", "Kuvvetli Baz", "13-14", "Endustri, analitik", "Zehirli, asindirici",
            "Beyaz katı. Suda orta duzeyde cozunur. Zehirli baryum iyonlari toksik.",
            "Seker rafinerisinde, analitik kimyada titrelerde, endustriyel sentezlerde kullanilir.",
            "BaO + H2O -> Ba(OH)2 reaksiyonuyla. Baryum oksidin su ile reaksiyonu.",
            "Barit mineralinde (BaSO4) dogal olarak bulunur. Saf Ba(OH)2 sentetik olarak uretilir."),
        BazData("Kalsiyum Hidroksit", "Ca(OH)2", "Orta Guclu Baz", "12-13", "Kirec, insaat, su aritma", "Tahris edici",
            "Beyaz toz. Suda dusuk cozunurluk. Kirec suyu olarak bilinir. CO2 ile sertlesir.",
            "Insaat sektorunde (harç, siva), su aritmada, dezenfeksiyonda, kirec-boyama islerinde kullanilir.",
            "CaO + H2O -> Ca(OH)2. Kirec tasinin kalsinasyonuyla elde edilen CaO'nun su ile reaksiyonu.",
            "Kirec tasinda, mermerde dogal olarak bulunur. Kirec suyu olarak tarihten beri kullanilir."),
        BazData("Lityum Hidroksit", "LiOH", "Kuvvetli Baz", "13-14", "Pil, eczacilik", "Asindirici",
            "Beyaz katı. Nem cekici. Li+ iyonu bipolar bozukluk tedavisinde kullanilir.",
            "Lityum-iyon pil uretiminde (katot stabilizor), eczacilikta (duygudurum duzenleyici), gres uretiminde kullanilir.",
            "Li2CO3 + Ca(OH)2 -> 2LiOH + CaCO3 reaksiyonuyla.",
            "Spanyolit (LiAlSi4O10) ve spodumen minerallerinde dogal olarak bulunur."),
        BazData("Amonyak", "NH3", "Zayif Baz", "11-12", "Temizlik, gubre, soğutma", "Keskin kokulu, tahris edici",
            "Renksiz, keskin kokulu gaz. Hafif baz. Suda iyonlasarak NH4+ ve OH- olusturur.",
            "Gubre (amonyum nitrat, ure) uretiminde, temizlik urunlerinde, sogutma sistemlerinde (soguk depo), tekstilde kullanilir.",
            "Haber-Bosch prosesi: N2 + 3H2 -> 2NH3 (yuksek basinc, Fe katalizor).",
            "Dogada bakteri metabolizmasi, yildirim, volkanik aktivite ile olusur. Gubrelerden kaynaklanir."),
        BazData("Sodyum Bikarbonat", "NaHCO3", "Zayif Baz", "8-9", "Kabartma tozu, antasit, temizlik", "Zararsiz",
            "Beyaz toz. Suda cozundugunde hafif bazik cozelti olusturur. CO2 gaz asagi cikarak kabartma yapar.",
            "Kabartma tozu uretiminde, antasit olarak (mide asidi notralize), temizlikte, yangin sondurucude kullanilir.",
            "Na2CO3 + CO2 + H2O -> 2NaHCO3. Solvay prosesinin yan urunu.",
            "Natron minerallerinde dogal olarak bulunur. Volkanik gollerde exists eder."),
        BazData("Magnezyum Hidroksit", "Mg(OH)2", "Zayif Baz", "10-11", "Antasit, yangin sondurucu", "Zararsiz",
            "Beyaz toz. Suda dusuk cozunurluk. Sut disturbansi olarak bilinir.",
            "Antasit olarak (mide yanmasi tedavisi), yangin sondurucude, su aritmada, eczacilikta kullanilir.",
            "MgO + H2O -> Mg(OH)2. Deniz suyundan magnezyum hidroksit cikartma ile.",
            "Brusit mineralinde dogal olarak bulunur. Deniz suyunda dusuk konsantrasyonda exists eder."),
        BazData("Kalsiyum Karbonat", "CaCO3", "Zayif Baz", "9-10", "Insaat, dis macunu, kirec", "Zararsiz",
            "Beyaz toz/kristal. Suda cok dusuk cozunurluk. Asitle CO2 asagi cikarak cozunur.",
            "Insaat sektorunde (cimento, mermer), dis macunu uretiminde, kirec uretiminde, gida katki maddesi olarak (E170) kullanilir.",
            "Dogal mermer ve kirec tasindan tugutilarak veya Ca(OH)2 + CO2 -> CaCO3 + H2O ile.",
            "Kirec tasinda, mermerde, absentta, yumurta kabugunda dogal olarak bol miktarda bulunur."),
        BazData("Alüminyum Hidroksit", "Al(OH)3", "Zayif Baz", "8-10", "Antasit, gunes koruyucu", "Zararsiz, dusuk cozunurluk",
            "Beyaz amorf toz. Suda cok dusuk cozunurluk. Amfoter yapi - hem asit hem baz ile reaksiyona girer.",
            "Antasit olarak, gunes koruyucu kremlerde, dolgu olarak, cam uretiminde kullanilir.",
            "Boksit cevherinden Bayer prosesi ile: NaAlO2 + H2O -> Al(OH)3.",
            "Behmit, diboksit minerallerinde dogal olarak bulunur. Toprakta exists eder."),
        BazData("Trietanolamin", "(C2H5)3NO", "Zayif Baz", "10-11", "Kozmetik, temizlik", "Hafif tahris edici",
            "Renksiz-seffaf sivi. Viskoz. Iyi cozucu. Amino alkol yapisinda.",
            "Kozmetikte (kremler, sampuanlar), temizlik urunlerinde, emulgator olarak, seker uretiminde kullanilir.",
            "Etilen oksit + amonyak reaksiyonu ile. 3(C2H4O) + NH3 -> (C2H5)3NO.",
            "Dogada sentetik olarak uretilir. Petrol urunlerinden elde edilir."),
        BazData("Piridin", "C5H5N", "Zayif Baz", "8-9", "Organik sentez, cozucu", "Zehirli, yanici",
            "Renksiz sivi. Keskin, hos olmayan koku. Aromatik heterosiklik bileşik.",
            "Organik sentezde cozucu ve reaktif olarak, ila uretiminde, borek ilaclariinda, boya uretiminde kullanilir.",
            "Kopr katiranindan damitma ile veya aldehit + amonyak reaksiyonuyla.",
            "Tutun dumaninda, bazi meyve ve sebzelerde dusuk miktarda bulunur. Pisirilmis etlerde dogal olarak exists eder.")
    )

    val periyodikVeri: Map<Pair<Int, Int>, Pair<String, Int>> by lazy {
        mapOf(
            (1 to 1) to ("H" to 1), (1 to 18) to ("He" to 2),
            (2 to 1) to ("Li" to 3), (2 to 2) to ("Be" to 4), (2 to 13) to ("B" to 5),
            (2 to 14) to ("C" to 6), (2 to 15) to ("N" to 7), (2 to 16) to ("O" to 8),
            (2 to 17) to ("F" to 9), (2 to 18) to ("Ne" to 10),
            (3 to 1) to ("Na" to 11), (3 to 2) to ("Mg" to 12), (3 to 13) to ("Al" to 13),
            (3 to 14) to ("Si" to 14), (3 to 15) to ("P" to 15), (3 to 16) to ("S" to 16),
            (3 to 17) to ("Cl" to 17), (3 to 18) to ("Ar" to 18),
            (4 to 1) to ("K" to 19), (4 to 2) to ("Ca" to 20), (4 to 3) to ("Sc" to 21),
            (4 to 4) to ("Ti" to 22), (4 to 5) to ("V" to 23), (4 to 6) to ("Cr" to 24),
            (4 to 7) to ("Mn" to 25), (4 to 8) to ("Fe" to 26), (4 to 9) to ("Co" to 27),
            (4 to 10) to ("Ni" to 28), (4 to 11) to ("Cu" to 29), (4 to 12) to ("Zn" to 30),
            (4 to 13) to ("Ga" to 31), (4 to 14) to ("Ge" to 32), (4 to 15) to ("As" to 33),
            (4 to 16) to ("Se" to 34), (4 to 17) to ("Br" to 35), (4 to 18) to ("Kr" to 36),
            (5 to 1) to ("Rb" to 37), (5 to 2) to ("Sr" to 38), (5 to 3) to ("Y" to 39),
            (5 to 4) to ("Zr" to 40), (5 to 5) to ("Nb" to 41), (5 to 6) to ("Mo" to 42),
            (5 to 7) to ("Tc" to 43), (5 to 8) to ("Ru" to 44), (5 to 9) to ("Rh" to 45),
            (5 to 10) to ("Pd" to 46), (5 to 11) to ("Ag" to 47), (5 to 12) to ("Cd" to 48),
            (5 to 13) to ("In" to 49), (5 to 14) to ("Sn" to 50), (5 to 15) to ("Sb" to 51),
            (5 to 16) to ("Te" to 52), (5 to 17) to ("I" to 53), (5 to 18) to ("Xe" to 54),
            (6 to 1) to ("Cs" to 55), (6 to 2) to ("Ba" to 56), (6 to 3) to ("La" to 57),
            (6 to 4) to ("Hf" to 72), (6 to 5) to ("Ta" to 73), (6 to 6) to ("W" to 74),
            (6 to 7) to ("Re" to 75), (6 to 8) to ("Os" to 76), (6 to 9) to ("Ir" to 77),
            (6 to 10) to ("Pt" to 78), (6 to 11) to ("Au" to 79), (6 to 12) to ("Hg" to 80),
            (6 to 13) to ("Tl" to 81), (6 to 14) to ("Pb" to 82), (6 to 15) to ("Bi" to 83),
            (6 to 16) to ("Po" to 84), (6 to 17) to ("At" to 85), (6 to 18) to ("Rn" to 86),
            (7 to 1) to ("Fr" to 87), (7 to 2) to ("Ra" to 88), (7 to 3) to ("Ac" to 89),
            (7 to 4) to ("Rf" to 104), (7 to 5) to ("Db" to 105), (7 to 6) to ("Sg" to 106),
            (7 to 7) to ("Bh" to 107), (7 to 8) to ("Hs" to 108), (7 to 9) to ("Mt" to 109),
            (7 to 10) to ("Ds" to 110), (7 to 11) to ("Rg" to 111), (7 to 12) to ("Cn" to 112),
            (7 to 13) to ("Nh" to 113), (7 to 14) to ("Fl" to 114), (7 to 15) to ("Mc" to 115),
            (7 to 16) to ("Lv" to 116), (7 to 17) to ("Ts" to 117), (7 to 18) to ("Og" to 118)
        )
    }

    val lantanitler = listOf(
        "Ce" to 58, "Pr" to 59, "Nd" to 60, "Pm" to 61, "Sm" to 62,
        "Eu" to 63, "Gd" to 64, "Tb" to 65, "Dy" to 66, "Ho" to 67,
        "Er" to 68, "Tm" to 69, "Yb" to 70, "Lu" to 71
    )

    val aktinitler = listOf(
        "Th" to 90, "Pa" to 91, "U" to 92, "Np" to 93, "Pu" to 94,
        "Am" to 95, "Cm" to 96, "Bk" to 97, "Cf" to 98, "Es" to 99,
        "Fm" to 100, "Md" to 101, "No" to 102, "Lr" to 103
    )

    fun elementRengi(tur: String): Int = when (tur) {
        "Soy Gaz" -> 0xFFE07090.toInt()
        "Ametal" -> 0xFF6AAF40.toInt()
        "Yari Metal" -> 0xFFAA70B8.toInt()
        "Alkali Metal" -> 0xFFCC5544.toInt()
        "Toprak Alkali" -> 0xFFCC7A30.toInt()
        "Lantanit" -> 0xFF55AA55.toInt()
        "Aktinit" -> 0xFFCC8830.toInt()
        else -> 0xFF40A0A8.toInt()
    }


    private fun elementlerOlustur(): Map<String, ElementData> {
        val raw = listOf(
            ElementData(atomNo=1, semIol="H", adi="Hidrojen", kutle=1.008, grup=1, periyot=1, metal=false, valans=listOf(1,-1), elektron="1s1", durum="Gaz", erime=-259.16, kaynama=-252.87, yogunluk=0.00009, ozelIsi=14.3, isilIletkenlik=0.1805, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1766, adinHikayesi="Yunancada 'hydro' (su) ve 'genes' (oluşturan) kelimelerinden türemiştir.", fiziksel="Renksiz, kokusuz, çok hafif bir gaz. Görünmez ve tatsızdır.", kimyasal="Yükseltgenme: +1, -1. Bileşikleri: H₂O (su), HCl (hidroklorik asit), NH₃ (amonyak), H₂SO₄ (sülfürik asit), CH₄ (metan). Suda çözünmez ama güçlü indirgeyicidir. Oksijenle şiddetli patlama ile su oluşturur. Asitlerde H₂ gazı açığa çıkar. Metal oksitlerini indirgeyerek metal elde eder.", kullanim="Yakıt hücresi, amonyak üretimi, hidrojenleme, uzay yakıtları, petrol rafinerisi"),
            ElementData(atomNo=2, semIol="He", adi="Helyum", kutle=4.003, grup=18, periyot=1, metal=false, valans=listOf(0), elektron="1s2", durum="Gaz", erime=-272.2, kaynama=-268.93, yogunluk=0.00018, ozelIsi=5.19, isilIletkenlik=0.152, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1868, adinHikayesi="Yunancada 'helios' (güneş) kelimesinden türemiştir.", fiziksel="Renksiz, kokusuz,logsuz bir gaz. Kaynama noktası tüm elementlerin en düşüğüdür.", kimyasal="Yükseltgenme: 0 (inert). Bileşikleri: HeNe (lazer gazı), He₂ (teorik). Hiçbir elementle bileşik oluşturmaz. Soy gazdır, kimyasal olarak tamamen pasiftir. Kriyojenik uygulamalarda soğutucu gaz olarak kullanılır.", kullanim="MRI soğutucu, balon, dalış tankları, lazer, neon tabelalar"),
            ElementData(atomNo=3, semIol="Li", adi="Lityum", kutle=6.941, grup=1, periyot=2, metal=true, valans=listOf(1), elektron="[He] 2s1", durum="Kati", erime=180.54, kaynama=1342.0, yogunluk=0.534, ozelIsi=3.58, isilIletkenlik=84.8, katilik=0.6, manyetik="Paramanyetik", bulunsenYil=1817, adinHikayesi="Yunancada 'lithos' (taştan) kelimesinden türemiştir.", fiziksel="Gümüşümsü, çok yumuşak bir metal. Bıçakla kesilebilir.", kimyasal="Yükseltgenme: +1. Bileşikleri: LiOH (lityum hidroksit), Li₂CO₃ (lityum karbonat), LiFePO₄ (pil katodu), LiAlH₄ (indirgeyici). Suda violent tepkimeye girerek LiOH ve H₂ açığa çıkarır. Havada hemen kararır. Güçlü indirgeyicidir. Yağlarla tepkimeye girme riski vardır.", kullanim="Lityum-iyon pil, ilaçlar, alaşımlar, seramik"),
            ElementData(atomNo=4, semIol="Be", adi="Berilyum", kutle=9.012, grup=2, periyot=2, metal=true, valans=listOf(2), elektron="[He] 2s2", durum="Kati", erime=1287.0, kaynama=2471.0, yogunluk=1.85, ozelIsi=1.825, isilIletkenlik=200.0, katilik=5.5, manyetik="Diamanyetik", bulunsenYil=1798, adinHikayesi="Mineral beryl'den türetilmiştir.", fiziksel="Gri-çelik renginde, sert ve hafif bir metal. Yüzeyi parlaktır.", kimyasal="Yükseltgenme: +2. Bileşikleri: BeO (berilyum oksit), Be(OH)₂ (berilyum hidroksit), BeF₂ (berilyum flüorür). Amfoter yapıdadır, hem asit hem baz ile tepkimeye girer. Toksiktir, tozu kanserojendir. Asitlerde zor çözünür, bazlarda daha kolay.", kullanim="Uzay ve havacılık alaşımları, X-ray pencere, nükleer reaktör"),
            ElementData(atomNo=5, semIol="B", adi="Bor", kutle=10.811, grup=13, periyot=2, metal=true, valans=listOf(3), elektron="[He] 2s2 2p1", durum="Kati", erime=2076.0, kaynama=3927.0, yogunluk=2.34, ozelIsi=1.026, isilIletkenlik=27.4, katilik=9.3, manyetik="Diamanyetik", bulunsenYil=1808, adinHikayesi="Arapça 'buraq' kelimesinden türemiştir.", fiziksel="Koyu kahverengi, çok sert bir yarı metal. Kristal yapıda parlak yüzey.", kimyasal="Yükseltgenme: +3. Bileşikleri: H₃BO₃ (borik asit), B₂O₃ (bor trioksit), Na₂B₄O₇ (boraks), BF₃ (bor triflorür). Asitlere karşı dirençlidir. Sıcak NaOH çözeltisinde çözünür. Yüksek erime noktasına sahiptir. Yarı iletken özellik gösterir.", kullanim="Cam (borosilikat), deterjan, fiberoptik, nükleer reaktör"),
            ElementData(atomNo=6, semIol="C", adi="Karbon", kutle=12.011, grup=14, periyot=2, metal=false, valans=listOf(4,2,-4), elektron="[He] 2s2 2p2", durum="Kati", erime=3550.0, kaynama=4027.0, yogunluk=2.267, ozelIsi=0.709, isilIletkenlik=140.0, katilik=10.0, manyetik="Diamanyetik", bulunsenYil=0, adinHikayesi="Latince 'carbo' (kömür) kelimesinden türemiştir.", fiziksel="Siyah (grafit) veya renksiz (elmas) katı. Allotropları çok farklı özellikler gösterir.", kimyasal="Yükseltgenme: +4, +2, -4. Bileşikleri: CO₂ (karbondioksit), CH₄ (metan), C₂H₅OH (etanol), C₆H₁₂O₆ (glikoz), H₂CO₃ (karbonik asit). Yanarak CO₂ oluşturur. Sıcak konsantrasyonlu sülfürik asitte oksitlenir. Allotropları: elmas, grafit, fuleren, grafen. Organik kimyanın temelidir.", kullanim="Elmas, grafit, kauçuk, lastik, aktif karbon, nükleer reaktör"),
            ElementData(atomNo=7, semIol="N", adi="Azot", kutle=14.007, grup=15, periyot=2, metal=false, valans=listOf(3,5,-3), elektron="[He] 2s2 2p3", durum="Gaz", erime=-210.0, kaynama=-195.79, yogunluk=0.00125, ozelIsi=1.04, isilIletkenlik=0.026, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1772, adinHikayesi="Yunancada 'nitron' (sodyum karbonat) kelimesinden türemiştir.", fiziksel="Renksiz, kokusuz, inert bir gaz. Atmosferin %78'ini oluşturur.", kimyasal="Yükseltgenme: -3, +3, +5. Bileşikleri: NH₃ (amonyak), HNO₃ (nitrik asit), NaNO₃ (sodyum nitrat), N₂O (gülme gazı), NO₂ (dinitrojen tetroksit). N₂ molekülü çok kararlıdır (üçlü bağ). HNO₃ güçlü oksitleyicidir. Amonyak konsantrasyonlu asitlerle tepkimeye girer.", kullanim="Gübre, patlayıcı, soğutucu, amonyak üretimi, gıda koruyucu"),
            ElementData(atomNo=8, semIol="O", adi="Oksijen", kutle=15.999, grup=16, periyot=2, metal=false, valans=listOf(2,-2,-1), elektron="[He] 2s2 2p4", durum="Gaz", erime=-218.79, kaynama=-182.96, yogunluk=0.00143, ozelIsi=0.92, isilIletkenlik=0.0265, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1774, adinHikayesi="Yunancada 'oxys' (asit) ve 'genes' (oluşturan) kelimelerinden türemiştir.", fiziksel="Renksiz, kokusuz bir gaz. Tüm canlılar için yaşamsal öneme sahiptir.", kimyasal="Yükseltgenme: -2, -1. Bileşikleri: H₂O (su), CO₂ (karbondioksit), H₂O₂ (hidrojen peroksit), SiO₂ (silisyum dioksit). Çok güçlü oksitleyicidir. Çoğu elementle bileşik oluşturur. Allotropu olan ozon (O₃) çok güçlü bir oksitleyicidir. Su molekülü hidrojen bağı oluşturur.", kullanim="Solunum, kaynak, su arıtma, tıp, metal işleme"),
            ElementData(atomNo=9, semIol="F", adi="Flor", kutle=18.998, grup=17, periyot=2, metal=false, valans=listOf(1,-1), elektron="[He] 2s2 2p5", durum="Gaz", erime=-219.67, kaynama=-188.11, yogunluk=0.0017, ozelIsi=0.824, isilIletkenlik=0.0279, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1886, adinHikayesi="Latince 'fluere' (akışkan) kelimesinden türemiştir.", fiziksel="Sarı-yeşil, keskin kokulu, çok zehirli bir gaz.", kimyasal="Yükseltgenme: -1. Bileşikleri: HF (hidroflorik asit), NaF (sodyum flüorür), Teflon (PTFE), UF₆ (uranyum hekzaflüorür). En reaktif ametaldir. Tüm elementlerle (soy gazlar hariç) bileşik oluşturur. En yüksek elektronegatifliğe (3.98) sahiptir. Camı aşındırır.", kullanim="Diş macunu (florür), Teflon, soğutucu gaz, uranyum zenginleştirme"),
            ElementData(atomNo=10, semIol="Ne", adi="Neon", kutle=20.180, grup=18, periyot=2, metal=false, valans=listOf(0), elektron="[He] 2s2 2p6", durum="Gaz", erime=-248.59, kaynama=-246.08, yogunluk=0.0009, ozelIsi=1.03, isilIletkenlik=0.0491, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1898, adinHikayesi="Yunancada 'neos' (yeni) kelimesinden türemiştir.", fiziksel="Renksiz, kokusuz bir gaz. Elektrik altında parlak turuncu-kırmızı ışık yayar.", kimyasal="Yükseltgenme: 0 (inert). Bileşikleri: NeF₂ (teorik, çok kararsız). Soy gazdır, kimyasal olarak pasiftir. Neon lambalarında turuncu-kırmızı ışık yayar. Yüksek iyonizasyon enerjisi nedeniyle bileşik oluşturmaz.", kullanim="Neon tabelalar, lazer, kriyojenik, vakum tüpleri"),
            ElementData(atomNo=11, semIol="Na", adi="Sodyum", kutle=22.990, grup=1, periyot=3, metal=true, valans=listOf(1), elektron="[Ne] 3s1", durum="Kati", erime=97.72, kaynama=883.0, yogunluk=0.971, ozelIsi=1.228, isilIletkenlik=142.0, katilik=0.5, manyetik="Paramanyetik", bulunsenYil=1807, adinHikayesi="Latince 'natrium' kelimesinden türemiştir.", fiziksel="Gümüşümsü, yumuşak bir metal. Bıçakla kesilebilir, hava kararır.", kimyasal="Yükseltgenme: +1. Bileşikleri: NaCl (sofra tuzu), NaOH (sodyum hidroksit), Na₂CO₃ (sodyum karbonat), NaHCO₃ (kabartma tozu), Na₂O₂ (sodyum peroksit). Suda violent tepkimeye girer. Alkali metal grubundadır. Yağ asitleriyle sabunlaşma reaksiyonu yapar.", kullanim="Sofra tuzu (NaCl), sabun, kağıt, cam, deterjan"),
            ElementData(atomNo=12, semIol="Mg", adi="Magnezyum", kutle=24.305, grup=2, periyot=3, metal=true, valans=listOf(2), elektron="[Ne] 3s2", durum="Kati", erime=650.0, kaynama=1091.0, yogunluk=1.738, ozelIsi=1.023, isilIletkenlik=156.0, katilik=2.5, manyetik="Paramanyetik", bulunsenYil=1755, adinHikayesi="Mineral magnesia'dan türetilmiştir.", fiziksel="Gümüşümsü, hafif ve güçlü bir metal. Parlak beyaz alevle yanar.", kimyasal="Yükseltgenme: +2. Bileşikleri: MgO (magnezyum oksit), Mg(OH)₂ (magnezyum hidroksit), MgCl₂ (magnezyum klorür), MgSO₄ (magnezyum sülfat). Suda yavaş tepkimeye girer. Asitlerle violent hidrojen açığa çıkarır. Havada ince oksit tabakası oluşturur.", kullanim="Alaşımlar, havai fişek, el feneri, antiasit, fotoğrafçılık"),
            ElementData(atomNo=13, semIol="Al", adi="Aluminyum", kutle=26.982, grup=13, periyot=3, metal=true, valans=listOf(3), elektron="[Ne] 3s2 3p1", durum="Kati", erime=660.32, kaynama=2519.0, yogunluk=2.7, ozelIsi=0.897, isilIletkenlik=237.0, katilik=2.75, manyetik="Paramanyetik", bulunsenYil=1825, adinHikayesi="Latince 'alumen' kelimesinden türemiştir.", fiziksel="Gümüşümsü, hafif, sünek bir metal. Korozyona dayanıklıdır.", kimyasal="Yükseltgenme: +3. Bileşikleri: Al₂O₃ (alüminyum oksit/boksit), Al(OH)₃ (alüminyum hidroksit), AlCl₃ (alüminyum klorür), Al₂(SO₄)₃ (alüminyum sülfat). Amfoter yapıdadır. Güçlü indirgeyicidir (termomit reaksiyonu). Oksit tabakası koruyucudur.", kullanim="Folyo, konserve kutusu, pencere, uçak, içecek kutuları"),
            ElementData(atomNo=14, semIol="Si", adi="Silisyum", kutle=28.086, grup=14, periyot=3, metal=true, valans=listOf(4), elektron="[Ne] 3s2 3p2", durum="Kati", erime=1414.0, kaynama=3265.0, yogunluk=2.3296, ozelIsi=0.705, isilIletkenlik=149.0, katilik=6.5, manyetik="Diamanyetik", bulunsenYil=1824, adinHikayesi="Latince 'silex' kelimesinden türemiştir.", fiziksel="Gri-mavi, parlak yarı metal. Kristal yapıda yarı saydam.", kimyasal="Yükseltgenme: +4, -4. Bileşikleri: SiO₂ (kuartz), SiC (silisyum karbür), Si₃N₄ (silisyum nitrür), silikon polymerleri. Asitlere karşı dirençlidir. Sıcak alkalilerde çözünür. Yarı iletken özellik gösterir. Silisyum dioksit cam ve seramiğin temelidir.", kullanim="Mikroçip, güneş paneli, silikon, cam, seramik"),
            ElementData(atomNo=15, semIol="P", adi="Fosfor", kutle=30.974, grup=15, periyot=3, metal=false, valans=listOf(3,5,-3), elektron="[Ne] 3s2 3p3", durum="Kati", erime=44.15, kaynama=280.5, yogunluk=1.82, ozelIsi=0.769, isilIletkenlik=0.236, katilik=0.5, manyetik="Diamanyetik", bulunsenYil=1669, adinHikayesi="Yunancada 'phosphoros' kelimesinden türemiştir.", fiziksel="Beyaz (zehirli, yanıcı) veya kırmızı (kararlı) katı.", kimyasal="Yükseltgenme: +3, +5, -3. Bileşikleri: H₃PO₄ (fosforik asit), P₄O₁₀ (fosfor pentoksit), Ca₃(PO₄)₂ (kalsiyum fosfat), PCl₃ (fosfor triklorür). Beyaz fosfor havada yanarak P₄O₁₀ oluşturur. Kırmızı fosfor daha kararlıdır. Nükleik asitlerin yapısında bulunur.", kullanim="Gübre, kibrit, deterjan, mermi, nükleer reaktör"),
            ElementData(atomNo=16, semIol="S", adi="Kükürt", kutle=32.065, grup=16, periyot=3, metal=false, valans=listOf(2,4,6,-2), elektron="[Ne] 3s2 3p4", durum="Kati", erime=115.21, kaynama=444.6, yogunluk=2.067, ozelIsi=0.706, isilIletkenlik=0.265, katilik=2.0, manyetik="Diamanyetik", bulunsenYil=0, adinHikayesi="Latince 'sulphur' kelimesinden türemiştir.", fiziksel="Sarı, kokusuz, kırılgan bir katı. Erime noktası düşüktür.", kimyasal="Yükseltgenme: -2, +4, +6. Bileşikleri: H₂SO₄ (sülfürik asit), SO₂ (kükürt dioksit), H₂S (hidrojen sülfür), CS₂ (karbon disülfit). Yanarak SO₂ açığa çıkarır. Sülfürik asit üretimi endüstrinin temelidir. Bakterilerle metabolik olarak indirgenebilir.", kullanim="Gübre, sülfürik asit, kauçuk, barut, ilaç"),
            ElementData(atomNo=17, semIol="Cl", adi="Klor", kutle=35.453, grup=17, periyot=3, metal=false, valans=listOf(1,3,5,7,-1), elektron="[Ne] 3s2 3p5", durum="Gaz", erime=-101.5, kaynama=-34.04, yogunluk=0.0032, ozelIsi=0.479, isilIletkenlik=0.0089, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1774, adinHikayesi="Yunancada 'chloros' kelimesinden türemiştir.", fiziksel="Sarı-yeşil, keskin kokulu, zehirli bir gaz.", kimyasal="Yükseltgenme: -1, +1, +3, +5, +7. Bileşikleri: NaCl (tuz), HCl (hidroklorik asit), NaOCl (çamaşır suyu), PVC (polivinil klorür), KClO₃ (potasyum klorat). Suda çözündüğünde HClO (hipoklorous asit) oluşturur. Güçlü oksitleyicidir. Dezenfektan olarak kullanılır.", kullanim="Su arıtma, çamaşır suyu, PVC, tuz, dezenfektan"),
            ElementData(atomNo=18, semIol="Ar", adi="Argon", kutle=39.948, grup=18, periyot=3, metal=false, valans=listOf(0), elektron="[Ne] 3s2 3p6", durum="Gaz", erime=-189.34, kaynama=-185.85, yogunluk=0.0018, ozelIsi=0.52, isilIletkenlik=0.0177, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1894, adinHikayesi="Yunancada 'argos' kelimesinden türemiştir.", fiziksel="Renksiz, kokusuz, inert bir gaz. Atmosferin %0.93'ünü oluşturur.", kimyasal="Yükseltgenme: 0 (inert). Bileşikleri: ArF (teorik). Soy gazdır, kimyasal olarak pasiftir. Atmosferin %0.93'ünü oluşturur. Kaynak işlemlerinde koruyucu gaz olarak kullanılır. Lambalarda tungsten flamentini oksidasyondan korur.", kullanim="Kaynak, ampul, lazer, cam üretimi, kriyojenik"),
            ElementData(atomNo=19, semIol="K", adi="Potasyum", kutle=39.098, grup=1, periyot=4, metal=true, valans=listOf(1), elektron="[Ar] 4s1", durum="Kati", erime=63.38, kaynama=759.0, yogunluk=0.89, ozelIsi=0.757, isilIletkenlik=102.4, katilik=0.4, manyetik="Paramanyetik", bulunsenYil=1807, adinHikayesi="Arapça 'al-qalyah' kelimesinden türemiştir.", fiziksel="Gümüşümsü, çok yumuşak bir metal. Hava ile temas edince çabucak kararır.", kimyasal="Yükseltgenme: +1. Bileşikleri: KOH (potasyum hidroksit), KCl (potasyum klorür), KNO₃ (potasyum nitrat), KMnO₄ (potasyum permanganat). Suda violent tepkimeye girer. Alkali metal grubundadır. Bitki besininde kritik rol oynar.", kullanim="Gübre (potasyum), sabun, cam, pil, tuz"),
            ElementData(atomNo=20, semIol="Ca", adi="Kalsiyum", kutle=40.078, grup=2, periyot=4, metal=true, valans=listOf(2), elektron="[Ar] 4s2", durum="Kati", erime=842.0, kaynama=1484.0, yogunluk=1.55, ozelIsi=0.647, isilIletkenlik=201.0, katilik=1.5, manyetik="Paramanyetik", bulunsenYil=1808, adinHikayesi="Latince 'calx' kelimesinden türemiştir.", fiziksel="Gümüşümsü, yumuşak bir metal. Kesildiğinde beyaz yüzey görünür.", kimyasal="Yükseltgenme: +2. Bileşikleri: CaCO₃ (kireç taşı), Ca(OH)₂ (sönmemiş kireç), CaO (kalsiyum oksit), CaSO₄ (jips). Suda hafifçe tepkimeye girer. Asitlerle CO₂ açığa çıkarır. Kemik ve diş yapısında kalsiyum fosfat bulunur.", kullanim="Kemik, diş, kireç, çimento, gıda takviyesi"),
            ElementData(atomNo=21, semIol="Sc", adi="Skandiyum", kutle=44.956, grup=3, periyot=4, metal=true, valans=listOf(3), elektron="[Ar] 3d1 4s2", durum="Kati", fiziksel="Gümüşümsü, nadir bir geçiş metali. Yumuşak ve sünek.", kimyasal="Yükseltgenme: +3. Bileşikleri: Sc₂O₃, ScCl₃, ScF₃. Alkali çözeltilerde çözünmez. +3 iyonu kararlıdır. Fluoresan lambalarda kullanılır.", kullanim="Uzay alaşımları, spor ekipmanları, lamba"),
            ElementData(atomNo=22, semIol="Ti", adi="Titan", kutle=47.867, grup=4, periyot=4, metal=true, valans=listOf(2,3,4), elektron="[Ar] 3d2 4s2", durum="Kati", fiziksel="Gümüşümsü, hafif ve çok güçlü bir metal. Korozyona dayanıklıdır.", kimyasal="Yükseltgenme: +2, +3, +4. Bileşikleri: TiO₂ (titanyum dioksit/beyaz boya), TiCl₄, TiC (titanyum karbür), TiN (titanyum nitrür). +4 en kararlıdır. TiO₂ UV ışınlarını emer. Korozyona karşı çok dayanıklıdır.", kullanim="Uzay, protez, diş implantı, pigment, spor ekipmanı"),
            ElementData(atomNo=23, semIol="V", adi="Vanadyum", kutle=50.942, grup=5, periyot=4, metal=true, valans=listOf(2,3,4,5), elektron="[Ar] 3d3 4s2", durum="Kati", fiziksel="Gümüşümsü-gri, sert bir geçiş metali.", kimyasal="Yükseltgenme: +2, +3, +4, +5. Bileşikleri: V₂O₅ (vanadyum pentoksit/katalizör), VO₂, VCl₄. Vanadat iyonları çözeltide renk değiştirir. Çelik üretiminde katalizör olarak kullanılır.", kullanim="Çelik alaşımı, katalizör, vanadyum pilleri"),
            ElementData(atomNo=24, semIol="Cr", adi="Krom", kutle=51.996, grup=6, periyot=4, metal=true, valans=listOf(2,3,6), elektron="[Ar] 3d5 4s1", durum="Kati", fiziksel="Gümüşümsü, parlak, çok sert bir metal.", kimyasal="Yükseltgenme: +2, +3, +6. Bileşikleri: Cr₂O₃ (yeşil boya), K₂Cr₂O₇ (potasyum dikromat), CrO₃ (krom trioksit), CrO₄²⁻ (kromat). +6 çok güçlü oksitleyicidir. Krom kaplama çok dayanıklıdır.", kullanim="Krom kaplama, paslanmaz çelik, boya, deri tabaklama"),
            ElementData(atomNo=25, semIol="Mn", adi="Mangan", kutle=54.938, grup=7, periyot=4, metal=true, valans=listOf(2,3,4,6,7), elektron="[Ar] 3d5 4s2", durum="Kati", fiziksel="Gümüşümsü, kırılgan bir metal. Hava kararıncaya kadar oksitlenir.", kimyasal="Yükseltgenme: +2, +4, +7. Bileşikleri: MnO₂ (manganez dioksit), KMnO₄ (potasyum permanganat), MnCl₂. KMnO₄ çok güçlü oksitleyicidir (mor çözelti). +2 en kararlı yükseltgenme basamağıdır.", kullanim="Çelik alaşımı, pil, boya, cam"),
            ElementData(atomNo=26, semIol="Fe", adi="Demir", kutle=55.845, grup=8, periyot=4, metal=true, valans=listOf(2,3), elektron="[Ar] 3d6 4s2", durum="Kati", fiziksel="Gümüşümsü, manyetik, sünek bir metal. Paslanmaya eğilimlidir.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: Fe₂O₃ (pas/rust), FeCl₃ (demir(III) klorür), FeSO₄ (demir(II) sülfat), Fe₃O₄ (manyetik demir oksit). +2 ve +3 arası geçiş kolaydır. HCl'de çözünerek H₂ açığa çıkarır. Manyetik özellik gösterir.", kullanim="Çelik, inşaat, makine, araç, mutfak eşyası"),
            ElementData(atomNo=27, semIol="Co", adi="Kobalt", kutle=58.933, grup=9, periyot=4, metal=true, valans=listOf(2,3), elektron="[Ar] 3d7 4s2", durum="Kati", fiziksel="Gümüşümsü-mavi, sert ve manyetik bir metal.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: CoCl₂ (pembe-mavi renk değiştirir), CoO, Co₃O₄, Co(OH)₂. +2 iyonu pembe, +3 iyonu mavi renklidir. Manyetik alaşımlarda kullanılır. B12 vitamininin merkezinde bulunur.", kullanim="Mıknatıs, pil, boya (mavi), katalizör, alaşımlar"),
            ElementData(atomNo=28, semIol="Ni", adi="Nikel", kutle=58.693, grup=10, periyot=4, metal=true, valans=listOf(2,3), elektron="[Ar] 3d8 4s2", durum="Kati", fiziksel="Gümüşümsü, manyetik, sünek bir metal. Korozyona dayanıklıdır.", kimyasal="Yükseltgenme: +2. Bileşikleri: NiCl₂, NiSO₄, Ni(OH)₂, NiO. +2 en kararlı basamağıdır. Asitlere karşı dayanıklıdır. Manyetiktir. Nikel-kadmiyum ve nikel-metallihidrit pillerde kullanılır.", kullanim="Paslanmaz çelik, pil, madeni para, katalizör"),
            ElementData(atomNo=29, semIol="Cu", adi="Bakır", kutle=63.546, grup=11, periyot=4, metal=true, valans=listOf(1,2), elektron="[Ar] 3d10 4s1", durum="Kati", fiziksel="Kırmızı-turuncu, parlak, sünek bir metal. İyi iletken.", kimyasal="Yükseltgenme: +1, +2. Bileşikleri: CuSO₄ (mavi çözelti), CuO, CuCl₂, Cu₂O, Cu(OH)₂ (mavi çökeltiler). +2 iyonu mavi renklidir. Amonyakta çözünerek koyu mavi kompleks oluşturur (Schweizer reaktifi). İyi iletken.", kullanim="Elektrik kablosu, su borusu, madeni para, takı"),
            ElementData(atomNo=30, semIol="Zn", adi="Çinko", kutle=65.409, grup=12, periyot=4, metal=true, valans=listOf(2), elektron="[Ar] 3d10 4s2", durum="Kati", fiziksel="Gümüşümsü-mavi, kırılgan bir metal.", kimyasal="Yükseltgenme: +2. Bileşikleri: ZnO (çinko oksit), ZnCl₂, ZnSO₄, ZnS (flüoresan). +2 tek kararlı basamağıdır. Amfoter yapıdadır, hem asit hem bazda çözünür. Galvanizleme korozyon koruması sağlar.", kullanim="Galvanizleme, pil, alaşımlar (pirinç), pigment"),
            ElementData(atomNo=31, semIol="Ga", adi="Galya", kutle=69.723, grup=13, periyot=4, metal=true, valans=listOf(3), elektron="[Ar] 3d10 4s2 4p1", durum="Kati", fiziksel="Gümüşümsü, elde eriyen bir metal (29.76°C). Bükülebilir.", kimyasal="Yükseltgenme: +3. Bileşikleri: Ga₂O₃, GaAs (galyum arsenid), GaN (galyum nitrür). Elde eriyen metal (29.76°C). Yarı iletken bileşikleri LED ve Güneş panelinde kullanılır.", kullanim="LED, yarı iletken, termometre, Güneş paneli"),
            ElementData(atomNo=32, semIol="Ge", adi="Germanyum", kutle=72.64, grup=14, periyot=4, metal=true, valans=listOf(4), elektron="[Ar] 3d10 4s2 4p2", durum="Kati", fiziksel="Gri-beyaz, parlak, kırılgan bir yarı metal.", kimyasal="Yükseltgenme: +4. Bileşikleri: GeO₂, GeCl₄, GeS₂. Yarı iletken özellik gösterir. +4 en kararlı basamağıdır. Fiberoptik ve infrared optikte kullanılır.", kullanim="Fiberoptik, transistör, Güneş paneli, katalizör"),
            ElementData(atomNo=33, semIol="As", adi="Arsenik", kutle=74.922, grup=15, periyot=4, metal=true, valans=listOf(3,5), elektron="[Ar] 3d10 4s2 4p3", durum="Kati", fiziksel="Gri, kırılgan, zehirli bir yarı metal.", kimyasal="Yükseltgenme: +3, +5. Bileşikleri: As₂O₃ (arsenik trioksit/zehir), As₂O₅, AsH₃ (arsin gazı). Çok zehirlidir. +3 en kararlı basamağıdır. Yarı iletken bileşiklerde (GaAs) kullanılır.", kullanim="Böcek ilacı, yarı iletken, alaşım, arsenik bileşikleri"),
            ElementData(atomNo=34, semIol="Se", adi="Selenyum", kutle=78.63, grup=16, periyot=4, metal=false, valans=listOf(2,4,6), elektron="[Ar] 3d10 4s2 4p4", durum="Kati", fiziksel="Gri-siyah, parlak bir yarı metal. Fotoğrafçılıkta kullanılır.", kimyasal="Yükseltgenme: -2, +4, +6. Bileşikleri: H₂SeO₃ (selenöz asit), H₂SeO₄ (selenik asit), SeO₂. Antioksidan olarak kullanılır. +4 en yaygın basamağıdır. Güneş paneli malzemesidir.", kullanim="Fotoğrafçılık, Güneş paneli, cam, antioksidan"),
            ElementData(atomNo=35, semIol="Br", adi="Brom", kutle=79.904, grup=17, periyot=4, metal=false, valans=listOf(1,3,5,7,-1), elektron="[Ar] 3d10 4s2 4p5", durum="Sivi", fiziksel="Koyu kırmızı-kahverengi, buhar oluşturan sıvı. Keskin kokulu.", kimyasal="Yükseltgenme: -1, +1, +3, +5. Bileşikleri: HBr (hidrobromik asit), NaBr, KBr, Br₂O. Sıvı halojendir. Suda çözündüğünde HBrO oluşturur. Alev geciktirici bileşikler üretir.", kullanim="Alev geciktirici, ilaç, boya, fotoğrafçılık"),
            ElementData(atomNo=36, semIol="Kr", adi="Kripton", kutle=83.798, grup=18, periyot=4, metal=false, valans=listOf(0,2), elektron="[Ar] 3d10 4s2 4p6", durum="Gaz", fiziksel="Renksiz, kokusuz bir gaz. Soğuk ışık yayar.", kimyasal="Yükseltgenme: 0, +2. Bileşikleri: KrF₂ (kriypton diflorür, çok kararsız). Soy gazdır. Çok nadir bileşikler oluşturur. Fluoresan lambalarda kullanılır.", kullanim="Fluoresan lamba, lazer, fotoğrafçılık"),
            ElementData(atomNo=37, semIol="Rb", adi="Rubidyum", kutle=85.468, grup=1, periyot=5, metal=true, valans=listOf(1), elektron="[Kr] 5s1", durum="Kati", erime=39.31, kaynama=688.0, yogunluk=1.532, manyetik="Paramanyetik", bulunsenYil=1861, fiziksel="Gümüşümsü, çok yumuşak metal. Hava ile temas edince tutuşur.", kimyasal="Yükseltgenme: +1. Bileşikleri: RbCl, RbOH, Rb₂SO₄. Suda violent tepkimeye girer. Son derece reaktiftir. Atom saatlerinde freq referansı olarak kullanılır.", kullanim="Atom saati, fotohücre, tıbbi görüntüleme"),
            ElementData(atomNo=38, semIol="Sr", adi="Stronsiyum", kutle=87.62, grup=2, periyot=5, metal=true, valans=listOf(2), elektron="[Kr] 5s2", durum="Kati", erime=777.0, kaynama=1382.0, yogunluk=2.64, manyetik="Paramanyetik", bulunsenYil=1790, fiziksel="Gümüşümsü, yumuşak bir metal. Alevle yakıldığında parlak kırmızı renk verir.", kimyasal="Yükseltgenme: +2. Bileşikleri: SrCl₂, Sr(OH)₂, SrCO₃, Sr(NO₃)₂. Suda çözünür. Alevle yakıldığında kırmızı renk verir. Havai fişek üretiminde kullanılır.", kullanim="Havai fişek (kırmızı), cam (televizyon tüpü), diş macunu"),
            ElementData(atomNo=39, semIol="Y", adi="İtriyum", kutle=88.906, grup=3, periyot=5, metal=true, valans=listOf(3), elektron="[Kr] 4d1 5s2", durum="Kati", erime=1526.0, kaynama=3345.0, yogunluk=4.469, manyetik="Paramanyetik", bulunsenYil=1794, fiziksel="Gümüşümsü, nadir bir geçiş metali. Yumuşak ve sünek.", kimyasal="Yükseltgenme: +3. Bileşikleri: Y₂O₃, YCl₃, YF₃, YBa₂Cu₃O₇ (süper iletken). +3 en kararlı basamağıdır. LED'lerde yeşil floresan pigment olarak kullanılır.", kullanim="LED (yeşil), lazer, süper iletken, katalizör"),
            ElementData(atomNo=40, semIol="Zr", adi="Zirkonyum", kutle=91.224, grup=4, periyot=5, metal=true, valans=listOf(4), elektron="[Kr] 4d2 5s2", durum="Kati", erime=1855.0, kaynama=4409.0, yogunluk=6.506, manyetik="Paramanyetik", bulunsenYil=1789, fiziksel="Gümüşümsü, parlak ve çok güçlü bir metal. Korozyona dayanıklıdır.", kimyasal="Yükseltgenme: +4. Bileşikleri: ZrO₂ (zirkonya), ZrCl₄, ZrSiO₄ (zirkon). +4 en kararlıdır. Nükleer reaktörlerde yakıt kılıfı olarak kullanılır. Korozyona karşı çok dayanıklıdır.", kullanim="Seramik, nükleer reaktör, protez, reçete"),
            ElementData(atomNo=41, semIol="Nb", adi="Niyobyum", kutle=92.906, grup=5, periyot=5, metal=true, valans=listOf(2,3,5), elektron="[Kr] 4d4 5s1", durum="Kati", erime=2477.0, kaynama=4744.0, yogunluk=8.57, manyetik="Süper İletken", bulunsenYil=1801, fiziksel="Gümüşümsü, geçiş metali. Süper iletken özellik gösterir.", kimyasal="Yükseltgenme: +5. Bileşikleri: Nb₂O₅, NbCl₅, NbF₅. +5 en kararlı basamağıdır. Süper iletken alaşımlarda (Nb₃Sn) kullanılır. Çelik alaşımlarını güçlendirir.", kullanim="Süper iletken mıknatıs, çelik alaşımı, jet motoru"),
            ElementData(atomNo=42, semIol="Mo", adi="Molibden", kutle=95.94, grup=6, periyot=5, metal=true, valans=listOf(2,3,4,5,6), elektron="[Kr] 4d5 5s1", durum="Kati", erime=2623.0, kaynama=4639.0, yogunluk=10.22, manyetik="Paramanyetik", bulunsenYil=1781, fiziksel="Gümüşümsü, çok sert bir metal. Yüksek erime noktasına sahiptir.", kimyasal="Yükseltgenme: +6. Bileşikleri: MoS₂ (molibden sülfür/kuru yağ), MoO₃, Na₂MoO₄. +6 en kararlıdır. Enzimlerde kofaktör olarak bulunur. Çelik alaşımlarını sertleştirir.", kullanim="Çelik alaşımı, katalizör, lamba filament, biyoteknoloji"),
            ElementData(atomNo=43, semIol="Tc", adi="Teknesyum", kutle=98.0, grup=7, periyot=5, metal=true, valans=listOf(4,7), elektron="[Kr] 4d5 5s2", durum="Kati", erime=2157.0, kaynama=4265.0, yogunluk=11.5, manyetik="Paramanyetik", bulunsenYil=1937, fiziksel="Gümüşümsü, radyoaktif bir metal. İlk yapay element.", kimyasal="Yükseltgenme: +7. Bileşikleri: TcO₄⁻ (perklorat), Tc₂O₇. Radyoaktiftir. +7 en kararlı basamağıdır. Tıbbi görüntülemede (Tc-99m) kullanılır.", kullanim="Tıbbi görüntüleme (sintigrafi), radyofarmasi"),
            ElementData(atomNo=44, semIol="Ru", adi="Rutenyum", kutle=101.07, grup=8, periyot=5, metal=true, valans=listOf(2,3,4,6,8), elektron="[Kr] 4d7 5s1", durum="Kati", erime=2334.0, kaynama=4150.0, yogunluk=12.37, manyetik="Paramanyetik", bulunsenYil=1844, fiziksel="Gümüşümsü, geçiş metali. Sert ve kırılgandır.", kimyasal="Yükseltgenme: +3, +4. Bileşikleri: RuO₂, RuCl₃, RuO₄ (tetroksit). +3 ve +4 kararlıdır. Asitlere karşı dayanıklıdır. Katalizör olarak etkilidir.", kullanim="Katalizör, elektronik, boya, takı"),
            ElementData(atomNo=45, semIol="Rh", adi="Rodyum", kutle=102.906, grup=9, periyot=5, metal=true, valans=listOf(2,3,4,6), elektron="[Kr] 4d8 5s1", durum="Kati", erime=1964.0, kaynama=3695.0, yogunluk=12.41, manyetik="Paramanyetik", bulunsenYil=1803, fiziksel="Gümüşümsü, parlak ve çok değerli bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: RhCl₃, Rh₂O₃, Rh(NO₃)₃. +3 en kararlı basamağıdır. Otomobil egzoz katalizöründe kritik rol oynar. Nadir ve değerli.", kullanim="Otomobil katalizörü, ayna kaplama, takı, katalizör"),
            ElementData(atomNo=46, semIol="Pd", adi="Paladyum", kutle=106.42, grup=10, periyot=5, metal=true, valans=listOf(2,4), elektron="[Kr] 4d10", durum="Kati", erime=1554.9, kaynama=2963.0, yogunluk=12.023, manyetik="Paramanyetik", bulunsenYil=1803, fiziksel="Gümüşümsü, sünek ve parlak bir metal.", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: PdCl₂, PdO, Pd(NO₃)₂. +2 ve +4 kararlıdır. Hidrojeni emebilen tek metaldir (1000x hacminde). Katalizör olarak çok etkilidir.", kullanim="Katalizör, takı, diş hekimliği, hidrojen depolama"),
            ElementData(atomNo=47, semIol="Ag", adi="Gümüş", kutle=107.868, grup=11, periyot=5, metal=true, valans=listOf(1), elektron="[Kr] 4d10 5s1", durum="Kati", erime=961.78, kaynama=2162.0, yogunluk=10.49, ozelIsi=0.235, isilIletkenlik=429.0, katilik=2.5, manyetik="Diamanyetik", bulunsenYil=0, adinHikayesi="Latince 'argentum' kelimesinden türemiştir.", fiziksel="Gümüş renginde, çok parlak ve sünek bir metal. En iyi iletken.", kimyasal="Yükseltgenme: +1. Bileşikleri: AgNO₃, AgCl (beyaz çökeltili), Ag₂O, Ag₂S (siyah). +1 en kararlıdır. Işıkla kararır (fotoğrafçılık temeli). Antibakteriyel özellik gösterir.", kullanim="Takı, fotoğraf, elektronik, antibakteriyel, madeni para"),
            ElementData(atomNo=48, semIol="Cd", adi="Kadmiyum", kutle=112.411, grup=12, periyot=5, metal=true, valans=listOf(2), elektron="[Kr] 4d10 5s2", durum="Kati", erime=321.07, kaynama=767.0, yogunluk=8.69, manyetik="Diamanyetik", bulunsenYil=1817, fiziksel="Gümüşümsü, yumuşak ve zehirli bir metal.", kimyasal="Yükseltgenme: +2. Bileşikleri: CdO, CdCl₂, CdS (sarı boya), CdSe. +2 tek kararlı basamağıdır. Zehirlidir. Nikel-kadmiyum pillerde kullanılır.", kullanim="Akü (Cd-Ni), kaplama, pigment, nükleer reaktör"),
            ElementData(atomNo=49, semIol="In", adi="İndiyum", kutle=114.818, grup=13, periyot=5, metal=true, valans=listOf(3), elektron="[Kr] 4d10 5s2 5p1", durum="Kati", erime=156.6, kaynama=2072.0, yogunluk=7.31, manyetik="Diamanyetik", bulunsenYil=1863, fiziksel="Gümüşümsü, çok yumuşak ve sünek bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: In₂O₃ (ITO), InCl₃, InSb. +3 en kararlıdır. İndiyum kalay oksit (ITO) dokunmatik ekranlarda kullanılır.", kullanim="LCD ekran (ITO), lehim, alaşım"),
            ElementData(atomNo=50, semIol="Sn", adi="Kalay", kutle=118.71, grup=14, periyot=5, metal=true, valans=listOf(2,4), elektron="[Kr] 4d10 5s2 5p2", durum="Kati", erime=231.93, kaynama=2602.0, yogunluk=7.287, manyetik="Diamanyetik", bulunsenYil=0, fiziksel="Gümüşümsü, iki allotropu olan metal (beyaz ve gri kalay).", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: SnO₂, SnCl₂, SnCl₄, SnS₂. +4 en kararlıdır. +2 indirgeyici özellik gösterir. Kalay klorür (SnCl₂) fotoğrafçılıkta kullanılır.", kullanim="Konserve kutusu, lehim, bronz, cam"),
            ElementData(atomNo=51, semIol="Sb", adi="Antimon", kutle=121.76, grup=15, periyot=5, metal=true, valans=listOf(3,5), elektron="[Kr] 4d10 5s2 5p3", durum="Kati", erime=630.63, kaynama=1587.0, yogunluk=6.685, manyetik="Diamanyetik", bulunsenYil=0, fiziksel="Gümüşümsü, kırılgan bir yarı metal.", kimyasal="Yükseltgenme: +3, +5. Bileşikleri: Sb₂O₃ (antimon trioksit), Sb₂O₅, SbCl₃. Alev geciktirici bileşikler oluşturur. +3 en kararlı basamağıdır.", kullanim="Alev geciktirici, akü, mühimmat, yarı iletken"),
            ElementData(atomNo=52, semIol="Te", adi="Tellür", kutle=127.6, grup=16, periyot=5, metal=true, valans=listOf(2,4,6), elektron="[Kr] 4d10 5s2 5p4", durum="Kati", erime=449.51, kaynama=988.0, yogunluk=6.232, manyetik="Diamanyetik", bulunsenYil=1783, fiziksel="Gümüşümsü, kırılgan bir yarı metal. Fotoğrafçılıkta kullanılır.", kimyasal="Yükseltgenme: -2, +4, +6. Bileşikleri: TeO₂, H₂TeO₃, TeCl₄. +4 en kararlıdır. Yarı iletken özellik gösterir. Termoelektrik cihazlarda kullanılır.", kullanim="Güneş paneli (CdTe), termoelektrik, kauçuk"),
            ElementData(atomNo=53, semIol="I", adi="İyot", kutle=126.904, grup=17, periyot=5, metal=false, valans=listOf(1,3,5,7,-1), elektron="[Kr] 4d10 5s2 5p5", durum="Kati", erime=113.7, kaynama=184.3, yogunluk=4.93, ozelIsi=0.214, isilIletkenlik=0.449, katilik=0.0, manyetik="Diamanyetik", bulunsenYil=1811, adinHikayesi="Yunancada 'iodes' kelimesinden türemiştir.", fiziksel="Mor buhar oluşturan, kırılgan katı bir ametal.", kimyasal="Yükseltgenme: -1, +1, +5, +7. Bileşikleri: KI (potasyum iyodür), I₂, HIO₃ (iyodik asit), AgI (gümüş iyodür). -1 en kararlıdır. Nişasta ile mavi renk oluşturur. Tiroid hormonu yapımında gereklidir.", kullanim="Dezenfektan (iyot), guatr tedavisi, fotoğrafçılık"),
            ElementData(atomNo=54, semIol="Xe", adi="Ksenon", kutle=131.293, grup=18, periyot=5, metal=false, valans=listOf(0,2,4,6), elektron="[Kr] 4d10 5s2 5p6", durum="Gaz", erime=-111.8, kaynama=-108.1, yogunluk=0.0059, manyetik="Diamanyetik", bulunsenYil=1898, fiziksel="Renksiz, kokusuz bir gaz. Yüksek basınçta renkli ışık yayar.", kimyasal="Yükseltgenme: +2, +4, +6, +8. Bileşikleri: XeF₂, XeF₄, XeF₆, XeO₃. Soy gazdır ama bazı bileşikler oluşturur. +2 ve +4 en kararlıdır. Işık kaynaklarında kullanılır.", kullanim="Işık kaynakları, lazer, anestezi, uzay itici (ion motor)"),
            ElementData(atomNo=55, semIol="Cs", adi="Sezyum", kutle=132.905, grup=1, periyot=6, metal=true, valans=listOf(1), elektron="[Xe] 6s1", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1861, fiziksel="Altın sarısı, çok yumuşak metal. En düşük erime noktasına sahip metal.", kimyasal="Yükseltgenme: +1. Bileşikleri: CsCl, CsOH, Cs₂CO₃. En düşük erime noktasına sahip metal. Son derece reaktiftir, suyla patlama yapar. Atom saatlerinde freq standardı.", kullanim="Atom saati (cesiyum), fotoelektrik hücre, tıbbi"),
            ElementData(atomNo=56, semIol="Ba", adi="Baryum", kutle=137.327, grup=2, periyot=6, metal=true, valans=listOf(2), elektron="[Xe] 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1808, fiziksel="Gümüşümsü, yumuşak bir metal. Alevle yakıldığında yeşil renk verir.", kimyasal="Yükseltgenme: +2. Bileşikleri: BaCl₂, BaSO₄ (baryum sülfat), Ba(OH)₂, BaCO₃. +2 tek kararlı basamağıdır. BaSO₄ radyolojide kontrast madde olarak kullanılır. Zehirlidir.", kullanim="X-ray kontrast maddesi, pyroteknik, greler, cam"),
            ElementData(atomNo=57, semIol="La", adi="Lantan", kutle=138.905, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 5d1 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1839, fiziksel="Gümüşümsü, yumuşak bir metal. Hava kararıncaya kadar oksitlenir.", kimyasal="Yükseltgenme: +3. Bileşikleri: La₂O₃, LaCl₃, LaF₃, LaB₆. +3 en kararlıdır. Kamera merceklerinde yüksek kırılma indeksli cam olarak kullanılır.", kullanim="Katalizör, kamera merceği, hidrid batarya"),
            ElementData(atomNo=58, semIol="Ce", adi="Seryum", kutle=140.116, grup=3, periyot=6, metal=true, valans=listOf(3,4), elektron="[Xe] 4f1 5d1 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1803, fiziksel="Gümüşümsü, yumuşak bir metal. Kolayca oksitlenir.", kimyasal="Yükseltgenme: +3, +4. Bileşikleri: CeO₂ (seryum dioksit), CeCl₃, Ce(SO₄)₂. +3 ve +4 arasında geçiş yapabilir. En bol lantanit. Cam parlatma ve katalizör olarak kullanılır.", kullanim="Katalizör, cam parlatma, benzin katkısı, çakmak"),
            ElementData(atomNo=59, semIol="Pr", adi="Praseodim", kutle=140.908, grup=3, periyot=6, metal=true, valans=listOf(3,4), elektron="[Xe] 4f3 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1885, fiziksel="Gümüşümsü-yeşil, yumuşak bir metal.", kimyasal="Yükseltgenme: +3, +4. Bileşikleri: Pr₂O₃, PrCl₃, Pr₆O₁₁. +3 ve +4 kararlıdır. NdFeB mıknatıslarında katı çözelti olarak kullanılır.", kullanim="Mıknatıs (NdFeB), cam, seramik, metal halide lamba"),
            ElementData(atomNo=60, semIol="Nd", adi="Neodim", kutle=144.242, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f4 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1885, fiziksel="Gümüşümsü, yumuşak bir metal. Hava kararıncaya kadar oksitlenir.", kimyasal="Yükseltgenme: +3. Bileşikleri: Nd₂O₃, NdCl₃, NdFeB (mıknatıs). +3 en kararlıdır. Güçlü kalıcı mıknatıslar üretir. Lazer ve metal halide lambalarda kullanılır.", kullanim="NdFeB mıknatıs, lazer, cam, metal halide lamba"),
            ElementData(atomNo=61, semIol="Pm", adi="Prometyum", kutle=145.0, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f5 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1945, fiziksel="Gümüşümsü, radyoaktif bir metal. Doğada çok az bulunur.", kimyasal="Yükseltgenme: +3. Bileşikleri: PmCl₃, Pm₂O₃. Radyoaktiftir. Doğada çok nadir bulunur. +3 tek kararlı basamağıdır.", kullanim="Tıbbi görüntüleme, nükleer pil, boyama"),
            ElementData(atomNo=62, semIol="Sm", adi="Samaryum", kutle=150.36, grup=3, periyot=6, metal=true, valans=listOf(2,3), elektron="[Xe] 4f6 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1879, fiziksel="Gümüşümsü, yumuşak bir metal.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: Sm₂O₃, SmCl₃, SmCo₅ (mıknatıs). +3 en kararlıdır. SmCo mıknatısları manyetik uygulamalarda kullanılır.", kullanim="Mıknatıs (SmCo), katalizör, nükleer reaktör"),
            ElementData(atomNo=63, semIol="Eu", adi="Europyum", kutle=151.964, grup=3, periyot=6, metal=true, valans=listOf(2,3), elektron="[Xe] 4f7 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1901, fiziksel="Gümüşümsü, en aktif lantanitlerden biri.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: Eu₂O₃, EuCl₃, EuSO₄. +2 ve +3 arasında geçiş yapabilir. Kızılötesi floresan ve LED'lerde kullanılır.", kullanim="Floresan lamba (kırmızı), Euro banknotlarda güvenlik"),
            ElementData(atomNo=64, semIol="Gd", adi="Gadolinyum", kutle=157.25, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f7 5d1 6s2", durum="Kati", manyetik="Feromanyetik", bulunsenYil=1880, fiziksel="Gümüşümsü, manyetik bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Gd₂O₃, GdCl₃, Gd-DTPA (MRI kontrastı). +3 en kararlıdır. Fermanyetik özellik gösterir. MRI'de kontrast madde olarak kullanılır.", kullanim="MRI kontrast maddesi, mıknatıs, nükleer reaktör"),
            ElementData(atomNo=65, semIol="Tb", adi="Terbiyum", kutle=158.925, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f9 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1843, fiziksel="Gümüşümsü, nadir bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Tb₄O₇, TbCl₃, Tb₂(SO₄)₃. +3 en kararlıdır. Yeşil floresan üretir. LED ve floresan lambalarda kullanılır.", kullanim="Floresan lamba (yeşil), manyetik alaşım, LED"),
            ElementData(atomNo=66, semIol="Dy", adi="Disprozyum", kutle=162.5, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f10 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1886, fiziksel="Gümüşümsü, parlak bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Dy₂O₃, DyCl₃, DyF₃. +3 en kararlıdır. Yüksek manyetik moment gösterir. NdFeB mıknatıslarında katı çözelti.", kullanim="Mıknatıs (NdFeB katkısı), lazer, nükleer reaktör"),
            ElementData(atomNo=67, semIol="Ho", adi="Holmiyum", kutle=164.93, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f11 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1878, fiziksel="Gümüşümsü, yumuşak bir metal. En yüksek manyetik momente sahiptir.", kimyasal="Yükseltgenme: +3. Bileşikleri: Ho₂O₃, HoCl₃. +3 tek kararlı basamağıdır. En yüksek manyetik momente sahiptir. Lazer ve manyetik uygulamalarda kullanılır.", kullanim="Mıknatıs, lazer, manyetik depolama"),
            ElementData(atomNo=68, semIol="Er", adi="Erbiyum", kutle=167.259, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f12 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1843, fiziksel="Gümüşümsü, yumuşak bir metal. Pembe-kırmızı floresans gösterir.", kimyasal="Yükseltgenme: +3. Bileşikleri: Er₂O₃, ErCl₃, Er₂O₃ (pembe). +3 en kararlıdır. Fiber optik amplifikatörlerde (EDFA) kullanılır.", kullanim="Fiber optik amplifikatör, lazer, metal halide lamba"),
            ElementData(atomNo=69, semIol="Tm", adi="Tulyum", kutle=168.934, grup=3, periyot=6, metal=true, valans=listOf(2,3), elektron="[Xe] 4f13 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1879, fiziksel="Gümüşümsü, çok nadir bir metal.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: Tm₂O₃, TmCl₃. +3 en kararlıdır. En nadir lantanitlerden biri. X-ray kaynaklarında kullanılır.", kullanim="X-ray cihazı, taşınabilir röntgen, süper iletken"),
            ElementData(atomNo=70, semIol="Yb", adi="İtterbiyum", kutle=173.04, grup=3, periyot=6, metal=true, valans=listOf(2,3), elektron="[Xe] 4f14 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1878, fiziksel="Gümüşümsü, yumuşak bir metal.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: Yb₂O₃, YbCl₃, YbF₃. +3 en kararlıdır. Süper iletken alaşımlar ve yüksek güçlü lazerlerde kullanılır.", kullanim="Lazer, süper iletken, çelik alaşımı"),
            ElementData(atomNo=71, semIol="Lu", adi="Lutesyum", kutle=174.967, grup=3, periyot=6, metal=true, valans=listOf(3), elektron="[Xe] 4f14 5d1 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1907, fiziksel="Gümüşümsü, çok sert ve yoğun bir lantanit.", kimyasal="Yükseltgenme: +3. Bileşikleri: Lu₂O₃, LuCl₃, Lu₂O₃. +3 tek kararlı basamağıdır. En pahalı lantanit. PET taramada (Lu-176) kullanılır.", kullanim="PET tarama (Lu-176), katalizör, nükleer tıp"),
            ElementData(atomNo=72, semIol="Hf", adi="Hafniyum", kutle=178.49, grup=4, periyot=6, metal=true, valans=listOf(4), elektron="[Xe] 4f14 5d2 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1923, fiziksel="Gümüşümsü, sert ve korozyona dayanıklı bir metal.", kimyasal="Yükseltgenme: +4. Bileşikleri: HfO₂ (hafniyum dioksit), HfCl₄, HfC. +4 en kararlıdır. Nükleer reaktörlerde nötron emici olarak kullanılır. Korozyona çok dayanıklıdır.", kullanim="Nükleer reaktör, jet motoru, reçete, filamente"),
            ElementData(atomNo=73, semIol="Ta", adi="Tantal", kutle=180.948, grup=5, periyot=6, metal=true, valans=listOf(5), elektron="[Xe] 4f14 5d3 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1802, fiziksel="Gümüşümsü, çok sert ve korozyona dayanıklı bir metal.", kimyasal="Yükseltgenme: +5. Bileşikleri: Ta₂O₅, TaCl₅, TaC. +5 en kararlıdır. Asitlere karşı tamamen dayanıklıdır. Cerrahi implantlarda ve kapasitörlerde kullanılır.", kullanim="Cerrahi alet, kapasitör, jet motoru, reçete"),
            ElementData(atomNo=74, semIol="W", adi="Tungsten", kutle=183.84, grup=6, periyot=6, metal=true, valans=listOf(2,3,4,5,6), elektron="[Xe] 4f14 5d4 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1783, fiziksel="Koyu gri, çok sert metal. Tüm elementlerin en yüksek erime noktasına sahiptir.", kimyasal="Yükseltgenme: +6. Bileşikleri: WO₃, WC (tungsten karbür), WF₆, WCl₆. +6 en kararlıdır. Tüm elementlerin en yüksek erime noktasına sahiptir (3422°C). Ampul flamentlerinde kullanılır.", kullanim="Ampul flament, kesici alet, zırh, askeri mühimmat"),
            ElementData(atomNo=75, semIol="Re", adi="Renyum", kutle=186.207, grup=7, periyot=6, metal=true, valans=listOf(2,4,6,7), elektron="[Xe] 4f14 5d5 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1925, fiziksel="Gümüşümsü, çok sert ve yoğun bir metal.", kimyasal="Yükseltgenme: +7. Bileşikleri: Re₂O₇, ReCl₅, ReF₆. +7 en kararlıdır. Jet motoru alaşımlarında ve katalizörlerde kullanılır.", kullanim="Jet motoru, katalizör, reçete, termokupl"),
            ElementData(atomNo=76, semIol="Os", adi="Osmiyum", kutle=190.23, grup=8, periyot=6, metal=true, valans=listOf(2,3,4,6,8), elektron="[Xe] 4f14 5d6 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1803, fiziksel="Mavi-gri, çok yoğun ve sert bir metal. Tüm elementlerin en yoğunudur.", kimyasal="Yükseltgenme: +4, +8. Bileşikleri: OsO₄ (tetroksit, çok zehirli), OsCl₄. +8 en kararlı basamağıdır. Tüm elementlerin en yoğunudur (22.59 g/cm³). Dolmakalem uçlarında kullanılır.", kullanim="Dolmakalem ucu, katalizör, elektrik kontak"),
            ElementData(atomNo=77, semIol="Ir", adi="İridyum", kutle=192.217, grup=9, periyot=6, metal=true, valans=listOf(2,3,4,6), elektron="[Xe] 4f14 5d7 6s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1803, fiziksel="Gümüşümsü, çok sert ve yoğun bir metal.", kimyasal="Yükseltgenme: +3, +4. Bileşikleri: IrCl₃, IrO₂, IrF₆. +3 ve +4 kararlıdır. Korozyona karşı en dayanıklı metaldir. Bujılarda ve dolmakalem uçlarında kullanılır.", kullanim="Dolmakalem ucu, bujı, katalizör, meteoroloji"),
            ElementData(atomNo=78, semIol="Pt", adi="Platin", kutle=195.084, grup=10, periyot=6, metal=true, valans=listOf(2,4), elektron="[Xe] 4f14 5d9 6s1", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1735, fiziksel="Gümüşümsü, parlak ve sünek bir metal. Korozyona karşı çok dayanıklıdır.", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: PtCl₂, PtCl₄, cisplatin (Pt(NH₃)₂Cl₂), PtO₂. +2 ve +4 kararlıdır. Katalizör olarak çok etkilidir. Cisplatin kanser ilacıdır.", kullanim="Takı, katalizör, ilaç (cisplatin), laboratuvar"),
            ElementData(atomNo=79, semIol="Au", adi="Altın", kutle=196.967, grup=11, periyot=6, metal=true, valans=listOf(1,3), elektron="[Xe] 4f14 5d10 6s1", durum="Kati", manyetik="Diamanyetik", bulunsenYil=0, fiziksel="Sarı, parlak ve sünek bir metal. Korozyona karşı tamamen dayanıklıdır.", kimyasal="Yükseltgenme: +1, +3. Bileşikleri: AuCl₃, AuCl (altın(I) klorür), Au₂O₃. Aqua regia'da çözünür. +1 ve +3 kararlıdır. Korozyona karşı tamamen dayanıklıdır.", kullanim="Takı, elektronik, diş hekimliği, para"),
            ElementData(atomNo=80, semIol="Hg", adi="Civa", kutle=200.59, grup=12, periyot=6, metal=true, valans=listOf(1,2), elektron="[Xe] 4f14 5d10 6s2", durum="Sivi", manyetik="Diamanyetik", bulunsenYil=0, fiziksel="Gümüşümsü, oda sıcaklığında sıvı metal. Zehirlidir.", kimyasal="Yükseltgenme: +1, +2. Bileşikleri: HgCl₂ (cıva(II) klorür), HgO, Hg₂Cl₂ (kalomel), HgS (cinnabar). +2 en kararlıdır. Oda sıcaklığında sıvı metaldir. Cıva buharı çok zehirlidir.", kullanim="Termometre, barometre, diş amalgamı, floresan lamba"),
            ElementData(atomNo=81, semIol="Tl", adi="Talyum", kutle=204.383, grup=13, periyot=6, metal=true, valans=listOf(1,3), elektron="[Xe] 4f14 5d10 6s2 6p1", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1861, fiziksel="Gümüşümsü, çok yumuşak ve zehirli bir metal.", kimyasal="Yükseltgenme: +1, +3. Bileşikleri: TlCl, Tl₂SO₄, Tl₂O₃. +1 en kararlıdır. Çok zehirlidir. Eski optik ve fotoğrafçılık bileşiklerinde kullanılmıştır.", kullanim="Fotoğrafçılık (eski), optik, bileşik cam"),
            ElementData(atomNo=82, semIol="Pb", adi="Kurşun", kutle=207.2, grup=14, periyot=6, metal=true, valans=listOf(2,4), elektron="[Xe] 4f14 5d10 6s2 6p2", durum="Kati", manyetik="Diamanyetik", bulunsenYil=0, fiziksel="Koyu gri, çok yumuşak ve yoğun bir metal.", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: PbO (litharge), PbO₂, PbSO₄ (akü), PbCl₂, PbS (galena). +2 en kararlıdır. Kurşun asit akülerde elektrot olarak kullanılır.", kullanim="Akü, kurşun kalem, radyasyon kalkanı, alaşım"),
            ElementData(atomNo=83, semIol="Bi", adi="Bizmut", kutle=208.98, grup=15, periyot=6, metal=true, valans=listOf(3,5), elektron="[Xe] 4f14 5d10 6s2 6p3", durum="Kati", manyetik="Diamanyetik", bulunsenYil=0, fiziksel="Pembe-gümüşümsü, kırılgan bir metal. Manyetik olmayan.", kimyasal="Yükseltgenme: +3, +5. Bileşikleri: Bi₂O₃, BiCl₃, Bi(NO₃)₃, Bi₂(SO₄)₃. +3 en kararlıdır. Radyoaktif olmayan en ağır metaldir. İlaç ve kozmetikte kullanılır.", kullanim="İlaç (Pepto-Bismol), kozmetik, alaşım"),
            ElementData(atomNo=84, semIol="Po", adi="Polonyum", kutle=209.0, grup=16, periyot=6, metal=true, valans=listOf(2,4,6), elektron="[Xe] 4f14 5d10 6s2 6p4", durum="Kati", manyetik="Diamanyetik", bulunsenYil=1898, fiziksel="Gümüşümsü, radyoaktif ve çok zehirli bir metal.", kimyasal="Yükseltgenme: +4, +6. Bileşikleri: PoO₂, PoCl₄. Radyoaktiftir. +4 en kararlıdır. Polonyum-210 çok zehirli ve radyoaktiftir.", kullanim="Radyoaktif kaynak, anti-statik cihaz (eski)"),
            ElementData(atomNo=85, semIol="At", adi="Astatin", kutle=210.0, grup=17, periyot=6, metal=false, valans=listOf(1,3,5,7), elektron="[Xe] 4f14 5d10 6s2 6p5", durum="Kati", manyetik="Diamanyetik", bulunsenYil=1940, fiziksel="Gümüşümsü veya siyah, radyoaktif bir ametal.", kimyasal="Yükseltgenme: -1, +1, +5. Bileşikleri: AtI, HAt. Radyoaktiftir. -1 en kararlıdır. Doğada çok nadir bulunur.", kullanim="Radyoaktif tedavi (araştırma aşamasında)"),
            ElementData(atomNo=86, semIol="Rn", adi="Radon", kutle=222.0, grup=18, periyot=6, metal=false, valans=listOf(0,2), elektron="[Xe] 4f14 5d10 6s2 6p6", durum="Gaz", manyetik="Diamanyetik", bulunsenYil=1900, fiziksel="Renksiz, kokusuz, radyoaktif bir gaz.", kimyasal="Yükseltgenme: 0, +2. Bileşikleri: RnF₂ (çok kararsız). Radyoaktif gaz. Kanserojendir. Radon-222 uranyum bozunmasında oluşur.", kullanim="Radyoterapi (radon iğnesi), jeolojik araştırma"),
            ElementData(atomNo=87, semIol="Fr", adi="Fransiyum", kutle=223.0, grup=1, periyot=7, metal=true, valans=listOf(1), elektron="[Rn] 7s1", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1939, fiziksel="Gümüşümsü, radyoaktif bir metal. Doğada çok nadir bulunur.", kimyasal="Yükseltgenme: +1. Bileşikleri: FrCl, FrOH. +1 tek kararlı basamağıdır. Son derece reaktiftir ve radyoaktiftir. Doğada çok nadir bulunur (toplam ~20-30g).", kullanim="Araştırma (radyoaktif)"),
            ElementData(atomNo=88, semIol="Ra", adi="Radyum", kutle=226.0, grup=2, periyot=7, metal=true, valans=listOf(2), elektron="[Rn] 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1898, fiziksel="Gümüşümsü, radyoaktif bir metal. Karanlıkta mavi-yeşil ışık yayar.", kimyasal="Yükseltgenme: +2. Bileşikleri: RaCl₂, Ra(OH)₂, RaSO₄. +2 tek kararlıdır. Radyoaktiftir. Karanlıkta mavi-yeşil ışık yayar.", kullanim="Radyoaktif kaynak (eski), tıbbi araştırma"),
            ElementData(atomNo=89, semIol="Ac", adi="Aktinyum", kutle=227.0, grup=3, periyot=7, metal=true, valans=listOf(3), elektron="[Rn] 6d1 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1899, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Ac₂O₃, AcCl₃. +3 tek kararlıdır. Radyoaktiftir. Nötron kaynağı olarak kullanılır.", kullanim="Nötron kaynağı (Ac-227), araştırma"),
            ElementData(atomNo=90, semIol="Th", adi="Toryum", kutle=232.038, grup=3, periyot=7, metal=true, valans=listOf(4), elektron="[Rn] 6d2 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1829, fiziksel="Gümüşümsü, radyoaktif bir metal. Yumuşak ve sünek.", kimyasal="Yükseltgenme: +4. Bileşikleri: ThO₂, ThCl₄, Th(NO₃)₄. +4 en kararlıdır. Nükleer yakıt potansiyeli yüksektir. Gaz maskesi filtrelerinde kullanılır.", kullanim="Nükleer yakıt (araştırma), gaz maskesi filtresi, katalizör"),
            ElementData(atomNo=91, semIol="Pa", adi="Protaktinyum", kutle=231.036, grup=3, periyot=7, metal=true, valans=listOf(5), elektron="[Rn] 5f2 6d1 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1913, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +4, +5. Bileşikleri: Pa₂O₅, PaCl₅. Radyoaktiftir. +5 en kararlıdır. Doğada çok nadir bulunur.", kullanim="Araştırma"),
            ElementData(atomNo=92, semIol="U", adi="Uranyum", kutle=238.029, grup=3, periyot=7, metal=true, valans=listOf(3,4,5,6), elektron="[Rn] 5f3 6d1 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1789, fiziksel="Gümüşümsü, yoğun ve radyoaktif bir metal.", kimyasal="Yükseltgenme: +3, +4, +5, +6. Bileşikleri: UF₆ (uranyum hekzaflüorür), UO₂, U₃O₈, uranyl sülfat. +6 (uranyl iyonu) ve +4 kararlıdır. Nükleer fisyon yapabilir. UF₆ zenginleştirme sürecinde kullanılır.", kullanim="Nükleer yakıt, zırh delici mermi, renklendirici (cam)"),
            ElementData(atomNo=93, semIol="Np", adi="Neptunyum", kutle=237.0, grup=3, periyot=7, metal=true, valans=listOf(3,4,5,6), elektron="[Rn] 5f4 6d1 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1940, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3, +4, +5, +6. Bileşikleri: NpO₂, NpCl₄, NpF₆. +5 en kararlıdır. İlk transuranyum elementtir. Neptunyum-237 nötron kaynağı olarak kullanılır.", kullanim="Nötron kaynağı (Np-237), araştırma"),
            ElementData(atomNo=94, semIol="Pu", adi="Plutonyum", kutle=244.0, grup=3, periyot=7, metal=true, valans=listOf(3,4,5,6), elektron="[Rn] 5f6 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1940, fiziksel="Gümüşümsü, radyoaktif ve toksik bir metal.", kimyasal="Yükseltgenme: +3, +4, +5, +6. Bileşikleri: PuO₂, PuCl₃, PuF₄, PuO₂(NO₃)₂. +4 en kararlıdır. Nükleer fisyon yapabilir. Nükleer silah ve yakıt üretiminde kullanılır.", kullanim="Nükleer silah, nükleer yakıt, uzay sondası (RTG)"),
            ElementData(atomNo=95, semIol="Am", adi="Amerikyum", kutle=243.0, grup=3, periyot=7, metal=true, valans=listOf(2,3,4,5,6), elektron="[Rn] 5f7 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1944, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3, +4, +5, +6. Bileşikleri: AmO₂, AmCl₃. +3 en kararlıdır. Duman dedektörlerinde (Am-241) kullanılır.", kullanim="Duman dedektörü (Am-241), araştırma"),
            ElementData(atomNo=96, semIol="Cm", adi="Kuriyum", kutle=247.0, grup=3, periyot=7, metal=true, valans=listOf(3), elektron="[Rn] 5f7 6d1 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1944, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Cm₂O₃, CmCl₃. +3 tek kararlıdır. Nötron kaynağı olarak kullanılır.", kullanim="Nötron kaynağı (Cm-244), araştırma"),
            ElementData(atomNo=97, semIol="Bk", adi="Berkelyum", kutle=247.0, grup=3, periyot=7, metal=true, valans=listOf(3,4), elektron="[Rn] 5f9 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1949, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3, +4. Bileşikleri: Bk₂O₃, BkCl₃. +3 en kararlıdır. Sentez yoluyla üretilir.", kullanim="Araştırma"),
            ElementData(atomNo=98, semIol="Cf", adi="Kaliforniyum", kutle=251.0, grup=3, periyot=7, metal=true, valans=listOf(2,3), elektron="[Rn] 5f10 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1950, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Cf₂O₃, CfCl₃. +3 tek kararlıdır. Güçlü nötron kaynağıdır (Cf-252). Kanser tedavisinde kullanılır.", kullanim="Nötron kaynağı (Cf-252), kanser tedavisi, mayın temizleme"),
            ElementData(atomNo=99, semIol="Es", adi="Einsteinium", kutle=252.0, grup=3, periyot=7, metal=true, valans=listOf(2,3), elektron="[Rn] 5f11 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1952, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Es₂O₃, EsCl₃. +3 tek kararlıdır. Sentez yoluyla üretilir.", kullanim="Araştırma"),
            ElementData(atomNo=100, semIol="Fm", adi="Fermiyum", kutle=257.0, grup=3, periyot=7, metal=true, valans=listOf(2,3), elektron="[Rn] 5f12 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1952, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: Fm₂O₃. +3 tek kararlıdır. Sentez yoluyla üretilir.", kullanim="Araştırma"),
            ElementData(atomNo=101, semIol="Md", adi="Mendelevyum", kutle=258.0, grup=3, periyot=7, metal=true, valans=listOf(2,3), elektron="[Rn] 5f13 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1955, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: MdCl₃. +3 tek kararlıdır. Sentez yoluyla üretilir.", kullanim="Araştırma"),
            ElementData(atomNo=102, semIol="No", adi="Nobelyum", kutle=259.0, grup=3, periyot=7, metal=true, valans=listOf(2,3), elektron="[Rn] 5f14 7s2", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1958, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +2, +3. Bileşikleri: NoCl₂, NoCl₃. +2 en kararlıdır. Sentez yoluyla üretilir.", kullanim="Araştırma"),
            ElementData(atomNo=103, semIol="Lr", adi="Lawrensiyum", kutle=266.0, grup=3, periyot=7, metal=true, valans=listOf(3), elektron="[Rn] 5f14 7s2 7p1", durum="Kati", manyetik="Paramanyetik", bulunsenYil=1961, fiziksel="Gümüşümsü, radyoaktif bir metal.", kimyasal="Yükseltgenme: +3. Bileşikleri: LrCl₃. +3 tek kararlıdır. Sentez yoluyla üretilir.", kullanim="Araştırma"),
            ElementData(atomNo=104, semIol="Rf", adi="Rutherfordium", kutle=267.0, grup=4, periyot=7, metal=true, valans=listOf(4), elektron="[Rn] 5f14 6d2 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1969, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +4. Bileşikleri: RfO₂ (teorik). Radyoaktiftir, çok kısa ömürlüdür. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=105, semIol="Db", adi="Dubniyum", kutle=268.0, grup=5, periyot=7, metal=true, valans=listOf(5), elektron="[Rn] 5f14 6d3 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1970, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +5. Bileşikleri: Db₂O₅ (teorik). Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=106, semIol="Sg", adi="Seaborgium", kutle=269.0, grup=6, periyot=7, metal=true, valans=listOf(6), elektron="[Rn] 5f14 6d4 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1974, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +6. Bileşikleri: SgO₃ (teorik). Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=107, semIol="Bh", adi="Bohrium", kutle=270.0, grup=7, periyot=7, metal=true, valans=listOf(7), elektron="[Rn] 5f14 6d5 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1981, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +7. Bileşikleri: BhO₄ (teorik). Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=108, semIol="Hs", adi="Hassium", kutle=277.0, grup=8, periyot=7, metal=true, valans=listOf(8), elektron="[Rn] 5f14 6d6 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1984, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +8. Bileşikleri: HsO₄ (teorik). Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=109, semIol="Mt", adi="Meitnerium", kutle=278.0, grup=9, periyot=7, metal=true, valans=listOf(3,6), elektron="[Rn] 5f14 6d7 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1982, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +3, +6. Bileşikleri: Bilinmiyor. Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=110, semIol="Ds", adi="Darmstadtium", kutle=281.0, grup=10, periyot=7, metal=true, valans=listOf(2,4), elektron="[Rn] 5f14 6d8 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1994, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: Bilinmiyor. Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=111, semIol="Rg", adi="Roentgenium", kutle=282.0, grup=11, periyot=7, metal=true, valans=listOf(1,3), elektron="[Rn] 5f14 6d9 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1994, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +1, +3. Bileşikleri: Bilinmiyor. Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=112, semIol="Cn", adi="Kopernikyum", kutle=285.0, grup=12, periyot=7, metal=true, valans=listOf(2), elektron="[Rn] 5f14 6d10 7s2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1996, fiziksel="Radyoaktif, çok kararlı olmayan bir metal.", kimyasal="Yükseltgenme: +2. Bileşikleri: Bilinmiyor. Radyoaktiftir. Geçiş metalidir.", kullanim="Araştırma"),
            ElementData(atomNo=113, semIol="Nh", adi="Nihonium", kutle=286.0, grup=13, periyot=7, metal=true, valans=listOf(1,3), elektron="[Rn] 5f14 6d10 7s2 7p1", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=2004, fiziksel="Radyoaktif, çok kararlı olmayan bir element.", kimyasal="Yükseltgenme: +1, +3. Bileşikleri: Bilinmiyor. Radyoaktiftir.", kullanim="Araştırma"),
            ElementData(atomNo=114, semIol="Fl", adi="Flerovium", kutle=289.0, grup=14, periyot=7, metal=true, valans=listOf(2,4), elektron="[Rn] 5f14 6d10 7s2 7p2", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=1999, fiziksel="Radyoaktif, çok kararlı olmayan bir element.", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: Bilinmiyor. Radyoaktiftir.", kullanim="Araştırma"),
            ElementData(atomNo=115, semIol="Mc", adi="Moscovium", kutle=290.0, grup=15, periyot=7, metal=true, valans=listOf(1,3), elektron="[Rn] 5f14 6d10 7s2 7p3", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=2004, fiziksel="Radyoaktif, çok kararlı olmayan bir element.", kimyasal="Yükseltgenme: +1, +3. Bileşikleri: Bilinmiyor. Radyoaktiftir.", kullanim="Araştırma"),
            ElementData(atomNo=116, semIol="Lv", adi="Livermorium", kutle=293.0, grup=16, periyot=7, metal=true, valans=listOf(2,4), elektron="[Rn] 5f14 6d10 7s2 7p4", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=2000, fiziksel="Radyoaktif, çok kararlı olmayan bir element.", kimyasal="Yükseltgenme: +2, +4. Bileşikleri: Bilinmiyor. Radyoaktiftir.", kullanim="Araştırma"),
            ElementData(atomNo=117, semIol="Ts", adi="Tennessine", kutle=294.0, grup=17, periyot=7, metal=false, valans=listOf(1,3,5,7), elektron="[Rn] 5f14 6d10 7s2 7p5", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=2010, fiziksel="Radyoaktif, çok kararlı olmayan bir element.", kimyasal="Yükseltgenme: -1, +1, +3, +5, +7. Bileşikleri: Bilinmiyor. Radyoaktiftir.", kullanim="Araştırma"),
            ElementData(atomNo=118, semIol="Og", adi="Oganesson", kutle=294.0, grup=18, periyot=7, metal=false, valans=listOf(0), elektron="[Rn] 5f14 6d10 7s2 7p6", durum="Kati", manyetik="Bilinmiyor", bulunsenYil=2006, fiziksel="Radyoaktif, çok kararlı olmayan bir element.", kimyasal="Yükseltgenme: 0, +2, +4. Bileşikleri: Bilinmiyor. Radyoaktiftir.", kullanim="Araştırma")
        )
        return raw.associateBy { it.semIol }.mapValues { (sym, el) ->
            el.copy(
                iyonlasmaEnerjisi = iyonlasmaEnerjileri[sym] ?: 0.0,
                elektronegatiflik = elektronegatiflikler[sym] ?: 0.0
            )
        }
    }

    fun elementBul(query: String): ElementData? {
        val q = query.trim()
        val ql = q.lowercase()
        if (ql.isEmpty()) return null
        // 1. Atom no ile tam e�le�me
        val no = q.toIntOrNull()
        if (no != null) return elementler.values.find { it.atomNo == no }
        // 2. Sembol ile tam e�le�me (b�y�k-k���k harf duyars�z)
        val symMatch = elementler.values.find { it.semIol.lowercase() == ql }
        if (symMatch != null) return symMatch
        // 3. Sembolle ba�layan elemanlar (k�smi e�le�me)
        val symStart = elementler.values.find { it.semIol.lowercase().startsWith(ql) }
        if (symStart != null) return symStart
        // 4. �simle e�le�me
        return elementler.values.find { it.adi.lowercase().contains(ql) }
    }

    /**
     * Formülü element sayaçlarına çevirir (büyük/küçük harfe duyarlı, iç içe
     * parantez destekler). Hatalı formülde null döner.
     * Örn: Ca(OH)2 → {Ca:1, O:2, H:2}
     */
    fun formulAyristir(formul: String): Map<String, Int>? {
        val f = formul.replace(" ", "")
        if (f.isEmpty()) return null
        val stack = ArrayDeque<MutableMap<String, Int>>()
        stack.addLast(mutableMapOf())
        var i = 0
        while (i < f.length) {
            val c = f[i]
            when {
                c == '(' -> {
                    stack.addLast(mutableMapOf())
                    i++
                }
                c == ')' -> {
                    if (stack.size <= 1) return null
                    i++
                    var j = i
                    while (j < f.length && f[j].isDigit()) j++
                    val carp = f.substring(i, j).toIntOrNull() ?: 1
                    if (carp <= 0 || carp > 1000) return null
                    val ust = stack.removeLast()
                    if (ust.isEmpty()) return null // boş parantez geçersizdir: "Na()", "()"
                    val alt = stack.last()
                    for ((s, n) in ust) alt[s] = (alt[s] ?: 0) + n * carp
                    i = j
                }
                c.isUpperCase() -> {
                    var j = i + 1
                    if (j < f.length && f[j].isLowerCase()) j++
                    val simge = f.substring(i, j)
                    var k = j
                    while (k < f.length && f[k].isDigit()) k++
                    val adet = f.substring(j, k).toIntOrNull() ?: 1
                    if (adet <= 0 || adet > 10000) return null
                    if (elementler[simge] == null) return null
                    val cur = stack.last()
                    cur[simge] = (cur[simge] ?: 0) + adet
                    i = k
                }
                else -> return null
            }
        }
        if (stack.size != 1) return null
        val sonuc = stack.first()
        if (sonuc.isEmpty()) return null
        return sonuc
    }

    fun molekulKutlesiHesapla(formul: String): Double? {
        val bilesenler = formulAyristir(formul) ?: return null
        var toplam = 0.0
        for ((s, n) in bilesenler) {
            val el = elementler[s] ?: return null
            toplam += el.kutle * n
        }
        return if (toplam > 0) toplam else null
    }

    fun phHesapla(deger: Double, tur: String): Map<String, Any> {
        if ((tur == "[H+]" || tur == "[OH-]") && !(deger > 0)) {
            return mapOf("hata" to "Derisim pozitif olmalı")
        }
        val (ph, poh, h, oh) = when (tur) {
            "pH" -> listOf(deger, 14 - deger, 10.0.pow(-deger), 10.0.pow(deger - 14))
            "pOH" -> listOf(14 - deger, deger, 10.0.pow(deger - 14), 10.0.pow(-deger))
            "[H+]" -> listOf(-log10(deger), 14 + log10(deger), deger, 1e-14 / deger)
            "[OH-]" -> listOf(14 + log10(deger), -log10(deger), 1e-14 / deger, deger)
            else -> return mapOf("hata" to "Gecersiz tur")
        }
        val tip = when {
            ph < 4 -> "Kuvvetli Asit"
            ph < 7 -> "Zayif Asit"
            ph == 7.0 -> "Notr"
            ph < 10 -> "Zayif Baz"
            else -> "Kuvvetli Baz"
        }
        return mapOf("pH" to ph, "pOH" to poh, "[H+]" to h, "[OH-]" to oh, "tur" to tip)
    }

    fun idealGaz(P: Double?, V: Double?, n: Double?, T: Double?, R: Double = 0.0821): Double? {
        return when {
            P == null && V != null && n != null && T != null -> n * R * T / V
            V == null && P != null && n != null && T != null -> n * R * T / P
            n == null && P != null && V != null && T != null -> P * V / (R * T)
            T == null && P != null && V != null && n != null -> P * V / (n * R)
            else -> null
        }
    }

    fun gazMolKutlesi(kutle: Double, V: Double, T: Double, P: Double, R: Double = 0.0821): Double? {
        if (P <= 0 || V <= 0 || T <= 0) return null
        val n = P * V / (R * T)
        if (n <= 0) return null
        return kutle / n
    }

    fun yogunluktanMolarite(yuzde: Double, d: Double, mK: Double): Double? {
        if (mK <= 0) return null
        return (yuzde * 10.0 * d) / mK
    }

    fun etkilesim(s1: String, s2: String, d1: Int?, d2: Int?): String {
        val el1 = elementler[s1] ?: return "Element bulunamadı: $s1"
        val el2 = elementler[s2] ?: return "Element bulunamadı: $s2"
        val v1 = d1 ?: el1.valans.first()
        val v2 = d2 ?: el2.valans.first()

        val bilinen = bilesikler.find { b ->
            val els = b.bilesenler.map { it.first }.toSet()
            els == setOf(s1, s2)
        }
        if (bilinen != null) return "${bilinen.adi} (${bilinen.formulu})"

        val metal = el1.metal
        val ametal = el2.metal
        return if (metal && !ametal) {
            val obeb = gcd(v1, kotlin.math.abs(v2))
            val k1 = kotlin.math.abs(v2) / obeb
            val k2 = v1 / obeb
            val f = "$s1${if (k1 == 1) "" else k1}$s2${if (k2 == 1) "" else k2}"
            "İyonik bileşik: $f (${el1.adi} ${roma(v1)}, ${el2.adi} ${roma(v2)})"
        } else if (!metal && metal) {
            val obeb = gcd(v2, kotlin.math.abs(v1))
            val k1 = kotlin.math.abs(v1) / obeb
            val k2 = v2 / obeb
            val f = "$s2${if (k1 == 1) "" else k1}$s1${if (k2 == 1) "" else k2}"
            "İyonik bileşik: $f"
        } else if (!metal && !metal) {
            val obeb = gcd(kotlin.math.abs(v1), kotlin.math.abs(v2))
            val k1 = kotlin.math.abs(v2) / obeb
            val k2 = kotlin.math.abs(v1) / obeb
            val f = "$s1${if (k1 == 1) "" else k1}$s2${if (k2 == 1) "" else k2}"
            "Kovalent bileşik: $f"
        } else {
            "Alaşım / Çözelti"
        }
    }

    fun reaksiyonDengele(reaktif: String, urun: String): String {
        val r = reaktif.replace(" ", "").replace("+", " + ").trim()
        val u = urun.replace(" ", "").replace("+", " + ").trim()
        val h = mapOf(
            ("H2+O2" to "H2O") to "2H2 + O2 -> 2H2O",
            ("Na+Cl2" to "NaCl") to "2Na + Cl2 -> 2NaCl",
            ("Ca+O2" to "CaO") to "2Ca + O2 -> 2CaO",
            ("H2+Cl2" to "HCl") to "H2 + Cl2 -> 2HCl",
            ("N2+H2" to "NH3") to "N2 + 3H2 -> 2NH3"
        )
        return h[Pair(r, u)] ?: h.entries.find { e ->
            r.split("+").map { it.trim() }.toSet() == e.key.first.split("+").map { it.trim() }.toSet() &&
                u.split("+").map { it.trim() }.toSet() == e.key.second.split("+").map { it.trim() }.toSet()
        }?.value ?: "Otomatik dengeleme desteklenmiyor: $r -> $u"
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    private fun roma(n: Int): String = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII").getOrElse(n - 1) { "$n" }

    val iyonlasmaEnerjileri = mapOf(
        "H" to 1312.0, "He" to 2372.3, "Li" to 520.2, "Be" to 899.5, "B" to 800.6,
        "C" to 1086.5, "N" to 1402.3, "O" to 1313.9, "F" to 1681.0, "Ne" to 2080.7,
        "Na" to 495.8, "Mg" to 737.7, "Al" to 577.5, "Si" to 786.5, "P" to 1011.8,
        "S" to 999.6, "Cl" to 1251.2, "Ar" to 1520.6, "K" to 418.8, "Ca" to 589.8,
        "Sc" to 631.0, "Ti" to 658.0, "V" to 650.0, "Cr" to 652.0, "Mn" to 717.0,
        "Fe" to 759.0, "Co" to 760.0, "Ni" to 737.0, "Cu" to 745.0, "Zn" to 906.0,
        "Ga" to 579.0, "Ge" to 762.0, "As" to 947.0, "Se" to 941.0, "Br" to 1139.9,
        "Kr" to 1350.8, "Rb" to 403.0, "Sr" to 549.5, "Y" to 600.0, "Zr" to 640.1,
        "Nb" to 652.1, "Mo" to 684.3, "Tc" to 702.0, "Ru" to 710.2, "Rh" to 719.7,
        "Pd" to 804.4, "Ag" to 731.0, "Cd" to 867.7, "In" to 558.3, "Sn" to 708.6,
        "Sb" to 834.0, "Te" to 869.3, "I" to 1008.4, "Xe" to 1170.4, "Cs" to 375.7,
        "Ba" to 502.9, "La" to 538.1, "Ce" to 534.4, "Pr" to 527.0, "Nd" to 533.1,
        "Pm" to 540.0, "Sm" to 544.5, "Eu" to 547.1, "Gd" to 593.4, "Tb" to 565.8,
        "Dy" to 573.0, "Ho" to 581.0, "Er" to 589.3, "Tm" to 596.7, "Yb" to 603.4,
        "Lu" to 523.5, "Hf" to 658.5, "Ta" to 761.0, "W" to 770.0, "Re" to 760.0,
        "Os" to 840.0, "Ir" to 880.0, "Pt" to 870.0, "Au" to 890.1, "Hg" to 1007.1,
        "Tl" to 589.4, "Pb" to 715.6, "Bi" to 703.0, "Po" to 812.1, "At" to 920.0,
        "Rn" to 1037.1, "Fr" to 380.0, "Ra" to 509.3, "Ac" to 499.0, "Th" to 587.0,
        "Pa" to 568.0, "U" to 597.6, "Np" to 604.5, "Pu" to 584.7, "Am" to 578.0,
        "Cm" to 581.0, "Bk" to 601.0, "Cf" to 608.0, "Es" to 619.0, "Fm" to 627.0,
        "Md" to 635.0, "No" to 642.0, "Lr" to 470.0
    )
    val elektronegatiflikler = mapOf(
        "H" to 2.20, "He" to 0.0, "Li" to 0.98, "Be" to 1.57, "B" to 2.04,
        "C" to 2.55, "N" to 3.04, "O" to 3.44, "F" to 3.98, "Ne" to 0.0,
        "Na" to 0.93, "Mg" to 1.31, "Al" to 1.61, "Si" to 1.90, "P" to 2.19,
        "S" to 2.58, "Cl" to 3.16, "Ar" to 0.0, "K" to 0.82, "Ca" to 1.00,
        "Sc" to 1.36, "Ti" to 1.54, "V" to 1.63, "Cr" to 1.66, "Mn" to 1.55,
        "Fe" to 1.83, "Co" to 1.88, "Ni" to 1.91, "Cu" to 1.90, "Zn" to 1.65,
        "Ga" to 1.81, "Ge" to 2.01, "As" to 2.18, "Se" to 2.55, "Br" to 2.96,
        "Kr" to 3.00, "Rb" to 0.82, "Sr" to 0.95, "Y" to 1.22, "Zr" to 1.33,
        "Nb" to 1.60, "Mo" to 2.16, "Tc" to 1.90, "Ru" to 2.20, "Rh" to 2.28,
        "Pd" to 2.20, "Ag" to 1.93, "Cd" to 1.69, "In" to 1.78, "Sn" to 1.96,
        "Sb" to 2.05, "Te" to 2.10, "I" to 2.66, "Xe" to 2.60, "Cs" to 0.79,
        "Ba" to 0.89, "La" to 1.10, "Ce" to 1.12, "Pr" to 1.13, "Nd" to 1.14,
        "Pm" to 1.13, "Sm" to 1.17, "Eu" to 1.20, "Gd" to 1.20, "Tb" to 1.10,
        "Dy" to 1.22, "Ho" to 1.23, "Er" to 1.24, "Tm" to 1.25, "Yb" to 1.10,
        "Lu" to 1.27, "Hf" to 1.30, "Ta" to 1.50, "W" to 2.36, "Re" to 1.90,
        "Os" to 2.20, "Ir" to 2.20, "Pt" to 2.28, "Au" to 2.54, "Hg" to 2.00,
        "Tl" to 1.62, "Pb" to 2.33, "Bi" to 2.02, "Po" to 2.00, "At" to 2.20,
        "Rn" to 2.20, "Fr" to 0.70, "Ra" to 0.90, "Ac" to 1.10, "Th" to 1.30,
        "Pa" to 1.50, "U" to 1.38, "Np" to 1.36, "Pu" to 1.28, "Am" to 1.30,
        "Cm" to 1.30, "Bk" to 1.30, "Cf" to 1.30, "Es" to 1.30, "Fm" to 1.30,
        "Md" to 1.30, "No" to 1.30, "Lr" to 1.30
    )
    val atomYaricapPm = mapOf(
        "H" to 31, "He" to 28,
        "Li" to 128, "Be" to 96, "B" to 84, "C" to 76, "N" to 71, "O" to 66, "F" to 57, "Ne" to 58,
        "Na" to 166, "Mg" to 141, "Al" to 121, "Si" to 111, "P" to 107, "S" to 105, "Cl" to 102, "Ar" to 106,
        "K" to 203, "Ca" to 176, "Sc" to 170, "Ti" to 160, "V" to 153, "Cr" to 139, "Mn" to 139, "Fe" to 132,
        "Co" to 126, "Ni" to 124, "Cu" to 132, "Zn" to 122, "Ga" to 122, "Ge" to 120, "As" to 119, "Se" to 120,
        "Br" to 120, "Kr" to 116,
        "Rb" to 220, "Sr" to 195, "Y" to 190, "Zr" to 175, "Nb" to 164, "Mo" to 154, "Tc" to 147, "Ru" to 146,
        "Rh" to 142, "Pd" to 139, "Ag" to 145, "Cd" to 144, "In" to 142, "Sn" to 139, "Sb" to 139, "Te" to 138,
        "I" to 139, "Xe" to 140,
        "Cs" to 244, "Ba" to 215, "La" to 207, "Hf" to 175, "Ta" to 170, "W" to 162, "Re" to 151, "Os" to 144,
        "Ir" to 141, "Pt" to 136, "Au" to 136, "Hg" to 132, "Tl" to 145, "Pb" to 146, "Bi" to 148, "Po" to 140,
        "At" to 150, "Rn" to 150,
        "Fr" to 260, "Ra" to 221, "Ac" to 215, "Rf" to 157, "Db" to 149, "Sg" to 143, "Bh" to 141, "Hs" to 134,
        "Mt" to 129, "Ds" to 128, "Rg" to 121, "Cn" to 122, "Nh" to 136, "Fl" to 143, "Mc" to 162, "Lv" to 175,
        "Ts" to 165, "Og" to 157,
        "Ce" to 204, "Pr" to 203, "Nd" to 201, "Pm" to 199, "Sm" to 198, "Eu" to 198, "Gd" to 196, "Tb" to 194,
        "Dy" to 192, "Ho" to 192, "Er" to 189, "Tm" to 190, "Yb" to 187, "Lu" to 187,
        "Th" to 206, "Pa" to 200, "U" to 196, "Np" to 190, "Pu" to 187, "Am" to 180, "Cm" to 169, "Bk" to 168,
        "Cf" to 168, "Es" to 165, "Fm" to 167, "Md" to 173, "No" to 176, "Lr" to 161
    )

    val molKutleleri = mapOf(
        "NaOH" to 40.0, "HCl" to 36.46, "H2SO4" to 98.08, "NH3" to 17.03,
        "NaCl" to 58.44, "KOH" to 56.11, "HNO3" to 63.01, "CH3COOH" to 60.05,
        "NaHCO3" to 84.01, "CaCO3" to 100.09, "Ca(OH)2" to 74.09,
        "H3PO4" to 98.00, "AgNO3" to 169.87, "KI" to 166.00,
        "KMnO4" to 158.04, "FeCl3" to 162.20, "CuSO4" to 159.61
    )
    val stoikiMol = mapOf(
        "H2SO4" to 98.08, "NaOH" to 40.0, "Na2SO4" to 142.04, "H2O" to 18.02,
        "HCl" to 36.46, "NaCl" to 58.44, "CaCO3" to 100.09, "CO2" to 44.01,
        "HNO3" to 63.01, "KOH" to 56.11, "H3PO4" to 98.00, "Na3PO4" to 163.94,
        "AgNO3" to 169.87, "AgCl" to 143.32, "CuSO4" to 159.61,
        "FeCl3" to 162.20, "KMnO4" to 158.04, "NH3" to 17.03
    )
}
