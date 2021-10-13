package com.yupfeg.sample.data.entity


/**
 * wanAndroid的登录接口返回实体
 * @author yuPFeG
 * @date 2021/09/24
 */
data class LoginResultEntity(
    val data : DataEntity? = null
) : WanAndroidHttpResponseEntity() {
    // 登录数据
    data class DataEntity(
        val chapterTops: MutableList<String>,
        val collectIds: MutableList<String>,
        val email: String,
        val icon: String,
        val id: Int,
        val password: String,
        val token: String,
        val type: Int,
        val username: String
    )
}
