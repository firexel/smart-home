package com.seraph.smarthome.client.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.seraph.smarthome.client.app.services
import com.seraph.smarthome.client.model.WidgetGroupModel
import com.seraph.smarthome.client.model.WidgetModel
import com.seraph.smarthome.client.presentation.WidgetListPresenterImpl
import ru.mail.march.interactor.InteractorObtainers
import java.util.*
import kotlin.math.sqrt

val p1 = 8.dp
val p2 = 16.dp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgets = mutableStateOf(listOf<WidgetGroupModel>())
        val presenter = WidgetListPresenterImpl(
                InteractorObtainers.Companion.from(this), services
        )
        presenter.widgets.observe {
            widgets.value = it
        }
        setContent {
            val widgetList: List<WidgetGroupModel> by widgets
            Content(widgetList)
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
            is WidgetModel.BrokenWidget -> BrokenWidget(widget)
            is WidgetModel.CompositeWidget -> CompositeWidget(widget)
        }
    }

    @Composable
    fun CompositeWidget(widget: WidgetModel.CompositeWidget) {
        val bg = when (widget.category) {
            WidgetModel.CompositeWidget.Category.LIGHT ->
                Color(0xffeed690) to Color(0xffdca324)

            else -> Color(0xffa4a4a4) to Color(0xff949494)
        }

        NamedCard(
                name = widget.name,
                bg = bg,
                onClick = widget.toggle) {

            val typo = MaterialTheme.typography

            when (val state = widget.state) {
                is WidgetModel.CompositeWidget.State.Binary -> BinaryState(state, typo)
                is WidgetModel.CompositeWidget.State.Numeric -> NumericState(state, typo)
                is WidgetModel.CompositeWidget.State.Unknown -> UnknownState(typo)
            }
        }
    }

    @Composable
    private fun UnknownState(typo: Typography) {
        Text("––", style = typo.h3)
    }

    @Composable
    private fun NumericState(state: WidgetModel.CompositeWidget.State.Numeric, typo: Typography) {
        val unitsInline = when (state.units) {
            WidgetModel.CompositeWidget.Units.CELSIUS -> "°"
            else -> ""
        }

        val unitsOutline = when (state.units) {
            WidgetModel.CompositeWidget.Units.PPM -> "ppm"
            WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> "%"
            else -> ""
        }

        val valueMultiplier = when (state.units) {
            WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> 100
            else -> 1
        }

        val value = "%.${state.precision}f"
                .format(Locale.ENGLISH, valueMultiplier * state.state)

        Row {
            Text(value + unitsInline, style = typo.h3, modifier = Modifier.alignByBaseline())
            if (unitsOutline.isNotEmpty()) {
                Text(unitsOutline, style = typo.body1, modifier = Modifier.alignByBaseline())
            }
        }
    }

    @Composable
    private fun BinaryState(state: WidgetModel.CompositeWidget.State.Binary, typo: Typography) {
        val txt = when (state.units) {
            WidgetModel.CompositeWidget.Units.ON_OFF ->
                if (state.state) "ON" else "OFF"
            else ->
                if (state.state) "True" else "False"
        }
        Text(txt, style = typo.h3)
    }

    @Composable
    fun BrokenWidget(widget: WidgetModel.BrokenWidget) {
        NamedCard(name = widget.name, bg = Color(0xffbd0106) to Color(0xff870501)) {
            Text(widget.message, style = MaterialTheme.typography.subtitle2)
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
                modifier = Modifier.aspectRatio(1.718f),
                elevation = p2) {

            Column(Modifier.gradientBackground(bg.first, bg.second).let {
                if (onClick != null) {
                    it.clickable(onClick = onClick)
                } else {
                    it
                }
            }) {

                androidx.compose.material.Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(1f),
                        elevation = p1) {

                    Box(modifier = Modifier.padding(start = p1, end = p1),
                            contentAlignment = Alignment.CenterStart,
                            content = child
                    )
                }

                Text(name, style = typo.body2,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                                .fillMaxWidth(1f)
                                .padding(start = p1, top = 6.dp, bottom = p1, end = p1)
                )
            }
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

    @Preview
    @Composable
    fun ContentPreview() {
        Content(getTestData())
    }

    private fun getTestData(): List<WidgetGroupModel> {
        val nope: () -> Unit = {}
        val groups = listOf(
                WidgetGroupModel(
                        "Гостиная", listOf(
                        WidgetModel.CompositeWidget(
                                "CO2",
                                WidgetModel.CompositeWidget.Category.GAUGE,
                                WidgetModel.CompositeWidget.State.Numeric(
                                        WidgetModel.CompositeWidget.Units.PPM,
                                        5000f
                                )
                        ),
                        WidgetModel.CompositeWidget(
                                "Температура",
                                WidgetModel.CompositeWidget.Category.GAUGE,
                                WidgetModel.CompositeWidget.State.Numeric(
                                        WidgetModel.CompositeWidget.Units.CELSIUS,
                                        23.6f,
                                        precision = 1
                                )
                        ),
                        WidgetModel.CompositeWidget(
                                "Влажность",
                                WidgetModel.CompositeWidget.Category.GAUGE,
                                WidgetModel.CompositeWidget.State.Numeric(
                                        WidgetModel.CompositeWidget.Units.PERCENTS_0_1,
                                        0.37f
                                )
                        ),
                        WidgetModel.CompositeWidget(
                                "PM2.5",
                                WidgetModel.CompositeWidget.Category.GAUGE,
                                WidgetModel.CompositeWidget.State.Numeric(
                                        WidgetModel.CompositeWidget.Units.PPM,
                                        853f
                                )
                        ),
                        WidgetModel.CompositeWidget(
                                "Основной",
                                WidgetModel.CompositeWidget.Category.LIGHT,
                                WidgetModel.CompositeWidget.State.Binary(
                                        WidgetModel.CompositeWidget.Units.ON_OFF,
                                        false
                                ),
                                toggle = nope
                        ),
                        WidgetModel.CompositeWidget(
                                "Рабочее место",
                                WidgetModel.CompositeWidget.Category.LIGHT,
                                WidgetModel.CompositeWidget.State.Binary(
                                        WidgetModel.CompositeWidget.Units.ON_OFF,
                                        true
                                ),
                                toggle = nope
                        ),
                        WidgetModel.CompositeWidget(
                                "Коридор",
                                WidgetModel.CompositeWidget.Category.LIGHT,
                                WidgetModel.CompositeWidget.State.Unknown(),
                                toggle = nope
                        ),
                        WidgetModel.BrokenWidget(
                                "Сломан",
                                "TypeMismatchException at input state"
                        ),
                )),
                WidgetGroupModel("Кухня", listOf(
                        WidgetModel.CompositeWidget(
                                "Печка",
                                WidgetModel.CompositeWidget.Category.SWITCH,
                                WidgetModel.CompositeWidget.State.Binary(
                                        WidgetModel.CompositeWidget.Units.ON_OFF,
                                        false
                                ),
                                toggle = nope
                        ),
                        WidgetModel.CompositeWidget(
                                "Сигнализация CO длинное имя",
                                WidgetModel.CompositeWidget.Category.GAUGE,
                                WidgetModel.CompositeWidget.State.Binary(
                                        WidgetModel.CompositeWidget.Units.NONE,
                                        true
                                ),
                                toggle = nope
                        ),
                )))
        return groups
    }
}