package au.edu.jcu.myapplication.databasepleaswork

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecipeDao {
    @Insert
    fun insert(recipe: RecipeFav): Long

    @Query("DELETE FROM favourite_recipes_table WHERE recipeId = :id")
    fun delete(id: String)

    @Query("SELECT * FROM favourite_recipes_table")
    fun getAll(): List<RecipeFav>

    @Query("SELECT * FROM favourite_recipes_table WHERE recipeId = :favouriteName")
    fun getFavourite(favouriteName: String): RecipeFav?

    @Query("SELECT * FROM favourite_recipes_table")
    fun getAllFavourites(): List<RecipeFav?>
}
