package com.yupfeg.sample.ui.replace

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yupfeg.sample.R
import com.yupfeg.sample.base.viewModelDelegate

/**
 * 动态替换BaseUrl的Activity
 * @author yuPFeG
 * @date 2021/09/27
 */
class ReplaceUrlActivity : AppCompatActivity(){

    private val mViewModel : ReplaceBaseUrlViewModel by viewModelDelegate()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replace_url)

        val btnJueJinQuery = findViewById<View>(R.id.btn_replace_url_jue_jin)
        val btnBaiduQuery = findViewById<View>(R.id.btn_replace_url_baidu)

        val tvRequestUrl = findViewById<TextView>(R.id.tv_replace_url_query_url)
        val tvResultContent = findViewById<TextView>(R.id.tv_replace_url_query_result_content)

        btnJueJinQuery.setOnClickListener {
            mViewModel.queryJueJinAdverts()
        }

        btnBaiduQuery.setOnClickListener {
            mViewModel.queryBaiduData()
        }

        mViewModel.httpQueryUrl.observe(this){requestUrl->
            tvRequestUrl.text = requestUrl
        }

        mViewModel.jsonStringLiveData.observe(this){jsonString->
            tvResultContent.text = jsonString
        }
    }
}