package com.akdogan.simpledivelog.application.ui.pictureview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.diveutil.Constants.FILE_NAME_DATE_FORMAT
import com.akdogan.simpledivelog.diveutil.Constants.FILE_NAME_EXTENSION_JPG
import com.akdogan.simpledivelog.diveutil.Constants.IMAGE_TYPE_PREFIX
import com.akdogan.simpledivelog.diveutil.Constants.SHARED_VIEW_MODEL_TAG
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class PictureFragment : Fragment() {

    private var cameraTempUri: Uri? = null

    private lateinit var imageView: ImageView

    private lateinit var viewModel: PictureFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewModel = ViewModelProvider(requireParentFragment()).get(
            SHARED_VIEW_MODEL_TAG,
            PictureFragmentViewModel::class.java
        )
        // TODO catch Runtime exception when viewModel cannot be instantiated

        return inflater.inflate(R.layout.fragment_picture, container, false)
    }

    private fun setupControls(){
        val openGalleryButton = view?.findViewById<FloatingActionButton>(R.id.open_gallery_button)
        val takePictureButton = view?.findViewById<FloatingActionButton>(R.id.take_picture_button)
        if (viewModel.readOnlyMode){
            openGalleryButton?.visibility = View.GONE
            takePictureButton?.visibility = View.GONE
        } else {
            openGalleryButton?.setOnClickListener {
                mRequestPermissionGallery()
            }

            takePictureButton?.setOnClickListener {
                    if (checkHardwareAvailable()) {
                        mRequestPermissionCamera()
                    }
                }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupControls()
        imageView = view.findViewById(R.id.picture_fragment_image_view)

        imageView.setOnClickListener {
            goFullScreen()
        }

        viewModel.loadRemotePicture.observe(viewLifecycleOwner, { load ->

            if (load) {
                viewModel.remoteImgUrl?.let {
                    Glide.with(requireContext())
                        .load(it.toUri())
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.loading_animation).apply {
                                    imageView.placeholding()
                                }
                                .fallback(R.drawable.ic_baseline_no_photography_24).apply {
                                    imageView.placeholding()
                                }
                        )
                        .into(imageView).apply {
                            imageView.loaded()
                        }
                }
            }
        })
    }

    private fun myDialog(title: String, text: String, settingsButton: Boolean = false) {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(text)
        if (settingsButton) {
            builder.setPositiveButton(
                getString(R.string.picture_fragment_dialog_positive_button_settings)
            ) { _, _ ->
                val i = Intent()
                i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                i.addCategory(Intent.CATEGORY_DEFAULT)
                i.data = Uri.parse("package:" + requireContext().packageName)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                requireContext().startActivity(i)

            }
            builder.setNegativeButton(android.R.string.cancel, null)
        } else {
            builder.setPositiveButton(android.R.string.ok, null)
        }
        builder.show()
    }

    private fun goFullScreen() {
        fun launch(uri: Uri) {
            val intent = Intent(requireContext(), PictureFullscreenActivity::class.java).apply {
                putExtra(BUNDLE_IMAGE_URI_NAME, uri)
            }
            startActivity(intent)
        }
        viewModel.remoteImgUrl?.let{
            launch(it.toUri())
        } ?: viewModel.contentUri?.let{
            launch(it)
        }

    }

    private fun checkHardwareAvailable(): Boolean =
        requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

    private fun mRequestPermissionGallery() {
        val requiredPermission = Manifest.permission.READ_EXTERNAL_STORAGE
        when {
            hasPermission(requiredPermission) -> openGallery()
            shouldShowRequestPermissionRationale(requiredPermission) -> showGalleryRationale()
            else -> requestPermissions(arrayOf(requiredPermission), REQUEST_GALLERY_IMAGE)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun openGallery() {
        val galleryIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = IMAGE_TYPE_PREFIX
            }
        try {
            startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE)
        } catch (e: ActivityNotFoundException) {
            // no suitable activity
        }
    }

    private fun showGalleryRationale() =
        myDialog(
            getString(R.string.picture_fragment_gallery_dialog_title),
            getString(R.string.picture_fragment_gallery_dialog_rationale_body),
            true
        )

    private fun showCameraRationale() = myDialog(
        getString(R.string.picture_fragment_camera_dialog_title),
        getString(R.string.picture_fragment_camera_dialog_rationale_body),
        true
    )

    private fun showGalleryDenied() = myDialog(
        getString(R.string.picture_fragment_gallery_dialog_title),
        getString(R.string.picture_fragment_dialog_denied_body),
        true
    )

    private fun showCameraDenied() = myDialog(
        getString(R.string.picture_fragment_camera_dialog_title),
        getString(R.string.picture_fragment_dialog_denied_body),
        true
    )


    private fun mRequestPermissionCamera() {
        // Let the system handle the request code. From androidx.activity 1.2.0 on, currently in RC: https://developer.android.com/training/permissions/requesting
        val requiredPermission = Manifest.permission.CAMERA
        when {
            hasPermission(requiredPermission) -> dispatchTakePictureIntent()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showCameraRationale()
            else -> requestPermissions(arrayOf(requiredPermission), REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    showCameraDenied()
                }
            }
            REQUEST_GALLERY_IMAGE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    showGalleryDenied()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val photoFile = createImageFile()
            val photoUri: Uri? =
                FileProvider.getUriForFile(
                    requireContext(),
                    PROVIDER_AUTHORITY,
                    photoFile
                )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraTempUri = photoUri
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: IOException) {
            // For creating file --> Error Handling
        } catch (e: IllegalArgumentException) {
            // for getUriForFile --> Error Handling
        } catch (e: ActivityNotFoundException) {
            // for No suitable Activity --> Error Handling
        }
    }

    // TODO: Maybe move Fileinteraction into its own repository
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    viewModel.contentUri = cameraTempUri
                    cameraTempUri = null
                }
                REQUEST_GALLERY_IMAGE -> {
                    val uri = data?.data
                    uri?.let {
                        // Try catch
                        val targetFile = createImageFile()
                        copyFileFromUri(it, targetFile)
                        val photoUri: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            PROVIDER_AUTHORITY,
                            targetFile
                        )
                        if (fileIsLegit(uri)) {
                            viewModel.contentUri = photoUri
                        }
                    }
                }
            }
            if (viewModel.contentUri != null) {
                view?.findViewById<ImageView>(R.id.picture_fragment_image_view)?.let {
                    it.scaleType = ImageView.ScaleType.CENTER_CROP
                    it.setPadding(0)
                    it.setImageURI(viewModel.contentUri)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun copyFileFromUri(uri: Uri, target: File): Boolean {
        val resolver = requireContext().contentResolver
        try {
            resolver.openInputStream(uri).use {
                it?.let {
                    val bufferSize = 4 * 1024
                    BufferedOutputStream(
                        FileOutputStream(target),
                        bufferSize
                    ).use { outStream ->
                        for (b in it.buffered(bufferSize)) {
                            outStream.write(b.toInt())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun fileIsLegit(uri: Uri?): Boolean {
        if (uri != null) {
            val resolver = requireContext().contentResolver
            resolver.openInputStream(uri).use {
                if ((it?.read(ByteArray(1)) ?: -1) >= 0) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Creates an Empty Image File in the Apps Cache Directory and returns it.
     * Files naming convention: "JPEG_yyyyMMdd_HHmmss.jpg"
     *
     * @exception IOException if the File cannot be created
     */
    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat(FILE_NAME_DATE_FORMAT).format(Date())
        val storageDir: File? =
            context?.cacheDir
        try {
            return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                FILE_NAME_EXTENSION_JPG, /* suffix */
                storageDir /* directory */
            )
        } catch (e: Exception) {
            throw e
        }
    }


    companion object {
        const val REQUEST_IMAGE_CAPTURE = 12342
        const val REQUEST_GALLERY_IMAGE = 13245
        const val PROVIDER_AUTHORITY = "com.akdogan.simpledivelog.fileprovider"
        const val BUNDLE_IMAGE_URI_NAME = "imageUri"
    }

    private fun ImageView.loaded() {
        this.scaleType = ImageView.ScaleType.CENTER_CROP
        this.setPadding(0)
    }

    private fun ImageView.placeholding() {
        this.scaleType = ImageView.ScaleType.FIT_CENTER
        this.setPadding(70)
    }
}