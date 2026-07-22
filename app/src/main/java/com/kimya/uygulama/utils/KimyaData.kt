package com.kimya.uygulama.utils

import kotlin.math.log10
import kotlin.math.pow

data class ElementData(
    val atomNo: Int, val semIol: String, val adi: String, val kutle: Double,
    val grup: Int, val periyot: Int, val metal: Boolean, val valans: List<Int>,
    val elektron: String, val kullanim: String, val ozellik: String, val durum: String,
    val iyonlasmaEnerjisi: Double = 0.0, val elektronegatiflik: Double = 0.0
) {
    val tur: String get() = when {
        durum == "Soy gaz" -> "Soy Gaz"
        !metal && durum != "Soy gaz" -> "Ametal"
        metal && grup in 3..12 -> "Gecis Metali"
        metal && grup == 1 -> "Alkali Metal"
        metal && grup == 2 -> "Toprak Alkali"
        metal && grup in 13..16 -> "Yari Metal"
        periyot >= 9 -> if (periyot == 9) "Lantanit" else "Aktinit"
        else -> "Metal"
    }
}

data class BilesikData(val adi: String, val formulu: String, val bilesenler: List<Pair<String, Int>>, val tur: String, val ozellik: String) {
    val molekulKutlesi: Double get() = KimyaData.elementler.let { els ->
        bilesenler.sumOf { (s, m) -> (els[s]?.kutle ?: 0.0) * m }
    }
}

data class AsitData(val adi: String, val formulu: String, val tur: String, val pH: String, val kullanim: String, val guvenlik: String)
data class BazData(val adi: String, val formulu: String, val tur: String, val pH: String, val kullanim: String, val guvenlik: String)

object KimyaData {
    val elementler: Map<String, ElementData> by lazy { elementlerOlustur() }

    val bilesikler: List<BilesikData> by lazy {
        listOf(
            BilesikData("Su", "H2O", listOf("H" to 2, "O" to 1), "Kovalent", "Sıvı, çözücü"),
            BilesikData("Karbondioksit", "CO2", listOf("C" to 1, "O" to 2), "Kovalent", "Gaz"),
            BilesikData("Sodyum Klorür", "NaCl", listOf("Na" to 1, "Cl" to 1), "İyonik", "Beyaz Kristal"),
            BilesikData("Kalsiyum Oksit", "CaO", listOf("Ca" to 1, "O" to 1), "İyonik", "Beyaz toz"),
            BilesikData("Demir(III) Oksit", "Fe2O3", listOf("Fe" to 2, "O" to 3), "İyonik", "Kırmızı kahve"),
            BilesikData("Amonyak", "NH3", listOf("N" to 1, "H" to 3), "Kovalent", "Gaz, keskin kokulu"),
            BilesikData("Metan", "CH4", listOf("C" to 1, "H" to 4), "Kovalent", "Gaz"),
            BilesikData("Sodyum Hidroksit", "NaOH", listOf("Na" to 1, "O" to 1, "H" to 1), "İyonik", "Beyaz Katı"),
            BilesikData("Sülfürik Asit", "H2SO4", listOf("H" to 2, "S" to 1, "O" to 4), "Asit", "Yağımsı sıvı"),
            BilesikData("Glikoz", "C6H12O6", listOf("C" to 6, "H" to 12, "O" to 6), "Kovalent", "Beyaz Kristal")
        )
    }

    val asitler: List<AsitData> = listOf(
        AsitData("Hidroklorik Asit", "HCl", "Kuvvetli Asit", "1-2", "Metal temizleme", "Oldukça aşındırıcı"),
        AsitData("Sülfürik Asit", "H2SO4", "Kuvvetli Asit", "1-2", "Pil üretimi, gübre", "Çok aşındırıcı"),
        AsitData("Nitrik Asit", "HNO3", "Kuvvetli Asit", "1-2", "Gübre, patlayıcı", "Yakıcı"),
        AsitData("Asetik Asit", "CH3COOH", "Zayıf Asit", "3-5", "Sirke, çözücü", "Tahriş edici"),
        AsitData("Fosforik Asit", "H3PO4", "Orta Kuvvetli Asit", "2-4", "Gübre, gıda", "Tahriş edici"),
        AsitData("Hidroflorik Asit", "HF", "Zayıf Asit", "3-5", "Cam aşındırma", "Çok zehirli")
    )

