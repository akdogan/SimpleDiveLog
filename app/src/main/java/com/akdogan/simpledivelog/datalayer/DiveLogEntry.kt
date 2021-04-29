
package com.akdogan.simpledivelog.datalayer

data class DiveLogEntry(
        var dataBaseId: String,
        val diveNumber: Int,
        val diveDuration: Int, // stored in minutes
        val maxDepth: Int, // stored in feet
        val diveLocation: String,
        val diveDate: Long,
        val weight: Int? = null, // stored in Pound
        val airIn: Int? = null, // stored in PSI
        val airOut: Int? = null, // stored in PSI
        val notes: String? = null,
        // Should be done in a different way
        val imgUrl: String? = null
)


