package com.smartwallet.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smartwallet.ai.databinding.FragmentInsightsBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.PreferenceManager

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferenceManager = PreferenceManager(requireContext())
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()

        viewModel.allExpenses.observe(viewLifecycleOwner) {
            viewModel.calculateInsights(income, goal)
        }

        viewModel.aiInsights.observe(viewLifecycleOwner) { insights ->
            if (insights.isNullOrEmpty()) {
                binding.tvInsights.text = "AI is still learning your habits. Keep adding expenses!"
            } else {
                binding.tvInsights.text = insights.joinToString("\n\n")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

