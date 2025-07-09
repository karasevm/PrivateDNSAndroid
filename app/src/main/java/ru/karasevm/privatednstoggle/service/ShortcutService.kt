package ru.karasevm.privatednstoggle.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.karasevm.privatednstoggle.PrivateDNSApp
import ru.karasevm.privatednstoggle.data.DnsServerRepository
import ru.karasevm.privatednstoggle.util.PreferenceHelper
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils

class ShortcutService : Service() {

    private val repository: DnsServerRepository by lazy { (application as PrivateDNSApp).repository }
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val ACTION_DO_CYCLE = "privatednstoggle://do_cycle" // regular cycle
        private const val ACTION_SWITCH_MODE = "privatednstoggle://switch_mode" // toggle private and non-private modes
        private const val TAG = "ShortcutService"
    }

    /**
     * Sets the dns mode and shows a toast with the new mode.
     *
     * @param dnsMode the new dns mode, one of [PrivateDNSUtils.DNS_MODE_OFF],
     * [PrivateDNSUtils.DNS_MODE_AUTO], or [PrivateDNSUtils.DNS_MODE_PRIVATE].
     * @param dnsProvider the dns provider to set when [dnsMode] is
     * [PrivateDNSUtils.DNS_MODE_PRIVATE].
     */
    private fun setDnsMode(dnsMode: String, dnsProvider: String? = null) {
        Log.d(TAG, "setDnsMode: attempting to set dns mode to $dnsMode with provider $dnsProvider")
        if (dnsMode == PrivateDNSUtils.DNS_MODE_PRIVATE) {
            PrivateDNSUtils.setPrivateProvider(contentResolver, dnsProvider)
        }
        PrivateDNSUtils.setPrivateMode(contentResolver, dnsMode)
//        val text = when (dnsMode) {
//            // TODO: Localize
//            PrivateDNSUtils.DNS_MODE_OFF -> "DNS set to Off"
//            PrivateDNSUtils.DNS_MODE_AUTO -> "DNS set to Auto"
//            PrivateDNSUtils.DNS_MODE_PRIVATE -> "DNS set to Private Provider"
//            else -> "Unknown"
//        }
//        scope.launch {
//            launch(Dispatchers.Main) {
//                Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.data.toString()
        val sharedPreferences = PreferenceHelper.defaultPreference(this)
        Log.d(TAG, "onStartCommand: got intent $data")
        if (PrivateDNSUtils.checkForPermission(this)) {
            when (data) {
                ACTION_DO_CYCLE -> {
                    PrivateDNSUtils.getNextProvider(
                        sharedPreferences,
                        scope,
                        repository,
                        contentResolver,
                        onNext = { dnsMode, dnsProvider ->
                            setDnsMode(dnsMode, dnsProvider)
                        })
                }

                ACTION_SWITCH_MODE -> {
                    PrivateDNSUtils.getNextMode(
                        sharedPreferences,
                        scope,
                        repository,
                        contentResolver,
                        onNext = { dnsMode, dnsProvider ->
                            setDnsMode(dnsMode, dnsProvider)
                        })
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}