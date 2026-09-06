package com.kimya.uygulama

import com.kimya.uygulama.features.AasEngine
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * FAZ 2 — AAS deney hattı: gaz → alev → lamba → blank → standart → ölçüm.
 * Referans: Beer-Lambert doğrusallığı (A = k·C), R² ≈ 1, bilinen derişim geri kazanımı.
 */
class AasTest {

    /** Motoru kalibrasyon bitmiş duruma getirir (Cu, varsayılan). */
    private fun kalibreliMotor(): AasEngine = AasEngine().apply {
        toggleGases()
        repeat(3) { tick(1f) }
        toggleFlame()
        toggleLamp()
        doBlank()
        repeat(3) {
            measureStd()
            repeat(3) { tick(1f) }
        }
    }

    @Test fun asama_sirasi() {
        val e = AasEngine()
        assertEquals(0, e.stage())
        e.toggleGases()
        assertEquals(1, e.stage())
        repeat(3) { e.tick(1f) }
        e.toggleFlame()
        assertTrue(e.flameOn)
        assertEquals(2, e.stage())
        e.toggleLamp()
        assertEquals(3, e.stage())
        e.doBlank()
        assertTrue(e.blankDone)
        assertEquals(4, e.stage())
    }

    @Test fun basincsiz_atesleme_basarisiz() {
        val e = AasEngine()
        e.toggleFlame() // vanalar kapalı
        assertFalse("basınçsız alev yanmamalı", e.flameOn)
    }

    @Test fun sirsasiz_std_olcumu_yok() {
        val e = AasEngine()
        e.measureStd() // alev/lamba/blank yok
        assertEquals(0, e.stdPoints.size)
    }

    @Test fun kalibrasyon_dogrusalligi() {
        val e = kalibreliMotor()
        assertTrue(e.stdDone())
        val (m, b, r2) = e.regression()
        assertTrue("eğim pozitif olmalı: $m", m > 0f)
        assertTrue("R² yüksek olmalı: $r2", r2 > 0.95f)
        assertTrue("kesim küçük olmalı: $b", abs(b) < 0.05f)
    }

    @Test fun bilinen_derisim_geri_kazanimi() {
        // 5 ppm Cu numunesi konup ölçülünce ~5 ppm okunmalı
        val e = kalibreliMotor()
        e.setConc(5f)
        e.toggleMeasure()
        repeat(30) { e.tick(1f) }
        assertEquals(5.0, e.ppmReadout.toDouble(), 1.0)
    }

    @Test fun standart_derisimler_artan() {
        val e = AasEngine()
        val c = e.stdConcs()
        assertEquals(3, c.size)
        assertTrue(c[0] > 0 && c[0] < c[1] && c[1] < c[2] && c[2] <= e.element.cMax)
    }

    @Test fun sentetik_regresyon() {
        // Saf matematik: (0,0),(5,0.5),(10,1.0) -> m=0.1, b=0, R²=1
        val e = AasEngine()
        e.stdPoints.addAll(listOf(0f to 0f, 5f to 0.5f, 10f to 1f))
        val (m, b, r2) = e.regression()
        assertEquals(0.1, m.toDouble(), 1e-6)
        assertEquals(0.0, b.toDouble(), 1e-6)
        assertEquals(1.0, r2.toDouble(), 1e-6)
    }

    @Test fun tek_nokta_guvenli() {
        val e = AasEngine()
        e.stdPoints.add(5f to 0.5f)
        val (_, _, r2) = e.regression() // çökmemeli
        assertTrue(r2.isFinite())
    }

    @Test fun bos_kalibrasyon_guvenli() {
        val e = AasEngine()
        val (m, _, _) = e.regression()
        assertTrue(m.isFinite())
        assertFalse(e.stdDone())
    }

    @Test fun giris_sinirlari() {
        val e = AasEngine()
        e.setConc(-5f)
        assertEquals(0f, e.samplePpm)
        e.setConc(999f)
        assertEquals(e.element.cMax, e.samplePpm)
        e.setLampCurrent(99f)
        assertEquals(15f, e.lampCurrent)
        e.setLampCurrent(-3f)
        assertEquals(0f, e.lampCurrent)
    }

    @Test fun element_listesi_gecerli() {
        assertTrue(AasEngine.ELEMENTS.size >= 10)
        for (el in AasEngine.ELEMENTS) {
            assertTrue(el.symbol.isNotBlank())
            assertTrue("dalgaboyu: ${el.symbol}", el.wavelengthNm in 190f..900f)
            assertTrue("hassasiyet: ${el.symbol}", el.sensitivity > 0f)
            assertTrue("cMax: ${el.symbol}", el.cMax > 0f)
        }
    }

    @Test fun reset_temizler() {
        val e = kalibreliMotor()
        e.reset()
        assertFalse(e.flameOn || e.lampOn || e.blankDone)
        assertEquals(0, e.stdPoints.size)
        assertEquals(0, e.stage()) // vanalar kapalı -> başa döner
    }
}
