package com.github.drewstephensdesigns.amxsfamilyzone.ui.search.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.LayoutItemTrendingPostBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore

class TrendingPostAdapter(
    options: FirestoreRecyclerOptions<Post>,
    val context: Context
) : FirestoreRecyclerAdapter<Post, TrendingPostAdapter.TrendingVH>(options) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingVH {
        val binding = LayoutItemTrendingPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrendingVH(binding)
    }

    override fun onBindViewHolder(holder: TrendingVH, position: Int, model: Post) {
        holder.bind(model)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class TrendingVH(binding: LayoutItemTrendingPostBinding) : RecyclerView.ViewHolder(binding.root){
        private val postAuthor : TextView = binding.trendingAuthor
        private val postDescription : TextView = binding.trendingDescription

        fun bind(trendingPost : Post){
            postAuthor.text = trendingPost.user.name
            postDescription.text = trendingPost.text

            val firestore = FirebaseFirestore.getInstance()
            val userID = FirebaseUtils.firebaseAuth.currentUser?.uid
            val postDoc = firestore
                .collection(Consts.POST_NODE)
                .document(snapshots.getSnapshot(bindingAdapterPosition).id)

            postDoc.get().addOnCompleteListener {
                if (it.isSuccessful){
                    it.result?.toObject(Post::class.java)
                }
            }
        }
    }
}