package com.yupfeg.remote.data.remote

import com.yupfeg.remote.data.entity.HttpResultBean
import com.yupfeg.remote.url.UrlRedirectHelper
import io.reactivex.rxjava3.core.Maybe
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 利用掘金开放的PC端接口进行测试
 * @author yuPFeG
 * @date 2021/03/30
 */
interface TestApi {
    //默认直接使用
    //https://api.juejin.cn/content_api/v1/advert/query_adverts
    @POST("content_api/v1/advert/query_adverts")
    fun queryAdverts() : Maybe<HttpResultBean>

    //默认使用
    //https://api.juejin.cn/interact_api/v1/collectionSet/list?user_id=2084329778585128&cursor=20&limit=20
    @GET("interact_api/v1/collectionSet/list")
    fun queryUserCollection(@Query("user_id") userId: String,
                            @Query("cursor") cursor: Int,
                            @Query("limit") limit : Int) : Maybe<HttpResultBean>

    //动态替换baseUrl
    ////https://www.baidu.com/s?tn=request_24_pg&word=android
    @Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}baidu")
    @GET("s")
    fun queryBaiduData(@Query("tn") tagId : String,
                       @Query("word") word : String) : Maybe<HttpResultBean>

    //动态替换baseUrl
    //https://api.juejin.cn/user_api/v1/user/get?aid=2068&not_self=1
    @Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}user")
    @GET("v1/user/get")
    fun queryUserData(
        @Query("aid") userId: String,
        @Query("not_self") notSelf : Int = 0
    ) : Maybe<HttpResultBean>
}