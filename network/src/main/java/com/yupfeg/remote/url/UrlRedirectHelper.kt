package com.yupfeg.remote.url

import com.yupfeg.remote.url.parase.DefaultUrlReplacer
import com.yupfeg.remote.url.parase.UrlReplaceable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.lang.NullPointerException

/**
 * 网络请求Url重定向管理的核心类
 * * 依附于`OkHttp`与`Retrofit`
 *
 * 如需要重定向Url :
 * 1. 需要通过[UrlRedirectHelper.putRedirectUrl]添加对应重定向url键值对，
 *
 * 2. 在网络请求API声明处，添加`@Headers("${REDIRECT_HOST_HEAD_PREFIX}${URL_KEY}")`请求头
 *
 * * 默认情况下，重定向Url替换规则：将重定向baseUrl，完整按位替换，原始请求的url
 *
 * example ：
 *
 * 原始请求url为：`https://www.github.com/testSegment1/segment2/`,
 * 重定向baseUrl为：`https://www.baidu.com/user/`,
 * 最后重定向后的url为：`https://www.baidu.com/user/segment2/`
 *
 * * 如需管理segment替换的数量，可以设置[globalReplaceSegmentSize],重置全局替换规则
 *
 * 对于单独请求应用的替换规则，可在网络请求API声明处，
 * 添加`@Headers("${REDIRECT_SEGMENT_SIZE_HEAD_PREFIX}${SIZE}")`请求头
 *
 * example：
 *
 * size = 0，表示只替换域名，新文段拼接在原始请求文段之前
 * 原始请求url： `https://www.baidu.com/user/sss?user_id=111` ,
 * 重定向baseUrl：`https://www.google.com/test1/list/`,
 * 新url：`https://www.google.com/user/test1/list/sss?user_id=111`,
 *
 * size = 1，只替换域名后的第一个文段。
 * 原始请求url：`https://www.baidu.com/user/sss?user_id=111`,
 * 重定向baseUrl：`https://www.google.com/test1/list/`,
 * 新url：`https://www.google.com/test1/list/sss?user_id=111`,
 *
 * 以此类推...
 * @author yuPFeG
 * @date 2021/03/10
 */
@Suppress("unused")
object UrlRedirectHelper {

    /**网络请求的请求头key，标识需要重定向的url域名*/
    private const val REDIRECT_HOST_HEAD_KEY = "Url-Redirect"
    private const val PATH_SEGMENT_SIZE_HEAD_KEY = "Url-PathSegmentsSize"
    /**
     * 标识为 `该请求需要重定向url` 的请求头前缀
     * * 在请求API，添加`@Headers("${REDIRECT_HOST_HEAD_PREFIX}${URL_NAME}")`
     * */
    @Suppress("unused")
    const val REDIRECT_HOST_HEAD_PREFIX = "$REDIRECT_HOST_HEAD_KEY: "

    /**
     * 标识为 `该请求需要设置重定向url替换文段的个数` 的请求头前缀
     * * 在请求API，添加`@Headers("${REDIRECT_SEGMENT_SIZE_HEAD_PREFIX}${SIZE}")`
     * */
    @Suppress("unused")
    const val REDIRECT_SEGMENT_SIZE_HEAD_PREFIX = "$PATH_SEGMENT_SIZE_HEAD_KEY: "

    /**重定向url集合*/
    private val mRedirectUrlMap : MutableMap<String,HttpUrl> = mutableMapOf()

    /**负责请求url重定向替换的策略类*/
    private var mUrlReplacer : UrlReplaceable = DefaultUrlReplacer()

    private var mGlobalReplaceSegmentSize : Int = 0

    /**
     * 全局配置的重定向替换url文段长度
     * * 默认为0，只替换域名，并将重定向url的文段拼接在原始请求文段之前
     *
     * 对于单独请求需要特殊替换规则的，添加[REDIRECT_SEGMENT_SIZE_HEAD_PREFIX]请求头，优先级最高
     */
    @JvmStatic
    @Suppress("MemberVisibilityCanBePrivate")
    var globalReplaceSegmentSize : Int
        get() = mGlobalReplaceSegmentSize
        set(value) {
            if (value < 0) return
            mGlobalReplaceSegmentSize = value
        }

    /**
     * 是否使用全局替换规则
     * * 如果为true-使用全局规则，false-按重定向BaseUrl文段长度替换规则
     * */
    @Suppress("MemberVisibilityCanBePrivate")
    var isUseGlobalReplaceRule : Boolean = true

    /**
     * 设置url重定向策略类
     * @param urlReplacer url替换策略类
     * */
    @Suppress("unused")
    fun setUrlReplacer(urlReplacer : UrlReplaceable) : UrlRedirectHelper {
        mUrlReplacer = urlReplacer
        return this
    }

