package com.kimya.uygulama

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.kimya.uygulama.features.*
import com.kimya.uygulama.fragments.*

data class ToolItem(val title: String, val desc: String, val fragment: Fragment, val colorRes: Int)

class DashboardFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val kesfet = listOf(
            ToolItem("Element", "Sembol, atom no, kütle", ElementFragment(), R.color.cat_kesfet),
            ToolItem("Bileşik", "Ad, formül, mol kütlesi", BilesikFragment(), R.color.cat_kesfet),
            ToolItem("Periyodik", "İnteraktif tablo", PeriyodikFragment(), R.color.cat_kesfet),
            ToolItem("Trend", "Periyodik özellikler", TrendFragment(), R.color.cat_kesfet),
            ToolItem("Lewis Yapısı", "Elektron noktalı yapı", LewisFragment(), R.color.cat_kesfet),
            ToolItem("Molekül Geometrisi", "VSEPR, açılar, polarite", MolekulGeometriFragment(), R.color.cat_kesfet),
        )
        val hesapla = listOf(
            ToolItem("Atom Kutlesi", "Element ve bilesik kutle hesaplama", MolKutlesiFragment(), R.color.cat_hesapla),
            ToolItem("Gaz Yasaları", "PV=nRT hesapları", GazFragment(), R.color.cat_hesapla),
            ToolItem("Çözelti", "Çözelti hazırlama", CozeltiFragment(), R.color.cat_hesapla),
            ToolItem("Molarite", "Molarite hesaplama", MolariteFragment(), R.color.cat_hesapla),
            ToolItem("Seyreltme", "Seyreltme oranları", SeyreltmeFragment(), R.color.cat_hesapla),
            ToolItem("Stokiyometri", "Mol oran hesapları", StokiyometriFragment(), R.color.cat_hesapla),
            ToolItem("Birim Dönüşüm", "Birim çevirme", BirimFragment(), R.color.cat_hesapla),
            ToolItem("Mol Hesap", "Mol, kutle, hacim hesaplama", DonusumFragment(), R.color.neon_lime),
            ToolItem("Termodinamik", "ΔG = ΔH − TΔS hesaplama", TermodinamikFragment(), R.color.cat_hesapla),
            ToolItem("Kinematik", "Hız yasaları, yarım ömür", KinematikFragment(), R.color.cat_hesapla),
            ToolItem("Kimyasal Denge", "Kp, Kc, Le Chatelier", KimyasalDengeFragment(), R.color.cat_hesapla),
            ToolItem("Elektrokimya", "Pil, Nernst, Faraday", ElektrokimyaFragment(), R.color.cat_hesapla),
            ToolItem("Çözeltiler", "Raoult, donma/kaynama", CozeltiRaoultFragment(), R.color.cat_hesapla),
        )
        val tepkime = listOf(
            ToolItem("Reaksiyon", "Kimyasal dengeleme", ReaksiyonFragment(), R.color.cat_tepkime),
            ToolItem("Asit/Baz", "pH, pOH hesapları", AsitBazFragment(), R.color.cat_tepkime),
            ToolItem("Redox", "Yükseltgenme/indirgenme", RedoxFragment(), R.color.cat_tepkime),
            ToolItem("Organik", "Organik kimya araçları", OrganicFragment(), R.color.cat_tepkime),
            ToolItem("Org. Reaksiyon", "Yer değiştirme, katılma", ReactionsFragment(), R.color.cat_tepkime),
            ToolItem("İzomerlik", "Yapı, geometrik, optik", IsomerismFragment(), R.color.cat_tepkime),
            ToolItem("Polimerler", "Katılma, yoğunlaşma", PolymersFragment(), R.color.cat_tepkime),
            ToolItem("Petrol", "Hidrokarbonlar", PetroleumFragment(), R.color.cat_tepkime),
            ToolItem("Biyomolekül", "Karbonhidrat, protein", BiomoleculesFragment(), R.color.cat_tepkime),
        )
        val araclar = listOf(
            ToolItem("Enstrümantal", "Spektroskopi analizi", EnstrumantalFragment(), R.color.cat_araclar),
            ToolItem("Notlar", "Hızlı not alma", NotFragment(), R.color.cat_araclar),
            ToolItem("Kronometre", "Reaksiyon süreölçer", TimerFragment(), R.color.cat_araclar),
            ToolItem("Quiz", "Kendini test et", QuizFragment(), R.color.cat_araclar),
            ToolItem("Geçmiş", "İşlem geçmişi", HistoryFragment(), R.color.cat_araclar),
        )

        val simulasyon = listOf(
            ToolItem("3D Molekül", "Molekulleri 3D dondur", Molecule3DFragment(), R.color.cat_kesfet),
            ToolItem("Titrasyon", "pH titrasyon simulasyonu", TitrasyonFragment(), R.color.cat_kesfet),
            ToolItem("Reaksiyon Hizi", "Hiz simulasyonu", ReactionRateFragment(), R.color.cat_hesapla),
            ToolItem("Faz Diyagrami", "Maddenin halleri", PhaseDiagramFragment(), R.color.cat_hesapla),
            ToolItem("Lab. Güvenlik", "Guvenlik sembolleri", LabSafetyFragment(), R.color.cat_araclar),
            ToolItem("Alev Testi", "Flame test simulasyonu", FlameTestFragment(), R.color.cat_tepkime),
            ToolItem("AAS Simülatörü", "Atomik absorpsiyon spektroskopisi", AASSimulatorFragment(), R.color.cat_tepkime),
        )
        addCards(v, R.id.kesfet_grid, kesfet)
        addCards(v, R.id.hesapla_grid, hesapla)
        addCards(v, R.id.tepkime_grid, tepkime)
        addCards(v, R.id.araclar_grid, araclar)
        addCards(v, R.id.simulasyon_grid, simulasyon)

        val searchBar = v.findViewById<EditText>(R.id.search_bar_input)
        val searchBtn = v.findViewById<View>(R.id.search_bar_btn)

        fun doSearch() {
            val q = searchBar.text.toString().trim()
            if (q.isNotEmpty()) {
                val frag = SearchFragment().apply { arguments = Bundle().apply { putString("query", q) } }
                openFragment(frag)
            }
        }

        searchBtn.setOnClickListener { doSearch() }
        searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }

        return v
    }

    private fun addCards(root: View, gridId: Int, items: List<ToolItem>) {
        val grid = root.findViewById<GridLayout>(gridId)
        val dp4 = (4 * resources.displayMetrics.density).toInt()
        for (item in items) {
            val card = layoutInflater.inflate(R.layout.card_tool, grid, false) as MaterialCardView
            card.findViewById<TextView>(R.id.card_title).text = item.title
            card.findViewById<TextView>(R.id.card_desc).text = item.desc
            val colorBar = card.findViewById<View>(R.id.color_bar)
            colorBar.backgroundTintList = ContextCompat.getColorStateList(requireContext(), item.colorRes)
            card.setOnClickListener { openFragment(item.fragment) }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp4, dp4, dp4, dp4)
            }
            card.layoutParams = lp
            grid.addView(card)
        }
    }

    private fun openFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
