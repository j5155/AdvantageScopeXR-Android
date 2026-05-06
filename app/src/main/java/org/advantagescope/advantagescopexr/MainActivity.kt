package org.advantagescope.advantagescopexr

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.browser.trusted.TrustedWebActivityIntentBuilder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.advantagescope.advantagescopexr.ui.theme.AdvantageScopeXRTheme
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


const val LOCAL_URL = "http://localhost:56328"
val LOCAL_URI = LOCAL_URL.toUri()

class MainActivity : ComponentActivity() {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
        )
        .enableAutoZoom()
        .build()

    lateinit var scanner: GmsBarcodeScanner

    // Source - https://stackoverflow.com/a/17552838
    // Posted by Yojimbo, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-05-06, License - CC BY-SA 3.0
    @RequiresApi(Build.VERSION_CODES.P)
    fun getFingerprint(): String {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES;
        val packageInfo = packageManager.getPackageInfo(packageName, flags)
        val signatures = packageInfo.signingInfo!!.signingCertificateHistory
        val cert = signatures[0].toByteArray()
        val input = ByteArrayInputStream(cert)

        val cf = CertificateFactory.getInstance("X509")
        val c = cf.generateCertificate(input) as X509Certificate

        val md = MessageDigest.getInstance("SHA256")

        val publicKey = md.digest(c.publicKey.encoded)

        // Source - https://stackoverflow.com/a/23404646
        // Posted by theHacker, modified by community. See post 'Timeline' for change history
        // Retrieved 2026-05-06, License - CC BY-SA 4.0
        val hexString = bytesToHex(publicKey).replace("..(?!$)".toRegex(), "$0:")

        println("publicKey: $hexString")
        return hexString
    }


    // Source - https://stackoverflow.com/a/9855338
    // Posted by maybeWeCouldStealAVan, modified by community. See post 'Timeline' for change history
    // Retrieved 2026-05-06, License - CC BY-SA 4.0
    val HEX_ARRAY: ByteArray = "0123456789ABCDEF".toByteArray(StandardCharsets.US_ASCII)
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = ByteArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars, StandardCharsets.UTF_8)
    }


    val client = HttpClient()



    fun startFromUrl(url: String) {
        val upstream = url.removeSuffix(":$UPSTREAM_PORT")

        // yes, this is easier than using the android service communication api
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        scope.launch {
            client.post(LOCAL_URL) {
                url {
                    appendPathSegments("setupstream")
                }
                setBody(upstream)
            }
            if (mSession != null) {
                val intent = TrustedWebActivityIntentBuilder(LOCAL_URI).build(mSession!!).intent
                startActivity(intent)
            } else { // no custom tabs support, open in browser
                val intent = Intent(Intent.ACTION_VIEW,LOCAL_URI)
                startActivity(intent)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // if app is too old to support accessing the real fingerprint, here's some random default so nothing gets mad that it's missing
        var fingerprint = "F6:F0:1F:2D:E9:B6:3C:6E:1A:B3:A3:E5:56:C2:4E:B4:A4:AC:43:26:71:BF:DE:34:C6:36:A3:A4:B1:ED:B5:C4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            fingerprint = getFingerprint()
        }


        val proxyIntent = Intent(this, ProxyService::class.java)
        proxyIntent.putExtra("fingerprint", fingerprint)

        startService(proxyIntent)



        scanner = GmsBarcodeScanning.getClient(this, options)

        bindCustomTabService(this)
        enableEdgeToEdge()
        setContent {
            AdvantageScopeXRTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Button(
                        onClick = {
                            println("scan start")
                            // THE SCAN RANDOMLY STARTED FAILING FOR NO REASON.
                            // https://stackoverflow.com/questions/76075230/how-to-fix-google-code-scanner-throwing-mlkitexception-failed-to-scan-code
                            // stopping here 5/5/25
                            scanner.startScan().addOnSuccessListener {
                            println("12087 scan success")
                            startFromUrl(it.url!!.url!!)
                        }.addOnFailureListener {
                            it.printStackTrace()
                            println("12087 scan fail")
                        }.addOnCanceledListener {
                            println("12087 scan cancel")
                        }
                                  },
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

