package com.github.drewstephensdesigns.amxsfamilyzone.ui.home

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.droidman.ktoasty.KToasty
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.LayoutItemPostBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.ui.profile.ProfileFragment
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts.POST_NODE
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts.REPORTS_NODE
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.internal.bind.TypeAdapters.URL
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.squareup.picasso.Picasso
import io.github.giangpham96.expandable_textview.ExpandableTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern


class HomeAdapter(
    options: FirestoreRecyclerOptions<Post>,
    val context: Context,
    val noResultsTextView: TextView,
    private val onUserNameClick: (User) -> Unit
) : FirestoreRecyclerAdapter<Post, HomeAdapter.HomeViewHolder>(options) {

    init {
        startListening()
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIfEmpty()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                checkIfEmpty()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                checkIfEmpty()
            }

            fun checkIfEmpty() {
                noResultsTextView.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = LayoutItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeAdapter.HomeViewHolder, position: Int, post: Post) {
        holder.bind(post)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class HomeViewHolder(binding: LayoutItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        private val postImage: ImageView = binding.feedPostImage
        private val authorText: TextView = binding.postAuthor
        //private val postText: TextView = binding.postText
        private val postText: ExpandableTextView = binding.postText
        private val postTimeText: TextView = binding.postTime
        private val postOptions: TextView = binding.vertMenuOptions
        private val postShareIcon: TextView = binding.shareText
        private val postBookmarkIcon: TextView = binding.postBookmarkIcon

        fun bind(itemPost: Post) {
            authorText.text = itemPost.user.email
            postTimeText.text = itemPost.getTimeStamp()
            applyHashtagColor(itemPost.text, postText)

            if (itemPost.imageUrl.isNullOrEmpty()) {
                postImage.visibility = View.GONE
                postBookmarkIcon.visibility = View.GONE
            } else {
                postImage.visibility = View.VISIBLE
                postBookmarkIcon.visibility = View.VISIBLE
                postImage.load(itemPost.imageUrl) {
                    crossfade(true)
                    crossfade(300)
                }
            }

            authorText.setOnClickListener {
                onUserNameClick(itemPost.user)
            }

            val firestore = FirebaseFirestore.getInstance()
            val userID = FirebaseUtils.firebaseAuth.currentUser?.uid

            val postDocument = firestore.collection(POST_NODE)
                .document(snapshots.getSnapshot(bindingAdapterPosition).id)

            postDocument.get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val post = it.result?.toObject(Post::class.java)

                    postShareIcon.setOnClickListener {
                        if (post?.imageUrl.isNullOrEmpty()) {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, post?.text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(context, shareIntent, null)
                        } else {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, post?.imageUrl)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(context, shareIntent, null)
                        }
                    }

                    postOptions.setOnClickListener {
                        val wrapper: Context = ContextThemeWrapper(context, R.style.Theme_AMXSFamilyZone)
                        val popup = PopupMenu(wrapper, postOptions)
                        popup.inflate(R.menu.popup_menu)

                        if (post?.creatorId != userID) {
                            popup.menu.findItem(R.id.menuActionEdit).isEnabled = false
                            popup.menu.findItem(R.id.menuActionDelete).isEnabled = false
                        } else {
                            popup.menu.findItem(R.id.menuActionReport).isEnabled = false
                        }

                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.menuActionEdit -> showEditPostDialog(postDocument.id, postText.text.toString())
                                R.id.menuActionDelete -> deletePost(postDocument.id)
                                R.id.menuActionReport -> reportDialog(postDocument.id)
                                else -> {}
                            }
                            false
                        }
                        popup.show()
                    }

                    postBookmarkIcon.setOnClickListener {
                        if (itemPost.imageUrl.isNullOrEmpty()) {
                            KToasty.warning(context, "No image to download", Toast.LENGTH_SHORT, true).show()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                downloadImageUsingMediaStore(context, itemPost.imageUrl!!)
                            } else {
                                downloadImageUsingDownloadManager(context, itemPost.imageUrl!!)
                            }
                        }
                    }
                }
            }
        }

        private fun applyHashtagColor(text: String, textView: TextView) {
            val spannable = SpannableStringBuilder(text)
            val hashtagPattern = Pattern.compile("#(\\w+)")
            val matcher = hashtagPattern.matcher(text)

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.hazard_orange)),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            textView.text = spannable
        }
    }

    private fun showEditPostDialog(postId: String, currentText: String) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_post, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_post_text)
        val updateButton = dialogView.findViewById<Button>(R.id.button_update_post)
        editText.setText(currentText)

        val alertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()

        updateButton.setOnClickListener {
            val newText = editText.text.toString().trim()
            if (newText.isNotEmpty()) {
                updatePost(postId, newText)
                alertDialog.dismiss()
            } else {
                KToasty.warning(context, "Post text cannot be empty", Toast.LENGTH_SHORT, true).show()
            }
        }
        alertDialog.show()
    }

    private fun updatePost(postId: String, newText: String) {
        val firestore = FirebaseFirestore.getInstance()
        val documentReference = firestore.collection(POST_NODE).document(postId)

        documentReference.get().addOnSuccessListener { documentSnapshot ->
            val existingPost = documentSnapshot.toObject(Post::class.java)
            if (existingPost?.creatorId == FirebaseUtils.firebaseAuth.currentUser?.uid) {
                documentReference.update("text", newText).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        KToasty.success(context, "Post updated!", Toast.LENGTH_SHORT, true).show()
                    } else {
                        KToasty.error(context, "Error updating post", Toast.LENGTH_SHORT, true).show()
                    }
                }
            } else {
                KToasty.warning(context, "You can only edit your own posts!", Toast.LENGTH_SHORT, true).show()
            }
        }
    }

    private fun deletePost(postId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val documentReference = firestore.collection(POST_NODE).document(postId)

        documentReference.get().addOnSuccessListener { documentSnapshot ->
            val existingPost = documentSnapshot.toObject(Post::class.java)
            if (existingPost?.creatorId == FirebaseUtils.firebaseAuth.currentUser?.uid) {
                val imageUrl = existingPost?.imageUrl
                if (!imageUrl.isNullOrEmpty()) {
                    val storageReference = storage.getReferenceFromUrl(imageUrl)
                    storageReference.delete().addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            deletePostDocument(documentReference)
                        } else {
                            KToasty.error(context, "Error deleting post image", Toast.LENGTH_SHORT, true).show()
                        }
                    }
                } else {
                    deletePostDocument(documentReference)
                }
            } else {
                KToasty.warning(context, "You can only delete your own posts!", Toast.LENGTH_SHORT, true).show()
            }
        }
    }

    private fun deletePostDocument(documentReference: DocumentReference) {
        documentReference.delete().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                KToasty.success(context, "Post deleted successfully!", Toast.LENGTH_SHORT, true).show()
            } else {
                KToasty.error(context, "Error deleting post", Toast.LENGTH_SHORT, true).show()
            }
        }
    }

    private fun reportDialog(postId: String) {
        InputSheet().show(context) {
            title("Report Post")
            with(InputRadioButtons() {
                label(R.string.report_text)
                options(
                    mutableListOf(
                        "Spam/harassment",
                        "OPSEC Violation",
                        "Made me uncomfortable"
                    )
                )

                val reportReason = mapOf(
                    0 to "Spam/harassment",
                    1 to "OPSEC Violation",
                    2 to "Made me uncomfortable"
                )

                changeListener { value ->
                    reportReason[value]?.let {
                        reportPost(postId, it)
                    }
                }
            })
        }
    }

    private fun reportPost(postId: String, reason: String) {
        val firestore = FirebaseFirestore.getInstance()
        val reportReference = firestore.collection(REPORTS_NODE).document()

        val reportData = hashMapOf(
            "postId" to postId,
            "reporterId" to FirebaseUtils.firebaseAuth.currentUser?.uid,
            "reason" to reason,
            "timestamp" to FieldValue.serverTimestamp()
        )

        reportReference.set(reportData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                KToasty.success(context, "Post reported successfully", Toast.LENGTH_SHORT, true).show()
            } else {
                KToasty.error(context, "Error reporting post", Toast.LENGTH_SHORT, true).show()
            }
        }
    }

    private fun downloadImageUsingMediaStore(context: Context, imageUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val uri = Uri.parse(imageUrl)
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, uri.lastPathSegment)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AMXS")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                val outputStream = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    try {
                        val inputStream = URL(imageUrl).openStream()
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input.copyTo(output)
                            }
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)

                        withContext(Dispatchers.Main) {
                            KToasty.success(context, "Image downloaded successfully!", Toast.LENGTH_SHORT, true).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            KToasty.error(context, "Failed to download image", Toast.LENGTH_SHORT, true).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    KToasty.error(context, "Failed to create MediaStore entry", Toast.LENGTH_SHORT, true).show()
                }
            }
        }
    }

    private fun downloadImageUsingDownloadManager(context: Context, imageUrl: String) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(imageUrl)
        val request = DownloadManager.Request(uri)

        // Allow the media scanner to scan the downloaded file
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.lastPathSegment)

        downloadManager.enqueue(request)
        KToasty.success(context, "Downloading Image...", Toast.LENGTH_SHORT, true).show()
    }
}