package com.tlz.andbase.http

import io.reactivex.Single
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.rx2.asSingle
import okhttp3.*
import java.io.File
import java.io.IOException

/**
 * Created by tomlezen.
 * Data: 2018/5/3.
 * Time: 10:41.
 */
class RxUploader private constructor(private val httpClient: OkHttpClient) {

  fun upload(serviceUrl: String, fileKey: String, fileValue: String) =
      upload(serviceUrl, arrayOf(Param(fileKey, fileValue)), null, null)

  fun upload(serviceUrl: String, fileKey: String, fileValue: String, params: Array<Param>) =
      upload(serviceUrl, arrayOf(Param(fileKey, fileValue)), params, null)

  fun upload(serviceUrl: String, fileKey: String, fileValue: String, params: Array<Param>, progressCallback: ProgressCallback) =
      upload(serviceUrl, arrayOf(Param(fileKey, fileValue)), params, progressCallback)

  fun upload(serviceUrl: String, files: Array<Param>, params: Array<Param>?, progressCallback: ProgressCallback?): Single<String> {
    val deferred = CompletableDeferred<String>()
    val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
    val headersBuilder = Headers.Builder()
    params?.forEach {
      headersBuilder.add(it.key, it.value)
    }
    val headers = headersBuilder.build()
    if(headers.size() > 0){
      builder.addPart(headers, RequestBody.create(null, ""))
    }
    files.filter { File(it.value).exists() }
        .forEach {
          val file = File(it.value)
          val fileName = file.name
          builder.addFormDataPart(it.key, fileName, RequestBody.create(MediaType.parse(fileName.guessMimeType()), file))
        }
    try {
      val requestBody = builder.build()
      val request = Request.Builder()
          .url(serviceUrl)
          .post(if(progressCallback == null) requestBody else ProgressRequestBody(requestBody, progressCallback))
          .tag(serviceUrl)
          .build()
      val call = httpClient.newCall(request)
      call.enqueue(ResponseCallback(deferred))
    }catch (e: Exception){
      deferred.completeExceptionally(e)
    }
    return deferred.asSingle(DefaultDispatcher)
  }

  private inner class ResponseCallback(private val deferred: CompletableDeferred<String>): Callback {
    override fun onFailure(call: Call?, e: IOException?) {
      e?.apply { deferred.completeExceptionally(this) }
    }

    override fun onResponse(call: Call?, response: Response?) {
      try {
        if (!deferred.isCancelled) {
          if (response?.isSuccessful == true) {
            val content = response.body()?.string() ?: throw NullPointerException("response body is null")
            deferred.complete(content)
          } else {
            deferred.completeExceptionally(Exception("code = ${response?.code() ?: "null"}"))
          }
        }
      } catch (e: Exception) {
        deferred.completeExceptionally(e)
      }
    }
  }

  companion object {
    @JvmStatic
    fun newInstance(okHttpClient: OkHttpClient) = RxUploader(okHttpClient)
  }

}