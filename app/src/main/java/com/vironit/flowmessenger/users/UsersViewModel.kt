package com.vironit.flowmessenger.users

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.models.Chat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UsersViewModel : ViewModel() {

    private val users = Firebase.firestore.collection("users")
    private val chats =
        Firebase.firestore.collection("/users/${Firebase.auth.currentUser?.email}/chats")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: String
        get() = _searchQuery.value

    private val existingChats = mutableSetOf<Chat>()

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        viewModelScope.launch {
            getChatsFlow().collect {
                existingChats.addAll(it)
            }
        }
    }

    private fun getChatsFlow() = callbackFlow {
        val registration = chats.addSnapshotListener { value, error ->
            if (error != null) {
                Log.w(TAG, "Listen failed.", error)
                cancel()
                return@addSnapshotListener
            }

            if (value != null) {
                val list = mutableListOf<Chat>()
                for (doc in value) {
                    list.add(Chat(doc.id))
                }
                viewModelScope.launch { send(list) }
            }
        }

        awaitClose { registration.remove() }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun getUsersFlow() = _searchQuery
        .map {
            it.trim().lowercase()
        }
        .debounce(500)
        .flatMapLatest { query ->
            callbackFlow {
                val registration = users
                    .whereNotEqualTo(FieldPath.documentId(), Firebase.auth.currentUser?.email)
                    .addSnapshotListener { value, error ->
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error)
                            cancel()
                            return@addSnapshotListener
                        }

                        if (value != null) {
                            val list = mutableListOf<Chat>()
                            for (doc in value) {
                                if (doc.id.trim().lowercase().contains(query))
                                    list.add(Chat(doc.id))
                            }
                            viewModelScope.launch { send(list) }
                        }
                    }

                awaitClose {
                    Log.d(TAG, "getUsersFlow: closed")
                    registration.remove()
                }
            }
        }
    
    companion object {
        private const val TAG = "UsersViewModel"
    }

}