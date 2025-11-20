package ru.karasevm.privatednstoggle.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ru.karasevm.privatednstoggle.util.PreferenceHelper
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertMode
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertProvider
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertScheduledAt
import android.widget.Toast

class RevertReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "RevertReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: revert alarm fired")
        val prefs = PreferenceHelper.defaultPreference(context)
        val mode = prefs.revertMode
        val provider = prefs.revertProvider
        val scheduledAt = prefs.revertScheduledAt

        Log.d(TAG, "onReceive: stored revert mode=$mode provider=$provider scheduledAt=$scheduledAt now=${System.currentTimeMillis()}")

        if (mode.isNullOrBlank()) {
            Log.d(TAG, "onReceive: nothing to revert")
            return
        }

        // Apply provider first if private
        if (mode.equals(PrivateDNSUtils.DNS_MODE_PRIVATE, true)) {
            PrivateDNSUtils.setPrivateProvider(context.contentResolver, if (provider.isNullOrBlank()) null else provider)
        } else {
            // when reverting to non-private, preserve provider value
            PrivateDNSUtils.setPrivateProvider(context.contentResolver, provider)
        }

        PrivateDNSUtils.setPrivateMode(context.contentResolver, mode)

        // Notify user for debugging so we can confirm revert fired
        try {
            val message = "Private DNS reverted to ${mode ?: "(null)"}"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "onReceive: failed to show toast: ${e.message}")
        }

        // clear saved revert info
        prefs.revertMode = null
        prefs.revertProvider = null

        Log.d(TAG, "onReceive: reverted to mode=$mode provider=$provider")
    }
}
