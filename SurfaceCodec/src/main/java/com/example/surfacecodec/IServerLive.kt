package com.example.surfacecodec

typealias ReceiverMsg = (data: ByteArray) -> Unit

interface IServerLive {

    fun sendData(data: ByteArray)

    fun setReceiverCB(cb: ReceiverMsg)
}