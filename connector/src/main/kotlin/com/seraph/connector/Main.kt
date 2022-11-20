package com.seraph.connector

import com.seraph.connector.api.startApiServer
import com.seraph.connector.configuration.ConfigChecker
import com.seraph.connector.configuration.ConfigStorage
import com.seraph.connector.configuration.FileConfigStorage
import com.seraph.connector.tools.BlockingNetworkImpl
import com.seraph.connector.tree.ConnectorTreeBuilder
import com.seraph.connector.tree.TreeRunner
import com.seraph.connector.usecase.*
import com.seraph.smarthome.domain.Network
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.domain.impl.Topics
import com.seraph.smarthome.threading.ThreadExecutor
import com.seraph.smarthome.transport.Broker
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.WildcardBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.Log
import com.seraph.smarthome.util.NetworkMonitor
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.runBlocking
import java.io.File

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            Application(argv).run()
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
        .default("tcp://broker:1883")

    val configStorage by parser.storing("-c", "--config", help = "config storage folder")
        .default("/etc/conf/connector")
}

class Application(argv: Array<String>) : Cases {

    private val log: Log
    private val params: CommandLineParams
    private val broker: Broker
    private val network: Network
    private val monitor: NetworkMonitor
    private val runner: TreeRunner
    private val storage: ConfigStorage

    init {
        log = ConsoleLog("Connector")
        log.i("Started with following params: ${argv.asList()}")
        params = CommandLineParams(ArgParser(argv))
        storage = FileConfigStorage(File(params.configStorage))
        broker = createBroker()
        network = MqttNetwork(broker, log.copy("Network"))
        monitor = NetworkMonitor(network, log.copy("Monitor"), false)
        monitor.start()
        runner = TreeRunner(log.copy("TreeRunner")) { holder ->
            TODO()
//            ConnectorTreeBuilder(
//                BlockingNetworkImpl(network, log.copy("BlockingNetwork")),
//                holder,
//
//                log.copy("ConfigRunner")
//            )
        }
    }

    private fun createBroker(): Broker {
        val inner = Brokers.unencrypted(
            params.brokerAddress, "Connector",
            log.copy("Broker")
        )
        return WildcardBroker(
            inner, ThreadExecutor(),
            log.copy("WildcardBroker"),
            listOf(Topics.structure())
        )
    }

    fun run() {
        runBlocking { applyConfig().applyLatestOrDefault() }
        startApiServer(this@Application)
    }

    override fun applyConfig(): ApplyConfigCase = ApplyConfigCase(
        ConfigChecker(monitor),
        runner,
        storage,
        log.copy("ApplyConfigCase")
    )

    override fun checkConfig(config: String): CheckConfigCase = CheckConfigCase(
        config,
        ConfigChecker(monitor)
    )

    override fun listConfigs(): ListConfigsCase = ListConfigsCase(storage)

    override fun dumpNetwork(): DumpNetworkStateCase = DumpNetworkStateCase(monitor)
}