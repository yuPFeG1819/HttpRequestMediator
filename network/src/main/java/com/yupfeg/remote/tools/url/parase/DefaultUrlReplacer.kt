package com.yupfeg.remote.tools.url.parase

import com.yupfeg.remote.tools.url.cache.LruCache
import okhttp3.HttpUrl

/**
 * url重定向的默认实现类
 * @author yuPFeG
 * @date 2021/03/10
 */
internal class DefaultUrlReplacer : UrlReplaceable {

    private val mPathSegmentsCache = LruCache.create<String,String>(100)

    /**
     * 重定向Url
     * @param originUrl 原始请求url
     * @param redirectBaseUrl 重定向的url(host)
     * @param ignoreSegmentSize 重定向的Url需要替换的文段数目，即从域名后开始算起，有几个`/`需要字符串替换
     * */
    override fun redirectedUrl(
        originUrl: HttpUrl,
        redirectBaseUrl: HttpUrl,
        ignoreSegmentSize: Int
    ): HttpUrl {
        val newUrlBuilder = originUrl.newBuilder()

        val cacheKey = getEncodedUrlPathCacheKey(originUrl,redirectBaseUrl,ignoreSegmentSize)
        mPathSegmentsCache[cacheKey]?.also { urlPath->
            //存在重定向url文段缓存
            newUrlBuilder.encodedPath(urlPath)
        }?:run {
            newUrlBuilder.processRedirectUrlSegments(
                originUrl,redirectBaseUrl,ignoreSegmentSize
            )
        }

        val newHttpUrl = newUrlBuilder.buildRedirectUrl(redirectBaseUrl)
        //缓存已重定向的url
        mPathSegmentsCache[cacheKey]?:run {
            mPathSegmentsCache.put(cacheKey,newHttpUrl.encodedPath)
        }

        return newHttpUrl
    }

    /**
     * 获取重定向请求文段url的缓存key
     * @param originUrl 原始请求url
     * @param redirectUrl 重定向url(host)
     * @param ignoreSize 重定向的替换文段位置
     * */
    private fun getEncodedUrlPathCacheKey(
        originUrl : HttpUrl,
        redirectUrl : HttpUrl,
        ignoreSize: Int
    ) : String{
        return "${originUrl.encodedPath} : ${redirectUrl.encodedPath} : $ignoreSize"
    }

    /**
     * [HttpUrl.Builder]的拓展函数，处理重定向后的请求url文段数据
     * @param originUrl 原始请求url
     * @param redirectUrl 目标重定向url
     * @param ignoreSegmentSize 需要忽略的原始请求文段大小，默认为0
     * */
    private fun HttpUrl.Builder.processRedirectUrlSegments(
        originUrl : HttpUrl,redirectUrl: HttpUrl,
        ignoreSegmentSize: Int
    ) {
        //1.先移除原始请求url的所有文段
        for (i in 0 until originUrl.pathSize){
            //PathSegment 的 item 会在删除后自动前进一位，故只需删除第0位
            this.removePathSegment(0)
        }
        //2.添加重定向域名的替换文段
        val newPathSegments = mutableListOf<String>()
        newPathSegments.addAll(redirectUrl.encodedPathSegments)

        takeIf { originUrl.pathSize > ignoreSegmentSize }?.also {
            //3.将原始请求url的剩余文段拼接到重定向url后面
            val originSegments = originUrl.encodedPathSegments
            for (index in ignoreSegmentSize..originSegments.lastIndex) {
                newPathSegments.add(originSegments[index])
            }
        }?:run {
            val errorMsg = String.format(
                "Your origin url is %s, path size is %s,but ignore segmentSize is %s",
                "${originUrl.scheme}://${originUrl.host}${originUrl.encodedPath}",
                "$${originUrl.pathSize}",
                "$ignoreSegmentSize"
            )
            throw IllegalArgumentException(errorMsg)
        }
        //4.将所有文段添加到HttpUrl
        for (segment in newPathSegments) {
            this.addEncodedPathSegment(segment)
        }
    }

    /**
     * [HttpUrl.Builder]的拓展函数，构建重定向[HttpUrl]
     * @param redirectBaseUrl 重定向目标url
     * */
    private fun HttpUrl.Builder.buildRedirectUrl(redirectBaseUrl : HttpUrl) : HttpUrl{
        return this.scheme(redirectBaseUrl.scheme)
            .host(redirectBaseUrl.host)
            .port(redirectBaseUrl.port)
            .build()
    }

}