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
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import ru.karasevm.privatednstoggle.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), AddServerDialogFragment.NoticeDialogListener, DeleteServerDialogFragment.NoticeDialogListener, Shizuku.OnRequestPermissionResultListener {

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

        sharedPrefs = this.getSharedPreferences("app_prefs", 0)

        items = sharedPrefs.getString("dns_servers", "")!!.split(",").toMutableList()
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
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/privacy_policy"))
                    startActivity(browserIntent)
                    true
                }

                R.id.enable_auto -> {
                    if (!item.isChecked){
                        Toast.makeText(this, R.string.auto_mode_clarification, Toast.LENGTH_LONG).show()
                    }
                    sharedPrefs.edit().putBoolean("auto_enabled", !item.isChecked).apply()
                    item.setChecked(!item.isChecked)
                    true
                }
                else -> true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val curVal = sharedPrefs.getBoolean("auto_enabled", false)
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.enable_auto)?.setChecked(curVal)

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
                    // if shizuku permission is granted, but WRITE_SECURE_SETTINGS is not, grant it
                    try {
                        grantPermissionWithShizuku()
                    } catch (exception: Exception) {
                        Log.e("SHIZUKU", "onRequestPermissionResult: ", exception)
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/"))
                        startActivity(browserIntent)
                        finish()
                    }
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

    override fun onDialogPositiveClick(dialog: DialogFragment, server: String) {
        if (server.isEmpty()) {
            Toast.makeText(this, R.string.server_length_error, Toast.LENGTH_SHORT).show()
            return
        }
        items.add(server)
        adapter.setData(items.toMutableList())
        binding.recyclerView.adapter?.notifyItemInserted(items.size - 1)
        sharedPrefs.edit()
            .putString("dns_servers", items.joinToString(separator = ",") { it }).apply()
    }

    override fun onDialogPositiveClick(dialog: DialogFragment,position: Int) {
        items.removeAt(position)
        adapter.setData(items.toMutableList())
        adapter.notifyItemRemoved(position)
        sharedPrefs.edit()
            .putString("dns_servers", items.joinToString(separator = ",") { it }).apply()
    }

    /**
     * Attempts to grant WRITE_SECURE_SETTINGS permission with Shizuku
     */
    private fun grantPermissionWithShizuku() {
        val packageName = "ru.karasevm.privatednstoggle"
        if (Build.VERSION.SDK_INT >= 31) {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/permission"
            )
            val binder =
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr"))
            val pm = IPermissionManager.Stub.asInterface(binder)
            pm.grantRuntimePermission(
                packageName,
                Manifest.permission.WRITE_SECURE_SETTINGS,
                0
            )
        } else {
            val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            val pm = IPackageManager.Stub.asInterface(binder)
            pm.grantRuntimePermission(
                packageName,
                Manifest.permission.WRITE_SECURE_SETTINGS,
                0
            )
        }
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/"))
            startActivity(browserIntent)
            finish()
        }
    }
    @SuppressLint("PrivateApi")
    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val isGranted = grantResult == PackageManager.PERMISSION_GRANTED

        if (isGranted) {
            try {
                grantPermissionWithShizuku()
            } catch (exception: Exception) {
                Log.e("SHIZUKU", "onRequestPermissionResult: ", exception)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/"))
                startActivity(browserIntent)
                finish()
            }

        } else if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/"))
            startActivity(browserIntent)
            finish()
        }
    }
}