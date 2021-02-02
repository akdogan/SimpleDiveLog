package com.akdogan.simpledivelog.application

import android.app.Application

class DiveLogApplication : Application() {
    override fun onCreate() {
        // TODO setup the singleton here instead of Main Activity
        // Mayb trigger fetching from database here as well
        super.onCreate()
    }
}