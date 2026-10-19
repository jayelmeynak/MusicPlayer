package com.jayelmeynak.lib.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Moves between screens by [NavKey]. Implemented by `app`; features get it through [EntryInstaller].
 */
public interface Navigator {

    /**
     * Ключ корня вкладки — переключает на эту вкладку (стек не меняется); любой другой ключ кладёт
     * наверх стека текущей вкладки, убирая его из остальных мест.
     *
     * Сбросить стек вкладки до корня может только оболочка (тап по вкладке): такой операции у фич нет.
     */
    public fun navigateTo(destination: NavKey)

    /** Pops the current back stack. */
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
