package ru.karasevm.privatednstoggle.util

import android.content.SharedPreferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.karasevm.privatednstoggle.data.DnsServerViewModel
import ru.karasevm.privatednstoggle.model.DnsServer
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoMode
import ru.karasevm.privatednstoggle.util.PreferenceHelper.requireUnlock

object BackupUtils {
    @Serializable
    data class Backup(
        @SerialName("dns_servers") val dnsServers: List<DnsServer>,
        @SerialName("auto_mode") val autoMode: Int?,
        @SerialName("require_unlock") val requireUnlock: Boolean?,
    )

    @Serializable
    data class LegacyBackup(
        @SerialName("dns_servers") val dnsServers: String,
        @SerialName("auto_mode") val autoMode: Int?,
        @SerialName("require_unlock") val requireUnlock: Boolean?,
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
     *  @param legacyBackup Deserialized backup
     *  @param viewModel View model
     *  @param sharedPreferences Shared preferences
     */
    fun importLegacy(
        legacyBackup: LegacyBackup,
        viewModel: DnsServerViewModel,
        sharedPreferences: SharedPreferences
    ) {
        legacyBackup.dnsServers.let { servers ->
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
        sharedPreferences.autoMode = legacyBackup.autoMode?: 0
        sharedPreferences.requireUnlock = legacyBackup.requireUnlock?: false
    }
}