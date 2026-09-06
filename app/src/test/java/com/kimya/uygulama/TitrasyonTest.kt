package com.kimya.uygulama

import com.kimya.uygulama.features.Chemical
import com.kimya.uygulama.features.ExperimentConfig
import com.kimya.uygulama.features.IndicatorType
import com.kimya.uygulama.features.TitrationEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * FAZ 1 — Titrasyon motoru.
 * Referans deney: 25.00 mL 0.100 M HCl + 0.100 M NaOH.
 * Başlangıç pH = -log(0.1) = 1.0; eşdeğerlik (25 mL) pH = 7.0;
 * 50 mL'de aşırılık: 2.5 mmol OH⁻ / 75 mL = 0.0333 M -> pOH 1.477 -> pH ≈ 12.52.
 */
class TitrasyonTest {

    private fun motor(): TitrationEngine = TitrationEngine().apply {
        setConfig(
            ExperimentConfig(
                Chemical("HCl", "HCl", 1, true),
                Chemical("NaOH", "NaOH", 1, false),
                25f, 0.1f, 0.1f, IndicatorType.PHENOLPHTHALEIN
            )
        )
    }

    @Test fun baslangic_ph() = assertEquals(1.0, motor().calcPH(0f).toDouble(), 0.05)

    @Test fun esdegerlik_ph() = assertEquals(7.0, motor().calcPH(25f).toDouble(), 0.1)

    @Test fun asiri_baz_ph() = assertEquals(12.52, motor().calcPH(50f).toDouble(), 0.15)

    @Test fun yari_esdegerlik() {
        // 12.5 mL: yarı nötrleşmiş kuvvetli asit, pH ≈ 1.3-1.5 bandında olmalı
        val ph = motor().calcPH(12.5f).toDouble()
        assertTrue("pH=$ph 1.0-2.0 bandında olmalı", ph in 1.0..2.0)
    }

    @Test fun monoton_artis() {
        // Kuvvetli asit + kuvvetli baz eğrisi monoton artar
        val m = motor()
        var onceki = -100f
        for (v in 0..50 step 2) {
            val ph = m.calcPH(v.toFloat())
            assertTrue("pH geriledi @ $v mL", ph >= onceki - 0.01f)
            onceki = ph
        }
    }

    @Test fun her_noktada_sonlu() {
        val m = motor()
        for (v in 0..50) {
            val ph = m.calcPH(v.toFloat()).toDouble()
            assertTrue("NaN/Infinity @ $v mL", ph.isFinite())
            assertTrue("pH aralık dışı @ $v mL: $ph", ph in -1.0..15.0)
        }
    }

    @Test fun varsayilan_eslesme_gecerli() {
        val m = TitrationEngine()
        assertTrue(m.pairValid())
    }

    @Test fun asit_asit_eslesmesi_gecersiz() {
        val m = TitrationEngine().apply {
            setConfig(
                ExperimentConfig(
                    Chemical("HCl", "HCl", 1, true),
                    Chemical("H2SO4", "H2SO4", 1, true),
                    25f, 0.1f, 0.1f, IndicatorType.LITMUS
                )
            )
        }
        assertFalse(m.pairValid())
        m.openValve()
        assertFalse("geçersiz eşleşmede vana açılmamalı", m.isValveOpen)
    }

    @Test fun zayif_asit_tampon_bolgesi() {
        // 0.1 M asetik asit (Ka=1.8e-5): başlangıç pH ≈ 2.87
        val m = TitrationEngine().apply {
            setConfig(
                ExperimentConfig(
                    Chemical("Asetik Asit", "CH3COOH", 1, true, kaList = listOf(1.8e-5)),
                    Chemical("NaOH", "NaOH", 1, false),
                    25f, 0.1f, 0.1f, IndicatorType.PHENOLPHTHALEIN
                )
            )
        }
        assertEquals(2.87, m.calcPH(0f).toDouble(), 0.1)
    }
}
