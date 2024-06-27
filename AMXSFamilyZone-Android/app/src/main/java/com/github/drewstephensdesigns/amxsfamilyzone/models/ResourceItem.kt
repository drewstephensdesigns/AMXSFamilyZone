package com.github.drewstephensdesigns.amxsfamilyzone.models

import com.google.gson.annotations.SerializedName

data class ResourceItem(
    @SerializedName("ResourceID")
    var ResourceID : String = "",

    @SerializedName("ResourceName")
    var ResourceName : String = "",

    @SerializedName("ResourceDescription")
    var ResourceDescription : String = "",

    @SerializedName("ResourceLink")
    var ResourceLink : String = ""
)
