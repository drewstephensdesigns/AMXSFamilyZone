package com.github.drewstephensdesigns.amxsfamilyzone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.ActivityRegisterBinding
import com.github.drewstephensdesigns.amxsfamilyzone.models.User
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Consts
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.negativeToast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.Extensions.successToast
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private lateinit var userFullName : String
    private lateinit var userName : String
    private lateinit var userEmail : String
    private lateinit var userPassword : String
    private lateinit var createAccountInputsArray : Array<EditText>

    companion object {
        const val TAG = "REGISTER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        createAccountInputsArray = arrayOf(
            binding.signupFullname,
            binding.signupUsername,
            binding.signupEmail,
            binding.signupPassword
        )

        binding.signupBtn.setOnClickListener {
            signIn()
        }

        binding.loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun notEmpty() : Boolean =
        binding.signupEmail.text.toString().trim().isNotEmpty() &&
                binding.signupPassword.text.toString().trim().isNotEmpty() &&
                binding.signupFullname.text.toString().trim().isNotEmpty() &&
                binding.signupUsername.text.toString().trim().isNotEmpty()


    private fun signIn() {
        userFullName = binding.signupFullname.text.toString().trim()
        userName = binding.signupUsername.text.toString().trim()
        userEmail = binding.signupEmail.text.toString().trim()
        userPassword = binding.signupPassword.text.toString().trim()

        if(notEmpty()) {
            FirebaseUtils.firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { signUp ->
                    if(signUp.isSuccessful){
                        val currentUser = FirebaseUtils.firebaseAuth.currentUser
                        val userId = currentUser?.uid ?: return@addOnCompleteListener
                        val user = User(
                            id = userId,
                            name = userFullName,
                            userName = userName,
                            email = userEmail,
                            accountCreated =  System.currentTimeMillis() // Capture current timestamp
                        )
                        val firestore = FirebaseFirestore.getInstance().collection(Consts.USER_NODE)
                        firestore.document(userId).set(user)
                            .addOnCompleteListener {
                                if(it.isSuccessful){
                                    successToast(getString(R.string.register_success_message))
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    negativeToast(getString(R.string.login_failed_message, it.exception))
                                    Log.d(TAG, it.exception.toString())
                                }
                            }
                    } else {
                        negativeToast(getString(R.string.login_failed_message, signUp.exception))
                    }
                }
        } else {
            createAccountInputsArray.forEach { input ->
                if(input.text.toString().trim().isEmpty()){
                    input.error = "${input.hint} is required"
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        startActivity(Intent(this, MainActivity::class.java))
    }
}