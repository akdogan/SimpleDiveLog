package com.akdogan.simpledivelog.application.ui.loginview

import androidx.lifecycle.*
import com.akdogan.simpledivelog.application.ui.loginview.TextInputState.*
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_ERROR
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


    internal val usernameState = Transformations.map(username) {
        checkInputMediator(
            input = it,
            fullPattern = USERNAME_PATTERN,
            allowedCharsPattern = USERNAME_VALID_CHARS_PATTERN,
            minLength = USERNAME_MIN_LENGTH
        )
    }

    internal val passwordState = Transformations.map(password) {
        checkInputMediator(
            input = it,
            fullPattern = PASSWORD_PATTERN,
            allowedCharsPattern = PASSWORD_VALID_CHARS_PATTERN,
            minLength = PASSWORD_MIN_LENGTH
        )
    }

    // Repeat state should be checked also when password is modified
    internal val passwordRepeatState = MediatorLiveData<TextInputState>().apply {
        // Might cause bugs when going to register and back to login
        fun addSourceWithSwitch(src: MutableLiveData<String>){
            this.addSource(src){
                if (!toggleIsSetToLogin) value = checkInputPasswordRepeat(passwordRepeat.value, password.value)
            }
        }
        addSourceWithSwitch(password)
        addSourceWithSwitch(passwordRepeat)
    }

    private fun checkInputPasswordRepeat(passwordRepeat: String?, password: String?): TextInputState {
        return checkInputMediator(
            input = passwordRepeat,
            fullPattern = PASSWORD_PATTERN,
            allowedCharsPattern = PASSWORD_VALID_CHARS_PATTERN,
            minLength = PASSWORD_MIN_LENGTH,
            twin = password
        )
    }

    val enableActionButton: MediatorLiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(usernameState) { value = checkEnableActionButton() }
        addSource(passwordState) { value = checkEnableActionButton() }
        addSource(passwordRepeatState) { value = checkEnableActionButton() }
    }

    private fun checkEnableActionButton(): Boolean {
        return usernameState.value == Valid &&
                passwordState.value == Valid &&
                (toggleIsSetToLogin || passwordRepeatState.value == Valid)
    }

    private fun checkInputMediator(
        input: String?,
        fullPattern: String,
        allowedCharsPattern: String,
        minLength: Int,
        twin: String? = input
    ): TextInputState {
        return when {
            input.isNullOrBlank() -> Empty
            input != twin -> DoesNotMatch
            matchPattern(input, fullPattern) -> Valid
            !matchPattern(input, allowedCharsPattern) -> InvalidCharacter
            input.length < minLength -> TooShort
            else -> Empty
        }
    }

    fun useLogin() = toggleIsSetToLogin

    fun toggleLogin() {
        toggleIsSetToLogin = !toggleIsSetToLogin
        enableActionButton.postValue(checkEnableActionButton())
    }

    fun isUserRegistering() = !toggleIsSetToLogin

    fun startRequest(){
        try {
            val localUsername = requireNotNull(username.value)
            val localPwd = requireNotNull(password.value)
            viewModelScope.launch {
                val loginAttemptResponse = when (toggleIsSetToLogin) {
                    true -> authRepository.login(localUsername, localPwd)
                    false -> authRepository.register(localUsername, localPwd)
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
        catch (e: IllegalArgumentException){
            _makeToast.postValue(GENERAL_ERROR)
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




