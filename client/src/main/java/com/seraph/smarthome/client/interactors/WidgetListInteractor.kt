package com.seraph.smarthome.client.interactors

import com.seraph.smarthome.client.model.WidgetGroupModel
import com.seraph.smarthome.client.model.WidgetModel
import com.seraph.smarthome.client.repositories.NetworkRepository
import com.seraph.smarthome.domain.EndpointAddr
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.domain.Units
import com.seraph.smarthome.domain.Widget
import com.seraph.smarthome.util.EndpointSnapshot
import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NetworkMonitor
import com.seraph.smarthome.util.NetworkSnapshot
import ru.mail.march.interactor.Interactor

class WidgetListInteractor(
        private val networkRepo: NetworkRepository,
        private val log: Log,
) : Interactor() {

    val widgets = stateChannel<List<WidgetGroupModel>>()

    private lateinit var subscription: NetworkMonitor.Subscription

    override fun create() {
        super.create()
        subscription = networkRepo.monitor.subscribe {
            val snapshot = it.snapshot()
            val meta = snapshot.metainfo
            val groups = meta.widgetGroups.map { group ->
                val widgets = group.widgets.map { widget ->
                    mapCompositeWidget(snapshot, widget)
                }
                WidgetGroupModel(group.name, widgets)
            }
            widgets.postValue(groups)
        }
    }

    override fun destroy() {
        subscription.unsubscribe()
        super.destroy()
    }

    private fun mapCompositeWidget(snapshot: NetworkSnapshot, widget: Widget): WidgetModel {
        return try {
            WidgetModel.CompositeWidget(
                    widget.name,
                    widget.category.mapCategory(),
                    widget.state?.mapState(snapshot),
                    widget.target?.mapTarget(snapshot),
                    widget.toggle?.mapToggler()
            )
        } catch (ex: NetworkTypeMismatchException) {
            WidgetModel.BrokenWidget(widget.name, ex.message ?: "")
        }
    }

    private fun Widget.TargetTrait.mapTarget(snapshot: NetworkSnapshot)
            : WidgetModel.CompositeWidget.Target {

        when (this) {
            is Widget.TargetTrait.Binary -> {
                val units = snapshot.get(this.endpoint, checkInit = false).endpoint.units.mapToUnits()
                val setter = { value: Boolean ->
                    safeCall {
                        val newSnapshot = networkRepo.monitor.snapshot().getBoolean(this.endpoint, checkIsSet = false)
                        networkRepo.network.publish(newSnapshot.device.id, newSnapshot.endpoint, value)
                    }()
                }
                return WidgetModel.CompositeWidget.Target.Binary(units, setter)
            }
            is Widget.TargetTrait.Numeric -> {
                val units = snapshot.get(this.endpoint, checkInit = false).endpoint.units.mapToUnits()
                val setter = { value: Float ->
                    safeCall {
                        val newSnapshot = networkRepo.monitor.snapshot().getFloat(this.endpoint, checkIsSet = false)
                        networkRepo.network.publish(newSnapshot.device.id, newSnapshot.endpoint, value)
                    }()
                }
                return WidgetModel.CompositeWidget.Target.Numeric(units, setter, this.min, this.max)
            }
        }
    }

    private fun Widget.ToggleTrait.mapToggler()
            : () -> Unit {

        return when (this) {
            is Widget.ToggleTrait.Action -> safeCall {
                val snapshot = networkRepo.monitor.snapshot().getAction(this.endpoint)
                networkRepo.network.publish(snapshot.device.id, snapshot.endpoint, Types.newActionId())
            }
            is Widget.ToggleTrait.Invert -> safeCall {
                val snapshot = networkRepo.monitor.snapshot()
                val src = snapshot.getBoolean(this.endpointRead)
                val dst = snapshot.getBoolean(this.endpointWrite, checkIsSet = false)
                networkRepo.network.publish(dst.device.id, dst.endpoint, !(src.value!!))
            }
            is Widget.ToggleTrait.OnOffActions -> safeCall {
                val snapshot = networkRepo.monitor.snapshot()
                val src = snapshot.getBoolean(this.endpointRead)
                val dst = snapshot.getAction(if (src.value!!) this.endpointOff else this.endpointOn)
                networkRepo.network.publish(dst.device.id, dst.endpoint, Types.newActionId())
            }
        }
    }

    private fun Widget.StateTrait.mapState(snapshot: NetworkSnapshot)
            : WidgetModel.CompositeWidget.State {
        try {
            when (this) {
                is Widget.StateTrait.Binary -> {
                    val source = snapshot.getBoolean(this.endpoint)
                    return WidgetModel.CompositeWidget.State.Binary(
                            units = source.endpoint.units.mapToUnits(),
                            state = source.value!!
                    )
                }
                is Widget.StateTrait.Numeric -> {
                    val source = snapshot.getFloat(this.endpoint)
                    return WidgetModel.CompositeWidget.State.Numeric(
                            units = source.endpoint.units.mapToUnits(),
                            state = source.value!!,
                            precision = this.precision
                    )
                }
            }
        } catch (ex: UnknownNetworkStateException) {
            return WidgetModel.CompositeWidget.State.Unknown()
        }
    }

    private fun Units.mapToUnits(): WidgetModel.CompositeWidget.Units {
        return when (this) {
            Units.NO -> WidgetModel.CompositeWidget.Units.NONE
            Units.CELSIUS -> WidgetModel.CompositeWidget.Units.CELSIUS
            Units.PPM -> WidgetModel.CompositeWidget.Units.PPM
            Units.PERCENTS_0_1 -> WidgetModel.CompositeWidget.Units.PERCENTS_0_1
            Units.ON_OFF -> WidgetModel.CompositeWidget.Units.ON_OFF
        }
    }

    private fun Widget.Category.mapCategory(): WidgetModel.CompositeWidget.Category {
        return when (this) {
            Widget.Category.GAUGE -> WidgetModel.CompositeWidget.Category.GAUGE
            Widget.Category.THERMOSTAT -> WidgetModel.CompositeWidget.Category.THERMOSTAT
            Widget.Category.LIGHT -> WidgetModel.CompositeWidget.Category.LIGHT
            Widget.Category.SWITCH -> WidgetModel.CompositeWidget.Category.SWITCH
        }
    }

    private fun safeCall(block: () -> Unit): () -> Unit {
        return {
            try {
                block()
            } catch (ex: UnknownNetworkStateException) {
                log.w(ex.message!!)
            } catch (ex: NetworkTypeMismatchException) {
                log.w(ex.message!!)
            }
        }
    }

    private fun NetworkSnapshot.get(endpoint: EndpointAddr, checkInit: Boolean = true): EndpointSnapshot<*> {
        val e = devices[endpoint.device]?.endpoints?.get(endpoint.endpoint)
                ?: throw UnknownNetworkStateException("$endpoint not found")

        return if (e.isSet || !checkInit) e else throw UnknownNetworkStateException("$endpoint is not set")
    }

    private fun NetworkSnapshot.getBoolean(
            endpoint: EndpointAddr,
            checkIsSet: Boolean = true,
    ): EndpointSnapshot<Boolean> {

        val snapshot = get(endpoint, checkIsSet)
        if (snapshot.endpoint.type != Types.BOOLEAN) {
            throw NetworkTypeMismatchException("Expecting Boolean type of ${endpoint}," +
                    " got ${snapshot.endpoint.type} instead")
        } else {
            @Suppress("UNCHECKED_CAST")
            return snapshot as EndpointSnapshot<Boolean>
        }
    }

    private fun NetworkSnapshot.getFloat(
            endpoint: EndpointAddr,
            checkIsSet: Boolean = true,
    ): EndpointSnapshot<Float> {

        val snapshot = get(endpoint, checkIsSet)
        if (snapshot.endpoint.type != Types.FLOAT) {
            throw NetworkTypeMismatchException("Expecting Float type of ${endpoint}," +
                    " got ${snapshot.endpoint.type} instead")
        } else {
            @Suppress("UNCHECKED_CAST")
            return snapshot as EndpointSnapshot<Float>
        }
    }

    private fun NetworkSnapshot.getAction(endpoint: EndpointAddr): EndpointSnapshot<Int> {
        val snapshot = get(endpoint, checkInit = false)
        if (snapshot.endpoint.type != Types.ACTION) {
            throw NetworkTypeMismatchException("Expecting Action type of ${endpoint}," +
                    " got ${snapshot.endpoint.type} instead")
        } else {
            @Suppress("UNCHECKED_CAST")
            return snapshot as EndpointSnapshot<Int>
        }
    }

    class NetworkTypeMismatchException(msg: String) : RuntimeException(msg)
    class UnknownNetworkStateException(msg: String) : RuntimeException(msg)
}