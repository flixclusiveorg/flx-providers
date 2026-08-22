package com.flixclusive.provider.app.discord.core.presence

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import kotlin.system.measureTimeMillis

class PresenceRateLimiterTest {
    @Test
    fun `lets the first burst through without waiting`() {
        var clock = 0L
        val limiter = PresenceRateLimiter(maxUpdates = 3, windowMs = 1_000, now = { clock })

        val elapsed = measureTimeMillis { runBlocking { repeat(3) { limiter.acquire() } } }

        expectThat(elapsed).isLessThan(100L)
    }

    @Test
    fun `admits another update once the window has rolled over`() {
        var clock = 0L
        val limiter = PresenceRateLimiter(maxUpdates = 2, windowMs = 1_000, now = { clock })

        runBlocking {
            limiter.acquire()
            limiter.acquire()
        }

        clock = 1_001

        val elapsed = measureTimeMillis { runBlocking { limiter.acquire() } }

        expectThat(elapsed).isLessThan(100L)
    }

    @Test
    fun `throttles once the window is full`() {
        val limiter = PresenceRateLimiter(maxUpdates = 1, windowMs = 200)

        val elapsed =
            measureTimeMillis {
                runBlocking {
                    limiter.acquire()
                    limiter.acquire()
                }
            }

        expectThat(elapsed).isGreaterThanOrEqualTo(150L)
    }

    @Test
    fun `reset clears the window`() {
        val limiter = PresenceRateLimiter(maxUpdates = 1, windowMs = 5_000)

        runBlocking { limiter.acquire() }
        limiter.reset()

        val elapsed = measureTimeMillis { runBlocking { limiter.acquire() } }

        expectThat(elapsed).isLessThan(100L)
    }
}
