package yuu.record.viewmodel

import android.content.Context
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
            val newRecord = calculateRangeAdded(record)
            repository.addRecord(newRecord)
        }
    }

    fun updateChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            val updatedRecord = calculateRangeAdded(record)
            repository.updateRecord(updatedRecord)
        }
    }

    fun deleteChargingRecord(record: ChargingRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    private fun calculateRangeAdded(record: ChargingRecord): ChargingRecord {
        val previousRecord = chargingRecords.value.lastOrNull()
        return if (previousRecord != null) {
            record.copy(rangeAdded = record.totalRange-previousRecord.totalRange)
        } else {
            record.copy(rangeAdded = record.totalRange)
        }
    }


    fun exportToExcel(context: Context) {
        viewModelScope.launch {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("充电记录")

            // Create header row
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("日期")
            headerRow.createCell(1).setCellValue("续航里程")
            headerRow.createCell(2).setCellValue("充电时间")
            headerRow.createCell(3).setCellValue("充电金额")
            headerRow.createCell(4).setCellValue("当前总续航")
            headerRow.createCell(5).setCellValue("备注")

            // Fill data
            chargingRecords.value.forEachIndexed { index, record ->
                val row = sheet.createRow(index+1)
                row.createCell(0).setCellValue(record.date)
                row.createCell(1).setCellValue(record.rangeAdded.toDouble())
                row.createCell(2).setCellValue(record.chargingTime)
                row.createCell(3).setCellValue(record.chargingCost)
                row.createCell(4).setCellValue(record.totalRange.toDouble())
                row.createCell(5).setCellValue(record.notes)
            }

            // Save the workbook
            val fileName = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())+"记录.xlsx"
            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use {
                workbook.write(it)
            }
            workbook.close()
        }
    }
}