package au.edu.jcu.myapplication.databases

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import au.edu.jcu.myapplication.R
import au.edu.jcu.myapplication.databinding.ActivityHorzRecipeAdapterBinding
import au.edu.jcu.myapplication.databinding.LayoutRecipeBinding
import com.bumptech.glide.Glide




class Adapter_Recipe_Class : RecyclerView.Adapter<Adapter_Recipe_Class.RecipeHolder>() {

    private var recipeList: List<Recipe> = ArrayList()
    private var onItemClickListener: ((Recipe) -> Unit)? = null


    fun setRecipe(recipeList: List<Recipe>) {
        this.recipeList = recipeList
        Log.d("Adapter_Recipe_Class", "Number of recipes set: ${recipeList.size}")
        notifyDataSetChanged()
    }


    // Method to set the click listener
    fun setOnItemClickListener(listener: (Recipe) -> Unit) {
        onItemClickListener = listener
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeHolder {
        val binding = LayoutRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        private val layoutBinding: LayoutRecipeBinding,
        private val onItemClickListener: ((Recipe) -> Unit)?
    ) : RecyclerView.ViewHolder(layoutBinding.root) {

        fun onBind(recipe: Recipe) {
            Glide.with(layoutBinding.root.context)
                .load(recipe.image)
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher)
                .into(layoutBinding.itemRecipeImage)

            layoutBinding.RecipeName.text = recipe.name

            layoutBinding.root.setOnClickListener {
                onItemClickListener?.invoke(recipe) // Invoke the click listener
            }
        }
    }


}


