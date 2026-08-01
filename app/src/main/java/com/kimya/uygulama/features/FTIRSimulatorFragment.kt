package com.kimya.uygulama.features

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.kimya.uygulama.R
import com.kimya.uygulama.views.FTIRSimulatorView

class FTIRSimulatorFragment : Fragment() {

    private lateinit var ftirView: FTIRSimulatorView
    private lateinit var tvSelectedGroups: TextView
    private lateinit var tvResolution: TextView
    private lateinit var tvScanCount: TextView
    private lateinit var tvActiveSample: TextView
    private lateinit var tvGroupDesc: TextView
    private lateinit var btnScan: Button
    private lateinit var groupChipsContainer: LinearLayout
    private var currentCompoundName: String? = null

    private var themePrimary = Color.rgb(0, 240, 200)
    private var themeAccent = Color.rgb(57, 255, 20)
    private var themeText = Color.rgb(220, 230, 240)
    private var themeMuted = Color.rgb(90, 106, 122)
    private var themeBg = Color.rgb(10, 14, 20)
    private var themeSurface = Color.rgb(21, 28, 36)
    private var themeLine = Color.rgb(30, 40, 50)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_ftir_simulator, container, false)

        resolveThemeColors()

        ftirView = v.findViewById(R.id.ftir_view)
        tvSelectedGroups = v.findViewById(R.id.tv_selected_groups)
        tvResolution = v.findViewById(R.id.tv_resolution)
        tvScanCount = v.findViewById(R.id.tv_scan_count)
        tvActiveSample = v.findViewById(R.id.tv_active_sample)
        tvGroupDesc = v.findViewById(R.id.tv_group_desc)
        btnScan = v.findViewById(R.id.btn_scan)
        groupChipsContainer = v.findViewById(R.id.group_chips)

        ftirView.setThemeColors(FTIRSimulatorView.ThemeColors(
            bg = themeBg, surface = themeSurface, primary = themePrimary,
            text = themeText, muted = themeMuted, accent = themeAccent, line = themeLine
        ))

        applyThemeToViews(v)
        buildGroupChips()
        setupControls(v)

        v.findViewById<Button>(R.id.btn_ftir_help).setOnClickListener { ftirView.toggleInfo() }

        return v
    }

    private fun setupControls(v: View) {
        val spinner = v.findViewById<Spinner>(R.id.spinner_sample_type)
        spinner.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            FTIRSimulatorView.SAMPLE_TYPES.map { it.first })
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                ftirView.setSampleType(FTIRSimulatorView.SAMPLE_TYPES[pos].first)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val resButtons = mapOf(
            R.id.btn_res_1 to 1, R.id.btn_res_2 to 2,
            R.id.btn_res_4 to 4, R.id.btn_res_8 to 8
        )
        resButtons.forEach { (btnId, res) ->
            v.findViewById<com.google.android.material.button.MaterialButton>(btnId)?.let { btn ->
                btn.setOnClickListener {
                    ftirView.setResolution(res)
                    tvResolution.text = "$res cm⁻¹"
                    updateResolutionSelection(v, btnId)
                }
                if (res == 4) {
                    btn.setBackgroundColor(themePrimary)
                    btn.setTextColor(Color.WHITE)
                }
            }
        }

        val scanButtons = mapOf(
            R.id.btn_scan_1 to 1, R.id.btn_scan_4 to 4,
            R.id.btn_scan_16 to 16, R.id.btn_scan_64 to 64
        )
        scanButtons.forEach { (btnId, count) ->
            v.findViewById<com.google.android.material.button.MaterialButton>(btnId)?.let { btn ->
                btn.setOnClickListener {
                    ftirView.setScanCount(count)
                    tvScanCount.text = "$count"
                    updateScanSelection(v, btnId)
                }
                if (count == 16) {
                    btn.setBackgroundColor(themePrimary)
                    btn.setTextColor(Color.WHITE)
                }
            }
        }

        btnScan.setOnClickListener {
            ftirView.startScan()
            btnScan.isEnabled = false
            btnScan.text = "SCANNING... %0"
            val startTime = System.currentTimeMillis()
            val duration = 2000L
            val updater = object : Runnable {
                override fun run() {
                    if (!isAdded) return
                    val elapsed = System.currentTimeMillis() - startTime
                    val pct = ((elapsed.toFloat() / duration) * 100).toInt().coerceAtMost(100)
                    btnScan.text = "SCANNING... %$pct"
                    if (elapsed < duration) {
                        btnScan.postDelayed(this, 50L)
                    } else {
                        btnScan.isEnabled = true
                        btnScan.text = "SCAN"
                    }
                }
            }
            btnScan.postDelayed(updater, 50L)
        }

        v.findViewById<Button>(R.id.btn_cookbook).setOnClickListener { showCookbook() }

        val btnToggleInterferogram = v.findViewById<Button>(R.id.btn_toggle_interferogram)
        btnToggleInterferogram?.setOnClickListener {
            ftirView.showInterferogram = !ftirView.showInterferogram
            btnToggleInterferogram.text = if (ftirView.showInterferogram) "SPECTRUM" else "INTERFEROGRAM"
        }
    }

    private fun updateResolutionSelection(v: View, selectedId: Int) {
        listOf(R.id.btn_res_1, R.id.btn_res_2, R.id.btn_res_4, R.id.btn_res_8).forEach { id ->
            v.findViewById<com.google.android.material.button.MaterialButton>(id)?.let { btn ->
                if (id == selectedId) {
                    btn.setBackgroundColor(themePrimary)
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(themePrimary)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(themePrimary)
                    btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                }
            }
        }
    }

    private fun updateScanSelection(v: View, selectedId: Int) {
        listOf(R.id.btn_scan_1, R.id.btn_scan_4, R.id.btn_scan_16, R.id.btn_scan_64).forEach { id ->
            v.findViewById<com.google.android.material.button.MaterialButton>(id)?.let { btn ->
                if (id == selectedId) {
                    btn.setBackgroundColor(themePrimary)
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(themePrimary)
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(themePrimary)
                    btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                }
            }
        }
    }

    private fun buildGroupChips() {
        groupChipsContainer.removeAllViews()
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val dp12 = (12 * resources.displayMetrics.density).toInt()
        val dp16 = (16 * resources.displayMetrics.density).toInt()

        for (group in FTIRSimulatorView.FUNCTIONAL_GROUPS) {
            val chip = MaterialCardView(requireContext()).apply {
                radius = 26f * resources.displayMetrics.density
                cardElevation = 3f * resources.displayMetrics.density
                val bg = GradientDrawable().apply {
                    setColor(themeSurface)
                    cornerRadius = 26f * resources.displayMetrics.density
                    setStroke(dp4, group.color)
                }
                background = bg
                setCardBackgroundColor(Color.TRANSPARENT)
                useCompatPadding = false
                preventCornerOverlap = false
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp16, dp12, dp16, dp12)
            }

            val dot = View(requireContext()).apply {
                val size = (16 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp8 }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(group.color)
                }
            }

            val textCol = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }

            val name = TextView(requireContext()).apply {
                text = group.nameTr
                setTextColor(Color.WHITE)
                textSize = 12f
                setSingleLine(true)
            }

            val wn = TextView(requireContext()).apply {
                text = "${group.peakCenter.toInt()} cm⁻¹"
                setTextColor(group.color)
                textSize = 10f
                paint.isFakeBoldText = true
                setSingleLine(true)
            }

            textCol.addView(name)
            textCol.addView(wn)

            inner.addView(dot)
            inner.addView(textCol)
            chip.addView(inner)

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp4; marginEnd = dp4; bottomMargin = dp4 }
            chip.layoutParams = lp

            chip.setOnClickListener {
                ftirView.toggleGroup(group.id)
                currentCompoundName = null
                updateChipSelection()
                updateReadings()
                tvGroupDesc.text = group.description
            }

            groupChipsContainer.addView(chip)
        }
    }

    private fun updateChipSelection() {
        for (i in 0 until groupChipsContainer.childCount) {
            val chip = groupChipsContainer.getChildAt(i) as? MaterialCardView ?: continue
            val innerLayout = chip.getChildAt(0) as? LinearLayout ?: continue
            val textCol = innerLayout.getChildAt(1) as? LinearLayout ?: continue
            val nameText = textCol.getChildAt(0) as? TextView ?: continue

            val groupId = FTIRSimulatorView.FUNCTIONAL_GROUPS[i].id
            val selected = ftirView.selectedGroups.contains(groupId)

            if (selected) {
                chip.cardElevation = 6f * resources.displayMetrics.density
                nameText.setTextColor(FTIRSimulatorView.FUNCTIONAL_GROUPS[i].color)
            } else {
                chip.cardElevation = 3f * resources.displayMetrics.density
                nameText.setTextColor(Color.WHITE)
            }
        }
    }

    private fun updateReadings() {
        val groups = ftirView.selectedGroups
        if (groups.isEmpty()) {
            tvSelectedGroups.text = "NONE"
            tvActiveSample.visibility = View.GONE
        } else {
            val names = FTIRSimulatorView.FUNCTIONAL_GROUPS
                .filter { groups.contains(it.id) }
                .map { it.name.split("(").first().trim() }
            tvSelectedGroups.text = names.joinToString(", ")
        }

        currentCompoundName?.let {
            tvActiveSample.text = "SAMPLE: $it"
            tvActiveSample.visibility = View.VISIBLE
        } ?: run {
            tvActiveSample.visibility = View.GONE
        }
    }

    private fun showCookbook() {
        val items = FTIRSimulatorView.COOKBOOK_COMPOUNDS.map { "${it.name} (${it.formula})" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Library — Yaygın Bileşikler")
            .setItems(items) { _, which ->
                val compound = FTIRSimulatorView.COOKBOOK_COMPOUNDS[which]
                ftirView.selectPreset(compound)
                currentCompoundName = compound.name
                updateChipSelection()
                updateReadings()
                tvGroupDesc.text = compound.description
                tvActiveSample.text = "SAMPLE: ${compound.name}"
                tvActiveSample.visibility = View.VISIBLE
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateReadings()
    }

    private fun resolveThemeColors() {
        val ctx = requireContext()
        val tv = TypedValue()

        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, tv, true)
        themePrimary = tv.data

        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorAccent, tv, true)
        themeAccent = tv.data

        ctx.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
        themeText = tv.data

        ctx.theme.resolveAttribute(android.R.attr.textColorSecondary, tv, true)
        themeMuted = tv.data

        ctx.theme.resolveAttribute(android.R.attr.windowBackground, tv, true)
        themeBg = tv.data

        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, tv, true)
        themeSurface = tv.data

        ctx.theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true)
        themeLine = tv.data
    }

    private fun applyThemeToViews(v: View) {
        v.findViewById<Button>(R.id.btn_ftir_help)?.backgroundTintList =
            android.content.res.ColorStateList.valueOf(themePrimary)
        btnScan.backgroundTintList = android.content.res.ColorStateList.valueOf(themePrimary)
    }
}
