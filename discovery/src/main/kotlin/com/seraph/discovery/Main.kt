package com.seraph.discovery

import com.seraph.discovery.model.ResponsesList
import com.seraph.discovery.server.DiscoveryServer
import com.seraph.smarthome.util.ConsoleLog
import com.seraph.smarthome.util.Log
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileInputStream

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            Application(argv).run()
        }
    }
}

class CommandLineParams(parser: ArgParser) {
    val configFile by parser.storing("-c", "--config", help = "config storage folder")
        .default("/etc/conf/discovery.conf")
}

@OptIn(ExperimentalSerializationApi::class)
class Application(argv: Array<String>) {

    private val log: Log
    private val params: CommandLineParams

    init {
        log = ConsoleLog("Discovery")
        log.i("Started with following params: ${argv.asList()}")
        params = CommandLineParams(ArgParser(argv))
    }

    fun run() {
        val responses: ResponsesList = Json.decodeFromStream(FileInputStream(params.configFile))
        runBlocking { DiscoveryServer(responses.responses, log.copy("Server")).serve() }
    }
}