package com.seraph.smarthome.client.model

data class WidgetGroupModel(
        val name: String,
        val widgets: List<WidgetModel>,
)

sealed class WidgetModel {
    abstract val id: String
    abstract val name: String

    class CompositeWidget(
            override val id: String,
            override val name: String,
            val category: Category,
            val state: State? = null,
            val target: Target? = null,
            val toggle: (() -> Unit)? = null,
    ) : WidgetModel() {

        enum class Category {
            GAUGE, THERMOSTAT, LIGHT, SWITCH
        }

        enum class Units {
            NONE, ON_OFF, PERCENTS_0_1, CELSIUS, PPM, PPB, LX, W, V, KWH, MBAR
        }

        sealed class State {
            abstract val units: Units

            data class Binary(
                    override val units: Units,
                    val state: Boolean,
            ) : State()

            data class NumericFloat(
                    override val units: Units,
                    val state: Float,
                    val precision: Int = 0,
            ) : State()

            data class NumericInt(
                    override val units: Units,
                    val state: Int
            ) : State()

            data class Unknown(override val units: Units = Units.NONE) : State()
        }

        sealed class Target {
            abstract val units: Units

            data class Binary(
                    override val units: Units,
                    val setter: (Boolean) -> Unit,
            ) : Target()

            data class Numeric(
                    override val units: Units,
                    val state: Float,
                    val setter: (Float) -> Unit,
                    val min: Float,
                    val max: Float,
            ) : Target()
        }
    }

    data class BrokenWidget(
            override val id: String,
            override val name: String,
            val message: String,
    ) : WidgetModel()
}