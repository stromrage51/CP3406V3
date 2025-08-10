package au.edu.jcu.myapplication.offlinemode

import android.content.Context
import androidx.room.Room
import au.edu.jcu.myapplication.databasepleaswork.RecipeFav
import au.edu.jcu.myapplication.databases.RecipeEntity

class RecipeRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "recipe-database"
    ).build()

    private val recipeDaoRoom = db.recipeDaoRoom()  // Handles RecipeEntity (Room)
    private val favouriteDao = db.favouriteRecipeDao() // Handles RecipeFav

    //offline recipes
    suspend fun insertRecipe(recipe: RecipeEntity) {
        recipeDaoRoom.insert(recipe)
    }

    suspend fun getAllRecipes(): List<RecipeEntity> {
        return recipeDaoRoom.getAllRecipes()
    }

    suspend fun deleteRecipe(recipe: RecipeEntity) {
        recipeDaoRoom.delete(recipe)
    }

    //favourite system
    fun insertFavourite(recipe: RecipeFav): Long {
        val future = AppDatabase.databaseWriteExecutor.submit<Long> {
            favouriteDao.insert(recipe)
        }
        return future.get()
    }

    fun deleteFavourite(id: String) {
        AppDatabase.databaseWriteExecutor.submit {
            favouriteDao.delete(id)
        }
    }

    fun getAllFavourites(): List<RecipeFav> {
        val future = AppDatabase.databaseWriteExecutor.submit<List<RecipeFav>> {
            favouriteDao.getAllFavourites().filterNotNull()
        }
        return future.get()
    }

}

