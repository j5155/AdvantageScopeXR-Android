package org.advantagescope.advantagescopexr

import android.R.attr.configure
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.advantagescope.advantagescopexr.ui.theme.AdvantageScopeXRTheme

class MainActivity : ComponentActivity() {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
        )
        .enableAutoZoom()
        .build()

    lateinit var scanner: GmsBarcodeScanner

    fun scan() {
        scanner.startScan().addOnSuccessListener {
            println(it.url?.url)
        }
            .addOnCanceledListener {
                println("canceled")
            }
            .addOnFailureListener {
                println("failure")
            }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        embeddedServer(Netty, configure = {
            connectors.add(EngineConnectorBuilder().apply {
                host = "127.0.0.1"
                port = 8080
            })
            enableH2c = true
        })

        scanner = GmsBarcodeScanning.getClient(this, options)
        enableEdgeToEdge()
        setContent {
            AdvantageScopeXRTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {LabelButton("scan") { scan() } }) { innerPadding ->
                    Greeting(
                        name = "Android AdvantageScope test two",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AdvantageScopeXRTheme {
        Greeting("Android")
    }
}

@Composable
fun LabelButton(label: String, callback: () -> Unit) {
    Button(
        onClick = callback,
        content = {
            Text(
                text = label
            )
        },
    )
}

