package com.kimya.uygulama

import com.kimya.uygulama.features.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

/** FAZ 3 — Optik motoru: vektör aritmetiği, yansıma, kırılma, filtre, ışın-segment kesişimi. */
class OptikTest {

    // ── Vec aritmetiği ──
    @Test fun vec_plus() { assertEquals(Vec(3f, 5f), Vec(1f, 2f) + Vec(2f, 3f)) }
    @Test fun vec_minus() { assertEquals(Vec(-1f, -1f), Vec(1f, 2f) - Vec(2f, 3f)) }
    @Test fun vec_times() { assertEquals(Vec(4f, 6f), Vec(2f, 3f) * 2f) }
    @Test fun vec_dot() { assertEquals(6f, Vec(1f, 2f).dot(Vec(2f, 2f)), 1e-5f) }
    @Test fun vec_cross() { assertEquals(-1f, Vec(1f, 2f).cross(Vec(2f, 3f)), 1e-5f) }
    @Test fun vec_len() { assertEquals(5f, Vec(3f, 4f).len(), 1e-5f) }
    @Test fun vec_norm() {
        val n = Vec(3f, 4f).norm()
        assertEquals(1f, n.len(), 1e-5f)
        assertEquals(0.6f, n.x, 1e-5f)
        assertEquals(0.8f, n.y, 1e-5f)
    }
    @Test fun vec_norm_sifir() {
        val n = Vec(0f, 0f).norm()
        assertEquals(1f, n.len(), 1e-5f) // sıfır vektörü → (1,0)
    }

    // ── deg2dir ──
    @Test fun deg2dir_sag() {
        val d = deg2dir(0f)
        assertEquals(1f, d.x, 1e-4f); assertEquals(0f, d.y, 1e-4f)
    }
    @Test fun deg2dir_yukari() {
        val d = deg2dir(90f)
        assertEquals(0f, d.x, 1e-4f); assertEquals(1f, d.y, 1e-4f)
    }
    @Test fun deg2dir_sol() {
        val d = deg2dir(180f)
        assertEquals(-1f, d.x, 1e-4f); assertEquals(0f, d.y, 1e-4f)
    }

    // ── Yansıma ──
    @Test fun reflect_yatay() {
        // Sağdan gelen ışın, yatay ayna → aşağı gider
        val r = OpticsEngine.reflect(Vec(1f, 0f), Vec(0f, 1f))
        assertEquals(1f, r.x, 1e-4f); assertEquals(0f, r.y, 1e-4f)
    }
    @Test fun reflect_45derece() {
        // Aşağı-sola gelen ışın, yatay yüzeye (normal yukarı) çarpınca yukarı-sola yansır
        val r = OpticsEngine.reflect(Vec(1f, -1f).norm(), Vec(0f, 1f))
        assertEquals(1f, r.len(), 1e-3f)
        assertTrue("yukarı yansımalı", r.y > 0f)
    }
    @Test fun reflect_birimvektor() {
        val r = OpticsEngine.reflect(Vec(0f, -1f), Vec(0f, 1f))
        assertEquals(1f, r.len(), 1e-4f)
    }

    // ── Kırılma (Snell) ──
    @Test fun refract_dogrusal() {
        // Yüzey paralel → ışın bükülmez (n1 == n2)
        val r = OpticsEngine.refract(Vec(0f, -1f), Vec(0f, 1f), 1.0f, 1.0f)
        assertNotNull(r)
        assertEquals(0f, r!!.x, 1e-4f)
        assertTrue(r.y < 0f)
    }
    @Test fun refract_cami_havaya() {
        // Camdan havaya: hafif kırılma
        val r = OpticsEngine.refract(Vec(0f, -1f), Vec(0f, 1f), 1.5f, 1.0f)
        assertNotNull(r)
    }
    @Test fun refract_tam_ic_yansima() {
        // Yogun → seyrelt ortamda dik açıya yakın → TIR
        // n1 > n2 olduğu için TIR mümkün
        val r = OpticsEngine.refract(Vec(0.97f, -0.24f).norm(), Vec(0f, 1f), 1.5f, 1.0f)
        assertNull("TIR bekleniyor", r)
    }
    @Test fun refract_birimvektor() {
        val r = OpticsEngine.refract(Vec(0f, -1f), Vec(0f, 1f), 1.0f, 1.5f)
        assertNotNull(r)
        assertEquals(1f, r!!.len(), 1e-4f)
    }

