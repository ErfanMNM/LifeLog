package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ScreenTab
import com.example.ui.calendar.CalendarScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LifeLog", appName)
  }

  @Test
  fun `render CalendarScreen with sample data`() {
    composeTestRule.setContent {
      CalendarScreen(
        notes = emptyList(),
        reminders = emptyList(),
        trips = emptyList(),
        photos = emptyList(),
        onNoteClick = {},
        onReminderClick = {},
        onToggleReminder = {},
        onPhotoClick = {},
        onCreateReminderForDate = {},
        onCreateNoteClick = {}
      )
    }
  }

  @Test
  fun `launch MainActivity with robolectric`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    controller.get()
  }
}

