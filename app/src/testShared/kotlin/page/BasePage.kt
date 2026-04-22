package net.pot8os.kotlintestsample.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import net.pot8os.kotlintestsample.R

/**
 * Shared navigation helpers for all screens
 * Subclasses inherit toolbar interactions (back, menu, etc.)
 *
 * Note: This base class serves as a forward-compatible extension point,
 * reserved for future multi-screen expansion
 */
abstract class BasePage {

    fun pressBack() {
        onView(withContentDescription("Navigate up")).perform(click())
    }

    fun openMenu() {
        onView(withContentDescription("Open navigation menu")).perform(click())
    }

    // Add shared toolbar / navigation actions here as the app grows.
}
