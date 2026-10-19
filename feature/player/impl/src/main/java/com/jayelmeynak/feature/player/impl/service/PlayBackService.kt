package com.jayelmeynak.feature.player.impl.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.R as Media3R
import com.jayelmeynak.feature.player.api.OpenPlayerRequest
import com.jayelmeynak.feature.player.impl.artwork.ContentArtworkBitmapLoader
import com.jayelmeynak.feature.player.impl.persistence.PlaybackPersistence
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * Owns the player and the media session for its lifetime: both are created in [onCreate] and
 * released in [onDestroy], so a service started again in the same process starts with a new
 * working pair. The app UI reaches them only through a `MediaController`. The standard Media3
 * notification follows the session and puts the service in the foreground while playback is
 * ongoing; nothing calls `startForeground` by hand.
 *
 * Новая сессия поднимает сохранённую очередь на паузе ([SessionRestorer]), сессия сохраняет очередь
 * и позицию ([PlaybackPersistence]); play медиакнопкой после смерти процесса стартует сервис через
 * [PlaybackMediaButtonReceiver] и продолжает сохранённый трек.
 *
 * [onTaskRemoved] is not overridden on purpose (owner's decision): the Media3 default keeps the
 * service while playing and stops it otherwise.
 */
@UnstableApi
@AndroidEntryPoint
internal class PlayBackService : MediaSessionService() {

    @Inject
    lateinit var playerFactory: PlayerFactory

    @Inject
    lateinit var sessionCallback: PlaybackSessionCallback

    @Inject
    lateinit var bitmapLoader: ContentArtworkBitmapLoader

    @Inject
    lateinit var persistence: PlaybackPersistence

    @Inject
    lateinit var restorer: SessionRestorer

    private var mediaSession: MediaSession? = null
    private var restoreJob: Job? = null
    private lateinit var notificationProvider: DefaultMediaNotificationProvider

    /**
     * Первый старт сервиса — медиакнопкой (`startForegroundService` из приёмника): сервис обязан уйти в
     * foreground или остановиться. Снимается после первого исхода возобновления: медиакнопки из
     * уведомления работающего сервиса и поздние запросы play к нему не относятся.
     */
    private var startedByMediaButton = false
    private var firstStartHandled = false

    override fun onCreate() {
        super.onCreate()
        notificationProvider = DefaultMediaNotificationProvider.Builder(this).build()
        setMediaNotificationProvider(notificationProvider)
        val player = playerFactory.create()
        setListener(object : Listener {
            /**
             * Android 12+ refuses a foreground start from the background (e.g. a play command
             * from a Bluetooth device long after the app was left). Sound without a notification
             * and a foreground service is not allowed: pause instead.
             */
            override fun onForegroundServiceStartNotAllowedException() {
                Log.w(TAG, "Foreground start not allowed, pausing playback")
                player.pause()
            }
        })
        val session = MediaSession.Builder(this, player)
            .setCallback(sessionCallback.also { it.onResumptionFinished = ::onResumptionFinished })
            .setBitmapLoader(CacheBitmapLoader(bitmapLoader))
            .apply { sessionActivity()?.let(::setSessionActivity) }
            .setSessionExtras(sessionExtras(restoring = true))
            .build()
        mediaSession = session
        // Media3 добавляет сессию, когда контроллер подключается через onGetSession. Сервис
        // стартует и без контроллера — медиакнопкой через PlaybackMediaButtonReceiver (например,
        // после смерти процесса): тогда сессию добавляет только этот вызов, и уведомление
        // появляется, как только возобновлённая очередь заиграет.
        addSession(session)
        persistence.attach(player)
        restoreJob = restorer.restore(session)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!firstStartHandled) {
            firstStartHandled = true
            startedByMediaButton = intent?.action == Intent.ACTION_MEDIA_BUTTON
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        restoreJob?.cancel()
        restoreJob = null
        sessionCallback.onResumptionFinished = null
        // До освобождения плеера: последняя позиция снимается с него.
        persistence.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        clearListener()
        super.onDestroy()
    }

    /** Исход первого возобновления после старта медиакнопкой; дальше признак старта не действует. */
    private fun onResumptionFinished(succeeded: Boolean) {
        val wasStartedByMediaButton = startedByMediaButton
        startedByMediaButton = false
        if (!succeeded && wasStartedByMediaButton) stopIfNothingToPlay()
    }

    /**
     * Медиакнопка запустила сервис как foreground, а возобновлять нечего: плеер пуст, Media3 не
     * уйдёт в foreground, и система уронит процесс (`ForegroundServiceDidNotStartInTimeException`).
     * Повторяет приватный `MediaSessionService.stopSelfSafely`: служебное скрытое уведомление
     * выполняет обязательство `startForeground`, затем оно снимается и сервис останавливается.
     * Запрет foreground-старта (API 31+) не мешает остановке.
     */
    private fun stopIfNothingToPlay() {
        val player = mediaSession?.player ?: return
        if (player.mediaItemCount > 0 || isPlaybackOngoing) return
        Log.w(TAG, "Nothing to resume after a media button, stopping the service")
        val channel = notificationProvider.notificationChannelInfo
        Util.ensureNotificationChannel(getSystemService(NotificationManager::class.java), channel.id, channel.name)
        val notification = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(Media3R.drawable.media3_notification_small_icon)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .setOngoing(false)
            .build()
        try {
            Util.setForegroundServiceNotification(
                this,
                SHUTDOWN_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                "mediaPlayback",
            )
            Util.stopForeground(this, true)
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (API 31+): foreground недоступен, но остановиться надо.
            Log.w(TAG, "Could not start foreground before stopping", e)
        }
        pauseAllPlayersAndStopSelf()
    }

    /**
     * Тап по уведомлению: экран плеера. Явный интент launcher-активности (`MainActivity`, её класс
     * модулю не виден) с действием [OpenPlayerRequest.ACTION]: действие отличает его от запуска
     * с иконки (`filterEquals`), extras нет. Неизменяемый — отправитель не допишет ни extras, ни
     * компонент. `MainActivity` — `singleTop`: живая Activity наверху получает `onNewIntent`, второй
     * копии нет. Без launcher-активности (компонент отключён) — `null`: уведомление без перехода.
     */
    private fun sessionActivity(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.component?.let { launcher ->
            val intent = Intent(OpenPlayerRequest.ACTION)
                .setComponent(launcher)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

    private companion object {
        const val TAG = "PlayBackService"

        /**
         * Id служебного уведомления остановки. Не совпадает с уведомлением плеера
         * (`DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID` = 1001): `stopForeground(true)`
         * снимает только служебное. Константа Media3 для того же случая (20938 в `MediaNotificationManager`)
         * не публичная — опираться на неё нельзя, поэтому свой id.
         */
        const val SHUTDOWN_NOTIFICATION_ID = 1002
    }
}
