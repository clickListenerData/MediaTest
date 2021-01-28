#include <jni.h>
#include <string>
#include "android/log.h"
#include "VideoChannel.h"
#include "AudioChannel.h"
#include "safe_queue.h"
#include "JavaCallHelper.h"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"zzzzzzz",__VA_ARGS__)
extern "C" {
#include "librtmp/rtmp.h"
}

typedef struct {
    RTMP *rtmp;
    int16_t sps_len;
    int16_t pps_len;
    int8_t *sps;
    int8_t *pps;
} Live;

JavaVM *javaVM = 0;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_4;
}

Live *live = NULL;
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_mediaproject_RtmpScrenLive_connect(JNIEnv *env,jobject thiz,jstring url_) {

    const char *url = env->GetStringUTFChars(url_,FALSE);
    int ret;

    do {
        live = (Live *)malloc(sizeof(Live));
        memset(live,0,sizeof(Live));

        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        LOGI("connect %s",url);
        ret = RTMP_SetupURL(live->rtmp,(char *)url);
        if (!ret) break;
        RTMP_EnableWrite(live->rtmp);
        LOGI("connect");
        if (!(ret = RTMP_Connect(live->rtmp,0))) break;
        LOGI("connect stream");
        if (!(ret = RTMP_ConnectStream(live->rtmp,0))) break;
        LOGI("connect success");
    }while (false);

    if (!ret && live) {
        free(live);
        live = nullptr;
    }
    env->ReleaseStringUTFChars(url_,url);
    return ret;
}

void prepareVideo(int8_t *buf,int len) {
    for (int i = 0; i < len; ++i) {
        if (i + 4 < len) {
            if (buf[i] == 0x00 && buf[i+1] == 0x00 && buf[i+2] == 0x00 && buf[i+3] == 0x01 && (buf[i+4] & 0x1F) == 8) { // vps 分隔符
                // sps 处理
                live->sps_len = i - 4;
                live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                memcpy(live->sps,buf+4,live->sps_len);

                // pps 处理
                live->pps_len = len - (live->sps_len + 4) - 4;
                live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                memcpy(live->pps,buf + 4 + live->sps_len + 4,live->pps_len);
            }
        }
    }
}

