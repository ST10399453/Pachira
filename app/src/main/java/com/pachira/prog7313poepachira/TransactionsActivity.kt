package com.pachira.prog7313poepachira

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var startDateLayout: LinearLayout
    private lateinit var endDateLayout: LinearLayout
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnApplyFilter: Button
    private lateinit var rvTransactions: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val transactions = mutableListOf<Transaction>()
    private lateinit var transactionAdapter: TransactionAdapter

    private var startDate = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1) // Default to 1 month ago
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private var endDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    private var transactionType = "" // "income" or "expense" or "" for all

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize UI elements
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        startDateLayout = findViewById(R.id.startDateLayout)
        endDateLayout = findViewById(R.id.endDateLayout)
        tvStartDate = findViewById(R.id.tvStartDate)
        tvEndDate = findViewById(R.id.tvEndDate)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        rvTransactions = findViewById(R.id.rvTransactions)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        // Get transaction type from intent
        transactionType = intent.getStringExtra("transactionType") ?: ""

        // Set title based on transaction type
        when (transactionType) {
            "income" -> tvTitle.text = "Income Transactions"
            "expense" -> tvTitle.text = "Expense Transactions"
            else -> tvTitle.text = "All Transactions"
        }

        // Set up RecyclerView
        transactionAdapter = TransactionAdapter(transactions) { transaction ->
            // Handle transaction click (e.g., show details)
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = transactionAdapter

        // Set up date displays
        updateDateDisplays()

        // Set up click listeners
        btnBack.setOnClickListener {
            finish()
        }

        startDateLayout.setOnClickListener {
            showDatePicker(true)
        }

        endDateLayout.setOnClickListener {
            showDatePicker(false)
        }

        btnApplyFilter.setOnClickListener {
            loadTransactions()
        }

        // Load initial transactions
        loadTransactions()
    }

    private fun updateDateDisplays() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvStartDate.text = dateFormat.format(startDate.time)
        tvEndDate.text = dateFormat.format(endDate.time)
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startDate else endDate
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                if (isStartDate) {
                    startDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    startDate.set(Calendar.MILLISECOND, 0)
                } else {
                    endDate.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)
                    endDate.set(Calendar.MILLISECOND, 999)
                }
                updateDateDisplays()
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    private fun loadTransactions() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Clear existing transactions
        transactions.clear()

        // Show loading state (could add a progress bar here)

        // Reference to the appropriate node based on transaction type
        val query = when (transactionType) {
            "income" -> database.child("users").child(userId).child("income")
            "expense" -> database.child("users").child(userId).child("expenses")
            else -> {
                // For "all" transactions, we need to query both income and expenses
                // This is a simplified approach; in a real app, you might want to use a more efficient method
                loadIncomeTransactions()
                loadExpenseTransactions()
                return
            }
        }

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (transactionSnapshot in snapshot.children) {
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        // Filter by date
                        if (it.date in startDate.timeInMillis..endDate.timeInMillis) {
                            transactions.add(it)
                        }
                    }
                }

                // Sort transactions by date (newest first)
                transactions.sortByDescending { it.date }

                // Update UI
                updateTransactionsUI()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                updateTransactionsUI()
            }
        })
    }

    private fun loadIncomeTransactions() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("income")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (transactionSnapshot in snapshot.children) {
                        val transaction = transactionSnapshot.getValue(Transaction::class.java)
                        transaction?.let {
                            // Filter by date
                            if (it.date in startDate.timeInMillis..endDate.timeInMillis) {
                                transactions.add(it)
                            }
                        }
                    }

                    // After loading income, load expenses
                    loadExpenseTransactions()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    loadExpenseTransactions()
                }
            })
    }

    private fun loadExpenseTransactions() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("expenses")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (transactionSnapshot in snapshot.children) {
                        val transaction = transactionSnapshot.getValue(Transaction::class.java)
                        transaction?.let {
                            // Filter by date
                            if (it.date in startDate.timeInMillis..endDate.timeInMillis) {
                                transactions.add(it)
                            }
                        }
                    }

                    // Sort transactions by date (newest first)
                    transactions.sortByDescending { it.date }

                    // Update UI
                    updateTransactionsUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                    updateTransactionsUI()
                }
            })
    }

    private fun updateTransactionsUI() {
        if (transactions.isEmpty()) {
            rvTransactions.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvTransactions.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            transactionAdapter.notifyDataSetChanged()
        }
    }
}
