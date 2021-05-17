package com.akdogan.simpledivelog.application.mainactivity

import android.util.Log
import androidx.lifecycle.*
import com.akdogan.simpledivelog.datalayer.LoginStatus
import com.akdogan.simpledivelog.datalayer.repository.AuthRepository
import com.akdogan.simpledivelog.datalayer.repository.DataRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_SUCCESS
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_UNVERIFIED
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val authRepository: AuthRepository,
    private val repository: DataRepository,
    private val prefsRepository: PreferencesRepository
) : ViewModel() {

    val networkAvailable = repository.networkAvailable
    val uploadStatus = repository.uploadApiStatus

    private var loginStatus = LoginStatus.UNVERIFIED

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin : LiveData<Boolean>
        get() = _navigateToLogin

    fun logout(){
        loginStatus = LoginStatus.FAILED
        viewModelScope.launch {
            prefsRepository.purgeCredentials()
            repository.cleanLogout()
            _navigateToLogin.postValue(true)
        }
    }

    fun onNavigateToLoginDone(){
        _navigateToLogin.value = null
    }

    fun onNetworkLost(){
        repository.onNetworkLost()
    }

    fun onNetworkAvailable(){
        repository.onNetworkAvailable()
        if (loginStatus == LoginStatus.UNVERIFIED) {
            viewModelScope.launch {
                prefsRepository.getCredentials()?.let{
                    when (authRepository.validateCredentials(it)){
                        LoginStatus.SUCCESS -> loginStatus = LoginStatus.SUCCESS
                        LoginStatus.UNVERIFIED -> loginStatus = LoginStatus.UNVERIFIED
                        LoginStatus.FAILED -> logout()
                    }

                    Log.d("LOGIN_STATUS", "on Network available called with revalidate and result: $loginStatus")
                } ?: logout()
            }
        }
    }

    fun setLoginStatus(loginVerified: Int) {
        when (loginVerified){
            LOGIN_SUCCESS -> loginStatus = LoginStatus.SUCCESS
            LOGIN_UNVERIFIED -> loginStatus = LoginStatus.UNVERIFIED
        }
    }


}


class MainActivityViewModelFactory(
    private val authRepo: AuthRepository,
    private val repo: DataRepository,
    private val prefsRepo: PreferencesRepository

) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel(authRepo, repo, prefsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}