RTMPPacket * createSPSPacket() {
    int size = 16 + live->sps_len + live->pps_len;
    RTMPPacket *packet = (RTMPPacket *)malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet,size);

    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = live->sps[1];
    packet->m_body[i++] = live->sps[2];
    packet->m_body[i++] = live->sps[3];
    packet->m_body[i++] = 0xFF;
    packet->m_body[i++] = 0xE1;
    packet->m_body[i++] = ((live->sps_len >> 8) & 0xFF);
    packet->m_body[i++] = (live->sps_len & 0xFF);
    memcpy(&packet->m_body[i],live->sps,live->sps_len);
    i += live->sps_len;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = ((live->pps_len >> 8) & 0xFF);
    packet->m_body[i++] = (live->pps_len & 0xFF);
    memcpy(&packet->m_body[i],live->pps,live->pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;

    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

RTMPPacket *createVideoPacket(int8_t *buf,int len,long time) {
    buf += 4;
    RTMPPacket *packet = (RTMPPacket *)malloc(sizeof(RTMPPacket));
    int size = len + 9;
    RTMPPacket_Alloc(packet,size);

    if ((buf[0] & 0x1F) == 5) {
        packet->m_body[0] = 0x17;
    } else {
        packet->m_body[0] = 0x27;
    }

    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    packet->m_body[5] = (len >> 24)&0xFF;
    packet->m_body[6] = (len >> 16)&0xFF;
    packet->m_body[7] = (len >> 8)&0xFF;
    packet->m_body[8] = (len)&0xFF;

    memcpy(&packet->m_body[9],buf,len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;

    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = time;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

int sendPacket(RTMPPacket *packet) {
    int ret = RTMP_SendPacket(live->rtmp,packet,1);
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}

void sendVideoData(int8_t *buf,int len,long time) {
    if ((buf[4] & 0x1F) == 7) { // sps
        if (live && (!live->pps || !live->sps)) {
            prepareVideo(buf,len);
        }
        return;  // 此时无需推流
    }

    if ((buf[4] & 0x1F) == 5) { // 关键帧
        // 先发送 sps 数据
        RTMPPacket *packet = createSPSPacket();
        sendPacket(packet);
    }

    // 发送 H264码流
    RTMPPacket *data = createVideoPacket(buf,len,time);
    sendPacket(data);

}

RTMPPacket *createAudioPacket(int8_t *buf,int len,long time,int type) {
    int bodySize = len + 2;
    RTMPPacket *packet = (RTMPPacket *)malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet,bodySize);
    packet->m_body[0] = 0xAF;
    if (type == 1)  {
        packet->m_body[1] = 0x00;
    } else {
        packet->m_body[1] = 0x01;
    }

    memcpy(&packet->m_body[2],buf,len);
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nChannel = 0x05;
    packet->m_nBodySize = bodySize;
    packet->m_nTimeStamp = time;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    return packet;
}

void sendAudioData(int8_t *buf,int len,long time,int type) {
    RTMPPacket *packet = createAudioPacket(buf,len,time,type);
    sendPacket(packet);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mediaproject_RtmpScrenLive_sendPackage(JNIEnv *env, jobject thiz, jbyteArray data_,
                                                        jint len, jlong time,jint type) {

    jbyte *data = env->GetByteArrayElements(data_, 0);
    switch (type) {
        case 0:
            sendVideoData(data,len,time);
            break;
        default:
            sendAudioData(data,len,time,type);
            break;
    }
    env->ReleaseByteArrayElements(data_, data, 0);
}


VideoChannel *videoChannel = 0;
AudioChannel *audioChannel = 0;
int isStart = 0;  // 开始线程
pthread_t pid;  // 记录子线程对象
int readPushing = 0;  // 推流标志位
SafeQueue<RTMPPacket *> *packets;
uint32_t start_time;
JavaCallHelper *helper = 0;
RTMP *rtmp = 0;


void callBack(RTMPPacket *packet) {
    if (packet) {
        if (packets->size() > 50) {
            packets->clear();
        }
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets->push(packet);
    }
}

void releasePacket(RTMPPacket *&packet) {
    if(packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}

void *start(void *args) {
    char *url = static_cast<char *>(args);
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            // 创建失败
            break;
        }
        RTMP_Init(rtmp);
        rtmp->Link.timeout = 5;
        int ret = RTMP_SetupURL(rtmp,url);
        if(!ret) {
            // 设置地址失败
            break;
        }
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp,0);
        if(!ret) {
            // 链接失败
            break;
        }
        ret = RTMP_ConnectStream(rtmp,0);
        if(!ret) {
            break;
        }

        readPushing = 1;
        start_time = RTMP_GetTime();
        packets->setWork(1);
        RTMPPacket *packet = 0;
        while (isStart) {
            packets->pop(packet);
            if(!isStart) break;
            if(!packet) continue;
            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp,packet,1);
            releasePacket(packet);
            if(!ret) {
                // 发送数据失败
                break;
            }
        }
        releasePacket(packet);
    }while (0);
    if(rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mediaproject_RtmpScrenLive_setVideoEncodeInfo(JNIEnv *env, jobject thiz,
                                                               jint width, jint height, jint fps,
                                                               jint bitrate) {
    helper = new JavaCallHelper(javaVM,env,thiz);

    videoChannel = new VideoChannel;
    videoChannel->setVideoCallBack(callBack);
    videoChannel->javaCallHelper = helper;

    videoChannel->setVideoEncodeInfo(width,height,fps,bitrate);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mediaproject_RtmpScrenLive_start(JNIEnv *env, jobject thiz, jstring url) {
    if (isStart) {
        return;
    }

    const char *path = env->GetStringUTFChars(url,0);
    char *url_ = new char[strlen(path) + 1];
    strcpy(url_,path);

    isStart = 1;
    pthread_create(&pid,0,start,url_);

    env->ReleaseStringUTFChars(url,path);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mediaproject_RtmpScrenLive_pushVideo(JNIEnv *env, jobject thiz, jbyteArray data) {

    if (!videoChannel || !readPushing) {
        return;
    }
    jbyte *buf = env->GetByteArrayElements(data,0);
    videoChannel->encodeData(buf);
    env->ReleaseByteArrayElements(data,buf,0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_mediaproject_RtmpScrenLive_stopVideo(JNIEnv *env, jobject thiz) {
    isStart = 0;
    readPushing = 0;
}

void release() {
    if(rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
        rtmp = 0;
    }
    if (videoChannel) {
        delete (videoChannel);
        videoChannel = 0;
    }
    if (helper) {
        delete (helper);
        helper = 0;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mediaproject_RtmpScrenLive_setAudioEncodeInfo(JNIEnv *env, jobject thiz,
                                                               jint sample_rate, jint channels) {

    audioChannel = new AudioChannel;
    audioChannel->setCallback(callBack);
    audioChannel->openCodec(sample_rate,channels);
    return audioChannel->getInputByteNum();

}extern "C"
JNIEXPORT void JNICALL
Java_com_example_mediaproject_RtmpScrenLive_pushAudio(JNIEnv *env, jobject thiz, jbyteArray data,
                                                      jint len) {
    jbyte *buf = env->GetByteArrayElements(data,0);
    audioChannel->encode(reinterpret_cast<int32_t *>(buf), len);
    env->ReleaseByteArrayElements(data,buf,0);
}