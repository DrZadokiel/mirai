package net.mamoe.mirai.network.protocol.tim.handler

import net.mamoe.mirai.network.LoginSession
import net.mamoe.mirai.network.protocol.tim.packet.ClientPacket
import net.mamoe.mirai.network.protocol.tim.packet.ServerPacket
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * 临时数据包处理器
 * ```kotlin
 * session.addHandler<ClientTouchResponsePacket>{
 *   toSend { ClientTouchPacket() }
 *   onExpect {//it: ClientTouchResponsePacket
 *      //do sth.
 *   }
 * }
 * ```
 *
 * @see LoginSession.expectPacket
 */
open class TemporaryPacketHandler<P : ServerPacket>(
        private val expectationClass: KClass<P>,
        private val future: CompletableFuture<Unit>,
        private val fromSession: LoginSession
) {
    private lateinit var toSend: ClientPacket

    private lateinit var expect: suspend (P) -> Unit


    lateinit var session: LoginSession//无需覆盖

    fun toSend(packet: () -> ClientPacket) {
        this.toSend = packet()
    }

    fun toSend(packet: ClientPacket) {
        this.toSend = packet
    }


    fun onExpect(handler: suspend (P) -> Unit) {
        this.expect = handler
    }

    suspend fun send(session: LoginSession) {
        this.session = session
        session.socket.sendPacket(toSend)
    }

    suspend fun onPacketReceived(session: LoginSession, packet: ServerPacket): Boolean {
        if (expectationClass.isInstance(packet) && session === this.fromSession) {
            @Suppress("UNCHECKED_CAST")
            expect(packet as P)
            future.complete(Unit)
            return true
        }
        return false
    }
}