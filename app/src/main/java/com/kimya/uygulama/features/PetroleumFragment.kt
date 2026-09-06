package com.kimya.uygulama.features

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class PetroleumFlatView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private var petType = 0
    private var fadeAlpha = 0f
    private var fadeAnimator: ValueAnimator? = null
    private var breathe = 0f
    private var breatheAnimator: ValueAnimator? = null

    private val bgPaint = Paint().apply { color = 0xFF0D1219.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x08FFFFFF.toInt(); strokeWidth = 0.5f }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF.toInt(); strokeWidth = 1f; style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val smallText = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
    private val arrowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }

    private var bgBitmap: Bitmap? = null

    fun setType(t: Int, animate: Boolean = true) {
        petType = t
        if (animate) {
            fadeAnimator?.cancel(); fadeAlpha = 0f
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 350L; interpolator = DecelerateInterpolator()
                addUpdateListener { fadeAlpha = it.animatedValue as Float; postInvalidate() }; start()
            }
        } else { fadeAlpha = 1f; postInvalidate() }
    }

    init {
        isClickable = true; isFocusable = true
        breatheAnimator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
            duration = 6000L; interpolator = LinearInterpolator(); repeatCount = ValueAnimator.INFINITE
            addUpdateListener { breathe = it.animatedValue as Float; postInvalidate() }; start()
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); breatheAnimator?.start() }
    override fun onDetachedFromWindow() { breatheAnimator?.cancel(); fadeAnimator?.cancel(); bgBitmap?.recycle(); bgBitmap = null; super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        bgBitmap?.recycle()
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        val gs = 40f; val cols = (w / gs).toInt() + 1; val rows = (h / gs).toInt() + 1
        for (i in 0 until cols) c.drawLine(i * gs, 0f, i * gs, h.toFloat(), gridPaint)
        for (i in 0 until rows) c.drawLine(0f, i * gs, w.toFloat(), i * gs, gridPaint)
        bgBitmap = bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.save(); canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
        when (petType) {
            0 -> drawBilesim(canvas)
            1 -> drawAlkanlar(canvas)
            2 -> drawKraking(canvas)
            3 -> drawReforming(canvas)
            4 -> drawPetrokimya(canvas)
            5 -> drawEnerji(canvas)
            6 -> drawCevre(canvas)
        }
        canvas.restore()
    }

    private fun sc(v: Float) = v * width / 400f
    private fun cx(f: Float) = width * f
    private fun cy(f: Float) = height * f
    private fun a(alpha: Int) = (alpha * fadeAlpha).toInt()

    private fun drawBar(canvas: Canvas, x: Float, yTop: Float, yBot: Float, w: Float, color: Int, label: String = "", value: String = "") {
        barPaint.color = Color.argb(a(220), Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawRoundRect(x - w / 2, yTop, x + w / 2, yBot, sc(4f), sc(4f), barPaint)
        canvas.drawRoundRect(x - w / 2, yTop, x + w / 2, yBot, sc(4f), sc(4f), barBorder)
        if (label.isNotEmpty()) {
            textPaint.color = Color.argb(a(255), 255, 255, 255); textPaint.textSize = sc(10f)
            canvas.drawText(label, x, yBot + sc(14f), textPaint)
        }
        if (value.isNotEmpty()) {
            smallText.color = Color.argb(a(200), 200, 200, 200); smallText.textSize = sc(9f)
            canvas.drawText(value, x, yTop - sc(4f), smallText)
        }
    }

    private fun drawBox(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int, text: String, sub: String = "") {
        boxPaint.color = Color.argb(a(100), Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawRoundRect(x - w / 2, y - h / 2, x + w / 2, y + h / 2, sc(6f), sc(6f), boxPaint)
        textPaint.color = Color.argb(a(255), 255, 255, 255); textPaint.textSize = sc(10f)
        canvas.drawText(text, x, y + sc(3f), textPaint)
        if (sub.isNotEmpty()) {
            smallText.color = Color.argb(a(160), 160, 160, 160); smallText.textSize = sc(8f)
            canvas.drawText(sub, x, y + sc(16f), smallText)
        }
    }

    private fun drawArrow(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int = 0xFF81C784.toInt()) {
        arrowPaint.color = Color.argb(a(200), Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawLine(x1, y1, x2, y2, arrowPaint)
        val dx = x2 - x1; val dy = y2 - y1; val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return
        val ux = dx / len; val uy = dy / len
        val path = Path(); path.moveTo(x2, y2)
        path.lineTo(x2 - ux * sc(10f) + uy * sc(5f), y2 - uy * sc(10f) - ux * sc(5f))
        path.lineTo(x2 - ux * sc(10f) - uy * sc(5f), y2 - uy * sc(10f) + ux * sc(5f)); path.close()
        arrowFill.color = Color.argb(a(200), Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawPath(path, arrowFill)
    }

    private fun drawTitle(canvas: Canvas, text: String, y: Float, color: Int = 0xFFFF8C00.toInt()) {
        labelPaint.color = Color.argb(a(220), Color.red(color), Color.green(color), Color.blue(color))
        labelPaint.textSize = sc(14f); canvas.drawText(text, cx(0.5f), y, labelPaint)
    }

    private fun drawSubtitle(canvas: Canvas, text: String, y: Float) {
        smallText.color = Color.argb(a(160), 160, 160, 160); smallText.textSize = sc(10f)
        canvas.drawText(text, cx(0.5f), y, smallText)
    }

    private fun drawBilesim(canvas: Canvas) {
        drawTitle(canvas, "Petrolun Bilesimi", cy(0.08f))
        drawSubtitle(canvas, "Fraksiyonel damitma - kaynama noktasina gore ayrim", cy(0.15f))
        data class Frac(val name: String, val pct: Float, val color: Int, val temp: String, val carbon: String)
        val fracs = listOf(
            Frac("Gaz (C1-C4)", 0.18f, 0xFFFF4444.toInt(), "<0 C", "Hafif"),
            Frac("Benzin (C5-C9)", 0.22f, 0xFFFF8C00.toInt(), "40-180 C", "O. agirlik"),
            Frac("Gaz yagi", 0.24f, 0xFFFFD700.toInt(), "180-250 C", "Orta"),
            Frac("Dizel (C14)", 0.16f, 0xFF32CD32.toInt(), "250-350 C", "Agir"),
            Frac("Yakit (C19+)", 0.12f, 0xFF4DD0E1.toInt(), "350-450 C", "Cok agir"),
            Frac("Asfalt (>C25)", 0.08f, 0xFF8B4513.toInt(), ">450 C", "En agir")
        )
        val barW = sc(120f); val barTop = cy(0.22f); val barBot = cy(0.82f); val barH = barBot - barTop
        var curY = barBot
        for (f in fracs) {
            val fh = barH * f.pct; curY -= fh
            drawBar(canvas, cx(0.5f), curY, curY + fh, barW, f.color, "", "")
            textPaint.color = Color.argb(a(255), 255, 255, 255); textPaint.textSize = sc(9f)
            canvas.drawText(f.name, cx(0.5f) - barW / 2 - sc(6f), curY + fh / 2 + sc(3f), textPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(f.temp, cx(0.5f) + barW / 2 + sc(6f), curY + fh / 2 + sc(3f), textPaint.apply { textAlign = Paint.Align.LEFT })
            textPaint.textAlign = Paint.Align.CENTER
        }
        drawSubtitle(canvas, "Karbon sayisi arttikca kaynama noktasi yukselir", cy(0.92f))
    }

    private fun drawAlkanlar(canvas: Canvas) {
        drawTitle(canvas, "Alkanlar - Doymus Hidrokarbonlar", cy(0.08f))
        drawSubtitle(canvas, "CnH2n+2  |  C-C tek bag  |  Doymus", cy(0.15f))
        val data = listOf("CH4" to -161f, "C2H6" to -88f, "C3H8" to -42f, "C4H10" to 0f, "C5H12" to 36f, "C6H14" to 69f, "C7H16" to 98f, "C8H18" to 125f)
        val names = listOf("Metan", "Etan", "Propan", "Butan", "Pentan", "Heksan", "Heptan", "Oktan")
        val maxBP = 150f; val barArea = cy(0.58f); val barBot = cy(0.78f); val barMaxH = barBot - cy(0.22f)
        val n = data.size; val totalW = sc(300f); val barW = totalW / n * 0.6f; val gap = totalW / n
        for (i in data.indices) {
            val (_, bp) = data[i]; val bh = ((bp + 170f) / (maxBP + 170f)) * barMaxH
            val bx = cx(0.5f) - totalW / 2 + i * gap + gap / 2
            val color = when { bp < -50 -> 0xFFFF4444.toInt(); bp < 0 -> 0xFF4DD0E1.toInt(); bp < 50 -> 0xFFFF0080.toInt(); bp < 100 -> 0xFFFFD700.toInt(); else -> 0xFF81C784.toInt() }
            drawBar(canvas, bx, barBot - bh, barBot, barW, color, names[i], "%.0f C".format(bp))
        }
        drawSubtitle(canvas, "Karbon sayisi arttikca van der Waals kuvvetleri artar", cy(0.88f))
        drawSubtitle(canvas, "C1-C4 gaz | C5-C17 sivi | C18+ kati", cy(0.94f))
    }

    private fun drawKraking(canvas: Canvas) {
        drawTitle(canvas, "Kraking (Parcalama)", cy(0.08f))
        drawSubtitle(canvas, "Uzun zincir -> Kisa zincir + alkenler", cy(0.15f))
        val midY = cy(0.35f)
        for (i in 0 until 8) {
            val x = cx(0.5f) - sc(100f) + i * sc(28f)
            barPaint.color = Color.argb(a(200), 0xFF, 0x44, 0x44)
            canvas.drawCircle(x, midY, sc(10f), barPaint)
            smallText.color = Color.argb(a(255), 255, 255, 255); smallText.textSize = sc(8f)
            canvas.drawText("C", x, midY + sc(3f), smallText)
        }
        drawSubtitle(canvas, "Uzun zincir (C20+)", cx(0.5f), midY + sc(22f))
        drawBox(canvas, cx(0.5f), cy(0.50f), sc(140f), sc(28f), 0xFFFFD700.toInt(), "Isi 500 C + Zeolit", "Katalitik kraking")
        drawArrow(canvas, cx(0.5f), cy(0.50f) + sc(18f), cx(0.5f), cy(0.60f))
        for (i in 0 until 5) {
            val x = cx(0.35f) - sc(50f) + i * sc(28f)
            barPaint.color = Color.argb(a(200), 0x39, 0xFF, 0x14)
            canvas.drawCircle(x, cy(0.68f), sc(10f), barPaint)
            smallText.color = Color.argb(a(255), 255, 255, 255); smallText.textSize = sc(8f)
            canvas.drawText("C", x, cy(0.68f) + sc(3f), smallText)
        }
        drawSubtitle(canvas, "Benzin (C5-C10)", cx(0.35f), cy(0.68f) + sc(20f))
        for (i in 0 until 2) {
            val x = cx(0.75f) + i * sc(28f)
            barPaint.color = Color.argb(a(200), 0x00, 0xF0, 0xFF)
            canvas.drawCircle(x, cy(0.68f), sc(10f), barPaint)
            smallText.color = Color.argb(a(255), 255, 255, 255); smallText.textSize = sc(8f)
            canvas.drawText("C", x, cy(0.68f) + sc(3f), smallText)
        }
        drawSubtitle(canvas, "Etilen (C2H4)", cx(0.78f), cy(0.68f) + sc(20f))
        drawSubtitle(canvas, "Kraking: petrokimyanin temeli", cy(0.88f))
        drawSubtitle(canvas, "Termal (700 C) ve katalitik (450 C, zeolit)", cy(0.94f))
    }

    private fun drawReforming(canvas: Canvas) {
        drawTitle(canvas, "Reforming", cy(0.08f))
        drawSubtitle(canvas, "Duz zincir -> Halkali/aromatik", cy(0.15f))
        val leftX = cx(0.25f); val rightX = cx(0.75f); val chainY = cy(0.35f)
        for (i in 0 until 7) {
            val x = leftX - sc(42f) + i * sc(14f)
            barPaint.color = Color.argb(a(200), 0xFF, 0x00, 0x80)
            canvas.drawCircle(x, chainY, sc(8f), barPaint)
        }
        drawSubtitle(canvas, "n-Heptan (OK 0)", leftX, chainY + sc(22f))
        val ringR = sc(30f); val ringAng = 2f * PI.toFloat() / 6f
        for (i in 0 until 6) {
            val ax = rightX + ringR * cos(i * ringAng - PI.toFloat() / 2f)
            val ay = cy(0.38f) + ringR * sin(i * ringAng - PI.toFloat() / 2f)
            barPaint.color = Color.argb(a(200), 0x39, 0xFF, 0x14)
            canvas.drawCircle(ax, ay, sc(8f), barPaint)
        }
        for (i in 0 until 6) {
            val x1 = rightX + ringR * cos(i * ringAng - PI.toFloat() / 2f)
            val y1 = cy(0.38f) + ringR * sin(i * ringAng - PI.toFloat() / 2f)
            val x2 = rightX + ringR * cos((i + 1) * ringAng - PI.toFloat() / 2f)
            val y2 = cy(0.38f) + ringR * sin((i + 1) * ringAng - PI.toFloat() / 2f)
            boxPaint.color = Color.argb(a(150), 0x39, 0xFF, 0x14); boxPaint.strokeWidth = sc(1.5f)
            canvas.drawLine(x1, y1, x2, y2, boxPaint)
        }
        drawSubtitle(canvas, "Benzen (OK 100)", rightX, cy(0.38f) + ringR + sc(20f))
        drawArrow(canvas, cx(0.40f), cy(0.58f), cx(0.60f), cy(0.58f), 0xFFFFD700.toInt())
        drawSubtitle(canvas, "Pt/Re katalizor  |  500 C", cx(0.5f), cy(0.55f))
        drawSubtitle(canvas, "Oktan sayisi artar -> benzin kalitesi yukselir", cy(0.82f))
        drawSubtitle(canvas, "Yan urun: H2 uretimi (yakit hucresi icin onemli)", cy(0.88f))
        drawSubtitle(canvas, "Aromatikler: benzen, toluen, ksilen", cy(0.94f))
    }

    private fun drawPetrokimya(canvas: Canvas) {
        drawTitle(canvas, "Petrokimya Urunleri", cy(0.08f))
        drawSubtitle(canvas, "Ham petrol -> yuzlerce degerli urun", cy(0.15f))
        val items = listOf(
            "Plastik" to "PE, PP, PVC" to 0xFF4DD0E1.toInt(),
            "Benzin" to "Tasit yakiti" to 0xFFFF8C00.toInt(),
            "Asfalt" to "Yol yapimi" to 0xFF8B4513.toInt(),
            "Gubre" to "Tarim" to 0xFF81C784.toInt(),
            "Ilac" to "Saglik" to 0xFFEF4444.toInt(),
            "Polyester" to "Tekstil" to 0xFF8B5CF6.toInt(),
            "Deterjan" to "Temizlik" to 0xFFFFD700.toInt(),
            "Kozmetik" to "Bakim" to 0xFFFF69B4.toInt()
        )
        val cols = 4; val rows = 2; val cellW = sc(85f); val cellH = sc(60f)
        val startX = cx(0.5f) - cols * cellW / 2 + cellW / 2; val startY = cy(0.30f)
        for (i in items.indices) {
            val (name, sub) = items[i].first; val color = items[i].second
            val col = i % cols; val row = i / cols
            val x = startX + col * cellW; val y = startY + row * cellH
            drawBox(canvas, x, y, cellW * 0.85f, cellH * 0.75f, color, name, sub)
        }
        drawSubtitle(canvas, "~90 milyon varil/gun dunya tuketimi", cy(0.82f))
        drawSubtitle(canvas, "Plastik ~250 milyon ton/yil uretim", cy(0.88f))
        drawSubtitle(canvas, "Rafineri: ham petrol -> degerli urunler", cy(0.94f))
    }

    private fun drawEnerji(canvas: Canvas) {
        drawTitle(canvas, "Alternatif Enerji Kaynaklari", cy(0.08f))
        drawSubtitle(canvas, "Petrolun sinirli olmasi alternatifleri dogurdu", cy(0.15f))
        data class Enerji(val name: String, val pct: Float, val color: Int, val desc: String)
        val kaynaklar = listOf(
            Enerji("Gunes", 0.25f, 0xFFFFD700.toInt(), "1000 W/m2"),
            Enerji("Ruzgar", 0.20f, 0xFF4DD0E1.toInt(), "5 MW/turbin"),
            Enerji("Hidro", 0.18f, 0xFF3050F8.toInt(), "500 MW/baraj"),
            Enerji("Biyokutle", 0.15f, 0xFF81C784.toInt(), "Etenol, biyodizel"),
            Enerji("Jeotermal", 0.12f, 0xFFFF4444.toInt(), "Yeralti isisi"),
            Enerji("Nukleer", 0.10f, 0xFFFF69B4.toInt(), "1 GW/reaktor")
        )
        val barW = sc(130f); val barTop = cy(0.22f); val barBot = cy(0.80f); val barH = barBot - barTop
        var curY = barBot
        for (k in kaynaklar) {
            val fh = barH * k.pct; curY -= fh
            drawBar(canvas, cx(0.5f), curY, curY + fh, barW, k.color, "", "")
            textPaint.color = Color.argb(a(255), 255, 255, 255); textPaint.textSize = sc(10f)
            canvas.drawText(k.name, cx(0.5f) - barW / 2 - sc(6f), curY + fh / 2 + sc(4f), textPaint.apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(k.desc, cx(0.5f) + barW / 2 + sc(6f), curY + fh / 2 + sc(4f), textPaint.apply { textAlign = Paint.Align.LEFT })
            textPaint.textAlign = Paint.Align.CENTER
        }
        drawSubtitle(canvas, "Dunya ~170.000 TWh/yil | Petrol ~%31", cy(0.88f))
        drawSubtitle(canvas, "Yenilenebilir enerji %25'e ulasti", cy(0.94f))
    }

    private fun drawCevre(canvas: Canvas) {
        drawTitle(canvas, "Petrol ve Cevre", cy(0.08f))
        drawSubtitle(canvas, "Fosil yakit kullaniminin etkileri", cy(0.15f))
        data class Etki(val name: String, val desc: String, val pct: Float, val color: Int)
        val etkiler = listOf(
            Etki("Sera Gazlari", "CO2, CH4 - kuresel isinma", 0.35f, 0xFFFF4444.toInt()),
            Etki("Hava Kirliligi", "NOx, SOx - asit yagmuru", 0.25f, 0xFFFFA500.toInt()),
            Etki("Su Kirliligi", "Petrol sızıntısı", 0.20f, 0xFF3050F8.toInt()),
            Etki("Toprak Kirliligi", "Plastik 400 yil cozunmez", 0.20f, 0xFF8B4513.toInt())
        )
        val barTop = cy(0.22f); val barBot = cy(0.78f); val totalH = barBot - barTop
        val barW = sc(60f); val gap = sc(80f); val startX = cx(0.5f) - 1.5f * gap
        for (i in etkiler.indices) {
            val e = etkiler[i]; val bh = totalH * e.pct
            val x = startX + i * gap
            drawBar(canvas, x, barBot - bh, barBot, barW, e.color, "", "")
            textPaint.color = Color.argb(a(255), 255, 255, 255); textPaint.textSize = sc(9f)
            canvas.drawText(e.name, x, barBot - bh - sc(8f), textPaint)
            smallText.color = Color.argb(a(150), 150, 150, 150); smallText.textSize = sc(8f)
            canvas.drawText(e.desc, x, barBot + sc(16f), smallText)
        }
        drawSubtitle(canvas, "CO2: 420 ppm | +1.2 C isinma | Deniz +20 cm", cy(0.86f))
        drawSubtitle(canvas, "Paris Anlasmasi: 1.5 C hedefi", cy(0.92f))
    }

    private fun drawSubtitle(canvas: Canvas, text: String, x: Float, y: Float) {
        smallText.color = Color.argb(a(160), 160, 160, 160); smallText.textSize = sc(9f)
        canvas.drawText(text, x, y, smallText)
    }

    override fun onMeasure(wm: Int, hm: Int) { setMeasuredDimension(MeasureSpec.getSize(wm), MeasureSpec.getSize(hm)) }
}

class PetroleumFragment : Fragment() {

    private lateinit var flatView: PetroleumFlatView
    private lateinit var titleTv: TextView
    private lateinit var whatTv: TextView
    private lateinit var howTv: TextView
    private lateinit var useTv: TextView
    private lateinit var examplesTv: TextView
    private lateinit var prosTv: TextView
    private lateinit var consTv: TextView
    private lateinit var propsTv: TextView
    private var currentType = 0

    private data class PType(val name: String, val color: Int)
    private val types = arrayOf(
        PType("Bilesim", 0xFFFF8C00.toInt()),
        PType("Alkanlar", 0xFF4DD0E1.toInt()),
        PType("Kraking", 0xFFEF4444.toInt()),
        PType("Reforming", 0xFF81C784.toInt()),
        PType("Petrokimya", 0xFF8B5CF6.toInt()),
        PType("Enerji", 0xFFFFD700.toInt()),
        PType("Cevre", 0xFF32CD32.toInt())
    )

    private val fullInfo = arrayOf(
        // 0 Bilesim
        arrayOf("Petrolun Bilesimi",
            "Ham petrol, dogal olarak olusan C1-C60+ hidrokarbonlarin karisimidir. Iceriginde alkanlar, sikloalkanlar, aromatikler ve az miktarda kükurt, azot ve oksijen bilesikleri bulunur. Dogal gaz ile birlikte veya bagimsiz yataklarda bulunur.",
            "Fraksiyonel damitma: Ham petrol isitilir ve kaynama noktalarina gore farkli fraksiyonlara ayrilir. Damitma kolonunda yukari dogru sicaklik azalir. Hafif fraksiyonlar ustte, agirlar altta toplanir. Her fraksiyon farkli bir alanda kullanilir.",
            "Enerji uretimi (elektrik santralleri, isinma)\nTasit yakitlari (benzin, dizel, jet yakiti)\nPlastik ve polimer uretimi\nAsfalt ve yol yapimi\nGubre ve tarim kimyasallari\nIlac ve kozmetik uretimi\nTekstil ve sentetik elyaf\nKimyasal solvent ve reaktorler",
            "Ham petrol C1-C60+ | Alkanlar %60-90 | Aromatikler %10-30\nSikloalkanlar %5-15 | Kukurt %0.1-5 | Azot %0.1-2\nYoğunluk: 0.8-0.95 g/cm3 | API gravite: 20-45 derece",
            "Dogal kaynak, yuksek enerji yogunlugu, kolay tasima, cok yonlu kullanilabilirlik, uretim altyapisi gelismis",
            "Fosil yakit, CO2 emisyonu, sinirli kaynak, cevre kirliligi, fiyat dalgalanmasi, jeopolitik bagimlilik",
            "Dunya gunluk: ~90 milyon varil | Kanitlanmis rezerv: ~1.7 trilyon varil\nTuketim: Cin %16, ABD %20, AB %14\nKalan sure: ~50 yil (mevcut hizda)"),

        // 1 Alkanlar
        arrayOf("Alkanlar - Doymus Hidrokarbonlar",
            "Alkanlar, sadece tekli kovalent baglar iceren doymus hidrokarbonlardir. Genel formul CnH2n+2'dir. Cift bag veya halka icermezler. Dogal gaz ve petrolun ana bilesenidirler. Kimyasal olarak göreceli olarak kararlidirlar.",
            "Metan (CH4): Dogal gazin ana bileseni. Isinma degeri yuksek.\nEtan (C2H6): Petrol gazlari icerisinde. Etilen uretimi.\nPropan (C3H8): LPG bileseni. Ev isitmasi.\nButan (C4H10): LPG, lighter yakiti.\nPentan-C8: Sivi yakitlar, solventler. Karbon sayisi arttikca kaynama noktasi yukselir (van der Waals kuvvetleri guclenir).",
            "Isinma yakitlari (dogal gaz, LPG)\nPlastik uretimi (etan -> etilen -> PE)\nSolvent ve temizlik urunleri\nGida paketleme (propan, butan)\nCeket yakiti (lighter, kamp ocagi)\nOtomobil yakiti (benzin, dizel)\nKimyasal hammadde (alkilasyon)\nKozmetik ve iletken",
            "CH4: -161 C | C2H6: -88 C | C3H8: -42 C\nC4H10: 0 C | C5H12: 36 C | C6H14: 69 C\nC7H16: 98 C | C8H18: 125 C\nMetan GLOBAL Isinma Potansiyeli (GWP): 28 kat CO2'den yuksek",
            "Yuksek enerji degeri, temiz yanma (CO2 disinda), cok yonlu hammadde, dogal kaynak olarak bol miktarda",
            "Metan kaçaigi (guclu sera gazi), tasima zorlugu (LNG), patlama riski, CO2 emisyonu, fosil kaynak",
            "C1-C4: gaz | C5-C17: sivi | C18+: kati\nKaynama noktasi: -161 C (CH4) -> +300 C (C20+)\nIsi degeri: 50-55 MJ/kg | Oktan sayisi: degisen"),

        // 2 Kraking
        arrayOf("Kraking (Parcalama)",
            "Kraking, uzun zincirli hidrokarbonlarin (C20+) isitma veya katalizor ile kisa zincir ve alkenlere parcalanmasi prosesidir. Petrokimya endustrisinin en onemli proseslerinden biridir. Benzin uretimi icin de onemlidir.",
            "Termal kraking: 500-700 C sicaklikta yapilir. Basinc altinda uzun zincir parcalanir. Verim dusuktur.\n\nKatalitik kraking: Zeolit (Al2O3/SiO2) katalizoru kullanilir. 450-500 C'de calisir. Daha yuksek verim ve secici. FCC (Fluid Catalytic Cracking) en yaygin yontemdir.\n\nSon urunler: C5-C10 benzin, C2-C4 alkenler (etilen, propilen, butilen).",
            "Benzin uretimi (en buyuk kullanim)\nEtilen uretimi -> PE, PET, PVC\nPropilen uretimi -> PP, akrilik\nButadien -> suni kauçuk\nYuksek oktan benzin katki\nHafif yakitlar uretimi\nPetrol gazlari (LPG)\nKimyasal hammadde tedarigi",
            "C20H42 -> C10H22 + C10H20 (ornek)\nVerim: %70-85 benzin\nKatalizor oncesi/sonrasi: 4 saatte bir rejenerasyon\nFCC kapasitesi: 50.000-100.000 varil/gun\nMaliyet: ~$5-10/varil isleme",
            "Yuksek verim, yuksek benzin kalitesi, esnek urun cesitliligi, otomatize isleme, dusuk enerji tuketimi (katalitik)",
            "Yuksek sicaklik enerji gereksinimi, katalizor cost ve rejenerasyon, yan urun tesis yonetimi, emisyon kontrol",
            "Isi: 500-700 C (termal), 450-500 C (katalitik)\nBasinc: 10-50 atm\nVerim: %70-85 (benzin)\nKatalizor: Zeolit (Al2O3/SiO2)\nKapasite: 50.000-100.000 varil/gun"),

        // 3 Reforming
        arrayOf("Reforming",
            "Reforming, duz zincirli alkanlarin (C6-C8) halkali veya aromatik hidrokarbonlara donusturulmesi prosesidir. Benzin kalitesini artirir (oktan sayisini yukseltir). Yan urun olarak hidrojen gazi uretilir.",
            "n-Heptan (C7H16, OK=0) -> Toluol (C7H8, OK=120) + 4H2\n\nPt/Re (platin-radyum) katalizoru kullanilir. 490-530 C sicaklikta, 5-35 atm basinc altinda calisir. Reaksiyon endotermiktir.\n\nAromatikler: Benzen (C6H6), Toluol (C7H8), Ksilen (C8H10) uretilir. H2 yan urunu onemli (yakit hucresi, amonyak uretimi).",
            "Benzin kalitesini artirma (OK sayisi)\nHidrojen uretimi (yakit hucresi, rafineri)\nAromatik uretimi (benzen, toluen, ksilen)\nSolvent ve kimyasal hammadde\nYakit katkilari (oktan yukseltici)\nIlac ve kozmetik hammaddesi\nPlastik uretimi (PET, polistiren)\nLey yakiti uretimi",
            "n-Heptan: OK 0 -> Benzen: OK 100+\nReaksiyon: endotermik (isi gerektirir)\nKatalizor omru: 1-3 yil\nH2 yan urunu: 3 mol H2/mol heptan\nVerim: %65-80 aromatik",
            "Yuksek OK sayisi, temiz benzin, H2 yan urunu degerli, iyi otomasyon, yuksek kapasite",
            "Pahali Pt/Re katalizor, yuksek sicaklik enerjisi, katalizor zehirlenme riski, aromatik sinirlamalari (saglik)",
            "Sicaklik: 490-530 C | Basinc: 5-35 atm\nKatalizor: Pt/Re (Al2O3 uzerinde)\nOK artisi: 0 -> 100+\nH2 uretimi: 3 mol/mol\nVerim: %65-80 aromatik"),

        // 4 Petrokimya
        arrayOf("Petrokimya Urunleri",
            "Petrokimya, petrol ve dogal gazdan elde edilen hammadelerle uretilen kimyasal urunlerin genel adidir. Plastik, sentetik elyaf, kauçuk, boya, ilac, gubre gibi yuzlerce urun petrokimya kaynaklidir.",
            "Etilen (CH2=CH2): Kraking ile uretilir. PE, PET, PVC, stiren uretimi.\nPropilen (CH3CH=CH2): PP, akrilik, epoksi.\nButadien: Suni kauçuk (SBR, NBR).\nBenzene/Toluen: Plastik, boya, ilac hammaddesi.\nAmonyak (NH3): N2 + H2 -> ure, gubre.\nMetanol: formaldehit, asetik asit.",
            "Ambalaj (plastik sise, film, kapak)\nTekstil (polyester, naylon, akrilik lif)\nOtomobil (tampon, lastik, boy, koltuk)\nInsaat (boru, yalitim, boya)\nElektronik (devre karti, kablo)\nMedikal (enjektor, protez, cerrahi)\nGida (saklama, ambalaj, gida katki)\nTarim (gubre, mulc film, sulama borusu)",
            "PE: 110 milyon ton/yil (en cok uretilen)\nPP: 80 milyon ton/yil\nPVC: 45 milyon ton/yil\nPET: 35 milyon ton/yil\nPS: 30 milyon ton/yil\nToplam dunya plastik: ~400 milyon ton/yil",
            "Yuksek hacim, dusuk maliyet, cok yonlu, hafif ve dayanikli, kolay islenebilir, ozellestirilebilir",
            "Geri donusum zorlugu, dogada cozunme suresi (yuzlerce yil), sera gazi emisyonu, mikroplastik kirliligi",
            "PE: 0.92-0.96 g/cm3 | PP: 0.90 g/cm3\nPVC: 1.30-1.45 g/cm3 | PET: 1.38 g/cm3\nErime: 110-260 C (tipe gore)\nIsi direnci: 60-200 C (tipe gore)"),

        // 5 Enerji
        arrayOf("Alternatif Enerji Kaynaklari",
            "Fosil yakitlarin sinirli olmasi ve cevre uzerindeki etkileri, alternatif enerji kaynaklarina olan ihtiyaci artirmistir. Gunes, ruzgar, hidroelektrik, jeotermal, biyokutle ve nukleer enerji en onemli alternatiflerdir.",
            "Gunes enerjisi: Fotovoltaik paneller ile gunes isigindan elektrik uretimi. Verim %15-22.\nRuzgar enerjisi: Turbinler ile ruzgar kinetik enerjisinden elektrik. 2-10 MW/turbin.\nHidroelektrik: Barajlarda su potansiyel enerjisinden elektrik. En buyuk yenilenebilir.\nJeotermal: Yeralti isisinden elektrik ve isitma.\nBiyokutle: Bitkisel atiklardan etanol/biyodizel.\nNukleer: Uranyum fisyonu ile yuksek enerji.",
            "Elektrik uretimi (santraller)\nIsinma ve sogutma sistemleri\nTasit yakitlari (elektrikli, hibrit)\nEv ve endustriyel enerji\nTarim ve sulama enerjisi\nDeniz suyu aritma enerjisi\nUzay ve uydu enerjisi\nAcil durum guc kaynaklari",
            "Dunya elektrik: ~28.000 TWh/yil\nGunes: ~1000 W/m2 (tepe)\nRuzgar: 3-25 m/s optimal\nHidro: ~3.700 TWh/yil (en buyuk)\nNukleer: ~2.800 TWh/yil\nJeo: ~90 TWh/yil\nBiyokutle: ~650 TWh/yil",
            "Yenilenebilir, dusuk CO2 emisyonu, enerji bagimsizligi, uzun vadeli maliyet avantaji, teknolojik gelisme",
            "Degisken kapasite (gunes/ruzgar), enerji depolama zorlugu, yuksek baslangic yatirimu, arazi ihtiyaci, gorunum etkisi",
            "Gunes panel verimi: %15-22\nRuzgar turbin kapasitesi: 2-10 MW\nHidro baraj omru: 50-100 yil\nNukleer yakit: Uranyum-235\nDunya enerji tuketimi: ~580 EJ/yil"),

        // 6 Cevre
        arrayOf("Petrol ve Cevre",
            "Fosil yakit kullanimi, atmosferdeki CO2 konsantrasyonunu artirmis ve kuresel isinmaya neden olmustur. 1850'lerden bu yana CO2 280 ppm'den 420 ppm'e yukselmistir. Bu durum iklim degisiklikleri, deniz seviyesi yukselmesi ve ekosistem bozulmalarina yol acmistir.",
            "CO2 emisyonu: Fosil yakit yanmasi ile atmosfere salinir. Sera etkisi ile dunya sicakligini artirir.\nMetan (CH4): Dogal gaz kacagi, tarim, hayvancilik. CO2'den 28 kat daha guclu sera gazi.\nNOx/SOx: Asit yagmuru, hava kirliligi, solunum yollari.\nPetrol sizintisi: Deniz ekosistemini yok eder.\nPlastik kirliligi: 400+ yil dogada kalir, mikroplastik olusur.",
            "Paris Anlasmasi: 1.5 C hedefi\nYenilenebilir enerjiye gecis\nElektrikli tasitlar\nKarbon yakalama teknolojisi\nGeri donusum ve sifir atik\nToplu tasima teşviki\nEnerji verimliligi\nBireysel bilinc ve farkindalik",
            "CO2: 420 ppm (sanayi oncesi: 280 ppm)\nGlobal sicaklik: +1.2 C artis\nDeniz seviyesi: +20 cm (1900-2020)\nArctic buz: %13 azalma/yil\nPlastik uretimi: 400 milyon ton/yil\nPlastik cozunme: 400-1000 yil\nPlastik deniz: 150 milyon ton",
            "Yenilenebilir enerjiye gecis hizi artiyor, elektrikli tasit yayginlasma, geri donusum orani yukseliyor, farkindalik artti",
            "Kuresel isinma hizi yeterince yavas degil, politik irade eksikligi, maliyet bariyeri, davranis degisikligi zor, geri donusum altyapisi yetersiz",
            "CO2: 420 ppm | Hedef: <450 ppm (2050)\nSera gazi: +45% (sanayi oncesi)\nDeniz: +20 cm, +25 cm/yil hizi\nArctic: 2050'de yazdon buruz olabilir\nPlastik: 8 milyon ton/yil denize")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_petroleum, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flatView = view.findViewById(R.id.pet_canvas)
        titleTv = view.findViewById(R.id.pet_title)
        whatTv = view.findViewById(R.id.pet_what)
        howTv = view.findViewById(R.id.pet_how)
        useTv = view.findViewById(R.id.pet_use)
        examplesTv = view.findViewById(R.id.pet_examples)
        prosTv = view.findViewById(R.id.pet_pros)
        consTv = view.findViewById(R.id.pet_cons)
        propsTv = view.findViewById(R.id.pet_props)
        val btnRow = view.findViewById<LinearLayout>(R.id.pet_btn_row)

        types.forEachIndexed { idx, t ->
            val btn = TextView(requireContext()).apply {
                text = t.name; textSize = 11f; setTextColor(if (idx == 0) 0xFF0D1219.toInt() else t.color)
                setPadding(26, 12, 26, 12)
                background = resources.getDrawable(if (idx == 0) R.drawable.bg_chip_dark_sel else R.drawable.bg_chip_dark, null)
                isClickable = true; isFocusable = true
                setOnTouchListener { v, e ->
                    when (e.action) {
                        android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(60).start()
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                    }; false
                }
                setOnClickListener { selectType(idx) }
            }
            btnRow.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 6 })
        }
        selectType(0)
    }

    private fun selectType(idx: Int) {
        currentType = idx
        flatView.setType(idx)
        val btnRow = view?.findViewById<LinearLayout>(R.id.pet_btn_row) ?: return
        for (i in 0 until btnRow.childCount) {
            val child = btnRow.getChildAt(i) as? TextView ?: continue
            child.background = resources.getDrawable(if (i == idx) R.drawable.bg_chip_dark_sel else R.drawable.bg_chip_dark, null)
            child.setTextColor(if (i == idx) 0xFF0D1219.toInt() else types[i].color)
        }
        val info = fullInfo[idx]
        titleTv.text = info[0]; titleTv.setTextColor(types[idx].color)
        whatTv.text = info[1]
        howTv.text = info[2]
        useTv.text = info[3]
        examplesTv.text = info[4]
        prosTv.text = info[5]
        consTv.text = info[6]
        propsTv.text = info[7]
    }
}
