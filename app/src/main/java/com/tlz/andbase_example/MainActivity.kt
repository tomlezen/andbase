package com.tlz.andbase_example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.tlz.andbase.delayOnMainThread
import com.tlz.andbase.http.ProgressCallback
import com.tlz.andbase.http.RxDownloader
import com.tlz.andbase.persmission.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import java.lang.Exception
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

  private val tag = MainActivity::class.java.canonicalName

  private val okhttpClient = OkHttpClient.Builder().build()
  private val rxDownloader = RxDownloader.newInstance(okhttpClient)

  private val rxPermissons: RxPermissions by lazy { RxPermissions.with(this) }
  private var isRequesting = false

  private var disposable: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)


    btn_download.setOnClickListener {
      disposable = rxDownloader.download("https://nodejs.org/dist/v8.11.1/node-v8.11.1-linux-x64.tar.xz", cacheDir.absolutePath, object : ProgressCallback {
        override fun onProgress(progress: Int) {
          Log.d(tag, "下载进度$progress")
        }

        override fun onFileSize(size: Long) {
          Log.d(tag, "文件大小$size")
        }
      }).subscribe()
    }

    btn_cancel_download.setOnClickListener {
      disposable?.dispose()
    }

  }

  private fun requestPermission() {
    Log.d("MainActivity", "do requestPermission")
    isRequesting = true
    rxPermissons.request(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
        .delay(1000, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { isRequesting = false }
        .subscribe({
          if (it) {
            Log.d("MainActivity", "permission request successful")
          } else {
            Toast.makeText(this@MainActivity, "您已拒绝了权限，2s后将开启权限设置界面", Toast.LENGTH_LONG).show()
            delayOnMainThread(2, TimeUnit.SECONDS) {
              openPermissionSettingPage()
            }
          }
        })
  }

  private fun openPermissionSettingPage() {
    try {
      val intent = Intent()
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      intent.data = Uri.fromParts("package", packageName, null)
      startActivity(intent)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onResume() {
    super.onResume()
    Log.d("MainActivity", "do onResume")
    if (!isRequesting) {
      requestPermission()
    }
  }


}
