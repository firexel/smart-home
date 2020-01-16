package com.seraph.smarthome.logic.scenes

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.domain.Endpoint
import com.seraph.smarthome.domain.Types
import com.seraph.smarthome.util.ConsoleLog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val log = ConsoleLog("DBG")

class Scene(
        channels: List<Channel>,
        private val reporter: ReportInterface
) {
    val channels: Map<String, Channel> = channels.map { Pair(it.token, it) }.toMap()

    private var brigtness: Float = 0f

    data class Channel(
            val token: String,
            val mapper: Mapper = FactorMapper(),
            var state: Float = 0f
    ) {
        interface Mapper {
            fun fromOverallBrightness(brightness: Float): Float
            fun toOverallBrightness(brightness: Float): Float
        }

        override fun toString(): String {
            return "$token: $state"
        }
    }

    interface ReportInterface {
        fun reportOverallBrightnessChanged(brightness: Float)
        fun reportChannelsBrightnessChanged(channels: List<Channel>)
    }

    fun toggle() {
        if (brigtness > 0.001) {
            off()
        } else {
            on()
        }
    }

    fun on() {
        set(1f)
    }

    fun off() {
        set(0f)
    }

    fun set(value: Float) {
        brigtness = value
        remixChannels()
        reporter.reportOverallBrightnessChanged(value)
    }

    fun reverseMix(updChannels: List<Channel>) {
        updChannels.forEach {
            channels[it.token]?.state = it.state
        }
        brigtness = channels.values.map { it.mapper.toOverallBrightness(it.state) }.average().toFloat()
        reporter.reportOverallBrightnessChanged(brigtness)
    }

    private fun remixChannels() {
        val br = brigtness
        channels.values.forEach {
            val newState = it.mapper.fromOverallBrightness(br)
            it.state = newState
        }
        reporter.reportChannelsBrightnessChanged(channels.values.toList())
    }
}

class FactorMapper(private val factor: Float = 1f) : Scene.Channel.Mapper {
    override fun fromOverallBrightness(brightness: Float): Float = brightness * factor
    override fun toOverallBrightness(brightness: Float): Float = brightness / factor
}

class RegionMapper(private val from: Float = 0f, private val to: Float = 1f) : Scene.Channel.Mapper {
    override fun fromOverallBrightness(brightness: Float): Float {
        return when {
            brightness < from -> 0f
            brightness > to -> 1f
            else -> (brightness - from) / abs(to - from)
        }
    }

    override fun toOverallBrightness(brightness: Float): Float {
        return when {
            brightness <= 0 -> from
            brightness >= 1 -> to
            else -> from + brightness * abs(to - from)
        }
    }
}

class SceneDriver(
        private val scene: Scene) : DeviceDriver {

    private var brightnessOut: DeviceDriver.Output<Float>? = null

    override fun bind(visitor: DeviceDriver.Visitor) {
        visitor.declareOutputPolicy(DeviceDriver.OutputPolicy.ALWAYS_ALLOW)
        val toggle = visitor.declareInput("toggle", Types.VOID, Endpoint.Retention.NOT_RETAINED)
        val on = visitor.declareInput("on", Types.VOID, Endpoint.Retention.NOT_RETAINED)
        val off = visitor.declareInput("off", Types.VOID, Endpoint.Retention.NOT_RETAINED)
        val set = visitor.declareInput("set", Types.BOOLEAN, Endpoint.Retention.NOT_RETAINED)
        val brightnessIn = visitor.declareInput("brightness", Types.FLOAT, Endpoint.Retention.NOT_RETAINED)
        brightnessOut = visitor.declareOutput("brightness_out", Types.FLOAT, Endpoint.Retention.RETAINED)

        toggle.observe { scene.toggle() }
        on.observe { scene.on() }
        off.observe { scene.off() }
        set.observe { scene.set(if (it) 1f else 0f) }
        brightnessIn.observe { scene.set(it) }
    }

    fun reportBrightness(brightness: Float) {
        brightnessOut?.set(brightness)
    }
}

class ChannelMixer(private val mixer: (Float, Float) -> Float) {
    private val channels = mutableMapOf<String, Float>()

    fun updateChannel(owner: String, brightness: Float): Float {
        channels[owner] = brightness
        return channels.values.fold(0f, mixer)
    }

    object Mixers {
        fun sum(a: Float, b: Float): Float {
            return max(0f, min(1f, a + b))
        }
    }

    override fun toString(): String {
        return "$channels"
    }
}

class ScenesManager {

    private val binders = mutableListOf<SceneBinder>()
    private val mixers = mutableMapOf<String, ChannelMixer>()
    private lateinit var rootDriver: RootDeviceDriver

    fun registerScene(name: String, channels: List<Scene.Channel>) {
        val binder = SceneBinder(name)
        binder.scene = Scene(channels, binder)
        binder.driver = SceneDriver(binder.scene)
        binders.add(binder)
    }

    fun bind(id: String, driverManager: DriversManager) {
        val allChannels = binders.flatMap { it.scene.channels.keys }.toSet()
        allChannels.forEach { mixers[it] = ChannelMixer(ChannelMixer.Mixers::sum) }
        rootDriver = RootDeviceDriver(allChannels.toList(), binders)
        driverManager.addDriver(Device.Id(id), rootDriver)
    }

    private fun handleChannelsUpdate(channels: List<Scene.Channel>, ownerName: String, owner: Scene) {
        mixOutputChannels(channels, ownerName)
        reverseMixOtherScenes(channels, owner)
    }

    private fun mixOutputChannels(channels: List<Scene.Channel>, ownerName: String) {
        log.i("Mixing channels of $ownerName: $channels")
        channels.forEach {
            val channelMixer = mixers[it.token]!!
            log.i("Mixer state for ${it.token} before upd is $channelMixer")
            val newValue = channelMixer.updateChannel(ownerName, it.state)
            log.i("Mixer state for ${it.token} after upd is  $channelMixer")
            rootDriver.reportBrightness(it.token, newValue)
        }
    }

    private fun reverseMixOtherScenes(channels: List<Scene.Channel>, owner: Scene) {
        binders.filter { it.scene !== owner }.forEach { it.scene.reverseMix(channels) }
    }

    private inner class SceneBinder(val name: String) : Scene.ReportInterface {
        lateinit var driver: SceneDriver
        lateinit var scene: Scene

        override fun reportOverallBrightnessChanged(brightness: Float) {
            driver.reportBrightness(brightness)
        }

        override fun reportChannelsBrightnessChanged(channels: List<Scene.Channel>) {
            handleChannelsUpdate(channels, name, scene)
        }
    }

    private class RootDeviceDriver(
            private val channels: List<String>,
            private val scenes: List<SceneBinder>
    ) : DeviceDriver {

        private val outputs = mutableMapOf<String, DeviceDriver.Output<Float>>()

        override fun bind(visitor: DeviceDriver.Visitor) {
            channels.forEach {
                val output = visitor.declareOutput(it, Types.FLOAT, Endpoint.Retention.RETAINED)
                outputs[it] = output
            }
            scenes.forEach {
                it.driver.bind(visitor.declareInnerDevice(it.name))
            }
        }

        fun reportBrightness(id: String, brightness: Float) {
            outputs[id]?.set(brightness)
        }
    }
}