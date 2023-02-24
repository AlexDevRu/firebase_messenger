package com.vironit.flowmessenger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vironit.flowmessenger.databinding.ItemChatBinding
import com.vironit.flowmessenger.models.Chat

class ChatsAdapter(
    private val listener : Listener
): ListAdapter<Chat, ChatsAdapter.ChatViewHolder>(DIFF_UTIL) {

    companion object {
        val DIFF_UTIL = object : ItemCallback<Chat>() {
            override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem.personEmail == newItem.personEmail
            }

            override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface Listener {
        fun onItemClick(chat: Chat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding : ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        private var chat: Chat? = null

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(chat: Chat) {
            this.chat = chat
            binding.title.text = chat.personEmail
        }

        override fun onClick(view: View?) {
            listener.onItemClick(chat!!)
        }

    }

}