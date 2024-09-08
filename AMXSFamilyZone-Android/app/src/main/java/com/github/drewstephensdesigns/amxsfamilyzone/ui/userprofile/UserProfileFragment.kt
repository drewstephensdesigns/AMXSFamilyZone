package com.github.drewstephensdesigns.amxsfamilyzone.ui.userprofile

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentUserProfileBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.models.UserPost
import com.github.drewstephensdesigns.amxsfamilyzone.ui.profile.adapter.UserPostAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts.notifyUserNoImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.squareup.picasso.Picasso

class UserProfileFragment : Fragment() {
    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var postAuthor: User
    private lateinit var firestore: FirebaseFirestore
    private lateinit var profileDocRef: DocumentReference
    private lateinit var currentUserDocRef: DocumentReference // Reference for the current user
    private var profileListener: ListenerRegistration? = null
    private var currentUserListener: ListenerRegistration? = null // Listener for the current user

    // List to store user posts
    private lateinit var profilePosts: MutableList<UserPost>

    // Adapter for the RecyclerView
    private lateinit var postAdapter: UserPostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        // Initialize Firestore and current user
        firestore = FirebaseFirestore.getInstance()

        // Retrieve the user ID from the fragment's arguments
        val userId = arguments?.getString("USER_ID")
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Set the Firestore document reference
        if (userId != null) {
            profileDocRef = firestore.collection(Consts.USER_NODE).document(userId)
        } else {
            // Handle the case where userId is null
            // For example, show an error message or navigate back
        }

        // Set the Firestore document reference for the current user
        if (currentUserId != null) {
            currentUserDocRef = firestore.collection(Consts.USER_NODE).document(currentUserId)
        } else {
            // Handle the case where currentUserId is null
            // For example, show an error message or navigate back
        }

        // Initialize the list for user posts
        profilePosts = mutableListOf()

        // Initialize the RecyclerView
        initRecyclerview()

        // Set up follow button click listener
        binding.buttonFollow.setOnClickListener {
            handleFollowButtonClick()
        }

        return binding.root
    }

    private fun initRecyclerview() {
        binding.rvPostsUserProfile.layoutManager = GridLayoutManager(requireContext(), 3)
        postAdapter = UserPostAdapter(profilePosts, { imageUrl ->
            if (imageUrl.isNullOrEmpty()) {
                notifyUserNoImage(requireContext(), "No Images")
            } else {
                showImageDialog(imageUrl)
            }
        }, {
            // not used for the user side
        })
        binding.rvPostsUserProfile.adapter = postAdapter
    }

    private fun fetchUserPosts(userId: String) {
        firestore.collection(Consts.POST_NODE)
            .whereEqualTo("user.id", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val posts = querySnapshot.toObjects(UserPost::class.java)
                profilePosts.clear()
                posts.forEach { post ->
                    if (post.id != null) {
                        profilePosts.add(post)
                    } else {
                        Log.w("UserProfileFragment", "Post with null ID found and ignored")
                    }
                }
                profilePosts.sortByDescending { it.creationTimeMs }
                postAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("UserProfileFragment", "Error Fetching Posts: ", exception)
            }
    }

    private fun updateViews(postCreator: User) {
        (activity as MainActivity).supportActionBar?.title = postCreator.name
        fetchUserPosts(postCreator.id)

        if (postCreator.imageUrl.isEmpty()) {
            binding.profileUserImage.load(Consts.DEFAULT_USER_IMAGE){
                transformations(CircleCropTransformation())
            }

        } else {
            binding.profileUserImage.load(postCreator.imageUrl) {
                crossfade(true)
                crossfade(300)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.amxs)
            }
        }
        binding.profileUserName.text = postCreator.name
        binding.profileUserBio.text = postCreator.bio
        binding.profileUserLink.text = postCreator.link

        // Update the followers and following counts
        binding.countOfFollowers.text = postCreator.getFollowersCount().toString()
        binding.countOfFollowing.text = postCreator.getFollowingCount().toString()

        binding.profileUserLink.setOnClickListener {
            openWebPage()
        }
    }

    private fun openWebPage() {
        // Handle opening the web page
    }

    private fun showImageDialog(imageUrl: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_post_view, null)
        val dialogImageView: ImageView = dialogView.findViewById(R.id.dialogImageView)

        Picasso.get().load(imageUrl).into(dialogImageView)
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setTitle(postAuthor.name)
            .create()
            .show()
    }

    override fun onStart() {
        super.onStart()
        profileListener = profileDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ProfileFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                postAuthor = snapshot.toObject(User::class.java)!!
                updateViews(postAuthor)
                updateFollowButton() // Update follow button state
            } else {
                Log.d("ProfileFragment", "Current data: null")
            }
        }

        currentUserListener = currentUserDocRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("ProfileFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Update the current user's data
            } else {
                Log.d("ProfileFragment", "Current data: null")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        profileListener?.remove()
        currentUserListener?.remove()
    }

    // Changes Button Text Between Follow/Un-Follow
    private fun updateFollowButton() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && postAuthor.followers.contains(currentUserId)) {

            binding.buttonFollow.text = getString(R.string.action_unfollow)
        } else {

            binding.buttonFollow.text = getString(R.string.action_follow)
        }
    }

    private fun handleFollowButtonClick() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null) {
            if (postAuthor.followers.contains(currentUserId)) {
                // Unfollow the user
                profileDocRef.update("followers", FieldValue.arrayRemove(currentUserId))
                currentUserDocRef.update("following", FieldValue.arrayRemove(postAuthor.id))
            } else {
                // Follow the user
                profileDocRef.update("followers", FieldValue.arrayUnion(currentUserId))
                currentUserDocRef.update("following", FieldValue.arrayUnion(postAuthor.id))
            }
        }
    }
}