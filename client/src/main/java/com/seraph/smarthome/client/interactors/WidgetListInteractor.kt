package com.seraph.smarthome.client.interactors

import com.seraph.smarthome.client.model.WidgetGroupModel
import com.seraph.smarthome.client.model.WidgetModel
import com.seraph.smarthome.client.repositories.NetworkRepository
import com.seraph.smarthome.domain.*
import com.seraph.smarthome.util.NetworkMonitor
import com.seraph.smarthome.util.NetworkSnapshot
import ru.mail.march.interactor.Interactor

class WidgetListInteractor(private val networkRepo: NetworkRepository) : Interactor() {
    val widgets = stateChannel<List<WidgetGroupModel>>()

    private lateinit var subscription: NetworkMonitor.Subscription

    override fun create() {
        super.create()
        subscription = networkRepo.monitor.subscribe {
            val snapshot = it.snapshot()
            val meta = snapshot.metainfo
            val groups = meta.widgetGroups.map { group ->
                val widgets = group.widgets.map { widget ->
                    when (widget) {
                        is Widget.BinaryLight -> mapBinaryLight(snapshot, widget)
                        else -> null
                    }
                }
                WidgetGroupModel(group.name, widgets.filterNotNull())
            }
            widgets.postValue(groups)
        }
    }

    private fun mapBinaryLight(snapshot: NetworkSnapshot, widget: Widget.BinaryLight): WidgetModel {
        try {
            val state = widget.state
            val endpoint = snapshot.devices[state.device]?.endpoints?.get(state.endpoint)
            val boolState = if (endpoint != null && endpoint.isSet && endpoint.value != null) {
                endpoint.endpoint.type.accept(CastToBooleanVisitor(endpoint.value!!))
            } else {
                null
            }
            val toggler: () -> Unit = {
                val monitor = networkRepo.monitor
                val network = networkRepo.network
                val endpointOnOff = monitor.snapshot(widget.onOff.device, widget.onOff.endpoint)
                endpointOnOff?.endpoint?.accept(CastFromBooleanVisitor(
                        network,
                        endpointOnOff.device.id,
                        boolState?.equals(false) // nullable inversion
                ))
            }
            return WidgetModel.BinaryLight(widget.name, boolState, toggler)
        } catch (ex:NetworkTypeMismatchException) {
            return WidgetModel.BrokenWidget(widget.name, "${ex.message}")
        }
    }

    override fun destroy() {
        subscription.unsubscribe()
        super.destroy()
    }
}

class CastToBooleanVisitor(private val value: Any) : Endpoint.Type.Visitor<Boolean?> {
    override fun onBoolean(type: Endpoint.Type<Boolean>): Boolean? = value as Boolean
    override fun onFloat(type: Endpoint.Type<Float>): Boolean? = value as Float > 0.00001f
    override fun onInt(type: Endpoint.Type<Int>): Boolean? = value as Int != 0

    override fun onDeviceState(type: Endpoint.Type<DeviceState>): Boolean? {
        throw NetworkTypeMismatchException("Incompatible type")
    }

    override fun onAction(type: Endpoint.Type<Int>): Boolean? {
        throw NetworkTypeMismatchException("Incompatible type")
    }
}

class CastFromBooleanVisitor(
        private val network: Network,
        private val devId: Device.Id,
        private val value: Boolean?,
) : Endpoint.Visitor<Unit> {

    override fun onInt(endpoint: Endpoint<Int>) {
        when (value) {
            null -> throw IllegalSnapshotStateException("Unknown current value")
            true -> network.publish(devId, endpoint, 1)
            false -> network.publish(devId, endpoint, 0)
        }
    }

    override fun onAction(endpoint: Endpoint<Int>) {
        network.publish(devId, endpoint, Types.newActionId())
    }

    override fun onBoolean(endpoint: Endpoint<Boolean>) {
        when (value) {
            null -> throw IllegalSnapshotStateException("Unknown current value")
            else -> network.publish(devId, endpoint, value)
        }
    }

    override fun onFloat(endpoint: Endpoint<Float>) {
        when (value) {
            null -> throw IllegalSnapshotStateException("Unknown current value")
            true -> network.publish(devId, endpoint, 1f)
            false -> network.publish(devId, endpoint, 0f)
        }
    }

    override fun onDeviceState(endpoint: Endpoint<DeviceState>) {
        throw NetworkTypeMismatchException("Unsupported type")
    }
}

class IllegalSnapshotStateException(msg: String) : RuntimeException(msg)
class NetworkTypeMismatchException(msg: String) : RuntimeException(msg)
