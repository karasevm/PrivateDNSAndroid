package ru.karasevm.privatednstoggle.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {

    private const val DNS_SERVERS = "dns_servers"
    private const val AUTO_MODE = "auto_mode"
    private const val REQUIRE_UNLOCK = "require_unlock"

    fun defaultPreference(context: Context): SharedPreferences =
        context.getSharedPreferences("app_prefs", 0)

    private inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
        val editMe = edit()
        operation(editMe)
        editMe.apply()
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

    var SharedPreferences.requireUnlock
        get() = getBoolean(REQUIRE_UNLOCK, false)
        set(value) {
            editMe {
                it.put(REQUIRE_UNLOCK to value)
            }
        }

    // export all the preferences
    fun SharedPreferences.export() = mapOf(
        DNS_SERVERS to getString(DNS_SERVERS, ""),
        AUTO_MODE to autoMode,
        REQUIRE_UNLOCK to requireUnlock
    )

    // import all the preferences
    fun SharedPreferences.import(map: Map<String, Any>) {
        editMe {
            map.forEach { (key, value) ->
                if (value is Number) {
                    it.put(key to value.toInt())
                    return@forEach
                }
                it.put(key to value)
            }
        }
    }
}
