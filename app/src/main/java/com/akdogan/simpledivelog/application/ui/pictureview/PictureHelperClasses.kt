package com.akdogan.simpledivelog.application.ui.pictureview

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel


abstract class PictureFragmentViewModel: ViewModel(){
    /**
     * Should be set to true as soon as data has been loaded and remoteImgUrl is available
     */
    abstract val loadRemotePicture : LiveData<Boolean>

    /**
     * the remote url of the image to load
     */
    abstract val remoteImgUrl: String?

    /**
     * the content url of the local image. Used when a new image is loaded from the device
     * Not required to interact with when Picturefragment is in read only mode
     */
    open var contentUri: Uri? = null

    /**
     * When set to false, controls for taking picture from camera or gallery are displayed
     */
    abstract val readOnlyMode: Boolean
}

