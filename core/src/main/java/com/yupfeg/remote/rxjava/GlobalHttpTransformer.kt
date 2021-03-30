package com.yupfeg.remote.rxjava

import com.yupfeg.remote.pool.GlobalHttpThreadPool
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import org.reactivestreams.Publisher

/**
 * 自定义统一处理网络请求异常的RxJava变换操作符
 *
 * * 支持Maybe、Observable、Flowable三种事件源类型
 * * 适用于RxJava 3.x版本
 * @author yuPFeG
 * @date 2020/02/21
 *
 * @param onNextErrorInterceptor 在onNext前执行错误码拦截检测
 * @param doOnErrorConsumer 在onError前统一处理错误
 */
@Suppress("unused")
class GlobalHttpTransformer<T>(
    /**在onNext前执行错误码拦截检测*/
    private val onNextErrorInterceptor: Function<T, Observable<T>>,
    /**在onError前统一处理错误*/
    private val doOnErrorConsumer: Consumer<Throwable>
) : MaybeTransformer<T, T>, ObservableTransformer<T, T>, FlowableTransformer<T, T> {


    //<editor-fold desc="Maybe类型数据源的预处理">

    override fun apply(upstream: Maybe<T>): MaybeSource<T> {
        return upstream
            //step1: 订阅（事件源/http请求）发生在子线程(也可交由OkHttp内部管理线程池异步执行)
            .subscribeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            //step2: 事件流下游发生在子线程
            .observeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            //step3: 预处理API返回错误码
            .flatMap { t ->
                //类似：处理接口返回错误码，抛出对应异常
                //其他情况，数据向下游正常传递
                onNextErrorInterceptor.apply(t).firstElement()
            }
            //重试相关请求
            //                .retryWhen()
            //拦截Throwable处理,能发送新的数据源，从而不中断事件流
            //                .onErrorResumeNext()
            //step4: 事件流下游发生在主线程
            .observeOn(AndroidSchedulers.mainThread())
            //step5: 处理在onError发生之前
            .doOnError(doOnErrorConsumer)
    }
    //</editor-fold desc="Maybe类型数据源的预处理">

    //<editor-fold desc="Observable类型数据源的预处理">

    override fun apply(upstream: Observable<T>): ObservableSource<T> {
        return upstream
            //step1: 订阅（事件源/http请求）发生在子线程(也可交由OkHttp内部管理线程池异步执行)
            .subscribeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            //step2: 事件流下游发生在子线程
            .observeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            //step3: 预处理API返回错误码
            .flatMap { t ->
                //类似：处理接口返回错误码，抛出对应异常
                //其他情况，数据向下游正常传递
                onNextErrorInterceptor.apply(t)
            }
            //重试相关请求
            //                .retryWhen()
            //拦截Throwable处理,发送新的数据源
            //                .onErrorResumeNext()
            //step4: 事件流下游发生在主线程
            .observeOn(AndroidSchedulers.mainThread())
            //step5: 处理在onError发生之前
            .doOnError(doOnErrorConsumer)
    }

    //</editor-fold desc="Observable类型数据源的预处理">

    //<editor-fold desc="Flowable类型数据源的预处理">

    override fun apply(upstream: Flowable<T>): Publisher<T> {
        return upstream
            //step1: 订阅（事件源/http请求）发生在子线程(也可交由OkHttp内部管理线程池异步执行)
            .subscribeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            //step2: 事件流下游发生在子线程
            .observeOn(Schedulers.from(GlobalHttpThreadPool.executorService))
            //step3: 预处理API返回错误码
            .flatMap { t ->
                //类似：处理接口返回错误码，抛出对应异常
                //其他情况，数据向下游正常传递
                onNextErrorInterceptor.apply(t)
                    .toFlowable(BackpressureStrategy.BUFFER)
            }
            //重试相关请求
            //                .retryWhen()
            //拦截Throwable处理,发送新的数据源
            //                .onErrorResumeNext()
            //step4: 事件流下游发生在主线程
            .observeOn(AndroidSchedulers.mainThread())
            //step5: 处理在onError发生之前
            .doOnError(doOnErrorConsumer)
    }

    //</editor-fold desc="Flowable类型数据源的预处理">

}