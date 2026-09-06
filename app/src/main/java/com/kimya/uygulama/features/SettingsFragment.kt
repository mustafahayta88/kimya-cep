package com.kimya.uygulama.features

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AboutDialog
import com.kimya.uygulama.utils.ThemeManager
import com.kimya.uygulama.viewmodel.KimyaViewModel

class SettingsFragment : Fragment() {

    private val vm: KimyaViewModel by activityViewModels()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(attrColor(android.R.attr.colorBackground))
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        root.addView(box, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        box.addView(TextView(context).apply {
            text = "AYARLAR"; setTextColor(attrColor(com.google.android.material.R.attr.colorPrimary)); textSize = 18f
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(10))
        })

        box.addView(sectionHead("GENEL"))
        box.addView(settingRow(
            "Ara", "Araç, element, bileşik bul",
            { "" },
            { openSearch() }
        ))
        box.addView(settingRow(
            "Yardım", "Uygulama kullanım kılavuzu",
            { "" },
            { showAppHelp() }
        ))
        box.addView(sectionHead("VERİ"))
        box.addView(settingRow(
            "Geçmiş", "İşlem kayıtlarını görüntüle",
            { "" },
            {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fragment_slide_up,
                        R.anim.fragment_fade_out,
                        R.anim.fragment_fade_in,
                        R.anim.fragment_slide_down
                    )
                    .replace(R.id.container, HistoryFragment())
                    .addToBackStack(null)
                    .commit()
            }
        ))

        box.addView(settingRow(
            "Tema", "Uygulama görünümü",
            { "Tema: ${ThemeManager.getSelectedTheme(requireContext()).label}" },
            { showThemePicker() }
        ))
        box.addView(settingRow(
            "Geçmişi Temizle", "Tüm işlem kayıtlarını sil",
            { "Kayıt: ${vm.history.value.size} öğe" },
            { confirmClearHistory() }
        ))
        box.addView(settingRow(
            "Son Kullanılanlar", "Ana ekrandaki kısayolları temizle",
            { "" },
            { clearRecents() }
        ))
        box.addView(sectionHead("UYGULAMA"))
        box.addView(settingRow(
            "Hakkında", "Sürüm ve geliştirici bilgisi",
            { "v${AboutDialog.appVersion(requireContext())}" },
            { AboutDialog.show(requireContext()) }
        ))

        return root
    }

    private fun attrColor(attr: Int): Int {
        val tv = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    private fun sectionHead(t: String): TextView {
        return TextView(context).apply {
            text = t
            setTextColor(attrColor(com.google.android.material.R.attr.colorPrimary))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(14), 0, dp(6))
        }.also { it.letterSpacing = 0.1f }
    }

    private fun rowIcon(title: String): String {
        return mapOf(
            "Yardım" to "📖", "Ara" to "🔍", "Tema" to "🎨",
            "Geçmiş" to "🕘", "Geçmişi Temizle" to "🧹",
            "Son Kullanılanlar" to "⭐", "Hakkında" to "ℹ️"
        )[title] ?: "⚙️"
    }

    private fun settingRow(
        title: String, desc: String,
        value: () -> String, act: () -> Unit
    ): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(attrColor(com.google.android.material.R.attr.colorSurface))
            }
            elevation = dp(1).toFloat()
        }
        val pc = attrColor(com.google.android.material.R.attr.colorPrimary)
        val disc = TextView(context).apply {
            text = rowIcon(title)
            textSize = 22f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(32, Color.red(pc), Color.green(pc), Color.blue(pc)))
            }
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }
        val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(context).apply {
            text = title; setTextColor(attrColor(com.google.android.material.R.attr.colorOnSurface)); textSize = 15f; typeface = Typeface.DEFAULT_BOLD
        })
        texts.addView(TextView(context).apply {
            text = desc; setTextColor(attrColor(android.R.attr.textColorSecondary)); textSize = 12f
        })
        val valueTv = TextView(context).apply {
            text = value(); setTextColor(attrColor(com.google.android.material.R.attr.colorPrimary)); textSize = 12f
            typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.END
        }
        row.addView(disc)
        row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(12), 0, 0, 0)
        })
        row.addView(valueTv)
        row.addView(TextView(context).apply {
            text = "›"; setTextColor(attrColor(android.R.attr.textColorSecondary)); textSize = 22f
            setPadding(dp(10), 0, 0, 0)
        })
        row.setOnClickListener { act(); valueTv.text = value() }
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dp(10)) }
        return row
    }

    private fun openSearch() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.fragment_slide_up,
                R.anim.fragment_fade_out,
                R.anim.fragment_fade_in,
                R.anim.fragment_slide_down
            )
            .replace(R.id.container, SearchFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun showAppHelp() {
        val ctx = requireContext()
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(16), dp(20), dp(8)) }
        val sv = ScrollView(ctx)
        sv.addView(box)
        fun head(t: String) {
            box.addView(TextView(ctx).apply { text = t; setTextColor(Color.rgb(77, 208, 225)); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(2)) })
        }
        fun body(t: String) {
            box.addView(TextView(ctx).apply { text = t; setTextColor(Color.rgb(200, 215, 235)); textSize = 15f; gravity = Gravity.CENTER })
        }
        box.addView(TextView(ctx).apply { text = "KİMYA UYGULAMASI"; setTextColor(Color.rgb(77, 208, 225)); textSize = 20f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(4)) })
        head("ANA EKRAN")
        body("Kategoriye dokunarak süzün, karta dokunarak aracı açın. Karta basılı tutarsanız yardım gelir. Üstteki arama ile her şeyi bulun.")
        head("ÜST BAR")
        body("Sol üst ok bir önceki ekrana döner. Dişli çark Ayarlar'ı açar; arama, tema ve diğer her şey buradadır.")
        head("ARAÇLAR")
        body("45 araç: hesaplayıcılar, simülasyonlar, testler ve laboratuvarlar. Kütüphaneli araçlarda (FTIR gibi) numuneyi kütüphaneden seçin.")
        AlertDialog.Builder(ctx).setView(sv).setPositiveButton("Kapat", null).show()
    }

    private fun showThemePicker() {
        val ctx = requireContext()
        val themes = ThemeManager.themes
        val currentKey = ThemeManager.getSelectedTheme(ctx).key
        val labels = themes.map { it.label }.toTypedArray()
        val checkedIndex = themes.indexOfFirst { it.key == currentKey }
        AlertDialog.Builder(ctx)
            .setTitle("Tema Seç")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selected = themes[which]
                ThemeManager.setSelectedTheme(ctx, selected.key)
                dialog.dismiss()
                // Aktiviteyi yerinde yeniden kur: onCreate temuayı yeniden uygular
                requireActivity().recreate()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(requireContext())
            .setTitle("Geçmişi Temizle")
            .setMessage("Tüm işlem kayıtları silinsin mi?")
            .setPositiveButton("Temizle") { _, _ -> vm.clearHistory() }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun clearRecents() {
        requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
            .edit().remove("recent_tools").apply()
    }
}
