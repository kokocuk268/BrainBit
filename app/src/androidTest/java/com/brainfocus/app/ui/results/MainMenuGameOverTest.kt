package com.brainfocus.app.ui.results

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.brainfocus.app.R
import com.brainfocus.app.ui.MainActivity
import com.brainfocus.app.ui.connection.ConnectionFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainMenuGameOverTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun main_menu_button_should_navigate_to_connection_and_clear_back_stack() {
        activityRule.scenario.onActivity { activity ->
            activity.navigateToGame()
        }

        activityRule.scenario.onActivity { activity ->
            activity.navigateToResults(0, 0f)
        }

        onView(withId(R.id.mainMenuButton)).perform(click())

        activityRule.scenario.onActivity { activity ->
            val currentFragment = activity.supportFragmentManager
                .findFragmentById(R.id.fragmentContainer)
            assert(currentFragment is ConnectionFragment) {
                "Expected ConnectionFragment but was ${currentFragment?.javaClass?.simpleName}"
            }
        }
    }
}
