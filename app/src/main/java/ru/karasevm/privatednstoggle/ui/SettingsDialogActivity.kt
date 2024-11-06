package ru.karasevm.privatednstoggle.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.karasevm.privatednstoggle.service.ShortcutService

class SettingsDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent != null && intent.data != null) {
            // Start the service when the shortcut is clicked
            val serviceIntent = Intent(this, ShortcutService::class.java)
            serviceIntent.data = intent.data
            startService(serviceIntent)
            finish()
        } else {
            val newFragment = DNSServerDialogFragment()
            newFragment.show(supportFragmentManager, DNSServerDialogFragment.TAG)
        }
    }
}