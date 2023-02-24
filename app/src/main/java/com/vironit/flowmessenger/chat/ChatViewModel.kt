package com.vironit.flowmessenger.chat

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.models.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge

class ChatViewModel: ViewModel() {

    private lateinit var myMessages: CollectionReference
    private lateinit var personMessages: CollectionReference

    private val myEmail: String?
        get() = Firebase.auth.currentUser?.email

    var userEmail: String = ""
        set(value) {
            field = value
            myMessages = Firebase.firestore.collection("/users/$myEmail/chats/$field/messages")
            personMessages = Firebase.firestore.collection("/users/$field/chats/$myEmail/messages")
        }

    private fun getMyMessages() = callbackFlow {
        val registration = myMessages.orderBy("date", Query.Direction.ASCENDING).addSnapshotListener { value, error ->
            if (value != null) {
                val list = mutableListOf<Message>()
                for (doc in value) {
                    val message = Message(
                        id = doc.id,
                        email = Firebase.auth.currentUser?.email.orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("date")!!.toDate()
                    )
                    list.add(message)
                }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }

    private fun getPersonMessages() = callbackFlow {
        val registration = personMessages.orderBy("date", Query.Direction.ASCENDING).addSnapshotListener { value, error ->
            if (value != null) {
                val list = mutableListOf<Message>()
                for (doc in value) {
                    val message = Message(
                        id = doc.id,
                        email = userEmail,
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("date")!!.toDate()
                    )
                    list.add(message)
                }
                trySend(list)
            }
        }

        awaitClose { registration.remove() }
    }

    fun getMessages() = merge(getMyMessages(), getPersonMessages())

    fun sendMessage(text: String) {
        Firebase.firestore.collection("/users/$myEmail/chats/$userEmail/messages").add(
            hashMapOf(
                "text" to text,
                "date" to Timestamp.now()
            )
        )
    }

}