    val bazlar: List<BazData> = listOf(
        BazData("Sodyum Hidroksit", "NaOH", "Kuvvetli Baz", "13-14", "Sabun, kağıt", "Aşındırıcı"),
        BazData("Potasyum Hidroksit", "KOH", "Kuvvetli Baz", "13-14", "Sabun, pil", "Aşındırıcı"),
        BazData("Kalsiyum Hidroksit", "Ca(OH)2", "Orta Kuvvetli Baz", "12-13", "Kireç, inşaat", "Tahriş edici"),
        BazData("Amonyak", "NH3", "Zayıf Baz", "11-12", "Temizlik", "Keskin kokulu, zehirli"),
        BazData("Sodyum Bikarbonat", "NaHCO3", "Zayıf Baz", "8-9", "Kabartma tozu", "Zararsız"),
        BazData("Magnezyum Hidroksit", "Mg(OH)2", "Zayıf Baz", "10-11", "Antiasit", "Zararsız")
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
        "Soy Gaz" -> 0xFFFF69B4.toInt()
        "Ametal" -> 0xFF7FFF00.toInt()
        "Yari Metal" -> 0xFFDA70D6.toInt()
        "Alkali Metal" -> 0xFFFF6347.toInt()
        "Toprak Alkali" -> 0xFFFF8C00.toInt()
        "Lantanit" -> 0xFF32CD32.toInt()
        "Aktinit" -> 0xFFFFA500.toInt()
        else -> 0xFF00CED1.toInt()
    }

