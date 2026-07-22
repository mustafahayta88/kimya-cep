package com.kimya.uygulama.utils

import android.content.Context
import android.content.Intent

object PdfExporter {
    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Paylas"))
    }

    fun saveToFile(context: Context, fileName: String, text: String) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { it.write(text.toByteArray()) }
    }
}