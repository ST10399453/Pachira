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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail : EditText
    private  lateinit var btnRestPassword : Button
    private lateinit var tvBackToLogin: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        etEmail = findViewById(R.id.etEmail)
        btnRestPassword = findViewById(R.id.btnRestPassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnRestPassword.setOnClickListener {
            resetPassword()
        }

        returnLogin()
    }
    private fun resetPassword()
    {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        //send link to email
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Password reset email sent. Check your inbox!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun returnLogin() {
        val fullText = "Back to Login"
        val spannableString = SpannableString(fullText)

        val loginStart = fullText.indexOf("Login")
        val loginEnd = loginStart + "Login".length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(this@ForgotPasswordActivity, R.color.purple) // Your purple color
                ds.isUnderlineText = true // Optional: remove underline
            }
        }

        spannableString.setSpan(clickableSpan, loginStart, loginEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvBackToLogin.text = spannableString
        tvBackToLogin.movementMethod = LinkMovementMethod.getInstance()
        tvBackToLogin.highlightColor = Color.TRANSPARENT
    }

}