package com.akdogan.simpledivelog.application

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akdogan.simpledivelog.datalayer.repository.Repository
import com.akdogan.simpledivelog.datalayer.repository.RepositoryUploadStatus

class CleanupCacheWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val status = Repository.uploadApiStatus.value?.status
        val avoid = listOf(
            RepositoryUploadStatus.PROGRESS_UPLOAD,
            RepositoryUploadStatus.INDETERMINATE_UPLOAD
        )
        val imageUriInput = inputData.getString("WORKER_TEST") ?: return Result.failure()
        Log.i("WORKER", "Repo Upload Status is $status")
        if (status !in avoid){
            Log.i("WORKER", "Task would be executed now with uri $imageUriInput")
            return Result.success()
        } else {
            Log.i("WORKER", "Api is busy, retrying with uri $imageUriInput")
            return Result.retry()
        }
    }
}