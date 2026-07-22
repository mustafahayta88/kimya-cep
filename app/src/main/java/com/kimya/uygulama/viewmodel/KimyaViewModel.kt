package com.kimya.uygulama.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kimya.uygulama.db.AppDatabase
import com.kimya.uygulama.db.Calculation
import com.kimya.uygulama.db.HistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KimyaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _calculations = MutableStateFlow<List<Calculation>>(emptyList())
    val calculations: StateFlow<List<Calculation>> = _calculations.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _history.value = db.getHistory()
            _calculations.value = db.getCalculations()
            _favorites.value = db.getFavorites().toSet()
            _isLoading.value = false
        }
    }

    fun addHistory(islemAdi: String, detay: String) {
        viewModelScope.launch {
            val entry = HistoryEntry(islemAdi = islemAdi, detay = detay)
            db.addHistory(entry)
            _history.value = db.getHistory()
        }
    }

    fun addCalculation(tur: String, girdi: String, sonuc: String) {
        viewModelScope.launch {
            val calc = Calculation(tur = tur, girdi = girdi, sonuc = sonuc)
            db.addCalculation(calc)
            _calculations.value = db.getCalculations()
        }
    }

    fun toggleFavorite(key: String) {
        viewModelScope.launch {
            db.toggleFavorite(key)
            _favorites.value = db.getFavorites().toSet()
        }
    }

    fun isFavorite(key: String): Boolean = key in _favorites.value

    fun clearHistory() {
        viewModelScope.launch {
            db.clearHistory()
            _history.value = emptyList()
        }
    }
}
