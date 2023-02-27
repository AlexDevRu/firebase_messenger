package com.vironit.flowmessenger

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.adapters.ChatsAdapter
import com.vironit.flowmessenger.auth.AuthActivity
import com.vironit.flowmessenger.chat.ChatActivity
import com.vironit.flowmessenger.databinding.ActivityMainBinding
import com.vironit.flowmessenger.models.Chat
import com.vironit.flowmessenger.users.UsersActivity
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_search -> {
                val intent = Intent(this, UsersActivity::class.java)
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    private fun logout() {
        Firebase.auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.your_web_client_id))
            .build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
        }
    }

    override fun onItemClick(chat: Chat) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("email", chat.personEmail)
        startActivity(intent)
    }
}