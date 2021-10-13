package com.yupfeg.remote.interceptor

import com.yupfeg.remote.download.DownProgressResponseBody
import com.yupfeg.remote.download.entity.DownloadProgressBean
import com.yupfeg.remote.log.HttpLogPrinter
import okhttp3.Interceptor
import okhttp3.Response
import java.lang.StringBuilder

/**
 * 监听下载网络文件进度的okHttp拦截器
 * @author yuPFeG
 * @date 2020/04/23
 */
class DownloadProgressInterceptor(
    private val logPrinter : HttpLogPrinter?,
    private val onProgressChange : ((DownloadProgressBean)->Unit)?
) : Interceptor{
    companion object{
        private const val DEBUG_HTTP_LOG_TAG = "okHttp_download"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val builder = StringBuilder().apply {
            append("<<< Download Response Start \n")
            append(String.format(
                "Received response %1\$s %2\$s %3\$s ",
                response.code, response.message,
                response.request.url
            ))
            append("<<< Download Response End \n")
        }

        logPrinter?.printDebugLog(DEBUG_HTTP_LOG_TAG, builder.toString())
        response.body ?: return response
        return response.newBuilder()
            .body(
                DownProgressResponseBody(
                    fileUrl = response.request.url.toString(),
                    responseBody = response.body!!,
                    logPrinter = logPrinter,
                    onProgressChangeAction = onProgressChange
                )
            ).build()

    }
}