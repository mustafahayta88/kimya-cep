package com.kimya.uygulama.features

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.AnimUtils

class TimerFragment : Fragment() {

    private var countdownTimer: CountDownTimer? = null
    private var stopwatchHandler: Handler? = null
    private var stopwatchRunnable: Runnable? = null
    private var isCountdownRunning = false
    private var isStopwatchRunning = false
    private var countdownMillis = 0L
    private var stopwatchMillis = 0L
    private var lapCount = 0
    private val laps = mutableListOf<String>()

    private lateinit var modeCountdownBtn: Button
    private lateinit var modeStopwatchBtn: Button
    private lateinit var countdownContainer: LinearLayout
    private lateinit var stopwatchContainer: LinearLayout
    private lateinit var countdownDisplay: TextView
    private lateinit var stopwatchDisplay: TextView
    private lateinit var lapList: LinearLayout
    private lateinit var presetContainer: LinearLayout

    private val presets = listOf(
        "1 dk" to 60_000L, "3 dk" to 180_000L, "5 dk" to 300_000L,
        "10 dk" to 600_000L, "15 dk" to 900_000L, "20 dk" to 1_200_000L,
        "30 dk" to 1_800_000L, "45 dk" to 2_700_000L, "60 dk" to 3_600_000L
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_timer, container, false)

        modeCountdownBtn = v.findViewById(R.id.timer_mode_countdown)
        modeStopwatchBtn = v.findViewById(R.id.timer_mode_stopwatch)
        countdownContainer = v.findViewById(R.id.timer_countdown_container)
        stopwatchContainer = v.findViewById(R.id.timer_stopwatch_container)
        countdownDisplay = v.findViewById(R.id.timer_countdown_display)
        stopwatchDisplay = v.findViewById(R.id.timer_stopwatch_display)
        lapList = v.findViewById(R.id.timer_lap_list)
        presetContainer = v.findViewById(R.id.timer_preset_container)

        val minuteInput = v.findViewById<EditText>(R.id.timer_minute_input)
        val secondInput = v.findViewById<EditText>(R.id.timer_second_input)

