package com.example.suicahistory.data.repository

import android.content.Context
import android.nfc.Tag
import com.example.suicahistory.data.db.AppDatabase
import com.example.suicahistory.data.db.MonthlyCategoryTotal
import com.example.suicahistory.data.db.Transaction
import com.example.suicahistory.data.nfc.FeliCaReader
import com.example.suicahistory.data.web.SuicaCookieJar
import com.example.suicahistory.data.web.SuicaScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).transactionDao()
    private val cookieJar = SuicaCookieJar()
    private val scraper = SuicaScraper(cookieJar)

    fun getAllTransactions(): Flow<List<Transaction>> = dao.getAllFlow()

    fun getByDateRange(from: Long, to: Long): Flow<List<Transaction>> =
        dao.getByDateRange(from, to)

    fun getMonthlyCategoryTotals(): Flow<List<MonthlyCategoryTotal>> =
        dao.getMonthlyCategoryTotals()

    suspend fun importFromNfc(tag: Tag): Int = withContext(Dispatchers.IO) {
        val records = FeliCaReader.readHistory(tag)
        dao.insertAll(records)
        records.size
    }

    suspend fun loginWeb(email: String, password: String): Boolean =
        withContext(Dispatchers.IO) { scraper.login(email, password) }

    suspend fun syncFromWeb(): Int = withContext(Dispatchers.IO) {
        val records = scraper.fetchHistory()
        dao.insertAll(records)
        records.size
    }
}
