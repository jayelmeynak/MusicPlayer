package com.jayelmeynak.musicplayer.presentation

import android.annotation.SuppressLint
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
import androidx.compose.material3.Scaffold
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import com.jayelmeynak.musicplayer.R
import com.jayelmeynak.musicplayer.presentation.navigation.BottomNavigationBar
import com.jayelmeynak.musicplayer.presentation.navigation.Navigation
import com.jayelmeynak.player.presentation.PlayerActivity
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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        setContent {
            AppTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(navController)
                    }
                ) {
                    Navigation(

                        navController = navController
                    ) {
                        handleTrackClick(it)
                    }
                }
            }
        }
    }

    private fun handleTrackClick(deepLinkUri: Uri) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            val source = deepLinkUri.getQueryParameter("source")
            source?.let {
                if(it == "local") {
                    val trackUriStringEncoded = deepLinkUri.getQueryParameter("trackUri")
                    val trackUriStringDecoded = trackUriStringEncoded?.let { Uri.decode(it) }
                    putExtra("source", source)
                    putExtra("trackUri", trackUriStringDecoded)
                }

                if(it == "api") {
                    val trackIdString = deepLinkUri.getQueryParameter("trackId")
                    putExtra("source", source)
                    putExtra("trackId", trackIdString)
                }
            }
        }
        startActivity(intent)
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
