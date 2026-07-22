package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class PetroleumCanvasView(context: Context) : View(context) {
    private var petType = 0
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val capP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val barP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL }
    private val bar2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.FILL }
    private val bar3P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0080.toInt(); style = Paint.Style.FILL }
    private val bar4P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4444.toInt(); style = Paint.Style.FILL }
    private val bar5P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); style = Paint.Style.FILL }
    private val fracP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
    private val highP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val elT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val bP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val b2P = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val arrowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); style = Paint.Style.FILL }
    private val condP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    fun setPetro(type: Int) { petType = type; invalidate() }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_MOVE -> { if (tMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }; lastTx = e.x; lastTy = e.y; invalidate() }
            MotionEvent.ACTION_UP -> { tMode = 0; return true }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        val c = canvas; val cx = w / 2f
        tP.textSize = h * 0.055f; sP.textSize = h * 0.03f; capP.textSize = h * 0.04f; fracP.textSize = h * 0.03f; highP.textSize = h * 0.035f

        when (petType) {
            0 -> {
                c.drawText("Petrolun Bilesimi", cx, h * 0.05f, tP)
                c.drawText("Ham petrol: C1-C60+ hidrokarbon karisimi", cx, h * 0.10f, sP)
                c.drawText("Fraksiyonel damitma ile ayrilir", cx, h * 0.14f, sP)

                data class Frac(val name: String, val pct: Float, val color: Int, val knRange: String)
                val fractions = listOf(
                    Frac("Gaz (C1-C4)", 0.18f, 0xFFFF4444.toInt(), "-160 C"),
                    Frac("Benzin (C5-C9)", 0.22f, 0xFFFF8C00.toInt(), "40-180 C"),
                    Frac("Gaz yagi (C10-C13)", 0.24f, 0xFFFFD700.toInt(), "180-250 C"),
                    Frac("Dizel (C14-C18)", 0.16f, 0xFF32CD32.toInt(), "250-350 C"),
                    Frac("Yakit (C19-C25)", 0.12f, 0xFF00F0FF.toInt(), "350-450 C"),
                    Frac("Asfalt (>C25)", 0.08f, 0xFF8B4513.toInt(), ">450 C"),
                )
                val barW = w * 0.50f; val barH = h * 0.42f; val baseY = h * 0.78f
                var curY = baseY
                for (frac in fractions) {
                    val fh = barH * frac.pct; curY -= fh
                    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = frac.color; style = Paint.Style.FILL }
                    c.drawRect(cx - barW / 2f, curY, cx + barW / 2f, curY + fh, bp)
                    c.drawLine(cx - barW / 2f, curY, cx + barW / 2f, curY, lineP)
                    fracP.textSize = h * 0.028f; fracP.color = 0xFFFFFFFF.toInt()
                    c.drawText(frac.name, cx - barW / 2f - 6f, curY + fh / 2f + fracP.textSize / 3f, fracP)
                    c.drawText("${(frac.pct * 100).toInt()}%", cx + barW / 2f + 6f, curY + fh / 2f + fracP.textSize / 3f, fracP)
                }
                c.drawText("Karbon sayisi arttikca kaynama noktasi yukselir", cx, h * 0.90f, sP)
                c.drawText("Cok karbonlu: agir (kati), az karbonlu: hafif (gaz)", cx, h * 0.95f, sP)
            }
            1 -> {
                c.drawText("Alkanlar - Doymus Hidrokarbonlar", cx, h * 0.05f, tP)
                c.drawText("Genel formul: CnH2n+2, C-C tek bag", cx, h * 0.10f, sP)

                val datas = listOf(
                    "Metan" to -161f, "Etan" to -88f, "Propan" to -42f,
                    "Butan" to 0f, "Pentan" to 36f, "Heksan" to 69f,
                    "Heptan" to 98f, "Oktan" to 125f
                )
                val maxBP = 150f; val barW = w * 0.65f / datas.size; val barMax = h * 0.42f; val barBase = h * 0.65f
                datas.forEachIndexed { i, (name, bp) ->
                    val bh = (bp / maxBP) * barMax; val bx = cx - barW * datas.size / 2f + i * barW + barW / 2f
                    val bc = when { bp < -50 -> barP; bp < 0 -> bar2P; bp < 50 -> bar3P; bp < 100 -> bar4P; else -> bar5P }
                    c.drawRect(bx - barW * 0.3f, barBase - bh, bx + barW * 0.3f, barBase, bc)
                    fracP.textSize = h * 0.025f; fracP.color = 0xFFFFFFFF.toInt()
                    c.drawText(name, bx, barBase + fracP.textSize * 1.2f, fracP)
                    c.drawText("%.0f C".format(bp), bx, barBase - bh - 4f, fracP)
                }
                c.drawText("Karbon sayisi arttikca van der Waals kuvvetleri artar,", cx, h * 0.80f, sP)
                c.drawText("kaynama noktasi yukselir (gaz->sivi->kati)", cx, h * 0.86f, sP)
                c.drawText("Metan CH4 / Etan C2H6 / Propan C3H8 / Butan C4H10", cx, h * 0.93f, sP)
            }
            2 -> {
                c.drawText("Kraking (Parcalama)", cx, h * 0.05f, tP)
                c.drawText("Uzun zincir -> Kisa zincir (benzin) + alken", cx, h * 0.10f, sP)

                val ar2 = (w * 0.03f).coerceAtMost(h * 0.04f).coerceAtMost(20f); val sp2 = ar2 * 4f
                val ay = h * 0.28f

                val chainPs = listOf(barP, bar2P, bar3P, bar4P, bar5P, barP, bar2P, bar3P)
                for (i in 0 until 8) {
                    val x = cx - sp2 * 3.5f + i * sp2
                    c.drawCircle(x, ay, ar2, chainPs[i]); c.drawCircle(x, ay, ar2, lineP)
                    elT.textSize = ar2 * 0.9f; elT.color = 0xFFFFFFFF.toInt()
                    c.drawText("C", x, ay + elT.textSize / 3f, elT)
                }
                c.drawText("Uzun zincir (>C20)", cx, ay + sp2 * 0.8f, sP)
                c.drawText("+ Isi 500 C", cx, ay + sp2 * 1.6f, highP)

                c.drawLine(cx - sp2 * 3f, ay + sp2 * 2f, cx + sp2 * 3f, ay + sp2 * 2f, arrowP)
                val ap = Path(); ap.moveTo(cx + sp2 * 3f, ay + sp2 * 2f); ap.lineTo(cx + sp2 * 3f - 12f, ay + sp2 * 2f - 7f); ap.lineTo(cx + sp2 * 3f - 12f, ay + sp2 * 2f + 7f); ap.close()
                c.drawPath(ap, arrowFill)

                for (j in 0 until 6) {
                    val x = cx - sp2 * 2.5f + j * sp2
                    c.drawCircle(x, ay + sp2 * 3f, ar2, bar5P); c.drawCircle(x, ay + sp2 * 3f, ar2, lineP)
                    elT.textSize = ar2 * 0.9f; elT.color = 0xFFFFFFFF.toInt()
                    c.drawText("C", x, ay + sp2 * 3f + elT.textSize / 3f, elT)
                }
                c.drawText("Benzin (C5-C10)", cx - sp2 * 1.5f, ay + sp2 * 4f, capP)

                c.drawCircle(cx + sp2 * 2f, ay + sp2 * 3f, ar2, barP); c.drawCircle(cx + sp2 * 2f, ay + sp2 * 3f, ar2, lineP)
                c.drawText("C", cx + sp2 * 2f - 6f, ay + sp2 * 3f + elT.textSize / 3f, elT)
                c.drawCircle(cx + sp2 * 3f, ay + sp2 * 3f, ar2, barP); c.drawCircle(cx + sp2 * 3f, ay + sp2 * 3f, ar2, lineP)
                c.drawText("C", cx + sp2 * 3f - 6f, ay + sp2 * 3f + elT.textSize / 3f, elT)
                c.drawText("Etilen", cx + sp2 * 2.5f, ay + sp2 * 4f, capP)

                c.drawText("Kraking: petrokimyanin temeli, benzin uretimi ve alken eldesi", cx, h * 0.84f, sP)
                c.drawText("Katalitik kraking: zeolit (Al2O3/SiO2) ile daha verimli", cx, h * 0.90f, sP)
            }
            3 -> {
                c.drawText("Reforming", cx, h * 0.05f, tP)
                c.drawText("Duz zincirli alkan -> Halkali/aromatik HC", cx, h * 0.10f, sP)

                val ar3 = (w * 0.03f).coerceAtMost(h * 0.04f).coerceAtMost(20f); val sp3 = ar3 * 4f
                val by = h * 0.35f
                for (j in 0 until 7) {
                    val x = w * 0.12f + j * sp3
                    c.drawCircle(x, by, ar3, bar3P); c.drawCircle(x, by, ar3, lineP)
                    elT.textSize = ar3 * 0.9f; elT.color = 0xFFFFFFFF.toInt()
                    c.drawText("C", x, by + elT.textSize / 3f, elT)
                }
                c.drawText("n-Heptan (duz zincir)", w * 0.35f, by + sp3, sP)
                c.drawText("OK 0", w * 0.35f, by + sp3 * 1.6f, sP)

                val rcx = w * 0.7f; val rcy = h * 0.42f; val r = sp3 * 1.5f
                val ang = 2f * PI.toFloat() / 6f
                for (i in 0 until 6) { val ax = rcx + r * cos(i * ang - PI.toFloat() / 2f); val ay = rcy + r * sin(i * ang - PI.toFloat() / 2f)
                    c.drawCircle(ax, ay, ar3, bar5P); c.drawCircle(ax, ay, ar3, lineP); elT.textSize = ar3 * 0.9f; elT.color = 0xFFFFFFFF.toInt(); c.drawText("C", ax, ay + elT.textSize / 3f, elT) }
                for (i in 0 until 6) { val a1 = rcx + r * cos(i * ang - PI.toFloat() / 2f); val a2 = rcx + r * cos((i + 1) * ang - PI.toFloat() / 2f)
                    val b1 = rcy + r * sin(i * ang - PI.toFloat() / 2f); val b2 = rcy + r * sin((i + 1) * ang - PI.toFloat() / 2f)
                    c.drawLine(a1, b1, a2, b2, if (i % 2 == 0) bP else b2P) }
                c.drawText("Benzen (halkali)", rcx, rcy + r + sp3, sP)
                c.drawText("OK 100", rcx, rcy + r + sp3 * 1.6f, sP)

                c.drawLine(cx - sp3 * 2f, h * 0.68f, cx + sp3 * 2f, h * 0.68f, arrowP)
                val ap = Path(); ap.moveTo(cx + sp3 * 2f, h * 0.68f); ap.lineTo(cx + sp3 * 2f - 12f, h * 0.68f - 7f); ap.lineTo(cx + sp3 * 2f - 12f, h * 0.68f + 7f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("Pt/Re katalizor, 500 C", cx, h * 0.66f, condP)

                c.drawText("Oktan sayisi artar -> benzin kalitesi yukselir", cx, h * 0.80f, sP)
                c.drawText("Aromatikler: benzen, toluen, ksilen - degerli ham madde", cx, h * 0.86f, sP)
                c.drawText("Reforming ile H2 de uretilir (hidrojenasyon icin)", cx, h * 0.92f, sP)
            }
            4 -> {
                c.drawText("Petrokimya Urunleri", cx, h * 0.05f, tP)
                c.drawText("Petrolden rafinasyonla elde edilen gunluk urunler", cx, h * 0.10f, sP)

                val items = listOf(
                    "Benzin" to "Yakit tasit", "Plastik" to "Ambalaj ve esya",
                    "Asfalt" to "Yol yapimi", "Gubre" to "Tarim",
                    "Ilac" to "Saglik", "Polyester" to "Tekstil",
                    "Deterjan" to "Temizlik", "Kozmetik" to "Kisisel bakim"
                )
                val cols = 4; val rows = 2
                val gridW = w * 0.80f; val gridH = h * 0.40f; val startX = cx - gridW / 2f; val startY = h * 0.22f
                val cellW = gridW / cols; val cellH = gridH / rows

                items.forEachIndexed { i, (name, sub) ->
                    val col = i % cols; val row = i / cols
                    val x = startX + col * cellW + cellW / 2f; val y = startY + row * cellH + cellH / 2f
                    val cellPs = arrayOf(barP, bar2P, bar3P, bar4P, bar5P, barP, bar2P, bar3P)
                    c.drawRoundRect(x - cellW * 0.35f, y - cellH * 0.3f, x + cellW * 0.35f, y + cellH * 0.3f, 10f, 10f, cellPs[i])
                    fracP.textSize = h * 0.035f; fracP.color = 0xFFFFFFFF.toInt()
                    c.drawText(name, x, y - 4f, fracP)
                    fracP.textSize = h * 0.024f; fracP.color = 0xFFAAAAAA.toInt()
                    c.drawText(sub, x, y + fracP.textSize * 1.6f, fracP)
                }
                c.drawText("Dunya ~90 milyon varil/gun | Rafineri: ham petrol -> degerli urun", cx, h * 0.82f, sP)
                c.drawText("Petrol olmasaydi: plastiksiz, asfaltsiz, ilacsiz bir dunya", cx, h * 0.88f, sP)
            }
            5 -> {
                c.drawText("Alternatif Enerji Kaynaklari", cx, h * 0.05f, tP)
                c.drawText("Petrolun sinirli olmasi ve cevre etkisi alternatifleri dogurdu", cx, h * 0.10f, sP)

                data class Enerji(val name: String, val pct: Float, val color: Int, val desc: String)
                val kaynaklar = listOf(
                    Enerji("Gunes", 0.25f, 0xFFFFD700.toInt(), "Fotovoltaik ~1 kW/m2"),
                    Enerji("Ruzgar", 0.20f, 0xFF00F0FF.toInt(), "Turpin ~5 MW"),
                    Enerji("Hidroelektrik", 0.18f, 0xFF3050F8.toInt(), "Baraj ~500 MW"),
                    Enerji("Jeotermal", 0.12f, 0xFFFF4444.toInt(), "Yeralti isisi"),
                    Enerji("Biyokutle", 0.15f, 0xFF39FF14.toInt(), "Biyodizel, etanol"),
                    Enerji("Nukleer", 0.10f, 0xFFFF69B4.toInt(), "Fisyon ~1 GW"),
                )
                val barW = w * 0.55f; val barH = h * 0.38f; val baseY = h * 0.75f
                var curY = baseY
                for (k in kaynaklar) {
                    val fh = barH * k.pct; curY -= fh
                    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = k.color; style = Paint.Style.FILL }
                    c.drawRect(cx - barW / 2f, curY, cx + barW / 2f, curY + fh, bp)
                    c.drawRect(cx - barW / 2f, curY, cx + barW / 2f, curY + fh, lineP)
                    fracP.textSize = h * 0.025f; fracP.color = 0xFFFFFFFF.toInt()
                    c.drawText(k.name, cx - barW / 2f - 6f, curY + fh / 2f + fracP.textSize / 3f, fracP)
                    fracP.textSize = h * 0.02f; fracP.color = 0xFFAAAAAA.toInt()
                    c.drawText(k.desc, cx + barW / 2f + 6f, curY + fh / 2f + fracP.textSize / 3f, fracP)
                }
                c.drawText("Dunya enerji tuketimi ~170.000 TWh/yil (petrol ~%31)", cx, h * 0.88f, highP)
                c.drawText("Yenilenebilir %25'e ulasti | Hedef: 2050 karbon notr", cx, h * 0.94f, sP)
            }
            6 -> {
                c.drawText("Petrol ve Cevre", cx, h * 0.05f, tP)
                c.drawText("Fosil yakit kullaniminin cevresel etkileri", cx, h * 0.10f, sP)

                val envY = h * 0.20f; val envH = h * 0.55f; val colW = w * 0.25f
                val envP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4444.toInt(); style = Paint.Style.FILL }
                val envP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL }
                val envP3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); style = Paint.Style.FILL }

                data class Etki(val name: String, val desc: String, val pct: Float, val color: Int)
                val etkiler = listOf(
                    Etki("Sera Gazlari", "CO2 metan -> kuresel isinma +1.2 C", 0.35f, 0xFFFF4444.toInt()),
                    Etki("Hava Kirliligi", "NOx SOx partikul -> astim kanser", 0.25f, 0xFFFFA500.toInt()),
                    Etki("Su Kirliligi", "Petrol sizintisi -> ekosistem yikimi", 0.20f, 0xFF3050F8.toInt()),
                    Etki("Toprak Kirliligi", "Plastik atik -> 400 yil bozunma", 0.20f, 0xFF8B4513.toInt()),
                )
                for ((i, etki) in etkiler.withIndex()) {
                    val x = cx - colW * 1.5f + i * colW; val y = envY
                    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = etki.color; style = Paint.Style.FILL }
                    c.drawRoundRect(x - colW * 0.35f, y, x + colW * 0.35f, y + envH * etki.pct, 10f, 10f, bp)
                    c.drawRoundRect(x - colW * 0.35f, y, x + colW * 0.35f, y + envH * etki.pct, 10f, 10f, lineP)
                    fracP.textSize = h * 0.025f; fracP.color = 0xFFFFFFFF.toInt()
                    c.drawText(etki.name, x, y + 14f, fracP)
                    fracP.textSize = h * 0.022f; fracP.color = 0xFFCCCCCC.toInt()
                    c.drawText(etki.desc, x, y + envH * etki.pct + 16f, fracP)
                }

                c.drawText("CO2: 420 ppm (sanayi oncesi 280) | Deniz seviyesi +20 cm (1900-2020)", cx, h * 0.84f, highP)
                c.drawText("Paris Iklim Anlasmasi: 1.5 C hedefi | Karbon vergisi, yesil enerji tesvigi", cx, h * 0.90f, sP)
                c.drawText("Bireysel: toplu tasima, geri donusum, enerji verimliligi", cx, h * 0.96f, sP)
            }
        }
        canvas.restore()
    }
}

