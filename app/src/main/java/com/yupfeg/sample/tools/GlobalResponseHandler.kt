package com.yupfeg.sample.tools

import com.google.gson.JsonParseException
import com.yupfeg.remote.data.HttpResponseParsable
import com.yupfeg.remote.tools.handler.HttpResponseHandler
import com.yupfeg.remote.tools.handler.RestApiException
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.ParseException
import java.util.concurrent.TimeoutException

/**
 * 业务层的全局http响应处理类
 * @author yuPFeG
 * @date 2021/10/07
 */
class GlobalResponseHandler : HttpResponseHandler{
    companion object{
        /**
         * 响应成功
         */
        const val SUCCESS = 0
        /**
         * Token 过期
         */
        const val TOKEN_INVALID = 401
        /**
         * 未知错误
         */
        const val UNKNOWN_ERROR = 1002
        /**
         * 服务器内部错误
         */
        const val SERVER_ERROR = 1003
        /**
         * 网络连接超时
         */
        const val NETWORK_ERROR = 1004
        /**
         * API解析异常（或者第三方数据结构更改）等其他异常
         */
        const val API_ERROR = 1005
    }

    override fun handleHttpResponse(responseBean: HttpResponseParsable): Boolean {
        return when(responseBean.code){
            SUCCESS -> true
            else -> false
        }
    }

    /**
     * 后台接口在出现错误时，可能会返回与正常接口不同的body格式，需要额外解析body时会调用该方法
     * * 通常为后台接口框架封装时存在
     * @param bodyString 请求执行异常时返回的json字符串
     * */
    override fun handleHttpErrorBody(bodyString: String) {
        //TODO 解析body内的json字符串
    }

    /**
     * 预处理网络请求出现的异常
     * @param throwable
     * */
    override fun handleHttpError(throwable: Throwable) {
        when(throwable){
            is RestApiException ->{
                //业务执行异常
                throwable.processApiError()
            }
            is SocketTimeoutException,is TimeoutException->{
                //网络请求超时
                throwable.printStackTrace()
            }
            is HttpException,is ConnectException->{
                //网路连接异常
                throwable.printStackTrace()
            }
            is JsonParseException,is JSONException,is ParseException ->{
                //json解析异常
                throwable.printStackTrace()
            }
            is UnknownHostException->{
                //无法连接到服务器
                throwable.printStackTrace()
            }
            is IllegalArgumentException->{
                //参数错误
                throwable.printStackTrace()
            }
            else->{
                //其他位置错误
                throwable.printStackTrace()
            }
        }
    }

    /**
     * [RestApiException]的拓展函数，处理业务请求执行错误
     * */
    private fun RestApiException.processApiError(){
        when(errorCode){
            //token失效
            TOKEN_INVALID ->{
                //清空用户本地缓存
                //TODO 跳转到登录页
            }
            else->{
                //其他操作
            }
        }
    }

}