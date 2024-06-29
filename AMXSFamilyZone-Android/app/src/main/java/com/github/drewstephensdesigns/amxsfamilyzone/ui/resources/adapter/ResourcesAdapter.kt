package com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.LayoutResourceItemsBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.ResourceItem
import com.google.android.material.card.MaterialCardView

class ResourcesAdapter(
    private val ct: Context, val resourceItemClickListener: ResourceItemClickListener
) : RecyclerView.Adapter<ResourcesAdapter.ResourcesVH>() {

    private var resources: List<ResourceItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourcesVH {
        val binding =
            LayoutResourceItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResourcesVH(binding)
    }

    override fun getItemCount(): Int {
        return resources.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: ResourcesVH, position: Int) {
        val resourceItem = resources[position]
        holder.bind(resourceItem)
    }

    fun setupResourceLinks(resourceItemLinks : List<ResourceItem>){
        resources = resourceItemLinks
        notifyDataSetChanged()

    }

    inner class ResourcesVH(binding: LayoutResourceItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var resourceCardView: MaterialCardView = binding.cardView
        private var resourceTitle: TextView = binding.resourceName
        private var resourceDescription : TextView = binding.resourceDescription

        fun bind(resource: ResourceItem) {
            resourceTitle.text = resource.ResourceName
            resourceDescription.text = resource.ResourceDescription

            val drawableArray = intArrayOf(
                R.drawable.ic_health,
                R.drawable.ic_emergency,
                R.drawable.ic_call,
                R.drawable.ic_link,
                R.drawable.ic_travel_explore,
                R.drawable.ic_phone,
                R.drawable.ic_stress_management,
                R.drawable.ic_hospital,
                R.drawable.ic_volunteer,
                R.drawable.ic_patient,
                R.drawable.ic_dentistry,
                R.drawable.ic_med_admin,
                R.drawable.ic_toys_games,
                R.drawable.ic_travel_explore,
                R.drawable.ic_school,
                R.drawable.ic_fact_check,
                R.drawable.ic_volunteer,
                R.drawable.ic_stress_management,
                R.drawable.ic_school,
                R.drawable.ic_diversity,
                R.drawable.ic_healing,
                R.drawable.ic_local_activity,
                R.drawable.ic_diversity_group,
                R.drawable.ic_global
            )

            // Adds drawables above to textviews for resources
            val drawableIndex = bindingAdapterPosition % drawableArray.size
            resourceTitle.setCompoundDrawablesWithIntrinsicBounds(
                drawableArray[drawableIndex],
                0,
                0,
                0
            )

            // Set colored border on MaterialCardView
            // First 3 positions get colored border to
            // easily denote crisis response
            if (bindingAdapterPosition < 3) {
                resourceCardView.strokeWidth = 2 // You can adjust the width as per your requirement
                resourceCardView.strokeColor = ContextCompat.getColor(ct, R.color.hazard_orange) // Set your desired color
            } else {
                // Remove border
                resourceCardView.strokeWidth = 0
            }

            resourceCardView.setOnClickListener {
                resourceItemClickListener.onItemClick(resources[bindingAdapterPosition])
            }
        }
    }

    interface ResourceItemClickListener {
        fun onItemClick(resourceItem: ResourceItem)
    }
}