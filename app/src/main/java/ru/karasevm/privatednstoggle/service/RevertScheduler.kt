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
    private const val ACTION_REVERT = "ru.karasevm.privatednstoggle.ACTION_REVERT"

    fun scheduleRevert(context: Context, minutes: Int) {
        val prefs = PreferenceHelper.defaultPreference(context)

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

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)

        // Persist scheduled time for debugging
        prefs.revertScheduledAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes.toLong())
        Log.d("RevertScheduler", "scheduleRevert: scheduled in $minutes minute(s), triggerAt(elapsed)=$triggerAt, scheduled_at=${prefs.revertScheduledAt}")
    }

    fun cancelRevert(context: Context) {
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
        Log.d("RevertScheduler", "cancelRevert: cancelled pending revert")
    }
}
