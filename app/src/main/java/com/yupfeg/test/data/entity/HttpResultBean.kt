package com.yupfeg.test.data.entity

import com.google.gson.annotations.SerializedName

/**
 *
 * @author yuPFeG
 * @date 2021/03/30
 */
class HttpResultBean {
    @SerializedName("err_no")
    var errNo : Int = 0
    @SerializedName("err_msg")
    var errMsg : String = ""
}