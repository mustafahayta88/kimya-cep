package com.kimya.uygulama

import com.kimya.uygulama.features.tumSorular
import org.junit.Assert.*
import org.junit.Test

/** FAZ 2 — Quiz veri bütünlüğü. */
class QuizVeriTest {

    @Test fun havuz_buyuklugu() {
        assertTrue("soru sayısı: ${tumSorular.size}", tumSorular.size >= 200)
    }

    @Test fun her_soru_gecerli() {
        for ((i, s) in tumSorular.withIndex()) {
            assertTrue("soru $i: metin boş", s.soru.isNotBlank())
            assertEquals("soru $i: 5 seçenek olmalı", 5, s.secenekler.size)
            assertTrue("soru $i: boş seçenek var", s.secenekler.all { it.isNotBlank() })
            assertTrue("soru $i: doğru indeksi aralık dışı (${s.dogru})", s.dogru in 0..4)
            assertTrue("soru $i: kategori boş", s.kategori.isNotBlank())
            assertTrue("soru $i: zorluk boş", s.zorluk.isNotBlank())
        }
    }

    @Test fun sorular_benzersiz() {
        val metinler = tumSorular.map { it.soru.trim() }
        assertEquals("tekrar eden soru var", metinler.size, metinler.toSet().size)
    }

    @Test fun dogru_cevap_dolu() {
        // doğru indeksindeki seçenek boş olmamalı (veri kayması yakalar)
        for ((i, s) in tumSorular.withIndex())
            assertTrue("soru $i", s.secenekler[s.dogru].isNotBlank())
    }
}
