package com.pachira.prog7313poepachira

data class CategorySummary(
    val categoryId: String,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val amount: Double,
    val limit: Double,
    val percentage: Int,
    val type: String
)
