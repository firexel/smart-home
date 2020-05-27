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
import com.google.protobuf.Descriptors.MethodDescriptor
import ola.rpc.Rpc.RpcMessage
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Basic RPC Channel implementation.  All calls are done
 * synchronously.
 *
 * The RPC Channel is hard coded to localhost 9010 where the
 * olad daemon is running.
 */
class StreamRpcChannel(
        private val oladHost: String,
        private val oladPort: Int
) : RpcChannel {

    private var socket: Socket? = null

    private var bos: BufferedOutputStream? = null

    private var bis: BufferedInputStream? = null

    private var sequence = 0


    init {
        connect()
    }


    /**
     * Open connection to olad daemon.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun connect() {

        if (socket != null && socket!!.isConnected) {
            logger.warning("Socket already connected.")
            return
        }

        try {
            socket = Socket(oladHost, oladPort)
            bos = BufferedOutputStream(socket!!.getOutputStream())
            bis = BufferedInputStream(socket!!.getInputStream())
        } catch (e: Exception) {
            logger.severe("Error connecting. Make sure the olad daemon is running on port 9010")
            throw e
        }

    }


    /**
     * Close Rpc Channel.
     */
    fun close() {

        if (socket != null && socket!!.isConnected) {
            try {
                socket!!.close()
            } catch (e: Exception) {
                logger.warning("Error closing socket. " + e.message)
            }

        }
    }


    /* (non-Javadoc)
     * @see com.google.protobuf.RpcChannel#callMethod(com.google.protobuf.Descriptors.MethodDescriptor, com.google.protobuf.RpcController, com.google.protobuf.Message, com.google.protobuf.Message, com.google.protobuf.RpcCallback)
     */
    override fun callMethod(method: MethodDescriptor, controller: RpcController,
                            requestMessage: Message, responseMessage: Message, done: RpcCallback<Message>?) {
        var responseMessage = responseMessage

        val messageId = sequence++

        val message = RpcMessage.newBuilder()
                .setType(ola.rpc.Rpc.Type.REQUEST)
                .setId(messageId)
                .setName(method.name)
                .setBuffer(requestMessage.toByteString())
                .build()

        try {

            sendMessage(message)
            if (responseMessage.descriptorForType.name == "STREAMING_NO_RESPONSE") {
                // don't wait for response on streaming messages..
                return
            }

            val response = readMessage()

            if (response.type == ola.rpc.Rpc.Type.RESPONSE) {
                if (response.id != messageId) {
                    controller.setFailed("Received message with id " + response.id + " , but was expecting " + messageId)
                } else {
                    responseMessage = DynamicMessage.parseFrom(responseMessage.descriptorForType, response.buffer)
                    done?.run(responseMessage)
                }
            } else {
                controller.setFailed("No valid response received !")
            }


        } catch (e: Exception) {

            logger.severe("Error sending rpc message: " + e.message)
            controller.setFailed(e.message)
            done!!.run(null)
        }


    }


    /**
     * Send rpc message to olad.
     *
     * @param msg RpcMessage
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun sendMessage(msg: RpcMessage) {

        val data = msg.toByteArray()

        var headerContent = PROTOCOL_VERSION shl 28 and VERSION_MASK
        headerContent = headerContent or (data.size and SIZE_MASK)
        val header = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(headerContent).array()

        if (logger.isLoggable(Level.FINEST)) {
            logger.info("Sending header " + header.size + " bytes")
            for (b in header) {
                System.out.format("0x%x ", b)
            }
            logger.info("Sending data " + data.size + " bytes")
            for (b in data) {
                System.out.format("0x%x ", b)
            }
        }

        bos!!.write(header)
        bos!!.write(data)
        bos!!.flush()
    }


    /**
     * @return RpcMessage read back from olad.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun readMessage(): RpcMessage {

        val header = ByteArray(4)
        bis!!.read(header)

        val headerValue = ByteBuffer.wrap(header).order(ByteOrder.nativeOrder()).int
        val size = headerValue and SIZE_MASK

        val data = ByteArray(size)
        bis!!.read(data)

        if (logger.isLoggable(Level.FINEST)) {
            logger.info("Received header ")
            for (b in header) {
                System.out.format("0x%x ", b)
            }
            logger.info("Received data ")
            for (b in data) {
                System.out.format("0x%x ", b)
            }
        }

        return RpcMessage.parseFrom(data)
    }

    companion object {

        private val logger = Logger.getLogger(StreamRpcChannel::class.java.name)

        private val PROTOCOL_VERSION = 1

        private val VERSION_MASK = -0x10000000

        private val SIZE_MASK = 0x0fffffff
    }
}