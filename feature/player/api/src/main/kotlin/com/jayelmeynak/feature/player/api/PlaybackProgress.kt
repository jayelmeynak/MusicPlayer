package com.jayelmeynak.feature.player.api

private const val PERCENT = 100f

/**
 * Позиция [positionMs] в процентах длительности [durationMs] (0..100) — прогресс экрана плеера и
 * мини-плеера. `0`, если длительность неизвестна (`durationMs <= 0`).
 */
public fun progressPercent(positionMs: Long, durationMs: Long): Float =
    if (durationMs > 0) (positionMs.toFloat() / durationMs * PERCENT).coerceIn(0f, PERCENT) else 0f

/**
 * Позиция в миллисекундах для доли [percent] (0..100) длительности [durationMs] — перемотка
 * ползунком. `null`, если длительность неизвестна: перематывать не на что.
 */
public fun positionForPercent(percent: Float, durationMs: Long): Long? =
    if (durationMs > 0) (durationMs * percent / PERCENT).toLong() else null
