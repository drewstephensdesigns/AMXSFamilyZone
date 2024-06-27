package com.github.drewstephensdesigns.amxsfamilyzone

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.ActivityLoginBinding
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.toast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils

class LoginActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoginBinding
    private lateinit var signInEmail : String
    private lateinit var signInPassword : String
    private lateinit var signInInputsArray: Array<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        signInInputsArray = arrayOf(
            binding.signupEmail,
            binding.signupPassword
        )

        // Login Button
        binding.signinBtn.setOnClickListener {
            signInUser()
        }

        // Reset Password Link
        binding.resetPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }
    }

    private fun notEmpty() : Boolean = signInEmail.isNotEmpty() && signInPassword.isNotEmpty()

    private fun signInUser() {
        signInEmail = binding.signupEmail.text.toString().trim()
        signInPassword = binding.signupPassword.text.toString().trim()

        if(notEmpty()){
            FirebaseUtils.firebaseAuth.signInWithEmailAndPassword(signInEmail, signInPassword)
                .addOnCompleteListener { signIn ->
                    if(signIn.isSuccessful){
                        val currentUserID = FirebaseUtils.firebaseAuth.currentUser?.email
                        toast("Welcome Back $currentUserID")
                        startActivity(Intent(this, MainActivity::class.java))

                        finish()
                    } else {
                        toast("sign in failed + ${signIn.exception}")
                    }
                }
        } else {
            signInInputsArray.forEach { input ->
                if(input.text.toString().trim().isEmpty()){
                    input.error = "${input.hint} is required"
                }
            }
        }
    }
}