package com.pachira.prog7313poepachira

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var btnGoogle: ImageView
    private lateinit var btnFacebook: ImageView
    private lateinit var btnApple: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var cbRememberMe: CheckBox
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        btnGoogle = findViewById(R.id.btnGoogle)
        btnFacebook = findViewById(R.id.btnFacebook)
        btnApple = findViewById(R.id.btnApple)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        val sharedPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        val savedEmail = sharedPrefs.getString("email", "")
        val savedPassword = sharedPrefs.getString("password", "")

        etEmail.setText(savedEmail)
        etPassword.setText(savedPassword)
        cbRememberMe.isChecked = !savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()


        btnLogin.setOnClickListener {
            loginUser()
        }

        // Set click listeners for social login buttons
        btnGoogle.setOnClickListener {
            // Implement Google sign-in logic
            Toast.makeText(this, "Google sign-in clicked", Toast.LENGTH_SHORT).show()
        }

        btnFacebook.setOnClickListener {
            // Implement Facebook sign-in logic
            Toast.makeText(this, "Facebook sign-in clicked", Toast.LENGTH_SHORT).show()
        }

        btnApple.setOnClickListener {
            // Implement Apple sign-in logic
            Toast.makeText(this, "Apple sign-in clicked", Toast.LENGTH_SHORT).show()
        }

        setRegTextView()
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate input
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        // Show loading indicator
        btnLogin.isEnabled = false
        btnLogin.text = "Logging in..."

        // Authenticate with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Hide loading indicator
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.Login)

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        // Save the user's credentials if "Remember Me" is checked
                        val sharedPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                        val editor = sharedPrefs.edit()
                        if (cbRememberMe.isChecked) {
                            editor.putString("email", email)
                            editor.putString("password", password)
                        } else {
                            editor.clear()
                        }
                        editor.apply()

                        // Login successful
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Close LoginActivity
                    } else {
                        // User exists but email is not verified
                        Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show()
                        auth.signOut() // Sign out just in case
                    }
                } else {
                    // Login failed
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }

            }
    }

    private fun setRegTextView() {
        val fullText = "Don't have an Account? Sign Up"
        val spannableString = SpannableString(fullText)

        val registerStart = fullText.indexOf("Sign Up")
        val registerEnd = registerStart + "Sign Up".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@LoginActivity, R.color.purple) // Your purple color
                ds.isUnderlineText = true // Optional: remove underline
            }
        }

        spannableString.setSpan(clickableSpan, registerStart, registerEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvRegister.text = spannableString
        tvRegister.movementMethod = LinkMovementMethod.getInstance()
        tvRegister.highlightColor = Color.TRANSPARENT
    }
}
