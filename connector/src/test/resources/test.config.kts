import script.definition.Clock
import script.definition.TreeBuilder

config {
    println("Applying config ---- ")
    configureBlink()
}

/**
 * Blink every second
 */
fun TreeBuilder.configureBlink() {
    /* Shortcuts */
    val relay2 = input("wb:wb_mrwm2_75", "k2_in", Boolean::class)
//    val relayKey1 = output("wb:wb_mrwm2_75", "input_1_out", Boolean::class)
//
//    val timer = timer(tickInterval = 1000L, stopAfter = 4000L)
//
//    relay2.value = map {
//        val millis = monitor(timer.millisPassed)
//        println("relay2.value = map ")
//        (millis / 1000) % 2 == 0L
//    }
//
//    relayKey1.onChanged {
//        println("relayKey1.onChanged()")
//        timer.start()
//    }

    val clock = clock(Clock.Interval.SECOND)
    var times = 0
    clock.time.onChanged { time ->
        println("Time changed to $time")
        relay2.value = constant(time.second % 2 == 0 && times < 4)
        times++
    }
}