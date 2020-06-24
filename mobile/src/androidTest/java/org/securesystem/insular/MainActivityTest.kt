package org.securesystem.insular


import android.view.View
import android.view.ViewGroup
import androidx.test.InstrumentationRegistry.getInstrumentation
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.securesystem.insular.mobile.R
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule


@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @ClassRule
    val localeTestRule = LocaleTestRule()

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun mainActivityTest() {
        // Improved screenshot capture with UI Automator
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy());

        Screengrab.screenshot("setup_wizard");

        val navButton = onView(
                allOf(withId(R.id.suw_navbar_next), withText("Accept"),
                        childAtPosition(
                                allOf(withId(R.id.suw_layout_navigation_bar),
                                        childAtPosition(
                                                withClassName(`is`("android.widget.LinearLayout")),
                                                1)),
                                3),
                        isDisplayed()))
        navButton.perform(click())

        Screengrab.screenshot("setting_work_profile");

        val bottomNavigationItemView = onView(
                allOf(withId(R.id.tab_mainland), withContentDescription("Mainland"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_navigation),
                                        0),
                                0),
                        isDisplayed()))
        bottomNavigationItemView.perform(click())

        Screengrab.screenshot("main_activity");

        val textView = onView(
                allOf(withId(R.id.entry_name), withText("Files"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.app_list),
                                        7),
                                0),
                        isDisplayed()))
        textView.perform(click())

        val textView2 = onView(
                allOf(withId(R.id.entry_name), withText("Calendar"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.app_list),
                                        1),
                                0),
                        isDisplayed()))
        textView2.perform(click())

        val actionMenuItemView = onView(
                allOf(withId(R.id.menu_clone), withContentDescription("Clone"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.toolbar),
                                        0),
                                0),
                        isDisplayed()))
        actionMenuItemView.perform(click())

        Screengrab.screenshot("clone_an_app");

        val bottomNavigationItemView2 = onView(
                allOf(withId(R.id.tab_discovery), withContentDescription("Discovery"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_navigation),
                                        0),
                                2),
                        isDisplayed()))
        bottomNavigationItemView2.perform(click())

        Screengrab.screenshot("discovery");

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())

        val textView3 = onView(
                allOf(withId(android.R.id.title), withText("Test"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()))
        textView3.perform(click())

        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext())

        val textView4 = onView(
                allOf(withId(android.R.id.title), withText("Settings"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()))
        textView4.perform(click())

        Screengrab.screenshot("settings");

        val linearLayout = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withClassName(`is`("android.widget.LinearLayout")),
                                0)))
                .atPosition(0)
        linearLayout.perform(click())

        Screengrab.screenshot("choosing_profile");

        val imageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withClassName(`is`("android.widget.Toolbar")),
                                        childAtPosition(
                                                withClassName(`is`("com.android.internal.widget.ActionBarContainer")),
                                                0)),
                                0),
                        isDisplayed()))
        imageButton.perform(click())

        val linearLayout2 = onData(anything())
                .inAdapterView(allOf(withId(android.R.id.list),
                        childAtPosition(
                                withClassName(`is`("android.widget.LinearLayout")),
                                0)))
                .atPosition(1)
        linearLayout2.perform(click())

        val textView5 = onData(anything())
                .inAdapterView(allOf(withClassName(`is`("com.android.internal.app.AlertController$RecycleListView")),
                        childAtPosition(
                                withClassName(`is`("android.widget.FrameLayout")),
                                0)))
                .atPosition(1)
        textView5.perform(click())
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
