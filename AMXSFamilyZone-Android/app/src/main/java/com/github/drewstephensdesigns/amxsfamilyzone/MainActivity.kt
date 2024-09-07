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
import android.widget.Toast
import androidx.activity.addCallback
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
import com.github.drewstephensdesigns.amxsfamilyzone.ui.posting.AddPostFragment
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.toast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils.firebaseAuth
import com.github.drewstephensdesigns.amxsfamilyzone.utils.PostListener
import com.github.drewstephensdesigns.amxsfamilyzone.utils.UserUtil
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var postListener: PostListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Override the back press behavior
        // If the user presses back on any fragment other than "Home," they will navigate to the previous fragment.
        // If they are on the "Home" fragment and press back, the app will close.
        onBackPressedDispatcher.addCallback(this) {
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            if (navController.currentDestination?.id == R.id.navigation_home) {
                // If we're on the home fragment, close the app
                finishAffinity()
            } else {
                // Otherwise, navigate back to the previous fragment
                navController.navigateUp()
            }
        }


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
                R.id.navigation_search,
                R.id.navigation_add_post,
                R.id.navigation_quick_links,
                R.id.navigation_profile
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        applyTheme()
        kToastyConfig()

        // Initialize the PostListener with the current context
        postListener = PostListener(this)

        // Start listening for new posts
        postListener.startListening()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PostListener.NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start listening for posts
                postListener.startListening()
                //Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied, notify user
                //Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop listening for new posts when the activity is destroyed
        postListener.stopListening()
    }

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }
}