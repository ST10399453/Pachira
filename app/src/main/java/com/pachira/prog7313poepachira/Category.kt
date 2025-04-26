package com.pachira.prog7313poepachira

data class Category(
    val id: String = "",
    val name: String = "",
    val colorHex: String = "#3F51B5",
    val iconName: String = "",
    val type: String = "expense",
    val budgetLimit: Double = 0.0
) {
    // Required empty constructor for Firebase
    constructor() : this("", "", "#3F51B5", "", "expense", 0.0)
}
