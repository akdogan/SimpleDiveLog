package com.akdogan.simpledivelog.application.ui.loginview

import androidx.lifecycle.*
import com.akdogan.simpledivelog.application.ui.loginview.TextInputErrorCases.*
import com.akdogan.simpledivelog.datalayer.Result
import com.akdogan.simpledivelog.datalayer.repository.AuthRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository
import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_MIN_LENGTH
import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_PATTERN
import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_VALID_CHARS_PATTERN
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_MIN_LENGTH
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_PATTERN
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_VALID_CHARS_PATTERN
import com.akdogan.simpledivelog.diveutil.matchPattern
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
    val loginStatus: LiveData<Boolean>
        get() = _loginStatus

    val username = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val passwordRepeat = MutableLiveData<String>()


    val usernameState = Transformations.map(username) {
        checkInputMediator(
            input = it,
            fullPattern = USERNAME_PATTERN,
            allowedCharsPattern = USERNAME_VALID_CHARS_PATTERN,
            minLength = USERNAME_MIN_LENGTH
        )
    }

    val passwordState = Transformations.map(password) {
        checkInputMediator(
            input = it,
            fullPattern = PASSWORD_PATTERN,
            allowedCharsPattern = PASSWORD_VALID_CHARS_PATTERN,
            minLength = PASSWORD_MIN_LENGTH
        )
    }

    val passwordRepeatState = Transformations.map(passwordRepeat) {
        checkInputMediator(
            input = it,
            fullPattern = PASSWORD_PATTERN,
            allowedCharsPattern = PASSWORD_VALID_CHARS_PATTERN,
            minLength = PASSWORD_MIN_LENGTH,
            twin = password.value
        )
    }

    val enableActionButton: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(usernameState) { value = checkEnableActionButton() }
        addSource(passwordState) { value = checkEnableActionButton() }
        addSource(passwordRepeatState) { value = checkEnableActionButton() }
    }

    private fun checkEnableActionButton(): Boolean {
        return usernameState.value == NoError &&
                passwordState.value == NoError &&
                (toggleIsSetToLogin || passwordRepeatState.value == NoError)
    }

    private fun checkInputMediator(
        input: String?,
        fullPattern: String,
        allowedCharsPattern: String,
        minLength: Int,
        twin: String? = input
    ): TextInputErrorCases {
        return when {
            input.isNullOrEmpty() -> NoError
            input != twin -> DoesNotMatch
            matchPattern(input, fullPattern) -> NoError
            !matchPattern(input, allowedCharsPattern) -> InvalidCharacter
            input.length < minLength -> TooShort
            else -> NoError
        }
    }

    fun useLogin() = toggleIsSetToLogin

    fun toggleLogin() {
        toggleIsSetToLogin = !toggleIsSetToLogin
        enableActionButton.postValue(checkEnableActionButton())
    }

    fun isUserRegistering() = !toggleIsSetToLogin

    fun startRequest(
        username: String,
        pwd: String
    ) {

        viewModelScope.launch {
            val loginAttemptResponse = when (toggleIsSetToLogin) {
                true -> authRepository.login(username, pwd)
                false -> authRepository.register(username, pwd)
            }
            when (loginAttemptResponse) {
                is Result.Success -> {
                    saveAuthCredentials(loginAttemptResponse.body)
                    _loginStatus.postValue(true)
                }
                is Result.Failure -> _makeToast.postValue(loginAttemptResponse.errorCode)
            }
        }

    }

    private fun saveAuthCredentials(token: String) {
        prefsRepository.saveCredentials(token)
    }

    fun makeToastDone() {
        _makeToast.value = null
    }

    fun loginDone() {
        _loginStatus.value = false
    }
}



