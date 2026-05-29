package com.smartwallet.ai.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.smartwallet.ai.data.model.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun exportToCSV(context: Context, expenses: List<Expense>): File? {
        val fileName = "Expenses_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.csv"
        val file = File(context.cacheDir, fileName)
        
        return try {
            file.printWriter().use { out ->
                out.println("Date,Category,Amount,Shop,Method,Note")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                expenses.forEach {
                    out.println("${sdf.format(Date(it.date))},${it.category},${it.amount},${it.shopName ?: ""},${it.inputMethod},${it.note}")
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Expenses"))
    }
}
