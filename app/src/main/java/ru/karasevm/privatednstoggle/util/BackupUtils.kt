package ru.karasevm.privatednstoggle.util

import android.content.SharedPreferences
import com.google.gson.annotations.SerializedName
import ru.karasevm.privatednstoggle.data.DnsServerViewModel
import ru.karasevm.privatednstoggle.model.DnsServer
import ru.karasevm.privatednstoggle.util.PreferenceHelper.AUTO_MODE
import ru.karasevm.privatednstoggle.util.PreferenceHelper.DNS_SERVERS
import ru.karasevm.privatednstoggle.util.PreferenceHelper.REQUIRE_UNLOCK
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoMode
import ru.karasevm.privatednstoggle.util.PreferenceHelper.requireUnlock

object BackupUtils {
    data class Backup(
        @SerializedName("dns_servers") val dnsServers: List<DnsServer>,
        @SerializedName("auto_mode") val autoMode: Int?,
        @SerializedName("require_unlock") val requireUnlock: Boolean?,
    )

    /**
     *  Exports all the preferences
     *  @param viewModel View model
     *  @param sharedPreferences Shared preferences
     */
    fun export(viewModel: DnsServerViewModel, sharedPreferences: SharedPreferences): Backup {
        return Backup(
            viewModel.allServers.value ?: listOf(),
            sharedPreferences.autoMode,
            sharedPreferences.requireUnlock
        )
    }

    /**
     *  Imports all the preferences
     *  @param backup Deserialized backup
     *  @param viewModel View model
     */
    fun import(
        backup: Backup,
        viewModel: DnsServerViewModel,
        sharedPreferences: SharedPreferences
    ) {
        backup.dnsServers.forEach { viewModel.insert(it) }
        sharedPreferences.autoMode = backup.autoMode ?: sharedPreferences.autoMode
        sharedPreferences.requireUnlock = backup.requireUnlock ?: sharedPreferences.requireUnlock
    }

    /**
     *  Imports old server list
     *  @param map Deserialized backup
     *  @param viewModel View model
     *  @param sharedPreferences Shared preferences
     */
    fun importLegacy(
        map: Map<String, Any>,
        viewModel: DnsServerViewModel,
        sharedPreferences: SharedPreferences
    ) {
        map[DNS_SERVERS]?.let { servers ->
            if (servers is String) {
                val serverList = servers.split(",")
                serverList.forEach { server ->
                    val parts = server.split(" : ")
                    if (parts.size == 2) {
                        viewModel.insert(DnsServer(0, parts[1], parts[0]))
                    } else {
                        viewModel.insert(DnsServer(0, server, ""))
                    }
                }
            }
        }
        sharedPreferences.autoMode = map[AUTO_MODE] as? Int ?: 0
        sharedPreferences.requireUnlock = map[REQUIRE_UNLOCK] as? Boolean ?: false
    }
}