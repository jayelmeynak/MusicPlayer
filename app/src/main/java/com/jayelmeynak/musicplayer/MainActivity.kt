package com.jayelmeynak.musicplayer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.compose.AppTheme
import com.jayelmeynak.download_tracks.presentation.DownloadTrackScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

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
            AppTheme {
//                ChartTracksScreen(
//                    onTrackClicked = { trackId ->
//                        val uri = Uri.parse("multiplayer://player?source=api&trackId=$trackId")
//                        val parentIntent = Intent("com.jayelmeynak.musicplayer.action.HOME").apply {
//                            addCategory(Intent.CATEGORY_DEFAULT)
//                        }
//                        val playerIntent = Intent(this, PlayerActivity::class.java).apply {
//                            action = Intent.ACTION_VIEW
//                            data = uri
//                        }
//
//                        TaskStackBuilder.create(this)
//                            .addNextIntent(parentIntent)
//                            .addNextIntent(playerIntent)
//                            .startActivities()
//                    }
//                )
                DownloadTrackScreen { trackUri ->
                    val trackUriString = Uri.encode(trackUri.toString())
                    val deepLinkUri =
                        Uri.parse("multiplayer://player?source=local&trackUri=$trackUriString")
                    val intent = Intent(Intent.ACTION_VIEW, deepLinkUri)
                    startActivity(intent)
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
}
