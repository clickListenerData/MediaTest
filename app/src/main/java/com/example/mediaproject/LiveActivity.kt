package com.example.mediaproject

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mediaproject.databinding.ActivityLiveBinding


class LiveActivity : AppCompatActivity() {

    private val socket by lazy { SocketLive() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        selectCodec(true,"")
//        selectCodec(false,"")
        binding.btnLive.setOnClickListener {
            socket.start()
            socket.setReceiverCB(binding.rsfv)

            binding.lsfv.startLive(socket)
        }
    }


    private fun selectCodec(encoder: Boolean, mimeType: String): MediaCodecInfo? {
        val list = MediaCodecList(REGULAR_CODECS)
        val infos = list.codecInfos
        for (m in infos) {
            if (m.isEncoder != encoder) {
                continue
            }
            for (type in m.supportedTypes) {
                /*if (type.equals(mimeType, ignoreCase = true)) {

                }*/
                Log.d("zzzzzzzzzzzzzzzzzzzzz", "$encoder  the selected encoder is :  $type   " + m.name)
//                return m
            }
        }
        return null
    }

}