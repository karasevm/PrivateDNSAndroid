package ru.karasevm.privatednstoggle

import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
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
                        changeDNSServer(DNS_MODE_AUTO,dnsProvider)
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
                    getNextAddress(dnsProvider)
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
                    getNextAddress(dnsProvider),
                    R.drawable.ic_private_black_24dp,
                    DNS_MODE_PRIVATE,
                    getNextAddress(dnsProvider)
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

    override fun onStartListening() {
        super.onStartListening()
        if (!checkForPermission(this)) {
            return
        }
        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")

        // Prevent some crashes
        if (qsTile == null) {
            return
        }

        if (dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
            refreshTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_off),
                R.drawable.ic_off_black_24dp
            )
        } else if (dnsMode == null) {
            refreshTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_unknown),
                R.drawable.ic_unknown_black_24dp
            )
        } else if (dnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            refreshTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_auto),
                R.drawable.ic_auto_black_24dp
            )
        } else if (dnsMode.equals(DNS_MODE_PRIVATE, ignoreCase = true)) {
            val dnsProvider = Settings.Global.getString(contentResolver, "private_dns_specifier")
            refreshTile(
                qsTile,
                Tile.STATE_ACTIVE,
                dnsProvider,
                R.drawable.ic_private_black_24dp
            )
        }

    }

    /**
     * Updates tile to specified parameters
     *
     * @param tile tile to update
     * @param state tile state
     * @param label tile label
     * @param icon tile icon
     */
    private fun refreshTile(tile: Tile, state: Int, label: String?, icon: Int) {
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
    private fun getNextAddress(currentAddress: String?): String? {
        val sharedPrefs = PreferenceHelper.defaultPreference(this)
        val items = sharedPrefs.dns_servers

        // Fallback if list is empty
        if (items[0] == "") {
            items.removeAt(0)
            items.add("dns.google")
        }

        val index = items.indexOf(currentAddress)

        if (index == -1 || currentAddress == null) {
            return items[0]
        }
        if (index == items.size - 1) {
            return null
        }
        return items[index + 1]
    }
}