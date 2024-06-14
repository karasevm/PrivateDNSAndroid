package ru.karasevm.privatednstoggle

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.permission.IPermissionManager
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import ru.karasevm.privatednstoggle.databinding.ActivityMainBinding
import ru.karasevm.privatednstoggle.utils.PreferenceHelper
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.dns_servers


class MainActivity : AppCompatActivity(), AddServerDialogFragment.NoticeDialogListener,
    DeleteServerDialogFragment.NoticeDialogListener, Shizuku.OnRequestPermissionResultListener {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var binding: ActivityMainBinding
    private var items = mutableListOf<String>()
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var adapter: RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addRequestPermissionResultListener(this::onRequestPermissionResult)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        linearLayoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = linearLayoutManager

        sharedPrefs = PreferenceHelper.defaultPreference(this)

        items = sharedPrefs.dns_servers
        if (items[0] == "") {
            items.removeAt(0)
        }
        adapter = RecyclerAdapter(items)
        adapter.onItemClick = { position ->
            val newFragment = DeleteServerDialogFragment(position)
            newFragment.show(supportFragmentManager, "delete_server")
        }
        binding.floatingActionButton.setOnClickListener {
            val newFragment = AddServerDialogFragment()
            newFragment.show(supportFragmentManager, "add_server")
        }
        binding.recyclerView.adapter = adapter

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.privacy_policy -> {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/privacy_policy")
                    )
                    startActivity(browserIntent)
                    true
                }

                R.id.options -> {
                    val newFragment = OptionsDialogFragment()
                    newFragment.show(supportFragmentManager, "options")
                    true
                }

                else -> true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        // Check if WRITE_SECURE_SETTINGS is granted
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            // Check if Shizuku is available
            if (Shizuku.pingBinder()) {
                // check if permission is granted already
                val isGranted = if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                    checkSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
                // request permission if not granted
                if (!isGranted && !Shizuku.shouldShowRequestPermissionRationale()) {
                    if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                        requestPermissions(arrayOf(ShizukuProvider.PERMISSION), 1)
                    } else {
                        Shizuku.requestPermission(1)
                    }
                } else {
                    grantPermissionWithShizuku()
                }
            } else {
                if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/")
                    )
                    startActivity(browserIntent)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this::onRequestPermissionResult)
    }

    override fun onDialogPositiveClick(server: String) {
        if (server.isEmpty()) {
            Toast.makeText(this, R.string.server_length_error, Toast.LENGTH_SHORT).show()
            return
        }
        items.add(server)
        adapter.setData(items.toMutableList())
        binding.recyclerView.adapter?.notifyItemInserted(items.size - 1)
        sharedPrefs.dns_servers = items
    }

    override fun onDialogPositiveClick(position: Int) {
        items.removeAt(position)
        adapter.setData(items.toMutableList())
        adapter.notifyItemRemoved(position)
        sharedPrefs.dns_servers = items
    }

    /**
     * Attempts to grant WRITE_SECURE_SETTINGS permission with Shizuku
     */
    private fun grantPermissionWithShizuku() {
        val packageName = "ru.karasevm.privatednstoggle"
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) {
                HiddenApiBypass.addHiddenApiExemptions("Landroid/permission")
                val binder =
                    ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr"))
                val pm = IPermissionManager.Stub.asInterface(binder)
                runCatching {
                    pm.grantRuntimePermission(
                        packageName,
                        Manifest.permission.WRITE_SECURE_SETTINGS,
                        0
                    )
                }.onFailure { _ ->
                    if (Build.VERSION.SDK_INT >= 34) {
                        pm.grantRuntimePermission(
                            packageName,
                            Manifest.permission.WRITE_SECURE_SETTINGS,
                            applicationContext.deviceId,
                            0
                        )
                    }
                }
            } else {
                val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
                val pm = IPackageManager.Stub.asInterface(binder)
                pm.grantRuntimePermission(
                    packageName,
                    Manifest.permission.WRITE_SECURE_SETTINGS,
                    0
                )
            }
        }.onFailure { e ->
            Log.e("SHIZUKU", "onRequestPermissionResult: ", e)
        }.also {
            if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/")
                )
                startActivity(browserIntent)
                finish()
            }
        }

    }

    @SuppressLint("PrivateApi")
    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val isGranted = grantResult == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            grantPermissionWithShizuku()
        } else if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/")
            )
            startActivity(browserIntent)
            finish()
        }
    }
}