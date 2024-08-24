package com.github.drewstephensdesigns.amxsfamilyzone.ui.posting

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentAddPostBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private var imageUri: Uri? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { result ->
        if (result == null) {
            KToasty.warning(requireContext(), "No image selected", Toast.LENGTH_SHORT, true).show()
        } else {
            binding.postImage.visibility = View.VISIBLE
            binding.postImage.setImageURI(result)
            binding.iconImage.visibility = View.GONE
            imageUri = result
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)

        binding.iconImage.setOnClickListener {
            launcher.launch(ActivityResultContracts.PickVisualMedia().let {
                PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
            })
        }

        binding.btnPost.setOnClickListener {
            val text = binding.postText.text.toString()
            if (text.isEmpty()) {
                KToasty.warning(requireContext(), "Description can't be empty", Toast.LENGTH_SHORT, true).show()
            } else {
                addPost(text)
                binding.postText.setText("")
                findNavController().navigate(R.id.navigation_home)
            }
        }

        // Create and add TextWatcher to change hashtag color
        val hashtagWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateHashtagColors(s, this)
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.postText.addTextChangedListener(hashtagWatcher)

        return binding.root
    }

    private fun updateHashtagColors(s: CharSequence?, watcher: TextWatcher) {
        if (s.isNullOrEmpty()) return

        val spannable = SpannableStringBuilder(s)
        val hashtagPattern = Pattern.compile("#(\\w+)")
        val matcher = hashtagPattern.matcher(s)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.eagle_blue)), // Change to your desired color
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.postText.removeTextChangedListener(watcher)
        binding.postText.text = spannable
        binding.postText.setSelection(spannable.length)
        binding.postText.addTextChangedListener(watcher)
    }

    private fun extractHashtags(text: String): List<String> {
        val hashtagPattern = Pattern.compile("#(\\w+)")
        val matcher = hashtagPattern.matcher(text)
        val hashtags = mutableListOf<String>()

        while (matcher.find()) {
            matcher.group(1)?.let { hashtags.add(it) }
        }
        return hashtags
    }

    private fun addPost(text: String) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = FirebaseUtils.firebaseAuth.currentUser

        currentUser?.uid?.let { uid ->
            firestore.collection(Consts.USER_NODE).document(uid).get().addOnCompleteListener { task ->
                val user = task.result?.toObject(User::class.java)
                val hashtags = extractHashtags(text)
                if (imageUri != null) {
                    uploadImageAndPost(text, user, firestore)
                } else {
                    createPostWithoutImage(id.toString(), text, user, firestore, currentUser.uid)
                }
            }
        }
    }

    private fun uploadImageAndPost(text: String, user: User?, firestore: FirebaseFirestore) {
        val currentUser = FirebaseUtils.firebaseAuth.currentUser
        val storageRef = FirebaseStorage.getInstance().reference.child("Images")
            .child("${currentUser?.email}_${System.currentTimeMillis()}.jpg")

        imageUri?.let { uri ->
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream().apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, this)
            }
            val reducedImage = byteArrayOutputStream.toByteArray()

            storageRef.putBytes(reducedImage).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }.addOnCompleteListener { urlTask ->
                if (urlTask.isSuccessful) {
                    val downloadUri = urlTask.result
                    createPost(id.toString(), text, downloadUri.toString(), user, firestore, currentUser?.uid.toString())
                } else {
                    KToasty.error(requireContext(), "Image upload failed: ${urlTask.exception?.message}", Toast.LENGTH_SHORT, true).show()
                }
            }
        }
    }

    private fun createPostWithoutImage(id: String,text: String, user: User?, firestore: FirebaseFirestore, creatorID: String) {
        createPost(id, text, null,user,firestore, creatorID)
    }

    private fun createPost(id: String,
                           text: String,
                           imageUrl: String?,
                           user: User?,
                           firestore: FirebaseFirestore,
                           creatorID: String) {

        val post = Post(id, text, imageUrl ,user!!, System.currentTimeMillis(), creatorID)

        firestore.collection(Consts.POST_NODE).document().set(post).addOnCompleteListener { postTask ->
            if (postTask.isSuccessful) {
                KToasty.success(requireContext(), "Posted Successfully!", Toast.LENGTH_SHORT, true).show()
            } else {
                KToasty.error(requireContext(), "Error occurred!", Toast.LENGTH_SHORT, true).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}