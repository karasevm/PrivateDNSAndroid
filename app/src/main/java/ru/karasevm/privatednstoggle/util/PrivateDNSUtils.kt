package ru.karasevm.privatednstoggle.util

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import ru.karasevm.privatednstoggle.R

@Suppress("unused")
object PrivateDNSUtils {
    const val DNS_MODE_OFF = "off"
    const val DNS_MODE_AUTO = "opportunistic"
    const val DNS_MODE_PRIVATE = "hostname"

    const val AUTO_MODE_OPTION_OFF = 0
    const val AUTO_MODE_OPTION_AUTO = 1
    const val AUTO_MODE_OPTION_OFF_AUTO = 2
    const val AUTO_MODE_OPTION_PRIVATE = 3

    private const val PRIVATE_DNS_MODE = "private_dns_mode"
    private const val PRIVATE_DNS_PROVIDER = "private_dns_specifier"


    // Gets the system dns mode
    fun getPrivateMode(contentResolver: ContentResolver): String {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)
    }

    // Gets the system dns provider
    fun getPrivateProvider(contentResolver: ContentResolver): String {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_PROVIDER)
    }

    // Sets the system dns mode
    fun setPrivateMode(contentResolver: ContentResolver, value: String) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, value)
    }

    // Sets the system dns provider
    fun setPrivateProvider(contentResolver: ContentResolver, value: String?) {
        Settings.Global.putString(contentResolver, PRIVATE_DNS_PROVIDER, value)
    }

    fun checkForPermission(context: Context): Boolean {
        if (checkSelfPermission(
                context,
                Manifest.permission.WRITE_SECURE_SETTINGS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        Toast.makeText(context, R.string.permission_missing, Toast.LENGTH_SHORT).show()
        return false
    }

}