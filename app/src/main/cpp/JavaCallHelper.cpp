//
// Created by xz on 2021/1/23 0023.
//

#include "JavaCallHelper.h"

JavaCallHelper::JavaCallHelper(JavaVM *vm, JNIEnv *env, jobject &thiz) : vm(vm),env(env) {
    this->thiz = env->NewGlobalRef(thiz);
    jclass c = env->GetObjectClass(thiz);
    id = env->GetMethodID(c,"postH264","([B)V");
}

void JavaCallHelper::postH264Data(char *data, int length,int thread) {

//    vm.AttachCurrentThread(env, nullptr);
//    vm.DetachCurrentThread();

    jbyteArray dd = env->NewByteArray(length);
    env->SetByteArrayRegion(dd, 0, length, reinterpret_cast<const jbyte *>(data));
    if (thread == 2) {
        JNIEnv *jniEnv;
        if ( vm->AttachCurrentThread(&jniEnv,0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(thiz,id,dd);
        vm->DetachCurrentThread();
    } else {
        env->CallVoidMethod(thiz,id,dd);
    }
}

JavaCallHelper::~JavaCallHelper() {
    env->DeleteGlobalRef(thiz);
    thiz = 0;
}