    private fun elementlerOlustur(): Map<String, ElementData> {
        val raw = listOf(
            ElementData(1,"H","Hidrojen",1.008,1,1,false,listOf(1,-1),"1s1","Yakit, amonyak","Renksiz gaz","Gaz"),
            ElementData(2,"He","Helyum",4.003,18,1,false,listOf(0),"1s2","Balon, sogutucu","Renksiz, soy gaz","Gaz"),
            ElementData(3,"Li","Lityum",6.941,1,2,true,listOf(1),"[He] 2s1","Pil, alasim","Yumusak, gumus","Kati"),
            ElementData(4,"Be","Berilyum",9.012,2,2,true,listOf(2),"[He] 2s2","Nukleer, alasim","Hafif, tok","Kati"),
            ElementData(5,"B","Bor",10.811,13,2,true,listOf(3),"[He] 2s2 2p1","Temizlik, cam","Yarimetal","Kati"),
            ElementData(6,"C","Karbon",12.011,14,2,false,listOf(4,2,-4),"[He] 2s2 2p2","Yakit, elmas","Esas element","Kati"),
            ElementData(7,"N","Azot",14.007,15,2,false,listOf(3,5,-3),"[He] 2s2 2p3","Gubre, patlayici","Renksiz gaz","Gaz"),
            ElementData(8,"O","Oksijen",15.999,16,2,false,listOf(2,-2,-1),"[He] 2s2 2p4","Solunum, yakit","Renksiz gaz","Gaz"),
            ElementData(9,"F","Flor",18.998,17,2,false,listOf(1,-1),"[He] 2s2 2p5","Dis macunu, Teflon","Sariyesil gaz","Gaz"),
            ElementData(10,"Ne","Neon",20.180,18,2,false,listOf(0),"[He] 2s2 2p6","Reklam tabelalari","Renksiz, soy gaz","Gaz"),
            ElementData(11,"Na","Sodyum",22.990,1,3,true,listOf(1),"[Ne] 3s1","Sofra tuzu","Yumusak, gumus","Kati"),
            ElementData(12,"Mg","Magnezyum",24.305,2,3,true,listOf(2),"[Ne] 3s2","Alasim, el feneri","Hafif, gumus","Kati"),
            ElementData(13,"Al","Aluminyum",26.982,13,3,true,listOf(3),"[Ne] 3s2 3p1","Folyo, konserve","Hafif, gumus","Kati"),
            ElementData(14,"Si","Silisyum",28.086,14,3,true,listOf(4),"[Ne] 3s2 3p2","Cip, elektronik","Yarimetal","Kati"),
            ElementData(15,"P","Fosfor",30.974,15,3,false,listOf(3,5,-3),"[Ne] 3s2 3p3","Gubre, kibrit","Beyaz/kirmizi","Kati"),
            ElementData(16,"S","Kukurt",32.065,16,3,false,listOf(2,4,6,-2),"[Ne] 3s2 3p4","Gubre, barut","Sari toz","Kati"),
            ElementData(17,"Cl","Klor",35.453,17,3,false,listOf(1,3,5,7,-1),"[Ne] 3s2 3p5","Dezenfektan","Sariyesil gaz","Gaz"),
            ElementData(18,"Ar","Argon",39.948,18,3,false,listOf(0),"[Ne] 3s2 3p6","Kaynak, ampul","Renksiz, soy gaz","Gaz"),
            ElementData(19,"K","Potasyum",39.098,1,4,true,listOf(1),"[Ar] 4s1","Gubre, sabun","Yumusak, gumus","Kati"),
            ElementData(20,"Ca","Kalsiyum",40.078,2,4,true,listOf(2),"[Ar] 4s2","Kemik, insaat","Gumus","Kati"),
            ElementData(21,"Sc","Skandiyum",44.956,3,4,true,listOf(3),"[Ar] 3d1 4s2","Uzay, spor","Gumus","Kati"),
            ElementData(22,"Ti","Titan",47.867,4,4,true,listOf(2,3,4),"[Ar] 3d2 4s2","Ucak, Implant","Gumus, guclu","Kati"),
            ElementData(23,"V","Vanadyum",50.942,5,4,true,listOf(2,3,4,5),"[Ar] 3d3 4s2","Celik","Gumus","Kati"),
            ElementData(24,"Cr","Krom",51.996,6,4,true,listOf(2,3,6),"[Ar] 3d5 4s1","Kaplama, alasim","Gumus","Kati"),
            ElementData(25,"Mn","Mangan",54.938,7,4,true,listOf(2,3,4,6,7),"[Ar] 3d5 4s2","Celik","Gri","Kati"),
            ElementData(26,"Fe","Demir",55.845,8,4,true,listOf(2,3),"[Ar] 3d6 4s2","Insaat, makine","Gri","Kati"),
            ElementData(27,"Co","Kobalt",58.933,9,4,true,listOf(2,3),"[Ar] 3d7 4s2","Pil, boya","Gri","Kati"),
            ElementData(28,"Ni","Nikel",58.693,10,4,true,listOf(2,3),"[Ar] 3d8 4s2","Para, alasim","Gumus","Kati"),
            ElementData(29,"Cu","Bakir",63.546,11,4,true,listOf(1,2),"[Ar] 3d10 4s1","Elekrik, tesisat","Kirmizi","Kati"),
            ElementData(30,"Zn","Cinko",65.409,12,4,true,listOf(2),"[Ar] 3d10 4s2","Galvaniz, pil","Gri","Kati"),
            ElementData(31,"Ga","Galya",69.723,13,4,true,listOf(3),"[Ar] 3d10 4s2 4p1","LED, termometre","Gumus","Kati"),
            ElementData(32,"Ge","Germanyum",72.64,14,4,true,listOf(4),"[Ar] 3d10 4s2 4p2","Yari iletken","Gri","Kati"),
            ElementData(33,"As","Arsenik",74.922,15,4,true,listOf(3,5),"[Ar] 3d10 4s2 4p3","Bocek ilaci","Gri","Kati"),
            ElementData(34,"Se","Selenyum",78.63,16,4,false,listOf(2,4,6),"[Ar] 3d10 4s2 4p4","Fotokolp","Gri","Kati"),
            ElementData(35,"Br","Brom",79.904,17,4,false,listOf(1,3,5,7,-1),"[Ar] 3d10 4s2 4p5","Ilac, fotograf","Kirmizi sivi","Sivi"),
            ElementData(36,"Kr","Kripton",83.798,18,4,false,listOf(0,2),"[Ar] 3d10 4s2 4p6","Lambda","Renksiz, soy gaz","Gaz"),
            ElementData(37,"Rb","Rubidyum",85.468,1,5,true,listOf(1),"[Kr] 5s1","Atom saati","Gumus","Kati"),
            ElementData(38,"Sr","Stronsiyum",87.62,2,5,true,listOf(2),"[Kr] 5s2","Havai fisek","Gumus","Kati"),
            ElementData(39,"Y","Itriyum",88.906,3,5,true,listOf(3),"[Kr] 4d1 5s2","LED, lazer","Gumus","Kati"),
            ElementData(40,"Zr","Zirkonyum",91.224,4,5,true,listOf(4),"[Kr] 4d2 5s2","Seramik","Gumus","Kati"),
            ElementData(41,"Nb","Niyobyum",92.906,5,5,true,listOf(2,3,5),"[Kr] 4d4 5s1","Sogutucu","Gri","Kati"),
            ElementData(42,"Mo","Molibden",95.94,6,5,true,listOf(2,3,4,5,6),"[Kr] 4d5 5s1","Celik","Gumus","Kati"),
            ElementData(43,"Tc","Teknesyum",98.0,7,5,true,listOf(4,7),"[Kr] 4d5 5s2","Radyofarmasi","Gumus","Kati"),
            ElementData(44,"Ru","Rutenyum",101.07,8,5,true,listOf(2,3,4,6,8),"[Kr] 4d7 5s1","Katalizor","Gumus","Kati"),
            ElementData(45,"Rh","Rodyum",102.906,9,5,true,listOf(2,3,4,6),"[Kr] 4d8 5s1","Katalizor","Gumus","Kati"),
            ElementData(46,"Pd","Paladyum",106.42,10,5,true,listOf(2,4),"[Kr] 4d10","Katalizor, mucevher","Gumus","Kati"),
            ElementData(47,"Ag","Gumus",107.868,11,5,true,listOf(1),"[Kr] 4d10 5s1","Mucevher, fotograf","Gumus","Kati"),
            ElementData(48,"Cd","Kadmiyum",112.411,12,5,true,listOf(2),"[Kr] 4d10 5s2","Pil, kaplama","Gri","Kati"),
            ElementData(49,"In","Indiyum",114.818,13,5,true,listOf(3),"[Kr] 4d10 5s2 5p1","LCD ekran","Gumus","Kati"),
            ElementData(50,"Sn","Kalay",118.71,14,5,true,listOf(2,4),"[Kr] 4d10 5s2 5p2","Konserve, lehim","Gumus","Kati"),
            ElementData(51,"Sb","Antimon",121.76,15,5,true,listOf(3,5),"[Kr] 4d10 5s2 5p3","Alev geciktirici","Gri","Kati"),
            ElementData(52,"Te","Tellur",127.6,16,5,true,listOf(2,4,6),"[Kr] 4d10 5s2 5p4","Gunes paneli","Gri","Kati"),
            ElementData(53,"I","Iyot",126.904,17,5,false,listOf(1,3,5,7,-1),"[Kr] 4d10 5s2 5p5","Dezenfektan","Mor-siya","Kati"),
            ElementData(54,"Xe","Ksenon",131.293,18,5,false,listOf(0,2,4,6),"[Kr] 4d10 5s2 5p6","Lambda, anestezi","Renksiz, soy gaz","Gaz"),
            ElementData(55,"Cs","Sezyum",132.905,1,6,true,listOf(1),"[Xe] 6s1","Atom saati","Altin","Kati"),
            ElementData(56,"Ba","Baryum",137.327,2,6,true,listOf(2),"[Xe] 6s2","Rontgen","Gumus","Kati"),
            ElementData(57,"La","Lantan",138.905,3,6,true,listOf(3),"[Xe] 5d1 6s2","Katalizor","Gumus","Kati"),
            ElementData(58,"Ce","Seryum",140.116,9,9,true,listOf(3,4),"[Xe] 4f1 5d1 6s2","Katalizor","Gumus","Kati"),
            ElementData(59,"Pr","Praseodim",140.908,9,9,true,listOf(3,4),"[Xe] 4f3 6s2","Miknatis","Sariyesil","Kati"),
            ElementData(60,"Nd","Neodim",144.242,9,9,true,listOf(3),"[Xe] 4f4 6s2","Miknatis","Gumus","Kati"),
            ElementData(61,"Pm","Prometyum",145.0,9,9,true,listOf(3),"[Xe] 4f5 6s2","Radyoaktif","Gumus","Kati"),
            ElementData(62,"Sm","Samaryum",150.36,9,9,true,listOf(2,3),"[Xe] 4f6 6s2","Miknatis","Gumus","Kati"),
            ElementData(63,"Eu","Europyum",151.964,9,9,true,listOf(2,3),"[Xe] 4f7 6s2","Lambda","Gumus","Kati"),
            ElementData(64,"Gd","Gadolinyum",157.25,9,9,true,listOf(3),"[Xe] 4f7 5d1 6s2","MRI","Gumus","Kati"),
            ElementData(65,"Tb","Terbiyum",158.925,9,9,true,listOf(3),"[Xe] 4f9 6s2","Ekran","Gumus","Kati"),
            ElementData(66,"Dy","Disprozyum",162.5,9,9,true,listOf(3),"[Xe] 4f10 6s2","Nukleer","Gumus","Kati"),
            ElementData(67,"Ho","Holmiyum",164.93,9,9,true,listOf(3),"[Xe] 4f11 6s2","Lazer","Gumus","Kati"),
            ElementData(68,"Er","Erbiyum",167.259,9,9,true,listOf(3),"[Xe] 4f12 6s2","Lazer","Gumus","Kati"),
            ElementData(69,"Tm","Tulyum",168.934,9,9,true,listOf(2,3),"[Xe] 4f13 6s2","Radyasyon","Gumus","Kati"),
            ElementData(70,"Yb","Itterbiyum",173.04,9,9,true,listOf(2,3),"[Xe] 4f14 6s2","Lazer","Gumus","Kati"),
            ElementData(71,"Lu","Lutesyum",174.967,9,9,true,listOf(3),"[Xe] 4f14 5d1 6s2","Katalizor","Gumus","Kati"),
            ElementData(72,"Hf","Hafniyum",178.49,4,6,true,listOf(4),"[Xe] 4f14 5d2 6s2","Nukleer","Gumus","Kati"),
            ElementData(73,"Ta","Tantal",180.948,5,6,true,listOf(5),"[Xe] 4f14 5d3 6s2","Kondansator","Gri","Kati"),
            ElementData(74,"W","Tungsten",183.84,6,6,true,listOf(2,3,4,5,6),"[Xe] 4f14 5d4 6s2","Ampul","Gri","Kati"),
            ElementData(75,"Re","Renyum",186.207,7,6,true,listOf(2,4,6,7),"[Xe] 4f14 5d5 6s2","Jet motoru","Gri","Kati"),
            ElementData(76,"Os","Osmiyum",190.23,8,6,true,listOf(2,3,4,6,8),"[Xe] 4f14 5d6 6s2","Dolum kalemi","Mavi","Kati"),
            ElementData(77,"Ir","Iridyum",192.217,9,6,true,listOf(2,3,4,6),"[Xe] 4f14 5d7 6s2","Buji","Gumus","Kati"),
            ElementData(78,"Pt","Platin",195.084,10,6,true,listOf(2,4),"[Xe] 4f14 5d9 6s1","Katalizor, mucevher","Gumus","Kati"),
            ElementData(79,"Au","Altin",196.967,11,6,true,listOf(1,3),"[Xe] 4f14 5d10 6s1","Mucevher, elektronik","Sari","Kati"),
            ElementData(80,"Hg","Civa",200.59,12,6,true,listOf(1,2),"[Xe] 4f14 5d10 6s2","Termometre","Gumus sivi","Sivi"),
            ElementData(81,"Tl","Talyum",204.383,13,6,true,listOf(1,3),"[Xe] 4f14 5d10 6s2 6p1","Zehir","Gri","Kati"),
            ElementData(82,"Pb","Kursun",207.2,14,6,true,listOf(2,4),"[Xe] 4f14 5d10 6s2 6p2","Aku, radyasyon","Gri","Kati"),
            ElementData(83,"Bi","Bizmut",208.98,15,6,true,listOf(3,5),"[Xe] 4f14 5d10 6s2 6p3","Kozmetik","Beyaz","Kati"),
            ElementData(84,"Po","Polonyum",209.0,16,6,true,listOf(2,4,6),"[Xe] 4f14 5d10 6s2 6p4","Radyoaktif","Gumus","Kati"),
            ElementData(85,"At","Astatin",210.0,17,6,false,listOf(1,3,5,7),"[Xe] 4f14 5d10 6s2 6p5","Radyoaktif","Siyah","Kati"),
            ElementData(86,"Rn","Radon",222.0,18,6,false,listOf(0,2),"[Xe] 4f14 5d10 6s2 6p6","Radyoterapi","Renksiz, soy gaz","Gaz"),
            ElementData(87,"Fr","Fransiyum",223.0,1,7,true,listOf(1),"[Rn] 7s1","Arastirma","Radyoaktif","Kati"),
            ElementData(88,"Ra","Radyum",226.0,2,7,true,listOf(2),"[Rn] 7s2","Radyoterapi","Radyoaktif","Kati"),
            ElementData(89,"Ac","Aktinyum",227.0,3,7,true,listOf(3),"[Rn] 6d1 7s2","Radyoaktif","Gumus","Kati"),
            ElementData(90,"Th","Toryum",232.038,10,10,true,listOf(4),"[Rn] 6d2 7s2","Nukleer","Gumus","Kati"),
            ElementData(91,"Pa","Protaktinyum",231.036,10,10,true,listOf(5),"[Rn] 5f2 6d1 7s2","Radyoaktif","Gumus","Kati"),
            ElementData(92,"U","Uranyum",238.029,10,10,true,listOf(3,4,5,6),"[Rn] 5f3 6d1 7s2","Nukleer, silah","Gumus","Kati"),
            ElementData(93,"Np","Neptunyum",237.0,10,10,true,listOf(3,4,5,6),"[Rn] 5f4 6d1 7s2","Nukleer","Gumus, radyoaktif","Kati"),
            ElementData(94,"Pu","Plutonyum",244.0,10,10,true,listOf(3,4,5,6),"[Rn] 5f6 7s2","Nukleer, silah","Gumus","Kati"),
            ElementData(95,"Am","Amerikyum",243.0,10,10,true,listOf(2,3,4,5,6),"[Rn] 5f7 7s2","Nukleer, dedektor","Gumus","Kati"),
            ElementData(96,"Cm","Kuriyum",247.0,10,10,true,listOf(3),"[Rn] 5f7 6d1 7s2","Nukleer","Gumus","Kati"),
            ElementData(97,"Bk","Berkelium",247.0,10,10,true,listOf(3,4),"[Rn] 5f9 7s2","Nukleer","Gumus","Kati"),
            ElementData(98,"Cf","Kaliforniyum",251.0,10,10,true,listOf(2,3),"[Rn] 5f10 7s2","Nukleer, dedektor","Gumus","Kati"),
            ElementData(99,"Es","Einsteinium",252.0,10,10,true,listOf(2,3),"[Rn] 5f11 7s2","Nukleer","Gumus","Kati"),
            ElementData(100,"Fm","Fermiyum",257.0,10,10,true,listOf(2,3),"[Rn] 5f12 7s2","Nukleer","Gumus","Kati"),
            ElementData(101,"Md","Mendelevyum",258.0,10,10,true,listOf(2,3),"[Rn] 5f13 7s2","Nukleer","Gumus","Kati"),
            ElementData(102,"No","Nobelyum",259.0,10,10,true,listOf(2,3),"[Rn] 5f14 7s2","Nukleer","Gumus","Kati"),
            ElementData(103,"Lr","Lavrensiyum",266.0,10,10,true,listOf(3),"[Rn] 5f14 7s2 7p1","Nukleer","Gumus","Kati"),
            ElementData(104,"Rf","Rutherfordium",267.0,4,7,true,listOf(4),"[Rn] 5f14 6d2 7s2","Arastirma","Gumus","Kati"),
            ElementData(105,"Db","Dubniyum",268.0,5,7,true,listOf(5),"[Rn] 5f14 6d3 7s2","Arastirma","Gumus","Kati"),
            ElementData(106,"Sg","Seaborgium",269.0,6,7,true,listOf(6),"[Rn] 5f14 6d4 7s2","Arastirma","Gumus","Kati"),
            ElementData(107,"Bh","Bohrium",270.0,7,7,true,listOf(7),"[Rn] 5f14 6d5 7s2","Arastirma","Gumus","Kati"),
            ElementData(108,"Hs","Hassium",277.0,8,7,true,listOf(8),"[Rn] 5f14 6d6 7s2","Arastirma","Gumus","Kati"),
            ElementData(109,"Mt","Meitnerium",278.0,9,7,true,listOf(3,6),"[Rn] 5f14 6d7 7s2","Arastirma","Gumus","Kati"),
            ElementData(110,"Ds","Darmstadtium",281.0,10,7,true,listOf(2,4),"[Rn] 5f14 6d8 7s2","Arastirma","Gumus","Kati"),
            ElementData(111,"Rg","Roentgenium",282.0,11,7,true,listOf(1,3),"[Rn] 5f14 6d9 7s2","Arastirma","Gumus","Kati"),
            ElementData(112,"Cn","Kopernikyum",285.0,12,7,true,listOf(2),"[Rn] 5f14 6d10 7s2","Arastirma","Gumus","Kati"),
            ElementData(113,"Nh","Nihonium",286.0,13,7,true,listOf(1,3),"[Rn] 5f14 6d10 7s2 7p1","Arastirma","Gumus","Kati"),
            ElementData(114,"Fl","Flerovium",289.0,14,7,true,listOf(2,4),"[Rn] 5f14 6d10 7s2 7p2","Arastirma","Gumus","Kati"),
            ElementData(115,"Mc","Moscovium",290.0,15,7,true,listOf(1,3),"[Rn] 5f14 6d10 7s2 7p3","Arastirma","Gumus","Kati"),
            ElementData(116,"Lv","Livermorium",293.0,16,7,true,listOf(2,4),"[Rn] 5f14 6d10 7s2 7p4","Arastirma","Gumus","Kati"),
            ElementData(117,"Ts","Tennessine",294.0,17,7,false,listOf(1,3,5,7),"[Rn] 5f14 6d10 7s2 7p5","Arastirma","Gumus","Kati"),
            ElementData(118,"Og","Oganesson",294.0,18,7,false,listOf(0),"[Rn] 5f14 6d10 7s2 7p6","Arastirma","Gumus","Kati")
        )
        return raw.associateBy { it.semIol }.mapValues { (sym, el) ->
            el.copy(
                iyonlasmaEnerjisi = iyonlasmaEnerjileri[sym] ?: 0.0,
                elektronegatiflik = elektronegatiflikler[sym] ?: 0.0
            )
        }
    }

