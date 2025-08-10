package au.edu.jcu.myapplication.databasepleaswork

import androidx.room.Entity
import androidx.room.PrimaryKey




@Entity(tableName = "favourite_recipes_table")
class RecipeFav(var recipeId: String) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0



}