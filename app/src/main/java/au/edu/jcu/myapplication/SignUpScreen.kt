package au.edu.jcu.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.edu.jcu.myapplication.databinding.ActivitySignUpScreenBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

class SignUpScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.signupButton.setOnClickListener {
            signup()
        }
        binding.loginInButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginScreen::class.java
                )
            )
        }
    }

    private fun signup() {

        val username = binding.username.text?.toString()?.trim() ?: ""
        val password = binding.password.text?.toString()?.trim() ?: ""
        val nameid = binding.nameid.text?.toString()?.trim() ?: ""

        //making sure cursor moves
        binding.username.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.password.requestFocus()
                true
            } else false
        }

        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.nameid.requestFocus()
                true
            } else false
        }

        binding.nameid.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Optional: hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.nameid.windowToken, 0)
                true
            } else false
        }

        if (username.isEmpty() || password.isEmpty() || nameid.isEmpty()) {

            Toast.makeText(
                this, "Enter username and password",
                Toast.LENGTH_SHORT
            ).show()
        } else if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Toast.makeText(
                this, "Enter a valid email",
                Toast.LENGTH_SHORT
            ).show()
        } else {
//            startActivity(Intent(this, MainActivity::class.java))
            createUser(username, password, nameid)


        }


    }

    private fun createUser(username: String, password: String, nameid: String) {

        FirebaseApp.initializeApp(this)
        val auth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    //update display name
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(nameid)
                        .build()


                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                val database = FirebaseDatabase.getInstance()
                                val usersRef = database.getReference("users")
                                val userId = user.uid
                                val userMap = mapOf(
                                    "name" to nameid,
                                    "email" to username
                                )

                                usersRef.child(userId).setValue(userMap)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Account created", Toast.LENGTH_SHORT)
                                            .show()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            "Failed to save user data",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }

                } else {
                    val errorMessage = task.exception?.localizedMessage ?: "Unknown error"
                    Toast.makeText(this, "Account creation failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("SignUpScreen", "Firebase error: ", task.exception)
                }
            }
    }
}