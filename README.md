# Calculator App — Espresso UI Test Suite

A comprehensive Kotlin + Espresso test suite for a calculator Android app, demonstrating the Page Object pattern and multi-environment test architecture. Tests run via both Robolectric (JVM-based) and Instrumented (emulator/device) runners, with shared spec logic and validation for the All Clear button.

## How to run

**Robolectric (no emulator needed):**
```bash
./gradlew :app:testDebugUnitTest
```

**Instrumented (requires emulator or device):**
```bash
./gradlew :app:connectedDebugAndroidTest
```

Both commands run the same 8 tests — 4 arithmetic tests from the original codebase and 4 AC button tests I added.

### Verbose test output

By default, Gradle outputs only a summary. To enable per-test result logging and capture test output, add the following configuration to the `android` block in `app/build.gradle.kts`:

```kotlin
testOptions {
    unitTests.isIncludeAndroidResources = true
    unitTests.all {
        it.testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
            showStandardStreams = true
        }
    }
}
```

The `showStandardStreams = true` flag is necessary to print captured output; without it, the `standardOut` and `standardError` events are registered but produce no output.

With this configuration, test results appear on the terminal as:

```
net.pot8os.kotlintestsample.RobolectricCalculatorTest > testSum PASSED
net.pot8os.kotlintestsample.RobolectricCalculatorTest > testSub PASSED
net.pot8os.kotlintestsample.RobolectricCalculatorTest > testMul PASSED
net.pot8os.kotlintestsample.RobolectricCalculatorTest > testDiv PASSED
net.pot8os.kotlintestsample.RobolectricAllClearTest > testAllClearAfterDigitEntry PASSED
net.pot8os.kotlintestsample.RobolectricAllClearTest > testAllClearAfterCalculation PASSED
net.pot8os.kotlintestsample.RobolectricAllClearTest > testAllClearMidOperation PASSED
net.pot8os.kotlintestsample.RobolectricAllClearTest > testAllClearThenNewCalculation PASSED
```

If Gradle reports all tests as `up-to-date` and skips execution, use `--rerun` to bypass the build cache:

```bash
./gradlew :app:testDebugUnitTest --rerun
```

> Note: `--rerun` affects only Robolectric tests. Instrumented tests always redeploy the APK to the target device; Gradle does not cache their results, so `--rerun` has no effect. Instrumented test results are available in the HTML report at `app/build/reports/androidTests/`.

### Running a single class or test method

**Single class (Robolectric):**
```bash
# Only the 4 AC tests
./gradlew :app:testDebugUnitTest --tests "net.pot8os.kotlintestsample.RobolectricAllClearTest"

# Only the 4 arithmetic tests
./gradlew :app:testDebugUnitTest --tests "net.pot8os.kotlintestsample.RobolectricCalculatorTest"
```

**Single class (Instrumented):**
```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.pot8os.kotlintestsample.InstrumentedAllClearTest
```

**Single test method (Robolectric):**
```bash
./gradlew :app:testDebugUnitTest \
  --tests "net.pot8os.kotlintestsample.RobolectricAllClearTest.testAllClearMidOperation"
```

**Single test method (Instrumented):**
```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.pot8os.kotlintestsample.InstrumentedAllClearTest#testAllClearMidOperation
```

**Wildcards (Robolectric only):**
```bash
# All classes containing "AllClear"
./gradlew :app:testDebugUnitTest --tests "*AllClear*"

# All methods starting with "testAllClear"
./gradlew :app:testDebugUnitTest --tests "*.testAllClear*"
```

> Robolectric uses `--tests`, Instrumented uses `-Pandroid.testInstrumentationRunnerArguments.class=`. The syntax is different because they run on different test runners.

If Gradle says **"no tests found"**, it's probably the build cache — add `--rerun`:
```bash
./gradlew :app:testDebugUnitTest --tests "*AllClear*" --rerun
```

## Architecture

This project extends the original codebase with a Page Object pattern implementation for test maintenance and readability. The following five new files were added while preserving all existing application and test code:

```
app/src/testShared/kotlin/
  page/
    BasePage.kt             Base class with shared navigation helpers
                             (pressBack, openMenu — stubs for now, ready
                             for multi-screen expansion).

    CalculatorPage.kt       Page Object for the calculator screen.
                             All Espresso calls go through here — typeNumber(),
                             pressAdd(), verifyDisplay(), etc. If button IDs
                             change, you only need to update this file.

  AllClearSpec.kt           Four AC button test cases as an abstract class.
                             No direct Espresso imports in the test logic;
                             everything goes through CalculatorPage.

app/src/androidTest/kotlin/.../
  InstrumentedAllClearTest.kt   Concrete runner for real device / emulator.

app/src/test/kotlin/.../
  RobolectricAllClearTest.kt    Concrete runner for JVM (Robolectric).
```

