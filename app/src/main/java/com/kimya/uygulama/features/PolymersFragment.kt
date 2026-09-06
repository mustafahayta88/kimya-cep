package com.kimya.uygulama.features

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class PolymerFlatView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {

    private var polType = 0
    private var fadeAlpha = 0f
    private var fadeAnimator: ValueAnimator? = null
    private var breathe = 0f
    private var breatheAnimator: ValueAnimator? = null

    private val bgPaint = Paint().apply { color = 0xFF0D1219.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x0AFFFFFF.toInt(); strokeWidth = 0.5f }
    private val bondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
    private val atomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    private var bgBitmap: Bitmap? = null

    fun setType(t: Int, animate: Boolean = true) {
        polType = t
        if (animate) {
            fadeAnimator?.cancel()
            fadeAlpha = 0f
            fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 400L
                interpolator = DecelerateInterpolator()
                addUpdateListener { fadeAlpha = it.animatedValue as Float; postInvalidate() }
                start()
            }
        } else {
            fadeAlpha = 1f; postInvalidate()
        }
    }

    init {
        isClickable = true
        breatheAnimator = ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
            duration = 6000L; interpolator = LinearInterpolator(); repeatCount = ValueAnimator.INFINITE
            addUpdateListener { breathe = it.animatedValue as Float; postInvalidate() }
            start()
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); breatheAnimator?.start() }
    override fun onDetachedFromWindow() { breatheAnimator?.cancel(); fadeAnimator?.cancel(); bgBitmap?.recycle(); bgBitmap = null; super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        bgBitmap?.recycle()
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        val gs = 40f
        val cols = (w / gs).toInt() + 1; val rows = (h / gs).toInt() + 1
        for (i in 0 until cols) c.drawLine(i * gs, 0f, i * gs, h.toFloat(), gridPaint)
        for (i in 0 until rows) c.drawLine(0f, i * gs, w.toFloat(), i * gs, gridPaint)
        bgBitmap = bmp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bgBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())

        when (polType) {
            0 -> drawAddition(canvas)
            1 -> drawCondensation(canvas)
            2 -> drawThermoplastic(canvas)
            3 -> drawThermoset(canvas)
            4 -> drawElastomer(canvas)
            5 -> drawCopolymer(canvas)
            6 -> drawBiodegradable(canvas)
            7 -> drawConductive(canvas)
            8 -> drawNatural(canvas)
            9 -> drawSmart(canvas)
            10 -> drawNanocomposite(canvas)
        }

        canvas.restore()
    }

    private fun drawAtom(canvas: Canvas, x: Float, y: Float, r: Float, color: Int, label: String = "", alpha: Int = 255) {
        val a = (alpha * fadeAlpha).toInt()
        atomPaint.color = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(x, y, r, atomPaint)
        if (label.isNotEmpty() && r > 8f) {
            labelPaint.color = Color.argb(a, 255, 255, 255)
            labelPaint.textSize = r * 0.9f
            canvas.drawText(label, x, y + r * 0.35f, labelPaint)
        }
    }

    private fun drawBond(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int = 0xFF445566.toInt(), alpha: Int = 255) {
        bondPaint.color = Color.argb((alpha * fadeAlpha).toInt(), Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawLine(x1, y1, x2, y2, bondPaint)
    }

    private fun cx(frac: Float) = width * frac
    private fun cy(frac: Float) = height * frac
    private fun sc(v: Float) = v * width / 400f
    private fun breatheOffset() = sin(breathe) * sc(1.5f)

    private fun drawAddition(canvas: Canvas) {
        val n = 6; val sp = sc(45f); val startX = cx(0.18f); val midY = cy(0.45f)
        val sideColors = intArrayOf(0xFF3B82F6.toInt(), 0xFFEF4444.toInt(), 0xFF3B82F6.toInt(), 0xFFEF4444.toInt(), 0xFF3B82F6.toInt(), 0xFFEF4444.toInt())
        for (i in 0 until n) {
            val x = startX + i * sp; val y = midY + if (i % 2 == 0) -sc(30f) else sc(30f) + breatheOffset()
            if (i > 0) {
                val px = startX + (i - 1) * sp; val py = midY + if ((i - 1) % 2 == 0) -sc(30f) else sc(30f)
                drawBond(canvas, px, py, x, y)
            }
            drawAtom(canvas, x, y, sc(14f), 0xFF8899AA.toInt(), "C")
            if (i % 2 == 1) {
                drawBond(canvas, x, y, x, y - sc(40f), 0xFF445566.toInt())
                drawAtom(canvas, x, y - sc(40f), sc(10f), sideColors[i], "X")
            }
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Katilma Polimerizasyonu", 0xFF4DD0E1.toInt())
    }

    private fun drawCondensation(canvas: Canvas) {
        val n = 8; val sp = sc(38f); val startX = cx(0.12f); val midY = cy(0.45f)
        for (i in 0 until n) {
            val x = startX + i * sp; val y = midY + if (i % 3 == 0) 0f else if (i % 3 == 1) -sc(25f) else sc(25f) + breatheOffset()
            if (i > 0) {
                val px = startX + (i - 1) * sp; val py = midY + if ((i - 1) % 3 == 0) 0f else if ((i - 1) % 3 == 1) -sc(25f) else sc(25f)
                drawBond(canvas, px, py, x, y)
            }
            val isO = i % 3 == 0
            drawAtom(canvas, x, y, if (isO) sc(11f) else sc(13f), if (isO) 0xFFEF4444.toInt() else 0xFF8899AA.toInt(), if (isO) "O" else "C")
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Yogunlasma Polimerizasyonu", 0xFFEF4444.toInt())
    }

    private fun drawThermoplastic(canvas: Canvas) {
        val colors = intArrayOf(0xFF4DD0E1.toInt(), 0xFF81C784.toInt(), 0xFFFF6B35.toInt())
        for (chain in 0 until 3) {
            val n = 5; val sp = sc(40f); val startX = cx(0.15f) + chain * sc(10f)
            val yOff = (chain - 1) * sc(35f)
            for (i in 0 until n) {
                val x = startX + i * sp; val y = yOff + cy(0.45f) + if (i % 2 == 0) -sc(18f) else sc(18f) + breatheOffset() * (chain + 1) * 0.3f
                if (i > 0) {
                    val px = startX + (i - 1) * sp; val py = yOff + cy(0.45f) + if ((i - 1) % 2 == 0) -sc(18f) else sc(18f)
                    drawBond(canvas, px, py, x, y, colors[chain])
                }
                drawAtom(canvas, x, y, sc(10f), colors[chain])
            }
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Termoplastik", 0xFF81C784.toInt())
    }

    private fun drawThermoset(canvas: Canvas) {
        val sp = sc(42f); val n = 5; val startX = cx(0.15f)
        for (row in 0 until 2) {
            val yOff = cy(0.35f) + row * sc(80f)
            for (i in 0 until n) {
                val x = startX + i * sp; val y = yOff + if (i % 2 == 0) -sc(15f) else sc(15f) + breatheOffset()
                if (i > 0) {
                    val px = startX + (i - 1) * sp; val py = yOff + if ((i - 1) % 2 == 0) -sc(15f) else sc(15f)
                    drawBond(canvas, px, py, x, y)
                }
                drawAtom(canvas, x, y, sc(11f), 0xFF8899AA.toInt(), "C")
            }
        }
        for (i in 0 until n - 1) {
            val x = startX + i * sp + sp / 2; val topY = cy(0.35f) + sc(15f); val botY = cy(0.35f) + sc(80f) - sc(15f)
            drawBond(canvas, x, topY, x, botY, 0xFFFBBF24.toInt())
            drawAtom(canvas, x, (topY + botY) / 2, sc(7f), 0xFFFBBF24.toInt(), "+")
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Termoset", 0xFFFF6B35.toInt())
    }

    private fun drawElastomer(canvas: Canvas) {
        val n = 10; val startX = cx(0.2f); val midY = cy(0.45f)
        val springAmpl = sc(35f) + breatheOffset() * 3f
        for (i in 0 until n) {
            val t = i * 0.8f; val x = startX + i * sc(28f)
            val y = midY + sin(t) * springAmpl
            if (i > 0) {
                val px = startX + (i - 1) * sc(28f); val py = midY + sin((i - 1) * 0.8f) * springAmpl
                drawBond(canvas, px, py, x, y, 0xFF8B5CF6.toInt())
            }
            drawAtom(canvas, x, y, sc(9f), 0xFF8899AA.toInt(), "C")
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Elastomer", 0xFF8B5CF6.toInt())
    }

    private fun drawCopolymer(canvas: Canvas) {
        val n = 8; val sp = sc(38f); val startX = cx(0.12f); val midY = cy(0.45f)
        for (i in 0 until n) {
            val x = startX + i * sp; val y = midY + if (i % 2 == 0) -sc(28f) else sc(28f) + breatheOffset()
            if (i > 0) {
                val px = startX + (i - 1) * sp; val py = midY + if ((i - 1) % 2 == 0) -sc(28f) else sc(28f)
                drawBond(canvas, px, py, x, y)
            }
            val isA = (i / 2) % 2 == 0
            drawAtom(canvas, x, y, sc(13f), if (isA) 0xFF4DD0E1.toInt() else 0xFFFF6B35.toInt(), if (isA) "A" else "B")
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Kopolimer", 0xFF06B6D4.toInt())
    }

    private fun drawBiodegradable(canvas: Canvas) {
        val n = 7; val sp = sc(42f); val startX = cx(0.14f); val midY = cy(0.45f)
        for (i in 0 until n) {
            val x = startX + i * sp; val y = midY + if (i % 2 == 0) -sc(25f) else sc(25f) + breatheOffset()
            if (i > 0) {
                val px = startX + (i - 1) * sp; val py = midY + if ((i - 1) % 2 == 0) -sc(25f) else sc(25f)
                drawBond(canvas, px, py, x, y, if (i % 3 == 0) 0xFFEF4444.toInt() else 0xFF445566.toInt())
            }
            drawAtom(canvas, x, y, sc(12f), if (i % 3 == 0) 0xFFEF4444.toInt() else 0xFF8899AA.toInt(), if (i % 3 == 0) "O" else "C")
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Biyobozunur Polimer", 0xFF10B981.toInt())
    }

    private fun drawConductive(canvas: Canvas) {
        val n = 8; val sp = sc(35f); val startX = cx(0.12f); val midY = cy(0.45f)
        for (i in 0 until n) {
            val x = startX + i * sp; val y = midY + if (i % 2 == 0) -sc(22f) else sc(22f) + breatheOffset()
            if (i > 0) {
                val px = startX + (i - 1) * sp; val py = midY + if ((i - 1) % 2 == 0) -sc(22f) else sc(22f)
                drawBond(canvas, px, py, x, y, if (i % 2 == 0) 0xFF4DD0E1.toInt() else 0xFF445566.toInt())
                if (i % 2 == 0) {
                    drawBond(canvas, px, py - sc(5f), x, y - sc(5f), 0xFF4DD0E1.toInt())
                }
            }
            drawAtom(canvas, x, y, sc(12f), if (i % 2 == 0) 0xFF4DD0E1.toInt() else 0xFF8899AA.toInt(), "C")
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Iletken Polimer", 0xFFFBBF24.toInt())
    }

    private fun drawNatural(canvas: Canvas) {
        val rings = 3; val ringR = sc(28f); val ringSpacing = sc(72f)
        val startX = cx(0.5f) - (rings - 1) * ringSpacing / 2; val midY = cy(0.42f)
        for (r in 0 until rings) {
            val cx = startX + r * ringSpacing; val baseIdx = 0
            for (i in 0 until 6) {
                val a1 = i * (Math.PI / 3f).toFloat(); val a2 = (i + 1) * (Math.PI / 3f).toFloat()
                val x1 = cx + cos(a1) * ringR; val y1 = midY + sin(a1) * ringR
                val x2 = cx + cos(a2) * ringR; val y2 = midY + sin(a2) * ringR
                drawBond(canvas, x1, y1, x2, y2, 0xFF81C784.toInt())
                drawAtom(canvas, x1, y1, sc(7f), 0xFF8899AA.toInt(), "")
            }
            if (r > 0) {
                val prevCx = startX + (r - 1) * ringSpacing
                drawBond(canvas, prevCx + ringR, midY, cx - ringR, midY, 0xFF445566.toInt())
            }
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Dogal Polimer", 0xFF22C55E.toInt())
    }

    private fun drawSmart(canvas: Canvas) {
        val n = 9; val startX = cx(0.15f); val midY = cy(0.45f)
        val coilR = sc(30f) + breatheOffset() * 4f
        for (i in 0 until n) {
            val x = startX + i * sc(30f); val y = midY + sin(i * 0.7f) * coilR
            if (i > 0) {
                val px = startX + (i - 1) * sc(30f); val py = midY + sin((i - 1) * 0.7f) * coilR
                drawBond(canvas, px, py, x, y, 0xFF8B5CF6.toInt())
            }
            drawAtom(canvas, x, y, sc(9f), if (i % 3 == 0) 0xFF8B5CF6.toInt() else 0xFF8899AA.toInt())
        }
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Akilli Polimer", 0xFFA855F7.toInt())
    }

    private fun drawNanocomposite(canvas: Canvas) {
        val n = 5; val sp = sc(45f); val startX = cx(0.18f); val midY = cy(0.45f)
        for (i in 0 until n) {
            val x = startX + i * sp; val y = midY + if (i % 2 == 0) -sc(20f) else sc(20f) + breatheOffset()
            if (i > 0) {
                val px = startX + (i - 1) * sp; val py = midY + if ((i - 1) % 2 == 0) -sc(20f) else sc(20f)
                drawBond(canvas, px, py, x, y)
            }
            drawAtom(canvas, x, y, sc(11f), 0xFF8899AA.toInt(), "C")
        }
        val nanoX = cx(0.5f); val nanoY = cy(0.3f)
        atomPaint.color = Color.argb((fadeAlpha * 60f).toInt(), 0xFF, 0x6B, 0x35)
        canvas.drawCircle(nanoX, nanoY, sc(22f) + breatheOffset() * 2f, atomPaint)
        drawAtom(canvas, nanoX, nanoY, sc(14f), 0xFFFF6B35.toInt(), "")
        val nanoX2 = cx(0.35f); val nanoY2 = cy(0.6f)
        atomPaint.color = Color.argb((fadeAlpha * 60f).toInt(), 0x00, 0xF0, 0xFF)
        canvas.drawCircle(nanoX2, nanoY2, sc(18f) + breatheOffset() * 1.5f, atomPaint)
        drawAtom(canvas, nanoX2, nanoY2, sc(11f), 0xFF4DD0E1.toInt(), "")
        drawLabel(canvas, cx(0.5f), cy(0.88f), "Nanokompozit", 0xFFFF4081.toInt())
    }

    private fun drawLabel(canvas: Canvas, x: Float, y: Float, text: String, color: Int) {
        labelPaint.color = Color.argb((fadeAlpha * 200f).toInt(), Color.red(color), Color.green(color), Color.blue(color))
        labelPaint.textSize = sc(13f)
        canvas.drawText(text, x, y, labelPaint)
    }
}

class PolymersFragment : Fragment() {

    private lateinit var flatView: PolymerFlatView
    private lateinit var titleTv: TextView
    private lateinit var whatTv: TextView
    private lateinit var howTv: TextView
    private lateinit var useTv: TextView
    private lateinit var examplesTv: TextView
    private lateinit var prosTv: TextView
    private lateinit var consTv: TextView
    private lateinit var propsTv: TextView
    private var selectedBtn: TextView? = null
    private var currentType = 0

    private data class PType(val name: String, val tag: String, val color: Int)
    private val types = arrayOf(
        PType("Katilma", "KAT", 0xFF4DD0E1.toInt()),
        PType("Yogunlasma", "YOG", 0xFFEF4444.toInt()),
        PType("Termoplastik", "TER", 0xFF81C784.toInt()),
        PType("Termoset", "TSM", 0xFFFF6B35.toInt()),
        PType("Elastomer", "ELA", 0xFF8B5CF6.toInt()),
        PType("Kopolimer", "KOP", 0xFF06B6D4.toInt()),
        PType("Biyobozunur", "BIO", 0xFF10B981.toInt()),
        PType("Iletken", "ILE", 0xFFFBBF24.toInt()),
        PType("Dogal", "DOG", 0xFF22C55E.toInt()),
        PType("Akilli", "AKI", 0xFFA855F7.toInt()),
        PType("Nanokompozit", "NANO", 0xFFFF4081.toInt())
    )

    private val fullInfo = arrayOf(
        // 0 Katilma
        arrayOf("Katilma Polimerizasyonu",
            "Monomerler uzerindeki cift bag (C=C) acilir ve monomerler arka arkaya birleserek uzun polimer zinciri olusturur. Her adimda yeni bir monomer eklenir ve reaksiyon serbest radikal, iyon veya koordinasyon mekanizmasiyla devam eder.",
            "Serbest radikal polimerizasyon: Initiator (BPO, APS) isinla veya kimyasal olarak parcalanarak serbest radikal uretir. Radikal, monomer cift bagini acar ve zincir buyur. Inisiyasyon, propagasyon ve sonlanma asamalari vardir.\n\nIyonik polimerizasyon: Kationik veya anyonik initiators ile calisir. Daha duzgun zincir yapisi saglar.\n\nKoordinasyon polimerizasyonu: Ziegler-Natta katalizoturleri ile yuksek selektiviteli polimerizasyon yapilir.",
            "Plastik sise ve kaplar (PE, PET)\nBoru ve kablo pencesi (PVC, PE)\nOtomobil parcalari (PP, ABS)\nTekstil lifleri (PAM, PES)\nAmbalaj filmleri (PE, PP, PS)\nTibbi malzemeler (PMMA, PTFE)\nYalitim malzemeleri (PS, PU)\nElektronik gonunekler (PC, ABS)",
            "PE (Polietilen), PP (Polipropilen), PVC (Poli vinyl klorur), PS (Polistiren), PMMA (Poli metil metakrilat), PTFE (Teflon), PAM (Poliamid), PVDC (Saran)",
            "Yuksek verim, hizli reaksiyon, cesitli monomer secenekleri, uretim maliyeti dusuk, ozellestirilebilir molekuler agirlik",
            "Isi ve isiga duyarlilik, some degrade olabilir, geri donusum zor, bazen toksik monomerler icerir",
            "Molekuler agirlik: 10.000-1.000.000 g/mol\nErime sicakligi: 110-230 C (tipe gore)\nYoğunluk: 0.9-1.4 g/cm3\nIsi direnci: 60-200 C arasi"),

        // 1 Yogunlasma
        arrayOf("Yogunlasma Polimerizasyonu",
            "Iki veya daha fazla monomer, aralarinda su veya kucuk bir molekul cikararak (ester, amid, eter bagi) birlesir. Kondansasyon (yogunlasma) reaksiyonuyla polimer olusur. Genellikle bifonksiyonel veya multifonksiyonel monomerler kullanilir.",
            "Difonksiyonel monomerler (diol + dikarboksilik asit veya diamin + dikarboksilik asit) isitilir. Her adimda su molekulu cikar ve yeni kovalent bag kurulur. Reaksiyon yavastadir ve denge reaksiyonudur. Fischer-Specker, Staudinger reaksiyonlari orneklerdir.\n\nUrethank olusturma: Diisosiyanat + diol -> poliuretan\nEster baglasma: Diol + asit anhidrit -> poliester",
            "Tekstil ve kiyafet (nailon, polyester)\nIc giyim ve spor giyim\nMobilya ve yalisitim (PU)\nOtomobil lastigi ve yedek parcasi\nSaglik malzemesi (dikiş ipi, damar protezi)\nMutfak esyalari (teflon kaplama)\nYapi malzemesi (epoksi, poliester reçine)\nAmbalaj (PET sise)",
            "Nailon-6,6 (PA66), Nailon-6 (PA6), PET (Polietilen tereftalat), PBT, PC (Policarbonat), PU (Poliuretan), Epoxy, Melamin formaldehit",
            "Yuksek mekanik dayanim, iyi kimyasal direnc, yuksek sicaklik dayanimi, iyi yuzey gorunumu, dayanikli",
            "Yuksek uretim maliyeti, bazen yuksek isil islem gerektirir, su absorpsiyonu yuksek olabilir, UV'e duyarlilik",
            "Molekuler agirlik: 15.000-50.000 g/mol\nErime sicakligi: 200-300 C\nYuksek çekme dayanimi: 50-90 MPa\nIsi direnci: 100-250 C arasi"),

        // 2 Termoplastik
        arrayOf("Termoplastik Polimer",
            "Isi ile yumusan, sogukta sertlesen polimerler. Tekrar isitilarak sekillendirilebilir. Molekuller arasi zayif van der Waals baglari vardir, isi ile bu baglar kopar ve zincirler kayar.",
            "Monomerlerden sentezlenir (katilma veya yogunlasma). Son urun granul veya toz seklinde uretilir. Isil islem (ekstruzyon, enjeksiyon, blow molding) ile seklendirilir. Isitma-sogutma dongusu tekrarlanabilir.",
            "Plastik sise ve kapak (PE, PP)\nPencere profil ve boru (PVC)\nOyun ve ev aleti (PS, ABS)\nBilgisayar kasasi ve telefون (ABS, PC)\nMutfak esyalari (PP, PC)\nOtomobil tampon ve panel (PP, ABS)\nYalitim kopuğü (PS, PU)\nMedikal enjektor (PP, PE)",
            "PE, PP, PVC, PS, ABS, PC, PMMA, PA, POM, PBT",
            "Tekrar isitilabilir, geri donusum mumkun, dusuk maliyet, genis kullanim alani, hizli uretim",
            "Yuksek sicaklikta bozulma, bazen kopupma, UV bozulmasi, kimyasal salinim riski",
            "Isitma sicakligi: 150-300 C\nYogunluk: 0.9-1.4 g/cm3\nCekme dayanimi: 20-70 MPa\nKopma uzamasi: %5-500 arasi"),

        // 3 Termoset
        arrayOf("Termoset Polimer",
            "Isi ile katilasan ve geri donusumsuz olarak katilesan polimerler. Cross-link (cagbulma) yapisi sayesinde bir kez sekillendirildikten sonra tekrar yumusamaz. Cok guclu ve dayaniklidir.",
            "Bifonksiyonel monomerler veya oligomerler + cross-linker karistirilir. Isitildiginde veya katalizor ile tetiklendiginde ag baglanmasi baslar. Jelleşme ve katilasma gercekleşir. Uretim genellikle kalip ile yapilir.",
            "Elektrik yalitimi (transformator, priz)\nUcak ve arac paneli (epoksi, fenolik)\nTencere ve tava kaplamasi (melamin)\nCocuk oyunaklari (melamin)\nTikis materiali (bakalit)\nYapi yapistirici (epoksi)\nSpor aletleri (PU reçine)\nYalitim kopugu (PU, fenolik)",
            "Fenol-Formaldehit (Bakalit), Melamin-Formaldehit, Epoksi, Urea-Formaldehit, DAP, Alkid reçine",
            "Cok yuksek mekanik dayanim, yuksek sicaklik direnci, iyi elektrik yalitimi, kimyasal direnc, boyutsal kararlilik",
            "Geri donusumu cok zor, kirilgan olabilir, isil genlesme, sert ve kiri",
            "Sertlik: Shore D 85-95\nIsi direnci: 150-300 C\nCekme dayanimi: 30-80 MPa\nDielektrik sabiti: 3-7"),

        // 4 Elastomer
        arrayOf("Elastomer Polimer",
            "Esnek, uzayabilir ve geri donuslu sekilde deforme olabilen polimerler. Yuksek uzama orani (%500+) ve dusuk elastik modul ile karakterize edilir. Kauçuk ve lastik malzemeler bu gruba girer.",
            "Uzun zincirli monomerler (izopren, butadien, siloxan) vulkanizasyon ile cagbulma (cross-link) yapisi kazandirilir. Kukla tuzu (sulfur) veya peroksit ile cagbulma yapilir. Cagbulma derecesi esnekligi belirler.",
            "Otomobil lastigi ve kamyon lastigi\nMedikal eldiven ve maskeler\nAyakkabi tabani ve spor ayakkabisi\nYalitim ve soku emici\nHortum ve kayis\nCocuk emziği ve oyuncak\nSpor topu ve uyku maskesi\nVibrasyon soku",
            "Silikon (PDMS), Buna-S (SBR), Neopren (CR), Buna-N (NBR), Poliizopren (PI), PU elastomer, EPDM, Fluoroelastomer",
            "Cok yuksek esneklik, iyi soku emme, genis sicaklik araligi, su ve hava direnci, yasam suresi uzun",
            "Dusuk mekanik dayanim, yavas deformasyon, bazen alerji yapar, sertlesme (aging) riski",
            "Uzama: %300-1000\nYuksek esneklik modulu: 1-10 MPa\nIsi direnci: -60 C ile 300 C arasi\nSertlik: Shore A 30-90"),

        // 5 Kopolimer
        arrayOf("Kopolimer",
            "Farkli turlu monomerlerden olusan polimerler. Monomerlerin siralamasina gore: rastgele, blok, alternans veya graf copolimerleri seklinde siniflandirilir. Tek monomerin yapamayacagi ozellikler kazandirir.",
            "Iki veya daha fazla farkli monomer ayni reaktorde katilma polimerizasyonu ile birlestirilir. Monomer oranlari, siralamasi ve reaksiyon kosullari (sicaklik, initiator miktarı) urunun yapisini belirler. Emulzyon, suspansiyon veya kokule polimerizasyon kullanilir.",
            "Otomobil tampon ve clapak (ABS)\nAyakkabi tabani (SBR)\nSpor ayakkabi (EVA)\nHortum ve boru (EPDM)\nBuzdolabi ve camlik (SAN)\nAmbalaj filmleri (EVA, EVOH)\nYapi yapistirici (PUR)\nOyun ve eglence (hiperlastik)",
            "ABS (Akrilonitril-butadien-stiren), SBR (Stiren-butadien), EVA (Etilen-vinil asetat), EPDM, SAN, SBS, SEBS, MBS",
            "Ozellestirilebilir ozellikler, dengeli maliyet-performans, genis kullanilabilirlik, iyi islemesi, kararlilik",
            "Karmaşık uretim, kalite kontrol zorlugu, bazi kombinasyonlar uyumsuz, maliyet yuksek olabilir",
            "Molekuler agirlik: 50.000-500.000 g/mol\nErime sicakligi: 100-250 C (tipe gore)\nCekme dayanimi: 15-60 MPa\nIsi direnci: 80-200 C arasi"),

        // 6 Biyobozunur
        arrayOf("Biyobozunur Polimer",
            "Dogada mikroorganizmalar (bakteri, mantar) veya fiziksel etkenler (su, isik, isı) ile parcalanabilen polimerler. Yenilenebilir kaynaklardan uretilir ve cevre dostu alternatifler sunar.",
            "Yenilenebilir kaynaklar (mısır nisastasi, kamis, patates, soya) veya mikrobiyal fermantasyon ile uretilir. Polilaktik asit (PLA) fermentasyon ile laktik asit uretilir, sonra polimerizasyon yapilir. PHA bakteri icinde dogal olarak uretilir.",
            "Tek kullanımlık ambalaj ve bardak (PLA)\nMutfak cogatalar ve pipet (PLA)\nCicek saksısı ve torba (biyobozunur)\nMedikal dikiş ipi ve implant (PLA, PGA)\nTarim mulc film (PBS, PLA)\nOyuncak ve ev esyasi (PHA)\nGida ambalajı (PBS, PLA)\nKozmetik kapak (PLA)",
            "PLA (Polilaktik asit), PHA (Polihidroksi alkanoat), PBS (Polibutilen s succinat), PCL (Polikaprolakton), Nisasta bazli polimer, Selüloz bazli (selüloz asetat)",
            "Yenilenebilir kaynak, dogada cozunebilir, dusuk karbon ayak izi, tibbi uygulamalar icin uygun, gida ile temas guvenli",
            "Yuksek maliyet, dusuk sicaklik direnci, sinirli mekanik ozellikler, yogun depolama kosullari, uretim kapasitesi sinirli",
            "Erime sicakligi: 150-180 C (PLA)\nBiyobozunma: 3-12 ay (kosullara gore)\nCekme dayanimi: 25-50 MPa\nIsi direnci: 50-80 C (PLA icin)"),

        // 7 Iletken
        arrayOf("Iletken Polimer",
            "Konjuglu (birbirini izleyen cift-tekli bag) yapisi sayesinde elektron tasima yetenegine sahip organik polimerler. Nobel odulu kazanmis alan: Heeger, MacDiarmid, Shirakawa (2000). Metale benzer elektriksel ozellikler gosterebilir.",
            "Anilin, pirrol, tiyofen gibi monomerler elektrokimyasal polimerizasyon veya kimyasal oksidasyon ile sentezlenir. Doping (iodin, FeCl3) ile elektriksel iletkenlik artirilir. Dopant turu ve miktarı ile iletkenlik 10^-10 - 10^5 S/cm arasinda ayarlanabilir.",
            "OLED ekran ve aydinlatma (PANI, PEDOT)\nGunes hucresi ve fotovoltaik (PTh, PPV)\nKoruyucu elektrostatik kaplama (PANI)\nHassas gaz ve kimyasal sensor (PANI, PPy)\nBiyouyumlu tibbi sensor (PPy)\nSupercapacitor ve batarya (PANI)\nRadar gorunmezlik kaplamasi\nAntistatik ambalaj (PANI, PPy)",
            "PANI (Polyanilin), PPy (Polipirrol), PEDOT:PSS, PTh (Politiyofen), PPV (Poliparafenilen vinilen)",
            "Hafif ve esnek, kolay islenebilir, ozellestirilebilir iletkenlik, ucuz hammadde, yuzey kaplama kolay",
            "Hava ve nem duyarliligi, isil kararsizlik, yavas degradasyon, isleme zorlugu, dukuk iletkenlik vs metallere gore",
            "Iletkenlik: 10^-5 - 10^3 S/cm (doping'e gore)\nBant araligi: 1.5-3.0 eV\nIsi kararsizligi: >200 C'de bozulma\nOpaklik: saydam-yarı saydam"),

        // 8 Dogal
        arrayOf("Dogal Polimer",
            "Dogada bulunan veya canli organizmalar tarafindan uretilen polimerler. Binlerce yildir kullanilir. Seluloz, protein, nukleik asitler, kauçuk ve nişasta bu gruba girer. Biyouyumlu ve genellikle yenilenebilirdir.",
            "Bitkiler selulozu fotosentez ile uretir (hucre duvari). Hayvanlar kolajeni ve keratini sentezler. Bakteriler PHA uretir. Islatma, kurutma, tuglastirma veya kimyasal isleme ile ham polimerlerden urunler elde edilir.",
            "Kagit ve karton (seluloz)\nTekstil pamuk, keten, ipek (seluloz, protein)\nYapi kerestesi ve mobilya (seluloz)\nYemek ve gida (nisasta, protein)\nIlac ve kozmetik (seluloz, kitin)\nBiyomedikal (kolajen, kitosan)\nKauçuk eldiven ve balon (dogal kauçuk)\nBiyosorban (kitin, selüloz)",
            "Seluloz, Lignin, Nisasta, Kolajen, Keratin, Kitin, Dogal Kauçuk (NR), Silk (Ipek), DNA, RNA",
            "Yenilenebilir, biyouyumlu, dogada cozunebilir, yuksek mekanik ozellik (seluloz), genis kullanilabilirlik",
            "Biyolojik bozunma hizi degisken, basit isleme ile sinirli ozellikler, hava ve nemden etkilenme, mikrobiyal bozulma riski",
            "Seluloz catlama dayanimi: 100-400 MPa\nKolajen elastikiyet: %10-15\nKauçuk uzama: %500-800\nIsi direnci: 100-250 C (seluloz)"),

        // 9 Akilli
        arrayOf("Akilli Polimer",
            "Dis uyaranlara (sicaklik, pH, isik, manyetik alan, kimyasal sinyal) gore yapisini veya ozelligini degistiren polimerler. Stimuli-responsive veya adaptive polimer olarak da bilinir. Bilimsel arastirmalarda ve yukselen teknolojilerde onemli rol oynar.",
            "Akilli monomerler (NIPAM, akrilik asit, aminometakrilat) katilma polimerizasyonu ile sentezlenir. LCST (dusuk cozunurluk sicakligi) veya UCST (yuksek cozunurluk sicakligi) noktasi tasarlanir. Cross-link yapisi ile hidrojel seklinda uretilir.",
            "Hedefli ilac tasima (kanser tedavisi)\nDoku muhendisligi ve hucre kulturu\nAkıllı/yuzenfiltre ve su aritma\nHassas sensor ve aktuator\nKozmetik ve cilt bakımı (nemlendirici)\nYapi malzemesi (akıllı boya)\nTekstil (sicaklik regule eden kumaş)\nGida ambalaj (tazelik gostergesi)",
            "PNIPAM (Polii-N-isopropil akrilamid), PAA (Poliasetik asit), PVA (Polivinil alkol), PEO-PPO blok kopolimer, Kolajen hidrojel, Kitosan hidrojel",
            "Uyarana tepki, yenilikci uygulamalar, biyouyumlu formlar, hassas kontrol, ozellestirilebilir yanit",
            "Yuksek maliyet, sinirli olcek, kararsizlik, tekrarlanabilirlik zorlugu, toksik monomer riski",
            "LCST (PNIPAM): 32 C\nSwelling orani: 10-100 kat\nTepki suresi: saniye-dakika arasi\nMekanik dayanim: 1-100 kPa"),

        // 10 Nanokompozit
        arrayOf("Nanokompozit Polimer",
            "Polimer matriks icerisine nano olcekli (1-100 nm) dolgu maddeleri yerlestirilerek olusturulan kompozit malzemeler. Yuksek yuzey alani/hacim orani sayesinde az miktarda dolgu ile buyuk ozellik artislar saglanir.",
            "Nanopartikuller (kil, CNT, grafen, nano SiO2) polimer matriks ile eritme, cozelti karistirma veya in-situ polimerizasyon ile birlestirilir. Dagilim ve yuzey etkilesimi (fonsiyonellesendirme) kritiktir. Ultrasonik dispersiyon veya yüksek hizli karistirma kullanilir.",
            "Otomobil gosussu ve motor parcasi (nanokil takviye)\nUcak ve havacilik parcası (CNT kompozit)\nCep telefonu kasasi (nano SiO2, grafen)\nSpor aletleri (grafen kompozit)\nInsaat malzemesi (nano sement)\nMedikal implant ve protez (hidroapatit nano)\nGida ambalajı (nano kil barrier)\nElektronik devre kartı (grafen, CNT)",
            "Nanokil (Montmorillonit), CNT (Karbon nanotup), Grafen, Nano SiO2 (silika), Nano TiO2, Nano ZnO, Nanocellulose, Nanokalsit",
            "Cuk yuksek mekanik dayanim, hafiflik, barik ozellikler, isal kararlik, ozel ozellikler (elektrik, manyetik)",
            "Yuksek uretim maliyeti, dagilim zorlugu, agglomerasyon riski, olcek buyutme guclugu, toksite endiseleri",
            "Cogaltma modulu: %20-500 artis\nsicaklik dayanimi: %30-100 artis\nO2 bariyer: %50-90 azalma\nMekanik dayanim: %10-300 artis")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_polymers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flatView = view.findViewById(R.id.pol_canvas_view) as PolymerFlatView
        titleTv = view.findViewById(R.id.pol_title)
        whatTv = view.findViewById(R.id.pol_what)
        howTv = view.findViewById(R.id.pol_how)
        useTv = view.findViewById(R.id.pol_use)
        examplesTv = view.findViewById(R.id.pol_examples)
        prosTv = view.findViewById(R.id.pol_pros)
        consTv = view.findViewById(R.id.pol_cons)
        propsTv = view.findViewById(R.id.pol_props)
        val btnRow = view.findViewById<LinearLayout>(R.id.pol_btn_row)

        types.forEachIndexed { idx, t ->
            val btn = TextView(requireContext()).apply {
                text = t.tag; textSize = 11f; setTextColor(if (idx == 0) 0xFF0D1219.toInt() else t.color)
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
        val btnRow = view?.findViewById<LinearLayout>(R.id.pol_btn_row) ?: return
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
