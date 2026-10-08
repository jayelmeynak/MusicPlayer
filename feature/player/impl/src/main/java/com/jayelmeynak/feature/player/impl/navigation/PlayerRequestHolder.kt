package com.jayelmeynak.feature.player.impl.navigation

import com.jayelmeynak.feature.player.api.PlayerOpener
import com.jayelmeynak.feature.player.api.TrackSource
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** A track asked for through [PlayerOpener] and not yet taken by the player screen. */
internal data class PlayerRequest(val source: TrackSource, val id: String)

/**
 * Keeps the latest [PlayerRequest] until the player screen takes it.
 *
 * A request lives until taken: if [PlayerOpener.open] is called but the player screen never
 * shows up (navigation did not happen), the stale request is played the next time the player
 * opens, e.g. from the mini player.
 *
 * Public only for the binding in `:di`; nothing outside the module can create it.
 */
@Singleton
public class PlayerRequestHolder @Inject internal constructor() : PlayerOpener {

    private val pending = AtomicReference<PlayerRequest?>(null)

    override fun open(source: TrackSource, id: String) {
        pending.set(PlayerRequest(source, id))
    }

    /** Returns the pending request and forgets it, so it is played once. */
    internal fun take(): PlayerRequest? = pending.getAndSet(null)
}
