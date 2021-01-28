package com.example.mediaproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File


class ScrrenServer : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mResultCode = intent?.getIntExtra("code", -1) ?: -1
        val mResultData = intent?.getParcelableExtra<Intent>("data") ?: return super.onStartCommand(
            intent,
            flags,
            startId
        )
        //mResultData = intent.getSelector();

        val mMediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(
            mResultCode,
            mResultData
        );
        EncodeLive(mMediaProjection, File(cacheDir.absolutePath + File.separator + "test.h264")).startLive()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this.applicationContext,"test") //获取一个Notification构造器
        val nfIntent = Intent(this, MainActivity::class.java) //点击后跳转的界面，可以设置跳转数据
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.ic_launcher
                )
            ) // 设置下拉列表中的图标(大图标)
            //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
            .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
            .setContentText("is running......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("test")
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "test",
                "test",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = builder.build() // 获取构建好的Notification
        startForeground(110, notification)
    }
}