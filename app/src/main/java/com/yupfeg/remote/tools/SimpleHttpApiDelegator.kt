package com.yupfeg.remote.tools

import com.yupfeg.logger.ext.setDslLoggerConfig
import com.yupfeg.remote.HttpRequestMediator
import com.yupfeg.remote.ext.addDslRemoteConfig
import com.yupfeg.remote.delegator.BaseRequestApiDelegator
import com.yupfeg.remote.interceptor.HttpLogInterceptor


/**
 * 网络请求retrofit api接口的委托
 * * 用于by关键字委托创建api接口实例
 * */
inline fun <reified T> httpApiDelegate() = SimpleHttpApiDelegator(clazz = T::class.java)

/**
 *
 * @author yuPFeG
 * @date 2021/03/30
 */
class SimpleHttpApiDelegator<T>(clazz: Class<T>)
    : BaseRequestApiDelegator<T>(clazz, HttpRequestMediator.DEFAULT_CLIENT_KEY){
    override fun addHttpRequestConfig(configKey: String) {
        setDslLoggerConfig {
            isDisplayClassInfo = false
            isDisplayThreadInfo = false
        }

        addDslRemoteConfig(configKey) {
            baseUrl = "https://api.juejin.cn/"
            connectTimeout = 5
            readTimeout = 10
            writeTimeout = 15
            isAllowProxy = true
            networkInterceptors.add(HttpLogInterceptor(LoggerHttpLogPrinterImpl()))
        }
    }
}