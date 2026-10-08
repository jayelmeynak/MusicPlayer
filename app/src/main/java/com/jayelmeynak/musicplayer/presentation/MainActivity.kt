package com.jayelmeynak.musicplayer.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jayelmeynak.musicplayer.R
import com.jayelmeynak.musicplayer.presentation.navigation.AppNavigation
import com.jayelmeynak.player.service.PlayBackService
import com.jayelmeynak.lib.designsystem.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isServiceRunning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Without the notification permission playback still works, so only the audio denial is reported.
        if (results[getRequiredPermission()] == false) handlePermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A recreated Activity must not ask again: a second denial blocks the system dialog for good.
        if (savedInstanceState == null) checkPermissions()
        setContent {
            AppTheme {
                AppNavigation(
                    startService = { startService() },
                )
            }
        }
    }

    private fun checkPermissions() {
        val deniedPermissions = buildList {
            add(getRequiredPermission())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED
        }
        if (deniedPermissions.isNotEmpty()) {
            permissionLauncher.launch(deniedPermissions.toTypedArray())
        }
    }

    private fun getRequiredPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun handlePermissionResult() {
        Toast.makeText(this, getString(R.string.read_media_audio_required), Toast.LENGTH_LONG)
            .show()
    }

    private fun startService() {
        if (!isServiceRunning) {
            val intent = Intent(this, PlayBackService::class.java)
            startForegroundService(intent)
            isServiceRunning = true
        }
    }
}
