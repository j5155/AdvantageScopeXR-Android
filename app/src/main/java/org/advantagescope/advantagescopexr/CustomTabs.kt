package org.advantagescope.advantagescopexr

import android.content.ComponentName
import android.content.Context
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession

// https://developer.chrome.com/docs/android/custom-tabs/guide-warmup-prefetch

var mClient: CustomTabsClient? = null
var mSession: CustomTabsSession? = null

val mConnection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        mClient = client
        mClient?.warmup(0L)


        mSession = client.newSession(CustomTabsCallback())!!;
    }
    override fun onServiceDisconnected(name: ComponentName) {
        mClient = null
        mSession = null
    }
}

fun bindCustomTabService(context: Context) {
    if (mClient != null) {
        return
    }

    val packageName = CustomTabsClient.getPackageName(context,null)
    if (packageName != null) {
        CustomTabsClient.bindCustomTabsService(context, packageName,mConnection)
    }
}