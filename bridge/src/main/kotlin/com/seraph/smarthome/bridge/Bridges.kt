package com.seraph.smarthome.bridge

import com.seraph.smarthome.device.DeviceDriver
import com.seraph.smarthome.device.DriversManager
import com.seraph.smarthome.domain.Device
import com.seraph.smarthome.transport.Topic

class Bridge(private val driversManager: DriversManager) {
    fun addBridge(topic: Topic, device: Device.Id) {
        driversManager.addDriver(device, DimmerDriver(topic))
    }
}

class DimmerDriver(rootTopic: Topic) : DeviceDriver {
    override fun bind(visitor: DeviceDriver.Visitor) {
        TODO("not implemented")
    }

}
