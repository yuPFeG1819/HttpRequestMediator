package com.yupfeg.sample.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.yupfeg.logger.ext.logd
import com.yupfeg.remote.tools.status.NetWorkStatus
import com.yupfeg.remote.tools.status.NetWorkStatusHelper
import com.yupfeg.remote.tools.status.NetworkStatusChangeListener
import com.yupfeg.sample.R
import com.yupfeg.sample.app.MyApplication
import com.yupfeg.sample.ui.download.DownloadApkActivity
import com.yupfeg.sample.ui.normal.NormalUseActivity
import com.yupfeg.sample.ui.replace.ReplaceUrlActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_main_base_use).setOnClickListener {
            startActivity(Intent(this,NormalUseActivity::class.java))
        }

        findViewById<View>(R.id.btn_main_replace_url).setOnClickListener {
            startActivity(Intent(this, ReplaceUrlActivity::class.java))
        }

        findViewById<View>(R.id.btn_download_apk).setOnClickListener {
            startActivity(Intent(this,DownloadApkActivity::class.java))
        }


        //订阅网络状态变化
        NetWorkStatusHelper.registerNetWorkStatusChange(
            MyApplication.appContext,
            object : NetworkStatusChangeListener {
                override fun onNetworkStatusChange(status: NetWorkStatus) {
                    logd("网络状态变化 ：${status}")
                }

                override fun onNetworkAvailable() {
                    logd("网络状态处于可用状态")
                }

            })
    }

    override fun onDestroy() {
        super.onDestroy()
        NetWorkStatusHelper.unRegisterNetworkStatusChange(this)
    }
}