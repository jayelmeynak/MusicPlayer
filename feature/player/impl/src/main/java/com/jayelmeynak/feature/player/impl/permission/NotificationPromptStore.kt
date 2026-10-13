package com.jayelmeynak.feature.player.impl.permission

import android.content.Context
import androidx.core.content.edit
import com.jayelmeynak.util.coroutines.Dispatcher
import com.jayelmeynak.util.coroutines.MusicPlayerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Флаг «разрешение уведомлений уже спрашивали»: экран плеера спрашивает его один раз на установку. */
internal interface NotificationPromptStore {
    suspend fun isPrompted(): Boolean
    suspend fun markPrompted()
}

/** [NotificationPromptStore] в `SharedPreferences`; чтение и запись файла — на IO. */
internal class SharedPreferencesNotificationPromptStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(MusicPlayerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : NotificationPromptStore {

    private val preferences by lazy { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }

    override suspend fun isPrompted(): Boolean = withContext(ioDispatcher) {
        preferences.getBoolean(KEY_NOTIFICATION_PROMPTED, false)
    }

    override suspend fun markPrompted() = withContext(ioDispatcher) {
        // commit, а не apply: флаг обязан лечь на диск до диалога — смерть процесса во время диалога
        // не должна дать второй запрос.
        preferences.edit(commit = true) { putBoolean(KEY_NOTIFICATION_PROMPTED, true) }
    }

    private companion object {
        const val PREFERENCES_NAME = "player_prompts"
        const val KEY_NOTIFICATION_PROMPTED = "notification_permission_prompted"
    }
}
