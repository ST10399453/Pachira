package com.pachira.prog7313poepachira

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val ivReceipt: ImageView = itemView.findViewById(R.id.ivReceipt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Format amount
        val format = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
        val amountText = if (transaction.type == "expense") {
            "-${format.format(transaction.amount)}"
        } else {
            format.format(transaction.amount)
        }

        // Format date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateText = dateFormat.format(Date(transaction.date))

        // Set text values
        holder.tvAmount.text = amountText
        holder.tvCategory.text = transaction.category
        holder.tvDescription.text = transaction.description
        holder.tvDate.text = dateText

        // Set text color based on transaction type
        val textColor = if (transaction.type == "expense") {
            holder.itemView.context.getColor(R.color.expense_red)
        } else {
            holder.itemView.context.getColor(R.color.income_green)
        }
        holder.tvAmount.setTextColor(textColor)

        // Handle receipt image if available
        if (transaction.imageData.isNotEmpty()) {
            val bitmap = ImageUtils.decodeBase64ToBitmap(transaction.imageData)
            if (bitmap != null) {
                holder.ivReceipt.setImageBitmap(bitmap)
                holder.ivReceipt.visibility = View.VISIBLE
            } else {
                holder.ivReceipt.visibility = View.GONE
            }
        } else {
            holder.ivReceipt.visibility = View.GONE
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size
}
