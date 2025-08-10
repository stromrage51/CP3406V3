package au.edu.jcu.myapplication.databases

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import au.edu.jcu.myapplication.R
import au.edu.jcu.myapplication.category.Adapter_Recipe_Class_Grid
import au.edu.jcu.myapplication.category.GridSpacingDecoration
import au.edu.jcu.myapplication.databinding.ActivityRecipeDetailsBinding
import au.edu.jcu.myapplication.offlinemode.AppDatabase
import au.edu.jcu.myapplication.offlinemode.toDomain
import au.edu.jcu.myapplication.offlinemode.toEntity
import au.edu.jcu.myapplication.ui.applyAppSettings
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RecipeDetails : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding
    private var type: String? = null
    private var lastLoadedInMemory: List<Recipe> = emptyList()
    val Int.dp get() = (this * Resources.getSystem().displayMetrics.density).toInt()



    private val appDb by lazy { AppDatabase.getInstance(this) }
    private val recipeDaoRoom by lazy { appDb.recipeDaoRoom() }
    //initialize firebase database
    private val recipesRef by lazy { FirebaseDatabase.getInstance().getReference("Recipes") }


    //adapter class
    private lateinit var gridAdapter: Adapter_Recipe_Class_Grid



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //settings
        applyAppSettings(binding.root)


        //toolbar
        setSupportActionBar(binding.toolbarz)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // Set up RecyclerView with grid apdatar
        binding.recipesView.layoutManager = GridLayoutManager(this, spanCount())
        binding.recipesView.addItemDecoration(GridSpacingDecoration(8))
        binding.recipesView.setHasFixedSize(true)


        gridAdapter = Adapter_Recipe_Class_Grid().apply {
            setOnItemClickListener { openRecipeDetail(it) }
        }
        binding.recipesView.adapter = gridAdapter



        //get category name
        type = intent.getStringExtra("category")

        //intial
        filterByCategory()
    }

    //backwards arrow
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { onBackPressedDispatcher.onBackPressed(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        applyAppSettings(binding.root)
    }

    /////////////////////////////////////////////////////////////////

    //search engine
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_details, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        searchView.queryHint = "Search in category"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchInCategory(query.orEmpty())
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                searchInCategory(newText.orEmpty())
                return true
            }
        })
        return true
    }

    private fun updateRecyclerView(recipes: List<Recipe>) {
//        gridAdapter.submit(recipes)
//        lastLoadedInMemory = recipes

        lastLoadedInMemory = recipes
        if (recipes.isEmpty()) {
            showEmptyState("Nothing here")
        } else {
            hideEmptyState()
            (binding.recipesView.adapter as? Adapter_Recipe_Class_Grid)?.submit(recipes)
        }

    }

    private fun openRecipeDetail(recipe: Recipe) {
        startActivity(Intent(this, RecipesHolder::class.java).putExtra("recipe", recipe))
    }


    //
    //category search
    private fun searchInCategory(query: String) {
        val category = type ?: return

        if (query.isBlank()) {
            if (lastLoadedInMemory.isNotEmpty()) {
                updateRecyclerView(lastLoadedInMemory)
                return
            }
            filterByCategory()
            return
        }
        if (isConnectedToInternet(this)) {
            recipesRef.orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val q = query.trim()
                        val recipes =
                            snapshot.children.mapNotNull { it.getValue(Recipe::class.java) }
                                .filter { it.name.contains(q, ignoreCase = true) }

                        if (recipes.isEmpty()) showEmptyState("No matches found.")
                        else updateRecyclerView(recipes)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val offline = recipeDaoRoom
                    .getRecipesByCategoryAndQuery(category, "%${query.trim()}%")
                    .map { it.toDomain() }
                withContext(Dispatchers.Main) { updateRecyclerView(offline)
                    if (offline.isEmpty()) showEmptyState("No matches found.")
                    else updateRecyclerView(offline)
                }
            }
        }
    }




    //////////////////////////////////

    //category system

    private fun filterByCategory() {
        val category = intent.getStringExtra("category") ?: return
        if (isConnectedToInternet(this)) {
            //online mode
            recipesRef.orderByChild("category").equalTo(category)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val recipes = snapshot.children.mapNotNull {
                            it.getValue(Recipe::class.java)
                        }
                        if (recipes.isEmpty()) {
                            showEmptyState("No recipes in this category")
                        } else {
                            updateRecyclerView(recipes)
                        }


                        //save to room for offline part
                        lifecycleScope.launch(Dispatchers.IO) {
                            recipeDaoRoom.insertAll(recipes.map { it.toEntity() })
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RecipeDetails", "Firebase error: ${error.message}")
                    }
                })
        } else {
            //offline
            lifecycleScope.launch(Dispatchers.IO) {
                val offlineEntities = recipeDaoRoom.getRecipesByCategory(category)
                val offlineRecipes = offlineEntities.map { it.toDomain() }
                withContext(Dispatchers.Main) {
                    if (offlineRecipes.isEmpty()) {
                        showEmptyState("No recipes cached for offline use.")
                    } else {
                        updateRecyclerView(offlineRecipes)
                        Toast.makeText(
                            this@RecipeDetails,
                            "Offline mode: Showing saved recipes", Toast.LENGTH_SHORT
                        ).show()
//                    updateRecyclerView(offlineRecipes)
//                    Toast.makeText(this@RecipeDetails, "Offline mode: Showing saved recipes", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }
    /////////////////////////

    //empty recipe mode

    private fun showEmptyState(message: String) {
        binding.emptyText.text = message
        binding.emptybox.visibility = View.VISIBLE
        binding.recipesView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.emptybox.visibility = View.GONE
        binding.recipesView.visibility = View.VISIBLE
    }





    //////////////////////////////////////



    private fun isConnectedToInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    //recyleviw card space
    private fun spanCount(): Int {
        val dpWidth = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        return when {
            dpWidth >= 900 -> 4   // for larger tablets
            dpWidth >= 600 -> 3   // normal tablets
            else -> 2             // phones settings
        }
    }


}

