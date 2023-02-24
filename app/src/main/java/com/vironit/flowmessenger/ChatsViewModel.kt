package com.vironit.flowmessenger

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.models.Chat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class ChatsViewModel : ViewModel() {

    private val chats = Firebase.firestore.collection("/users/${Firebase.auth.currentUser?.email}/chats")

    fun getChatsFlow() = callbackFlow {
        val registration = chats.addSnapshotListener { value, error ->
            if (value != null) {
                val list = mutableListOf<Chat>()
                for (doc in value) {
                    list.add(Chat(doc.id))
                }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }

}