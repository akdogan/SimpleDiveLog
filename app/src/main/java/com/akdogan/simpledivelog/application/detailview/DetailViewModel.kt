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

package com.akdogan.simpledivelog.application.detailview

import android.app.Application
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabaseDao
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch


class DetailViewModel(
    val database: DiveLogDatabaseDao,
    application: Application,
    val diveLogId: String
) : AndroidViewModel(application) {

    private val _diveLogEntry = MutableLiveData<DiveLogEntry>()
    val diveLogEntry: LiveData<DiveLogEntry>
        get() = _diveLogEntry

    val apiError = Repository.apiError

    val repositoryApiStatus = Repository.repositoryApiStatus

    private val _makeToast = MutableLiveData<String>()
    val makeToast: LiveData<String>
        get() = _makeToast

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean>
        get() = _navigateBack

    init {
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
                onMakeToast("Element found: #${item.diveNumber}")
            }
        }
    }

    private fun onMakeToast(message: String) {
        _makeToast.value = message
    }

    fun onMakeToastDone() {
        _makeToast.value = null
    }

    fun onNavigateBack() {
        _navigateBack.value = true
    }

    fun onNavigateBackDone() {
        _navigateBack.value = false
    }

    fun onErrorDone() {
        Repository.onErrorDone()
    }


}

