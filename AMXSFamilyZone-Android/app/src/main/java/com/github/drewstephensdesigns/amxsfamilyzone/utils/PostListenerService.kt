package com.github.drewstephensdesigns.amxsfamilyzone.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration


class PostListenerService : Service() {

    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection(Consts.POST_NODE)
    private lateinit var postListenerRegistration: ListenerRegistration
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate() {
        super.onCreate()
        setupPostListener()
        startForegroundService()
    }

    private fun setupPostListener() {
        postListenerRegistration = postsCollection.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("PostListener", "Listen failed.", e)
                return@addSnapshotListener
            }

            for (dc in snapshots!!.documentChanges) {
                when (dc.type) {
                    DocumentChange.Type.ADDED -> {
                        val post = dc.document.toObject(Post::class.java)
                        if (post.creatorId != currentUser?.uid) {
                            sendNewPostNotification(post)
                        }
                    } else -> {}
                }
            }
        }
    }

    private fun sendNewPostNotification(post: Post) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannelId = "new_post_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(notificationChannelId, "New Post Notifications", NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.description = "Channel for new post notifications"
                notificationManager.createNotificationChannel(notificationChannel)
            }

            // Create an intent to open the app when the notification is clicked
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("postId", post.id) // Pass the post ID if you want to navigate to a specific post
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
                .setContentTitle("New Post by ${post.user.name}")
                .setContentText(post.text)
                .setSmallIcon(R.drawable.amxs)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // Set the pending intent to open the app
                .setAutoCancel(true) // Auto-cancel the notification when clicked

            notificationManager.notify(post.creatorId.hashCode(), notificationBuilder.build())
        } else {
            Log.w("PostListenerService", "Notification permission not granted.")
        }
    }


    private fun startForegroundService() {
        val notificationChannelId = "post_listener_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(notificationChannelId, "Post Listener Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(
            this, notificationChannelId)
            .setContentTitle("Post Listener Service")
            .setContentText("Listening for new posts...")
            .setSmallIcon(R.drawable.amxs)
            .setPriority(NotificationCompat.PRIORITY_LOW)

            startForeground(1, notificationBuilder.build())
    }



    override fun onDestroy() {
        super.onDestroy()
        postListenerRegistration.remove()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}