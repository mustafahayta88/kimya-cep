package com.kimya.uygulama.features

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R

class TimerFragment : Fragment() {
    private var timer: CountDownTimer? = null
    private var isRunning = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_timer, container, false)
        val timeDisplay = v.findViewById<TextView>(R.id.timer_time)
        val minuteInput = v.findViewById<EditText>(R.id.timer_input)
        val startBtn = v.findViewById<Button>(R.id.timer_start)
        val stopBtn = v.findViewById<Button>(R.id.timer_stop)

        startBtn.setOnClickListener {
            if (isRunning) { timer?.cancel(); isRunning = false }
            val mins = minuteInput.text.toString().toIntOrNull() ?: 1
            val millis = mins * 60 * 1000L
            timer = object : CountDownTimer(millis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val sec = millisUntilFinished / 1000
                    timeDisplay.text = "${sec / 60}:${"%02d".format(sec % 60)}"
                }
                override fun onFinish() {
                    timeDisplay.text = "00:00"
                    isRunning = false
                    startBtn.text = "Baslat"
                }
            }.start()
            isRunning = true
            startBtn.text = "Yeniden Baslat"
        }

        stopBtn.setOnClickListener {
            timer?.cancel()
            isRunning = false
            startBtn.text = "Baslat"
            timeDisplay.text = "00:00"
        }
        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Kimya Timer")
                .setMessage("Deneyleriniz icin zaman tutun!\n\n" +
                    "- Dakika olarak sure girin\n" +
                    "- Baslat dugmesiyle geri sayimi baslatin\n" +
                    "- Durdur dugmesiyle duraklatin\n" +
                    "- Sure doldugunda 00:00 gorunur\n\n" +
                    "Laboratuvarda reaksiyon surelerini olcerken cok ise yarar.")
                .setPositiveButton("Anladim") { d, _ -> d.dismiss() }
                .show()
        }
        return v
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
