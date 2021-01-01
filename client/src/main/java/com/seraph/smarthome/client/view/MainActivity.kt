package com.seraph.smarthome.client.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
import com.seraph.smarthome.client.model.WidgetGroupModel
import com.seraph.smarthome.client.model.WidgetModel
import java.util.*
import kotlin.math.sqrt

val p1 = 8.dp
val p2 = 16.dp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Content(getTestData())
        }
    }

    @Composable
    private fun Content(groups: List<WidgetGroupModel>) {
        MaterialTheme {
            ScrollableColumn(modifier = Modifier.background(Color.White)) {
                groups.forEach { group ->
                    Group(group)
                }
            }
        }
    }

    @Composable
    private fun Group(group: WidgetGroupModel) {
        val typo = MaterialTheme.typography
        Text(group.name, style = typo.h4, modifier = Modifier.padding(start = 16.dp))
        GridView(2, group.widgets) {
            Widget(it)
        }
    }

    @Composable
    fun <T> GridView(columns: Int, items: List<T>, child: @Composable (T) -> Unit) {
        val rows = items.chunked(columns)
        val padding = 8.dp
        Column(modifier = Modifier.padding(padding)) {
            rows.forEach { row ->
                Row {
                    for ((index, item) in row.withIndex()) {
                        Box(Modifier
                                .fillMaxWidth(1f / (columns - index))
                                .padding(padding)) {
                            child(item)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Widget(widget: WidgetModel) {
        when (widget) {
            is WidgetModel.BinaryLight -> BinaryLight(widget)
            is WidgetModel.BrokenWidget -> BrokenWidget(widget)
            is WidgetModel.Gauge -> Gauge(widget)
        }
    }

    fun Modifier.gradientBackground(firstColor: Color, lastColor: Color) = drawWithCache {
        onDrawBehind {
            val radius = sqrt(size.height * size.height + size.width * size.width)
            val gradient = RadialGradient(
                    0f to firstColor,
                    0.5f to firstColor,
                    1f to lastColor,
                    centerX = 0f,
                    centerY = 0f,
                    radius = radius
            )
            drawRect(brush = gradient)
        }
    }

    @Composable
    fun BinaryLight(widget: WidgetModel.BinaryLight) {

        val state = when (widget.isOn) {
            null -> "––"
            false -> "OFF"
            true -> "ON"
        }

        NamedCard(
                name = widget.name,
                bg = Color(0xffeed690) to Color(0xffdca324),
                onClick = widget.toggle) {

            Text(state, style = MaterialTheme.typography.h3)
        }
    }

    @Composable
    fun NamedCard(
            name: String,
            bg: Pair<Color, Color>,
            onClick: (() -> Unit)? = null,
            child: @Composable BoxScope.() -> Unit,
    ) {

        val typo = MaterialTheme.typography

        androidx.compose.material.Card(
                modifier = Modifier.aspectRatio(1.718f).let {
                    if (onClick != null) {
                        it.clickable(onClick = onClick)
                    } else {
                        it
                    }
                },
                elevation = p2) {

            Column(Modifier.gradientBackground(bg.first, bg.second)) {

                androidx.compose.material.Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(1f),
                        elevation = p1) {

                    Box(modifier = Modifier.padding(start = p1, end = p1),
                            alignment = Alignment.CenterStart,
                            children = child
                    )
                }

                Text(name, style = typo.body2, modifier = Modifier
                        .fillMaxWidth(1f)
                        .padding(start = p1, top = 6.dp, bottom = p1)
                )
            }
        }
    }

    @Composable
    fun BrokenWidget(widget: WidgetModel.BrokenWidget) {
        NamedCard(name = widget.name, bg = Color(0xffbd0106) to Color(0xff870501)) {
            Text(widget.message, style = MaterialTheme.typography.subtitle2)
        }
    }

    @Preview
    @Composable
    fun ContentPreview() {
        Content(getTestData())
    }

    @Composable
    fun Gauge(gauge: WidgetModel.Gauge) {

        val unitsInline = when (gauge.units) {
            WidgetModel.Units.TEMP_CELSIUS -> "°"
            else -> ""
        }

        val unitsOutline = when (gauge.units) {
            WidgetModel.Units.CO2_PPM -> "ppm"
            WidgetModel.Units.PM25_PPM -> "ppm"
            WidgetModel.Units.HUMIDITY_PERCENT -> "%"
            else -> ""
        }

        val accuracy = when (gauge.units) {
            WidgetModel.Units.CO2_PPM -> 0
            WidgetModel.Units.TEMP_CELSIUS -> 1
            WidgetModel.Units.HUMIDITY_PERCENT -> 0
            WidgetModel.Units.PM25_PPM -> 0
        }

        val value = if (gauge.value == null) "?" else "%.${accuracy}f".format(Locale.ENGLISH, gauge.value)

        val nameReplacement = when (gauge.units) {
            WidgetModel.Units.CO2_PPM -> "CO2"
            WidgetModel.Units.TEMP_CELSIUS -> "Температура"
            WidgetModel.Units.HUMIDITY_PERCENT -> "Влажность"
            WidgetModel.Units.PM25_PPM -> "PM 2.5"
        }

        val bg = when (gauge.units) {
            WidgetModel.Units.CO2_PPM -> Color(0xffa4a4a4) to Color(0xff949494)
            WidgetModel.Units.TEMP_CELSIUS -> Color(0xffa4a4a4) to Color(0xff949494)
            WidgetModel.Units.HUMIDITY_PERCENT -> Color(0xffa4a4a4) to Color(0xff949494)
            WidgetModel.Units.PM25_PPM -> Color(0xffa4a4a4) to Color(0xff949494)
        }

        val name = if (gauge.name.isEmpty()) nameReplacement else gauge.name
        val typo = MaterialTheme.typography

        NamedCard(name = name, bg = bg) {
            Row {
                Text(value + unitsInline, style = typo.h3, modifier = Modifier.alignByBaseline())
                if (unitsOutline.isNotEmpty()) {
                    Text(unitsOutline, style = typo.body1, modifier = Modifier.alignByBaseline())
                }
            }
        }
    }

    private fun getTestData(): List<WidgetGroupModel> {
        val nope: () -> Unit = {}
        val groups = listOf(
                WidgetGroupModel(
                        "Гостиная", listOf(
                        WidgetModel.Gauge("", 5000f, WidgetModel.Units.CO2_PPM),
                        WidgetModel.Gauge("", 23.6f, WidgetModel.Units.TEMP_CELSIUS),
                        WidgetModel.Gauge("", 37f, WidgetModel.Units.HUMIDITY_PERCENT),
                        WidgetModel.Gauge("", 853f, WidgetModel.Units.PM25_PPM),
                        WidgetModel.BinaryLight("Основной", false, nope),
                        WidgetModel.BinaryLight("Рабочее место", true, nope),
                        WidgetModel.BinaryLight("Коридор", null, nope),
                        WidgetModel.BrokenWidget("Сломан", "TypeMismatchException at input state"),
                )),
                WidgetGroupModel("Кухня", listOf(
                        WidgetModel.BinaryLight("Основной", true, nope),
                        WidgetModel.BinaryLight("Над столом", false, nope),
                        WidgetModel.BinaryLight("Основной", true, nope),
                        WidgetModel.BinaryLight("Над столом", false, nope),
                        WidgetModel.BinaryLight("Основной", true, nope),
                        WidgetModel.BinaryLight("Над столом", false, nope),
                        WidgetModel.BinaryLight("Основной", true, nope),
                        WidgetModel.BinaryLight("Над столом", false, nope),
                        WidgetModel.BinaryLight("Основной", true, nope),
                        WidgetModel.BinaryLight("Над столом", false, nope)
                )))
        return groups
    }
}