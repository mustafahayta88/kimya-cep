package com.kimya.uygulama

import com.kimya.uygulama.utils.KimyaData
import org.junit.Assert.*
import org.junit.Test

/**
 * FAZ 1 — Formül ayrıştırıcı ve mol kütlesi.
 * Beklenen değerler IUPAC atom ağırlıklarından ELLE hesaplanmıştır
 * (H 1.008, C 12.011, N 14.007, O 15.999, Na 22.99, Cl 35.45,
 *  Ca 40.078, S 32.06, Al 26.982, Fe 55.845, Mg 24.305).
 * Tolerans 0.1: veri yuvarlamasını affeder, yapısal hatayı yakalar.
 */
class FormulParserTest {

    private fun kutle(formul: String): Double =
        KimyaData.molekulKutlesiHesapla(formul)
            ?: fail("Formül ayrıştırılamadı: $formul") as Double

    @Test fun su() = assertEquals(18.015, kutle("H2O"), 0.1)

    @Test fun karbondioksit() = assertEquals(44.009, kutle("CO2"), 0.1)

    @Test fun tuz() = assertEquals(58.44, kutle("NaCl"), 0.1)

    @Test fun parantezli_kalsiyum_hidroksit() = assertEquals(74.092, kutle("Ca(OH)2"), 0.1)

    @Test fun parantezli_aluminyum_sulfat() = assertEquals(342.132, kutle("Al2(SO4)3"), 0.1)

    @Test fun parantezli_amonyum_sulfat() = assertEquals(132.134, kutle("(NH4)2SO4"), 0.1)

    @Test fun benzen() = assertEquals(78.114, kutle("C6H6"), 0.1)

    @Test fun demir_oksit() = assertEquals(159.687, kutle("Fe2O3"), 0.1)

    @Test fun magnezyum_hidroksit() = assertEquals(58.319, kutle("Mg(OH)2"), 0.1)

    @Test fun demir_nitrat() = assertEquals(241.857, kutle("Fe(NO3)3"), 0.1)

    @Test fun bilesen_haritasi_parantez() {
        assertEquals(mapOf("Ca" to 1, "O" to 2, "H" to 2), KimyaData.formulAyristir("Ca(OH)2"))
    }

    @Test fun bilesen_haritasi_icice() {
        assertEquals(mapOf("Al" to 2, "S" to 3, "O" to 12), KimyaData.formulAyristir("Al2(SO4)3"))
    }

    @Test fun tek_element() {
        assertEquals(mapOf("H" to 1), KimyaData.formulAyristir("H"))
        assertEquals(mapOf("C" to 1), KimyaData.formulAyristir("C"))
    }

    // --- Parser saldırısı: hiçbiri çökmemeli, hepsi null dönmeli ---

    @Test fun bos_girdi() {
        assertNull(KimyaData.formulAyristir(""))
        assertNull(KimyaData.formulAyristir("   "))
    }

    @Test fun bilinmeyen_element() {
        assertNull(KimyaData.formulAyristir("XYZ"))
        assertNull(KimyaData.formulAyristir("H2X"))
    }

    @Test fun rakamla_baslama() = assertNull(KimyaData.formulAyristir("123"))

    @Test fun dengesiz_parantez() {
        assertNull(KimyaData.formulAyristir("NaCl)"))
        assertNull(KimyaData.formulAyristir("(NaCl"))
        assertNull(KimyaData.formulAyristir("Ca(OH)2))"))
    }

    @Test fun bos_parantez() {
        assertNull(KimyaData.formulAyristir("()"))
        assertNull(KimyaData.formulAyristir("Na()"))
    }

    @Test fun sifir_ve_negatif_adet() {
        assertNull(KimyaData.formulAyristir("Na0"))
        assertNull(KimyaData.formulAyristir("H-2"))
    }

    @Test fun gecersiz_karakter() {
        assertNull(KimyaData.formulAyristir("H2O·5H2O")) // kristal suyu noktası desteklenmiyor
        assertNull(KimyaData.formulAyristir("Na+Cl-"))
    }

    @Test fun kutle_gecersizde_null() {
        assertNull(KimyaData.molekulKutlesiHesapla(""))
        assertNull(KimyaData.molekulKutlesiHesapla("XYZ"))
        assertNull(KimyaData.molekulKutlesiHesapla("(NaCl"))
    }
}
