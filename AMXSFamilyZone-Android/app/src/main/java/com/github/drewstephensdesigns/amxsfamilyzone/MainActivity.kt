package com.github.drewstephensdesigns.amxsfamilyzone

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.ActivityMainBinding
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.toast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils.firebaseAuth
import com.github.drewstephensdesigns.amxsfamilyzone.utils.PostListenerService
import com.github.drewstephensdesigns.amxsfamilyzone.utils.UserUtil
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        setupMenu()

        UserUtil.getCurrentUser()

        val navView: BottomNavigationView = binding.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_add_post,
                R.id.navigation_quick_links,
                R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        applyTheme()
        kToastyConfig()

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            } else {
                startPostListenerService()
            }
        } else {
            startPostListenerService()
        }
    }

    private fun startPostListenerService() {
        val intent = Intent(this, PostListenerService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setupMenu() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when(menuItem.itemId){

                    // Settings Menu
                    R.id.action_settings -> {
                        val navController = findNavController(R.id.nav_host_fragment_activity_main)
                        navController.navigate(R.id.navigation_settings)
                        true
                    }

                    // Sign out user from Firebase
                    // Returns to LoginActivity.kt
                    R.id.action_sign_out ->{
                        logout()
                        toast("Signed Out")
                        true
                    }

                    else -> {false}
                }
            }
        })
    }

    /**
     * Applies the App's Theme from sharedPrefs
     */
    private fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val modeNight = sharedPreferences.getInt(
            getString(R.string.pref_key_mode_night),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        AppCompatDelegate.setDefaultNightMode(modeNight)
    }

    private fun kToastyConfig() {
        val typeface: Typeface? = ResourcesCompat.getFont(applicationContext, R.font.alata)
        KToasty.Config.getInstance()
            .setTextSize(13)
            .setToastTypeface(typeface!!)
            .apply()
    }

    private fun logout() {
        firebaseAuth.signOut()
        val intent = Intent(
            applicationContext,
            LoginActivity::class.java
        )
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, start the service
                startPostListenerService()
            } else {
                // Permission denied, handle accordingly
                toast("Notification permission denied. Notifications will not be shown.")
            }
        }
    }

    // called when the configuration of the device changes. It is typically used
    // to handle changes in device orientation or changes in other device-specific
    // configurations such as the language or screen size.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
        getOrientation()
        recreate()
    }

    private fun getOrientation() {
        requestedOrientation =
            if (Settings.System.getInt(
                    contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0
                ) == 1
            ) {
                //Auto Rotate is on, so don't lock
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                //Auto Rotate is off, so lock
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
    }


    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
    }
}