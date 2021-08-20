package ru.karasevm.privatednstoggle

import android.Manifest
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import ru.karasevm.privatednstoggle.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), AddServerDialogFragment.NoticeDialogListener, DeleteServerDialogFragment.NoticeDialogListener {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var binding: ActivityMainBinding
    private var items = mutableListOf<String>()
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var adapter: RecyclerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/"))
            startActivity(browserIntent)
            finish()
        }
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
        binding.recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_server -> {
            val newFragment = AddServerDialogFragment()
            newFragment.show(supportFragmentManager, "add_server")
            true
        }
        R.id.privacy_policy -> {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://karasevm.github.io/PrivateDNSAndroid/privacy_policy"))
            startActivity(browserIntent)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
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


}