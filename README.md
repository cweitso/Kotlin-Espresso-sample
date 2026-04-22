# Calculator App — Espresso UI Test Suite

A Kotlin + Espresso test suite for a simple calculator Android app. Demonstrates Page Object pattern for mobile UI testing, shared test specs across Robolectric and Instrumented runners, and structured AC (All Clear) button validation.

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

By default Gradle only prints a summary. To see per-test PASSED / FAILED lines — plus any stdout/stderr the tests emit — add the following to the `android` block in `app/build.gradle.kts`:

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

`showStandardStreams = true` is required for `standardOut` and `standardError` to actually appear; without it those two events are registered but produce no output.

You will then see output like:

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

If Gradle skips the tests with `32 up-to-date` (nothing changed since the last run), force a rerun:

```bash
./gradlew :app:testDebugUnitTest --rerun
```

> Note: This only affects Robolectric (`testDebugUnitTest`). Instrumented test output is controlled by the Android test runner and appears in the HTML report under `app/build/reports/androidTests/`.

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
# Only the 4 AC tests
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.pot8os.kotlintestsample.InstrumentedAllClearTest
  
# Only the 4 arithmetic tests
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=net.pot8os.kotlintestsample.InstrumentedCalculatorTest
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

> Robolectric uses `--tests`, Instrumented uses `-Pandroid.testInstrumentationRunnerArguments.class=`. The syntax differs because they run on different test runners.

If Gradle says **"no tests found"**, add `--rerun` to bypass the build cache:
```bash
./gradlew :app:testDebugUnitTest --tests "*AllClear*" --rerun
```

> `--rerun` only applies to Robolectric. Instrumented tests (`connectedDebugAndroidTest`) always re-deploy the APK to the device and rerun from scratch — Gradle never caches their results, so `--rerun` has no effect on them.

## What I added

All existing code is untouched. My changes are five new files:

```
app/src/testShared/kotlin/
  page/
    BasePage.kt             Shared navigation helpers (extension point for
                             multi-screen apps — back button, toolbar, etc.)

    CalculatorPage.kt       Page Object for the calculator screen. Wraps
                             every Espresso interaction behind intent-revealing
                             methods: typeNumber(), pressAdd(), verifyDisplay().
                             Digit-to-resource-ID mapping lives here so if the
                             layout changes, only this file needs updating.

  AllClearSpec.kt           Four AC button test cases as an abstract class.
                             Uses CalculatorPage exclusively — zero direct
                             Espresso imports in the test logic.

app/src/androidTest/kotlin/.../
  InstrumentedAllClearTest.kt   Concrete runner for real device / emulator.

app/src/test/kotlin/.../
  RobolectricAllClearTest.kt    Concrete runner for JVM (Robolectric).
```

The structure follows the existing project convention: shared test logic lives in `testShared/`, concrete runners in `test/` and `androidTest/`. `AllClearSpec` parallels `CalculatorSpec` — both are abstract classes inherited by environment-specific runners.

## Test cases

4 scenarios, each testing AC under a different calculator state:

| Test | What it does | What it validates |
|------|-------------|-------------------|
| `testAllClearAfterDigitEntry` | Type 789, press AC | Display resets to "0" with only `current` populated |
| `testAllClearAfterCalculation` | Compute 25 + 75 = 100, press AC | Display resets after a completed operation (memory popped, operator cleared) |
| `testAllClearMidOperation` | Type 42 + 8, press AC without `=` | Memory stack and pending operator are both cleared |
| `testAllClearThenNewCalculation` | AC, then compute 3 × 7 | New calculation yields 21, proving no residual state from before AC |

The last test is the most important one — it catches the case where AC clears the display but leaves stale values in the ViewModel's memory stack.

## A note on test execution order

JUnit 4 does **not** guarantee the execution order of test methods. Internally it uses `getDeclaredMethods()`, whose ordering depends on the JVM implementation and may differ from the source file order. The tests above might run in any permutation.

This is fine because every test method is preceded by `@Before setup()`, which launches a fresh `CalculatorFragment`. Each test starts from a clean state (`current = 0`, `memory = empty`, `figure = NONE`), so results are independent of execution order.

## Page Object design

`CalculatorPage` encapsulates all Espresso calls so that test specs read like user intent rather than framework boilerplate. Compare the original style:

```kotlin
// CalculatorSpec (original) — 9 lines of Espresso calls for 123 + 321
onView(withId(R.id.button_1)).perform(click())
onView(withId(R.id.button_2)).perform(click())
onView(withId(R.id.button_3)).perform(click())
onView(withId(R.id.button_sum)).perform(click())
// ... 5 more lines
```

with the Page Object approach:

```kotlin
// AllClearSpec — same operation in 5 readable lines
calculator.typeNumber(123)
calculator.pressAdd()
calculator.typeNumber(321)
calculator.pressEquals()
calculator.verifyDisplay("444")
```

`CalculatorPage` inherits from `BasePage`, which currently provides `pressBack()` and `openMenu()` as forward-compatible stubs. If the app grows to multiple screens, new Page Objects can inherit shared navigation from `BasePage` without refactoring.

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

## Why no negative tests?

`onClickedAllClear()` in the ViewModel has no conditional branches — it unconditionally resets `current`, `memory`, and `figure` regardless of prior state. There's no failure path to test. The four scenarios above cover all meaningful state combinations (digits only, post-calculation, mid-operation, and post-AC reuse). Edge cases like overflow or division-by-zero are better suited for ViewModel unit tests, not UI-level Espresso tests.

## Attribution

Forked from [cardinalblue/Kotlin-Espresso-sample](https://github.com/cardinalblue/Kotlin-Espresso-sample), originally authored by So Nakamura. 