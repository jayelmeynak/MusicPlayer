package com.jayelmeynak.musicplayer.screen

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor as anyAncestor
import androidx.compose.ui.test.hasTestTag as testTag
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import com.jayelmeynak.musicplayer.presentation.navigation.MINI_PLAYER_TAG

// Матчеры Compose под другими именами: внутри onNode { } одноимённые методы ViewBuilder их перекрывают.

/** Навигация по вкладкам «Remote» и «Local» (нижняя панель или боковая на широком экране). */
class NavigationBarScreen(provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<NavigationBarScreen>(provider) {

    // Пункт меню с подписью: описание иконки Material3 убирает из семантики, остаётся текст подписи.
    val remoteTab: KNode = onNode {
        hasText(REMOTE)
        isSelectable()
    }

    val localTab: KNode = onNode {
        hasText(LOCAL)
        isSelectable()
    }

    private companion object {
        const val REMOTE = "Remote"
        const val LOCAL = "Local"
    }
}

/** Список треков вкладки: строка трека по заголовку и поле поиска. */
class TrackListScreen(provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<TrackListScreen>(provider) {

    val searchField: KNode = onNode { hasSetTextAction() }

    /** Строка трека в списке; заголовок того же трека в мини-плеере не совпадает. */
    fun track(title: String): KNode = onNode {
        hasText(title)
        hasClickAction()
        addSemanticsMatcher(!testTag(MINI_PLAYER_TAG))
        addSemanticsMatcher(!anyAncestor(testTag(MINI_PLAYER_TAG)))
    }
}

/** Мини-плеер над вкладками. */
class MiniPlayerScreen(provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MiniPlayerScreen>(provider) {

    /** Весь мини-плеер (кликабелен, содержит заголовок и исполнителя). */
    val root: KNode = onNode { hasTestTag(MINI_PLAYER_TAG) }

    val pauseButton: KNode = onNode {
        hasContentDescription(PAUSE)
        addSemanticsMatcher(anyAncestor(testTag(MINI_PLAYER_TAG)))
    }

    val playButton: KNode = onNode {
        hasContentDescription(PLAY)
        addSemanticsMatcher(anyAncestor(testTag(MINI_PLAYER_TAG)))
    }
}

/** Экран плеера (настоящий, из `feature:player:impl`). */
class PlayerScreen(provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PlayerScreen>(provider) {

    val nextButton: KNode = onNode { hasContentDescription("Следующий трек") }

    val previousButton: KNode = onNode { hasContentDescription("Предыдущий трек") }

    /** Кнопка в состоянии «играет»: нажатие ставит паузу. */
    val pauseButton: KNode = onNode {
        hasContentDescription(PAUSE)
        addSemanticsMatcher(!anyAncestor(testTag(MINI_PLAYER_TAG)))
    }

    /** Кнопка в состоянии «пауза»: нажатие продолжает воспроизведение. */
    val playButton: KNode = onNode {
        hasContentDescription(PLAY)
        addSemanticsMatcher(!anyAncestor(testTag(MINI_PLAYER_TAG)))
    }

    fun title(title: String): KNode = onNode { hasText(title) }
}

private const val PAUSE = "Пауза"
private const val PLAY = "Воспроизведение"
