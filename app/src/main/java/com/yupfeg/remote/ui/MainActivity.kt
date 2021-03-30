package com.yupfeg.remote.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.yupfeg.remote.R

class MainActivity : AppCompatActivity() {
    private val mViewModel : MainViewModel by lazy(LazyThreadSafetyMode.NONE){
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_base_use_1).setOnClickListener {
            mViewModel.queryAdverts()
        }

        findViewById<View>(R.id.btn_base_use_2).setOnClickListener {
            mViewModel.queryUserCollect()
        }

        findViewById<View>(R.id.btn_replace_url_1).setOnClickListener {
            mViewModel.getRedirectUrlBaiduData()
        }

        findViewById<View>(R.id.btn_replace_url_2).setOnClickListener {
            mViewModel.queryRedirectUrlUserData()
        }
    }
}