package com.akdogan.simpledivelog.application
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //This should be done with Dependency Injection
        lifecycleScope.launch(Dispatchers.IO) {

            Repository.setup(applicationContext)
            Log.d("MAIN THREAD", "End of Coroutine")
        }
        Log.d("MAIN THREAD", "After Coroutine")
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
                Log.i("NETWORKING", "Network LOST: ${network.networkHandle}")
                Repository.onNetworkLost()
                super.onLost(network)
            }

            override fun onCapabilitiesChanged(nw: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    Log.i("NETWORKING", "Capability validated for ${nw.networkHandle}")
                    Repository.onNetworkAvailable()
                }
                super.onCapabilitiesChanged(nw, caps)
            }
        }
        Log.i("MAIN THREAD", "End of onCreate Activity")
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
        Repository.networkAvailable.observe(this, Observer {
            menu.findItem(R.id.action_connectivity).isVisible = !it
        })
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
                /*val intent = Intent(this, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)*/
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