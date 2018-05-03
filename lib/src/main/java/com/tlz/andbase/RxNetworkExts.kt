package com.tlz.andbase

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.annotation.RequiresPermission
import android.telephony.TelephonyManager
import android.text.TextUtils
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.MainThreadDisposable
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 14:37.
 */
@RequiresPermission(value = Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.observeNetworkAvailable(): Single<Boolean> =
    Single.create {
      try {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        it.onSuccess(networkInfo != null && networkInfo.isAvailable)
      } catch (exception: Exception) {
        it.onSuccess(false)
      }
    }

@SuppressLint("MissingPermission")
fun Context.observeNetworkState(): Observable<NetworkInfo.State> =
    observeBroadcast(IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
        .map {
          val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
          val info = connectivityManager.activeNetworkInfo
          if (info != null) {
            info.state
          } else {
            NetworkInfo.State.UNKNOWN
          }
        }

@SuppressLint("MissingPermission")
fun Context.observeNeworkType(): Observable<Int> =
    observeNetworkState()
        .map {
          val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
          val networkInfo = manager.activeNetworkInfo
          var networkType = NetworkType.INVALID
          if (networkInfo != null && networkInfo.isConnected) {
            val type = networkInfo.typeName
            if (type.equals("WIFI", ignoreCase = true)) {
              networkType = NetworkType.WIFI
            } else if (type.equals("MOBILE", ignoreCase = true)) {
              val proxyHost = android.net.Proxy.getDefaultHost()
              networkType = if (TextUtils.isEmpty(proxyHost)) if (isFastMobileNetwork()) NetworkType.N_3G else NetworkType.N_2G else NetworkType.WAP
            }
          }
          networkType
        }

private fun Context.isFastMobileNetwork(): Boolean {
  val telephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
  when (telephonyManager.networkType) {
    TelephonyManager.NETWORK_TYPE_1xRTT -> return false // ~ 50-100 kbps
    TelephonyManager.NETWORK_TYPE_CDMA -> return false // ~ 14-64 kbps
    TelephonyManager.NETWORK_TYPE_EDGE -> return false // ~ 50-100 kbps
    TelephonyManager.NETWORK_TYPE_EVDO_0 -> return true // ~ 400-1000 kbps
    TelephonyManager.NETWORK_TYPE_EVDO_A -> return true // ~ 600-1400 kbps
    TelephonyManager.NETWORK_TYPE_GPRS -> return false // ~ 100 kbps
    TelephonyManager.NETWORK_TYPE_HSDPA -> return true // ~ 2-14 Mbps
    TelephonyManager.NETWORK_TYPE_HSPA -> return true // ~ 700-1700 kbps
    TelephonyManager.NETWORK_TYPE_HSUPA -> return true // ~ 1-23 Mbps
    TelephonyManager.NETWORK_TYPE_UMTS -> return true // ~ 400-7000 kbps
    TelephonyManager.NETWORK_TYPE_EHRPD -> return true // ~ 1-2 Mbps
    TelephonyManager.NETWORK_TYPE_EVDO_B -> return true // ~ 5 Mbps
    TelephonyManager.NETWORK_TYPE_HSPAP -> return true // ~ 10-20 Mbps
    TelephonyManager.NETWORK_TYPE_IDEN -> return false // ~25 kbps
    TelephonyManager.NETWORK_TYPE_LTE -> return true // ~ 10+ Mbps
    TelephonyManager.NETWORK_TYPE_UNKNOWN -> return false
    else -> return false
  }
}

object NetworkType {

  /** 没有网络 */
  const val INVALID = 0
  /** wap网络 */
  const val WAP = 1
  /** 2G网络 */
  const val N_2G = 2
  /** 3G和3G以上网络，或统称为快速网络 */
  const val N_3G = 3
  /** wifi网络 */
  const val WIFI = 4
}
