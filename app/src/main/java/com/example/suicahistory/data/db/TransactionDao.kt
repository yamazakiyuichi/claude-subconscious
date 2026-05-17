package com.example.suicahistory.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllFlow(): Flow<List<Transaction>>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :from AND date < :to
        ORDER BY date DESC
    """)
    fun getByDateRange(from: Long, to: Long): Flow<List<Transaction>>

    @Query("""
        SELECT strftime('%Y-%m', date/1000, 'unixepoch', 'localtime') AS month,
               category,
               SUM(CASE WHEN amount < 0 THEN -amount ELSE 0 END) AS total
        FROM transactions
        GROUP BY month, category
        ORDER BY month DESC
    """)
    fun getMonthlyCategoryTotals(): Flow<List<MonthlyCategoryTotal>>

    @Query("SELECT MAX(date) FROM transactions WHERE source = :source")
    suspend fun getLatestDate(source: String): Long?

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

data class MonthlyCategoryTotal(
    val month: String,
    val category: String,
    val total: Int
)
