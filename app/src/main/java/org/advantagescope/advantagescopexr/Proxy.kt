package org.advantagescope.advantagescopexr

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets
import kotlin.time.Duration.Companion.seconds

fun Application.configureResources() {
    install(Resources)
}

fun Application.configureWebsockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}

fun Application.configureRouting() {
    val client = HttpClient()
    val upstream = "100.107.1.122"
    val upstreamPort = 56328
    routing {
        get {
            println("request got ${call.request.uri}")
            call.respondBytes(client.get("http://$upstream:$upstreamPort${call.request.uri}").readRawBytes())
        }
        webSocket("/ws") { // websocketSession
            val upstream = client.webSocketSession(
                method = HttpMethod.Get,
                host = upstream,
                port = upstreamPort,
                path = "/ws"
            )
            for (frame in upstream.incoming) {
                outgoing.send(frame)
            }
            for (frame in incoming) {
                upstream.outgoing.send(frame)
            }
        }
    }
}