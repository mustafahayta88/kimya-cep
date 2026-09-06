package com.kimya.uygulama

import com.kimya.uygulama.utils.KimyaData
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log10

/**
 * FAZ 1 — Asit/baz motoru (KimyaData.phHesapla).
 * Referans: pH = -log[H+], pOH = 14 - pH (25 °C, Kw = 1e-14).
 */
class AsitBazTest {

    @Test fun ph7_notr() {
        val r = KimyaData.phHesapla(7.0, "pH")
        assertEquals(1e-7, r["[H+]"] as Double, 1e-12)
        assertEquals(1e-7, r["[OH-]"] as Double, 1e-12)
        assertEquals("Notr", r["tur"])
    }

    @Test fun ph1_kuvvetli_asit() {
        val r = KimyaData.phHesapla(1.0, "pH")
        assertEquals(0.1, r["[H+]"] as Double, 1e-9)
        assertEquals(13.0, r["pOH"] as Double, 1e-9)
        assertEquals("Kuvvetli Asit", r["tur"])
    }

    @Test fun ph14_kuvvetli_baz() {
        val r = KimyaData.phHesapla(14.0, "pH")
        assertEquals(1e-14, r["[H+]"] as Double, 1e-20)
        assertEquals("Kuvvetli Baz", r["tur"])
    }

    @Test fun derisimden_ph() {
        val r = KimyaData.phHesapla(0.1, "[H+]")
        assertEquals(1.0, r["pH"] as Double, 1e-9)
        val r2 = KimyaData.phHesapla(1e-7, "[H+]")
        assertEquals(7.0, r2["pH"] as Double, 1e-9)
    }

    @Test fun poh_girdisi() {
        val r = KimyaData.phHesapla(3.0, "pOH")
        assertEquals(11.0, r["pH"] as Double, 1e-9)
        assertEquals(1e-3, r["[OH-]"] as Double, 1e-12)
    }

    @Test fun siniflandirma_sinirlari() {
        assertEquals("Kuvvetli Asit", KimyaData.phHesapla(2.0, "pH")["tur"])
        assertEquals("Zayif Asit", KimyaData.phHesapla(5.0, "pH")["tur"])
        assertEquals("Zayif Baz", KimyaData.phHesapla(8.0, "pH")["tur"])
        assertEquals("Kuvvetli Baz", KimyaData.phHesapla(12.0, "pH")["tur"])
    }

    // Gidiş-dönüş: pH -> [H+] -> pH aynı değeri vermeli
    @Test fun gidis_donus_tutarliligi() {
        for (ph in listOf(1.0, 2.5, 4.5, 7.0, 9.3, 12.0, 13.7)) {
            val h = KimyaData.phHesapla(ph, "pH")["[H+]"] as Double
            val geri = KimyaData.phHesapla(h, "[H+]")["pH"] as Double
            assertEquals("pH=$ph gidiş-dönüş tutarsız", ph, geri, 1e-9)
        }
    }

    // log(0) ve log(negatif) asla sonuç sızdırmamalı
    @Test fun sifir_derisim_hata() {
        val r = KimyaData.phHesapla(0.0, "[H+]")
        assertTrue("hata anahtarı olmalı", r.containsKey("hata"))
        assertFalse(r["pH"] is Double && (r["pH"] as Double).isFinite())
    }

    @Test fun negatif_derisim_hata() {
        assertTrue(KimyaData.phHesapla(-1.0, "[H+]").containsKey("hata"))
        assertTrue(KimyaData.phHesapla(-0.5, "[OH-]").containsKey("hata"))
    }

    @Test fun nan_derisim_hata() {
        assertTrue(KimyaData.phHesapla(Double.NaN, "[H+]").containsKey("hata"))
    }

    @Test fun gecersiz_tur_hata() {
        assertTrue(KimyaData.phHesapla(7.0, "pKa").containsKey("hata"))
    }

    @Test fun sonuclar_sonlu() {
        for (ph in listOf(0.0, 1.0, 7.0, 14.0)) {
            val r = KimyaData.phHesapla(ph, "pH")
            for (k in listOf("pH", "pOH", "[H+]", "[OH-]")) {
                val v = r[k] as Double
                assertTrue("$k sonsuz/NaN olmamalı (pH=$ph)", v.isFinite())
            }
        }
    }
}
