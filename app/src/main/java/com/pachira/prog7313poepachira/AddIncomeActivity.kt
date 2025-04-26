package com.pachira.prog7313poepachira

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AddIncomeActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSave: Button
    private lateinit var tvDate: TextView
    private lateinit var btnDatePicker: LinearLayout
    private lateinit var rvCategories: RecyclerView
    private lateinit var btnAddCategory: View
    private lateinit var btnBack: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

    private var selectedDate = Calendar.getInstance()
    private var selectedCategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_income)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI elements
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSave)
        tvDate = findViewById(R.id.tvDate)
        btnDatePicker = findViewById(R.id.btnDatePicker)
        rvCategories = findViewById(R.id.rvCategories)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        btnBack = findViewById(R.id.btnBack)

        // Add this after initializing variables in onCreate
        if (savedInstanceState != null) {
            selectedCategoryId = savedInstanceState.getString("selectedCategoryId")
        }

        // Set up RecyclerView for categories
        categoryAdapter = CategoryAdapter(categories) { category ->
            selectedCategoryId = category.id
            // Update UI to show selected category
            categoryAdapter.setSelectedCategory(category.id)
        }

        rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCategories.adapter = categoryAdapter

        // Set up date picker
        updateDateDisplay()
        btnDatePicker.setOnClickListener {
            showDatePicker()
        }

        // Set up add category button
        btnAddCategory.setOnClickListener {
            showAddCategoryBottomSheet()
        }

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up save button
        btnSave.setOnClickListener {
            saveIncome()
        }

        // Load categories from Firebase
        loadCategories()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the selected category
        outState.putString("selectedCategoryId", selectedCategoryId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore the selected category
        selectedCategoryId = savedInstanceState.getString("selectedCategoryId")
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMMM - yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())

        val monthYear = dateFormat.format(selectedDate.time)
        val day = dayFormat.format(selectedDate.time)

        tvDate.text = monthYear

        // Update calendar view (in a real implementation, you would update the calendar grid)
    }

    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                updateDateDisplay()
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    private fun loadCategories() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoriesRef = database.reference
            .child("users")
            .child(userId)
            .child("categories")
            .orderByChild("type")
            .equalTo("income")

        categoriesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()

                for (categorySnapshot in snapshot.children) {
                    val category = categorySnapshot.getValue(Category::class.java)
                    category?.let {
                        categories.add(it)
                    }
                }

                // If no categories exist, add default ones
                if (categories.isEmpty()) {
                    addDefaultCategories()
                } else {
                    categoryAdapter.notifyDataSetChanged()
                }

                // Add this at the end of the onDataChange callback in loadCategories method
                if (selectedCategoryId != null) {
                    categoryAdapter.setSelectedCategory(selectedCategoryId!!)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AddIncomeActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addDefaultCategories() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoriesRef = database.reference.child("users").child(userId).child("categories")

        val defaultCategories = listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "Salary",
                colorHex = "#36D1DC",
                iconName = "ic_category_salary",
                type = "income",
                budgetLimit = 0.0 // Income doesn't typically have a budget limit
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Gifts",
                colorHex = "#FFDD00",
                iconName = "ic_category_gift",
                type = "income",
                budgetLimit = 0.0
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Investment",
                colorHex = "#FF5733",
                iconName = "ic_category_investment",
                type = "income",
                budgetLimit = 0.0
            )
        )

        for (category in defaultCategories) {
            categoriesRef.child(category.id).setValue(category)
        }
    }

    private fun showAddCategoryBottomSheet(categoryToEdit: Category? = null) {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_category, null)
        bottomSheetDialog.setContentView(dialogView)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val etCategoryName = dialogView.findViewById<EditText>(R.id.et_category_name)
        val budgetLimitContainer = dialogView.findViewById<LinearLayout>(R.id.budget_limit_container)
        val colorPreview = dialogView.findViewById<View>(R.id.color_preview)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.color_container)
        val iconContainer = dialogView.findViewById<LinearLayout>(R.id.icon_container)
        val btnCreateCategory = dialogView.findViewById<Button>(R.id.btn_create_category)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btn_close)

        // Hide budget limit for income categories
        budgetLimitContainer.visibility = View.GONE

        // Set up for editing if a category is provided
        if (categoryToEdit != null) {
            dialogTitle.text = "Edit Category"
            etCategoryName.setText(categoryToEdit.name)
            btnCreateCategory.text = "Update Category"
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

        var selectedColor = categoryToEdit?.colorHex ?: colors[9] // Default to green or existing color
        var selectedIcon = categoryToEdit?.iconName ?: icons[6] // Default to salary icon for income or existing icon

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
            if (categoryToEdit == null || name != categoryToEdit.name) {
                if (isDuplicateCategory(name)) {
                    Toast.makeText(this, "A category with this name already exists", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // For income categories, budget limit is not applicable
            val budgetLimit = 0.0

            if (categoryToEdit != null) {
                // Update existing category
                updateCategory(categoryToEdit.id, name, selectedColor, selectedIcon, budgetLimit)
            } else {
                // Add new category
                addNewCategory(name, selectedColor, selectedIcon, budgetLimit)
            }

            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun isDuplicateCategory(name: String): Boolean {
        return categories.any {
            it.name.equals(name, ignoreCase = true)
        }
    }

    private fun addNewCategory(name: String, colorHex: String, iconName: String, budgetLimit: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoriesRef = database.reference.child("users").child(userId).child("categories")
        val newCategoryId = categoriesRef.push().key ?: UUID.randomUUID().toString()

        val newCategory = Category(
            id = newCategoryId,
            name = name,
            colorHex = colorHex,
            iconName = iconName,
            type = "income",
            budgetLimit = budgetLimit
        )

        categoriesRef.child(newCategoryId).setValue(newCategory)
            .addOnSuccessListener {
                Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add category: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCategory(categoryId: String, name: String, colorHex: String, iconName: String, budgetLimit: Double) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        val categoryRef = database.reference.child("users").child(userId).child("categories").child(categoryId)

        // Update each field individually
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

    private fun saveIncome() {
        val amountStr = etAmount.text.toString().replace("[^0-9.]".toRegex(), "")
        val description = etDescription.text.toString()

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategoryId == null) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to add income", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading state
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val userId = currentUser.uid
        val incomeRef = database.reference.child("users").child(userId).child("income").push()
        val incomeId = incomeRef.key ?: UUID.randomUUID().toString()

        // Find the selected category
        val selectedCategory = categories.find { it.id == selectedCategoryId }

        val transaction = Transaction(
            id = incomeId,
            amount = amount,
            category = selectedCategory?.name ?: "",
            categoryId = selectedCategoryId ?: "",
            description = description,
            date = selectedDate.timeInMillis,
            type = "income"
        )

        incomeRef.setValue(transaction)
            .addOnSuccessListener {
                Toast.makeText(this, "Income added successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                btnSave.text = getString(R.string.add_income)
                Toast.makeText(this, "Failed to add income: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
