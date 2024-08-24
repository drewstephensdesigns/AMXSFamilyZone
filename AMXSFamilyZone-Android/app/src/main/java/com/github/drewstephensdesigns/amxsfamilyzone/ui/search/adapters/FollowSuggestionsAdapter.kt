package com.github.drewstephensdesigns.amxsfamilyzone.ui.search.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.LayoutItemFollowSuggestionBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class FollowSuggestionsAdapter(
    options: FirestoreRecyclerOptions<User>,
    val context: Context
) : FirestoreRecyclerAdapter<User, FollowSuggestionsAdapter.SuggestionsVH>(options) {

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
                //noResultsTextView.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionsVH {
        val binding = LayoutItemFollowSuggestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuggestionsVH(binding)
    }

    override fun onBindViewHolder(holder: SuggestionsVH, position: Int, model: User) {
        holder.bind(model)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class SuggestionsVH(binding: LayoutItemFollowSuggestionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val trendingUserImage : ImageView = binding.followUserImage
        private val trendingUser: TextView = binding.followUserName
        private val trendingUserFollowers: TextView = binding.followUserFollowers
        private val followUserButton: MaterialButton = binding.buttonFollow

        fun bind(trendingUsers: User) {
            trendingUser.text = trendingUsers.name
            trendingUserFollowers.text = trendingUsers.getFollowersCount().toString()

            // Check if the current user is already following the suggested user
            val currentUserId = FirebaseUtils.firebaseAuth.currentUser?.uid
            val userId = trendingUsers.id // Assuming User model has a field id for user's UID

            if(trendingUsers.imageUrl.isEmpty()){

                trendingUserImage.load(Consts.DEFAULT_USER_IMAGE){
                    transformations(CircleCropTransformation())
                }
            } else {
                trendingUserImage.load(trendingUsers.imageUrl){
                    transformations(CircleCropTransformation())
                    crossfade(true)
                    crossfade(300)
                }
            }

            if (currentUserId != null) {
                FirebaseFirestore.getInstance().collection(Consts.USER_NODE)
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val followers = document.get("followers") as? List<String> ?: listOf()
                        if (followers.contains(currentUserId)) {
                            followUserButton.text = context.getString(R.string.action_unfollow)
                        } else {
                            followUserButton.text = context.getString(R.string.action_follow)
                        }
                    }
            }

            // Set click listener for follow/unfollow functionality
            followUserButton.setOnClickListener {
                if (followUserButton.text == context.getString(R.string.action_follow)) {
                    followUser(currentUserId, userId)
                } else {
                    unfollowUser(currentUserId, userId)
                }
            }
        }

        private fun followUser(currentUserId: String?, userId: String) {
            currentUserId?.let { currentId ->
                val userDocRef = FirebaseFirestore.getInstance().collection(Consts.USER_NODE).document(userId)
                val currentUserDocRef = FirebaseFirestore.getInstance().collection(Consts.USER_NODE).document(currentId)

                FirebaseFirestore.getInstance().runTransaction { transaction ->
                    val userSnapshot = transaction.get(userDocRef)
                    val currentUserSnapshot = transaction.get(currentUserDocRef)

                    val followersList = userSnapshot.get("followers") as? MutableList<String> ?: mutableListOf()
                    val followingList = currentUserSnapshot.get("following") as? MutableList<String> ?: mutableListOf()

                    if (!followersList.contains(currentId)) {
                        followersList.add(currentId)
                        transaction.update(userDocRef, "followers", followersList)
                    }

                    if (!followingList.contains(userId)) {
                        followingList.add(userId)
                        transaction.update(currentUserDocRef, "following", followingList)
                    }
                }.addOnSuccessListener {
                    followUserButton.text = context.getString(R.string.action_unfollow)
                }
            }
        }

        private fun unfollowUser(currentUserId: String?, userId: String) {
            currentUserId?.let { currentId ->
                val userDocRef = FirebaseFirestore.getInstance().collection(Consts.USER_NODE).document(userId)
                val currentUserDocRef = FirebaseFirestore.getInstance().collection(Consts.USER_NODE).document(currentId)

                FirebaseFirestore.getInstance().runTransaction { transaction ->
                    val userSnapshot = transaction.get(userDocRef)
                    val currentUserSnapshot = transaction.get(currentUserDocRef)

                    val followersList = userSnapshot.get("followers") as? MutableList<String> ?: mutableListOf()
                    val followingList = currentUserSnapshot.get("following") as? MutableList<String> ?: mutableListOf()

                    if (followersList.contains(currentId)) {
                        followersList.remove(currentId)
                        transaction.update(userDocRef, "followers", followersList)
                    }

                    if (followingList.contains(userId)) {
                        followingList.remove(userId)
                        transaction.update(currentUserDocRef, "following", followingList)
                    }
                }.addOnSuccessListener {
                    followUserButton.text = context.getString(R.string.action_follow)
                }
            }
        }
    }
}