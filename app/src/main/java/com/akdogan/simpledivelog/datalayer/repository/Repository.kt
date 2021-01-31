package com.akdogan.simpledivelog.datalayer.repository

import android.content.Context
import android.net.Uri
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


object Repository {
    // TODO Exceptions auf sealed class fehlertypen mappen

    // Todo Repo hat jetzt dependencies auf das Android System
    // Stattdessen in der MainActivity betwork status observen und hier nur zwei funktionen have / lost aufrufen


    private val _networkAvailable = MutableLiveData<Boolean>()
    val networkAvailable: LiveData<Boolean>
        get() = _networkAvailable

    private val _apiError = MutableLiveData<Exception>()
    val apiError: LiveData<Exception>
        get() = _apiError

    private var _listOfDives: LiveData<List<DatabaseDiveLogEntry>> =
        liveData { emit(emptyList<DatabaseDiveLogEntry>()) }
    val listOfDives: LiveData<List<DiveLogEntry>>
        get() = Transformations.map(_listOfDives) { list: List<DatabaseDiveLogEntry>? ->
            list?.asDomainModel() ?: emptyList()
        }

    private lateinit var database: DiveLogDatabaseDao

    private val _downloadApiStatus = MutableLiveData<RepositoryDownloadStatus>()
    val downloadStatus: LiveData<RepositoryDownloadStatus>
        get() = _downloadApiStatus

    private val _uploadApiStatus = MutableLiveData<RepositoryUploadProgressStatus>()
    val uploadApiStatus: LiveData<RepositoryUploadProgressStatus>
        get() = _uploadApiStatus


    // Called from the MainActivity to setup the database
    suspend fun setup(
        context: Context,
    ) {
        onFetching()
        database = DiveLogDatabase.getInstance(context).diveLogDatabaseDao
        _listOfDives = database.getAllEntriesAsLiveData()
        fetchDives()
        CloudinaryApi.setup(context)
    }

    fun onNetworkAvailable() {
        _networkAvailable.postValue(true)
    }

    fun onNetworkLost() {
        _networkAvailable.postValue(false)
    }

    suspend fun forceUpdate() {
        fetchDives()
    }

    fun getLatestDiveNumber(): Int {
        Log.i("DIVENUMER TRACING", "getLatestDiveNumber called")
        val test: Int = _listOfDives.value?.maxByOrNull { it.diveNumber }?.diveNumber ?: 0
        Log.i("DIVENUMER TRACING", "divenumber is $test")
        return test
    }

    private fun onFetching() {
        _downloadApiStatus.value = RepositoryDownloadStatus.FETCHING
    }

    // TODO Not sure if its a problem that the status stays on Error
    private fun onFetchingDone() {
        if (_downloadApiStatus.value == RepositoryDownloadStatus.FETCHING) {
            _downloadApiStatus.value = RepositoryDownloadStatus.DONE
        }
    }

    private suspend fun fetchDives() {
        onFetching()
        try {
            val list = DiveLogApi.retrofitService.getDives()
            database.deleteAll()
            database.insertAll(list.asDatabaseModel())
        } catch (e: Exception) {
            onDownloadErrorOccured(e)
        }
        onFetchingDone()
    }


    // Fetched from remote into the database, then fetched from database
    suspend fun getSingleDive(diveId: String): DiveLogEntry? {
        onFetching()
        // Delay for debuggin purpose so we can actually see the loading animation
        delay(200)
        var entry = getSingleDiveFromDataBase(diveId)
        if (entry == null) {
            getSingleDiveFromRemote(diveId)
            entry = getSingleDiveFromDataBase(diveId)
        }
        onFetchingDone()
        return entry
    }


