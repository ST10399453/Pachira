package com.pachira.prog7313poepachira

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class BudgetsActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var rvBudgetGoals: RecyclerView
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var emptyStateLayout: LinearLayout

    private lateinit var budgetGoalAdapter: BudgetGoalAdapter
    private val budgetGoals = mutableListOf<BudgetGoal>()

    private var availableBalance = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budgets)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is logged in
        if (auth.currentUser == null) {
            finish()
            return
        }

        // Initialize UI elements
        rvBudgetGoals = findViewById(R.id.rvBudgetGoals)
        fabAddGoal = findViewById(R.id.fabAddGoal)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        // Set up RecyclerView with the correct adapter initialization
        budgetGoalAdapter = BudgetGoalAdapter(
            budgetGoals,
            { budgetGoal -> showBudgetGoalDetails(budgetGoal) },
            { budgetGoal -> showAddFundsDialog(budgetGoal) }
        )

        rvBudgetGoals.layoutManager = LinearLayoutManager(this)
        rvBudgetGoals.adapter = budgetGoalAdapter

        // Set click listeners
        fabAddGoal.setOnClickListener {
            showAddBudgetGoalDialog()
        }

        // Load data
        loadAvailableBalance()
        loadBudgetGoals()

        // Setup bottom navigation
        setupBottomNavigation()
        setSelectedNavItem(R.id.nav_budgets)
    }

    private fun loadAvailableBalance() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Get total income
        database.child("users").child(userId).child("income")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalIncome = 0.0
                    for (incomeSnapshot in snapshot.children) {
                        val income = incomeSnapshot.getValue(Transaction::class.java)
                        income?.let {
                            totalIncome += it.amount
                        }
                    }

                    // Get total expenses
                    database.child("users").child(userId).child("expenses")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(expenseSnapshot: DataSnapshot) {
                                var totalExpenses = 0.0
                                for (expenseItem in expenseSnapshot.children) {
                                    val expense = expenseItem.getValue(Transaction::class.java)
                                    expense?.let {
                                        totalExpenses += it.amount
                                    }
                                }

                                // Calculate available balance
                                availableBalance = totalIncome - totalExpenses
                                if (availableBalance < 0) availableBalance = 0.0
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@BudgetsActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetsActivity, "Failed to load income", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadBudgetGoals() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.child("users").child(userId).child("budgetGoals")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    budgetGoals.clear()

                    for (goalSnapshot in snapshot.children) {
                        val goal = goalSnapshot.getValue(BudgetGoal::class.java)
                        goal?.let {
                            budgetGoals.add(it)
                        }
                    }

                    // Sort goals by progress percentage (highest first)
                    budgetGoals.sortByDescending { it.currentAmount / it.targetAmount }

                    // Update UI
                    updateBudgetGoalsUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@BudgetsActivity, "Failed to load budget goals", Toast.LENGTH_SHORT).show()
                    updateBudgetGoalsUI()
                }
            })
    }

    private fun updateBudgetGoalsUI() {
        if (budgetGoals.isEmpty()) {
            rvBudgetGoals.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvBudgetGoals.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            budgetGoalAdapter.notifyDataSetChanged()
        }
    }

    private fun showAddBudgetGoalDialog() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_budget_goal, null)
        bottomSheetDialog.setContentView(dialogView)

        val etGoalName = dialogView.findViewById<EditText>(R.id.etGoalName)
        val etTargetAmount = dialogView.findViewById<EditText>(R.id.etTargetAmount)
        val etInitialAmount = dialogView.findViewById<EditText>(R.id.etInitialAmount)
        val btnCreateGoal = dialogView.findViewById<Button>(R.id.btnCreateGoal)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnCreateGoal.setOnClickListener {
            val name = etGoalName.text.toString().trim()
            val targetAmountStr = etTargetAmount.text.toString().trim()
            val initialAmountStr = etInitialAmount.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (targetAmountStr.isEmpty()) {
                Toast.makeText(this, "Please enter a target amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetAmount = targetAmountStr.toDoubleOrNull() ?: 0.0
            if (targetAmount <= 0) {
                Toast.makeText(this, "Target amount must be greater than zero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val initialAmount = initialAmountStr.toDoubleOrNull() ?: 0.0

            if (initialAmount > availableBalance) {
                Toast.makeText(this, "Initial amount cannot exceed available balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create new budget goal
            addBudgetGoal(name, targetAmount, initialAmount)

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun addBudgetGoal(name: String, targetAmount: Double, initialAmount: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val goalRef = database.child("users").child(userId).child("budgetGoals").push()
        val goalId = goalRef.key ?: UUID.randomUUID().toString()

        val budgetGoal = BudgetGoal(
            id = goalId,
            name = name,
            targetAmount = targetAmount,
            currentAmount = initialAmount,
            createdAt = System.currentTimeMillis()
        )

        goalRef.setValue(budgetGoal)
            .addOnSuccessListener {
                Toast.makeText(this, "Budget goal added successfully", Toast.LENGTH_SHORT).show()

                // If initial amount > 0, create a transaction for it
                if (initialAmount > 0) {
                    createBudgetTransfer(budgetGoal, initialAmount)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add budget goal: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun showBudgetGoalDetails(budgetGoal: BudgetGoal) {
        // Show a dialog with goal details and history
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget_goal_details, null)
        dialog.setContentView(dialogView)

        val tvGoalName = dialogView.findViewById<TextView>(R.id.tvGoalName)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val tvRemainingAmount = dialogView.findViewById<TextView>(R.id.tvRemainingAmount)
        val btnAddFunds = dialogView.findViewById<Button>(R.id.btnAddFunds)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        // Set goal details
        tvGoalName.text = budgetGoal.name

        val progress = if (budgetGoal.targetAmount > 0)
            ((budgetGoal.currentAmount / budgetGoal.targetAmount) * 100).toInt()
        else 0

        tvProgress.text = "$progress%"
        progressBar.progress = progress

        val remainingAmount = budgetGoal.targetAmount - budgetGoal.currentAmount
        tvRemainingAmount.text = "Remaining: ${formatCurrency(remainingAmount)}"

        btnAddFunds.setOnClickListener {
            dialog.dismiss()
            showAddFundsDialog(budgetGoal)
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showAddFundsDialog(budgetGoal: BudgetGoal) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_funds, null)
        dialog.setContentView(dialogView)

        val tvAvailableBalance = dialogView.findViewById<TextView>(R.id.tvAvailableBalance)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnAddFunds = dialogView.findViewById<Button>(R.id.btnAddFunds)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)

        tvAvailableBalance.text = "Available Balance: ${formatCurrency(availableBalance)}"

        btnAddFunds.setOnClickListener {
            val amountStr = etAmount.text.toString().trim()

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull() ?: 0.0

            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount > availableBalance) {
                Toast.makeText(this, "Amount cannot exceed available balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add funds to budget goal
            addFundsToBudgetGoal(budgetGoal, amount)

            dialog.dismiss()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addFundsToBudgetGoal(budgetGoal: BudgetGoal, amount: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val goalRef = database.child("users").child(userId).child("budgetGoals").child(budgetGoal.id)

        // Update current amount
        val newAmount = budgetGoal.currentAmount + amount
        goalRef.child("currentAmount").setValue(newAmount)
            .addOnSuccessListener {
                Toast.makeText(this, "Funds added successfully", Toast.LENGTH_SHORT).show()

                // Create a transaction for the transfer
                createBudgetTransfer(budgetGoal, amount)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add funds: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createBudgetTransfer(budgetGoal: BudgetGoal, amount: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val expenseRef = database.child("users").child(userId).child("expenses").push()
        val expenseId = expenseRef.key ?: UUID.randomUUID().toString()

        val transaction = Transaction(
            id = expenseId,
            amount = amount,
            category = "Budget Transfer",
            categoryId = "budget_transfer",
            description = "Transfer to ${budgetGoal.name}",
            date = System.currentTimeMillis(),
            type = "expense"
        )

        expenseRef.setValue(transaction)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to record transfer: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        return format.format(amount)
    }
}
