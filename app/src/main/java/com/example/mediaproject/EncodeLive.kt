package com.example.mediaproject

import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Bundle
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class EncodeLive(private val projection: MediaProjection,private val file: File) : Thread() {

    private val width = 720
    private val height = 1280

    private val mediaCodec by lazy { MediaCodec.createEncoderByType("video/hevc") }
    private val serverSocket by lazy { SocketLive() }

    private var pps_sps_data : ByteArray? = null


    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC,width,height)
        format.setInteger(MediaFormat.KEY_BIT_RATE,width*height)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        format.setInteger(MediaFormat.KEY_FRAME_RATE,20)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    fun startLive() {
        serverSocket.start()
        val surface = mediaCodec.createInputSurface()
        projection.createVirtualDisplay("-test",width,height,1,DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,surface,null,null)
        start()
    }

    override fun run() {
        mediaCodec.start()
//        val fos = FileOutputStream(file)
        val info = MediaCodec.BufferInfo()
        while (true) {
            /*mediaCodec.setParameters(Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME,0)  // 手动触发I帧
            })*/
            var dequeueOutputBuffer = mediaCodec.dequeueOutputBuffer(info, 100_000)
            if (dequeueOutputBuffer >= 0) {
                val putBuffer = mediaCodec.getOutputBuffer(dequeueOutputBuffer)
                if (putBuffer != null) dealFrame(putBuffer,info.size)
                mediaCodec.releaseOutputBuffer(dequeueOutputBuffer,false)
//                dequeueOutputBuffer = mediaCodec.dequeueOutputBuffer(info,0)
            }
        }
    }

    private fun dealFrame(buffer: ByteBuffer,size: Int) {
        var offset = 4
        if (buffer.get(2) == 0x01.toByte()) {
            offset = 3
        }
        val type = (buffer.get(offset).toInt() and 0x7E) shr 1
        Log.i("zzzzzzzzzzzzz", "$type")
        when(type) {  // hevc  & 0x7e >> 1   // 32 19  // H264 & 0x1f  7 5
            32 -> {  // sps pps
                pps_sps_data = ByteArray(size)
                buffer.get(pps_sps_data)
            }
            19 -> {  // I 帧
                val data = ByteArray(size)
                buffer.get(data)
//                fos.write(data)
                if (pps_sps_data != null) serverSocket.sendData(pps_sps_data!! + data) else serverSocket.sendData(data)
            }
            else -> {
                val data = ByteArray(size)
                buffer.get(data)
//                fos.write(data)
                serverSocket.sendData(data)
            }
        }
    }
}