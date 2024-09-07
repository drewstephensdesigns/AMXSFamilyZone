package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidman.ktoasty.KToasty
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first

object Extensions {

    fun Activity.toast(msg: String){
        KToasty.normal(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun Activity.successToast(msg: String){
        KToasty.success(this, msg, Toast.LENGTH_SHORT)
    }

    fun Activity.negativeToast(msg: String){
        KToasty.error(this, msg, Toast.LENGTH_SHORT)
    }

    fun Activity.infoToast(msg: String){
        KToasty.info(this, msg, Toast.LENGTH_SHORT)
    }
}