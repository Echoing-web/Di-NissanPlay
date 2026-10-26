package com.shilapi.xcertplay.bluetooth

import com.shilapi.xcertplay.transport.BlockingDuplexByteStream
import java.io.IOException

/**
 * 厂商 socket 到 [BlockingDuplexByteStream] 的适配（SDD：字节流等价，上层
 * iAP2 无感知）。有界 FIFO 保持部分读顺序；故障在 send 与 recv 间传播；
 * close 断开自己占用的厂商 socket。
 */
class HsaeRfcommStream(
    private val channel: AnwVendorChannel,
    private val connectionIndex: Int,
) : BlockingDuplexByteStream {

    private val lock = Object()
    private val pending = ArrayDeque<ByteArray>()
    @Volatile private var closed = false
    @Volatile private var peerEnded = false
    @Volatile private var failure: IOException? = null

    init {
        channel.setListener(
            connectionIndex,
            object : AnwVendorChannel.Listener {
                override fun onData(data: ByteArray) {
                    synchronized(lock) {
                        if (!closed && !peerEnded) {
                            pending.addLast(data)
                            lock.notifyAll()
                        }
                    }
                }

                override fun onDisconnected() {
                    synchronized(lock) {
                        peerEnded = true
                        lock.notifyAll()
                    }
                }
            },
        )
    }

    fun onVendorWriteError(error: IOException) {
        synchronized(lock) {
            failure = error
            lock.notifyAll()
        }
    }

    override fun send(data: ByteArray) {
        synchronized(lock) {
            failure?.let { throw it }
            if (closed) throw IOException("AnwPhoneLink stream is closed")
        }
        var offset = 0
        while (offset < data.size) {
            val chunk = data.copyOfRange(offset, minOf(offset + WRITE_CHUNK_BYTES, data.size))
            val written = channel.write(connectionIndex, chunk)
            if (written < 0) {
                val error = IOException("AnwPhoneLink vendor write failed: ret=$written")
                onVendorWriteError(error)
                throw error
            }
            offset += if (written > 0) written else chunk.size
        }
    }

    override fun recv(maxBytes: Int, timeoutMillis: Long): ByteArray? {
        require(maxBytes > 0) { "maxBytes must be positive" }
        require(timeoutMillis >= 0) { "timeoutMillis must not be negative" }
        val deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L
        synchronized(lock) {
            while (true) {
                takePendingLocked(maxBytes)?.let { return it }
                failure?.let { throw it }
                if (closed) return ByteArray(0)
                if (peerEnded) return ByteArray(0)
                val remaining = deadlineNanos - System.nanoTime()
                if (remaining <= 0) return null
                try {
                    val millis = remaining / 1_000_000L
                    lock.wait(millis, (remaining % 1_000_000L).toInt())
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                }
            }
        }
    }

    private fun takePendingLocked(maxBytes: Int): ByteArray? {
        val first = pending.firstOrNull() ?: return null
        return if (first.size <= maxBytes) {
            pending.removeFirst()
            first
        } else {
            val chunk = first.copyOf(maxBytes)
            pending.removeFirst()
            pending.addFirst(first.copyOfRange(maxBytes, first.size))
            chunk
        }
    }

    override fun close() {
        val alreadyClosed = synchronized(lock) {
            if (closed) return
            closed = true
            lock.notifyAll()
            true
        }
        if (alreadyClosed) {
            channel.setListener(connectionIndex, null)
            runCatching { channel.closeSocket(connectionIndex) }
        }
    }

    private companion object {
        const val WRITE_CHUNK_BYTES = 4096
    }
}
