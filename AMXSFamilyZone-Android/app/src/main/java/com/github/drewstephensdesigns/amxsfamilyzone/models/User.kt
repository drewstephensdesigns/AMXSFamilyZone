package com.github.drewstephensdesigns.amxsfamilyzone.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val following: MutableList<String> = mutableListOf(),
    val bio: String = "",
    val imageUrl: String = "",
    val link : String = "",
    val accountCreated: Long = 0L,
){
    fun getCreatedDate(): String? {
        val rawDate = accountCreated
        return SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault()).format(Date(rawDate))
    }
}
