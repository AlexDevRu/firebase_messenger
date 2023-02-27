package com.vironit.flowmessenger.chat

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.stfalcon.chatkit.messages.MessagesListAdapter
import com.vironit.flowmessenger.R
import com.vironit.flowmessenger.databinding.ActivityChatBinding
import com.vironit.flowmessenger.models.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class ChatActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityChatBinding

    private lateinit var adapter: MessagesListAdapter<Message>

    private val viewModel by viewModels<ChatViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.userEmail = intent?.getStringExtra("email").orEmpty()
        title = viewModel.userEmail

        viewModel.start()

        adapter = MessagesListAdapter<Message>(Firebase.auth.currentUser?.email) { imageView, url, payload ->
            Glide
                .with(this)
                .load(url)
                .centerCrop()
                .into(imageView)
        }

        binding.messagesList.setAdapter(adapter)

        binding.btnSend.setOnClickListener(this)

        observe()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnSend -> {
                viewModel.sendMessage(binding.etMessage.text?.toString().orEmpty())
                binding.etMessage.setText("")
            }
        }
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect {
                    it.forEach {
                        adapter.upsert(it)
                    }
                    binding.messagesList.scrollToPosition(0)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collectLatest {
                    binding.progressBar.isVisible = it
                }
            }
        }
    }

}