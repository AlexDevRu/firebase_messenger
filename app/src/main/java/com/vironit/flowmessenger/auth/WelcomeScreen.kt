package com.vironit.flowmessenger.auth

import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.MainActivity
import com.vironit.flowmessenger.R
import com.vironit.flowmessenger.databinding.ActivityAuthBinding


class WelcomeScreen : AppCompatActivity(), View.OnClickListener {

    private lateinit var signInRequest : BeginSignInRequest
    private lateinit var signUpRequest : BeginSignInRequest
    private lateinit var oneTapClient: SignInClient

    private lateinit var binding: ActivityAuthBinding

    private val oneTapResult = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(it.data)
            val idToken = credential.googleIdToken
            when {
                idToken != null -> {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    Firebase.auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = Firebase.auth.currentUser
                                val email = user?.email.orEmpty()
                                Log.d(TAG, "onActivityResult: user = $email")
                                Firebase.firestore.collection("/users")
                                    .document(email).set(hashMapOf<String, Any>()).addOnCompleteListener {
                                        if (it.isSuccessful)
                                            goToMainScreen()
                                    }
                            }
                        }
                }
                else -> {
                    // Shouldn't happen.
                }
            }
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> {
                    Log.d("one tap", "One-tap dialog was closed.")
                    // Don't re-prompt the user.
                    Snackbar.make(binding.root, "One-tap dialog was closed.", Snackbar.LENGTH_SHORT).show()
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    Log.d("one tap", "One-tap encountered a network error.")
                    // Try again or just ignore.
                    Snackbar.make(binding.root, "One-tap encountered a network error.", Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    Log.d("one tap", "Couldn't get credential from result." +
                            " (${e.localizedMessage})")
                    Snackbar.make(binding.root, "Couldn't get credential from result.\" +\n" +
                            " (${e.localizedMessage})", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oneTapClient = Identity.getSignInClient(this)
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.your_web_client_id))
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.your_web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            .build()

        binding.btnGoogle.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        if (Firebase.auth.currentUser != null)
            goToMainScreen()
    }

    private fun goToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    override fun onClick(view: View?) {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(
                this
            ) { result ->
                try {
                    val ib = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapResult.launch(ib)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("btn click", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e -> // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG, e.localizedMessage.orEmpty())
                displaySignUp()
            }
    }

    private fun displaySignUp() {
        oneTapClient.beginSignIn(signUpRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    val ib = IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    oneTapResult.launch(ib)
                } catch (e: SendIntentException) {
                    Log.e("btn click", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                displaySignUp()
                Log.d("btn click", e.localizedMessage!!)
            }
    }
    
    companion object {
        private const val TAG = "WelcomeScreen"
    }

}