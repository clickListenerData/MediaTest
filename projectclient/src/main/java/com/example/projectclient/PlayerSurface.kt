package com.example.projectclient

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class PlayerSurface @JvmOverloads constructor(context: Context,attributeSet: AttributeSet? = null,defStyle: Int = 0) :
    SurfaceView(context,attributeSet,defStyle), SurfaceHolder.Callback {

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        DecodeLive(holder.surface).startLive()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {

    }
}