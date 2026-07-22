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

class KimyasalDengeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kimyasal_denge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etKc = view.findViewById<EditText>(R.id.et_kc)
        val etDeltaN = view.findViewById<EditText>(R.id.et_delta_n)
        val etTemp = view.findViewById<EditText>(R.id.et_temp_kc)
        val tvResult = view.findViewById<TextView>(R.id.tv_kp_kc_result)
        val tvLeResult = view.findViewById<TextView>(R.id.tv_le_result)

        view.findViewById<Button>(R.id.btn_kp_kc).setOnClickListener {
            val kc = etKc.text.toString().toDoubleOrNull()
            val dn = etDeltaN.text.toString().toDoubleOrNull()
            val t = etTemp.text.toString().toDoubleOrNull()
            if (kc == null || dn == null || t == null) {
                Toast.makeText(context, "Tüm değerleri girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val R = 0.08206
            val kp = kc * Math.pow(R * t, dn)
            tvResult.text = buildString {
                appendLine("Kp = Kc × (RT)^Δn")
                appendLine("Kp = ${"%.4f".format(kc)} × (0.08206 × ${"%.0f".format(t)})^${"%.0f".format(dn)}")
                appendLine("Kp = ${"%.4f".format(kc)} × ${"%.4f".format(Math.pow(R * t, dn))}")
                appendLine("Kp = ${"%.6f".format(kp)}")
                appendLine()
                if (kp > 1000) appendLine("Ürünler baskın (denge sağa kaymış)")
                else if (kp < 0.001) appendLine("Reaktifler baskın (denge sola kaymış)")
                else appendLine("Reaktif ve ürünler dengede")
            }
        }

        view.findViewById<Button>(R.id.btn_le_conc).setOnClickListener {
            tvLeResult.text = buildString {
                appendLine("KONSANTRASYON DEĞİŞİKLİKLERİ:")
                appendLine()
                appendLine("▸ [Reaktif] artırılır → Denge SAĞA kayar (ürün yönü)")
                appendLine("▸ [Reaktif] azaltılır → Denge SOLA kayar (başlangıç yönü)")
                appendLine("▸ [Ürün] artırılır → Denge SOLA kayar")
                appendLine("▸ [Ürün] azaltılır → Denge SAĞA kayar")
                appendLine()
                appendLine("Not: K sabit kalır, sadece denge konumu değişir.")
            }
        }

        view.findViewById<Button>(R.id.btn_le_pressure).setOnClickListener {
            tvLeResult.text = buildString {
                appendLine("BASINÇ DEĞİŞİKLİKLERİ:")
                appendLine()
                appendLine("▸ Basınç artırılır → Denge gaz mol sayısı AZ olan tarafa kayar")
                appendLine("▸ Basınç azaltılır → Denge gaz mol sayısı ÇOK olan tarafa kayar")
                appendLine("▸ Δn = 0 ise → Basınç değişimi dengeyi ETKİLEMEZ")
                appendLine()
                appendLine("Örnek:")
                appendLine("N₂(g) + 3H₂(g) ⇌ 2NH₃(g)")
                appendLine("Δn = 2 − 4 = −2 → Basınç artışı SAĞA kaydırır")
            }
        }

        view.findViewById<Button>(R.id.btn_le_temp).setOnClickListener {
            tvLeResult.text = buildString {
                appendLine("SICAKLIK DEĞİŞİKLİKLERİ:")
                appendLine()
                appendLine("▸ Ekzotermik (ΔH {'<'} 0):")
                appendLine("  Sıcaklık artışı → K AZALIR, denge SOLA kayar")
                appendLine("  Sıcaklık azalışı → K ARTAR, denge SAĞA kayar")
                appendLine()
                appendLine("▸ Endotermik (ΔH {'>'} 0):")
                appendLine("  Sıcaklık artışı → K ARTAR, denge SAĞA kayar")
                appendLine("  Sıcaklık azalışı → K AZALIR, denge SOLA kayar")
                appendLine()
                appendLine("Not: Sıcaklık K değerini DEĞİŞTİRİR (diğerleri sadece dengeyi kaydırır)")
            }
        }

        view.findViewById<Button>(R.id.btn_help).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Kimyasal Denge Yardımı")
                .setMessage("Bu araç kimyasal tepkilerin ne tarafta denge kurduğunu gösterir.\n\nKp ↔ Kc DÖNÜŞÜM:\n• Kc: Molar derişim cinsinden denge sabiti\n• Kp: Basınç cinsinden denge sabiti\n• Δn: Gaz fazındaki ürün mol sayısından başlangıç mol sayısı çıkarılır\n• T: Sıcaklık (Kelvin)\n\nLe Chatelier:\n• Bir tepki dengedeyken ortama müdahale edildiğinde ne olur?\n• Konsantrasyon/Basınç/Sıcaklık butonlarına tıklayın\n• Sistem, değişikliği dengelemek için hangi tarafa kayacağını gösterir\n\nÖrnek: Sıcaklık artırılırsa, ekzotermik (ısı veren) tepki yavaşlar.")
                .setPositiveButton("Anladım", null)
                .show()
        }
    }
}
