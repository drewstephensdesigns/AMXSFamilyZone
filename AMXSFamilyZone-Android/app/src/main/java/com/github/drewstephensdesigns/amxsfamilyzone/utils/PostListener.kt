package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID

class PostListener(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("Post") // Replace with your collection name
    private var postListenerRegistration: ListenerRegistration? = null
    private val currentUser = FirebaseUtils.firebaseAuth.currentUser
    private val notifiedPostIDs = mutableSetOf<String>() // Track notified post IDs

    init {
        // Create notification channel upon initialization
        createNotificationChannel()
    }

    // Starts the listener for new posts
    fun startListening() {
        postListenerRegistration = postsCollection.addSnapshotListener { snapshots, error ->
            if (error != null) {
                println("Listen failed: ${error.localizedMessage}")
                return@addSnapshotListener
            }

            snapshots?.documentChanges?.forEach { documentChange ->
                if (documentChange.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                    val post = documentChange.document.toObject(Post::class.java)
                    if (post.creatorId != currentUser?.uid) {
                        if (!notifiedPostIDs.contains(post.id)) {
                            notifiedPostIDs.add(post.id)
                            sendNewPostNotification(post)
                        }
                    }
                }
            }
        }
    }

    // Stops the Firestore listener
    fun stopListening() {
        postListenerRegistration?.remove()
    }

    // Sends a notification for new posts
    private fun sendNewPostNotification(post: Post) {
        val notificationId = UUID.randomUUID().hashCode()

        // Create notification content
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.amxs) // Replace with your app's notification icon
            .setContentTitle("New Post by ${post.user}")
            .setContentText(post.text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        context as MainActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                        )
                }
                return
            }
            notify(notificationId, notificationBuilder.build())
        }
    }

    // Creates a notification channel for API level 26+
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Post Notifications"
            val descriptionText = "Notifications for new posts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "new_post_channel"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}