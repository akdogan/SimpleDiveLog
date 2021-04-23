package com.akdogan.simpledivelog.application.ui.listview

import android.util.Log
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_UNAUTHORIZED
import com.akdogan.simpledivelog.datalayer.Result
import com.akdogan.simpledivelog.datalayer.repository.Repository
import com.akdogan.simpledivelog.diveutil.getSampleData
import kotlinx.coroutines.launch


class ListViewModel(
    val repository: Repository,
) : ViewModel() {

    //val apiError = repository.apiError

    private val _unauthorizedAccess = MutableLiveData<Boolean>()
    val unauthorizedAccess: LiveData<Boolean>
        get() = _unauthorizedAccess

    val repositoryApiStatus = repository.downloadStatus

    val listOfLogEntries = repository.listOfDives

    private val _navigateToNewEntry = MutableLiveData<Boolean>()
    val navigateToNewEntry: LiveData<Boolean>
        get() = _navigateToNewEntry

    private val _navigateToDetailView = MutableLiveData<String>()
    val navigateToDetailView: LiveData<String>
        get() = _navigateToDetailView

    private val _makeToast = MutableLiveData<Int>()
    val makeToast: LiveData<Int>
        get() = _makeToast

    init {
        viewModelScope.launch {
            repository.forceUpdate()
        }
    }


    //fun onErrorDone() = repository.onErrorDone()

    private suspend fun <T> safeCall(
        callFunction: suspend () -> Result<T>
    ): Result<T> {
        val result = callFunction.invoke()
        if (result is Result.Failure) {
            Log.i("REPO_V2", "Result failure called in Viewmodel with $result and ${result.errorCode}")
            onMakeToast(result.errorCode)
            if (result.errorCode == GENERAL_UNAUTHORIZED) {
                _unauthorizedAccess.postValue(true)
            }
        }
        return result
    }

    fun onRefresh() = updateList()

    private fun updateList() = viewModelScope.launch{
        safeCall(repository::forceUpdate)
    }

    fun deleteRemoteItem(diveId: String) = viewModelScope.launch{
        safeCall{
            repository.deleteDive(diveId)
        }
    }

    fun deleteAllRemote() = viewModelScope.launch {
        safeCall(repository::deleteAll)
    }


    fun createDummyData() {
        viewModelScope.launch {
            val latestDiveNumber = repository.getLatestDiveNumber()
            val list = getSampleData(1, latestDiveNumber)
            list.forEach {
                safeCall {
                    repository.startUpload(it, true)
                }
            }
        }
    }


    fun onListItemClicked(id: String) {
        _navigateToDetailView.value = id
    }

    fun onNavigateToDetailViewFinished() {
        _navigateToDetailView.value = null
    }

    private fun onMakeToast(code: Int) {
        _makeToast.postValue(code)
    }

    fun onMakeToastDone() {
        _makeToast.value = null
    }

    fun onCreateNewEntry() {
        _navigateToNewEntry.value = true
    }

    fun onNavigateToNewEntryDone() {
        _navigateToNewEntry.value = null
    }


}


class ListViewModelFactory(
    private val repo: Repository
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            return ListViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


