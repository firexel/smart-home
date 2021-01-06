package com.seraph.smarthome.transport.impl

import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.util.Log
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory
import java.io.File
import java.util.*
import javax.net.SocketFactory
import javax.net.ssl.SSLContext


object Brokers {
    fun unencrypted(addr: String, name: String, log: Log): Broker {
        val options = PahoClientWrapper.Options(
                hostUrl = addr,
                name = name
        )
        return createBroker(options, log)
    }

    fun encrypted(
            addr: String,
            name: String,
            userName: String,
            userPswd: String,
            caFile: File,
            caPswd: String,
            log: Log): Broker {

        val options = PahoClientWrapper.Options(
                hostUrl = addr,
                name = name,
                userName = userName,
                password = userPswd,
                socketFactory = createSslSocketFactory(),
                sslOptions = createSslOptions(caFile, caPswd),
                publishQos = 2,
                subscribeQos = 2
        )
        return createBroker(options, log)
    }

    fun unencrypted(
            addr: String,
            name: String,
            userName: String,
            userPswd: String,
            log: Log): Broker {

        val options = PahoClientWrapper.Options(
                hostUrl = addr,
                name = name,
                userName = userName,
                password = userPswd,
                sslOptions = null,
                publishQos = 2,
                subscribeQos = 2
        )
        return createBroker(options, log)
    }

    private fun createSslOptions(caFile: File, pswd: String): Properties {
        return Properties().apply {
            this[SSLSocketFactoryFactory.TRUSTSTORE] = caFile.absolutePath
            this[SSLSocketFactoryFactory.TRUSTSTORETYPE] = "JKS"
            this[SSLSocketFactoryFactory.TRUSTSTOREPWD] = pswd
            this[SSLSocketFactoryFactory.CLIENTAUTH] = false
        }
    }

    private fun createSslSocketFactory(): SocketFactory {
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(null, null, null)
        return context.socketFactory
    }

    private fun createBroker(options: PahoClientWrapper.Options, log: Log) =
            StatefulMqttBroker(PahoClientWrapper(options, log.copy("Transport")), log)
}