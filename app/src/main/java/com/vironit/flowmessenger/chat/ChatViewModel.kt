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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resumeWithException

class ChatViewModel: ViewModel() {

    private lateinit var myMessagesCollection: CollectionReference
    private lateinit var personMessagesCollection: CollectionReference

    private val myEmail: String?
        get() = Firebase.auth.currentUser?.email

    var userEmail: String = ""
        set(value) {
            field = value
            myMessagesCollection = Firebase.firestore.collection("/users/$myEmail/chats/$field/messages")
            personMessagesCollection = Firebase.firestore.collection("/users/$field/chats/$myEmail/messages")
        }

    private val _messages = MutableStateFlow(emptyList<Message>())
    val messages = _messages.asSharedFlow()

    private var init = true

    companion object {
        private const val TAG = "ChatViewModel"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        viewModelScope.launch {
            val myMessages = suspendCancellableCoroutine<List<Message>> { continuation ->
                myMessagesCollection.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val list = mutableListOf<Message>()
                        for (doc in task.result.documents) {
                            val message = Message(
                                id = doc.id,
                                email = Firebase.auth.currentUser?.email.orEmpty(),
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
            val personMessages = suspendCancellableCoroutine<List<Message>> { continuation ->
                personMessagesCollection.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val list = mutableListOf<Message>()
                        for (doc in task.result.documents) {
                            val message = Message(
                                id = doc.id,
                                email = userEmail,
                                text = doc.getString("text").orEmpty(),
                                createdAt = doc.getTimestamp("date")!!.toDate()
                            )
                            Log.d(TAG, "getPersonMessages: $message")
                            list.add(message)
                        }
                        continuation.resume(list) {}
                    } else {
                        continuation.resumeWithException(task.exception ?: Exception())
                    }
                }
            }
            val newList = mutableListOf<Message>()
            newList.addAll(myMessages)
            newList.addAll(personMessages)
            newList.sortBy { it.createdAt }
            _messages.value = newList
            Log.d(TAG, "start1: $newList")
            collectNewMessages()
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
        val registration = myMessagesCollection.orderBy("date", Query.Direction.ASCENDING).addSnapshotListener { value, error ->
            if (value != null) {
                for (doc in value) {
                    val message = Message(
                        id = doc.id,
                        email = Firebase.auth.currentUser?.email.orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("date")!!.toDate()
                    )
                    Log.d(TAG, "getMyMessages: $message")
                    viewModelScope.launch { send(message) }
                }
            }
        }

        awaitClose { registration.remove() }
    }

    private fun getPersonMessages() = callbackFlow {
        val registration = personMessagesCollection.orderBy("date", Query.Direction.ASCENDING).addSnapshotListener { value, error ->
            if (value != null) {
                for (doc in value) {
                    val message = Message(
                        id = doc.id,
                        email = userEmail,
                        text = doc.getString("text").orEmpty(),
                        createdAt = doc.getTimestamp("date")!!.toDate()
                    )
                    viewModelScope.launch { send(message) }
                }
            }
        }

        awaitClose { registration.remove() }
    }

    fun sendMessage(text: String) {
        Firebase.firestore.collection("/users/$myEmail/chats/$userEmail/messages").add(
            hashMapOf(
                "text" to text,
                "date" to Timestamp.now()
            )
        )
    }

}