package com.example.mediaproject

import java.util.concurrent.LinkedBlockingDeque

class RtmpScrenLive : Thread() {

    init {
        System.loadLibrary("native-lib")
    }

    private val queues = LinkedBlockingDeque<RtmpPackage>()

    private var isLive = false

    fun addPackage(data: ByteArray,time: Long) {
        queues.add(RtmpPackage(data, time))
    }

    fun startLive() {
        start()
    }

    private fun postH264(data: ByteArray) {

    }

    override fun run() {
        super.run()

        isLive = true
        while (isLive) {
            val data = queues.take()
            sendPackage(data.data,data.data.size,data.time,0)
        }
    }

    external fun connect(url: String) : Boolean  // 链接rtmp 服务器

    private external fun sendPackage(data: ByteArray, len: Int, time: Long,type: Int)   // 发送h264 码流

    /**
     * x264  编码
     *
     */
    external fun setVideoEncodeInfo(width: Int,height: Int,fps: Int,bitrate: Int)

    // 开线程 链接服务器
    external fun start(url: String)

    // 开始编码 推流 数据
    external fun pushVideo(data: ByteArray)

    // 停止 推流
    external fun stopVideo()

    /**
     * aac 编码
     * 返回 最大音频输入 minBufferSize
     */
    external fun setAudioEncodeInfo(sampleRate: Int,channels: Int) : Int

    external fun pushAudio(data: ByteArray,len: Int)

}