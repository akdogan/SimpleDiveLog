package com.akdogan.simpledivelog.datalayer.database

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface DiveLogDatabaseDao {
    // CoRoutines #1: Mark DAO functions as suspend functions
    @Insert
    suspend fun insert(item: DatabaseDiveLogEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DatabaseDiveLogEntry>)

    @Update
    suspend fun update(item: DatabaseDiveLogEntry)

    @Query("SELECT EXISTS(SELECT 1 FROM dive_log_table WHERE dataBaseId = :key)")
    suspend fun checkIfEntryExists(key: String): Int

    @Query("SELECT * from dive_log_table WHERE dataBaseId = :key")
    suspend fun get(key: String): DatabaseDiveLogEntry?

    @Query("DELETE FROM dive_log_table")
    suspend fun deleteAll()

    @Query("DELETE FROM dive_log_table WHERE dataBaseId = :key")
    suspend fun delete(key: String)

    @Query("SELECT * from dive_log_table ORDER BY dive_number DESC")
    fun getAllEntriesAsLiveData(): LiveData<List<DatabaseDiveLogEntry>>


    // mark for removal
    @Query("SELECT * from dive_log_table ORDER BY dive_number DESC LIMIT 1")
    suspend fun getLatestDiveByDiveNumber(): DatabaseDiveLogEntry?

    // mark for removal
    @Query("SELECT MAX(dive_number) from dive_log_table")
    suspend fun getLatestDiveNumber(): Int?


    // mark for removal
    @Query("SELECT COUNT (*) from dive_log_table")
    suspend fun getNumberOfEntries(): Int
}
