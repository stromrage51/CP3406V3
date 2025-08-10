package au.edu.jcu.myapplication.databases


import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import au.edu.jcu.myapplication.R
import au.edu.jcu.myapplication.category.Category
import au.edu.jcu.myapplication.databinding.ActivityNewRecipeBinding
import au.edu.jcu.myapplication.offlinemode.RecipeRepository
import au.edu.jcu.myapplication.ui.applyAppSettings
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class NewRecipe : AppCompatActivity() {

    private lateinit var binding: ActivityNewRecipeBinding
    private var imageUri: Uri? = null
    private var recipeId: String? = null
    private var isEdit = false
    private var recipeImageSet = false
    private var selectedImageUri: Uri? = null

    private var originalImageUrl: String? = null
    private var hasNewImage = false

    private val categoryNames = mutableListOf<String>()
    private lateinit var categoryAdapter: ArrayAdapter<String>

    private var isSaving = false


    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            hasNewImage = true
            recipeImageSet = true
            Glide.with(this).load(it).centerCrop().into(binding.addRecipeImage)
        }
    }

    private fun pickFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //settings
        applyAppSettings(binding.root)

        setSupportActionBar(binding.toolbarz) // make sure your layout has a Toolbar with this id
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarz.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.addRecipeImage.setOnClickListener {
            pickImageFromGalleryOrInternet()
        }

        binding.addRecipeButton.setOnClickListener {
            if (isSaving) return@setOnClickListener
            getData()
        }

        //category loadup
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.addRecipeCategory.setAdapter(categoryAdapter)
        binding.addRecipeCategory.threshold = 0
        binding.addRecipeCategory.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) binding.addRecipeCategory.showDropDown()
        }

        loadCategoriesForAutocomplete()

        //if editing
        val recipe = intent.getSerializableExtra("recipe") as? Recipe
        if (recipe != null) {
            isEdit = true
            recipeId = recipe.id
            originalImageUrl = recipe.image  // keep original
            binding.addRecipeName.setText(recipe.name)
            binding.addRecipeidgentis.setText(recipe.ingredient)
            binding.addRecipeSteps.setText(recipe.steps)
            binding.addRecipeCategory.setText(recipe.category)

            Glide.with(this)
                .load(recipe.image)
                .centerCrop()
                .placeholder(R.drawable.pizza)
                .into(binding.addRecipeImage)

            recipeImageSet = true
            hasNewImage = false
            binding.addRecipeButton.text = "Update Recipe"
        }
    }

    override fun onResume() {
        super.onResume()
        applyAppSettings(binding.root)
    }

    private fun showUrlInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Image URL")

        val input = EditText(this)
        input.hint = "https://example.com/image.jpg"
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val url = input.text.toString()
            val imageRegex = Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg|tiff?)$", RegexOption.IGNORE_CASE)

            if (url.isNotBlank() && imageRegex.matches(url)) {
                Glide.with(this)
                    .load(url)
                    .centerCrop()
                    .into(binding.addRecipeImage)

                selectedImageUri = Uri.parse(url)
                recipeImageSet = true
            } else {
                Toast.makeText(this, "Invalid image URL", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }


    private fun enableSaveButton() {
        isSaving = false
        binding.addRecipeButton.isEnabled = true
        binding.addRecipeButton.alpha = 1f
    }


    private fun getData() {
        if (isSaving) return
        isSaving = true
        binding.addRecipeButton.isEnabled = false
        binding.addRecipeButton.alpha = 0.6f


        val recipeName = binding.addRecipeName.text.toString().trim()
        val recipeIngredients = binding.addRecipeidgentis.text.toString().trim()
        val recipeSteps = binding.addRecipeSteps.text.toString().trim()
        val recipeCategory = binding.addRecipeCategory.text.toString().trim()


        val matched = categoryNames.firstOrNull { it.equals(recipeCategory, ignoreCase = true) }
        val nameLower = recipeName.lowercase()
        val authorId = FirebaseAuth.getInstance().uid.orEmpty()


        val normalizedCategory = matched ?: recipeCategory



        Log.d("NewRecipe", "getData() called")
        Log.d("NewRecipe", "Fields: name=$recipeName, " +
                "ingredients=$recipeIngredients, steps=$recipeSteps, category=$recipeCategory")

        if (recipeName.isEmpty() || recipeIngredients.isEmpty() || recipeSteps.isEmpty() ||
            recipeCategory.isEmpty()) {
            Toast.makeText(this,
                "Please fill in all fields", Toast.LENGTH_SHORT).show()
            Log.e("NewRecipe", "Missing fields!")
            enableSaveButton()
            return
        }

        //////////////////////////////////////////////



        if (!recipeImageSet && !isEdit) {
            Toast.makeText(this, "Please select or capture an image",
                Toast.LENGTH_SHORT).show()
            enableSaveButton()
            return
        }



        val recipe = Recipe(
            name = recipeName,
            ingredient = recipeIngredients,
            steps = recipeSteps,
            category = normalizedCategory,
            image = "",
            authorId = authorId
        )
        recipe.authorId_name_lc = "${authorId}_${nameLower}"


        if (isEdit) {
            recipe.id = recipeId
        }

        if (!isOnline()) {
            Snackbar.make(binding.root, "No internet. Saved offline.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry") {
                    getData() // Try again when user clicks Retry
                }
                .show()

            saveOffline(recipe)
            enableSaveButton()
            return
        }


        Log.d("NewRecipe", "All fields and image OK. Checking category")

        checkDuplicateAndProceed(recipe) {
            checkAndSaveCategory(normalizedCategory) {
                if (hasNewImage && selectedImageUri != null) {
                    uploadImage(recipe)
                } else {
                    //keep original image if editing without changing image
                    if (isEdit && !hasNewImage && !originalImageUrl.isNullOrBlank()) {
                        recipe.image = originalImageUrl!!
                    }
                    saveRecipeData(recipe)
                }
            }
        }

    }

    private fun checkDuplicateAndProceed(recipe: Recipe, proceed: () -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("Recipes")
        ref.orderByChild("authorId_name_lc")
            .equalTo(recipe.authorId_name_lc)
            .get()

            .addOnSuccessListener { snap ->
                if (snap.exists() && !isEdit) {
                    Toast.makeText(this, "You already have a recipe named '${recipe.name}'.",
                        Toast.LENGTH_LONG).show()
                    enableSaveButton()
                } else {
                    proceed()
                }
            }
            .addOnFailureListener {
                proceed()
            }
    }




    private fun loadCategoriesForAutocomplete() {
        FirebaseDatabase.getInstance().getReference("Categories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoryNames.clear()
                    snapshot.children.mapNotNull {
                        it.getValue(Category::class.java)?.name
                    }.mapTo(categoryNames) { it.trim() }
                    //remove duplicates and sort
                    val uniqueSorted = categoryNames
                        .filter { it.isNotEmpty() }
                        .distinctBy { it.lowercase() }
                        .sorted()
                    categoryNames.clear()
                    categoryNames.addAll(uniqueSorted)
                    categoryAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }



    private fun uploadImage(recipe: Recipe) {
        val uri = selectedImageUri ?: run {
            //no new image chosen save with original
            saveRecipeData(recipe)
            return
        }

        if (uri.toString().startsWith("http")) {
            recipe.image = uri.toString()
            saveRecipeData(recipe)
            return
        }


        val drawable = binding.addRecipeImage.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap

        if (bitmap == null) {
            Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show()
            enableSaveButton()
            return
        }

        val id = recipe.id ?: System.currentTimeMillis().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("images/$id.jpg")
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = storageRef.putBytes(data)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) throw task.exception!!
            storageRef.downloadUrl

        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                recipe.image = task.result.toString()
                saveRecipeData(recipe)
            } else {
                task.exception?.printStackTrace()
                Toast.makeText(this, "Image upload failed",
                    Toast.LENGTH_SHORT).show()
                enableSaveButton()
            }
        }.addOnFailureListener { e ->
            e.printStackTrace()
            Toast.makeText(this, "Upload failed: ${e.message}",
                Toast.LENGTH_SHORT).show()
            enableSaveButton()
        }
    }

    private fun pickImageFromGalleryOrInternet() {
        val options = arrayOf("Choose from Gallery", "Enter Image URL")

        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickFromGallery()
                    1 -> showUrlInputDialog()
                }
            }
            .show()
    }


    private fun saveRecipeData(recipe: Recipe) {
        val dbRef = FirebaseDatabase.getInstance().getReference("Recipes")
        val id = recipe.id ?: dbRef.push().key ?: return

        recipe.id = id
        dbRef.child(id).setValue(recipe).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveOffline(recipe) // save to Room
                Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show()
                Log.d("NewRecipe", "Recipe successfully saved: $recipe")
                finish()
            } else {
                Toast.makeText(this, "Failed to save",
                    Toast.LENGTH_SHORT).show()
                enableSaveButton()
            }
        }
        Log.d("NewRecipe", "Saving recipe to Firebase: ${recipe.name} with ID ${recipe.id}")

    }


    private fun saveOffline(recipe: Recipe) {
        val repository = RecipeRepository(applicationContext)

        val recipeEntity = RecipeEntity(
            id = recipe.id ?: "",
            name = recipe.name,
            ingredient = recipe.ingredient,
            steps = recipe.steps,
            category = recipe.category,
            image = recipe.image,
            authorId = recipe.authorId
        )

        CoroutineScope(Dispatchers.IO).launch {
            repository.insertRecipe(recipeEntity)
        }
        Log.d("NewRecipe", "Saving recipe offline: ${recipe.name} with ID ${recipe.id}")

    }

    //proof check category
    private fun checkAndSaveCategory(categoryName: String, onComplete: () -> Unit) {
        val categoriesRef = FirebaseDatabase.getInstance().getReference("Categories")
        val lowkey = categoryName.trim().lowercase()
        categoriesRef.child(lowkey).get()
            .addOnSuccessListener { snapshot: DataSnapshot ->
                if (!snapshot.exists()) {
                    val category = Category(name = categoryName.trim())
                    categoriesRef.child(lowkey).setValue(category)
                        .addOnSuccessListener { onComplete() }
                        .addOnFailureListener { onComplete() }
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { onComplete() }
    }



    fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


}





