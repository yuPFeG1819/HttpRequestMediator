package com.yupfeg.sample.tools.rxjava2

import com.yupfeg.remote.data.HttpResponseParsable
import com.yupfeg.remote.tools.handler.GlobalHttpResponseProcessor
import com.yupfeg.remote.tools.handler.RestApiException

///**
// * 对RxJava2事件流的http预处理操作
// * RxJava3和RxJava2会编译冲突（RxAndroid和RxKotlin冲突）
// */
//fun <T : HttpResponseParsable> preHandlerRxJava2Response()
//    = GlobalHttpTransformer<T>(
//    onNextErrorIntercept = { response->
//        val isSuccess = GlobalHttpResponseProcessor.preHandleHttpResponse(response)
//        if (isSuccess){
//            //业务执行成功
//            response
//        }else{
//            //业务执行异常
//            throw RestApiException(response.code,response.message)
//        }
//    },
//    doOnErrorConsumer = {error->
//        //预处理业务请求出现的异常
//        GlobalHttpResponseProcessor.handleHttpError(error)
//    })
