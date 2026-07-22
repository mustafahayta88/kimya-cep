package com.kimya.uygulama.fragments

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kimya.uygulama.R
import com.kimya.uygulama.utils.ElementData
import com.kimya.uygulama.utils.KimyaData
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AtomPlaceholderView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {
    private var animAngle = 0f
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 8000L; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animAngle = it.animatedValue as Float; invalidate() }
    }
    init { animator.start() }

    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f
        val maxR = min(w, h) / 2f - 20f

        // Outer glow
        val glowPaint = Paint().apply {
            shader = RadialGradient(cx, cy, maxR * 1.2f,
                intArrayOf(Color.argb(0, 0, 240, 255), Color.argb(20, 0, 240, 255), Color.argb(0, 0, 240, 255)),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, maxR * 1.2f, glowPaint)

        // Orbital rings
        val ringColors = intArrayOf(
            Color.argb(80, 0, 240, 255),
            Color.argb(60, 180, 255, 100),
            Color.argb(60, 255, 100, 200)
        )
        val ringAngles = floatArrayOf(0f, 60f, -30f)
        val ringRadii = floatArrayOf(maxR * 0.95f, maxR * 0.75f, maxR * 0.55f)

        for (i in 0..2) {
            canvas.save()
            canvas.rotate(ringAngles[i], cx, cy)
            val ringPaint = Paint().apply {
                color = ringColors[i]; style = Paint.Style.STROKE; strokeWidth = 1.8f
                isAntiAlias = true
            }
            canvas.drawOval(cx - ringRadii[i], cy - ringRadii[i] * 0.35f,
                cx + ringRadii[i], cy + ringRadii[i] * 0.35f, ringPaint)
            canvas.restore()
        }

        // Electrons on orbits
        val electronPaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
        val electronGlow = Paint().apply {
            color = Color.argb(100, 0, 240, 255); isAntiAlias = true; style = Paint.Style.FILL
        }
        for (i in 0..2) {
            val angle = Math.toRadians((animAngle * (1.5f - i * 0.3f)).toDouble())
            canvas.save()
            canvas.rotate(ringAngles[i], cx, cy)
            val ex = cx + ringRadii[i] * cos(angle).toFloat()
            val ey = cy + ringRadii[i] * 0.35f * sin(angle).toFloat()
            canvas.drawCircle(ex, ey, 8f, electronGlow)
            canvas.drawCircle(ex, ey, 4f, electronPaint)
            canvas.restore()
        }

        // Nucleus with glow
        val nucGlow = Paint().apply {
            shader = RadialGradient(cx, cy, maxR * 0.15f,
                intArrayOf(Color.argb(200, 255, 80, 80), Color.argb(80, 255, 40, 40), Color.argb(0, 255, 40, 40)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, maxR * 0.15f, nucGlow)

        val nucPaint = Paint().apply {
            shader = RadialGradient(cx - 5f, cy - 5f, maxR * 0.1f,
                intArrayOf(Color.rgb(255, 120, 120), Color.rgb(200, 40, 40)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL; isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, maxR * 0.1f, nucPaint)

        val nucText = Paint().apply {
            color = Color.WHITE; textSize = maxR * 0.08f
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("e⁻", cx, cy + maxR * 0.03f, nucText)
    }
}

class AtomModelView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle) {
    var semIol: String? = null
    var atomNo: Int = 0
    var elektronConIig: String? = null
    private var animAngle = 0f
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 12000L; repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animAngle = it.animatedValue as Float; invalidate() }
    }
    init { isClickable = true; isFocusable = true; animator.start() }

    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = semIol ?: return
        val ec = elektronConIig ?: return
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val cx = w / 2f; val cy = h / 2f
        val maxR = min(w, h) / 2f - 20f
        val shells = parseElectronShells(ec, atomNo)
        if (shells.isEmpty()) return

        // Background glow
        val bgGlow = Paint().apply {
            shader = RadialGradient(cx, cy, maxR,
                intArrayOf(Color.argb(15, 0, 240, 255), Color.argb(0, 0, 240, 255)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, maxR, bgGlow)

        val nucR = maxR * 0.09f + 14f

        // Orbital rings with gradient
        val shellColors = intArrayOf(
            Color.argb(100, 0, 200, 255),
            Color.argb(80, 0, 255, 180),
            Color.argb(70, 180, 100, 255),
            Color.argb(60, 255, 200, 50),
            Color.argb(50, 255, 100, 150),
            Color.argb(50, 100, 255, 200),
            Color.argb(40, 200, 200, 255)
        )

        for (i in shells.indices) {
            val r = maxR * (i + 1).toFloat() / (shells.size + 1).toFloat() + nucR + 8f
            val ringPaint = Paint().apply {
                color = shellColors[i % shellColors.size]
                style = Paint.Style.STROKE; strokeWidth = 1.8f; isAntiAlias = true
            }
            canvas.drawCircle(cx, cy, r, ringPaint)

            // Electrons
            val count = min(shells[i], 32)
            val eSpeed = 1f + i * 0.4f
            for (e in 0 until count) {
                val angle = Math.toRadians((-90.0 + e * (360.0 / count) + animAngle * eSpeed))
                val ex = cx + r * cos(angle).toFloat()
                val ey = cy + r * sin(angle).toFloat()

                // Electron glow
                val glow = Paint().apply {
                    color = Color.argb(60, 57, 255, 20); style = Paint.Style.FILL
                }
                canvas.drawCircle(ex, ey, 7f, glow)
                // Electron core
                val ePaint = Paint().apply {
                    shader = RadialGradient(ex, ey, 4f,
                        intArrayOf(Color.WHITE, Color.rgb(57, 255, 20)),
                        floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                    style = Paint.Style.FILL; isAntiAlias = true
                }
                canvas.drawCircle(ex, ey, 4f, ePaint)
            }

            // Shell label
            val labelPaint = Paint().apply {
                color = Color.argb(120, Color.red(shellColors[i % shellColors.size]),
                    Color.green(shellColors[i % shellColors.size]),
                    Color.blue(shellColors[i % shellColors.size]))
                textSize = 10f; textAlign = Paint.Align.CENTER; isAntiAlias = true
            }
            canvas.drawText("n=${i+1} (${shells[i]}e⁻)", cx, cy - r - 8f, labelPaint)
        }

        // Nucleus with glow + gradient
        val nucGlow = Paint().apply {
            shader = RadialGradient(cx, cy, nucR * 1.8f,
                intArrayOf(Color.argb(120, 255, 60, 60), Color.argb(0, 255, 60, 60)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, nucR * 1.8f, nucGlow)

        val nucPaint = Paint().apply {
            shader = RadialGradient(cx - 4f, cy - 4f, nucR,
                intArrayOf(Color.rgb(255, 130, 100), Color.rgb(200, 40, 40)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            style = Paint.Style.FILL; isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, nucR, nucPaint)

        val nucBorder = Paint().apply {
            color = Color.argb(80, 255, 200, 200); style = Paint.Style.STROKE; strokeWidth = 1.5f
        }
        canvas.drawCircle(cx, cy, nucR, nucBorder)

        val nucText = Paint().apply {
            color = Color.WHITE; textSize = nucR * 0.9f
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText(s, cx, cy + nucR * 0.35f, nucText)

        // Atom name below
        val namePaint = Paint().apply {
            color = Color.argb(150, 200, 220, 255); textSize = 13f
            textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        canvas.drawText("Z=$atomNo", cx, cy + nucR + 22f, namePaint)
    }

    private fun parseElectronShells(config: String, atomNo: Int): List<Int> {
        var cfg = config.trim()
        var base = 0
        for ((core, cnt) in mapOf("[He]" to 2, "[Ne]" to 10, "[Ar]" to 18, "[Kr]" to 36, "[Xe]" to 54, "[Rn]" to 86)) {
            if (cfg.startsWith(core)) { base = cnt; cfg = cfg.removePrefix(core).trim(); break }
        }
        val shells = mutableListOf<Int>()
        var rem = base; var n = 1
        while (rem > 0) { val c = min(2 * n * n, rem); shells.add(c); rem -= c; n++ }
        val sub = mutableMapOf<Int, Int>()
        for (m in Regex("""(\d+)[spdI](\d+)""").findAll(cfg)) {
            val s = m.groupValues[1].toInt(); val c = m.groupValues[2].toInt()
            sub[s] = (sub[s] ?: 0) + c
        }
        for ((sn, cnt) in sub) { while (shells.size < sn) shells.add(0); shells[sn-1] = shells[sn-1] + cnt }
        return shells
    }
}

class ElementFragment : Fragment() {

    private var quickElements = listOf(
        "H", "C", "N", "O", "Na", "Fe", "Cu", "Ag", "Au", "Hg", "U", "He", "Li", "Si", "Cl", "Ca"
    )
    private val categoryInfo = listOf(
        Triple("Alkali Metal", 0xFFFF6347.toInt(), "M+"),
        Triple("Toprak Alkali", 0xFFFF8C00.toInt(), "M2+"),
        Triple("Gecis Metali", 0xFF00CED1.toInt(), "Mn+"),
        Triple("Yari Metal", 0xFFDA70D6.toInt(), "~"),
        Triple("Ametal", 0xFF39FF14.toInt(), "-"),
        Triple("Soy Gaz", 0xFFFF69B4.toInt(), "0"),
        Triple("Lantanit", 0xFF32CD32.toInt(), "Ln"),
        Triple("Aktinit", 0xFFFFA500.toInt(), "Ac")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_element, container, false)
        val arama = v.findViewById<EditText>(R.id.el_arama)
        val emptyState = v.findViewById<LinearLayout>(R.id.el_empty)
        val detailState = v.findViewById<LinearLayout>(R.id.el_detail)
        val propBars = v.findViewById<LinearLayout>(R.id.el_prop_bars)
        val legendRow = v.findViewById<LinearLayout>(R.id.el_legend)
        val quickGrid = v.findViewById<GridLayout>(R.id.el_quick_grid)

        // -- Helper functions FIRST --
        fun propBar(label: String, value: Double, maxVal: Double, renk: Int): View {
            val lay = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL; setPadding(0, 6, 0, 6)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val txt = TextView(requireContext()).apply {
                text = label; setTextColor(0xFFCCCCCC.toInt()); textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valTxt = TextView(requireContext()).apply {
                text = if (value == value.toLong().toDouble()) value.toLong().toString() else "%.3f".format(value)
                setTextColor(renk); textSize = 12f; typeface = Typeface.MONOSPACE
            }
            row.addView(txt); row.addView(valTxt); lay.addView(row)
            val barBg = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 10); setPadding(0, 3, 0, 0)
                background = GradientDrawable().apply { setColor(0x33FFFFFF.toInt()); cornerRadius = 5f }
            }
            val barW = if (maxVal > 0) (value / maxVal * 10000).toInt().coerceIn(0, 10000) else 0
            val barFill = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(barW, 8)
                background = GradientDrawable().apply { setColor(renk); cornerRadius = 5f }
            }
            barBg.addView(barFill); lay.addView(barBg)
            return lay
        }

        fun showElement(q: String) {
            val el = KimyaData.elementBul(q) ?: return
            emptyState.visibility = View.GONE
            detailState.visibility = View.VISIBLE

            val atomCanvas = v.findViewById<com.kimya.uygulama.fragments.AtomModelView>(R.id.el_atom_canvas)
            atomCanvas.semIol = el.semIol; atomCanvas.atomNo = el.atomNo
            atomCanvas.elektronConIig = el.elektron; atomCanvas.invalidate()

            val color = KimyaData.elementRengi(el.tur)
            v.findViewById<TextView>(R.id.el_sym_badge).apply {
                text = el.semIol; background = GradientDrawable().apply { setColor(color); cornerRadius = 16f }
            }
            v.findViewById<TextView>(R.id.el_name).text = "${el.adi} (${el.semIol})"
            v.findViewById<TextView>(R.id.el_type_badge).apply {
                text = el.tur; background = GradientDrawable().apply {
                    setColor(color and 0x55FFFFFF.toInt()); cornerRadius = 10f; setStroke(1, color)
                }
            }
            val grupAdlari = mapOf(
                1 to "Alkali Metaller", 2 to "Toprak Alkali", 3 to "Skandiyum Grubu",
                4 to "Titan Grubu", 5 to "Vanadyum Grubu", 6 to "Krom Grubu",
                7 to "Manganez Grubu", 8 to "Demir Grubu", 9 to "Kobalt Grubu",
                10 to "Nikel Grubu", 11 to "Bakir Grubu", 12 to "Cinko Grubu",
                13 to "Bor Grubu", 14 to "Karbon Grubu", 15 to "Azot Grubu",
                16 to "Oksijen Grubu", 17 to "Halojenler", 18 to "Soy Gazlar"
            )
            v.findViewById<TextView>(R.id.el_subtitle).text =
                "${grupAdlari[el.grup] ?: ""} | ${el.periyot}. Periyot | ${el.durum}"
            v.findViewById<TextView>(R.id.el_stat_z).text = "${el.atomNo}"
            v.findViewById<TextView>(R.id.el_stat_mass).text = "%.2f".format(el.kutle)
            v.findViewById<TextView>(R.id.el_stat_group).text = "${el.grup}"
            v.findViewById<TextView>(R.id.el_stat_period).text = "${el.periyot}"

            propBars.removeAllViews()
            if (el.iyonlasmaEnerjisi > 0)
                propBars.addView(propBar("Iyonlasma Enerjisi (kJ/mol)", el.iyonlasmaEnerjisi, 2500.0, 0xFFFFB400.toInt()))
            if (el.elektronegatiflik > 0)
                propBars.addView(propBar("Elektronegatiflik (Pauling)", el.elektronegatiflik, 4.0, 0xFF00C8FF.toInt()))
            propBars.addView(propBar("Atom Kutlesi (g/mol)", el.kutle, 300.0, 0xFF39FF14.toInt()))
            propBars.addView(propBar("Periyot", el.periyot.toDouble(), 7.0, 0xFFB388FF.toInt()))
            propBars.addView(propBar("Grup", el.grup.toDouble(), 18.0, 0xFFFF0080.toInt()))

            v.findViewById<TextView>(R.id.el_electron_config).text = el.elektron

            val valRow = v.findViewById<LinearLayout>(R.id.el_valence_row); valRow.removeAllViews()
            for (v2 in el.valans) {
                val chip = TextView(requireContext()).apply {
                    text = if (v2 > 0) "+$v2" else "$v2"
                    textSize = 12f; setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE
                    setPadding(10, 4, 10, 4)
                    background = GradientDrawable().apply {
                        setColor(0x4400F0FF.toInt()); cornerRadius = 8f; setStroke(1, 0xFF00F0FF.toInt())
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(4, 2, 4, 2) }
                }
                valRow.addView(chip)
            }

            val metalText = if (el.tur.contains("Metal") || el.tur == "Toprak Alkali" || el.tur == "Lantanit" || el.tur == "Aktinit") {
                "Metalik ozellik gosterir. Isi ve elektrik iletkenligi yuksektir. Parlak gorunume sahiptir."
            } else {
                "Ametalik ozellik gosterir. Isi ve elektrik iletkenligi dusuktur. Elektron almaya egilimlidir."
            }
            v.findViewById<TextView>(R.id.el_desc_text).text = "$metalText\n\n${el.ozellik}"
            v.findViewById<TextView>(R.id.el_usage_text).text = el.kullanim.replace(",", "\n")
            v.parent?.requestChildFocus(v, v)
        }

        // -- Category legend --
        for ((name, color, _) in categoryInfo) {
            val chip = TextView(requireContext()).apply {
                text = name; textSize = 10f; setTextColor(Color.WHITE); setPadding(12, 4, 12, 4)
                background = GradientDrawable().apply {
                    setColor(color and 0x33FFFFFF.toInt()); cornerRadius = 12f; setStroke(1, color)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(4, 2, 4, 2) }
            }
            legendRow.addView(chip)
        }

        // -- Quick pick elements --
        for (sym in quickElements) {
            val el = KimyaData.elementler[sym] ?: continue
            val color = KimyaData.elementRengi(el.tur)
            val badge = TextView(requireContext()).apply {
                text = sym; textSize = 13f; setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE
                setPadding(8, 8, 8, 8); gravity = android.view.Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(3, 3, 3, 3)
                }
                background = GradientDrawable().apply {
                    setColor(color and 0x44FFFFFF.toInt()); cornerRadius = 8f; setStroke(1, color)
                }
                setOnClickListener { showElement(sym) }
            }
            quickGrid.addView(badge)
        }

        // -- Button handlers --
        v.findViewById<Button>(R.id.el_ara_itn).setOnClickListener { showElement(arama.text.toString().trim()) }
        arama.setOnEditorActionListener { _, _, _ -> showElement(arama.text.toString().trim()); true }

        v.findViewById<Button>(R.id.el_back_btn).setOnClickListener {
            detailState.visibility = View.GONE; emptyState.visibility = View.VISIBLE
        }

        v.findViewById<Button>(R.id.el_listele_itn).setOnClickListener {
            emptyState.visibility = View.GONE; detailState.visibility = View.GONE
            val atomCanvas = v.findViewById<com.kimya.uygulama.fragments.AtomModelView>(R.id.el_atom_canvas)
            atomCanvas.semIol = null; atomCanvas.invalidate()
            v.findViewById<TextView>(R.id.el_desc_text).text = KimyaData.elementler.values.sortedBy { it.atomNo }.joinToString("\n") {
                "${it.atomNo}. ${it.semIol} - ${it.adi} (${"%.2f".format(it.kutle)} g/mol) | ${it.tur}"
            }
            v.findViewById<TextView>(R.id.el_usage_text).text = ""
            v.findViewById<TextView>(R.id.el_electron_config).text = ""
            v.findViewById<LinearLayout>(R.id.el_valence_row).removeAllViews()
            propBars.removeAllViews()
            v.findViewById<TextView>(R.id.el_sym_badge).text = "#"
            v.findViewById<TextView>(R.id.el_name).text = "TUM ELEMENTLER"
            v.findViewById<TextView>(R.id.el_type_badge).apply { text = "118 Element"; background = GradientDrawable().apply { setColor(0x4400F0FF.toInt()); cornerRadius = 10f } }
            v.findViewById<TextView>(R.id.el_subtitle).text = "Periyodik tablodaki tum elementler"
            v.findViewById<TextView>(R.id.el_stat_z).text = "118"
            v.findViewById<TextView>(R.id.el_stat_mass).text = "-"
            v.findViewById<TextView>(R.id.el_stat_group).text = "-"
            v.findViewById<TextView>(R.id.el_stat_period).text = "-"
            detailState.visibility = View.VISIBLE
        }

        v.findViewById<Button>(R.id.btn_help)?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Periyodik Tablo")
                .setMessage(buildString {
                    appendLine("118 elementin detayli bilgisi.")
                    appendLine()
                    appendLine("• Arama cubugundan element ara")
                    appendLine("• Populer elementlere hizli erisim")
                    appendLine("• Atom modeli: elektron katmanlari ve yörüngeleri")
                    appendLine("• Renk kodlari: element turunu gosterir")
                })
                .setPositiveButton("Anladim", null)
                .show()
        }

        return v
    }
}
