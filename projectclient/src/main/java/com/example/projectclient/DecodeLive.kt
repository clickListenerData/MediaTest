package com.example.projectclient

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface

class DecodeLive(private val surface: Surface) {

    private val mediaCodec by lazy { MediaCodec.createDecoderByType("video/hevc") }

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC,720,1280)
        mediaCodec.configure(format,surface,null,0)
    }

    fun startLive() {
        mediaCodec.start()
//        SocketLive().start()
    }

    fun decodeFrame(it: ByteArray) {
        val info = MediaCodec.BufferInfo()
        val inIndex = mediaCodec.dequeueInputBuffer(100_000)
        if (inIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inIndex)
            inputBuffer?.clear()
            inputBuffer?.put(it,0,it.size)
            mediaCodec.queueInputBuffer(inIndex,0,it.size,System.currentTimeMillis(),0)
        }
        var outIndex = mediaCodec.dequeueOutputBuffer(info,100_000)
        while (outIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outIndex,true)
            outIndex = mediaCodec.dequeueOutputBuffer(info,0)
        }
    }
}