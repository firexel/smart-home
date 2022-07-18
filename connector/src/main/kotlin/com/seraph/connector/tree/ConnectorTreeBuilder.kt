package com.seraph.connector.tree

import com.seraph.connector.tools.BlockingNetwork
import com.seraph.smarthome.util.Log
import script.definition.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class ConnectorTreeBuilder(
    private val network: BlockingNetwork,
    private val log: Log
) : TreeBuilder {

    private val holder = TreeHolder()

    override fun <T : Any> input(devId: String, endId: String, type: KClass<T>): Consumer<T> {
        val key = EndpointKey(devId, endId, type)
        val node: InputNode<T> = holder.obtain(key, InputNode::class) {
            val log = log.copy("Input").copy("$devId/$endId")
            InputNode(devId, endId, type, network, log)
        } as InputNode<T>
        return node.consumer.wrap()
    }

    override fun <T : Any> output(devId: String, endId: String, type: KClass<T>): Producer<T> {
        val key = EndpointKey(devId, endId, type)
        val node: OutputNode<T> = holder.obtain(key, OutputNode::class) {
            val log = log.copy("Output").copy("$devId/$endId")
            OutputNode(devId, endId, type, network, log)
        } as OutputNode<T>
        return node.producer.wrap()
    }

    override fun <T : Any> constant(value: T): Producer<T> {
        val node = ConstantNode(value)
        holder.install(node)
        return node.value.wrap()
    }

    override fun <T : Any> Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit) {
        val node = CallbackNode(this.unwrap(), block, this@ConnectorTreeBuilder)
        holder.install(node)
    }

    override fun <T : Any> map(block: suspend MapContext.() -> T): Producer<T> {
        val node = MapNode(block)
        holder.install(node)
        return node.output.wrap()
    }

    override fun timer(tickInterval: Long, stopAfter: Long): Timer {
        TODO("not implemented")
    }

    override fun date(): Date {
        TODO("not implemented")
    }

    data class ProducerImpl<T>(val nodeProducer: Node.Producer<T>) : Producer<T>

    private fun <T> Node.Producer<T>.wrap(): Producer<T> = ProducerImpl(this)
    private fun <T> Producer<T>.unwrap(): Node.Producer<T> = (this as ProducerImpl<T>).nodeProducer

    inner class ConsumerImpl<T>(val nodeConsumer: Node.Consumer<T>) : Consumer<T> {
        override var value: Producer<T>? = null
            set(producer) {
                field = producer
                if (producer != null) {
                    holder.connect(producer.unwrap(), nodeConsumer)
                } else {
                    holder.disconnect(nodeConsumer)
                }
            }
    }

    private fun <T> Node.Consumer<T>.wrap(): Consumer<T> = ConsumerImpl(this)
    private fun <T> Consumer<T>.unwrap(): Node.Consumer<T> = (this as ConsumerImpl<T>).nodeConsumer

    data class EndpointKey(
        val devId: String,
        val endId: String,
        val type: KClass<*>
    )
}