package ru.karasevm.privatednstoggle

import android.app.Application
import com.google.android.material.color.DynamicColors
import ru.karasevm.privatednstoggle.data.DnsServerRepository
import ru.karasevm.privatednstoggle.data.database.DnsServerRoomDatabase

class PrivateDNSApp : Application() {

    private val database by lazy { DnsServerRoomDatabase.getDatabase(this) }
    val repository by lazy { DnsServerRepository(database.dnsServerDao()) }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}