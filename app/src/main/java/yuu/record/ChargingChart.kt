package yuu.record

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@Composable
fun ChargingChart(records: List<ChargingRecord>) {
    val chartEntries = records.mapIndexed { index, record ->
        index.toFloat() to (record.rangeAdded.toFloat() / (record.chargingTime.toFloatOrNull() ?: 1f))
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("充电效率图表", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Chart(
            chart = lineChart(), model = entryModelOf(*chartEntries.toTypedArray()), startAxis = startAxis(), bottomAxis = bottomAxis(), modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}
