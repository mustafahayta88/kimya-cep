package com.kimya.uygulama.features

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.*

class LabSafetyView(context: Context) : View(context) {
    private var selectedIndex = -1
    private var zoomScale = 1f; private var panX = 0f; private var panY = 0f
    private var lastTx = 0f; private var lastTy = 0f; private var tMode = 0
    private val sDetector: ScaleGestureDetector
    private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D1117.toInt(); style = Paint.Style.FILL }
    private val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A1A2E.toInt(); style = Paint.Style.FILL }
    private val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF444444.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val activeStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFAAAAAA.toInt(); textAlign = Paint.Align.CENTER }
    private val symP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFA500.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val ghsP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF0000.toInt(); textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val sectionP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00F0FF.toInt(); textAlign = Paint.Align.LEFT; isFakeBoldText = true }

    // [bgColor, iconColor, GHS_number, symbol, name, description, precaution, emergency]
    private val symbols = listOf(
        listOf(0xFFD32F2F.toInt(), 0xFFFFFFFF.toInt(), "GHS07", "!", "Zararli / Tahris Edici",
            "Ciltte ve gozde tahrise neden olur. Uzun sureli maruziyette saglik sorunlari olusturabilir.",
            "Eldiven ve koruyucu gozluk kullanin. Temas halinde bol suyla yikayin.",
            "Goz temasinda: 15 dk suyla yika. Cilt temasinda: sabunlu suyla yika. Dokumeyi temizle."),
        listOf(0xFFF57C00.toInt(), 0xFFFFFFFF.toInt(), "GHS07", "~", "Tahris Edici (Xi)",
            "Solunum yoluyla tahris edici. Astim, alerjik reaksiyon veya solunum sorunlarina yol acabilir.",
            "Ortam havalandirmasi sart. Tozunu solumayin. Maske kullanilmali.",
            "Solunumda: temiz havaya cik. Belirtiler devam ederse doktora basvur."),
        listOf(0xFF388E3C.toInt(), 0xFFFFFFFF.toInt(), "GHS09", "+", "Cevreye Zararli (N)",
            "Sucul ortamda uzun sureli zararli etki. Ekosisteme ve su kaynaklarina karisabilir.",
            "Ozel atik toplama kovasi kullanin. Sulara ve topraga karismamali. Dokulmeleri hemen temizleyin.",
            "Dokulmeyi emici maddeyle topla, kapali kaba koy. Kanalizasyona dokme."),
        listOf(0xFF1976D2.toInt(), 0xFFFFFFFF.toInt(), "GHS02", "F", "Parlayici (F)",
            "Kolayca ates alir. Havayla patlayici karisim olusturabilir. Statik elektrikle tutusabilir.",
            "Atesten ve isidan uzak tutun. Statik elektriklenmeyi onleyin. Cekmece altinda saklayin.",
            "Yangin: kuru kimyasal toz kullan. Su kullanma. Kabi sogut. Alani bosalt."),
        listOf(0xFF7B1FA2.toInt(), 0xFFFFFFFF.toInt(), "GHS06", "T", "Tokstik (T)",
            "Yutulmasi, solunmasi veya deriden emilmesi halinde olumcul olabilir. Cok dusuk dozlarda bile tehlikeli.",
            "Kapali sistemde calisin. Cift eldiven kullanin. Cekmece icinde kullanin. Yiyecek icmek yasak.",
            "Zehirlenme: 112 acil. Kusdurme. Zehir danisma: 114. Hastaneye basvur."),
        listOf(0xFF5D4037.toInt(), 0xFFFFFFFF.toInt(), "GHS01", "E", "Patlayici (E)",
            "Darbeye, sure tunmeye, elektriksel kivilcima ve isiya karsi hassas. Kontrolsuz patlama riski.",
            "Ozel koruyucu kabin gerekli. Statik elektrik ve isidan uzak tutun. Darbeden koruyun.",
            "Patlama aninda: alani bosalt. Yangin varse uzaktan mudahale. Enkaza dokunma."),
        listOf(0xFF455A64.toInt(), 0xFFFFFFFF.toInt(), "RAD", "%", "Radyoaktif",
            "Iyonalastirici radyasyon yayar. Genetik hasar ve kansere yol acabilir. Birikimli etki.",
            "Kursun kalkan gereklidir. Doz olcer (dozimetre) kullanin. Yiyecek bulundurmayin. Sureyi sinirla.",
            "Kazada: alani bosalt, yetkiliye haber ver. Radyasyon olcumu yap. Kirlenen giysileri cikar."),
        listOf(0xFFC62828.toInt(), 0xFFFFFFFF.toInt(), "GHS05", "R", "Korozif (C)",
            "Metal ve dokulari tahris eder. Asit ve baz icerir. Geri donusumlu olmayan hasar verebilir.",
            "Asit ve bazlardan ayri saklayin. Koruyucu siperlik sart. Paslanmaz malzeme kullanin.",
            "Ciltte: 20 dk suyla yika. Gozde: 15 dk suyla yika. Hemen doktora basvur."),
        listOf(0xFF00838F.toInt(), 0xFFFFFFFF.toInt(), "GHS03", "O", "Oksitleyici (O)",
            "Yanici maddelerle temasta yangina yol acar veya yangini siddetlendirir. Oksijen kaynagi.",
            "Yanici maddelerden ayri saklayin. Isidan ve organik maddelerden uzak tutun. Ayri dolap.",
            "Yanginda: bol suyla sogut. Kucuk yanginda kuru kimyasal. Kaba su sıkma."),
    )

    init { isClickable = true; isFocusable = true
        sDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean { zoomScale *= d.scaleFactor; zoomScale = zoomScale.coerceIn(0.3f, 4f); invalidate(); return true }
        })
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        sDetector.onTouchEvent(e)
        when (e.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> { lastTx = e.x; lastTy = e.y; tMode = 1; return true }
            MotionEvent.ACTION_POINTER_DOWN -> { tMode = 2 }
            MotionEvent.ACTION_MOVE -> { if (tMode == 1 && zoomScale > 1f) { panX += e.x - lastTx; panY += e.y - lastTy }; lastTx = e.x; lastTy = e.y; invalidate() }
            MotionEvent.ACTION_UP -> { if (tMode == 1 && zoomScale <= 1.05f) { val r = hitTest(e.x, e.y); selectedIndex = if (r == selectedIndex) -1 else r; invalidate() }; tMode = 0; return true }
        }
        return true
    }

    private fun hitTest(x: Float, y: Float): Int {
        val w = width.toFloat(); val h = height.toFloat()
        val cols = 3; val rows = 3; val cw = w * 0.28f; val ch = cw * 1.1f
        val startX = w * 0.04f; val startY = h * 0.02f
        for (i in 0 until 9) {
            val col = i % cols; val row = i / cols
            val cx = startX + col * (cw + w * 0.03f) + cw / 2f
            val cy = startY + row * (ch + h * 0.02f) + ch / 2f
            if (abs(x - cx) < cw / 2f && abs(y - cy) < ch / 2f) return i
        }
        return -1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgP)
        canvas.save(); canvas.scale(zoomScale, zoomScale, w / 2f, h / 2f); canvas.translate(panX / zoomScale, panY / zoomScale)
        val cols = 3; val rows = 3; val cw = w * 0.28f; val ch = cw * 1.1f
        val startX = w * 0.04f; val startY = h * 0.02f

        for (i in 0 until 9) {
            val col = i % cols; val row = i / cols
            val sX = startX + col * (cw + w * 0.03f); val sY = startY + row * (ch + h * 0.02f)
            val isActive = i == selectedIndex

            canvas.drawRoundRect(sX, sY, sX + cw, sY + ch, 10f, 10f, cardBg)
            canvas.drawRoundRect(sX, sY, sX + cw, sY + ch, 10f, 10f, if (isActive) activeStroke else cardStroke)

            val iconSize = cw * 0.4f; val iconCx = sX + cw / 2f; val iconCy = sY + ch * 0.4f
            canvas.drawCircle(iconCx, iconCy, iconSize, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = symbols[i][0] as Int; style = Paint.Style.FILL })
            canvas.drawCircle(iconCx, iconCy, iconSize, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f })

            ghsP.textSize = iconSize * 0.25f; ghsP.color = 0xFFFFFFFF.toInt()
            canvas.drawText(symbols[i][2] as String, iconCx, iconCy - iconSize + ghsP.textSize * 1.5f, ghsP)

            symP.textSize = iconSize * 1.0f; symP.color = 0xFFFFFFFF.toInt()
            canvas.drawText(symbols[i][3] as String, iconCx, iconCy + symP.textSize / 3f, symP)

            titleP.textSize = h * 0.02f; titleP.color = 0xFFFFFFFF.toInt()
            canvas.drawText(symbols[i][4] as String, sX + cw / 2f, sY + ch * 0.82f, titleP)
        }

        // Detail area
        val infoY = startY + 3 * (ch + h * 0.02f) + h * 0.01f
        if (selectedIndex >= 0) {
            val s = symbols[selectedIndex]
            sectionP.textSize = h * 0.028f
            canvas.drawText("${s[4]} (${s[2]})", w * 0.05f, infoY, sectionP)

            textP.textSize = h * 0.022f; textP.color = 0xFFFFA500.toInt(); textP.textAlign = Paint.Align.LEFT
            canvas.drawText("□ Tanim:", w * 0.05f, infoY + h * 0.04f, textP)
            textP.textSize = h * 0.02f; textP.color = 0xFFAAAAAA.toInt()
            wrapText(canvas, s[5] as String, w * 0.05f, infoY + h * 0.065f, w * 0.88f, h * 0.03f, textP)

            textP.color = 0xFFFFA500.toInt(); textP.textSize = h * 0.022f
            canvas.drawText("□ Onlem:", w * 0.05f, infoY + h * 0.13f, textP)
            textP.color = 0xFFAAAAAA.toInt(); textP.textSize = h * 0.02f
            wrapText(canvas, s[6] as String, w * 0.05f, infoY + h * 0.155f, w * 0.88f, h * 0.03f, textP)

            textP.color = 0xFFFF0000.toInt(); textP.textSize = h * 0.022f
            canvas.drawText("□ Acil Durum:", w * 0.05f, infoY + h * 0.22f, textP)
            textP.color = 0xFFAAAAAA.toInt(); textP.textSize = h * 0.02f
            wrapText(canvas, s[7] as String, w * 0.05f, infoY + h * 0.245f, w * 0.88f, h * 0.03f, textP)

            textP.textAlign = Paint.Align.CENTER
        } else {
            textP.textSize = h * 0.025f; textP.color = 0xFF666666.toInt(); textP.textAlign = Paint.Align.CENTER
            canvas.drawText("Bir sembole dokunarak guvenlik detayini ve acil durum mudahale bilgisini goruntule", w / 2f, infoY + h * 0.02f, textP)
        }
        canvas.restore()
    }

    private fun wrapText(canvas: Canvas, text: String, x: Float, y: Float, maxW: Float, lineH: Float, paint: Paint) {
        val chars = text.toCharArray(); val sb = StringBuilder(); var lineY = y
        for (c in chars) {
            sb.append(c); val lw = paint.measureText(sb.toString())
            if (lw > maxW) {
                sb.deleteCharAt(sb.length - 1); canvas.drawText(sb.toString(), x, lineY, paint)
                sb.setLength(0); sb.append(c); lineY += lineH
            }
        }
        if (sb.isNotEmpty()) canvas.drawText(sb.toString(), x, lineY, paint)
    }
}

class LabSafetyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val ll = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF0D1117.toInt()) }
        val headerRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(8, 8, 8, 4)
        }
        headerRow.addView(TextView(requireContext()).apply {
            text = "Lab. Guvenlik Sembolleri"; setTextColor(0xFF00F0FF.toInt())
            textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val helpBtnLab = android.widget.Button(requireContext()).apply {
            text = "?"; textSize = 20f; setTextColor(-0x1)
            backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.neon_purp)
            layoutParams = LinearLayout.LayoutParams((40 * resources.displayMetrics.density).toInt(), (40 * resources.displayMetrics.density).toInt())
        }
        headerRow.addView(helpBtnLab)
        ll.addView(headerRow)
        val view = LabSafetyView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (700 * resources.displayMetrics.density).toInt())
        }
        ll.addView(view)
        ll.addView(TextView(requireContext()).apply {
            text = "GHS Laboratuvar Guvenlik Sembolleri | Bir sembole dokun: aciklama, onlem ve acil durum bilgisi"
            setTextColor(0xFFAAAAAA.toInt()); textSize = 12f; gravity = android.view.Gravity.CENTER; setPadding(8, 8, 8, 8)
        })
        helpBtnLab.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Laboratuvar Guvenlik Sembolleri")
                .setMessage("GHS (Kuresel Uyumlu Siniflandirma Sistemi) sembollerini ogrenin.\n\n" +
                    "- Her sembole dokunarak detayli bilgi alabilirsiniz\n" +
                    "- Tehlike turu, onlem ve acil durum bilgileri gosterilir\n" +
                    "- Yanicilik, tokisite, cevreye zararlilik gibi kategoriler\n\n" +
                    "Laboratuvarda calisirken bu sembolleri bilmek cok onemlidir.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return ll
    }
}
