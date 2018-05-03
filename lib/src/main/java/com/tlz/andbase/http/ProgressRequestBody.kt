package com.tlz.andbase.http

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import java.io.IOException

/**
 * Created by tomlezen.
 * Data: 2018/5/3.
 * Time: 10:42.
 */
internal class ProgressRequestBody(private val requestBody: RequestBody, val progressCallback: ProgressCallback) : RequestBody() {

  private var bufferedSink: BufferedSink? = null

  override fun contentType(): MediaType? {
    return requestBody.contentType()
  }

  @Throws(IOException::class)
  override fun contentLength(): Long {
    try {
      return requestBody.contentLength()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return -1
  }

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) {
    try {
      if (bufferedSink == null) {
        bufferedSink = Okio.buffer(sink(sink))
      }
      requestBody.writeTo(bufferedSink!!)
      bufferedSink?.flush()
    } catch (e: Exception) {
      throw e
    }
  }

  private fun sink(sink: Sink): Sink {
    return object : ForwardingSink(sink) {
      internal var bytesWritten = 0L
      internal var contentLength = 0L
      @Throws(IOException::class)
      override fun write(source: Buffer, byteCount: Long) {
        super.write(source, byteCount)
        if (contentLength == 0L) {
          contentLength = contentLength()
          progressCallback.sendFileSize(contentLength)
        }
        bytesWritten += byteCount
        progressCallback.sendProgress((bytesWritten * 1.0f / contentLength * 100).toInt())
      }
    }
  }

}