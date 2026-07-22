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
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import com.kimya.uygulama.R
import kotlin.math.*

class BioCanvasView(context: Context) : View(context) {
    private var bioType = 0
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val bP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8B949E.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val tP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val sP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val capP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val cP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B6B6B.toInt(); style = Paint.Style.FILL }
    private val oP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); style = Paint.Style.FILL }
    private val nAt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3050F8.toInt(); style = Paint.Style.FILL }
    private val hP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    private val pP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL }
    private val bdr = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val elT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val highP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val arrowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f) }
    private val arrowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); style = Paint.Style.FILL }
    private val condP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textSize = 0f; textAlign = Paint.Align.CENTER }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334455.toInt(); strokeWidth = 1f }

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    fun setBio(type: Int) { bioType = type; invalidate() }

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
        val c = canvas
        val ar = (w * 0.032f).coerceAtMost(h * 0.045f).coerceAtMost(22f); val sp = ar * 4f; val cx = w / 2f
        tP.textSize = h * 0.055f; sP.textSize = h * 0.035f; capP.textSize = h * 0.04f; highP.textSize = h * 0.04f; elT.textSize = ar * 0.9f

        fun da(x: Float, y: Float, e: String, p: Paint = cP) { c.drawCircle(x, y, ar, p); c.drawCircle(x, y, ar, bdr); elT.textSize = ar * 0.9f; c.drawText(e, x, y + elT.textSize / 3f, elT) }
        fun db(x1: Float, y1: Float, x2: Float, y2: Float, t: Int = 1) { c.drawLine(x1, y1, x2, y2, if (t == 2) bP else bP) }

        when (bioType) {
            0 -> {
                c.drawText("Karbonhidratlar", cx, h * 0.05f, tP)
                c.drawText("Cn(H2O)n - sekerler, nisasta, seluloz", cx, h * 0.10f, sP)
                c.drawText("Monosakkarit: 1 seker | Disakkarit: 2 | Polisakkarit: cok", cx, h * 0.14f, sP)

                val rc = cx - w * 0.15f; val ry = h * 0.38f; val rr = sp * 1.3f
                val pang = 2f * PI.toFloat() / 6f
                val hexPts = (0 until 6).map { i -> Pair(rc + rr * cos(i * pang - PI.toFloat() / 2f), ry + rr * sin(i * pang - PI.toFloat() / 2f)) }
                for (pt in hexPts) da(pt.first, pt.second, "C")
                for (i in 0 until 6) { val n = (i + 1) % 6; db(hexPts[i].first, hexPts[i].second, hexPts[n].first, hexPts[n].second) }
                da(hexPts[0].first - sp * 0.8f, hexPts[0].second + sp * 0.4f, "O", oP)
                da(hexPts[1].first - sp * 0.3f, hexPts[1].second - sp * 0.6f, "OH")
                da(hexPts[2].first + sp * 0.3f, hexPts[2].second - sp * 0.6f, "OH")
                da(hexPts[3].first + sp * 0.8f, hexPts[3].second + sp * 0.1f, "O", oP)
                da(hexPts[4].first + sp * 0.6f, hexPts[4].second + sp * 0.6f, "OH")
                da(hexPts[5].first - sp * 0.3f, hexPts[5].second + sp * 0.6f, "OH")
                db(hexPts[0].first, hexPts[0].second, hexPts[0].first - sp * 0.8f, hexPts[0].second + sp * 0.4f)
                c.drawText("Glukoz (C6H12O6)", rc, ry + rr + sp * 0.5f, capP)
                c.drawText("Kan sekeri, ana enerji", rc, ry + rr + sp, sP)

                c.drawText("Sukroz = Glukoz+Fruktoz", cx + w * 0.18f, h * 0.35f, highP)
                c.drawText("Nisasta: 100-1000 glukoz", cx + w * 0.18f, h * 0.48f, highP)
                c.drawText("Seluloz: bitki duvari", cx + w * 0.18f, h * 0.58f, highP)
                c.drawText("Glikozid bagi", cx + w * 0.18f, h * 0.68f, sP)
                c.drawText("Enerji depo + yapi", cx + w * 0.18f, h * 0.80f, sP)
            }
            1 -> {
                c.drawText("Proteinler", cx, h * 0.05f, tP)
                c.drawText("Amino asit polimerleri - 20 standart aa", cx, h * 0.10f, sP)

                val aa_x = cx - sp * 3f
                da(aa_x, h * 0.28f, "H2N", nAt); db(aa_x, h * 0.28f, aa_x + sp, h * 0.28f); da(aa_x + sp, h * 0.28f, "C")
                da(aa_x + sp, h * 0.28f - sp * 0.8f, "H", hP); da(aa_x + sp, h * 0.28f + sp * 0.8f, "R")
                db(aa_x + sp, h * 0.28f, aa_x + sp * 2f, h * 0.28f); da(aa_x + sp * 2f, h * 0.28f, "C")
                da(aa_x + sp * 2f, h * 0.28f + sp * 0.7f, "O", oP); da(aa_x + sp * 2f, h * 0.28f - sp * 0.8f, "OH")
                c.drawText("Amino asit genel: NH2-C(R)-COOH", cx - sp, h * 0.52f, sP)

                val pepP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                val path = Path(); path.moveTo(w * 0.12f, h * 0.62f)
                for (i in 0 until 6) { path.lineTo(w * 0.12f + i * w * 0.11f, h * 0.62f + sin(i * 0.6f) * 6f) }; c.drawPath(path, pepP)
                c.drawText("-[NH-CO]n- polipeptid zinciri (protein)", cx, h * 0.72f, capP)
                c.drawText("1: aa dizisi | 2: a-heliks, b-tabaka | 3: 3D katlanma | 4: alt birim", cx, h * 0.80f, sP)
                c.drawText("Enzim, antikor, kas, kolajen - 4 kcal/g", cx, h * 0.88f, sP)
            }
            2 -> {
                c.drawText("Yaglar (Lipitler)", cx, h * 0.05f, tP)
                c.drawText("Gliserol + 3 yag asidi = Trigliserit", cx, h * 0.10f, sP)

                val glyY = h * 0.28f; val s5 = sp * 0.8f
                da(cx, glyY, "C"); da(cx - s5, glyY + s5, "C"); da(cx + s5, glyY + s5, "C")
                db(cx, glyY, cx - s5, glyY + s5); db(cx, glyY, cx + s5, glyY + s5)
                da(cx - s5, glyY + s5 + s5, "O", oP); da(cx + s5, glyY + s5 + s5, "O", oP); da(cx, glyY - s5, "O", oP)

                for ((dx, dy) in listOf(cx - s5 to glyY + s5 + s5, cx + s5 to glyY + s5 + s5, cx to glyY - s5)) {
                    for (j in 0 until 3) {
                        val ex = dx + s5 * (j + 1) * if (dx < cx) -1f else 1f
                        da(ex, dy + s5 * 0.3f * j, "C")
                        if (j > 0) db(ex - s5 * if (dx < cx) -1f else 1f, dy + s5 * 0.3f * (j - 1), ex, dy + s5 * 0.3f * j)
                    }
                }
                c.drawText("Gliserol + 3 yag asidi zinciri", cx, h * 0.66f, sP)

                c.drawText("Doymus yag: hayvansal, kati (tereyagi)", cx, h * 0.76f, highP)
                c.drawText("Doymamis yag: bitkisel, sivi (zeytinyagi)", cx, h * 0.82f, capP)
                c.drawText("Hucrre zari, enerji depo (9 kcal/g), hormon", cx, h * 0.89f, sP)
            }
            3 -> {
                c.drawText("Vitaminler", cx, h * 0.05f, tP)
                c.drawText("Organik bilesikler - Eksikliginde hastalik", cx, h * 0.10f, sP)
                c.drawText("Yagda cozunen: A, D, E, K | Suda cozunen: B, C", cx, h * 0.14f, sP)

                data class Vit(val name: String, val source: String, val fn: String, val def: String)
                val vits = listOf(
                    Vit("A", "Havuc ispanak", "Gorme bagisiklik", "Gece korlugu"),
                    Vit("C", "Portakal limon", "Kollajen antioksidan", "Iskorbüt"),
                    Vit("D", "Gunes balik", "Kalsiyum emilimi", "Rasitizm"),
                    Vit("B12", "Et sut yumurta", "Sinir sistemi", "Anemi"),
                    Vit("E", "Findik aycicek", "Hucre koruma", "Noropati"),
                )
                val tableTop = h * 0.20f; val rowH = h * 0.10f; val colW = w * 0.22f
                val headerP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = h * 0.035f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                val dataP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }
                val defP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4444.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }
                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF334455.toInt(); strokeWidth = 1f }

                val hdrs = listOf("Vitamin", "Kaynak", "Gorev", "Eksiklik")
                for ((ci, hdr) in hdrs.withIndex()) { c.drawText(hdr, cx - colW * 1.5f + ci * colW, tableTop - 6f, headerP) }
                for ((ri, vit) in vits.withIndex()) {
                    val y = tableTop + (ri + 1) * rowH
                    c.drawLine(w * 0.08f, y, w * 0.92f, y, linePaint)
                    c.drawText(vit.name, cx - colW * 1.5f, y + rowH * 0.6f, dataP)
                    c.drawText(vit.source, cx - colW * 0.5f, y + rowH * 0.6f, dataP)
                    c.drawText(vit.fn, cx + colW * 0.5f, y + rowH * 0.6f, dataP)
                    c.drawText(vit.def, cx + colW * 1.5f, y + rowH * 0.6f, defP2)
                }
            }
            4 -> {
                c.drawText("DNA / RNA", cx, h * 0.05f, tP)
                c.drawText("Nukleik asitler - Genetik bilgi", cx, h * 0.10f, sP)
                c.drawText("Nukleotid: fosfat + seker + baz (A,T,G,C/U)", cx, h * 0.14f, sP)

                val helP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                val helP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
                val baseP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE }

                val path1 = Path(); val path2 = Path(); val cx1 = w * 0.35f; val cx2 = w * 0.65f
                val topY = h * 0.22f; val botY = h * 0.68f; val steps = 80
                for (i in 0..steps) {
                    val frac = i.toFloat() / steps; val y = topY + (botY - topY) * frac
                    val amp = h * 0.07f; val a1x = cx1 + sin(frac * PI.toFloat() * 5f) * amp
                    val a2x = cx2 + sin(frac * PI.toFloat() * 5f + PI.toFloat()) * amp
                    if (i == 0) { path1.moveTo(a1x, y); path2.moveTo(a2x, y) }
                    else { path1.lineTo(a1x, y); path2.lineTo(a2x, y) }
                }
                c.drawPath(path1, helP); c.drawPath(path2, helP2)

                val ar2 = ar * 0.5f
                for (i in 0 until 6) {
                    val frac = (i + 0.5f) / 6; val y = topY + (botY - topY) * frac
                    val bx1 = cx1 + sin(frac * PI.toFloat() * 5f) * h * 0.07f
                    val bx2 = cx2 + sin(frac * PI.toFloat() * 5f + PI.toFloat()) * h * 0.07f
                    c.drawLine(bx1, y, bx2, y, baseP)
                    c.drawCircle(bx1 + (bx2 - bx1) * 0.3f, y, ar2, hP)
                    c.drawCircle(bx1 + (bx2 - bx1) * 0.7f, y, ar2, nAt)
                }

                c.drawText("DNA: Cift sarmal (Watson-Crick)", cx, h * 0.78f, capP)
                c.drawText("Eslesme: A=T (2 H bagi), G=C (3 H bagi)", cx, h * 0.84f, highP)
                c.drawText("Insan: ~3 milyar bc, ~20.000 gen", cx, h * 0.90f, sP)
                c.drawText("RNA: Tek sarmal, U (urasil), protein sentezi", cx, h * 0.96f, sP)
            }
            5 -> {
                c.drawText("Enzimler", cx, h * 0.05f, tP)
                c.drawText("Biyolojik katalizorler - reaksiyon hizini artirir", cx, h * 0.10f, sP)
                c.drawText("Aktif bolge: substrat baglanir, urune donusur", cx, h * 0.14f, sP)

                val ey = h * 0.28f; val er = sp * 2.2f
                val enzP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); style = Paint.Style.FILL }
                val subP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); style = Paint.Style.FILL }
                c.drawCircle(cx, ey, er, enzP)
                c.drawCircle(cx, ey, er, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF69B4.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f })
                c.drawText("Enzim", cx, ey - er - sp * 0.4f, capP)

                c.drawCircle(cx, ey + sp * 0.3f, ar * 0.7f, bP)
                c.drawText("Aktif", cx, ey + sp * 0.3f + ar * 0.1f, elT)
                c.drawText("bolge", cx, ey + sp * 0.3f + ar * 0.7f, elT)

                da(cx - sp * 0.5f, ey + sp * 1.5f, "S", subP2)
                c.drawText("Substrat", cx - sp * 0.5f, ey + sp * 2.5f, sP)

                c.drawLine(cx - sp * 2f, h * 0.65f, cx + sp * 2f, h * 0.65f, arrowP)
                val ap = Path(); ap.moveTo(cx + sp * 2f, h * 0.65f); ap.lineTo(cx + sp * 2f - 14f, h * 0.65f - 8f); ap.lineTo(cx + sp * 2f - 14f, h * 0.65f + 8f); ap.close()
                c.drawPath(ap, arrowFill)
                c.drawText("Enzim-substrat kompleksi", cx, h * 0.63f, condP)

                da(cx - ar * 0.5f, h * 0.78f, "P", subP2); da(cx + ar * 0.5f, h * 0.78f, "P", subP2)
                c.drawText("Urun", cx, h * 0.86f, capP)
                c.drawText("Enzim degismeden kalir, tekrar kullanilir", cx, h * 0.94f, highP)

                c.drawText("Anahtar-Kilit + Induced Fit modelleri | Aktivasyon enerjisini dusurur", cx, h * 0.04f, sP)
            }
            6 -> {
                c.drawText("Hormonlar", cx, h * 0.05f, tP)
                c.drawText("Kimyasal haberci molekuller - endokrin sistem", cx, h * 0.10f, sP)
                c.drawText("Kan yoluyla tasinir, hedef hucrede etki gosterir", cx, h * 0.14f, sP)

                val horY = h * 0.20f; val colW = w * 0.22f; val rowH = h * 0.10f
                val hdrP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textSize = h * 0.03f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                val dP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }
                val lP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }

                data class Hormon(val name: String, val src: String, val fn: String, val def: String)
                val hormons = listOf(
                    Hormon("Insulin", "Pankreas", "Kan sekerini dusurur", "Diyabet"),
                    Hormon("Adrenalin", "Bobrek ustu", "Savas/kac tepkisi", "Yuksek tansiyon"),
                    Hormon("Testosteron", "Testis", "Erkek ozellikler", "Hipogonadizm"),
                    Hormon("Ostrojen", "Yumurtalik", "Kadin ozellikler", "Menopoz"),
                    Hormon("Tiroksin", "Tiroid", "Metabolizma hizi", "Guatr"),
                )
                val hdrs = listOf("Hormon", "Kaynak", "Gorev", "Eksiklik")
                for ((ci, hdr) in hdrs.withIndex()) { c.drawText(hdr, cx - colW * 1.5f + ci * colW, horY - 6f, hdrP) }
                for ((ri, h) in hormons.withIndex()) {
                    val y = horY + (ri + 1) * rowH
                    c.drawLine(w * 0.08f, y, w * 0.92f, y, lineP)
                    c.drawText(h.name, cx - colW * 1.5f, y + rowH * 0.6f, dP)
                    c.drawText(h.src, cx - colW * 0.5f, y + rowH * 0.6f, dP)
                    c.drawText(h.fn, cx + colW * 0.5f, y + rowH * 0.6f, dP)
                    c.drawText(h.def, cx + colW * 1.5f, y + rowH * 0.6f, lP2)
                }

                c.drawText("Insulin: 51 aa polipeptid | Steroid hormonlar: kolesterolden turetilir", cx, h * 0.82f, highP)
                c.drawText("Geri bildirim mekanizmasi ile kontrol (negatif feedback)", cx, h * 0.88f, sP)
                c.drawText("Hormon bozukluklari: diyabet, guatr, buyume geriligi, kisirlik", cx, h * 0.94f, sP)
            }
            7 -> {
                c.drawText("Metabolizma", cx, h * 0.05f, tP)
                c.drawText("Hucredeki tum kimyasal reaksiyonlarin toplami", cx, h * 0.10f, sP)
                c.drawText("Katabolizma (yikim) + Anabolizma (yapim)", cx, h * 0.14f, sP)

                val metaY = h * 0.20f; val metaH = h * 0.72f
                val metaP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF4444.toInt(); style = Paint.Style.FILL }
                val metaP2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF39FF14.toInt(); style = Paint.Style.FILL }
                val metaLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF555555.toInt(); strokeWidth = 2f; style = Paint.Style.STROKE }
                val metaT = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = h * 0.035f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
                val metaS = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFCCCCCC.toInt(); textSize = h * 0.025f; textAlign = Paint.Align.CENTER }

                c.drawRoundRect(w * 0.06f, metaY, w * 0.46f, metaY + metaH, 16f, 16f, metaP)
                c.drawRoundRect(w * 0.06f, metaY, w * 0.46f, metaY + metaH, 16f, 16f, metaLine)
                c.drawText("Katabolizma", w * 0.26f, metaY + 20f, metaT)
                c.drawText("Bilesik -> Enerji", w * 0.26f, metaY + metaH * 0.12f, metaS)
                c.drawText("Glikoz ->", w * 0.26f, metaY + metaH * 0.22f, metaS)
                c.drawText("Piruvat", w * 0.26f, metaY + metaH * 0.28f, metaS)
                c.drawText("Asetil-CoA ->", w * 0.26f, metaY + metaH * 0.36f, metaS)
                c.drawText("Krebs ->", w * 0.26f, metaY + metaH * 0.44f, metaS)
                c.drawText("Elek. Tasima ->", w * 0.26f, metaY + metaH * 0.52f, metaS)
                c.drawText("ATP (~36 mol)", w * 0.26f, metaY + metaH * 0.60f, metaS)
                c.drawText("Glikoliz: 10 enzimatik adim", w * 0.26f, metaY + metaH * 0.72f, metaS)
                c.drawText("Oksijenli solunum", w * 0.26f, metaY + metaH * 0.82f, metaS)
                c.drawText("(aerobik)", w * 0.26f, metaY + metaH * 0.88f, metaS)

                c.drawRoundRect(w * 0.54f, metaY, w * 0.94f, metaY + metaH, 16f, 16f, metaP2)
                c.drawRoundRect(w * 0.54f, metaY, w * 0.94f, metaY + metaH, 16f, 16f, metaLine)
                c.drawText("Anabolizma", w * 0.74f, metaY + 20f, metaT)
                c.drawText("Enerji -> Bilesik", w * 0.74f, metaY + metaH * 0.12f, metaS)
                c.drawText("Fotosentez:", w * 0.74f, metaY + metaH * 0.22f, metaS)
                c.drawText("CO2 + H2O ->", w * 0.74f, metaY + metaH * 0.30f, metaS)
                c.drawText("Glikoz + O2", w * 0.74f, metaY + metaH * 0.38f, metaS)
                c.drawText("Protein sentezi:", w * 0.74f, metaY + metaH * 0.50f, metaS)
                c.drawText("aa -> polipeptid", w * 0.74f, metaY + metaH * 0.58f, metaS)
                c.drawText("Yag sentezi:", w * 0.74f, metaY + metaH * 0.68f, metaS)
                c.drawText("Gliserol + FA", w * 0.74f, metaY + metaH * 0.76f, metaS)
                c.drawText("ATP gerektirir", w * 0.74f, metaY + metaH * 0.86f, metaS)

                c.drawText("Bazal metabolizma hizi: ~1500-2000 kcal/gun | ATP: hucrenin enerji para birimi", cx, h * 0.96f, highP)
            }
        }
        canvas.restore()
    }
}

