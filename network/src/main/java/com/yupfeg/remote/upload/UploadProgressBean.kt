package com.yupfeg.remote.upload

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 网络上传进度监听实体
 * @author yuPFeG
 * @date 2021/10/14
 */
@Parcelize
data class UploadProgressBean(
    /**唯一标识符，用于识别上传文件*/
    val id : String,
    /**已写入（已上传）字节数，单位bytes*/
    val writtenBytes : Long,
    /**总计字节数，单位bytes*/
    val totalBytes : Long,
    /**上传进度*/
    val progress : Float,
    /**是否上传成功*/
    val isDone : Boolean = false,
    /**是否出现异常*/
    val isFailure : Boolean = false
) : Parcelable{
    companion object{
        /**
         * 创建上传失败实体类
         * @param id 唯一标识符
         */
        @Suppress("unused")
        @JvmStatic
        fun createFailure(id : String) = UploadProgressBean(
            id = id,writtenBytes = 0,totalBytes = 0,progress = 0f,isDone = false,isFailure = true
        )
    }
}