    /**
     * 设置重定向的url域名
     * @param name 域名映射key
     * @param url 重定向域名
     * @return 返回自身，便于链式调用
     */
    @Suppress("unused")
    fun putRedirectUrl(name : String, url : String) : UrlRedirectHelper {
        val baseUrl = url.toHttpUrlOrNull()
            ?: throw NullPointerException(
                "host cant parse to httpUrl，you configured invalid url"
            )
        mRedirectUrlMap[name] = baseUrl
        return this
    }

    /**
     * 移除重定向域名映射与url的键值对
     * @param name 域名映射key
     * @return 返回自身，便于链式调用
     * */
    @Suppress("unused")
    fun removeRedirectUrlName(name : String) : UrlRedirectHelper {
        takeIf { mRedirectUrlMap.containsKey(name) }?.run {
            mRedirectUrlMap.remove(name)
        }
        return this
    }

    /**
     * 清空所有重定向域名映射集合
     * @return 返回自身，便于链式调用
     * */
    @Suppress("unused")
    fun clearRedirectUrlMap() : UrlRedirectHelper {
        mRedirectUrlMap.clear()
        return this
    }

    // <editor-fold desc="重定向请求处理">

    /**
     * 获取重定向Url后的网络请求
     * @param request 原始网络请求
     * @return 如果能够重定向，则返回重定向后的请求，否则返回原始请求
     */
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    fun obtainRedirectedUrlRequest(request: Request) : Request{
        val newBuilder = request.newBuilder()

        val urlName = request.fetchRedirectUrlNameFromHeader()
            .takeUnless { it.isNullOrEmpty() }
            ?.also {
                //移除重定向域名映射标识的请求头
                newBuilder.removeRedirectUrlHeader()
            }?: return newBuilder.build()
        //目标重定向url
        val redirectUrl = fetchedBaseUrl(urlName)
            ?: run {
                //如果未能获取到重定向域名（baseUrl），则请求原样返回
                return newBuilder.build()
            }

        // 获取需要替换(移除忽略)的原始请求url文段数量，默认为0
        val ignoreSegmentsSize = request.obtainRedirectSegmentSize()?.toIntOrNull()
            ?.also {
                //移除表示文段大小标识的请求头
                newBuilder.removeSegmentSizeHeader()
            }?: run {
                mGlobalReplaceSegmentSize
            }

        val newUrl = mUrlReplacer.redirectedUrl(request.url,redirectUrl,ignoreSegmentsSize)

        return newBuilder
            .url(newUrl)
            .build()
    }

    /**
     * 根据映射名key取出对应的`baseUrl`
     * @param name
     */
    private fun fetchedBaseUrl(name  : String) : HttpUrl?{
        return mRedirectUrlMap[name]
    }

    /**
     * [Request]的拓展函数，尝试从原始请求头中，获取重定向的url映射
     * @return 如果请求内添加了重定向域名映射，返回
     * */
    @Throws(IllegalAccessException::class)
    private fun Request.fetchRedirectUrlNameFromHeader() : String?{
        val hostHeaders = this.headers(REDIRECT_HOST_HEAD_KEY)
        if(hostHeaders.isNullOrEmpty()) return ""
        takeIf { hostHeaders.size > 1 }?.run {
            throw IllegalArgumentException("Only one host name tag in the headers")
        }
        return this.header(REDIRECT_HOST_HEAD_KEY)
    }

    /**
     * [Request.Builder]的拓展函数，移除标识需要重定向的请求头
     * */
    private fun Request.Builder.removeRedirectUrlHeader(){
        this.removeHeader(REDIRECT_HOST_HEAD_KEY)
    }

    /**
     * [Request]的拓展函数，尝试从原始请求的请求头中，获取重定向url替换的文段数
     * @return 如果请求头存在替换文段数的标识，则返回int值的字符串，否则默认返回null，即只替换域名部分
     */
    @Throws(IllegalAccessException::class)
    private fun Request.obtainRedirectSegmentSize() : String?{
        val hostHeaders = this.headers(PATH_SEGMENT_SIZE_HEAD_KEY)
        takeIf { hostHeaders.isNullOrEmpty() } ?: return ""
        takeIf { hostHeaders.size > 1 }?.run {
            throw IllegalArgumentException("Only one segment size tag in the headers")
        }
        return this.header(PATH_SEGMENT_SIZE_HEAD_KEY)
    }

    /**
     * [Request.Builder]的拓展函数，移除标识重定向替换文段个数的请求头
     * */
    private fun Request.Builder.removeSegmentSizeHeader(){
        this.removeHeader(PATH_SEGMENT_SIZE_HEAD_KEY)
    }

    // </editor-fold>

}