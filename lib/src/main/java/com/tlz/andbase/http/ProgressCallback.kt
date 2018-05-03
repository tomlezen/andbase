package com.tlz.andbase.http

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 15:39.
 */
interface ProgressCallback {
  fun onProgress(progress: Int)
  fun onFileSize(size: Long)
}