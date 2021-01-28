package com.example.mediaproject

import android.util.Log
import com.example.surfacecodec.IServerLive
import com.example.surfacecodec.ReceiverMsg
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class SocketLive : IServerLive {

    private var cb: ReceiverMsg ? = null
    private var websocket : WebSocket? = null

    private val webSocketServer = object : WebSocketServer(InetSocketAddress(1400)) {
        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            Log.i("zzzzzzzzzzzzzzzzz", "onOpen: open socket ")
            websocket = conn;
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            Log.i("zzzzzzzzzzzzzzzzz", "onClose: 关闭 socket ")
        }

        override fun onMessage(conn: WebSocket?, message: String?) {

        }

        override fun onMessage(conn: WebSocket?, message: ByteBuffer?) {
//            super.onMessage(conn, message)
            if (message != null) {
                val data = ByteArray(message.remaining())
                message.get(data)
                cb?.invoke(data)
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            Log.i("zzzzzzzzzzzzzzzzz", "onError: 错误 socket ")
        }

        override fun onStart() {

        }

    }

    fun start() {
        webSocketServer.start()
    }

    override fun setReceiverCB(cb: ReceiverMsg) {
        this.cb = cb
    }

    override fun sendData(data: ByteArray) {
        if (websocket != null && websocket?.isOpen == true) {
            websocket?.send(data)
        }
    }

    fun close() {
        websocket?.close()
        webSocketServer.stop()
    }
}