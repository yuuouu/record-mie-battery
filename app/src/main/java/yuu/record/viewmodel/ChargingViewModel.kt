package yuu.record.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log.e
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import yuu.record.ChargingRecord
import yuu.record.ChargingRepository
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
class ChargingViewModel(private val repository: ChargingRepository): ViewModel() {
    private val _chargingRecords = MutableStateFlow<List<ChargingRecord>>(emptyList())
    val chargingRecords = _chargingRecords.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllRecords().collect { records ->
                _chargingRecords.value = records
            }
        }
    }

    fun addChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val newRecord = calculateRangeAdded(record, null)
            repository.addRecord(newRecord)
        }
    }

    fun updateChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val updatedRecord = calculateRangeAdded(record, findPreviousRecord(record))
            repository.updateRecord(updatedRecord)
        }
    }

    fun deleteChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    private fun calculateRangeAdded(record: ChargingRecord, previousRecord: ChargingRecord?): ChargingRecord {
        return if (previousRecord != null) {
            record.copy(rangeAdded = record.totalRange - previousRecord.totalRange)
        } else {
            record.copy(rangeAdded = record.totalRange)
        }
    }

    private fun findPreviousRecord(record: ChargingRecord): ChargingRecord? {
        val sortedRecords = chargingRecords.value.sortedBy {
            try {
                record.date.split("/").map { it.toInt() }.let { parts ->
                    when (parts.size) {
                        2    -> parts[0] * 100 + parts[1]
                        3    -> parts[0] * 1000 + parts[1] * 100 + parts[2]
                        else -> Int.MAX_VALUE   // 如果格式不正确，将该记录排到最后
                    }
                }
            } catch (e: NumberFormatException) {   // 如果无法解析为数字，将该记录排到最后
                Int.MAX_VALUE
            }
        }
        val index = sortedRecords.indexOfFirst { it.id == record.id }
        return if (index > 0) sortedRecords[index - 1] else null
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
}