- **`BasePage`** — Abstract base class providing shared navigation helpers (`pressBack()`, `openMenu()`). Serves as an extension point for multi-screen test architecture.
- **`CalculatorPage`** — Page Object encapsulating all calculator UI interactions. Methods like `typeNumber()`, `pressAdd()`, `verifyDisplay()` replace direct Espresso calls. The `digitButtons` map centralizes button ID mappings, so layout changes only require updates in this file.
- **`AllClearSpec`** — Abstract spec containing four AC button test scenarios. No direct Espresso imports in test logic; all interactions route through `CalculatorPage`.

This structure follows the project's existing convention: shared test specifications reside in `testShared/`, while environment-specific runners are in `test/` and `androidTest/`. Both `AllClearSpec` and `CalculatorSpec` are abstract classes inherited by their respective concrete runners, enabling test reuse across multiple execution contexts.

## Test scenarios

Four distinct test cases cover the All Clear button across different calculator states:

| Test | Setup | Validation |
|------|-------|-----------|
| `testAllClearAfterDigitEntry` | Enter 789, press AC | Display resets to "0"; internal state has only `current` populated |
| `testAllClearAfterCalculation` | Compute 25 + 75 = 100, press AC | Display resets to "0"; memory stack is popped, operator is cleared |
| `testAllClearMidOperation` | Enter 42, press +, enter 8, press AC | Display resets to "0"; memory stack and pending operator are both cleared |
| `testAllClearThenNewCalculation` | Press AC, then compute 3 × 7 | Result is 21 (no residual state from previous calculation) |

The last scenario is particularly important: it detects the case where AC clears the display visually but leaves stale values in the ViewModel's memory stack. This bug would only surface in a subsequent calculation.

## Test execution order

JUnit 4 does not guarantee test method execution order by specification. The framework uses reflection (`getDeclaredMethods()`) to discover test methods, and the resulting order depends on the JVM implementation. Tests may execute in any order, not necessarily the order they appear in source code.

This architecture mitigates this uncertainty through isolation. Each test method is preceded by a `@Before` fixture that launches a fresh `CalculatorFragment` with clean state (`current = 0`, `memory = empty`, `figure = NONE`). Tests are therefore independent — the execution order cannot affect correctness or outcomes.

## Page Object pattern

The Page Object pattern abstracts UI interaction details away from test logic. Test code reads as a sequence of user actions rather than framework calls. This improves readability and locates brittle selectors in one place.

Example comparison:

**Without Page Object (original code):**
```kotlin
// 9 lines of Espresso framework calls to compute 123 + 321
onView(withId(R.id.button_1)).perform(click())
onView(withId(R.id.button_2)).perform(click())
onView(withId(R.id.button_3)).perform(click())
onView(withId(R.id.button_sum)).perform(click())
// ... 5 more lines
```

**With Page Object (new code):**
```kotlin
// Same operation, expressed as user intent
calculator.typeNumber(123)
calculator.pressAdd()
calculator.typeNumber(321)
calculator.pressEquals()
calculator.verifyDisplay("444")
```

`CalculatorPage` maintains a `digitButtons` map that translates digit values to resource IDs. When the layout changes, button IDs are updated in this map only — test code remains unchanged.

`BasePage` provides a foundation for multi-screen navigation (`pressBack()`, `openMenu()`). New Page Objects can inherit shared navigation behavior without duplication.

## Project structure

```
app/src/
├── main/kotlin/.../                  App source (MVVM: Fragment → ViewModel → Figure)
├── testShared/kotlin/
│   ├── CalculatorSpec.kt             Original arithmetic tests (4 tests, not modified)
│   ├── AllClearSpec.kt               AC button tests (4 tests, new)
│   └── page/
│       ├── BasePage.kt               Shared navigation base class (new)
│       └── CalculatorPage.kt         Calculator Page Object (new)
├── test/kotlin/.../
│   ├── RobolectricCalculatorTest.kt  Robolectric runner for arithmetic (original)
│   └── RobolectricAllClearTest.kt    Robolectric runner for AC tests (new)
└── androidTest/kotlin/.../
    ├── InstrumentedCalculatorTest.kt Instrumented runner for arithmetic (original)
    └── InstrumentedAllClearTest.kt   Instrumented runner for AC tests (new)
```

## Test coverage and design rationale

The All Clear button's implementation contains no conditional branches — it unconditionally resets `current`, `memory`, and `figure` regardless of prior state. Therefore, there is no negative or error path to test at the UI level.

The four test scenarios above cover all meaningful state combinations at the UI boundary:
- Isolated digit entry
- Post-calculation state with populated memory stack
- Mid-operation state with pending operator
- Post-AC recovery and subsequent calculation

Edge cases such as arithmetic overflow or division-by-zero belong in ViewModel unit tests, not in UI-level Espresso tests. Maintaining clear scope boundaries improves test maintainability and diagnostic clarity.

## Attribution

Forked from [cardinalblue/Kotlin-Espresso-sample](https://github.com/cardinalblue/Kotlin-Espresso-sample), originally authored by So Nakamura.