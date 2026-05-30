package com.smartwallet.ai.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwallet.ai.R
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.ActivityStatementBinding
import com.smartwallet.ai.databinding.ItemStatementTransactionBinding
import com.smartwallet.ai.utils.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class StatementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatementBinding
    private lateinit var preferenceManager: PreferenceManager
    private var transactions: List<Expense> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        
        // Data passed from Dashboard
        val period = intent.getStringExtra("PERIOD") ?: "This Month"
        transactions = intent.getSerializableExtra("DATA") as? List<Expense> ?: emptyList()

        setupUI(period)
        setupRecyclerView()

        binding.btnShareStatement.setOnClickListener {
            captureAndShare()
        }
    }

    private fun setupUI(period: String) {
        binding.tvStatementName.text = preferenceManager.getUserName()
        binding.tvStatementPeriod.text = period
        binding.tvStatementDate.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val income = preferenceManager.getMonthlyIncome()
        val totalExpense = transactions.sumOf { it.amount }
        val savings = income - totalExpense

        binding.tvStatementInflow.text = "PKR ${String.format("%.0f", income)}"
        binding.tvStatementOutflow.text = "PKR ${String.format("%.0f", totalExpense)}"
        binding.tvStatementSavings.text = "PKR ${String.format("%.0f", savings)}"
    }

    private fun setupRecyclerView() {
        binding.rvStatementTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvStatementTransactions.adapter = StatementAdapter(transactions)
    }

    private fun captureAndShare() {
        try {
            val bitmap = Bitmap.createBitmap(binding.layoutStatement.width, binding.layoutStatement.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.layoutStatement.draw(canvas)

            // Add Verified Stamp
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#4CAF50") // Material Green
                alpha = 80 // Semi-transparent
                textSize = 50f
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 5f
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }

            canvas.save()
            val centerX = bitmap.width / 2f
            val centerY = bitmap.height / 2f
            canvas.rotate(-45f, centerX, centerY)
            
            val stampText = "VERIFIED BY SMART WALLET AI"
            val textWidth = paint.measureText(stampText)
            val rect = android.graphics.RectF(
                centerX - (textWidth / 2) - 30,
                centerY - 50,
                centerX + (textWidth / 2) + 30,
                centerY + 30
            )
            canvas.drawRoundRect(rect, 15f, 15f, paint)
            
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawText(stampText, centerX, centerY + 10, paint)
            canvas.restore()

            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SmartWallet_Statement_${System.currentTimeMillis()}.png")
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
            startActivity(android.content.Intent.createChooser(intent, "Share Mini Statement"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    class StatementAdapter(private val items: List<Expense>) : RecyclerView.Adapter<StatementAdapter.ViewHolder>() {
        
        class ViewHolder(val binding: ItemStatementTransactionBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStatementTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            holder.binding.tvStDate.text = sdf.format(Date(item.date))
            holder.binding.tvStNote.text = item.note.ifEmpty { item.shopName ?: "Transaction" }
            holder.binding.tvStAmount.text = String.format("%.2f", item.amount)

            holder.itemView.setOnLongClickListener {
                val fullText = "Note: ${holder.binding.tvStNote.text}\nAmount: PKR ${item.amount}\nDate: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.date))}"
                Toast.makeText(holder.itemView.context, fullText, Toast.LENGTH_LONG).show()
                true
            }
        }

        override fun getItemCount() = items.size
    }
}
