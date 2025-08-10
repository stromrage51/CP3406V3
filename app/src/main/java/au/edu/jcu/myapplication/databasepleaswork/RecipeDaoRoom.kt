package au.edu.jcu.myapplication.databasepleaswork

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.edu.jcu.myapplication.databases.RecipeEntity

@Dao
interface RecipeDaoRoom {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipes: List<RecipeEntity>)


    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE category = :category")
    suspend fun getRecipesByCategory(category: String): List<RecipeEntity>

    @Query("""
        SELECT * FROM recipes 
        WHERE category = :category AND name LIKE :query COLLATE NOCASE
    """)
    suspend fun getRecipesByCategoryAndQuery(category: String, query: String): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecipeEntity?

    @Delete
    suspend fun delete(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: String)


    @Query("""
SELECT * FROM recipes
WHERE name LIKE :q OR category LIKE :q OR ingredient LIKE :q
ORDER BY name COLLATE NOCASE
""")
    suspend fun searchAll(q: String): List<RecipeEntity>

}