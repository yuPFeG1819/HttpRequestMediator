package com.yupfeg.sample.data.entity

import com.google.gson.annotations.SerializedName
import com.yupfeg.remote.data.HttpResponseParsable

/**
 * 掘金PC端接口返回实体
 * * 仅用于测试切换baseUrl
 * @author yuPFeG
 * @date 2021/03/30
 */
data class JueJinHttpResultBean(
    @SerializedName("err_no")
    var errNo : Int = 0,
    @SerializedName("err_msg")
    var errMsg : String = ""
) : HttpResponseParsable{
    override val code: Int
        get() = errNo
    override val message: String
        get() = errMsg

}