package com.yupfeg.remote.tools.handler

import com.yupfeg.remote.data.HttpResponseParsable

/**
 * 网络请求的响应内容处理器接口声明
 * * 由业务层实现具体逻辑，统一处理全局的网络响应，如判断接口执行状态，出现异常的处理
 * @author yuPFeG
 * @date 2021/09/16
 */
interface HttpResponseHandler {

    /**
     * 预处理网络请求返回
     * @param responseBean 网络接口返回最外层的bean类
     * */
    fun handleHttpResponse(responseBean : HttpResponseParsable) : Boolean

    /**
     * 后台接口在出现错误时，可能会返回与正常接口不同的body格式，需要另外解析body时会调用该方法
     * @param bodyString 请求执行异常时返回的json字符串
     * */
    fun handleHttpErrorBody(bodyString : String)

    /**
     * 预处理网络请求出现的异常
     * @param throwable
     * */
    fun handleHttpError(throwable: Throwable)
}