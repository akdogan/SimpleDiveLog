package com.akdogan.simpledivelog.datalayer

import android.content.res.Resources
import com.akdogan.simpledivelog.R


sealed class Result<out T> {
    class Success<out T>(val body: T) : Result<T>()
    object EmptySuccess : Result<Nothing>()
    class Failure(val errorCode: Int) : Result<Nothing>()
}

// Maybe better
sealed class Result2<out T> {
    open class Success2<out T> : Result2<T>(){
        class SuccessWithData<out T>(val body: T) : Success2<T>()
        class SuccessEmpty : Success2<Nothing>()
    }
    class Failure(val errorCode: Int) : Result2<Nothing>()
}


// TODO: some apis have data, others not. Write a custom converter Adapter Maybe
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
    const val ITEM_NOT_FOUND = 404
    // Login / Register
    const val LOGIN_UNAUTHORIZED = 401
    const val REGISTER_USER_EXISTS_ALREADY = 200

    // INTERNAL
    const val CALL_SUCCESS_EMPTY_BODY = 910

    fun getMessage(res: Resources, error: Int): String {
        return when (error) {
            NO_INTERNET_CONNECTION -> res.getString(R.string.no_internet_connection)
            SERVER_ERROR -> res.getString(R.string.server_error)
            DATABASE_ERROR -> res.getString(R.string.database_error)
            GENERAL_UNAUTHORIZED -> res.getString(R.string.general_unauthorized)
            ITEM_NOT_FOUND -> res.getString(R.string.item_not_found)
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

