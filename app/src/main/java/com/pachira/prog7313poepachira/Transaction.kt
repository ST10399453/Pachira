package com.pachira.prog7313poepachira

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val categoryId: String = "",
    val description: String = "",
    val date: Long = 0,
    val type: String = "", // "income" or "expense"
    val imageData: String = "" // Base64 encoded image data for expense receipts
) {
    // Empty constructor required for Firebase
    constructor() : this("", 0.0, "", "", "", 0, "", "")
}

