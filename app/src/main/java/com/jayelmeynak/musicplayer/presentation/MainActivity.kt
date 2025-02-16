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
import androidx.compose.material3.Scaffold
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.jayelmeynak.musicplayer.R
import com.jayelmeynak.musicplayer.presentation.navigation.BottomNavigationBar
import com.jayelmeynak.musicplayer.presentation.navigation.Navigation
import com.jayelmeynak.player.player.service.PlayBackService
import com.jayelmeynak.player.presentation.AudioViewModel
import com.jayelmeynak.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isServiceRunning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            handlePermissionResult()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        setContent {
            val viewModel: AudioViewModel = hiltViewModel()
            AppTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            viewModel,
                            navController
                        )
                    }
                ) { scaffoldPadding ->
                    Navigation(
                        viewModel = viewModel,
                        scaffoldPadding = scaffoldPadding,
                        navController = navController,
                        startService = {
                            startService()
                        }
                    )
                }
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermission = getRequiredPermission()
        when {
            ContextCompat.checkSelfPermission(
                this,
                requiredPermission
            ) == PackageManager.PERMISSION_DENIED -> {
                permissionLauncher.launch(requiredPermission)
            }
        }
    }

    private fun getRequiredPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun handlePermissionResult() {
        Toast.makeText(this, this.getString(R.string.read_media_audio_required), Toast.LENGTH_LONG)
            .show()
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
