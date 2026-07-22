package com.kimya.uygulama.features

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.kimya.uygulama.R
import com.kimya.uygulama.views.FlameView

class FlameTestFragment : Fragment() {

    private lateinit var flameView: FlameView
    private lateinit var saltName: TextView
    private lateinit var saltFormula: TextView
    private lateinit var saltWavelength: TextView
    private lateinit var saltDesc: TextView
    private lateinit var colorPreview: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_flame_test, container, false)

        flameView = v.findViewById(R.id.flame_view)
        saltName = v.findViewById(R.id.salt_name)
        saltFormula = v.findViewById(R.id.salt_formula)
        saltWavelength = v.findViewById(R.id.salt_wavelength)
        saltDesc = v.findViewById(R.id.salt_desc)
        colorPreview = v.findViewById(R.id.salt_color_preview)

        v.findViewById<Button>(R.id.btn_help).setOnClickListener { showHelp() }

        buildSaltChips(v.findViewById(R.id.salt_chips_container))
        updateInfo("none")

        return v
    }

    private fun buildSaltChips(container: LinearLayout) {
        val dp6 = (6 * resources.displayMetrics.density).toInt()
        val dp12 = (12 * resources.displayMetrics.density).toInt()
        val dp4 = (4 * resources.displayMetrics.density).toInt()

        val ctx = requireContext()

        for (salt in FlameView.SALTS) {
            val chip = MaterialCardView(ctx).apply {
                radius = 20f * resources.displayMetrics.density
                cardElevation = 2f * resources.displayMetrics.density
                val bg = GradientDrawable().apply {
                    setColor(Color.rgb(30, 30, 40))
                    cornerRadius = 20f * resources.displayMetrics.density
                    setStroke(dp4, if (salt.key == "none") Color.rgb(80, 80, 90) else salt.colorMid)
                }
                background = bg
                setCardBackgroundColor(Color.TRANSPARENT)
                useCompatPadding = false
                preventCornerOverlap = false
            }

            val inner = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp12, dp6, dp12, dp6)
            }

            val dot = View(ctx).apply {
                val size = (10 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp4 }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (salt.key == "none") Color.rgb(80, 160, 255) else salt.colorMid)
                }
            }

            val label = TextView(ctx).apply {
                text = if (salt.key == "none") "Yok" else salt.key
                setTextColor(Color.WHITE)
                textSize = 12f
            }

            inner.addView(dot)
            inner.addView(label)
            chip.addView(inner)

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp4; marginEnd = dp4 }
            chip.layoutParams = lp

            chip.setOnClickListener {
                flameView.selectSalt(salt.key)
                updateInfo(salt.key)
                updateChipSelection(container, salt.key)
            }

            container.addView(chip)
        }
    }

    private fun updateChipSelection(container: LinearLayout, selectedKey: String) {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? MaterialCardView ?: continue
            val innerLayout = chip.getChildAt(0) as? LinearLayout ?: continue
            val label = innerLayout.getChildAt(1) as? TextView ?: continue
            val salt = FlameView.SALTS.find { it.key == (if (selectedKey == "none") "none" else selectedKey) }

            if (label.text.toString() == selectedKey || (selectedKey == "none" && label.text == "Yok")) {
                chip.cardElevation = 6f * resources.displayMetrics.density
                label.setTextColor(salt?.colorMid ?: Color.WHITE)
            } else {
                chip.cardElevation = 2f * resources.displayMetrics.density
                label.setTextColor(Color.WHITE)
            }
        }
    }

    private fun updateInfo(key: String) {
        val salt = FlameView.SALTS.find { it.key == key } ?: return
        saltName.text = if (key == "none") "Normal Asetilen Alevi" else "${salt.name} Testi"
        saltFormula.text = salt.formula
        saltWavelength.text = if (key == "none") "" else "Dalga boyu: ${salt.wavelength}"
        saltDesc.text = salt.description

        val color = if (key == "none") Color.rgb(80, 160, 255) else salt.colorMid
        colorPreview.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 3f * resources.displayMetrics.density
        }
    }

    private fun showHelp() {
        AlertDialog.Builder(requireContext())
            .setTitle("Alev Testi Nedir?")
            .setMessage(
                "Alev testi, metal tuzlarinin alevde yanarken karakteristik" +
                " renkler cikarmasina dayanan analiz yontemidir.\n\n" +
                "Nasil calisir:\n" +
                "1. Bunsen burnerinda asetilen (C\u2082H\u2082) ve hava karisimi yanar\n" +
                "2. Nikel-krom teli tuz cozeltisine batirin\n" +
                "3. Teli alevin icine yerlestirin\n" +
                "4. Alev rengi degisir - her element kendine ozgu renk cikarir\n\n" +
                "Renklerin nedeni:\n" +
                "Isinlanan elektronlar yuksek enerji katmanina gecer ve" +
                " geri donerken ozgun dalga boyunda isin yayilir.\n\n" +
                "Her elementin elektron yapisi farkli oldugundan," +
                " cikardiklari isin rengi de farklidir."
            )
            .setPositiveButton("Tamam", null)
            .show()
    }
}
