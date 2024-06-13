package ru.karasevm.privatednstoggle.utils

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import ru.karasevm.privatednstoggle.R

object PrivateDNSUtils {
    const val DNS_MODE_OFF = "off"
    const val DNS_MODE_AUTO = "opportunistic"
    const val DNS_MODE_PRIVATE = "hostname"

    private const val PRIVATE_DNS_MODE = "private_dns_mode"
    private const val PRIVATE_DNS_PROVIDER = "private_dns_specifier"

    fun getPrivateMode(contentResolver: ContentResolver): String {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)
    }

    fun getPrivateProvider(contentResolver: ContentResolver): String {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_PROVIDER)
    }

    fun setPrivateMode(contentResolver: ContentResolver, value: String) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, value)
    }

    fun setPrivateProvider(contentResolver: ContentResolver, value: String?) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_PROVIDER, value)
    }

    fun checkForPermission(context: Context): Boolean {
        if (checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        Toast.makeText(context, R.string.permission_missing, Toast.LENGTH_SHORT).show()
        return false
    }

}