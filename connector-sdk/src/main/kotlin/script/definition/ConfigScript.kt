package script.definition

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
        context.block()
    }
}

class TreeBuilderContext(private val builder: TreeBuilder) : TreeBuilder by builder

interface Producer<T>
interface Consumer<T> {
    var value: Producer<T>?
}

interface TreeBuilder {
    fun <T : Any> input(devId: String, endId: String, type: KClass<T>): Consumer<T>
    fun <T : Any> output(devId: String, endId: String, type: KClass<T>): Producer<T>
    fun <T : Any> constant(value: T): Producer<T>
    fun <T : Any> map(block: suspend MapContext.() -> T): Producer<T>
    fun <T : Any> Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit)

    fun timer(tickInterval: Long = 1000L, stopAfter: Long = -1): Timer
    fun date(): Date
}

interface Timer {
    fun start()
    fun stop()
    val active: Producer<Boolean>
    val millisPassed: Producer<Long>
}

interface Date {
    val hourOfDate: Producer<Int>
}

interface MapContext {
    suspend fun <T> monitor(producer: Producer<T>): T
}

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