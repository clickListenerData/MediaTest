cmake_minimum_required(VERSION 3.10.2)   # cmake 允许编译得最低版本
add_subdirectory(librtmp)


file(GLOB cpp_source *.cpp)

# 引入x264头文件
include_directories(${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/include)
include_directories(${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/include)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/x264/${ANDROID_ABI}/lib")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/faac/${ANDROID_ABI}/lib")

add_library(
             native-lib    # 动态库名字
#            STATIC   # 编译成静态库  a
             SHARED   # 编译动态库  so
             ${cpp_source} )  # 需要编译得 c++ 文件

message("${PROJECT_SOURCE_DIR}")  # 打印信息
#set_target_properties()  设置属性值

# 从 系统中引入得库
find_library(
              log-lib   # 引入库命名为 log-lib
              log )    # 引入库：liblog.so

target_link_libraries(
                       native-lib
                       ${log-lib}
                        rtmp
                        x264
                        faac)   # 该库链接到 native里