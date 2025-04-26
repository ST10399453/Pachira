package com.pachira.prog7313poepachira

data class BudgetGoal(
    var id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val createdAt: Long = 0
)
