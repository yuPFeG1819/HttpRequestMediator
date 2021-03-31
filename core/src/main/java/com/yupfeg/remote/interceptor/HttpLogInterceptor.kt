package com.yupfeg.remote.interceptor

import com.yupfeg.remote.BuildConfig
import com.yupfeg.remote.log.HttpLogPrinter
import okhttp3.*
import okio.Buffer
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 输出http请求日志的OkHttp网络层拦截器（在实际发出请求前拦截）
 * * 仅实现打印请求日志功能
 * @author yuPFeG
 * @date 2020/02/21
 */
class HttpLogInterceptor(private val logPrinter: HttpLogPrinter) : Interceptor{
    companion object{
        private val UTF8 = Charset.forName("UTF-8")
        private const val DEBUG_HTTP_LOG_TAG = "okHttp"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        //获取原始的Request
        val request = chain.request()
        val builder = request.newBuilder()
        val startNs = System.nanoTime()

        request.body?.contentType()?.charset(UTF8)
        //创建新网络请求(url等参数不需要重复添加)
        val newRequest = builder.build()
        if (BuildConfig.DEBUG) {
            //打印请求log
            printRequestDebugLog(newRequest, chain.connection())
        }
        //------------------请求响应返回------------------
        //执行网络请求，获取响应返回
        val response = chain.proceed(newRequest)
        val chainMs: Long = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        response.body?.contentType()?.charset(UTF8)

        if (BuildConfig.DEBUG) {
            //打印响应log
            printResponseDebugLog(response,chainMs)
        }
        return response
    }

    /**
     * 在Debug模式，打印请求日志信息
     * @param newRequest 请求
     * @param connection 请求连接类型
     *  */
    @Throws(IOException::class)
    private fun printRequestDebugLog(newRequest: Request, connection: Connection?) {
        StringBuilder().apply {
            val protocol = connection?.protocol() ?: Protocol.HTTP_1_1
            val requestLogTop = String.format(
                "Sending request %1\$s %2\$s , %3\$s",
                newRequest.method, newRequest.url, protocol.name
            )
            append(">>> START  ")
            append(newRequest.method)
            append("  Request \n")
            append("$requestLogTop \n")
            //请求头
            appendAllHeaders(newRequest.headers)
            //POST请求的body
            appendRequestBodyContent(newRequest.body)
            append("\n>>> END ${newRequest.method} Request")
            logPrinter.printDebugLog(DEBUG_HTTP_LOG_TAG, this.toString())
        }
    }

    /**
     * 在Debug模式，打印请求返回日志信息
     * @param response 请求响应返回
     * @param useTimeMillis 请求响应时间
     * */
    @Throws(IOException::class)
    private fun printResponseDebugLog(response: Response, useTimeMillis: Long) {
        StringBuilder().apply {
            val responseLogTop = String.format(
                "Received response %1\$s %2\$s %3\$s %4\$s",
                response.code, response.message,
                response.request.url,"( $useTimeMillis ms)"
            )
            append("<<< ${response.code} Response\n")
            append("${responseLogTop}\n")
            //请求响应header
            appendAllHeaders(response.headers)
            //请求响应body
            appendResponseBodyContent(response.body)
            append("\n <<<END HTTP")
            logPrinter.printDebugLog(DEBUG_HTTP_LOG_TAG, this.toString())
        }
    }

    /**
     * 添加所有请求头信息
     * @param headers 请求头
     * */
    private fun StringBuilder.appendAllHeaders(headers: Headers){
        for (i in 0 until headers.size){
            this.append("${headers.name(i)}  :  ${headers.value(i)}")
            if (i < headers.size-1) this.append("\n")
        }
    }

    /**
     * [StringBuilder]拓展函数，添加网络请求的body内容
     * @param body [RequestBody]
     * */
    private fun StringBuilder.appendRequestBodyContent(body : RequestBody?){
        body?:return

        append("\n")
        body.contentType()?.also {contentType->
            append("Content-Type : $contentType \n")
        }
        body.contentLength().takeIf { it > 0 }?.also {
            append("Content-Length : ${body.contentLength()} \n")
        }
        (body as? MultipartBody)?.also {
            //字节流body
            append(">>> MultipartBody START \n ")
            appendMultipartBodyContent(it)
            append(">>> MultipartBody END \n ")
        }?:run{
            append(">>> Body START \n ")
            append("${body.parseBodyToString()} \n")
            append(">>> Body END")
        }
    }

    /**
     * [StringBuilder]拓展函数，添加网络响应的body内容
     * @param body [ResponseBody]
     * */
    private fun StringBuilder.appendResponseBodyContent(body : ResponseBody?){
        body?:return
        //不能直接取body().toString(),否则会直接结束
        val source = body.source()
        source.request(Long.MAX_VALUE)
        val bodyString = source.buffer.clone().readString(UTF8)
        // Buffer the entire body.
        append("\n <<< Body START \n $bodyString \n <<< Body END")
    }

    /**
     * 添加[MultipartBody]类型的body内容
     * */
    private fun StringBuilder.appendMultipartBodyContent(body: MultipartBody){
        for (part in body.parts) {
            val requestBody = part.body
            if (requestBody.contentType()?.isPlainText() == true){
                //只打印文本类型的body内容
                append("${requestBody.parseBodyToString()} \n")
            }else{
                append("other-media-type")
                append("${requestBody.contentType()}")
                append("can not print to log \n")
            }
        }
    }

    /**[RequestBody]的拓展函数，解析body内容为字符串*/
    private fun RequestBody.parseBodyToString() : String{
        //不能直接取body().toString()
        val bodyBuffer = Buffer()
        this.writeTo(bodyBuffer)
        //将request body内的中文进行UTF-8转码，否则中文参数会显示乱码
        return try {
            URLDecoder.decode(bodyBuffer.readString(UTF8), "UTF-8")
        }catch (e: UnsupportedEncodingException){
            logPrinter.printErrorLog(DEBUG_HTTP_LOG_TAG,e)
            bodyBuffer.readString(UTF8)
        }
    }

    /**body内容是否为文本类型*/
    private fun MediaType.isPlainText(): Boolean {
        this.toString().takeIf { it.isNotEmpty() }?.also {
            val strType = it.toLowerCase(Locale.getDefault())
            if (strType.contains("text") || strType.contains("application/json")) {
                return true
            }
        }
        return false
    }

}