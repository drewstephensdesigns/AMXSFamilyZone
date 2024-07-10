package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.getSystemService
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.R

object Consts {

    // Firebase Collection Names
    const val USER_NODE = "Users"
    const val POST_NODE = "Post"
    const val REPORTS_NODE = "Reports"
    const val COMMENTS_NODE = "Comments"
    const val NOTIFICATIONS_NODE = "Notifications"

    // Github URL serving static data
    const val FEATURED_URL = "https://drewstephensdesigns.github.io/AMXSFamilyZone/data/"

    // Static function for saving to clipboard
    fun save(context: Context, text: String) {
        val clip = ClipData.newPlainText(context.getString(R.string.copied_to_clipboard), text)
        context.getSystemService<ClipboardManager>()!!.setPrimaryClip(clip)
    }

    fun notifyUserNoImage(context: Context, message: String) {
        KToasty.warning(context, message, Toast.LENGTH_SHORT, true).show()
    }
}