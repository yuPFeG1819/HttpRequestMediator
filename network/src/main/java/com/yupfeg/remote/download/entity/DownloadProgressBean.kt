package com.yupfeg.remote.download.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 文件下载进度信息的数据传递bean类
 * @author yuPFeG
 * @date 2020/04/24
 */
@Parcelize
data class DownloadProgressBean(
    /**下载文件唯一标识符*/
    val fileTag : String,
    /**文件对应的百分比进度*/
    val progress : Float,
    /**当前已下载文件大小,单位byte*/
    val currByte : Long,
    /**总计文件大小，单位byte*/
    val totalByte : Long,
    /**是否下载完成*/
    val isDone : Boolean = false,
    /**是否下载失败*/
    val isFailure : Boolean = false
) : Parcelable{
    companion object{
        /**
         * 创建下载失败bean
         * @param fileTag 文件下载地址，作为唯一标识符
         * */
        @Suppress("unused")
        @JvmStatic
        fun createDownloadFailure(fileTag: String) : DownloadProgressBean{
            return DownloadProgressBean(
                fileTag = fileTag,progress = 0f,
                currByte = 0,totalByte = 0,
                isFailure = true
            )
        }
    }
}