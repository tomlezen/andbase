package com.tlz.andbase

import android.annotation.SuppressLint
import android.content.SharedPreferences
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 15:08.
 */
class RxPreferences(val sharedPreferences: SharedPreferences) {

  private val subject = PublishSubject.create<String>()
  private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> subject.onNext(key) }

  init {
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
  }

  fun <T> get(key: String, defValue: T): T = with(sharedPreferences) {
    val res: Any = when (defValue) {
      is Int -> getInt(key, defValue)
      is Float -> getFloat(key, defValue)
      is Long -> getLong(key, defValue)
      is String -> getString(key, defValue)
      is Boolean -> getBoolean(key, defValue)
      else -> throw  IllegalArgumentException("This type cant be saved")
    }
    res as T
  }

  @SuppressLint("CommitPrefEdits")
  fun <T> put(key: String, value: T) = with(sharedPreferences.edit()) {
    when (value) {
      is Int -> putInt(key, value)
      is Float -> putFloat(key, value)
      is Long -> putLong(key, value)
      is String -> putString(key, value)
      is Boolean -> putBoolean(key, value)
      else -> throw  IllegalArgumentException("This type can be saved")
    }.apply()
  }

  private fun observe(key: String, emitOnStart: Boolean): Observable<String> {
    return subject
        .startWith(Observable.just(key).filter { emitOnStart })
        .filter { s -> key == s }
  }

  fun observeBoolean(key: String, defValue: Boolean, emitOnStart: Boolean): Observable<Boolean> {
    return observe(key, emitOnStart)
        .map { sharedPreferences.getBoolean(key, defValue) }
  }

  fun observeString(key: String, defValue: String, emitOnStart: Boolean): Observable<String> {
    return observe(key, emitOnStart)
        .map { sharedPreferences.getString(key, defValue) }
  }

  fun observeInt(key: String, defValue: Int, emitOnStart: Boolean): Observable<Int> {
    return observe(key, emitOnStart)
        .map { sharedPreferences.getInt(key, defValue) }
  }

  fun observeLong(key: String, defValue: Long, emitOnStart: Boolean): Observable<Long> {
    return observe(key, emitOnStart)
        .map { sharedPreferences.getLong(key, defValue) }
  }

  fun observeFloat(key: String, defValue: Float, emitOnStart: Boolean): Observable<Float> {
    return observe(key, emitOnStart)
        .map { sharedPreferences.getFloat(key, defValue) }
  }

  fun observeStringSet(key: String, defValue: Set<String>, emitOnStart: Boolean): Observable<Set<String>> {
    return observe(key, emitOnStart)
        .map { sharedPreferences.getStringSet(key, defValue) }
  }

  fun release() {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
  }

}