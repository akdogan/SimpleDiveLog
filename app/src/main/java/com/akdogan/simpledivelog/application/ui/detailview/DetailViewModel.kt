package com.akdogan.simpledivelog.application.ui.detailview

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.akdogan.simpledivelog.application.ui.pictureview.PictureFragmentViewModel
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_UNAUTHORIZED
import com.akdogan.simpledivelog.datalayer.Result
import com.akdogan.simpledivelog.datalayer.repository.DataRepository
import kotlinx.coroutines.launch

class DetailViewModel(
    val repository: DataRepository,
    val diveLogId: String
) : PictureFragmentViewModel() {

    private val _unauthorizedAccess = MutableLiveData<Boolean>()
    val unauthorizedAccess: LiveData<Boolean>
        get() = _unauthorizedAccess

    private val _diveLogEntry = MutableLiveData<DiveLogEntry>()
    val diveLogEntry: LiveData<DiveLogEntry>
        get() = _diveLogEntry

    val repositoryApiStatus = repository.downloadStatus

    private val _makeToast = MutableLiveData<Int>()
    val makeToast: LiveData<Int>
        get() = _makeToast

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    private val _loadRemotePicture = MutableLiveData<Boolean>()
    override val loadRemotePicture: LiveData<Boolean>
        get() = _loadRemotePicture

    override var remoteImgUrl: String? = null
        private set

    override val readOnlyMode: Boolean = true

    // contentUri only satisfies the PictureFragmentViewModel, its not actively used
    override var contentUri: Uri? = null

    init {
        fetchDiveLogEntry()
    }


    // Fragment can call refresh when it was navigated back to (from edit view)
    fun refresh() {
        Log.i("NAVIGATION TEST", "refresh called")
        fetchDiveLogEntry()
    }

    private fun fetchDiveLogEntry() {
        viewModelScope.launch {
            val result = repository.getSingleDive(diveLogId)
            if (result is Result.Failure) {
                onMakeToast(result.errorCode)
                when (result.errorCode) {
                    GENERAL_UNAUTHORIZED -> _unauthorizedAccess.postValue(true)
                    else -> onNavigateBack()
                }
            } else {
                val entry = (result as Result.Success).body
                _diveLogEntry.postValue(entry)
                remoteImgUrl = entry.imgUrl
                _loadRemotePicture.postValue(true)
            }
        }
    }

    private fun onMakeToast(code: Int) {
        _makeToast.postValue(code)
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




}

class DetailViewModelFactory(
    private val repo: DataRepository,
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
