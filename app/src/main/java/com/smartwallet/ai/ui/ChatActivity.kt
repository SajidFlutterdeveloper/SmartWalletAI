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

        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnSend.setOnClickListener { sendMessage() }
        
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        // Welcome Message
        addAIMessage("Hello! I'm your Smart Wallet AI. How can I help you with your finances today?")
    }

    private fun setupChat() {
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isNotEmpty()) {
            chatAdapter.addMessage(ChatMessage(text, true))
            binding.etMessage.setText("")
            binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            
            processAIResponse(text)
        }
    }

    private fun processAIResponse(query: String) {
        val expenses = viewModel.allExpenses.value ?: emptyList()
        val income = preferenceManager.getMonthlyIncome()
        val goal = preferenceManager.getSavingsGoal()
        
        // Simulate AI processing delay
        binding.root.postDelayed({
            val response = AIChatEngine.getResponse(query, expenses, income, goal)
            addAIMessage(response)
        }, 1000)
    }

    private fun addAIMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, false))
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }
}
