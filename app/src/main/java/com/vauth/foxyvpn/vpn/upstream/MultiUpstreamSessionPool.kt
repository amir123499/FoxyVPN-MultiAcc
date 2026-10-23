package com.vauth.foxyvpn.vpn.upstream

import com.vauth.foxyvpn.data.model.AccountLiveStats
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val STATS_TRACKER_INTERVAL_MS = 2_000L

/**
 * Multi-account session orchestrator (multi-account patch).
 *
 * Holds one [UpstreamSession] per account and multiplexes new streams across
 * them with round-robin. Implements [UpstreamSession] so the rest of the VPN
 * service (watchdog, redial, teardown) keeps working unchanged.
 *
 * Byte counters are gathered by wrapping every opened stream; speeds are
 * derived by the stats tracker.
 */
class MultiUpstreamSessionPool : UpstreamSession {

    private data class AccountEntry(
        val accountId: String,
        val email: String,
        val session: UpstreamSession,
        @Volatile var isExhausted: Boolean = false,
        @Volatile var passExpiresAtEpochSeconds: Long? = null,
        val bytesDown: AtomicLong = AtomicLong(0L),
        val bytesUp: AtomicLong = AtomicLong(0L),
        val activeStreams: AtomicInteger = AtomicInteger(0),
        @Volatile var speedDownBytesPerSec: Long = 0L,
        @Volatile var speedUpBytesPerSec: Long = 0L,
    )

    private val entries = CopyOnWriteArrayList<AccountEntry>()
    private val roundRobin = AtomicInteger(0)

    private val _liveStats = MutableStateFlow<List<AccountLiveStats>>(emptyList())
    val liveStats: StateFlow<List<AccountLiveStats>> = _liveStats

    val accountCount: Int get() = entries.size

    fun addAccountSession(accountId: String, email: String, session: UpstreamSession) {
        entries.removeAll { it.accountId == accountId }
        entries.add(AccountEntry(accountId = accountId, email = email, session = session))
        publishStatsSnapshot()
    }

    fun removeAccountSession(accountId: String) {
        entries.filter { it.accountId == accountId }.forEach { runCatching { it.session.close() } }
        entries.removeAll { it.accountId == accountId }
        publishStatsSnapshot()
    }

    fun markExhausted(accountId: String) {
        entries.find { it.accountId == accountId }?.isExhausted = true
        publishStatsSnapshot()
    }

    fun setPassExpiry(accountId: String, expiresAtEpochSeconds: Long?) {
        entries.find { it.accountId == accountId }?.passExpiresAtEpochSeconds = expiresAtEpochSeconds
    }

    fun minPassExpiry(): Long? = entries.mapNotNull { it.passExpiresAtEpochSeconds }.minOrNull()

    fun updateAccountToken(accountId: String, token: String) {
        entries.find { it.accountId == accountId }?.session?.updateBearerToken(token)
    }

    override suspend fun connect() {
        // Sessions are dialed before being added; nothing to do here.
    }

    override suspend fun openStream(targetHost: String, targetPort: Int): TunneledStream {
        val candidates = entries.filter { it.session.isConnected && !it.isExhausted }
        if (candidates.isEmpty()) throw IOException("No connected upstream account available")
        val start = roundRobin.getAndIncrement()
        var lastError: Throwable? = null
        for (i in candidates.indices) {
            val entry = candidates[(start + i).mod(candidates.size)]
            try {
                val stream = entry.session.openStream(targetHost, targetPort)
                entry.activeStreams.incrementAndGet()
                var closed = false
                fun onClose() {
                    if (!closed) {
                        closed = true
                        entry.activeStreams.decrementAndGet()
                    }
                }
                return TunneledStream(
                    input = CountingInputStream(stream.input) { n -> entry.bytesDown.addAndGet(n) },
                    output = CountingOutputStream(stream.output) { n -> entry.bytesUp.addAndGet(n) },
                    close = {
                        try {
                            stream.close()
                        } finally {
                            onClose()
                        }
                    },
                )
            } catch (failure: Throwable) {
                lastError = failure
            }
        }
        throw lastError ?: IOException("No connected upstream account available")
    }

    override fun disableNewStreamsAndCloseWhenIdle() {
        entries.forEach { runCatching { it.session.disableNewStreamsAndCloseWhenIdle() } }
    }

    override fun updateBearerToken(token: String) {
        entries.forEach { runCatching { it.session.updateBearerToken(token) } }
    }

    override val isConnected: Boolean
        get() = entries.any { it.session.isConnected && !it.isExhausted }

    override fun close() {
        entries.forEach { runCatching { it.session.close() } }
        entries.clear()
        _liveStats.value = emptyList()
    }

    fun startStatsTracker(scope: CoroutineScope): Job = scope.launch {
        var lastBytes = snapshotBytes()
        var lastAt = System.currentTimeMillis()
        while (isActive) {
            delay(STATS_TRACKER_INTERVAL_MS)
            val now = System.currentTimeMillis()
            val elapsedSeconds = (now - lastAt).coerceAtLeast(1L).toDouble() / 1_000.0
            _liveStats.value = entries.map { entry ->
                val (prevDown, prevUp) = lastBytes[entry.accountId] ?: (0L to 0L)
                val down = entry.bytesDown.get()
                val up = entry.bytesUp.get()
                entry.speedDownBytesPerSec = ((down - prevDown).coerceAtLeast(0L) / elapsedSeconds).toLong()
                entry.speedUpBytesPerSec = ((up - prevUp).coerceAtLeast(0L) / elapsedSeconds).toLong()
                AccountLiveStats(
                    accountId = entry.accountId,
                    email = entry.email,
                    isConnected = entry.session.isConnected,
                    isExhausted = entry.isExhausted,
                    activeStreams = entry.activeStreams.get(),
                    bytesDown = down,
                    bytesUp = up,
                    speedDownBytesPerSec = entry.speedDownBytesPerSec,
                    speedUpBytesPerSec = entry.speedUpBytesPerSec,
                )
            }
            lastBytes = snapshotBytes()
            lastAt = now
        }
    }

    private fun snapshotBytes(): Map<String, Pair<Long, Long>> =
        entries.associate { it.accountId to (it.bytesDown.get() to it.bytesUp.get()) }

    private fun publishStatsSnapshot() {
        _liveStats.value = entries.map { entry ->
            AccountLiveStats(
                accountId = entry.accountId,
                email = entry.email,
                isConnected = entry.session.isConnected,
                isExhausted = entry.isExhausted,
                activeStreams = entry.activeStreams.get(),
                bytesDown = entry.bytesDown.get(),
                bytesUp = entry.bytesUp.get(),
                speedDownBytesPerSec = entry.speedDownBytesPerSec,
                speedUpBytesPerSec = entry.speedUpBytesPerSec,
            )
        }
    }

    private class CountingInputStream(
        private val delegate: InputStream,
        private val onBytes: (Long) -> Unit,
    ) : InputStream() {
        override fun read(): Int = delegate.read().also { if (it >= 0) onBytes(1L) }
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            delegate.read(b, off, len).also { if (it > 0) onBytes(it.toLong()) }
        override fun available(): Int = delegate.available()
        override fun skip(n: Long): Long = delegate.skip(n)
        override fun close() = delegate.close()
    }

    private class CountingOutputStream(
        private val delegate: OutputStream,
        private val onBytes: (Long) -> Unit,
    ) : OutputStream() {
        override fun write(b: Int) {
            delegate.write(b)
            onBytes(1L)
        }
        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            onBytes(len.toLong())
        }
        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }
}
