package ru.karasevm.privatednstoggle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import ru.karasevm.privatednstoggle.utils.DnsServer
import ru.karasevm.privatednstoggle.utils.PreferenceHelper
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.autoMode
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.dns_servers
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.requireUnlock
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.AUTO_MODE_OPTION_AUTO
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.AUTO_MODE_OPTION_OFF_AUTO
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.AUTO_MODE_OPTION_PRIVATE
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.DNS_MODE_AUTO
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.DNS_MODE_OFF
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.DNS_MODE_PRIVATE
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils.checkForPermission

class DnsTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        checkForPermission(this)
        // Update state
        qsTile.state = Tile.STATE_INACTIVE

        // Update looks
        qsTile.updateTile()
    }

    /**
     *  Set's the state of the tile to the next state
     */
    private fun cycleState() {
        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val dnsProvider = Settings.Global.getString(contentResolver, "private_dns_specifier")

        val sharedPrefs = PreferenceHelper.defaultPreference(this)
        if (dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
            if (sharedPrefs.autoMode == AUTO_MODE_OPTION_AUTO || sharedPrefs.autoMode == AUTO_MODE_OPTION_OFF_AUTO) {
                changeDNSServer(DNS_MODE_AUTO, dnsProvider)
            } else {
                changeDNSServer(DNS_MODE_PRIVATE, dnsProvider)
            }

        } else if (dnsMode == null || dnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            changeDNSServer(DNS_MODE_PRIVATE, null)
        } else if (dnsMode.equals(DNS_MODE_PRIVATE, ignoreCase = true)) {
            if (getNextAddress(dnsProvider) == null) {
                if (sharedPrefs.autoMode == AUTO_MODE_OPTION_PRIVATE) {
                    changeDNSServer(DNS_MODE_PRIVATE, null)
                } else {
                    if (sharedPrefs.autoMode == AUTO_MODE_OPTION_AUTO) {
                        changeDNSServer(DNS_MODE_AUTO, dnsProvider)
                    } else {
                        changeDNSServer(DNS_MODE_OFF, dnsProvider)
                    }
                }
            } else {
                changeDNSServer(DNS_MODE_PRIVATE, dnsProvider)
            }
        }
    }

    private fun changeDNSServer(server: String, dnsProvider: String?) {
        when (server) {
            DNS_MODE_OFF -> {
                changeTileState(
                    qsTile,
                    Tile.STATE_INACTIVE,
                    getString(R.string.dns_off),
                    R.drawable.ic_off_black_24dp,
                    DNS_MODE_OFF,
                    null
                )
            }

            DNS_MODE_AUTO -> {
                changeTileState(
                    qsTile,
                    Tile.STATE_INACTIVE,
                    getString(R.string.dns_auto),
                    R.drawable.ic_auto_black_24dp,
                    DNS_MODE_AUTO,
                    dnsProvider
                )
            }

            DNS_MODE_PRIVATE -> {
                changeTileState(
                    qsTile,
                    Tile.STATE_ACTIVE,
                    getNextAddress(dnsProvider)?.label,
                    R.drawable.ic_private_black_24dp,
                    DNS_MODE_PRIVATE,
                    getNextAddress(dnsProvider)?.server
                )
            }
        }
    }

    override fun onClick() {
        super.onClick()
        if (!checkForPermission(this)) {
            return
        }
        val sharedPrefs = PreferenceHelper.defaultPreference(this)
        val requireUnlock = sharedPrefs.requireUnlock
        if (isLocked && requireUnlock) {
            unlockAndRun(this::cycleState)
        } else {
            cycleState()
        }


    }

    /**
     *  Refreshes the state of the tile
     */
    private fun refreshTile() {
        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        if (dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
            setTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_off),
                R.drawable.ic_off_black_24dp
            )
        } else if (dnsMode == null) {
            setTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_unknown),
                R.drawable.ic_unknown_black_24dp
            )
        } else if (dnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            setTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_auto),
                R.drawable.ic_auto_black_24dp
            )
        } else if (dnsMode.equals(DNS_MODE_PRIVATE, ignoreCase = true)) {
            val dnsProvider = Settings.Global.getString(contentResolver, "private_dns_specifier")
            val sharedPrefs = PreferenceHelper.defaultPreference(this)
            val items = sharedPrefs.dns_servers.map {
                val parts = it.split(" : ")
                if (parts.size == 2)
                    DnsServer(parts[0], parts[1])
                else
                    DnsServer(parts[0], parts[0])
            }

            if (items.isEmpty() || items[0].server == "") {
                setTile(
                    qsTile,
                    Tile.STATE_ACTIVE,
                    "Google",
                    R.drawable.ic_private_black_24dp
                )
            } else {
                val index = items.indexOfFirst { it.server == dnsProvider }
                if (index == -1) {
                    setTile(
                        qsTile,
                        Tile.STATE_ACTIVE,
                        dnsProvider,
                        R.drawable.ic_private_black_24dp
                    )
                } else {
                    setTile(
                        qsTile,
                        Tile.STATE_ACTIVE,
                        items[index].label,
                        R.drawable.ic_private_black_24dp
                    )
                }
            }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!checkForPermission(this)) {
            return
        }

        // Prevent some crashes
        if (qsTile == null) {
            return
        }
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            IntentFilter("refresh_tile"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        refreshTile()

    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterReceiver(broadcastReceiver)
    }

    /**
     * Updates tile to specified parameters
     *
     * @param tile tile to update
     * @param state tile state
     * @param label tile label
     * @param icon tile icon
     */
    private fun setTile(tile: Tile, state: Int, label: String?, icon: Int) {
        tile.state = state
        tile.label = label
        tile.icon = Icon.createWithResource(this, icon)
        tile.updateTile()
    }

    /**
     * Updates tile and system settings to specified parameters
     *
     * @param tile tile to update
     * @param state tile state
     * @param label tile label
     * @param icon tile icon
     * @param dnsMode system dns mode
     * @param dnsProvider system dns provider
     */
    private fun changeTileState(
        tile: Tile,
        state: Int,
        label: String?,
        icon: Int,
        dnsMode: String,
        dnsProvider: String?
    ) {
        tile.label = label
        tile.state = state
        tile.icon = Icon.createWithResource(this, icon)
        PrivateDNSUtils.setPrivateMode(contentResolver, dnsMode)
        PrivateDNSUtils.setPrivateProvider(contentResolver, dnsProvider)
        tile.updateTile()
    }

    /**
     * Gets next dns address from preferences,
     * if current address is last returns null
     *
     * @param currentAddress currently set address
     * @return next address
     */
    private fun getNextAddress(currentAddress: String?): DnsServer? {
        val sharedPrefs = PreferenceHelper.defaultPreference(this)
        val items = sharedPrefs.dns_servers.map {
            val parts = it.split(" : ")
            // Assuming string is in the format "$label : $server"
            if (parts.size == 2)
                DnsServer(parts[0], parts[1])
            else
                DnsServer(parts[0], parts[0])
        }.toMutableList()

        // Fallback if list is empty
        if (items.isEmpty() || items[0].server == "") {
            items.apply {
                removeAt(0)
                add(DnsServer("Google", "dns.google"))
            }
        }

        val index = items.indexOfFirst { it.server == currentAddress }

        if (index == -1 || currentAddress == null) {
            return items[0]
        }
        if (index == items.size - 1) {
            return null
        }
        return items[index + 1]
    }
}