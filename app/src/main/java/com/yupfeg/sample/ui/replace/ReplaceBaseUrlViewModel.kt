package com.yupfeg.sample.ui.replace

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yupfeg.logger.ext.logw
import com.yupfeg.remote.tools.url.UrlRedirectHelper
import com.yupfeg.sample.constant.AppConstant
import com.yupfeg.sample.data.remote.RemoteDataSource
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * 动态替换url的ViewModel
 * @author yuPFeG
 * @date 2021/09/27
 */
class ReplaceBaseUrlViewModel : ViewModel(){

    private val mDisposables = CompositeDisposable()

    val httpQueryUrl : LiveData<String>
        get() = mHttpQueryUrl
    private val mHttpQueryUrl = MutableLiveData<String>()

    val jsonStringLiveData : LiveData<String>
        get() = mJsonStringLiveData
    private val mJsonStringLiveData = MutableLiveData<String>()

    init {
        //添加需要动态替换的baseUrl
        UrlRedirectHelper.putRedirectUrl(AppConstant.JUE_JIN_URL_TAG,"https://api.juejin.cn/")
        UrlRedirectHelper.putRedirectUrl(AppConstant.BAI_DU_URL_TAG,"https://www.baidu.com/")
    }

    /**
     * 请求掘金PC端的文章列表
     * * 无法正常请求，仅用于测试替换baseUrl
     */
    fun queryJueJinAdverts(){
        val disposable = RemoteDataSource.queryJueJinAdvertsByRxJava3()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                mHttpQueryUrl.value = "query 掘金PC端文章：https://api.juejin.cn/content_api/v1/advert/query_adverts"
            }
            .subscribe ({
                mJsonStringLiveData.value = it.toString()
            },{
                logw(it)

                mJsonStringLiveData.value = it.toString()
            })
        mDisposables.add(disposable)
    }

    /**
     * 请求百度PC端查询数据
     * * 无法正常请求，仅用于测试替换baseUrl
     */
    fun queryBaiduData(){
        val disposable = RemoteDataSource.queryBaiduData()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                mHttpQueryUrl.value = "百度搜索PC端请求 https://www.baidu.com/s?tn=98010089_dg&wd=android"
            }
            .subscribe ({
                mJsonStringLiveData.value = it.toString()
            },{
                logw(it)
                mJsonStringLiveData.value = it.toString()
            })
        mDisposables.add(disposable)
    }
}