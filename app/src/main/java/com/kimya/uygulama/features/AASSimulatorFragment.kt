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
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.kimya.uygulama.R
import com.kimya.uygulama.views.AASSimulatorView

class AASSimulatorFragment : Fragment() {

    private lateinit var aasView: AASSimulatorView
    private lateinit var tvAbsorbance: TextView
    private lateinit var tvConcentration: TextView
    private lateinit var tvWavelength: TextView
    private lateinit var tvAcetylene: TextView
    private lateinit var tvAir: TextView
    private lateinit var tvSampleConc: TextView
    private lateinit var tvSignalPct: TextView
    private lateinit var signalBar: android.widget.ProgressBar
    private lateinit var btnFlameToggle: Button
    private lateinit var btnSampleInsert: Button

    private val updateUi = object : Runnable {
        override fun run() {
            if (!isAdded) return
            tvAbsorbance.text = String.format("%.4f", aasView.currentAbsorbance)
            tvConcentration.text = String.format("%.2f ppm", aasView.currentConcentration)
            tvWavelength.text = "${aasView.currentElement.wavelength}"
            signalBar.progress = aasView.signalPercent.toInt()
            tvSignalPct.text = String.format("%.1f%%", aasView.signalPercent)
            handler.postDelayed(this, 50L)
        }
    }
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_aas_simulator, container, false)

        aasView = v.findViewById(R.id.aas_view)
        tvAbsorbance = v.findViewById(R.id.tv_absorbance)
        tvConcentration = v.findViewById(R.id.tv_concentration)
        tvWavelength = v.findViewById(R.id.tv_wavelength)
        tvAcetylene = v.findViewById(R.id.tv_acetylene)
        tvAir = v.findViewById(R.id.tv_air)
        tvSampleConc = v.findViewById(R.id.tv_sample_conc)
        tvSignalPct = v.findViewById(R.id.tv_signal_pct)
        signalBar = v.findViewById(R.id.signal_bar)
        btnFlameToggle = v.findViewById(R.id.btn_flame_toggle)
        btnSampleInsert = v.findViewById(R.id.btn_sample_insert)

        v.findViewById<Button>(R.id.btn_help).setOnClickListener { showHelp() }

        buildElementChips(v.findViewById(R.id.element_chips))

        v.findViewById<SeekBar>(R.id.seekbar_acetylene).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                aasView.acetyleneFlow = progress.toFloat()
                tvAcetylene.text = "${progress} L/min"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        v.findViewById<SeekBar>(R.id.seekbar_air).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                aasView.airFlow = progress.toFloat()
                tvAir.text = "${progress} L/min"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        v.findViewById<SeekBar>(R.id.seekbar_sample).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                aasView.sampleConcentration = progress * 0.1f
                tvSampleConc.text = String.format("%.1f ppm", progress * 0.1f)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnFlameToggle.setOnClickListener {
            aasView.flameOn = !aasView.flameOn
            btnFlameToggle.text = if (aasView.flameOn) "Alevi Söndür" else "Alevi Yak"
            btnSampleInsert.isEnabled = aasView.flameOn
            if (!aasView.flameOn) {
                aasView.sampleInserted = false
                btnSampleInsert.text = "Numune Ver"
            }
        }

        btnSampleInsert.setOnClickListener {
            aasView.sampleInserted = !aasView.sampleInserted
            btnSampleInsert.text = if (aasView.sampleInserted) "Numune Çek" else "Numune Ver"
        }

        btnSampleInsert.isEnabled = false

        v.findViewById<ChipGroup>(R.id.chipgroup_level).setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chip_level1) -> {}
                checkedIds.contains(R.id.chip_level2) -> {}
                checkedIds.contains(R.id.chip_level3) -> {}
                checkedIds.contains(R.id.chip_level4) -> {}
            }
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateUi)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateUi)
    }

    private fun buildElementChips(container: LinearLayout) {
        val dp6 = (6 * resources.displayMetrics.density).toInt()
        val dp12 = (12 * resources.displayMetrics.density).toInt()
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val ctx = requireContext()

        for (element in AASSimulatorView.ELEMENTS) {
            val chip = MaterialCardView(ctx).apply {
                radius = 20f * resources.displayMetrics.density
                cardElevation = 2f * resources.displayMetrics.density
                val bg = GradientDrawable().apply {
                    setColor(Color.rgb(30, 30, 40))
                    cornerRadius = 20f * resources.displayMetrics.density
                    setStroke(dp4, element.hclColor)
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
                    setColor(element.hclColor)
                }
            }

            val label = TextView(ctx).apply {
                text = element.symbol
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
                aasView.selectElement(element.symbol)
                updateInfo(element)
                updateChipSelection(container, element.symbol)
            }

            container.addView(chip)
        }
    }

    private fun updateChipSelection(container: LinearLayout, selectedSymbol: String) {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? MaterialCardView ?: continue
            val innerLayout = chip.getChildAt(0) as? LinearLayout ?: continue
            val label = innerLayout.getChildAt(1) as? TextView ?: continue
            val element = AASSimulatorView.ELEMENTS.find { it.symbol == selectedSymbol }

            if (label.text.toString() == selectedSymbol) {
                chip.cardElevation = 6f * resources.displayMetrics.density
                label.setTextColor(element?.hclColor ?: Color.WHITE)
            } else {
                chip.cardElevation = 2f * resources.displayMetrics.density
                label.setTextColor(Color.WHITE)
            }
        }
    }

    private fun updateInfo(element: AASSimulatorView.ElementInfo) {
        tvWavelength.text = element.wavelength
    }

    private fun showHelp() {
        AlertDialog.Builder(requireContext())
            .setTitle("AAS Simülatörü Hakkında")
            .setMessage(
                "Atomik Absorpsiyon Spektroskopisi (AAS), bir numunedeki" +
                " metal elementlerin konsantrasyonunu ölçen analitik bir tekniktir.\n\n" +
                "Nasıl çalışır:\n" +
                "1. Işık Kaynağı (HCL): Seçilen elemente özgü dalga boyunda ışık yayar\n" +
                "2. Nebulizatör: Numune çözeltisini ince bir sise dönüştürür\n" +
                "3. Atomizer: Alevde numuneyi buharlaştırarak serbest atomlara dönüştürür\n" +
                "4. Monokromatör: İstenilen dalga boyunu seçer\n" +
                "5. Dedektör (PMT): Emilim miktarını ölçer\n" +
                "6. Ekran: Absorbans ve konsantrasyon değerini gösterir\n\n" +
                "Beer-Lambert Yasası:\n" +
                "A = ε × l × c\n" +
                "Absorbans, konsantrasyonla doğru orantılıdır.\n\n" +
                "Kontroller:\n" +
                "- Element seçin (HCL lambası değişir)\n" +
                "- Asetilen ve hava akışını ayarlayın\n" +
                "- Alevi yakın\n" +
                "- Numune verin\n" +
                "- Okuma değerlerini izleyin\n\n" +
                "Seviyeler:\n" +
                "1 - Temel: Basit okuma\n" +
                "2 - Orta: Kalibrasyon eğrisi\n" +
                "3 - İleri: Girişim etkileri\n" +
                "4 - Uzman: Tam analiz"
            )
            .setPositiveButton("Tamam", null)
            .show()
    }
}
