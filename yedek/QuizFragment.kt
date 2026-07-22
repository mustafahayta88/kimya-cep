package com.kimya.uygulama.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kimya.uygulama.R
import com.kimya.uygulama.viewmodel.KimyaViewModel

class QuizFragment : Fragment() {
    private val vm: KimyaViewModel by activityViewModels()

    private val sorular = listOf(
        Triple("1. Suyun kimyasal formülü nedir?", listOf("H2O", "CO2", "NaCl", "CH4"), 0),
        Triple("2. Asitlerin pH değeri hangi aralıktadır?", listOf("0-7", "7-14", "14-100", "0-14"), 0),
        Triple("3. Hangi element 'H' sembolü ile gösterilir?", listOf("Helyum", "Hidrojen", "Hafniyum", "Holmiyum"), 1),
        Triple("4. Bir mol kaç tanecik içerir?", listOf("6.02x10^23", "3.01x10^23", "1x10^23", "12x10^23"), 0),
        Triple("5. Organik bileşiklerin temel elementi hangisidir?", listOf("Oksijen", "Azot", "Karbon", "Hidrojen"), 2)
    )

    private var currentQ = 0
    private var score = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_quiz, container, false)
        val question = v.findViewById<TextView>(R.id.quiz_question)
        val answerList = v.findViewById<ListView>(R.id.quiz_answer)
        val result = v.findViewById<TextView>(R.id.quiz_result)
        val nextBtn = v.findViewById<Button>(R.id.quiz_next)

        fun loadQuestion() {
            if (currentQ >= sorular.size) {
                question.text = "Tebrikler! Quiz tamamlandı!"
                answerList.adapter = null
                result.text = "Skor: $score/${sorular.size}"
                nextBtn.isEnabled = false
                vm.addHistory("Quiz", "Skor: $score/${sorular.size}")
                return
            }
            val (q, opts, _) = sorular[currentQ]
            question.text = q
            answerList.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, opts)
            result.text = "Soru ${currentQ + 1}/${sorular.size} | Skor: $score"
            nextBtn.isEnabled = false
        }

        answerList.setOnItemClickListener { _, _, pos, _ ->
            val correct = sorular[currentQ].third
            if (pos == correct) {
                score++
                result.text = "Doğru! Skor: $score"
            } else {
                result.text = "Yanlış! Cevap: ${sorular[currentQ].second[correct]}"
            }
            nextBtn.isEnabled = true
        }

        nextBtn.setOnClickListener {
            currentQ++
            loadQuestion()
        }

        val restartBtn = v.findViewById<Button>(R.id.quiz_restart)
        restartBtn.setOnClickListener {
            currentQ = 0; score = 0
            loadQuestion()
            nextBtn.isEnabled = false
        }

        loadQuestion()
        return v
    }
}
