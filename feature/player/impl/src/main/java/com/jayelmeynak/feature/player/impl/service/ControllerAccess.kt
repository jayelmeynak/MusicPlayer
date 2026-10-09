package com.jayelmeynak.feature.player.impl.service

import android.os.Process
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession

/**
 * Whether a controller may connect to the playback session.
 *
 * The session queue is the visible list of a screen, up to the user's whole local library (titles,
 * artists, `content://` ids), so it is readable only by controllers the user or the system trusts.
 *
 * Access is binary on purpose. Any accepted connection, even one with a reduced command set, hands
 * the controller the platform session token, and the platform session publishes the current
 * metadata and the queue to every holder of that token, bypassing the command set. A
 * "transport only" level would leak the queue again. Playback resumption and media buttons
 * (a later stage) must come through the trusted system path, not through a new access level.
 */
internal enum class ControllerAccess {
    /** Every command, including reading the queue and its metadata. */
    FULL,

    /** The connection is rejected: no commands, nothing to read. */
    NONE,
}

/**
 * The access policy of the playback session:
 * - this app (same uid): the UI controller and the Media3 notification controller;
 * - controllers trusted by the system ([MediaSession.ControllerInfo.isTrusted]): System UI, the
 *   system media key dispatcher, Bluetooth, apps holding `MEDIA_CONTENT_CONTROL` or with an enabled
 *   notification listener. Media3 checks this by the caller's uid and pid. A Media3 controller of
 *   another uid must also have its package name verified against that uid; platform (legacy)
 *   controllers cannot always be verified and are judged by trust alone;
 * - everyone else, platform controllers included, gets [ControllerAccess.NONE]. Headset and
 *   Bluetooth keys and the notification shade reach the session through the notification
 *   controller or the system, which are covered above; an untrusted app can get the platform
 *   session token only by browsing this service, and that path asks this policy too. Third-party
 *   media key remappers that dispatch keys as themselves (not as the system) are rejected too.
 *
 * Reads only fields of [MediaSession.ControllerInfo]: for platform controllers `onConnect` may block
 * the main thread.
 */
@OptIn(UnstableApi::class)
internal fun MediaSession.ControllerInfo.access(ownUid: Int = Process.myUid()): ControllerAccess = when {
    uid == ownUid -> ControllerAccess.FULL
    !isTrusted -> ControllerAccess.NONE
    controllerVersion == MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION -> ControllerAccess.FULL
    isPackageNameVerified -> ControllerAccess.FULL
    else -> ControllerAccess.NONE
}
