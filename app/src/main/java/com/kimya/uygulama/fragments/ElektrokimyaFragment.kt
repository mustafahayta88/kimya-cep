package com.kimya.uygulama.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import kotlin.math.log10

class ElektrokimyaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_elektrokimya, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etCathode = view.findViewById<EditText>(R.id.et_e_cathode)
        val etAnode = view.findViewById<EditText>(R.id.et_e_anode)
        val tvCellResult = view.findViewById<TextView>(R.id.tv_cell_result)

        val etNernstE0 = view.findViewById<EditText>(R.id.et_nernst_e0)
        val etNernstN = view.findViewById<EditText>(R.id.et_nernst_n)
        val etNernstQ = view.findViewById<EditText>(R.id.et_nernst_q)
        val tvNernstResult = view.findViewById<TextView>(R.id.tv_nernst_result)

        val etFaradayM = view.findViewById<EditText>(R.id.et_faraday_m)
        val etFaradayI = view.findViewById<EditText>(R.id.et_faraday_i)
        val etFaradayT = view.findViewById<EditText>(R.id.et_faraday_t)
        val etFaradayN = view.findViewById<EditText>(R.id.et_faraday_n)
        val tvFaradayResult = view.findViewById<TextView>(R.id.tv_faraday_result)

        view.findViewById<Button>(R.id.btn_cell_calc).setOnClickListener {
            val ec = etCathode.text.toString().toDoubleOrNull()
            val ea = etAnode.text.toString().toDoubleOrNull()
            if (ec == null || ea == null) {
                Toast.makeText(context, "Değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val eCell = ec - ea
            tvCellResult.text = buildString {
                appendLine("E°cell = E°katot − E°anot")
                appendLine("E°cell = ${"%.3f".format(ec)} − ${"%.3f".format(ea)}")
                appendLine("E°cell = ${"%.3f".format(eCell)} V")
                appendLine()
                if (eCell > 0) {
                    appendLine("✓ Galvanik pil (spontan)")
                    appendLine("✓ ΔG° = −nFE° < 0")
                } else if (eCell == 0.0) {
                    appendLine("Denge halinde")
                } else {
                    appendLine("✗ Galvanik pil çalışmaz (spontan değil)")
                    appendLine("✗ Elektroliz gerekli")
                }
            }
        }

        view.findViewById<Button>(R.id.btn_nernst).setOnClickListener {
            val e0 = etNernstE0.text.toString().toDoubleOrNull()
            val n = etNernstN.text.toString().toIntOrNull()
            val q = etNernstQ.text.toString().toDoubleOrNull()
            if (e0 == null || n == null || n == 0 || q == null || q <= 0) {
                Toast.makeText(context, "Değerleri doğru girin (Q > 0)", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val e = e0 - (0.0592 / n) * log10(q)
            tvNernstResult.text = buildString {
                appendLine("E = E° − (0.0592/n) × log(Q)")
                appendLine("E = ${"%.4f".format(e0)} − (0.0592/${n}) × log(${q})")
                appendLine("E = ${"%.4f".format(e0)} − ${"%.4f".format(0.0592 / n * log10(q))}")
                appendLine("E = ${"%.4f".format(e)} V")
                appendLine()
                if (e > e0) appendLine("Q < 1 → Ürünler baskın, E artar")
                else if (e < e0) appendLine("Q > 1 → Reaktifler baskın, E azalır")
                else appendLine("Q = 1 → Standart koşullar")
            }
        }

        view.findViewById<Button>(R.id.btn_faraday).setOnClickListener {
            val m = etFaradayM.text.toString().toDoubleOrNull()
            val i = etFaradayI.text.toString().toDoubleOrNull()
            val t = etFaradayT.text.toString().toDoubleOrNull()
            val n = etFaradayN.text.toString().toIntOrNull()
            if (m == null || i == null || t == null || n == null || n == 0) {
                Toast.makeText(context, "Değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val F = 96485.0
            val kütle = (m * i * t) / (n * F)
            val mol = kütle / m
            val q = i * t
            tvFaradayResult.text = buildString {
                appendLine("m = (M × I × t) / (n × F)")
                appendLine("m = (${"%.2f".format(m)} × ${"%.2f".format(i)} × ${"%.0f".format(t)}) / (${n} × 96485)")
                appendLine("m = ${"%.6f".format(kütle)} g")
                appendLine()
                appendLine("Mol: ${"%.6f".format(mol)} mol")
                appendLine("Toplam yük: ${"%.1f".format(q)} Coulomb")
                appendLine("Toplam yük: ${"%.4f".format(q / 96485)} Faraday")
            }
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Elektrokimya Aracı Yardımı")
                .setMessage("Bu araç elektrik ile kimya arasındaki ilişkiyi hesaplar.\n\nHÜCRE POTANSİYELİ:\n• Bir pilin ne kadar güçlü olduğunu söyler\n• Katot (+): İndirgenmenin olduğu elektrot (pozitif kutup)\n• Anot (−): Oksidasyonun olduğu elektrot (negatif kutup)\n• E°cell pozitifse pil elektrik üretir\n\nNERNST DENKLEMİ:\n• Pilin standart dışı koşullardaki gerilimini hesaplar\n• Q: Reaksiyonun o anki durumu\n\nFARADAY YASASI:\n• Elektroliz ile ne kadar madde biriktirilir\n• Akım × Süre = Toplam elektrik yükü\n• Bu yük kadar madde çökelir\n• Cu elektrolizi için: M=63.55, n=2")
                .setPositiveButton("Anladım", null)
                .show()
        }
    }
}
