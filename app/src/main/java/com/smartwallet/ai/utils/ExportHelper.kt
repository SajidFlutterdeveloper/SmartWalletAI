package com.smartwallet.ai.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.smartwallet.ai.data.model.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun exportExpensesToCSV(context: Context, expenses: List<Expense>) {
        val fileName = "SmartWallet_Export_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        try {
            file.writer().use { out ->
                out.write("ID,Date,Category,Shop,Note,Amount,InputMethod\n")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                
                expenses.forEach { exp ->
                    val line = "${exp.id},${sdf.format(Date(exp.date))},\"${exp.category}\",\"${exp.shopName ?: ""}\",\"${exp.note}\",${exp.amount},${exp.inputMethod}\n"
                    out.write(line)
                }
            }
            
            shareFile(context, file, "text/csv", "Export Transactions (CSV)")
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
