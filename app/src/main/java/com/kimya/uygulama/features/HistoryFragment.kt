package com.kimya.uygulama.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.kimya.uygulama.R
import com.kimya.uygulama.db.HistoryEntry
import com.kimya.uygulama.viewmodel.KimyaViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()
    private lateinit var listView: ListView
    private lateinit var infoText: TextView
    private var allEntries = listOf<HistoryEntry>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_history, container, false)
        listView = v.findViewById(R.id.hist_list)
        infoText = v.findViewById(R.id.hist_info)
        val clearBtn = v.findViewById<Button>(R.id.hist_clear)

        lifecycleScope.launch {
            vm.history.collectLatest { entries ->
                allEntries = entries
                val items = entries.map { "${it.islemAdi} - ${it.detay.take(50)}" }
                listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
                infoText.text = "Toplam ${entries.size} kayit"
            }
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val e = allEntries[pos]
            infoText.text = "${e.islemAdi}\n${e.detay}\n${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(e.zaman))}"
        }

        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Gecmis")
                .setMessage("Yaptiginiz tum islemlerin kaydini goruntuleyebilirsiniz.\n\n" +
                    "- Her islem tarihi ve saati ile kaydedilir\n" +
                    "- Listeden bir kayda dokunarak detaylari gorebilirsiniz\n" +
                    "- Temizle dugmesiyle tum gecmisi silebilirsiniz\n\n" +
                    "Hangi modulleri ne zaman kullandiginizi takip edebilirsiniz.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        clearBtn.setOnClickListener { vm.clearHistory() }
        return v
    }
}
