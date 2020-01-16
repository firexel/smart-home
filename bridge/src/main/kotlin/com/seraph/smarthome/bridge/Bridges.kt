package com.seraph.smarthome.bridge

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.Topic
import com.seraph.smarthome.util.Log
import kotlin.math.abs
import kotlin.math.round

class Bridges(
        private val driversManager: DriversManager,
        private val externalBroker: Broker,
        private val rootTopic: Topic,
        private val log: Log) {

    fun addLightBridge(topic: Topic, device: Device.Id) {
        driversManager.addDriver(device, DimmerDriver(Topic(rootTopic.segments + topic.segments)))
    }

    inner class DimmerDriver(private val deviceTopic: Topic) : DeviceDriver {
        override fun bind(visitor: DeviceDriver.Visitor) {
            val brIn = visitor.declareInput("brightness", Types.FLOAT, Endpoint.Retention.NOT_RETAINED)
            val brOut = visitor.declareOutput("brightness_out", Types.FLOAT, Endpoint.Retention.NOT_RETAINED)

            val topicOnoff = deviceTopic.subtopic("onoff")
            val topicBrightness = deviceTopic.subtopic("brightness")

            externalBroker.subscribe(topicOnoff) { t, d ->
                try {
                    when (readStringInt(d)) {
                        0 -> brOut.set(0f)
                        1 -> brOut.set(1f)
                        else -> log.w("Unknown value for onoff")
                    }
                } catch (e: Exception) {
                    log.w("Malformed input ${d.pretty()} caused $e")
                }
            }
            externalBroker.subscribe(topicBrightness) { t, d ->
                try {
                    val brightness = readStringInt(d)
                    when (brightness) {
                        in 0..100 -> brOut.set(brightness / 100f)
                        else -> log.w("Unknown value for brightness: $brightness")
                    }
                } catch (e: Exception) {
                    log.w("Malformed input ${d.pretty()} caused $e")
                }
            }
            brIn.observe {
                if (it < 0 || it > 1) {
                    log.w("Bad float value")
                } else {
                    if (abs(it) < 0.01f) {
                        setToExternal(topicOnoff, 0)
                    } else {
                        setToExternal(topicOnoff, 1)
                    }
                    setToExternal(topicBrightness, round(it * 100).toInt())
                }
            }
        }

        private fun setToExternal(topic: Topic, i: Int) {
            externalBroker.publish(topic.subtopic("set"), i.toString().toByteArray())
        }

        private fun readStringInt(d: ByteArray) = d.toString(Charsets.UTF_8).toInt(10)

        private fun ByteArray.pretty() = joinToString(" ") { it.toString(16).padStart(2, '0') }
    }
}
