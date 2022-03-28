package com.yupfeg.remote.config

import com.yupfeg.remote.interceptor.MultipleHostInterceptor
import okhttp3.*
import retrofit2.CallAdapter
import retrofit2.Converter
import java.util.concurrent.ExecutorService
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * 网络请求相关配置拓展类
 * * 用于kotlin dsl方式配置
 * @author yuPFeG
 * @date 2021/01/25
 */

@Suppress("unused")
class HttpRequestConfig constructor(){

    // <editor-fold desc="retrofit配置">
    /**api域名*/
    @JvmField
    var baseUrl : String ?= null

    /**retrofit解析器的集合*/
    @JvmField
    var converterFactories : MutableList<Converter.Factory> = mutableListOf()
    /**retrofit回调支持类的集合*/
    @JvmField
    var callAdapterFactories : MutableList<CallAdapter.Factory> = mutableListOf()

    // </editor-fold>

    // <editor-fold desc="okhttp配置">
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

    /**
     * 是否在连接失败后自动重试，默认为false，
     * - 建议在外部自行处理重试逻辑，如果设置为true，最多重试20次，包含重定向等次数
     * */
    @JvmField
    var isRetryOnConnectionFailure : Boolean = false
    /**
     * 是否在code = 300 、301 、302 、303 、307 、308 的情况下，自动进行重定向操作，默认为true。
     * - 推荐在外部自行处理重定向逻辑，默认最多支持20次，包含连接错误次数
     * */
    @JvmField
    var isFollowRedirects : Boolean = true
    /**
     * 网络响应的本地缓存
     * * 如`Cache(file = 缓存文件,size = 最大缓存字节大小)`
     * * Android 10以下版本需要本地读写权限
     * */
    @JvmField
    var responseCache : Cache ?= null

    /**应用层拦截器*/
    @JvmField
    var applicationInterceptors : MutableList<Interceptor> = mutableListOf()
    /**网络层拦截器*/
    @JvmField
    var networkInterceptors : MutableList<Interceptor> = mutableListOf()

    /**https的ssl证书校验配置*/
    @JvmField
    var sslSocketConfig : SSLSocketTrustConfig ?= null

    /**
     * 持久化Cookie的工具
     * - 在`OkHttp`内置的`BridgeInterceptor`拦截器内被调用，避免外部手动添加拦截器进行缓存
     * */
    @JvmField
    var cookieJar : CookieJar? = null

    /**
     * 最大并发请求数
     * */
    @JvmField
    var maxRequestSize : Int = 64

    /**
     * 设置同一Host请求的最大并发数
     * */
    @JvmField
    var maxRequestsPerHost : Int = 5

    /**
     * 外部设置调度器
     * */
    @JvmField
    var executorService : ExecutorService? = null

    /**
     * okhttp配置，如果需要更多拓展配置需要可以直接传入build对象
     * * 优先级默认最高，忽略所有其他okhttp快捷配置
     * */
    @JvmField
    var okhttpClientBuilder : OkHttpClient.Builder?= null

    // </editor-fold>

    constructor(okhttpBuilder : OkHttpClient.Builder) : this(){
        okhttpClientBuilder = okhttpBuilder
    }

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





