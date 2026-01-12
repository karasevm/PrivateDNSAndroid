package ru.karasevm.privatednstoggle.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import ru.karasevm.privatednstoggle.R

class AboutLibsActivity : AppCompatActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (isSystemInDarkTheme())
                        dynamicDarkColorScheme(applicationContext)
                    else
                        dynamicLightColorScheme(applicationContext)
                } else {
                    if (isSystemInDarkTheme()) {
                        darkColorScheme()
                    } else {
                        lightColorScheme()
                    }
                }
            ) {
                val libraries by produceLibraries(R.raw.aboutlibraries)
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Libraries") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.arrow_back_24px),
                                            contentDescription = "back"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        LibrariesContainer(
                            libraries = libraries,
                            contentPadding = paddingValues
                        )
                    }
                )
            }
        }
    }
}