package com.smartwallet.ai.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onPrintReceipt: (Expense) -> Unit,
    private val onEdit: (Expense) -> Unit,
    private val onDelete: (Expense) -> Unit
) : ListAdapter<Expense, TransactionAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.tvCategory.text = expense.category
            binding.tvAmount.text = "PKR ${String.format("%.0f", expense.amount)}"
            
            val context = binding.root.context
            
            // Category specific styling
            val color = when(expense.category) {
                "Food" -> "#F59E0B"
                "Fuel" -> "#EF4444"
                "Salary" -> "#10B981"
                "Shopping" -> "#EC4899"
                "Bills" -> "#3B82F6"
                else -> "#6366F1"
            }
            
            binding.viewCategoryBg.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
            binding.ivCategoryIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
            
            val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(expense.date))
            binding.tvMethod.text = "AI ${expense.inputMethod} Entry"
            
            binding.btnOptions.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Print Receipt")
                popup.menu.add("Edit")
                popup.menu.add("Delete")
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Print Receipt" -> onPrintReceipt(expense)
                        "Edit" -> onEdit(expense)
                        "Delete" -> onDelete(expense)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense) = oldItem == newItem
    }
}
