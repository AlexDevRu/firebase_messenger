package com.vironit.flowmessenger.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _navigateToMain = MutableSharedFlow<Unit>()
    val navigateToMain : SharedFlow<Unit> = _navigateToMain

    fun createUser() {
        val user = Firebase.auth.currentUser
        val email = user?.email.orEmpty()
        Log.d(TAG, "onActivityResult: user = $email")

        Firebase.firestore.collection("users")
            .document(email).set(hashMapOf<String, Any>()).addOnCompleteListener {
                if (it.isSuccessful) {
                    viewModelScope.launch { _navigateToMain.emit(Unit) }
                }
            }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }

}