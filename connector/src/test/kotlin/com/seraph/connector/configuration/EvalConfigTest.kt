package com.seraph.connector.configuration

import com.seraph.connector.tree.Node
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import script.definition.*
import java.time.LocalDateTime
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

    @Test
    fun testNoSyntaxErrorsInEmptyConfig() {
        val result = evalConfigFromFile("test.empty.config.kts")
        assertEquals(emptyList<ExecutionNote>(), result)
    }

    private fun evalConfigFromFile(name: String): List<ExecutionNote> {
        return runBlocking {
            val config = getFileFromResources(name)
            ConfigExecutor(config).execute(mockTreeBuilder())
        }
    }

    private fun getFileFromResources(fileName: String): String {
        val classLoader = javaClass.classLoader
        val inputStream = classLoader.getResourceAsStream(fileName)
            ?: throw IllegalArgumentException("file not found! $fileName")

        return inputStream.bufferedReader().readText()
    }

    private fun mockTreeBuilder(): TreeBuilder {
        return object : TreeBuilder {
            override fun <T : Any> input(
                devId: String,
                endId: String,
                type: KClass<T>
            ): Node.Consumer<T> = mockConsumer()

            override fun <T : Any> output(
                devId: String,
                endId: String,
                type: KClass<T>
            ): Node.Producer<T> = mockProducer()

            override fun <T : Any> constant(value: T): Node.Producer<T> = mockProducer()

            override fun <T : Any> map(block: suspend MapContext.() -> T): Node.Producer<T> =
                mockProducer()

            override fun <T : Any> Node.Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit) {
            }

            override fun <T : Any> Node.Producer<T>.transmitTo(consumer: Node.Consumer<T>) {
            }

            override fun <T : Any> Node.Consumer<T>.receiveFrom(producer: Node.Producer<T>) {
            }

            override fun <T : Any> Node.Consumer<T>.disconnect() {
            }

            override fun <T : Any> synthetic(
                devId: String,
                type: KClass<T>,
                access: Synthetic.ExternalAccess,
                persistence: Synthetic.Persistence<T>
            ): Synthetic<T> = object : Synthetic<T> {
                override val output: Node.Producer<T>
                    get() = mockProducer()
                override val input: Node.Consumer<T>
                    get() = mockConsumer()
            }

            override fun timer(tickInterval: Long, stopAfter: Long): Timer {
                return object : Timer {
                    override fun start() {}

                    override fun stop() {}

                    override val active: Node.Producer<Boolean>
                        get() = mockProducer()

                    override val millisPassed: Node.Producer<Long>
                        get() = mockProducer()
                }
            }

            override fun clock(tickInterval: Clock.Interval): Clock {
                return object : Clock {
                    override val time: Node.Producer<LocalDateTime>
                        get() = mockProducer()
                }
            }

            fun <T> mockConsumer(): Node.Consumer<T> {
                return object : Node.Consumer<T> {
                    override val parent: Node
                        get() = TODO("Not yet implemented")

                    override suspend fun consume(flow: StateFlow<T?>) {
                        TODO("Not yet implemented")
                    }
                }
            }

            fun <T> mockProducer(): Node.Producer<T> {
                return object : Node.Producer<T> {
                    override val parent: Node
                        get() = TODO("Not yet implemented")
                    override val flow: Flow<T?>
                        get() = TODO("Not yet implemented")
                }
            }
        }
    }
}