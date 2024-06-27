package com.github.drewstephensdesigns.amxsfamilyzone.models

import com.google.firebase.firestore.PropertyName

data class UserPost(
    @get:PropertyName("imageUrl") @set:PropertyName("image_url") var imageUrl: String = "",
    @get:PropertyName("time") @set:PropertyName("created_at") var creationTimeMs: Long = 0,
)
