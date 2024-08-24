package com.github.drewstephensdesigns.amxsfamilyzone.ui.profile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.LayoutUserItemBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.UserPost
import kotlin.math.truncate

/**
 * Adapter class for displaying user posts in a RecyclerView.
 * @param profilePosts A mutable list of UserPost objects representing the posts to be displayed.
 */
class UserPostAdapter(
    private val profilePosts: MutableList<UserPost>,
    private val onItemLongClick: (String?) -> Unit,
    private val editPostClickListener: (UserPost) -> Unit
)
    : RecyclerView.Adapter<UserPostAdapter.UserPostVH>() {

    /**
     * Creates and returns a ViewHolder object, inflating the layout for each item in the RecyclerView.
     * @param parent The ViewGroup into which the new view will be added.
     * @param viewType The view type of the new View.
     * @return A new UserPostVH that holds a View of the given type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostVH {
        val binding = LayoutUserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserPostVH(binding)
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The size of the profilePosts list.
     */
    override fun getItemCount(): Int {
        return profilePosts.size
    }

    /**
     * Binds the data from the profilePosts list to the ViewHolder.
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: UserPostVH, position: Int) {
        val post = profilePosts[position]
        holder.bind(post)
    }

    /**
     * ViewHolder class for the UserPostAdapter.
     * @param binding The binding object for the item layout.
     */
    inner class UserPostVH(binding: LayoutUserItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var userPostedImage: ImageView = binding.ivPostPhoto

        /**
         * Binds a UserPost object to the ViewHolder.
         * @param userItemPost The UserPost object to be bound to the ViewHolder.
         **/
        fun bind(userItemPost: UserPost) {
            userPostedImage.load(userItemPost.imageUrl){
                crossfade(true)
                crossfade(500)
                placeholder(R.drawable.amxs)
                error(com.droidman.ktoasty.R.drawable.ic_error_outline_white_48dp)
            }
            userPostedImage.setOnLongClickListener {
                onItemLongClick(userItemPost.imageUrl)
                true
            }

            userPostedImage.setOnClickListener {
                editPostClickListener(userItemPost)
            }
        }
    }
}