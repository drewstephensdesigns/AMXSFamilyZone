package com.github.drewstephensdesigns.amxsfamilyzone.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Future: add ability for PDF
 * val pdfUrl: String?,
 */


data class Post(
    val id: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val user: User = User(),
    val time: Long = 0L,
    val creatorId: String = "",
    val likeList: MutableList<String> = mutableListOf(),
    val hashtags: MutableList<String> = mutableListOf()

){
    fun getTimeStamp(): String? {
        val rawDate = time
        return SimpleDateFormat("MMMM d, yyyy hh:mm a", Locale.getDefault()).format(Date(rawDate))
    }
}