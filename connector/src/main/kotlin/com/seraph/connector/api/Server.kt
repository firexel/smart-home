package com.seraph.connector.api

import com.seraph.connector.configuration.ConfigStorage
import com.seraph.connector.configuration.ExecutionNote
import com.seraph.connector.configuration.errors
import com.seraph.connector.configuration.warns
import com.seraph.connector.usecase.Cases
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import java.time.format.DateTimeFormatter

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun ConfigStorage.ConfigHeader.toResponse(): ConfigDataResponse {
    return ConfigDataResponse(id.hash, dateCreated.format(formatter))
}

fun startApiServer(cases: Cases) {
    val server = embeddedServer(Netty, port = 8888) {
        routing {
            install(ContentNegotiation) {
                json()
            }
            post("config/list") {
                val list = cases.listConfigs().run().map { it.toResponse() }
                call.respond(
                    HttpStatusCode.OK, ConfigsListResponse(list)
                )
            }
            post("config/reapply") {
                val id = call.request.queryParameters.getOrFail("id")
                val confId = ConfigStorage.ConfigHeader.Id(id)
                val applicationResult = cases.applyConfig().apply(confId)
                val notes = applicationResult.notes
                val installed = when (notes.errors().isNotEmpty()) {
                    true -> ApplyStatus.NOT_APPLIED
                    else -> ApplyStatus.OK
                }
                call.respond(
                    HttpStatusCode.OK, ConfigApplyResponse(
                        installed,
                        notes.errors().asStrings(),
                        notes.warns().asStrings(),
                        applicationResult.configHeader?.toResponse()
                    )
                )
            }
            post("config/apply") {
                val newConfig = call.receiveText()
                val applicationResult = cases.applyConfig().apply(newConfig)
                val notes = applicationResult.notes
                val installed = when (notes.errors().isNotEmpty()) {
                    true -> ApplyStatus.NOT_APPLIED
                    else -> ApplyStatus.OK
                }
                call.respond(
                    HttpStatusCode.OK, ConfigApplyResponse(
                        installed,
                        notes.errors().asStrings(),
                        notes.warns().asStrings(),
                        applicationResult.configHeader?.toResponse()
                    )
                )
            }
            post("config/check") {
                val newConfig = call.receiveText()
                val res = cases.checkConfig(newConfig).run()
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

fun List<ExecutionNote>.asStrings() = map { it.toString() }