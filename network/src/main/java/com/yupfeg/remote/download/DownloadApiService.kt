package com.yupfeg.remote.download

import io.reactivex.rxjava3.core.Flowable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * 文件下载的Retrofit API声明
 * @author yuPFeG
 * @date 2020/04/24
 */
interface DownloadApiService {

    /**
     * 下载文件
     * 大文件官方建议用 [Streaming]来进行注解，不然会出现IO异常，小文件可以忽略不注入
     * @param fileUrl 文件下载完整地址
     */
    @Streaming
    @GET
    fun downloadFileFromUrl(@Url fileUrl : String) : Flowable<ResponseBody>
}