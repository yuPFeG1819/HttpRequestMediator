package com.yupfeg.sample.data.entity

import com.yupfeg.remote.data.HttpResponseParsable
import java.io.Serializable

/**
 * WanAndroid的api接口最外层的返回实体
 * * 避免在使用泛型时，由于接口返回格式不规范导致出现异常
 * @author yuPFeG
 * @date 2021/09/24
 */
open class WanAndroidHttpResponseEntity : HttpResponseParsable,Serializable{
    /**
     * 接口执行状态码
     * * 0-表示执行正常，否则表示执行错误
     * */
    var errorCode : Int = 0
    var errorMsg : String? = ""

    // <editor-fold desc="统一处理请求响应接口实现">
    override val code: Int
        get() = errorCode
    override val message: String
        get() = errorMsg?:""
    // </editor-fold>
}