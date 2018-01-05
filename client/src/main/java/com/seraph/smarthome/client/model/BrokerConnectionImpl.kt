package com.seraph.smarthome.client.model

import com.seraph.smarthome.client.cases.BrokerConnection
import com.seraph.smarthome.client.util.replace
import com.seraph.smarthome.transport.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

class BrokerConnectionImpl(private val broker: Broker) : BrokerConnection {
    private val propertyStorageAdapter = PropertyStorageAdapter()
    private val deviceMap = mutableMapOf<Device.Id, Device>()
    private val delayedPropertyUpdates = mutableMapOf<PropertyPath, String>()

    private val devicesSubject = ReplaySubject.createWithSize<List<Device>>(1)
            .apply { subscribeOn(Schedulers.io()) }

    private val metadataSubject = ReplaySubject.createWithSize<Metadata>(1)
            .apply { subscribeOn(Schedulers.io()) }

    private val stateSubject = ReplaySubject.createWithSize<BrokerConnection.State>(1)
            .apply { subscribeOn(Schedulers.io()) }

    override val devices: Observable<List<Device>> = devicesSubject
    override val metadata: Observable<Metadata> = metadataSubject
    override val state: Observable<BrokerConnection.State> = stateSubject

    init {
        broker.addStateListener(object : Broker.StateListener {
            override fun onStateChanged(brokerState: Broker.BrokerState) {
                handleStateChange(brokerState)
            }
        })
        Topics.metadata().subscribe(broker) { metadata ->
            metadataSubject.onNext(metadata.map())
        }
        Topics.structure().subscribe(broker) { commonDevice ->
            handleDevicePublished(commonDevice)
        }
        broker.subscribe(Topics.property()) { topic, data ->
            handlePropertyPublished(topic, data)
        }
    }

    private fun handleStateChange(brokerState: Broker.BrokerState) {
        stateSubject.onNext(brokerState.map())
    }

    private fun handleDevicePublished(commonDevice: com.seraph.smarthome.model.Device) {
        val device = commonDevice.map(propertyStorageAdapter)
        deviceMap[device.id] = applyDelayedUpdates(device)
        updateDevicesSubject()
    }

    private fun handlePropertyPublished(topic: Topic, data: String) {
        val path = PropertyPath.fromTopic(topic)
        val device = deviceMap[path.devId]
        if (device != null) {
            deviceMap[path.devId] = device.copy(
                    properties = device.properties.replace(
                            predicate = { it.id == path.propId },
                            constructor = { it.accept(CloneVisitor(data)) }
                    )
            )
            updateDevicesSubject()
        } else {
            delayedPropertyUpdates[path] = data
        }
    }

    private fun updateDevicesSubject() {
        devicesSubject.onNext(ArrayList(deviceMap.values))
    }

    private fun applyDelayedUpdates(device: Device): Device = device.copy(
            properties = device.properties.replace(
                    predicate = {
                        delayedPropertyUpdates.contains(PropertyPath(device.id, it.id))
                    },
                    constructor = {
                        val path = PropertyPath(device.id, it.id)
                        val newVal = delayedPropertyUpdates[path]!!
                        delayedPropertyUpdates.remove(path)
                        it.accept(CloneVisitor(newVal))
                    }
            )
    )

    override fun <T> change(deviceId: Device.Id, property: Property<T>, value: T)
            : Observable<Unit> = Observable.fromCallable {

        val topic = Topics.property(deviceId.map(), property.id.map())
        property.accept(PublishVisitor(topic, broker, value))
    }

    private data class PropertyPath(val devId: Device.Id, val propId: Property.Id) {
        companion object {
            fun fromTopic(topic: Topic): PropertyPath {
                val ids = topic.segments.takeLast(2)
                val propertyId = Property.Id(ids[1])
                val deviceId = Device.Id(ids[0])
                return PropertyPath(deviceId, propertyId)
            }
        }
    }

    class CloneVisitor(private val data: String) : Property.Visitor<Property<*>> {
        override fun onIndicatorVisited(property: IndicatorProperty): Property<*> = IndicatorProperty(
                id = property.id,
                name = property.name,
                priority = property.priority,
                value = BooleanConverter().fromString(data)
        )

        override fun onActionVisited(property: ActionProperty): Property<*> = property
    }

    class PublishVisitor<T>(
            private val topic: Topic,
            private val broker: Broker,
            private val value: T)
        : Property.Visitor<Unit> {

        override fun onIndicatorVisited(property: IndicatorProperty)
                = topic.typed(BooleanConverter()).publish(broker, value as Boolean)

        override fun onActionVisited(property: ActionProperty)
                = topic.unpersisted().typed(ActionConverter()).publish(broker, value as Unit)
    }

    private inner class PropertyStorageAdapter : PropertyStorage {
        override fun <T : Any> getValueFor(deviceId: Device.Id, propertyId: Property.Id, clazz: KClass<T>): T {
            val property = deviceMap[deviceId]?.properties?.find { it.id == propertyId }
            val value = property?.accept(ValueVisitor()) ?: when (clazz) {
                Int::class -> 0
                Boolean::class -> false
                String::class -> ""
                else -> throw IllegalArgumentException("Unknown type $clazz")
            }
            return clazz.cast(value)
        }
    }

    class ValueVisitor : Property.Visitor<Any> {
        override fun onIndicatorVisited(property: IndicatorProperty): Any = property.value
        override fun onActionVisited(property: ActionProperty): Any = Unit
    }
}