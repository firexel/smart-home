import com.seraph.connector.tree.Node
import script.definition.Clock
import script.definition.Synthetic
import script.definition.TreeBuilder
import kotlin.math.abs

enum class PowerType {
    CITY, GENERATOR, UPS
}

config {
    configureTechRoomAutoLight()
    configureFacadeLight()
    configureFilterAirCompressor()
    configureLivingroomCandleLight()
    val powerType = configurePowerMonitoring()
    configureEconomizer(powerType)
    configureMonitor()
}

/**
 * Auto enable technical room light on enter and disable after 5min delay
 */
fun TreeBuilder.configureTechRoomAutoLight() {
    /* Shortcuts */
    val techRoomLightKey = output("relay6", "key6_on", Boolean::class)
    val techRoomLight = input("relay6", "relay5_on_set", Boolean::class)
    val techRoomDoorSensor = output("door_sensor_tech_room", "open", Boolean::class)

    val lightTimeoutS = 5 * 60
    val enterTimer = timer(tickInterval = 1000L, stopAfter = (lightTimeoutS + 1) * 1000L)
    techRoomDoorSensor.onChanged { value ->
        if (value) {
            enterTimer.start()
        }
    }
    techRoomLightKey.onChanged { value ->
        if (!value) {
            enterTimer.stop()
        }
    }
    techRoomLight receiveFrom map {
        val keyOn = snapshot(techRoomLightKey)
        val timerActive = snapshot(enterTimer.active)
        val timerMillisPassed = snapshot(enterTimer.millisPassed)
        keyOn || (timerActive && timerMillisPassed < lightTimeoutS * 1000)
    }
}

/**
 *  Auto enable facade light on sunset and disable on the sunrise
 */
fun TreeBuilder.configureFacadeLight() {
    val facadeLightRelay = input("wb:relay5", "relay1_on_set", Boolean::class)
    val facadeLightSwitch = output("wb:relay5", "relay1_on", Boolean::class)
    val sunrise = output("geo:current_day", "sun_is_risen", Boolean::class)

    sunrise.onChanged { isRisen ->
        facadeLightRelay receiveFrom constant(isRisen)
    }
    facadeLightSwitch.onChanged { isOn ->
        facadeLightRelay receiveFrom constant(isOn)
    }
}

/**
 * Enable air compressor only
 * after 30s delay and only while sensor is on
 * only in daytime
 */
fun TreeBuilder.configureFilterAirCompressor() {
    val compressorRelay = input("wb:r3", "r2_on_set", Boolean::class)
    val flowSensor = output("wb:di", "di_1", Boolean::class)

    val compressorDelayTimer = timer(1000L, 30 * 1000L)
    flowSensor.onChanged {
        if (it) {
            compressorDelayTimer.start()
        } else {
            compressorDelayTimer.stop()
        }
    }
    compressorRelay receiveFrom map {
        val flowGoes = snapshot(flowSensor)
        val delayPassed = !snapshot(compressorDelayTimer.active)
        val itIsDaytime = snapshot(clock(Clock.Interval.HOUR).time).hour >= 6
        flowGoes && delayPassed && itIsDaytime
    }
}

/**
 * 2-key control of candle light in livingroom
 * 00 - 0%
 * 01 - 10%
 * 10 - 30%
 * 11 - 100%
 */
fun TreeBuilder.configureLivingroomCandleLight() {
    val lightKey1 = output("wb:di1", "di1", Boolean::class)
    val lightKey2 = output("wb:di1", "di2", Boolean::class)
    val channelIsOn = input("wb:d1", "channel1_on_set", Boolean::class)
    val channelPower = input("wb:d1", "channel1_power_set", Int::class)

    channelIsOn receiveFrom map { snapshot(lightKey1) || snapshot(lightKey2) }
    channelPower receiveFrom map {
        when (snapshot(lightKey1) to snapshot((lightKey2))) {
            false to false -> 0
            false to true -> 10
            true to false -> 30
            else -> 100
        }
    }
}

fun TreeBuilder.configurePowerMonitoring(): Node.Producer<PowerType> {
    val phaseVoltage = (1..3).map {
        output("wb:power_meter", "u$it", Float::class) // volts
    }
    val phaseAngle = (1..3).map {
        output("wb:power_meter", "phi$it", Float::class) // degrees
    }
    val typeMonitor = monitor<PowerType>(2000L) { measures ->
        if (measures.all { it == measures.last() }) {
            measures.last()
        } else {
            null
        }
    }
    typeMonitor.input receiveFrom map {
        val voltages = phaseVoltage.map { snapshot(it) }
        val angles = phaseAngle.map { snapshot(it) }
        when {
            voltages.all { it < 50 } -> PowerType.UPS
            angles.all { abs(angles.first() - it) < 10 } -> PowerType.GENERATOR
            else -> PowerType.CITY
        }
    }
    return typeMonitor.output
}

fun TreeBuilder.configureEconomizer(powerType: Node.Producer<PowerType>) {
    val boilerReducePower = input("wb:r7", "input1", Boolean::class)
    val boilerEnable = input("wb:r7", "input2", Boolean::class)
    boilerReducePower receiveFrom map { snapshot(powerType) != PowerType.CITY }
    boilerEnable receiveFrom map { snapshot(powerType) != PowerType.UPS }
}

fun TreeBuilder.configureMonitor() {
    val m = monitor<Float>(10_000) { it.average().toFloat() }
    m.input receiveFrom output("wb:power_meter", "p1", Float::class)
    m.output transmitTo synthetic(
        "power_average",
        Float::class,
        Synthetic.ExternalAccess.READ,
        persistence = Synthetic.Persistence.None()
    ).input
}