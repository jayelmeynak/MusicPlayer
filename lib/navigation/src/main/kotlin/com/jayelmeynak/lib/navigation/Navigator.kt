package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Переходы между экранами по [NavKey]. Реализует `app`; фичи получают его через [EntryInstaller].
 */
public interface Navigator {

    /**
     * Ключ корня вкладки — переключает на эту вкладку (стек не меняется); любой другой ключ кладёт
     * наверх стека текущей вкладки, убирая его из остальных мест.
     *
     * Сбросить стек вкладки до корня может только оболочка (тап по вкладке): такой операции у фич нет.
     */
    public fun navigateTo(destination: NavKey)

    /** Снимает верх текущего back stack. */
    public fun goBack()

    /**
     * Убирает [destination] из всех стеков, кроме корня вкладки; нет ключа — ничего не делает.
     * Текущая вкладка не меняется.
     *
     * Для экрана, который закрывает сам себя (например, плеер без очереди): [goBack] закрыл бы
     * экран, открытый поверх него.
     */
    public fun remove(destination: NavKey)
}
