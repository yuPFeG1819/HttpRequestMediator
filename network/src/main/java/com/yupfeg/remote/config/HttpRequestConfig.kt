package com.yupfeg.remote.config

import com.yupfeg.remote.interceptor.MultipleHostInterceptor
import okhttp3.Cache
import okhttp3.EventListener
import okhttp3.Interceptor
import retrofit2.CallAdapter
import retrofit2.Converter
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * 网络请求相关配置拓展类
 * * 用于kotlin dsl方式配置
 * @author yuPFeG
 * @date 2021/01/25
 */
class HttpRequestConfig{
    /**api域名*/
    @JvmField
    var baseUrl : String ?= null
    /**链接超时时间，单位s*/
    @JvmField
    var connectTimeout : Long = 10
    /**读取超时时间，单位s*/
    @JvmField
    var readTimeout : Long = 10
    /**写入上传的超时时间，单位s*/
    @JvmField
    var writeTimeout : Long = 15

    /**是否允许代理*/
    @JvmField
    var isAllowProxy : Boolean = false
    /**是否在连接失败后自动重试，默认为false，在外部自行处理重试逻辑*/
    @JvmField
    var isRetryOnConnectionFailure : Boolean = false

    /**
     * 网络响应的本地缓存
     * * `Cache(file = 缓存文件,size = 最大缓存大小)`
     * * Android 10以下版本需要本地读写权限
     * */
    @JvmField
    var responseCache : Cache ?= null

    /**应用层拦截器*/
    @JvmField
    val applicationInterceptors : MutableList<Interceptor> = mutableListOf()
    /**网络层拦截器*/
    @JvmField
    val networkInterceptors : MutableList<Interceptor> = mutableListOf()

    /**retrofit解析器的集合*/
    @JvmField
    val converterFactories : MutableList<Converter.Factory> = mutableListOf()
    /**retrofit回调支持类的集合*/
    @JvmField
    val callAdapterFactories : MutableList<CallAdapter.Factory> = mutableListOf()

    /**https的ssl证书校验配置*/
    @JvmField
    var sslSocketConfig : SSLSocketTrustConfig ?= null

    /**网络请求事件监听，可监听网络请求指标数据*/
    @JvmField
    var eventListenerFactory : EventListener.Factory? = null

    init {
        applicationInterceptors.apply {
            //默认添加动态切换baseUrl的拦截器
            add(MultipleHostInterceptor())
        }
        //json解析器与回调类型支持都交由外部决定添加内容
    }
}

/**https的ssl证书校验配置*/
data class SSLSocketTrustConfig(
    /**https的ssl证书校验，需与[x509TrustManager]同时设置才会生效*/
    var sslSocketFactory : SSLSocketFactory ?= null,
    /**https的ssl证书检验，需与[sslSocketFactory]同时设置才会生效*/
    var x509TrustManager : X509TrustManager ?= null
)





