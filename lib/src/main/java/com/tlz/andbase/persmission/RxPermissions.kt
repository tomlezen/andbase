package com.tlz.andbase.persmission

import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 17:26.
 */
class RxPermissions private constructor(activity: FragmentActivity) {

  private val activityRef = WeakReference<FragmentActivity>(activity)

  private val subjectMap = HashMap<String, PublishSubject<Permission>>()

  private var permissionFragment: PermissionFragment? = null

  fun request(permissions: Array<String>): Observable<Boolean> {
    return Observable.just(Any()).compose { upstream -> request(upstream, *permissions) }
  }

  private fun request(observable: Observable<Any>, vararg permissions: String): Observable<Boolean> {
    return doRequest(observable, *permissions)
        .buffer(permissions.size)
        .flatMap {
          if (it.isEmpty()) {
            return@flatMap Observable.empty<Boolean>()
          }
          Observable.just(!it.any { !it.granted })
        }
  }

  fun requestEach(vararg permissions: String): Observable<Permission> {
    return Observable.just(true).compose { upstream -> doRequest(upstream, *permissions) }
  }

  private fun doRequest(observable: Observable<*>, vararg permissions: String): Observable<Permission> {
    if (permissions.isEmpty()) {
      throw IllegalArgumentException("Requires at least one input permission")
    }
    return observable.flatMap { doRequest(*permissions) }
  }

  private fun doRequest(vararg permissions: String): Observable<Permission> {

    val list = ArrayList<Observable<Permission>>(permissions.size)
    val unrequestedPermissions = ArrayList<String>()

    Observable.fromArray(*permissions)
        .filter{ s ->
          if (isGranted(s)) {
            list.add(Observable.just(Permission(s, true)))
            return@filter false
          }
          if (isRevoked(s)) {
            list.add(Observable.just(Permission(s, false)))
            return@filter false
          }
          true
        }
        .subscribe({ s ->
          var subject: PublishSubject<Permission>? = subjectMap[s]
          if (subject == null) {
            unrequestedPermissions.add(s)
            subject = PublishSubject.create<Permission>()
            subjectMap[s] = subject
          }
          list.add(subject!!)
        })

    if (!unrequestedPermissions.isEmpty()) {
      if (permissionFragment == null) {
        permissionFragment = PermissionFragment()
      }
      permissionFragment?.let {
        if (it.isAdded && it.activity != activityRef.get()) {
          it.removeSelf()
          addFragment(it)
        } else if (!it.isAdded) {
          addFragment(it)
        }

        it.requestPermissions(this, unrequestedPermissions.toTypedArray())
      }
    }
    return Observable.concat(Observable.fromIterable(list))
  }

  private fun addFragment(permissionFragment: PermissionFragment) {
    activityRef.get()?.supportFragmentManager?.beginTransaction()?.add(permissionFragment, PermissionFragment.TAG)?.commitNowAllowingStateLoss()
  }

  internal fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    permissions.forEachIndexed { index, s ->
      val subject = subjectMap[s] ?: throw IllegalStateException("didn't find the corresponding permission request.")
      subjectMap.remove(s)
      val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
      subject.onNext(Permission(s, granted))
      subject.onComplete()
    }
  }

  fun shouldShowRequestPermissionRationale(activity: Activity, vararg permissions: String): Observable<Boolean> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return Observable.just(false)
    }
    return Observable.just(shouldShowRequestPermissionRationaleM(activity, *permissions))
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun shouldShowRequestPermissionRationaleM(activity: Activity, vararg permissions: String): Boolean {
    return permissions.none { !isGranted(it) && !activity.shouldShowRequestPermissionRationale(it) }
  }

  fun isGranted(permission: String): Boolean {
    return activityRef.get()?.let {
      return ActivityCompat.checkSelfPermission(it, permission) == PackageManager.PERMISSION_GRANTED
    } ?: false
  }

  fun isRevoked(permission: String): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return isM() && isRevokedM(permission)
    }
    return false
  }

  private fun isM(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun isRevokedM(permission: String): Boolean {
    return activityRef.get()?.let {
      return it.packageManager.isPermissionRevokedByPolicy(permission, it.packageName)
    } ?: false
  }

  companion object {
    @JvmStatic
    fun with(activity: FragmentActivity): RxPermissions = RxPermissions(activity)
  }

}