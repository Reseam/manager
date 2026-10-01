package app.reseam.manager.platform

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.FileChannel
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.concurrent.thread
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Runs [app] only in the instance that holds [directory]'s lock; a later launch signals [app]'s activations and exits. */
fun runSingleInstance(directory: Path, app: (activations: Flow<Unit>) -> Unit) {
    val address = UnixDomainSocketAddress.of(directory.resolve("manager.sock"))
    FileChannel.open(directory.resolve("manager.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
        val lock = channel.tryLock()
        if (lock == null) {
            SocketChannel.open(address).close()
            return
        }
        lock.use {
            Files.deleteIfExists(address.path)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(address).use { server ->
                val activations = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                thread(isDaemon = true, name = "manager-activations") {
                    while (true) server.accept().use { activations.tryEmit(Unit) }
                }
                app(activations)
            }
        }
    }
}
