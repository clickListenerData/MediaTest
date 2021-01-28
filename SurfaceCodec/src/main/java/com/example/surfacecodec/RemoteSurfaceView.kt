package com.example.surfacecodec

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class RemoteSurfaceView  @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) :
    SurfaceView(context,attributeSet,defStyle), SurfaceHolder.Callback,ReceiverMsg {

    init {
        holder.addCallback(this)
    }

    private lateinit var playerLive: DecodePlayerLive

    override fun surfaceCreated(holder: SurfaceHolder) {
        playerLive = DecodePlayerLive()
        // TODO: 2020/12/23 0023 解析视频宽高
        playerLive.startLive(1080,1920,holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }

    override fun invoke(data: ByteArray) {
        if (::playerLive.isInitialized) {
            playerLive.decodeFrame(data)
        }
    }
}