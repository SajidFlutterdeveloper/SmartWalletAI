package com.smartwallet.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartwallet.ai.data.model.Expense
import java.util.regex.Pattern

object ReceiptScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun scanReceipt(context: Context, uri: Uri, callback: (Expense?) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            processImage(image, callback)
        } catch (e: Exception) {
            callback(null)
        }
    }

    fun scanReceipt(bitmap: Bitmap, callback: (Expense?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        processImage(image, callback)
    }

    private fun processImage(image: InputImage, callback: (Expense?) -> Unit) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                if (fullText.isBlank()) {
                    callback(null)
                    return@addOnSuccessListener
                }

                val amount = extractAmount(fullText)
                val shopName = extractShopName(visionText.textBlocks.map { it.text })
                val date = extractDate(fullText)
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                if (amount != null) {
                    callback(Expense(
                        userId = userId,
                        amount = amount,
                        category = detectCategoryFromText(fullText),
                        shopName = shopName,
                        note = "OCR Scan: $shopName",
                        date = date ?: System.currentTimeMillis(),
                        inputMethod = "Receipt"
                    ))
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun extractAmount(text: String): Double? {
        val lines = text.split("\n")
        val candidates = mutableListOf<Double>()
        
        // Robust regex for currency/amounts: handles commas, dots, and common spacings
        val amountPattern = Pattern.compile("(?:^|\\s)(?:PKR|Rs|\\$)?\\s?(\\d{1,3}(?:[,\\s]\\d{3})*(?:\\.\\d{2})?|\\d+(?:\\.\\d{2})?)", Pattern.CASE_INSENSITIVE)

        val totalKeywords = listOf("total", "amount", "payable", "net", "sum", "grand", "due", "balance")
        
        var foundNearKeyword = false
        for (line in lines) {
            val lowerLine = line.lowercase()
            if (totalKeywords.any { lowerLine.contains(it) }) {
                val matcher = amountPattern.matcher(line)
                while (matcher.find()) {
                    matcher.group(1)?.replace(",", "")?.replace(" ", "")?.toDoubleOrNull()?.let {
                        candidates.add(it)
                        foundNearKeyword = true
                    }
                }
            }
        }

        if (!foundNearKeyword) {
            val matcher = amountPattern.matcher(text)
            while (matcher.find()) {
                matcher.group(1)?.replace(",", "")?.replace(" ", "")?.toDoubleOrNull()?.let {
                    if (it > 1 && it < 1000000) { // Basic sanity check
                        // Filter out common years
                        if (it != 2023.0 && it != 2024.0 && it != 2025.0) {
                            candidates.add(it)
                        }
                    }
                }
            }
        }

        // Return the largest candidate. On a receipt, the grand total is almost always the largest amount.
        return candidates.maxOrNull()
    }

    private fun extractShopName(blocks: List<String>): String? {
        return if (blocks.isNotEmpty()) {
            val firstBlockLines = blocks[0].split("\n")
            if (firstBlockLines.isNotEmpty()) firstBlockLines[0].trim() else "Unknown Store"
        } else {
            "Unknown Store"
        }
    }

    private fun extractDate(text: String): Long? {
        val datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        val matcher = datePattern.matcher(text)
        if (matcher.find()) {
            val dateStr = matcher.group(1) ?: return null
            val formats = arrayOf("dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy", "MM-dd-yyyy", "dd/MM/yy", "MM/dd/yy")
            for (format in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(format, java.util.Locale.US)
                    sdf.isLenient = false
                    return sdf.parse(dateStr)?.time
                } catch (e: Exception) {
                    // continue
                }
            }
        }
        return null
    }

    private fun detectCategoryFromText(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("petrol") || lowerText.contains("fuel") || lowerText.contains("shell") || lowerText.contains("pso") -> "Fuel"
            lowerText.contains("restaurant") || lowerText.contains("food") || lowerText.contains("cafe") || lowerText.contains("kfc") || lowerText.contains("mcdonald") -> "Food"
            lowerText.contains("grocery") || lowerText.contains("mart") || lowerText.contains("super") || lowerText.contains("store") -> "Grocery"
            lowerText.contains("pharmacy") || lowerText.contains("medical") || lowerText.contains("health") -> "Health"
            lowerText.contains("electric") || lowerText.contains("bill") -> "Bills"
            lowerText.contains("clothes") || lowerText.contains("shopping") -> "Shopping"
            else -> "Other"
        }
    }
}
