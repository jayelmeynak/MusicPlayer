package com.jayelmeynak.feature.catalog.impl.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri

/**
 * Открывает [link] в браузере или приложении, которое умеет такие ссылки (Deezer). `false` — открыть
 * не удалось: нет подходящего приложения или система запретила запуск; это не ошибка приложения.
 */
internal fun openExternalLink(context: Context, link: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, link.toUri()).addCategory(Intent.CATEGORY_BROWSABLE)
    // Вне Activity запуск без новой задачи невозможен; из Activity ссылка открывается поверх неё.
    if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (refused: RuntimeException) {
        // ActivityNotFoundException — открыть нечем, SecurityException — запуск запрещён.
        false
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext?.findActivity()
    else -> null
}
