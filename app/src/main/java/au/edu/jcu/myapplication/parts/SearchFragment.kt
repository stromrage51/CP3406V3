package au.edu.jcu.myapplication.parts

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import au.edu.jcu.myapplication.category.Adapter_Recipe_Class_Grid
import au.edu.jcu.myapplication.databases.Recipe
import au.edu.jcu.myapplication.databases.RecipeEntity
import au.edu.jcu.myapplication.databases.RecipesHolder
import au.edu.jcu.myapplication.databinding.FragmentSearchBinding
import au.edu.jcu.myapplication.offlinemode.AppDatabase
import au.edu.jcu.myapplication.ui.applyAppSettings
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    //adapters rid
    private lateinit var adapter: Adapter_Recipe_Class_Grid

    //local datbase and online database
    private val appDb by lazy { AppDatabase.getInstance(requireContext()) }
    private val recipeDao by lazy { appDb.recipeDaoRoom() }
    private val recipesRef by lazy { FirebaseDatabase.getInstance().getReference("Recipes") }


    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("SearchFragment", "onViewCreated: Initializing UI components")
        //settings
        applyAppSettings(binding.root)

        setupRecycler()
        setupSearch()

    }
//////////////////////////

    //setingup recyleview for results
    private fun setupRecycler() {
        Log.d("SearchFragment", "Setting up RecyclerView")
        adapter = Adapter_Recipe_Class_Grid().apply {
            setOnItemClickListener { recipe ->
                Log.d("SearchFragment", "Recipe clicked: ${recipe.name}")
                startActivity(
                    Intent(requireContext(), RecipesHolder::class.java)
                        .putExtra("recipe", recipe)
                )
            }
        }
        binding.searchrecycler.layoutManager = GridLayoutManager(requireContext(), spanCount())
        binding.searchrecycler.adapter = adapter
        binding.searchrecycler.setHasFixedSize(true)
    }


//setting up search bar
    private fun setupSearch() {
    Log.d("SearchFragment", "Setting up search bar")
    binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(v.text?.toString().orEmpty())
                true
            } else false
        }

        binding.searchEditText.addTextChangedListener { text ->
            val query = text?.toString().orEmpty()
            searchJob?.cancel()
            Log.d("SearchFragment", "Search text changed: $query")
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(250)
                runSearch(query)
            }
        }
    }

    /////////////////////////////////

    //running search
    private fun runSearch(raw: String) {
        val query = raw.trim()
        Log.d("SearchFragment", "Running search for: $query")
        if (query.isEmpty()) {
            adapter.submit(emptyList())
            return
        }

        if (isConnectedToInternet(requireContext())) {
            recipesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("SearchFragment", "Fetched ${snapshot.childrenCount} recipes from Firebase")
                    val lower = query.lowercase()
                    val all = snapshot.children.mapNotNull { it.getValue(Recipe::class.java) }
                    val filtered = all.filter { recipe ->
                        recipe.name.contains(lower, true) ||
                                recipe.category.contains(lower, true) ||
                                recipe.ingredient.contains(lower, true)
                    }
                    Log.d("SearchFragment", "Found ${filtered.size} matching recipes")
                    adapter.submit(filtered)

                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        recipeDao.insertAll(filtered.map {
                            RecipeEntity(
                                id = it.id.orEmpty(),
                                name = it.name,
                                ingredient = it.ingredient,
                                steps = it.steps,
                                category = it.category,
                                image = it.image,
                                authorId = it.authorId
                            )
                        })
                        Log.d("SearchFragment", "Saved ${filtered.size} recipes to room db")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SearchFragment", "Firebase error: ${error.message}")
                }
            })
        } else {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val entities = recipeDao.searchAll("%$query%")
                val recipes = entities.map {
                    Recipe(
                        id = it.id, name = it.name, ingredient = it.ingredient,
                        steps = it.steps, category = it.category, image = it.image, authorId = it.authorId
                    )
                }
                Log.d("SearchFragment", "Found ${recipes.size} recipes offline")
                withContext(Dispatchers.Main) { adapter.submit(recipes) }
            }
        }
    }

    /////////////////////////////////////////////

    //grid pattern
    private fun spanCount(): Int {
        val dpWidth = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        return when {
            dpWidth >= 900 -> 4
            dpWidth >= 600 -> 3
            else -> 2
        }
    }

    override fun onResume() {
        super.onResume()
        applyAppSettings(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }


    //check if connected to interent
    private fun isConnectedToInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }



}
