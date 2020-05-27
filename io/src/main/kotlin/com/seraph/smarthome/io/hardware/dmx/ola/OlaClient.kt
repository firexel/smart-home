/***********************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package com.seraph.smarthome.io.hardware.dmx.ola

import com.google.protobuf.*
import com.seraph.smarthome.util.Log
import ola.proto.Ola.*
import kotlin.experimental.and

class OlaClient(
        oladHost: String,
        oladPort: Int,
        private val log: Log
) {
    private val serverService: OlaServerService
    private val controller: RpcController
    private val channel: RpcChannel

    init {
        channel = StreamRpcChannel(oladHost, oladPort)
        controller = SimpleRpcController()
        serverService = OlaServerService.Stub.newStub(channel)
    }

    private val deviceInfo: DeviceInfoReply
        get() = callRpcMethod("GetDeviceInfo", DeviceInfoRequest.newBuilder().build()) as DeviceInfoReply


    private val universeList: UniverseInfoReply
        get() {
            val request = OptionalUniverseRequest.newBuilder().build()
            return callRpcMethod("GetUniverseInfo", request) as UniverseInfoReply
        }

    fun getSession(deviceName: String, portIndex: Int): DmxSession {
        log.v("Searching for universe for device $deviceName port $portIndex...")
        val devices = deviceInfo.deviceList
        val device = devices.find {
            it.deviceName == deviceName
        } ?: throw DmxException("Cannot find device with name $deviceName")

        val port = device.outputPortList.find {
            it.portId == portIndex
        } ?: throw DmxException("Cannot find output port $portIndex in $deviceName")

        return if (port.hasUniverse() && port.universe >= 0) {
            log.v("Found universe ${port.universe}. Reusing it.")
            DmxSessionImpl(port.universe)
        } else {
            val universes = universeList.universeList
            val nextFreeUniverseIndex = when (universes.size) {
                0 -> 1
                1 -> universes.first().universe + 1
                else -> 1 + universes
                        .map { it.universe }
                        .reduce { u1, u2 -> Math.max(u1, u2) }
            }
            log.v("No existing universe found patching to universe $nextFreeUniverseIndex")
            patchPort(device.deviceAlias, port.portId, PatchAction.PATCH, nextFreeUniverseIndex)
            DmxSessionImpl(nextFreeUniverseIndex)
        }
    }

    private fun callRpcMethod(method: String, inputMessage: Message): Message {

        val outputMessage = arrayOfNulls<Message>(1)
        controller.reset()

        val cb = RpcCallback<Message> { arg0 -> outputMessage[0] = arg0 }

        serverService.callMethod(serverService.descriptorForType.findMethodByName(method), controller, inputMessage, cb)

        if (controller.failed()) {
            log.w("RPC Call failed: " + controller.errorText())
            throw DmxException("RPC Call failed: " + controller.errorText())
        } else if (outputMessage[0] == null) {
            log.w("RPC Call failed: no output message")
            throw DmxException("RPC Call failed: no output message")
        }

        return outputMessage[0]!!
    }

    private fun callRpcprocedure(method: String, inputMessage: Message) {

        val outputMessage = arrayOfNulls<Message>(1)
        controller.reset()

        val cb = RpcCallback<Message> { arg0 -> outputMessage[0] = arg0 }

        serverService.callMethod(serverService.descriptorForType.findMethodByName(method), controller, inputMessage, cb)

        if (controller.failed()) {
            log.w("RPC Call failed: " + controller.errorText())
            throw DmxException("RPC Call failed: " + controller.errorText())
        }
    }

    private fun getCandidatePorts(universe: Int): DeviceInfoReply {
        val request = OptionalUniverseRequest.newBuilder().setUniverse(universe).build()
        return callRpcMethod("GetCandidatePorts", request) as DeviceInfoReply
    }


    private fun configureDevice(device: Int, data: ShortArray): DeviceConfigReply {
        val request = DeviceConfigRequest.newBuilder()
                .setDeviceAlias(device)
                .setData(convertToUnsigned(data))
                .build()
        return callRpcMethod("ConfigureDevice", request) as DeviceConfigReply
    }

    private fun getUniverseInfo(universe: Int): UniverseInfoReply {
        val request = OptionalUniverseRequest.newBuilder().setUniverse(universe).build()
        return callRpcMethod("GetUniverseInfo", request) as UniverseInfoReply
    }

    private fun patchPort(deviceAlias: Int, port: Int, action: PatchAction, universe: Int): Boolean {

        val patchRequest = PatchPortRequest.newBuilder()
                .setPortId(port)
                .setAction(action)
                .setDeviceAlias(deviceAlias)
                .setUniverse(universe)
                .setIsOutput(true)
                .build()

        return callRpcMethod("PatchPort", patchRequest) != null
    }

    private fun registerForDmx(universe: Int, action: RegisterAction): Boolean {
        val request = RegisterDmxRequest.newBuilder()
                .setUniverse(universe)
                .setAction(action)
                .build()
        return callRpcMethod("RegisterForDmx", request) != null
    }

    private fun streamDmx(universe: Int, values: ShortArray) {

        val dmxData = DmxData.newBuilder()
                .setUniverse(universe)
                .setData(convertToUnsigned(values))
                .build()

        callRpcprocedure("StreamDmxData", dmxData)
    }

    class DmxException(msg: String) : RuntimeException(msg)

    private inner class DmxSessionImpl(private val universe: Int) : DmxSession {
        override fun sendDmx(values: ShortArray) {
            streamDmx(universe, values)
        }
    }

    companion object {

        /**
         * Convert short array to bytestring
         */
        fun convertToUnsigned(values: ShortArray): ByteString {
            val unsigned = ByteArray(values.size)
            for (i in values.indices) {
                unsigned[i] = values[i].toByte()
            }
            return ByteString.copyFrom(unsigned)
        }


        /**
         * Convert bytestring to short array.
         */
        fun convertFromUnsigned(data: ByteString): ShortArray {
            val values = data.toByteArray()
            val signed = ShortArray(values.size)
            for (i in values.indices) {
                signed[i] = (values[i].toShort() and 0xFF).toShort()
            }
            return signed
        }
    }
}