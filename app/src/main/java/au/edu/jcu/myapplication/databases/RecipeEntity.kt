package au.edu.jcu.myapplication.databases

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ingredient: String,
    val steps: String,
    val category: String,
    val image: String,
    val authorId: String
)
