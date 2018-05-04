package com.seraph.smarthome.io

/**
 * Created by aleksandr.naumov on 10.03.18.
 */
data class Config(
        val buses: List<Rs485Bus>
)

data class Rs485Bus(
        val name:String,
        val settings: PortSettings,
        val modules: List<ModbusModule>
)

data class PortSettings(
        val path: String,
        val baudRate: Int,
        val parity: Parity,
        val dataBits: Int,
        val stopBits: Int
)

enum class Parity {
    NO, ODD, EVEN, MARK, SPACE
}

data class ModbusModule(
        val name: String,
        val index: Byte,
        val model: ModbusDeviceModel
)

enum class ModbusDeviceModel {
    WELLPRO_8028
}