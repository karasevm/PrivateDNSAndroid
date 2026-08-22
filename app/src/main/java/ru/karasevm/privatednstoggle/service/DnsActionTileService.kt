package ru.karasevm.privatednstoggle.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.graphics.drawable.Icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.karasevm.privatednstoggle.PrivateDNSApp
import ru.karasevm.privatednstoggle.R
import ru.karasevm.privatednstoggle.data.DnsServerRepository
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils

abstract class DnsActionTileService : TileService() {

    private val repository: DnsServerRepository by lazy { (application as PrivateDNSApp).repository }
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    protected abstract val dnsMode: String
    protected abstract val tileLabelRes: Int
    protected abstract val tileIconRes: Int

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!PrivateDNSUtils.checkForPermission(this)) {
            return
        }

        scope.launch {
            if (dnsMode == PrivateDNSUtils.DNS_MODE_PRIVATE) {
                val currentProvider = PrivateDNSUtils.getPrivateProvider(contentResolver)
                val dnsProvider = if (currentProvider != null &&
                    repository.getFirstByServer(currentProvider)?.enabled == true
                ) {
                    currentProvider
                } else {
                    repository.getFirstEnabled()?.server
                }
                if (dnsProvider == null) {
                    return@launch
                }
                PrivateDNSUtils.setPrivateProvider(contentResolver, dnsProvider)
            }
            PrivateDNSUtils.setPrivateMode(contentResolver, dnsMode)
            updateTile()
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (PrivateDNSUtils.getPrivateMode(contentResolver) == dnsMode) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.label = getString(tileLabelRes)
        tile.icon = Icon.createWithResource(this, tileIconRes)
        tile.updateTile()
    }
}

class DnsOnTileService : DnsActionTileService() {
    override val dnsMode = PrivateDNSUtils.DNS_MODE_PRIVATE
    override val tileLabelRes = R.string.tile_turn_on
    override val tileIconRes = R.drawable.ic_private_black_24dp
}

class DnsOffTileService : DnsActionTileService() {
    override val dnsMode = PrivateDNSUtils.DNS_MODE_OFF
    override val tileLabelRes = R.string.tile_turn_off
    override val tileIconRes = R.drawable.ic_off_black_24dp
}
