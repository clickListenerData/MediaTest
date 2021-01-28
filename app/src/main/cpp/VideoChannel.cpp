//
// Created by xz on 2021/1/20 0020.
//

#include <cstring>
#include "VideoChannel.h"

VideoChannel::VideoChannel() {}

void VideoChannel::setVideoEncodeInfo(int width, int height, int fps, int bitrate) {
    mWidth = width;
    mHeight = height;
    mFps = fps;
    mBitrate = bitrate;

    ySize = width * height;
    uvSize = ySize / 4;
    if (videoCodec) {  // videoCodec  非null
        x264_encoder_close(videoCodec);  // 关闭编码器
        videoCodec = 0;
    }

    x264_param_t param;  // 编码参数
    x264_param_default_preset(&param,"ultrafast","zerolatency");
    param.i_level_idc = 32;  // 编码等级
    param.i_csp = X264_CSP_I420; // 显示格式
    param.i_width = width;
    param.i_height = height;
    param.i_bframe = 0;  // B帧数量  直播做好不要B帧
    param.rc.i_rc_method = X264_RC_ABR;
    param.rc.i_bitrate = bitrate / 1024;  // 码率
    param.i_fps_num = fps;  // 帧率
    param.i_fps_den = 1;  // 帧率 时间
    param.i_timebase_den = param.i_fps_num;   // 分母
    param.i_timebase_num = param.i_fps_den;   // 分子  1/25  每帧间隔时间 单位时间 不用时间戳存储
    param.b_repeat_headers = 1;  //每个 关键帧之前都携带sps/pps信息
    param.i_threads = 1;  // 多线程
    x264_param_apply_profile(&param,"baseline");

    videoCodec = x264_encoder_open(&param);
    pic_in = new x264_picture_t();  // 初始化 容器

    x264_picture_alloc(pic_in,X264_CSP_I420,width,height);  // 根据宽高 分配空间
}

void VideoChannel::setVideoCallBack(VideoChannel::VideoCallBack callBack1) {

    this->callBack = callBack1;

}


void VideoChannel::encodeData(int8_t *data) {
    memcpy(pic_in->img.plane[0],data,ySize);  // y 数据
    for (int i = 0; i < uvSize; ++i) {
        *(pic_in->img.plane[1] + i) = *(data + ySize + i * 2 + 1);
        *(pic_in->img.plane[2] + i) = *(data + ySize + i * 2);
    }

    int pi_nal; // 编码出得nal单元个数
    x264_nal_t *pp_nal;  // 编码出得数据
    x264_picture_t pic_out;  //编码出得参数 bufferInfo

    x264_encoder_encode(videoCodec,&pp_nal,&pi_nal,pic_in,&pic_out);  // 编码

    uint8_t sps[100];
    uint8_t pps[100];

    int sps_len,pps_len;
    if (pi_nal > 0) {
        for (int i = 0; i < pi_nal; ++i) {
            javaCallHelper->postH264Data(reinterpret_cast<char *>(pp_nal[i].p_payload), pp_nal[i].i_payload);
            if (pp_nal[i].i_type == NAL_SPS) {
                sps_len = pp_nal[i].i_payload - 4;
                memcpy(sps,pp_nal[i].p_payload + 4,sps_len);
            } else if (pp_nal[i].i_type == NAL_PPS) {
                pps_len = pp_nal[i].i_payload - 4;
                memcpy(pps,pp_nal[i].p_payload + 4,pps_len);
                sendSpsPps(sps,pps,sps_len,pps_len);
            } else {
                sendFrame(pp_nal[i].i_type,pp_nal[i].i_payload,pp_nal[i].p_payload);
            }
        }
    }
}

void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    int size = 16 + sps_len + pps_len;
    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet,size);

    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];
    packet->m_body[i++] = 0xFF;
    packet->m_body[i++] = 0xE1;
    packet->m_body[i++] = ((sps_len >> 8) & 0xFF);
    packet->m_body[i++] = (sps_len & 0xFF);
    memcpy(&packet->m_body[i],sps,sps_len);
    i += sps_len;
    packet->m_body[i++] = 0x01;
    packet->m_body[i++] = ((pps_len >> 8) & 0xFF);
    packet->m_body[i++] = (pps_len & 0xFF);
    memcpy(&packet->m_body[i],pps,pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;

    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    if (callBack) {
        callBack(packet);
    }
}

void VideoChannel::sendFrame(int type, int payload, uint8_t *p_payload) {
    if (p_payload[2] == 0x00) {
        payload -= 4;
        p_payload += 4;
    } else if (p_payload[2] == 0x01) {
        p_payload -= 3;
        p_payload += 3;
    }

    RTMPPacket *packet = new RTMPPacket ;
    int size = payload + 9;
    RTMPPacket_Alloc(packet,size);
    RTMPPacket_Reset(packet);

    if (type == NAL_SLICE_IDR) {
        packet->m_body[0] = 0x17;
    } else {
        packet->m_body[0] = 0x27;
    }

    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    packet->m_body[5] = (payload >> 24)&0xFF;
    packet->m_body[6] = (payload >> 16)&0xFF;
    packet->m_body[7] = (payload >> 8)&0xFF;
    packet->m_body[8] = (payload)&0xFF;

    memcpy(&packet->m_body[9],p_payload,payload);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = size;

    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;

    if (callBack) {
        callBack(packet);
    }
}

VideoChannel::~VideoChannel() {
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = 0;
    }
}