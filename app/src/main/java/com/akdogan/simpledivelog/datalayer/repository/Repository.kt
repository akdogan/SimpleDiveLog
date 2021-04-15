package com.akdogan.simpledivelog.datalayer.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.akdogan.simpledivelog.datalayer.Data
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.ErrorCases.CALL_SUCCESS_EMPTY_BODY
import com.akdogan.simpledivelog.datalayer.ErrorCases.DATABASE_ERROR
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_ERROR
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_UNAUTHORIZED
import com.akdogan.simpledivelog.datalayer.ErrorCases.SERVER_ERROR
import com.akdogan.simpledivelog.datalayer.Result
import com.akdogan.simpledivelog.datalayer.database.DatabaseDiveLogEntry
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabase
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabaseDao
import com.akdogan.simpledivelog.datalayer.database.asDomainModel
import com.akdogan.simpledivelog.datalayer.network.*
import kotlinx.coroutines.delay
import retrofit2.Response
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface Repository {

    val loginStatus: LiveData<Boolean>

    val networkAvailable: LiveData<Boolean>

    val apiError: LiveData<Exception>

    val listOfDives: LiveData<List<DiveLogEntry>>

    val downloadStatus: LiveData<RepositoryDownloadStatus>

    val uploadApiStatus: LiveData<RepositoryUploadProgressStatus>

    fun onNetworkAvailable()

    fun onNetworkLost()

    /**
     * Set the authentication token
     * If no token is set and the api is attempted to call, an IllegalStateException will be thrown
     * @param token The Basic Auth token to be used for the Api
     */
    fun setAuthToken(token: String)

    suspend fun forceUpdate(): Result<Any>

    fun getLatestDiveNumber(): Int

    suspend fun getSingleDive(diveId: String): DiveLogEntry?

    suspend fun startUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean,
        imageUri: Uri? = null
    )

    suspend fun deleteDive(diveId: String)

    suspend fun deleteAll()

    suspend fun cleanLogout()

    fun onErrorDone()


}

