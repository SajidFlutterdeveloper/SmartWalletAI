package com.smartwallet.ai.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.R
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.FragmentAddExpenseBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by viewModels()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val pickCsvLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processCsv(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        animateEntrance()
        startActiveAnimations()
    }

    private fun animateEntrance() {
        binding.gridOptions.scheduleLayoutAnimation()
    }

    private fun startActiveAnimations() {
        val pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
        binding.cardBulk.startAnimation(pulse)
    }

    private fun setupListeners() {
        binding.cardVoice.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                intent.putExtra("ACTION", "VOICE")
                startActivity(intent)
            }.start()
        }

        binding.cardScan.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                intent.putExtra("ACTION", "SCAN")
                startActivity(intent)
            }.start()
        }

        binding.cardManual.setOnClickListener {
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                startActivity(Intent(requireContext(), AddTransactionActivity::class.java))
            }.start()
        }

        binding.cardBulk.setOnClickListener {
            it.clearAnimation() // Stop pulse on click
            it.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                pickCsvLauncher.launch("text/*")
                startActiveAnimations() // Restart pulse
            }.start()
        }
    }

    private fun processCsv(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val expenses = mutableListOf<Expense>()
            val userId = auth.currentUser?.uid ?: return
            
            var line: String?
            var count = 0
            
            // Basic header detection: skip if first column is not a number
            line = reader.readLine()
            if (line != null) {
                val firstTokens = line!!.split(",")
                if (firstTokens.isNotEmpty() && firstTokens[0].toDoubleOrNull() == null) {
                    // This is likely a header, skip it
                } else {
                    // Not a header, process it
                    processLine(line!!, userId, expenses)
                    count++
                }
            }

            while (reader.readLine().also { line = it } != null) {
                processLine(line!!, userId, expenses)
                count++
            }
            
            expenses.forEach { viewModel.addExpense(it) }
            Toast.makeText(requireContext(), "Successfully imported $count expenses!", Toast.LENGTH_LONG).show()
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error importing CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processLine(line: String, userId: String, expenses: MutableList<Expense>) {
        val tokens = line.split(",")
        if (tokens.size >= 2) {
            val amount = tokens[0].trim().toDoubleOrNull() ?: 0.0
            val category = tokens[1].trim()
            val note = if (tokens.size > 2) tokens[2].trim() else ""
            val dateStr = if (tokens.size > 3) tokens[3].trim() else ""

            val dateLong = parseDate(dateStr)

            expenses.add(Expense(
                userId = userId,
                amount = amount,
                category = category,
                note = note,
                date = dateLong,
                inputMethod = "Bulk"
            ))
        }
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isEmpty()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.cardBulk.clearAnimation()
        _binding = null
    }
}
