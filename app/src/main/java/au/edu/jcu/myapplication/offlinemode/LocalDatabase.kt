package au.edu.jcu.myapplication.offlinemode

import android.content.Context
import androidx.room.Room

object LocalDatabase {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "recipe-database"
            ).build().also { INSTANCE = it }
        }
    }
}