package com.pachira.prog7313poepachira

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedCategoryId: String? = null

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val iconView: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        fun bind(category: Category) {
            tvCategoryName.text = category.name

            try {
                cardView.setCardBackgroundColor(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                cardView.setCardBackgroundColor(Color.parseColor("#3F51B5"))
            }

            val resourceId = itemView.context.resources.getIdentifier(
                category.iconName, "drawable", itemView.context.packageName
            )
            if (resourceId != 0) {
                iconView.setImageResource(resourceId)
                iconView.visibility = View.VISIBLE
            } else {
                iconView.setImageResource(R.drawable.ic_default_icon) // fallback
            }

            // Set selected state
            val isSelected = category.id == selectedCategoryId
            cardView.strokeWidth = if (isSelected) 4 else 0

            itemView.setOnClickListener {
                selectedCategoryId = category.id
                onCategorySelected(category)
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    fun setSelectedCategory(categoryId: String) {
        selectedCategoryId = categoryId
        notifyDataSetChanged()
    }
}
