
package com.akdogan.simpledivelog.application.ui.detailview

import android.util.Log
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch

// TODO Refactor all Viewmodels to standard Viewmodel if app is not needed
class DetailViewModel(
    val repository: Repository,
    val diveLogId: String
) : ViewModel() {

    private val _diveLogEntry = MutableLiveData<DiveLogEntry>()
    val diveLogEntry: LiveData<DiveLogEntry>
        get() = _diveLogEntry

    val apiError = repository.apiError

    val repositoryApiStatus = repository.downloadStatus

    private val _makeToast = MutableLiveData<String>()
    val makeToast: LiveData<String>
        get() = _makeToast

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    init {
        fetchDiveLogEntry()
    }

    // Fragment can call refresh when it was navigated back to (from edit view)
    fun refresh(){
        Log.i("NAVIGATION TEST", "refresh called")
        fetchDiveLogEntry()
    }

    private fun fetchDiveLogEntry() {
        viewModelScope.launch {
            val item = repository.getSingleDive(diveLogId)
            if (item == null) {
                onMakeToast("Error: No Element found")
                onNavigateBack()
            } else {
                _diveLogEntry.value = item
            }
        }
    }

    private fun onMakeToast(message: String) {
        _makeToast.value = message
    }

    fun onMakeToastDone() {
        _makeToast.value = null
    }

    private fun onNavigateBack() {
        _navigateBack.value = true
    }

    fun onNavigateBackDone() {
        _navigateBack.value = false
    }

    fun onErrorDone() {
        repository.onErrorDone()
    }


}

class DetailViewModelFactory(
    private val repo: Repository,
    private val diveLogId: String
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            return DetailViewModel(repo, diveLogId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
