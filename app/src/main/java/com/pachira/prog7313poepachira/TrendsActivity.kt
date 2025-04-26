package com.pachira.prog7313poepachira

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TrendsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvNetBalance: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var pieChartView: PieChartView
    private lateinit var incomeTab: TextView
    private lateinit var expenseTab: TextView

    private lateinit var transactionAdapter: TransactionAdapter
    private val transactions = mutableListOf<Transaction>()

    private var totalIncome = 0.0
    private var totalExpenses = 0.0
    private var currentTab = "expense" // Default to expense tab

    // Data for pie chart
    private val categoryAmounts = mutableMapOf<String, Double>()
    private val categoryColors = mutableMapOf<String, String>()
    private val categoryNames = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trends)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup bottom navigation
        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_trends)
    }
}
