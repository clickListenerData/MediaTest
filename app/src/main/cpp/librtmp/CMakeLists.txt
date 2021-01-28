cmake_minimum_required(VERSION 3.10.2)   # cmake 允许编译得最低版本
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")
file(GLOB rtmp_source *.c)
add_library(rtmp
        STATIC
        ${rtmp_source})