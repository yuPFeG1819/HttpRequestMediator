package com.yupfeg.sample.tools.delegate

import com.yupfeg.remote.HttpRequestMediator
import com.yupfeg.remote.ext.addDslRemoteConfig
import com.yupfeg.remote.tools.delegator.BaseRequestApiDelegator
import com.yupfeg.remote.interceptor.HttpLogInterceptor
import com.yupfeg.sample.tools.LoggerHttpLogPrinterImpl


/**
 * 简单网络请求retrofit api接口的委托
 * * 用于by关键字委托创建api接口实例
 * */
inline fun <reified T> simpleHttpApiDelegate() = SimpleHttpApiDelegator(clazz = T::class.java)

/**
 * 简单网络请求配置代理类
 * @author yuPFeG
 * @date 2021/03/30
 */
class SimpleHttpApiDelegator<T>(clazz: Class<T>)
    : BaseRequestApiDelegator<T>(clazz, HttpRequestMediator.DEFAULT_CLIENT_KEY){
    override fun addHttpRequestConfig(configKey: String) {
        addDslRemoteConfig(configKey) {
            baseUrl = "https://www.wanandroid.com/"
            connectTimeout = 5
            readTimeout = 10
            writeTimeout = 15
            isAllowProxy = true
            networkInterceptors.add(HttpLogInterceptor(LoggerHttpLogPrinterImpl()))
        }
    }
}