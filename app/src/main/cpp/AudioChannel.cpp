//
// Created by xz on 2021/1/28 0028.
//

#include <malloc.h>
#include <cstring>
#include "AudioChannel.h"

AudioChannel::AudioChannel() {}

AudioChannel::~AudioChannel() {}

void AudioChannel::openCodec(int sampleRate, int channels) {

    unsigned long inputSamples;

    encHandle = faacEncOpen(sampleRate,channels,&inputSamples,&maxOutputBytes);

    inputByteNum = inputSamples * 2;
    outputBuffer = static_cast<unsigned char *>(malloc(maxOutputBytes));

    faacEncConfigurationPtr configurationPtr = faacEncGetCurrentConfiguration(encHandle);
    configurationPtr->mpegVersion = MPEG4;
    configurationPtr->aacObjectType = LOW;
    configurationPtr->outputFormat = 0;
    configurationPtr->inputFormat = FAAC_INPUT_16BIT;

    faacEncSetConfiguration(encHandle,configurationPtr);
}

void AudioChannel::encode(int32_t *data, int len) {

    int byteLen = faacEncEncode(encHandle,data,len,outputBuffer,maxOutputBytes);
    if (byteLen > 0) {
        RTMPPacket *packet = new RTMPPacket();
        RTMPPacket_Alloc(packet,byteLen + 2);
        packet->m_body[0] = 0xAF;
        packet->m_body[1] = 0x01;
        memcpy(&packet->m_body[2],outputBuffer,byteLen);
        packet->m_hasAbsTimestamp = 0;
        packet->m_nBodySize = byteLen + 2;
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
        packet->m_nChannel = 0x11;
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
        callBack(packet);
    }
}

RTMPPacket * AudioChannel::getAudioConfig() {
    u_char *buf;
    u_long len;

    faacEncGetDecoderSpecificInfo(encHandle,&buf,&len);

    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet,len + 2);
    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x00;
    memcpy(&packet->m_body[2],buf,len);
    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = len + 2;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    return packet;

}