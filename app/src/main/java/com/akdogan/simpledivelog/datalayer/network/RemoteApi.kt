package com.akdogan.simpledivelog.datalayer.network

import com.akdogan.simpledivelog.datalayer.Data
import retrofit2.Response

interface RemoteApi {

    suspend fun getDives(token: String): Response<Data<List<NetworkDiveLogEntry>>>

    suspend fun getSingleDive(id: String): NetworkDiveLogEntry

    suspend fun createDive(item: NetworkDiveLogEntry, token: String): Response<Data<Any>>

    suspend fun updateDive(item: NetworkDiveLogEntry, token: String)

    suspend fun deleteAll(): Boolean

    suspend fun delete(diveId: String): Boolean

}


class DefaultApi : RemoteApi {

    private val apiService: DiveLogApiService = DiveLogApi.retrofitService
    private val apiServiceV2: DiveLogApiServiceV2 = DiveLogApi.retrofitServiceV2

    override suspend fun getDives(token: String): Response<Data<List<NetworkDiveLogEntry>>> {
        return apiServiceV2.getDives(token)
    }

    override suspend fun getSingleDive(id: String): NetworkDiveLogEntry {
        return apiService.getSingleDive(id)
    }

    override suspend fun createDive(item: NetworkDiveLogEntry, token: String): Response<Data<Any>> {
        return apiServiceV2.createDive(item, token)
    }

    override suspend fun updateDive(item: NetworkDiveLogEntry, token: String) {
        return apiService.updateDive(item)
    }

    override suspend fun deleteAll(): Boolean {
        apiService.deleteAll()
        return true
    }

    override suspend fun delete(diveId: String): Boolean {
        apiService.delete(diveId)
        return true
    }

}

