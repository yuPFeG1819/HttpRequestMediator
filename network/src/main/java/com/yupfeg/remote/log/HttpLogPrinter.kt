package com.yupfeg.remote.log

/**
 * 网络请求日志输出接口声明
 * @author yuPFeG
 * @date 2021/03/31
 */
interface HttpLogPrinter {
    /**输出debug日志*/
    fun printDebugLog(tag : String, content : Any)

    /**输出info日志*/
    fun printInfoLog(tag: String,content: Any)

    /**输出warning日志*/
    fun printWarningLog(tag : String,content : Any)

    /**输出error日志*/
    fun printErrorLog(tag : String,content : Any)
}