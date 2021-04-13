package com.akdogan.simpledivelog.datalayer.network

import com.akdogan.simpledivelog.datalayer.repository.Data
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "http://ec2-3-127-80-31.eu-central-1.compute.amazonaws.com:8080/"
private const val BASE_URL_V2 = "http://ec2-3-127-80-31.eu-central-1.compute.amazonaws.com:8080/v2/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

interface DiveLogApiService {

    @GET("dives")
    suspend fun getDives():
            List<NetworkDiveLogEntry>

    @GET("dive/{id}")
    suspend fun getSingleDive(@Path("id")id: String?): NetworkDiveLogEntry

    @POST("dive/create")
    suspend fun createDive(@Body d: NetworkDiveLogEntry)

    @PUT("dive/update")
    suspend fun updateDive(@Body d: NetworkDiveLogEntry)

    @DELETE("dives")
    suspend fun deleteAll()

    @DELETE("dive/{id}")
    suspend fun delete(@Path("id")id: String)
}

interface DiveLogApiServiceV2{
    @POST("user/{user}")
    suspend fun createUser(
        @Path("user") userName: String,
        @Header("Authorization") authToken: String
    ): Response<Data<Any>>


    @GET("dives")
    suspend fun login(
        @Header("Authorization") authToken: String
    ): Response<Data<Any>>

    @GET("dives")
    suspend fun getDives():
            List<NetworkDiveLogEntry>

    @GET("dive/{id}")
    suspend fun getSingleDive(
        @Path("id")id: String?,
        @Header("Authorization") authToken: String
    ): NetworkDiveLogEntry

    @POST("dive/create")
    suspend fun createDive(
        @Body d: NetworkDiveLogEntry,
        @Header("Authorization") authToken: String)

    @PUT("dive/update")
    suspend fun updateDive(
        @Body d: NetworkDiveLogEntry,
        @Header("Authorization") authToken: String)

    @DELETE("dives")
    suspend fun deleteAll(@Header("Authorization") authToken: String)

    @DELETE("dive/{id}")
    suspend fun delete(
        @Path("id")id: String,
        @Header("Authorization") authToken: String)
}


private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

private val retrofitV2 = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL_V2)
    .build()



object DiveLogApi {

    val retrofitService: DiveLogApiService by lazy {
        retrofit.create(DiveLogApiService::class.java)
    }

    val retrofitServiceV2: DiveLogApiServiceV2 by lazy {
        retrofitV2.create(DiveLogApiServiceV2::class.java)
    }
}