class DefaultRepository private constructor(
    context: Context,
    private val api: RemoteApi,
    private val database: DiveLogDatabaseDao
) : Repository {

    companion object {
        private const val TAG = "REPO_V2"

        @Volatile
        private var INSTANCE: Repository? = null

        fun getDefaultRepository(
            context: Context,
            api: RemoteApi = DefaultApi(),
            database: DiveLogDatabaseDao? = null
        ): Repository {
            val db = database ?: DiveLogDatabase.getInstance(context).diveLogDatabaseDao
            return INSTANCE ?: synchronized(this) {
                DefaultRepository(context, api, db).also {
                    INSTANCE = it
                }
            }
        }
    }

    // TODO Exceptions auf sealed class fehlertypen mappen

    private val _loginStatus = MutableLiveData<Boolean>()
    override val loginStatus: LiveData<Boolean>
        get() = _loginStatus

    private var authToken: String? = null

    private fun useToken(): String =
        authToken ?: throw IllegalStateException("Auth Token has not been setup yet")

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

    private suspend fun <T> safeCall(
        apiCall: suspend (String) -> Response<Data<T>>
    ): Result<T> {

        try {
            val response = apiCall.invoke(useToken())
            return if (response.isSuccessful) {
                val body: T =
                    response.body()?.data ?: return Result.Failure(CALL_SUCCESS_EMPTY_BODY)
                Result.Success(body)
            } else {
                when (response.code()) {
                    401 -> Result.Failure(GENERAL_UNAUTHORIZED)
                    500 -> Result.Failure(SERVER_ERROR)
                    else -> Result.Failure(GENERAL_ERROR)
                }
            }
        } catch (e: UnknownHostException) {
            return Result.Failure(ErrorCases.NO_INTERNET_CONNECTION)
        } catch (e: Exception) {
            return Result.Failure(GENERAL_ERROR)
        }
    }

    override fun onNetworkAvailable() {
        _networkAvailable.postValue(true)
    }

    override fun onNetworkLost() {
        _networkAvailable.postValue(false)
    }

    override fun setAuthToken(token: String) {
        authToken = token
    }

    override suspend fun forceUpdate(): Result<Any> {
        return fetchDives()
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

    private suspend fun fetchDives(): Result<Any> {
        onFetching()
        val result = safeCall { token ->
            api.getDives(token)
        }
        return try {
            if (result is Result.Success) {
                database.deleteAll()
                database.insertAll(result.body.asDatabaseModel())
                Result.EmptySuccess
            } else result
        } catch (e: Exception) {
            Log.i(TAG, "Database Exception: $e")
            Result.Failure(DATABASE_ERROR)
        } finally {
            onFetchingDone()
        }


        /*try {
            val response = api.getDives(useToken())
            if (response.isSuccessful){
                val list = response.body()?.data
                list?.let{
                    database.deleteAll()
                    database.insertAll(list.asDatabaseModel())
                    return Result.EmptySuccess
                } ?: return Result.Failure(ErrorCases.GENERAL_ERROR)// TODO ?: response was empty, what should we do?
            } else {
                return when (response.code()){
                    401 -> Result.Failure(ErrorCases.GENERAL_UNAUTHORIZED)
                    else -> Result.Failure(ErrorCases.GENERAL_ERROR)
                }
            }
        } catch (e: UnknownHostException) {
            return Result.Failure(ErrorCases.NO_INTERNET_CONNECTION)
        } catch (e: Exception) {
            return Result.Failure(ErrorCases.GENERAL_ERROR)
        }
        finally {
            onFetchingDone()
        }*/
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
    ) {
        uploadStart()
        if (imageUri != null) {
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
        return suspendCoroutine { continuation ->
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
    private suspend fun decideUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean
    ): Result<Any> {
        uploadStart()
        return if (createNewEntry) {
            var res = createDiveRemote(diveLogEntry)
            res = forceUpdate()
            uploadDone()
            res
        } else {
            updateDiveRemote(diveLogEntry)
            // When the dive entry does not exist, it will be created instead with the id the app sends
            // This could cause a problem when the dive was deleted in the meantime
            getSingleDiveFromRemote(diveLogEntry.dataBaseId)
            uploadDone()
            Result.EmptySuccess
        }
    }

    private fun uploadStart() {
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.INDETERMINATE_UPLOAD
        )
    }

    private fun mediaUploadProgress(progress: Long, total: Long) {
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.PROGRESS_UPLOAD,
            progress = progress,
            total = total
        )
    }

    private fun mediaUploadDone() {
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.INDETERMINATE_UPLOAD,
            progress = 0,
            total = 0
        )
    }

    private fun uploadDone() {
        _uploadApiStatus.value = RepositoryUploadProgressStatus(
            status = RepositoryUploadStatus.DONE
        )
    }

    private suspend fun updateDiveRemote(entry: DiveLogEntry) {
        try {
            api.updateDive(entry.asNetworkModel(), useToken())
        } catch (e: Exception) {
            onUploadErrorOccured(e)
        }
    }

    // Creates a single dive on the server. API does not respond the id, so all entities need to be refetched
    // TODO WIP IMPLEMENTATION
    private suspend fun createDiveRemote(entry: DiveLogEntry): Result<Any> {
        return safeCall { token ->
            api.createDive(entry.asNetworkModel(), token)
        }
        /*try {
            val response = api.createDive(entry.asNetworkModel(), useToken())
            if (!response.isSuccessful && response.code() == 401) {
                //unauthorizedCallDetected()
            }
        } catch (e: Exception) {
            onUploadErrorOccured(e)
        }*/
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

    override suspend fun cleanLogout() {
        authToken = null
        database.deleteAll()
    }

    private fun onDownloadErrorOccured(e: Exception) {
        _apiError.value = e
        _downloadApiStatus.value = RepositoryDownloadStatus.ERROR
    }

    private fun onUploadErrorOccured(e: Exception) {
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
