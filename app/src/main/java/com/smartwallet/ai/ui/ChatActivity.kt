package com.smartwallet.ai.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartwallet.ai.data.model.ChatMessage
import com.smartwallet.ai.databinding.ActivityChatBinding
import com.smartwallet.ai.ui.adapter.ChatAdapter
import com.smartwallet.ai.ui.viewmodel.ExpenseViewModel
import com.smartwallet.ai.utils.AIChatEngine
import com.smartwallet.ai.utils.PreferenceManager

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ExpenseViewModel by viewModels()
    private val chatAdapter = ChatAdapter()
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        setupChat()
        setupSuggestions()

        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnSend.setOnClickListener { sendMessage() }
        
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        generateDynamicWelcome()
    }

    private fun setupSuggestions() {
        binding.chipStatus.setOnClickListener { sendMessage("How much have I spent?") }
        binding.chipTips.setOnClickListener { sendMessage("Give me some saving tips") }
        binding.chipJoke.setOnClickListener { sendMessage("Tell me a joke") }
        binding.chipForecast.setOnClickListener { sendMessage("What is my forecast?") }
    }

    private fun generateDynamicWelcome() {
        val name = preferenceManager.getUserName()
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()
        val spent = viewModel.totalExpensesThisMonth.value ?: 0.0
        val remaining = (income - goal - spent)
        
        // Fully dynamic greeting from the engine
        val welcomeText = AIChatEngine.getResponse("hi", emptyList(), emptyList(), income, goal)
        addAIMessage(welcomeText.replace("Friend", name))
    }

    private fun setupChat() {
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun sendMessage(customText: String? = null) {
        val text = customText ?: binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            chatAdapter.addMessage(ChatMessage(text, true))
            if (customText == null) binding.etMessage.setText("")
            binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            
            binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            processAIResponse(text)
        }
    }

    private fun processAIResponse(query: String) {
        val expenses = viewModel.expensesThisMonth.value ?: emptyList()
        val allTimeExpenses = viewModel.allExpenses.value ?: emptyList()
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()
        val history = chatAdapter.getMessages()
        
        // Show typing indicator
        binding.tvTyping.visibility = android.view.View.VISIBLE
        
        // Simulate AI processing delay
        binding.root.postDelayed({
            binding.tvTyping.visibility = android.view.View.GONE
            val response = AIChatEngine.getResponse(query, expenses, allTimeExpenses, income, goal, history)
            addAIMessage(response)
        }, 1200)
    }

    private fun addAIMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, false))
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }
}
