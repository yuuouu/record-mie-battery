package yuu.record.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
            val updatedRecords = (currentRecords + record).sortedBy { sortKeyForRecord(it) }
            preferences[recordsKey] = Json.encodeToString(updatedRecords)
        }
    }

    suspend fun updateRecord(record: ChargingRecord) {
        context.dataStore.edit { preferences ->
            val currentRecords = preferences[recordsKey]?.let { Json.decodeFromString<List<ChargingRecord>>(it) } ?: emptyList()
            val updatedRecords = currentRecords.map { if (it.id == record.id) record else it }.sortedBy { sortKeyForRecord(it) }
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

    suspend fun replaceAllRecords(newRecords: List<ChargingRecord>) {
        context.dataStore.edit { preferences ->
            preferences[recordsKey] = Json.encodeToString(newRecords)
        }
    }

    suspend fun deleteAllRecords() {
        context.dataStore.edit { preferences ->
            preferences.remove(recordsKey)
        }
    }

    private fun sortKeyForRecord(record: ChargingRecord): Int {
        return try {
            record.date.split("/").map { it.toInt() }.let { parts ->
                when (parts.size) {
                    2    -> parts[0] * 100 + parts[1]
                    3    -> parts[0] * 10000 + parts[1] * 100 + parts[2]
                    else -> Int.MAX_VALUE // 如果格式不正确，将该记录排到最后
                }
            }
        } catch (e: NumberFormatException) {
            Int.MAX_VALUE // 如果无法解析为数字，将该记录排到最后
        }
    }
}