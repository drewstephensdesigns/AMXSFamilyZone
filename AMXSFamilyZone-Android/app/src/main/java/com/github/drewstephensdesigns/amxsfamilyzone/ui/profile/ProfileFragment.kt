package com.github.drewstephensdesigns.amxsfamilyzone.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentProfileBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.models.UserPost
import com.github.drewstephensdesigns.amxsfamilyzone.ui.profile.adapter.UserPostAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts.POST_NODE
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts.USER_NODE
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts.notifyUserNoImage
import com.github.drewstephensdesigns.amxsfamilyzone.utils.UserUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.dialog.MaterialDialogs
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.option.DisplayMode
import com.maxkeppeler.sheets.option.Option
import com.maxkeppeler.sheets.option.OptionSheet
import com.squareup.picasso.Picasso


/**
 * Fragment class for displaying the user's profile.
 */
class ProfileFragment : Fragment() {

    // View binding for the fragment
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // List to store user posts
    private lateinit var profilePosts: MutableList<UserPost>

    // Adapter for the RecyclerView
    private lateinit var postAdapter: UserPostAdapter

    // Current user and Firestore references
    private lateinit var currentUser: User

    private lateinit var firestore: FirebaseFirestore
    private lateinit var profileDocRef: DocumentReference

    private var profileListener: ListenerRegistration? = null
    private var countPost: Int = 0

    // Inflates the fragment's layout and initializes necessary components.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Initialize the list for user posts
        profilePosts = mutableListOf()

        // Initialize the RecyclerView
        initRecyclerview()

        // Initialize Firestore and current user
        firestore = FirebaseFirestore.getInstance()
        currentUser = UserUtil.user!!

        // Set the Firestore document reference
        profileDocRef = firestore.collection(USER_NODE).document(currentUser.id)

        // Update views with the current user's information
        updateViews(currentUser)

