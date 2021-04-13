package com.akdogan.simpledivelog.datalayer.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.database.DatabaseDiveLogEntry
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabase
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabaseDao
import com.akdogan.simpledivelog.datalayer.database.asDomainModel
import com.akdogan.simpledivelog.datalayer.network.*
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface Repository{

    val loginStatus: LiveData<Boolean>

    val networkAvailable: LiveData<Boolean>

    val apiError: LiveData<Exception>

    val listOfDives: LiveData<List<DiveLogEntry>>

    val downloadStatus: LiveData<RepositoryDownloadStatus>

    val uploadApiStatus: LiveData<RepositoryUploadProgressStatus>

    fun onNetworkAvailable()

    fun onNetworkLost()

    suspend fun forceUpdate()

    fun getLatestDiveNumber(): Int

    suspend fun getSingleDive(diveId: String): DiveLogEntry?

    suspend fun startUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean,
        imageUri: Uri? = null
    )

    suspend fun deleteDive(diveId: String)

    suspend fun deleteAll()

    fun onErrorDone()

}

class DefaultRepository private constructor(
    context: Context,
    private val api: RemoteApi
) : Repository{

    companion object {
        @Volatile
        private var INSTANCE: Repository? = null

        fun getDefaultRepository(
            context: Context,
            api: RemoteApi = DefaultApi()
        ): Repository{
            return INSTANCE ?: synchronized(this){
                DefaultRepository(context, api).also {
                    INSTANCE = it
                }
            }
        }
    }

    // TODO Exceptions auf sealed class fehlertypen mappen

    override val loginStatus = api.loginStatus

    private val _networkAvailable = MutableLiveData<Boolean>()
    override val networkAvailable: LiveData<Boolean>
        get() = _networkAvailable

    private val _apiError = MutableLiveData<Exception>()
    override val apiError: LiveData<Exception>
        get() = _apiError

    private var _listOfDives: LiveData<List<DatabaseDiveLogEntry>> =
        liveData { emit(emptyList<DatabaseDiveLogEntry>()) }
    override val listOfDives: LiveData<List<DiveLogEntry>>
        get() = Transformations.map(_listOfDives) { list: List<DatabaseDiveLogEntry>? ->
            list?.asDomainModel() ?: emptyList()
        }

    private var database: DiveLogDatabaseDao = DiveLogDatabase.getInstance(context).diveLogDatabaseDao

    private val _downloadApiStatus = MutableLiveData<RepositoryDownloadStatus>()
    override val downloadStatus: LiveData<RepositoryDownloadStatus>
        get() = _downloadApiStatus

    private val _uploadApiStatus = MutableLiveData<RepositoryUploadProgressStatus>()
    override val uploadApiStatus: LiveData<RepositoryUploadProgressStatus>
        get() = _uploadApiStatus


    init {
        _listOfDives = database.getAllEntriesAsLiveData()
        CloudinaryApi.setup(context)
    }

    override fun onNetworkAvailable() {
        _networkAvailable.postValue(true)
    }

    override fun onNetworkLost() {
        _networkAvailable.postValue(false)
    }

    override suspend fun forceUpdate() {
        fetchDives()
    }

    override fun getLatestDiveNumber(): Int {
        return _listOfDives.value?.maxByOrNull { it.diveNumber }?.diveNumber ?: 0
    }

    private fun onFetching() {
        _downloadApiStatus.postValue(RepositoryDownloadStatus.FETCHING)
    }

    // TODO Not sure if its a problem that the status stays on Error
    private fun onFetchingDone() {
        if (_downloadApiStatus.value == RepositoryDownloadStatus.FETCHING) {
            _downloadApiStatus.postValue(RepositoryDownloadStatus.DONE)
        }
    }

    private suspend fun fetchDives() {
        onFetching()
        try {
            val list = api.getDives()
            database.deleteAll()
            database.insertAll(list.asDatabaseModel())
        } catch (e: Exception) {
            onDownloadErrorOccured(e)
        }
        onFetchingDone()
    }



    // Fetched from remote into the database, then fetched from database
    override suspend fun getSingleDive(diveId: String): DiveLogEntry? {
        onFetching()
        // Delay for debugging purpose so we can actually see the loading animation
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
    override suspend fun startUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean,
        imageUri: Uri?
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
    // TODO: Check if the coroutine can run into an endless situation (maybe needs a timeout to be save?)
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
            api.updateDive(entry.asNetworkModel())
        } catch (e: Exception) {
            onUploadErrorOccured(e)
        }
    }

    // Creates a single dive on the server. API does not respond the id, so all entities need to be refetched
    private suspend fun createDiveRemote(entry: DiveLogEntry) {
        try {
            api.createDive(entry.asNetworkModel())
        } catch (e: Exception) {
            onUploadErrorOccured(e)
        }
    }


    // Tries to fetch a single dive from remote and puts in the cache. Should be followed by getSingleDiveFromDatabase
    private suspend fun getSingleDiveFromRemote(diveId: String) {
        try {
            val networkEntry = api.getSingleDive(diveId)
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


    override suspend fun deleteDive(diveId: String) {
        onFetching()
        try {
            api.delete(diveId)
            fetchDives()
        } catch (e: Exception) {
            onDownloadErrorOccured(e)
        }
        onFetchingDone()
    }


    override suspend fun deleteAll() {
        onFetching()
        try {
            api.deleteAll()
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

    override fun onErrorDone() {
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


// TODO Cleanup, not sure if I need this
abstract class ClUploaderCallback {
    open fun clOnSuccess(result: String?) {}

    open fun clOnProgress(bytes: Long, totalBytes: Long) {}

    open fun clOnReschedule() {}

    open fun clOnError() {}

    open fun clOnStart() {}

}
