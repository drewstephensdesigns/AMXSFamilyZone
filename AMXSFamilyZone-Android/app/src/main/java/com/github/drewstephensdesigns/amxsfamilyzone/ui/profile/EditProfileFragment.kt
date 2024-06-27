package com.github.drewstephensdesigns.amxsfamilyzone.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentEditProfileBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.toast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.UserUtil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class EditProfileFragment : Fragment() {

    private var _binding : FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null

    private val launcher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { result ->
        if (result == null) {
            activity?.toast("No image selected")
        } else {
            binding.profileImage.setImageURI(result)
            imageUri = result
            uploadUserImage()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(layoutInflater, container, false)

        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as MainActivity).supportActionBar?.setHomeButtonEnabled(true)

        val imageUrl = UserUtil.user?.imageUrl
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(imageUrl)
                //.rotate(90F)
                .placeholder(R.drawable.amxs) // Replace with your placeholder image resource
                .error(R.drawable.ic_person) // Replace with your error image resource
                .into(binding.profileImage)
        } else {
            // Load a default or placeholder image if imageUrl is null or empty
            Picasso.get()
                .load(R.drawable.amxs) // Replace with your default profile image resource
                .into(binding.profileImage)
        }

        setupMenuProvider()
        UserUtil.getCurrentUser()
        populateUserData()

        binding.changePicture.setOnClickListener {
            launcher.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }

        binding.updateButton.setOnClickListener {
            updateUserProfile()
        }

        return binding.root
    }

    private fun populateUserData() {
        binding.userEditname.setText(UserUtil.user?.name)
        binding.userEditBio.setText(UserUtil.user?.bio)
        binding.userEditLink.setText(UserUtil.user?.link)

        if (UserUtil.user?.imageUrl?.isEmpty() == true) {
            Picasso.get()
                .load(R.drawable.amxs)
                .into(binding.profileImage)
        } else {
            Picasso.get()
                .load(UserUtil.user?.imageUrl)
                .rotate(90F)
                .into(binding.profileImage)
        }
    }

    private fun updateUserProfile() {
        val newUserName = binding.userEditname.text.toString()
        val newBio = binding.userEditBio.text.toString()
        val newLink = binding.userEditLink.text.toString()

        if (newUserName.isEmpty()) {
            activity?.toast("Name is required!")
            return
        }

        val user = User(
            id = UserUtil.user!!.id,
            name = newUserName,
            email = UserUtil.user!!.email,
            following = UserUtil.user!!.following,
            bio = newBio,
            imageUrl = UserUtil.user!!.imageUrl,
            link = newLink,
            accountCreated = UserUtil.user!!.accountCreated
        )

        val userDoc = FirebaseFirestore.getInstance()
            .collection("Users")
            .document(user.id)

        userDoc.set(user).addOnCompleteListener {
            if (it.isSuccessful) {
                activity?.toast("Profile Updated!")
                Log.i("EDIT PROFILE", newBio)
                findNavController().popBackStack()
            } else {
                activity?.toast("Something went wrong!")
            }
        }
    }

    private fun uploadUserImage() {
        val storage = FirebaseStorage.getInstance().reference.child("Images")
            .child("${UserUtil.user?.email}_${System.currentTimeMillis()}.jpg")

        val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, byteArrayOutputStream)
        val reducedImage = byteArrayOutputStream.toByteArray()

        val uploadTask = storage.putBytes(reducedImage)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storage.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                updateUserImage(downloadUri.toString())
            } else {
                activity?.toast("Something went wrong")
            }
        }
    }

    private fun updateUserImage(imageUrl: String) {
        val firestore = FirebaseFirestore.getInstance()
        val user = User(
            id = UserUtil.user!!.id,
            name = UserUtil.user!!.name,
            email = UserUtil.user!!.email,
            following = UserUtil.user!!.following,
            bio = UserUtil.user!!.bio,
            imageUrl = imageUrl,
            link = UserUtil.user!!.link,
            accountCreated = UserUtil.user!!.accountCreated
        )

        firestore.collection("Users")
            .document(user.id)
            .set(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    activity?.toast("Image Uploaded")
                } else {
                    activity?.toast("Something went wrong")
                }
            }
    }

    private fun setupMenuProvider(){
        requireActivity().addMenuProvider(object : MenuProvider{
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when(menuItem.itemId){
                    R.id.navigation_home -> { findNavController().popBackStack() }
                    else -> { false }
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}