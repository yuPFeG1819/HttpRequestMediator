package com.yupfeg.remote.interceptor

import android.util.Log
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
 * 输出http请求日志的网络层拦截器.
 * （在实际发出请求前拦截）
 * * 仅实现打印请求日志功能
 * @author yuPFeG
 * @date 2020/02/21
 */
open class HttpLogInterceptor(
    private val logPrinter: HttpLogPrinter
) : Interceptor{
    companion object{
        private val UTF8 = Charset.forName("UTF-8")
        private const val DEBUG_HTTP_LOG_TAG = "okHttp"

        /**
         * 默认Logcat输出日志的最大长度
         * * ide的Logcat日志长度会有限度，超出4K长度会截断
         * * 尽可能调小了一点，在防止外部调用的日志输出框架自带的内容前缀，叠加后会超出长度限制
         * */
        private const val MAX_LOGCAT_STRING_LENGTH = 2 * 1000
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        //获取原始的Request
        val request = chain.request()
        val builder = request.newBuilder()
        val startNs = System.nanoTime()

        request.body?.contentType()?.charset(UTF8)
        //创建新网络请求(url等参数不需要重复添加)
        val newRequest = builder.build()
        //打印请求log
        printRequestDebugLog(newRequest, chain.connection())
        //执行网络请求，获取响应返回
        val response = chain.proceed(newRequest)
        val chainMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        response.body?.contentType()?.charset(UTF8)

        //打印响应log
        printResponseDebugLog(response,chainMs)
        return response
    }

    // <editor-fold desc="网络请求日志">

    /**
     * 在Debug模式下，打印请求日志信息
     * @param newRequest 请求
     * @param connection 请求连接类型
     *  */
    @Throws(IOException::class)
    protected open fun printRequestDebugLog(newRequest: Request, connection: Connection?) {
        StringBuilder().apply {
            val protocol = connection?.protocol() ?: Protocol.HTTP_1_1
            val requestUrl = decodeHttpRequestUrl(url = newRequest.url.toString())
            val requestLogTop = String.format(
                "Sending request %1\$s %2\$s , %3\$s",
                newRequest.method, requestUrl, protocol.name
            )
            append(">>> START  ")
            append(newRequest.method)
            append("Http Request \n")
            append("$requestLogTop \n")
            //请求头
            appendAllHeaders(newRequest.headers)
            //添加请求的body
            appendRequestBodyContent(newRequest.body)
            append("\n>>> END ${newRequest.method} Request")

            //日志输出长度超出限制时的日志接续前缀，仅用于区分接续日志所在的请求地址
            val continueRequestPrefix = String.format(
                ">>>Continue request %1\$s %2\$s , %3\$s \n" +
                        ">>> Continue Body \n",
                newRequest.method, requestUrl, protocol.name
            )
            //输出网络请求日志
            preparePrintLongLog(Log.DEBUG,continueRequestPrefix,this.toString())
        }
    }

    /**
     * [StringBuilder]拓展函数，添加post请求的body内容到StringBuilder
     * @param body [RequestBody]
     * @return true - body内容长度过多，输出到logcat需要
     * */
    protected open fun StringBuilder.appendRequestBodyContent(body : RequestBody?) : Boolean{
        body?:return false

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
        return isLogContentTooLong(this.toString())
    }

    /**
     * [StringBuilder]的拓展函数，添加[MultipartBody]类型的body内容
     * @param body [MultipartBody]
     * */
    protected open fun StringBuilder.appendMultipartBodyContent(body: MultipartBody){
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
    protected open fun RequestBody.parseBodyToString() : String{
        //不能直接取body().toString()
        val bodyBuffer = Buffer()
        this.writeTo(bodyBuffer)
        //将request body内的中文进行UTF-8转码，否则中文参数会显示乱码
        return try {
            URLDecoder.decode(bodyBuffer.readString(UTF8), "UTF-8")
        }catch (e: UnsupportedEncodingException){
            bodyBuffer.readString(UTF8)
        }
    }

    /**body内容是否为文本类型*/
    protected open fun MediaType.isPlainText(): Boolean {
        this.toString().takeIf { it.isNotEmpty() }?.also {
            val strType = it.toLowerCase(Locale.getDefault())
            if (strType.contains("text") || strType.contains("application/json")) {
                return true
            }
        }
        return false
    }

    // </editor-fold>

    // <editor-fold desc="网络响应日志">

    /**
     * 打印请求返回日志信息
     * @param response 请求响应返回
     * @param useTimeMillis 请求响应时间
     * */
    @Throws(IOException::class)
    protected open fun printResponseDebugLog(response: Response, useTimeMillis: Long) {
        StringBuilder().apply {
            val requestUrl = decodeHttpRequestUrl(url = response.request.url.toString())
            val responseLogTop = String.format(
                "<<< Received response %1\$s %2\$s %3\$s %4\$s",
                response.code, response.message,
                requestUrl,"( $useTimeMillis ms)"
            )
            append("<<< ${response.code} Response\n")
            append("${responseLogTop}\n")
            //请求响应header
            appendAllHeaders(response.headers)
            //请求响应body
            appendResponseBodyContent(response.body)
            append("\n <<<END HTTP")

            //日志输出长度超出限制时的日志接续前缀，仅用于区分接续日志的请求地址
            val continueResponsePrefix = String.format(
                "<<<Continue Received response %1\$s %2\$s %3\$s %4\$s \n" +
                        " <<< Continue Body \n",
                response.code, response.message,
                requestUrl,"( $useTimeMillis ms)"
            )
            //输出日志
            preparePrintLongLog(Log.DEBUG,continueResponsePrefix,this.toString())
        }
    }

    /**
     * [StringBuilder]拓展函数，添加网络响应的body内容
     * @param body [ResponseBody]
     * @return true - 超出logcat长度，需要分段输出日志
     * */
    protected open fun StringBuilder.appendResponseBodyContent(body : ResponseBody?){
        body?:return
        //不能直接取body().toString(),否则会直接结束
        val source = body.source()
        source.request(Long.MAX_VALUE)
        // Buffer the entire body.
        val bodyString = source.buffer.clone().readString(UTF8)
        append("\n <<< Body START \n $bodyString \n <<< Body END")
    }

    // </editor-fold>

    /**
     * [StringBuilder]的拓展函数，添加所有请求头信息
     * @param headers 请求头
     * */
    protected open fun StringBuilder.appendAllHeaders(headers: Headers){
        for (i in 0 until headers.size){
            this.append("${headers.name(i)}  :  ${headers.value(i)}")
            if (i < headers.size-1) this.append("\n")
        }
    }

    /**
     * 字符串长度是否超出限制
     * @param content 日志内容
     * */
    protected open fun isLogContentTooLong(content: String) : Boolean
            = content.length > MAX_LOGCAT_STRING_LENGTH

    /**
     * 解码网络请求url
     * * 将get请求中包含的参数解码，避免中文参数在日志输出内为乱码
     * @param url 原始网络请求地址
     * @param decodeFormat 解码格式，默认为UTF-8
     * @return 解码后的网络请求地址，仅用于日志输出
     */
    protected open fun decodeHttpRequestUrl(url : String,decodeFormat : String = "UTF-8") : String{
        return try {
            URLDecoder.decode(url, "UTF-8")
        }catch (e: UnsupportedEncodingException){
            url
        }
    }

    // <editor-fold desc="输出日志">

    /**
     * 准备输出长日志
     * * 支持打印长日志（Logcat有最大单次输出长度限制）
     * @param logLevel 日志等级
     * @param continuePrefix 日志分段接续的内容前缀，默认为“”，通常仅在输出日志内容太长时，
     * @param content 日志内容
     */
    @Suppress("SameParameterValue")
    protected open fun preparePrintLongLog(
        logLevel : Int,
        continuePrefix : String = "",
        content: String
    ) {
        if (!isLogContentTooLong(content)){
            printHttpLog(logLevel, content)
            return
        }
        val logLength = content.length
        var length = 0
        while (length < logLength) {
            if (length + MAX_LOGCAT_STRING_LENGTH < content.length) {
                if (length==0) {
                    //第一段日志
                    val printContent = content.substring(length, length + MAX_LOGCAT_STRING_LENGTH)
                    printHttpLog(logLevel,printContent)
                } else {
                    //接续的分段日志
                    val subString = content.substring(length, length + MAX_LOGCAT_STRING_LENGTH)
                    val continueLogContent = "${continuePrefix}${subString}"
                    printHttpLog(logLevel, continueLogContent)
                }
            } else {
                //接续的分段日志
                val logContent = "${continuePrefix}${content.substring(length, content.length)}"
                printHttpLog(logLevel, logContent)
            }
            length += MAX_LOGCAT_STRING_LENGTH
        }
    }

    /**
     * 输出网络请求日志
     * @param logLevel 日志等级
     * @param content 日志内容
     */
    protected open fun printHttpLog(logLevel: Int, content: String){
        if(!logPrinter.isPrintLog) return

        when(logLevel) {
            Log.ERROR -> logPrinter.printErrorLog(DEBUG_HTTP_LOG_TAG, content)
            Log.WARN  -> logPrinter.printWarningLog(DEBUG_HTTP_LOG_TAG, content)
            Log.INFO  -> logPrinter.printInfoLog(DEBUG_HTTP_LOG_TAG,content)
            else -> logPrinter.printDebugLog(DEBUG_HTTP_LOG_TAG,content)
        }
    }

}