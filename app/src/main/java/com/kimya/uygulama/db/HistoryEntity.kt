package com.kimya.uygulama.db

data class HistoryEntry(
    val id: Long = System.currentTimeMillis(),
    val islemAdi: String,
    val detay: String,
    val zaman: Long = System.currentTimeMillis(),
    val favori: Boolean = false
)

data class Calculation(
    val id: Long = System.currentTimeMillis(),
    val tur: String,
    val girdi: String,
    val sonuc: String,
    val zaman: Long = System.currentTimeMillis()
)
