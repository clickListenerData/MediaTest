package com.example.surfacecodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 数据 编码层  推流
 */
class EncodePushLive(
    private val serverLive: IServerLive,
    private val width: Int,
    private val height: Int
) {

    private val mediaCodec by lazy { MediaCodec.createEncoderByType("video/avc") }

    private lateinit var yuv : ByteArray
    private lateinit var nv12 : ByteArray

    val NV_I = 5
    val NV_VPS = 7

    private lateinit var vps_buf: ByteArray

    private var mFrameIndex = 0L

    fun startLive() {
        val me = MediaExtractor()
        me.setDataSource("")

//        me.setda
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height, width)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        )
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        format.setInteger(MediaFormat.KEY_BIT_RATE, width * height)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        yuv = ByteArray(width * height * 3 / 2)

        mediaCodec.start()
    }

    // 回调摄像头数据  进行编码 推流传输
    fun encodeFrame(data: ByteArray) {
        // 摄像头 数据处理
        nv12 = YUVUtils.nv21ToNV12(data, width, height)
        YUVUtils.portraitData2Raw(nv12, yuv, width, height)

        val inIndex = mediaCodec.dequeueInputBuffer(100_000)
        if (inIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inIndex)
            inputBuffer?.clear()
            inputBuffer?.put(yuv)
            val time = computePresentationTime(mFrameIndex)
            mediaCodec.queueInputBuffer(inIndex, 0, yuv.size, time, 0)
            mFrameIndex++
        }
        val info = MediaCodec.BufferInfo()
        var outIndex = mediaCodec.dequeueOutputBuffer(info, 100_000)
        while (outIndex >= 0) {
            val outputBuffer = mediaCodec.getOutputBuffer(outIndex)
            if (outputBuffer != null) dealFrame(outputBuffer, info)
            mediaCodec.releaseOutputBuffer(outIndex, false)
            outIndex = mediaCodec.dequeueOutputBuffer(info, 0)
        }
    }

    private fun computePresentationTime(frameIndex: Long): Long {
        return 132 + frameIndex * 1000000 / 15
    }

    private fun dealFrame(byteBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        var offset = 4
        if (byteBuffer[2] == 0x01.toByte()) {
            offset = 3
        }

        when((byteBuffer[offset].toInt() and 0x1F)) {  // & 0x7E shr 1  I:19  VPS:32
            NV_VPS -> {
                vps_buf = ByteArray(info.size)
                byteBuffer.get(vps_buf)
            }
            NV_I -> {
                val data = ByteArray(info.size)
                byteBuffer.get(data)
                serverLive.sendData(vps_buf + data)
            }
            else -> {
                val data = ByteArray(info.size)
                byteBuffer.get(data)
                serverLive.sendData(data)
            }
        }
    }
}