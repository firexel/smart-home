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
            NONE, ON_OFF, PERCENTS_0_1, CELSIUS, PPM, PPB, LX, W, V, A, KWH, MBAR
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
            ) : Target() {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Binary

                    if (units != other.units) return false

                    return true
                }

                override fun hashCode(): Int {
                    return units.hashCode()
                }
            }

            data class Numeric(
                    override val units: Units,
                    val state: Float,
                    val setter: (Float) -> Unit,
                    val min: Float,
                    val max: Float,
            ) : Target() {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false

                    other as Numeric

                    if (units != other.units) return false
                    if (state != other.state) return false
                    if (min != other.min) return false
                    if (max != other.max) return false

                    return true
                }

                override fun hashCode(): Int {
                    var result = units.hashCode()
                    result = 31 * result + state.hashCode()
                    result = 31 * result + min.hashCode()
                    result = 31 * result + max.hashCode()
                    return result
                }
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CompositeWidget

            if (id != other.id) return false
            if (name != other.name) return false
            if (category != other.category) return false
            if (state != other.state) return false
            if (target != other.target) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + category.hashCode()
            result = 31 * result + (state?.hashCode() ?: 0)
            result = 31 * result + (target?.hashCode() ?: 0)
            return result
        }
    }

    data class BrokenWidget(
            override val id: String,
            override val name: String,
            val message: String,
    ) : WidgetModel()
}