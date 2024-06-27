package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.app.Activity
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

object Extensions {

    fun Activity.toast(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    fun Activity.snackbar(view: View, msg: String){
        Snackbar.make(view, msg, Snackbar.LENGTH_SHORT ).show()
    }
}