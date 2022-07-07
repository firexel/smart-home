package api

import configuration.ExecutionNote
import configuration.errors
import configuration.warns
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun startServer(installer: ConfigInstaller) {
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            install(ContentNegotiation) {
                json()
            }
            post("config/install") {
                val newConfig = call.receiveText()
                val check = installer.checkConfig(newConfig)
                if (check.errors().isNotEmpty()) {
                    call.respond(
                        HttpStatusCode.OK,
                        ConfigInstallResponse(
                            InstallStatus.NOT_INSTALLED,
                            check.errors().asStrings(),
                            check.warns().asStrings()
                        )
                    )
                }
                val install = installer.installConfig(newConfig)
                val installed = when (install.errors().isNotEmpty()) {
                    true -> InstallStatus.NOT_INSTALLED
                    else -> InstallStatus.OK
                }
                call.respond(
                    HttpStatusCode.OK, ConfigInstallResponse(
                        installed,
                        (check.errors() + install.errors()).asStrings(),
                        (check.warns() + install.warns()).asStrings()
                    )
                )
            }
            post("config/check") {
                val newConfig = call.receiveText()
                val res = installer.checkConfig(newConfig)
                call.respond(
                    HttpStatusCode.OK, ConfigCheckResponse(
                        res.errors().asStrings(),
                        res.warns().asStrings()
                    )
                )
            }
        }
    }
    server.start(wait = true)
}

interface ConfigInstaller {
    suspend fun installConfig(config: String): List<ExecutionNote>
    suspend fun checkConfig(config: String): List<ExecutionNote>
}

fun List<ExecutionNote>.asStrings() = map { it.toString() }