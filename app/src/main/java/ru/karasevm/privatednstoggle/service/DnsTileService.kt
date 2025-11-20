package ru.karasevm.privatednstoggle.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import ru.karasevm.privatednstoggle.PrivateDNSApp
import ru.karasevm.privatednstoggle.R
import ru.karasevm.privatednstoggle.data.DnsServerRepository
import ru.karasevm.privatednstoggle.util.PreferenceHelper
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoRevertEnabled
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoRevertMinutes
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertMode
import ru.karasevm.privatednstoggle.util.PreferenceHelper.revertProvider
import ru.karasevm.privatednstoggle.util.PreferenceHelper.requireUnlock
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.DNS_MODE_AUTO
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.DNS_MODE_OFF
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.DNS_MODE_PRIVATE
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.checkForPermission

class DnsTileService : TileService() {

    private val repository: DnsServerRepository by lazy { (application as PrivateDNSApp).repository }
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val sharedPreferences by lazy { PreferenceHelper.defaultPreference(this) }
    private var isBroadcastReceiverRegistered = false

    override fun onTileAdded() {
        super.onTileAdded()
        checkForPermission(this)
        // Update state
        qsTile.state = Tile.STATE_INACTIVE

        // Update looks
        qsTile.updateTile()
    }

    /**
     *  Set's the state of the tile and system settings to the next state
     */
    private fun cycleState() {
        PrivateDNSUtils.getNextProvider(sharedPreferences, scope, repository, contentResolver, onNext = { mode, provider ->
            changeDNSServer(mode, provider)
        })
    }

    /**
     *  Sets the state of the tile to the provided values
     *  @param mode dns mode
     *  @param dnsProvider dns provider
     */
    private fun changeDNSServer(mode: String, dnsProvider: String?) {
        when (mode) {
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
                scope.launch {
                    if (dnsProvider == null) {
                        return@launch
                    }
                    val dnsServer = repository.getFirstByServer( dnsProvider)
                    if (dnsServer != null) {
                        changeTileState(
                            qsTile,
                            Tile.STATE_ACTIVE,
                            dnsServer.label.ifEmpty { dnsServer.server },
                            R.drawable.ic_private_black_24dp,
                            DNS_MODE_PRIVATE,
                            dnsServer.server
                        )
                    }
                }
            }
        }
    }

    override fun onClick() {
        super.onClick()
        if (!checkForPermission(this)) {
            return
        }

        // Require unlock to change mode according to user preference
        val requireUnlock = sharedPreferences.requireUnlock
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
        val isPermissionGranted = checkForPermission(this)
        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        when (dnsMode?.lowercase()) {
            DNS_MODE_OFF -> {
                setTile(
                    qsTile,
                    if (!isPermissionGranted) Tile.STATE_UNAVAILABLE else Tile.STATE_INACTIVE,
                    getString(R.string.dns_off),
                    R.drawable.ic_off_black_24dp
                )
            }

            DNS_MODE_AUTO -> {
                setTile(
                    qsTile,
                    if (!isPermissionGranted) Tile.STATE_UNAVAILABLE else Tile.STATE_INACTIVE,
                    getString(R.string.dns_auto),
                    R.drawable.ic_auto_black_24dp
                )
            }

            DNS_MODE_PRIVATE -> {
                scope.launch {
                    val activeAddress =
                        Settings.Global.getString(contentResolver, "private_dns_specifier")
                    val dnsServer = repository.getFirstByServer(activeAddress)
                    setTile(
                        qsTile,
                        if (!isPermissionGranted) Tile.STATE_UNAVAILABLE else Tile.STATE_ACTIVE,
                        // display server address if either there is no label or the server is not known
                        dnsServer?.label?.ifBlank { activeAddress } ?: activeAddress,
                        R.drawable.ic_private_black_24dp
                    )
                }
            }

            else -> {
                setTile(
                    qsTile,
                    if (!isPermissionGranted) Tile.STATE_UNAVAILABLE else Tile.STATE_INACTIVE,
                    getString(R.string.dns_unknown),
                    R.drawable.ic_unknown_black_24dp
                )
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

        // Prevent some crashes
        if (qsTile == null) {
            Log.w(TAG, "onStartListening: qsTile is null")
            return
        }


        // Receive broadcasts to update the tile when server is changed from the dialog
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            IntentFilter("refresh_tile"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        isBroadcastReceiverRegistered = true
        refreshTile()

    }

    override fun onStopListening() {
        super.onStopListening()
        if (isBroadcastReceiverRegistered) {
            unregisterReceiver(broadcastReceiver)
            isBroadcastReceiverRegistered = false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancelChildren()
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
        // If auto-revert enabled and user is turning DNS off, schedule revert
        try {
            val autoRevertEnabled = sharedPreferences.autoRevertEnabled
            if (autoRevertEnabled && dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
                // Save current system mode/provider to preferences
                val currentMode = PrivateDNSUtils.getPrivateMode(contentResolver)
                val currentProvider = PrivateDNSUtils.getPrivateProvider(contentResolver)
                sharedPreferences.revertMode = currentMode
                sharedPreferences.revertProvider = currentProvider
                // Schedule revert
                val minutes = sharedPreferences.autoRevertMinutes
                RevertScheduler.scheduleRevert(this, minutes)
            } else if (!dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
                // If switching back to some non-off state, cancel scheduled revert
                RevertScheduler.cancelRevert(this)
                sharedPreferences.revertMode = null
                sharedPreferences.revertProvider = null
            }
        } catch (e: Exception) {
            // swallow scheduling errors; still attempt to set DNS
        }

        PrivateDNSUtils.setPrivateMode(contentResolver, dnsMode)
        PrivateDNSUtils.setPrivateProvider(contentResolver, dnsProvider)
        tile.updateTile()
    }

    companion object {
        private const val TAG = "DnsTileService"
    }
}