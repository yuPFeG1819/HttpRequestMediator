package com.yupfeg.remote.data

/**
 * 网络请求返回的body内容，通常为最外层。
 * * 在实际数据业务层使用时，后台返回的数据格式bean（entity）类都实现该接口
 * @author yuPFeG
 * @date 2021/09/22
 */
interface HttpResponseParsable {
    /**接口返回状态，用于判断是否为业务请求成功以及出现错误*/
    val code : Int
    /**接口返回的说明文本，提示网络接口执行情况*/
    val message : String
}