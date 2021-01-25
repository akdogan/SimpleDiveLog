package com.akdogan.simpledivelog.datalayer.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.akdogan.simpledivelog.datalayer.database.DatabaseDiveLogEntry
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabase
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabaseDao
import com.akdogan.simpledivelog.datalayer.database.asDomainModel

import com.akdogan.simpledivelog.datalayer.network.*
import kotlinx.coroutines.delay
import java.lang.Exception


object Repository {
    // TODO Exceptions auf sealed class fehlertypen mappen
    private val _apiError = MutableLiveData<Exception>()
    val apiError : LiveData<Exception>
        get() = _apiError

    private var _listOfDives : LiveData<List<DatabaseDiveLogEntry>> = liveData { emit(emptyList()) }
    val listOfDives: LiveData<List<DiveLogEntry>>
        get() = Transformations.map(_listOfDives) { list: List<DatabaseDiveLogEntry>? ->
            list?.asDomainModel() ?: emptyList()
        }

    lateinit var database: DiveLogDatabaseDao

    private val _repositoryApiStatus = MutableLiveData<RepositoryApiStatus>()
    val repositoryApiStatus: LiveData<RepositoryApiStatus>
        get() = _repositoryApiStatus

    // Called from the MainActivity to setup the database
    suspend fun setup(context: Context) {
        onFetching()
        database = DiveLogDatabase.getInstance(context).diveLogDatabaseDao
        _listOfDives = database.getAllEntriesAsLiveData()
        fetchDives()
    }

    suspend fun forceUpdate(){
        fetchDives()
    }

    fun getLatestDiveNumber(): Int{
        return _listOfDives.value?.maxBy { it.diveNumber }?.diveNumber ?: 0
    }

    private fun onFetching() { _repositoryApiStatus.value = RepositoryApiStatus.FETCHING }

    // TODO Not sure if its a problem that the status stays on Error
    private fun onFetchingDone() {
        if (_repositoryApiStatus.value == RepositoryApiStatus.FETCHING) {
            _repositoryApiStatus.value = RepositoryApiStatus.DONE
        }
    }

    // TODO ?? Maybe better to split fetching from remote and fetching from database into separate functions?
    private suspend fun fetchDives() {
        onFetching()
        try {
            val list = DiveLogApi.retrofitService.getDives()
            database.deleteAll()
            database.insertAll(list.asDatabaseModel())
        } catch (e: Exception){
            onErrorOccured(e)
        }
        onFetchingDone()
    }

    // TODO ?? Does this make sense? Item gets fetched from database. If it does not exist, it gets
    // Fetched from remote into the database. Then tried again
    suspend fun getSingleDive(diveId: String): DiveLogEntry?{
        onFetching()
        delay(1000)
        var entry = getSingleDiveFromDataBase(diveId)
        if (entry == null){
            getSingleDiveFromRemote(diveId)
            entry = getSingleDiveFromDataBase(diveId)
        }
        onFetchingDone()
        return entry
    }

    suspend fun uploadSingleDive(entry: DiveLogEntry){
        onFetching()
        createDiveRemote(entry)
        forceUpdate()
        onFetchingDone()
    }

    suspend fun updateSingleDive(entry: DiveLogEntry){
        onFetching()
        updateDiveRemote(entry)
        getSingleDiveFromRemote(entry.dataBaseId)
        onFetchingDone()
    }

    private suspend fun updateDiveRemote(entry: DiveLogEntry){
        try {
            DiveLogApi.retrofitService.updateDive(entry.asNetworkModel())
        } catch (e: Exception) {
            onErrorOccured(e)
        }
    }

    // Creates a single dive on the server. API does not respond the id, so all entities need to be refetched
    private suspend fun createDiveRemote(entry: DiveLogEntry){
        try {
            DiveLogApi.retrofitService.createDive(entry.asNetworkModel())
        } catch (e: Exception) {
            onErrorOccured(e)
        }
    }


    // Tries to fetch a single dive from remote and puts in the cache. Should be followed by getSingleDiveFromDatabase
    private suspend fun getSingleDiveFromRemote(diveId: String){
        try {
            var networkEntry = DiveLogApi.retrofitService.getSingleDive(diveId)
            if (database.checkIfEntryExists(networkEntry.dataBaseId) == 1)
            {
                database.update(networkEntry.asDataBaseModel())
            } else {
                database.insert(networkEntry.asDataBaseModel())
            }
        } catch (e: Exception){
            onErrorOccured(e)
            Log.i("DATABASE", e.toString())
        }
    }

    // Tries to fetch a single dive from the cache and returns it, or returns null if no such element exists
    private suspend fun getSingleDiveFromDataBase(diveId: String): DiveLogEntry?{
        val entry = database.get(diveId)
        return entry?.asDomainModel()
    }

    //TODO ?? Delete on server and update all data OR delete on server and in cache, no update?
    suspend fun deleteDive(diveId: String){
        onFetching()
        try {
            DiveLogApi.retrofitService.delete(diveId)
            fetchDives()
        } catch (e: Exception){
            onErrorOccured(e)
        }
        onFetchingDone()
    }

    //TODO ?? Delete on server and update all data OR delete on server and in cache, no update?
    suspend fun deleteAll(){
        onFetching()
        try {
            DiveLogApi.retrofitService.deleteAll()
            fetchDives()
        } catch (e: Exception){
            onErrorOccured(e)
        }
        onFetchingDone()
    }

    private fun onErrorOccured(e: Exception){
        _apiError.value = e
        _repositoryApiStatus.value = RepositoryApiStatus.ERROR
    }

    fun onErrorDone() {
        _apiError.value = null
    }


}

// Todo Maybe find better way with nested enum or sealed class / Api status object with status enum and error enum
enum class RepositoryApiStatus {
    FETCHING,
    DONE,
    ERROR
}