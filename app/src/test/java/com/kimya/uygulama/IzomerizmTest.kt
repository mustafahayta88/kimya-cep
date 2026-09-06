package com.kimya.uygulama

import com.kimya.uygulama.features.IsomerCanvasView
import org.junit.Assert.*
import org.junit.Test

/** FAZ 3 — İzomerizm veri bütünlüğü. */
class IzomerizmTest {

    @Test fun izomer_sayisi() {
        val canvas = IsomerCanvasView(android.app.Application())
        assertEquals(7, canvas.isos.size)
    }

    @Test fun izomer_turleri() {
        val canvas = IsomerCanvasView(android.app.Application())
        val titles = canvas.isos.map { it.title }
        assertTrue(titles.contains("Yapi Izomerligi"))
        assertTrue(titles.contains("Geometrik Izomerlik"))
        assertTrue(titles.contains("Optik Izomerlik"))
        assertTrue(titles.contains("Fonksiyonel Grup"))
        assertTrue(titles.contains("Pozisyon Izomerligi"))
        assertTrue(titles.contains("Tautomeri"))
        assertTrue(titles.contains("Konformasyon"))
    }

    @Test fun izomer_alanlari_dolu() {
        val canvas = IsomerCanvasView(android.app.Application())
        for ((i, iso) in canvas.isos.withIndex()) {
            assertTrue("[$i] başlık boş", iso.title.isNotBlank())
            assertTrue("[$i] alt başlık boş", iso.subtitle.isNotBlank())
            assertTrue("[$i] formül boş", iso.formula.isNotBlank())
            assertTrue("[$i] sol ad boş", iso.leftName.isNotBlank())
            assertTrue("[$i] sağ ad boş", iso.rightName.isNotBlank())
            assertTrue("[$i] sol özellikler boş", iso.leftProps.isNotEmpty())
            assertTrue("[$i] sağ özellikler boş", iso.rightProps.isNotEmpty())
            assertTrue("[$i] fark boş", iso.diff.isNotBlank())
            assertTrue("[$i] detay boş", iso.detail.isNotBlank())
        }
    }

    @Test fun izomer_benzersiz_baslik() {
        val canvas = IsomerCanvasView(android.app.Application())
        val basliklar = canvas.isos.map { it.title }
        assertEquals(basliklar.size, basliklar.toSet().size)
    }

    @Test fun setIso_gecerli() {
        val canvas = IsomerCanvasView(android.app.Application())
        canvas.setIso(3)
        assertEquals(3, canvas.isoIdx)
    }

    @Test fun setIso_sinir_disi() {
        val canvas = IsomerCanvasView(android.app.Application())
        canvas.setIso(99)
        assertTrue(canvas.isoIdx in 0..6)
        canvas.setIso(-5)
        assertTrue(canvas.isoIdx in 0..6)
    }
}
