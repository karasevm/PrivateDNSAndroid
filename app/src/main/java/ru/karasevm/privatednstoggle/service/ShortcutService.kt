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
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoRevertEnabled
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoRevertMinutes
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertMode
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertProvider
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
        
        // Auto-revert: capture current state BEFORE change and schedule revert
        try {
            val prefs = PreferenceHelper.defaultPreference(this)
            if (prefs.autoRevertEnabled) {
                // Save CURRENT state as revert target (will revert back to it after X minutes)
                val currentMode = PrivateDNSUtils.getPrivateMode(contentResolver)
                val currentProvider = PrivateDNSUtils.getPrivateProvider(contentResolver)
                prefs.revertMode = currentMode
                prefs.revertProvider = currentProvider
                RevertScheduler.scheduleRevert(this, prefs.autoRevertMinutes)
                Log.d(TAG, "setDnsMode: auto-revert scheduled. Will revert FROM $dnsMode back TO $currentMode in ${prefs.autoRevertMinutes} minute(s)")
            } else {
                // Auto-revert disabled; cancel any pending revert
                RevertScheduler.cancelRevert(this)
                prefs.revertMode = null
                prefs.revertProvider = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "setDnsMode: error with auto-revert: ${e.message}")
        }
        
        if (dnsMode == PrivateDNSUtils.DNS_MODE_PRIVATE) {
            PrivateDNSUtils.setPrivateProvider(contentResolver, dnsProvider)
        }
        PrivateDNSUtils.setPrivateMode(contentResolver, dnsMode)
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