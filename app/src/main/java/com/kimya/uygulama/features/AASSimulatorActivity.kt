package com.kimya.uygulama.features

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kimya.uygulama.R
import com.kimya.uygulama.views.AASSimulatorView

class AASSimulatorActivity : AppCompatActivity() {

    private lateinit var aasView: AASSimulatorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aas_simulator)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        aasView = findViewById(R.id.aas_view)
        aasView.onSelectSampleRequested = { showSelectSampleDialog() }
    }

    private fun showSelectSampleDialog() {
        val elements = AASSimulatorView.ELEMENTS
        val names = elements.map { "${it.symbol} - ${it.name} (${it.wavelength})" }.toTypedArray()
        var selectedIdx = elements.indexOfFirst { it.symbol == aasView.currentElement.symbol }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Eleman Sec")
            .setSingleChoiceItems(names, selectedIdx) { dialog, which ->
                selectedIdx = which
            }
            .setPositiveButton("Devam") { _, _ ->
                showConcDialog(elements, selectedIdx)
            }
            .setNegativeButton("Iptal", null)
            .show()
    }

    private fun showConcDialog(elements: List<AASSimulatorView.ElementInfo>, elemIdx: Int) {
        val elem = elements[elemIdx]

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val tvConcLabel = TextView(this).apply {
            text = "Konsantrasyon: ${String.format("%.2f", aasView.sampleConcentration)} ppm"
            textSize = 14f
        }

        val seekBar = SeekBar(this).apply {
            max = 2000
            progress = (aasView.sampleConcentration * 100).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvConcLabel.text = "Konsantrasyon: ${String.format("%.2f", progress / 100.0f)} ppm"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        val etSampleId = EditText(this).apply {
            hint = "Numune ID"
            setText(aasView.sampleId)
        }

        layout.addView(etSampleId)
        layout.addView(tvConcLabel)
        layout.addView(seekBar)

        AlertDialog.Builder(this)
            .setTitle("${elem.symbol} - Konsantrasyon")
            .setView(layout)
            .setPositiveButton("Uygula") { _, _ ->
                val conc = seekBar.progress / 100.0f
                val id = etSampleId.text.toString().ifBlank { "Sample_01" }
                aasView.setElement(elem, conc, id)
                Toast.makeText(this, "${elem.symbol} secildi (${elem.wavelength})", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Iptal", null)
            .show()
    }
}
