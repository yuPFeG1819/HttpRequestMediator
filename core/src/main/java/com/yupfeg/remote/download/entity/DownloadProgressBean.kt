package com.yupfeg.remote.download.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 文件下载进度信息的RxJava传递bean类
 * @author yuPFeG
 * @date 2020/04/24
 */
@Parcelize
data class DownloadProgressBean(
    /**下载文件唯一标识符*/
    val fileTag : String,
    /**文件对应的百分比进度*/
    val progress : Float,
    /**当前已下载文件大小*/
    val currByte : Long,
    /**总计文件大小*/
    val totalByte : Long,
    /**是否下载完成*/
    val isDone : Boolean = false,
    /**是否下载失败*/
    val isFailure : Boolean = false
) : Parcelable