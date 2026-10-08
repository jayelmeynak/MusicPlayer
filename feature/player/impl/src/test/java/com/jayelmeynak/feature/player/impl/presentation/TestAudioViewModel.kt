package com.jayelmeynak.feature.player.impl.presentation

import com.jayelmeynak.feature.player.impl.domain.usecase.GetLocalTrackListUseCase
import com.jayelmeynak.feature.player.impl.domain.usecase.GetRemoteAlbumUseCase
import com.jayelmeynak.feature.player.impl.domain.usecase.GetRemoteTrackUseCase
import com.jayelmeynak.feature.player.impl.navigation.PlayerRequestHolder
import com.jayelmeynak.feature.player.impl.service.MusicServiceHandler
import com.jayelmeynak.lib.mediastore.domain.usecase.GetLocalTracksUseCase
import com.jayelmeynak.lib.mediastore.domain.usecase.GetTrackArtworkUseCase

/** The real AudioViewModel over fake repositories, for Compose tests of the player UI. */
internal fun testAudioViewModel(
    handler: MusicServiceHandler,
    requests: PlayerRequestHolder = PlayerRequestHolder(),
    remoteRepository: FakeMusicRemoteRepository = FakeMusicRemoteRepository(),
    localRepository: FakeLocalTracksRepository = FakeLocalTracksRepository(),
) = AudioViewModel(
    audioServiceHandler = handler,
    getLocalTrackListUseCase = GetLocalTrackListUseCase(GetLocalTracksUseCase(localRepository)),
    getRemoteTrackUseCase = GetRemoteTrackUseCase(remoteRepository),
    getRemoteAlbumUseCase = GetRemoteAlbumUseCase(remoteRepository),
    getTrackArtworkUseCase = GetTrackArtworkUseCase(localRepository),
    playerRequests = requests,
)
