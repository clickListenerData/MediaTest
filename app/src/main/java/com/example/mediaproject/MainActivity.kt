package com.example.mediaproject

import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.io.File

class MainActivity : AppCompatActivity() {

    private val manager by lazy { getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startActivityForResult(manager.createScreenCaptureIntent(),0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && data != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(Intent(this,ScrrenServer::class.java).apply {
                    putExtra("code",resultCode)
                    putExtra("data",data)
                })
            } else {
                startService(Intent(this,ScrrenServer::class.java).apply {
                    putExtra("code",resultCode)
                    putExtra("data",data)
                })
            }
        }
    }
}