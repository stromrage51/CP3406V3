package au.edu.jcu.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import au.edu.jcu.myapplication.databinding.ActivityLoginScreenBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class LoginScreen : AppCompatActivity() {

    private lateinit var binding: ActivityLoginScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //login in
        binding.loginButton.setOnClickListener { login() }
        //sign up activity start up
        binding.signupButton.setOnClickListener { startActivity(Intent(this,
            SignUpScreen::class.java)) }
    }

    private fun login(){
        val username = binding.username.text?.toString()?.trim() ?:""
        val password = binding.password.text?.toString()?.trim() ?:""

        if (username.isNullOrEmpty() or password.isNullOrEmpty()) {

            Toast.makeText(this, "Enter username and password",
                    Toast.LENGTH_SHORT).show()
        }

        else if(!Patterns.EMAIL_ADDRESS.matcher(username).matches()){
            Toast.makeText(this, "Enter a valid email",
                Toast.LENGTH_SHORT).show()
        }

        else {

            FirebaseApp.initializeApp(this)
            val auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){

                        Toast.makeText(this, "Login Success: ",
                            Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    else {
                        Toast.makeText(this, "Login failed: ",
                            Toast.LENGTH_SHORT).show()
                    }


                }


        }

    }

}