package com.jayelmeynak.feature.player.impl.presentation

internal sealed interface PlayerEffect {
    /**
     * Играть нечего: подключённая сессия закончила восстановление, а очередь пуста (например,
     * экран восстановлен из стека, а сохранённой очереди нет). Экран убирает свой ключ из стека.
     */
    data object Close : PlayerEffect
}
