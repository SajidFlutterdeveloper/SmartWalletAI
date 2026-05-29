package com.smartwallet.ai.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter { expense ->
            // Edit transaction logic
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra("EXPENSE_ID", expense.id)
            startActivity(intent)
        }
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allExpenses.observe(this) { expenses ->
            adapter.submitList(expenses)
        }
    }
}
