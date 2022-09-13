package script.definition

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
//    fun <T : Any> Producer<T>.stored(name: String): Producer<T>
//    fun <T : Any> virtual(devId: String, type: KClass<T>, readonly: Boolean = false): Producer<T>
    fun timer(tickInterval: Long = 1000L, stopAfter: Long): Timer
    fun clock(tickInterval: Clock.Interval): Clock
}

interface Timer {
    fun start()
    fun stop()
    val active: Producer<Boolean>
    val millisPassed: Producer<Long>
}

interface Clock {
    val time: Producer<LocalDateTime>

    enum class Interval {
        HOUR, MINUTE, SECOND
    }
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