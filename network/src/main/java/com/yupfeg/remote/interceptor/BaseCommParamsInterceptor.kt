package com.yupfeg.remote.interceptor

import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.nio.charset.Charset
import java.util.HashMap

/**
 * 添加公共请求参数与请求头的应用层拦截器基类
 * @author yuPFeG
 * @date 2021/01/25
 */
@Suppress("unused")
abstract class BaseCommParamsInterceptor : Interceptor{
    companion object{
        private val UTF8 = Charset.forName("UTF-8")
    }

    /**
     * 网络请求的公共请求头
     */
    abstract val commHeaders : Map<String, String>?

    /**
     * post请求的body公共参数
     * @return 公共参数map
     */
    abstract val commBodyParams : Map<String, String>?

    override fun intercept(chain: Interceptor.Chain): Response {
        //获取原始的Request
        val request = chain.request()
        val builder = request.newBuilder()
        //---------添加公共的请求头--------------
        commHeaders?.takeIf {it.isNotEmpty()}?.also { headers->
            for ((key, value) in headers) {
                //添加请求头
                builder.addHeader(key, value)
            }
        }
        //-------------添加公用的参数------------------
        addPostBasicParamsToBuilder(request, builder)

        request.body?.contentType()?.charset(UTF8)

        return chain.proceed(builder.build())
    }

    /**
     * 添加Post公共参数到请求构造体
     * * 只适用于FormBody
     * @param oldRequest 原始请求
     * @param builder   新请求构造体
     */
    protected open fun addPostBasicParamsToBuilder(oldRequest: Request, builder: Request.Builder) {
        //POST请求表单提交(判断非空)
        val oldFormBody = oldRequest.body as? FormBody
        oldFormBody?:return
        //获取公共固定参数
        commBodyParams?.takeIf {map->
            //如果没有固定参数，则直接发出原始请求
            map.isNotEmpty()
        }?.also { params->
            val bodySize = oldFormBody.size
            val paramsMap = HashMap<String, String>(bodySize + 1)
            //原始请求body参数
            for (i in 0 until bodySize) {
                paramsMap[oldFormBody.name(i)] = oldFormBody.value(i)
            }
            paramsMap.putAll(params)
            val bodyBuilder = FormBody.Builder()
            for ((key, value) in paramsMap) {
                //重新添加所有参数到body中
                bodyBuilder.add(key, value)
            }
            val newBody = bodyBuilder.build()
            //请求头‘Content-Length’字段长度必须为body的长度，否则太短会被截断，太长会超时
            builder.header("Content-Length", newBody.contentLength().toString() + "")
                //添加新的参数body到请求构造体
                .method(oldRequest.method, newBody)
        }
    }
}