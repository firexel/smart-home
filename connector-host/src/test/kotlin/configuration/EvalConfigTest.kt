package configuration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import script.definition.*
import kotlin.reflect.KClass


internal class EvalConfigTest {

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun testNoSyntaxErrors() {
        val result = evalConfigFromFile("test.full_syntax.config.kts")
        assertEquals(emptyList<ExecutionNote>(), result)
    }

    private fun evalConfigFromFile(name: String): List<ExecutionNote> {
        return runBlocking {
            EvalConfigInstaller { mockTreeBuilder() }.checkConfig(getFileFromResources(name))
        }
    }

    private fun getFileFromResources(fileName: String): String {
        val classLoader = javaClass.classLoader
        val inputStream = classLoader.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("file not found! $fileName")

        return inputStream.bufferedReader().readText()
    }

    private fun mockTreeBuilder(): EvalConfigInstaller.CheckingTreeBuilder {
        return object : EvalConfigInstaller.CheckingTreeBuilder {
            override val results: List<ExecutionNote>
                get() = emptyList()

            override fun <T : Any> input(
                devId: String,
                endId: String,
                type: KClass<T>
            ): Consumer<T> = mockConsumer()

            override fun <T : Any> output(
                devId: String,
                endId: String,
                type: KClass<T>
            ): Producer<T> = mockProducer()

            override fun <T : Any> constant(value: T): Producer<T> = mockProducer()

            override fun <T : Any> map(block: MapContext.() -> T): Producer<T> = mockProducer()

            override fun <T : Any> Producer<T>.onChanged(block: TreeBuilderContext.(value: T) -> Unit) {

            }

            override fun timer(tickInterval: Long, stopAfter: Long): Timer {
                return object : Timer {
                    override fun start() {}

                    override fun stop() {}

                    override val active: Producer<Boolean>
                        get() = mockProducer()

                    override val millisPassed: Producer<Long>
                        get() = mockProducer()
                }
            }

            override fun date(): Date {
                return object : Date {
                    override val hourOfDate: Producer<Int>
                        get() = mockProducer()
                }
            }

            fun <T> mockConsumer(): Consumer<T> {
                return object : Consumer<T> {
                    override var value: Producer<T>?
                        get() = TODO("not implemented")
                        set(value) {}
                }
            }

            fun <T> mockProducer(): Producer<T> {
                return object : Producer<T> {}
            }
        }
    }
}