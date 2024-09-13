package ru.karasevm.privatednstoggle.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.karasevm.privatednstoggle.model.DnsServer

class DnsServerViewModel(private val dnsServerRepository: DnsServerRepository) : ViewModel() {

    val allServers: LiveData<List<DnsServer>> = dnsServerRepository.allServers.asLiveData()

    fun getAll() = dnsServerRepository.getAll()

    suspend fun getById(id: Int) = dnsServerRepository.getById(id)

    fun insert(dnsServer: DnsServer) =
        viewModelScope.launch {
            dnsServerRepository.insert(dnsServer)
        }

    fun update(
        id: Int,
        server: String? = null,
        label: String? = null,
        sortOrder: Int? = null,
        enabled: Boolean? = null
    ) = viewModelScope.launch { dnsServerRepository.update(id, server, label, sortOrder, enabled) }

    fun move(sortOrder: Int, newSortOrder: Int, id: Int) =
        viewModelScope.launch { dnsServerRepository.move(sortOrder, newSortOrder, id) }

    fun delete(id: Int) = viewModelScope.launch { dnsServerRepository.delete(id) }

}

class DnsServerViewModelFactory(private val dnsServerRepository: DnsServerRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DnsServerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DnsServerViewModel(dnsServerRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}