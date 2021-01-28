package com.example.projectclient

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projectclient.databinding.ActivityLiveBinding

class LiveActivity : AppCompatActivity() {

    private val socket by lazy { SocketLive() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityLiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLive.setOnClickListener {
            socket.start()
            socket.setReceiverCB(binding.rsfv)

            binding.lsfv.startLive(socket)
        }
    }
}