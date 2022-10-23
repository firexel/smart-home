import script.definition.Synthetic
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
    val relay1 = output("wb:wb_mrwm2_75", "k1_out", Boolean::class)
    synthetic(
        "synth", Int::class,
        Synthetic.ExternalAccess.READ,
        Synthetic.Persistence.Stored(0)
    ).input receiveFrom map {
        if (monitor(relay1)) {
            42
        } else {
            0
        }
    }
}