package com.example.screenlive.mediaproject

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.os.Bundle

/**
 * 录屏 采集 AVC
 * @param mediaProjection 录屏工具类
 */
class VideoCodec(private val mediaProjection: MediaProjection) : Thread() {

    private lateinit var mediaCodec: MediaCodec
    private lateinit var display: VirtualDisplay

    private var startTime = 0L
    private var timeStamp = 0L
    private var isLiving = false

    fun startLive() {
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,720,1280)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE,400_000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE,15)       // 15帧/s
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)   // I帧 触发间隔1s
        mediaCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)

        val surface = mediaCodec.createInputSurface()
        display = mediaProjection.createVirtualDisplay("screen_video",720,1280,1,DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,surface,null,null)
    }

    override fun run() {
        super.run()
        isLiving = true
        mediaCodec.start()
        val info = MediaCodec.BufferInfo()

        while (isLiving) {
            if (System.currentTimeMillis() - timeStamp >= 2000) {
                val bundle = Bundle()
                bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME,0)
                mediaCodec.setParameters(bundle)  // 设置参数  手动触发关键帧
                timeStamp = System.currentTimeMillis()
            }

            var index = mediaCodec.dequeueOutputBuffer(info,100000)
            if (index >= 0) {
                val buffer = mediaCodec.getOutputBuffer(index)
                val outData = ByteArray(info.size)
                buffer?.get(outData)
                if (startTime == 0L) {
                    startTime = info.presentationTimeUs / 1000
                }
                // push h264 data       data: outData   time: (info.presentationTimeUs / 1000) - startTime  type: video
                mediaCodec.releaseOutputBuffer(index,false)
            }
        }

        isLiving = false
        startTime = 0
        mediaCodec.stop()
        mediaCodec.release()
        display.release()
        mediaProjection.stop()
    }
}