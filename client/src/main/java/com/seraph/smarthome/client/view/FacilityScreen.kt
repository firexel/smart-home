package com.seraph.smarthome.client.view

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.seraph.smarthome.client.app.services
import com.seraph.smarthome.client.model.WidgetGroupModel
import com.seraph.smarthome.client.model.WidgetModel
import com.seraph.smarthome.client.presentation.WidgetListPresenter
import com.seraph.smarthome.client.presentation.WidgetListPresenterImpl
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

val p1 = 8.dp
val p2 = 16.dp

class FacilityScreen : AppCompatActivity() {

    private val defaultState = WidgetListPresenter.ViewModel(
        null, emptyList(), emptyList()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val model = mutableStateOf(defaultState)
        val presenter = WidgetListPresenterImpl(services)

        lifecycleScope.launch {
            presenter.run()
                .flowWithLifecycle(lifecycle)
                .collect {
                    model.value = it
                }
        }

        setContent {
            val modelSnapshot by model
            Content(modelSnapshot)
        }
    }

    @Composable
    private fun Content(model: WidgetListPresenter.ViewModel) {
        MaterialTheme {
            CombinedList(model)
        }
    }

    @Composable
    private fun CombinedList(model: WidgetListPresenter.ViewModel) {

        LazyColumn(
            modifier = Modifier
                .background(Color.White)
                .fillMaxHeight()
        ) {
            item { FacilityHeader(model) }
            items(
                model.groups.size,
                { model.groups[it].name.hashCode() },
                { 1 },
                { Group(model.groups[it]) }
            )
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun FacilityHeader(model: WidgetListPresenter.ViewModel, defExpanded: Boolean = false) {
        var expanded by remember { mutableStateOf(defExpanded) }
        val transition = updateTransition(targetState = expanded, label = "HeaderTransition")
        val elevation by transition.animateDp(label = "elevation") {
            when (it) {
                true -> 8.dp
                false -> 0.dp
            }
        }
        val outerPadding by transition.animateDp(label = "outer") {
            when (it) {
                true -> 16.dp
                false -> 0.dp
            }
        }

        Column(
            modifier = Modifier.padding(
                start = outerPadding,
                end = outerPadding,
                top = outerPadding
            )
        ) {
            val stroke by transition.animateColor(label = "color") {
                when (it) {
                    true -> Color(0xffa4a4a4)
                    false -> Color(0)
                }
            }
            Card(
                elevation = elevation,
                border = BorderStroke(1.dp, stroke),
                onClick = { expanded = !expanded }
            ) {
                if (model.currentFacility != null) {
                    Column(Modifier) {
                        Facility(model.currentFacility, expanded)
                    }
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                    ) {
                        if (model.otherFacilities.isEmpty()) {
                            Text("Нет доступных домов", style = MaterialTheme.typography.h4)
                        } else {
                            Text("Выберите дом...", style = MaterialTheme.typography.h4)
                        }
                    }
                }
            }
            if (model.otherFacilities.isNotEmpty()) {
                AnimatedVisibility(expanded) {
                    Column {
                        model.otherFacilities.forEach {
                            Card(
                                elevation = 8.dp,
                                border = BorderStroke(1.dp, Color(0xffa4a4a4)),
                                onClick = {
                                    expanded = !expanded
                                    it.select()
                                },
                                modifier = Modifier.padding(top = p1)
                            ) {
                                Facility(it, true)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Facility(place: WidgetListPresenter.FacilityViewModel, showCover: Boolean) {

        val transition = updateTransition(targetState = showCover, label = "FacilityTransition")
        val widthTransition by transition.animateDp(label = "width") {
            when (it) {
                true -> 112.dp
                else -> 0.dp
            }
        }
        val alphaTransition by transition.animateFloat(label = "alpha") {
            when (it) {
                true -> 1f
                else -> 0f
            }
        }
        val stroke by transition.animateColor(label = "color") {
            when (it) {
                true -> Color(0xffa4a4a4)
                false -> Color(0)
            }
        }

        Row(
            Modifier.fillMaxWidth()
        ) {
            Card(
                elevation = p2,
                border = BorderStroke(1.dp, stroke),
                modifier = Modifier
                    .width(widthTransition)
                    .height(84.dp)
                    .alpha(alphaTransition)
            ) {
                val w = with(LocalDensity.current) { 112.dp.roundToPx() }
                val h = with(LocalDensity.current) { 84.dp.roundToPx() }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(place.cover)
                        .size(w, h)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Home cover"
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(place.name, style = MaterialTheme.typography.h4, maxLines = 1)
                Text("Локальное подключение", style = MaterialTheme.typography.subtitle1)
            }
        }
    }

    private var prevGroupModel: WidgetGroupModel? = null

    @Composable
    private fun Group(group: WidgetGroupModel) {
        val typo = MaterialTheme.typography
        if (group.name == "Кабинет") {
            if (group == prevGroupModel) {
                Log.d("ComposeDbg", "Groups are equals")
            } else {
                Log.d("ComposeDbg", "Groups are NOT equals")
            }
            prevGroupModel = group
        }
        Text(
            group.name,
            style = typo.h4.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )
        GridView(integerResource(id = gridColumns), group.widgets) {
            Widget(group, it)
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
                        Box(
                            Modifier
                                .fillMaxWidth(1f / (columns - index))
                                .padding(padding)
                        ) {
                            child(item)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Widget(group: WidgetGroupModel, widget: WidgetModel) {
        when (widget) {
            is WidgetModel.BrokenWidget -> BrokenWidget(widget)
            is WidgetModel.CompositeWidget -> CompositeWidget(group.name, widget)
        }
    }

    @Composable
    fun CompositeWidget(groupName: String, widget: WidgetModel.CompositeWidget) {
        val bg = when (widget.category) {
            WidgetModel.CompositeWidget.Category.LIGHT ->
                Color(0xffeed690) to Color(0xffdca324)

            WidgetModel.CompositeWidget.Category.THERMOSTAT ->
                Color(0xFFFF9D9D) to Color(0xFF64A2FF)

            else -> Color(0xffa4a4a4) to Color(0xff949494)
        }

        var dialogShown by remember { mutableStateOf(false) }

        val onLongClick: (() -> Unit)? = when {
            widget.target != null -> {
                { dialogShown = true }
            }
            else -> null
        }

        val onClick: (() -> Unit)? = when {
            widget.toggle != null -> widget.toggle
            onLongClick != null -> onLongClick
            else -> null
        }

        NamedCard(
            name = widget.name,
            bg = bg,
            onClick = onClick,
            onLongClick = onLongClick
        ) {
            CompositeWidgetState(widget)
        }

        if (dialogShown) {
            Dialog(onDismissRequest = { dialogShown = false }) {
                ChangeTargetDialog(groupName, widget, bg)
            }
        }
    }

    @Composable
    private fun CompositeWidgetState(widget: WidgetModel.CompositeWidget) {
        when (val state = widget.state) {
            is WidgetModel.CompositeWidget.State.Binary -> BinaryState(state)
            is WidgetModel.CompositeWidget.State.NumericFloat -> NumericFloatState(state)
            is WidgetModel.CompositeWidget.State.NumericInt -> NumericIntState(state)
            is WidgetModel.CompositeWidget.State.Unknown -> UnknownState()
        }
    }

    @Composable
    private fun UnknownState() {
        Text("––", style = MaterialTheme.typography.h3)
    }

    @Composable
    private fun NumericFloatState(state: WidgetModel.CompositeWidget.State.NumericFloat) {
        val unitsInline = state.units.toUnitsInlineText()
        val unitsOutline = state.units.toUnitsOutlineText()
        val valueMultiplier = state.units.toUnitsMultiplier()
        val value = (valueMultiplier * state.state).format(state.precision)
        ValueWithUnits(value + unitsInline, unitsOutline)
    }

    @Composable
    private fun NumericIntState(state: WidgetModel.CompositeWidget.State.NumericInt) {
        ValueWithUnits(
            "${state.state}${state.units.toUnitsInlineText()}",
            state.units.toUnitsOutlineText()
        )
    }

    @Composable
    private fun ValueWithUnits(valueText: String, unitsOutline: String) {
        Row {
            Text(
                valueText,
                style = MaterialTheme.typography.h3,
                modifier = Modifier.alignByBaseline()
            )
            if (unitsOutline.isNotEmpty()) {
                Text(
                    unitsOutline,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }

    fun Float.format(precision: Int): String = "%.${precision}f".format(Locale.ENGLISH, this)

    @Composable
    private fun BinaryState(state: WidgetModel.CompositeWidget.State.Binary) {
        val txt = when (state.units) {
            WidgetModel.CompositeWidget.Units.ON_OFF ->
                if (state.state) "ON" else "OFF"
            else ->
                if (state.state) "True" else "False"
        }
        Text(txt, style = MaterialTheme.typography.h3)
    }

    @Composable
    fun BrokenWidget(widget: WidgetModel.BrokenWidget) {
        NamedCard(name = widget.name, bg = Color(0xffbd0106) to Color(0xff870501)) {
            Text(widget.message, style = MaterialTheme.typography.subtitle2)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun NamedCard(
        name: String,
        bg: Pair<Color, Color>,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
        child: @Composable BoxScope.() -> Unit,
    ) {

        val typo = MaterialTheme.typography

        Card(
            modifier = Modifier.aspectRatio(1.718f),
            elevation = p2,
            border = BorderStroke(1.dp, bg.second)
        ) {
            Column(
                Modifier
                    .gradientBackground(bg.first, bg.second)
                    .appendIf(onClick != null) {
                        if (onLongClick == null) {
                            clickable(onClick = onClick!!)
                        } else {
                            combinedClickable(
                                onClick = onClick!!,
                                onLongClick = onLongClick
                            )
                        }
                    }
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(1f),
                    elevation = p1
                ) {
                    Box(
                        modifier = Modifier.padding(start = p1, end = p1),
                        contentAlignment = Alignment.CenterStart,
                        content = child
                    )
                }

                Text(
                    name, style = typo.body2,
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

    @Composable
    private fun ChangeTargetDialog(
        groupName: String,
        widget: WidgetModel.CompositeWidget,
        bg: Pair<Color, Color>
    ) {
        val typo = MaterialTheme.typography
        Card(elevation = 16.dp) {
            Box(
                modifier = Modifier
                    .padding(start = p1 * 2, end = p1 * 2, top = p1, bottom = p1 * 2)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column {
                    Text(text = widget.name, style = typo.h4)
                    Text(
                        text = groupName,
                        style = typo.body1,
                        modifier = Modifier.padding(bottom = p1),
                        color = Color(0xb3000000)
                    )

                    CompositeWidgetState(widget)

                    when (widget.target) {
                        is WidgetModel.CompositeWidget.Target.Numeric -> NumericTarget(
                            widget.target,
                            bg
                        )
                        is WidgetModel.CompositeWidget.Target.Binary -> BinaryTarget(widget.target)
                    }
                }
            }
        }
    }

    @Composable
    fun BinaryTarget(target: WidgetModel.CompositeWidget.Target.Binary) {

    }

    @Composable
    fun NumericTarget(
        target: WidgetModel.CompositeWidget.Target.Numeric,
        bg: Pair<Color, Color>
    ) {
        val valueMultiplier = when (target.units) {
            WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> 100
            else -> 1
        }

        var sliderState by remember { mutableStateOf(target.state) }

        val colors = object : SliderColors {
            @Composable
            override fun thumbColor(enabled: Boolean): State<Color> =
                rememberUpdatedState(bg.second)

            @Composable
            override fun tickColor(enabled: Boolean, active: Boolean): State<Color> =
                rememberUpdatedState(if (active) bg.second else bg.first)

            @Composable
            override fun trackColor(enabled: Boolean, active: Boolean): State<Color> =
                rememberUpdatedState(if (active) bg.second else bg.first)

        }
        Slider(
            value = sliderState,
            valueRange = target.min..target.max,
            steps = (abs(target.min - target.max) / 0.1f).roundToInt() - 1,
            onValueChange = {
                sliderState = it
            },
            onValueChangeFinished = {
                target.setter(sliderState)
            },
            colors = colors
        )

        val unitsSymbol = when (target.units) {
            WidgetModel.CompositeWidget.Units.NONE -> ""
            WidgetModel.CompositeWidget.Units.ON_OFF -> ""
            WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> "%"
            WidgetModel.CompositeWidget.Units.CELSIUS -> "°"
            WidgetModel.CompositeWidget.Units.PPM -> "ppm"
            WidgetModel.CompositeWidget.Units.PPB -> "ppb"
            WidgetModel.CompositeWidget.Units.LX -> "lx"
            WidgetModel.CompositeWidget.Units.W -> "w"
            WidgetModel.CompositeWidget.Units.V -> "v"
            WidgetModel.CompositeWidget.Units.A -> "A"
            WidgetModel.CompositeWidget.Units.KWH -> "KWh"
            WidgetModel.CompositeWidget.Units.MBAR -> "mBar"
        }

        Row {
            val left = (target.min * valueMultiplier).format(0)
            val right = (target.max * valueMultiplier).format(0)
            Text(
                text = "$left$unitsSymbol", modifier = Modifier
                    .weight(1f)
                    .padding(start = p1)
            )
            Text(text = "$right$unitsSymbol", modifier = Modifier.padding(end = p1))
        }
    }

    fun Modifier.appendIf(
        predicate: Boolean,
        modifier: @Composable Modifier.() -> Modifier
    ): Modifier = composed {
        if (predicate) {
            modifier()
        } else {
            this
        }
    }

    fun Modifier.gradientBackground(firstColor: Color, lastColor: Color) = drawWithCache {
        onDrawBehind {
            val radius = sqrt(size.height * size.height + size.width * size.width)
            val gradient = Brush.radialGradient(
                listOf(firstColor, firstColor, lastColor),
                Offset.Zero,
                radius = radius
            )
            drawRect(brush = gradient)
        }
    }

    @Preview(name = "Change target dialog")
    @Composable
    fun DialogPreview() {
        val widget = WidgetModel.CompositeWidget(
            "livingroom_light_main",
            "Основной",
            WidgetModel.CompositeWidget.Category.LIGHT,
            WidgetModel.CompositeWidget.State.Binary(
                WidgetModel.CompositeWidget.Units.ON_OFF,
                false
            ),
            target = WidgetModel.CompositeWidget.Target.Numeric(
                WidgetModel.CompositeWidget.Units.PERCENTS_0_1,
                0.42f,
                {},
                0.08f,
                1f
            ),
            toggle = {}
        )
        ChangeTargetDialog("Гостиная", widget, Color(0xffeed690) to Color(0xffdca324))
    }

    @Preview(name = "Widget list")
    @Composable
    fun ContentPreview() {
        Content(WidgetListPresenter.ViewModel(testFacility(), emptyList(), testWidgets()))
    }

    @Preview(name = "No homes state")
    @Composable
    fun ContentPreviewNoHomes() {
        FacilityHeader(WidgetListPresenter.ViewModel(null, emptyList(), emptyList()))
    }

    @Preview(name = "Expanded homes state")
    @Composable
    fun ContentPreviewExpandedHomes() {
        FacilityHeader(
            WidgetListPresenter.ViewModel(testFacility(), listOf(testFacility()), emptyList()),
            defExpanded = true
        )
    }

    private fun testFacility(): WidgetListPresenter.FacilityViewModel {
        return object : WidgetListPresenter.FacilityViewModel {
            override val name: String
                get() = "Demo stand"
            override val cover: String
                get() = ""

            override fun select() {
                // not intended
            }
        }
    }

    private fun testWidgets(): List<WidgetGroupModel> {
        val nope: () -> Unit = {}
        val groups = listOf(
            WidgetGroupModel(
                "Гостиная", listOf(
                    WidgetModel.CompositeWidget(
                        "livingroom_co2",
                        "CO2",
                        WidgetModel.CompositeWidget.Category.GAUGE,
                        WidgetModel.CompositeWidget.State.NumericFloat(
                            WidgetModel.CompositeWidget.Units.PPM,
                            5000f
                        )
                    ),
                    WidgetModel.CompositeWidget(
                        "livingroom_temp",
                        "Температура",
                        WidgetModel.CompositeWidget.Category.GAUGE,
                        WidgetModel.CompositeWidget.State.NumericFloat(
                            WidgetModel.CompositeWidget.Units.CELSIUS,
                            23.6f,
                            precision = 1
                        )
                    ),
                    WidgetModel.CompositeWidget(
                        "livingroom_hum",
                        "Влажность",
                        WidgetModel.CompositeWidget.Category.GAUGE,
                        WidgetModel.CompositeWidget.State.NumericFloat(
                            WidgetModel.CompositeWidget.Units.PERCENTS_0_1,
                            0.37f
                        )
                    ),
                    WidgetModel.CompositeWidget(
                        "livingroom_pm2.5",
                        "PM2.5",
                        WidgetModel.CompositeWidget.Category.GAUGE,
                        WidgetModel.CompositeWidget.State.NumericInt(
                            WidgetModel.CompositeWidget.Units.PPM,
                            853
                        )
                    ),
                    WidgetModel.CompositeWidget(
                        "livingroom_light_main",
                        "Основной",
                        WidgetModel.CompositeWidget.Category.LIGHT,
                        WidgetModel.CompositeWidget.State.Binary(
                            WidgetModel.CompositeWidget.Units.ON_OFF,
                            false
                        ),
                        toggle = nope
                    ),
                    WidgetModel.CompositeWidget(
                        "livingroom_light_workplace",
                        "Рабочее место",
                        WidgetModel.CompositeWidget.Category.LIGHT,
                        WidgetModel.CompositeWidget.State.Binary(
                            WidgetModel.CompositeWidget.Units.ON_OFF,
                            true
                        ),
                        toggle = nope
                    ),
                    WidgetModel.CompositeWidget(
                        "livingroom_light_entrance",
                        "Коридор",
                        WidgetModel.CompositeWidget.Category.LIGHT,
                        WidgetModel.CompositeWidget.State.Unknown(),
                        toggle = nope
                    ),
                    WidgetModel.BrokenWidget(
                        "livingroom_broken",
                        "Сломан",
                        "TypeMismatchException at input state"
                    ),
                )
            ),
            WidgetGroupModel(
                "Кухня", listOf(
                    WidgetModel.CompositeWidget(
                        "kitchen_onoff_stove",
                        "Печка",
                        WidgetModel.CompositeWidget.Category.SWITCH,
                        WidgetModel.CompositeWidget.State.Binary(
                            WidgetModel.CompositeWidget.Units.ON_OFF,
                            false
                        ),
                        toggle = nope
                    ),
                    WidgetModel.CompositeWidget(
                        "kitchen_co",
                        "Сигнализация CO длинное имя",
                        WidgetModel.CompositeWidget.Category.GAUGE,
                        WidgetModel.CompositeWidget.State.Binary(
                            WidgetModel.CompositeWidget.Units.NONE,
                            true
                        ),
                        toggle = nope
                    ),
                )
            )
        )
        return groups
    }
}

@Composable
private fun WidgetModel.CompositeWidget.Units.toUnitsOutlineText() = when (this) {
    WidgetModel.CompositeWidget.Units.PPM -> "ppm"
    WidgetModel.CompositeWidget.Units.PPB -> "ppb"
    WidgetModel.CompositeWidget.Units.LX -> "lx"
    WidgetModel.CompositeWidget.Units.W -> "w"
    WidgetModel.CompositeWidget.Units.V -> "v"
    WidgetModel.CompositeWidget.Units.A -> "A"
    WidgetModel.CompositeWidget.Units.KWH -> "KWh"
    WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> "%"
    WidgetModel.CompositeWidget.Units.NONE -> ""
    WidgetModel.CompositeWidget.Units.ON_OFF -> ""
    WidgetModel.CompositeWidget.Units.CELSIUS -> ""
    WidgetModel.CompositeWidget.Units.MBAR -> "mBar"
}

@Composable
private fun WidgetModel.CompositeWidget.Units.toUnitsInlineText() = when (this) {
    WidgetModel.CompositeWidget.Units.PPM -> ""
    WidgetModel.CompositeWidget.Units.PPB -> ""
    WidgetModel.CompositeWidget.Units.LX -> ""
    WidgetModel.CompositeWidget.Units.W -> ""
    WidgetModel.CompositeWidget.Units.V -> ""
    WidgetModel.CompositeWidget.Units.A -> ""
    WidgetModel.CompositeWidget.Units.KWH -> ""
    WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> ""
    WidgetModel.CompositeWidget.Units.NONE -> ""
    WidgetModel.CompositeWidget.Units.ON_OFF -> ""
    WidgetModel.CompositeWidget.Units.CELSIUS -> "°"
    WidgetModel.CompositeWidget.Units.MBAR -> ""
}

@Composable
private fun WidgetModel.CompositeWidget.Units.toUnitsMultiplier() = when (this) {
    WidgetModel.CompositeWidget.Units.PERCENTS_0_1 -> 100
    else -> 1
}