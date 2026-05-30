package com.smartwallet.ai.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.FragmentTransactionsBinding
import com.smartwallet.ai.ui.adapter.TransactionAdapter
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter
    private var fullList: List<Expense> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchAndFilters()
        setupObservers()
    }

    private fun setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, _ ->
            applyFilters()
        }
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().lowercase()
        val checkedChipId = binding.chipGroupFilters.checkedChipId
        val selectedCategory = if (checkedChipId != View.NO_ID) {
            view?.findViewById<Chip>(checkedChipId)?.text.toString() ?: "All"
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
                val intent = Intent(requireContext(), ReceiptActivity::class.java)
                intent.putExtra("EXPENSE", expense)
                startActivity(intent)
            },
            onEdit = { expense ->
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                intent.putExtra("EXPENSE_ID", expense.id)
                startActivity(intent)
            },
            onDelete = { expense ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Transaction")
                    .setMessage("Are you sure you want to delete this transaction?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteExpense(expense)
                        Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            fullList = expenses
            applyFilters()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
