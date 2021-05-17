package com.akdogan.simpledivelog.application.launcher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.application.ui.loginview.LoginViewActivity
import com.akdogan.simpledivelog.datalayer.LoginStatus
import com.akdogan.simpledivelog.datalayer.repository.DefaultAuthRepository
import com.akdogan.simpledivelog.datalayer.repository.DefaultPreferencesRepository
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_SUCCESS
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_UNVERIFIED
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_VERIFIED_KEY

class LaunchActivity : AppCompatActivity() {

    private val viewModel: LaunchViewModel by viewModels {
        LaunchViewModelFactory(
            DefaultAuthRepository(),
            DefaultPreferencesRepository(PreferenceManager.getDefaultSharedPreferences(this))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO show a toast on errors?
        viewModel.userIsLoggedIn.observe(this) {
            Log.i("LAUNCH_ACTIVITY_TRACING", "LaunchActivity observer called with $it")
            it?.let {
                when (it){
                    LoginStatus.SUCCESS -> startApp(LOGIN_SUCCESS)
                    LoginStatus.UNVERIFIED -> startApp(LOGIN_UNVERIFIED)
                    LoginStatus.FAILED -> startLogin()
                }
                viewModel.userIsLoggedInDone()
            }
        }
        viewModel.validateLogin()

    }

    private fun startLogin() {
        // Login is triggered
        val intent = Intent(this, LoginViewActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startApp(verified: Int) {
        // App is started, information about the login being verified or not passed to main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(LOGIN_VERIFIED_KEY, verified)
        startActivity(intent)
        finish()
    }
}