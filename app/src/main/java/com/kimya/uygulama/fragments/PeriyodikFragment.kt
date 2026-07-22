package com.kimya.uygulama.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData

class PeriyodikFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val outerScroll = ScrollView(requireContext())
        val main = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(6, 6, 6, 6)
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg))
        }

        val cellSize = dp(42)
        val margin = dp(1)
        val fontSz = 11f

        // Header
        main.addView(TextView(requireContext()).apply {
            text = "PERIYODIK TABLO"; setTextColor(ContextCompat.getColor(requireContext(), R.color.neon_cyan))
            textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 4)
        })

        val hScroll = HorizontalScrollView(requireContext())
        val grid = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val detayKart = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 8, 10, 8)
            setBackgroundColor(Color.argb(30, 57, 255, 20))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, dp(4), 0, 0)
            layoutParams = lp
            visibility = View.GONE
            setOnClickListener { visibility = View.GONE }
        }
        val detay = TextView(requireContext()).apply {
            setTextColor(Color.WHITE); textSize = 13f; setLineSpacing(2f, 1f)
        }
        detayKart.addView(detay)

        fun cellBg(renk: Int) = GradientDrawable().apply {
            setColor(renk); cornerRadius = 3f; setStroke(1, Color.argb(40, 255, 255, 255))
        }

        fun elBtn(s: String, atomNo: Int, renk: Int, click: () -> Unit) = Button(requireContext()).apply {
            val lbl = "$s\n$atomNo"
            text = lbl; setTextColor(Color.WHITE); textSize = if (s.length > 2) 8f else fontSz
            background = cellBg(renk)
            val lp = LinearLayout.LayoutParams(cellSize, cellSize)
            lp.setMargins(margin, margin, margin, margin)
            layoutParams = lp
            setPadding(0, 0, 0, 0)
            setOnClickListener { click() }
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setIncludeFontPadding(false)
            minWidth = 0; minHeight = 0
        }

        fun emptyCell() = Button(requireContext()).apply {
            val lp = LinearLayout.LayoutParams(cellSize, cellSize)
            lp.setMargins(margin, margin, margin, margin)
            layoutParams = lp
            background = null; isEnabled = false; alpha = 0f
        }

        fun grupLabel(text: String) = TextView(requireContext()).apply {
            this.text = text
            setTextColor(Color.argb(120, 200, 200, 200))
            textSize = 8f; gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(cellSize, dp(14))
            lp.setMargins(margin, 0, margin, 0)
            layoutParams = lp
        }

        // Grup numaralari
        val grupRow = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        grupRow.addView(emptyCell())
        for (g in 1..18) grupRow.addView(grupLabel("$g"))
        grid.addView(grupRow)

        for (periyot in 1..10) {
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            // Periyot numarasi
            row.addView(TextView(requireContext()).apply {
                text = "$periyot"; setTextColor(Color.argb(100, 200, 200, 200))
                textSize = 8f; gravity = Gravity.CENTER
                val lp = LinearLayout.LayoutParams(dp(14), cellSize)
                lp.setMargins(0, margin, 0, margin)
                layoutParams = lp
            })

            when (periyot) {
                in 1..7 -> {
                    for (grup in 1..18) {
                        val kv = KimyaData.periyodikVeri[periyot to grup]
                        if (kv != null) {
                            val (semIol, no) = kv
                            val el = KimyaData.elementler[semIol]
                            val renk = if (el != null) KimyaData.elementRengi(el.tur) else Color.GRAY
                            row.addView(elBtn(semIol, no, renk) {
                                detay.text = elementDetay(semIol)
                                detayKart.visibility = View.VISIBLE
                            })
                        } else {
                            row.addView(emptyCell())
                        }
                    }
                }
                9 -> {
                    row.addView(TextView(requireContext()).apply {
                        text = "Lantanitler"; setTextColor(Color.argb(180, 50, 255, 50))
                        textSize = 9f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD)
                        val lp = LinearLayout.LayoutParams(cellSize * 2 + margin * 4, cellSize)
                        lp.setMargins(margin, margin, margin, margin)
                        layoutParams = lp
                    })
                    for ((s, no) in KimyaData.lantanitler) {
                        val el = KimyaData.elementler[s]
                        val renk = if (el != null) KimyaData.elementRengi(el.tur) else Color.GRAY
                        row.addView(elBtn(s, no, renk) {
                            detay.text = elementDetay(s); detayKart.visibility = View.VISIBLE
                        })
                    }
                    for (i in 1..3) row.addView(emptyCell())
                }
                10 -> {
                    row.addView(TextView(requireContext()).apply {
                        text = "Aktinitler"; setTextColor(Color.argb(180, 255, 165, 0))
                        textSize = 9f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD)
                        val lp = LinearLayout.LayoutParams(cellSize * 2 + margin * 4, cellSize)
                        lp.setMargins(margin, margin, margin, margin)
                        layoutParams = lp
                    })
                    for ((s, no) in KimyaData.aktinitler) {
                        val el = KimyaData.elementler[s]
                        val renk = if (el != null) KimyaData.elementRengi(el.tur) else Color.GRAY
                        row.addView(elBtn(s, no, renk) {
                            detay.text = elementDetay(s); detayKart.visibility = View.VISIBLE
                        })
                    }
                    for (i in 1..3) row.addView(emptyCell())
                }
                else -> {}
            }
            grid.addView(row)
        }

        // Legend / Lejant
        val lejant = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, 0)
        }
        val lejantRow1 = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val lejantRow2 = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val items = listOf(
            "Soy Gaz" to 0xFFFF69B4.toInt(), "Ametal" to 0xFF7FFF00.toInt(),
            "Yari Metal" to 0xFFDA70D6.toInt(), "Alkali Metal" to 0xFFFF6347.toInt(),
            "Toprak Alkali" to 0xFFFF8C00.toInt(), "Gecis Metali" to 0xFF00CED1.toInt(),
            "Lantanit" to 0xFF32CD32.toInt(), "Aktinit" to 0xFFFFA500.toInt()
        )
        items.forEachIndexed { idx, (t, r) ->
            val tv = TextView(requireContext()).apply {
                text = t; setTextColor(r); textSize = 11f; setPadding(6, 2, 6, 2)
            }
            if (idx < 4) lejantRow1.addView(tv) else lejantRow2.addView(tv)
        }
        lejant.addView(lejantRow1); lejant.addView(lejantRow2)

        hScroll.addView(grid)
        main.addView(hScroll)
        main.addView(lejant)
        main.addView(detayKart)
        outerScroll.addView(main)
        return outerScroll
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun elementDetay(sembol: String): String {
        val el = KimyaData.elementler[sembol] ?: return "Bulunamadi"
        val grupAdlari = mapOf(
            1 to "Alkali Metal", 2 to "Toprak Alkali", 11 to "Bakir Grubu", 12 to "Cinko Grubu",
            13 to "Bor Grubu", 14 to "Karbon Grubu", 15 to "Azot Grubu",
            16 to "Oksijen Grubu", 17 to "Halojen", 18 to "Soy Gaz"
        )
        val gAd = grupAdlari[el.grup] ?: "Grup ${el.grup}"
        val ozellikKisa = el.ozellik.take(100).let { if (el.ozellik.length > 100) "$it.." else it }
        val kullanimKisa = el.kullanim.take(80).let { if (el.kullanim.length > 80) "$it.." else it }
        return """
[${el.semIol}] ${el.adi}
Z = ${el.atomNo}  |  Kutle = ${el.kutle} g/mol
${el.tur} | $gAd | ${el.durum}
Degerlik: ${el.valans.joinToString(", ")}
Elek: ${el.elektron}

[OZELLIK]
$ozellikKisa

[KULLANIM]
$kullanimKisa
""".trimMargin()
    }
}
