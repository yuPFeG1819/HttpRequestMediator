package com.yupfeg.remote.tools.status

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
import com.yupfeg.remote.log.HttpLogPrinter

/**
 * 网络连接状态
 * @author yuPFeG
 * @date 2020/03/13
 */
enum class NetWorkStatus{ WIFI,MOBILE,OTHER,NONE }

/**
 * 网络连接状态变化监听
 * @author yuPFeG
 * @date 2021/09/24
 */
interface NetworkStatusChangeListener {

    /**
     * 网络连接状态变化回调
     * @param status 网络连接状态
     * */
    fun onNetworkStatusChange(status : NetWorkStatus)

    /**
     * 网络状态可用时回调
     * */
    fun onNetworkAvailable()
}

/**
 * 网络状态变化管理类
 * * 兼容Android 6.0以下版本，将两种版本统一成相同格式的使用方式
 * @author yuPFeG
 * @date 2020/03/13
 */
@Suppress("unused")
object NetWorkStatusHelper {
    private const val NETWORK_LOG_TAG = "NetWorkStatusHelper"

    private var mLogPrinter : HttpLogPrinter ?= null

    /**
     * 网络状态变化监听
     * */
    private var mNetworkStatusChangeListener : NetworkStatusChangeListener? = null

    /**
     * 当前的网络状态
     * */
    private var mCurrStatus = NetWorkStatus.NONE

    /**Android6.0以下，监听网络状态变化的广播接收*/
    private val oldVersionNetworkReceiver : BroadcastReceiver
        by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
            createOldVersionNetworkStatusReceiver()
        }

    /**android 6.0以上的网络状态变化监听*/
    private val netWorkStatusCallBack : ConnectivityManager.NetworkCallback
        by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
            createNetworkStatusCallBack()
        }


    /**
     * 设置日志输出类
     * * 推荐替换为外部统一的日志输出对象，统一管理日志输出
     * */
    @JvmStatic fun setLogPrinter(logPrinter: HttpLogPrinter) {
        mLogPrinter = logPrinter
    }

    // <editor-fold desc="网络状态判断">

    /**
     * 判断网络是否可用
     *
     * Must hold `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" `
     * */
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
            return networkInfo != null && networkInfo.isConnected
                    && networkInfo.type == ConnectivityManager.TYPE_MOBILE
        }
        return false
    }

    // </editor-fold>

    /**
     * 获取当前网络连接状态
     * */
    fun getCurrNetworkStatus() = mCurrStatus

    /**注册监听网络状态变化*/
    @SuppressLint("ObsoleteSdkInt")
    @JvmStatic
    fun registerNetWorkStatusChange(context: Context,listener: NetworkStatusChangeListener){
        mNetworkStatusChangeListener = listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            val connectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder().build(),
                netWorkStatusCallBack
            )
        }else{
            //6.0以下注册广播监听
            val filter = IntentFilter()
            @Suppress("DEPRECATION")
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            context.applicationContext.registerReceiver(oldVersionNetworkReceiver,filter)
        }
    }
    /**注销网络状态变化监听*/
    @SuppressLint("ObsoleteSdkInt")
    @JvmStatic
    fun unRegisterNetworkStatusChange(context : Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            val connectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(netWorkStatusCallBack)
        }else{
            context.applicationContext.unregisterReceiver(oldVersionNetworkReceiver)
        }
    }

    /**
     * 创建6.0以下的网络状态回调
     * */
    @Suppress("DEPRECATION")
    private fun createOldVersionNetworkStatusReceiver() : BroadcastReceiver
        = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                @Suppress("DEPRECATION")
                if (intent?.action != ConnectivityManager.CONNECTIVITY_ACTION) return
                val manager = context?.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as ConnectivityManager
                manager.activeNetworkInfo?.apply {
                    if (!isConnected) {
                        //网络连接断开
                        mLogPrinter?.printDebugLog(NETWORK_LOG_TAG,"network status is lost")
                        dispatchNetworkStatusChange(NetWorkStatus.NONE)
                        return
                    }
                    dispatchNetworkAvailable()
                    when (type) {
                        ConnectivityManager.TYPE_MOBILE -> {
                            mLogPrinter?.printDebugLog(
                                NETWORK_LOG_TAG,"network status changed : mobile"
                            )
                            dispatchNetworkStatusChange(NetWorkStatus.MOBILE)
                        }
                        ConnectivityManager.TYPE_WIFI -> {
                            mLogPrinter?.printDebugLog(
                                NETWORK_LOG_TAG,"network status changed : wifi "
                            )
                            mCurrStatus = NetWorkStatus.WIFI
                            dispatchNetworkStatusChange(NetWorkStatus.WIFI)
                        }
                        else ->{
                            mLogPrinter?.printDebugLog(
                                NETWORK_LOG_TAG,"network status changed : other"
                            )
                            dispatchNetworkStatusChange(NetWorkStatus.OTHER)
                        }
                    }
                }
            }
        }

    /**
     *
     * */
    private fun createNetworkStatusCallBack() : ConnectivityManager.NetworkCallback
        = object : ConnectivityManager.NetworkCallback(){
            /**
             * 网络可用的回调
             * */
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                mLogPrinter?.printDebugLog(NETWORK_LOG_TAG,"network is available")
                dispatchNetworkAvailable()
            }
            /**
             * 网络丢失的回调
             * */
            override fun onLost(network: Network) {
                super.onLost(network)
                mLogPrinter?.printDebugLog(NETWORK_LOG_TAG,"network status is lost")
                dispatchNetworkStatusChange(NetWorkStatus.NONE)
            }

            /**当网络状态修改但仍旧是可用状态时回调*/
            override fun onCapabilitiesChanged(network: Network,
                                               networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)){
                    return
                }

                when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        mLogPrinter?.printDebugLog(
                            NETWORK_LOG_TAG,"network status onCapabilitiesChanged : wifi"
                        )
                        mCurrStatus = NetWorkStatus.WIFI
                        mNetworkStatusChangeListener?.onNetworkStatusChange(NetWorkStatus.WIFI)
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        mLogPrinter?.printDebugLog(
                            NETWORK_LOG_TAG,"network status onCapabilitiesChanged : mobile"
                        )
                        mCurrStatus = NetWorkStatus.MOBILE
                        mNetworkStatusChangeListener?.onNetworkStatusChange(NetWorkStatus.MOBILE)
                    }
                    else -> {
                        mLogPrinter?.printDebugLog(
                            NETWORK_LOG_TAG,"network status onCapabilitiesChanged : other"
                        )
                        mCurrStatus = NetWorkStatus.OTHER
                        mNetworkStatusChangeListener?.onNetworkStatusChange(NetWorkStatus.OTHER)
                    }
                }
            }
        }

    /**
     * 分发网络状态变化监听的回调
     * */
    private fun dispatchNetworkStatusChange(status : NetWorkStatus){
        mCurrStatus = status
        mNetworkStatusChangeListener?.onNetworkStatusChange(status)
    }

    /**
     * 通知网络连接处于可用状态
     * */
    private fun dispatchNetworkAvailable(){
        mNetworkStatusChangeListener?.onNetworkAvailable()
    }

}