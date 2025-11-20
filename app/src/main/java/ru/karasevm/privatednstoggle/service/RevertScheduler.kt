package ru.karasevm.privatednstoggle.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import ru.karasevm.privatednstoggle.util.PreferenceHelper
import java.util.concurrent.TimeUnit
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertScheduledAt
import android.util.Log

object RevertScheduler {
    private const val TAG = "RevertScheduler"
    private const val ACTION_REVERT = "ru.karasevm.privatednstoggle.ACTION_REVERT"

    fun scheduleRevert(context: Context, minutes: Int) {
        val prefs = PreferenceHelper.defaultPreference(context)

        val logMsg1 = "scheduleRevert: CALLED with minutes=$minutes"
        Log.d(TAG, logMsg1)

        // Build intent to fire our RevertReceiver
        val intent = Intent(context, RevertReceiver::class.java).apply {
            action = ACTION_REVERT
            setPackage(context.packageName)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Use elapsed realtime + minutes
        val triggerAt = SystemClock.elapsedRealtime() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        val triggerAtWall = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())

        val logMsg2 = "scheduleRevert: setting alarm triggerAt(elapsed)=$triggerAt triggerAt(wall)=$triggerAtWall"
        Log.d(TAG, logMsg2)

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
            
            val logMsg3 = "scheduleRevert: alarm SET SUCCESSFULLY"
            Log.d(TAG, logMsg3)
        } catch (e: Exception) {
            val logMsg3 = "scheduleRevert: ERROR setting alarm: ${e.message}"
            Log.e(TAG, logMsg3)
        }

        // Persist scheduled time for debugging
        prefs.revertScheduledAt = triggerAtWall
        val logMsg4 = "scheduleRevert: persisted scheduled_at=$triggerAtWall"
        Log.d(TAG, logMsg4)
    }

    fun cancelRevert(context: Context) {
        val logMsg1 = "cancelRevert: CALLED"
        Log.d(TAG, logMsg1)

        val intent = Intent(context, RevertReceiver::class.java).apply {
            action = ACTION_REVERT
            setPackage(context.packageName)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        val prefs = PreferenceHelper.defaultPreference(context)
        prefs.revertScheduledAt = 0L
        val logMsg2 = "cancelRevert: CANCELLED pending revert"
        Log.d(TAG, logMsg2)
    }
}
