package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.R
import java.util.concurrent.TimeUnit

object Consts {

    // Firebase Collection Names
    const val USER_NODE = "Users"
    const val POST_NODE = "Post"
    const val REPORTS_NODE = "Reports"
    const val COMMENTS_NODE = "Comments"
    const val NOTIFICATIONS_NODE = "Notifications"

    // Github URL serving static data
    const val FEATURED_URL = "https://drewstephensdesigns.github.io/AMXSFamilyZone/data/"
    const val DEFAULT_USER_IMAGE = "https://firebasestorage.googleapis.com/v0/b/amxs-family-zone-a2d4e.appspot.com/o/Images%2Fdefault_user_image.jpg?alt=media&token=24692997-da61-4e3a-b4c1-6858236d29c6"

    // Static function for saving to clipboard
    fun save(context: Context, text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.copied_to_clipboard), text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
    }

    fun notifyUserNoImage(context: Context, message: String) {
        KToasty.warning(context, message, Toast.LENGTH_SHORT, true).show()
    }
}