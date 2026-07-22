package com.kimya.uygulama.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData

class SearchFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_search, container, false)
        val input = v.findViewById<EditText>(R.id.search_input)
        val result = v.findViewById<TextView>(R.id.search_result)

        val query = arguments?.getString("query")
        if (!query.isNullOrEmpty()) {
            input.setText(query)
            performSearch(input, result, query)
        }

        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Arama")
                .setMessage("Periyodik tablodaki elementleri ve bileşikleri arayabilirsiniz.\n\n" +
                    "- Element adı veya sembolü ile arama\n" +
                    "- Kimyasal formül ile molekül kütlesi\n" +
                    "- Sonuçlarda element özelliklerini görün\n\n" +
                    "Örnek: 'O' veya 'Oksijen' yazarak element bilgisi alın.")
                .setPositiveButton("Anladım") { d, _ -> d.dismiss() }
                .show()
        }
        v.findViewById<Button>(R.id.search_button).setOnClickListener {
            performSearch(input, result, input.text.toString().trim())
        }
        return v
    }

    private fun performSearch(input: EditText, result: TextView, q: String) {
        if (q.isEmpty()) return

        val el = KimyaData.elementBul(q)
        if (el != null) {
            result.text = """|[ELEMENT BULUNDU]
                |Sembol: ${el.semIol}
                |Adı: ${el.adi}
                |Atom No: ${el.atomNo}
                |Kütle: ${el.kutle} g/mol
                |Grup: ${el.grup} | Periyot: ${el.periyot}
                |Tür: ${el.tur}
                |Valans: ${el.valans.joinToString(", ")}
                |Hal: ${el.durum}
                |Elektronegatiflik: ${el.elektronegatiflik}
                |İyonlaşma Enerjisi: ${el.iyonlasmaEnerjisi} kJ/mol
                |Elektron Dizilimi: ${el.elektron}
                |Kullanım: ${el.kullanim}
                |Özellik: ${el.ozellik}""".trimMargin()
        } else {
            val mk = KimyaData.molekulKutlesiHesapla(q)
            if (mk != null) {
                result.text = "Molekül Kütlesi: $q = ${"%.4f".format(mk)} g/mol"
            } else {
                result.text = "Eşleşme bulunamadı: $q"
            }
        }
    }
}
