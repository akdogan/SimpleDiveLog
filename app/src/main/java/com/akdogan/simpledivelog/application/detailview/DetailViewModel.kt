
package com.akdogan.simpledivelog.application.detailview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch


class DetailViewModel(
    application: Application,
    val diveLogId: String
) : AndroidViewModel(application) {

    private val _diveLogEntry = MutableLiveData<DiveLogEntry>()
    val diveLogEntry: LiveData<DiveLogEntry>
        get() = _diveLogEntry

    val apiError = Repository.apiError

    val repositoryApiStatus = Repository.downloadStatus

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
            val item = Repository.getSingleDive(diveLogId)
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
        Repository.onErrorDone()
    }


}

