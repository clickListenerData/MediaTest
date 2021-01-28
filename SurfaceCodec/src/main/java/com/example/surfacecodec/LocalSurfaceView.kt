package com.example.surfacecodec

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * 本地摄像头数据
 */
class LocalSurfaceView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) :
        SurfaceView(context, attributeSet, defStyle), SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {

    private lateinit var mCamera: Camera
    private lateinit var size : Camera.Size

    private lateinit var buffer: ByteArray

    private lateinit var encodePushLive: EncodePushLive

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        openCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    private fun openCamera() {

        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK)
        val parameters = mCamera.parameters
        // 9:16
        /*parameters.supportedPreviewSizes.forEach {

        }*/
//        parameters.setPreviewSize(1280, 720)
        parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        parameters.colorEffect = Camera.Parameters.EFFECT_NEGATIVE
        mCamera.parameters = parameters
        size = mCamera.parameters.previewSize

        mCamera.setPreviewDisplay(holder)
        mCamera.setDisplayOrientation(90)
        buffer = ByteArray(size.width * size.height * 3 / 2)
        mCamera.addCallbackBuffer(buffer)
        mCamera.setPreviewCallbackWithBuffer(this)

        mCamera.startPreview()

        mCamera.autoFocus(this)  // 自动聚焦

    }

    fun startLive(serverLive: IServerLive) {
        Log.i("zzzzzzzzzzzzz", "${size.width} ,, ${size.height}")  // 1920 1080
        encodePushLive = EncodePushLive(serverLive, size.width, size.height)
        encodePushLive.startLive()
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (::encodePushLive.isInitialized && data != null) {
            encodePushLive.encodeFrame(data)
        }
        // 数据
        mCamera.addCallbackBuffer(data)
    }

    override fun onAutoFocus(success: Boolean, camera: Camera?) {
        postDelayed({mCamera.autoFocus(this)},2000)
    }
}