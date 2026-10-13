package app.reseam.manager.platform

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
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

/** Runs [app] once per user; a later launch hands its [arguments] to the running one as an activation and exits. */
fun runSingleInstance(directory: Path, arguments: List<String>, app: (activations: Flow<List<String>>) -> Unit) {
    val address = UnixDomainSocketAddress.of(directory.resolve("manager.sock"))
    FileChannel.open(directory.resolve("manager.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
        val lock = channel.tryLock()
        if (lock == null) {
            SocketChannel.open(address).use { it.write(ByteBuffer.wrap(arguments.joinToString("\n").toByteArray())) }
            return
        }
        lock.use {
            Files.deleteIfExists(address.path)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(address).use { server ->
                val activations = MutableSharedFlow<List<String>>(replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
                activations.tryEmit(arguments)
                thread(isDaemon = true, name = "manager-activations") {
                    while (true) server.accept().use { client -> activations.tryEmit(Channels.newInputStream(client).bufferedReader().readLines()) }
                }
                app(activations)
            }
        }
    }
}
