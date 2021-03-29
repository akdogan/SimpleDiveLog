
package com.akdogan.simpledivelog.application.listview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.akdogan.simpledivelog.datalayer.repository.Repository
import com.akdogan.simpledivelog.diveutil.getSampleData
import kotlinx.coroutines.launch


class ListViewViewModel(
    application: Application
) : AndroidViewModel(application) {

    val apiError = Repository.apiError

    val repositoryApiStatus = Repository.downloadStatus

    val listOfLogEntries = Repository.listOfDives

    private val _navigateToNewEntry = MutableLiveData<Boolean>()
    val navigateToNewEntry: LiveData<Boolean>
        get() = _navigateToNewEntry

    private val _navigateToDetailView = MutableLiveData<String>()
    val navigateToDetailView: LiveData<String>
        get() = _navigateToDetailView

    private val _makeToast = MutableLiveData<String>()
    val makeToast: LiveData<String>
        get() = _makeToast



    fun onErrorDone() = Repository.onErrorDone()

    fun onRefresh() = updateList()

    private fun updateList() = viewModelScope.launch { Repository.forceUpdate() }

    fun deleteRemoteItem(diveId: String) =
        viewModelScope.launch { Repository.deleteDive(diveId) }

    fun deleteAllRemote() =
        viewModelScope.launch { Repository.deleteAll() }


    fun createDummyData() {
        viewModelScope.launch {
            val latestDiveNumber = Repository.getLatestDiveNumber()
            val list = getSampleData(1, latestDiveNumber)
            list.forEach {
                Repository.startUpload(it, true)
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



