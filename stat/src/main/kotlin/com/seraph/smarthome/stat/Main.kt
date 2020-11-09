package com.seraph.smarthome.stat

import com.seraph.smarthome.util.ConsoleLog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Created by aleksandr.naumov on 09.10.2020
 */

class Main {
    companion object {
        @JvmStatic
        fun main(argv: Array<String>) {
            val log = ConsoleLog("Stat").apply { i("Starting...") }
            val config = readConfig(File("config.json"))
            log.i("Config is $config")
        }
    }
}