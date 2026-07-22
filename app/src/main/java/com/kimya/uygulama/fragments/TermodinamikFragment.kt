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

class TermodinamikFragment : Fragment() {

    private lateinit var cardGibbs: View
    private lateinit var cardHess: View
    private lateinit var etEnthalpy: EditText
    private lateinit var etEntropy: EditText
    private lateinit var etTemperature: EditText
    private lateinit var tvGibbsResult: TextView
    private lateinit var etHessElements: EditText
    private lateinit var etHessReactantH: EditText
    private lateinit var etHessProductH: EditText
    private lateinit var etHessProductN: EditText
    private lateinit var tvHessResult: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_termodinamik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardGibbs = view.findViewById(R.id.card_gibbs)
        cardHess = view.findViewById(R.id.card_hess)
        etEnthalpy = view.findViewById(R.id.et_enthalpy)
        etEntropy = view.findViewById(R.id.et_entropy)
        etTemperature = view.findViewById(R.id.et_temperature)
        tvGibbsResult = view.findViewById(R.id.tv_gibbs_result)
        etHessElements = view.findViewById(R.id.et_hess_elements)
        etHessReactantH = view.findViewById(R.id.et_hess_reactant_h)
        etHessProductH = view.findViewById(R.id.et_hess_product_h)
        etHessProductN = view.findViewById(R.id.et_hess_product_n)
        tvHessResult = view.findViewById(R.id.tv_hess_result)

        view.findViewById<Button>(R.id.btn_hesapla_gibbs).setOnClickListener {
            cardGibbs.visibility = View.VISIBLE
            cardHess.visibility = View.GONE
        }
        view.findViewById<Button>(R.id.btn_hess).setOnClickListener {
            cardGibbs.visibility = View.GONE
            cardHess.visibility = View.VISIBLE
        }

        view.findViewById<Button>(R.id.btn_calc_gibbs).setOnClickListener { calcGibbs() }
        view.findViewById<Button>(R.id.btn_calc_hess).setOnClickListener { calcHess() }

        view.findViewById<Button>(R.id.btn_ex1).setOnClickListener {
            etEnthalpy.setText("-890"); etEntropy.setText("-242"); etTemperature.setText("298"); calcGibbs()
        }
        view.findViewById<Button>(R.id.btn_ex2).setOnClickListener {
            etEnthalpy.setText("172"); etEntropy.setText("161"); etTemperature.setText("298"); calcGibbs()
        }
        view.findViewById<Button>(R.id.btn_ex3).setOnClickListener {
            etEnthalpy.setText("-2220"); etEntropy.setText("-386"); etTemperature.setText("298"); calcGibbs()
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Termodinamik Aracı Yardımı")
                .setMessage("Bu araç size kimyasal tepkilerin kendi kendine olup olmayacağını söyler.\n\nΔG HESAPLA sekmesinde:\n• ΔH girin: Tepkide ne kadar ısı açığa çıkıyor veya soğuruluyor (negatif = ısı verir)\n• ΔS girin: Düzen ne kadar değişiyor (pozitif = düzen artar)\n• T girin: Sıcaklık (Kelvin cinsinden, 25°C = 298 K)\n• Sonuç: ΔG negatifse tepki kendi kendine gerçekleşir\n\nHess Yasası sekmesinde:\n• Bir tepkideki toplam ısı değişimini hesaplarsınız\n• Başlangıç ve ürün maddelerinin bilinen ısı değerlerini girin")
                .setPositiveButton("Anladım", null)
                .show()
        }
    }

    private fun calcGibbs() {
        val h = etEnthalpy.text.toString().toDoubleOrNull()
        val s = etEntropy.text.toString().toDoubleOrNull()
        val t = etTemperature.text.toString().toDoubleOrNull()
        if (h == null || s == null || t == null) {
            Toast.makeText(context, "Tüm değerleri girin", Toast.LENGTH_SHORT).show(); return
        }
        val sKj = s / 1000.0
        val dG = h - t * sKj
        val spontan = when { dG < 0 -> "Süreç spontandır"; dG == 0.0 -> "Denge"; else -> "Süreç spontan değil" }
        val tEsdeger = if (sKj != 0.0) h / sKj else Double.NaN
        val sonuc = buildString {
            appendLine("ΔG = ${"%.2f".format(h)} − ${"%.0f".format(t)} × ${"%.4f".format(sKj)}")
            appendLine("ΔG = ${"%.2f".format(dG)} kJ/mol")
            appendLine()
            appendLine("Sonuç: $spontan")
            if (!tEsdeger.isNaN() && tEsdeger > 0) {
                appendLine("Denge sıcaklığı: ${"%.1f".format(tEsdeger)} K (${"%.1f".format(tEsdeger - 273.15)} °C)")
            }
            if (h < 0 && s > 0) appendLine("→ Her sıcaklıkta spontan (ekzotermik + entropy artışı)")
            else if (h > 0 && s < 0) appendLine("→ Hiçbir sıcaklıkta spontan değil")
        }
        tvGibbsResult.text = sonuc
    }

    private fun calcHess() {
        try {
            val rCounts = etHessElements.text.toString().split(",").map { it.trim().toInt() }
            val rHs = etHessReactantH.text.toString().split(",").map { it.trim().toDouble() }
            val pHs = etHessProductH.text.toString().split(",").map { it.trim().toDouble() }
            val pCounts = etHessProductN.text.toString().split(",").map { it.trim().toInt() }

            if (rCounts.size != rHs.size || pCounts.size != pHs.size) {
                Toast.makeText(context, "Sayılar eşleşmiyor", Toast.LENGTH_SHORT).show(); return
            }

            val rTotal = rCounts.zip(rHs).sumOf { (n, h) -> n * h }
            val pTotal = pCounts.zip(pHs).sumOf { (n, h) -> n * h }
            val dH = pTotal - rTotal

            tvHessResult.text = buildString {
                appendLine("ΣΔH°f (başlangıç) = ${"%.1f".format(rTotal)} kJ/mol")
                appendLine("ΣΔH°f (ürün) = ${"%.1f".format(pTotal)} kJ/mol")
                appendLine()
                appendLine("ΔH°_tepki = ${"%.1f".format(pTotal)} − ${"%.1f".format(rTotal)}")
                appendLine("ΔH°_tepki = ${"%.1f".format(dH)} kJ/mol")
                appendLine()
                if (dH < 0) appendLine("Ekzotermik reaksiyon (ısı açığa çıkar)")
                else appendLine("Endotermik reaksiyon (ısı soğurulur)")
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Değerleri doğru formatta girin", Toast.LENGTH_SHORT).show()
        }
    }
}
