package com.jayelmeynak.musicplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.TaskStackBuilder
import com.example.compose.AppTheme
import com.jayelmeynak.player.presentation.PlayerActivity
import com.jayelmeynak.search_tracks.presentation.ChartTracksScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                ChartTracksScreen(
                    onTrackClicked = { trackId ->
                        val uri = Uri.parse("multiplayer://player?source=api&trackId=$trackId")
                        val parentIntent = Intent("com.jayelmeynak.musicplayer.action.HOME").apply {
                            addCategory(Intent.CATEGORY_DEFAULT)
                        }
                        val playerIntent = Intent(this, PlayerActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            data = uri
                        }

                        TaskStackBuilder.create(this)
                            .addNextIntent(parentIntent)
                            .addNextIntent(playerIntent)
                            .startActivities()
                    }
                )
            }
        }
    }
}
