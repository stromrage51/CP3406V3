package au.edu.jcu.myapplication

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import java.util.regex.Pattern.matches

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("au.edu.jcu.myapplication", appContext.packageName)
    }

    @RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
    @androidx.test.filters.LargeTest
    class HomeUiTest {

        @get:org.junit.Rule
        val rule = androidx.test.ext.junit.rules.ActivityScenarioRule(MainActivity::class.java)

        /** Small helper action to wait for UI/data (500ms default) */
        private fun waitFor(millis: Long) = object : androidx.test.espresso.ViewAction {
            override fun getConstraints() =
                org.hamcrest.CoreMatchers.any(android.view.View::class.java)

            override fun getDescription() = "Wait for $millis ms"
            override fun perform(
                uiController: androidx.test.espresso.UiController,
                view: android.view.View?
            ) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }

        @org.junit.Test
        fun searchFiltersCategories() {
            // Type into the search field
            androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withId(R.id.searchEditText)
            ).perform(
                androidx.test.espresso.action.ViewActions.typeText("dinner"),
                androidx.test.espresso.action.ViewActions.pressImeActionButton(),
                androidx.test.espresso.action.ViewActions.closeSoftKeyboard()
            )

            // optional give the UI a brief moment if results come from database
            androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.isRoot()
            ).perform(waitFor(500))

            //verify to see after filtering
            androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withText(
                    org.hamcrest.CoreMatchers.containsString(
                        "dinner"
                    )
                )
            ).check(
                androidx.test.espresso.assertion.ViewAssertions.matches(
                    androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
                )
            )
        }

        @org.junit.Test
        fun favoritesEmptyState() {

            androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withId(R.id.noFavouritesTextView)
            ).check(
                androidx.test.espresso.assertion.ViewAssertions.matches(
                    androidx.test.espresso.matcher.ViewMatchers.isDisplayed()
                )
            )

            androidx.test.espresso.Espresso.onView(
                androidx.test.espresso.matcher.ViewMatchers.withId(R.id.favouritesRecyclerView)
            ).check(
                androidx.test.espresso.assertion.ViewAssertions.matches(
                    androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility(
                        androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
                    )
                )
            )
        }
    }
}