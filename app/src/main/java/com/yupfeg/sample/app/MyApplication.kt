package com.yupfeg.sample.app

import android.app.Application
import android.content.Context
import com.yupfeg.logger.ext.setDslLoggerConfig
import com.yupfeg.logger.printer.LogcatPrinter
import com.yupfeg.remote.tools.handler.GlobalHttpResponseProcessor
import com.yupfeg.sample.tools.GlobalResponseHandler
import kotlin.properties.Delegates

/**
 *
 * @author yuPFeG
 * @date
 */
class MyApplication : Application(){

    companion object{
        var appContext: Context by Delegates.notNull()
            private set

        lateinit var instance : MyApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = this.applicationContext
        //配置日志输出
        setDslLoggerConfig {
            isDisplayClassInfo = true
            isDisplayThreadInfo = true
            logPrinters = listOf(LogcatPrinter())
        }

        //设置全局http响应
        GlobalHttpResponseProcessor.setResponseHandler(GlobalResponseHandler())
    }

}