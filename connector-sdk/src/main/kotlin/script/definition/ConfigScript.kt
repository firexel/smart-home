package script.definition

import com.seraph.connector.tree.Node
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    displayName = "Test config script",
    fileExtension = "config.kts",
    compilationConfiguration = ConfigScriptCompilationConfiguration::class
)
abstract class ConfigScript(private val builder: TreeBuilder) {
    fun config(block: TreeBuilder.() -> Unit) {
        val context = TreeBuilderContext(builder)
        try {
            context.block()
        } catch (ex: Throwable) {
            val exception = RuntimeException("Error executing script", ex)
            exception.printStackTrace()
            throw exception
        }
    }
}

class TreeBuilderContext(private val builder: TreeBuilder) : TreeBuilder by builder

interface TreeBuilder {

    infix fun <T : Any> Node.Producer<T>.transmitTo(consumer: Node.Consumer<T>)
    infix fun <T : Any> Node.Consumer<T>.receiveFrom(producer: Node.Producer<T>)
    fun <T : Any> Node.Consumer<T>.disconnect()

    fun <T : Any> input(devId: String, endId: String, type: KClass<T>): Node.Consumer<T>
    fun <T : Any> output(devId: String, endId: String, type: KClass<T>): Node.Producer<T>
    fun <T : Any> constant(value: T): Node.Producer<T>
    fun <T : Any> map(block: suspend RuntimeReadContext.() -> T): Node.Producer<T>
    fun <T : Any> Node.Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit)

    fun <T : Any> synthetic(
        devId: String,
        type: KClass<T>,
        access: Synthetic.ExternalAccess,
        units: Units = Units.NO,
        persistence: Synthetic.Persistence<T> = Synthetic.Persistence.None()
    ): Synthetic<T>

    fun timer(tickInterval: Long = 1000L, stopAfter: Long): Timer
    fun clock(tickInterval: Clock.Interval): Clock
    fun <R, T> monitor(windowWidthMs: Long, aggregator: (List<R>) -> T?): Monitor<R, T>
}

interface Monitor<R, T> : Node {
    val input: Node.Consumer<R>
    val output: Node.Producer<T>
}

interface Synthetic<T> : Node {
    val output: Node.Producer<T>
    val input: Node.Consumer<T>

    enum class ExternalAccess { READ, WRITE, READ_WRITE }
    sealed class Persistence<T> {
        class None<T> : Persistence<T>()
        class Runtime<T>(val default: T?) : Persistence<T>()
        class Stored<T>(val default: T?) : Persistence<T>()
    }
}

enum class Units {
    NO,
    CELSIUS,
    PPM,
    PPB,
    PERCENTS_0_1,
    LX,
    ON_OFF,
    W, // watts
    KWH, // kWh
    V, // volts,
    A, // amperes,
    MBAR // millibars
}

interface Timer : Node {
    fun start()
    fun stop()
    val active: Node.Producer<Boolean>
    val millisPassed: Node.Producer<Long>
}

interface Clock : Node {
    val time: Node.Producer<LocalDateTime>

    enum class Interval {
        HOUR, MINUTE, SECOND
    }
}

interface RuntimeReadContext {
    suspend fun <T> snapshot(producer: Node.Producer<T>): T
}

interface RuntimeWriteContext : RuntimeReadContext {
    suspend infix fun <T> Node.Consumer<T>.setTo(data: T)
}

interface RuntimeFullContext : RuntimeWriteContext, TreeBuilder

object ConfigScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        jvm {
            // Extract the whole classpath from context classloader and use it as dependencies
            dependenciesFromCurrentContext(wholeClasspath = true)
        }

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    }
)