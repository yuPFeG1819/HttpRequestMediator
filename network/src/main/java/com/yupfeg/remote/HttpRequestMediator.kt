package com.yupfeg.remote

import com.yupfeg.remote.config.HttpRequestConfig
import com.yupfeg.remote.factory.DefaultHttpClientFactoryImpl
import com.yupfeg.remote.factory.HttpClientFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.lang.NullPointerException
import kotlin.jvm.Throws

/**
 * [Retrofit]网络请求的核心管理类
 *
 * * 使用流程：
 *
 * 1.先通过[addDefaultHttpClientFactory]或者[addHttpClientFactory]添加网络请求的工厂对象。
 *
 * 2.调用[getRetrofitInstance]获取[Retrofit]实例，来创建请求api对象，
 * 或者直接调用[createRequestApi]创建请求api对象
 *
 * @author yuPFeG
 * @date 2021/01/25
 */
@Suppress("unused")
object HttpRequestMediator {
    const val DEFAULT_CLIENT_KEY = "defaultClient"
    const val DEFAULT_DOWNLOAD_CLIENT_KEY = "defaultDownloadClient"

    /**创建网络请求client对象的工厂类集合*/
    private val mHttpClientFactories : HashMap<String, HttpClientFactory> = HashMap()

    /**
     * 获取Retrofit实例
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * */
    @JvmStatic
    @Throws(NullPointerException::class,IllegalAccessException::class)
    fun getRetrofitInstance(configKey : String = DEFAULT_CLIENT_KEY) : Retrofit{
        return fetchHttpClientFactory(configKey).getRetrofitInstance()
    }

    /**
     * 获取OkHttpClient实例
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * */
    @JvmStatic
    @Throws(NullPointerException::class,IllegalAccessException::class)
    fun getOkHttpInstance(configKey: String = DEFAULT_CLIENT_KEY) : OkHttpClient{
        return fetchHttpClientFactory(configKey).getOkHttpClientInstance()
    }

    /**
     * 构建Retrofit的API接口动态代理实例
     * * 内部调用[Retrofit.create]
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * @param clazz retrofit代理接口类
     * @param <T> 接口class实例，代理请求
     */
    @Suppress("unused")
    @JvmStatic
    @Throws(NullPointerException::class,IllegalAccessException::class)
    fun <T> createRequestApi(configKey : String = DEFAULT_CLIENT_KEY, clazz : Class<T>) : T{
        return getRetrofitInstance(configKey).create(clazz)
    }

    /**
     * 是否包含指定的配置标识符
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * @return
     */
    @JvmStatic
    fun containsRequestKey(configKey: String) : Boolean{
        return mHttpClientFactories.containsKey(configKey)
    }

    /**
     * 添加默认实现的网络请求工厂类
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * @param init 用于kotlin DSL方式配置网络请求参数[HttpRequestConfig]
     * */
    @JvmStatic
    fun addDefaultHttpClientFactory(configKey: String = DEFAULT_CLIENT_KEY,
                                    init : HttpRequestConfig.()->Unit) : HttpRequestMediator {
        mHttpClientFactories[configKey] = DefaultHttpClientFactoryImpl.create(init)
        return this
    }

    /**
     * 添加默认实现的网络请求工厂类
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * @param config 网络请求参数配置[HttpRequestConfig]
     * */
    @JvmStatic
    fun addDefaultHttpClientFactory(configKey: String = DEFAULT_CLIENT_KEY,
                                    config: HttpRequestConfig) : HttpRequestMediator{
        mHttpClientFactories[configKey] = DefaultHttpClientFactoryImpl.create(config)
        return this
    }

    /**
     * 添加网络请求工厂
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * @param factory [HttpClientFactory]实现类，网络请求配置工厂类对象
     * */
    @JvmStatic
    fun addHttpClientFactory(configKey: String = DEFAULT_CLIENT_KEY,
                             factory: HttpClientFactory
    ) : HttpRequestMediator {
        mHttpClientFactories[configKey] = factory
        return this
    }

    /**
     * 尝试获取对应key的网络请求配置工厂
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * @return
     */
    @Throws(NullPointerException::class,IllegalAccessException::class)
    private fun fetchHttpClientFactory(
        configKey : String = DEFAULT_CLIENT_KEY
    ) : HttpClientFactory {
        if (mHttpClientFactories.containsKey(configKey)){
            return mHttpClientFactories[configKey]
                ?: throw NullPointerException(
                    "Http Client is Null，you should add clientFactory before use"
                )
        }
        throw IllegalAccessException("You must add http client Factory")
    }

}