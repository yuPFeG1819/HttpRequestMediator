package com.yupfeg.remote.tools

import com.yupfeg.logger.ext.logd
import com.yupfeg.logger.ext.loge
import com.yupfeg.logger.ext.logi
import com.yupfeg.logger.ext.logw
import com.yupfeg.remote.log.HttpLogPrinter

class LoggerHttpLogPrinterImpl : HttpLogPrinter{
    override fun printDebugLog(tag: String, content: Any) {
        logd(tag,content)
    }

    override fun printInfoLog(tag: String, content: Any) {
        logi(tag,content)
    }

    override fun printWarningLog(tag: String, content: Any) {
        logw(tag,content)
    }

    override fun printErrorLog(tag: String, content: Any) {
        loge(tag,content)
    }
}