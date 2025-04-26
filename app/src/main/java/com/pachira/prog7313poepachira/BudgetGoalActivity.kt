package com.pachira.prog7313poepachira

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.NumberFormat
import java.util.*

class BudgetGoalActivity : Activity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BudgetGoalAdapter
    private val budgetGoals = mutableListOf<BudgetGoal>()

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_goal)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.rvBudgetGoals)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter
        adapter = BudgetGoalAdapter(
            budgetGoals,
            { budgetGoal -> showBudgetGoalDetails(budgetGoal) },
            { budgetGoal -> showAddFundsDialog(budgetGoal) }
        )
        recyclerView.adapter = adapter

        // Load budget goals
        loadBudgetGoals()

        // Set up back button
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadBudgetGoals() {
        // Implementation would be similar to BudgetsActivity
        // For now, we'll just use sample data
        val sampleGoal = BudgetGoal(
            id = "1",
            name = "Vacation",
            targetAmount = 10000.0,
            currentAmount = 5000.0,
            createdAt = System.currentTimeMillis()
        )
        budgetGoals.add(sampleGoal)
        adapter.notifyDataSetChanged()
    }

    private fun showBudgetGoalDetails(budgetGoal: BudgetGoal) {
        // Implementation would be similar to BudgetsActivity
    }

    private fun showAddFundsDialog(budgetGoal: BudgetGoal) {
        // Implementation would be similar to BudgetsActivity
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        return format.format(amount)
    }
}
