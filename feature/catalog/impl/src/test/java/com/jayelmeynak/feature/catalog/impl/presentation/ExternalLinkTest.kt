package com.jayelmeynak.feature.catalog.impl.presentation

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Открытие ссылки во внешнем приложении. Robolectric — ради `Context.startActivity` и менеджера пакетов. */
@RunWith(RobolectricTestRunner::class)
class ExternalLinkTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val link = "https://www.deezer.com/track/42"

    @Before
    fun setUp() {
        shadowOf(app).checkActivities(true)
    }

    @Test
    fun `есть браузер - открывается просмотр ссылки из браузера`() {
        installBrowser()

        assertTrue(openExternalLink(app, link))

        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(Uri.parse(link), started.data)
        assertTrue(started.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun `из контекста приложения - новая задача`() {
        installBrowser()

        openExternalLink(app, link)

        assertTrue(shadowOf(app).nextStartedActivity.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `из Activity, в том числе обёрнутой, - без новой задачи`() {
        installBrowser()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        assertTrue(openExternalLink(ContextWrapper(activity), link))

        val started = shadowOf(activity).nextStartedActivity
        assertEquals(0, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `нет приложения для ссылки - false без падения`() {
        assertFalse(openExternalLink(app, link))
    }

    @Test
    fun `система запретила запуск - false без падения`() {
        val forbidden = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) = throw SecurityException("запрещено")
        }

        assertFalse(openExternalLink(forbidden, link))
    }

    private fun installBrowser() {
        val browser = ComponentName("com.example.browser", "com.example.browser.Main")
        shadowOf(app.packageManager).apply {
            addActivityIfNotPresent(browser)
            addIntentFilterForActivity(
                browser,
                IntentFilter(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addDataScheme("https")
                },
            )
        }
    }
}
