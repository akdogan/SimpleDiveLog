
package com.akdogan.simpledivelog.application.listview

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * This is pretty much boiler plate code for a ViewModel Factory.
 *
 * Provides the SleepDatabaseDao and context to the ViewModel.
 */
class ListViewViewModelFactory(
        private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewViewModel::class.java)) {
            return ListViewViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

