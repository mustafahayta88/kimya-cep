package com.kimya.uygulama.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.PdfExporter
import com.kimya.uygulama.viewmodel.KimyaViewModel
import java.io.File

class NotFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_not, container, false)
        val edit = v.findViewById<EditText>(R.id.not_edit)

        val notlarDosya = File(requireContext().filesDir, "notlar.txt")
        if (notlarDosya.exists()) edit.setText(notlarDosya.readText())

        v.findViewById<Button>(R.id.not_kaydet).setOnClickListener {
            notlarDosya.writeText(edit.text.toString())
            vm.addHistory("Not Kaydedildi", edit.text.toString().take(100) + if (edit.text.length > 100) "..." else "")
            Toast.makeText(requireContext(), "Notlar kaydedildi", Toast.LENGTH_SHORT).show()
        }
        v.findViewById<Button>(R.id.not_temizle).setOnClickListener {
            edit.text.clear()
            notlarDosya.writeText("")
            Toast.makeText(requireContext(), "Temizlendi", Toast.LENGTH_SHORT).show()
        }
        v.findViewById<Button>(R.id.not_paylas).setOnClickListener {
            if (edit.text.isBlank()) { Toast.makeText(context, "Not boş", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PdfExporter.shareText(requireContext(), "Lab Notlari", edit.text.toString())
        }
        v.findViewById<Button>(R.id.not_kaydet_dosya).setOnClickListener {
            val content = edit.text.toString()
            if (content.isBlank()) { Toast.makeText(context, "Not boş", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            PdfExporter.saveToFile(requireContext(), "lab_notlari_${System.currentTimeMillis()}.txt", content)
        }
        return v
    }
}
