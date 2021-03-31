package com.yupfeg.remote.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.yupfeg.remote.NetWorkStatusHelper
import com.yupfeg.remote.data.remote.TestApi
import com.yupfeg.remote.tools.httpApiDelegate
import com.yupfeg.remote.url.UrlRedirectHelper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*

/**
 *
 * @author yuPFeG
 * @date 2021/03/30
 */
class MainViewModel : ViewModel(){
    private val mTestApi : TestApi by httpApiDelegate()
    private val mDisposables = CompositeDisposable()

    init {
        //添加需要动态替换的baseUrl
        UrlRedirectHelper.putRedirectUrl("user","https://api.juejin.cn/user_api/")
        UrlRedirectHelper.putRedirectUrl("baidu","https://www.baidu.com/")

        val disposable = NetWorkStatusHelper.observeNetWorkStatus()
            .subscribe {
                Log.d("okhttp","网络状态变化：${it.name}")
            }
        mDisposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }

    fun queryUserCollect() {
        val disposable = mTestApi.queryUserCollection("2084329778585128",0,20)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
            },{
                Log.e("okHttp",Arrays.toString(it.stackTrace))
            })
        mDisposables.add(disposable)
    }

    fun queryAdverts(){
        val disposable = mTestApi.queryAdverts()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({

            },{
                Log.e("okHttp",Arrays.toString(it.stackTrace))
            })
        mDisposables.add(disposable)
    }

    //获取动态替换url的收藏数据
    fun getRedirectUrlBaiduData(){
        val disposable = mTestApi.queryBaiduData("request_24_pg","android")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({

            },{
                Log.e("okHttp",Arrays.toString(it.stackTrace))
            })
        mDisposables.add(disposable)
    }

    //获取动态替换baseUrl的用户数据
    fun queryRedirectUrlUserData(){
        val disposable = mTestApi.queryUserData("2608",1)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({

            },{
                Log.e("okHttp",Arrays.toString(it.stackTrace))
            })
        mDisposables.add(disposable)
    }
}