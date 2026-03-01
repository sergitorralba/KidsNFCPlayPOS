package com.kidsnfcplaypos.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsnfcplaypos.MainActivity
import com.kidsnfcplaypos.R
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @Test
    fun fullAppFlowTest() {
        // Launch MainActivity
        ActivityScenario.launch(MainActivity::class.java)

        // 1. Splash Screen
        // We wait for splash screen to finish (2 seconds delay)
        Thread.sleep(4000)

        // 2. Direct Input Screen should be visible
        onView(withId(R.id.amountDisplayText)).check(matches(isDisplayed()))
        onView(withId(R.id.amountDisplayText)).check(matches(org.hamcrest.Matchers.anyOf(withText("0.00"), withText("0,00"))))

        // 3. Type an amount: 1 2 5
        onView(withId(R.id.btn1)).perform(click())
        onView(withId(R.id.btn2)).perform(click())
        onView(withId(R.id.btn5)).perform(click())
        
        onView(withId(R.id.amountDisplayText)).check(matches(org.hamcrest.Matchers.anyOf(withText("1.25"), withText("1,25"))))

        // 4. Click ENTER to go to Payment
        onView(withId(R.id.btnEnter)).perform(click())

        // 5. Check if Payment Simulation is visible
        onView(withId(R.id.text_waiting_for_nfc)).check(matches(isDisplayed()))
        onView(withId(R.id.text_amount_to_pay)).check(matches(org.hamcrest.Matchers.anyOf(withText(containsString("1.25")), withText(containsString("1,25")))))
    }
}
