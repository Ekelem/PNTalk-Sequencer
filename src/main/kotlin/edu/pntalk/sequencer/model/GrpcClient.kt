/**
 * @author Erik Kelemen <xkelem01@stud.fit.vutbr.cz>
 */

package edu.pntalk.sequencer.model

import io.grpc.ManagedChannel
import kotlinx.coroutines.Deferred
import translator.TranslatorGrpcKt.TranslatorCoroutineStub
import virtualmachine.SimulatorGrpcKt.SimulatorCoroutineStub
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import translator.Translate
import virtualmachine.Simulate
import java.io.Closeable
import java.util.concurrent.TimeUnit

/**
 * Grpc client participating on communication with gRPC protocol.
 *
 * Uses gRPC protocol
 *
 * @param channel channel
 * @constructor Creates client ready for communication on selected [channel].
 */
class GrpcClient constructor(
        private val channel: ManagedChannel
) : Closeable {
    private val stub: SimulatorCoroutineStub = SimulatorCoroutineStub(channel)

    /**
     * Communicate with simulator component.
     * @param code complete PNTalk code.
     * @param steps number of steps.
     */
    suspend fun simulate(code: String, steps: Long): Simulate.SimulateReply = runBlocking {
        val request = Simulate.SimulateRequest.newBuilder().setCode(code).setSteps(steps).build()
        return@runBlocking async { stub.simulate(request) }
    }.await()

    /**
     * Close communication.
     */
    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
