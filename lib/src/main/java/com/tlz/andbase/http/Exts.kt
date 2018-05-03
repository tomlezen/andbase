package com.tlz.andbase.http

import com.tlz.andbase.observeOnUI
import io.reactivex.Observable
import java.net.URLConnection

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 15:38.
 */

internal fun String.getFileName(): String {
  val separatorIndex = lastIndexOf("/")
  var name = if (separatorIndex < 0) this else substring(separatorIndex + 1, length)
  name = name.replace("/", "_").replace("?", "_").replace("-", "_")
  if (name.length > 30) {
    name = name.substring(0, 30)
  }
  return "$name.temp"
}

internal fun String.guessMimeType(): String =
    URLConnection.getFileNameMap().getContentTypeFor(this) ?: "application/octet-stream"

internal fun ProgressCallback.sendProgress(progress: Int) =
    Observable.just(progress)
        .observeOnUI()
        .subscribe { onProgress(progress) }


internal fun ProgressCallback.sendFileSize(size: Long) =
    Observable.just(size)
        .observeOnUI()
        .subscribe { onFileSize(size) }

fun ProgressCallback(totalSize: (size: Long) -> Unit, progress: (progress: Int) -> Unit) =
    object : ProgressCallback {
      override fun onFileSize(size: Long) {
        totalSize(size)
      }

      override fun onProgress(progress: Int) {
        progress(progress)
      }
    }
