package com.jayelmeynak.feature.player.impl.service

import android.content.Intent
import android.view.KeyEvent
import com.jayelmeynak.feature.player.impl.persistence.savedQueueFile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric — ради `Intent`, `KeyEvent` и запуска сервиса: приёмник ищет сервис сессии в
 * манифесте модуля и стартует его как foreground-сервис.
 */
@RunWith(RobolectricTestRunner::class)
class PlaybackMediaButtonReceiverTest {

    private val context = RuntimeEnvironment.getApplication()

    @After
    fun tearDown() {
        context.savedQueueFile().delete()
    }

    @Test
    fun `нет сохранённой очереди - сервис не стартует`() {
        PlaybackMediaButtonReceiver().onReceive(context, playIntent())

        assertNull(shadowOf(context).nextStartedService)
    }

    @Test
    fun `есть сохранённая очередь - стартует сервис плеера с медиакнопкой`() {
        writeSavedQueue("""{"items":[{"id":"42","source":"DEEZER","title":"T","artist":"A","artworkUri":null,"durationMs":0}]}""")

        PlaybackMediaButtonReceiver().onReceive(context, playIntent())

        val started = shadowOf(context).nextStartedService
        assertEquals(PlayBackService::class.java.name, started?.component?.className)
        assertEquals(Intent.ACTION_MEDIA_BUTTON, started?.action)
    }

    @Test
    fun `сохранённая очередь пуста (файл после замены повреждённого) - сервис не стартует`() {
        writeSavedQueue("""{"items":[]}""")

        PlaybackMediaButtonReceiver().onReceive(context, playIntent())

        assertNull(shadowOf(context).nextStartedService)
    }

    private fun writeSavedQueue(json: String) {
        context.savedQueueFile().apply { parentFile?.mkdirs() }.writeText(json)
    }

    private fun playIntent() = Intent(Intent.ACTION_MEDIA_BUTTON)
        .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
}
