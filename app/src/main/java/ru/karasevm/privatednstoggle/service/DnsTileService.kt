package ru.karasevm.privatednstoggle.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
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
import ru.karasevm.privatednstoggle.util.PreferenceHelper.requireUnlock
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.DNS_MODE_AUTO
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.DNS_MODE_OFF
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.DNS_MODE_PRIVATE
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.checkForPermission
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils.getNextAddress

class DnsTileService : TileService() {

    private val repository: DnsServerRepository by lazy { (application as PrivateDNSApp).repository }
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

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
        val sharedPrefs = PreferenceHelper.defaultPreference(this)

        PrivateDNSUtils.getNextProvider(sharedPrefs, scope, repository, contentResolver, onNext = { mode, provider ->
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
                    val nextDnsServer = getNextAddress(repository, dnsProvider)
                    if (nextDnsServer != null) {
                        changeTileState(
                            qsTile,
                            Tile.STATE_ACTIVE,
                            nextDnsServer.label.ifEmpty { nextDnsServer.server },
                            R.drawable.ic_private_black_24dp,
                            DNS_MODE_PRIVATE,
                            getNextAddress(repository, dnsProvider)?.server
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
        val sharedPrefs = PreferenceHelper.defaultPreference(this)

        // Require unlock to change mode according to user preference
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
        val dnsMode = PrivateDNSUtils.getPrivateMode(contentResolver)
        when (dnsMode?.lowercase()) {
            DNS_MODE_OFF -> {
                setTile(
                    qsTile,
                    Tile.STATE_INACTIVE,
                    getString(R.string.dns_off),
                    R.drawable.ic_off_black_24dp
                )
            }

            DNS_MODE_AUTO -> {
                setTile(
                    qsTile,
                    Tile.STATE_INACTIVE,
                    getString(R.string.dns_auto),
                    R.drawable.ic_auto_black_24dp
                )
            }

            DNS_MODE_PRIVATE -> {
                scope.launch {
                    val activeAddress =
                        PrivateDNSUtils.getPrivateProvider(contentResolver)
                    val dnsServer = repository.getFirstByServer(activeAddress!!)
                    setTile(
                        qsTile,
                        Tile.STATE_ACTIVE,
                        // display server address if either there is no label or the server is not known
                        dnsServer?.label?.ifBlank { activeAddress } ?: activeAddress,
                        R.drawable.ic_private_black_24dp
                    )
                }
            }

            else -> {
                setTile(
                    qsTile,
                    Tile.STATE_INACTIVE,
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
        if (!checkForPermission(this)) {
            return
        }

        // Prevent some crashes
        if (qsTile == null) {
            return
        }


        // Receive broadcasts to update the tile when server is changed from the dialog
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
        PrivateDNSUtils.setPrivateMode(contentResolver, dnsMode)
        PrivateDNSUtils.setPrivateProvider(contentResolver, dnsProvider)
        tile.updateTile()
    }
}