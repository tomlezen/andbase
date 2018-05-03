package com.tlz.andbase.http

import io.reactivex.Single
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.rx2.asSingle
import okhttp3.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 15:42.
 */
class RxDownloader private constructor(private val httpClient: OkHttpClient) {

  fun download(downloadUrl: String, saveFileDir: String): Single<File> =
      download(downloadUrl, saveFileDir, downloadUrl.getFileName(), null)

  fun download(downloadUrl: String, saveFileDir: String, saveFileName: String): Single<File> =
      download(downloadUrl, saveFileDir, saveFileName, null)

  fun download(downloadUrl: String, saveFileDir: String, progressCallback: ProgressCallback): Single<File> =
      download(downloadUrl, saveFileDir, downloadUrl.getFileName(), progressCallback)

  fun download(downloadUrl: String, saveFileDir: String, saveFileName: String, progressCallback: ProgressCallback?): Single<File> {
    val deferred = CompletableDeferred<File>()
    val request = Request.Builder()
        .url(downloadUrl)
        .tag(downloadUrl)
        .addHeader("Accept-Encoding", "identity")
        .build()
    val call = httpClient.newCall(request)
    call.enqueue(ResponseCallback(deferred, saveFileDir, saveFileName, progressCallback))
    deferred.invokeOnCompletion {
      if (deferred.isCancelled) {
        call.cancel()
      }
    }
    return deferred.asSingle(DefaultDispatcher)
  }

  private inner class ResponseCallback(
      private val deferred: CompletableDeferred<File>,
      private val saveFileDir: String,
      private val saveFileName: String?,
      private val progressCallback: ProgressCallback?
  ) : Callback {
    override fun onFailure(call: Call?, e: IOException?) {
      e?.let { deferred.completeExceptionally(it) }
    }

    override fun onResponse(call: Call?, response: Response?) {
      response?.body()?.let { body ->
        try {
          if (deferred.isCancelled) {
            call?.cancel()
          } else {
            val totalSize = body.contentLength()
            body.source()
            body.byteStream()?.use {
              progressCallback?.sendFileSize(totalSize)
              val saveDir = File(saveFileDir).apply {
                if (!exists()) {
                  mkdirs()
                }
              }
              val fileName = "$saveFileName.temp"
              val file = File(saveDir, fileName)
              file.outputStream().use { out ->
                var bytesCopied: Long = 0
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = it.read(buffer)
                while (bytes >= 0 && !deferred.isCancelled) {
                  out.write(buffer, 0, bytes)
                  bytesCopied += bytes
                  progressCallback?.sendProgress((bytesCopied * 1.0f / totalSize * 100).toInt())
                  bytes = it.read(buffer)
                }
                if (bytesCopied == totalSize) {
                  val saveFile = File(saveFileDir, saveFileName)
                  if (file.renameTo(saveFile)) {
                    if (!deferred.isCancelled) {
                      deferred.complete(saveFile)
                    }
                  } else if (!deferred.isCancelled) {
                    deferred.completeExceptionally(FileNotFoundException())
                  }
                } else if (!deferred.isCancelled) {
                  deferred.completeExceptionally(FileNotFoundException())
                }
              }
            } ?: deferred.completeExceptionally(NullPointerException("InputStream is null!"))
          }
        } catch (e: Exception) {
          deferred.completeExceptionally(e)
        }
      } ?: deferred.completeExceptionally(NullPointerException("response body is null"))
    }
  }

  companion object {
    @JvmStatic
    fun newInstance(okHttpClient: OkHttpClient) = RxDownloader(okHttpClient)
  }

}