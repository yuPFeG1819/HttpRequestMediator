package com.yupfeg.remote.config

import com.google.gson.GsonBuilder
import com.yupfeg.remote.interceptor.MultipleHostInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
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
    var baseUrl : String ?= null
    /**链接超时时间，单位s*/
    var connectTimeout : Long = 5
    /**读取超时时间，单位s*/
    var readTimeout : Long = 10
    /**写入上传的超时时间，单位s*/
    var writeTimeout : Long = 15

    /**是否允许代理*/
    var isAllowProxy : Boolean = false
    /**是否在连接失败后自动重试，默认为false，在外部自行处理重试逻辑*/
    var isRetryOnConnectionFailure : Boolean = false

    /**
     * 网络响应的本地缓存
     * * `Cache(file = 缓存文件,size = 最大缓存大小)`
     * */
    var responseCache : Cache ?= null

    /**应用层拦截器*/
    var applicationInterceptors : MutableList<Interceptor> = mutableListOf()
    /**网络层拦截器*/
    var networkInterceptors : MutableList<Interceptor> = mutableListOf()

    /**retrofit解析器的集合*/
    var converterFactories : MutableList<Converter.Factory> = mutableListOf()
    /**retrofit回调支持类的集合*/
    var callAdapterFactories : MutableList<CallAdapter.Factory> = mutableListOf()

    /**https的ssl证书校验配置*/
    var sslSocketConfig : SSLSocketTrustConfig ?= null

    init {
        applicationInterceptors.apply {
            //默认添加动态切换baseUrl的拦截器
            add(MultipleHostInterceptor())
        }

        //json解析器（按添加顺序尝试解析）
        converterFactories.apply {
            //gson解析
            add(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            //fastjson解析
            //add(FastJsonConverterFactory.create())
        }
        //请求响应回调支持
        callAdapterFactories.apply {
            // 添加对RxJava3支持，利用RxJava3对事件流进行操作
            // (create()表示执行同步请求，createAsync()执行异步请求，交由okHttp线程池维护)
            add(RxJava3CallAdapterFactory.create())
        }
    }
}

/**https的ssl证书校验配置*/
data class SSLSocketTrustConfig(
    /**https的ssl证书校验，需与[x509TrustManager]同时设置才会生效*/
    var sslSocketFactory : SSLSocketFactory ?= null,
    /**https的ssl证书检验，需与[sslSocketFactory]同时设置才会生效*/
    var x509TrustManager : X509TrustManager ?= null
)





