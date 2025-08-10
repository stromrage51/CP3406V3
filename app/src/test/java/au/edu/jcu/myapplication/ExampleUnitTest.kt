package au.edu.jcu.myapplication

import android.content.Context
import androidx.room.Room
import au.edu.jcu.myapplication.databases.RecipeEntity
import au.edu.jcu.myapplication.offlinemode.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertRecipe_savesToDatabase() = runBlocking {
        val recipe = RecipeEntity(
            id = "test1",
            name = "Test Pizza",
            ingredient = "Flour, Water, Yeast",
            steps = "Mix, Bake",
            category = "Pizza",
            image = "https://example.com/pizza.jpg",
            authorId = "user1"
        )

        //save to database
        db.recipeDaoRoom().insert(recipe)

        //checking if stored
        val saved = db.recipeDaoRoom().getById("test1")
        Assert.assertNotNull(saved)
        Assert.assertEquals("Test Pizza", saved?.name)
        Assert.assertEquals("Pizza", saved?.category)
    }


}