package com.smartwallet.ai.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.ActivityTransactionsBinding
import com.smartwallet.ai.ui.adapter.TransactionAdapter
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel

class TransactionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionsBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchAndFilters()
        setupObservers()
    }

    private var fullList: List<Expense> = emptyList()

    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().lowercase()
        val checkedChipId = binding.chipGroupFilters.checkedChipId
        val selectedCategory = if (checkedChipId != -1) {
            findViewById<Chip>(checkedChipId).text.toString()
        } else {
            "All"
        }

        val filteredList = fullList.filter { expense ->
            val matchesQuery = expense.category.lowercase().contains(query) || 
                               expense.note.lowercase().contains(query) ||
                               (expense.shopName?.lowercase()?.contains(query) ?: false)
            
            val matchesCategory = selectedCategory == "All" || expense.category == selectedCategory
            
            matchesQuery && matchesCategory
        }
        adapter.submitList(filteredList)
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onPrintReceipt = { expense ->
                val intent = Intent(this, ReceiptActivity::class.java)
                intent.putExtra("EXPENSE", expense)
                startActivity(intent)
            },
            onEdit = { expense ->
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra("EXPENSE_ID", expense.id)
                startActivity(intent)
            },
            onDelete = { expense ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteExpense(expense)
                        Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allExpenses.observe(this) { expenses ->
            fullList = expenses
            applyFilters()
        }
    }
}
