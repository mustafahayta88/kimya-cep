package com.kimya.uygulama.db

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AppDatabase(context: Context) {
    private val historyFile = File(context.filesDir, "history.json")
    private val calcFile = File(context.filesDir, "calculations.json")
    private val favFile = File(context.filesDir, "favorites.json")

    private fun readText(file: File): String = if (file.exists()) file.readText() else "[]"
    private fun writeText(file: File, text: String) { file.writeText(text) }

    fun getHistory(): List<HistoryEntry> {
        val list = mutableListOf<HistoryEntry>()
        try {
            val arr = JSONArray(readText(historyFile))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(HistoryEntry(
                    obj.optLong("id"), obj.optString("islemAdi"),
                    obj.optString("detay"), obj.optLong("zaman"), obj.optBoolean("favori")
                ))
            }
        } catch (_: Exception) {}
        return list
    }

    fun addHistory(entry: HistoryEntry) {
        val list = getHistory().toMutableList()
        list.add(0, entry)
        if (list.size > 100) list.removeAt(list.size - 1)
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().apply {
                put("id", e.id); put("islemAdi", e.islemAdi); put("detay", e.detay)
                put("zaman", e.zaman); put("favori", e.favori)
            })
        }
        writeText(historyFile, arr.toString())
    }

    fun clearHistory() { writeText(historyFile, "[]") }

    fun getCalculations(): List<Calculation> {
        val list = mutableListOf<Calculation>()
        try {
            val arr = JSONArray(readText(calcFile))
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Calculation(
                    obj.optLong("id"), obj.optString("tur"),
                    obj.optString("girdi"), obj.optString("sonuc"), obj.optLong("zaman")
                ))
            }
        } catch (_: Exception) {}
        return list
    }

    fun addCalculation(calc: Calculation) {
        val list = getCalculations().toMutableList()
        list.add(0, calc)
        if (list.size > 50) list.removeAt(list.size - 1)
        val arr = JSONArray()
        for (c in list) {
            arr.put(JSONObject().apply {
                put("id", c.id); put("tur", c.tur); put("girdi", c.girdi)
                put("sonuc", c.sonuc); put("zaman", c.zaman)
            })
        }
        writeText(calcFile, arr.toString())
    }

    fun getFavorites(): List<String> {
        try {
            val arr = JSONArray(readText(favFile))
            return List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { return emptyList() }
    }

    fun toggleFavorite(key: String): Boolean {
        val list = getFavorites().toMutableList()
        return if (key in list) { list.remove(key); writeText(favFile, JSONArray(list).toString()); false }
        else { list.add(key); writeText(favFile, JSONArray(list).toString()); true }
    }

    fun isFavorite(key: String): Boolean = key in getFavorites()

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) { INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it } }
    }
}
