package com.smartwallet.ai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartwallet.ai.data.model.ChatMessage
import com.smartwallet.ai.databinding.ItemChatMessageBinding

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    class ViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.binding.layoutUser.visibility = View.VISIBLE
            holder.binding.layoutAI.visibility = View.GONE
            holder.binding.tvUserMessage.text = message.text
        } else {
            holder.binding.layoutUser.visibility = View.GONE
            holder.binding.layoutAI.visibility = View.VISIBLE
            holder.binding.tvAIMessage.text = message.text
        }
    }

    override fun getItemCount() = messages.size
}
