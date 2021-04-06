package com.akdogan.simpledivelog.application

import android.app.Application

class SimpleDiveLogApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.setupDefaultRepository(applicationContext)
    }
}