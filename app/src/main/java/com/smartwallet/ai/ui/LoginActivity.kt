package com.smartwallet.ai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.smartwallet.ai.R
import com.smartwallet.ai.data.remote.FirestoreManager
import com.smartwallet.ai.databinding.ActivityLoginBinding
import com.smartwallet.ai.utils.PreferenceManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferenceManager = PreferenceManager(this)

        animateUI()
        setupGoogleSignIn()

        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.btnGoogleLogin.setOnClickListener {
            showLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email).addOnCompleteListener {
                    Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Enter email to reset password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateUI() {
        binding.topGradient.translationY = -300f
        binding.topGradient.animate().translationY(0f).setDuration(600).setInterpolator(DecelerateInterpolator()).start()

        binding.cardLogin.alpha = 0f
        binding.cardLogin.translationY = 200f
        binding.cardLogin.animate().alpha(1f).translationY(0f).setStartDelay(400).setDuration(800).start()
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString()
        val pass = binding.etPassword.text.toString()

        if (email.isNotEmpty() && pass.isNotEmpty()) {
            showLoading(true)
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    syncProfileAndNavigate()
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncProfileAndNavigate() {
        val user = auth.currentUser
        if (user != null) {
            lifecycleScope.launch {
                try {
                    val profile = FirestoreManager.getUserProfile(user.uid)
                    if (profile != null) {
                        // Sync local preference with cloud data
                        preferenceManager.saveProfile(
                            profile.name,
                            profile.mobile,
                            profile.monthlyIncome,
                            profile.savingsGoal,
                            profile.salaryDate,
                            profile.currency
                        )
                        profile.profilePicUrl?.let { preferenceManager.setProfilePicUrl(it) }
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        // New user or no profile found
                        startActivity(Intent(this@LoginActivity, ProfileActivity::class.java).apply {
                            putExtra("FIRST_TIME", true)
                        })
                    }
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Sync Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
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
        binding.btnLogin.isEnabled = !show
        binding.btnGoogleLogin.isEnabled = !show
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
                Toast.makeText(this, "Google Sign In Failed: ${e.statusCode}\nCheck SHA-1 and Client ID configuration.", Toast.LENGTH_LONG).show()
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
                syncProfileAndNavigate()
            } else {
                showLoading(false)
                Toast.makeText(this, "Firebase Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
