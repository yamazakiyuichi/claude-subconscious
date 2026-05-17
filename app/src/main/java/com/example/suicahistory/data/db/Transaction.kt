package com.example.suicahistory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,           // Unix timestamp (ms)
    val shopName: String,     // 店舗名 or 路線名
    val amount: Int,          // 金額（負=支払い、正=チャージ）
    val balance: Int,         // 残高
    val category: String,     // 分類
    val source: String        // "nfc" or "web"
)
