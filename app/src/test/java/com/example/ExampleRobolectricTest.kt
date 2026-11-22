package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.KickoffRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kickoff Arena", appName)
    }

    @Test
    fun `calculate match points matches kickoff arena formula`() {
        // 2-0 win: 2*100 + 250(W) + 100(clean sheet) = 550
        assertEquals(550, KickoffRepository.calculatePoints(2, 0, "W"))
        // 1-1 draw: 1*100 + 80(D) - 25 = 155
        assertEquals(155, KickoffRepository.calculatePoints(1, 1, "D"))
    }
}
