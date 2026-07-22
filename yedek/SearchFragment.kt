package com.kimya.uygulama.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.KimyaData

class SearchFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_search, container, false)
        val input = v.findViewById<EditText>(R.id.search_input)
        val result = v.findViewById<TextView>(R.id.search_result)

        v.findViewById<Button>(R.id.search_button).setOnClickListener {
            val q = input.text.toString().trim()
            if (q.isEmpty()) return@setOnClickListener

            val el = KimyaData.elementBul(q)
            if (el != null) {
                result.text = """|[ELEMENT BULUNDU]
                    |Sembol: ${el.semIol}
                    |Adi: ${el.adi}
                    |Atom No: ${el.atomNo}
                    |Kütle: ${el.kutle} g/mol
                    |Grup: ${el.grup} | Periyot: ${el.periyot}
                    |Tur: ${el.tur}
                    |Valans: ${el.valans.joinToString(", ")}
                    |Hal: ${el.durum}
                    |Elektronegatiflik: ${el.elektronegatiflik}
                    |Iyonlasma Enerjisi: ${el.iyonlasmaEnerjisi} kJ/mol
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
        return v
    }
}
