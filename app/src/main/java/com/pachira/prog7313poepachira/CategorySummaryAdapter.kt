package com.pachira.prog7313poepachira

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.*

class CategorySummaryAdapter(
    private val categorySummaries: List<CategorySummary>,
    private val onItemClick: (CategorySummary) -> Unit,
    private val onEditClick: (CategorySummary) -> Unit
) : RecyclerView.Adapter<CategorySummaryAdapter.CategorySummaryViewHolder>() {

    class CategorySummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvCategoryInitial: TextView = itemView.findViewById(R.id.tvCategoryInitial)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val tvLimit: TextView = itemView.findViewById(R.id.tvLimit)
        val btnEditCategory: ImageButton = itemView.findViewById(R.id.btnEditCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategorySummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return CategorySummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategorySummaryViewHolder, position: Int) {
        val categorySummary = categorySummaries[position]

        // Format currency
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        // Set text values
        holder.tvCategoryName.text = categorySummary.name
        holder.tvAmount.text = format.format(categorySummary.amount)
        holder.tvPercentage.text = "${categorySummary.percentage}%"
        holder.tvLimit.text = "Limit: ${format.format(categorySummary.limit)}"

        // Set progress
        holder.progressBar.progress = categorySummary.percentage

        // Make the layout more compact for horizontal scrolling
        if (categorySummary.limit <= 0) {
            // Hide the limit text if there's no limit set
            holder.tvLimit.visibility = View.GONE
        }

        // Adjust the percentage display to be more compact
        holder.tvPercentage.text = if (categorySummary.percentage > 0) "${categorySummary.percentage}%" else ""

        // Set background color for icon container
        try {
            val drawable = holder.ivCategoryIcon.background.mutate()
            drawable.setTint(Color.parseColor(categorySummary.colorHex))
            holder.ivCategoryIcon.background = drawable

            // Also set the same background color for the initial text if needed
            holder.tvCategoryInitial.background = drawable.constantState?.newDrawable()?.mutate()
        } catch (e: Exception) {
            // Use default color if there's an error
            holder.ivCategoryIcon.setBackgroundColor(Color.parseColor("#3F51B5"))
            holder.tvCategoryInitial.setBackgroundColor(Color.parseColor("#3F51B5"))
        }

        // Handle icon or initial
        if (!categorySummary.iconName.isNullOrEmpty()) {
            try {
                // Map common category names to their resource IDs
                val iconResourceId = when (categorySummary.name.lowercase(Locale.ROOT)) {
                    "food" -> R.drawable.ic_category_food
                    "housing", "home" -> R.drawable.ic_category_home
                    "transport", "transportation" -> R.drawable.ic_category_transport
                    "shopping" -> R.drawable.ic_category_shopping
                    "entertainment" -> R.drawable.ic_category_entertainment
                    "health" -> R.drawable.ic_category_health
                    "salary" -> R.drawable.ic_category_salary
                    "gift" -> R.drawable.ic_category_gift
                    "investment" -> R.drawable.ic_category_investment
                    else -> {
                        // Try to get the resource ID from the iconName
                        holder.itemView.context.resources.getIdentifier(
                            categorySummary.iconName, "drawable", holder.itemView.context.packageName)
                    }
                }

                if (iconResourceId != 0) {
                    holder.ivCategoryIcon.setImageResource(iconResourceId)
                    holder.ivCategoryIcon.visibility = View.VISIBLE
                    holder.tvCategoryInitial.visibility = View.GONE
                } else {
                    // No valid icon, show initial
                    showInitial(holder, categorySummary.name)
                }
            } catch (e: Exception) {
                // Error loading icon, show initial
                showInitial(holder, categorySummary.name)
            }
        } else {
            // No icon name, show initial
            showInitial(holder, categorySummary.name)
        }

        // NEW CODE: Set color based on percentage of limit
        val cardView = holder.itemView as? com.google.android.material.card.MaterialCardView

        if (categorySummary.limit > 0) {
            when {
                // Over limit - red color and red outline
                categorySummary.amount > categorySummary.limit -> {
                    holder.tvAmount.setTextColor(Color.parseColor("#E53935")) // Red
                    cardView?.strokeWidth = 6 // Changed from 2 to 6
                    cardView?.strokeColor = Color.parseColor("#E53935") // Red outline
                }
                // Close to limit (80% or more) - amber/yellow warning
                categorySummary.percentage >= 80 -> {
                    holder.tvAmount.setTextColor(Color.parseColor("#FFC107")) // Amber
                    cardView?.strokeWidth = 3
                    cardView?.strokeColor = Color.parseColor("#FFC107") // Amber outline
                }
                // Normal - use theme color
                else -> {
                    holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.purple)) // Theme color
                    cardView?.strokeWidth = 0 // No outline
                }
            }
        } else {
            // No limit set, use default theme color
            holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.purple))
            cardView?.strokeWidth = 0
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            onItemClick(categorySummary)
        }

        holder.btnEditCategory.setOnClickListener {
            onEditClick(categorySummary)
        }
    }

    private fun showInitial(holder: CategorySummaryViewHolder, name: String) {
        holder.ivCategoryIcon.visibility = View.INVISIBLE
        holder.tvCategoryInitial.visibility = View.VISIBLE
        holder.tvCategoryInitial.text = if (!name.isNullOrEmpty()) name[0].toString().uppercase(Locale.ROOT) else "?"
    }

    override fun getItemCount(): Int = categorySummaries.size
}
