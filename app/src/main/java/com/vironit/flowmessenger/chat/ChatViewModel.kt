package com.vironit.flowmessenger.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.models.Message
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resumeWithException

class ChatViewModel : ViewModel() {

    private lateinit var myMessagesCollection: CollectionReference
    private lateinit var personMessagesCollection: CollectionReference

    private val myEmail: String
        get() = Firebase.auth.currentUser?.email.orEmpty()

    var userEmail: String = ""
        set(value) {
            field = value
            myMessagesCollection =
                Firebase.firestore.collection("/users/$myEmail/chats/$field/messages")
            personMessagesCollection =
                Firebase.firestore.collection("/users/$field/chats/$myEmail/messages")
        }

    private val _messages = MutableStateFlow(emptyList<Message>())
    val messages = _messages.asSharedFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asSharedFlow()

    companion object {
        private const val TAG = "ChatViewModel"
    }

    fun start() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val myMessages = getMessages(myMessagesCollection, myEmail)
                val personMessages = getMessages(personMessagesCollection, userEmail)
                val newList = mutableListOf<Message>()
                newList.addAll(myMessages)
                newList.addAll(personMessages)
                newList.sortBy { it.createdAt }
                _messages.value = newList
                Log.d(TAG, "start1: $newList")
            } finally {
                _loading.value = false
            }
            collectNewMessages()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getMessages(collectionReference: CollectionReference, email: String) =
        suspendCancellableCoroutine<List<Message>> { continuation ->
            collectionReference.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val list = mutableListOf<Message>()
                    for (doc in task.result.documents) {
                        val message = Message(
                            id = doc.id,
                            email = email,
                            text = doc.getString("text").orEmpty(),
                            createdAt = doc.getTimestamp("date")!!.toDate()
                        )
                        Log.d(TAG, "getMyMessages: $message")
                        list.add(message)
                    }
                    continuation.resume(list) {}
                } else {
                    continuation.resumeWithException(task.exception ?: Exception())
                }
            }
        }

    private fun collectNewMessages() {
        viewModelScope.launch {
            merge(getMyMessages(), getPersonMessages()).collect { newMessage ->
                _messages.value = when {
                    _messages.value.isEmpty() -> mutableListOf(newMessage)
                    _messages.value.last() <= newMessage -> _messages.value + listOf(newMessage)
                    else -> {
                        val newList = mutableListOf<Message>()
                        newList.addAll(_messages.value)
                        val i = newList.lastIndex
                        newList.add(newMessage)
                        val newValue = newList[i]
                        newList[i] = newList[i + 1]
                        newList[i + 1] = newValue
                        newList
                    }
                }
            }
        }
    }

    private fun getMyMessages() = callbackFlow {
        val registration = listenMessages(myMessagesCollection, myEmail)
        awaitClose { registration.remove() }
    }

    private fun getPersonMessages() = callbackFlow {
        val registration = listenMessages(personMessagesCollection, userEmail)
        awaitClose { registration.remove() }
    }

    private fun ProducerScope<Message>.listenMessages(
        collectionReference: CollectionReference,
        email: String
    ) = collectionReference.orderBy("date", Query.Direction.ASCENDING)
        .addSnapshotListener { value, error ->
            if (error != null) {
                Log.w(TAG, "Listen failed.", error)
                cancel()
                return@addSnapshotListener
            }

            if (value != null) {
                for (doc in value) {
                    val message = Message(
                        id = doc.id,
                        email = email,
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("date")!!.toDate()
                    )
                    viewModelScope.launch { send(message) }
                }
            }
        }

    fun sendMessage(text: String) {
        myMessagesCollection.add(
            hashMapOf(
                "text" to text,
                "date" to Timestamp.now()
            )
        )

        val userChatDoc = Firebase.firestore.collection("/users/$userEmail/chats").document(myEmail)
        userChatDoc.get().addOnCompleteListener {
            if (it.isSuccessful && !it.result.exists()) {
                userChatDoc.set(hashMapOf<String, Any>())
            }
        }
    }

}