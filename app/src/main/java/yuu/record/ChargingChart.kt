package yuu.record

/**
 * @Author      : yuu
 * @Date        : 2024-07-23
 * @Description :
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import yuu.record.data.ChargingRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingChart(records: List<ChargingRecord>) {
    val window = (LocalContext.current as? android.app.Activity)?.window

    DisposableEffect(Unit) {
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                it.statusBarColor = android.graphics.Color.BLACK // 或者你想要的默认颜色
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 添加自定义的顶部栏，包含返回按钮
        TopAppBar(title = { Text("充电效率图表") }, navigationIcon = {})

        // 图表内容
        Box(modifier = Modifier.fillMaxSize()) {
            if (records.size > 1) {
                val chartEntries = calculateChartEntries(records)

                val chart = lineChart(
                    lines = listOf(
                        LineChart.LineSpec(
//                            color = Color.Blue.toArgb(), thickness = 2.dp.value, pointSize = 0f  // 设置为0以移除点
                        )
                    )
                )

                val xAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                    "${value.toInt()}%" // 显示为百分比
                }

                val yAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                    "%.2f".format(value)
                }

                Chart(
                    chart = chart, model = entryModelOf(chartEntries), startAxis = startAxis(
                        valueFormatter = yAxisValueFormatter, title = "充电效率", tickLength = 0.dp
                    ), bottomAxis = bottomAxis(
                        valueFormatter = xAxisValueFormatter, title = "时间进度", tickLength = 0.dp
                    ), modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            } else {
                Text("没有足够的数据来生成图表", modifier = Modifier.padding(16.dp))
            }
        }

    }
}

private fun calculateChartEntries(records: List<ChargingRecord>): List<FloatEntry> {
    if (records.size < 2) return emptyList()

    val sortedRecords = records.sortedBy { it.date }
    val firstDate = parseDate(sortedRecords.first().date)
    val lastDate = parseDate(sortedRecords.last().date)
    val totalDays = ((lastDate.time - firstDate.time) / (1000 * 60 * 60 * 24)).toFloat().coerceAtLeast(1f)

    return sortedRecords.zipWithNext().mapIndexed { index, (current, next) ->
        val currentDate = parseDate(current.date)
        val daysFromStart = ((currentDate.time - firstDate.time) / (1000 * 60 * 60 * 24)).toFloat()
        val xValue = (daysFromStart / totalDays) * 100 // 转换为百分比
        val yValue = next.rangeAdded.toFloat() / (current.chargingTime.toFloatOrNull() ?: 1f)
        FloatEntry(xValue, yValue)
    }
}

private fun parseDate(dateString: String): Date {
    val format = SimpleDateFormat("M/d", Locale.getDefault())
    return format.parse(dateString) ?: Date()
}