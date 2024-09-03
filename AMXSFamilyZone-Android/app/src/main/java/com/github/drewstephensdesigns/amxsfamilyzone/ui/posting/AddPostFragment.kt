package com.github.drewstephensdesigns.amxsfamilyzone.ui.posting

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentAddPostBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    var imageUri: Uri? = null

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

    val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.postImage.visibility = View.VISIBLE
            binding.postImage.setImageURI(imageUri)
            binding.iconImage.visibility = View.GONE
        } else {
            KToasty.warning(requireContext(), "No image captured", Toast.LENGTH_SHORT, true).show()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)

        // Button to add image from device
        // Default to only one image because I didn't
        // Think of this when I built the app...
        binding.iconImage.setOnClickListener {
            launcher.launch(ActivityResultContracts.PickVisualMedia().let {
                PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
            })
        }

        binding.iconTakePhoto.setOnClickListener {

            val photoFile = createImageFile()
            imageUri = FileProvider.getUriForFile(
                requireContext(),
                "com.github.drewstephensdesigns.amxsfamilyzone.fileprovider",
                photoFile
            )

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                if(ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CAMERA), MainActivity.CAMERA_PERMISSION_REQUEST_CODE)
                } else{
                    cameraLauncher.launch(imageUri)
                }
            }else{
                cameraLauncher.launch(imageUri)
            }
        }

        // Button to add link (makes textview visible)
        binding.iconLinkImage.setOnClickListener {
            binding.linkText.visibility = View.VISIBLE
        }

        binding.btnPost.setOnClickListener {
            val text = binding.postText.text.toString()
            val link = binding.linkText.text.toString()
            if (text.isEmpty()) {
                KToasty.warning(requireContext(), "Description can't be empty", Toast.LENGTH_SHORT, true).show()
            } else {
                addPost(text, link)
                binding.postText.setText("")
                binding.linkText.setText("")
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

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
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

    private fun addPost(text: String, link: String) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUser = FirebaseUtils.firebaseAuth.currentUser

        currentUser?.uid?.let { uid ->
            firestore.collection(Consts.USER_NODE).document(uid).get().addOnCompleteListener { task ->
                val user = task.result?.toObject(User::class.java)
                //val hashtags = extractHashtags(text)
                if (imageUri != null) {
                    uploadImageAndPost(text, link, user, firestore)
                } else {
                    createPostWithoutImage(id.toString(), text, link, user, firestore, currentUser.uid)
                }
            }
        }
    }

    private fun uploadImageAndPost(text: String, link: String?, user: User?, firestore: FirebaseFirestore) {
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
                    createPost(id.toString(), text, link, downloadUri.toString(), user, firestore, currentUser?.uid.toString())
                } else {
                    KToasty.error(requireContext(), "Image upload failed: ${urlTask.exception?.message}", Toast.LENGTH_SHORT, true).show()
                }
            }
        }
    }

    private fun createPostWithoutImage(id: String,text: String,link: String, user: User?, firestore: FirebaseFirestore, creatorID: String) {
        createPost(id, text, link,null,user,firestore, creatorID)
    }

    private fun createPost(id: String,
                           text: String,
                           link: String?,
                           imageUrl: String?,
                           user: User?,
                           firestore: FirebaseFirestore,
                           creatorID: String) {

        val post = Post(id, text, link!! ,imageUrl ,user!!, System.currentTimeMillis(), creatorID)

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