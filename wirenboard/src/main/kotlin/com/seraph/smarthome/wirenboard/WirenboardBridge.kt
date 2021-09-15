package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Converters
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint.DataKind
import com.seraph.smarthome.domain.Units
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.wirenboard.WirenboardDeviceDriver.Control.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WirenboardBridge(
        private val wbBroker: LocalBroker,
        private val drivers: DriversManager,
        private val log: ConsoleLog) {

    suspend fun serve(scope: CoroutineScope) {
        wbBroker.subscribeAsFlow(WirenboardTopics.deviceName())
                .map { it.topic.segments[2] }
                .onEach { log.v("New device $it found") }
                .collect { devId ->
                    scope.launch {
                        createDevice(devId)
                    }
                }
    }

    private suspend fun createDevice(deviceId: String) {
        val typedControls = wbBroker.subscribeAsFlow(WirenboardTopics.controlType(deviceId))
                .timeout(500L)
                .map { p ->
                    TypedControl(p.topic.segments[4], Converters.STRING.fromBytes(p.data))
                            .apply { log.v("Control $id found for $deviceId with type $type") }
                }
                .onCompletion { log.v("Finished waiting for controls for $deviceId") }
                .toList()

        val deviceControls = typedControls.map { control ->
            val readonly = wbBroker.subscribeAsFlow(WirenboardTopics.controlReadonly(deviceId, control.id))
                    .timeout(300L)
                    .map { Converters.INT.fromBytes(it.data) == 1 }
                    .firstOrDefault(false)

            createControl(deviceId, control, readonly)
        }

        drivers.addDriver(
                Device.Id(deviceId),
                WirenboardDeviceDriver(wbBroker,
                        deviceId,
                        deviceControls.filterNotNull(),
                        log.copy("Driver").copy(deviceId)
                )
        )
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
            log.w("Unknown type ${control.type}")
            null
        }
    }

    internal data class TypedControl(val id: String, val type: String)
}