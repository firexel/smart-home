import script.definition.TreeBuilderContext

config {
    configureTechRoomAutoLight()
    configureFacadeLight()
    configureFilterAirCompressor()
    configureLivingroomCandleLight()
}

/**
 * Auto enable technical room light on enter and disable after 5min delay
 */
fun TreeBuilderContext.configureTechRoomAutoLight() {
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
    techRoomLight.value = map {
        val keyOn = monitor(techRoomLightKey)
        val timerActive = monitor(enterTimer.active)
        val timerMillisPassed = monitor(enterTimer.millisPassed)
        keyOn || (timerActive && timerMillisPassed < lightTimeoutS * 1000)
    }
}

/**
 *  Auto enable facade light on sunset and disable on the sunrise
 */
fun TreeBuilderContext.configureFacadeLight() {
    val facadeLightRelay = input("wb:relay5", "relay1_on_set", Boolean::class)
    val facadeLightSwitch = output("wb:relay5", "relay1_on", Boolean::class)
    val sunrise = output("geo:current_day", "sun_is_risen", Boolean::class)

    sunrise.onChanged { isRisen ->
        facadeLightRelay.value = constant(isRisen)
    }
    facadeLightSwitch.onChanged { isOn ->
        facadeLightRelay.value = constant(isOn)
    }
}

/**
 * Enable air compressor only
 * after 30s delay and only while sensor is on
 * only in daytime
 */
fun TreeBuilderContext.configureFilterAirCompressor() {
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
    compressorRelay.value = map {
        val flowGoes = monitor(flowSensor)
        val delayPassed = !monitor(compressorDelayTimer.active)
        val itIsDaytime = monitor(date().hourOfDate) >= 6
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
fun TreeBuilderContext.configureLivingroomCandleLight() {
    val lightKey1 = output("wb:di1", "di1", Boolean::class)
    val lightKey2 = output("wb:di1", "di2", Boolean::class)
    val channelIsOn = input("wb:d1", "channel1_on_set", Boolean::class)
    val channelPower = input("wb:d1", "channel1_power_set", Int::class)

    channelIsOn.value = map { monitor(lightKey1) || monitor(lightKey2) }
    channelPower.value = map {
        when (monitor(lightKey1) to monitor((lightKey2))) {
            false to false -> 0
            false to true -> 10
            true to false -> 30
            else -> 100
        }
    }
}