package com.yupfeg.remote.factory

import com.yupfeg.remote.config.HttpRequestConfig
import okhttp3.*
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.NullPointerException
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * 创建网络请求client对象的工厂基类
 * @author yuPFeG
 * @date 2021/02/09
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseHttpClientFactoryImpl : HttpClientFactory {

    // <editor-fold desc="网络请求配置变量">
    /**api的url域名*/
    protected var mBaseUrl : String = ""

    /**链接超时时间，单位s*/
    protected var mConnectTimeout : Long = 10
    /**读取超时时间，单位s*/
    protected var mReadTimeout : Long = 15
    /**写入超时时间，单位s*/
    protected var mWriteTimeout : Long = 15

    /**是否允许使用代理访问*/
    protected var mAllowProxy : Boolean = true
    /**是否允许自动重试*/
    protected var isRetryOnConnectionFailure : Boolean = false

    /**应用层拦截器(先接收到请求，后接收到响应)集合，按顺序添加*/
    protected val mInterceptors : MutableList<Interceptor> = mutableListOf()
    /**网络层拦截器(先接收到响应，后接收到请求)集合，按顺序添加*/
    protected val mNetworkInterceptors : MutableList<Interceptor> = mutableListOf()

    /**retrofit解析器的集合*/
    protected val mConverterFactories : MutableList<Converter.Factory> = mutableListOf()
    /**retrofit回调支持类的集合*/
    protected var mCallAdapterFactories : MutableList<CallAdapter.Factory> = mutableListOf()

    protected var mSSLSocketFactory : SSLSocketFactory ?= null
    protected var mX509TrustManager : X509TrustManager ?= null

    /**网络响应的本地缓存*/
    protected var mResponseFileCache : Cache?= null

    /**网络请求数据指标的监听*/
    protected var mEventListenerFactory : EventListener.Factory? = null

    /**
     * okhttp异步网络请求任务调度器
     * * 默认使用全局的网络请求线程池
     * */
    protected var mRequestDispatcher : Dispatcher? = null

    // </editor-fold>

    /**retrofit实例*/
    @Volatile
    protected var mRetrofit : Retrofit ?= null

    /**okHttp实例*/
    @Volatile
    protected var mOkHttpClient : OkHttpClient ?= null

    /**
     * 执行配置网络请求工厂类属性操作
     * * 便于使用kotlin DSL方式配置
     * @param config [HttpRequestConfig]
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    protected open fun performRetrofitConfig(config: HttpRequestConfig) : BaseHttpClientFactoryImpl {
        if (config.baseUrl.isNullOrEmpty()){
            throw NullPointerException("you must set baseUrl to HttpClientFactory")
        }

        return this.setBaseUrl(config.baseUrl?:"")
            .setResponseFileCache(config.responseCache)
            .setRetryOnConnectionFailure(config.isRetryOnConnectionFailure)
            .setConnectTimeout(config.connectTimeout)
            .setReadTimeout(config.readTimeout)
            .setWriteTimeout(config.writeTimeout)
            .setAllowProxy(config.isAllowProxy)
            .addInterceptors(config.applicationInterceptors)
            .addNetworkInterceptors(config.networkInterceptors)
            .addConverterFactories(config.converterFactories)
            .addCallAdapterFactories(config.callAdapterFactories)
            .setSSLSocketFactory(config.sslSocketConfig?.sslSocketFactory)
            .setX509TrustManager(config.sslSocketConfig?.x509TrustManager)
            .setEventListenerFactory(config.eventListenerFactory)
    }

    /**
     * 创建[OkHttpClient]实例对象
     */
    protected open fun performCreateOkHttpClient() : OkHttpClient{
        val builder = OkHttpClient.Builder()
        //缓存
        mResponseFileCache?.also { builder.cache(it) }
        //应用层拦截器
        mInterceptors.takeUnless { it.isNullOrEmpty() }?.forEach { interceptor->
            builder.addInterceptor(interceptor)
        }
        //网络层拦截器
        mNetworkInterceptors.takeUnless { it.isNullOrEmpty() }?.forEach{ interceptor ->
            builder.addNetworkInterceptor(interceptor)
        }
        // <editor-fold desc="超时时间配置">
        //设置连接时间
        builder.connectTimeout(mConnectTimeout, TimeUnit.SECONDS)
            //设置读取数据时间
            .readTimeout(mReadTimeout, TimeUnit.SECONDS)
            //设置写入（上传）时间
            .writeTimeout(mWriteTimeout, TimeUnit.SECONDS)

        // </editor-fold>

        // <editor-fold desc="网络优化相关">

        //网络性能监听
        mEventListenerFactory?.also {
            builder.eventListenerFactory(it)
        }

        // </editor-fold>

        // <editor-fold desc="其他杂项">
        //代理访问权限
        takeIf { !mAllowProxy }?.run { builder.proxy(Proxy.NO_PROXY) }

        //ssl证书校验
        if (mSSLSocketFactory != null && mX509TrustManager != null){
            builder.sslSocketFactory(mSSLSocketFactory!!,mX509TrustManager!!)
        }
        //自动断线重连（默认为false）
        builder.retryOnConnectionFailure(isRetryOnConnectionFailure)
        // </editor-fold>

        return builder.build()
    }

    /**
     * 创建Retrofit实例
     * @param okHttpClient [OkHttpClient]实例
     */
    protected fun performCreateRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val builder = Retrofit.Builder()
        builder.baseUrl(mBaseUrl)
        //------添加解析器(按添加顺序尝试解析)---------
        mConverterFactories.takeUnless { it.isNullOrEmpty() }?.forEach { converter->
            builder.addConverterFactory(converter)
        }
        //------添加响应回调支持----------
        mCallAdapterFactories.takeIf { it.size > 0 }?.forEach { callAdapter->
            builder.addCallAdapterFactory(callAdapter)
        }
        //okhttp实例
        builder.client(okHttpClient)
        return builder.build()
    }

    //<editor-fold desc="配置方法">

    /**
     * 设置网络请求的域名
     * @param url 网络请求域名
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun setBaseUrl(url : String) : BaseHttpClientFactoryImpl {
        this.mBaseUrl = url
        return this
    }

    /**
     * 设置连接超时时间
     * @param timeout 超时时间，单位s
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     * */
    fun setConnectTimeout(timeout : Long) : BaseHttpClientFactoryImpl {
        this.mConnectTimeout = timeout
        return this
    }

    /**
     * 设置读取的超时时间
     * @param timeout 超时时间，单位s
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun setReadTimeout(timeout: Long) : BaseHttpClientFactoryImpl {
        this.mReadTimeout = timeout
        return this
    }

    /**
     * 设置写入的超时时间
     * @param timeout 超时时间，单位s
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun setWriteTimeout(timeout: Long) : BaseHttpClientFactoryImpl {
        this.mWriteTimeout = timeout
        return this
    }

    /**
     * 设置是否允许使用代理访问
     * @param isAllow 是否允许代理访问
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     * */
    fun setAllowProxy(isAllow : Boolean) : BaseHttpClientFactoryImpl {
        this.mAllowProxy = isAllow
        return this
    }

    /**
     * 设置okhttp应用层拦截器
     * @param interceptors okhttp拦截器[Interceptor]的集合，按顺序添加
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun addInterceptors(interceptors : List<Interceptor>?) : BaseHttpClientFactoryImpl {
        interceptors?.also { this.mInterceptors.addAll(it) }
        return this
    }

    /**
     * 添加okhttp应用层拦截器
     * @param interceptor okhttp拦截器[Interceptor]
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    @Suppress("unused")
    fun addInterceptor(interceptor: Interceptor?) : BaseHttpClientFactoryImpl {
        interceptor?.also { this.mInterceptors.add(it) }
        return this
    }

    /**
     * 设置okhttp网络层拦截器
     * @param interceptors okhttp拦截器[Interceptor]的集合，按顺序添加
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */

    fun addNetworkInterceptors(interceptors : List<Interceptor>?) : BaseHttpClientFactoryImpl {
        interceptors?.also { this.mNetworkInterceptors.addAll(it) }
        return this
    }

    /**
     * 添加okhttp网络层拦截器
     * @param interceptor okhttp拦截器[Interceptor]
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    @Suppress("unused")
    fun addNetworkInterceptor(interceptor: Interceptor?) : BaseHttpClientFactoryImpl {
        interceptor?.also { this.mNetworkInterceptors.add(it) }
        return this
    }

    /**
     * 添加Retrofit的解析器
     * @param converterFactories 解析器工厂类[Converter.Factory]的集合
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun addConverterFactories(
        converterFactories : List<Converter.Factory>?
    ) : BaseHttpClientFactoryImpl {
        converterFactories ?: return this
        this.mConverterFactories.addAll(converterFactories)
        return this
    }

    /**
     * 添加请求响应回调支持
     * * 默认已添加了RxJava3的支持，不需要重复添加
     * @param callAdapterFactories 响应回调支持类[CallAdapter.Factory]的集合
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun addCallAdapterFactories(
        callAdapterFactories : List<CallAdapter.Factory>?
    ) : BaseHttpClientFactoryImpl {
        callAdapterFactories ?: return this
        this.mCallAdapterFactories.addAll(callAdapterFactories)
        return this
    }

    /**
     * 设置是否断线重连
     *  @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     * */
    fun setRetryOnConnectionFailure(autoRetry : Boolean) : BaseHttpClientFactoryImpl {
        this.isRetryOnConnectionFailure = autoRetry
        return this
    }

    /**
     * 设置ssl证书校验
     * * 需要同时设置[setX509TrustManager]
     * @param factory
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     * */
    fun setSSLSocketFactory(factory : SSLSocketFactory?) : BaseHttpClientFactoryImpl {
        factory ?: return this
        this.mSSLSocketFactory = factory
        return this
    }

    /**
     * 设置x509证书校验
     * * 需要同时设置[setSSLSocketFactory]
     * @param manager
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     * */
    fun setX509TrustManager(manager: X509TrustManager?) : BaseHttpClientFactoryImpl {
        manager ?: return this
        this.mX509TrustManager = manager
        return this
    }

    /**
     * 设置网络请求的响应文件缓存
     * @param cache
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     * */
    fun setResponseFileCache(cache: Cache?) : BaseHttpClientFactoryImpl {
        cache ?: return this
        this.mResponseFileCache = cache
        return this
    }

    /**
     * 设置http请求指标监控
     * @param eventListenerFactory 监听的工程类，根据网络请求地址key创建对应的监听对象
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    fun setEventListenerFactory(eventListenerFactory : EventListener.Factory?) : BaseHttpClientFactoryImpl{
        eventListenerFactory ?: return this
        this.mEventListenerFactory = eventListenerFactory
        return this
    }

    /**
     * 设置网络请求任务调度器
     * @param dispatcher
     * @return [BaseHttpClientFactoryImpl]类本身，便于链式调用
     */
    @Suppress("unused")
    fun setDispatcher(dispatcher: Dispatcher?) : BaseHttpClientFactoryImpl{
        dispatcher?:return this
        this.mRequestDispatcher = dispatcher
        return this
    }
    //</editor-fold>
}