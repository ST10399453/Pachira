package com.pachira.prog7313poepachira

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class DashboardActivity : BaseActivity() {

    // Firebase references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI elements
    private lateinit var tvBalanceAmount: TextView
    private lateinit var tvIncomeAmount: TextView
    private lateinit var tvExpenseAmount: TextView
    private lateinit var btnAddIncome: MaterialButton
    private lateinit var btnAddExpense: MaterialButton
    private lateinit var btnLogout: LinearLayout
    private lateinit var cardIncome: CardView
    private lateinit var cardExpenses: CardView
    private lateinit var rvCategorySummaries: RecyclerView

    // Adapters
    private lateinit var categorySummaryAdapter: CategorySummaryAdapter

    // Data variables
    private var totalIncome = 0.0
    private var totalExpenses = 0.0
    private var currentBalance = 0.0
    private val categorySummaries = mutableListOf<CategorySummary>()
    private val categories = mutableListOf<Category>()
    private val categoryAmounts = mutableListOf<Pair<String, Double>>()

    // ValueEventListener references to remove when activity is destroyed
    private var expensesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Check if user is logged in
        if (auth.currentUser == null) {
            // User is not logged in, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize UI elements
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount)
        tvIncomeAmount = findViewById(R.id.tvIncomeAmount)
        tvExpenseAmount = findViewById(R.id.tvExpenseAmount)
        btnAddIncome = findViewById(R.id.btnAddIncome)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnLogout = findViewById(R.id.btnLogout)
        cardIncome = findViewById(R.id.cardIncome)
        cardExpenses = findViewById(R.id.cardExpenses)
        rvCategorySummaries = findViewById(R.id.rvCategorySummaries)

        // Set up RecyclerView
        categorySummaryAdapter = CategorySummaryAdapter(
            categorySummaries,
            { categorySummary ->
                // Handle category summary click
                val intent = Intent(this, TransactionsActivity::class.java)
                intent.putExtra("transactionType", categorySummary.type)
                intent.putExtra("categoryId", categorySummary.categoryId)
                startActivity(intent)
            },
            { categorySummary ->
                // Handle edit click
                val category = categories.find { it.id == categorySummary.categoryId }
                category?.let {
                    showEditCategoryBottomSheet(it)
                }
            }
        )

        rvCategorySummaries.layoutManager = LinearLayoutManager(this)
        rvCategorySummaries.adapter = categorySummaryAdapter

        // Set click listeners
        btnAddIncome.setOnClickListener {
            startActivity(Intent(this, AddIncomeActivity::class.java))
        }

        btnAddExpense.setOnClickListener {
            if (currentBalance <= 0) {
                Toast.makeText(this, "You have no available balance to add expenses", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra("availableBalance", currentBalance)
                startActivity(intent)
            }
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        cardIncome.setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra("transactionType", "income")
            startActivity(intent)
        }

        cardExpenses.setOnClickListener {
            val intent = Intent(this, TransactionsActivity::class.java)
            intent.putExtra("transactionType", "expense")
            startActivity(intent)
        }

        // Load data from Firebase
        loadUserData()

        // Setup bottom navigation
        setupBottomNavigation()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Perform logout
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Remove listeners to prevent memory leaks
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            expensesListener?.let {
                database.child("users").child(userId).child("expenses").removeEventListener(it)
            }
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Listen for income changes
        database.child("users").child(userId).child("income")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalIncome = 0.0
                    for (incomeSnapshot in snapshot.children) {
                        val income = incomeSnapshot.getValue(Transaction::class.java)
                        income?.let {
                            totalIncome += it.amount
                        }
                    }
                    updateIncomeUI()
                    calculateBalance()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        // Listen for expense changes
        database.child("users").child(userId).child("expenses")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalExpenses = 0.0
                    for (expenseSnapshot in snapshot.children) {
                        val expense = expenseSnapshot.getValue(Transaction::class.java)
                        expense?.let {
                            totalExpenses += it.amount
                        }
                    }
                    updateExpenseUI()
                    calculateBalance()
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        // Load category summaries
        loadCategorySummaries()
    }

    private fun loadCategorySummaries() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // First, load all categories
        database.child("users").child(userId).child("categories")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categories.clear()

                    for (categorySnapshot in snapshot.children) {
                        val category = categorySnapshot.getValue(Category::class.java)
                        category?.let {
                            categories.add(it)
                        }
                    }

                    // Now load transactions to calculate amounts per category
                    calculateCategorySummaries(categories)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun calculateCategorySummaries(categories: List<Category>) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Clear existing summaries
        categorySummaries.clear()
        categoryAmounts.clear()

        // Process expense categories
        val expenseCategories = categories.filter { it.type == "expense" }

        // Remove previous listener if it exists
        expensesListener?.let {
            database.child("users").child(userId).child("expenses").removeEventListener(it)
        }

        // Create and add new listener
        expensesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear category amounts before recalculating
                categoryAmounts.clear()

                // Calculate total for each category
                for (transactionSnapshot in snapshot.children) {
                    val transaction = transactionSnapshot.getValue(Transaction::class.java)
                    transaction?.let {
                        val categoryId = it.categoryId
                        val amount = it.amount

                        // Find if category already exists in our list
                        val existingPairIndex = categoryAmounts.indexOfFirst { pair -> pair.first == categoryId }

                        if (existingPairIndex != -1) {
                            // Update existing amount
                            val existingPair = categoryAmounts[existingPairIndex]
                            val newTotal = existingPair.second + amount
                            categoryAmounts[existingPairIndex] = Pair(categoryId, newTotal)
                        } else {
                            // Add new category amount
                            categoryAmounts.add(Pair(categoryId, amount))
                        }
                    }
                }

                // Clear existing summaries before adding new ones
                categorySummaries.clear()

                // Create category summaries
                for (category in expenseCategories) {
                    val amountPair = categoryAmounts.find { it.first == category.id }
                    val amount = amountPair?.second ?: 0.0
                    val limit = category.budgetLimit
                    val percentage = if (limit > 0) ((amount / limit) * 100).toInt().coerceAtMost(100) else 0

                    val summary = CategorySummary(
                        categoryId = category.id,
                        name = category.name,
                        colorHex = category.colorHex,
                        iconName = category.iconName,
                        amount = amount,
                        limit = limit,
                        percentage = percentage,
                        type = "expense"
                    )

                    categorySummaries.add(summary)
                }

                // Sort by percentage of limit (highest first), then by amount
                categorySummaries.sortWith(compareByDescending<CategorySummary> {
                    if (it.limit > 0) it.percentage else 0
                }.thenByDescending {
                    it.amount
                })

                // Update UI
                categorySummaryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Toast.makeText(this@DashboardActivity, "Failed to load expenses: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Add the listener
        database.child("users").child(userId).child("expenses").addValueEventListener(expensesListener!!)
    }

    private fun showEditCategoryBottomSheet(category: Category) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null)
        bottomSheetDialog.setContentView(dialogView)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.et_category_name)
        val budgetLimitContainer = dialogView.findViewById<LinearLayout>(R.id.budget_limit_container)
        val etBudgetLimit = dialogView.findViewById<EditText>(R.id.et_budget_limit)
        val colorPreview = dialogView.findViewById<View>(R.id.color_preview)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val iconContainer = dialogView.findViewById<LinearLayout>(R.id.icon_container)
        val btnCreateCategory = dialogView.findViewById<Button>(R.id.btn_create_category)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close)

        // Set up for editing
        dialogTitle.text = "Edit Category"
        etCategoryName.setText(category.name)
        btnCreateCategory.text = "Update Category"

        // Show/hide budget limit based on category type
        if (category.type == "expense") {
            budgetLimitContainer.visibility = View.VISIBLE
            etBudgetLimit.setText(category.budgetLimit.toString())
        } else {
            budgetLimitContainer.visibility = View.GONE
        }

        // Available colors
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E"
        )

        // Available icons
        val icons = listOf(
            "", // No icon option
            "ic_category_food", "ic_category_shopping", "ic_category_transport",
            "ic_category_home", "ic_category_entertainment", "ic_category_health",
            "ic_category_salary", "ic_category_gift", "ic_category_investment"
        )

        var selectedColor = category.colorHex
        var selectedIcon = category.iconName

        // Set initial color preview
        try {
            val drawable = colorPreview.background.mutate()
            drawable.setTint(Color.parseColor(selectedColor))
            colorPreview.background = drawable
        } catch (e: Exception) {
            colorPreview.setBackgroundColor(Color.parseColor(selectedColor))
        }

        // Add color options
        for (color in colors) {
            val colorItemView = LayoutInflater.from(this).inflate(R.layout.item_color_select, colorContainer, false)
            val colorView = colorItemView.findViewById<View>(R.id.color_view)
            val checkIcon = colorItemView.findViewById<ImageView>(R.id.check_icon)

            try {
                val drawable = colorView.background.mutate()
                drawable.setTint(Color.parseColor(color))
                colorView.background = drawable
            } catch (e: Exception) {
                colorView.setBackgroundColor(Color.parseColor(color))
            }

            if (color == selectedColor) {
                checkIcon.visibility = View.VISIBLE
            }

            colorItemView.setOnClickListener {
                // Update selected color
                selectedColor = color

                // Update color preview
                try {
                    val drawable = colorPreview.background.mutate()
                    drawable.setTint(Color.parseColor(color))
                    colorPreview.background = drawable
                } catch (e: Exception) {
                    colorPreview.setBackgroundColor(Color.parseColor(color))
                }

                // Update check icons
                for (i in 0 until colorContainer.childCount) {
                    val child = colorContainer.getChildAt(i)
                    child.findViewById<ImageView>(R.id.check_icon).visibility =
                        if (i == colors.indexOf(color)) View.VISIBLE else View.GONE
                }
            }

            colorContainer.addView(colorItemView)
        }

        // Add icon options (including "No Icon" option)
        for (icon in icons) {
            val iconItemView = LayoutInflater.from(this).inflate(R.layout.item_icon_select, iconContainer, false)
            val iconView = iconItemView.findViewById<ImageView>(R.id.icon_view)
            val selectionIndicator = iconItemView.findViewById<View>(R.id.selection_indicator)

            if (icon.isEmpty()) {
                // "No Icon" option
                iconView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                iconView.setColorFilter(Color.GRAY)
            } else {
                // Get drawable resource ID
                val resourceId = resources.getIdentifier(icon, "drawable", packageName)
                iconView.setImageResource(resourceId)
            }

            if (icon == selectedIcon) {
                selectionIndicator.visibility = View.VISIBLE
            }

            iconItemView.setOnClickListener {
                // Update selected icon
                selectedIcon = icon

                // Update selection indicators
                for (i in 0 until iconContainer.childCount) {
                    val child = iconContainer.getChildAt(i)
                    child.findViewById<View>(R.id.selection_indicator).visibility =
                        if (i == icons.indexOf(icon)) View.VISIBLE else View.GONE
                }
            }

            iconContainer.addView(iconItemView)
        }

        btnClose.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        btnCreateCategory.setOnClickListener {
            val name = etCategoryName.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check for duplicate category name (only if name has changed)
            if (name != category.name && isDuplicateCategory(name, category.type)) {
                Toast.makeText(this, "A category with this name already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Parse budget limit
            val budgetLimitStr = etBudgetLimit.text.toString().trim()
            val budgetLimit = if (budgetLimitStr.isEmpty() || category.type != "expense")
                0.0
            else
                budgetLimitStr.toDoubleOrNull() ?: 0.0

            // Update existing category
            updateCategory(category.id, name, selectedColor, selectedIcon, budgetLimit)

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun isDuplicateCategory(name: String, type: String): Boolean {
        return categories.any {
            it.name.equals(name, ignoreCase = true) && it.type == type
        }
    }

    private fun updateCategory(categoryId: String, name: String, colorHex: String, iconName: String, budgetLimit: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoryRef = database.child("users").child(userId).child("categories").child(categoryId)

        // Instead of using a HashMap, update each field individually
        categoryRef.child("name").setValue(name)
        categoryRef.child("colorHex").setValue(colorHex)
        categoryRef.child("iconName").setValue(iconName)
        categoryRef.child("budgetLimit").setValue(budgetLimit)
            .addOnSuccessListener {
                Toast.makeText(this, "Category updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update category: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateIncomeUI() {
        tvIncomeAmount.text = formatCurrency(totalIncome)
    }

    private fun updateExpenseUI() {
        tvExpenseAmount.text = formatCurrency(totalExpenses)
    }

    private fun calculateBalance() {
        currentBalance = totalIncome - totalExpenses
        // Ensure balance is never negative
        if (currentBalance < 0) {
            currentBalance = 0.0
        }
        tvBalanceAmount.text = formatCurrency(currentBalance)
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        return format.format(amount)
    }
}
