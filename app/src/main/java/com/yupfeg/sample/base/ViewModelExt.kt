package com.yupfeg.sample.base

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.yupfeg.sample.base.ViewModelDelegate

/**
 * [ComponentActivity]的拓展函数，Activity生命周期作用域的ViewModel
 * * 用于by关键字委托创建viewModel实例
 * */
@Suppress("unused")
@MainThread
inline fun <reified T : ViewModel> ComponentActivity.viewModelDelegate()
        = ViewModelDelegate(clazz = T::class,fromActivity = true)

/**
 * [Fragment]的拓展函数，
 * 获取与Fragment生命周期作用域的ViewModel
 * 用于by关键字委托创建viewModel实例
 */
@Suppress("unused")
@MainThread
inline fun <reified T : ViewModel> Fragment.viewModelDelegate()
        = ViewModelDelegate(clazz = T::class)

/**
 * [Fragment]的拓展函数
 * 获取与fragment所在Activity生命周期作用域的ViewModel
 * * 用于by关键字委托创建viewModel实例
 */
@Suppress("unused")
@MainThread
inline fun <reified T : ViewModel> Fragment.activityViewModelDelegate()
        = ViewModelDelegate(clazz = T::class, fromActivity = true)

/**
 * [Fragment]的拓展函数
 * 获取与Fragment嵌套的Parent Fragment生命周期作用域的ViewModel
 * * 用于by关键字委托创建viewModel实例
 */
@Suppress("unused")
@MainThread
inline fun <reified T : ViewModel> Fragment.parentFragmentViewModelDelegate()
        = ViewModelDelegate(clazz = T::class, fromParentFragment = true)