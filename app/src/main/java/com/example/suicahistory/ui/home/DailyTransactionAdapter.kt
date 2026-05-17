package com.example.suicahistory.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.suicahistory.data.db.Transaction
import com.example.suicahistory.databinding.ItemDayHeaderBinding
import com.example.suicahistory.databinding.ItemTransactionBinding

sealed class DailyItem {
    data class Header(val date: String, val totalSpent: Int) : DailyItem()
    data class Row(val transaction: Transaction) : DailyItem()
}

class DailyTransactionAdapter : ListAdapter<DailyItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
        private val DIFF = object : DiffUtil.ItemCallback<DailyItem>() {
            override fun areItemsTheSame(a: DailyItem, b: DailyItem) = a == b
            override fun areContentsTheSame(a: DailyItem, b: DailyItem) = a == b
        }
    }

    override fun getItemViewType(position: Int) =
        if (getItem(position) is DailyItem.Header) TYPE_HEADER else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER)
            HeaderVH(ItemDayHeaderBinding.inflate(inflater, parent, false))
        else
            RowVH(ItemTransactionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DailyItem.Header -> (holder as HeaderVH).bind(item)
            is DailyItem.Row -> (holder as RowVH).bind(item.transaction)
        }
    }

    class HeaderVH(private val b: ItemDayHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: DailyItem.Header) {
            b.dateText.text = item.date
            b.totalText.text = "合計 ¥${"%,d".format(item.totalSpent)}"
        }
    }

    class RowVH(private val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(tx: Transaction) {
            b.shopName.text = tx.shopName
            b.category.text = tx.category
            b.amount.text = if (tx.amount < 0) "¥${"%,d".format(-tx.amount)}" else "+¥${"%,d".format(tx.amount)}"
            b.balance.text = "残高 ¥${"%,d".format(tx.balance)}"
            b.amount.setTextColor(
                if (tx.amount < 0) b.root.context.getColor(android.R.color.holo_red_dark)
                else b.root.context.getColor(android.R.color.holo_green_dark)
            )
        }
    }
}
