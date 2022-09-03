package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Converters
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint.DataKind
import com.seraph.smarthome.domain.Units
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.wirenboard.WirenboardDeviceDriver.Control.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.util.List.copyOf

class WirenboardBridge(
    private val wbBroker: Broker,
    private val drivers: DriversManager,
    private val log: ConsoleLog,
    private val filters: List<DeviceInfoFilter>
) {

    private val knownDeviceIds = mutableSetOf<String>()

    suspend fun serve(scope: CoroutineScope) {
        wbBroker.subscribeAsFlow(WirenboardTopics.deviceName())
            .map { it.topic.segments[2] }
            .collect { devId ->
                if (!knownDeviceIds.contains(devId)) {
                    log.v("New device $devId received")
                    knownDeviceIds.add(devId)
                    scope.launch {
                        createDevice(devId)
                    }
                } else {
                    log.w("Skipping known device $devId")
                }
            }
    }

    private suspend fun createDevice(deviceId: String) {
        val topic = WirenboardTopics.controlType(deviceId)
        val typedControls = wbBroker.subscribeAsFlow(topic)
            .timeout(15000L)
            .map { p ->
                TypedControl(p.topic.segments[4], Converters.STRING.fromBytes(p.data))
                    .apply { log.v("Control '$id' found for '$deviceId' with type '$type'") }
            }
            .toList()

        log.i("Finished waiting for controls for '$deviceId' on topic $topic. " +
                "${typedControls.size} found total")

        val deviceControls = typedControls.map { control ->
            val readonly =
                wbBroker.subscribeAsFlow(WirenboardTopics.controlReadonly(deviceId, control.id))
                    .timeout(300L)
                    .map { Converters.INT.fromBytes(it.data) == 1 }
                    .firstOrDefault(false)

            createControl(deviceId, control, readonly)
        }

        val info = runFilters(DeviceInfo(deviceId, deviceControls.filterNotNull()))
        if (info != null) {
            val outerId = info.outerId.safe
            drivers.addDriver(
                Device.Id(outerId),
                WirenboardDeviceDriver(
                    wbBroker,
                    deviceId, // preserve original wb id to subscribe correctly
                    info.controls,
                    log.copy("Driver").copy(outerId)
                )
            )
        } else {
            log.i("Device '$deviceId' was filtered out")
        }
    }

    private fun runFilters(i: DeviceInfo): DeviceInfo? {
        var info: DeviceInfo = i
        filters.forEach { filter ->
            val n = filter.filter(info)
            if (n == null) {
                return null
            } else {
                info = n
            }
        }
        return info
    }

    private suspend fun createControl(
        deviceId: String, control: TypedControl, readonly: Boolean
    ) = when (control.type) {
        "switch" ->
            BooleanControl(control.id, readonly)
        "pushbutton" ->
            ActionControl(control.id)
        "temperature" ->
            FloatControl(control.id, readonly, 1f, Units.CELSIUS)
        "rel_humidity" ->
            FloatControl(control.id, readonly, 0.01f, Units.PERCENTS_0_1)
        "sound_level" ->
            FloatControl(control.id, readonly, 1f, Units.NO)
        "lux" ->
            FloatControl(control.id, readonly, 1f, Units.LX)
        "voltage" ->
            FloatControl(control.id, readonly, 1f, Units.V)
        "power" ->
            FloatControl(control.id, readonly, 1f, Units.W)
        "power_consumption" ->
            FloatControl(control.id, readonly, 1f, Units.KWH, DataKind.CUMULATIVE)
        "concentration" ->
            FloatControl(control.id, readonly, 1f, Units.PPM)
        "atmospheric_pressure" ->
            FloatControl(control.id, readonly, 1f, Units.MBAR)
        "text" ->
            StringControl(control.id, readonly)
        "value" ->
            FloatControl(control.id, readonly, 1f, Units.NO)
        "range" -> {
            val max = wbBroker.subscribeAsFlow(WirenboardTopics.controlRange(deviceId, control.id))
                .timeout(300L)
                .map { Converters.INT.fromBytes(it.data) }
                .firstOrDefault(1)

            IntControl(control.id, readonly, max, Units.NO)
        }
        else -> {
            log.w("Unknown type ${control.type} of $deviceId/${control.id}")
            null
        }
    }

    internal data class TypedControl(val id: String, val type: String)

    data class DeviceInfo(
        val outerId: String,
        val controls: List<WirenboardDeviceDriver.Control>
    )

    interface DeviceInfoFilter {
        fun filter(info: DeviceInfo): DeviceInfo?
    }

    companion object {
        fun filterOutByDeviceId(ids: List<String>): DeviceInfoFilter = object : DeviceInfoFilter {
            override fun filter(info: DeviceInfo): DeviceInfo? {
                return if (ids.contains(info.outerId)) {
                    null
                } else {
                    info
                }
            }
        }

        fun filterOutEndpointsById(ids: List<String>): DeviceInfoFilter =
            object : DeviceInfoFilter {
                override fun filter(info: DeviceInfo): DeviceInfo? {
                    return info.copy(controls = info.controls.filter { !ids.contains(it.id) })
                }
            }

        fun changeDeviceId(id: String, name: String): DeviceInfoFilter = object : DeviceInfoFilter {
            override fun filter(info: DeviceInfo): DeviceInfo? {
                return if (info.outerId == id) {
                    info.copy(outerId = name)
                } else {
                    info
                }
            }
        }

        fun changeEndpointId(devId: String, endId: String, name: String): DeviceInfoFilter =
            object : DeviceInfoFilter {
                override fun filter(info: DeviceInfo): DeviceInfo? {
                    return if (info.outerId == devId) {
                        info.copy(controls = copyOf(info.controls).apply {
                            firstOrNull { it.id == endId }?.rename(name)
                        })
                    } else {
                        info
                    }
                }
            }
    }
}