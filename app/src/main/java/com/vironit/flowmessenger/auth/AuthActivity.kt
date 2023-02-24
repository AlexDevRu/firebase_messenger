package com.vironit.flowmessenger.auth

import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.vironit.flowmessenger.MainActivity
import com.vironit.flowmessenger.R
import com.vironit.flowmessenger.databinding.ActivityAuthBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class AuthActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var signInRequest : BeginSignInRequest
    private lateinit var signUpRequest : BeginSignInRequest
    private lateinit var oneTapClient: SignInClient

    private lateinit var binding: ActivityAuthBinding

    private lateinit var googleSignInClient: GoogleSignInClient

    private val viewModel by viewModels<AuthViewModel>()

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
                                viewModel.createUser()
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

    private val googleSignInResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val authCredential = GoogleAuthProvider.getCredential(account.idToken, null)
            Firebase.auth.signInWithCredential(authCredential)
                .addOnCompleteListener(this) {
                    if (it.isSuccessful) {
                        viewModel.createUser()
                    }
                }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.your_web_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnGoogle.setOnClickListener(this)

        observe()
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToMain.collectLatest {
                    goToMainScreen()
                }
            }
        }
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
        /*oneTapClient.beginSignIn(signInRequest)
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
            }*/
        val signInIntent = googleSignInClient.signInIntent
        googleSignInResult.launch(signInIntent)
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