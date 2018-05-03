package com.tlz.andbase

import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 14:56.
 */
fun <T> applyUIForObservable(scheduler: Scheduler = Schedulers.io()): ObservableTransformer<T, T> {
  return ObservableTransformer {
    it.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())
  }
}

fun <T> applyUIForFlowable(scheduler: Scheduler = Schedulers.io()): FlowableTransformer<T, T> {
  return FlowableTransformer {
    it.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())
  }
}

fun <T> applyUIForSingle(scheduler: Scheduler = Schedulers.io()): SingleTransformer<T, T> {
  return SingleTransformer {
    it.subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread())
  }
}

fun <T> Observable<T>.applyUI(scheduler: Scheduler = Schedulers.io()): Observable<T> =
    this.compose(applyUIForObservable(scheduler))

fun <T> Observable<T>.observeOnUI(): Observable<T> =
    this.observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.subscribeOnIO(): Observable<T> =
    this.subscribeOn(Schedulers.io())

fun <T> Single<T>.applyUI(scheduler: Scheduler = Schedulers.io()): Single<T> =
    this.compose(applyUIForSingle(scheduler))

fun <T> Single<T>.observeOnUI(): Single<T> =
    this.observeOn(AndroidSchedulers.mainThread())

fun <T> Single<T>.subscribeOnIO(): Single<T> =
    this.subscribeOn(Schedulers.io())

fun <T> Flowable<T>.applyUI(scheduler: Scheduler = Schedulers.io()): Flowable<T> =
    this.compose(applyUIForFlowable(scheduler))

fun <T> Flowable<T>.observeOnUI(): Flowable<T> =
    this.observeOn(AndroidSchedulers.mainThread())

fun <T> Flowable<T>.subscribeOnIO(): Flowable<T> =
    this.subscribeOn(Schedulers.io())

fun countdown(time: Int, unit: TimeUnit): Flowable<Int> {
  val countTime = if (time < 0) 0 else time
  return Flowable.interval(0, 1, unit)
      .map { increaseTime -> countTime - increaseTime.toInt() }
      .take((countTime + 1).toLong())
      .observeOnUI()
}

fun delay(time: Long, block: () -> Unit): Job =
    delay(time, TimeUnit.MILLISECONDS, block)

fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, block: () -> Unit): Job = launch {
  delay(time, unit)
  block.invoke()
}

fun delayOnMainThread(millisDelayTime: Long, block: () -> Unit): Job =
    delayOnMainThread(millisDelayTime, TimeUnit.MILLISECONDS, block)

fun delayOnMainThread(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, block: () -> Unit): Job = launch(UI) {
  delay(time, unit)
  block.invoke()
}

fun <T> async(context: CoroutineContext = DefaultDispatcher, start: CoroutineStart = CoroutineStart.DEFAULT, parent: Job? = null, block: suspend CoroutineScope.() -> T) =
    kotlinx.coroutines.experimental.async(context, start, parent, block)

fun ui(parent: Job? = null, block: suspend CoroutineScope.() -> Unit) =
    launch(UI, CoroutineStart.DEFAULT, parent, block)