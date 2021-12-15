package com.yupfeg.sample.tools.coroutine

import com.yupfeg.remote.data.HttpResponseParsable
import com.yupfeg.remote.tools.handler.GlobalHttpResponseProcessor
import com.yupfeg.remote.tools.handler.RestApiException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * 预处理网络请求返回
 * @param dispatcher 线程调度器
 */
fun <T : HttpResponseParsable> Flow<T>.preHandleHttpResponse(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    errorData : T?
){
    map{response->
        val isSuccess = GlobalHttpResponseProcessor.preHandleHttpResponse(response)
        if (isSuccess){
            //业务执行成功
            response
        }else{
            //业务执行异常
            throw RestApiException(response.code,response.message)
        }
    }
        .flowOn(dispatcher)
        .catch{error->
            GlobalHttpResponseProcessor.handleHttpError(error)
            errorData?:return@catch
            emit(errorData)
        }
}