    fun elementBul(query: String): ElementData? {
        val q = query.trim().lowercase()
        return elementler.values.find { it.semIol.lowercase() == q || it.adi.lowercase().contains(q) }
    }

    fun molekulKutlesiHesapla(formul: String): Double? {
        val regex = Regex("([A-Z][a-z]?)(\\d*)")
        var toplam = 0.0
        for (m in regex.findAll(formul)) {
            val s = m.groupValues[1]
            val cnt = m.groupValues[2].toIntOrNull() ?: 1
            val el = elementler[s] ?: return null
            toplam += el.kutle * cnt
        }
        return toplam
    }

    fun phHesapla(deger: Double, tur: String): Map<String, Any> {
        val (ph, poh, h, oh) = when (tur) {
            "pH" -> listOf(deger, 14 - deger, 10.0.pow(-deger), 10.0.pow(deger - 14))
            "pOH" -> listOf(14 - deger, deger, 10.0.pow(deger - 14), 10.0.pow(-deger))
            "[H+]" -> listOf(-log10(deger), 14 + log10(deger), deger, 1e-14 / deger)
            "[OH-]" -> listOf(14 + log10(deger), -log10(deger), 1e-14 / deger, deger)
            else -> return mapOf("hata" to "Geçersiz tür")
        }
        val tip = when {
            ph < 4 -> "Kuvvetli Asit"
            ph < 7 -> "Zayıf Asit"
            ph == 7.0 -> "Nötr"
            ph < 10 -> "Zayıf Baz"
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
            val f = if (k1 == 1 && k2 == 1) "$s1$s2" else "${s1}${k1}${s2}${k2}"
            "İyonik bileşik: $f (${el1.adi} ${roma(v1)}, ${el2.adi} ${roma(v2)})"
        } else if (!metal && metal) {
            val obeb = gcd(v2, kotlin.math.abs(v1))
            val k1 = kotlin.math.abs(v1) / obeb
            val k2 = v2 / obeb
            val f = if (k1 == 1 && k2 == 1) "$s2$s1" else "${s2}${k1}${s1}${k2}"
            "İyonik bileşik: $f"
        } else if (!metal && !metal) {
            val obeb = gcd(kotlin.math.abs(v1), kotlin.math.abs(v2))
            val k1 = kotlin.math.abs(v2) / obeb
            val k2 = kotlin.math.abs(v1) / obeb
            val f = if (k1 == 1 && k2 == 1) "$s1$s2" else "${s1}${k1}${s2}${k2}${k2}"
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
