package com.github.drewstephensdesigns.amxsfamilyzone.ui.resources.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.drewstephensdesigns.amxsfamilyzone.models.FeaturedItem
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import java.lang.Exception

class FeaturedViewModel(
    private val app: Application
) : AndroidViewModel(app){

    // Featured
    private var _featuredList = ArrayList<FeaturedItem>()
    private val _featuredPublications = MutableLiveData<List<FeaturedItem>>()
    val featuredPublications: MutableLiveData<List<FeaturedItem>>
        get() = _featuredPublications

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

               _featuredList.clear()
                _featuredList.addAll(response)

                _featuredPublications.postValue(_featuredList)
            } catch (e : Exception){
                showErrorToast(e)
            }
        }
    }

    private fun showErrorToast(e: Exception){
        Toast.makeText(app.applicationContext, "An error occured: $e", Toast.LENGTH_SHORT).show()
    }

    interface ApiService{
        @GET("data.json")
        suspend fun getFeaturedItems() : List<FeaturedItem>
    }
}