package com.akdogan.simpledivelog.application.ui.loginview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.repository.DefaultAuthRepository
import com.akdogan.simpledivelog.datalayer.repository.DefaultPreferencesRepository
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_SUCCESS
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_VERIFIED_KEY
import com.google.android.material.textfield.TextInputLayout

class LoginViewActivity : AppCompatActivity() {
    private lateinit var userNameField: TextInputLayout
    private lateinit var passwordField: TextInputLayout
    private lateinit var passwordRepeatField: TextInputLayout
    private lateinit var loginRegisterButton: Button
    private lateinit var loginRegisterSwitch: Button
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            DefaultAuthRepository(),
            DefaultPreferencesRepository(PreferenceManager.getDefaultSharedPreferences(this))
        )
    }

    private var toast: Toast? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_view)

        userNameField = findViewById(R.id.login_username)
        passwordField = findViewById(R.id.login_password)
        passwordRepeatField = findViewById(R.id.login_password_repeat)
        loginRegisterButton = findViewById(R.id.register_login_button)
        loginRegisterSwitch = findViewById(R.id.register_login_switch)

        loginRegisterSwitch.setOnClickListener { toggleLoginRegister() }
        loginRegisterButton.setOnClickListener { loginOrRegister() }

        viewModel.makeToast.observe(this) { code ->
            code?.let {
                makeToast(ErrorCases.getMessage(resources, it))
                viewModel.makeToastDone()
            }
        }

        viewModel.loginStatus.observe(this) { userIsLoggedIn: Boolean? ->
            Log.i("LOGIN_TEST", "Login Activity observer called with: $userIsLoggedIn")
            if (userIsLoggedIn == true) {
                proceed()
                viewModel.loginDone()
            }
        }
    }

    private fun proceed() {
        val intent = Intent(this, MainActivity::class.java)
        // Login is verified, only possible to login from here by checking against the server
        intent.putExtra(LOGIN_VERIFIED_KEY, LOGIN_SUCCESS)
        startActivity(intent)
        finish()
    }

    private fun loginOrRegister() {
        val username = userNameField.editText?.text.toString()
        val pwd = passwordField.editText?.text.toString()
        viewModel.startRequest(username, pwd)
    }

    private fun toggleLoginRegister() {
        if (viewModel.useLogin()) {
            showWarningDialog()

        } else {
            setLoginView()
        }
        viewModel.toggleLogin()
    }

    private fun setLoginView() {
        passwordRepeatField.visibility = View.GONE
        loginRegisterButton.text = "Login"
        loginRegisterSwitch.text = "Register instead"
    }

    private fun setRegisterView() {
        //passwordRepeatField.visibility = View.VISIBLE
        loginRegisterButton.text = "Register"
        loginRegisterSwitch.text = "Login instead"
    }

    private fun showWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle(android.R.string.dialog_alert_title)
            .setMessage(
                "This is an example app.\nTraffic is plaintext and not secured.\n\nPlease do " +
                        "NOT use any real passwords"
            )
            .setPositiveButton(android.R.string.ok) { _, _ ->
                setRegisterView()
            }
            .create()
            .show()
    }

    private fun makeToast(msg: String) {
        toast?.cancel()
        val t = Toast.makeText(this, msg, Toast.LENGTH_LONG)
        t.show()
        toast = t
    }

}