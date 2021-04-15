package com.akdogan.simpledivelog.datalayer

import android.content.res.Resources
import com.akdogan.simpledivelog.R


sealed class Result<out T> {
    class Success<out T>(val body: T) : Result<T>()
    object EmptySuccess : Result<Nothing>()
    class Failure(val errorCode: Int) : Result<Nothing>()
}


data class Data<out T>(
    val data: T?,
    val error: String?
)


object ErrorCases {
    // USER FACING
    const val GENERAL_ERROR = 999 // Fallback error
    const val NO_INTERNET_CONNECTION = 900
    const val SERVER_ERROR = 500
    const val DATABASE_ERROR = 510
    const val GENERAL_UNAUTHORIZED = 410 // Unauthorized response during general use of the app
    // Login / Register
    const val LOGIN_UNAUTHORIZED = 401
    const val REGISTER_USER_EXISTS_ALREADY = 200

    // INTERNAL
    const val CALL_SUCCESS_EMPTY_BODY = 910

    fun getMessage(res: Resources, error: Int): String {
        return when (error) {
            NO_INTERNET_CONNECTION -> res.getString(R.string.no_internet_connection)
            LOGIN_UNAUTHORIZED -> res.getString(R.string.login_unauthorized)
            REGISTER_USER_EXISTS_ALREADY -> res.getString(R.string.register_user_exists_already)
            else -> res.getString(R.string.general_error)
        }
    }
}

// Write test, make authRepo take a testdouble then check proper behaviour on timeout
enum class LoginStatus {
    SUCCESS,
    UNVERIFIED,
    FAILED
}

//HIGHLY EXPERIMENTAL STUFF GOING ON HERE
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.SOURCE)
annotation class ErrorFlag

