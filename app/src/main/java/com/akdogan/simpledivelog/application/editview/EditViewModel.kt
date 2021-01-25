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
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabaseDao
import com.akdogan.simpledivelog.diveutil.ActWithString
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch
import java.lang.Exception



class EditViewModel(
    val database: DiveLogDatabaseDao,
    application: Application,
    entryId: String?
    // TODO Bleibt hängen wenn keine Internetverbindung
) : AndroidViewModel(application) {
    private val createNewEntry: Boolean = entryId == null

    private var entry: DiveLogEntry? = null

    val apiError = Repository.apiError

    val repositoryApiStatus = Repository.repositoryApiStatus

    private val _navigateBack = MutableLiveData<ActWithString>()
    val navigateBack: LiveData<ActWithString>
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
                    onNavigateBack(entryId)
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
        }
    }

    private fun checkEnableSaveButton(): Boolean {
        return diveNumber.value != null &&
                liveDate.value != null &&
                diveDuration.value != null &&
                maxDepth.value != null &&
                !locationInput.value.isNullOrBlank() &&
                _savingInProgress.value != true
    }

    private fun putEntry(newEntry: DiveLogEntry) {
        var navigateBackWithString: String? = null
        viewModelScope.launch {

            if (createNewEntry) {
                Repository.uploadSingleDive(newEntry)
            } else {
                Repository.updateSingleDive(newEntry)
                navigateBackWithString = newEntry.dataBaseId
            }

        }
        onNavigateBack(navigateBackWithString)
    }

    fun onSaveButtonPressed() {
        if (checkEnableSaveButton()) {
            _savingInProgress.value = true
            try {
                var newEntry = createEntry()
                putEntry(newEntry)
            } catch (e: IllegalArgumentException) {
                onMakeToast("Try create entry catch block called with $e")
            }
            _savingInProgress.value = false
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
            notesInput.value
        )
        onMakeToast("New Entry Created")
        return result
    }

    private fun onNavigateBack(diveId: String?) {
        _navigateBack.value = ActWithString(true, diveId)
    }

    fun onNavigateBackFinished() {
        _navigateBack.value = null
    }

    fun updateDate(timeInMillis: Long) {
        _liveDate.value = timeInMillis
    }

    fun onMakeToast(message: String) {
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
    return if (this == null) {
        null
    } else {
        this.toString()
    }
}