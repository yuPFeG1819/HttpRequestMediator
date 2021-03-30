package com.yupfeg.remote.delegator

import com.yupfeg.remote.HttpRequestMediator
import retrofit2.Retrofit
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * 网络请求api的委托基类
 * * 外部调用通过by关键字委托创建api接口实例
 * * 在外部调用时通常是val修饰的不可重复赋值对象，故只重载了getValue()方法
 *
 * 子类应实现[addHttpRequestConfig]函数，配置对应key的请求配置
 * @author yuPFeG
 * @date 2021/03/24
 */
@Suppress("unused")
abstract class BaseRequestApiDelegator<out T>(
    private val clazz: Class<T>,
    private val clientKey : String = HttpRequestMediator.DEFAULT_CLIENT_KEY
) : ReadOnlyProperty<Any,T>{
    @Volatile
    private var mApiService : T? = null

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return mApiService ?: synchronized(this){
            mApiService ?: obtainRetrofitInstance().create(clazz).apply { mApiService = this }
        }
    }

    /**
     * 尝试获取对应配置的网络请求client
     * */
    protected open fun obtainRetrofitInstance() : Retrofit {
        takeUnless { HttpRequestMediator.containsRequestKey(clientKey) }
            ?.also {
                //未添加过指定请求配置，需要先添加该配置
                addHttpRequestConfig(clientKey)
            }
        return HttpRequestMediator.getRetrofitInstance()
    }

    /**
     * 添加网络请求配置
     * @param configKey 配置标识符，标识特定配置的网络请求client
     * */
    abstract fun addHttpRequestConfig(configKey: String)

}