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
import com.akdogan.simpledivelog.datalayer.ErrorCases.DATABASE_ERROR
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_ERROR
import com.akdogan.simpledivelog.datalayer.ErrorCases.GENERAL_UNAUTHORIZED
import com.akdogan.simpledivelog.datalayer.ErrorCases.ITEM_NOT_FOUND
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

class DefaultDataRepository private constructor(
    context: Context,
    private val api: RemoteApi,
    private val database: DiveLogDatabaseDao
) : DataRepository {

    companion object {
        private const val TAG = "REPO_V2"

        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getDefaultRepository(
            context: Context,
            api: RemoteApi = DefaultApi(),
            database: DiveLogDatabaseDao? = null
        ): DataRepository {
            val db = database ?: DiveLogDatabase.getInstance(context).diveLogDatabaseDao
            return INSTANCE ?: synchronized(this) {
                DefaultDataRepository(context, api, db).also {
                    INSTANCE = it
                }
            }
        }
    }

    private var authToken: String? = null

    private fun useToken(): String =
        authToken ?: throw IllegalStateException("Auth Token has not been setup yet")

    private val _networkAvailable = MutableLiveData<Boolean>()
    override val networkAvailable: LiveData<Boolean>
        get() = _networkAvailable

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
        Log.i(TAG, "safeCall was triggered")
        try {
            val response = apiCall.invoke(useToken())

            return if (response.isSuccessful) {
                Log.i(TAG, "SafeCall Response Success: ${response.code()}, ${response.body()}")

                val body: T = response.body()?.data ?: return Result.EmptySuccess
                Result.Success(body)
            } else {
                Log.i(TAG, "SafeCall Response Failure: ${response.code()}, ${response.body()}")
                when (response.code()) {
                    401 -> Result.Failure(GENERAL_UNAUTHORIZED)
                    404 -> Result.Failure(ITEM_NOT_FOUND)
                    500 -> Result.Failure(SERVER_ERROR)
                    else -> Result.Failure(GENERAL_ERROR)
                }
            }
        } catch (e: UnknownHostException) {
            return Result.Failure(ErrorCases.NO_INTERNET_CONNECTION)
        } catch (e: Exception) {
            Log.i(TAG, "catch general e called with $e")
            return Result.Failure(GENERAL_ERROR)
        }
    }

    override fun onNetworkAvailable() = _networkAvailable.postValue(true)
    override fun onNetworkLost() = _networkAvailable.postValue(false)
    override suspend fun forceUpdate(): Result<Any> = fetchDives()

    override fun getLatestDiveNumber(): Int =
        _listOfDives.value?.maxByOrNull { it.diveNumber }?.diveNumber ?: 0

    override fun setAuthToken(token: String) {
        authToken = token
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
    }

    // Fetched from remote into the database, then fetched from database
    override suspend fun getSingleDive(diveId: String): Result<DiveLogEntry> {
        onFetching()
        // Delay for debugging purpose so we can actually see the loading animation
        delay(1000)
        var entry = getSingleDiveFromDataBase(diveId)
        if (entry == null) {
            val result = getSingleDiveFromRemote(diveId)
            if (result is Result.Failure){
                return result
            }
            entry = getSingleDiveFromDataBase(diveId)
            if (entry == null) {
                onFetchingDone()
                return result
            }
        }
        onFetchingDone()
        return Result.Success(entry)
    }


    // Starts creation of new entry. If there is a imgUri, image upload is done first
    override suspend fun startUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean,
        imageUri: Uri?
    ): Result<Any> {
        uploadStart()
        //delay(4000)
        Log.i(TAG, "Start upload called with create new: $createNewEntry")
        return if (imageUri != null) {
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
    // TODO When there is no proper internet connection, the app just hangs..
    private suspend fun decideUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean
    ): Result<Any> {
        uploadStart()
        return if (createNewEntry) {
            //TODO Get remote id from the result and only fetch that item into the database
            var res = createDiveRemote(diveLogEntry)
            if (res !is Result.Failure) {
                res = forceUpdate()
            }
            uploadDone()
            res
        } else {
            var res = updateDiveRemote(diveLogEntry)
            // When the dive entry does not exist, it will be created instead with the id the app sends
            // This could cause a problem when the dive was deleted in the meantime
            if (res !is Result.Failure) {
                res = getSingleDiveFromRemote(diveLogEntry.dataBaseId)
            }
            uploadDone()
            res
        }
    }

    private suspend fun updateDiveRemote(entry: DiveLogEntry): Result<Any> = safeCall { token ->
        api.updateDive(entry.asNetworkModel(), token)
    }

    // Creates a single dive on the server.
    // API does not respond the id, so all entities need to be refetched
    private suspend fun createDiveRemote(entry: DiveLogEntry): Result<Any> = safeCall { token ->
        api.createDive(entry.asNetworkModel(), token)
    }

    // Tries to fetch a single dive from remote and puts in the cache.
    private suspend fun getSingleDiveFromRemote(diveId: String): Result<DiveLogEntry> {
        val result = safeCall { token ->
            api.getSingleDive(diveId, token)
        }
        Log.i(TAG, "getSingleDiveFromRemote() called with result: $result for diveId: $diveId")
        return try {
            if (result is Result.Success) {
                val remoteEntry = result.body.asDataBaseModel()
                if (database.checkIfEntryExists(remoteEntry.dataBaseId) == 1) {
                    database.update(remoteEntry)
                } else {
                    database.insert(remoteEntry)
                }
                Result.Success(remoteEntry.asDomainModel())
            } else {
                // Cast to Failure; Api never returns type EmptySuccess
                val error = (result as Result.Failure).errorCode
                Result.Failure(error)
            }
        } catch (e: Exception) {
            Log.i(TAG, "Database Exception: $e")
            Result.Failure(DATABASE_ERROR)
        } finally {
            onFetchingDone()
        }
    }

    // Tries to fetch a single dive from the cache and returns it, or returns null if no such element exists
    private suspend fun getSingleDiveFromDataBase(diveId: String): DiveLogEntry? =
        database.get(diveId)?.asDomainModel()

    override suspend fun deleteDive(diveId: String): Result<Any> {
        uploadStart()
        val result = safeCall { token ->
            api.delete(diveId, token)
        }
        if (result !is Result.Failure){
            fetchDives()
        }
        Log.i("REPO_V2_DELETE", "DeleteDive called with result: $result")
        uploadDone()
        return result
    }

    override suspend fun deleteAll(): Result<Any> {
        uploadStart()
        val result = safeCall { token ->
            api.deleteAll(token)
        }
        // TODO API returns an empty array instead of enpty json on success.
        // Until fixed, this is a workaround
        /*if (result !is Result.Failure) {
            fetchDives()
        }*/
        fetchDives()
        uploadDone()
        return result
    }

    override suspend fun cleanLogout() {
        authToken = null
        database.deleteAll()
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

}

