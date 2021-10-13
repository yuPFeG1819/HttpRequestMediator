package com.yupfeg.sample.base

import androidx.lifecycle.*
import autodispose2.androidx.lifecycle.AndroidLifecycleScopeProvider
import autodispose2.lifecycle.CorrespondingEventsFunction
import autodispose2.lifecycle.LifecycleScopeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * 基类ViewModel
 * * 修改自[AndroidLifecycleScopeProvider]，实现`AutoDispose`的`LifecycleScopeProvider`接口，
 * 使子类能直接使用`autoDispose(this)`关联生命周期，自动管理`RxJava`事件流
 * @author yuPFeG
 * @date
 */
open class BaseViewModel : ViewModel(), LifecycleScopeProvider<Lifecycle.Event>, LifecycleObserver {
    // Subject backing the auto disposing of subscriptions.
    private val mLifecycleEventSubject = BehaviorSubject.createDefault(Lifecycle.Event.ON_CREATE)

    /**
     * 最终结束的RxJava生命周期事件
     * * 即自动结束RxJava数据链的目标生命周期
     * */
    protected open val correspondingEvents =
        CorrespondingEventsFunction { _: Lifecycle.Event ->
            /**当前生命周期事件*/
            return@CorrespondingEventsFunction endLifecycleEvent
        }

    /**
     * 自动结束事件流的目标生命周期事件
     * * 默认为onDestroy时结束数据流，可在子类覆盖重写
     * */
    open val endLifecycleEvent : Lifecycle.Event
        get() = Lifecycle.Event.ON_DESTROY

    //<editor-fold desc="LifecycleScopeProvider接口实现">

    override fun lifecycle(): Observable<Lifecycle.Event> {
        return mLifecycleEventSubject.hide()
    }

    override fun correspondingEvents(): CorrespondingEventsFunction<Lifecycle.Event> {
        return correspondingEvents
    }

    override fun peekLifecycle(): Lifecycle.Event? {
        return mLifecycleEventSubject.value!!
    }

    //</editor-fold>

    //<editor-fold desc="Lifecycle监听">
    /**
     * 外部绑定的生命周期onCreate
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    protected open fun onCreate() {
        backFillEvents(Lifecycle.State.CREATED)
    }

    /**
     * 外部绑定的生命周期onStart
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected open fun onStart() {
        backFillEvents(Lifecycle.State.STARTED)
    }

    /**
     * 外部绑定的生命周期onResume
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    protected open fun onResume() {
        backFillEvents(Lifecycle.State.RESUMED)
    }

    /**
     * 外部绑定的生命周期onPause
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    protected open fun onPause() {
        backFillEvents(Lifecycle.State.STARTED)
    }

    /**
     * 外部绑定的生命周期onStop
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected open fun onStop() {
        backFillEvents(Lifecycle.State.CREATED)
    }

    /**
     * 外部绑定的生命周期onDestroy
     * */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    protected open fun onDestroy() {
        backFillEvents(Lifecycle.State.DESTROYED)
    }


    /**
     * Backfill if already created for boundary checking. We do a trick here for corresponding events
     * where we pretend something is created upon initialized state so that it assumes the
     * corresponding event is DESTROY.
     */
    protected open fun backFillEvents(lifecycleState : Lifecycle.State) {
        val correspondingEvent = when (lifecycleState) {
            Lifecycle.State.INITIALIZED -> Lifecycle.Event.ON_CREATE
            Lifecycle.State.CREATED -> Lifecycle.Event.ON_START
            Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> Lifecycle.Event.ON_RESUME
            else -> Lifecycle.Event.ON_DESTROY
        }
        mLifecycleEventSubject.onNext(correspondingEvent)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    private fun onStateChange(owner: LifecycleOwner?, event: Lifecycle.Event) {
        if (!(event == Lifecycle.Event.ON_CREATE && mLifecycleEventSubject.value == event)) {
            // Due to the INITIALIZED->ON_CREATE mapping trick we do in backfill(),
            // we fire this conditionally to avoid duplicate CREATE events.
            mLifecycleEventSubject.onNext(event)
        }
    }

    //</editor-fold>
}