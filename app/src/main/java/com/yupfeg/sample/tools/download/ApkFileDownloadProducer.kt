package com.yupfeg.sample.tools.download

import com.jakewharton.rxrelay3.PublishRelay
import com.yupfeg.remote.HttpRequestMediator
import com.yupfeg.remote.config.HttpRequestConfig
import com.yupfeg.remote.download.BaseFileDownloadProducer
import com.yupfeg.remote.download.entity.DownloadProgressBean
import com.yupfeg.remote.interceptor.DownloadProgressInterceptor
import com.yupfeg.remote.log.HttpLogPrinter
import com.yupfeg.remote.tools.pool.GlobalHttpThreadPoolExecutor
import com.yupfeg.sample.data.remote.DownloadApiService
import com.yupfeg.sample.tools.LoggerHttpLogPrinterImpl
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import okhttp3.Interceptor
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

/**
 * 基于RxJava3+Retrofit实现下载apk功能的数据源提供类
 * 使用[BehaviorSubject]向外部发送下载进度变化，fileUrl作为文件唯一标识符，用于区分下载进度所属文件
 * @author yuPFeG
 * @date 2021/09/23
 */
class ApkFileDownloadProducer(
    private val requestTag : String = HttpRequestMediator.DEFAULT_DOWNLOAD_CLIENT_KEY,
    private val requestConfig : HttpRequestConfig
) : BaseFileDownloadProducer(){

    private val mDownloadApiService : DownloadApiService by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HttpRequestMediator.createRequestApi(
            requestTag, DownloadApiService::class.java
        )
    }

    /**
     * 文件下载进度百分比的可观察对象，
     * [PublishRelay]只有onNext的subject，不会因为OnError中断信息
     * */
    private val mDownloadProgressSubject = PublishRelay.create<DownloadProgressBean>()

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
                //添加下载进度监听的拦截器
                add(createDownloadInterceptor(LoggerHttpLogPrinterImpl()))
            }
            //支持RxJava3(create()方法创建的是采用okHttp内置的线程池，下游数据流使用subscribeOn无效
            // createSynchronous()则使用下游subscribeOn提供的线程池)
            callAdapterFactories = mutableListOf(RxJava3CallAdapterFactory.createSynchronous())
        }
    }

    /**
     * 订阅指定地址的下载进度变化
     * @param fileUrl 文件下载地址
     * */
    fun observeDownloadProgressChange(fileUrl: String) : Flowable<DownloadProgressBean>{
        return mDownloadProgressSubject.toFlowable(BackpressureStrategy.LATEST)
            //仅允许指定下载地址的进度继续向下游进行
            .filter { it.fileTag == fileUrl }
    }

    /**
     * 下载网络文件
     * @param fileUrl 文件下载地址
     * @param saveFilePath 文件保存路径
     * */
    fun downloadApk(
        fileUrl : String, saveFilePath : String
    ) : Maybe<Unit> {
        return mDownloadApiService.downloadFileFromUrl(fileUrl)
            //step 下载文件响应，将byte字节数组保存到本地文件
            .map { responseBody->
                writeResponseBodyToDiskFile(
                    fileUrl = fileUrl,
                    fileBody = responseBody,
                    filePath = saveFilePath
                )
            }
            //step 下载出现异常
            .doOnError {
                sendDownloadProgressChange(DownloadProgressBean.createDownloadFailure(fileUrl))
            }
            //上游执行在子线程，下游执行在主线程
            .subscribeOn(Schedulers.from(GlobalHttpThreadPoolExecutor.executorService))
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun createDownloadInterceptor(logPrinter : HttpLogPrinter? = null) : Interceptor{
        return DownloadProgressInterceptor(
            logPrinter = logPrinter,
        ){progressBean->
            //此时处于子线程，不能直接回调执行UI操作
            sendDownloadProgressChange(progressBean)
        }
    }

    private fun sendDownloadProgressChange(progressBean: DownloadProgressBean){
        mDownloadProgressSubject.accept(progressBean)
    }

}