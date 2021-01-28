package com.example.projectclient

import android.util.Log
import com.example.surfacecodec.IServerLive
import com.example.surfacecodec.ReceiverMsg
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer

class SocketLive() : IServerLive  {

    private lateinit var client: WebSocketClient
    private lateinit var cb: ReceiverMsg

    fun start() {
        val uri = URI("ws://192.168.3.16:1400")
        client = CustomClient(uri)
        client.connect()
    }


    inner class CustomClient(uri: URI) : WebSocketClient(uri) {
        override fun onOpen(handshakedata: ServerHandshake?) {
            Log.i("zzzzzzzzzzzzzzzz","on open")
        }

        override fun onMessage(message: String?) {

        }

        override fun onMessage(bytes: ByteBuffer?) {
//            super.onMessage(bytes)
            Log.i("zzzzzzzzzzzz","read socket:: ${bytes?.remaining()}")
            if (bytes != null) {
                val data = ByteArray(bytes.remaining())
                bytes.get(data)
                cb(data)
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            Log.i("zzzzzzzzzzzzzzzz","on close  $reason  $remote  $code")
        }

        override fun onError(ex: Exception?) {
            Log.i("zzzzzzzzzzzzzzzz","on error")
        }
    }

    override fun setReceiverCB(cb: ReceiverMsg) {
        this.cb = cb
    }

    override fun sendData(data: ByteArray) {
        client.send(data)
    }

    fun close() {
        client.close()
    }
}