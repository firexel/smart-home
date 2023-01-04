import script.definition.Synthetic
import script.definition.TreeBuilder

config {
    println("Applying config ---- ")
    configureFilter()
    configureThermostat()
}

/**
 * Blink every second
 */
fun TreeBuilder.configureFilter() {
    val m = monitor<Boolean, Float>(5_000) {
        println(it.toString())
        when (it.size) {
            0 -> 0f
            else -> it.count { it }.toFloat() / it.size
        }
    }
    m.input receiveFrom output("wb:wb_mrwm2_75", "input_1_out", Boolean::class)
    m.output transmitTo synthetic(
        "push_ratio", Float::class,
        Synthetic.ExternalAccess.READ,
        persistence = Synthetic.Persistence.None()
    ).input
}

fun TreeBuilder.configureThermostat() {
    val thermostat = synthetic(
        "temp_target", Float::class,
        Synthetic.ExternalAccess.READ_WRITE,
        persistence = Synthetic.Persistence.Stored(23f)
    )

    synthetic(
        "temp_target", Unit::class,
        Synthetic.ExternalAccess.READ_WRITE
    ).output.onChanged {
        // todo
    }

    synthetic(
        "temp_target", Unit::class,
        Synthetic.ExternalAccess.READ_WRITE
    ).output.onChanged {
        // todo
    }
}