package com.akdogan.simpledivelog.application.ui.loginview

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.akdogan.simpledivelog.R
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginViewActivityUiTest {

    private val loginStateSwitchButton = "Register instead"
    private val loginStateActionButton = "Login"
    private val registerStateSwitchButton = "Login instead"
    private val registerStateActionButton = "Register"

    private val alertDialogTitle = "Warning"

    //@get:Rule var activityScenarioRul = activityScenarioRule<MainActivity>

    @Test
    fun checkElementsAreDisplayedCorrect_OnStartup(){
        launchActivity<LoginViewActivity>()
        onView(withId(R.id.register_login_switch)).check(matches(withText(loginStateSwitchButton)))
        onView(withId(R.id.login_username)).check(matches(isDisplayed()))
        onView(withId(R.id.login_password)).check(matches(isDisplayed()))
        onView(withId(R.id.login_password_repeat)).check(matches(not(isDisplayed())))
        onView(withId(R.id.register_login_button)).check(matches(withText(loginStateActionButton)))
    }

    @Test
    fun checkElementsAreDisplayedCorrect_AfterSwitch(){
        launchActivity<LoginViewActivity>()
        // Tap button to switch to RegisterView
        onView(withId(R.id.register_login_switch)).perform(click())
        // Check if Dialog is displayed and dismiss
        onView(withText(alertDialogTitle)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        // Check if RegisterView is displayed correctly
        onView(withId(R.id.register_login_switch)).check(matches(withText(registerStateSwitchButton)))
        onView(withId(R.id.login_password_repeat)).check(matches(not(isDisplayed())))
        onView(withId(R.id.register_login_button)).check(matches(withText(registerStateActionButton)))

    }
}