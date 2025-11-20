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
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RevertReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "RevertReceiver"
    }

    private fun writeDebugLog(context: Context, message: String) {
        try {
            val logFile = File(context.cacheDir, "revert_debug.log")
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            val line = "[$timestamp] $message\n"
            logFile.appendText(line)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write debug log: ${e.message}")
        }
    }

    private fun showNotification(context: Context, message: String) {
        try {
            val channelId = "revert_debug"
            val notificationId = 12345

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel for Android 8+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Revert Debug", NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle("DNS Revert")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification: ${e.message}")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: revert alarm fired - ENTERING BROADCAST RECEIVER")
        writeDebugLog(context, "onReceive: revert alarm fired - ENTERING BROADCAST RECEIVER")

        val prefs = PreferenceHelper.defaultPreference(context)
        val mode = prefs.revertMode
        val provider = prefs.revertProvider
        val scheduledAt = prefs.revertScheduledAt

        val logMsg = "onReceive: stored revert mode=$mode provider=$provider scheduledAt=$scheduledAt now=${System.currentTimeMillis()}"
        Log.d(TAG, logMsg)
        writeDebugLog(context, logMsg)

        if (mode.isNullOrBlank()) {
            Log.d(TAG, "onReceive: nothing to revert")
            writeDebugLog(context, "onReceive: nothing to revert - returning")
            return
        }

        writeDebugLog(context, "onReceive: applying revert to mode=$mode")

        // Apply provider first if private
        if (mode.equals(PrivateDNSUtils.DNS_MODE_PRIVATE, true)) {
            PrivateDNSUtils.setPrivateProvider(context.contentResolver, if (provider.isNullOrBlank()) null else provider)
        } else {
            // when reverting to non-private, preserve provider value
            PrivateDNSUtils.setPrivateProvider(context.contentResolver, provider)
        }

        PrivateDNSUtils.setPrivateMode(context.contentResolver, mode)

        val revertMsg = "Private DNS reverted to $mode"

        // Show notification (persistent & visible)
        showNotification(context, revertMsg)

        // Notify user via toast for debugging
        try {
            Toast.makeText(context, revertMsg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "onReceive: failed to show toast: ${e.message}")
        }

        // clear saved revert info
        prefs.revertMode = null
        prefs.revertProvider = null

        val finalMsg = "onReceive: reverted to mode=$mode provider=$provider"
        Log.d(TAG, finalMsg)
        writeDebugLog(context, finalMsg)
    }
}
