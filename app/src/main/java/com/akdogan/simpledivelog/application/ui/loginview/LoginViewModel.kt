package com.akdogan.simpledivelog.application.ui.loginview

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch

class LoginViewModel(
    application: Application,
    val repository: Repository
) : AndroidViewModel(application) {

    private var toggleIsSetToLogin = true

    private val _makeToast = MutableLiveData<String>()
    val makeToast: LiveData<String>
        get() = _makeToast

    val loginStatus = repository.loginStatus



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
                true -> repository.login(username, pwd)
                false -> repository.register(username, pwd)
            }
            if (loginAttemptResponse){
                saveAuthCredentials(username, pwd)
            }
        }

    }

    private fun saveAuthCredentials(username: String, pwd: String){
        // TODO Well save the auth credentials ;)
        Log.d("LOGIN_VIEW_MODEL", "Saving the credentials was called")
    }

    fun makeToastDone() {
        _makeToast.value = null
    }
}

class LoginViewModelFactory(
    private val application: Application,
    private val repo: Repository

) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}