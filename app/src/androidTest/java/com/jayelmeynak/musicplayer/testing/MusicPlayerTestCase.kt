package com.jayelmeynak.musicplayer.testing

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.service.notification.StatusBarNotification
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.jayelmeynak.feature.player.api.PlaybackController
import com.jayelmeynak.feature.player.api.PlaybackState
import com.jayelmeynak.musicplayer.presentation.MainActivity
import com.jayelmeynak.musicplayer.screen.TrackListScreen
import com.kaspersky.components.composesupport.config.addComposeSupport
import com.kaspersky.kaspresso.interceptors.watcher.testcase.impl.screenshot.ScreenshotStepWatcherInterceptor
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.BaseTestContext
import dagger.hilt.android.testing.HiltAndroidRule
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.ExternalResource

/**
 * Основа инструментальных UI-тестов: Kaspresso с поддержкой Compose (шаги в логе, при падении —
 * скриншот, видео, дамп иерархии и logcat), Hilt с фейковым Deezer, настоящие сервис, плеер и сессия.
 *
 * Данные приложения не стираются: каждый тест сам задаёт очередь и не рассчитывает на пустое
 * начальное состояние; сервис плеера живёт между тестами одного процесса.
 *
 * Помощники с ожиданием — расширения контекста шага Kaspresso (`flakySafely`).
 *
 * Контроллер плеера тест берёт из `MainActivity`, а не инъекцией в тест: первая инъекция контроллера
 * обязана быть на main-потоке, а `HiltAndroidRule.inject()` идёт на потоке инструментации.
 *
 * Контроллеры прошлых тестов не освобождаются: на каждый тест `HiltTestApplication` создаёт новый
 * `SingletonComponent` и новый `@Singleton`-контроллер, подписанный на процессный `LifecycleOwner`;
 * к концу прогона их около дюжины, все подключены к той же сессии. Заменить процессный
 * `LifecycleOwner` можно только в `internal`-модуле `:impl`, а `app` от `:impl` не зависит — принято:
 * команды шлёт только контроллер текущего теста, состояние у всех — снапшот одной сессии.
 */
abstract class MusicPlayerTestCase : TestCase(kaspressoBuilder = kaspressoBuilder()) {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val serverRule = FakeDeezerServerRule()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(*requiredPermissions())

