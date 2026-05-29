package com.smartwallet.ai.utils

import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.data.model.Expense
import java.util.regex.Pattern

object SmartParser {

    private val categoryMap = mapOf(

        // Fuel
        "petrol" to "Fuel",
        "diesel" to "Fuel",
        "fuel" to "Fuel",
        "shell" to "Fuel",
        "pso" to "Fuel",
        "oil" to "Fuel",
        "gaari oil" to "Fuel",
        "bike oil" to "Fuel",
        "پٹرول" to "Fuel",
        "تیل" to "Fuel",
        "ٹرول" to "Fuel",

        // Food
        "khana" to "Food",
        "meal" to "Food",
        "burger" to "Food",
        "pizza" to "Food",
        "biryani" to "Food",
        "chai" to "Food",
        "tea" to "Food",
        "coffee" to "Food",
        "nashta" to "Food",
        "breakfast" to "Food",
        "lunch" to "Food",
        "dinner" to "Food",
        "hotel" to "Food",
        "restaurant" to "Food",
        "juice" to "Food",
        "cold drink" to "Food",
        "roti" to "Food",
        "khanay" to "Food",
        "کھانا" to "Food",
        "چائے" to "Food",

        // Grocery
        "rashan" to "Grocery",
        "kiryana" to "Grocery",
        "grocery" to "Grocery",
        "sabzi" to "Grocery",
        "fruit" to "Grocery",
        "atta" to "Grocery",
        "chini" to "Grocery",
        "daal" to "Grocery",
        "rice" to "Grocery",
        "vegetable" to "Grocery",
        "rashan pani" to "Grocery",
        "سودا" to "Grocery",
        "سبزی" to "Grocery",

        // Bills
        "bill" to "Bills",
        "bijli" to "Bills",
        "electricity" to "Bills",
        "gas" to "Bills",
        "water" to "Bills",
        "internet" to "Bills",
        "wifi" to "Bills",
        "mobile bill" to "Bills",
        "easypaisa bill" to "Bills",
        "بل" to "Bills",
        "بجلی" to "Bills",

        // Health
        "medicine" to "Health",
        "doctor" to "Health",
        "hospital" to "Health",
        "clinic" to "Health",
        "dawa" to "Health",
        "dawai" to "Health",
        "tablet" to "Health",
        "injection" to "Health",
        "دوائی" to "Health",
        "دوا" to "Health",

        // Transport
        "bus" to "Transport",
        "rickshaw" to "Transport",
        "riksha" to "Transport",
        "uber" to "Transport",
        "careem" to "Transport",
        "taxi" to "Transport",
        "van" to "Transport",
        "metro" to "Transport",
        "train" to "Transport",
        "kiraya" to "Transport",
        "کرایہ" to "Transport",

        // Shopping
        "shopping" to "Shopping",
        "kapray" to "Shopping",
        "clothes" to "Shopping",
        "shirt" to "Shopping",
        "shoes" to "Shopping",
        "jacket" to "Shopping",
        "mobile" to "Shopping",
        "laptop" to "Shopping",
        "کپڑے" to "Shopping",

        // Education
        "fees" to "Education",
        "school" to "Education",
        "college" to "Education",
        "university" to "Education",
        "academy" to "Education",
        "course" to "Education",
        "book" to "Education",
        "copy" to "Education",
        "فیس" to "Education",

        // Salary / Income
        "salary" to "Salary",
        "income" to "Salary",
        "pay" to "Salary",
        "tankhwa" to "Salary",
        "تنخواہ" to "Salary",

        // Transfer
        "jazzcash" to "Transfer",
        "easypaisa" to "Transfer",
        "bank" to "Transfer",
        "transfer" to "Transfer",
        "send money" to "Transfer",
        "paisa bheja" to "Transfer",
        "online payment" to "Transfer",
        "بینک" to "Transfer",

        // Entertainment
        "movie" to "Entertainment",
        "cinema" to "Entertainment",
        "game" to "Entertainment",
        "picnic" to "Entertainment",
        "fun" to "Entertainment",
        "تفریح" to "Entertainment"
    )

