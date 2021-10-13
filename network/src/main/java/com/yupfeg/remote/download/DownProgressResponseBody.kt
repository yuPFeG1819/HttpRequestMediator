package com.yupfeg.remote.download

import com.yupfeg.remote.download.entity.DownloadProgressBean
import com.yupfeg.remote.log.HttpLogPrinter
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

/**
 * 监听文件下载进度的 ResponseBody
 * * 暂不支持断点续传
 * @author yuPFeG
 * @date 2020/04/23
 */
class DownProgressResponseBody(
    /**文件下载地址*/
    private val fileUrl : String,
    /**原始的接口返回body*/
    private val responseBody: ResponseBody,
    /**日志输出类*/
    private val logPrinter: HttpLogPrinter ?= null,
    /**下载进度百分比变化*/
    private val onProgressChangeAction : ((DownloadProgressBean)->Unit)?= null
) : ResponseBody(){

    private val logTag = DownProgressResponseBody::class.java.simpleName

    /**
     * BufferedSource 是okio库中的输入流，这里就当作inputStream来使用。
     */
    private val bufferedSource: BufferedSource by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        source(responseBody.source()).buffer()
    }

    override fun contentLength(): Long = responseBody.contentLength()
    override fun contentType(): MediaType = responseBody.contentType()!!
    override fun source(): BufferedSource = bufferedSource

    private fun source(source : Source) : Source{
        return object : ForwardingSource(source){
            var downloadBytes : Long = 0L
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                //增加当前读取的字节数，如果读取完成了bytesRead会返回-1
                downloadBytes += if ((-1).toLong() != bytesRead ) bytesRead else 0
                val totalBytes = responseBody.contentLength()
                val progress = if(totalBytes <= 0) 0f
                else (downloadBytes.toFloat() / totalBytes) * 100
                logPrinter?.printDebugLog(
                    logTag,"file download result-------->> fileTag : $fileUrl \n " +
                        "downloaded bytes ：$downloadBytes \n " +
                        "total download bytes ： ${responseBody.contentLength()}," +
                            "\n downloaded progress ：$progress"
                )
                onProgressChangeAction?.invoke(
                    DownloadProgressBean(
                        fileTag = fileUrl,
                        progress = progress,
                        currByte = downloadBytes,
                        totalByte = responseBody.contentLength(),
                        isDone = bytesRead == (-1).toLong()
                    )
                )
                return bytesRead
            }
        }
    }
}