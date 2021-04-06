package com.akdogan.simpledivelog.application
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.repository.Repository

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var repository: Repository

    // TODO: Add Login and use different users instead of only one

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Setup Repository. This should be done with Dependency Injection
        repository = ServiceLocator.repo
        setContentView(R.layout.activity_main)
        // Setup Action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        // Setup NavHost Fragment Navigation for the actionbar
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // setup Network check
        networkCallback = object : ConnectivityManager.NetworkCallback() {

            override fun onLost(network: Network) {
                repository.onNetworkLost()
                super.onLost(network)
            }

            override fun onCapabilitiesChanged(nw: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    repository.onNetworkAvailable()
                }
                super.onCapabilitiesChanged(nw, caps)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    // Registering and unregistering Network callback, should only be active while the app is active
    override fun onResume() {
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
        super.onResume()
    }

    override fun onPause() {
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
        super.onPause()
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        repository.networkAvailable.observe(this) {
            menu.findItem(R.id.action_connectivity).isVisible = !it
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
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
}