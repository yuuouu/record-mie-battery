package yuu.record.data

/**
 * @Author      : yuu
 * @Date        : 2024-07-26
 * @Description :
 */
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import yuu.record.viewmodel.ChargingViewModel

@Composable
fun DataManagementScreen(viewModel: ChargingViewModel, context: Context) {
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
        if (uri != null) {
            showImportDialog = true
        }
    }

    Column {
        Button(onClick = { launcher.launch("application/json") }) {
            Text("选择JSON文件导入")
        }

        if (showImportDialog && selectedUri != null) {
            AlertDialog(onDismissRequest = { showImportDialog = false }, title = { Text("确认导入") }, text = { Text("1,是否继续？") }, confirmButton = {
                Button(onClick = {
                    showImportDialog = false
                    viewModel.importJsonData(context, selectedUri!!)
                    selectedUri = null
                }) {
                    Text("确认导入")
                }
            }, dismissButton = {
                Button(onClick = {
                    showImportDialog = false
                    selectedUri = null
                }) {
                    Text("取消")
                }
            })
        }
    }
}