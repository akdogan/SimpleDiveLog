package com.akdogan.simpledivelog.application
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
//<div>Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a></div>
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.repository.Repository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO ?? if the scope gets canceled fast enough, database in the repo will not be setup
        lifecycleScope.launch {
            Repository.setup(applicationContext)

        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))



    }

    // Registering and unregistering Network callback, should only be active while the app is active
    override fun onResume() {
        Repository.registerNetworkCallback(getSystemService(ConnectivityManager::class.java))
        super.onResume()

    }

    override fun onPause() {
        Repository.unregisterNetworkCallback(getSystemService(ConnectivityManager::class.java))
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
            R.id.action_settings -> true
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