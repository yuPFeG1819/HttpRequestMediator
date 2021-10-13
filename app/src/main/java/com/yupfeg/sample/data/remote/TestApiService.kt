package com.yupfeg.sample.data.remote

import com.yupfeg.sample.data.entity.JueJinHttpResultBean
import com.yupfeg.remote.tools.url.UrlRedirectHelper
import com.yupfeg.sample.constant.AppConstant
import com.yupfeg.sample.data.entity.WanAndroidArticleListResponseEntity
import io.reactivex.rxjava3.core.Maybe
import retrofit2.http.*

/**
 * 测试的API接口
 * @author yuPFeG
 * @date 2021/03/30
 */
interface TestApiService {

//    /**
//     * 基于RxJava2，获取WanAndroid的文章列表
//     * @param pageNum 列表分页的页数
//     * */
//    @GET("article/list/{pageNum}/json")
//    fun queryWanAndroidArticleByRxJava2(
//        @Path("pageNum") pageNum: Int
//    ) : io.reactivex.Maybe<WanAndroidArticleListResponseEntity>

    /**
     * 基于RxJava3，获取WanAndroid 文章列表
     * http://www.wanandroid.com/article/list/0/json
     * @param pageNum
     */
    @GET("article/list/{pageNum}/json")
    fun getWanAndroidArticlesByRxJava3(
        @Path("pageNum") pageNum: Int
    ): Maybe<WanAndroidArticleListResponseEntity>

    /**
     * 基于kotlin 协程，获取WanAndroid的文章列表
     * @param pageNum 列表分页页数
     * */
    @GET("article/list/{pageNum}/json")
    suspend fun queryWanAndroidArticleByCoroutine(
        @Path("pageNum") pageNum: Int
    ) : WanAndroidArticleListResponseEntity

    /**
     * 掘金PC端的个人中心接口
     * https://api.juejin.cn/content_api/v1/advert/query_adverts
     * * 无法正常请求，仅用作测试替换baseUrl
     * */
    @Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}${AppConstant.JUE_JIN_URL_TAG}")
    @POST("content_api/v1/advert/query_adverts")
    fun queryJueJinAdverts() : Maybe<JueJinHttpResultBean>

    /**
     * 动态替换baseUrl
     * * 实际请求https://www.baidu.com/s?tn=request_24_pg&word=android
     *  * 无法正常请求，仅用作测试替换baseUrl
     */
    @Headers("${UrlRedirectHelper.REDIRECT_HOST_HEAD_PREFIX}${AppConstant.BAI_DU_URL_TAG}")
    @GET("s")
    fun queryBaiduData(@Query("tn") tagId : String,
                       @Query("word") word : String) : Maybe<JueJinHttpResultBean>

}