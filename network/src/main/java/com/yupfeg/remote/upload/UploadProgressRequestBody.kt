package com.yupfeg.remote.upload

import com.yupfeg.remote.log.HttpLogPrinter
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*

/**
 * 网络上传进度监听
 * */
interface OnUploadProgressListener{
    /**
     * 文件上传进度变化
     * * 处于子线程，不可直接修改UI
     * @param progressBean
     */
    fun onProgressChange(progressBean: UploadProgressBean)
}

/**
 * 监听上传进度的[RequestBody]代理类
 * @author yuPFeG
 * @date 2021/10/14
 */
@Suppress("unused")
class UploadProgressMultipartBody(
    /**唯一标识符，区分上传文件*/
    private val fileId : String,
    /**原始请求提*/
    private val originRequestBody: RequestBody,
    private val logPrinter: HttpLogPrinter? = null,
    /**上传进度监听*/
    private val mProgressChangeListener : OnUploadProgressListener
) : RequestBody(){

    private val logTag = UploadProgressMultipartBody::class.java.simpleName

    /**
     * BufferedSource 是okio库中的输入流，这里就当作inputStream来使用。
     */
    private var mBufferedSink: BufferedSink? = null

    override fun contentType(): MediaType? {
        return originRequestBody.contentType()
    }

    override fun contentLength(): Long {
        return originRequestBody.contentLength()
    }

    /**
     * 重写上传写入操作
     * @param sink okio中的输出流
     * */
    override fun writeTo(sink: BufferedSink) {
        val bufferSink = getProgressBufferSink(sink)
        originRequestBody.writeTo(bufferSink)
        //必须调用flush，否则最后一点字节会被忽略
        bufferSink.flush()
    }

    private fun getProgressBufferSink(sink: BufferedSink) : BufferedSink {
        return mBufferedSink?: synchronized(this){
            mBufferedSink ?: createWriteProgressSink(sink).apply { mBufferedSink = this }
        }
    }

    /**
     * 创建监听写入（上传）进度的输出流
     * */
    private fun createWriteProgressSink(sink: BufferedSink) : BufferedSink {
        return object : ForwardingSink(sink){
            /**已上传字节量*/
            private var mUploadBytes : Long = 0
            /**总字节长度，避免多次调用contentLength*/
            private var mTotalBytes : Long = -1

            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                if(mTotalBytes <= 0){
                    //仅调用一次contentLength
                    mTotalBytes = contentLength()
                }

                mUploadBytes += byteCount
                val progress = if(mTotalBytes <= 0) 0f
                else (mUploadBytes.toFloat() / mTotalBytes) * 100

                logPrinter?.printDebugLog(
                    logTag,"file upload to http -------->> " +
                            "fileId : $fileId \n " +
                            "upload bytes ：$mUploadBytes \n " +
                            "total download bytes ： ${mTotalBytes}," +
                            "\n downloaded progress ：$progress"
                )
                //记载进度
                mProgressChangeListener.onProgressChange(
                    UploadProgressBean(
                        id = fileId,
                        writtenBytes = mUploadBytes,
                        totalBytes = mTotalBytes,
                        progress = progress,
                        isDone = mUploadBytes >= mTotalBytes
                    )
                )
            }

        }.buffer()
    }
}