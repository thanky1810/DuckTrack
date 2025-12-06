package com.example.ducktrack.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.glance.unit.ColorProvider
import com.example.ducktrack.MyApplication
import com.example.ducktrack.R
import com.example.ducktrack.ui.main.tasks.TodoTask

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
                        .background(ColorProvider(R.color.widget_bg_white))
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
                                color = ColorProvider(R.color.widget_text_gray),
                                fontSize = 12.sp // Tăng nhẹ nút làm mới
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
        // Lấy nhiều task hơn (5 task mỗi ô)
        val doNow = tasks.filter { it.isImportant && it.isUrgent }.sortedBy { it.isCompleted }.take(5)
        val schedule = tasks.filter { it.isImportant && !it.isUrgent }.sortedBy { it.isCompleted }.take(5)
        val delegate = tasks.filter { !it.isImportant && it.isUrgent }.sortedBy { it.isCompleted }.take(5)
        val delete = tasks.filter { !it.isImportant && !it.isUrgent }.sortedBy { it.isCompleted }.take(5)

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Hàng 1
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                QuadrantBox("Quan trọng & Khẩn cấp", R.color.widget_bg_red, R.color.widget_text_red, doNow, R.drawable.duck_waiting, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                QuadrantBox("Quan trọng & Không khẩn cấp", R.color.widget_bg_blue, R.color.widget_text_blue, schedule, R.drawable.duck_farming, GlanceModifier.defaultWeight())
            }
            Spacer(GlanceModifier.height(4.dp))
            // Hàng 2
            Row(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                QuadrantBox("Không quan trọng & Khẩn cấp", R.color.widget_bg_orange, R.color.widget_text_orange, delegate, R.drawable.duck_happy, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(4.dp))
                QuadrantBox("Không quan trọng & Không khẩn cấp", R.color.widget_bg_green, R.color.widget_text_green, delete, R.drawable.duck_celebrate, GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    fun QuadrantBox(
        title: String,
        bgColorRes: Int,
        textColorRes: Int,
        tasks: List<TodoTask>,
        emptyImageRes: Int,
        modifier: GlanceModifier
    ) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(ColorProvider(bgColorRes))
                .cornerRadius(12.dp) // Bo góc to hơn xíu
                .padding(8.dp)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(textColorRes),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp // Tăng tiêu đề lên 13sp
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
                // Dùng LazyColumn để nếu list dài quá 5 cái thì có thể cuộn được (nếu Widget đủ to)
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(tasks) { task ->
                        TaskItemWidget(task)
                        Spacer(GlanceModifier.height(6.dp)) // Khoảng cách giữa các task thoáng hơn
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
                    color = ColorProvider(if(task.isCompleted) R.color.widget_text_gray else R.color.widget_text_black),
                    fontSize = 13.sp, // Tăng nội dung lên 13sp
                    textDecoration = if (task.isCompleted) androidx.glance.text.TextDecoration.LineThrough else androidx.glance.text.TextDecoration.None
                ),
                maxLines = 1
            )
        }
    }
}