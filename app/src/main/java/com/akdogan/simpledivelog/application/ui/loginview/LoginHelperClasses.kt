package com.akdogan.simpledivelog.application.ui.loginview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.akdogan.simpledivelog.datalayer.repository.AuthRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository

internal enum class TextInputState{
    Empty,
    TooShort,
    InvalidCharacter,
    DoesNotMatch,
    Valid
}

internal class LoginViewModelFactory(
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