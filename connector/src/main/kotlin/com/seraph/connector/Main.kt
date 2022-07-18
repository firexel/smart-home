import com.seraph.connector.api.startServer
import com.seraph.smarthome.domain.impl.MqttNetwork
import com.seraph.smarthome.transport.impl.Brokers
import com.seraph.smarthome.transport.impl.LocalBroker
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.NetworkMonitor
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.seraph.connector.configuration.ConfigChecker
import com.seraph.connector.configuration.EvalConfigInstaller

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Connector").apply { i("Starting...") }
            log.i("Started with following params: ${argv.asList()}")
            val params = CommandLineParams(ArgParser(argv))
            val broker = Brokers.unencrypted(params.brokerAddress, "Connector", log.copy("Broker"))
            val network = MqttNetwork(LocalBroker(broker), log.copy("Network"))
            val networkMonitor = NetworkMonitor(network, log.copy("Monitor"), false)
            startServer(EvalConfigInstaller { ConfigChecker(networkMonitor) })
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val brokerAddress by parser.storing("-b", "--broker", help = "ip or domain of the mqtt broker")
        .default("tcp://localhost:1883")
}