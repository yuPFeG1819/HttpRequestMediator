package com.yupfeg.sample.data.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * 玩Android的首页文章列表返回实体类
 * @author yuPFeG
 * @date 2021/09/25
 */
data class WanAndroidArticleListResponseEntity (
    val data : DataEntity? = null
) : WanAndroidHttpResponseEntity(){
    data class DataEntity(
        @SerializedName("curPage")
        val curPage: Int = 0,
        @SerializedName("datas")
        var datas: List<WanAndroidArticleListEntity>? = null,
        @SerializedName("offset")
        val offset: Int = 0,
        @SerializedName("over")
        val over: Boolean = false,
        @SerializedName("pageCount")
        val pageCount: Int = 0,
        @SerializedName("size")
        val size: Int = 0,
        @SerializedName("total")
        val total: Int = 0
    ) : Serializable
}

/**
 * WanAndroid首页文章列表实体
 * */
data class WanAndroidArticleListEntity(
    val apkLink: String,
    val audit: Int,
    val author: String,
    val chapterId: Int,
    val chapterName: String,
    var collect: Boolean,
    val courseId: Int,
    val desc: String,
    val envelopePic: String,
    val fresh: Boolean,
    val id: Int,
    val link: String,
    val niceDate: String,
    val niceShareDate: String,
    val origin: String,
    val prefix: String,
    val projectLink: String,
    val publishTime: Long,
    val shareDate: String,
    val shareUser: String,
    val superChapterId: Int,
    val superChapterName: String,
    val tags: List<TagEntity>,
    val title: String,
    val type: Int,
    val userId: Int,
    val visible: Int,
    val zan: Int,
    var top: String
)

data class TagEntity(
    val name: String,
    val url: String
)