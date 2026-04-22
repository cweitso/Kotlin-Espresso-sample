package net.pot8os.kotlintestsample

import androidx.fragment.app.testing.launchFragmentInContainer
import net.pot8os.kotlintestsample.page.CalculatorPage
import org.junit.Before
import org.junit.Test

/** AC button tests via Page Object. */
abstract class AllClearSpec {

    private lateinit var calculator: CalculatorPage

    @Before
    fun setup() {
        launchFragmentInContainer<CalculatorFragment>(
            themeResId = R.style.Theme_MyApp
        )
        calculator = CalculatorPage()
    }

    @Test
    fun testAllClearAfterDigitEntry() {
        calculator.typeNumber(789)
        calculator.pressAllClear()
        calculator.verifyDisplay("0")
    }

    @Test
    fun testAllClearAfterCalculation() {
        // 25 + 75 = 100, then AC
        calculator.typeNumber(25)
        calculator.pressAdd()
        calculator.typeNumber(75)
        calculator.pressEquals()
        calculator.verifyDisplay("100")

        calculator.pressAllClear()
        calculator.verifyDisplay("0")
    }

    @Test
    fun testAllClearMidOperation() {
        // AC before pressing equals
        calculator.typeNumber(42)
        calculator.pressAdd()
        calculator.typeNumber(8)
        calculator.pressAllClear()
        calculator.verifyDisplay("0")
    }

    @Test
    fun testAllClearThenNewCalculation() {
        calculator.typeNumber(99)
        calculator.pressAllClear()
        // 3 * 7 = 21
        calculator.typeNumber(3)
        calculator.pressMultiply()
        calculator.typeNumber(7)
        calculator.pressEquals()
        calculator.verifyDisplay("21")
    }
}