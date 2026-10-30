package ru.karasevm.privatednstoggle.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PreferenceHelper {

    const val DNS_SERVERS = "dns_servers"
    const val AUTO_MODE = "auto_mode"
    const val REQUIRE_UNLOCK = "require_unlock"
    const val AUTO_REVERT_ENABLED = "auto_revert_enabled"
    const val AUTO_REVERT_MINUTES = "auto_revert_minutes"
    const val REVERT_MODE = "revert_mode"
    const val REVERT_PROVIDER = "revert_provider"
    const val REVERT_SCHEDULED_AT = "revert_scheduled_at"

    fun defaultPreference(context: Context): SharedPreferences =
        context.getSharedPreferences("app_prefs", 0)

    private inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
        edit {
            operation(this)
        }
    }

    private fun SharedPreferences.Editor.put(pair: Pair<String, Any>) {
        val key = pair.first
        when (val value = pair.second) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            else -> error("Only primitive types can be stored in SharedPreferences, got ${value.javaClass}")
        }
    }

    var SharedPreferences.dns_servers
        get() = getString(DNS_SERVERS, "")!!.split(",").toMutableList()
        set(items) {
            editMe {
                it.put(DNS_SERVERS to items.joinToString(separator = ","))
            }
        }


    var SharedPreferences.autoMode
        get() = getInt(AUTO_MODE, PrivateDNSUtils.AUTO_MODE_OPTION_OFF)
        set(value) {
            editMe {
                it.put(AUTO_MODE to value)
            }
        }

    var SharedPreferences.autoRevertEnabled
        get() = getBoolean(AUTO_REVERT_ENABLED, false)
        set(value) {
            editMe {
                it.put(AUTO_REVERT_ENABLED to value)
            }
        }

    var SharedPreferences.autoRevertMinutes
        get() = getInt(AUTO_REVERT_MINUTES, 5)
        set(value) {
            editMe {
                it.put(AUTO_REVERT_MINUTES to value)
            }
        }

    var SharedPreferences.revertMode
        get() = getString(REVERT_MODE, null)
        set(value) {
            editMe {
                it.put(REVERT_MODE to (value ?: ""))
            }
        }

    var SharedPreferences.revertProvider
        get() = getString(REVERT_PROVIDER, null)
        set(value) {
            editMe {
                it.put(REVERT_PROVIDER to (value ?: ""))
            }
        }

    var SharedPreferences.revertScheduledAt
        get() = getLong(REVERT_SCHEDULED_AT, 0L)
        set(value) {
            editMe {
                it.put(REVERT_SCHEDULED_AT to value)
            }
        }

    var SharedPreferences.requireUnlock
        get() = getBoolean(REQUIRE_UNLOCK, false)
        set(value) {
            editMe {
                it.put(REQUIRE_UNLOCK to value)
            }
        }
}
