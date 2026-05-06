package org.advantagescope.advantagescopexr

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.widget.Toast
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

const val UPSTREAM_PORT = 56328
class ProxyService : Service() {
    var scope: CoroutineScope? = null
    val client = setupClient()
    var upstream = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope?.cancel()
        scope = CoroutineScope(Job() + Dispatchers.Main)

        val fingerprint = intent!!.getStringExtra("fingerprint")!!

        scope!!.launch {
            val server = embeddedServer(Netty, configure = {
                connectors.add(EngineConnectorBuilder().apply {
                    host = "127.0.0.1"
                    port = 56328
                })
                enableH2c = true
            }) {
                install(Resources)
                install(WebSockets) {
                    pingPeriod = 15.seconds
                    timeout = 15.seconds
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                routing {
                    post("/setupstream") {
                        upstream = call.receiveText()
                        call.respondText(upstream, ContentType.Text.Plain)
                    }
                    get("/.well-known/assetlinks.json") { // https://developers.google.com/digital-asset-links/v1/getting-started
                        call.respondText("""
                            [{
                              "relation": ["delegate_permission/common.handle_all_urls"],
                              "target" : { "namespace": "android_app", "package_name": "org.advantagescope.advantagescopexr",
                                           "sha256_cert_fingerprints": ["$fingerprint"] }
                            }]""".trimIndent(),
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK
                        )
                    }
                    get("/{...}") {
                        call.application.environment.log.info("request got ${call.request.uri}")
                        val upstreamReq = client.get("http://$upstream:$UPSTREAM_PORT${call.request.uri}")
                        call.respondBytes(upstreamReq.readRawBytes(), upstreamReq.contentType(), upstreamReq.status)
                    }
                    webSocket("/ws") { // websocketSession
                        val upstream = client.webSocketSession(
                            method = HttpMethod.Get,
                            host = upstream,
                            port = UPSTREAM_PORT,
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
                configureRouting()
            }
            server.startSuspend(wait = true)
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope?.cancel()
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

// has to be its own function due to scoping issues
fun setupClient(): HttpClient = HttpClient(CIO) {
    install(io.ktor.client.plugins.websocket.WebSockets)
}

fun Application.configureRouting() {

}