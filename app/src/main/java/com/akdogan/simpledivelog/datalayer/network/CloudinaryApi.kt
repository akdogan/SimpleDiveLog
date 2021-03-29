package com.akdogan.simpledivelog.datalayer.network

import android.content.Context
import android.net.Uri
import com.akdogan.simpledivelog.datalayer.repository.ClUploaderCallback
import com.cloudinary.android.LogLevel
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

// TODO Change to instance, as the repository is already a Singleton and the only entity using this
object CloudinaryApi {
    private var initialized = false
    private val apiConfig: HashMap<String, String> = hashMapOf("cloud_name" to "dcftx5e2")
    private const val uploadPreset = "wpzagd1i"


    fun setup(con: Context) {
        if (!initialized) {
            MediaManager.init(con, apiConfig)
            MediaManager.setLogLevel(LogLevel.DEBUG)
            initialized = true
        }
    }


    fun uploadPicture(
        uri: Uri,
        cbImp: ClUploaderCallback
    ) {
        val pictureUploadCallback = object: UploadCallback{
            override fun onStart(requestId: String?) {
                cbImp.clOnStart()
            }

            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                cbImp.clOnProgress(bytes, totalBytes)
            }
            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                val imgUrl = resultData?.get("secure_url").toString()
                cbImp.clOnSuccess(imgUrl)
            }

            override fun onError(requestId: String?, error: ErrorInfo?) {
                cbImp.clOnError()

            }

            override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                cbImp.clOnReschedule()
                // If a reschedule happens, the request is canceled for now instead as there is no
                // appropriate handling for this case in the application layer
                MediaManager.get().cancelRequest(requestId)
            }}

        MediaManager.get().upload(uri)
            .unsigned(uploadPreset)
            .callback(pictureUploadCallback)
            .dispatch()
    }
}

fun getThumbnailFromImageUrl(url: String?): String? {
    url?.let {
        val parameters = "c_thumb,g_auto:classic,ar_1:1,w_200/"
        val delimiter = "upload/"
        val missingDelimiter = "INVALID"
        val first = it.substringBefore(delimiter, missingDelimiter)
        val second = it.substringAfter(delimiter, missingDelimiter)
        return if (first == missingDelimiter || second == missingDelimiter) {
            null
        } else {
            first + delimiter + parameters + second
        }
    } ?: return null
}