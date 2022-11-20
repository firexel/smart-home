package com.seraph.connector.tree

import com.seraph.connector.tools.BlockingNetwork
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.util.Log
import script.definition.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class ConnectorTreeBuilder(
    private val network: BlockingNetwork,
    private val holder: TreeHolder,
    private val storageProvider: StorageProvider,
    private val driversManager: DriversManager,
    private val log: Log
) : TreeBuilder {

    override fun <T : Any> input(devId: String, endId: String, type: KClass<T>): Node.Consumer<T> {
        val key = EndpointKey(devId, endId, type)
        val node: InputNode<T> = holder.obtain(key, InputNode::class) {
            val log = log.copy("Input").copy("$devId/$endId")
            InputNode(devId, endId, type, network, log)
        } as InputNode<T>
        return node.consumer
    }

    override fun <T : Any> output(devId: String, endId: String, type: KClass<T>): Node.Producer<T> {
        val key = EndpointKey(devId, endId, type)
        val node: OutputNode<T> = holder.obtain(key, OutputNode::class) {
            val log = log.copy("Output").copy("$devId/$endId")
            OutputNode(devId, endId, type, network, log)
        } as OutputNode<T>
        return node.producer
    }

    override fun <T : Any> constant(value: T): Node.Producer<T> {
        val node = ConstantNode(value)
        holder.install(node)
        return node.value
    }

    override fun <T : Any> Node.Producer<T>.onChanged(block: TreeBuilder.(value: T) -> Unit) {
        val node = CallbackNode(block, this@ConnectorTreeBuilder, log.copy("Callback"))
        holder.install(node)
        holder.connect(this, node.consumer)
    }

//    override fun <T : Any> synthetic(
//        devId: String,
//        type: KClass<T>,
//        access: Synthetic.ExternalAccess,
//        persistence: Synthetic.Persistence<T>
//    ): Synthetic<T> {
//        val node: SyntheticNode<T> = holder.obtain(devId, SyntheticNode::class) {
//            TODO()
////            SyntheticNode(
////                devId,
////                type,
////                persistence,
////                storageProvider.makeStorage(devId),
////                driversManager,
////                log.copy("Synthetic").copy(devId)
////            )
//        } as SyntheticNode<T>
//        return node
//    }

//    override fun <T> monitor(windowWidthMs: Long, aggregator: (List<T>) -> T?): Monitor<T> {
//        return MonitorNode(windowWidthMs, aggregator, java.time.Clock.systemDefaultZone())
//            .apply { holder.install(this) }
//    }

    override fun <T : Any> map(block: suspend MapContext.() -> T): Node.Producer<T> {
        return MapNode(block).apply { holder.install(this) }.output
    }

    override fun timer(tickInterval: Long, stopAfter: Long): Timer {
        val log = log.copy("Timer")
        return TimerNode(tickInterval, stopAfter, log).apply { holder.install(this) }
    }

    override fun clock(tickInterval: Clock.Interval): Clock {
        return ClockNode(tickInterval).apply { holder.install(this) }
    }

    override fun <T : Any> Node.Producer<T>.transmitTo(consumer: Node.Consumer<T>) {
        holder.connect(this, consumer)
    }

    override fun <T : Any> Node.Consumer<T>.receiveFrom(producer: Node.Producer<T>) {
        holder.connect(producer, this)
    }

    override fun <T : Any> Node.Consumer<T>.disconnect() {
        holder.disconnect(this)
    }

    data class EndpointKey(
        val devId: String,
        val endId: String,
        val type: KClass<*>
    )
}