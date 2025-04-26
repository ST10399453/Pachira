package com.pachira.prog7313poepachira

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var tvEmail: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize UI elements
        tvEmail = findViewById(R.id.tvEmail)
        tvMemberSince = findViewById(R.id.tvMemberSince)
        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // Set click listeners
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Load user data
        loadUserData()

        // Setup bottom navigation
        setupBottomNavigation()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Set email
        tvEmail.text = currentUser.email

        // Get user data from database
        database.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L

                    if (createdAt > 0) {
                        val date = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                            .format(Date(createdAt))
                        tvMemberSince.text = "Member since: $date"
                    } else {
                        tvMemberSince.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    tvMemberSince.visibility = View.GONE
                }
            })
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Perform logout
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Yes") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Delete user data from database
        database.child("users").child(userId).removeValue()
            .addOnSuccessListener {
                // Delete user authentication
                currentUser.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finishAffinity()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete account data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
