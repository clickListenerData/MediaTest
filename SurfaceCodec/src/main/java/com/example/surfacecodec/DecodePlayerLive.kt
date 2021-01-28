package com.example.surfacecodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface

/**
 *  解码 播放
 */
class DecodePlayerLive {

    private val mediaCodec by lazy { MediaCodec.createDecoderByType("video/avc") }

    fun startLive(width: Int,height: Int,surface: Surface) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,width,height)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        format.setInteger(MediaFormat.KEY_BIT_RATE,width * height)
        format.setInteger(MediaFormat.KEY_FRAME_RATE,15)
        mediaCodec.configure(format,surface,null,0)

        mediaCodec.start()
    }

    fun decodeFrame(data: ByteArray) {
        val inIndex = mediaCodec.dequeueInputBuffer(100_000)
        if (inIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inIndex)
            inputBuffer?.clear()
            inputBuffer?.put(data)
            mediaCodec.queueInputBuffer(inIndex,0,data.size,System.currentTimeMillis(),0)
        }
        val info = MediaCodec.BufferInfo()
        var outIndex = mediaCodec.dequeueOutputBuffer(info, 100_000)
        while (outIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outIndex,true)
            outIndex = mediaCodec.dequeueOutputBuffer(info,0)
        }
    }

}