    /** Погасший экран телефона (Dozing) не даёт Activity выйти в RESUMED — будим до её запуска. */
    @get:Rule(order = 3)
    val wakeUpRule = object : ExternalResource() {
        override fun before() {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).wakeUp()
        }
    }

    @get:Rule(order = 4)
    val composeRule = createAndroidComposeRule<MainActivity>()

    protected val chart = FakeDeezerServer.chartTracks

    protected val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun resetPlayback() {
        // Звук между тестами не нужен. Позиция — в начало: `play` той же очереди продолжает с паузы,
        // и без сброса позиция копилась бы от теста к тесту, а превью кончалось бы посреди теста.
        val state = runCatching { controller().state.value }.getOrNull() as? PlaybackState.Active ?: return
        onMain {
            if (!state.showPlayButton) controller().togglePlayPause()
            controller().seekTo(0)
        }
    }

    protected fun controller(): PlaybackController {
        var controller: PlaybackController? = null
        composeRule.activityRule.scenario.onActivity { controller = it.playbackController }
        return checkNotNull(controller)
    }

    protected fun active(): PlaybackState.Active =
        controller().state.value as? PlaybackState.Active ?: throw AssertionError("Контроллер не подключён")

    /** Тап по треку чарта и ожидание, что он стал текущим и играет. */
    protected fun BaseTestContext.playFromChart(index: Int) {
        val title = chart[index].title
        onComposeScreen<TrackListScreen>(composeRule) {
            flakySafely(LOAD_TIMEOUT_MS) { track(title).assertIsDisplayed() }
            track(title).performClick()
        }
        awaitPlaying(title)
    }

    protected fun BaseTestContext.awaitPlaying(title: String) = flakySafely(PLAYBACK_TIMEOUT_MS) {
        val state = active()
        assertEquals(title, state.current?.title)
        assertTrue("плеер не играет: $state", state.isPlaying)
    }

    protected fun BaseTestContext.awaitPaused(title: String) = flakySafely(PLAYBACK_TIMEOUT_MS) {
        val state = active()
        assertEquals(title, state.current?.title)
        assertTrue("плеер не на паузе: $state", !state.isPlaying && state.showPlayButton)
    }

    /** Состояние системной сессии пакета из `dumpsys media_session` (то же, что в ручной проверке). */
    protected fun session(): SessionDump = SessionDump.parse(shell("dumpsys media_session"), context.packageName)

    protected fun BaseTestContext.awaitSession(state: Int, title: String) = flakySafely(PLAYBACK_TIMEOUT_MS) {
        val dump = session()
        assertEquals("состояние сессии: ${dump.raw}", state, dump.state)
        assertTrue("описание сессии: ${dump.description}", dump.description.orEmpty().contains(title))
    }

    /** Уведомление плеера своего пакета (медиа-уведомление сессии). */
    protected fun playerNotification(): StatusBarNotification {
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.activeNotifications.firstOrNull {
            it.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
        } ?: throw AssertionError("Уведомления плеера нет")
    }

    protected fun BaseTestContext.awaitNotificationTitle(title: String) = flakySafely(PLAYBACK_TIMEOUT_MS) {
        assertEquals(title, playerNotification().notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    }

    /**
     * Нажатие кнопки уведомления: то же действие, что тап по ней в шторке. Кнопки уведомление
     * обновляет отдельно от заголовка — их появления ждём.
     */
    protected fun BaseTestContext.tapNotificationAction(media3StringName: String) {
        val label = context.getString(context.resources.getIdentifier(media3StringName, "string", context.packageName))
        val action = flakySafely(PLAYBACK_TIMEOUT_MS) {
            // AssertionError, а не checkNotNull: flakySafely повторяет только ошибки проверок.
            playerNotification().notification.actions?.firstOrNull { it.title?.toString() == label }
                ?: throw AssertionError("В уведомлении нет кнопки «$label»")
        }
        action.actionIntent.send()
    }

    /** Заголовок трека виден в шторке уведомлений (текст — наши данные, не зависит от языка системы). */
    protected fun assertTitleInNotificationShade(title: String) {
        val uiDevice = device.uiDevice
        uiDevice.openNotification()
        try {
            assertTrue(
                "В шторке нет «$title»",
                uiDevice.wait(Until.hasObject(By.textContains(title)), SHADE_TIMEOUT_MS),
            )
        } finally {
            shell("cmd statusbar collapse")
        }
    }

    protected fun resumedMainActivities(): List<Activity> {
        var resumed: List<Activity> = emptyList()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>()
        }
        return resumed
    }

    protected fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    /** Команда оболочки с правами shell (как `adb shell`), без AdbServer. */
    protected fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    }

    companion object {
        const val LOAD_TIMEOUT_MS = 10_000L
        const val PLAYBACK_TIMEOUT_MS = 15_000L
        const val SHADE_TIMEOUT_MS = 5_000L

        const val STATE_PAUSED = 2
        const val STATE_PLAYING = 3

        /**
         * Расширенная конфигурация Kaspresso: при падении — скриншот, видео, дамп иерархии и logcat
         * в [ArtifactsDirsProvider]; скриншоты каждого шага отключены (лишние файлы на устройстве).
         */
        private fun kaspressoBuilder(): Kaspresso.Builder =
            Kaspresso.Builder.advanced { dirsProvider = ArtifactsDirsProvider() }
                .addComposeSupport()
                .apply { stepWatcherInterceptors.removeAll { it is ScreenshotStepWatcherInterceptor } }

        private fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
    }
}
