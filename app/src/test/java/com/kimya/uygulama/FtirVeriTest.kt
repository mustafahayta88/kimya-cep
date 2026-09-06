package com.kimya.uygulama

import com.kimya.uygulama.features.FtirEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * FAZ 2 — FTIR veri bütünlüğü.
 * NOT: formüller alt-simgeli görüntü metnidir (C₂H₅OH); mol hesabında
 * kullanılmaz, kütle ayrı `mw` alanında tutulur. Burada yapı test edilir.
 */
class FtirVeriTest {

    @Test fun kutuphane_buyuklugu() {
        assertTrue(
            "bileşik sayısı: ${FtirEngine.COOKBOOK_COMPOUNDS.size}",
            FtirEngine.COOKBOOK_COMPOUNDS.size >= 80
        )
    }

    @Test fun grup_kimlikleri_gecerli() {
        val ids = FtirEngine.FUNCTIONAL_GROUPS.map { it.id }.toSet()
        for (c in FtirEngine.COOKBOOK_COMPOUNDS) {
            assertTrue("${c.name}: grup listesi boş", c.groups.isNotEmpty())
            for (g in c.groups)
                assertTrue("${c.name}: tanımsız grup $g", g in ids)
        }
    }

    @Test fun grup_pikleri_fiziksel_aralikta() {
        for (g in FtirEngine.FUNCTIONAL_GROUPS) {
            assertTrue("${g.id}: pik 400-4000 dışında (${g.peak})", g.peak in 400f..4000f)
            assertTrue("${g.id}: wMin<peak<wMax olmalı", g.wMin < g.peak && g.peak < g.wMax)
            assertTrue("${g.id}: genişlik pozitif olmalı", g.width > 0f)
            assertTrue("${g.id}: şiddet 0-1 aralığında olmalı", g.intensity in 0f..1f)
        }
    }

    @Test fun grup_kimlikleri_benzersiz() {
        val ids = FtirEngine.FUNCTIONAL_GROUPS.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun bilesik_alanlari_dolu() {
        for (c in FtirEngine.COOKBOOK_COMPOUNDS) {
            assertTrue("boş ad", c.name.isNotBlank())
            assertTrue("${c.name}: boş formül", c.formula.isNotBlank())
            assertTrue("${c.name}: boş kategori", c.category.isNotBlank())
            assertTrue("${c.name}: boş açıklama", c.desc.isNotBlank())
            val mw = c.mw.toDoubleOrNull()
            assertNotNull("${c.name}: mw sayı değil (${c.mw})", mw)
            assertTrue("${c.name}: mw pozitif olmalı", mw!! > 0.0)
        }
    }

    @Test fun molekul_yapilari_tutarli() {
        for (c in FtirEngine.COOKBOOK_COMPOUNDS) {
            val s = c.structure ?: continue
            assertTrue("${c.name}: atomsuz yapı", s.atoms.isNotEmpty())
            for (b in s.bonds) {
                assertTrue("${c.name}: bağ atom dışı (${b.from})", b.from in s.atoms.indices)
                assertTrue("${c.name}: bağ atom dışı (${b.to})", b.to in s.atoms.indices)
                assertTrue("${c.name}: bağ derecesi 1-3 olmalı", b.order in 1..3)
            }
            for (a in s.atoms)
                assertTrue("${c.name}: boş atom sembolü", a.symbol.isNotBlank())
        }
    }

    @Test fun bilesik_adlari_benzersiz() {
        val adlar = FtirEngine.COOKBOOK_COMPOUNDS.map { it.name }
        assertEquals(adlar.size, adlar.toSet().size)
    }

    @Test fun secim_gruplari_yukler() {
        val e = FtirEngine()
        val c = FtirEngine.COOKBOOK_COMPOUNDS.first()
        e.selectCompound(c)
        assertEquals(c.groups.toSet(), e.selectedGroups)
        e.selectCompound(null)
        assertTrue(e.selectedGroups.isEmpty())
    }

    @Test fun cozunurluk_sinirlari() {
        val e = FtirEngine()
        e.setResolution(99)
        assertEquals(8, e.resolution)
        e.setResolution(-5)
        assertEquals(1, e.resolution)
    }
}
