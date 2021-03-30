package com.yupfeg.remote.factory

import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * 网络请求工厂类的接口定义
 * @author yuPFeG
 * @date 2021/02/16
 */
interface HttpClientFactory {
    /**
     * 获取[Retrofit]单例
     * @return [Retrofit]对象单例
     */
    fun getRetrofitInstance() : Retrofit

    /**
     * 获取[OkHttpClient]单例
     * @return [OkHttpClient]单例对象
     * */
    fun getOkHttpClientInstance() : OkHttpClient
}