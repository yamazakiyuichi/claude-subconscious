package com.example.suicahistory

import android.app.Application
import com.example.suicahistory.data.repository.TransactionRepository

class SuicaApp : Application() {
    // アプリ全体で唯一のRepositoryインスタンス（Cookieセッションを共有）
    val repository: TransactionRepository by lazy { TransactionRepository(this) }
}
