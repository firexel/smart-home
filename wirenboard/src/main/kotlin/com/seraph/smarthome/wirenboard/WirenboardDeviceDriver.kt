package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.domain.*
import com.seraph.smarthome.domain.Endpoint.DataKind
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import java.util.*

class WirenboardDeviceDriver(
    private val wbBroker: Broker,
    private val devId: String,
    private val controls: List<Control>,
    private val log: Log
) : DeviceDriver {

    override fun bind(visitor: DeviceDriver.Visitor) {
        val subscriptions = controls.map { control ->
            when (control) {
                is Control.BooleanControl -> {
                    visitor.declareInputOutput(control,
                        Types.BOOLEAN,
                        Units.ON_OFF,
                        DataKind.CURRENT,
                        { Converters.INT.fromBytes(it) == 1 },
                        { Converters.INT.toBytes(if (it) 1 else 0) }
                    )
                }
                is Control.FloatControl -> {
                    visitor.declareInputOutput(control, Types.FLOAT, control.units, control.kind,
                        { Converters.FLOAT.fromBytes(it) * control.multiplier },
                        { Converters.FLOAT.toBytes(it / control.multiplier) }
                    )
                }
                is Control.ActionControl -> {
                    visitor.declareInput(control, Types.ACTION, Units.NO, DataKind.EVENT) {
                        Converters.INT.toBytes(1)
                    };
                    {}
                }
                is Control.IntControl -> {
                    visitor.declareInputOutput(control,
                        Types.INTEGER,
                        control.units,
                        DataKind.CURRENT,
                        { Converters.INT.fromBytes(it) },
                        { Converters.INT.toBytes(it) }
                    )
                }
                is Control.StringControl -> {
                    log.i("String control ${control.id} not supported");
                    {}
                }
            }
        }
        visitor.onOperational {
            subscriptions.forEach { it() }
        }
    }

    private fun <T> DeviceDriver.Visitor.declareInputOutput(
        control: Control, type: Endpoint.Type<T>, units: Units, kind: DataKind,
        reader: (ByteArray) -> T, writer: (T) -> ByteArray
    ): () -> Any {

        if (!control.readonly) {
            declareInput(control, type, units, kind, writer)
        }
        return declareOutput(control, type, units, kind, reader)
    }

    private fun <T> DeviceDriver.Visitor.declareInput(
        control: Control, type: Endpoint.Type<T>, units: Units, kind: DataKind,
        writer: (T) -> ByteArray
    ) {

        declareInput(control.outerId.safe + "_in", type)
            .setUnits(units)
            .setDataKind(kind)
            .observe {
                val data = writer(it)
                wbBroker.publish(control.writeTopic, data)
            }
    }

    private fun <T> DeviceDriver.Visitor.declareOutput(
        control: Control, type: Endpoint.Type<T>, units: Units, kind: DataKind,
        reader: (ByteArray) -> T
    ): () -> Any {

        val out = declareOutput(control.outerId.safe + "_out", type)
            .setUnits(units)
            .setDataKind(kind);

        return {
            wbBroker.subscribe(control.readTopic) { _, d ->
                try {
                    out.set(reader(d))
                } catch (ex:Serializer.TypeMismatchException) {
                    log.w("Type mismatch because of '${ex.message}'. Update skipped.")
                }
            }
        }
    }

    sealed class Control(
        open val id: String,
        open val readonly: Boolean
    ) {
        class BooleanControl(id: String, readonly: Boolean) : Control(id, readonly)
        class ActionControl(id: String) : Control(id, false)

        class FloatControl(
            id: String, readonly: Boolean, val multiplier: Float, val units: Units,
            val kind: DataKind = DataKind.CURRENT
        ) : Control(id, readonly)

        class IntControl(
            id: String, readonly: Boolean, val max: Int,
            val units: Units
        ) : Control(id, readonly)

        class StringControl(id: String, readonly: Boolean) : Control(id, readonly)

        private var _outerId: String? = null
        val outerId: String
            get() = if (_outerId == null) id else _outerId!!

        fun rename(id: String) {
            _outerId = id
        }
    }

    private val Control.readTopic: Topic
        get() = WirenboardTopics.control(devId, this.id)

    private val Control.writeTopic: Topic
        get() = WirenboardTopics.controlWrite(devId, this.id)
}

val String.safe: String
    get() {
        return replace(' ', '_')
            .replace("(", "")
            .replace(")", "")
            .replace('-', '_')
            .lowercase(Locale.ENGLISH)
    }
