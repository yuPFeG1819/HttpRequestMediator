package com.yupfeg.sample.ui.download

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import autodispose2.autoDispose
import com.yupfeg.logger.ext.logw
import com.yupfeg.remote.download.entity.DownloadProgressBean
import com.yupfeg.sample.app.MyApplication
import com.yupfeg.sample.base.BaseViewModel
import com.yupfeg.sample.data.remote.RemoteDataSource
import kotlin.math.roundToInt

/**
 * 测试下载apk（单文件）的ViewModel
 * @author yuPFeG
 */
class DownloadApkViewModel : BaseViewModel(){

    companion object{
        private const val MB = 1024 * 1024
        private const val APK_URL = "https://dl.hdslb.com/mobile/latest/iBiliPlayer-bili.apk?t=1633926374000&spm_id_from=333.47.b_646f776e6c6f61642d6c696e6b.1"
    }

    val downloadProgressLiveData : LiveData<Int>
        get() = mDownloadProgressLiveData
    private val mDownloadProgressLiveData = MutableLiveData<Int>()

    val downloadedProgressTextString : LiveData<String>
        get() = mDownloadedProgressTextString
    private val mDownloadedProgressTextString = MutableLiveData<String>()

    val downloadFileSizeTextStringLiveData : LiveData<String>
        get() = mDownloadFileSizeTextStringLiveData
    private val mDownloadFileSizeTextStringLiveData = MutableLiveData<String>()

    /**下载状态*/
    private var mDownloadStatus = DownloadStatus.COMPLETE

    /**
     * 下载apk
     */
    fun downloadApk(){
        if (mDownloadStatus == DownloadStatus.DOWNLOADING){
            return
        }

        RemoteDataSource.downloadApk(APK_URL)
            //step 开启下载前重置下载状态
            .doOnSubscribe{ resetDownloadState() }
            .autoDispose(this)
            .subscribe ({},{ logw(it)})
        observeDownloadProgress()
    }

    /**
     * 订阅文件下载进度
     * */
    private fun observeDownloadProgress(){
        RemoteDataSource.observeApkDownloadProgress(APK_URL)
            .autoDispose(this)
            .subscribe({progressBean->
                processDownloadProgressChange(progressBean)
            },{
                logw(it)
                processDownloadProgressChange(DownloadProgressBean.createDownloadFailure(APK_URL))
            })
    }

    private fun resetDownloadState(){
        mDownloadProgressLiveData.value = 0
        mDownloadedProgressTextString.value = "下载进度 ：0%"
        mDownloadFileSizeTextStringLiveData.value = "下载文件大小 ：0 M"
        mDownloadStatus = DownloadStatus.DOWNLOADING
    }

    private fun processDownloadProgressChange(progressBean: DownloadProgressBean){
        if (progressBean.isFailure){
            //下载失败
            Toast.makeText(MyApplication.appContext,"下载失败",Toast.LENGTH_SHORT).show()
            mDownloadStatus = DownloadStatus.FAIL
            return
        }
        if (progressBean.isDone){
            //下载完成
            renderDownloadComplete(progressBean.totalByte)
            return
        }

        val progress = if (progressBean.progress > 0) progressBean.progress else 0f
        mDownloadProgressLiveData.value = progress.roundToInt()
        val progressText = if (progressBean.progress > 0) String.format("%.1f",progressBean.progress)
        else 0
        mDownloadedProgressTextString.value = "${progressText}%"
        val currSize = if (progressBean.currByte > 0) progressBean.currByte.toFloat()/MB else 0
        val totalFileSize = if (progressBean.totalByte > 0) progressBean.totalByte.toFloat()/MB else 0
        val fileSizeString = "${String.format("%.2f",currSize)} " +
                "/ ${String.format("%.2f",totalFileSize)}"
        mDownloadFileSizeTextStringLiveData.value = fileSizeString
    }

    /**
     * 文件下载完成
     * @param totalSize 下载文件的总计byte数
     * */
    private fun renderDownloadComplete(totalSize : Long){
        mDownloadProgressLiveData.value = 100
        mDownloadedProgressTextString.value = "文件下载进度：100%"
        val fileTotalSize = String.format("%.2f",totalSize.toFloat()/MB)
        mDownloadFileSizeTextStringLiveData.value = "文件下载大小：$fileTotalSize / $fileTotalSize MB"
        mDownloadStatus = DownloadStatus.COMPLETE
    }
}

enum class DownloadStatus{
    DOWNLOADING,FAIL,COMPLETE
}