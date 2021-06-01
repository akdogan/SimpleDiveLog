package com.akdogan.simpledivelog.application.ui.loginview

import android.util.Log
import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.akdogan.simpledivelog.R
import com.google.android.material.textfield.TextInputLayout
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginViewActivityUiTest {

    @StringRes
    private val loginStateSwitchButton = R.string.login_view_login_state_switch_button

    @StringRes
    private val loginStateActionButton = R.string.login_view_login_state_action_button

    @StringRes
    private val registerStateSwitchButton = R.string.login_view_register_state_switch_button

    @StringRes
    private val registerStateActionButton = R.string.login_view_register_state_action_button

    @StringRes
    private val alertDialogTitle = R.string.login_view_dialog_title

    private val usernameEmptyHelperText = "Min. 6 characters: a–z, 0–9, -+_"
    private val usernameTooShortHelperText = "Too short: Min. 6 characters"
    private val usernameInvalidCharHelperText = "Allowed: a–z, 0–9, -+_"

    private val passwordEmptyHelperText = "Min. 8 characters: a–z, 0–9, -+$!_*"
    private val passwordTooShortHelperText = "Too short: Min. 8 characters"
    private val passwordInvalidCharacterHelperText = "Allowed: a–z, 0–9, -+$!_*"

    private val passwordRepeatNoMatchHelperText = "Does not match"

    private val allFieldsValidHelperText = ""

    @Before
    fun setup() {
        launchActivity<LoginViewActivity>()
    }

    private fun setupRegisterView() {
        // Proceeding directly to register view
        onView(withId(R.id.register_login_switch)).perform(click())
        onView(withText(alertDialogTitle)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        Thread.sleep(0)
    }

    @Test
    fun checkElementsAreDisplayedCorrect_OnStartup() {
        launchActivity<LoginViewActivity>()
        onView(withId(R.id.register_login_switch)).check(matches(withText(loginStateSwitchButton)))
        onView(withId(R.id.login_username)).check(matches(isDisplayed()))
        onView(withId(R.id.login_password)).check(matches(isDisplayed()))
        onView(withId(R.id.login_password_repeat)).check(matches(not(isDisplayed())))
        onView(withId(R.id.register_login_button)).check(matches(withText(loginStateActionButton)))
    }

    @Test
    fun checkElementsAreDisplayedCorrect_AfterSwitch() {
        launchActivity<LoginViewActivity>()
        // Tap button to switch to RegisterView
        onView(withId(R.id.register_login_switch)).perform(click())
        // Check if Dialog is displayed and dismiss
        onView(withText(alertDialogTitle)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        // Check if RegisterView is displayed correctly
        onView(withId(R.id.register_login_switch)).check(matches(withText(registerStateSwitchButton)))
        onView(withId(R.id.login_password_repeat)).check(matches(isDisplayed()))
        onView(withId(R.id.register_login_button)).check(matches(withText(registerStateActionButton)))

    }

    @Test
    fun checkDefaultHintsDisplayedCorrect() {
        setupRegisterView()
        onView(withId(R.id.login_username))
            .check(matches(hasTextInputLayoutHintText(usernameEmptyHelperText)))
        onView(withId(R.id.login_password))
            .check(matches(hasTextInputLayoutHintText(passwordEmptyHelperText)))
        onView(withId(R.id.login_password_repeat))
            .check(matches(hasTextInputLayoutHintText(passwordEmptyHelperText)))
    }

    @Test
    fun checkTooShortHintsDisplayedCorrect() {
        setupRegisterView()
        materialInputTypeAndCheckHint(R.id.login_username, "a", usernameTooShortHelperText)
        materialInputTypeAndCheckHint(R.id.login_password, "a", passwordTooShortHelperText)
        materialInputTypeAndCheckHint(R.id.login_password_repeat, "a", passwordTooShortHelperText)
    }

    @Test
    fun checkInvalidHintsDisplayedCorrect(){
        setupRegisterView()
        materialInputTypeAndCheckHint(R.id.login_username, "%", usernameInvalidCharHelperText)
        materialInputTypeAndCheckHint(R.id.login_password, "%", passwordInvalidCharacterHelperText)
        materialInputTypeAndCheckHint(R.id.login_password_repeat, "%", passwordInvalidCharacterHelperText)
    }

    @Test
    fun checkAllValidDisplayedCorrect(){
        setupRegisterView()
        val validInput = "aaaaaaaa"
        materialInputTypeAndCheckHint(R.id.login_username, validInput, allFieldsValidHelperText)
        materialInputTypeAndCheckHint(R.id.login_password, validInput, allFieldsValidHelperText)
        materialInputTypeAndCheckHint(R.id.login_password_repeat, validInput, allFieldsValidHelperText)
    }

    @Test
    fun checkDoesNotMatchHintDisplayedCorrect(){
        setupRegisterView()
        val validInput = "aaaaaaaa"
        val invalidInput = "a"
        materialInputTypeAndCheckHint(R.id.login_password, validInput, allFieldsValidHelperText)
        materialInputTypeAndCheckHint(R.id.login_password_repeat, invalidInput, passwordRepeatNoMatchHelperText)
        materialInputTypeAndCheckHint(R.id.login_password_repeat, validInput, allFieldsValidHelperText)
    }

    @Test
    fun checkRepeatFieldUpdatesHint(){
        setupRegisterView()
        val validInput = "aaaaaaaa"
        materialInputAddText(R.id.login_password, validInput)
        // Repeat should have empty hint
        materialInputCheckHint(R.id.login_password_repeat, passwordEmptyHelperText)
        // Add Same input and check
        materialInputTypeAndCheckHint(R.id.login_password_repeat, validInput, allFieldsValidHelperText)
        // Change password input and check repeat password hint
        materialInputAddText(R.id.login_password, "a")
        materialInputCheckHint(R.id.login_password_repeat, passwordRepeatNoMatchHelperText)
        // Change password back and check repeat password hint
        materialInputClear(R.id.login_password)
        materialInputAddText(R.id.login_password, validInput)
        materialInputCheckHint(R.id.login_password_repeat, allFieldsValidHelperText)
    }

    private fun materialInputClear(@LayoutRes id: Int){
        onView(allOf(isDescendantOfA(withId(id)), supportsInputMethods()))
            .perform(clearText())
    }

    private fun materialInputAddText(@LayoutRes id: Int, input: String){
        onView(allOf(isDescendantOfA(withId(id)), supportsInputMethods()))
            .perform(typeText(input))
    }

    private fun materialInputCheckHint(@LayoutRes id: Int, expectedHint: String){
        onView(withId(id))
            .check(matches(hasTextInputLayoutHintText(expectedHint)))
    }

    private fun materialInputTypeAndCheckHint(@LayoutRes id: Int, input: String, expectedHint: String){
        materialInputClear(id)
        materialInputAddText(id, input)
        Thread.sleep(2000)
        materialInputCheckHint(id, expectedHint)
    }
}

internal fun hasTextInputLayoutHintText(expectedHintText: String): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        override fun matchesSafely(item: View?): Boolean {
            // Check if the item is an TextInputlayout
            if (item !is TextInputLayout) return false
            // Get the error message (hint) from the item - item is smartcastet
            val actualHintText: CharSequence? = item.helperText
            Log.d("CUSTOM_TEST", "hint is: $actualHintText")
            Log.d("CUSTOM_TEST", "expected is: $expectedHintText")
            // Return if the item matches the expected, or false if the item is null
            return if (actualHintText != null){
                    expectedHintText == actualHintText.toString()
                } else {
                    expectedHintText == ""
            }
        }

        override fun describeTo(description: Description?) {}
    }
}
