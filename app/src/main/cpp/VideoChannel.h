//
// Created by xz on 2021/1/20 0020.
//

#ifndef MEDIAPROJECT_VIDEOCHANNEL_H
#define MEDIAPROJECT_VIDEOCHANNEL_H

#include <inttypes.h>
#include "x264.h"
#include <jni.h>
#include "JavaCallHelper.h"
extern "C" {
#include "librtmp/rtmp.h"
}

class VideoChannel {

    typedef void (*VideoCallBack) (RTMPPacket *packet);

public:
    VideoChannel();
    ~VideoChannel();

    void setVideoCallBack(VideoCallBack callBack1);
    void setVideoEncodeInfo(int width,int height,int fps,int bitrate);
    void encodeData(int8_t *data);
    void sendSpsPps(uint8_t* sps,uint8_t* pps,int sps_len,int pps_len);
    void sendFrame(int type,int payload,uint8_t* p_payload);

private:
    int mWidth;
    int mHeight;
    int mFps;
    int mBitrate;
// 输入容器 input ByteBuffer
    x264_picture_t *pic_in = 0;
    int ySize = 0;
    int uvSize = 0;
// 编码器
    x264_t *videoCodec = 0;
    VideoCallBack  callBack;
public:
    JavaCallHelper *javaCallHelper;
};


#endif //MEDIAPROJECT_VIDEOCHANNEL_H
