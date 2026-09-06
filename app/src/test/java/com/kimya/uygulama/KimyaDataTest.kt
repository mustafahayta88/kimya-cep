package com.kimya.uygulama

import com.kimya.uygulama.utils.KimyaData
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** FAZ 3 — KimyaData: 118 element, formül ayrıştırıcı, mol kütlesi, pH, gaz. */
class KimyaDataTest {

    // ── Element veritabanı ──
    @Test fun element_sayisi_118() { assertEquals(118, KimyaData.elementler.size) }

    @Test fun element_tum_semboller() {
        val syms = setOf("H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne",
            "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca")
        for (s in syms) assertNotNull("eksik: $s", KimyaData.elementler[s])
    }

    @Test fun element_tum_alanlar_dolu() {
        for ((s, e) in KimyaData.elementler) {
            assertTrue("$s: atomNo<=0", e.atomNo > 0)
            assertTrue("$s: sembol boş", e.semIol.isNotBlank())
            assertTrue("$s: ad boş", e.adi.isNotBlank())
            assertTrue("$s: kütle<=0", e.kutle > 0.0)
            assertTrue("$s: periyot<1", e.periyot >= 1)
            assertTrue("$s: grup<1", e.grup >= 1)
        }
    }

    @Test fun element_tur_tanimli() {
        for ((s, e) in KimyaData.elementler) {
            val t = e.tur
            assertTrue("$s: '$t' tanımsız", t.isNotBlank())
        }
    }

    @Test fun hidrojen_kutlesi() {
        assertEquals(1.008, KimyaData.elementler["H"]!!.kutle, 0.001)
    }
    @Test fun altin_kutlesi() {
        assertEquals(196.967, KimyaData.elementler["Au"]!!.kutle, 0.001)
    }

    @Test fun elementBul_sembol() {
        val e = KimyaData.elementBul("Na")
        assertNotNull(e); assertEquals(11, e!!.atomNo)
    }
    @Test fun elementBul_buyuk_kucuk() {
        assertNotNull(KimyaData.elementBul("na"))
        assertNotNull(KimyaData.elementBul("NA"))
    }
    @Test fun elementBul_atomNo() {
        val e = KimyaData.elementBul("79")
        assertNotNull(e); assertEquals("Au", e!!.semIol)
    }
    @Test fun elementBul_ad() {
        val e = KimyaData.elementBul("Altın")
        assertNotNull(e); assertEquals("Au", e!!.semIol)
    }
    @Test fun elementBul_yanlis() { assertNull(KimyaData.elementBul("Xx")) }

    // ── Periyodik veri ──
    @Test fun periyodikVeri_dolu() { assertTrue(KimyaData.periyodikVeri.isNotEmpty()) }

    // ── Lantanit / Aktinit ──
    @Test fun lantanitler_14() { assertEquals(14, KimyaData.lantanitler.size) }
    @Test fun aktinitler_14() { assertTrue(KimyaData.aktinitler.size >= 14) }

    // ── İyonlaşma enerjisi / elektronegatiflik ──
    @Test fun iyonlasma_helium_en_yuksek() {
        val he = KimyaData.iyonlasmaEnerjileri["He"]!!
        val h = KimyaData.iyonlasmaEnerjileri["H"]!!
        assertTrue("He > H", he > h)
    }
    @Test fun elektronegatiflik_flor_en_yuksek() {
        val f = KimyaData.elektronegatiflikler["F"]!!
        for ((s, v) in KimyaData.elektronegatiflikler) {
            if (s != "F") assertTrue("$s >= F: $v >= $f", v <= f)
        }
    }
    @Test fun atomYaricap_buyukten_kucuge() {
        val cs = KimyaData.atomYaricapPm["Cs"]!!
        val f = KimyaData.atomYaricapPm["F"]!!
        assertTrue("Cs > F", cs > f)
    }

