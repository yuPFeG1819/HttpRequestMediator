package com.yupfeg.remote

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresPermission
import com.yupfeg.logger.ext.logd
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * 网络连接状态
 * @author yuPFeG
 * @date 2020/03/13
 */
enum class NetWorkStatus{ WIFI,MOBILE,OTHER,NONE }

/**
 * 网络状态变化管理类
 * * 统一管理网络状态变化
 * * 外部通过观察BehaviorSubject的数据变化来获取当前网络状态
 * @author yuPFeG
 * @date 2020/03/13
 */
@Suppress("unused")
object NetWorkStatusHelper {
    private const val NETWORK_LOG_TAG = "NetWorkStatusHelper"

    @JvmStatic fun getInstance() = this

    /**
     * 判断网络是否可用
     *
     * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" `
     * */
    @SuppressLint("ObsoleteSdkInt")
    @Suppress("unused")
    @RequiresPermission(ACCESS_NETWORK_STATE)
    @JvmStatic
    fun isNetworkConnect(context : Context) : Boolean{
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            if (networkCapabilities != null) {
                return (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = manager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
        return false
    }

    /**
     * 判断Wifi连接是否可用
     *
     * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" `
     * */
    @SuppressLint("ObsoleteSdkInt")
    @RequiresPermission(ACCESS_NETWORK_STATE)
    @Suppress("unused")
    @JvmStatic
    fun isWifiConnected(context : Context) : Boolean{
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            if (networkCapabilities != null) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = manager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
                    && networkInfo.type == ConnectivityManager.TYPE_WIFI
        }
        return false
    }

    /**
     * 判断移动网络连接是否可用
     *
     * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" `
     * */
    @SuppressLint("ObsoleteSdkInt")
    @RequiresPermission(ACCESS_NETWORK_STATE)
    @JvmStatic
    fun isMobileConnected(context: Context) : Boolean{
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            if (networkCapabilities != null) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = manager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_MOBILE
        }
        return false
    }

    /**注册监听网络状态变化*/
    @SuppressLint("ObsoleteSdkInt")
    @JvmStatic
    fun registerNetWorkChange(context: Context){
        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().build(),
                networkCallback
            )
        }else{
            val filter = IntentFilter()
            @Suppress("DEPRECATION")
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            context.applicationContext.registerReceiver(oldVersionNetworkReceiver,filter)
        }
    }
    /**注销网络状态变化监听*/
    @SuppressLint("ObsoleteSdkInt")
    @JvmStatic
    fun unRegisterNetworkChange(context : Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            val connectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }else{
            context.applicationContext.unregisterReceiver(oldVersionNetworkReceiver)
        }
    }

    /**订阅网络状态变化*/
    fun observeNetWorkStatus() : Observable<NetWorkStatus> {
        return networkStatusSubject.hide().distinctUntilChanged()
    }

    /**网络状态的可观察对象*/
    private val networkStatusSubject = BehaviorSubject.createDefault(NetWorkStatus.NONE)

    /**Android6.0以下，监听网络状态变化的广播接收*/
    private val oldVersionNetworkReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            @Suppress("DEPRECATION")
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION){
                val manager =
                    context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = manager.activeNetworkInfo
                networkInfo?.apply {
                    if (!isConnected){
                        networkStatusSubject.onNext(NetWorkStatus.NONE)
                        return@apply
                    }
                    when(type){
                        ConnectivityManager.TYPE_MOBILE->{
                            networkStatusSubject.onNext(NetWorkStatus.MOBILE)
                        }
                        ConnectivityManager.TYPE_WIFI->{
                            networkStatusSubject.onNext(NetWorkStatus.WIFI)
                        }
                    }
                }
            }
        }

    }

    /**Android6.0以上的网络状态变化监听*/
    private val networkCallback = object : ConnectivityManager.NetworkCallback(){
        /**
         * 网络可用的回调
         * */
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            logd(NETWORK_LOG_TAG,"网络连接可用")
        }
        /**
         * 网络丢失的回调
         * */
        override fun onLost(network: Network) {
            super.onLost(network)
            logd(NETWORK_LOG_TAG,"网络连接已断开")
            networkStatusSubject.onNext(NetWorkStatus.NONE)
        }

        /**当网络状态修改但仍旧是可用状态时调用*/
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)){
                when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        logd(NETWORK_LOG_TAG,"onCapabilitiesChanged: 网络类型为wifi")
                        networkStatusSubject.onNext(NetWorkStatus.WIFI)
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        logd(NETWORK_LOG_TAG,"onCapabilitiesChanged: 网络类型为流量网络")
                        networkStatusSubject.onNext(NetWorkStatus.MOBILE)
                    }
                    else -> {
                        logd(NETWORK_LOG_TAG,"onCapabilitiesChanged: 网络类型为其他网络")
                        networkStatusSubject.onNext(NetWorkStatus.OTHER)
                    }
                }
            }
        }
    }

}