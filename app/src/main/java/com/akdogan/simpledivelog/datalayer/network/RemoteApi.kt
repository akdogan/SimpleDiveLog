package com.akdogan.simpledivelog.datalayer.network

import com.akdogan.simpledivelog.datalayer.Data
import retrofit2.Response

interface RemoteApi {

    suspend fun getDives(token: String): Response<Data<List<NetworkDiveLogEntry>>>

    suspend fun getSingleDive(id: String, token: String): Response<Data<NetworkDiveLogEntry>>

    suspend fun createDive(item: NetworkDiveLogEntry, token: String): Response<Data<Any>>

    suspend fun updateDive(item: NetworkDiveLogEntry, token: String): Response<Data<Any>>

    suspend fun deleteAll(token: String): Response<Data<Any>>

    suspend fun delete(diveId: String, token: String): Response<Data<Any>>

}


class DefaultApi : RemoteApi {

    private val apiService: DiveLogApiService = DiveLogApi.retrofitService
    private val apiServiceV2: DiveLogApiServiceV2 = DiveLogApi.retrofitServiceV2

    override suspend fun getDives(token: String): Response<Data<List<NetworkDiveLogEntry>>> {
        return apiServiceV2.getDives(token)
    }

    override suspend fun getSingleDive(id: String, token: String): Response<Data<NetworkDiveLogEntry>> {
        return apiServiceV2.getSingleDive(id, token)
    }

    override suspend fun createDive(item: NetworkDiveLogEntry, token: String): Response<Data<Any>> {
        return apiServiceV2.createDive(item, token)
    }

    override suspend fun updateDive(item: NetworkDiveLogEntry, token: String): Response<Data<Any>> {
        return apiServiceV2.updateDive(item, token)
    }

    override suspend fun delete(diveId: String, token: String): Response<Data<Any>> {
        return apiServiceV2.delete(diveId, token)
    }

    override suspend fun deleteAll(token: String): Response<Data<Any>> {
        return apiServiceV2.deleteAll(token)
    }

}

