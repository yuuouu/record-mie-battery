package yuu.record

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*  // 注意这里使用的是 material，不是 material3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChargingList(
    records: List<ChargingRecord>, onEdit: (ChargingRecord) -> Unit, onDelete: (ChargingRecord) -> Unit
) {
    var recordToDelete by remember { mutableStateOf<ChargingRecord?>(null) }

    LazyColumn {
        items(records, key = { it.id }) { record ->
            val dismissState = rememberDismissState(confirmStateChange = {
                if (it == DismissValue.DismissedToStart) {
                    recordToDelete = record
                    false
                } else {
                    true
                }
            })

            SwipeToDismiss(state = dismissState, background = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError
                    )
                }
            }, dismissContent = {
                ChargingRecordItem(record = record, onEdit = onEdit, modifier = Modifier.clickable { onEdit(record) })
            }, directions = setOf(DismissDirection.EndToStart)
            )
        }
    }

    recordToDelete?.let { record ->
        AlertDialog(onDismissRequest = { recordToDelete = null }, title = { Text("确认删除") }, text = { Text("您确定要删除这条记录吗？") }, confirmButton = {
            Button(onClick = {
                onDelete(record)
                recordToDelete = null
            }) {
                Text("确认")
            }
        }, dismissButton = {
            Button(onClick = { recordToDelete = null }) {
                Text("取消")
            }
        })
    }
}

@Composable
fun ChargingRecordItem(
    record: ChargingRecord, onEdit: (ChargingRecord) -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp), elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("日期: ${record.date}", style = MaterialTheme.typography.bodyMedium)
            Text("续航里程: ${record.rangeAdded} km", style = MaterialTheme.typography.bodyMedium)
            Text("充电时间: ${record.chargingTime} h", style = MaterialTheme.typography.bodyMedium)
            Text("充电金额: ¥${record.chargingCost}", style = MaterialTheme.typography.bodyMedium)
            Text("当前总续航: ${record.totalRange} km", style = MaterialTheme.typography.bodyMedium)
            Text("备注: ${record.notes}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}