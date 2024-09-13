package ru.karasevm.privatednstoggle.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// All fields must have default values for proper deserialization
@Entity(tableName = "dns_servers")
data class DnsServer(
    @SerializedName("id")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @SerializedName("server")
    val server: String = "",
    @SerializedName("label")
    val label: String = "",
    @SerializedName("enabled")
    @ColumnInfo(defaultValue = "1")
    val enabled: Boolean = true,
    val sortOrder: Int? = null
)