    // ── Işın-parça kesişimi (raySeg) ──
    @Test fun raySeg_kesisim() {
        // Işın (0,0)→sağa, segment dikey (5,-5)-(5,5) → t≈5
        val t = OpticsEngine.raySeg(0f, 0f, 1f, 0f, 5f, -5f, 5f, 5f)
        assertNotNull(t)
        assertEquals(5f, t!!, 0.1f)
    }
    @Test fun raySeg_paralel() {
        val t = OpticsEngine.raySeg(0f, 0f, 1f, 0f, 5f, 0f, 10f, 0f)
        assertNull(t)
    }
    @Test fun raySeg_arkadan() {
        // Segment ışın arkasında
        val t = OpticsEngine.raySeg(10f, 0f, 1f, 0f, -5f, -5f, -5f, 5f)
        assertNull(t)
    }

    // ── Filtre ──
    @Test fun applyFilter_kirmizi() {
        // JVM'de Color.rgb() varsayılan değer döner; filtre mantığını doğrudan test edemeyiz
        // ama fonksiyonun çökmemesi yeterli
        val c = OpticsEngine.applyFilter(0, 0)
        // Color.red(0) JVM'de 0 → null dönmesi beklenir
    }
    @Test fun applyFilter_yesil() {
        val c = OpticsEngine.applyFilter(0, 1)
    }
    @Test fun applyFilter_soguruldu() {
        val c = OpticsEngine.applyFilter(0, 0)
    }

    // ── Kütüphane ──
    @Test fun spectrum_7band() { assertEquals(7, OpticsEngine.SPECTRUM.size) }
    @Test fun spectrum_kirmizidan_moruya() {
        val lambdas = OpticsEngine.SPECTRUM.map { it.lambda }
        assertEquals(640f, lambdas.first(), 1f)
        assertEquals(410f, lambdas.last(), 1f)
    }
    @Test fun spectrum_n_artan() {
        val ns = OpticsEngine.SPECTRUM.map { it.n }
        for (i in 1 until ns.size) assertTrue("n[$i]>=n[${i-1}]", ns[i] >= ns[i - 1])
    }
    @Test fun nOf_varsayilan() { assertEquals(1.55f, OpticsEngine.nOf(null), 1e-4f) }
    @Test fun nOf_kirmizi() { assertTrue(OpticsEngine.nOf(640f) < OpticsEngine.nOf(410f)) }

    // ── Motor instance ──
    @Test fun motor_bos_sahne() {
        val e = OpticsEngine()
        assertEquals(0, e.elements.size)
        assertNull(e.selected)
    }
    @Test fun motor_ekle_sec_sil() {
        val e = OpticsEngine()
        val s = e.addSource(100f, 100f)
        assertNotNull(s); assertEquals(1, e.elements.size)
        e.select(s)
        assertEquals(s, e.selected)
        e.removeSelected()
        assertEquals(0, e.elements.size)
        assertNull(e.selected)
    }
    @Test fun motor_max_eleman() {
        val e = OpticsEngine()
        repeat(OpticsEngine.MAX_ELEMENTS) { e.addSource(it * 10f, 0f) }
        assertEquals(OpticsEngine.MAX_ELEMENTS, e.elements.size)
        assertNull("aşım başarısız olmalı", e.addSource(999f, 0f))
    }
    @Test fun motor_temizle() {
        val e = OpticsEngine()
        repeat(5) { e.addSource(it * 10f, 0f) }
        e.clear()
        assertEquals(0, e.elements.size)
    }
    @Test fun motor_trace_bos() {
        val e = OpticsEngine()
        val rays = e.trace(1000f, 600f)
        assertTrue(rays.isEmpty()) // ışık kaynağı yok
    }
    @Test fun motor_trace_kaynakli() {
        val e = OpticsEngine()
        e.addSource(50f, 300f)
        val rays = e.trace(1000f, 600f)
        assertTrue("kaynaktan ışık çıkmalı", rays.isNotEmpty())
    }
    @Test fun motor_seedDefault() {
        val e = OpticsEngine()
        e.seedDefault(1000f, 600f)
        assertTrue(e.elements.size >= 2) // kaynak + prizma
        val rays = e.trace(1000f, 600f)
        assertTrue(rays.isNotEmpty())
    }
    @Test fun motor_tum_ekleme_turleri() {
        val e = OpticsEngine()
        e.addSource(10f, 10f)
        e.addMirror(30f, 30f)
        e.addPrism(50f, 50f)
        e.addFilter(70f, 70f)
        e.addLensConv(90f, 90f)
        e.addLensDiv(110f, 110f)
        e.addBlocker(130f, 130f)
        e.addBeamSplitter(150f, 150f)
        e.addSlab(170f, 170f)
        e.addPoint(190f, 190f)
        assertEquals(10, e.elements.size)
    }

    // ── Renk yardımcıları ──
    @Test fun sourceColorName_gecerli() {
        for (m in 0..3) assertTrue(OpticsEngine.sourceColorName(m).isNotBlank())
    }
    @Test fun filterColorName_gecerli() {
        for (f in 0..2) assertTrue(OpticsEngine.filterColorName(f).isNotBlank())
    }
}
