package com.yupfeg.sample.tools.rxjava3

import com.yupfeg.remote.data.HttpResponseParsable
import com.yupfeg.remote.tools.handler.GlobalHttpResponseProcessor
import com.yupfeg.remote.tools.handler.RestApiException

/**
 * 对于RxJava3事件流的http事件流预处理操作
 */
fun <T : HttpResponseParsable> preHandlerRxJava3Response()
        = GlobalHttpTransformer<T>(
    onNextErrorIntercept = { response->
        val isSuccess = GlobalHttpResponseProcessor.preHandleHttpResponse(response)
        if (isSuccess){
            //业务执行成功
            response
        }else{
            //业务执行异常
            throw RestApiException(response.code,response.message)
        }
    },
    doOnErrorConsumer = {error->
        //预处理业务请求出现的异常
        GlobalHttpResponseProcessor.handleHttpError(error)
    })

