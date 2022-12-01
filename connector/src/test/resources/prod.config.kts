import script.definition.Clock
import script.definition.Synthetic
import script.definition.TreeBuilder
import script.definition.Units
import kotlin.math.abs
import kotlin.math.min

config {
    println("Applying config ---- ")
    configureCandleLight()
    configureMasterBedroomLights()
    configureStreetLights()
    configureStreetTemp()
}

/**
 * Candle light in projector area controls with 2-key wall switch
 */
fun TreeBuilder.configureCandleLight() {
    /* Shortcuts */
    val key1 = output("wb:wb_gpio", "ext2_in5_out", Boolean::class)
    val key2 = output("wb:wb_gpio", "ext2_in6_out", Boolean::class)
    val candleOn = input("wb:d1", "k1_in", Boolean::class)
    val candlePower = input("wb:d1", "channel_1_in", Int::class)

    map { snapshot(key1) || snapshot(key2) } transmitTo candleOn
    candlePower receiveFrom map {
        when (snapshot(key1) to snapshot(key2)) {
            false to false -> 0
            false to true -> 15
            true to false -> 50
            else -> 100
        }
    }
}

/**
 * Master bedroom bed reading lights and ceil spots are controlled by wire
 */
fun TreeBuilder.configureMasterBedroomLights() {
    /* Shortcuts */
    val spotsKey = output("wb:wb_gpio", "ext2_in3_out", Boolean::class)
    val ntshLightKey = output("wb:wb_gpio", "ext2_in2_out", Boolean::class)
    val alexLightKey = output("wb:wb_gpio", "ext2_in1_out", Boolean::class)
    val spotsOn = input("wb:d1", "k2_in", Boolean::class)
    val ntshLightOn = input("wb:r1", "k5_in", Boolean::class)
    val alexLightOn = input("wb:r1", "k3_in", Boolean::class)
    val spotsPower = input("wb:d1", "channel_2_in", Int::class)

    spotsOn receiveFrom spotsKey
    spotsPower receiveFrom map { if (snapshot(spotsKey)) 100 else 0 }
    ntshLightOn receiveFrom ntshLightKey
    alexLightOn receiveFrom alexLightKey
}

/**
 * Facade lights and projectors are on only on evening and night hours
 */
fun TreeBuilder.configureStreetLights() {
    /* Shortcuts */
    val projectors = input("wb:r2", "k1_in", Boolean::class)
    val facade = input("wb:r5", "k1_in", Boolean::class)

    val clock = clock(Clock.Interval.MINUTE)

    val onTime = map {
        val time = snapshot(clock.time)

        val start = time
            .withHour(8)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val end = start
            .withHour(17)
            .withMinute(30)

        time.isAfter(end) || time.isBefore(start)
    }
    projectors receiveFrom onTime
    facade receiveFrom onTime
}

/**
 * Deduce outside temperature
 */
fun TreeBuilder.configureStreetTemp() {
    /* Shortcuts */
    val t1 = output("wb:wb_w1", "28_0517c0ddbbff_out", Float::class)
    val t2 = output("wb:wb_w1", "28_0517c0e102ff_out", Float::class)
    val t3 = output("wb:wb_w1", "28_0517c0e7e2ff_out", Float::class)

    val quorum = map {
        val measures = listOf(snapshot(t1), snapshot(t2), snapshot(t3))
            .filter { it <= 50 && it >= -50 }

        when (measures.size) {
            0 -> 0f
            1 -> measures.first()
            2 -> measures.average().toFloat()
            else -> {
                measures
                    .map { m ->
                        measures.sumOf { abs(it - m).toDouble() } to m
                    }
                    .sortedByDescending { it.first }
                    .takeLast(measures.size - 1)
                    .map { it.second }
                    .average()
                    .toFloat()
            }
        }
    }

    quorum transmitTo synthetic(
        "outside_temp", Float::class,
        Synthetic.ExternalAccess.READ,
        Units.CELSIUS
    ).input

    synthetic("target_coolant_temp", Float::class,
        Synthetic.ExternalAccess.READ, Units.CELSIUS
    ).input receiveFrom map {
        val t = snapshot(quorum)
        val k = 0.97 // heat curve type
        val a = -0.1 * k - 0.06
        val b = 6.04 * k + 1.98
        val c = -5.06 * k + 18.06
        val x = -0.2 * t + 5
        val target = a*x*x + b*x + c
        when {
            target > 70 -> 70
            target < 20 -> 20
            else -> target
        }.toFloat()
    }
}