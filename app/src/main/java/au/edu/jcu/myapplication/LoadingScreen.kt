package au.edu.jcu.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.edu.jcu.myapplication.databinding.ActivityLoadingScreenBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LoadingScreen : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //show indeterminate progress bar
        binding.progressBar2.isIndeterminate = true

        //firebase must be initiazed before auth
        FirebaseApp.initializeApp(this)

        //launch pattern for delay and screen transition
        lifecycleScope.launch {
            delay(3000)

            val user = FirebaseAuth.getInstance().currentUser
            val intent = if (user != null) {
                Intent(this@LoadingScreen, MainActivity::class.java)
            } else {
                Intent(this@LoadingScreen, LoginScreen::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
}