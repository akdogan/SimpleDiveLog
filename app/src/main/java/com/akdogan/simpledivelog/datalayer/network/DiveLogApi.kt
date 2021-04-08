package com.akdogan.simpledivelog.datalayer.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "http://ec2-3-127-80-31.eu-central-1.compute.amazonaws.com:8080/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()


private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

// TODO Add Apis for users

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


object DiveLogApi {
    val retrofitService: DiveLogApiService by lazy {
        retrofit.create(DiveLogApiService::class.java)
    }
}