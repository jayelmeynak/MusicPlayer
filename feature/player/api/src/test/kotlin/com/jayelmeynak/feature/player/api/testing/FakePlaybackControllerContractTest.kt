package com.jayelmeynak.feature.player.api.testing

/** Общий контракт контроллера на фейке: тесты экранов опираются именно на это поведение. */
class FakePlaybackControllerContractTest : PlaybackControllerContract() {

    private val fake = FakePlaybackController(connected = false)

    override val controller: FakePlaybackController get() = fake

    override fun connect() = fake.connect()

    override fun disconnect() = fake.disconnect()
}
