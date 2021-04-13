package com.akdogan.simpledivelog.datalayer.network

import com.akdogan.simpledivelog.datalayer.database.DatabaseDiveLogEntry
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.squareup.moshi.Json

data class NetworkDiveLogEntry(
    @Json(name = "id") var dataBaseId: String,
    val diveNumber: Int,
    val diveDuration: Int, // stored in minutes
    val maxDepth: Int, // stored in feet
    @Json(name = "location")val diveLocation: String,
    var diveDate: Long,
    val weight: Int? = null, // stored in Pound
    val airIn: Int? = null, // stored in PSI
    val airOut: Int? = null, // stored in PSI
    val notes: String? = null,
    val imgUrl: String? = null,
)

fun NetworkDiveLogEntry.asDataBaseModel(): DatabaseDiveLogEntry {
    return DatabaseDiveLogEntry(
        dataBaseId = this.dataBaseId,
        diveNumber = this.diveNumber,
        diveDuration = this.diveDuration, // stored in minutes
        maxDepth = this.maxDepth, // stored in feet
        diveLocation = this.diveLocation,
        diveDate = this.diveDate,
        weight = this.weight, // stored in Pound
        airIn = this.airIn, // stored in PSI
        airOut = this.airOut, // stored in PSI
        notes = this.notes,
        imgUrl = imgUrl
    )
}

fun List<NetworkDiveLogEntry>.asDatabaseModel(): List<DatabaseDiveLogEntry>{
    return this.map{
        it.asDataBaseModel()
    }
}

fun DiveLogEntry.asNetworkModel(): NetworkDiveLogEntry {
    return NetworkDiveLogEntry(
        dataBaseId = this.dataBaseId,
        diveNumber = this.diveNumber,
        diveDuration = this.diveDuration, // stored in minutes
        maxDepth = this.maxDepth, // stored in feet
        diveLocation = this.diveLocation,
        diveDate = this.diveDate,
        weight = this.weight, // stored in Pound
        airIn = this.airIn, // stored in PSI
        airOut = this.airOut, // stored in PSI
        notes = this.notes,
        imgUrl = this.imgUrl
    )
}