    // Starts creation of new entry. If there is a imgUri, image upload is done first
    suspend fun startUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean,
        imageUri: Uri? = null
    ){
        uploadStart()
        if (imageUri != null){
            val resultUrl = startPictureUploadCoRoutine(imageUri)
            decideUpload(diveLogEntry.copy(imgUrl = resultUrl), createNewEntry)
        } else {
            decideUpload(diveLogEntry, createNewEntry)
        }
    }

    // Start picture upload. Suspends execution until upload is done, url is returned
    private suspend fun startPictureUploadCoRoutine(
        uri: Uri,
    ): String? {
        return suspendCoroutine {continuation ->
            CloudinaryApi.uploadPicture(
                uri,
                object : ClUploaderCallback() {

                    override fun clOnError() {
                        mediaUploadDone()
                        continuation.resume(null)
                    }

                    override fun clOnSuccess(result: String?) {
                        mediaUploadDone()
                        Log.i("UPLOADSTATUS", "OnSuccessCalled")
                        continuation.resume(result)
                    }

                    override fun clOnReschedule() {
                        mediaUploadDone()
                        continuation.resume(null)
                    }

                    override fun clOnProgress(bytes: Long, totalBytes: Long) {
                        mediaUploadProgress(bytes, totalBytes)
                    }
                })
        }
    }

    // Start upload of actual entry (create or update)
    private suspend fun decideUpload(diveLogEntry: DiveLogEntry, createNewEntry: Boolean){
        uploadStart()
        if (createNewEntry){
            createDiveRemote(diveLogEntry)
            forceUpdate()
            uploadDone()
        } else {
            updateDiveRemote(diveLogEntry)
            getSingleDiveFromRemote(diveLogEntry.dataBaseId)
            uploadDone()
        }
    }

    private fun uploadStart(){
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.INDETERMINATE_UPLOAD
        )
    }
    private fun mediaUploadProgress(progress: Long, total: Long){
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.PROGRESS_UPLOAD,
            progress = progress,
            total = total
        )
    }
    private fun mediaUploadDone(){
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.INDETERMINATE_UPLOAD,
            progress = 0,
            total = 0
        )
    }

    private fun uploadDone(){
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.DONE
        )
    }

    private suspend fun updateDiveRemote(entry: DiveLogEntry) {
        try {
            DiveLogApi.retrofitService.updateDive(entry.asNetworkModel())
        } catch (e: Exception) {
            onUploadErrorOccured(e)
        }
    }

    // Creates a single dive on the server. API does not respond the id, so all entities need to be refetched
    private suspend fun createDiveRemote(entry: DiveLogEntry) {
        try {
            DiveLogApi.retrofitService.createDive(entry.asNetworkModel())
        } catch (e: Exception) {
            onUploadErrorOccured(e)
        }
    }


    // Tries to fetch a single dive from remote and puts in the cache. Should be followed by getSingleDiveFromDatabase
    private suspend fun getSingleDiveFromRemote(diveId: String) {
        try {
            val networkEntry = DiveLogApi.retrofitService.getSingleDive(diveId)
            if (database.checkIfEntryExists(networkEntry.dataBaseId) == 1) {
                database.update(networkEntry.asDataBaseModel())
            } else {
                database.insert(networkEntry.asDataBaseModel())
            }
        } catch (e: Exception) {
            onDownloadErrorOccured(e)
            Log.i("DATABASE", e.toString())
        }
    }

    // Tries to fetch a single dive from the cache and returns it, or returns null if no such element exists
    private suspend fun getSingleDiveFromDataBase(diveId: String): DiveLogEntry? {
        val entry = database.get(diveId)
        return entry?.asDomainModel()
    }


    suspend fun deleteDive(diveId: String) {
        onFetching()
        try {
            DiveLogApi.retrofitService.delete(diveId)
            fetchDives()
        } catch (e: Exception) {
            onDownloadErrorOccured(e)
        }
        onFetchingDone()
    }


    suspend fun deleteAll() {
        onFetching()
        try {
            DiveLogApi.retrofitService.deleteAll()
            fetchDives()
        } catch (e: Exception) {
            onDownloadErrorOccured(e)
        }
        onFetchingDone()
    }

    private fun onDownloadErrorOccured(e: Exception) {
        _apiError.value = e
        _downloadApiStatus.value = RepositoryDownloadStatus.ERROR
    }

    private fun onUploadErrorOccured(e: Exception){
        _apiError.value = e
        uploadDone()
    }

    fun onErrorDone() {
        _apiError.value = null
    }


}

// Todo Maybe find better way with nested enum or sealed class / Api status object with status enum and error enum
enum class RepositoryDownloadStatus {
    FETCHING,
    DONE,
    ERROR
}

enum class RepositoryUploadStatus {
    INDETERMINATE_UPLOAD,
    PROGRESS_UPLOAD,
    DONE
}


data class RepositoryUploadProgressStatus(
    val status: RepositoryUploadStatus = RepositoryUploadStatus.DONE,
    val progress: Long = 0L,
    val total: Long = 0L,
) {
    val percentage: Int
        get() {
            return if (total > 0) {
                (progress.toInt() / (total.toInt() / 100))
            } else {
                0
            }
        }
}

abstract class ClUploaderCallback {
    open fun clOnSuccess(result: String?) {}

    open fun clOnProgress(bytes: Long, totalBytes: Long) {}

    open fun clOnReschedule() {}

    open fun clOnError() {}

    open fun clOnStart() {}

}
