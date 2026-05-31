package com.smartwallet.ai.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.smartwallet.ai.R
import com.smartwallet.ai.data.local.AppDatabase
import com.smartwallet.ai.data.model.Expense
import com.smartwallet.ai.databinding.ActivityAddTransactionBinding
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.ReceiptScanner
import com.smartwallet.ai.utils.SmartParser
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private var editingExpense: Expense? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var photoUri: Uri? = null
    private var currentInputMethod: String = "Manual"
    private var selectedDate: Long = System.currentTimeMillis()

    private val categories = arrayOf("Food", "Fuel", "Shopping", "Bills", "Health", "Education", "Transfer", "Entertainment", "Salary", "Grocery", "Transport", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategorySpinner()
        updateDateDisplay()
        checkIntent()
        setupListeners()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        (binding.etCategory as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun checkIntent() {
        val expenseId = intent.getLongExtra("EXPENSE_ID", -1)
        if (expenseId != -1L) {
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                editingExpense = db.expenseDao().getExpenseById(expenseId)
                editingExpense?.let { populateFields(it) }
            }
        }

        val action = intent.getStringExtra("ACTION")
        if (action == "VOICE") startVoiceRecognition()
        else if (action == "SCAN") launchCamera()
    }

    private fun populateFields(expense: Expense) {
        selectedDate = expense.date
        updateDateDisplay()
        binding.etAmount.setText(expense.amount.toString())
        (binding.etCategory as? AutoCompleteTextView)?.setText(expense.category, false)
        binding.etNote.setText(expense.note)
        binding.etShopName.setText(expense.shopName ?: "")
        binding.btnDelete.visibility = View.VISIBLE
    }

    private fun updateDateDisplay() {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.etDate.setText(sdf.format(Date(selectedDate)))
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDate
        
        val dpd = android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                val newCal = Calendar.getInstance()
                newCal.set(year, month, day)
                selectedDate = newCal.timeInMillis
                updateDateDisplay()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    private fun setupListeners() {
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnVoice.setOnClickListener { startVoiceRecognition() }
        binding.btnScan.setOnClickListener { launchCamera() }
        binding.btnUpload.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnSave.setOnClickListener { saveExpense() }
        binding.btnDelete.setOnClickListener { 
            editingExpense?.let { 
                viewModel.deleteExpense(it)
                finish()
            }
        }

        binding.etAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreTransactionInsight()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun updatePreTransactionInsight() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            binding.tvPreTransactionInsight.visibility = View.GONE
            return
        }

        val preferenceManager = com.smartwallet.ai.utils.PreferenceManager(this)
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()
        val spentSoFar = viewModel.totalExpensesThisMonth.value ?: 0.0
        
        val newTotal = spentSoFar + amount
        val budgetLimit = income - goal
        
        binding.tvPreTransactionInsight.visibility = View.VISIBLE
        
        when {
            newTotal > income -> {
                binding.tvPreTransactionInsight.text = "⚠️ This will put you into negative balance!"
                binding.tvPreTransactionInsight.setTextColor(getColor(R.color.danger))
            }
            newTotal > budgetLimit -> {
                binding.tvPreTransactionInsight.text = "🚩 This will use PKR ${(newTotal - budgetLimit).toInt()} from your savings goal."
                binding.tvPreTransactionInsight.setTextColor(getColor(R.color.warning))
            }
            else -> {
                val dailyLimit = com.smartwallet.ai.utils.BudgetCalculator.calculateSafeDailyLimit(income, goal, newTotal)
                binding.tvPreTransactionInsight.text = "✅ Within budget. Remaining daily safe: PKR ${dailyLimit.toInt()}"
                binding.tvPreTransactionInsight.setTextColor(getColor(R.color.success))
            }
        }
    }

    private fun launchCamera() {
        val photoFile = File(getExternalFilesDir("Pictures"), "receipt_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
        takePhotoLauncher.launch(photoUri)
    }

    private val voiceResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            currentInputMethod = "Voice"
            
            // Set the note to EXACTLY what was spoken
            binding.etNote.setText(spokenText)
            
            val parsedExpense = SmartParser.parseExpense(spokenText)
            
            if (parsedExpense != null && parsedExpense.amount > 0) {
                binding.etAmount.setText(parsedExpense.amount.toInt().toString()) // Use toInt() to avoid .0
                binding.etCategory.setText(parsedExpense.category, false)
                
                Toast.makeText(this, "AI: ${parsedExpense.category} - ${parsedExpense.amount.toInt()}\nHeard: $spokenText", Toast.LENGTH_LONG).show()
                
                saveExpense()
            } else {
                Toast.makeText(this, "Amount not found.\nHeard: $spokenText", Toast.LENGTH_LONG).show()
                binding.etAmount.requestFocus()
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { processReceiptUri(it) }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processReceiptUri(it) }
    }

    private fun processReceiptUri(uri: Uri) {
        currentInputMethod = "Receipt"
        Toast.makeText(this, "AI is scanning receipt...", Toast.LENGTH_SHORT).show()
        ReceiptScanner.scanReceipt(this, uri) { expense ->
            if (expense != null) {
                binding.etAmount.setText(expense.amount.toString())
                (binding.etCategory as? AutoCompleteTextView)?.setText(expense.category, false)
                binding.etShopName.setText(expense.shopName ?: "")
                binding.etNote.setText(expense.note)
                Toast.makeText(this, "AI Parsed Receipt Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "AI couldn't find amount. Please enter manually.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ur-PK")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak expense (e.g., Petrol 2000 rupay)")
        }
        voiceResultLauncher.launch(intent)
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val category = binding.etCategory.text.toString()
        val note = binding.etNote.text.toString()
        val shopName = binding.etShopName.text.toString()

        if (amount != null && category.isNotEmpty()) {
            showLoading(true)
            val expense = Expense(
                id = editingExpense?.id ?: 0,
                userId = userId,
                amount = amount,
                category = category,
                note = note,
                shopName = if (shopName.isEmpty()) null else shopName,
                date = selectedDate,
                inputMethod = if (editingExpense != null) editingExpense!!.inputMethod else currentInputMethod
            )
            
            lifecycleScope.launch {
                try {
                    showLoading(true)
                    if (editingExpense != null) viewModel.updateExpense(expense)
                    else viewModel.addExpense(expense)

                    // Haptic Feedback & Success
                    binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    Toast.makeText(this@AddTransactionActivity, "Universe Synchronized Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@AddTransactionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    showLoading(false)
                }
            }
        } else {
            Toast.makeText(this, "Please fill Amount and Category", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.loadingOverlay.visibility = View.VISIBLE
            val rotate = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_universe)
            findViewById<View>(R.id.ivLoadingLogo)?.startAnimation(rotate)
        } else {
            binding.loadingOverlay.visibility = View.GONE
            findViewById<View>(R.id.ivLoadingLogo)?.clearAnimation()
        }
        binding.btnSave.isEnabled = !show
    }
}
