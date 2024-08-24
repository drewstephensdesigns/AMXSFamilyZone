package com.github.drewstephensdesigns.amxsfamilyzone.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentSearchBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.ui.search.adapters.FollowSuggestionsAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.ui.search.adapters.TrendingPostAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var trendingPostAdapter: TrendingPostAdapter
    private lateinit var suggestionsAdapter: FollowSuggestionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTrendingRecyclerview()
        setupSuggestedUsersRecyclerview()
    }

    private fun setupTrendingRecyclerview() {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseUtils.firebaseAuth.currentUser?.uid

        val query = firestore.collection(Consts.POST_NODE)
            .whereNotEqualTo("creatorId", currentUserId)
            .orderBy("creatorId")
            .orderBy("time", Query.Direction.DESCENDING)

        val recyclerViewOptions = FirestoreRecyclerOptions.Builder<Post>()
            .setQuery(query, Post::class.java)
            .setLifecycleOwner(this)
            .build()

        context?.let {
            trendingPostAdapter = TrendingPostAdapter(recyclerViewOptions, it)
        }

        if (this::trendingPostAdapter.isInitialized) {
            binding.recyclerTrendingPosts.adapter = trendingPostAdapter
        }

        binding.recyclerTrendingPosts.layoutManager = LinearLayoutManager(activity)
        binding.recyclerTrendingPosts.itemAnimator = DefaultItemAnimator()
        trendingPostAdapter.notifyDataSetChanged()
    }

    private fun setupSuggestedUsersRecyclerview() {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseUtils.firebaseAuth.currentUser?.email

        val query = firestore.collection(Consts.USER_NODE)
            .whereNotEqualTo("email", currentUserId)
            .orderBy("name")
            .orderBy("accountCreated", Query.Direction.DESCENDING)

        val trendingUsersRecyclerOptions = FirestoreRecyclerOptions.Builder<User>()
            .setQuery(query, User::class.java)
            .setLifecycleOwner(this)
            .build()

        context?.let {
            suggestionsAdapter = FollowSuggestionsAdapter(trendingUsersRecyclerOptions, it)
        }

        if (this::suggestionsAdapter.isInitialized) {
            binding.recyclerFollowSuggestions.adapter = suggestionsAdapter
        }

        binding.recyclerFollowSuggestions.layoutManager = LinearLayoutManager(activity)
        binding.recyclerFollowSuggestions.itemAnimator = DefaultItemAnimator()
        suggestionsAdapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        trendingPostAdapter.startListening()
        suggestionsAdapter.startListening()  // Ensure the adapter starts listening for changes
    }

    override fun onStop() {
        super.onStop()
        trendingPostAdapter.stopListening()
        suggestionsAdapter.stopListening()  // Stop listening to Firestore changes
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}