class BiomoleculesFragment : Fragment() {
    private lateinit var bioView: BioCanvasView
    private val categories = listOf("Karbonhidrat", "Protein", "Yag", "Vitamin", "DNA/RNA", "Enzim", "Hormon", "Metabolizma")
    private val details = listOf(
        "Karbonhidratlar: Genel formul Cn(H2O)n. Monosakkarit (glukoz C6H12O6, fruktoz), disakkarit (sukroz C12H22O11, laktoz), polisakkarit (nisasta, seluloz, glikojen). Glikozid bagi ile baglanirlar. Enerji kaynagi (4 kcal/g), yapi maddesi (seluloz). Kan sekeri glukoz.",
        "Proteinler: 20 standart amino asidin peptid bagi (-CO-NH-) ile polimerlesmesi. Birincil yapi (aa dizisi), ikincil (a-heliks, b-tabaka), ucuncul (3D katlanma), dorduncul (alt birimler). Enzim, antikor, kas proteini, kolajen. 4 kcal/g.",
        "Yaglar (Lipitler): Gliserol + 3 yag asidi (trigliserit). Ester bagi. Doymus yag: hayvansal, kati. Doymamis yag: bitkisel, sivi. Hucrre zari (fosfolipit), enerji depo (9 kcal/g), hormon (steroid). C18:1 (oleik), C18:2 (linoleik).",
        "Vitaminler: Organik bilesikler, vucut sentezleyemez. Yagda cozunen: A (gorme), D (Ca emilimi), E (antioksidan), K (pıhtilasma). Suda cozunen: B (metabolizma), C (kollajen). Eksiklik: A -> gece korlugu, C -> iskorbüt, D -> rasitizm, B12 -> anemi.",
        "DNA/RNA: Nukleotid: fosfat + seker (deoksiriboz/riboz) + baz (A,T,G,C/U). DNA: cift sarmal, A=T (2 H bagi), G=C (3 H bagi). RNA: tek sarmal, U (urasil). mRNA, tRNA, rRNA. Insan: 3 milyar bc, 20000 gen.",
        "Enzimler: Biyolojik katalizorler (genelde protein). Aktif bolge: substrat baglanir. Anahtar-Kilit ve Induced Fit modelleri. Aktivasyon enerjisini dusurur. Spesifik (bir enzim bir substrat). Orn: Amilaz (nisasta), Lipaz (yag), Proteaz (protein). Kofaktor: vitamin/mineral.",
        "Hormonlar: Endokrin bezlerden salgilanan kimyasal haberciler. Insulin (kan sekeri), Adrenalin (stres), Testosteron/Ostrojen (cinsiyet), Tiroksin (metabolizma), Kortizol (stres). Kan yoluyla tasinir. Negatif feedback ile kontrol. Eksiklik/fazlalik hastaliga yol acar.",
        "Metabolizma: Katabolizma (yikim, enerji uretimi) + Anabolizma (yapim, ATP tuketir). Glikoliz: 10 adim, glukoz -> piruvat. Krebs (sitrik asit) dongusu. Elektron tasima zinciri (36 ATP/glukoz). Fotosentez: CO2 + H2O -> glukoz + O2. BMH: ~1500-2000 kcal/gun."
    )
    private val formulas = listOf(
        "Glukoz C6H12O6 | Seluloz: (C6H10O5)n | Nisasta: ~100-1000 glukoz",
        "Insulin: 51 aa | Kollajen: ~1000 aa | Hemoglobin: 4 alt birim 574 aa",
        "Doymus: C16-C18 | Doymamis: C18:1 oleik, C18:2 linoleik | 9 kcal/g",
        "A: gece korlugu | C: iskorbüt | D: rasitizm | B12: anemi",
        "Insan: 3 milyar bc | DNA: A=T G=C | RNA: Uracil A=U",
        "Amilaz (nisasta) | Lipaz (yag) | Proteaz (protein) | Kofaktor gerektirir",
        "Insulin 51 aa | Adrenalin katekolamin | Steroid hormon kolesterolden",
        "Glikoliz 10 adim | Krebs 8 adim | ETS ~36 ATP | Fotosentez: Calvin"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_biomolecules, container, false)
        val placeholder = v.findViewById<View>(R.id.bio_canvas_placeholder)
        val parent = placeholder.parent as ViewGroup; val idx = parent.indexOfChild(placeholder)
        parent.removeView(placeholder)
        bioView = BioCanvasView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (380 * resources.displayMetrics.density).toInt())
        }
        parent.addView(bioView, idx)

        val btnRow = v.findViewById<LinearLayout>(R.id.bio_btn_row)
        val btnIds = mutableListOf<Button>()
        categories.forEachIndexed { i, name ->
            Button(requireContext()).apply {
                text = name; textSize = 12f; setTextColor(-0x1)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    btnIds.forEach { it.alpha = 0.5f }; alpha = 1f
                    bioView.setBio(i)
                    v.findViewById<TextView>(R.id.bio_title).text = categories[i]
                    v.findViewById<TextView>(R.id.bio_detail).text = details[i]
                    v.findViewById<TextView>(R.id.bio_facts).text = formulas[i]
                }
                btnIds.add(this); btnRow.addView(this)
            }
        }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Biyomolekuller")
                .setMessage("Biyomolekuller, canlilarin yapisinda bulunan buyuk molekullerdir.\n\n" +
                    "Bu bolumde 4 kategori incelenir:\n" +
                    "- Karbonhidratlar: Enerji kaynagi (seker, nisastase)\n" +
                    "- Proteinler: Enzimler, yapi taslari\n" +
                    "- Lipitler: Yaglar, enerji depolari\n" +
                    "- Nukleik Asitler: DNA ve RNA, genetik bilgi tasiticisi\n\n" +
                    "Her kategoride molekullerin yapisi ve ozellikleri gosterilir.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }
}
