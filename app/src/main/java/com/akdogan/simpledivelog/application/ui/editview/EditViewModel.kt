package com.akdogan.simpledivelog.application.ui.editview

import android.app.Application
import android.icu.text.DateFormat
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.akdogan.simpledivelog.application.CleanupCacheWorker
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_UNAUTHORIZED
import com.akdogan.simpledivelog.datalayer.Result
import com.akdogan.simpledivelog.datalayer.repository.DataRepository

import com.akdogan.simpledivelog.diveutil.Constants
import com.akdogan.simpledivelog.diveutil.UnitConverter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// TODO Remove Application access and use preferences repo instead
class EditViewModelFactory(
    private val application: Application,
    private val repo: DataRepository,
    private val entryId: String?

) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            return EditViewModel(application, repo, entryId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class EditViewModel(
    application: Application,
    val repository: DataRepository,
    diveLogId: String?
) : AndroidViewModel(application) {

    var contentUri: Uri? = null
        set(value) {
            if (value != null) {
                imgUrl = null
                remoteImgUrl = null
            }
            field = value
        }

    private var imgUrl: String? = null

    var remoteImgUrl: String? = null
        private set

    private val _loadRemotePicture = MutableLiveData<Boolean>()
    val loadRemotePicture: LiveData<Boolean>
        get() = _loadRemotePicture

    var networkAvailable = repository.networkAvailable


    private val createNewEntry: Boolean = diveLogId == null

    private var entry: DiveLogEntry? = null

    //val apiError = repository.apiError

    val downloadStatus = repository.downloadStatus

    val uploadStatus = repository.uploadApiStatus

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    private val _savingInProgress = MutableLiveData<Boolean>()
    val savingInProgress: LiveData<Boolean>
        get() = _savingInProgress

    private val _makeToast = MutableLiveData<Int>()
    val makeToast: LiveData<Int>
        get() = _makeToast

    private val _unauthorizedAccess = MutableLiveData<Boolean>()
    val unauthorizedAccess: LiveData<Boolean>
        get() = _unauthorizedAccess

    val weightInput = MutableLiveData<String>()
    val airInInput = MutableLiveData<String>()
    val airOutInput = MutableLiveData<String>()
    val notesInput = MutableLiveData<String>()


    // Todo Check if the transformations are actually required at all
    // Todo Alternatively have one DiveLogEntry LiveData Object, then we dont need to extract and then put together the body again. But all fields would need to be vars
    // Todo maybe make intermediate class. Could also use the copy function
    // When using livedata divelogentry object, maybe use single values with set to expose the write interface for only those values
    val diveNumberInput = MutableLiveData<String>()
    private val diveNumber = Transformations.map(diveNumberInput) { it?.toIntOrNull() }

    private val _liveDate = MutableLiveData<Long>()
    val liveDate: LiveData<Long>
        get() = _liveDate
    val liveDateString = Transformations.map(_liveDate) { DateFormat.getDateInstance().format(it) }

    val diveDurationInput = MutableLiveData<String>()
    private val diveDuration = Transformations.map(diveDurationInput) { it?.toIntOrNull() }

    val maxDepthInput = MutableLiveData<String>()
    private val maxDepth = Transformations.map(maxDepthInput) { it?.toIntOrNull() }

    val locationInput = MutableLiveData<String>()

    private var converter: UnitConverter


    val enableSaveButton: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(diveNumber) { value = checkEnableSaveButton() }
        addSource(liveDate) { value = checkEnableSaveButton() }
        addSource(diveDuration) { value = checkEnableSaveButton() }
        addSource(maxDepth) { value = checkEnableSaveButton() }
        addSource(locationInput) { value = checkEnableSaveButton() }
        addSource(savingInProgress) { value = checkEnableSaveButton() }
        addSource(networkAvailable) { value = checkEnableSaveButton() }
    }

    init {

        val prefs = PreferenceManager.getDefaultSharedPreferences(getApplication())

        val convertPressure =
            prefs.getBoolean(Constants.PREF_PRESSURE_UNIT_KEY, Constants.PREF_PRESSURE_UNIT_DEAFULT)
        val convertDepth =
            prefs.getBoolean(Constants.PREF_DEPTH_UNIT_KEY, Constants.PREF_DEPTH_UNIT_DEFAULT)
        converter = UnitConverter(convertDepth, convertPressure)
        if (createNewEntry) {
            diveNumberInput.value = (repository.getLatestDiveNumber() + 1).toString()
        } else {
            fetchEntry(diveLogId)
        }
    }


    private fun fetchEntry(entryId: String?) {
        viewModelScope.launch {
            if (entryId != null) {
                val result = repository.getSingleDive(entryId)
                if (result is Result.Failure) {
                    onMakeToast(result.errorCode)
                    when (result.errorCode) {
                        GENERAL_UNAUTHORIZED -> _unauthorizedAccess.postValue(true)
                        else -> onNavigateBack()
                    }
                } else {
                    entry = (result as Result.Success).body
                    extractData()
                }
            }
        }
    }

    private fun extractData() {
        entry?.let {
            diveNumberInput.value = it.diveNumber.toString()
            diveDurationInput.value = it.diveDuration.toStringOrNull()
            maxDepthInput.value = converter.depthToDisplay(it.maxDepth).toStringOrNull()
            locationInput.value = it.diveLocation
            _liveDate.value = it.diveDate
            weightInput.value = it.weight.toStringOrNull()
            airInInput.value = converter.pressureToDisplay(it.airIn).toStringOrNull()
            airOutInput.value = converter.pressureToDisplay(it.airOut).toStringOrNull()
            notesInput.value = it.notes
            imgUrl = it.imgUrl // ???
            remoteImgUrl = it.imgUrl
        }

        if (remoteImgUrl != null) {
            _loadRemotePicture.value = true
        }
    }


    private fun checkEnableSaveButton(): Boolean {
        return diveNumber.value != null &&
                liveDate.value != null &&
                diveDuration.value != null &&
                maxDepth.value != null &&
                !locationInput.value.isNullOrBlank() &&
                _savingInProgress.value != true &&
                networkAvailable.value == true
    }

    fun onSaveButtonPressed() {
        if (checkEnableSaveButton()) {
            _savingInProgress.value = true
            startUploadCoroutine()
        }
    }

    fun uploadDone() {
        onNavigateBack()
        _savingInProgress.value = false
    }

    private fun startUploadCoroutine() {
        val uri = contentUri
        val newEntry = createEntry()
        // Trigger the coroutine upload in the repository
        viewModelScope.launch {
            // Take the result and react to it
            // ViewModelScope.launch should maybe wrap the whole function.
            // Also not sure if we need try / catch anymore
            val result = repository.startUpload(
                newEntry,
                createNewEntry,
                uri
            )
            Log.i("UPLOAD_TRACING", "Upload result is: $result")
            if (result is Result.Failure) {
                Log.i("UPLOAD_TRACING", "Upload Failure code: ${result.errorCode}")
                _makeToast.postValue(result.errorCode)
                if (result.errorCode == GENERAL_UNAUTHORIZED){
                    _unauthorizedAccess.postValue(true)
                }
            } else {
                // If there was a file to be uploaded, post a worker to clean the cache
                uri?.let {
                    setupWorker(it)
                }
            }
            uploadDone()
        }
    }

    fun setupWorker(localUri: Uri) {
        val filename = localUri.lastPathSegment
        val oneTimeRequest = OneTimeWorkRequestBuilder<CleanupCacheWorker>()
            .setInputData(workDataOf(Constants.CACHE_CLEANUP_WORKER_FILENAME_KEY to filename))
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(getApplication()).enqueue(oneTimeRequest)
    }


    // TODO: Unit conversion from the settings needs to be completely redone
    @Throws(IllegalArgumentException::class)
    private fun createEntry(): DiveLogEntry {
        val result = DiveLogEntry(
            entry?.dataBaseId ?: "",
            requireNotNull(diveNumber.value),
            requireNotNull(diveDuration.value),
            converter.depthToData(requireNotNull(maxDepth.value)),
            requireNotNull(locationInput.value),
            requireNotNull(liveDate.value),
            weightInput.value?.toIntOrNull(),
            converter.pressureToData(airInInput.value?.toIntOrNull()),
            converter.pressureToData(airOutInput.value?.toIntOrNull()),
            notesInput.value,
            imgUrl
        )
        //onMakeToast("New Entry Created")
        return result
    }

    private fun onNavigateBack() {
        _navigateBack.value = true
    }

    fun onNavigateBackFinished() {
        _navigateBack.value = null
    }

    fun updateDate(timeInMillis: Long) {
        _liveDate.value = timeInMillis
    }

    private fun onMakeToast(code: Int) {
        _makeToast.postValue(code)
    }

    fun onMakeToastFinished() {
        _makeToast.value = null
    }



}

fun Int?.toStringOrNull(): String? {
    return this?.toString()
}