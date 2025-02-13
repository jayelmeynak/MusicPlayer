package com.jayelmeynak.player.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import com.jayelmeynak.player.player.service.PlayBackService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {
    private val viewModel: AudioViewModel by viewModels()
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val source = intent.getStringExtra("source")

        if (source == "local") {
            val trackUri = intent.getStringExtra("trackUri")
            if (!trackUri.isNullOrEmpty()) {
                viewModel.loadLocalTrack(trackUri)
            }
        } else {
            val trackId = intent.getStringExtra("trackId")
            if (!trackId.isNullOrEmpty()) {
                viewModel.loadRemoteTrack(trackId)
            }
        }
        startService()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PlayerScreen(viewModel)
                }
            }
        }
    }

    // Я привык что если закрыть приложение, то музыка тоже останавливается
    override fun onDestroy() {
        stopService()
        super.onDestroy()
    }

    private fun startService() {
        if (!isServiceRunning) {
            val intent = Intent(this, PlayBackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            isServiceRunning = true
        }
    }

    private fun stopService() {
        if (isServiceRunning) {
            val intent = Intent(this, PlayBackService::class.java)
            stopService(intent)
            isServiceRunning = false
        }
    }
}