package com.akdogan.simpledivelog.application.ui.loginview

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.application.ui.loginview.TextInputState.*
import com.akdogan.simpledivelog.databinding.ActivityLoginViewBinding
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.repository.DefaultAuthRepository
import com.akdogan.simpledivelog.datalayer.repository.DefaultPreferencesRepository
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_SUCCESS
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_VERIFIED_KEY
import com.akdogan.simpledivelog.diveutil.Constants.NEW_REGISTERED_USER_KEY
import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_ALLOWED_CHARS
import com.akdogan.simpledivelog.diveutil.Constants.PASSWORD_MIN_LENGTH
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_ALLOWED_CHARS
import com.akdogan.simpledivelog.diveutil.Constants.USERNAME_MIN_LENGTH
import com.google.android.material.textfield.TextInputLayout


class LoginViewActivity : AppCompatActivity() {

    companion object {
        // TODO: Add to theme and retrieve via function
        const val ERROR_COLOR = R.color.design_default_color_error
        const val WARNING_COLOR = R.color.error_yellow
        const val VALID_COLOR = R.color.valid_green
    }

    @ColorInt
    fun getDefaultColor(): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }

    // Workaround: When Textinputlayout errorcolor changes, it needs to be refreshed
    // Otherwise the hint color will not change immediately
    private class ErrorStateHolder(
        val emptyHelperText: String,
        val tooShortHelperText: String,
        val invalidHelperText: String,
        var state: TextInputState = Empty
    )

    private lateinit var binding: ActivityLoginViewBinding
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            DefaultAuthRepository(),
            DefaultPreferencesRepository(PreferenceManager.getDefaultSharedPreferences(this))
        )
    }

    private lateinit var usernameErrorState: ErrorStateHolder
    private lateinit var passwordErrorState: ErrorStateHolder
    private lateinit var passwordRepErrorState: ErrorStateHolder

    private fun getEmptyHelperText(num: Int, allowedChars: String): String {
        return getString(
            R.string.login_view_helper_template,
            getString(R.string.login_view_helper_min_chars, num),
            getString(R.string.login_view_helper_allowed_chars, allowedChars)
        )
    }

    private fun getTooShortHelperText(num: Int): String {
        return getString(
            R.string.login_view_helper_template,
            getString(R.string.login_view_error_too_short),
            getString(R.string.login_view_helper_min_chars, num)
        )
    }

    private fun getInvalidHelperText(allowedChars: String): String {
        return getString(
            R.string.login_view_helper_template,
            getString(R.string.login_view_error_invalid_char),
            getString(R.string.login_view_helper_allowed_chars, allowedChars)
        )
    }

    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_login_view
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.registerLoginSwitch.setOnClickListener { toggleLoginRegister() }
        binding.registerLoginButton.setOnClickListener { viewModel.startRequest() }

        setupViews()

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

        viewModel.enableActionButton.observe(this) { enable: Boolean? ->
            enable?.let {
                binding.registerLoginButton.isEnabled = it
            }
        }

        viewModel.usernameState.observe(this) {
            binding.loginUsername.handleError(it, usernameErrorState)
        }

        viewModel.passwordState.observe(this) {
            binding.loginPassword.handleError(it, passwordErrorState)
        }

        viewModel.passwordRepeatState.observe(this) {
            binding.loginPasswordRepeat.handleError(it, passwordRepErrorState)
        }
    }


    private fun setupViews(){
        val usernameText = getEmptyHelperText(USERNAME_MIN_LENGTH, USERNAME_ALLOWED_CHARS)
        val passwordText = getEmptyHelperText(PASSWORD_MIN_LENGTH, PASSWORD_ALLOWED_CHARS)

        val usernameTooShort = getTooShortHelperText(USERNAME_MIN_LENGTH)
        val passwordTooShort = getTooShortHelperText(PASSWORD_MIN_LENGTH)

        val usernameInvalid = getInvalidHelperText(USERNAME_ALLOWED_CHARS)
        val passwordInvalid = getInvalidHelperText(PASSWORD_ALLOWED_CHARS)

        usernameErrorState = ErrorStateHolder(
            emptyHelperText = usernameText,
            tooShortHelperText = usernameTooShort,
            invalidHelperText = usernameInvalid
        )
        passwordErrorState = ErrorStateHolder(
            emptyHelperText = passwordText,
            tooShortHelperText = passwordTooShort,
            invalidHelperText = passwordInvalid
        )
        passwordRepErrorState = ErrorStateHolder(
            emptyHelperText = passwordText,
            tooShortHelperText = passwordTooShort,
            invalidHelperText = passwordInvalid
        )

        binding.loginUsername.setEmptyState(usernameText)
        binding.loginPassword.setEmptyState(passwordText)
        binding.loginPasswordRepeat.setEmptyState(passwordText)
    }


    private fun TextInputLayout.handleError(
        errorCase: TextInputState?,
        errorState: ErrorStateHolder
    ) {
        if (errorCase != errorState.state) {
            errorState.state = errorCase ?: Empty
            errorCase?.let {
                when (errorCase) {
                    Empty -> this.setEmptyState(errorState.emptyHelperText)
                    InvalidCharacter -> this.setErrorState(
                        errorState.invalidHelperText,
                        ERROR_COLOR,
                    )
                    TooShort -> this.setErrorState(
                        errorState.tooShortHelperText,
                        WARNING_COLOR,
                    )
                    DoesNotMatch -> this.setErrorState(
                        getString(R.string.login_view_error_no_match),
                        ERROR_COLOR,
                    )
                    Valid -> this.setValidState()
                }
            }
        }
    }

    private fun TextInputLayout.setEmptyState(
        helperText: String
    ) {
        val colorStateList = ColorStateList.valueOf(getDefaultColor())
        this.endIconMode = TextInputLayout.END_ICON_NONE
        setBoxStrokeColorStateList(colorStateList)
        this.hintTextColor = colorStateList
        this.helperText = helperText
        this.error = null
    }

    private fun TextInputLayout.setValidState() {
        val colorStateList = ColorStateList.valueOf(getColor(VALID_COLOR))
        this.endIconDrawable =
            ContextCompat.getDrawable(this.context, R.drawable.ic_baseline_check_circle_24)
        this.endIconMode = TextInputLayout.END_ICON_CUSTOM
        this.setEndIconTintList(colorStateList)
        this.setBoxStrokeColorStateList(colorStateList)
        this.hintTextColor = colorStateList
        this.helperText = null
        this.error = null
    }

    private fun TextInputLayout.setErrorState(
        msg: String,
        @ColorRes color: Int,
    ) {
        val colorStateList = ColorStateList.valueOf(getColor(color))
        this.endIconMode = TextInputLayout.END_ICON_NONE
        this.boxStrokeErrorColor = colorStateList
        this.setErrorTextColor(colorStateList)
        this.setErrorIconTintList(colorStateList)
        this.helperText = msg // change helpertext to msg to avoid animation glitch
        this.error = null // Workaround: Refresh if the error color changes
        this.error = msg
    }

    private fun proceed() {
        val intent = Intent(this, MainActivity::class.java)
        // Login is verified, only possible to login from here by checking against the server
        intent.putExtra(LOGIN_VERIFIED_KEY, LOGIN_SUCCESS)
        intent.putExtra(NEW_REGISTERED_USER_KEY, viewModel.isUserRegistering())
        startActivity(intent)
        finish()
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
        title = getString(R.string.login_view_login_state_title)
        binding.loginPasswordRepeat.visibility = View.GONE
        binding.registerLoginButton.text = getString(R.string.login_view_login_state_action_button)
        binding.registerLoginSwitch.text = getString(R.string.login_view_login_state_switch_button)
    }

    private fun setRegisterView() {
        title = getString(R.string.login_view_register_state_title)
        binding.loginPasswordRepeat.visibility = View.VISIBLE
        binding.registerLoginButton.text =
            getString(R.string.login_view_register_state_action_button)
        binding.registerLoginSwitch.text =
            getString(R.string.login_view_register_state_switch_button)
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