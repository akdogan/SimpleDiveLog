package com.akdogan.simpledivelog.datalayer.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry

@Entity(tableName = "dive_log_table")
data class DatabaseDiveLogEntry(
    @PrimaryKey
    val dataBaseId: String,

    @ColumnInfo(name = "dive_number")
    var diveNumber: Int,

    @ColumnInfo(name = "dive_date")
    var diveDate: Long,

    @ColumnInfo(name = "dive_duration") // stored in minutes
    var diveDuration: Int,

    @ColumnInfo(name = "max_depth") // stored in feet
    var maxDepth: Int,

    @ColumnInfo(name = "dive_location")
    var diveLocation: String,

    @ColumnInfo(name = "weight") // stored in Pound
    var weight: Int? = null,

    @ColumnInfo(name = "air_in") // stored in PSI
    var airIn: Int? = null,

    @ColumnInfo(name = "air_out") // stored in PSI
    var airOut: Int? = null,

    @ColumnInfo(name = "notesInput")
    var comment: String? = null,

    @ColumnInfo(name = "notes")
    var notes: String? = null,

    @ColumnInfo(name = "imgSrc")
    var imgUrl: String? = null
)
fun DatabaseDiveLogEntry.asDomainModel(): DiveLogEntry {
    return DiveLogEntry(
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

fun List<DatabaseDiveLogEntry>.asDomainModel(): List<DiveLogEntry>{
    return this.map{
        it.asDomainModel()
    }
}

