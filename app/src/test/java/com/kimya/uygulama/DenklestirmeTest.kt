package com.kimya.uygulama

import com.kimya.uygulama.utils.ReactionBalancer
import org.junit.Assert.*
import org.junit.Test

/** FAZ 1 — Denklem denkleştirme (ReactionBalancer.dene). */
class DenklestirmeTest {

    @Test fun su_olusumu() {
        val r = ReactionBalancer.dene("H2 + O2 -> H2O")!!
        assertTrue(r.denkMi)
        assertEquals(listOf("H2" to 2, "O2" to 1), r.reaktifler)
        assertEquals(listOf("H2O" to 2), r.urunler)
    }

    @Test fun metan_yanmasi() {
        val r = ReactionBalancer.dene("CH4 + O2 -> CO2 + H2O")!!
        assertTrue(r.denkMi)
        assertEquals(listOf("CH4" to 1, "O2" to 2), r.reaktifler)
        assertEquals(listOf("CO2" to 1, "H2O" to 2), r.urunler)
    }

    @Test fun zaten_denk() {
        val r = ReactionBalancer.dene("NaCl -> NaCl")!!
        assertTrue(r.denkMi)
        assertEquals(listOf("NaCl" to 1), r.reaktifler)
    }

    @Test fun parantezli_bilesik() {
        val r = ReactionBalancer.dene("Ca(OH)2 + HCl -> CaCl2 + H2O")!!
        assertTrue(r.denkMi)
        assertEquals(listOf("Ca(OH)2" to 1, "HCl" to 2), r.reaktifler)
        assertEquals(listOf("CaCl2" to 1, "H2O" to 2), r.urunler)
    }

    @Test fun atom_korunumu_dogrulamasi() {
        // Dönen katsayılar gerçekten atomları denkeliyor mu? (çapraz kontrol)
        val r = ReactionBalancer.dene("Fe + O2 -> Fe2O3")!!
        assertTrue(r.denkMi)
        fun sayim(liste: List<Pair<String, Int>>): Map<String, Int> {
            val m = mutableMapOf<String, Int>()
            for ((f, k) in liste)
                for ((el, n) in ReactionBalancer.parseBilesif(f))
                    m[el] = (m[el] ?: 0) + n * k
            return m
        }
        val sol = sayim(r.reaktifler)
        val sag = sayim(r.urunler)
        assertEquals(sol, sag)
        assertEquals(4, r.reaktifler.first { it.first == "Fe" }.second)
    }

    @Test fun ok_yoksa_null() {
        assertNull(ReactionBalancer.dene("H2O"))
        assertNull(ReactionBalancer.dene("H2 + O2"))
    }

    @Test fun bilinmeyen_element_null() {
        assertNull(ReactionBalancer.dene("XYZ -> ABC"))
    }

    @Test fun bos_taraf_null() {
        assertNull(ReactionBalancer.dene("H2 -> "))
        assertNull(ReactionBalancer.dene(" -> H2O"))
    }

    @Test fun denklesmeyen_uyari_bayrakli() {
        // Kütle korunumunu ihlal eden uydurma tepkime: sessizce doğru gösterilmemeli
        val r = ReactionBalancer.dene("H2 -> O2")!!
        assertFalse("denkMi=false olmalı", r.denkMi)
    }
}
