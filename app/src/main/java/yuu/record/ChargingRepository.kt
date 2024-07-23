package yuu.record

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 在伴生对象中定义 DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "charging_records")

class ChargingRepository(private val context: Context) {
    private val recordsKey = stringPreferencesKey("charging_records")

    fun getAllRecords(): Flow<List<ChargingRecord>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[recordsKey] ?: "[]"
            Json.decodeFromString(json)
        }
    }

    suspend fun addRecord(record: ChargingRecord) {
        context.dataStore.edit { preferences ->
            val currentRecords = preferences[recordsKey]?.let { Json.decodeFromString<List<ChargingRecord>>(it) } ?: emptyList()
            val updatedRecords = currentRecords+record
            preferences[recordsKey] = Json.encodeToString(updatedRecords)
        }
    }

    suspend fun updateRecord(record: ChargingRecord) {
        context.dataStore.edit { preferences ->
            val currentRecords = preferences[recordsKey]?.let { Json.decodeFromString<List<ChargingRecord>>(it) } ?: emptyList()
            val updatedRecords = currentRecords.map { if (it.id == record.id) record else it }
            preferences[recordsKey] = Json.encodeToString(updatedRecords)
        }
    }

    suspend fun deleteRecord(record: ChargingRecord) {
        context.dataStore.edit { preferences ->
            val currentRecords = preferences[recordsKey]?.let { Json.decodeFromString<List<ChargingRecord>>(it) } ?: emptyList()
            val updatedRecords = currentRecords.filter { it.id != record.id }
            preferences[recordsKey] = Json.encodeToString(updatedRecords)
        }
    }
}