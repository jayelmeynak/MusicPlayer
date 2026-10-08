package com.jayelmeynak.player.service

/**
 * MediaMetadata extras key for the full track length in ms. Not durationMs: for Deezer the media is
 * a 30-second preview, and Media3 shows durationMs in system controls until the player knows better.
 */
internal const val EXTRA_TRACK_DURATION_MS = "com.jayelmeynak.player.TRACK_DURATION_MS"
