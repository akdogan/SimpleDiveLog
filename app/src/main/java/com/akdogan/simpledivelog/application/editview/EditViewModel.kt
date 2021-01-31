/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akdogan.simpledivelog.application.editview

import android.app.Application
import android.icu.text.DateFormat
import android.net.Uri
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch

class EditViewModelFactory(
    private val application: Application,
    private val entryId: String?

) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            return EditViewModel(application, entryId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class EditViewModel(
    application: Application,
    entryId: String?
    // TODO Bleibt hängen wenn keine Internetverbindung
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

    var networkAvailable = Repository.networkAvailable


    private val createNewEntry: Boolean = entryId == null

    private var entry: DiveLogEntry? = null

    val apiError = Repository.apiError

    val downloadStatus = Repository.downloadStatus

    val uploadStatus = Repository.uploadApiStatus

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    private val _savingInProgress = MutableLiveData<Boolean>()
    val savingInProgress: LiveData<Boolean>
        get() = _savingInProgress

    private val _makeToast = MutableLiveData<String>()
    val makeToast: LiveData<String>
        get() = _makeToast

    val weightInput = MutableLiveData<String>()
    val airInInput = MutableLiveData<String>()
    val airOutInput = MutableLiveData<String>()
    val notesInput = MutableLiveData<String>()


    // Todo Check if the transformations are actually required at all
    // Todo Alternatively have one DiveLogEntry LiveData Object, then we dont need to extract and then put together the data again. But all fields would need to be vars
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
        if (createNewEntry) {
            diveNumberInput.value = (Repository.getLatestDiveNumber() + 1).toString()
        } else {
            fetchEntry(entryId)
        }
    }


    private fun fetchEntry(entryId: String?) {
        viewModelScope.launch {
            if (entryId != null) {
                entry = Repository.getSingleDive(entryId)
                if (entry != null) {
                    extractData()
                    onMakeToast("Element found: #${entry?.diveNumber}")
                } else {
                    onMakeToast("Error: No Element found")
                    onNavigateBack()
                }
            }
        }
    }

    private fun extractData() {
        entry?.let {
            diveNumberInput.value = it.diveNumber.toString()
            diveDurationInput.value = it.diveDuration.toStringOrNull()
            maxDepthInput.value = it.maxDepth.toStringOrNull()
            locationInput.value = it.diveLocation
            _liveDate.value = it.diveDate
            weightInput.value = it.weight.toStringOrNull()
            airInInput.value = it.airIn.toStringOrNull()
            airOutInput.value = it.airOut.toStringOrNull()
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
            //startImageUpload() // ORIGINAL
            startUploadCoroutine() // COROUTINE REFACTOR
        }
    }

    fun uploadDone(){
        onNavigateBack()
        _savingInProgress.value = false
    }

    // COROUTINE REFACTOR
    private fun startUploadCoroutine() {
        val uri = contentUri
        try {
            val newEntry = createEntry()
            // Trigger the coroutine upload in the repository
            viewModelScope.launch {
                Repository.startUpload(
                    newEntry,
                    createNewEntry,
                    uri
                )
                uploadDone()
            }

        } catch (e: java.lang.IllegalArgumentException) {
            onMakeToast("Try create entry catch block called with $e")
            uploadDone()
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun createEntry(): DiveLogEntry {
        //val test: Int? = null // TODO remove after testing
        val result = DiveLogEntry(
            entry?.dataBaseId ?: "",
            requireNotNull(diveNumber.value),
            requireNotNull(diveDuration.value),
            requireNotNull(maxDepth.value),
            requireNotNull(locationInput.value),
            requireNotNull(liveDate.value),
            weightInput.value?.toIntOrNull(),
            airInInput.value?.toIntOrNull(),
            airOutInput.value?.toIntOrNull(),
            notesInput.value,
            imgUrl
        )
        onMakeToast("New Entry Created")
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

    private fun onMakeToast(message: String) {
        _makeToast.value = message
    }

    fun onMakeToastFinished() {
        _makeToast.value = null
    }

    fun onErrorDone() {
        Repository.onErrorDone()
    }


}

fun Int?.toStringOrNull(): String? {
    return this?.toString()
}