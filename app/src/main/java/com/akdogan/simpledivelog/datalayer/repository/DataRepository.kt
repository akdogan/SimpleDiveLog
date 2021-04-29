package com.akdogan.simpledivelog.datalayer.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.Result

interface DataRepository {

    val networkAvailable: LiveData<Boolean>

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

    suspend fun getSingleDive(diveId: String): Result<DiveLogEntry>

    suspend fun startUpload(
        diveLogEntry: DiveLogEntry,
        createNewEntry: Boolean,
        imageUri: Uri? = null
    ): Result<Any>

    suspend fun deleteDive(diveId: String): Result<Any>

    suspend fun deleteAll(): Result<Any>

    suspend fun cleanLogout()
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
