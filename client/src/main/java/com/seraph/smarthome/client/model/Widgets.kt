package com.seraph.smarthome.client.model

data class WidgetGroupModel(
        val name: String,
        val widgets: List<WidgetModel>,
)

sealed class WidgetModel {
    abstract val name: String

    data class BinaryLight(
            override val name: String,
            val isOn: Boolean?,
            val toggle: () -> Unit,
    ) : WidgetModel()

    data class BrokenWidget(
            override val name: String,
            val message: String,
    ) : WidgetModel()

    data class Gauge(
            override val name:String,
            val value:Float?,
            val units:Units
    ) : WidgetModel()

    enum class Units {
        CO2_PPM, TEMP_CELSIUS, HUMIDITY_PERCENT, PM25_PPM
    }
}