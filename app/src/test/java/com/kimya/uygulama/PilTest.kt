package com.kimya.uygulama

import com.kimya.uygulama.features.CellEngine
import com.kimya.uygulama.features.CellMode
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * FAZ 2 — Pil / Nernst motoru.
 * Referans: Daniell pili Zn|Cu E° = 0.34 - (-0.76) = 1.10 V.
 * Nernst (25 °C): E = E° - (0.0592/n)·logQ (k = 2.303·RT/nF).
 */
class PilTest {

    @Test fun daniell_standart_emf() {
        // Varsayılan: sol Zn, sağ Cu, 1 M — E ≈ 1.10 V
        assertEquals(1.10, CellEngine().nernstE().toDouble(), 0.01)
    }

    @Test fun cinko_gumus_hucresi() {
        val e = CellEngine().apply { setRight(4) } // Ag
        // E° = 0.80 + 0.76 = 1.56 V (n = EKOK(2,1) = 2, Q = 1)
        assertEquals(1.56, e.nernstE().toDouble(), 0.01)
    }

    @Test fun derisim_pili_nernst() {
        val e = CellEngine().apply { setConcL(0.1f) } // anot 0.1 M
        // E = 1.10 - 0.0296·log(0.1) = 1.10 + 0.0296 ≈ 1.1296
        assertEquals(1.1296, e.nernstE().toDouble(), 0.005)
    }

    @Test fun ayni_metal_olu_pil() {
        val e = CellEngine().apply { setLeft(2); setRight(2) } // Ni|Ni, eşit derişim
        assertTrue(e.deadCell)
        assertEquals(0.0, e.nernstE().toDouble(), 0.02)
    }

    @Test fun anot_katot_yonu() {
        val e = CellEngine() // Zn sol, Cu sağ
        assertEquals("Zn", e.anode.symbol)
        assertEquals("Cu", e.cathode.symbol)
        assertTrue(e.anodeLeft)
    }

    @Test fun ters_baglanti_anot_degisir() {
        val e = CellEngine().apply { setLeft(3); setRight(0) } // sol Cu, sağ Zn
        assertEquals("Zn", e.anode.symbol)
        assertEquals("Cu", e.cathode.symbol)
        assertFalse(e.anodeLeft)
        assertEquals(1.10, e.nernstE().toDouble(), 0.01)
    }

    @Test fun indeks_sinirlari() {
        val e = CellEngine().apply { setLeft(99); setRight(-5) }
        assertTrue(e.nernstE().isFinite())
    }

    @Test fun emf_asla_negatif_degildir() {
        val e = CellEngine()
        for (l in 0..4) for (r in 0..4) {
            e.setLeft(l); e.setRight(r)
            assertTrue("E<0 @ $l|$r", e.nernstE() >= 0f)
        }
    }

    @Test fun faraday_kutle_birikimi() {
        // Galvanik deşarjda anotta çözünen kütle artmalı ve sonlu olmalı
        val e = CellEngine()
        repeat(40) { e.tick(0.5f) }
        assertTrue(e.dissolvedGrams() > 0f)
        assertTrue(e.dissolvedGrams().isFinite())
        assertTrue(e.platedGrams() > 0f)
        assertTrue(e.platedGrams().isFinite())
    }

    @Test fun kablo_sokukken_akim_yok() {
        val e = CellEngine().apply { toggleWireL() }
        assertFalse(e.circuitComplete())
        repeat(10) { e.tick(0.5f) }
        assertEquals(0f, e.dissolvedGrams())
    }

    @Test fun reset_temizler() {
        val e = CellEngine()
        repeat(20) { e.tick(0.5f) }
        e.reset()
        assertEquals(0f, e.dissolvedGrams())
        assertEquals(0f, e.platedGrams())
    }

    @Test fun elektroliz_modu() {
        val e = CellEngine().apply { setMode(CellMode.ELECTROLYSIS) }
        assertTrue(e.nernstE().isFinite())
    }
}
