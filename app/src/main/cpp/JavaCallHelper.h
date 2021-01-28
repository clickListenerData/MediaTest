//
// Created by xz on 2021/1/23 0023.
//

#ifndef MEDIAPROJECT_JAVACALLHELPER_H
#define MEDIAPROJECT_JAVACALLHELPER_H

#include "jni.h"
#define THREAD_MAIN 1;
#define THREAD_CHILD 2;

class JavaCallHelper {

public:
    JavaCallHelper(JavaVM *vm,JNIEnv *env,jobject &thiz);
    ~JavaCallHelper();

    void postH264Data(char *data,int length,int thread = 1);

private:
    JavaVM *vm;
    JNIEnv *env;
    jobject thiz;
    jmethodID id;

};


#endif //MEDIAPROJECT_JAVACALLHELPER_H
