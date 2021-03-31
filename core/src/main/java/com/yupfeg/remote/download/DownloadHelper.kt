package com.yupfeg.remote.download

import com.jakewharton.rxrelay3.PublishRelay
import com.yupfeg.remote.HttpRequestMediator
import com.yupfeg.remote.config.HttpRequestConfig
import com.yupfeg.remote.download.entity.DownloadProgressBean
import com.yupfeg.remote.interceptor.DownloadHttpInterceptor
import com.yupfeg.remote.log.HttpLogPrinter
import com.yupfeg.remote.tools.pool.GlobalHttpThreadPool
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Interceptor
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Http文件下载帮助类
 * 目前仅支持单文件下载
 *
 * 基于RxJava+Retrofit实现下载功能
 * 使用[PublishRelay]向外部发送下载进度变化，fileUrl作为文件唯一标识符，用于区分下载进度所属文件
 * @param requestTag
 * @author yuPFeG
 * @date 2020/04/23
 */
@Suppress("unused")
class DownloadHelper(
    private val requestTag : String = HttpRequestMediator.DEFAULT_DOWNLOAD_CLIENT_KEY,
    private val requestConfig : HttpRequestConfig
) {

    companion object{
        private var logPrinter : HttpLogPrinter? = null

        fun setLogPrinter(printer: HttpLogPrinter){
            logPrinter = printer
        }
    }

    private val downloadApiService : DownloadApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpRequestMediator.createRequestApi(
            requestTag, DownloadApiService::class.java
        )
    }
    /**
     * 文件下载进度百分比的可观察对象，
     * [PublishRelay]只有onNext的subject，不会因为OnError中断信息
     * */
    private val downloadProgressSubject = PublishRelay.create<DownloadProgressBean>()

    /**下载进度变化监听*/
    private val onDownloadProgressListener : ((DownloadProgressBean)->Unit)= {progress->
        downloadProgressSubject.accept(progress)
    }

    init {
        HttpRequestMediator.addDefaultHttpClientFactory (requestTag){
            this.baseUrl = requestConfig.baseUrl
            connectTimeout = requestConfig.connectTimeout
            readTimeout = requestConfig.readTimeout
            writeTimeout = requestConfig.writeTimeout
            isAllowProxy = requestConfig.isAllowProxy
            applicationInterceptors = requestConfig.applicationInterceptors
            networkInterceptors = mutableListOf<Interceptor>().apply{
                addAll(requestConfig.networkInterceptors)
                //默认添加下载进度监听的拦截器
                add(DownloadHttpInterceptor(logPrinter,onDownloadProgressListener))
            }
        }
    }

    /**订阅下载进度变化的可观察对象*/
    fun observeDownloadProgressChange() : Flowable<DownloadProgressBean> {
        return downloadProgressSubject.toFlowable(BackpressureStrategy.BUFFER)
    }

    /**
     * 下载网络文件
     * @param fileUrl 文件下载地址
     * @param saveFilePath 文件保存路径
     * */
    fun downloadFile(fileUrl : String,saveFilePath : String) : Flowable<Unit>{
        return downloadApiService.downloadFileFromUrl(fileUrl)
            .map {responseBody ->
                writeResponseBodyToDiskFile(fileUrl = fileUrl,fileBody = responseBody,filePath = saveFilePath)
            }
            .doOnError {
                downloadProgressSubject.accept(createOnDownloadFailBean(fileUrl))
            }
            //上游执行在子线程，下游执行在主线程
            .subscribeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            .observeOn(AndroidSchedulers.mainThread())
    }


    /**
     * 保存网络返回body到本地路径文件
     * @param fileUrl 文件下载路径,作为文件唯一表示符
     * @param fileBody 网络返回body
     * @param filePath 文件本地保存路径
     * */
    private fun writeResponseBodyToDiskFile(
        fileUrl: String,
        fileBody : ResponseBody,
        filePath : String
    ){
        val downloadFile = File(filePath)
        if (downloadFile.exists()){
            downloadFile.delete()
        }
        val inputStream = fileBody.byteStream()
        var fos : FileOutputStream ?= null
        try {
            downloadFile.createNewFile()
            val buffer = ByteArray(2048)
            var len: Int
            fos = FileOutputStream(downloadFile)
            do {
                len = inputStream.read(buffer)
                //没有更多数据则跳出循环
                if (len == -1) break
                fos.write(buffer, 0, len)
            }while (true)
        }catch (e : IOException){
            downloadProgressSubject.accept(createOnDownloadFailBean(fileUrl))
        }finally {
            fos?.flush()
            fos?.close()
            inputStream.close()
        }
    }

    /**
     * 创建下载失败的状态bean类
     * @param fileTag 文件下载地址，唯一标识符
     */
    private fun createOnDownloadFailBean(fileTag : String) : DownloadProgressBean {
        return DownloadProgressBean(fileTag = fileTag,currByte = 0,
            progress = 0f,totalByte = 0, isDone = false,isFailure = true)
    }
}