package com.yupfeg.remote.tools.handler

import com.yupfeg.remote.data.HttpResponseParsable
import okhttp3.ResponseBody
import java.lang.Exception
import java.nio.charset.Charset

/**
 * 全局的网络请求响应返回统一处理类
 *
 * 通常用于设置一些全局网络请求的统一判断
 * * 通过`setResultHandler`方法设置处理类
 * @author yuPFeG
 * @date 2021/09/22
 */
object GlobalHttpResponseProcessor {

    private var mHttpResultHandler : HttpResponseHandler? = null

    /**
     * 设置网络请求返回的统一处理类
     * * 交由业务层实现具体逻辑
     * @param handler
     * */
    @JvmStatic
    fun setResponseHandler(handler: HttpResponseHandler){
        mHttpResultHandler = handler
    }

    /**
     * 预处理网络请求返回body
     * @param responseBean 请求返回的body解析后的实体类
     * @return true - 表示请求正常执行，false - 请求执行异常，需要执行错误流程
     * * 推荐在该函数返回false时，抛出异常如`RestApiException`,然后统一在`handleHttpResult`处理
     * */
    @JvmStatic
    fun <T : HttpResponseParsable> preHandleHttpResponse(responseBean : T) : Boolean{
        return mHttpResultHandler?.handleHttpResponse(responseBean)?:true
    }

    /**
     * 解析网络请求执行异常时返回的body信息
     * * 网络接口在网络请求执行异常时，可能会返回另一套与正常执行网络请求不同的接口参数（通常为框架封装），
     * 此时最好调用该方法去解析请求
     * @param responseBody 接口异常返回body内容
     *
     * */
    @Suppress("unused")
    @JvmStatic
    fun handleHttpErrorBody(responseBody: ResponseBody){
        try {
            //不能直接取body().toString(),否则会直接结束
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val bodyString = source.buffer.clone().readString(
                Charset.forName("UTF-8")
            )
            mHttpResultHandler?.handleHttpErrorBody(bodyString)
        }catch (e : Exception){
            mHttpResultHandler?.handleHttpError(e)
        }
    }

    /**
     * 全局统一处理网络请求的异常
     * @param throwable 网络请求的异常
     */
    @JvmStatic
    fun handleHttpError(throwable: Throwable){
        mHttpResultHandler?.handleHttpError(throwable)
    }
}