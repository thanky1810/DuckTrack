package com.example.ducktrack.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.example.ducktrack.MyApplication
import com.example.ducktrack.R
import com.example.ducktrack.ui.main.tasks.TodoTask

// --- ĐỊNH NGHĨA TRỰC TIẾP MÀU BẰNG COMPOSE COLOR ---
val WidgetBgWhite = Color(0xFFFFFFFF)
val WidgetTextGray = Color(0xFF9E9E9E)
val WidgetTextBlack = Color(0xFF212121)

val WidgetBgRed = Color(0xFFFFEBEE)
val WidgetTextRed = Color(0xFFD32F2F)

val WidgetBgBlue = Color(0xFFE3F2FD)
val WidgetTextBlue = Color(0xFF1976D2)

val WidgetBgOrange = Color(0xFFFFF3E0)
val WidgetTextOrange = Color(0xFFF57C00)

val WidgetBgGreen = Color(0xFFE8F5E9)
val WidgetTextGreen = Color(0xFF388E3C)

// [ĐÃ SỬA]: Hàm hỗ trợ truyền 1 màu cho cả Day/Night để không bị lỗi thiếu tham số
fun singleColor(color: Color) = ColorProvider(day = color, night = color)
// -------------------------------------------------------------------------

// 1. Class Receiver
class DuckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DuckWidget()
}

// 2. Action để làm mới Widget
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        DuckWidget().update(context, glanceId)
    }
}

// 3. Class Widget chính
class DuckWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as MyApplication).repository

        provideContent {
            val tasks by repo.getTasksStream(System.currentTimeMillis()).collectAsState(initial = emptyList())

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(singleColor(WidgetBgWhite))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↻ Làm mới",
                            style = TextStyle(
                                color = singleColor(WidgetTextGray),
                                fontSize = 12.sp
                            ),
                            modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>())
                        )
                    }
                    EisenhowerWidgetContent(tasks)
                }
            }
        }
    }

    @Composable
    fun EisenhowerWidgetContent(tasks: List<TodoTask>) {
        val doNow = tasks.filter { it.isImportant && it.isUrgent }.sortedBy { it.isCompleted }.take(5)
        val schedule = tasks.filter { it.isImportant && !it.isUrgent }.sortedBy { it.isCompleted }.take(5)
        val delegate = tasks.filter { !it.isImportant && it.isUrgent }.sortedBy { it.isCompleted }.take(5)
        val delete = tasks.filter { !it.isImportant && !it.isUrgent }.sortedBy { it.isCompleted }.take(5)

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Hàng 1
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                QuadrantBox("Quan trọng & Khẩn cấp", WidgetBgRed, WidgetTextRed, doNow, R.drawable.duck_waiting, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                QuadrantBox("Quan trọng & Không khẩn cấp", WidgetBgBlue, WidgetTextBlue, schedule, R.drawable.duck_farming, GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(4.dp))
            // Hàng 2
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                QuadrantBox("Không quan trọng & Khẩn cấp", WidgetBgOrange, WidgetTextOrange, delegate, R.drawable.duck_happy, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                QuadrantBox("Không quan trọng & Không khẩn cấp", WidgetBgGreen, WidgetTextGreen, delete, R.drawable.duck_celebrate, GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    fun QuadrantBox(
        title: String,
        bgColor: Color,
        textColor: Color,
        tasks: List<TodoTask>,
        emptyImageRes: Int,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(singleColor(bgColor))
                .cornerRadius(12.dp)
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = singleColor(textColor),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                maxLines = 2
            )
            Spacer(GlanceModifier.height(6.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(emptyImageRes),
                        contentDescription = "Empty",
                        modifier = GlanceModifier.size(80.dp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks) { task ->
                        TaskItemWidget(task)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun TaskItemWidget(task: TodoTask) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            androidx.glance.appwidget.CheckBox(
                checked = task.isCompleted,
                onCheckedChange = actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(
                        com.example.ducktrack.ui.widget.ActionTaskKey to task.id,
                        com.example.ducktrack.ui.widget.ActionIsCompletedKey to task.isCompleted
                    )
                )
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = task.description,
                style = TextStyle(
                    color = singleColor(if (task.isCompleted) WidgetTextGray else WidgetTextBlack),
                    fontSize = 13.sp,
                    textDecoration = if (task.isCompleted) androidx.glance.text.TextDecoration.LineThrough else androidx.glance.text.TextDecoration.None
                ),
                maxLines = 1
            )
        }
    }
}