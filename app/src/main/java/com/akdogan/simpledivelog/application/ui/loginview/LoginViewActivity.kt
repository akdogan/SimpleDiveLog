package com.akdogan.simpledivelog.application.ui.loginview

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.databinding.ActivityLoginViewBinding
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.repository.DefaultAuthRepository
import com.akdogan.simpledivelog.datalayer.repository.DefaultPreferencesRepository
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_SUCCESS
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_VERIFIED_KEY
import com.akdogan.simpledivelog.diveutil.Constants.NEW_REGISTERED_USER_KEY

class LoginViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginViewBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            DefaultAuthRepository(),
            DefaultPreferencesRepository(PreferenceManager.getDefaultSharedPreferences(this))
        )
    }

    private var toast: Toast? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_login_view
        )

        binding.registerLoginSwitch.setOnClickListener { toggleLoginRegister() }
        binding.registerLoginButton.setOnClickListener { loginOrRegister() }

        viewModel.makeToast.observe(this) { code ->
            code?.let {
                makeToast(ErrorCases.getMessage(resources, it))
                viewModel.makeToastDone()
            }
        }

        viewModel.loginStatus.observe(this) { userIsLoggedIn: Boolean? ->
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
        intent.putExtra(NEW_REGISTERED_USER_KEY, viewModel.isUserRegistering())
        startActivity(intent)
        finish()
    }

    private fun loginOrRegister() {
        val username = binding.loginUsername.editText?.text.toString()
        val pwd = binding.loginPassword.editText?.text.toString()
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
        binding.loginPasswordRepeat.visibility = View.GONE
        binding.registerLoginButton.text = getString(R.string.login_view_login_state_action_button)
        binding.registerLoginSwitch.text = getString(R.string.login_view_login_state_switch_button)
    }

    private fun setRegisterView() {
        //passwordRepeatField.visibility = View.VISIBLE
        binding.registerLoginButton.text = getString(R.string.login_view_register_state_action_button)
        binding.registerLoginSwitch.text = getString(R.string.login_view_login_state_switch_button)
    }

    private fun showWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.login_view_dialog_title))
            .setMessage(getString(R.string.login_view_dialog_message))
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