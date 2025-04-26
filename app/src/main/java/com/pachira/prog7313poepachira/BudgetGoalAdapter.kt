package com.pachira.prog7313poepachira

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.*

class BudgetGoalAdapter(
    private val budgetGoals: List<BudgetGoal>,
    private val onItemClick: (BudgetGoal) -> Unit,
    private val onAddFundsClick: (BudgetGoal) -> Unit
) : RecyclerView.Adapter<BudgetGoalAdapter.BudgetGoalViewHolder>() {

    class BudgetGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalName: TextView = itemView.findViewById(R.id.tvGoalName)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvCurrentAmount: TextView = itemView.findViewById(R.id.tvCurrentAmount)
        val tvTargetAmount: TextView = itemView.findViewById(R.id.tvTargetAmount)
        val btnAddFunds: Button = itemView.findViewById(R.id.btnAddFunds)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetGoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_goal, parent, false)
        return BudgetGoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: BudgetGoalViewHolder, position: Int) {
        val budgetGoal = budgetGoals[position]

        // Format currency
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        // Calculate progress percentage
        val progress = if (budgetGoal.targetAmount > 0)
            ((budgetGoal.currentAmount / budgetGoal.targetAmount) * 100).toInt()
        else 0

        // Set text values
        holder.tvGoalName.text = budgetGoal.name
        holder.tvProgress.text = "$progress%"
        holder.tvCurrentAmount.text = format.format(budgetGoal.currentAmount)
        holder.tvTargetAmount.text = "of ${format.format(budgetGoal.targetAmount)}"

        // Set progress
        holder.progressBar.progress = progress

        // Set click listeners
        holder.itemView.setOnClickListener {
            onItemClick(budgetGoal)
        }

        holder.btnAddFunds.setOnClickListener {
            onAddFundsClick(budgetGoal)
        }
    }

    override fun getItemCount(): Int = budgetGoals.size
}
