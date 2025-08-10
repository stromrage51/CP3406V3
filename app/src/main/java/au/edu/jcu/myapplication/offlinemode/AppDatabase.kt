package au.edu.jcu.myapplication.offlinemode


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import au.edu.jcu.myapplication.categorys.CategoryDao
import au.edu.jcu.myapplication.categorys.CategoryEntity
import au.edu.jcu.myapplication.databasepleaswork.RecipeDao
import au.edu.jcu.myapplication.databasepleaswork.RecipeDaoRoom
import au.edu.jcu.myapplication.databasepleaswork.RecipeFav
import au.edu.jcu.myapplication.databases.RecipeEntity
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        //ceate the new categories table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                name TEXT NOT NULL PRIMARY KEY
            )
            """.trimIndent()
        )
    }
}



@Database(entities = [RecipeEntity::class,  CategoryEntity::class, RecipeFav::class],
    version = 3, exportSchema = false)


abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDaoRoom(): RecipeDaoRoom
    abstract fun categoryDao(): CategoryDao
    abstract fun favouriteRecipeDao(): RecipeDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        //single-threaded executor for database writes
        val databaseWriteExecutor: ExecutorService = Executors.newSingleThreadExecutor()

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe-database"
                )
                    //use the same executor for both query & transactions
                    .setQueryExecutor(databaseWriteExecutor)
                    .setTransactionExecutor(databaseWriteExecutor)

                    //applying migrations
                    .addMigrations(MIGRATION_2_3)

                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}
