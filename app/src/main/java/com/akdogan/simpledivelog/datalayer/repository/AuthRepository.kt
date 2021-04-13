package com.akdogan.simpledivelog.datalayer.repository

import android.util.Log
import com.akdogan.simpledivelog.datalayer.network.DiveLogApi
import com.akdogan.simpledivelog.datalayer.repository.ErrorCases.GENERAL_ERROR
import com.akdogan.simpledivelog.datalayer.repository.ErrorCases.LOGIN_UNAUTHORIZED
import com.akdogan.simpledivelog.datalayer.repository.ErrorCases.NO_INTERNET_CONNECTION
import com.akdogan.simpledivelog.datalayer.repository.ErrorCases.REGISTER_USER_EXISTS_ALREADY
import com.akdogan.simpledivelog.diveutil.Constants.AUTH_TEMPLATE
import retrofit2.Response
import java.net.UnknownHostException
import java.util.*

interface AuthRepository{
    suspend fun login(username: String, pwd: String): Result<String>

    suspend fun validateCredentials(token: String): LoginStatus

    suspend fun register(username: String, pwd: String): Result<String>
}

class DefaultAuthRepository : AuthRepository {

    private val apiService = DiveLogApi.retrofitServiceV2

    private suspend fun call(
        token: String,
        callFunction: suspend () -> Response<Data<Any>>
    ) : Result<String>{
        try{
            val response = callFunction.invoke()
            parseResponse(response)
            return if (response.isSuccessful) {
                Result.Success(token)
            } else {
                when (response.code()){
                    401 -> Result.Failure(LOGIN_UNAUTHORIZED)
                    400 -> Result.Failure(REGISTER_USER_EXISTS_ALREADY)
                    else -> Result.Failure(GENERAL_ERROR)
                }
            }
        } catch (e: UnknownHostException) {
            return Result.Failure(NO_INTERNET_CONNECTION)
        } catch (e: Exception) {
            return Result.Failure(GENERAL_ERROR)
        }
    }

    override suspend fun login(username: String, pwd: String): Result<String> {
        val token = createAuthHeader(username, pwd)
        return call(token){
            apiService.login(token)
        }
    }

    override suspend fun validateCredentials(token: String): LoginStatus {
        // If there is no internet or a server error occurs, the app goes into the logged in but not
        // verified state and will try to verify the login later on
        // Basically the user only gets logged out if the credentials are deemed wrong by the server
        // If response timed out, maybe post a worker that checks the login
        // status later again (currently only check happens on internet restored)
        val result = call(token){
            apiService.login(token)
        }
        return if (result is Result.Failure){
            when (result.errorCode){
                LOGIN_UNAUTHORIZED -> LoginStatus.FAILED
                else -> LoginStatus.UNVERIFIED
            }
        } else {
            LoginStatus.SUCCESS
        }
    }

    override suspend fun register(username: String, pwd: String): Result<String> {
        val token = createAuthHeader(username, pwd)
        return call(token){
            apiService.createUser(username, token)
        }
    }

    private fun createAuthHeader(username: String, pwd: String): String {
        val encoder = Base64.getEncoder()
        val mByteArray = "$username:$pwd".toByteArray()
        return AUTH_TEMPLATE + encoder.encodeToString(mByteArray)
    }

    // For Logging only
    private fun <T> parseResponse(response: Response<T>?): String {
        response?.let {
            val parsedResponse = StringBuilder()
                .append("Is Successful: ${response.isSuccessful}\n")
                .append("StatusCode: ${response.code()}\n")
                .append("HTTP StatusMessage: ${response.message()}\n")
                .apply {
                    if (response.isSuccessful) {
                        this.append("\n${response.body().toString()}")
                    } else {
                        this.append("ErrorBody: ${response.errorBody()?.string()}")
                    }
                }
                .toString()
            Log.d("LOGIN_TEST", parsedResponse)
            return parsedResponse
        } ?: return "response was null"
    }
}