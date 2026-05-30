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
            binding.tvAmount.text = "PKR ${String.format("%.2f", expense.amount)}"
            
            val sdf = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
            binding.tvDate.text = sdf.format(Date(expense.date))
            binding.tvMethod.text = "Entry: ${expense.inputMethod}"
            
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
