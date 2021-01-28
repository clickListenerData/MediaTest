package com.example.surfacecodec

object YUVUtils {

    fun nv21ToNV12(src: ByteArray, width: Int, height: Int) : ByteArray{
        val newBuffer = ByteArray(src.size)
        val len = src.size * 2 / 3
        var i = 0
        while (i < src.size - 1) {
            if (i >= len) {
                newBuffer[i] = src[i + 1]
                newBuffer[i + 1] = src[i]
                i += 2
            } else {
                newBuffer[i] = src[i]
                ++i
            }
        }

        return newBuffer

        /*var tmp : Byte
        val len = height * 3 / 2  // u v 数据   data 总大小 width * height * 3 / 2
        for (i in 0 until width step 2) {
            for (j in height until len) {
                val index = j * width + i
                tmp = src[index]
                src[index] = src[index + 1]
                src[index + 1] = tmp
            }
        }
        return src*/
    }

    fun portraitData2Raw(src: ByteArray, dest: ByteArray, width: Int, height: Int) {
        val y_len = width * height
        val uvHeight = height shr 1
        var k = 0
        for (i in 0 until width) {
            for (j in (height - 1) downTo 0) {
                dest[k++] = src[j * width + i]
            }
        }

        for (i in 0 until width step 2) {
            for (j in (uvHeight - 1) downTo 0) {
                dest[k++] = src[y_len + j * width + i]
                dest[k++] = src[y_len + j * width + i + 1]
            }
        }

        /*for (i in 0 until width) {
            for (j in 0 until height) {
                // 新数组中摆放Y值 旋转后(i,j) --> 旋转前(srcHeight-1-j, i)
                dest[i * height + j] = src[(height - 1 - j) * width + i]
                // 确认是左上角的点
                if (i % 2 == 0 && j % 2 == 0) {
                    // 摆放V值 目标行号= 行号/2 + 高
                    dest[(i / 2 + width) * height + j] = src[((height - 1 - j) / 2 + height) * width + j]
                    // 摆放U值
                    dest[(i / 2 + width) * height + j + 1] = src[((height - 1 - j) / 2 + height) * width + j + 1]
                }
            }
        }*/

    }

}