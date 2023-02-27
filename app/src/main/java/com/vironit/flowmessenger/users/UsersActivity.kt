package com.vironit.flowmessenger.users

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vironit.flowmessenger.R
import com.vironit.flowmessenger.adapters.ChatsAdapter
import com.vironit.flowmessenger.chat.ChatActivity
import com.vironit.flowmessenger.databinding.ActivityUsersBinding
import com.vironit.flowmessenger.models.Chat
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UsersActivity : AppCompatActivity(), SearchView.OnQueryTextListener, ChatsAdapter.Listener {

    private lateinit var binding: ActivityUsersBinding

    private val viewModel by viewModels<UsersViewModel>()
    private val chatsAdapter = ChatsAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.chats.adapter = chatsAdapter
        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getUsersFlow().collectLatest {
                    chatsAdapter.submitList(it)
                }
            }
        }
    }

    override fun onItemClick(chat: Chat) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("email", chat.personEmail)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_users, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setIconifiedByDefault(false)
        searchView.setQuery(viewModel.searchQuery, false)
        searchView.maxWidth = Int.MAX_VALUE
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        viewModel.setQuery(newText.orEmpty())
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }
}