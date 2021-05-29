package com.akdogan.simpledivelog.application.ui.loginview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.akdogan.simpledivelog.datalayer.repository.AuthRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository

enum class TextInputErrorCases{
    Empty,
    TooShort,
    InvalidCharacter,
    DoesNotMatch,
    Valid
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