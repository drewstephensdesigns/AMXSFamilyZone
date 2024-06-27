package com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.drewstephensdesigns.amxsfamilyzone.models.FeaturedItem
import com.github.drewstephensdesigns.amxsfamilyzone.models.ResourceItem
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.lang.Exception

class ResourcesViewModel(
    private val app: Application
) : AndroidViewModel(app){

    // Featured
    private var _resourceList = ArrayList<ResourceItem>()
    private val _resourceItems = MutableLiveData<List<ResourceItem>>()
    val resourceItems: MutableLiveData<List<ResourceItem>>
        get() = _resourceItems

    init {
        fetchDocs()
    }

    private fun fetchDocs() {
        viewModelScope.launch {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl(Consts.FEATURED_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ApiService::class.java)
                val response = withContext(Dispatchers.IO){
                    service.getFeaturedItems()
                }

                _resourceList.clear()
                _resourceList.addAll(response)

                _resourceItems.postValue(_resourceList)
            } catch (e : Exception){
                showErrorToast(e)
            }
        }
    }

    private fun showErrorToast(e: Exception){
        Toast.makeText(app.applicationContext, "An error occured: $e", Toast.LENGTH_SHORT).show()
    }

    interface ApiService{
        @GET("resources.json")
        suspend fun getFeaturedItems() : List<ResourceItem>
    }
}