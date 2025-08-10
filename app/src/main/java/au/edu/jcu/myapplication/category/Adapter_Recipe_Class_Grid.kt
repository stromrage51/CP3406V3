package au.edu.jcu.myapplication.category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.edu.jcu.myapplication.R
import au.edu.jcu.myapplication.databases.Recipe
import com.bumptech.glide.Glide

class Adapter_Recipe_Class_Grid : RecyclerView.Adapter<Adapter_Recipe_Class_Grid.VH>() {
    private val items = mutableListOf<Recipe>()
    private var onItemClick: ((Recipe) -> Unit)? = null



    fun setOnItemClickListener(block: (Recipe) -> Unit) { onItemClick = block }

    fun submit(list: List<Recipe>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(
            R.layout.item_recipe_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.categoryimage)
        private val title = view.findViewById<TextView>(R.id.categorytitle)

        fun bind(item: Recipe) {
            title.text = item.name

            Glide.with(image).load(item.image)
                //pushholder image
                .placeholder(R.drawable.pizza)
                .centerCrop()
                .into(image)

            itemView.setOnClickListener { onItemClick?.invoke(item) }
        }
    }

}

