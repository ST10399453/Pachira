package com.pachira.prog7313poepachira

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is DashboardActivity) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_budgets -> {
                    if (this !is BudgetsActivity) {
                        startActivity(Intent(this, BudgetsActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_trends -> {
                    if (this !is TrendsActivity) {
                        startActivity(Intent(this, TrendsActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_wallets -> {
                    if (this !is WalletsActivity) {
                        startActivity(Intent(this, WalletsActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfileActivity) {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        overridePendingTransition(0, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }

    protected fun setSelectedNavItem(itemId: Int) {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation?.selectedItemId = itemId
    }

    override fun onResume() {
        super.onResume()

        // Set the correct navigation item based on the current activity
        when (this) {
            is DashboardActivity -> setSelectedNavItem(R.id.nav_home)
            is BudgetsActivity -> setSelectedNavItem(R.id.nav_budgets)
            is TrendsActivity -> setSelectedNavItem(R.id.nav_trends)
            is WalletsActivity -> setSelectedNavItem(R.id.nav_wallets)
            is ProfileActivity -> setSelectedNavItem(R.id.nav_profile)
        }
    }
}
