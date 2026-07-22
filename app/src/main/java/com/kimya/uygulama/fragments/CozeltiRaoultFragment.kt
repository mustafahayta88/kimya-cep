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

class CozeltiRaoultFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cozelti_raoult, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etPure = view.findViewById<EditText>(R.id.et_raoult_pure)
        val etMolSolute = view.findViewById<EditText>(R.id.et_raoult_mol_solute)
        val etMolSolvent = view.findViewById<EditText>(R.id.et_raoult_mol_solvent)
        val tvRaoultResult = view.findViewById<TextView>(R.id.tv_raoult_result)

        view.findViewById<Button>(R.id.btn_raoult).setOnClickListener {
            val p0 = etPure.text.toString().toDoubleOrNull()
            val ns = etMolSolute.text.toString().toDoubleOrNull()
            val nw = etMolSolvent.text.toString().toDoubleOrNull()
            if (p0 == null || ns == null || nw == null || (ns + nw) == 0.0) {
                Toast.makeText(context, "Değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val xSolvent = nw / (ns + nw)
            val xSolute = ns / (ns + nw)
            val pSolution = xSolvent * p0
            val deltaP = xSolute * p0
            tvRaoultResult.text = buildString {
                appendLine("X_çözücü = ${"%.4f".format(nw)} / (${"%.4f".format(ns)} + ${"%.4f".format(nw)})")
                appendLine("X_çözücü = ${"%.4f".format(xSolvent)}")
                appendLine()
                appendLine("P_çözelti = ${"%.4f".format(xSolvent)} × ${"%.1f".format(p0)}")
                appendLine("P_çözelti = ${"%.2f".format(pSolution)} mmHg")
                appendLine()
                appendLine("Buhar basıncı düşüşü: ΔP = ${"%.2f".format(deltaP)} mmHg")
                appendLine("Düşüş oranı: ${"%.1f".format(xSolute * 100)}%")
            }
        }

        val etFpM = view.findViewById<EditText>(R.id.et_fp_molality)
        val etFpI = view.findViewById<EditText>(R.id.et_fp_i)
        val tvFpResult = view.findViewById<TextView>(R.id.tv_fp_result)

        view.findViewById<Button>(R.id.btn_fp).setOnClickListener {
            val m = etFpM.text.toString().toDoubleOrNull()
            val i = etFpI.text.toString().toDoubleOrNull()
            if (m == null || i == null) {
                Toast.makeText(context, "Değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val Kf = 1.86
            val dT = Kf * m * i
            tvFpResult.text = buildString {
                appendLine("ΔT_f = K_f × m × i")
                appendLine("ΔT_f = 1.86 × ${"%.3f".format(m)} × ${"%.2f".format(i)}")
                appendLine("ΔT_f = ${"%.2f".format(dT)} °C")
                appendLine()
                appendLine("Donma noktası: 0 − ${"%.2f".format(dT)} = ${"%.2f".format(-dT)} °C")
                appendLine()
                if (dT > 5) appendLine("⚠ Yüksek konsantrasyon - ideal davranış sapabilir")
            }
        }

        val etBpM = view.findViewById<EditText>(R.id.et_bp_molality)
        val etBpI = view.findViewById<EditText>(R.id.et_bp_i)
        val tvBpResult = view.findViewById<TextView>(R.id.tv_bp_result)

        view.findViewById<Button>(R.id.btn_bp).setOnClickListener {
            val m = etBpM.text.toString().toDoubleOrNull()
            val i = etBpI.text.toString().toDoubleOrNull()
            if (m == null || i == null) {
                Toast.makeText(context, "Değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val Kb = 0.512
            val dT = Kb * m * i
            tvBpResult.text = buildString {
                appendLine("ΔT_b = K_b × m × i")
                appendLine("ΔT_b = 0.512 × ${"%.3f".format(m)} × ${"%.2f".format(i)}")
                appendLine("ΔT_b = ${"%.2f".format(dT)} °C")
                appendLine()
                appendLine("Kaynama noktası: 100 + ${"%.2f".format(dT)} = ${"%.2f".format(100 + dT)} °C")
                appendLine()
                if (dT > 5) appendLine("⚠ Yüksek konsantrasyon - ideal davranış sapabilir")
            }
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Çözelti ve Raoult Yasası Yardımı")
                .setMessage("Bu araç çözeltilerin özelliklerini hesaplar.\n\nRAOULT YASASI:\n• Bir sıvıya başka bir madde katıldığında buhar basıncı düşer\n• P°: Saf sıvının buhar basıncı\n• X: Çözücünün mol oranı (toplam mola bölin)\n\nDONMA NOKTASI ALÇALMASI:\n• Tuzlu suyun donma noktası saf sudan düşüktür\n• m: Molerlik (kg başına mol)\n• i: Parçacık sayısı (NaCl için 2, CaCl₂ için 3)\n• K_f = 1.86 (su için)\n\nKAYNAMA NOKTASI YÜKSELMESİ:\n• Tuzlu suyun kaynama noktası saf sudan yüksektir\n• K_b = 0.512 (su için)\n\nvan 't Hoff: Çözünen maddenin parçalanma sayısı")
                .setPositiveButton("Anladım", null)
                .show()
        }
    }
}
