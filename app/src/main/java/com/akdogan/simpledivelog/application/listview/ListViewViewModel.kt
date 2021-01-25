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

package com.akdogan.simpledivelog.application.listview

import android.app.Application
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabaseDao
import com.akdogan.simpledivelog.datalayer.repository.Repository
import com.akdogan.simpledivelog.diveutil.getSampleData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

const val TAG = "ListViewViewModel"
// TODO W/View: requestLayout() improperly called by StatusBarIconView(slot=mobile icon=null notification=null) during layout: running second layout pass
/**
 * ViewModel for SleepTrackerFragment.
 */
class ListViewViewModel(
    val database: DiveLogDatabaseDao,
    application: Application
) : AndroidViewModel(application) {

    private val errorsFlow = MutableStateFlow<Exception?>(null)
    val errors: Flow<Exception> = errorsFlow.filterNotNull()

    val apiError = Repository.apiError

    val repositoryApiStatus = Repository.repositoryApiStatus

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

    // Todo ?? Über Festivals reden nicht vergessen
    fun createDummyData() {
        viewModelScope.launch {
            val latestDiveNumber = Repository.getLatestDiveNumber()
            val list = getSampleData(1, latestDiveNumber)
            list.forEach {
                Repository.uploadSingleDive(it)
            }
            onMakeToast("Sample Data Created")

            errorsFlow.value = Exception("erag")
        }
    }


    fun onListItemClicked(id: String) {
        _navigateToDetailView.value = id
    }

    fun onNavigateToDetailViewFinished() {
        _navigateToDetailView.value = null
    }

    fun onMakeToast(message: String) {
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