        // Set up the edit profile button click listener
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_edit_profile)
        }

        // Clicking profile image opens larger image to view
        binding.userImage.setOnClickListener { showUserImageDialog(currentUser.imageUrl) }
        (activity as MainActivity).supportActionBar?.title = currentUser.name

        return binding.root
    }

    // Sets up the Firestore listener for real-time updates when the fragment starts.
    override fun onStart() {
        super.onStart()
        profileListener = profileDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ProfileFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                currentUser = snapshot.toObject(User::class.java)!!
                updateViews(currentUser)
            } else {
                Log.d("ProfileFragment", "Current data: null")
            }
        }
    }

    // Removes the Firestore listener to avoid memory leaks when the fragment stops.
    override fun onStop() {
        super.onStop()
        profileListener?.remove()
    }

    //Initializes the RecyclerView with a grid layout and sets its adapter
    private fun initRecyclerview() {
        binding.rvPhoto.layoutManager = GridLayoutManager(requireContext(), 3)
        postAdapter = UserPostAdapter(profilePosts, { imageUrl ->
            if (imageUrl.isNullOrEmpty()) {
                notifyUserNoImage(requireContext(), "No Images")
            } else {
                showImageDialog(UserPost(imageUrl = imageUrl))
            }
        }, { post ->
            showEditPostDialog(post.id.toString(), post.description, post.imageUrl)
        })
        binding.rvPhoto.adapter = postAdapter
    }

    private fun showEditPostDialog(postId: String, postDescription: String, imageUrl: String) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_post, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_post_text)
        val updateButton = dialogView.findViewById<MaterialButton>(R.id.button_update_post)
        val deleteButton = dialogView.findViewById<MaterialButton>(R.id.button_delete_post)
        editText.setText(postDescription)

        val alertDialog = MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()
        alertDialog.show()

        updateButton.setOnClickListener {
            val newText = editText.text.toString().trim()
            if (newText.isNotEmpty()) {
                updatePost(postId, newText)
                alertDialog.dismiss()
            } else {
                KToasty.warning(
                    requireContext(),
                    "Post text cannot be empty",
                    Toast.LENGTH_SHORT,
                    true
                ).show()
            }
        }

        deleteButton.setOnClickListener {
            deletePost(postId, imageUrl)
            alertDialog.dismiss()
        }
    }

    // Allows user to update post description
    private fun updatePost(postId: String, newDescription: String) {
        Log.d("DeletePost", "Post ID: $postId")

        val firestore = FirebaseFirestore.getInstance()
        val docRef = firestore.collection(POST_NODE).document(postId)

        docRef.update("text", newDescription)
            .addOnSuccessListener {
                KToasty.success(
                    requireContext(),
                    "Post updated successfully",
                    Toast.LENGTH_SHORT,
                    true
                ).show()
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                KToasty.error(
                    requireContext(),
                    "Error updating post: ${exception.message}",
                    Toast.LENGTH_SHORT,
                    true
                ).show()
            }
    }

    // Allows user to delete their post and removes image from firebase
    private fun deletePost(postId: String, imageUrl: String) {

        // First, delete the image from Firebase Storage
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        storageRef.delete().addOnSuccessListener {

            // Image deleted successfully, now delete the Firestore document
            val postRef = FirebaseFirestore
                .getInstance()
                .collection(POST_NODE)
                .document(postId)

            postRef
                .delete()
                .addOnSuccessListener {
                    // Firestore document deleted successfully
                    KToasty.success(requireContext(), "Post deleted successfully", Toast.LENGTH_SHORT).show()
                    postAdapter.notifyDataSetChanged()
                    updateViews(currentUser)
            }.addOnFailureListener { e ->
                // Firestore document deletion failed
                KToasty.error(requireContext(), "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            // Image deletion failed
            KToasty.error(requireContext(), "Failed to delete image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Shows users photos in AlertDialog View
    private fun showImageDialog(post: UserPost) {

        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_view, null)
        val dialogImageView: ImageView = dialogView.findViewById(R.id.dialogImageView)
        val vertMenu: ImageView = dialogView.findViewById(R.id.menuIcon)

        Picasso.get().load(post.imageUrl).into(dialogImageView)
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setTitle(currentUser.name)
            .create()
            .show()

        vertMenu.setOnClickListener {
            OptionSheet().show(requireContext()) {
                style(SheetStyle.BOTTOM_SHEET)
                displayMode(DisplayMode.LIST)
                with(
                    Option("Share", "share image with your friends"),
                )
                onPositive { index: Int, _: Option ->
                    when (index) {
                        0 -> {
                            sharePost(post.imageUrl)
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun sharePost(imageUrl: String){
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this image: $imageUrl")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Image URL"))
    }

    // Shows profile image in an AlertDialog View
    private fun showUserImageDialog(userImage: String) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_view, null)
        val dialogImageView: ImageView = dialogView.findViewById(R.id.dialogImageView)

        Picasso.get().load(userImage).into(dialogImageView)
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .create()
            .show()
    }

    // Fetches the user's posts from Firestore and updates the RecyclerView.
    // @param displayName The display name of the user whose posts are to be fetched.
    private fun fetchUserPosts(displayName: String) {
        firestore.collection(POST_NODE)
            .whereEqualTo("user.name", displayName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                countPost = querySnapshot.size()
                Log.e("Count Post", "$countPost")
                profilePosts.clear()
                for (document in querySnapshot.documents) {
                    val userPost = document.toObject(UserPost::class.java)
                    if (userPost != null) {
                        userPost.id = document.id // Ensure this line is present
                        profilePosts.add(userPost)
                    }
                }
                if (profilePosts.isNotEmpty()) {
                    binding.emptyText.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.VISIBLE
                }

                profilePosts.sortByDescending { it.creationTimeMs }
                postAdapter.notifyDataSetChanged()
                binding.totalPosts.text = countPost.toString()
            }
            .addOnFailureListener { exception ->
                Log.e("TAG", "Error Fetching Posts :", exception)
            }
    }


    // Updates the fragment's views with the current user's information.
    // @param currentUser The user whose information is to be displayed.
    private fun updateViews(currentUser: User) {
        fetchUserPosts(currentUser.name)
        binding.userName.text = currentUser.name
        binding.userBio.text = currentUser.bio
        binding.link.text = currentUser.link
        binding.accountCreated.text =
            resources.getString(R.string.account_created_text, currentUser.getCreatedDate())

        binding.noOfFollowers.text = currentUser.getFollowersCount().toString()
        binding.noOfFollowings.text = currentUser.getFollowingCount().toString()

        if (currentUser.imageUrl.isEmpty()) {
            binding.userImage.setImageResource(R.drawable.amxs)
        } else {
            binding.userImage.load(currentUser.imageUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.amxs)
            }
        }
    }

    // Cleans up the view binding when the view is destroyed.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}