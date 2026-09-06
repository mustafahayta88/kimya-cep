package com.kimya.uygulama

import com.kimya.uygulama.utils.KimyaData
import org.junit.Assert.*
import org.junit.Test

/**
 * FAZ 1 — Gaz yasaları ve derişim (KimyaData.idealGaz, gazMolKutlesi, yogunluktanMolarite).
 * Referans: PV = nRT, R = 0.0821 L·atm/(mol·K).
 * 1 mol ideal gaz (0 °C, 1 atm): V = 0.0821 × 273.15 = 22.4256 L.
 */
class GazYasalariTest {

    @Test fun hacim_bilinmeyeni() {
        // n=1, T=273.15 K, P=1 atm -> V ≈ 22.4256 L
        assertEquals(22.4256, KimyaData.idealGaz(1.0, null, 1.0, 273.15)!!, 0.001)
    }

    @Test fun basinc_bilinmeyeni() {
        // n=1, V=22.4256 L, T=273.15 K -> P ≈ 1 atm
        assertEquals(1.0, KimyaData.idealGaz(null, 22.4256, 1.0, 273.15)!!, 0.001)
    }

    @Test fun mol_bilinmeyeni() {
        // P=1, V=22.4, T=273.15 -> n ≈ 0.9989
        assertEquals(0.9989, KimyaData.idealGaz(1.0, 22.4, null, 273.15)!!, 0.0005)
    }

    @Test fun sicaklik_bilinmeyeni() {
        // P=1, V=22.4, n=1 -> T ≈ 272.82 K
        assertEquals(272.82, KimyaData.idealGaz(1.0, 22.4, 1.0, null)!!, 0.05)
    }

    @Test fun iki_bilinmeyen_null_doner() {
        assertNull(KimyaData.idealGaz(null, null, 1.0, 273.15))
        assertNull(KimyaData.idealGaz(1.0, 22.4, null, null))
        assertNull(KimyaData.idealGaz(null, null, null, null))
    }

    @Test fun oda_kosulu_hacim() {
        // 1 mol, 25 °C (298.15 K), 1 atm -> V = 0.0821 × 298.15 ≈ 24.4781 L
        assertEquals(24.4781, KimyaData.idealGaz(1.0, null, 1.0, 298.15)!!, 0.002)
    }

    @Test fun gazdan_mol_kutlesi_oksijen() {
        // 32 g O2, STP hacmi 22.4256 L -> M ≈ 32 g/mol
        assertEquals(32.0, KimyaData.gazMolKutlesi(32.0, 22.4256, 273.15, 1.0)!!, 0.05)
    }

    @Test fun gaz_mol_kutlesi_gecersiz_girdi() {
        assertNull(KimyaData.gazMolKutlesi(32.0, 22.4, 273.15, 0.0)) // P=0
        assertNull(KimyaData.gazMolKutlesi(32.0, 0.0, 273.15, 1.0)) // V=0
        assertNull(KimyaData.gazMolKutlesi(32.0, 22.4, -5.0, 1.0)) // negatif T
        assertNull(KimyaData.gazMolKutlesi(32.0, 22.4, 0.0, 1.0)) // T=0
    }

    @Test fun yuzdeden_molarite_hcl() {
        // %37 HCl, d=1.19 -> M = (37×10×1.19)/36.46 ≈ 12.08
        assertEquals(12.08, KimyaData.yogunluktanMolarite(37.0, 1.19, 36.46)!!, 0.05)
    }

    @Test fun yuzdeden_molarite_sifir_kutle() {
        assertNull(KimyaData.yogunluktanMolarite(37.0, 1.19, 0.0))
        assertNull(KimyaData.yogunluktanMolarite(37.0, 1.19, -5.0))
    }
}
