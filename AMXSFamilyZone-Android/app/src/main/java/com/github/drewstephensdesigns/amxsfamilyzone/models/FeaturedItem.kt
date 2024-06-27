package com.github.drewstephensdesigns.amxsfamilyzone.models

import com.google.gson.annotations.SerializedName

data class FeaturedItem(
    @SerializedName("PubID")
    var PubID: Int,

    @SerializedName("Number")
    var Number: String? = "",

    @SerializedName("Title")
    var Title: String? = "",

    @SerializedName("OPR")
    var OPR: String? = "",

    @SerializedName("DocumentUrl")
    var DocumentUrl: String? = "",
)
