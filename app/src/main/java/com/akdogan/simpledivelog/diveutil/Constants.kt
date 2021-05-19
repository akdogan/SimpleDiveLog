package com.akdogan.simpledivelog.diveutil

object Constants{
    // Shared Preferences Keys
    const val PREF_DEPTH_UNIT_KEY = "pref_depth_unit_key"
    const val PREF_DEPTH_UNIT_DEFAULT = false
    const val PREF_PRESSURE_UNIT_KEY = "pref_pressure_unit_key"
    const val PREF_PRESSURE_UNIT_DEAFULT = false

    const val CACHE_CLEANUP_WORKER_FILENAME_KEY = "filename_key"

    // used to communicate from LaunchActivity to MainActivity
    const val LOGIN_VERIFIED_KEY = "login_verified_key"
    const val LOGIN_SUCCESS = 11
    const val LOGIN_UNVERIFIED = 22
    const val LOGIN_DEFAULT_VALUE = -1

    // used to communicate from LoginActivity to MainActivity
    const val NEW_REGISTERED_USER_KEY = "new_registered_user_key"
    const val CREATE_SAMPLE_DATA = "createSampleData"

    // Auth Template
    const val AUTH_TEMPLATE = "Basic "

    // Username and Password
    const val USERNAME_MIN_LENGTH = 6
    const val USERNAME_PATTERN = "^[\\w][\\w|\\-+_]{${USERNAME_MIN_LENGTH-1},}$"//"""(^[0-9|a-z])([0-9|a-z|\-+_]+)"""//+($[\w])"""
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_PATTERN = "^[\\w|\\-+$!_*]{${PASSWORD_MIN_LENGTH},}$"
}