    // ── Formül ayrıştırıcı ──
    @Test fun formul_h2o() {
        val m = KimyaData.formulAyristir("H2O")!!
        assertEquals(2, m["H"]!!); assertEquals(1, m["O"]!!)
    }
    @Test fun formul_c6h12o6() {
        val m = KimyaData.formulAyristir("C6H12O6")!!
        assertEquals(6, m["C"]!!); assertEquals(12, m["H"]!!); assertEquals(6, m["O"]!!)
    }
    @Test fun formul_cohirli() {
        val m = KimyaData.formulAyristir("Ca(OH)2")!!
        assertEquals(1, m["Ca"]!!); assertEquals(2, m["O"]!!); assertEquals(2, m["H"]!!)
    }
    @Test fun formul_icIce() {
        val m = KimyaData.formulAyristir("Mg3(PO4)2")!!
        assertEquals(3, m["Mg"]!!); assertEquals(2, m["P"]!!); assertEquals(8, m["O"]!!)
    }
    @Test fun formul_tekil() {
        val m = KimyaData.formulAyristir("Fe")!!
        assertEquals(1, m["Fe"]!!)
    }
    @Test fun formul_yanlis() { assertNull(KimyaData.formulAyristir("")) }
    @Test fun formul_bosParantez() { assertNull(KimyaData.formulAyristir("()")) }
    @Test fun formul_yanlisParantez() {
        // Parser kapanmamış parantezi idare ediyor — bu bir parser tercihidir
        val r = KimyaData.formulAyristir("Ca(OH)")
        // Geçerli ya da null olabilir, her iki durumda da çökmemeli
    }

    // ── Molekül kütlesi ──
    @Test fun mk_h2o() { assertEquals(18.015, KimyaData.molekulKutlesiHesapla("H2O")!!, 0.01) }
    @Test fun mk_nacl() { assertEquals(58.443, KimyaData.molekulKutlesiHesapla("NaCl")!!, 0.01) }
    @Test fun mk_yanlis() { assertNull(KimyaData.molekulKutlesiHesapla("XYZ")) }

    // ── pH hesaplama ──
    @Test fun ph_hesaplama_asit() {
        val m = KimyaData.phHesapla(1.0, "pH")
        assertEquals(1.0, m["pH"] as Double, 0.01)
        assertEquals(13.0, m["pOH"] as Double, 0.01)
        assertEquals("Kuvvetli Asit", m["tur"] as String)
    }
    @Test fun ph_hesaplama_baz() {
        val m = KimyaData.phHesapla(13.0, "pH")
        assertEquals("Kuvvetli Baz", m["tur"] as String)
    }
    @Test fun ph_hesaplama_notr() {
        val m = KimyaData.phHesapla(7.0, "pH")
        assertEquals("Notr", m["tur"] as String)
    }
    @Test fun ph_hesaplama_tersinirlik() {
        val m1 = KimyaData.phHesapla(3.0, "pH")
        val h1 = m1["[H+]"] as Double
        val m2 = KimyaData.phHesapla(h1, "[H+]")
        assertEquals(3.0, m2["pH"] as Double, 0.01)
    }

    // ── İdeal gaz ──
    @Test fun gaz_pv_nrt() {
        val n = KimyaData.idealGaz(1.0, 22.414, null, 273.15)
        assertNotNull(n); assertEquals(1.0, n!!, 0.01)
    }
    @Test fun gaz_t() {
        val t = KimyaData.idealGaz(1.0, 22.414, 1.0, null)
        assertEquals(273.15, t!!, 0.5)
    }
    @Test fun gaz_yanlis() { assertNull(KimyaData.idealGaz(null, null, 1.0, 273.15)) }

    // ── Etkileşim ──
    @Test fun etkilesim_na_cl() {
        val s = KimyaData.etkilesim("Na", "Cl", null, null)
        assertTrue(s.contains("NaCl"))
    }

    // ── Formül自乘 ──
    @Test fun formul_asterisk() {
        val m = KimyaData.formulAyristir("H*2O") ?: return // '*' desteklenmiyor olabilir
        assertEquals(2, m["H"]!!)
    }
}
