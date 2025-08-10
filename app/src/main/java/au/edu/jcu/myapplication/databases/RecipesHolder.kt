package au.edu.jcu.myapplication.databases

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import au.edu.jcu.myapplication.R
import au.edu.jcu.myapplication.databinding.ActivityRecipesHolderBinding
import au.edu.jcu.myapplication.offlinemode.AppDatabase
import au.edu.jcu.myapplication.ui.applyAppSettings
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipesHolder : AppCompatActivity() {

    private lateinit var binding: ActivityRecipesHolderBinding
    private var recipeId: String? = null
    private var currentRecipe: Recipe? = null


    //room and firebase variables
    private val appDb by lazy { AppDatabase.getInstance(this) }
    private val recipeDaoRoom by lazy { appDb.recipeDaoRoom() }
    private val recipesRef by lazy {
        FirebaseDatabase.getInstance().getReference("Recipes") }




    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_holder, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            R.id.holder_export -> { exportRecipe(); true }     // no args
            R.id.holder_delete -> { confirmDelete(); true }    // no args
            R.id.holder_edit   -> { editRecipe(); true }       // optional
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun editRecipe() {
        val r = currentRecipe ?: return
        val intent = Intent(this, NewRecipe::class.java).apply {
            putExtra("recipe", r)
            putExtra("isEdit", true)
            putExtra("recipeId", r.id)
        }
        startActivity(intent)
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecipesHolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyAppSettings(binding.root)

        // Toolbar once
        setSupportActionBar(binding.toolbarz)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarz.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        //get recipe id from intent
        recipeId = (intent.getSerializableExtra("recipe") as? Recipe)?.id
        if (recipeId == null) {
            Toast.makeText(this, "Recipe details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //load the recipe
        loadRecipe(requireNotNull(recipeId))
    }





    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val canEdit = currentRecipe?.authorId == FirebaseAuth.getInstance().uid
        menu.findItem(R.id.holder_edit)?.isVisible = canEdit

        menu.findItem(R.id.holder_delete)?.isVisible = canEdit
        return super.onPrepareOptionsMenu(menu)
    }



    private fun loadRecipe(id: String) {
        if (!isConnectedToInternet(this)) {
            loadRecipeOffline(id); return
        }

        //read just one recipe
        recipesRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val recipe = snapshot.getValue(Recipe::class.java)
                if (recipe != null) {
                    //id is set even if missing in database
                    if (recipe.id.isNullOrBlank()) recipe.id = id
                    currentRecipe = recipe
                    bindRecipe(recipe)

                    //offline
                    lifecycleScope.launch(Dispatchers.IO) {
                        recipeDaoRoom.insert(
                            RecipeEntity(
                                id = recipe.id ?: id,
                                name = recipe.name,
                                ingredient = recipe.ingredient,
                                steps = recipe.steps,
                                category = recipe.category,
                                image = recipe.image,
                                authorId = recipe.authorId
                            )
                        )
                    }
                    invalidateOptionsMenu()
                } else {
                    //not found online instead try offline
                    loadRecipeOffline(id)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                loadRecipeOffline(id)
            }
        })
    }



    private fun loadRecipeOffline(id: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val entity = recipeDaoRoom.getById(id)
            withContext(Dispatchers.Main) {
                if (entity != null) {
                    val recipe = Recipe(
                        id = entity.id,
                        name = entity.name,
                        ingredient = entity.ingredient,
                        steps = entity.steps,
                        category = entity.category,
                        image = entity.image,
                        authorId = entity.authorId
                    )
                    currentRecipe = recipe
                    bindRecipe(recipe)
                    invalidateOptionsMenu()
                } else {
                    Toast.makeText(this@RecipesHolder, "Recipe not available offline.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }




    private fun bindRecipe(it: Recipe) {
        binding.holderRecipeName.text = it.name
        binding.categoryHolder.text = it.category
        binding.stepsHolder.text = it.steps
        binding.indgetintsHolder.text = it.ingredient

        Glide.with(this)
            .load(it.image)
            .centerCrop()
            .placeholder(R.drawable.pizza)
            .error(R.drawable.pizza)
            .into(binding.imgRecipe)


    }







    override fun onResume() {
        super.onResume()
        recipeId?.let { loadRecipe(it) }
        applyAppSettings(binding.root)
    }


    private fun isConnectedToInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }


    private fun deleteRecipe(recipe: Recipe) {
        //delete from Room
        lifecycleScope.launch(Dispatchers.IO) {
            appDb.recipeDaoRoom().deleteById(recipe.id ?: "")

            //if online delete from Firebase
            if (isConnectedToInternet(this@RecipesHolder) && !recipe.id.isNullOrBlank()) {
                FirebaseDatabase.getInstance()
                    .getReference("Recipes")
                    .child(recipe.id!!)
                    .removeValue()
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@RecipesHolder, "Recipe deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun exportRecipe() {
        val recipe = currentRecipe ?: return
        val shareText = buildString {
            appendLine("Recipe: ${recipe.name}")
            appendLine("Category: ${recipe.category}")
            appendLine()
            appendLine("Ingredients:")
            appendLine(recipe.ingredient)
            appendLine()
            appendLine("Steps:")
            appendLine(recipe.steps)
            if (!recipe.image.isNullOrBlank()) {
                appendLine()
                appendLine("Image: ${recipe.image}")
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Recipe: ${recipe.name}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(sendIntent, "Share recipe"))
    }




    private fun confirmDelete() {
        val recipe = currentRecipe ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete recipe?")
            .setMessage("This will remove \"${recipe.name}\". This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteRecipe(recipe) }
            .setNegativeButton("Cancel", null)
            .show()
    }



}
