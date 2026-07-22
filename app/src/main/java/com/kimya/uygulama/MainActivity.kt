package com.kimya.uygulama

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import com.kimya.uygulama.databinding.ActivityMainBinding
import com.kimya.uygulama.features.*
import com.kimya.uygulama.fragments.*
import com.kimya.uygulama.utils.ThemeManager
import com.kimya.uygulama.viewmodel.KimyaViewModel

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    lateinit var viewModel: KimyaViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[KimyaViewModel::class.java]
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        binding.navView.setNavigationItemSelectedListener(this)

        binding.toolbar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            else
                binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        setupBackPress()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DashboardFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> { openFragment(SearchFragment()); true }
            R.id.action_history -> { openFragment(HistoryFragment()); true }
            R.id.action_theme -> { showThemePicker(); true }
            R.id.action_settings -> { showSettings(); true }
            R.id.action_about -> { showAbout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showThemePicker() {
        val themes = ThemeManager.themes
        val currentKey = ThemeManager.getSelectedTheme(this).key
        val labels = themes.map { it.label }.toTypedArray()
        val checkedIndex = themes.indexOfFirst { it.key == currentKey }

        AlertDialog.Builder(this)
            .setTitle("Tema Sec")
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selected = themes[which]
                ThemeManager.setSelectedTheme(this, selected.key)
                dialog.dismiss()
                val intent = Intent(this@MainActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                finish()
                startActivity(intent)
            }
            .setNegativeButton("Iptal", null)
            .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment: Fragment? = when (item.itemId) {
            R.id.nav_giris -> DashboardFragment()
            R.id.nav_element -> ElementFragment()
            R.id.nav_bilesik -> BilesikFragment()
            R.id.nav_periyodik -> PeriyodikFragment()
            R.id.nav_trend -> TrendFragment()
            R.id.nav_molkutlesi -> MolKutlesiFragment()
            R.id.nav_gaz -> GazFragment()
            R.id.nav_cozelti -> CozeltiFragment()
            R.id.nav_molarite -> MolariteFragment()
            R.id.nav_seyreltme -> SeyreltmeFragment()
            R.id.nav_stoykiyometri -> StokiyometriFragment()
            R.id.nav_birim -> BirimFragment()
            R.id.nav_donusum -> DonusumFragment()
            R.id.nav_reaksiyon -> ReaksiyonFragment()
            R.id.nav_asitbaz -> AsitBazFragment()
            R.id.nav_redox -> RedoxFragment()
            R.id.nav_organik -> OrganicFragment()
            R.id.nav_enstrumantal -> EnstrumantalFragment()
            R.id.nav_not -> NotFragment()
            R.id.nav_timer -> TimerFragment()
            R.id.nav_quiz -> QuizFragment()
            R.id.nav_search -> SearchFragment()
            R.id.nav_history -> HistoryFragment()
            R.id.nav_reaction_types -> ReactionsFragment()
            R.id.nav_isomerism -> IsomerismFragment()
            R.id.nav_biomolecules -> BiomoleculesFragment()
            R.id.nav_polymers -> PolymersFragment()
            R.id.nav_petroleum -> PetroleumFragment()
            R.id.nav_drawer -> MoleculeDrawerFragment()
            R.id.nav_lewis -> LewisFragment()
            R.id.nav_vsepr -> MolekulGeometriFragment()
            R.id.nav_termodinamik -> TermodinamikFragment()
            R.id.nav_kinematik -> KinematikFragment()
            R.id.nav_denge -> KimyasalDengeFragment()
            R.id.nav_elektrokimya -> ElektrokimyaFragment()
            R.id.nav_cozelti_raoult -> CozeltiRaoultFragment()
            R.id.nav_flame_test -> FlameTestFragment()
            R.id.nav_aas_sim -> AASSimulatorFragment()
            else -> null
        }
        fragment?.let { openFragment(it) }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showSettings() {
        android.widget.Toast.makeText(this, "Ayarlar yakinda eklenecek", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showAbout() {
        android.widget.Toast.makeText(this, "Kimya Pro v2.0 - Kimya Muhendisligi Araclari", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            else isEnabled = false
        }
    }
}
