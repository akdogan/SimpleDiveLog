package com.akdogan.simpledivelog.datalayer.network

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Response
import java.util.*

interface RemoteApi {

    val loginStatus: LiveData<Boolean>

    suspend fun login(user: String, pwd: String): Boolean

    suspend fun register(user: String, pwd: String): Boolean

    suspend fun validateCredentials(token: String): Boolean

    suspend fun getDives(): List<NetworkDiveLogEntry>

    suspend fun getSingleDive(id: String): NetworkDiveLogEntry

    suspend fun createDive(item: NetworkDiveLogEntry): Boolean

    suspend fun updateDive(item: NetworkDiveLogEntry): Boolean

    suspend fun deleteAll(): Boolean

    suspend fun delete(diveId: String): Boolean

}


class DefaultApi : RemoteApi {

    private val apiService: DiveLogApiService = DiveLogApi.retrofitService
    private val apiServiceV2: DiveLogApiServiceV2 = DiveLogApi.retrofitServiceV2

    private val _loginStatus = MutableLiveData(false)
    override val loginStatus: LiveData<Boolean>
        get() = _loginStatus

    private var authToken: String? = null

    override suspend fun login(user: String, pwd: String): Boolean {
        val token = createAuthHeader(user, pwd)
        val response = apiServiceV2.login(token)
        parseResponse(response)
        if (response.isSuccessful) {
            _loginStatus.postValue(true)
            authToken = token
            return true
        }
        return false
    }

    override suspend fun register(user: String, pwd: String): Boolean {
        val token = createAuthHeader(user, pwd)
        val response = apiServiceV2.createUser(user, token)
        parseResponse(response)
        if (response.isSuccessful){
            _loginStatus.postValue(true)
            authToken = token
            return true
        }
        return false
    }

    override suspend fun validateCredentials(token: String): Boolean {
        val response = apiServiceV2.login(token)
        parseResponse(response)
        if (response.isSuccessful) {
            _loginStatus.postValue(true)
            authToken = token
            return true
        }
        return false
    }



    override suspend fun getDives(): List<NetworkDiveLogEntry> {
        return apiService.getDives()
    }

    override suspend fun getSingleDive(id: String): NetworkDiveLogEntry {
        return apiService.getSingleDive(id)
    }

    override suspend fun createDive(item: NetworkDiveLogEntry): Boolean {
        apiService.createDive(item)
        return true
    }

    override suspend fun updateDive(item: NetworkDiveLogEntry): Boolean {
        apiService.updateDive(item)
        return true
    }

    override suspend fun deleteAll(): Boolean {
        apiService.deleteAll()
        return true
    }

    override suspend fun delete(diveId: String): Boolean {
        TODO("Not yet implemented")
    }

    private fun createAuthHeader(username: String, pwd: String): String {
        val encoder = Base64.getEncoder()
        val mByteArray = "$username:$pwd".toByteArray()
        val token = "Basic ${encoder.encodeToString(mByteArray)}"
        Log.d("REPOSITORY_AUTHHEADER", "username: $username | pwd: $pwd")
        Log.d("REPOSITORY_AUTHHEADER", "token: $token")
        return token
    }

    private fun <T> parseResponse(response: Response<T>): String {
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
    }
}

