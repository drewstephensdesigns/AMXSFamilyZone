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
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
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
import coil.size.ViewSizeResolver
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.internal.bind.TypeAdapters.URL
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.input.InputSheet
import com.maxkeppeler.sheets.input.type.InputRadioButtons
import com.maxkeppeler.sheets.option.DisplayMode
import com.maxkeppeler.sheets.option.Option
import com.maxkeppeler.sheets.option.OptionSheet
import com.soufianekre.linkpreviewer.data.UrlPreviewItem
import com.soufianekre.linkpreviewer.views.UrlPreviewCard
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
        private val postText: TextView = binding.postText
        private val postTimeText: TextView = binding.postTime
        private val postOptions: TextView = binding.vertMenuOptions
        private val postShareIcon: TextView = binding.shareText
        private val postBookmarkIcon: TextView = binding.postBookmarkIcon
        private val postLinkText: TextView = binding.postLinkText
        private val postLinkPreview : UrlPreviewCard = binding.linkPreview

        fun bind(itemPost: Post) {
            authorText.text = itemPost.user.email
            postTimeText.text = itemPost.getTimeStamp()
            applyHashtagColor(itemPost.text, postText)

            if (itemPost.link.isNotEmpty()){
                postLinkText.visibility = View.VISIBLE
                postLinkPreview.visibility = View.VISIBLE
                postLinkText.text = itemPost.link

                // UrlPreviewCard doesn't work with PDF, so if the link
                // contains .pdf like an AFI, we'll hide the preview so the
                // app doesn't break, else show the preview
                if(itemPost.link.contains(".pdf")){
                    postLinkPreview.visibility = View.GONE
                } else {
                    // Preview Post Link
                    postLinkPreview.setUrl(itemPost.link,object : UrlPreviewCard.OnPreviewLoad {
                        override fun onLinkLoaded(url:String,urlPreview: UrlPreviewItem) {}
                    })
                }
            } else{
                postLinkText.visibility = View.GONE
                postLinkPreview.visibility = View.GONE
            }

            // If post includes images, shows bookmark
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

            // Since I haven't figured out a good size for images, this keeps
            // the images smaller, but allows the user to view a full size in a popup window
            postImage.setOnClickListener {
                if(itemPost.imageUrl!!.isNotEmpty()){
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_post_view, null)
                    val dialogImageView: ImageView = dialogView.findViewById(R.id.dialogImageView)
                    val menuIcon: ImageView = dialogView.findViewById(R.id.menuIcon)

                    Picasso.get().load(itemPost.imageUrl).into(dialogImageView)
                    MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
                        .setView(dialogView)
                        //.setTitle(currentUser.name)
                        .create()
                        .show()

                    menuIcon.setOnClickListener {
                        OptionSheet().show(context) {
                            style(SheetStyle.BOTTOM_SHEET)
                            displayMode(DisplayMode.LIST)
                            with(
                                Option("Download Image", "Save image to your device"),
                                Option("Share Image", "Share this image with your friends")
                            )
                            onPositive { index: Int, _: Option ->
                                when (index) {
                                    0 -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            downloadImageUsingMediaStore(context!!, itemPost.imageUrl)
                                        } else {
                                            downloadImageUsingDownloadManager(context!!, itemPost.imageUrl)
                                        }
                                        //sharePost(post.imageUrl)
                                        //dismiss()
                                    }
                                    1 ->{
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, itemPost.imageUrl)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(context!!, shareIntent, null)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val firestore = FirebaseFirestore.getInstance()
            val userID = FirebaseUtils.firebaseAuth.currentUser?.uid
            val postDocument = firestore.collection(POST_NODE)
                .document(snapshots.getSnapshot(bindingAdapterPosition).id)

            // Allows the user to view other profiles
            authorText.setOnClickListener {
                if (itemPost.creatorId != userID){
                    onUserNameClick(itemPost.user)
                }
            }

            postDocument.get().addOnCompleteListener {
                if (it.isSuccessful) {
                    val post = it.result?.toObject(Post::class.java)

                    // If the post is text only, it'll share the text
                    // If post includes image, the Firebase Storage url will be shared
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

                    // Popup Menu - Report Post
                    postOptions.setOnClickListener {
                        val wrapper: Context = ContextThemeWrapper(context, R.style.Theme_AMXSFamilyZone)
                        val popup = PopupMenu(wrapper, postOptions)
                        popup.inflate(R.menu.popup_menu)

                        popup.menu.findItem(R.id.menuActionReport).isEnabled = post?.creatorId != userID

                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.menuActionReport -> reportDialog(postDocument.id)
                                else -> {}
                            }
                            false
                        }
                        popup.show()
                    }

                    // Allows user to save image to device
                    postBookmarkIcon.setOnClickListener {
                        if (itemPost.imageUrl!!.isEmpty()) {
                            KToasty.warning(context, "No image to download", Toast.LENGTH_SHORT, true).show()
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                downloadImageUsingMediaStore(context, itemPost.imageUrl)
                            } else {
                                downloadImageUsingDownloadManager(context, itemPost.imageUrl)
                            }
                        }
                    }
                }
            }
        }

        // Changes colors of hashtags
        private fun applyHashtagColor(text: String, textView: TextView) {
            val spannable = SpannableStringBuilder(text)
            val hashtagPattern = Pattern.compile("#(\\w+)")
            val matcher = hashtagPattern.matcher(text)

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val hashtag = matcher.group()

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        // Handle the hashtag click here
                        //onHashtagClick(hashtag)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(context, R.color.hazard_orange)
                        ds.isUnderlineText = false // remove underline
                    }
                }
                spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            textView.text = spannable
            textView.movementMethod = LinkMovementMethod.getInstance() // This is important to make the links clickable
        }
    }

    // Opens dialog for reporting options
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

    // Adds reported post to a new Firebase collection for review
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