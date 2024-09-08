package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.droidman.ktoasty.KToasty

object Extensions {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    fun Activity.toast(msg: String){
        KToasty.normal(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun Activity.successToast(msg: String){
        KToasty.success(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun Activity.negativeToast(msg: String){
        KToasty.error(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun Activity.infoToast(msg: String){
        KToasty.info(this, msg, Toast.LENGTH_SHORT).show()
    }
}