package ru.karasevm.privatednstoggle

import android.app.Application
import com.google.android.material.color.DynamicColors

class PrivateDNSApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}