class PetroleumFragment : Fragment() {
    private lateinit var petView: PetroleumCanvasView
    private val categories = listOf("Bilesim", "Alkanlar", "Kraking", "Reforming", "Petrokimya", "Alt. Enerji", "Cevre")
    private val details = listOf(
        "Petrolun Bilesimi: Ham petrol dogal halde C1-C60+ hidrokarbon karisimi. Alkanlar, sikloalkanlar, aromatikler ve az miktarda S, N, O bilesikleri icerir. Fraksiyonel damitma ile kaynama noktasina gore ayrilir. En hafif: CH4 (metan, gaz). En agir: C60H122 (asfalt, kati).",
        "Alkanlar: Genel formul CnH2n+2. Doymus hidrokarbonlar (sadece C-C tek bag). Karbon sayisi arttikca kaynama noktasi ve erime noktasi artar. C1-C4 gaz, C5-C17 sivi, C18+ kati. Petrolun ana bileseni. Yanma: CO2 + H2O + enerji.",
        "Kraking: Uzun zincirli alkanlarin (C20+) isi ve katalizor ile kisa zincirli alkan (benzin) ve alkenlere (etilen, propilen) parcalanmasi. Termal (700 C) ve katalitik (zeolit, 450 C). Petrokimyanin temel prosesi.",
        "Reforming: Duz zincirli alkanlardan (C6-C8) halkali ve aromatik HC (benzen, toluen, ksilen) uretilmesi. Katalizor (Pt/Re/Al2O3), 500 C. Oktan sayisini yukseltir (0->100). Yan urun H2 de onemlidir.",
        "Petrokimya Urunleri: Plastik (PE, PP, PVC), sentetik elyaf (polyester, nylon), ilac (aspirin, antibiyotik), gubre (amonyak), boya, deterjan, kozmetik, asfalt. ~90 milyon varil/gun petrol.",
        "Alternatif Enerji Kaynaklari: Gunes (fotovoltaik, 1 kW/m2), ruzgar (turbin, 5 MW), hidroelektrik (baraj, 500 MW), jeotermal (yeralti isisi), biyokutle (etanol, biyodizel), nukleer (fisyon, 1 GW). Dunya ~170.000 TWh/yil. Petrol %31, yenilenebilir %25. 2050 karbon notr hedefi.",
        "Petrol ve Cevre: CO2 (420 ppm, sanayi oncesi 280), kuresel isinma (+1.2 C). Sera gazlari (CO2, CH4), hava kirliligi (NOx, SOx, partikul), su kirliligi (petrol sizintisi), toprak kirliligi (plastik, 400 yil). Deniz seviyesi +20 cm. Paris Anlasmasi: 1.5 C hedefi."
    )
    private val formuls = listOf(
        "En hafif: CH4 (metan, en -182 C) | En agir: C60H122 (asfalt, en 100 C+)",
        "Metan CH4 / Etan C2H6 / Propan C3H8 / Butan C4H10 / Oktan C8H18",
        "C20H42 -> C10H22 (dekan) + C10H20 (deken) | Isi + katalizor",
        "C7H16 (n-heptan, OK 0) -> C6H6 (benzen, OK 100) + 4H2 | Pt/Re",
        "Plastik ~250 milyon ton/yil | Ilac ~1 trilyon $/yil",
        "Gunes 1000 W/m2 | Ruzgar 5 MW | Dunya 170.000 TWh | Petrol %31",
        "CO2 420 ppm | +1.2 C isinma | Deniz +20 cm | 400 yil plastik"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_petroleum, container, false)
        val placeholder = v.findViewById<View>(R.id.pet_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        petView = PetroleumCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * resources.displayMetrics.density).toInt())
        }
        parent.addView(petView, idx)

        val btnRow = v.findViewById<LinearLayout>(R.id.pet_btn_row)
        val btnIds = mutableListOf<Button>()
        categories.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 12f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    btnIds.forEach { it.alpha = 0.5f }; alpha = 1f
                    petView.setPetro(i)
                    v.findViewById<TextView>(R.id.pet_title).text = categories[i]
                    v.findViewById<TextView>(R.id.pet_detail).text = details[i]
                    v.findViewById<TextView>(R.id.pet_data).text = formuls[i]
                }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Petrol ve Hidrokarbonlar")
                .setMessage("Petrol, dogal olarak olusan hidrokarbon karisimidir.\n\n" +
                    "Bu bolumde:\n" +
                    "- Hidrokarbon turleri (alifatik, aromatik)\n" +
                    "- Dogalgaz ve rafinasyon sureci\n" +
                    "- Benzin, motorin, jet yagini olusturan fraksiyonlar\n" +
                    "- Petrol kimyasallari ve kullanim alanlari\n\n" +
                    "Her konu gorsel olarak aciklanir.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }
}
