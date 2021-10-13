package com.yupfeg.sample.ui.normal

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yupfeg.sample.R
import com.yupfeg.sample.base.viewModelDelegate

/**
 * 进行基础网络请求Activity
 * @author yuPFeG
 * @date 2021/09/25
 */
class NormalUseActivity : AppCompatActivity(){

    private val mViewModel : NormalUseViewModel by viewModelDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_normal_use)
        initView()
    }

    private fun initView(){
        val btnRxJava3 = findViewById<View>(R.id.btn_normal_use_rxjava3)
        val btnCoroutine = findViewById<View>(R.id.btn_normal_use_coroutine)

        val tvResultContent = findViewById<TextView>(R.id.tv_normal_use_result_content)

        btnRxJava3.setOnClickListener{
            mViewModel.queryWanAndroidArticleByRxJava3()
        }
        btnCoroutine.setOnClickListener {
            mViewModel.queryWanAndroidArticleByCoroutine()
        }

        mViewModel.articleJsonStringLiveData.observe(this){jsonString->
            tvResultContent.text = jsonString
        }
    }
}