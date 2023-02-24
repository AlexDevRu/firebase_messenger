package com.vironit.flowmessenger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.vironit.flowmessenger.adapters.ChatsAdapter
import com.vironit.flowmessenger.databinding.ActivityMainBinding
import com.vironit.flowmessenger.models.Chat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ChatsAdapter.Listener {

    private lateinit var binding: ActivityMainBinding

    private val chatsAdapter = ChatsAdapter(this)

    private val viewModel by viewModels<ChatsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        title = "Chats"
        binding.chats.adapter = chatsAdapter
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getChatsFlow().collectLatest {
                    chatsAdapter.submitList(it)
                }
            }
        }
    }

    override fun onItemClick(chat: Chat) {

    }
}