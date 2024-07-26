package yuu.record

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import yuu.record.viewmodel.ChargingViewModel

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ChargingViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<ChargingRecord?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showChartPage by remember { mutableStateOf(false) }
    val records by viewModel.chargingRecords.collectAsState()
    val context = LocalContext.current

    // 添加 BackHandler
    BackHandler(enabled = showChartPage) {
        showChartPage = false
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("电动车充电记录") }, actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("充电记录图表") }, onClick = {
                    showChartPage = true
                    showMenu = false
                })
                DropdownMenuItem(text = { Text("导出数据到Excel") }, onClick = {
                    viewModel.exportToExcel(context)
                    showMenu = false
                })
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = "添加记录")
        }
    }) { paddingValues ->
        if (showChartPage) {
            ChargingChart(records = records)
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                ChargingList(records = records, onEdit = { editingRecord = it }, onDelete = { viewModel.deleteChargingRecord(it) })
            }
        }
    }

    if (showAddDialog) {
        AddChargingDialog(onDismiss = { showAddDialog = false }, onAdd = { record ->
            viewModel.addChargingRecord(record)
            showAddDialog = false
        })
    }

    editingRecord?.let { record ->
        AddChargingDialog(onDismiss = { editingRecord = null }, onAdd = { updatedRecord ->
            viewModel.updateChargingRecord(updatedRecord.copy(id = record.id))
            editingRecord = null
        }, initialRecord = record)
    }
}

@Composable
fun AddChargingDialog(
    onDismiss: () -> Unit, onAdd: (ChargingRecord) -> Unit, initialRecord: ChargingRecord? = null
) {
    var date by remember { mutableStateOf(initialRecord?.date ?: "") }
    var rangeAdded by remember { mutableStateOf(initialRecord?.rangeAdded?.toString() ?: "") }
    var chargingTime by remember { mutableStateOf(initialRecord?.chargingTime ?: "") }
    var chargingCost by remember { mutableStateOf(initialRecord?.chargingCost?.toString() ?: "") }
    var totalRange by remember { mutableStateOf(initialRecord?.totalRange?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialRecord?.notes ?: "") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initialRecord == null) "添加充电记录" else "编辑充电记录") }, text = {
        Column {
            TextField(value = date, onValueChange = { date = it }, label = { Text("日期") })
//            TextField(value = rangeAdded, onValueChange = { rangeAdded = it }, label = { Text("续航里程") })
            TextField(value = chargingTime, onValueChange = { chargingTime = it }, label = { Text("充电时间") })
            TextField(value = chargingCost, onValueChange = { chargingCost = it }, label = { Text("充电金额") })
            TextField(value = totalRange, onValueChange = { totalRange = it }, label = { Text("当前总续航") })
            TextField(value = notes, onValueChange = { notes = it }, label = { Text("备注") })
        }
    }, confirmButton = {
        Button(onClick = {
            onAdd(
                ChargingRecord(
                    date = date,
                    rangeAdded = rangeAdded.toIntOrNull() ?: 0,
                    chargingTime = chargingTime,
                    chargingCost = chargingCost.toDoubleOrNull() ?: 0.0,
                    totalRange = totalRange.toIntOrNull() ?: 0,
                    notes = notes
                )
            )
        }) {
            Text(if (initialRecord == null) "添加" else "更新")
        }
    }, dismissButton = {
        Button(onClick = onDismiss) {
            Text("取消")
        }
    })
}