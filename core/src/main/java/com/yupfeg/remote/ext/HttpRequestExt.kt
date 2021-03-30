package com.yupfeg.remote.ext

import com.yupfeg.remote.HttpRequestMediator
import com.yupfeg.remote.config.HttpRequestConfig

/**
 * 使用kotlin DSL方式添加网络请求参数配置
 * @param tag 网络请求配置的标识符，用于获取特定配置的网络请求client，
 * 默认为[HttpRequestMediator.DEFAULT_CLIENT_KEY]，相同tag会覆盖原有配置
 * @param init 以[HttpRequestConfig]为接收对象的函数，初始化网络参数配置
 */
@Suppress("unused")
fun addDslRemoteConfig(tag : String = HttpRequestMediator.DEFAULT_CLIENT_KEY,
                       init : HttpRequestConfig.()->Unit) : HttpRequestMediator {
    return HttpRequestMediator.addDefaultHttpClientFactory(tag, init)
}
