package ru.karasevm.privatednstoggle.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val newFragment = DNSServerDialogFragment()
        newFragment.show(supportFragmentManager, DNSServerDialogFragment.TAG)
    }
}