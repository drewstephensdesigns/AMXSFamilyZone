package com.github.drewstephensdesigns.amxsfamilyzone.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentHomeBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var adapter: HomeAdapter

    private lateinit var noResultsTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        noResultsTextView = binding.noResultsFoundText
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    // Firestore Query Update: Added a condition to exclude posts created by the current user using .whereNotEqualTo("creatorId", currentUserId).
    // Order By Clause: Retained the order by time clause to sort the posts by time in descending order.
    // This ensures that your Firestore query will only fetch posts not created by the currently logged-in user and display them in the RecyclerView.
    private fun setupRecyclerView() {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseUtils.firebaseAuth.currentUser?.uid

        // Firestore query to exclude posts by the current user
        val query = firestore.collection(Consts.POST_NODE)
            //.whereNotEqualTo("creatorId", currentUserId)
            .orderBy("creatorId") // Ensure this field matches the index order
            .orderBy("time", Query.Direction.DESCENDING) // Ensure this field matches the index order

        val recyclerViewOptions = FirestoreRecyclerOptions.Builder<Post>()
            .setQuery(query, Post::class.java)
            .setLifecycleOwner(this)
            .build()

        context?.let {
            adapter = HomeAdapter(recyclerViewOptions, it, noResultsTextView) { postCreator ->
                val bundle = Bundle().apply {
                    putString("USER_ID", postCreator.id)
                }
                findNavController().navigate(R.id.navigation_user_profile, bundle)
            }
        }

        if (this::adapter.isInitialized) {
            binding.feedRecyclerView.adapter = adapter
        }

        binding.feedRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.feedRecyclerView.itemAnimator = null

        adapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}