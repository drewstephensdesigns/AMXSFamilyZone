package com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.LayoutFeaturedItemsBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.FeaturedItem
import com.google.android.material.card.MaterialCardView

class FeaturedAdapter(
    private val ct: Context,
    val clickListener : FeaturedItemClickListener
) : RecyclerView.Adapter<FeaturedAdapter.FeaturedVH>() {

    private var featuredItems : List<FeaturedItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedVH {
        val binding = LayoutFeaturedItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedVH(binding)
    }

    override fun getItemCount(): Int {
        return featuredItems.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onBindViewHolder(holder: FeaturedVH, position: Int) {
        val featuredItem = featuredItems[position]
        holder.bind(featuredItem)

    }

    fun setupFeaturedItems(itemsFeatured : List<FeaturedItem>){
        featuredItems = itemsFeatured
        notifyDataSetChanged()
    }

    inner class FeaturedVH(binding: LayoutFeaturedItemsBinding) : RecyclerView.ViewHolder(binding.root) {
        private var cardView: MaterialCardView = binding.singlePubCard
        private var featuredTextTitle: TextView = binding.singlePubTitle
        private var featuredTextNumber: TextView = binding.singlePubNumber
        private var featuredTextOPR : TextView = binding.pubOpr

        fun bind(featured: FeaturedItem) {

            featuredTextTitle.text = featured.Number
            featuredTextNumber.text = featured.Title
            featuredTextOPR.text = featured.OPR

            val colorResources = intArrayOf(
                R.color.charger_red,
                R.color.purple_500,
                R.color.eagle_blue,
                R.color.hazard_orange
            )

            val colorIndex = bindingAdapterPosition % colorResources.size
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    ct,
                    colorResources[colorIndex]
                )
            )





            cardView.setOnClickListener {
                clickListener.onclickListener(featuredItems[bindingAdapterPosition])
            }
        }
    }

    interface FeaturedItemClickListener{
        fun onclickListener(featured : FeaturedItem)
    }
}