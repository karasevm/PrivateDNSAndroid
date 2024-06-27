package ru.karasevm.privatednstoggle.utils

data class DnsServer(val label: String, val server: String) {
    override fun toString(): String {
        return "$label : $server"
    }
}
