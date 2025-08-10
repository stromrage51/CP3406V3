package au.edu.jcu.myapplication.databasepleaswork

import android.app.Application
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import au.edu.jcu.myapplication.databases.Recipe
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [RecipeFav::class], version = 1, exportSchema = false)
abstract class Database_Recipe : RoomDatabase() {
    abstract fun favouriteDao(): RecipeDao

    companion object {
        @Volatile
        private var instance: Database_Recipe? = null
        private const val NUMBER_OF_THREADS = 4
        val databaseWriteExecutor: ExecutorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS)

        fun getInstance(context: Context): Database_Recipe =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    Database_Recipe::class.java, "recipe_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}