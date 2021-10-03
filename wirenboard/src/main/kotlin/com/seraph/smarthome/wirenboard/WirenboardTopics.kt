package com.seraph.smarthome.wirenboard

import com.seraph.smarthome.transport.Topic

class WirenboardTopics {
    companion object {
        fun device(id: String = "+") = Topic.fromString("/devices/${id}")
        fun deviceMeta(id: String = "+") = device(id) + "meta"
        fun deviceName(id: String = "+") = deviceMeta(id) + "name"
        fun control(dId: String = "+", cId: String = "+") = (device(dId) + "controls") + cId
        fun controlWrite(dId: String, cId: String = "+") = control(dId, cId) + "on"
        fun controlMeta(dId: String, cId: String = "+") = control(dId, cId) + "meta"
        fun controlType(dId: String, cId: String = "+") = controlMeta(dId, cId) + "type"
        fun controlReadonly(dId: String, cId: String = "+") = controlMeta(dId, cId) + "readonly"
        fun controlRange(dId: String, cId: String = "+") = controlMeta(dId, cId) + "range"
    }
}