package com.github.drewstephensdesigns.amxsfamilyzone.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentHomeBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.Post
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
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

    private fun setupRecyclerView(){
        val firestore = FirebaseFirestore.getInstance()
        val query = firestore.collection(Consts.POST_NODE).orderBy("time", Query.Direction.DESCENDING)

        val recyclerViewOptions = FirestoreRecyclerOptions.Builder<Post>()
            .setQuery(query, Post::class.java)
            .setLifecycleOwner(this)
            .build()


        context?.let {
            adapter = HomeAdapter(recyclerViewOptions, it, noResultsTextView)
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