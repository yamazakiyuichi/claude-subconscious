package com.example.suicahistory.ui

import android.content.Context
import android.nfc.Tag
import androidx.lifecycle.*
import com.example.suicahistory.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class MainViewModel(context: Context) : ViewModel() {

    val repository = TransactionRepository(context)

    private val _nfcResult = MutableLiveData<Int>()
    val nfcResult: LiveData<Int> = _nfcResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _webSyncResult = MutableLiveData<Int>()
    val webSyncResult: LiveData<Int> = _webSyncResult

    fun importFromNfc(tag: Tag) = viewModelScope.launch {
        runCatching { repository.importFromNfc(tag) }
            .onSuccess { _nfcResult.value = it }
            .onFailure { _error.value = "NFC読み取りエラー: ${it.message}" }
    }

    fun syncFromWeb() = viewModelScope.launch {
        runCatching { repository.syncFromWeb() }
            .onSuccess { _webSyncResult.value = it }
            .onFailure { _error.value = "Web同期エラー: ${it.message}" }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            MainViewModel(context.applicationContext) as T
    }
}
