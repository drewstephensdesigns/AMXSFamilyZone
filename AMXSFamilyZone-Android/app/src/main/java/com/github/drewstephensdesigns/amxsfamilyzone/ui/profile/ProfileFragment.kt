package com.github.drewstephensdesigns.amxsfamilyzone.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.droidman.ktoasty.KToasty
import com.github.drewstephensdesigns.amxsfamilyzone.MainActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentProfileBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.models.UserPost
import com.github.drewstephensdesigns.amxsfamilyzone.ui.profile.adapter.UserPostAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.utils.UserUtil
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    /**
     * Inflates the fragment's layout and initializes necessary components.
     */
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
        profileDocRef = firestore.collection("Users").document(currentUser.id)

        // Update views with the current user's information
        updateViews(currentUser)

        // Set up the edit profile button click listener
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_edit_profile)
        }

        // Clicking profile image opens larger image to view
        binding.userImage.setOnClickListener {
            showUserImageDialog(currentUser.imageUrl)
        }
        (activity as MainActivity).supportActionBar?.title = currentUser.name

        return binding.root
    }

    /**
     * Sets up the Firestore listener for real-time updates when the fragment starts.
     */
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

    /**
     * Removes the Firestore listener to avoid memory leaks when the fragment stops.
     */
    override fun onStop() {
        super.onStop()
        profileListener?.remove()
    }

    /**
     * Initializes the RecyclerView with a grid layout and sets its adapter.
     */
    private fun initRecyclerview() {
        binding.rvPhoto.layoutManager = GridLayoutManager(requireContext(), 3)
        postAdapter = UserPostAdapter(profilePosts) { imageUrl ->
            if (imageUrl.isNullOrEmpty()) {
                notifyUserNoImage()
            } else {
                showImageDialog(imageUrl)
            }
        }
        binding.rvPhoto.adapter = postAdapter
    }

    private fun notifyUserNoImage() {
        KToasty.warning(requireContext(), "This post does not have an image", Toast.LENGTH_SHORT, true).show()
    }

    // Shows users photos in AlertDialog View
    private fun showImageDialog(imageUrl: String) {

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_view, null)
        val dialogImageView: ImageView = dialogView.findViewById(R.id.dialogImageView)

        Picasso.get().load(imageUrl).into(dialogImageView)

        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setTitle(currentUser.name)
            //.setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }

    // Shows profile image in an AlertDialog View
    private fun showUserImageDialog(userImage: String){
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_image_view, null)
        val dialogImageView: ImageView = dialogView.findViewById(R.id.dialogImageView)

        Picasso.get().load(userImage).into(dialogImageView)

        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setTitle(currentUser.name)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }

    /**
     * Fetches the user's posts from Firestore and updates the RecyclerView.
     *
     * @param displayName The display name of the user whose posts are to be fetched.
     */
    private fun fetchUserPosts(displayName: String) {
        firestore.collection("Post")
            .whereEqualTo("user.name", displayName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                countPost = querySnapshot.size()
                Log.e("Count Post", "$countPost")
                profilePosts.clear()
                val posts = querySnapshot.toObjects(UserPost::class.java)
                profilePosts.addAll(posts)
               if (profilePosts.isNotEmpty()){
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

    /**
     * Updates the fragment's views with the current user's information.
     *
     * @param currentUser The user whose information is to be displayed.
     */
    private fun updateViews(currentUser: User) {
        fetchUserPosts(currentUser.name)
        binding.userName.text = currentUser.name
        binding.userBio.text = currentUser.bio
        binding.link.text = currentUser.link
        binding.accountCreated.text = resources.getString(R.string.account_created_text, currentUser.getCreatedDate())

        if (currentUser.imageUrl.isEmpty()){
            Picasso.get()
                .load(R.drawable.amxs)
                //.rotate(90F)
                .into(binding.userImage)
        } else {
            Picasso.get()
                .load(currentUser.imageUrl)
                .placeholder(R.drawable.ic_person)
                //.rotate(90F)
                .into(binding.userImage)
        }
    }

    /**
     * Cleans up the view binding when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}