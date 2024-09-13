package ru.karasevm.privatednstoggle.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.karasevm.privatednstoggle.model.DnsServer

@Dao
interface DnsServerDao {

    @Query("SELECT * FROM dns_servers ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<DnsServer>>

    @Query("SELECT * FROM dns_servers WHERE enabled = 1 ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getFirstEnabled(): DnsServer

    @Query("SELECT * FROM dns_servers WHERE server = :server LIMIT 1")
    suspend fun getFirstByServer(server: String): DnsServer?

    @Query("SELECT * FROM dns_servers WHERE id = :id")
    suspend fun getById(id: Int): DnsServer?

    @Query("SELECT * FROM dns_servers " +
            "WHERE sortOrder > (SELECT sortOrder FROM dns_servers WHERE server = :server) AND enabled = 1 " +
            "ORDER BY sortOrder ASC " +
            "LIMIT 1")
    suspend fun getNextEnabledByServer(server: String): DnsServer?

    @Query("DELETE FROM dns_servers")
    suspend fun deleteAll()

    @Query("DELETE FROM dns_servers WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE dns_servers SET sortOrder = sortOrder + 1 " +
            "WHERE sortOrder >= :startSortOrder AND sortOrder <= :endSortOrder")
    suspend fun incrementSortOrder(startSortOrder: Int, endSortOrder: Int = Int.MAX_VALUE)

    @Query("UPDATE dns_servers SET sortOrder = sortOrder - 1 " +
            "WHERE sortOrder >= :startSortOrder AND sortOrder <= :endSortOrder")
    suspend fun decrementSortOrder(startSortOrder: Int, endSortOrder: Int = Int.MAX_VALUE)

    @Query("UPDATE dns_servers SET sortOrder = sortOrder - 1 " +
            "WHERE sortOrder > (SELECT sortOrder FROM dns_servers WHERE id = :id)")
    suspend fun decrementSortOrderById(id: Int)

    @Transaction
    suspend fun deleteAndDecrement(id: Int) {
        decrementSortOrderById(id)
        deleteById(id)
    }

    @Query("UPDATE dns_servers SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: Int, label: String)

    @Query("UPDATE dns_servers SET server = :server WHERE id = :id")
    suspend fun updateServer(id: Int, server: String)

    @Query("UPDATE dns_servers " +
            "SET server = COALESCE(:server, server), " +
            "    label = COALESCE(:label, label), " +
            "    sortOrder = COALESCE(:sortOrder, sortOrder), " +
            "    enabled = COALESCE(:enabled, enabled) " +
            "WHERE id = :id")
    suspend fun update(id: Int, server: String?, label: String?, sortOrder: Int?, enabled: Boolean?)

    @Transaction
    suspend fun moveUp(sortOrder: Int, newSortOrder: Int, id: Int){
        incrementSortOrder(newSortOrder, sortOrder)
        update(id, null, null, newSortOrder, null)
    }

    @Transaction
    suspend fun moveDown(sortOrder: Int, newSortOrder: Int, id: Int){
        decrementSortOrder(sortOrder, newSortOrder)
        update(id, null, null, newSortOrder, null)
    }

    @Query("INSERT INTO dns_servers(server, label, sortOrder, enabled) " +
            "VALUES(:server, :label, COALESCE((SELECT MAX(sortOrder) + 1 FROM dns_servers), 0), :enabled)")
    suspend fun insert(server: String, label: String, enabled: Boolean)

}