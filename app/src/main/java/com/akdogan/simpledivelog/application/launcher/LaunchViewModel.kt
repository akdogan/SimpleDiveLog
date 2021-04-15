package com.akdogan.simpledivelog.application.launcher

import android.util.Log
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.LoginStatus
import com.akdogan.simpledivelog.datalayer.repository.AuthRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LaunchViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: PreferencesRepository
) : ViewModel() {

    private val _userIsLoggedIn = MutableLiveData<LoginStatus>(null)
    val userIsLoggedIn: LiveData<LoginStatus>
        get() = _userIsLoggedIn

    fun validateLogin() {
        val token = prefsRepository.getCredentials()
        Log.i("LAUNCH_ACTIVITY_TRACING", "validate login: token retrieved $token")
        viewModelScope.launch {
            delay(3000)
            token?.let {
                val result = authRepository.validateCredentials(token)
                Log.i("LAUNCH_VIEWMODEL", "validate with result: $result")
                if (result == LoginStatus.FAILED) {
                    purgeCredentials()
                }
                _userIsLoggedIn.postValue(result)
            } ?: _userIsLoggedIn.postValue(LoginStatus.FAILED)
        }
    }


    private fun purgeCredentials() {
        prefsRepository.purgeCredentials()
    }

    fun userIsLoggedInDone() {
        _userIsLoggedIn.value = null
    }
}


class LaunchViewModelFactory(
    private val authRepo: AuthRepository,
    private val prefsRepo: PreferencesRepository

) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LaunchViewModel::class.java)) {
            return LaunchViewModel(authRepo, prefsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}