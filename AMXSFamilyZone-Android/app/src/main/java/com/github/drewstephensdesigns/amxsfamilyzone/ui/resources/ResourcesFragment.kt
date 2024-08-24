package com.github.drewstephensdesigns.amxsfamilyzone.ui.resources

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentResourcesBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.FeaturedItem
import com.github.drewstephensdesigns.amxsfamilyzone.models.ResourceItem
import com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.adapter.FeaturedAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.adapter.ResourcesAdapter
import com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.viewmodel.FeaturedViewModel
import com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.viewmodel.ResourcesViewModel
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.toast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.tools.DotsIndicatorDecoration

class ResourcesFragment : Fragment(),
    FeaturedAdapter.FeaturedItemClickListener,
    ResourcesAdapter.ResourceItemClickListener{

    private var _binding: FragmentResourcesBinding? = null

    private val binding get() = _binding!!

    private var featuredAdapter : FeaturedAdapter? = null
    private var featuredViewModel : FeaturedViewModel? = null

    private var resourcesAdapter : ResourcesAdapter? = null
    private var resourcesViewModel : ResourcesViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        initFeaturedVM()
        initResourceVM()

        return binding.root
    }

    private fun initFeaturedVM(){

        binding.wingFamiliesHeader.setOnClickListener {
            handleLink("https://www.facebook.com/groups/317awfamilies/?ref=share&mibextid=NSMWBT")
        }

        binding.dyessSpousesHeader.setOnClickListener {
            handleLink("https://www.facebook.com/groups/259025040926614/?ref=share&mibextid=NSMWBT")
        }

        featuredViewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
            )[FeaturedViewModel::class.java]

        featuredViewModel!!.featuredPublications.observe(viewLifecycleOwner){featured ->
            featured.let {
                featuredAdapter = FeaturedAdapter(requireContext(), this)
                binding.featuredRv.adapter = featuredAdapter
            }
            when{
                featured.isEmpty() -> {
                    requireActivity().toast("Not Available")
                }
                featured != null ->{
                    initUI()
                    featuredAdapter?.setupFeaturedItems(featured)
                }
            }
        }
    }

    private fun initResourceVM(){
        resourcesViewModel = ViewModelProvider(requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[ResourcesViewModel::class.java]

        resourcesViewModel!!.resourceItems.observe(viewLifecycleOwner){item ->
            item.let {
                resourcesAdapter = ResourcesAdapter(requireContext(), this)
                binding.resourceRv.adapter = resourcesAdapter
            }
            when{
                item.isEmpty() -> {
                    requireActivity().toast("Not Available")
                }
                item != null ->{
                    initResourceUI()
                    resourcesAdapter?.setupResourceLinks(item)
                }
            }
        }
    }

    private fun initUI(){
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.featuredRv.layoutManager = layoutManager
        binding.featuredRv.itemAnimator = DefaultItemAnimator()

        // enforce a "snap-to-position" effect, particularly useful when you want your
        // RecyclerView to emulate the behavior of a ViewPager or a horizontal pager.
        val pageSnapper = PagerSnapHelper()
        pageSnapper.attachToRecyclerView(binding.featuredRv)

        // Dot indicator view
        val radius = resources.getDimensionPixelSize(R.dimen.dot_radius)
        val dotsHeight = resources.getDimensionPixelSize(R.dimen.dot_height)

        // Gets light/dark theme for dots indicator
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(com.google.android.material.R.attr.colorControlNormal, typedValue, true)
        val color = ContextCompat.getColor(requireContext(), typedValue.resourceId)

        // Adds page indicator to recyclerview
        binding.featuredRv.addItemDecoration(
            DotsIndicatorDecoration(
                radius,
                radius * 4,
                dotsHeight,
                color,
                color
            )
        )
    }

    private fun initResourceUI(){
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.resourceRv.layoutManager = layoutManager
        binding.resourceRv.itemAnimator = DefaultItemAnimator()
    }

    override fun onclickListener(featured: FeaturedItem) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(featured.DocumentUrl), "application/pdf")
        startActivity(intent)
    }

    private fun handleLink(link: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = (Uri.parse(link))
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(resourceItem: ResourceItem) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = (Uri.parse(resourceItem.ResourceLink))
        startActivity(intent)
    }
}