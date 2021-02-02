package com.akdogan.simpledivelog.application

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akdogan.simpledivelog.diveutil.Constants
import java.io.File
import java.io.IOException

class CleanupCacheWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val filename = inputData.getString(Constants.CACHE_CLEANUP_WORKER_FILENAME_KEY) ?: return Result.failure()
        val cacheFile = File(applicationContext.cacheDir, filename )
        return try {
            if (cacheFile.delete()){
                Log.i("WORKER_TESTS", "File ${cacheFile.name} deleted Successfully")
                Result.success()
            } else {
                Log.i("WORKER_TESTS", "Deletion failed")
                Result.failure()
            }
        } catch (e: IOException){
            Log.i("WORKER_TESTS", "catch block called with IOException: $e")
            Result.failure()
        } catch (e: SecurityException){
            Log.i("WORKER_TESTS", "catch block called with SecurityException: $e")
            Result.failure()
        }
    }
}
