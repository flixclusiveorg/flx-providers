package com.flixclusive.provider.app.discord.core.presence

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PresenceRateLimiter(
    private val maxUpdates: Int = MAX_UPDATES,
    private val windowMs: Long = WINDOW_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private val sentAt = ArrayDeque<Long>()

    suspend fun acquire() {
        mutex.withLock {
            while (true) {
                val current = now()
                while (sentAt.isNotEmpty() && current - sentAt.first() >= windowMs) {
                    sentAt.removeFirst()
                }

                if (sentAt.size < maxUpdates) {
                    sentAt.addLast(current)
                    return
                }

                delay(windowMs - (current - sentAt.first()))
            }
        }
    }

    fun reset() = sentAt.clear()

    private companion object {
        const val MAX_UPDATES = 5
        const val WINDOW_MS = 20_000L
    }
}
