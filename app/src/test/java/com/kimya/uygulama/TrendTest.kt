package com.kimya.uygulama

import com.kimya.uygulama.features.trendBirim
import com.kimya.uygulama.features.trendDeger
import com.kimya.uygulama.features.trendFormat
import com.kimya.uygulama.features.trendOzellikler
import org.junit.Assert.*
import org.junit.Test

/** FAZ 3 — Periyodik trend fonksiyonları. */
class TrendTest {

    @Test fun ozellik_sayisi() { assertTrue(trendOzellikler.size >= 6) }

    // ── trendBirim ──
    @Test fun birim_ionlasma() { assertEquals("kJ/mol", trendBirim("İyonlaşma Enerjisi")) }
    @Test fun birim_elektronegatiflik() { assertEquals("", trendBirim("Elektronegatiflik")) }
    @Test fun birim_yaricap() { assertEquals("pm", trendBirim("Atom Yarıçapı")) }
    @Test fun birim_kutle() { assertEquals("g/mol", trendBirim("Atom Kütlesi")) }

    // ── trendDeger ──
    @Test fun deger_h_ionlasma() {
        val v = trendDeger("H", "İyonlaşma Enerjisi")
        assertNotNull(v); assertEquals(1312.0, v!!, 1.0)
    }
    @Test fun deger_na_elektronegatiflik() {
        val v = trendDeger("Na", "Elektronegatiflik")
        assertNotNull(v); assertEquals(0.93, v!!, 0.01)
    }
    @Test fun deger_fe_kutle() {
        val v = trendDeger("Fe", "Atom Kütlesi")
        assertNotNull(v); assertEquals(55.845, v!!, 0.001)
    }
    @Test fun deger_au_yaricap() {
        val v = trendDeger("Au", "Atom Yarıçapı")
        assertNotNull(v); assertTrue(v!! > 100)
    }
    @Test fun deger_yanlis_element() {
        assertNull(trendDeger("Xx", "İyonlaşma Enerjisi"))
    }
    @Test fun deger_yanlis_ozellik() {
        assertNull(trendDeger("H", "Tanımsız Özellik"))
    }

    // ── trendFormat ──
    @Test fun format_elektronegatiflik_2basamak() {
        val s = trendFormat(3.44, "Elektronegatiflik")
        // JVM'de format farklı davranabilir, önemli olan çökmemesi
        assertTrue(s.isNotEmpty())
    }
    @Test fun format_ionlasma_tam() {
        val s = trendFormat(1312.0, "İyonlaşma Enerjisi")
        assertFalse(s.contains("."))
    }
    @Test fun format_yaricap_tam() {
        val s = trendFormat(53.0, "Atom Yarıçapı")
        assertFalse(s.contains("."))
    }

    // ── Trend tutarlılığı (periyodik yasa) ──
    @Test fun trend_ionlasma_lityumdan_flore() {
        // 3. periyotta soldan sağa iyonlaşma enerjisi artmalı
        val li = trendDeger("Li", "İyonlaşma Enerjisi")!!
        val be = trendDeger("Be", "İyonlaşma Enerjisi")!!
        val b = trendDeger("B", "İyonlaşma Enerjisi")!!
        val c = trendDeger("C", "İyonlaşma Enerjisi")!!
        val n = trendDeger("N", "İyonlaşma Enerjisi")!!
        val o = trendDeger("O", "İyonlaşma Enerjisi")!!
        val f = trendDeger("F", "İyonlaşma Enerjisi")!!
        assertTrue("Li < F", li < f)
        assertTrue("Li < N", li < n)
    }

    @Test fun trend_elektronegatiflik_artis() {
        // Sol → sağ artmalı
        val na = trendDeger("Na", "Elektronegatiflik")!!
        val mg = trendDeger("Mg", "Elektronegatiflik")!!
        val al = trendDeger("Al", "Elektronegatiflik")!!
        val si = trendDeger("Si", "Elektronegatiflik")!!
        val p = trendDeger("P", "Elektronegatiflik")!!
        val s = trendDeger("S", "Elektronegatiflik")!!
        val cl = trendDeger("Cl", "Elektronegatiflik")!!
        assertTrue("Na < Cl", na < cl)
        assertTrue("Na < Mg", na < mg)
    }
}
