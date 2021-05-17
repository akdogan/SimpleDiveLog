package com.akdogan.simpledivelog.application.mainactivity
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
// <div>Icons made by <a href="https://www.freepik.com" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.ServiceLocator
import com.akdogan.simpledivelog.application.ui.loginview.LoginViewActivity
import com.akdogan.simpledivelog.application.ui.settingsView.SettingsActivity
import com.akdogan.simpledivelog.datalayer.repository.DefaultAuthRepository
import com.akdogan.simpledivelog.datalayer.repository.DefaultPreferencesRepository
import com.akdogan.simpledivelog.datalayer.repository.PreferencesRepository
import com.akdogan.simpledivelog.datalayer.repository.RepositoryUploadStatus
import com.akdogan.simpledivelog.diveutil.Constants.CREATE_SAMPLE_DATA
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_DEFAULT_VALUE
import com.akdogan.simpledivelog.diveutil.Constants.LOGIN_VERIFIED_KEY
import com.akdogan.simpledivelog.diveutil.Constants.NEW_REGISTERED_USER_KEY
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity(), AuthExpiredReceiver {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    private val viewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory(
            DefaultAuthRepository(),
            ServiceLocator.repo,
            DefaultPreferencesRepository(PreferenceManager.getDefaultSharedPreferences(this))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Action bar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Setup NavHost Fragment Navigation for the actionbar
        setupNavigation()

        // Setup Repo Auth Token
        setupRepoAuthToken()

        // setup Network check
        setupNetworkCallback()

        // setup UploadIndicator
        setupProgressListener()

        // Retrieve Login status and pass it to the Viewmodel
        val loginVerified = intent.getIntExtra(LOGIN_VERIFIED_KEY, LOGIN_DEFAULT_VALUE)
        viewModel.setLoginStatus(loginVerified)

        viewModel.navigateToLogin.observe(this) {
            if (it == true) {
                navigateToLogin()
                viewModel.onNavigateToLoginDone()
            }
        }
    }

    fun setupNavigation() {
        // Setup NavHost with Args for StartDestination
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bundle = Bundle()
        // Check if sample data should be created and put the information in the bundle
        // Bundle is passed to the start destination (listViewFragment)
        val createDummyContent = intent.getBooleanExtra(NEW_REGISTERED_USER_KEY, false)
        bundle.putBoolean(CREATE_SAMPLE_DATA, createDummyContent)
        // setup the graph including the bundle for the start destination args
        navController.setGraph(R.navigation.nav_graph, bundle)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupProgressListener() {
        //  Turns on the linear progress animation when uploading
        viewModel.uploadStatus.observe(this, {
            // TODO Create Espresso Test
            // Start Progress indeterminate, verifiy progress bar is shown indeterminate
            // Switch to determinate progress, verify progress is shown determinate
            // Increment progress, verify progress displayed matches
            // Switch to indeterminate, verify progress bar is shown indeterminate
            // Switch off upload, verify progress bar is not shown anymore
            // Maybe create fragment and activity in test? then control from fragment check in activity
            it?.let {
                val progressBar =
                    findViewById<LinearProgressIndicator>(R.id.main_view_upload_progress)
                when (it.status) {
                    RepositoryUploadStatus.INDETERMINATE_UPLOAD -> {
                        progressBar.apply {
                            if (!this.isIndeterminate) {
                                visibility = View.INVISIBLE
                                this.isIndeterminate = true
                                this.progress = 70
                                this.visibility = View.VISIBLE
                            } else {
                                this.visibility = View.VISIBLE
                            }
                        }
                    }
                    RepositoryUploadStatus.PROGRESS_UPLOAD -> {
                        progressBar.apply {
                            if (this.isIndeterminate) {
                                this.setProgressCompat(it.percentage, true)
                            } else {
                                this.progress = it.percentage
                            }
                        }
                    }
                    RepositoryUploadStatus.DONE -> {
                        progressBar.apply {
                            this.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        })
    }

    private fun setupRepoAuthToken() {
        val token = (DefaultPreferencesRepository(
            PreferenceManager.getDefaultSharedPreferences(this)
        ) as PreferencesRepository).getCredentials()
        token?.let {
            ServiceLocator.repo.setAuthToken(token)
        }
    }

    private fun setupNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onLost(network: Network) {
                viewModel.onNetworkLost()
                super.onLost(network)
            }

            override fun onCapabilitiesChanged(nw: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    viewModel.onNetworkAvailable()
                }
                super.onCapabilitiesChanged(nw, caps)
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginViewActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    // Registering and unregistering Network callback, should only be active while the app is active
    override fun onResume() {
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(
            networkCallback
        )
        super.onResume()
    }

    override fun onPause() {
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
        super.onPause()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        viewModel.networkAvailable.observe(this) {
            menu.findItem(R.id.action_connectivity).isVisible = !it
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_logout -> {
                viewModel.logout()
                return true
            }
            R.id.action_settings -> {
                val i = Intent(this, SettingsActivity::class.java)
                startActivity(i)
                return true

            }
            R.id.action_connectivity -> {
                val i = Intent()
                i.action = Settings.ACTION_WIRELESS_SETTINGS
                i.addCategory(Intent.CATEGORY_DEFAULT)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                applicationContext.startActivity(i)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun authExpired() {
        viewModel.logout()
    }
}

// Fragments use this to communicate to the activity that the auth token is not valid anymore
interface AuthExpiredReceiver {

    fun authExpired()


}