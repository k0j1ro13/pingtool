package com.pingmonitor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val AD_UNIT_ID = "ca-app-pub-5822507697217607/2431956981"

@Composable
actual fun AdBanner() {
    val adRequest = remember { AdRequest.Builder().build() }
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AD_UNIT_ID
                loadAd(adRequest)
            }
        },
        modifier = Modifier
    )
}
