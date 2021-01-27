package com.akdogan.simpledivelog.application.editview

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.akdogan.simpledivelog.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class PictureFullscreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_picture)
        val imageUri = intent.getParcelableExtra<Uri>(BUNDLE_IMAGE_URI_NAME)
        val imageView = findViewById<ImageView>(R.id.full_screen_image_View)

        when (imageUri?.scheme){
            LOCAL_SCHEME -> imageView.setImageURI(imageUri)
            REMOTE_SCHEME -> {
                Glide.with(applicationContext)
                    .load(imageUri)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.loading_animation)
                            .fallback(R.drawable.ic_baseline_no_photography_24)
                    )
                    .into(imageView)
            }
            else -> this.onBackPressed()
        }
        imageView.setOnClickListener {
            this.onBackPressed()
        }
    }

    companion object{
        const val BUNDLE_IMAGE_URI_NAME = "imageUri"
        const val LOCAL_SCHEME = "content"
        const val REMOTE_SCHEME = "https"
    }


}