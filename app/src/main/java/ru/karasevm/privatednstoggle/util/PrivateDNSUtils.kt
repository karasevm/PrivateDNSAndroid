package ru.karasevm.privatednstoggle.util

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.karasevm.privatednstoggle.data.DnsServerRepository
import ru.karasevm.privatednstoggle.model.DnsServer
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoMode

object PrivateDNSUtils {
    const val DNS_MODE_OFF = "off"
    const val DNS_MODE_AUTO = "opportunistic"
    const val DNS_MODE_PRIVATE = "hostname"

    // What options to use when cycling
    const val AUTO_MODE_OPTION_OFF = 0
    const val AUTO_MODE_OPTION_AUTO = 1
    const val AUTO_MODE_OPTION_OFF_AUTO = 2
    const val AUTO_MODE_OPTION_PRIVATE = 3

    private const val PRIVATE_DNS_MODE = "private_dns_mode"
    private const val PRIVATE_DNS_PROVIDER = "private_dns_specifier"


    // Gets the system dns mode
    fun getPrivateMode(contentResolver: ContentResolver): String? {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)
    }

    // Gets the system dns provider
    fun getPrivateProvider(contentResolver: ContentResolver): String? {
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
        return (checkSelfPermission(
            context, Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Gets next dns address from the database,
     * if current address is last or unknown returns null
     *
     * @param currentAddress currently set address
     * @return next address
     */
    suspend fun getNextAddress(
        repository: DnsServerRepository, currentAddress: String?
    ): DnsServer? {
        return if (currentAddress.isNullOrEmpty()) {
            repository.getFirstEnabled()
        } else {
            repository.getNextByServer(currentAddress)
        }
    }

    /**
     * Gets next dns mode while preserving the current dns provider
     *
     * @param sharedPreferences shared preferences
     * @param scope coroutine scope
     * @param repository dns server repository
     * @param contentResolver content resolver
     * @param onNext callback to invoke with the next dns mode and current dns provider
     */
    fun getNextMode(
        sharedPreferences: SharedPreferences,
        scope: CoroutineScope,
        repository: DnsServerRepository,
        contentResolver: ContentResolver,
        onNext: ((String, String?) -> Unit)
    ) {
        Log.d("PrivateDNSUtils", "getNextMode: called")
        val systemDnsMode = getPrivateMode(contentResolver)
        val systemDnsProvider = getPrivateProvider(contentResolver)


        // System dns mode is off or auto
        if (systemDnsMode.equals(DNS_MODE_OFF, ignoreCase = true) || systemDnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            if (systemDnsProvider == null) { // no provider set, use regular logic
                getNextProvider(
                    sharedPreferences,
                    scope,
                    repository,
                    contentResolver,
                    onNext = { mode, provider -> onNext(mode, provider) })
            } else {
                if (systemDnsMode.equals(DNS_MODE_OFF, ignoreCase = true) && (sharedPreferences.autoMode == AUTO_MODE_OPTION_OFF_AUTO || sharedPreferences.autoMode == AUTO_MODE_OPTION_AUTO)) {
                    // If system dns mode is off and auto is enabled switch to auto
                    onNext(DNS_MODE_AUTO, systemDnsProvider)
                } else {
                    // If system dns mode is off or auto, the next mode is private, and the system dns provider is set
                    onNext(DNS_MODE_PRIVATE, systemDnsProvider)
                }
            }
        } else {
            // System dns mode is private
            when (sharedPreferences.autoMode) {
                AUTO_MODE_OPTION_PRIVATE -> onNext(DNS_MODE_OFF, systemDnsProvider)
                AUTO_MODE_OPTION_AUTO -> onNext(DNS_MODE_AUTO, systemDnsProvider)
                AUTO_MODE_OPTION_OFF_AUTO -> onNext(DNS_MODE_OFF, systemDnsProvider)
                AUTO_MODE_OPTION_OFF -> onNext(DNS_MODE_OFF, systemDnsProvider)
            }
        }
    }

    /**
     * Gets next dns provider from the database, taking into account the current
     * auto mode and private dns mode.
     *
     * @param sharedPreferences shared preferences
     * @param scope coroutine scope
     * @param repository dns server repository
     * @param contentResolver content resolver
     * @param onNext callback to invoke with the next dns mode and current dns provider
     */
    fun getNextProvider(
        sharedPreferences: SharedPreferences,
        scope: CoroutineScope,
        repository: DnsServerRepository,
        contentResolver: ContentResolver,
        onNext: ((String, String?) -> Unit)
    ) {
        Log.d("PrivateDNSUtils", "getNextProvider: called")
        val systemDnsMode = getPrivateMode(contentResolver)
        val systemDnsProvider = getPrivateProvider(contentResolver)

        // System dns mode is off
        if (systemDnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
            if (sharedPreferences.autoMode == AUTO_MODE_OPTION_AUTO || sharedPreferences.autoMode == AUTO_MODE_OPTION_OFF_AUTO) {
                // if auto available set to auto preserving provider
                onNext.invoke(DNS_MODE_AUTO, systemDnsProvider)
            } else {
                // otherwise set to private with first provider
                scope.launch {
                    onNext.invoke(
                        DNS_MODE_PRIVATE,
                        getNextAddress(repository, null)?.server
                    )
                }
            }
        // system dns mode is auto or unknown
        } else if (systemDnsMode == null || systemDnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            // set to private with first provider
            scope.launch {
                onNext.invoke(
                    DNS_MODE_PRIVATE,
                    getNextAddress(repository, null)?.server
                )
            }
        // system dns mode is private
        } else if (systemDnsMode.equals(DNS_MODE_PRIVATE, ignoreCase = true)) {
            scope.launch {
                val nextAddress = getNextAddress(repository, systemDnsProvider)
                if (nextAddress == null) {
                    // if there are no more providers, set to first
                    if (sharedPreferences.autoMode == AUTO_MODE_OPTION_PRIVATE) {
                        onNext.invoke(DNS_MODE_PRIVATE, getNextAddress(repository, null)!!.server)
                    } else {
                        // otherwise set to auto or off
                        if (sharedPreferences.autoMode == AUTO_MODE_OPTION_AUTO) {
                            onNext.invoke(DNS_MODE_AUTO, systemDnsProvider)
                        } else {
                            onNext.invoke(DNS_MODE_OFF, systemDnsProvider)
                        }
                    }
                } else {
                    // otherwise set to private with next provider
                    onNext.invoke(DNS_MODE_PRIVATE, nextAddress.server)
                }
            }
        }
    }

}