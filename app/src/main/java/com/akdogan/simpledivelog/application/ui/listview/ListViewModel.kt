package com.akdogan.simpledivelog.application.ui.listview

import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.repository.Repository
import com.akdogan.simpledivelog.diveutil.getSampleData
import kotlinx.coroutines.launch


class ListViewModel(
    val repository: Repository,
) : ViewModel() {

    val apiError = repository.apiError

    val repositoryApiStatus = repository.downloadStatus

    val listOfLogEntries = repository.listOfDives

    private val _navigateToNewEntry = MutableLiveData<Boolean>()
    val navigateToNewEntry: LiveData<Boolean>
        get() = _navigateToNewEntry

    private val _navigateToDetailView = MutableLiveData<String>()
    val navigateToDetailView: LiveData<String>
        get() = _navigateToDetailView

    private val _makeToast = MutableLiveData<String>()
    val makeToast: LiveData<String>
        get() = _makeToast

    init {
        viewModelScope.launch {
            repository.forceUpdate()
        }
    }


    fun onErrorDone() = repository.onErrorDone()

    fun onRefresh() = updateList()

    private fun updateList() = viewModelScope.launch { repository.forceUpdate() }

    fun deleteRemoteItem(diveId: String) =
        viewModelScope.launch { repository.deleteDive(diveId) }

    fun deleteAllRemote() =
        viewModelScope.launch { repository.deleteAll() }


    fun createDummyData() {
        viewModelScope.launch {
            val latestDiveNumber = repository.getLatestDiveNumber()
            val list = getSampleData(1, latestDiveNumber)
            list.forEach {
                repository.startUpload(it, true)
            }
            onMakeToast("Sample Data Created")
        }
    }


    fun onListItemClicked(id: String) {
        _navigateToDetailView.value = id
    }

    fun onNavigateToDetailViewFinished() {
        _navigateToDetailView.value = null
    }

    private fun onMakeToast(message: String) {
        _makeToast.value = message
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


