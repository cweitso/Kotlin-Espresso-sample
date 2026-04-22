package net.pot8os.kotlintestsample.page

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import net.pot8os.kotlintestsample.R

/** Page Object for Calculator screen. */
class CalculatorPage : BasePage() {

    private val digitButtons = mapOf(
        0 to R.id.button_0,
        1 to R.id.button_1,
        2 to R.id.button_2,
        3 to R.id.button_3,
        4 to R.id.button_4,
        5 to R.id.button_5,
        6 to R.id.button_6,
        7 to R.id.button_7,
        8 to R.id.button_8,
        9 to R.id.button_9,
    )

    fun typeDigit(digit: Int) {
        val resId = digitButtons[digit]
            ?: throw IllegalArgumentException("Invalid digit: $digit")
        onView(withId(resId)).perform(click())
    }

    /** e.g. typeNumber(456) presses 4 → 5 → 6 */
    fun typeNumber(number: Int) {
        number.toString().forEach { char ->
            typeDigit(char.digitToInt())
        }
    }

    fun pressAdd()      = onView(withId(R.id.button_sum)).perform(click())
    fun pressSubtract() = onView(withId(R.id.button_sub)).perform(click())
    fun pressMultiply() = onView(withId(R.id.button_mul)).perform(click())
    fun pressDivide()   = onView(withId(R.id.button_div)).perform(click())
    fun pressEquals()   = onView(withId(R.id.button_calc)).perform(click())

    fun pressAllClear() = onView(withId(R.id.button_all_clear)).perform(click())

    fun verifyDisplay(expected: String) {
        onView(withId(R.id.field)).check(matches(withText(expected)))
    }
}