    fun parseExpense(input: String): Expense? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null

        // 1. Extract Amount dynamically
        val amount = extractAmount(input) ?: return null

        // 2. Map Category intelligently
        var detectedCategory = "Other"
        val lowerInput = input.lowercase()

        // Sort by length descending to match longer keywords first
        val sortedKeywords = categoryMap.keys.sortedByDescending { it.length }
        for (keyword in sortedKeywords) {
            // Check if the keyword exists as a whole word or is part of the Urdu script
            val isUrduScript = keyword.any { it.code in 0x0600..0x06FF }
            val matchFound = if (isUrduScript) {
                lowerInput.contains(keyword)
            } else {
                // Use regex for word boundary to avoid false positives (e.g. "petrol" in a longer word)
                "\\b$keyword\\b".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(lowerInput)
            }

            if (matchFound) {
                detectedCategory = categoryMap[keyword] ?: "Other"
                break
            }
        }

        return Expense(
            userId = userId,
            amount = amount,
            category = detectedCategory,
            note = input,
            inputMethod = "Voice"
        )
    }

    private fun extractAmount(input: String): Double? {
        var text = input.lowercase()

        // 1. Handle thousands and millions
        text = text.replace("hazar", "000")
                  .replace("hazaar", "000")
                  .replace("thousand", "000")
                  .replace("ہزار", "000")
                  .replace(" k ", "000 ")
                  .replace(" k", "000")
                  .replace("lak", "00000")
                  .replace("lakh", "00000")
                  .replace("لاکھ", "00000")

        // 2. Handle hundreds
        text = text.replace("soo", "00")
                  .replace("sau", "00")
                  .replace("hundred", "00")
                  .replace("سو", "00")

        // 3. Handle Roman Urdu and Urdu script numbers
        val numberMap = mapOf(
            "aik" to "1", "ek" to "1", "ایک" to "1",
            "do" to "2", "دو" to "2",
            "teen" to "3", "تین" to "3",
            "chaar" to "4", "char" to "4", "چار" to "4",
            "panch" to "5", "پانچ" to "5",
            "che" to "6", "چھ" to "6",
            "saat" to "7", "سات" to "7",
            "aath" to "8", "آٹھ" to "8",
            "no" to "9", "نو" to "9",
            "das" to "10", "دس" to "10"
        )

        for ((word, digit) in numberMap) {
            val regex = "(?i)\\b$word\\b".toRegex()
            text = text.replace(regex, digit)
            // Also replace without word boundary for Urdu script as it doesn't work well with \b
            if (word.any { it.code in 0x0600..0x06FF }) {
                text = text.replace(word, digit)
            }
        }

        // 4. Convert Urdu/Arabic digits to Western digits
        val urduDigits = mapOf('۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4', '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9')
        text = text.map { urduDigits[it] ?: it }.joinToString("")

        // 5. Remove currency and stop words
        text = text.replace(Regex("(?i)\\b(rs|rupees|rupaye|rupay|rupiya|روپے|روپیہ|ki|ka|ka|per)\\b"), " ")

        // 6. Final cleanup: Merge numbers separated by spaces (e.g. "1 000" -> "1000")
        var cleaned = text.replace(Regex("(\\d)\\s+(\\d)"), "$1$2")
        cleaned = cleaned.replace(Regex("(\\d)\\s+(\\d)"), "$1$2") // Double pass to be safe

        // 7. Extract all numbers
        val pattern = Pattern.compile("(\\d+)")
        val matcher = pattern.matcher(cleaned)

        val candidates = mutableListOf<Double>()
        while (matcher.find()) {
            matcher.group(1)?.toDoubleOrNull()?.let {
                if (it > 0) candidates.add(it)
            }
        }

        // Filter out dates (common years)
        val filtered = candidates.filter { it !in 1990.0..2100.0 }

        return if (filtered.isNotEmpty()) filtered.maxOrNull() else candidates.maxOrNull()
    }
}
