package com.smartwallet.ai.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.ActivityReceiptBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val expense = intent.getSerializableExtra("EXPENSE") as? Expense ?: return finish()

        setupUI(expense)

        binding.btnShareReceipt.setOnClickListener {
            captureAndShare()
        }
    }

    private fun setupUI(expense: Expense) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        binding.tvReceiptDate.text = sdf.format(Date(expense.date))
        binding.tvReceiptCategory.text = expense.category
        binding.tvReceiptMethod.text = expense.inputMethod
        binding.tvReceiptNote.text = expense.note.ifEmpty { expense.shopName ?: "Transaction Details" }
        binding.tvReceiptAmount.text = "PKR ${String.format("%.2f", expense.amount)}"
        
        // Unique Transaction ID: SW-AI-Timestamp-ID
        val transId = "SW-AI-${expense.date}-${expense.id}"
        binding.tvReceiptId.text = "TRANS ID: $transId"
    }

    private fun captureAndShare() {
        try {
            val bitmap = Bitmap.createBitmap(binding.layoutReceipt.width, binding.layoutReceipt.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.layoutReceipt.draw(canvas)

            // Add Verified Stamp
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#4CAF50")
                alpha = 60
                textSize = 50f
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 4f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }

            canvas.save()
            val centerX = bitmap.width / 2f
            val centerY = bitmap.height / 2f
            canvas.rotate(-30f, centerX, centerY)
            
            val stampText = "VERIFIED BY SMART WALLET AI"
            val textWidth = paint.measureText(stampText)
            val rect = android.graphics.RectF(
                centerX - (textWidth / 2) - 20,
                centerY - 40,
                centerX + (textWidth / 2) + 20,
                centerY + 20
            )
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawText(stampText, centerX, centerY, paint)
            canvas.restore()

            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Receipt_${System.currentTimeMillis()}.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Share Receipt"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
