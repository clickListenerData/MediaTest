package com.example.mediaproject

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class LocalSurfaceView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyle: Int = 0) :
        SurfaceView(context,attributeSet,defStyle), SurfaceHolder.Callback, Camera.PreviewCallback {

    private lateinit var mCamera: Camera
    private lateinit var size : Camera.Size

    private lateinit var buffer: ByteArray

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
        size = mCamera.parameters.previewSize

        mCamera.setPreviewDisplay(holder)
        mCamera.setDisplayOrientation(90)
        buffer = ByteArray(size.width * size.height * 3 / 2)
        mCamera.addCallbackBuffer(buffer)
        mCamera.setPreviewCallbackWithBuffer(this)

        mCamera.startPreview()

    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {

        // 数据
        mCamera.addCallbackBuffer(data)
    }
}