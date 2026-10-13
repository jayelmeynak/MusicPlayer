package com.jayelmeynak.musicplayer

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Разрешения смёрженного манифеста приложения: каждое объявляет модуль-владелец (`local-music:impl` —
 * аудио, `player:impl` — уведомления), итог собирает merge. Robolectric — ради разбора манифеста сборки
 * на заданном уровне API: разрешение с `maxSdkVersion` выше него не запрашивается.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ManifestPermissionsTest {

    @Test
    fun `API 34 - аудио и уведомления, без широкого доступа к хранилищу`() {
        val requested = requestedPermissions()

        assertTrue(requested.toString(), Manifest.permission.READ_MEDIA_AUDIO in requested)
        assertTrue(requested.toString(), Manifest.permission.POST_NOTIFICATIONS in requested)
        assertFalse(requested.toString(), Manifest.permission.READ_EXTERNAL_STORAGE in requested)
    }

    @Test
    @Config(sdk = [29])
    fun `API 29 - аудио читается по READ_EXTERNAL_STORAGE`() {
        val requested = requestedPermissions()

        assertTrue(requested.toString(), Manifest.permission.READ_EXTERNAL_STORAGE in requested)
    }

    private fun requestedPermissions(): List<String> {
        val app = RuntimeEnvironment.getApplication()
        return app.packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toList()
    }
}
