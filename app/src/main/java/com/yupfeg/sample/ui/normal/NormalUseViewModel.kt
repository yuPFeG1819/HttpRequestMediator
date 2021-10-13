package com.yupfeg.sample.ui.normal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import autodispose2.autoDispose
import com.yupfeg.logger.ext.logw
import com.yupfeg.remote.tools.handler.GlobalHttpResponseProcessor
import com.yupfeg.sample.base.BaseViewModel
import com.yupfeg.sample.data.remote.RemoteDataSource
import kotlinx.coroutines.launch

/**
 * 基础使用页的ViewModel
 * @author yuPFeG
 * @date 2021/09/29
 */
class NormalUseViewModel : BaseViewModel(){

//    private val mRxJava2Disposables = io.reactivex.disposables.CompositeDisposable()


    val articleJsonStringLiveData : LiveData<String>
        get() = mArticleJsonStringLiveData
    private val mArticleJsonStringLiveData = MutableLiveData<String>()

    override fun onCleared() {
        super.onCleared()
//        mRxJava2Disposables.clear()
    }

//    fun queryWanAndroidArticleByRxJava2(){
//        val disposable = RemoteDataSource.queryWanAndroidArticleByRxJava2(0)
//            .subscribe({entity->
//                mArticleJsonStringLiveData.value = "query WanAndroid Article from RxJava2 \n $entity"
//            },{
//                logw(it)
//            })
//        mRxJava2Disposables.add(disposable)
//    }

    fun queryWanAndroidArticleByRxJava3(){
        RemoteDataSource.queryWanAndroidArticlesByRxJava3(0)
            .autoDispose(this)
            .subscribe ({entity->
                mArticleJsonStringLiveData.value = "query WanAndroid Article from RxJava3 \n $entity"
            },{
                logw(it)
                mArticleJsonStringLiveData.value = "query WanAndroid Article from RxJava3 $it"
            })
    }

    fun queryWanAndroidArticleByCoroutine(){
        viewModelScope.launch{
            //TODO 仅作测试，后续使用flow优化
            try {
                val responseEntity = RemoteDataSource.queryWanAndroidArticleByCoroutine(0)
                mArticleJsonStringLiveData.value = "query WanAndroid Article from Coroutine \n $responseEntity"
            }catch (e : Exception){
                GlobalHttpResponseProcessor.handleHttpError(e)
                mArticleJsonStringLiveData.value = "query WanAndroid Article from Coroutine $e"
            }
        }
    }
}