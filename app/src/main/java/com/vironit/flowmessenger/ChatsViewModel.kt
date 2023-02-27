package com.vironit.flowmessenger

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.models.Chat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ChatsViewModel : ViewModel() {

    private val chats =
        Firebase.firestore.collection("/users/${Firebase.auth.currentUser?.email}/chats")

    fun getChatsFlow() = callbackFlow {
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

    companion object {
        private const val TAG = "ChatsViewModel"
    }

}