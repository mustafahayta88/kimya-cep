package com.kimya.uygulama.utils

import android.content.Context
import com.kimya.uygulama.R

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"

    data class ThemeDef(val key: String, val label: String, val styleRes: Int)

    val themes = listOf(
        ThemeDef("neon", "Neon Cyber", R.style.Theme_KimyaUygulamasi),
        ThemeDef("ocean", "Ocean Deep", R.style.Theme_KimyaUygulamasi_Ocean),
        ThemeDef("forest", "Forest Night", R.style.Theme_KimyaUygulamasi_Forest),
        ThemeDef("sunset", "Sunset Blaze", R.style.Theme_KimyaUygulamasi_Sunset),
        ThemeDef("lavender", "Lavender Dream", R.style.Theme_KimyaUygulamasi_Lavender),
        ThemeDef("crimson", "Crimson Fire", R.style.Theme_KimyaUygulamasi_Crimson),
        ThemeDef("arctic", "Arctic Frost", R.style.Theme_KimyaUygulamasi_Arctic),
        ThemeDef("mint", "Mint Fresh", R.style.Theme_KimyaUygulamasi_Mint)
    )

    fun getSelectedTheme(context: Context): ThemeDef {
        val key = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "neon") ?: "neon"
        return themes.find { it.key == key } ?: themes[0]
    }

    fun setSelectedTheme(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, key).apply()
    }

    fun applyTheme(context: Context) {
        val theme = getSelectedTheme(context)
        context.setTheme(theme.styleRes)
    }
}
