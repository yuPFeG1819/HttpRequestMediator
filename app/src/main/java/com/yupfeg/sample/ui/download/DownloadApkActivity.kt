package com.yupfeg.sample.ui.download

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yupfeg.sample.R
import com.yupfeg.sample.base.viewModelDelegate

/**
 * 测试下载单个文件的Activity
 * @author yuPFeG
 * @date
 */
class DownloadApkActivity : AppCompatActivity(){

    private val mViewModel : DownloadApkViewModel by viewModelDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_apk)

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar_download_apk)
        val btnDownload = findViewById<Button>(R.id.btn_download_apk)
        val tvProgress = findViewById<TextView>(R.id.tv_download_apk_progress)
        val tvFileSize = findViewById<TextView>(R.id.tv_download_apk_file_size)

        btnDownload.setOnClickListener{
            mViewModel.downloadApk()
        }

        mViewModel.downloadProgressLiveData.observe(this){progress->
            progressBar.progress = progress
        }

        mViewModel.downloadedProgressTextString.observe(this){progressText->
            tvProgress.text = progressText
        }

        mViewModel.downloadFileSizeTextStringLiveData.observe(this){fileSize->
            tvFileSize.text = fileSize
        }
    }

}