        for ((label, millis) in presets) {
            val btn = Button(requireContext()).apply {
                text = label
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(16, 8, 16, 8)
                backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.neon_cyan)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 8, 0) }
                setOnClickListener {
                    minuteInput.setText((millis / 60000).toString())
                    secondInput.setText(((millis % 60000) / 1000).toString())
                    Toast.makeText(context, "Sure ayarlandi: $label", Toast.LENGTH_SHORT).show()
                }
            }
            presetContainer.addView(btn)
        }

        v.findViewById<Button>(R.id.timer_cd_start).setOnClickListener {
            if (isCountdownRunning) { stopCountdown(); return@setOnClickListener }
            val mins = minuteInput.text.toString().toIntOrNull() ?: 0
            val secs = secondInput.text.toString().toIntOrNull() ?: 0
            val total = mins * 60_000L + secs * 1000L
            if (total <= 0) { Toast.makeText(context, "Sure girin", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            startCountdown(total)
        }

        v.findViewById<Button>(R.id.timer_cd_stop).setOnClickListener { stopCountdown() }

        v.findViewById<Button>(R.id.timer_sw_start).setOnClickListener {
            if (isStopwatchRunning) { pauseStopwatch() } else { startStopwatch() }
        }

        v.findViewById<Button>(R.id.timer_sw_stop).setOnClickListener { resetStopwatch() }

        v.findViewById<Button>(R.id.timer_sw_lap).setOnClickListener {
            if (isStopwatchRunning || stopwatchMillis > 0) { recordLap() }
        }

        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Kimya Zamanlayici")
                .setMessage("Deneyleriniz icin zaman tutun!\n\n" +
                    "Geri Sayim:\n" +
                    "- Dakika ve saniye olarak sure girin\n" +
                    "- Hazir surelerden birini secebilirsiniz\n" +
                    "- Baslat ile geri sayimi baslatin\n\n" +
                    "Kronometre:\n" +
                    "- Baslat/Duraklat ile zaman tutun\n" +
                    "- Tur ile ara sureleri kaydedin\n" +
                    "- Sifirla ile basa donun\n\n" +
                    "Laboratuvarda reaksiyon surelerini olcerken cok ise yarar.")
                .setPositiveButton("Anladim", null)
                .show()
        }

        modeCountdownBtn.setOnClickListener { showMode(false) }
        modeStopwatchBtn.setOnClickListener { showMode(true) }

        showMode(false)
        return v
    }

    private fun showMode(isStopwatch: Boolean) {
        countdownContainer.visibility = if (isStopwatch) View.GONE else View.VISIBLE
        stopwatchContainer.visibility = if (isStopwatch) View.VISIBLE else View.GONE
        modeCountdownBtn.alpha = if (isStopwatch) 0.5f else 1f
        modeStopwatchBtn.alpha = if (isStopwatch) 1f else 0.5f
    }

    private fun startCountdown(millis: Long) {
        countdownMillis = millis
        isCountdownRunning = true
        countdownTimer = object : CountDownTimer(millis, 50) {
            override fun onTick(millisUntilFinished: Long) {
                countdownMillis = millisUntilFinished
                countdownDisplay.text = formatTime(millisUntilFinished)
                countdownDisplay.setTextColor(
                    if (millisUntilFinished <= 10_000) 0xFFFF4444.toInt()
                    else if (millisUntilFinished <= 30_000) 0xFFFFA500.toInt()
                    else 0xFF4DD0E1.toInt()
                )
            }
            override fun onFinish() {
                countdownDisplay.text = "00:00.0"
                countdownDisplay.setTextColor(0xFF81C784.toInt())
                isCountdownRunning = false
                Toast.makeText(context, "Sure doldu!", Toast.LENGTH_LONG).show()
                try {
                    val vb = requireContext().getSystemService(android.os.Vibrator::class.java)
                    vb?.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun stopCountdown() {
        countdownTimer?.cancel()
        isCountdownRunning = false
        countdownDisplay.text = "00:00.0"
        countdownDisplay.setTextColor(0xFF4DD0E1.toInt())
    }

    private fun startStopwatch() {
        isStopwatchRunning = true
        val startTime = System.currentTimeMillis() - stopwatchMillis
        stopwatchHandler = Handler(Looper.getMainLooper())
        stopwatchRunnable = object : Runnable {
            override fun run() {
                stopwatchMillis = System.currentTimeMillis() - startTime
                stopwatchDisplay.text = formatTime(stopwatchMillis)
                stopwatchHandler?.postDelayed(this, 50)
            }
        }
        stopwatchRunnable?.let { stopwatchHandler?.post(it) }
    }

    private fun pauseStopwatch() {
        isStopwatchRunning = false
        stopwatchRunnable?.let { stopwatchHandler?.removeCallbacks(it) }
    }

    private fun resetStopwatch() {
        isStopwatchRunning = false
        stopwatchRunnable?.let { stopwatchHandler?.removeCallbacks(it) }
        stopwatchMillis = 0
        lapCount = 0
        laps.clear()
        lapList.removeAllViews()
        stopwatchDisplay.text = "00:00.0"
    }

    private fun recordLap() {
        lapCount++
        val lapTime = formatTime(stopwatchMillis)
        laps.add(0, "Tur $lapCount: $lapTime")
        lapList.removeAllViews()
        for (lap in laps.take(10)) {
            val tv = TextView(requireContext()).apply {
                text = lap
                setTextColor(0xFFAAAAAA.toInt())
                textSize = 13f
                setPadding(8, 4, 8, 4)
            }
            lapList.addView(tv)
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSec = millis / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        val tenths = (millis % 1000) / 100
        return "%02d:%02d.%d".format(mins, secs, tenths)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        stopwatchHandler?.removeCallbacksAndMessages(null)
    }
}

