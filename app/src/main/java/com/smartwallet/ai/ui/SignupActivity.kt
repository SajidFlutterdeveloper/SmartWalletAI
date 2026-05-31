package com.smartwallet.ai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartwallet.ai.R
import com.smartwallet.ai.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateUI()
        setupGoogleSignIn()

        binding.btnSignup.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty() && confirmPass.isNotEmpty()) {
                if (pass == confirmPass) {
                    showLoading(true)
                    auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Universe Registered Successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ProfileActivity::class.java).apply {
                                putExtra("FIRST_TIME", true)
                            })
                            finish()
                        } else {
                            showLoading(false)
                            Toast.makeText(this, "Error: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogleSignup.setOnClickListener {
            showLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Google Error: ID Token is null. Check Web Client ID in strings.xml", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                showLoading(false)
                Toast.makeText(this, "Google Sign In Failed: ${e.statusCode}\nCommonly caused by wrong SHA-1 or Client ID.", Toast.LENGTH_LONG).show()
            }
        } else {
            showLoading(false)
            Toast.makeText(this, "Sign-in cancelled or failed (Code: ${result.resultCode})", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val googleUser = auth.currentUser
                startActivity(Intent(this, ProfileActivity::class.java).apply {
                    putExtra("FIRST_TIME", true)
                    putExtra("GOOGLE_NAME", googleUser?.displayName)
                })
                finish()
            } else {
                showLoading(false)
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.loadingOverlay.visibility = View.VISIBLE
            val rotate = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_universe)
            findViewById<View>(R.id.ivLoadingLogo)?.startAnimation(rotate)
        } else {
            binding.loadingOverlay.visibility = View.GONE
            findViewById<View>(R.id.ivLoadingLogo)?.clearAnimation()
        }
        binding.btnSignup.isEnabled = !show
        binding.btnGoogleSignup.isEnabled = !show
    }

    private fun animateUI() {
        binding.topGradient.translationY = -300f
        binding.topGradient.animate().translationY(0f).setDuration(600).setInterpolator(DecelerateInterpolator()).start()

        binding.cardSignup.alpha = 0f
        binding.cardSignup.translationY = 200f
        binding.cardSignup.animate().alpha(1f).translationY(0f).setStartDelay(400).setDuration(800).start()
    }
}
