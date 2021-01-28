package com.example.screenlive

import android.media.*

/**
 * 音频 采集 编码 AAC
 */
class AudioCodec : Thread() {

    private lateinit var mediaCodec: MediaCodec
    private lateinit var audioRecord: AudioRecord
    private var isRecording = false
    private var startTime = 0L
    private var minBufferSize = 0

    fun startLive() {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_VIDEO_AVC,44100,1)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE,64000)

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mediaCodec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()

        minBufferSize = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,minBufferSize)
    }

    override fun run() {
        super.run()

        isRecording = true

        val bufferInfo = MediaCodec.BufferInfo()

        val firstData = byteArrayOf(0x12,0x08)
        // push aac first data   开始录音前 先发一段准备数据

        audioRecord.startRecording()

        val buffer = ByteArray(minBufferSize)

        while (isRecording) {
            val len = audioRecord.read(buffer,0,buffer.size)
            if (len <= 0) continue
            val inputIndex = mediaCodec.dequeueInputBuffer(1000)
            if (inputIndex >= 0) {
                val inputBuffer = mediaCodec.getInputBuffer(inputIndex)
                inputBuffer?.clear()
                inputBuffer?.put(buffer,0,len)
                mediaCodec.queueInputBuffer(inputIndex,0,len,System.nanoTime() / 1000,0)
            }
            var outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,1000)

            while (outputIndex >= 0 && isRecording) {
                val outputBuffer = mediaCodec.getOutputBuffer(outputIndex)
                val outData = ByteArray(bufferInfo.size)
                outputBuffer?.get(outData)

                if (startTime == 0L) {
                    startTime = bufferInfo.presentationTimeUs / 1000
                }

                // push audio data:  data: outData  time: (bufferInfo.presentationTimeUs / 1000) - startTime  type: audio
                mediaCodec.releaseOutputBuffer(outputIndex,false)
                outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0)
            }
        }

        audioRecord.stop()
        audioRecord.release()
        mediaCodec.stop()
        mediaCodec.release()
        startTime = 0
        isRecording = false
    }
}