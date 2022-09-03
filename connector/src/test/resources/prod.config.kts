import script.definition.Clock
import script.definition.TreeBuilder

config {
    println("Applying config ---- ")
    configureCandleLight()
    configureMasterBedroomLights()
    configureStreetLights()
}

/**
 * Candle light in projector area controls with 2-key wall switch
 */
fun TreeBuilder.configureCandleLight() {
    /* Shortcuts */
    val key1 = output("wb:wb_gpio", "ext2_in5_out", Boolean::class)
    val key2 = output("wb:wb_gpio", "ext2_in6_out", Boolean::class)
    val candleOn = input("wb:wb_mdm3_0x54", "k1_in", Boolean::class)
    val candlePower = input("wb:wb_mdm3_0x54", "channel_1_in", Int::class)

    candleOn.value = map { monitor(key1) || monitor(key2) }
    candlePower.value = map {
        when (monitor(key1) to monitor(key2)) {
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
    val spotsOn = input("wb:wb_mdm3_0x54", "k2_in", Boolean::class)
    val ntshLightOn = input("wb:wb_mr6c_31", "k5_in", Boolean::class)
    val alexLightOn = input("wb:wb_mr6c_31", "k3_in", Boolean::class)
    val spotsPower = input("wb:wb_mdm3_0x54", "channel_2_in", Int::class)

    spotsOn.value = spotsKey
    spotsPower.value = map { if (monitor(spotsKey)) 100 else 0 }
    ntshLightOn.value = ntshLightKey
    alexLightOn.value = alexLightKey
}

/**
 * Facade lights and projectors are on only on evening and night hours
 */
fun TreeBuilder.configureStreetLights() {
    /* Shortcuts */
    val projectors = input("wb:wb_mr6c_57", "k1_in", Boolean::class)
    val facade = input("wb:wb_mr3_29", "k1_in", Boolean::class)

    val clock = clock(Clock.Interval.HOUR)
    val onTime = map {
        val time = monitor(clock.time)
        time.hour >= 19 || time.hour <= 8
    }
    projectors.value = onTime
    facade.value = onTime
}
