package com.tlz.andbase.persmission

import android.support.v4.app.Fragment

/**
 * Created by tomlezen.
 * Data: 2018/5/2.
 * Time: 17:25.
 */
internal class PermissionFragment : Fragment() {

  companion object {
    const val TAG = "PermissionFragment"
    const val REQUEST_CODE = 10101
  }

  private var rxPermissions: RxPermissions? = null

  internal fun requestPermissions(rxPermissions: RxPermissions, permissions: Array<String>) {
    this@PermissionFragment.rxPermissions = rxPermissions
    requestPermissions(permissions, REQUEST_CODE)
  }

  internal fun removeSelf() {
    activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commitNowAllowingStateLoss()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                          grantResults: IntArray) {
    if (requestCode == REQUEST_CODE) {
      removeSelf()
      rxPermissions?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

}