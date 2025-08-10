package au.edu.jcu.myapplication.databasepleaswork

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.jcu.myapplication.R
import au.edu.jcu.myapplication.databases.Recipe
import au.edu.jcu.myapplication.databases.RecipeDetails
import au.edu.jcu.myapplication.databinding.ActivityHorzRecipeAdapterBinding
import com.bumptech.glide.Glide


class HorzRecipeAdapter : RecyclerView.Adapter<HorzRecipeAdapter.RecipeHolder>() {

    private var recipeList: List<Recipe> = ArrayList()
    private var onItemClickListener: ((Recipe) -> Unit)? = null


    private lateinit var horzAdapter: HorzRecipeAdapter



    fun setRecipeList(recipeList: List<Recipe>) {
        this.recipeList = recipeList
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Recipe) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeHolder {
        val binding = ActivityHorzRecipeAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: RecipeHolder, position: Int) {
        val recipe = recipeList[position]
        holder.onBind(recipe)
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    class RecipeHolder(
        private val binding: ActivityHorzRecipeAdapterBinding,
        private val onItemClickListener: ((Recipe) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun onBind(recipe: Recipe) {
            Glide.with(binding.root.context)
                .load(recipe.image)
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(binding.horzRecipeImage)

            binding.hozRecipeName.text = recipe.name

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(recipe)
            }
        }
    }
}

