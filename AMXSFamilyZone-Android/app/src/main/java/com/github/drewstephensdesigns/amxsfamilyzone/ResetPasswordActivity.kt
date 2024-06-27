package com.github.drewstephensdesigns.amxsfamilyzone

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.ActivityResetPasswordBinding
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.snackbar
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.toast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding : ActivityResetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.resetPasswordBtn.setOnClickListener {
            val emailAddress = binding.signupEmail.text.toString().trim()
            FirebaseUtils.firebaseAuth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener {task ->
                    if (task.isSuccessful) {
                        toast(resources.getString(R.string.password_reset_message))
                        startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java))
                        finishAffinity()
                    }
                }
        }
    }
}