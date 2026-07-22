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
import kotlin.math.ln

class KinematikFragment : Fragment() {

    private var currentOrder = 0
    private lateinit var tvTitle: TextView
    private lateinit var tvFormula: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvGraphInfo: TextView
    private lateinit var etC0: EditText
    private lateinit var etK: EditText
    private lateinit var etT: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kinematik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTitle = view.findViewById(R.id.tv_kin_title)
        tvFormula = view.findViewById(R.id.tv_kin_formula)
        tvResult = view.findViewById(R.id.tv_kin_result)
        tvGraphInfo = view.findViewById(R.id.tv_kin_graph_info)
        etC0 = view.findViewById(R.id.et_kin_c0)
        etK = view.findViewById(R.id.et_kin_k)
        etT = view.findViewById(R.id.et_kin_t)

        view.findViewById<Button>(R.id.btn_zero).setOnClickListener { selectOrder(0) }
        view.findViewById<Button>(R.id.btn_first).setOnClickListener { selectOrder(1) }
        view.findViewById<Button>(R.id.btn_second).setOnClickListener { selectOrder(2) }
        view.findViewById<Button>(R.id.btn_kin_calc).setOnClickListener { calculate() }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Kinematik Aracı Yardımı")
                .setMessage("Bu araç bir kimyasal tepkinin ne kadar hızlı olduğunu hesaplar.\n\n1. Mertebe seçin:\n• 0. Mertebe: Hız sabittir, derişim değişmez\n• 1. Mertebe: Hız derişime bağlıdır (radyoaktif bozunma gibi)\n• 2. Mertebe: Hız derişimin karesine bağlıdır\n\nSonra girin:\n• [A]₀: Başlangıçta ne kadar madde var (mol/litre)\n• k: Hız sabiti (teknik bir değer)\n• t: Geçen süre (saniye)\n\nSonuç: O sürede ne kadar madde kaldığını gösterir.\n\nYarım ömür: Bir maddenin yarısının bozunma süresidir.")
                .setPositiveButton("Anladım", null)
                .show()
        }

        selectOrder(1)
    }

    private fun selectOrder(order: Int) {
        currentOrder = order
        when (order) {
            0 -> {
                tvTitle.text = "0. Mertebe Hız Yasası"
                tvFormula.text = "[A] = [A]₀ − kt"
                tvTitle.setTextColor(0xFF00F0FF.toInt())
                tvFormula.setTextColor(0xFF00F0FF.toInt())
                tvGraphInfo.text = "0. Mertebe Grafikleri:\n[A] vs t → düz doğru ( eğim = −k)\nYarım ömür: t½ = [A]₀ / 2k\nBirim: k = M/s"
            }
            1 -> {
                tvTitle.text = "1. Mertebe Hız Yasası"
                tvFormula.text = "ln[A] = ln[A]₀ − kt"
                tvTitle.setTextColor(0xFF39FF14.toInt())
                tvFormula.setTextColor(0xFF39FF14.toInt())
                tvGraphInfo.text = "1. Mertebe Grafikleri:\nln[A] vs t → düz doğru ( eğim = −k)\nYarım ömür: t½ = 0.693 / k\nBirim: k = s⁻¹"
            }
            2 -> {
                tvTitle.text = "2. Mertebe Hız Yasası"
                tvFormula.text = "1/[A] = 1/[A]₀ + kt"
                tvTitle.setTextColor(0xFFFF0080.toInt())
                tvFormula.setTextColor(0xFFFF0080.toInt())
                tvGraphInfo.text = "2. Mertebe Grafikleri:\n1/[A] vs t → düz doğru ( eğim = +k)\nYarım ömür: t½ = 1 / (k[A]₀)\nBirim: k = M⁻¹s⁻¹"
            }
        }
        tvResult.text = ""
    }

    private fun calculate() {
        val c0 = etC0.text.toString().toDoubleOrNull()
        val k = etK.text.toString().toDoubleOrNull()
        val t = etT.text.toString().toDoubleOrNull()
        if (c0 == null || k == null || t == null) {
            Toast.makeText(context, "Tüm değerleri girin", Toast.LENGTH_SHORT).show(); return
        }
        if (c0 <= 0 || k <= 0 || t < 0) {
            Toast.makeText(context, "Değerler pozitif olmalı (t hariç)", Toast.LENGTH_SHORT).show(); return
        }

        val result = when (currentOrder) {
            0 -> {
                val c = c0 - k * t
                val halfLife = c0 / (2 * k)
                if (c < 0) {
                    "⚠ Reaksiyon ${"%.2f".format(c0 / k)} saniyede tamamlanmış!\n" +
                    "Verilen zamanda ürün kalmamış.\n" +
                    "Yarım ömür: ${"%.2f".format(halfLife)} s\n" +
                    "Tamamlanma süresi: ${"%.2f".format(c0 / k)} s"
                } else {
                    "[A] = ${"%.4f".format(c0)} − ${"%.4f".format(k)} × ${"%.2f".format(t)}\n" +
                    "[A]${"%.2f".format(t)}s = ${"%.4f".format(c)} M\n\n" +
                    "Yarım ömür: ${"%.2f".format(halfLife)} s\n" +
                    "Tüketim yüzdesi: ${"%.1f".format((1 - c / c0) * 100)}%"
                }
            }
            1 -> {
                val lnC = ln(c0) - k * t
                val c = Math.exp(lnC)
                val halfLife = 0.693 / k
                "ln[A] = ln(${c0}) − ${"%.4f".format(k)} × ${"%.2f".format(t)}\n" +
                "ln[A] = ${"%.4f".format(lnC)}\n" +
                "[A]${"%.2f".format(t)}s = ${"%.4f".format(c)} M\n\n" +
                "Yarım ömür: ${"%.2f".format(halfLife)} s\n" +
                "Tüketim yüzdesi: ${"%.1f".format((1 - c / c0) * 100)}%"
            }
            2 -> {
                val invC = 1.0 / c0 + k * t
                val c = 1.0 / invC
                val halfLife = 1.0 / (k * c0)
                "1/[A] = 1/${c0} + ${"%.4f".format(k)} × ${"%.2f".format(t)}\n" +
                "1/[A] = ${"%.4f".format(invC)}\n" +
                "[A]${"%.2f".format(t)}s = ${"%.4f".format(c)} M\n\n" +
                "Yarım ömür: ${"%.4f".format(halfLife)} s\n" +
                "Tüketim yüzdesi: ${"%.1f".format((1 - c / c0) * 100)}%"
            }
            else -> ""
        }
        tvResult.text = result
    }
}
