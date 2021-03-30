package com.yupfeg.remote.pool

import com.yupfeg.logger.ext.logd
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * 网络请求专用的全局线程池
 * @author yuPFeG
 * @date 2019/11/1
 */
internal object GlobalHttpThreadPool {
    /**虚拟机cpu核心数量 */
    private val cpu_count = Runtime.getRuntime().availableProcessors()
    /**最大核心线程数量，维持最少2个一直保持的核心线程,最多维持4个核心 */
    private val core_pool_count = max(2, min(cpu_count - 1, 4))
    /**线程池最大容量 */
    private val max_pool_count = cpu_count * 2 + 1

    /**线程池非核心线程存活时间（单位·秒） */
    private const val KEEP_ALIVE_SECONDS = 30
    /**
     * 线程池，维持最少2个核心线程，最大允许存在2倍cpu核心数的非核心线程,
     * 非核心线程存活时间默认为30s
     * */
    val executorService : ThreadPoolExecutor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ThreadPoolExecutor(
            core_pool_count, max_pool_count,
                KEEP_ALIVE_SECONDS.toLong(), TimeUnit.SECONDS,
                LinkedBlockingQueue(), GlobalThreadFactory()
        )
    }
    init {
        logd("global thread pool init")
    }
}

/**用于创建线程的工厂类 */
private class GlobalThreadFactory : ThreadFactory {
    companion object{
        /**线程池内的的线程名称*/
        private const val GLOBAL_THREAD_NAME = "global_http_back_stage_thread"
    }

    override fun newThread(runnable: Runnable): Thread {
        val result = Thread(runnable, GLOBAL_THREAD_NAME)
        //设置为守护线程
        result.isDaemon = true
        return result
    }
}