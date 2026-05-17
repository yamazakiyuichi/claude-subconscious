package com.example.suicahistory.ui

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.*
import com.example.suicahistory.SuicaApp
import com.example.suicahistory.data.CredentialStore
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    // アプリ全体のシングルトンRepositoryを使用
    val repository = (app as SuicaApp).repository

    private val _nfcResult = MutableLiveData<Int>()
    val nfcResult: LiveData<Int> = _nfcResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _webSyncResult = MutableLiveData<Int>()
    val webSyncResult: LiveData<Int> = _webSyncResult

    private val _syncInProgress = MutableLiveData(false)
    val syncInProgress: LiveData<Boolean> = _syncInProgress

    fun importFromNfc(tag: Tag) = viewModelScope.launch {
        runCatching { repository.importFromNfc(tag) }
            .onSuccess { _nfcResult.value = it }
            .onFailure { _error.value = "NFC読み取りエラー: ${it.message}" }
    }

    fun syncFromWeb() = viewModelScope.launch {
        val creds = CredentialStore.load(app)
        if (creds == null) {
            _error.value = "先に設定からログインしてください"
            return@launch
        }

        _syncInProgress.value = true
        runCatching {
            // 毎回ログインしてセッションを確保してからフェッチ
            val loggedIn = repository.loginWeb(creds.first, creds.second)
            if (!loggedIn) throw Exception("ログインに失敗しました。認証情報を確認してください")
            repository.syncFromWeb()
        }
            .onSuccess { count -> _webSyncResult.value = count }
            .onFailure { e -> _error.value = "Web同期エラー: ${e.message}" }
        _syncInProgress.value = false
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            MainViewModel(app) as T
    }
}
