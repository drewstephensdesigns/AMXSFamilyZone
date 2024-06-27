package com.github.drewstephensdesigns.amxsfamilyzone.utils

import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.google.firebase.firestore.FirebaseFirestore

object UserUtil {
    var user: User? = null

    fun getCurrentUser() {
        if (FirebaseUtils.firebaseAuth.currentUser != null) {
            FirebaseFirestore.getInstance().collection("Users")
                .document(FirebaseUtils.firebaseAuth.currentUser?.uid!!)
                .get().addOnCompleteListener {
                    user = it.result?.toObject(User::class.java)
                }
        }
    }
}