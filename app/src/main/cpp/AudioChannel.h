//
// Created by xz on 2021/1/28 0028.
//

#ifndef MEDIAPROJECT_AUDIOCHANNEL_H
#define MEDIAPROJECT_AUDIOCHANNEL_H

#include "faac.h"

extern "C" {
#include "librtmp/rtmp.h"
}


class AudioChannel {

    typedef void (*AudioCallBack) (RTMPPacket *packet);

public:
    AudioChannel();
    ~AudioChannel();

    void openCodec(int sampleRate,int channels);
    void encode(int32_t *data,int len);

    RTMPPacket *getAudioConfig();

    void setCallback(AudioCallBack callBack1) {
        this->callBack = callBack1;
    }

    int getInputByteNum() {
        return inputByteNum;
    }

private:
    AudioCallBack callBack;
    faacEncHandle encHandle = 0;

    // 压缩后 得最大数据量
    unsigned long maxOutputBytes;
    // 输出得数据
    unsigned char *outputBuffer = 0;
    // 输入容器大小
    unsigned long inputByteNum;
};


#endif //MEDIAPROJECT_AUDIOCHANNEL_H
