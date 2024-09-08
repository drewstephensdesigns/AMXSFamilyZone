package com.github.drewstephensdesigns.amxsfamilyzone

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.drewstephensdesigns.amxsfamilyzone.utils.tools.DelegatesExt

class AMXSApplication : Application() {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    companion object {
        var instance: AMXSApplication by DelegatesExt.notNullSingleValue()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        applyTheme()
    }

    /**
     * Applies the App's Theme from sharedPrefs
     */
    fun applyTheme() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val modeNight = sharedPreferences.getInt(
            getString(R.string.pref_key_mode_night),
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )

        AppCompatDelegate.setDefaultNightMode(modeNight)
    }
}