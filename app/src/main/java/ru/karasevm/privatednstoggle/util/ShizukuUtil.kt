package ru.karasevm.privatednstoggle.util

import android.Manifest
import android.content.Context
import android.content.pm.IPackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.permission.IPermissionManager
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.checkForPermission

object ShizukuUtil {

    private const val TAG = "ShizukuUtil"

    /**
     * Attempts to grant the WRITE_SECURE_SETTINGS permission using Shizuku.
     *
     * @param context The context from which the method is called.
     * @return True if the permission was granted successfully, false otherwise.
     */
    fun grantPermissionWithShizuku(context: Context): Boolean {
        val packageName = context.packageName
        var userId = 0
        runCatching {
            val userHandle = Process.myUserHandle()
            userId = UserHandle::class.java.getMethod("getIdentifier").invoke(userHandle) as? Int ?: 0
        }
        if (Build.VERSION.SDK_INT >= 31) {
            HiddenApiBypass.addHiddenApiExemptions("Landroid/permission")
            val binder =
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr"))
            val pm = IPermissionManager.Stub.asInterface(binder)
            runCatching {
                pm.grantRuntimePermission(
                    packageName,
                    Manifest.permission.WRITE_SECURE_SETTINGS,
                    userId
                )
            }.onFailure { e ->
                Log.w(TAG, "Android 12 method failed: ", e)
                runCatching {
                    pm.grantRuntimePermission(
                        packageName,
                        Manifest.permission.WRITE_SECURE_SETTINGS,
                        0,
                        userId
                    )
                }.onFailure { e ->
                    Log.w(TAG, "Android 14 QPR2 method failed: ", e)
                    runCatching {
                        pm.grantRuntimePermission(
                            packageName,
                            Manifest.permission.WRITE_SECURE_SETTINGS,
                            "default:0",
                            userId
                        )
                    }.onFailure { e ->
                        Log.w(TAG, "Android 14 QPR3 method failed: ", e)
                    }
                }
            }
        } else {
            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            val pm = IPackageManager.Stub.asInterface(binder)
            runCatching {
                pm.grantRuntimePermission(
                    packageName,
                    Manifest.permission.WRITE_SECURE_SETTINGS,
                    userId
                )
            }.onFailure { e ->
                Log.w(TAG, "Android <12 method failed: ", e)
            }
        }
        return checkForPermission(context)
    }
}