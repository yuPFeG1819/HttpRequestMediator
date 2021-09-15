package com.yupfeg.test

import android.app.Application
import com.yupfeg.logger.ext.setDslLoggerConfig
import com.yupfeg.logger.printer.LogcatPrinter

/**
 *
 * @author yuPFeG
 * @date
 */
class MyApplication : Application(){

    override fun onCreate() {
        super.onCreate()
        //配置日志输出
        setDslLoggerConfig {
            isDisplayClassInfo = false
            isDisplayThreadInfo = false
            logPrinters = listOf(LogcatPrinter())
        }
    }

}