package org.advantagescope.advantagescopexr

import android.R.attr.configure
import android.R.attr.label
import android.R.attr.onClick
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.trusted.TrustedWebActivityIntent
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
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
import androidx.core.net.toUri
import io.ktor.server.util.url
import kotlinx.coroutines.CoroutineScope

val uri = "http://127.0.0.1:56328".toUri()

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

        startService(Intent(this, ProxyService::class.java))

        scanner = GmsBarcodeScanning.getClient(this, options)

        bindCustomTabService(this)
        enableEdgeToEdge()
        setContent {
            AdvantageScopeXRTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Button(
                        onClick = { scan() },
                        content = {
                            Text(
                                text = "Scan"
                            )
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                    Greeting(
                        name = "Android AdvantageScope test two",
                        modifier = Modifier.padding(innerPadding)
                    )
                    Button(
                        onClick = {
                            if (mSession != null) {
                                val intent = TrustedWebActivityIntentBuilder(uri).build(mSession!!).intent
                                startActivity(intent)
                            } else { // no custom tabs support, open in browser
                                val intent = Intent(Intent.ACTION_VIEW,uri)
                                startActivity(intent)
                            }

                        },
                        modifier = Modifier.padding(innerPadding),
                        content = {
                            Text("Open")
                        })
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

