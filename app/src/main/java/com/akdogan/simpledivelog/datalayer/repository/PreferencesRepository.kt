package com.akdogan.simpledivelog.datalayer.repository

import android.content.SharedPreferences
import android.util.Log

interface PreferencesRepository {

    fun getCredentials() : String?

    fun saveCredentials(token: String): Boolean

    fun purgeCredentials(): Boolean

}

class DefaultPreferencesRepository(
    private val prefs: SharedPreferences
) : PreferencesRepository{

    companion object {
        private const val AUTH_TOKEN = "auth_token"
    }
    override fun getCredentials(): String? {
        return prefs.getString(AUTH_TOKEN, null)
    }

    override fun saveCredentials(token: String): Boolean {
        return prefs.edit().putString(AUTH_TOKEN, token).commit()
    }

    override fun purgeCredentials(): Boolean {
        val result = prefs.edit().remove(AUTH_TOKEN).commit()
        Log.i("LOGOUT_TRACING", "commit done with result: $result")
        return result
    }

}