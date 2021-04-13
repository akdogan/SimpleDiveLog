package com.akdogan.simpledivelog.application.ui.loginview

import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.repository.AuthRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository
import com.akdogan.simpledivelog.datalayer.repository.Result
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val prefsRepository: PreferencesRepository
) : ViewModel() {

    private var toggleIsSetToLogin = true

    private val _makeToast = MutableLiveData<Int>()
    val makeToast: LiveData<Int>
        get() = _makeToast

    private val _loginStatus = MutableLiveData(false)
    val loginStatus : LiveData<Boolean>
        get() = _loginStatus

    fun useLogin() = toggleIsSetToLogin

    fun toggleLogin() {
        toggleIsSetToLogin = !toggleIsSetToLogin
    }

    fun startRequest(
        username: String,
        pwd: String
    ) {

        viewModelScope.launch {
            val loginAttemptResponse = when (toggleIsSetToLogin) {
                true -> authRepository.login(username, pwd)
                false -> authRepository.register(username, pwd)
            }
            when (loginAttemptResponse){
                is Result.Success -> {
                    saveAuthCredentials(loginAttemptResponse.data)
                    _loginStatus.postValue(true)
                }
                is Result.Failure -> _makeToast.postValue(loginAttemptResponse.errorCode)
            }
        }

    }

    private fun saveAuthCredentials(token: String){
        prefsRepository.saveCredentials(token)
    }

    fun makeToastDone() {
        _makeToast.value = null
    }

    fun loginDone() {
        _loginStatus.value = false
    }
}

class LoginViewModelFactory(
    private val repo: AuthRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repo, prefsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}