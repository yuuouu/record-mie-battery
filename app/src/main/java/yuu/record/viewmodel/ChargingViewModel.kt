package yuu.record.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log.e
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import yuu.record.data.ChargingRecord
import yuu.record.data.ChargingRepository
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
class ChargingViewModel(private val repository: ChargingRepository): ViewModel() {
    private val _chargingRecords = MutableStateFlow<List<ChargingRecord>>(emptyList())
    private val chargingRecords = _chargingRecords.asStateFlow()
    private val _reversedChargingRecords = MutableStateFlow<List<ChargingRecord>>(emptyList())
    val reversedChargingRecords: StateFlow<List<ChargingRecord>> = _reversedChargingRecords
    private val _justAddedRecord = MutableStateFlow(false)
    val justAddedRecord: StateFlow<Boolean> = _justAddedRecord


    init {
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _chargingRecords.value = records
                _reversedChargingRecords.value = records.reversed()
            }
        }
    }

    fun addChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val sortedRecords = chargingRecords.value.sortedBy { sortKeyForRecord(it) }
            val previousRecord = sortedRecords.lastOrNull { sortKeyForRecord(it) < sortKeyForRecord(record) }
            val newRecord = calculateRangeAdded(record, previousRecord)
            repository.addRecord(newRecord)
            _justAddedRecord.value = true
        }
    }

    fun updateChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val sortedRecords = chargingRecords.value.sortedBy { sortKeyForRecord(it) }
            val index = sortedRecords.indexOfFirst { it.id == record.id }
            val previousRecord = if (index > 0) sortedRecords[index - 1] else null
            val updatedRecord = calculateRangeAdded(record, previousRecord)
            repository.updateRecord(updatedRecord)
        }
    }
    fun deleteChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun resetJustAddedFlag() {
        _justAddedRecord.value = false
    }

 /*   private fun calculateRangeAdded(record: ChargingRecord, previousRecord: ChargingRecord?): ChargingRecord {
        return if (previousRecord != null) {
            record.copy(rangeAdded = record.totalRange - previousRecord.totalRange)
        } else {
            record.copy(rangeAdded = record.totalRange)
        }
    }*/

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

    private fun calculateRangeAdded(record: ChargingRecord, previousRecord: ChargingRecord?): ChargingRecord {
        return if (previousRecord != null) {
            record.copy(rangeAdded = record.totalRange - previousRecord.totalRange)
        } else {
            record
        }
    }

    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("充电记录")

            // Create header row
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("日期")
            headerRow.createCell(1).setCellValue("续航里程(km)")
            headerRow.createCell(2).setCellValue("充电时间(h)")
            headerRow.createCell(3).setCellValue("充电金额")
            headerRow.createCell(4).setCellValue("当前总续航(km)")
            headerRow.createCell(5).setCellValue("备注")

            // Fill data
            chargingRecords.value.forEachIndexed { index, record ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(record.date)
                row.createCell(1).setCellValue(record.rangeAdded.toDouble())
                row.createCell(2).setCellValue(record.chargingTime)
                row.createCell(3).setCellValue(record.chargingCost)
                row.createCell(4).setCellValue(record.totalRange.toDouble())
                row.createCell(5).setCellValue(record.notes)
            }

            // Save the workbook
            val resolver = context.contentResolver
            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val fileName = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) + "记录.xlsx"
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = resolver.insert(contentUri, contentValues)
            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri)?.use { outputStream: OutputStream ->
                    workbook.write(outputStream)
                }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(fileUri, contentValues, null, null)

                // 可选：通知用户文件已保存
//                 showNotification(context, "Excel 文件已保存到下载目录", fileUri)
                e("TAG", "exportToExcel:Excel 文件已保存到下载目录 path= $fileUri")
                Toast.makeText(context, "Excel 文件已保存到下载目录", Toast.LENGTH_SHORT).show()
            }
            workbook.close()
        }
    }

    fun exportToJson(context: Context) {
        val json = Json.encodeToString(chargingRecords.value)

        val resolver = context.contentResolver
        val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val fileName = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) + "记录.json"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(contentUri, contentValues)
        uri?.let { fileUri ->
            resolver.openOutputStream(fileUri)?.use { outputStream: OutputStream ->
                outputStream.write(json.toByteArray())
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(fileUri, contentValues, null, null)

            Toast.makeText(context, "JSON 文件已保存到下载目录", Toast.LENGTH_SHORT).show()
        }
    }

    fun importJsonData(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader().use { it?.readText() }
                content?.let {
                    val importedRecords: List<ChargingRecord> = Json.decodeFromString(it)
                    // 验证导入的数据
                    if (validateImportedRecords(importedRecords)) {
                        // 替换所有记录
                        repository.replaceAllRecords(importedRecords)
                        // 更新 ViewModel 中的数据
                        _chargingRecords.value = importedRecords
                        Toast.makeText(context, "JSON 数据导入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "导入的数据格式不正确", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateImportedRecords(records: List<ChargingRecord>): Boolean {
        // 在这里添加验证逻辑
        // 例如，检查每条记录是否包含所有必要的字段，日期格式是否正确等
        return records.all { record ->
            record.date.isNotBlank() && record.chargingTime.isNotBlank() && record.chargingCost >= 0 && record.totalRange > 0
        }
    }

    private val _showImportConfirmation = MutableStateFlow<Uri?>(null)
    val showImportConfirmation: StateFlow<Uri?> = _showImportConfirmation

    fun triggerImport(uri: Uri) {
        _showImportConfirmation.value = uri
    }

    fun confirmImport(context: Context) {
        _showImportConfirmation.value?.let { uri ->
            importJsonData(context, uri)
            _showImportConfirmation.value = null
        }
    }

    fun cancelImport() {
        _showImportConfirmation.value = null
    }
}