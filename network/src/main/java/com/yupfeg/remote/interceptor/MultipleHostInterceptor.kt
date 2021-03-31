package com.yupfeg.remote.interceptor

import com.yupfeg.remote.url.UrlRedirectHelper
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 动态替换多域名的拦截器
 * @author yuPFeG
 * @date 2021/03/08
 */
@Suppress("unused")
class MultipleHostInterceptor : Interceptor{

    override fun intercept(chain: Interceptor.Chain): Response {
        val redirectRequest = UrlRedirectHelper.obtainRedirectedUrlRequest(chain.request())
        return chain.proceed(redirectRequest)
    }

}